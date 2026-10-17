package com.newuniverse.nusmp.arc.sds;

/**
 * Why: Meliodas does not get stronger by the same ladder as the Zogratis, he gets stronger the way the series tells it: the Demon Mark spreads over
 * him step by step, in Assault Mode he gives up his guard for violence, and at the Gates he is the Demon King. The framework already gives every
 * boss five power steps (20, 40, 60, 80, 100); this pure class says what each step MEANS for him (the mark level, the extra damage and speed, how
 * long and how big his Full Counter is, whether he can raise it at all, how many images and dashes he has, how big his Hellblaze is) and holds the
 * arithmetic of the Demon King's Wrath (the nova that grows with the magic thrown into it). Keeping it here means one test proves the ladder never
 * goes backwards, the nova can always be walked out of, and Assault Mode really is the window that the party was promised.
 */
public final class DemonKingMath {
    private DemonKingMath() {}

    /** The power steps of the framework, by what they are to Meliodas. */
    public static final int CAPTAIN = 20, MARK = 40, SPREAD = 60, ASSAULT = 80, KING = 100;

    // ---------------------------------------------------------------- the mark
    /** 0 the Captain (20), 1 the Demon Mark wakes (40), 2 the mark spreads (60), 3 Assault Mode (80), 4 the Demon King (100). */
    public static int mark(int power) { return power >= KING ? 4 : power >= ASSAULT ? 3 : power >= SPREAD ? 2 : power >= MARK ? 1 : 0; }

    /** Assault Mode: from 80% until the Gates. */
    public static boolean assault(int power) { return power >= ASSAULT && power < KING; }

    /** The Demon King: the Gates are open. */
    public static boolean king(int power) { return power >= KING; }

    /** Full Counter needs a calm mind: he cannot raise it in Assault Mode (and that is the party's window). */
    public static boolean canCounter(int power) { return !assault(power); }

    private static final double[] WRATH = {1.00, 1.04, 1.10, 1.22, 1.30};
    private static final double[] SPEED = {0.00, 0.00, 0.03, 0.12, 0.08};

    /** Extra damage factor of his own, on top of the framework's power tuning: 1 at 20% up to 1.30 as the Demon King. */
    public static double damageScale(int power) { return lerp(WRATH, power); }

    /** Extra speed (a fraction added to his movement speed): none until the mark spreads, most in Assault Mode. */
    public static double speedBonus(int power) { return lerp(SPEED, power); }

    /** Assault Mode drops his guard: he takes this much more from every blow (1 otherwise). */
    public static double takenFactor(int power) { return assault(power) ? 1.12 : 1.0; }

    // ---------------------------------------------------------------- Full Counter
    private static final int[] STANCE_TICKS = {50, 60, 70, 0, 80};
    private static final int[] STANCE_COOLDOWN = {380, 360, 330, 0, 300};
    private static final double[] BREAK = {0.16, 0.18, 0.20, 0.0, 0.26};

    /** Ticks the stance lasts: 50 at 20%, 60, 70, none in Assault Mode, 80 as the Demon King. */
    public static int stanceTicks(int power) { return STANCE_TICKS[mark(power)]; }

    /** Ticks after the stance ends before it can be raised again: 380, 360, 330, (none), 300. */
    public static int stanceCooldown(int power) { return STANCE_COOLDOWN[mark(power)]; }

    /** The share of his maximum health in magic the stance swallows before it shatters: 0.16, 0.18, 0.20, (none), 0.26. */
    public static double breakFraction(int power) { return BREAK[mark(power)]; }

    /** The conical half-angle (radians) in front of him that the stance covers: a little over a hemisphere. */
    public static final double FRONT_HALF_ANGLE = Math.toRadians(100);

    /** Is a caster at {@code dot} (cosine of the angle between his facing and the direction to the caster) inside the covered cone? */
    public static boolean frontal(double dot) { return dot >= Math.cos(FRONT_HALF_ANGLE); }

    // ---------------------------------------------------------------- Lostvayne
    /** Images of Lostvayne's Real Image: none before the mark spreads, three, four as the Demon King. */
    public static int imageCount(int power) { return power < SPREAD ? 0 : king(power) ? 4 : 3; }

