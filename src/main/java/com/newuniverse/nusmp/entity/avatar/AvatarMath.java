package com.newuniverse.nusmp.entity.avatar;

import java.util.ArrayList;
import java.util.List;

/**
 * The arithmetic behind the Devil Avatar's moves (pure, no Minecraft types, unit tested): how high the Ceiling Pull lifts and how fast, how
 * hard the Floor Crush slams, how the Inversion Field flips, how many hex bolts a volley throws and how deep the curse they leave is, where a
 * Shadow Step lands, and the timeline of Abyssal Collapse. {@code AvatarMoves} applies the numbers to the world; nothing here knows what a
 * block or an entity is. Speeds are blocks per tick, distances blocks, times ticks.
 */
public final class AvatarMath {
    private AvatarMath() {}

    // ---------------------------------------------------------------- what a telegraph has to allow
    /** A walker, a body in the air steering (a lifted foe), and the half width of a body: the figures the escape tests use. */
    public static final double WALK = 0.215, AIR_STRAFE = 0.13, BODY = 0.3;

    /** Ticks needed to cover 'distance' blocks at 'speed'. */
    public static int ticksToCover(double distance, double speed) { return (int) Math.ceil(Math.max(0, distance) / Math.max(1e-6, speed)); }

    /** Can someone standing 'standoff' blocks from the middle of a ring of 'radius' get out of it within 'ticks' at 'speed'? */
    public static boolean leavable(int ticks, double radius, double standoff, double speed) {
        return ticks >= ticksToCover(radius + BODY - Math.max(0, standoff), speed);
    }

    /** How strongly a push grips a foe: 0.5 for a holder of a Mana Zone or of anti-magic, 1 for everyone else. */
    public static double resist(boolean manaZone, boolean antiMagic) { return manaZone || antiMagic ? 0.5 : 1.0; }

    // ---------------------------------------------------------------- Ceiling Pull
    /** The pull's radius (the ultimate's is wider), how far under and over the boss a foe is still caught, how many are lifted at most. */
    public static final double PULL_RADIUS = 14, ULT_PULL_RADIUS = 16, PULL_BELOW = 6, PULL_ABOVE = 14;
    public static final int PULL_MAX_VICTIMS = 16;
    /**
     * The highest a foe is lifted, the least worth lifting, the clear air kept between the top of a lifted head and the roof (the Levitation
     * that holds it drifts up at most 0.05 a tick for {@link #LEVITATION_TICKS}, so three blocks, and half a block more), the climb speeds.
     */
    public static final double LIFT_MAX = 10, LIFT_MIN = 1.5, CEILING_CLEARANCE = 3.5, LIFT_SPEED_MAX = 0.55, LIFT_SPEED_MIN = 0.12;
    /** The longest the controlled climb runs; the Levitation (3 s) that holds a lifted foe; the Slow Falling (3 s of Levitation and 6 s after it) that lets it down. */
    public static final int LIFT_TICKS = 24, LEVITATION_TICKS = 60, SLOW_FALL_TICKS = 180;
    /** When the hex bolts rain on the lifted foes (ticks after the pull), and the most bolts of one rain. */
    public static final int[] RAIN_AT = {16, 30, 44};
    public static final int RAIN_MAX_BOLTS = 6;

    /**
     * How many blocks a foe is lifted, given the clear air above its head ('headGap' is the distance from the top of its hitbox to the first
     * solid block over it): up to {@link #LIFT_MAX}, never so high that the drift of the Levitation reaches the roof, 0 when there is not
     * room for a lift worth the name (a low ceiling), and the 'resist' share of it for a foe that resists.
     */
    public static double liftHeight(double headGap, double resist) {
        double room = Math.min(LIFT_MAX, headGap - CEILING_CLEARANCE);
        if (room < LIFT_MIN) return 0;
        return room * clamp(resist, 0.25, 1.0);
    }

