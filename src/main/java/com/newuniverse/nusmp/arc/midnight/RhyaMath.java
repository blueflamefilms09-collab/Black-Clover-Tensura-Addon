package com.newuniverse.nusmp.arc.midnight;

import com.newuniverse.nusmp.entity.zogratis.PowerCurve;

/**
 * Why: every number of Rhya's fight that is not a damage figure, in one pure class, so the telegraph and the strike read the same constants and a test can
 * prove the fairness rules. The imitation moves take their SHAPES (how many bolts, how big a nova, how long it is marked) from the Tower tables that Lucius
 * already uses ({@code TowerMath}), so "a spell copied from a starter page is a bolt, from a signature page a rain" means the same thing in both fights.
 */
public final class RhyaMath {
    private RhyaMath() {}

    private static int byStage(int power, int... values) { return values[Math.max(0, Math.min(values.length - 1, PowerCurve.stage(power)))]; }

    /** Magics he borrows when nothing has hit him yet and no foe carries a grimoire: the four the story shows him throwing, then darkness. */
    public static final String[] DEFAULTS = {"FLAME", "WATER", "LIGHT", "SPATIAL", "DARK"};

    /** The default magic for a counter: the same salt always gives the same one, and every one comes up in turn. */
    public static String defaultFor(int salt) { return DEFAULTS[Math.floorMod(salt, DEFAULTS.length)]; }

    /** How many magics he holds at once: one, two once the Third Eye is open. */
    public static int memoryCapacity(boolean thirdEyeOpen) { return thirdEyeOpen ? 2 : 1; }

    /** Copied bolts in one volley, and swords of the copied Light Sword. */
    public static int boltCount(int power) { return byStage(power, 3, 3, 4, 5, 6); }

    public static int lightCopyBlades(int power) { return byStage(power, 3, 3, 4, 4, 5); }

    /** Novas of the Imitation: Nova (the Tower table: 1 or 2). */
    public static int novaCount(int power) { return byStage(power, 1, 1, 2, 2, 2); }

    // ---------------------------------------------------------------- the copied bolts
    public static final double BOLT_SPEED = 1.3, BOLT_RADIUS = 0.6, BOLT_SPREAD = 0.6;
    public static final int BOLT_LIFE = 36, BOLT_HITS_PER_FOE = 2;
    /** The volley follows its target until this many ticks are left of the wind-up, then the lines freeze and it is only a matter of stepping aside. */
    public static final int BOLT_LOCK = 8;
    /** The copied lance and its aim: it follows the target until this many ticks are left, then freezes. */
    public static final double LANCE_LENGTH = 28.0, LANCE_HALF_WIDTH = 1.2;
    public static final int LANCE_LOCK = 10;
    /** The copied nova's radius and how many foes it may be set on. */
    public static final double NOVA_RADIUS = 5.0;

    // ---------------------------------------------------------------- the story's copies
    public static final double AVIDYA_LENGTH = 20.0, AVIDYA_HALF_WIDTH = 3.2;
    public static final double JAVELIN_LENGTH = 34.0, JAVELIN_HALF_WIDTH = 0.8;
    public static final double SCOUT_BEHIND = 2.4, SCOUT_RADIUS = 2.6;
    public static final double SEAL_RADIUS = 3.0;

    /** Rings of chains set at once: one, then two from the fourth power step. */
    public static int sealCount(int power) { return byStage(power, 1, 1, 1, 2, 2); }
    public static final double COPY_BREATH_REACH = 11.0, COPY_BREATH_HALF_ANGLE = Math.toRadians(28);
    public static final double LIGHT_COPY_SPEED = 3.5, LIGHT_COPY_SPREAD = 0.8;
    public static final int LIGHT_COPY_LOCK = 8;

    // ---------------------------------------------------------------- the Stolen Spellbook
    public static final int BOOK_SALVOS = 5, BOOK_GAP = 14, BOOK_WARN = 18;
    public static final double BOOK_RADIUS = 3.0;

    /** How close (blocks) he dashes to a foe to touch its grimoire. */
    public static final double TOUCH_REACH = 2.0;
}
