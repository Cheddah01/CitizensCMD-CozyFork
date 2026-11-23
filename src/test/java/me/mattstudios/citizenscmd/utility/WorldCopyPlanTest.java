package me.mattstudios.citizenscmd.utility;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class WorldCopyPlanTest {
    private static WorldCopyPlan.Npc npc(int id, String world, Integer from) { return new WorldCopyPlan.Npc(id, world, from); }

    @Test
    void copiesEveryNpcInTheSourceWorldOnly() {
        var plan = WorldCopyPlan.plan(List.of(npc(1, "spawn", null), npc(2, "spawn", null), npc(3, "survival", null)), "spawn", "xmas");
        assertEquals(List.of(1, 2), plan.toCopy());
        assertEquals(0, plan.alreadyCopied());
    }

    @Test
    void runningAgainSkipsNpcsThatAlreadyHaveACopy() {
        var plan = WorldCopyPlan.plan(List.of(npc(1, "spawn", null), npc(2, "spawn", null), npc(10, "xmas", 1)), "spawn", "xmas");
        assertEquals(List.of(2), plan.toCopy());
        assertEquals(1, plan.alreadyCopied());
    }

    @Test
    void copiesAreNeverCopiedBack() {
        var plan = WorldCopyPlan.plan(List.of(npc(1, "spawn", null), npc(10, "xmas", 1), npc(11, "xmas", null)), "xmas", "spawn");
        assertEquals(List.of(11), plan.toCopy(), "the copy of #1 stays put; a hand-made Christmas NPC is copied");
    }

    @Test
    void storedIdsReadBackAsNumbersOrText() {
        assertEquals(7, WorldCopyPlan.parseId(7));
        assertEquals(7, WorldCopyPlan.parseId("7"));
        assertEquals(7, WorldCopyPlan.parseId(7.0));
        assertNull(WorldCopyPlan.parseId("abc"));
        assertNull(WorldCopyPlan.parseId(null));
    }
}