    /** The upward speed of the climb with 'remaining' blocks to go: fast at first, easing to a crawl so the foe settles at its height. */
    public static double liftSpeed(double remaining) { return clamp(remaining * 0.3, LIFT_SPEED_MIN, LIFT_SPEED_MAX); }

    // ---------------------------------------------------------------- Floor Crush
    /** The radius of one crush mark, how far under its floor and over it a foe is still slammed, the most marks of the ultimate. */
    public static final double CRUSH_RADIUS = 3.4, CRUSH_BELOW = 3, CRUSH_ABOVE = 26;
    public static final int ULT_MAX_MARKS = 8;
    /** Slowness III for 3 s after a slam. */
    public static final int CRUSH_SLOW_TICKS = 60, CRUSH_SLOW_AMP = 2;

    /** A foe whose feet are 'h' blocks over the floor takes 1x (on the ground, or a jump's height) up to 1.7x (six blocks up and more): the air is the worst place to be. */
    public static double crushMultiplier(double h) { return 1.0 + 0.7 * clamp((h - 0.75) / 5.25, 0.0, 1.0); }

    /** The downward speed of a slam of a foe 'h' blocks over the floor: 0.9 on the ground to 1.4 from the height of the ceiling pull, a resisting foe half of it. */
    public static double slamSpeed(double h, double resist) { return (0.9 + Math.min(0.5, Math.max(0, h) * 0.05)) * clamp(resist, 0.25, 1.0); }

    /** How many marks one Floor Crush places: one on the target at 20%, two from 40%, three from 80%, four at the Gates. */
    public static int markCount(int power) { return power >= 100 ? 4 : power >= 80 ? 3 : power >= 40 ? 2 : 1; }

    /** Keeps the points that are at least 'spacing' from every point kept before them, at most 'cap' (each point is {x, z}); the first always stays. */
    public static List<double[]> spreadMarks(List<double[]> points, double spacing, int cap) {
        List<double[]> out = new ArrayList<>();
        for (double[] p : points) {
            if (out.size() >= cap) break;
            boolean clear = true;
            for (double[] k : out) {
                double dx = k[0] - p[0], dz = k[1] - p[1];
                if (dx * dx + dz * dz < spacing * spacing) { clear = false; break; }
            }
            if (clear) out.add(p);
        }
        return out;
    }

    // ---------------------------------------------------------------- Inversion Field
    /** The field's radius, how far under its mark and over it it grips, its length, the time between two flips, the ease-in of each flip, the interval of its steps. */
    public static final double FIELD_RADIUS = 7, FIELD_BELOW = 2, FIELD_ABOVE = 12;
    public static final int FIELD_TICKS = 160, FLIP_TICKS = 30, FIELD_RAMP = 10, FIELD_STEP = 2;
    /** The speed the field lifts at, and the speed it presses down at. */
    public static final double FIELD_UP_SPEED = 0.18, FIELD_DOWN_SPEED = 0.40;

    /** +1 while the field lifts, -1 while it presses down, 0 outside its life: it begins by lifting and flips every {@link #FLIP_TICKS}. */
    public static int inversionDir(int age) {
        if (age < 0 || age >= FIELD_TICKS) return 0;
        return (age / FLIP_TICKS) % 2 == 0 ? 1 : -1;
    }

    /** The vertical speed the field asks for at 'age' (positive up): its direction times its speed, easing in over {@link #FIELD_RAMP} ticks after every flip, times 'resist'. */
    public static double inversionPush(int age, double resist) {
        int dir = inversionDir(age);
        if (dir == 0) return 0;
        double ease = 0.5 + 0.5 * clamp((age % FLIP_TICKS) / (double) FIELD_RAMP, 0.0, 1.0);
        return dir * (dir > 0 ? FIELD_UP_SPEED : FIELD_DOWN_SPEED) * ease * clamp(resist, 0.25, 1.0);
    }

