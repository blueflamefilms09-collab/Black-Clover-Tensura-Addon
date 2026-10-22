package com.newuniverse.nusmp.undine;

/**
 * Why: every number and every small decision of the Undine (Noelle Silva's water Spirit Lord as a casting companion, docs/magic/undine.md) lives here as pure functions (no game classes), so the
 * caps, the timings, the emitter points and the prop word are unit tested once and the server code only asks.
 *
 * <p>Ticks are game ticks (20 a second), distances are blocks, angles degrees. A Minecraft yaw of 0 faces +z, the forward vector of a yaw is (-sin, 0, cos) and the player's right is (-cos, 0, -sin).</p>
 */
public final class UndineRules {
    private UndineRules() {}

    // ================================================================================================ the prop word
    /**
     * The Undine is a prop entity; everything the clients need to draw her is ONE int, the prop param: bits 0-3 the variant (companion, full form), bit 8 hidden (the companion while the full form is
     * out), bits 9-11 the face ({@link UndineFace}), bits 12-15 the pose ({@link UndinePose}), bits 16-31 the game tick (mod 65536) at which the pose began (0 is 'never').
     */
    public static final int V_COMPANION = 0, V_FULL = 1, V_MASK = 15;
    public static final int HIDDEN = 1 << 8, FACE_SHIFT = 9, FACE_MASK = 7, POSE_SHIFT = 12, POSE_MASK = 15, STAMP_SHIFT = 16, STAMP_MASK = 0xFFFF;

    /** The word for a variant (the rest is zero: no pose, calm face, no stamp). */
    public static int param(int variant) { return variant & V_MASK; }

    public static int variant(int param) { return param & V_MASK; }

    public static boolean hidden(int param) { return (param & HIDDEN) != 0; }

    public static int face(int param) { return (param >>> FACE_SHIFT) & FACE_MASK; }

    public static int pose(int param) { return (param >>> POSE_SHIFT) & POSE_MASK; }

    public static int stamp(int param) { return param >>> STAMP_SHIFT; }

    /** The word with this pose, face and start tick (the variant and the hidden bit are kept). */
    public static int withPose(int param, int pose, int face, long gameTime) {
        int keep = param & (V_MASK | HIDDEN);
        return keep | ((face & FACE_MASK) << FACE_SHIFT) | ((pose & POSE_MASK) << POSE_SHIFT) | (stampOf(gameTime) << STAMP_SHIFT);
    }

    /** The word with the pose and the face cleared (the stamp stays, it only matters while a pose is set). */
    public static int withoutPose(int param) { return param & ~((FACE_MASK << FACE_SHIFT) | (POSE_MASK << POSE_SHIFT)); }

    /** The stamp the server writes when a pose begins (0 is reserved for 'never'). */
    public static int stampOf(long gameTime) {
        int s = (int) (gameTime & STAMP_MASK);
        return s == 0 ? 1 : s;
    }

    /** Ticks since the stamp (a huge number for 'never'). */
    public static int sinceStamp(int stamp, long gameTime) { return stamp == 0 ? Integer.MAX_VALUE : (int) ((gameTime - stamp) & STAMP_MASK); }

    // ================================================================================================ timings and caps
    /** The full form: how long it stays (5 s), how long before it may come again (30 s), the cost tier of the pages that call it, how many a caster may have. */
    public static final int FULL_TICKS = 100, FULL_COOLDOWN = 600, FULL_PER_PLAYER = 1;
    public static final double FULL_COST_PERCENT = 20.0;
    /** One companion per player. */
    public static final int SPIRITS_PER_PLAYER = 1;
    /** Server defaults of the caps across the whole server (the config may change them): 48 companions, 6 full forms. */
    public static final int DEFAULT_MAX_SPIRITS = 48, DEFAULT_MAX_FULL = 6;
    /** At least this many ticks between two answers to one player's key packets. */
    public static final int PACKET_GAP = 10;
    /** The same message to the same player is not repeated within this many ticks. */
    public static final int MESSAGE_GAP = 40;
    /** Effects of the Undine reach players within this many blocks only. */
    public static final double FX_RADIUS = 48.0;
    /** The most the emitter may sit away from the caster's eye (so a spell still goes where the caster aims). */
    public static final double EMITTER_MAX_SHIFT = 4.6;
    /** A spell start point within this many blocks of the caster's eye or feet counts as 'the caster's own start' and moves to the emitter. */
    public static final double EMITTER_CAPTURE = 2.2;
    /** Ticks between two position updates of a following prop (the clients place it from the owner every frame; this only keeps the entity near its owner). */
    public static final int NUDGE = 5;

