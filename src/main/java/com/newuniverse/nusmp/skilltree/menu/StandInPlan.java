package com.newuniverse.nusmp.skilltree.menu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Why: the stand-in skill of a line sits in the player's Tensura skill list exactly while the player OWNS the line, and "exactly while" has to survive everything that can change either side: a
 * Skill Book read, a line taken away by a command, a respec, a death, a login on a world saved by an older version, a stand-in copied from the player by another Tensura skill, a datapack that
 * dropped a route. Doing that with "on this event add, on that event remove" drifts; doing it as ONE comparison of two sets, run whenever anything might have changed, cannot. This class is that
 * comparison, pure and unit tested: given the lines whose stand-in the player holds, the lines the player owns and the lines that have a stand-in at all, it says which to teach and which to
 * forget. A stand-in that is held without being owned is always forgotten (the fog of war holds even for a skill that arrived by some other route), an owned id that has no stand-in (a classic
 * route such as Fire Dragon Slayer, or a route added by a datapack after the registry froze) is ignored, never an error. Results are sorted so the order of the teaching is the same every time.
 */
public final class StandInPlan {
    private StandInPlan() {}

    /** What to do: route ids whose stand-in must be taught, and route ids whose stand-in must be forgotten. */
    public record Plan(List<String> learn, List<String> forget) {
        public boolean isEmpty() { return learn.isEmpty() && forget.isEmpty(); }
    }

    public static Plan diff(Collection<String> heldRouteIds, Collection<String> ownedRouteIds, Set<String> standInRouteIds) {
        Set<String> held = new TreeSet<>(heldRouteIds);
        Set<String> owned = new TreeSet<>();
        for (String id : ownedRouteIds) if (standInRouteIds.contains(id)) owned.add(id);
        List<String> learn = new ArrayList<>();
        for (String id : owned) if (!held.contains(id)) learn.add(id);
        List<String> forget = new ArrayList<>();
        for (String id : held) if (!owned.contains(id)) forget.add(id);
        return new Plan(List.copyOf(learn), List.copyOf(forget));
    }

    /** An order-independent signature of a set of ids (equal sets give equal signatures), so a player is compared only when the owned set really changed. */
    public static int signature(Collection<String> ids) {
        int sum = 0, xor = 0;
        for (String id : ids) {
            int h = id.hashCode() * 0x9E3779B1;
            sum += h;
            xor ^= Integer.rotateLeft(h, 13);
        }
        return sum * 31 + xor + ids.size() * 0x85EBCA6B;
    }
}