    // ---------------------------------------------------------------- Hex Volley
    public static final double BOLT_SPEED = 1.15, BOLT_REACH = 0.55, BOLT_TURN = 0.10;
    /** Ticks a bolt lives, ticks between two bolts leaving, the hits one volley may land on one victim. */
    public static final int BOLT_LIFE = 32, BOLT_STAGGER = 2, BOLT_MAX_HITS = 3;

    /** Bolts in a volley for a power with 'castSlots' casts (1 to 4): 5, 6, 7 and 9. */
    public static int boltCount(int castSlots) { return new int[]{5, 6, 7, 9}[(int) clamp(castSlots, 1, 4) - 1]; }

    /** Ticks from the first bolt leaving until the last one has surely ended. */
    public static int volleyTicks(int bolts) { return Math.max(0, bolts - 1) * BOLT_STAGGER + BOLT_LIFE + 2; }

    /** The depth of the Curse of Decay a victim of 'hits' bolts is left with (0 for none): one level per two bolts, at most 1 under 60% power, 2 under the Gates, 3 at the Gates. */
    public static int hexLevel(int hits, int power) {
        if (hits <= 0) return 0;
        int most = power >= 100 ? 3 : power >= 60 ? 2 : 1;
        return Math.min(most, 1 + (hits - 1) / 2);
    }

    // ---------------------------------------------------------------- Dread Brand
    /** The brand of the Dread Brand lasts 3 s as long as its victim stays in the sigil; the brand a hex volley leaves lasts 5 s. */
    public static final int DREAD_TICKS = 60, HEX_BRAND_TICKS = 100;

    // ---------------------------------------------------------------- Shadow Step
    /** How far behind its target the Avatar steps out, the reach of the strike it makes there, and the ticks between arriving and striking. */
    public static final double SHADOW_BEHIND = 2.2, SHADOW_STRIKE_RADIUS = 3.8;
    public static final int SHADOW_STRIKE_DELAY = 4;

    /** The spot 'distance' blocks behind something at (x, z) that faces 'yawDeg' (Minecraft yaw: 0 faces +z, 90 faces -x): {x, z}. */
    public static double[] behind(double x, double z, float yawDeg, double distance) {
        double a = Math.toRadians(yawDeg);
        return new double[]{x + Math.sin(a) * distance, z - Math.cos(a) * distance};
    }

    // ---------------------------------------------------------------- Void Claws
    /** The reach of the claws and the half angle of the sweep (140 degrees in all). */
    public static final double CLAW_REACH = 6.0, CLAW_HALF_ANGLE = Math.toRadians(70);

    /**
     * Is a body at offset (dx, dz) inside the cone that opens along (fx, fz) with 'halfAngle' and 'reach'? 'radius' is the body's half width:
     * it counts if any of it is in the cone (a body on top of the apex always is).
     */
    public static boolean inCone(double dx, double dz, double fx, double fz, double halfAngle, double reach, double radius) {
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d <= radius) return true;
        if (d > reach + radius) return false;
        double fl = Math.sqrt(fx * fx + fz * fz);
        if (fl < 1e-9) return false;
        double cos = (dx * fx + dz * fz) / (d * fl);
        double slack = Math.asin(Math.min(1.0, radius / d));
        return Math.acos(clamp(cos, -1.0, 1.0)) <= halfAngle + slack;
    }

    // ---------------------------------------------------------------- Curse Nova
    /** The reach of the nova's wave: 10 blocks, 12 at the Gates; 13 for the ultimate's. */
    public static double novaReach(boolean gates) { return gates ? 12 : 10; }

    public static final double ULT_NOVA_REACH = 13;

    // ---------------------------------------------------------------- Abyssal Collapse
    /** Ticks after the release: the floor marks are drawn under the lifted foes, the crush lands on them, the curse pulse leaves the Avatar. */
    public static final int ULT_MARK_AT = 26, ULT_CRUSH_AT = 58, ULT_NOVA_AT = 64;

    // ---------------------------------------------------------------- small helpers
    static double clamp(double v, double lo, double hi) { return v < lo ? lo : v > hi ? hi : v; }
}
