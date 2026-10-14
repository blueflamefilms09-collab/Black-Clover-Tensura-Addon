package com.newuniverse.nusmp.boss;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The safe Usurper (pure, unit tested). Tensura's Usurper steals an enemy's skills; the boss overhaul forbids that: the boss may only
 * <b>lend itself a copy</b> of one skill the target has, for {@link LuciusNumbers#USURPER_LEND_TICKS} ticks, and drain a small share of
 * the target's magicules (they regenerate). The plan this class returns has no field that names anything to remove, the glue never
 * calls a forget on a player, and {@link #playerAfter} is the identity: the target's skills are the same before and after.
 */
public final class UsurperPolicy {
    private UsurperPolicy() {}

    /** How a skill ranks; the glue maps Tensura's skill types onto it. */
    public enum Kind { RESISTANCE, INTRINSIC, COMMON, EXTRA, UNIQUE, ULTIMATE, OTHER }

    /** One skill of the target. */
    public record Seen(String id, Kind kind) {}

    /** What the boss does: lend itself a copy of 'copyId' for 'ticks', and drain 'drainFraction' of the target's max magicules. Nothing is taken from the target. */
    public record Plan(String copyId, int ticks, double drainFraction) {}

    /** Words that keep a skill from being copied: the immunities and the Usurper itself (a copy would stack on the boss's own set). */
    private static final String[] DENY = {"nullification", "usurper", "anti_skill", "tuner", "immortal", "time_stop", "invulnerab"};

    /** Can this skill be copied? Resistances, common, extra and unique skills only, and never a denied one. */
    public static boolean copyable(Seen s) {
        if (s == null || s.id() == null || s.id().isBlank()) return false;
        if (s.kind() == Kind.ULTIMATE || s.kind() == Kind.INTRINSIC || s.kind() == Kind.OTHER) return false;
        String id = s.id().toLowerCase(Locale.ROOT);
        for (String d : DENY) if (id.contains(d)) return false;
        return true;
    }

    /**
     * Chooses the copy. 'bossHas' are the skills the boss owns (never copied over its own), 'leaseActive' says a copy is already
     * lent, 'now'/'nextAllowed' keep the cooldown, 'roll' (any int) picks among the candidates. Empty when nothing should happen.
     */
    public static Optional<Plan> choose(List<Seen> targetSkills, Set<String> bossHas, boolean leaseActive, long now, long nextAllowed, int roll) {
        if (leaseActive || now < nextAllowed || targetSkills == null) return Optional.empty();
        List<String> candidates = new ArrayList<>();
        for (Seen s : targetSkills) if (copyable(s) && !bossHas.contains(s.id()) && !candidates.contains(s.id())) candidates.add(s.id());
        if (candidates.isEmpty()) return Optional.empty();
        candidates.sort(null);
        String pick = candidates.get(Math.floorMod(roll, candidates.size()));
        return Optional.of(new Plan(pick, LuciusNumbers.USURPER_LEND_TICKS, LuciusNumbers.USURPER_DRAIN));
    }

    /** The target's skills after a Usurper plan: the same set. This is the rule, written as code. */
    public static Set<String> playerAfter(Set<String> before, Optional<Plan> plan) { return before; }

    /** When the next copy may happen after one at 'now'. */
    public static long nextAllowed(long now) { return now + LuciusNumbers.USURPER_COOLDOWN_TICKS; }
}
