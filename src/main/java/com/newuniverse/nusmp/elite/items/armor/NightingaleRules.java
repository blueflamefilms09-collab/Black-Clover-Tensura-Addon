package com.newuniverse.nusmp.elite.items.armor;

/**
 * Why: the numbers and thresholds of the Nightingale set bonus ("Nightsong") with no Minecraft in them, so they can be tested and the lang text cannot drift from the code. Designed here: the
 * Nightingale is a night singer, so the full set (hood, cloak, leggings, boots) makes its wearer quick, lets them see in the dark and lets a quiet wearer sing the people around them back to health.
 * <ul>
 *   <li>4 of 4 pieces: Speed I, refreshed every second;</li>
 *   <li>in the dark (block and sky light at the feet at most {@link #DARK_LIGHT}): Night Vision, lasting {@link #NIGHT_VISION_TICKS} (longer than 10 s (200 ticks, where vanilla starts to flash it) so it never flickers);</li>
 *   <li>sneaking: the Lullaby, once every {@link #LULLABY_PERIOD} ticks the wearer and allied players within {@link #LULLABY_RADIUS} blocks regain {@link #LULLABY_HEAL} health.</li>
 * </ul>
 */
public final class NightingaleRules {
    private NightingaleRules() {}

    public static final int PIECES = 4;
    public static final int DARK_LIGHT = 7;
    public static final int NIGHT_VISION_TICKS = 260;
    public static final int SPEED_TICKS = 60;
    public static final int LULLABY_PERIOD = 40;
    public static final double LULLABY_RADIUS = 6.0;
    public static final float LULLABY_HEAL = 1.0f;

    /** Is the set bonus active with this many pieces worn? */
    public static boolean bonusActive(int worn) { return worn >= PIECES; }

    /** Does the wearer standing in this light level (0..15) get Night Vision? */
    public static boolean needsNightVision(int light) { return light >= 0 && light <= DARK_LIGHT; }

    /** Is this whole second (the player's tick count / 20, checked once a second) one on which the Lullaby sings? Every {@link #LULLABY_PERIOD} / 20 seconds. */
    public static boolean lullabySecond(long seconds) { return seconds % (LULLABY_PERIOD / 20) == 0; }
}