    /** Dashes of the Assault Rush: three in Assault Mode, four as the Demon King. */
    public static int rushDashes(int power) { return power < ASSAULT ? 0 : king(power) ? 4 : 3; }

    /** Ticks between one image's strike and the next. */
    public static final int IMAGE_GAP = 8;

    /** Where the images stand: {@code n} spots on a ring of {@code radius} round (tx, tz), equally spaced, the first at angle {@code phase}. Each is {x, z}. */
    public static double[][] imageSpots(int n, double tx, double tz, double radius, double phase) {
        int count = Math.max(0, Math.min(8, n));
        double[][] out = new double[count][];
        for (int i = 0; i < count; i++) {
            double a = phase + i * Math.PI * 2 / count;
            out[i] = new double[]{tx + Math.cos(a) * radius, tz + Math.sin(a) * radius};
        }
        return out;
    }

    // ---------------------------------------------------------------- Hellblaze
    /** Radius of a Hellblaze field: 3.4 blocks at 20%, 0.45 more for each mark level (5.2 as the Demon King). */
    public static double fieldRadius(int power) { return 3.4 + 0.45 * mark(power); }

    /** Ticks a Hellblaze field burns: 80, +12 per mark level. */
    public static int fieldTicks(int power) { return 80 + 12 * mark(power); }

    /** Base damage of one burn tick (every 10 ticks) inside a field, before the boss's damage law. */
    public static float burnDamage(int power) { return 2.4f + 0.4f * mark(power); }

    /** Ticks the burn clings to someone who left the field: 40, +10 per mark level. Water does not put it out; leaving does, in time. */
    public static int lingerTicks(int power) { return 40 + 10 * mark(power); }

    // ---------------------------------------------------------------- the Demon King's Wrath (the nova)
    /** The nova's radius with nothing thrown into it, its most, the share of his maximum health in magic that fills it, and the share of physical damage that breaks it. */
    public static final double NOVA_BASE_RADIUS = 7.0, NOVA_MAX_RADIUS = 11.0, NOVA_FEED_FULL = 0.30, NOVA_BREAK = 0.12;
    /** Ticks from the end of the channel to the detonation: the ring stands still and red for all of them. */
    public static final int NOVA_LOCK_TICKS = 60;
    /** The pace of a player on foot (blocks a second) that the nova must always allow to get out. */
    public static final double WALK_SPEED = 4.3;

    /** 0..1: how much of the nova's growth the magic absorbed so far has filled (the share of his maximum health). */
    public static double feed(double absorbedShare) { return Math.max(0.0, Math.min(1.0, absorbedShare / NOVA_FEED_FULL)); }

    /** The nova's radius: 7 blocks, up to 11 when the party fed it. */
    public static double novaRadius(double absorbedShare) { return NOVA_BASE_RADIUS + (NOVA_MAX_RADIUS - NOVA_BASE_RADIUS) * feed(absorbedShare); }

    /** Base damage of the nova (before the boss's damage law): 26, up to 40 when fed. */
    public static float novaDamage(double absorbedShare) { return (float) (26.0 + 14.0 * feed(absorbedShare)); }

    /** Can someone at the very centre walk out of a nova of this radius during the lock? (The test that keeps the numbers honest.) */
    public static boolean walkable(double radius, int lockTicks) { return radius <= WALK_SPEED * lockTicks / 20.0; }

    /** Has the physical damage dealt during the channel broken it? */
    public static boolean broken(double physicalShare) { return physicalShare >= NOVA_BREAK; }

    // ---------------------------------------------------------------- helpers
    /** Linear value between the anchors at power 20, 40, 60, 80, 100 (the same anchors as the framework's tuning). */
    private static double lerp(double[] anchors, int power) {
        double x = (Math.max(CAPTAIN, Math.min(KING, power)) - CAPTAIN) / 20.0;
        int i = (int) Math.floor(x);
        if (i >= anchors.length - 1) return anchors[anchors.length - 1];
        return anchors[i] + (anchors[i + 1] - anchors[i]) * (x - i);
    }
}