    /** True when {@code now} is at least {@code gap} ticks after {@code last}. */
    public static boolean gapOk(long now, long last, int gap) { return now - last >= gap; }

    /** Does a page of this cost tier call the full form? (the zone and signature pages) */
    public static boolean callsFull(double costPercent) { return costPercent >= FULL_COST_PERCENT; }

    // ================================================================================================ where she is
    /** Where the companion floats: beside the caster's right shoulder, a little behind, hem just above the ground. */
    public static double[] companion(double px, double py, double pz, double bodyYaw) {
        double[] o = new double[3];
        companionTo(o, px, py, pz, bodyYaw);
        return o;
    }

    /** {@link #companion} into a caller's array (the client draws every frame and allocates nothing). */
    public static void companionTo(double[] o, double px, double py, double pz, double bodyYaw) {
        double r = Math.toRadians(bodyYaw), s = Math.sin(r), c = Math.cos(r);
        double right = 1.45, back = 0.55;
        o[0] = px + (-c) * right - (-s) * back;
        o[1] = py + 0.25;
        o[2] = pz + (-s) * right - c * back;
    }

    /** Where the full form stands: behind the caster, her hem on the caster's feet level, facing the way the caster faces. */
    public static double[] full(double px, double py, double pz, double bodyYaw) {
        double[] o = new double[3];
        fullTo(o, px, py, pz, bodyYaw);
        return o;
    }

    public static void fullTo(double[] o, double px, double py, double pz, double bodyYaw) {
        double r = Math.toRadians(bodyYaw), s = Math.sin(r), c = Math.cos(r);
        o[0] = px - (-s) * 2.7;
        o[1] = py;
        o[2] = pz - c * 2.7;
    }

    /** The companion's open palm: in front of her hand height, where a spell leaves her. */
    public static double[] companionPalm(double px, double py, double pz, double bodyYaw) {
        double[] a = companion(px, py, pz, bodyYaw), f = forward(bodyYaw);
        return new double[]{a[0] + f[0] * 0.6, a[1] + 1.1, a[2] + f[1] * 0.6};
    }

    /** The full form's raised hands: between her spread palms at chest height, a little in front of her. */
    public static double[] fullPalms(double px, double py, double pz, double bodyYaw) {
        double[] a = full(px, py, pz, bodyYaw), f = forward(bodyYaw);
        return new double[]{a[0] + f[0] * 1.3, a[1] + 2.0, a[2] + f[1] * 1.3};
    }

    /** The horizontal forward vector (x, z) of a yaw. */
    public static double[] forward(double yawDeg) {
        double r = Math.toRadians(yawDeg);
        return new double[]{-Math.sin(r), Math.cos(r)};
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

    // ================================================================================================ the clips
    /**
     * The clip the painter plays and the seconds into it: the pose's clip (its form's flavour) while the pose is held ({@link UndinePose#ticks()} after its stamp, plus a short tail so a late
     * packet does not cut it), else the idle of the form. {@code sinceTicks} counts from the stamp with the partial tick.
     */
    public static String clip(boolean full, int pose, float sinceTicks) {
        UndinePose p = UndinePose.of(pose);
        if (p == UndinePose.NONE || sinceTicks < 0f || sinceTicks > p.ticks() + POSE_TAIL) return UndinePose.NONE.clip(full);
        return p.clip(full);
    }

    /** Seconds into the clip: a pose clip counts from its stamp, an idle from the age of the prop. */
    public static float clipSeconds(int pose, float sinceTicks, float age) {
        UndinePose p = UndinePose.of(pose);
        boolean held = p != UndinePose.NONE && sinceTicks >= 0f && sinceTicks <= p.ticks() + POSE_TAIL;
        return (held ? sinceTicks : age) / 20f;
    }

    /** Ticks a client keeps showing a pose after the server's hold time (the server clears the pose in the word every few ticks; this only covers the gap). */
    public static final int POSE_TAIL = 8;

    /** The face to draw: the word's face while a pose is held or the face is not calm because of the idle moment; calm otherwise. */
    public static int shownFace(int param, float sinceTicks) {
        UndinePose p = UndinePose.of(pose(param));
        if (p == UndinePose.NONE) return face(param);
        return sinceTicks >= 0f && sinceTicks <= p.ticks() + POSE_TAIL ? face(param) : 0;
    }

    /** The fade (0..1) of a form that forms over {@code in} ticks and fades over the last {@code out} ticks of its life. */
    public static float fade(float life, float maxLife, float in, float out) {
        float f = Math.min(life / in, (maxLife - life) / out);
        return f < 0f ? 0f : Math.min(f, 1f);
    }
}
