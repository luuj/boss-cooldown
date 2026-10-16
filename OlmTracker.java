package net.runelite.client.plugins.tickcd;

import java.util.Map;
import net.runelite.api.NPC;
import net.runelite.api.gameval.NpcID;

/**
 * Great Olm head counter. Olm can't be tracked by animation, so its counter starts when the
 * head NPC changes (each phase) and repeats every 4 ticks until the head despawns.
 */
class OlmTracker {
    private static final int HEAD_NPC_ID = NpcID.OLM_HEAD;
    private static final int FIRST_PHASE_TICKS = 5;
    private static final int CYCLE_TICKS = 4;

    private final TickcdConfig config;
    private final Map<NPC, NpcInfo> npcInfos;

    private boolean active;
    private short phase;

    OlmTracker(TickcdConfig config, Map<NPC, NpcInfo> npcInfos) {
        this.config = config;
        this.npcInfos = npcInfos;
    }

    void reset() {
        phase = 1;
        active = false;
    }

    /**
     * Called when a counter reaches 0. @return true if it's the Olm head and the counter
     * was restarted for its next cycle (so it shouldn't be removed).
     */
    boolean restartIfOlm(NpcInfo info) {
        if (active && info.currNPC.getId() == HEAD_NPC_ID && !info.currNPC.isDead() && config.enableOlm()) {
            info.ticks += CYCLE_TICKS;
            return true;
        }
        return false;
    }

    void onNpcChanged(NPC npc) {
        if (npc.getId() != HEAD_NPC_ID || !config.enableOlm() || npcInfos.containsKey(npc)) {
            return;
        }

        active = true;
        int ticks = phase == 1 ? FIRST_PHASE_TICKS : CYCLE_TICKS;
        npcInfos.put(npc, new NpcInfo(npc, ticks, config.npcColor()));
        ++phase;
    }

    void onNpcDespawned(NPC npc) {
        if (npc.getId() != HEAD_NPC_ID || !config.enableOlm()) {
            return;
        }
        if (phase == 4) {
            phase = 1;
        }
        active = false;
    }
}
