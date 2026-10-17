package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Why: "a permanent stat boost OR a Shadow Core" is a dice roll the owner wants weighted (70 / 30 by default) and the server must be able to REPRODUCE, so it is
 * a pure function of a seed and the facts: the same quest completed by the same player always pays the same, which also means a save-and-reload cannot be used
 * to reroll a bad result. The rules around the roll keep it from ever paying nothing by accident: the stat is drawn only from the stats the quest pays that are
 * still below the per-stat cap; if none is left, the core is paid instead (when the template allows it); a template that forbids the core always pays a stat;
 * zero weights are honoured (0 core weight = never a core by chance). Pure, so the distribution and every edge are pinned by JUnit.
 */
public final class RewardRoller {
    private RewardRoller() {}

    public enum Kind { STAT, CORE, NONE }

    /** What a success pays: a number of points of one stat, a Shadow Core, or nothing (every stat capped and no core allowed). */
    public record Reward(Kind kind, Stat stat, int points) {
        public static Reward core() { return new Reward(Kind.CORE, null, 0); }

        public static Reward none() { return new Reward(Kind.NONE, null, 0); }
    }

    /** The seed of a reward: the player, the floor, the quest id and how many quests the player has finished, scrambled. */
    public static long seed(UUID player, int floor, String questId, int finished) {
        long h = player.getMostSignificantBits() ^ Long.rotateLeft(player.getLeastSignificantBits(), 29);
        h ^= (questId == null ? 0 : questId.hashCode()) * 0x9E3779B97F4A7C15L;
        h ^= floor * 0xC2B2AE3D27D4EB4FL;
        h ^= finished * 0x165667B19E3779F9L;
        return QuestRoller.mix(h);
    }

    /**
     * Rolls a reward.
     *
     * @param statWeight  weight of "a stat point" (0 or less = never by chance)
     * @param coreWeight  weight of "a Shadow Core" (0 or less = never by chance)
     * @param candidates  the stats the quest pays
     * @param points      points to pay (cut down to what is left under the cap)
     * @param coreAllowed whether the quest's template allows a core at all
     * @param have        the player's points per stat, indexed by {@link Stat#ordinal()}
     * @param cap         the most points one stat can hold
     */
    public static Reward roll(long seed, int statWeight, int coreWeight, List<Stat> candidates, int points, boolean coreAllowed, int[] have, int cap) {
        List<Stat> open = new ArrayList<>();
        for (Stat s : candidates) if (s.ordinal() < have.length && have[s.ordinal()] < cap && !open.contains(s)) open.add(s);
        if (open.isEmpty()) return coreAllowed ? Reward.core() : Reward.none();
        Random rnd = new Random(seed);
        int sw = Math.max(0, statWeight), cw = coreAllowed ? Math.max(0, coreWeight) : 0;
        int roll = sw + cw == 0 ? 0 : rnd.nextInt(sw + cw);
        Stat picked = open.get(rnd.nextInt(open.size()));
        if (sw + cw > 0 && roll >= sw) return Reward.core();
        int give = Math.max(1, Math.min(points, cap - have[picked.ordinal()]));
        return new Reward(Kind.STAT, picked, give);
    }
}
