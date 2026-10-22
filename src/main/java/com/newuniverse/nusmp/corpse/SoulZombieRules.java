package com.newuniverse.nusmp.corpse;

/**
 * Why: Soul Corpse Magic summons real zombies now (the Marionette, Necropolis, Legion and Reanimation pages), and what one is worth is a few numbers a unit test can pin: its health and
 * blow grow with the grimoire's power the way the old puppets did (the same 1.0 to 1.4 size factor), a summon lasts longer for a mighty caster (up to +50 percent, the same rule as
 * {@code SongRebuildKit.span}), nobody owns more than the cap at once, and daylight never burns one. No Minecraft types here; {@link SoulZombies} turns the numbers into attributes.
 */
public final class SoulZombieRules {
    private SoulZombieRules() {}

    /** Default most zombies one caster owns at once (config {@code corpse.zombieCap}); the old puppets were capped at 4. */
    public static final int CAP_DEFAULT = 6;
    /** A zombie of a novice caster: health, blow (raw, before the balance law's cap on players) and walking speed (a vanilla zombie has 20, 3 and 0.23). */
    public static final double BASE_HEALTH = 24.0, BASE_ATTACK = 5.0, SPEED = 0.24;
    /** The size factor of the mightiest caster (1.0 for a novice), as in {@code SongRebuildKit.size}. */
    public static final double MAX_SCALE = 1.4;
    /** Life of a summon in ticks is never shorter or longer than this, whatever the config or the power says. */
    public static final int MIN_LIFE = 60, MAX_LIFE = 12000;
    /** Every kind of daylight, by design: a summoned zombie never ignites from the sun. */
    public static final boolean SUN_BURNS = false;

    /** The growth of a summon from the caster's EP scale ({@code BalanceLaw.epScale}, 1 up to 12): 1.0 up to {@link #MAX_SCALE}. */
    public static double scale(double epScale) {
        return Math.min(MAX_SCALE, 1.0 + 0.05 * Math.min(8.0, Math.max(0.0, epScale - 1.0)));
    }

    public static double health(double scale) { return BASE_HEALTH * clampScale(scale); }

    public static double attack(double scale) { return BASE_ATTACK * clampScale(scale); }

    /** Ticks a summon of 'baseTicks' lasts for a caster of this EP scale: up to +50 percent, within {@link #MIN_LIFE}..{@link #MAX_LIFE}. */
    public static int life(int baseTicks, double epScale) {
        double f = Math.min(1.5, 1.0 + 0.04 * Math.max(0.0, epScale - 1.0));
        return Math.max(MIN_LIFE, Math.min(MAX_LIFE, (int) (baseTicks * f)));
    }

    /** How many of 'wanted' may be made now and how many of the caster's oldest must go so that the live count never passes the cap. {made, dismissed}. */
    public static int[] fit(int live, int wanted, int cap) {
        int c = Math.max(1, cap);
        int made = Math.max(0, Math.min(wanted, c));
        int dismissed = Math.max(0, live + made - c);
        return new int[]{made, Math.min(dismissed, live)};
    }

    private static double clampScale(double s) { return Math.max(1.0, Math.min(MAX_SCALE, s)); }
}
