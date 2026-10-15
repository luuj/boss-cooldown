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
import net.runelite.api.HeadIcon;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = "<html><font color=#b82584>[J] Boss Ticks",
        description = "",
        tags = {"boss", "tick"},
        enabledByDefault = false
)
@Slf4j
public class TickcdPlugin extends Plugin {
    private static final Splitter SPLITTER = Splitter.on("\n").omitEmptyStrings().trimResults();
    private static final String CONFIG_GROUP = "tickcd";
    private static final String ALL_NPC_CONFIG_KEY = "allNPC";
    private static final int OLM_HEAD_NPC_ID = NpcID.OLM_HEAD;
    private static final String JAL_AK_NAME = "Jal-Ak";
    private static final int JAL_AK_BLOCK_ANIMATION = AnimationID.JALAK_DEFEND;
    private static final int JAL_AK_BLOCK_ATTACK_WINDOW_TICKS = 1;
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

    private boolean olmActive;
    private short olmPhase;

    @Provides
    TickcdConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(TickcdConfig.class);
    }

    @Override
    protected void startUp() {
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
        olmPhase = 1;
        olmActive = false;
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
        if (npcInfos.isEmpty()) {
            return;
        }

        Iterator<NpcInfo> iterator = npcInfos.values().iterator();
        while (iterator.hasNext()) {
            NpcInfo curr = iterator.next();
            --curr.ticks;

            if (curr.ticks == 3 && JAL_AK_NAME.equalsIgnoreCase(curr.currNPC.getName())) {
                Player localPlayer = client.getLocalPlayer();
                HeadIcon overhead = localPlayer != null ? localPlayer.getOverheadIcon() : null;
                if (overhead == HeadIcon.MAGIC) {
                    curr.color = Color.GREEN;
                } else if (overhead == HeadIcon.RANGED) {
                    curr.color = Color.CYAN;
                }
            }

            if (curr.ticks <= 0 || curr.currNPC.isDead()) {
                if (olmActive && curr.currNPC.getId() == OLM_HEAD_NPC_ID && !curr.currNPC.isDead() && config.enableOlm()) {
                    curr.ticks += 4;
                    continue;
                }

                iterator.remove();
            }
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        Actor actor = event.getActor();
        if (!(actor instanceof NPC)) {
            return;
        }

        NPC npc = (NPC) actor;
        int animation = npc.getAnimation();
        if (animation == JAL_AK_BLOCK_ANIMATION && JAL_AK_NAME.equalsIgnoreCase(npc.getName())) {
            handleJalAkBlockAnimation(npc);
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
            } else {
                npcInfos.put(npc, new NpcInfo(npc, rule.ticks, selectedColor));
            }
            return;
        }
    }

    private void handleJalAkBlockAnimation(NPC npc) {
        NpcInfo npcInfo = npcInfos.get(npc);
        if (npcInfo == null) {
            return;
        }

        if (npcInfo.ticks <= JAL_AK_BLOCK_ATTACK_WINDOW_TICKS) {
            npcInfo.ticks = npcInfo.attackTicks;
            npcInfo.color = npcInfo.attackColor;
        }
    }

    private void rebuildAnimationRules() {
        animationRules.clear();

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
    public void onNpcDespawned(final NpcDespawned event) {
        final NPC npc = event.getNpc();
        npcInfos.remove(npc);

        if (npc.getId() == OLM_HEAD_NPC_ID && config.enableOlm()) {
            if (olmPhase == 4) {
                olmPhase = 1;
            }
            olmActive = false;
        }

    }

    @Subscribe
    public void onNpcChanged(final NpcChanged event) {
        final NPC npc = event.getNpc();

        if (npc.getId() == OLM_HEAD_NPC_ID && config.enableOlm()) {
            if (npcInfos.containsKey(npc)) {
                return;
            }

            olmActive = true;

            if (olmPhase == 1) {
                npcInfos.put(npc, new NpcInfo(npc, 5, config.npcColor()));
            } else {
                npcInfos.put(npc, new NpcInfo(npc, 4, config.npcColor()));
            }
            ++olmPhase;
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
