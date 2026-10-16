package net.runelite.client.plugins.tickcd;

import net.runelite.api.NPC;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

/** Range and line-of-sight checks from an NPC to a target tile. */
final class NpcLineOfSight {
    private NpcLineOfSight() {
    }

    /**
     * Single line from the target to the NPC tile closest to it. RuneLite's multi-tile
     * hasLineOfSightTo (since Aug 2024) passes if ANY edge tile of the NPC can see the target,
     * which is more lenient than large NPCs are in game and reports LOS around pillar corners.
     * This matches the pre-2024 check the OpenOSRS Inferno plugin was built against.
     */
    static boolean hasLos(WorldView worldView, NPC npc, WorldPoint target) {
        WorldArea npcArea = npc.getWorldArea();
        if (target == null || npcArea == null || npcArea.getPlane() != target.getPlane()) {
            return false;
        }
        if (npcArea.contains(target)) {
            return true;
        }

        int closestX = clamp(target.getX(), npcArea.getX(), npcArea.getX() + npcArea.getWidth() - 1);
        int closestY = clamp(target.getY(), npcArea.getY(), npcArea.getY() + npcArea.getHeight() - 1);
        WorldPoint closest = new WorldPoint(closestX, closestY, npcArea.getPlane());
        return target.toWorldArea().hasLineOfSightTo(worldView, closest.toWorldArea());
    }

    static boolean inRange(NPC npc, WorldPoint target, int range) {
        if (target == null) {
            return false;
        }
        WorldArea npcArea = npc.getWorldArea();
        return npcArea != null && npcArea.distanceTo(target) <= range;
    }

    static boolean canAttack(WorldView worldView, NPC npc, WorldPoint target, int range) {
        return inRange(npc, target, range) && hasLos(worldView, npc, target);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
