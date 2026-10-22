package com.newuniverse.nusmp.firecast;

/**
 * Why: every number and every small decision of the four fire casting styles lives here as pure functions (no game classes), so the combo, the charge, the swing hitbox, the caps and the
 * emitter points are unit tested once and the server code only asks. See docs/magic/fire_casting.md for the flows these numbers drive.
 *
 * <p>Ticks are game ticks (20 a second), distances are blocks, angles degrees. Vectors are plain doubles (x, y, z); a Minecraft yaw of 0 faces +z, the forward vector of a yaw is
 * (-sin, 0, cos) and the player's right is (-cos, 0, -sin).</p>
 */
public final class FireCastRules {
    private FireCastRules() {}

    // ================================================================================================ shared
    /** The most any one player can ask a fire style packet of the server in a row (a key press) before it is ignored, in ticks. */
    public static final int PACKET_GAP = 10;
    /** Effects of the styles reach players within this many blocks only. */
    public static final double FX_RADIUS = 48.0;
    /** The same message to the same player is not repeated within this many ticks. */
    public static final int MESSAGE_GAP = 40;

    /** True when {@code now} is at least {@code gap} ticks after {@code last}. */
    public static boolean gapOk(long now, long last, int gap) { return now - last >= gap; }

    // ================================================================================================ Magna
    /** Ticks from the toss of the ball to the swing's contact frame (the server tests the hit on this tick; the clip's contact keyframe is the same moment). */
    public static final int SWING_IMPACT_TICK = 8;
    /** The swing clip's length in ticks (wind-up, contact at {@link #SWING_IMPACT_TICK}, follow-through). */
    public static final int SWING_CLIP_TICKS = 16;
    /** How long the hovering ball may live at most (the swing's tick plus a grace), a ball that is not hit is gone by then. */
    public static final int BALL_LIFE = SWING_IMPACT_TICK + 8;
    /** The ball hangs this far in front of the eye, a little below it. */
    public static final double BALL_DISTANCE = 1.9, BALL_DROP = 0.25;
    /** The bat's reach from the eye to the ball's centre and the half angle of the swing cone around the look direction. */
    public static final double SWING_REACH = 3.6, SWING_HALF_ANGLE = 55.0;
    /** At least this many ticks between two swings of one player, and only one ball at a time per caster, a few on the whole server. */
    public static final int MIN_SWING_GAP = 24, BALLS_PER_PLAYER = 1;
    /** A missed ball drops and bursts weakly: this much raw damage in this radius, at most this many foes. */
    public static final float WEAK_BURST_DAMAGE = 2.0f;
    public static final double WEAK_BURST_RADIUS = 2.0;
    public static final int WEAK_BURST_TARGETS = 4;
    /** The optional homing of a launched bolt: only a target within this cone of the look direction and this range is followed. */
    public static final double HOMING_CONE = 9.0, HOMING_RANGE = 24.0;

    /** The centre of the tossed ball: in front of the eye along the look, a little low. */
    public static double[] ballPosition(double ex, double ey, double ez, double lx, double ly, double lz) {
        double n = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (n < 1e-6) { lx = 0; ly = 0; lz = 1; n = 1; }
        return new double[]{ex + lx / n * BALL_DISTANCE, ey + ly / n * BALL_DISTANCE - BALL_DROP, ez + lz / n * BALL_DISTANCE};
    }

