package net.runelite.client.plugins.tickcd;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.HeadIcon;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.AnimationID;

/**
 * Inferno blob (Jal-Ak) handling for the tick counter: line-of-sight cycle starts and
 * prayer detection colouring. Flinch resets are handled generically in TickcdPlugin.
 */
class JalAkTracker {
    static final String NAME = "Jal-Ak";

    private static final int ATTACK_MAGIC = AnimationID.JALAK_ATTACK_MAGIC;
    private static final int ATTACK_RANGED = AnimationID.JALAK_ATTACK_RANGED;
    // Max distance (tiles) at which a Jal-Ak attacks / reads your prayer.
    private static final int RANGE = 15;
    // Ticks until attack when a blob gains LOS without animating (it reads your prayer first).
    private static final int LOS_PREV_TILE_TICKS = 3;
    private static final int LOS_NOW_TICKS = 4;
    private static final int DETECTION_TICK = 3;
    // Fallback attack cycle (6 ticks + the 1 tick the GameTick decrement eats) if no Jal-Ak rule is configured.
    private static final int DEFAULT_ATTACK_TICKS = 7;
    private static final Color UNKNOWN_COLOR = Color.WHITE;

    private final Client client;
    private final Map<NPC, NpcInfo> npcInfos;

    // Whether each Jal-Ak could attack the player at the end of the previous tick.
    private final Map<NPC, Boolean> couldAttack = new HashMap<>();
    // Jal-Aks that spawned since the last tick; on their first tick they can't use the 3-tick start.
    private final Set<NPC> justSpawned = new HashSet<>();
    private WorldPoint lastPlayerLocation;
    // Attack cycle from the configured Jal-Ak magic/ranged rule, used when an LOS-started counter is
    // reset by a block. Melee is excluded because its cycle is configured differently.
    private int attackTicks = DEFAULT_ATTACK_TICKS;
    private boolean attackRuleSet;

    // Per-tick state, set in startTick().
    private Player localPlayer;
    private WorldPoint playerLocation;
    private boolean losAvailable;

    JalAkTracker(Client client, Map<NPC, NpcInfo> npcInfos) {
        this.client = client;
        this.npcInfos = npcInfos;
    }

    static boolean isJalAk(NPC npc) {
        return NAME.equalsIgnoreCase(npc.getName());
    }

    void reset() {
        couldAttack.clear();
        justSpawned.clear();
        lastPlayerLocation = null;
    }

    void resetAttackRule() {
        attackTicks = DEFAULT_ATTACK_TICKS;
        attackRuleSet = false;
    }

    /**
     * Called while parsing the animation list for each Jal-Ak entry. Only the first magic or
     * ranged entry is used, so a melee entry (or entry order) can't change the LOS block reset.
     */
    void addAttackRule(int animation, int ticks) {
        if (attackRuleSet || (animation != ATTACK_MAGIC && animation != ATTACK_RANGED)) {
            return;
        }
        attackTicks = ticks;
        attackRuleSet = true;
    }

    /**
     * Called each game tick after counters are decremented, so a counter started here shows
     * its full value this tick and a 3-tick start gets coloured in the same tick.
     */
    void startTick() {
        localPlayer = client.getLocalPlayer();
        playerLocation = localPlayer != null ? localPlayer.getWorldLocation() : null;
        WorldView worldView = client.getTopLevelWorldView();
        losAvailable = playerLocation != null && worldView != null;

        if (losAvailable) {
            updateLineOfSight(worldView);
        }
    }

    /** Called for each live counter; colours a Jal-Ak's counter on its prayer detection tick. */
    void onCounterTick(NpcInfo info) {
        if (info.ticks != DETECTION_TICK || !isJalAk(info.currNPC)) {
            return;
        }
        if (losAvailable && !inRange(info.currNPC, playerLocation)) {
            return;
        }

        HeadIcon overhead = localPlayer != null ? localPlayer.getOverheadIcon() : null;
        if (overhead == HeadIcon.MAGIC) {
            info.color = Color.GREEN;
        } else if (overhead == HeadIcon.RANGED) {
            info.color = Color.CYAN;
        } else if (overhead == HeadIcon.MELEE) {
            // Praying melee gives the blob no style to dodge, so its attack style is unknown.
            info.color = UNKNOWN_COLOR;
        }
    }

    void endTick() {
        lastPlayerLocation = playerLocation;
    }

    void onNpcSpawned(NPC npc) {
        if (isJalAk(npc)) {
            justSpawned.add(npc);
            couldAttack.remove(npc);
        }
    }

    void onNpcDespawned(NPC npc) {
        couldAttack.remove(npc);
        justSpawned.remove(npc);
    }

    /**
     * Blobs don't always animate before an attack cycle. Ported from the OpenOSRS Inferno plugin:
     * if an idle blob could see the tile you were on last tick, its cycle has already started (3 ticks);
     * if it can see you now but couldn't last tick (you stepped out of a safespot), it's reading your
     * prayer and attacks in 4.
     *
     * Two additions over the original: an LOS-started counter is cancelled if you get out of range or
     * LOS before the blob checks your prayer, and an idle blob that can reach you always gets a counter,
     * so a counter that ran out without an attack can't leave the next real attack without a countdown.
     */
    private void updateLineOfSight(WorldView worldView) {
        for (NPC npc : worldView.npcs()) {
            if (npc == null || npc.isDead() || !isJalAk(npc)) {
                continue;
            }

            // A blob that just spawned didn't exist last tick, so it can't have seen your previous tile.
            boolean activeLastTick = !justSpawned.remove(npc);

            boolean canAttackNow = canAttack(worldView, npc, playerLocation);
            boolean couldAttackLastTick = couldAttack.getOrDefault(npc, false);
            couldAttack.put(npc, canAttackNow);

            NpcInfo info = npcInfos.get(npc);

            // You got out of range or LOS before the blob checked your prayer: it won't attack, so drop
            // the counter. After the check it attacks regardless, so the counter is kept.
            if (info != null && info.awaitingAttack && info.ticks > DETECTION_TICK && !canAttackNow) {
                npcInfos.remove(npc);
                info = null;
            }

            boolean idle = info == null || info.ticks <= 0;
            if (!idle) {
                continue;
            }

            int startTicks;
            if (!couldAttackLastTick && activeLastTick && lastPlayerLocation != null
                    && canAttack(worldView, npc, lastPlayerLocation)) {
                startTicks = LOS_PREV_TILE_TICKS;
            } else if (canAttackNow) {
                startTicks = LOS_NOW_TICKS;
            } else {
                continue;
            }

            NpcInfo started = new NpcInfo(npc, startTicks, UNKNOWN_COLOR);
            // If the blob blocks right before attacking, fall back to its normal magic/ranged cycle.
            // The style is still unknown at that point, so keep the counter white.
            started.attackTicks = attackTicks;
            started.attackColor = UNKNOWN_COLOR;
            started.awaitingAttack = true;
            npcInfos.put(npc, started);
        }
    }

    private static boolean inRange(NPC npc, WorldPoint target) {
        return NpcLineOfSight.inRange(npc, target, RANGE);
    }

    private static boolean canAttack(WorldView worldView, NPC npc, WorldPoint target) {
        return NpcLineOfSight.canAttack(worldView, npc, target, RANGE);
    }
}
