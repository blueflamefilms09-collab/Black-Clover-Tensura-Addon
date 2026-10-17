package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Why: which quest a floor gives, and how big it is, must be a pure function of facts the server already has, for four reasons. (1) FAIRNESS: the same player
 * on the same floor always gets the same quest, so re-entering a floor, relogging or dying cannot be used to re-roll for an easy one (the seed is the player's
 * id mixed with the floor). (2) VARIETY: a player never gets the same KIND twice in a row, so a run does not turn into "kill six things" forty times. (3)
 * SCALING: the target grows with the floor's threat, {@code clamp(round(base + perFloor * threat), min, max)}, so +40 asks more than +4. (4) HONESTY: a quest the
 * floor cannot satisfy is never offered: "clear 8 rooms" on a floor with five, or an ambush quest when the ambush switch is off. No Minecraft types, so every one
 * of those rules is pinned by JUnit over thousands of seeds.
 */
public final class QuestRoller {
    private QuestRoller() {}

    private static final long GOLDEN = 0x9E3779B97F4A7C15L;

    /**
     * What the roller needs to know about the floor: how many rooms a player could clear on it (rooms that are neither safe nor corridors), and whether the
     * ambush quests are switched on.
     */
    public record FloorFacts(int countableRooms, boolean ambushEnabled) {}

    /** The seed of a player's quest on a floor: the player's id mixed with the floor, scrambled so neighbouring floors are unrelated. */
    public static long seed(UUID player, int floor) {
        long h = player.getMostSignificantBits() ^ Long.rotateLeft(player.getLeastSignificantBits(), 17);
        return mix(h ^ (floor * GOLDEN));
    }

    /** SplitMix64's finaliser: a cheap bijective scramble of a 64-bit value. */
    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** The target of a template on a floor of this threat: {@code clamp(round(base + perFloor * threat), min, max)}. */
    public static int target(QuestTemplate t, int threat) {
        double raw = t.base() + t.perFloor() * Math.max(1, threat);
        long r = Math.round(raw);
        return (int) Math.max(t.min(), Math.min(t.max(), r));
    }

    /** Does this floor have a quest at all? Deterministic for a seed: {@code chance} 1 always, 0 never. */
    public static boolean hasQuest(long seed, double chance) {
        if (!(chance > 0.0)) return false;
        if (chance >= 1.0) return true;
        return new Random(mix(seed ^ 0x51ED270B0B0CL)).nextDouble() < chance;
    }

    /** The templates this floor can be asked for: weighted above zero, and satisfiable (see the class note). */
    public static List<QuestTemplate> eligible(QuestCatalog catalog, int threat, FloorFacts facts) {
        List<QuestTemplate> out = new ArrayList<>();
        for (QuestTemplate t : catalog.all()) {
            if (t.weight() <= 0) continue;
            if (t.kind() == QuestKind.CLEAR_ROOMS && target(t, threat) > facts.countableRooms()) continue;
            if (t.kind() == QuestKind.SURVIVE_AMBUSH && !facts.ambushEnabled()) continue;
            out.add(t);
        }
        return out;
    }

    /**
     * Picks the quest for a floor: a weighted draw from the eligible templates, skipping the previous kind unless nothing else is left. Null when no template
     * is eligible (an empty catalog, or every kind ruled out).
     */
    public static QuestTemplate pick(QuestCatalog catalog, long seed, int threat, QuestKind previous, FloorFacts facts) {
        List<QuestTemplate> pool = eligible(catalog, threat, facts);
        if (pool.isEmpty()) return null;
        if (previous != null) {
            List<QuestTemplate> other = new ArrayList<>();
            for (QuestTemplate t : pool) if (t.kind() != previous) other.add(t);
            if (!other.isEmpty()) pool = other;
        }
        int total = 0;
        for (QuestTemplate t : pool) total += t.weight();
        int roll = new Random(seed).nextInt(total);
        for (QuestTemplate t : pool) {
            roll -= t.weight();
            if (roll < 0) return t;
        }
        return pool.get(pool.size() - 1);
    }
}
