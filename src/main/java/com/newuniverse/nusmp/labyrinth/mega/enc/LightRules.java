package com.newuniverse.nusmp.labyrinth.mega.enc;

/**
 * Why: in the Mega-Dungeon light is a decision, not a setting: the gloom clock already punishes standing in the dark with nothing lit, and the
 * Dungeon Master has to answer the other half of the bargain: a party that walks with torches and lanterns is easier to find. So the party's
 * light turns into two things. A small d20 modifier (-3 for a party that creeps through the dark, +3 for one that blazes: a bright party is
 * simply met more often) and two booleans the encounter table can ask for: {@code dark} (stealthy hunters only come for a party that has no
 * light, and a loud brute is no surprise to it) and {@code lit} (light-eaters, moths and wisps, only come for a party that shines). The input
 * is the brightest member: block light at their eyes, or the light they hold. A held torch counts as {@link #HELD_LIGHT} because vanilla gives
 * a carried torch no light of its own, yet everyone in the room can see it (the same "light in either hand" idea as the gloom clock). Pure (no
 * Minecraft types), so the thresholds are tested without a level.
 */
public final class LightRules {
    private LightRules() {}

    /** The light level a carried light counts as: a torch on a wall gives 14, a lantern 15, the player's own hand is a little less. */
    public static final int HELD_LIGHT = 13;
    /** Block light at or below this is dark (the same number as the gloom clock's {@code gloomLight}). */
    public static final int DARK_LIMIT = 4;
    /** Block light at or above this is bright enough to attract light-hunters (right next to a lantern or with one in hand). */
    public static final int LIT_LIMIT = 13;
    private static final int MIDDLE = 8;

    /** The verdict: the d20 modifier (-3..+3), the two flags and the brightness it was read from. */
    public record Light(int mod, boolean dark, boolean lit, int brightness) {}

    /**
     * The party's light: {@code blockLight[i]} is the block light at member i's eyes, {@code held[i]} says that member carries a light. An
     * empty party is read as dark nothing (mod 0, neither flag). The brightest member decides: one torch lights the whole party for a hunter.
     */
    public static Light evaluate(int[] blockLight, boolean[] held) {
        int n = blockLight == null ? 0 : blockLight.length;
        if (n == 0) return new Light(0, false, false, 0);
        int best = 0;
        for (int i = 0; i < n; i++) {
            int b = Math.max(0, Math.min(15, blockLight[i]));
            if (held != null && i < held.length && held[i]) b = Math.max(b, HELD_LIGHT);
            best = Math.max(best, b);
        }
        return new Light(mod(best), best <= DARK_LIMIT, best >= LIT_LIMIT, best);
    }

    /** The modifier of a brightness: 0 at 8, one point per 2.5 levels either way, clamped to -3..+3. */
    public static int mod(int brightness) {
        int b = Math.max(0, Math.min(15, brightness));
        return Math.max(-3, Math.min(3, (int) Math.round((b - MIDDLE) / 2.5)));
    }
}
