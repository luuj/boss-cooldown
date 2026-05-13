package net.runelite.client.plugins.tickcd;

import com.google.common.base.Splitter;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
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
public class TickcdPlugin extends Plugin {
    private static final Splitter SPLITTER = Splitter.on("\n").omitEmptyStrings().trimResults();
    private static final String CONFIG_GROUP = "tickcd";
    private static final String ALL_NPC_CONFIG_KEY = "allNPC";
    private static final int OLM_HEAD_NPC_ID = 7554;
    private static final String JAL_AK_NAME = "Jal-Ak";
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

    final List<NpcInfo> npcList = new ArrayList<>();
    private final Map<String, List<AnimationRule>> animationRules = new HashMap<>();

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
        npcList.clear();
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
        if (event.getGameState() != GameState.LOGGED_IN) {
            reset();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        for (int i = npcList.size() - 1; i >= 0; --i) {
            NpcInfo curr = npcList.get(i);
            --curr.ticks;

            if (JAL_AK_NAME.equalsIgnoreCase(curr.currNPC.getName()) && curr.ticks == 3) {
                Player localPlayer = client.getLocalPlayer();
                if (localPlayer != null && localPlayer.getOverheadIcon() == HeadIcon.MAGIC) {
                    curr.color = Color.GREEN;
                } else if (localPlayer != null && localPlayer.getOverheadIcon() == HeadIcon.RANGED) {
                    curr.color = Color.CYAN;
                }
            }

            if (curr.ticks <= 0 || curr.currNPC.isDead()) {
                if (config.enableOlm() && olmActive && curr.currNPC.getId() == OLM_HEAD_NPC_ID && !curr.currNPC.isDead()) {
                    curr.ticks += 4;
                    continue;
                }

                npcList.remove(i);
            }
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        Actor actor = event.getActor();
        if (!(actor instanceof NPC) || actor.getName() == null) {
            return;
        }

        NPC npc = (NPC) actor;
        List<AnimationRule> rules = animationRules.get(actor.getName().toLowerCase(Locale.ROOT));
        if (rules == null) {
            return;
        }

        for (AnimationRule rule : rules) {
            if (actor.getAnimation() != rule.animation) {
                continue;
            }

            Color selectedColor = getConfiguredColor(rule.colorNumber);
            Optional<NpcInfo> existingNpc = containsNPC(npcList, npc);
            if (existingNpc.isPresent()) {
                existingNpc.get().ticks = rule.ticks;
                existingNpc.get().color = selectedColor;
            } else {
                npcList.add(new NpcInfo(npc, rule.ticks, selectedColor));
            }
            return;
        }
    }

    private void rebuildAnimationRules() {
        animationRules.clear();

        for (String str : SPLITTER.splitToList(config.allNPC())) {
            String[] stringList = str.split(",");
            if (stringList.length <= ENTRY_SIZE) {
                continue;
            }

            String npcName = stringList[NPC_NAME_INDEX].trim();
            if (npcName.isEmpty()) {
                continue;
            }

            List<AnimationRule> rules = animationRules.computeIfAbsent(npcName.toLowerCase(Locale.ROOT), k -> new ArrayList<>());
            int numEntries = (stringList.length - 1) / ENTRY_SIZE;
            for (int i = 0; i < numEntries; i++) {
                int entryIndex = (i * ENTRY_SIZE) + 1;
                try {
                    int animation = Integer.parseInt(stringList[entryIndex].trim());
                    int ticks = Integer.parseInt(stringList[entryIndex + 1].trim()) + 1;
                    int colorNumber = Integer.parseInt(stringList[entryIndex + 2].trim());
                    rules.add(new AnimationRule(animation, ticks, colorNumber));
                } catch (NumberFormatException ex) {
                    // Skip malformed entries without disabling the rest of the config.
                }
            }

            if (rules.isEmpty()) {
                animationRules.remove(npcName.toLowerCase(Locale.ROOT));
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

        if (config.enableOlm() && npc.getId() == OLM_HEAD_NPC_ID) {
            if (olmPhase == 4) {
                olmPhase = 1;
            }
            olmActive = false;
        }

    }

    @Subscribe
    public void onNpcChanged(final NpcChanged event) {
        final NPC npc = event.getNpc();

        if (config.enableOlm() && npc.getId() == OLM_HEAD_NPC_ID) {
            olmActive = true;

            if (olmPhase == 1) {
                npcList.add(new NpcInfo(npc, 5, config.npcColor()));
            } else {
                npcList.add(new NpcInfo(npc, 4, config.npcColor()));
            }
            ++olmPhase;
        }
    }

    private Optional<NpcInfo> containsNPC(final List<NpcInfo> list, final NPC npc) {
        return list.stream().filter(o -> o.currNPC.equals(npc)).findFirst();
    }

    private static class AnimationRule {
        private final int animation;
        private final int ticks;
        private final int colorNumber;

        private AnimationRule(int animation, int ticks, int colorNumber) {
            this.animation = animation;
            this.ticks = ticks;
            this.colorNumber = colorNumber;
        }
    }
}
