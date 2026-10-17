package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * Why: in a party the dungeon is walked by several people at once and the owner wants a floor quest to be a shared effort: when one member slays a monster, it
 * counts for the "defeat N monsters" quest of every member who is on that floor and close enough to have been part of the fight, not only for the one who struck
 * the last blow. Who counts as a member is decided by {@link SystemParty} (today the players of the same sector, tomorrow the FTB Team); WHICH of them are
 * credited is arithmetic over plain values, so it is here, generic over the player type and free of Minecraft types, and a unit test pins it. A member who is
 * dead, in another dimension (the caller's {@code eligible} test) or farther than {@code radius} blocks is not credited (nobody can leech a floor from the
 * other side of it); a radius of 0 or less means the whole floor. The killer is always credited first. Pure.
 */
public final class PartyRules {
    private PartyRules() {}

    /**
     * The members of one group: {@code self} first, then every candidate that is not {@code self} and whose group key equals self's. A null key means "no
     * group" (a player in a gap between sectors), and then only {@code self} is returned. Each candidate appears once.
     */
    public static <T> List<T> sameGroup(T self, Collection<? extends T> candidates, Function<T, ?> groupOf) {
        List<T> out = new ArrayList<>();
        out.add(self);
        Object key = groupOf.apply(self);
        if (key == null) return out;
        for (T c : candidates) {
            if (c == self || out.contains(c)) continue;
            if (key.equals(groupOf.apply(c))) out.add(c);
        }
        return out;
    }

    /**
     * Who is credited for a shared achievement of {@code self}: self (always) and every member that passes {@code eligible} and is within {@code radius} blocks
     * ({@code distSq} gives a member's squared distance to the place of the shared achievement, for a kill the fallen creature). {@code radius <= 0} means no distance limit. The order of {@code members} is kept.
     */
    public static <T> List<T> credit(T self, Collection<? extends T> members, Predicate<T> eligible, ToDoubleFunction<T> distSq, int radius) {
        List<T> out = new ArrayList<>();
        out.add(self);
        double limit = radius <= 0 ? Double.POSITIVE_INFINITY : (double) radius * radius;
        for (T m : members) {
            if (m == self || out.contains(m)) continue;
            if (eligible.test(m) && distSq.applyAsDouble(m) <= limit) out.add(m);
        }
        return out;
    }
}
