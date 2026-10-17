package com.newuniverse.nusmp.arc.world;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Why: on every tenth floor the Guardian's arena may host a character of the arcs instead of the plain Labyrinth Guardian, and WHICH one must be a decision
 * nobody can reroll: a party that leaves and comes back, a server that restarts, two parties of one world must all find the same boss on the same floor, or
 * the dungeon stops being a place and becomes a slot machine. So the choice is a pure function of the dungeon's seed, the sector, the floor and the party's
 * story: a splitmix64 hash for the coin ("does this floor host an arc boss at all", the {@code arc.floorBossChance}) and another for the pick, and nothing
 * else (no clock, no {@code Random}, no list order: the candidates are sorted by id first, so a roster that is merged in another order picks the same boss).
 * The rules, in order: no candidates, the layer or the switch off, or a coin that says no, mean the plain Guardian (the safe default that always works);
 * a boss whose themes name other themes than this floor's is never offered here, while a boss with no theme list is welcome anywhere and a boss that names
 * this theme is three times as likely as one that does not care (the "theme favour"); a boss the party's story has not opened is skipped (the story gate;
 * with {@code arc.storyGate} off the caller says everything is open). Pure, no Minecraft types, unit tested.
 */
public final class FloorBossChoice {
    private FloorBossChoice() {}

    /** How much likelier a boss that names this theme is than one that does not care. */
    public static final int FAVOURED_WEIGHT = 3, NEUTRAL_WEIGHT = 1;

    /**
     * What the dungeon asks.
     *
     * @param seed     the dungeon's seed (any long; the same world always gives the same one)
     * @param sectorId the sector (a floor is one sector)
     * @param floor    the signed floor (the boss stands on its absolute value)
     * @param road     the road of the sector
     * @param theme    the id of the sector's theme ({@code radiant}), may be empty
     * @param chance   the chance (0 to 1) that a floor that CAN host an arc boss does
     * @param enabled  the layer and the floor-boss switch are on
     * @param standing the characters that may stand on this floor and road ({@code ArcRoster.floorBosses}), in any order
     * @param allowed  does the party's story open this character (always true with the story gate off)
     */
    public record Request(long seed, int sectorId, int floor, Polarity road, String theme, double chance, boolean enabled,
                          List<ArcCharacter> standing, Predicate<ArcCharacter> allowed) {}

    /** The answer: the character, or null for "the plain Guardian". */
    public static ArcCharacter choose(Request r) {
        if (r == null || !r.enabled() || r.standing() == null || r.standing().isEmpty()) return null;
        List<ArcCharacter> pool = new ArrayList<>();
        String theme = r.theme() == null ? "" : r.theme().toLowerCase(Locale.ROOT);
        for (ArcCharacter c : r.standing()) {
            if (c == null || !c.isBoss() || !c.standsOn(r.floor(), r.road())) continue;      // the caller already filtered by floor and road, the check is the cheap second lock
            if (r.allowed() != null && !r.allowed().test(c)) continue;
            if (!themeFits(c, theme)) continue;
            pool.add(c);
        }
        if (pool.isEmpty()) return null;
        if (!hosts(r.seed(), r.sectorId(), r.floor(), r.chance())) return null;
        pool.sort(Comparator.comparing(ArcCharacter::id));
        int total = 0;
        for (ArcCharacter c : pool) total += weight(c, theme);
        double roll = unit(mix(r.seed(), r.sectorId(), r.floor(), 2)) * total;
        for (ArcCharacter c : pool) {
            roll -= weight(c, theme);
            if (roll < 0) return c;
        }
        return pool.get(pool.size() - 1);
    }

    /** Does this floor host an arc boss at all? The coin of the floor: the same for everyone, every time. Chance 0 never, chance 1 always. */
    public static boolean hosts(long seed, int sectorId, int floor, double chance) {
        if (!(chance > 0.0)) return false;
        if (chance >= 1.0) return true;
        return unit(mix(seed, sectorId, floor, 1)) < chance;
    }

    /** May a character with these themes stand on a floor of this theme: no list means anywhere, a list must name the theme. */
    public static boolean themeFits(ArcCharacter c, String theme) {
        if (c.themes().isEmpty()) return true;
        return favours(c, theme);
    }

    /** Does the character name this theme? */
    public static boolean favours(ArcCharacter c, String theme) {
        if (theme == null || theme.isEmpty()) return false;
        for (String t : c.themes()) if (t.equalsIgnoreCase(theme)) return true;
        return false;
    }

    /** The draw weight: three for a character that names the floor's theme, one for one that does not care. */
    public static int weight(ArcCharacter c, String theme) { return favours(c, theme) ? FAVOURED_WEIGHT : NEUTRAL_WEIGHT; }

    // ---------------------------------------------------------------- the hash
    /** splitmix64 over the four numbers: a pure mix, so two floors that differ in any number are unrelated. */
    static long mix(long seed, int sectorId, int floor, int salt) {
        long z = seed * 0x9E3779B97F4A7C15L + sectorId * 0xBF58476D1CE4E5B9L + floor * 0x94D049BB133111EBL + salt * 0xD6E8FEB86659FD93L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** The top 53 bits of a hash as a number in [0, 1). */
    static double unit(long hash) { return (hash >>> 11) * 0x1.0p-53; }
}
