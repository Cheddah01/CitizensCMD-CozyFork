package me.mattstudios.citizenscmd.utility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Decides which NPCs {@code /npcmd copyworld} copies. Copies remember the NPC they came from, so running
 * the command again only copies NPCs that don't have a copy in the target world yet, and copies are
 * never copied again (no ping-pong between worlds).
 */
public final class WorldCopyPlan {

    /** One NPC as the plan sees it. {@code copiedFrom} is null for an NPC this command didn't make. */
    public record Npc(int id, String world, Integer copiedFrom) {}

    public record Result(List<Integer> toCopy, int alreadyCopied) {}

    private WorldCopyPlan() {}

    public static Result plan(final List<Npc> npcs, final String from, final String to) {
        final Set<Integer> copiedInTarget = new HashSet<>();
        for (final Npc npc : npcs) {
            if (npc.copiedFrom() != null && to.equals(npc.world())) {
                copiedInTarget.add(npc.copiedFrom());
            }
        }
        final List<Integer> toCopy = new ArrayList<>();
        int already = 0;
        for (final Npc npc : npcs) {
            if (!from.equals(npc.world()) || npc.copiedFrom() != null) {
                continue;
            }
            if (copiedInTarget.contains(npc.id())) {
                already++;
            } else {
                toCopy.add(npc.id());
            }
        }
        return new Result(List.copyOf(toCopy), already);
    }

    /** Reads the stored "copied from" id, which may come back as a number or a string after a reload. */
    public static Integer parseId(final Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
