package net.runelite.client.plugins.tickcd;

import com.google.common.base.Splitter;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.AnimationID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = "<html><font color=#b82584>[J] Boss Ticks",
        description = "Shows a tick counter over NPCs after configured animations",
        tags = {"boss", "tick", "counter", "animation", "npc", "overlay"},
        enabledByDefault = false
)
@Slf4j
public class TickcdPlugin extends Plugin {
    private static final Splitter SPLITTER = Splitter.on("\n").omitEmptyStrings().trimResults();
    private static final String CONFIG_GROUP = "tickcd";
    private static final String ALL_NPC_CONFIG_KEY = "allNPC";
    // NPCs whose flinch (defend) animation can replace their attack animation, so the attack
    // isn't seen and the counter would run out. Keyed by flinch animation.
    private static final Map<Integer, FlinchNpc> FLINCH_NPCS = Map.of(
            // Idle blobs are handled by line of sight in JalAkTracker, so no first-attack start here.
            AnimationID.JALAK_DEFEND, new FlinchNpc("jal-ak", -1, 0),
            // Bats attack as soon as they reach you, so a flinch in range can hide their first attack.
            // Range 4 is from the OpenOSRS Inferno plugin.
            AnimationID.JALMEJRAH_DEFEND, new FlinchNpc("jal-mejrah", AnimationID.JALMEJRAH_ATTACK, 4)
    );
    // A flinch this close to the end of a counter is treated as covering an attack.
    private static final int FLINCH_ATTACK_WINDOW_TICKS = 1;
    private static final int NPC_NAME_INDEX = 0;
    private static final int ENTRY_SIZE = 3;

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private TickcdOverlay overlay;

    @Inject
    private TickcdConfig config;

    final Map<NPC, NpcInfo> npcInfos = new HashMap<>();
    // Keyed by animation id so the hot path (every NPC animation in the game) is an int lookup
    // and only animations that appear in the config pay for a name lookup.
    private final Map<Integer, List<AnimationRule>> animationRules = new HashMap<>();

    // Created in startUp(), after injection; events are only delivered while the plugin is running.
    private JalAkTracker jalAk;
    private OlmTracker olm;

