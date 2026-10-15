package com.newuniverse.nusmp.labyrinth.seal;

import java.util.List;
import java.util.function.Predicate;

/**
 * Why: the Peak Altar's boss is DATA in the structure template ({@code bosses:[{id, weight}]}, written by the template builder), not a
 * constant in Java, so a datapack can change which floor masters a Spire calls without a mod update, and other mods' bosses can be put on
 * the roster by id. The pick itself is pure: a weighted draw from a seed the altar makes of its position and the game time (two altars that
 * wake in the same tick draw differently; one altar re-rolling after a reset draws afresh), finalised with a splitmix64 mix so neighbouring
 * seeds are not neighbouring draws. Entries the predicate rejects (an entity id no loaded mod registers) and entries with weight {@code <= 0}
 * are skipped, and the empty case returns {@code null} so the altar falls back to the Floor Warden, which this mod always has. Unit-tested
 * ({@code BossRosterTest}); no Minecraft types.
 */
public final class BossRoster {
    private BossRoster() {}

    /** The boss every altar can always call: ours, registered by {@link SealRegistry}. */
    public static final String FALLBACK = "nusmp:floor_warden";

    /** One roster line: an entity id and its weight (0 or less = never). */
    public record Entry(String id, int weight) {}

    /** The seed of one draw: the altar's packed position and the tick it woke in, mixed so either alone changes the result. */
    public static long seed(long posKey, long gameTime) {
        return mix(posKey * 0x9E3779B97F4A7C15L + mix(gameTime ^ 0x632BE59BD9B4E019L));
    }

    /** splitmix64's finaliser: a bijection with good avalanche, enough for a one-shot draw. */
    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /**
     * Draws one id from the roster in proportion to the weights, skipping entries {@code known} rejects and weights {@code <= 0}.
     * {@code null} when nothing is drawable (empty roster, every id unknown, every weight zero).
     */
    public static String pick(List<Entry> roster, long seed, Predicate<String> known) {
        long total = 0;
        for (Entry e : roster) if (e != null && e.id() != null && e.weight() > 0 && known.test(e.id())) total += e.weight();
        if (total <= 0) return null;
        long r = Math.floorMod(mix(seed), total);
        for (Entry e : roster) {
            if (e == null || e.id() == null || e.weight() <= 0 || !known.test(e.id())) continue;
            if (r < e.weight()) return e.id();
            r -= e.weight();
        }
        return null;                                                               // unreachable: the weights sum to total
    }

    /** {@link #pick}, or {@link #FALLBACK} when the roster gives nothing. */
    public static String pickOrFallback(List<Entry> roster, long seed, Predicate<String> known) {
        String id = pick(roster, seed, known);
        return id != null ? id : FALLBACK;
    }
}
