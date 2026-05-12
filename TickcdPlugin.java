package net.runelite.client.plugins.tickcd;

import com.google.common.base.Splitter;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
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
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
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
    private static final int OLM_HEAD_NPC_ID = 7554;
    private static final int JAD_ATTACK_SOUND_ID = 163;
    private static final String JAL_AK_NAME = "Jal-Ak";
    private static final String JALTOK_JAD_NAME = "JalTok-Jad";
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
    private final List<NPC> jads = new ArrayList<>();

    private boolean olmActive;
    private short olmPhase;
    private short jadCount;

    @Provides
    TickcdConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(TickcdConfig.class);
    }

    @Override
    protected void startUp() {
        reset();
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() {
        reset();
        overlayManager.remove(overlay);
    }

    private void reset() {
        npcList.clear();
        jads.clear();
        olmPhase = 1;
        olmActive = false;
        jadCount = 0;
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
        for (String str : SPLITTER.splitToList(config.allNPC())) {
            String[] stringList = str.split(",");
            if (stringList.length <= ENTRY_SIZE || !actor.getName().equalsIgnoreCase(stringList[NPC_NAME_INDEX].trim())) {
                continue;
            }

            int numEntries = (stringList.length - 1) / ENTRY_SIZE;
            for (int i = 0; i < numEntries; i++) {
                int entryIndex = (i * ENTRY_SIZE) + 1;
                int animation;
                int ticks;
                int colorNumber;
                try {
                    animation = Integer.parseInt(stringList[entryIndex].trim());
                    ticks = Integer.parseInt(stringList[entryIndex + 1].trim()) + 1;
                    colorNumber = Integer.parseInt(stringList[entryIndex + 2].trim());
                } catch (NumberFormatException ex) {
                    continue;
                }

                if (actor.getAnimation() != animation) {
                    continue;
                }

                Color selectedColor = getConfiguredColor(colorNumber);
                Optional<NpcInfo> existingNpc = containsNPC(npcList, npc);
                if (existingNpc.isPresent()) {
                    existingNpc.get().ticks = ticks;
                    existingNpc.get().color = selectedColor;
                } else {
                    npcList.add(new NpcInfo(npc, ticks, selectedColor));
                }
                return;
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

        if (config.enableJad() && JALTOK_JAD_NAME.equals(npc.getName())) {
            jads.remove(npc);
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

    @Subscribe
    public void onNpcSpawned(final NpcSpawned event) {
        final NPC npc = event.getNpc();

        if (config.enableJad() && JALTOK_JAD_NAME.equals(npc.getName())) {
            npcList.add(new NpcInfo(npc, 8, config.npcColor()));
            jads.add(npc);
        }
    }

    @Subscribe
    public void onSoundEffectPlayed(SoundEffectPlayed event) {
        if (!config.enableJad() || event.getSoundId() != JAD_ATTACK_SOUND_ID) {
            return;
        }

        NPC curr = getJadFromSound(event);
        if (curr == null) {
            return;
        }

        Optional<NpcInfo> existingNpc = containsNPC(npcList, curr);
        if (existingNpc.isPresent()) {
            existingNpc.get().ticks = 9;
            existingNpc.get().color = config.npcColor();
        } else {
            npcList.add(new NpcInfo(curr, 9, config.npcColor()));
        }
    }

    private NPC getJadFromSound(SoundEffectPlayed event) {
        Actor source = event.getSource();
        if (source instanceof NPC && JALTOK_JAD_NAME.equals(source.getName())) {
            NPC sourceNpc = (NPC) source;
            if (!jads.contains(sourceNpc)) {
                jads.add(sourceNpc);
            }
            return sourceNpc;
        }

        if (jadCount >= jads.size()) {
            jadCount = 0;
        }

        if (jads.isEmpty()) {
            return null;
        }

        NPC fallback = jads.get(jadCount);
        jadCount++;
        return fallback;
    }

    private Optional<NpcInfo> containsNPC(final List<NpcInfo> list, final NPC npc) {
        return list.stream().filter(o -> o.currNPC.equals(npc)).findFirst();
    }
}