    /** The angle in degrees between two vectors (0 when either is zero). */
    public static double angle(double ax, double ay, double az, double bx, double by, double bz) {
        double na = Math.sqrt(ax * ax + ay * ay + az * az), nb = Math.sqrt(bx * bx + by * by + bz * bz);
        if (na < 1e-9 || nb < 1e-9) return 0.0;
        double c = (ax * bx + ay * by + az * bz) / (na * nb);
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, c))));
    }

    /** Does the swing (from the eye along the look, at the contact frame) catch a ball at (bx, by, bz)? Distance within the reach and inside the cone. */
    public static boolean swingHits(double ex, double ey, double ez, double lx, double ly, double lz, double bx, double by, double bz) {
        double dx = bx - ex, dy = by - ey, dz = bz - ez;
        if (dx * dx + dy * dy + dz * dz > SWING_REACH * SWING_REACH) return false;
        return angle(lx, ly, lz, dx, dy, dz) <= SWING_HALF_ANGLE;
    }

    /** Is a point at offset (dx, dy, dz) from the caster inside the homing cone around the look direction and the homing range? */
    public static boolean inHomingCone(double lx, double ly, double lz, double dx, double dy, double dz) {
        if (dx * dx + dy * dy + dz * dz > HOMING_RANGE * HOMING_RANGE) return false;
        return angle(lx, ly, lz, dx, dy, dz) <= HOMING_CONE;
    }

    // ================================================================================================ Mereoleona
    /** The longest pause between two punches of one combo, and how much longer it is while the Mana Zone is open. */
    public static final int COMBO_TIMEOUT = 30, COMBO_TIMEOUT_ZONE_BONUS = 12;
    /** An unarmed combo counts up to this and no further. */
    public static final int COMBO_CAP = 8;
    /** A readied page stays readied this long (30 seconds), then it must be readied again. */
    public static final int ARM_TICKS = 600;
    /** A page whose cost tier is this percent or more (zone and signature pages) needs the long finisher. */
    public static final double LONG_FINISHER_COST = 20.0;
    public static final int SHORT_FINISHER = 3, LONG_FINISHER = 5, MIN_FINISHER = 2;

    public static int comboTimeout(boolean zone) { return COMBO_TIMEOUT + (zone ? COMBO_TIMEOUT_ZONE_BONUS : 0); }

    /** How many landed punches release the readied page: 3 (starter, mid) or 5 (zone, signature), one fewer with the Mana Zone open, never below 2. */
    public static int finisherHits(double costPercent, boolean zone) {
        int n = costPercent >= LONG_FINISHER_COST ? LONG_FINISHER : SHORT_FINISHER;
        return Math.max(MIN_FINISHER, zone ? n - 1 : n);
    }

    /** The fist's point: in front of the shoulder, low, on the right side (the arm that throws the finisher). */
    public static double[] fistPosition(double ex, double ey, double ez, double lx, double ly, double lz) {
        double n = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (n < 1e-6) { lx = 0; ly = 0; lz = 1; n = 1; }
        lx /= n; ly /= n; lz /= n;
        double h = Math.sqrt(lx * lx + lz * lz);
        double rx = h < 1e-6 ? -1 : -lz / h, rz = h < 1e-6 ? 0 : lx / h;          // the right of the look, level
        return new double[]{ex + lx * 0.9 + rx * 0.35, ey + ly * 0.9 - 0.32, ez + lz * 0.9 + rz * 0.35};
    }

    // ================================================================================================ Leopold
    /** The key must be held this many ticks for the plain cast (the grimoire's own cast time); the spiral winds beyond it. */
    public static final int CHARGE_FREE_TICKS = 20;
    /** Hold ticks at which the spiral reaches level 1, 2, 3, 4 (the hard cap). */
    public static final int[] LEVEL_AT = {20, 40, 65, 90};
    public static final int MAX_LEVEL = LEVEL_AT.length, MAX_HOLD = LEVEL_AT[LEVEL_AT.length - 1];
    /** Mana drawn every this many ticks past the free ticks, as a fraction of the page's own cost (the page's cost already carries the mana law). */
    public static final int CHARGE_STEP = 5;
    public static final double CHARGE_STEP_COST = 0.05;
    /** What a charged cast does to the spell, by level 0..4: damage and size multipliers (hard cap at the last), and for how many ticks after the cast they apply. */
    private static final double[] DAMAGE = {1.0, 1.0, 1.15, 1.3, 1.5};
    private static final double[] SIZE = {1.0, 1.0, 1.12, 1.25, 1.4};
    public static final int CHARGE_EFFECT_TICKS = 80;

    /** The spiral's level after {@code held} ticks of the key: 0 until the first threshold, then 1..4 (capped). */
    public static int chargeLevel(int held) {
        int l = 0;
        for (int at : LEVEL_AT) if (held >= at) l++;
        return l;
    }

    public static double chargeDamage(int level) { return DAMAGE[Math.max(0, Math.min(MAX_LEVEL, level))]; }

    public static double chargeSize(int level) { return SIZE[Math.max(0, Math.min(MAX_LEVEL, level))]; }

    /** Is there a mana step due on the tick the key has been held {@code held} ticks (steps start after the free ticks and stop at the cap)? */
    public static boolean chargeStepDue(int held) { return held > CHARGE_FREE_TICKS && held <= MAX_HOLD && (held - CHARGE_FREE_TICKS) % CHARGE_STEP == 0; }

    // ================================================================================================ Fuegoleon: the Salamander
    /** The Salamander is a prop entity; its variant is the low four bits of the prop param. */
    public static final int V_BALL = 0, V_SPIRAL = 1, V_SALAMANDER = 2, V_DRAGON = 3, V_MASK = 15;
    /** Param bit: the small Salamander is hidden (the big one is out). Bits 12 and up carry a level (spiral) or the tick the breath begins (dragon). */
    public static final int HIDDEN = 1 << 8, EXTRA_SHIFT = 12;
    /** The big form: how long it stays, how long before it may come again, the cost tier of the pages that call it, how many at once. */
    public static final int DRAGON_TICKS = 60, DRAGON_COOLDOWN = 400, DRAGON_BREATH_TICK = 8, DRAGONS_PER_PLAYER = 1;
    public static final double DRAGON_COST_PERCENT = 20.0;
    /** One small Salamander per player. */
    public static final int SPIRITS_PER_PLAYER = 1;
    /** The most the emitter may sit away from the caster's eye (so a spell still goes where the caster aims). */
    public static final double EMITTER_MAX_SHIFT = 4.6;
    /** A spell start point within this many blocks of the caster's eye or feet counts as 'the caster's own start' and moves to the emitter. */
    public static final double EMITTER_CAPTURE = 2.2;

    public static int param(int variant, int extra) { return (variant & V_MASK) | (extra << EXTRA_SHIFT); }

    public static int variant(int param) { return param & V_MASK; }

    public static int extra(int param) { return param >>> EXTRA_SHIFT; }

    public static boolean hidden(int param) { return (param & HIDDEN) != 0; }

    private static double[] axes(double yawDeg) {
        double r = Math.toRadians(yawDeg), s = Math.sin(r), c = Math.cos(r);
        return new double[]{-s, c, -c, -s};                                             // forward x z, right x z
    }

    /** Where the small Salamander sits: on the right shoulder (a little back, up) of a body at (px, py, pz) turned {@code bodyYaw} degrees. */
    public static double[] shoulder(double px, double py, double pz, double bodyYaw) {
        double[] o = new double[3];
        shoulderTo(o, px, py, pz, bodyYaw);
        return o;
    }

    /** {@link #shoulder} into a caller's array (the client draws every frame and allocates nothing). */
    public static void shoulderTo(double[] o, double px, double py, double pz, double bodyYaw) {
        double r = Math.toRadians(bodyYaw), s = Math.sin(r), c = Math.cos(r);
        o[0] = px + (-c) * 0.40 + (-s) * -0.04;
        o[1] = py + 1.46;
        o[2] = pz + (-s) * 0.40 + c * -0.04;
    }

    /** The small Salamander's mouth: in front of its perch, a little up. */
    public static double[] shoulderMouth(double px, double py, double pz, double bodyYaw) {
        double[] s = shoulder(px, py, pz, bodyYaw), a = axes(bodyYaw);
        return new double[]{s[0] + a[0] * 0.34 + a[2] * 0.02, s[1] + 0.10, s[2] + a[1] * 0.34 + a[3] * 0.02};
    }

    /** Where the big dragon hangs: above and behind the caster, facing the way the caster faces. */
    public static double[] dragonPosition(double px, double py, double pz, double bodyYaw) {
        double[] o = new double[3];
        dragonPositionTo(o, px, py, pz, bodyYaw);
        return o;
    }

    /** {@link #dragonPosition} into a caller's array. */
    public static void dragonPositionTo(double[] o, double px, double py, double pz, double bodyYaw) {
        double r = Math.toRadians(bodyYaw), s = Math.sin(r), c = Math.cos(r);
        o[0] = px - (-s) * 1.8;
        o[1] = py + 3.3;
        o[2] = pz - c * 1.8;
    }

    /** The big dragon's mouth: out in front of its body (the neck stretched forward), at its head height. */
    public static double[] dragonMouth(double px, double py, double pz, double bodyYaw) {
        double[] d = dragonPosition(px, py, pz, bodyYaw), a = axes(bodyYaw);
        return new double[]{d[0] + a[0] * 3.0, d[1] + 0.25, d[2] + a[1] * 3.0};
    }

    /**
     * The emitter a spell start is moved to: the start is only moved when it was the caster's own start (within {@link #EMITTER_CAPTURE} of the eye or the feet), and the emitter is kept within
     * {@link #EMITTER_MAX_SHIFT} of the eye so the spell still leaves toward the aim. Returns the point to use (the start itself when it is not captured).
     */
    public static double[] emit(double sx, double sy, double sz, double eyeX, double eyeY, double eyeZ, double feetX, double feetY, double feetZ, double mx, double my, double mz) {
        double de = dist(sx, sy, sz, eyeX, eyeY, eyeZ), df = dist(sx, sy, sz, feetX, feetY, feetZ);
        if (de > EMITTER_CAPTURE && df > EMITTER_CAPTURE) return new double[]{sx, sy, sz};
        double dx = mx - eyeX, dy = my - eyeY, dz = mz - eyeZ, n = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (n > EMITTER_MAX_SHIFT) { double k = EMITTER_MAX_SHIFT / n; mx = eyeX + dx * k; my = eyeY + dy * k; mz = eyeZ + dz * k; }
        return new double[]{mx, my, mz};
    }

    private static double dist(double ax, double ay, double az, double bx, double by, double bz) {
        double dx = ax - bx, dy = ay - by, dz = az - bz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