    @Provides
    TickcdConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(TickcdConfig.class);
    }

    @Override
    protected void startUp() {
        jalAk = new JalAkTracker(client, npcInfos);
        olm = new OlmTracker(config, npcInfos);
        reset();
        rebuildAnimationRules();
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() {
        reset();
        overlayManager.remove(overlay);
    }

    private void reset() {
        npcInfos.clear();
        jalAk.reset();
        olm.reset();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (CONFIG_GROUP.equals(event.getGroup()) && ALL_NPC_CONFIG_KEY.equals(event.getKey())) {
            rebuildAnimationRules();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        // LOADING fires on every scene reload (including inside instances), which used to wipe
        // live counters and the Olm phase mid-fight. Despawns already clean up NPCs that leave.
        GameState state = event.getGameState();
        if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING || state == GameState.CONNECTION_LOST) {
            reset();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        for (NpcInfo curr : npcInfos.values()) {
            --curr.ticks;
        }

        jalAk.startTick();

        Iterator<NpcInfo> iterator = npcInfos.values().iterator();
        while (iterator.hasNext()) {
            NpcInfo curr = iterator.next();
            jalAk.onCounterTick(curr);

            if (curr.ticks <= 0 || curr.currNPC.isDead()) {
                if (olm.restartIfOlm(curr)) {
                    continue;
                }
                iterator.remove();
            }
        }

        jalAk.endTick();
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        Actor actor = event.getActor();
        if (!(actor instanceof NPC)) {
            return;
        }

        NPC npc = (NPC) actor;
        int animation = npc.getAnimation();
        FlinchNpc flinch = FLINCH_NPCS.get(animation);
        // Only swallow the flinch if it started or restarted a counter; otherwise let it reach the
        // configured animation list in case the flinch animation itself has a rule.
        if (flinch != null && flinch.name.equalsIgnoreCase(npc.getName()) && handleFlinch(npc, flinch)) {
            return;
        }

        List<AnimationRule> rules = animationRules.get(animation);
        if (rules == null) {
            return;
        }

        String name = npc.getName();
        if (name == null) {
            return;
        }

        String npcName = name.toLowerCase(Locale.ROOT);
        for (AnimationRule rule : rules) {
            if (!rule.npcName.equals(npcName)) {
                continue;
            }

            Color selectedColor = getConfiguredColor(rule.colorNumber);
            NpcInfo existingNpc = npcInfos.get(npc);
            if (existingNpc != null) {
                existingNpc.ticks = rule.ticks;
                existingNpc.color = selectedColor;
                existingNpc.attackTicks = rule.ticks;
                existingNpc.attackColor = selectedColor;
                existingNpc.awaitingAttack = false;
            } else {
                npcInfos.put(npc, new NpcInfo(npc, rule.ticks, selectedColor));
            }
            return;
        }
    }

    /**
     * A flinch can replace the attack animation, so the attack is never seen:
     * - with a counter on its last tick, restart it for the attack cycle instead of letting it expire;
     * - with no counter, if the NPC can attack you right now, treat the flinch as its first attack.
     */
    private boolean handleFlinch(NPC npc, FlinchNpc flinch) {
        NpcInfo info = npcInfos.get(npc);
        if (info != null) {
            if (info.ticks > FLINCH_ATTACK_WINDOW_TICKS) {
                return false;
            }
            info.ticks = info.attackTicks;
            info.color = info.attackColor;
            info.awaitingAttack = false;
            return true;
        }

        if (flinch.attackAnimation < 0) {
            return false;
        }
        AnimationRule rule = findRule(flinch.attackAnimation, flinch.name);
        Player player = client.getLocalPlayer();
        WorldView worldView = client.getTopLevelWorldView();
        if (rule == null || player == null || worldView == null
                || !NpcLineOfSight.canAttack(worldView, npc, player.getWorldLocation(), flinch.range)) {
            return false;
        }

        npcInfos.put(npc, new NpcInfo(npc, rule.ticks, getConfiguredColor(rule.colorNumber)));
        return true;
    }

    private AnimationRule findRule(int animation, String npcName) {
        List<AnimationRule> rules = animationRules.get(animation);
        if (rules == null) {
            return null;
        }
        for (AnimationRule rule : rules) {
            if (rule.npcName.equalsIgnoreCase(npcName)) {
                return rule;
            }
        }
        return null;
    }

    private void rebuildAnimationRules() {
        animationRules.clear();
        jalAk.resetAttackRule();

        for (String str : SPLITTER.splitToList(config.allNPC())) {
            String[] stringList = str.split(",", -1);
            if (stringList.length <= ENTRY_SIZE || (stringList.length - 1) % ENTRY_SIZE != 0) {
                log.warn("Ignoring malformed NPC animation configuration: {}", str);
                continue;
            }

            String npcName = stringList[NPC_NAME_INDEX].trim();
            if (npcName.isEmpty()) {
                continue;
            }

            String npcKey = npcName.toLowerCase(Locale.ROOT);
            int numEntries = (stringList.length - 1) / ENTRY_SIZE;
            for (int i = 0; i < numEntries; i++) {
                int entryIndex = (i * ENTRY_SIZE) + 1;
                try {
                    int animation = Integer.parseInt(stringList[entryIndex].trim());
                    int configuredTicks = Integer.parseInt(stringList[entryIndex + 1].trim());
                    int colorNumber = Integer.parseInt(stringList[entryIndex + 2].trim());
                    if (configuredTicks <= 0 || colorNumber < 1 || colorNumber > 5) {
                        log.warn("Ignoring invalid NPC animation rule: {}", str);
                        continue;
                    }

                    int ticks = configuredTicks + 1;
                    animationRules.computeIfAbsent(animation, k -> new ArrayList<>())
                            .add(new AnimationRule(npcKey, ticks, colorNumber));
                    if (JalAkTracker.NAME.equalsIgnoreCase(npcKey)) {
                        jalAk.addAttackRule(animation, ticks);
                    }
                } catch (NumberFormatException ex) {
                    log.warn("Ignoring invalid NPC animation rule: {}", str);
                }
            }
        }
    }

    private Color getConfiguredColor(int colorNumber) {
        switch (colorNumber) {
            case 2:
                return config.npcColor2();
            case 3:
                return config.npcColor3();
            case 4:
                return config.npcColor4();
            case 5:
                return config.npcColor5();
            default:
                return config.npcColor();
        }
    }

    @Subscribe
    public void onNpcSpawned(final NpcSpawned event) {
        jalAk.onNpcSpawned(event.getNpc());
    }

    @Subscribe
    public void onNpcDespawned(final NpcDespawned event) {
        final NPC npc = event.getNpc();
        npcInfos.remove(npc);
        jalAk.onNpcDespawned(npc);
        olm.onNpcDespawned(npc);
    }

    @Subscribe
    public void onNpcChanged(final NpcChanged event) {
        olm.onNpcChanged(event.getNpc());
    }

    private static class FlinchNpc {
        private final String name;
        // Attack animation whose configured rule a first-attack flinch starts; -1 for none.
        private final int attackAnimation;
        private final int range;

        private FlinchNpc(String name, int attackAnimation, int range) {
            this.name = name;
            this.attackAnimation = attackAnimation;
            this.range = range;
        }
    }

    private static class AnimationRule {
        private final String npcName;
        private final int ticks;
        private final int colorNumber;

        private AnimationRule(String npcName, int ticks, int colorNumber) {
            this.npcName = npcName;
            this.ticks = ticks;
            this.colorNumber = colorNumber;
        }
    }
}
