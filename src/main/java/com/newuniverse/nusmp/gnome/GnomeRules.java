package com.newuniverse.nusmp.gnome;

/**
 * Why: every number and every small decision of the Gnome (the Earth Spirit of Black Clover, docs/magic/gnome.md) lives here as pure functions (no game classes), so the caps, the timings, the
 * emitter points and the clip choice are unit tested once and the server and the painter only ask. The Gnome is a lightweight prop (one {@code MagicPropEntity} of kind GNOME_1) in two forms: the
 * COMPANION (about 1.5 blocks, walks beside its master) and the FULL form (a stone giant of about 6 blocks that rises out of the ground for the big earth pages, for a short time, with a cooldown).
 *
 * <p>Ticks are game ticks (20 a second), distances are blocks, angles degrees. A Minecraft yaw of 0 faces +z, the forward vector of a yaw is (-sin, 0, cos) and the right is (-cos, 0, -sin).
 * The prop word ({@code param}) is: the low four bits the variant, bit 8 HIDDEN, the bits from 12 the tick stamp of the last cast.</p>
 */
public final class GnomeRules {
    private GnomeRules() {}

    // ================================================================================================ shared
    /** At most one key ask per this many ticks is answered; the same message is not repeated within {@link #MESSAGE_GAP}. */
    public static final int PACKET_GAP = 10, MESSAGE_GAP = 40;
    /** Effects and sounds of the Gnome reach players within this many blocks only. */
    public static final double FX_RADIUS = 48.0;

    public static boolean gapOk(long now, long last, int gap) { return now - last >= gap; }

    // ================================================================================================ forms, caps
    public static final int V_COMPANION = 0, V_FULL = 1, V_MASK = 15;
    /**
     * The prop word: bits 0..3 the variant; bit 8 HIDDEN (the companion while the giant stands); bits 9..12 the POSE (a clip id, 0 = none); bits 13..15 the FACE (an expression); bits 16..31 the tick stamp
     * of the event that set the pose and the face (16 bits of game time: 54 minutes, far longer than any clip or face lasts and than a companion's life).
     */
    public static final int HIDDEN = 1 << 8, POSE_SHIFT = 9, POSE_MASK = 15, FACE_SHIFT = 13, FACE_MASK = 7, STAMP_SHIFT = 16, STAMP_MASK = 0xFFFF;
    /** One companion and one full form per player. The server-wide caps are config values (defaults below). */
    public static final int SPIRITS_PER_PLAYER = 1, FULL_PER_PLAYER = 1, DEFAULT_MAX_SPIRITS = 48, DEFAULT_MAX_FULL = 6;
    /** The full form: how long it stays (rise, hold, sink) and how long before it may come again; the cost tier of the pages that call it. */
    public static final int FULL_TICKS = 120, FULL_RISE_TICKS = 20, FULL_SINK_TICKS = 20, FULL_SLAM_TICK = 14, DEFAULT_FULL_COOLDOWN = 600;
    public static final double FULL_COST_PERCENT = 20.0;
    /** The cast clip of both forms: 24 ticks, the fists land on tick 12. */
    public static final int CAST_CLIP_TICKS = 24, CAST_CONTACT_TICK = 12;
    /** The punch clip (a page below the big tiers: one fist drives forward, the spell leaves it): 20 ticks, the fist lands on tick 10. The stomp clip (the giant lifts a foot and slams it now and then): 18 ticks, the foot lands on tick 10. */
    public static final int PUNCH_CLIP_TICKS = 20, PUNCH_CONTACT_TICK = 10, STOMP_CLIP_TICKS = 18, STOMP_CONTACT_TICK = 10, STOMP_AT = 62;
    /** The one-fist slam (a mid page): 22 ticks, the fist lands on tick 10. The shrug (a hit rolls off the stone): 22 ticks. The victory chest-pound: 36 ticks. The kneel and rise from the earth: 40 ticks. */
    public static final int SLAM_CLIP_TICKS = 22, SLAM_CONTACT_TICK = 10, SHRUG_CLIP_TICKS = 22, POUND_CLIP_TICKS = 36, KNEEL_CLIP_TICKS = 40;
    /** Ticks between two position nudges of the companion prop (the clients place it from the owner every frame). */
    public static final int NUDGE = 5;
    /** Hit boxes (width, height) of the two forms: they only keep the entity inside the clients' tracking range and out of the way. */
    public static final float COMPANION_W = 0.9f, COMPANION_H = 1.6f, FULL_W = 3.6f, FULL_H = 7.0f;

    public static int param(int variant, int stamp) { return word(variant, false, CLIP_IDLE, FACE_CALM, stamp); }

    /** The whole prop word. */
    public static int word(int variant, boolean hidden, int pose, int face, int stamp) {
        return (variant & V_MASK) | (hidden ? HIDDEN : 0) | ((pose & POSE_MASK) << POSE_SHIFT) | ((face & FACE_MASK) << FACE_SHIFT) | ((stamp & STAMP_MASK) << STAMP_SHIFT);
    }

    public static int variant(int param) { return param & V_MASK; }

    /** The tick stamp of the last event (a game tick modulo 2^16, 0 = none). */
    public static int stamp(int param) { return (param >>> STAMP_SHIFT) & STAMP_MASK; }

    /** The pose (a clip id) the last event asked for; 0 = none (the clients choose walk or idle). */
    public static int pose(int param) { return (param >>> POSE_SHIFT) & POSE_MASK; }

    /** The expression the last event asked for. */
    public static int face(int param) { return (param >>> FACE_SHIFT) & FACE_MASK; }

    public static boolean hidden(int param) { return (param & HIDDEN) != 0; }

    /** The word with another hidden bit and nothing else changed. */
    public static int withHidden(int param, boolean hidden) { return hidden ? param | HIDDEN : param & ~HIDDEN; }

    /** The word with a new pose, face and stamp, the variant and the hidden bit kept. */
    public static int withPose(int param, int pose, int face, int stamp) { return word(variant(param), hidden(param), pose, face, stamp); }

    /** The stamp an event at game time {@code now} writes (never 0, which means "no event"). */
    public static int stampOf(long now) { int s = (int) (now & STAMP_MASK); return s == 0 ? 1 : s; }

    /** Ticks since the stamp, or {@link Integer#MAX_VALUE} for none; wraps correctly over the 16 bit clock. */
    public static int sinceStamp(long now, int stamp) { return stamp == 0 ? Integer.MAX_VALUE : (int) ((now - stamp) & STAMP_MASK); }

    /** Does a page of this cost tier (percent of the mana pool) call the full form? */
    public static boolean callsFull(double costPercent) { return costPercent >= FULL_COST_PERCENT; }

    /** May the full form come now: its cooldown is over and a slot of both ledgers is free. */
    public static boolean fullAllowed(long now, long readyAt, int mine, int total, int serverMax) {
        return now >= readyAt && mine < FULL_PER_PLAYER && total < serverMax;
    }

    // ================================================================================================ what clip, when
    public static final int CLIP_IDLE = 0, CLIP_WALK = 1, CLIP_RISE = 2, CLIP_CAST = 3, CLIP_SINK = 4, CLIP_PUNCH = 5, CLIP_STOMP = 6, CLIP_SLAM = 7, CLIP_SHRUG = 8, CLIP_GUARD = 9, CLIP_POUND = 10,
            CLIP_DOZE = 11, CLIP_KNEEL = 12;
    /** The faces of the Gnome (a golem: its eye and mouth plates and the strength of its glow change). */
    public static final int FACE_CALM = 0, FACE_AMUSED = 1, FACE_ANGRY = 2, FACE_SLEEPY = 3, FACE_SURPRISED = 4, FACE_STERN = 5, FACES = 6;
    /** How long a face is held after the event that set it (the calm face never expires; a doze and a guard keep their faces while they last). */
    public static final int FACE_TICKS = 100;

    /** The length in ticks of a one-shot pose clip; 0 for the poses that loop until the server clears them (the guard and the doze) and for the plain clips. */
    public static int poseTicks(int clip) {
        return switch (clip) {
            case CLIP_PUNCH -> PUNCH_CLIP_TICKS;
            case CLIP_CAST -> CAST_CLIP_TICKS;
            case CLIP_SLAM -> SLAM_CLIP_TICKS;
            case CLIP_STOMP -> STOMP_CLIP_TICKS;
            case CLIP_SHRUG -> SHRUG_CLIP_TICKS;
            case CLIP_POUND -> POUND_CLIP_TICKS;
            case CLIP_KNEEL -> KNEEL_CLIP_TICKS;
            case CLIP_SINK -> FULL_SINK_TICKS;
            default -> 0;
        };
    }

    /** The poses that hold until the server sets another (the arms-crossed guard and the dozing settle). */
    public static boolean persistent(int clip) { return clip == CLIP_GUARD || clip == CLIP_DOZE; }

    /** Is a pose set {@code since} ticks ago still playing? (A persistent pose always is; a one-shot for its clip's length.) */
    public static boolean poseActive(int pose, int since) {
        if (pose == CLIP_IDLE) return false;
        return persistent(pose) || (since >= 0 && since < poseTicks(pose));
    }

    /** The tick of a clip at which a fist or a foot lands (the server's effect and sound, the client's dust), or -1 for a clip with no landing. */
    public static int contactTick(int clip) {
        return switch (clip) {
            case CLIP_CAST -> CAST_CONTACT_TICK;
            case CLIP_PUNCH -> PUNCH_CONTACT_TICK;
            case CLIP_SLAM -> SLAM_CONTACT_TICK;
            case CLIP_STOMP -> STOMP_CONTACT_TICK;
            case CLIP_RISE -> FULL_SLAM_TICK;
            default -> -1;
        };
    }

    /** The expression drawn now: the one the last event set while it lasts, calm after; a doze is always sleepy and a guard always at least stern. */
    public static int faceNow(int face, int pose, int since) {
        if (pose == CLIP_DOZE) return FACE_SLEEPY;
        if (pose == CLIP_GUARD) return face == FACE_CALM ? FACE_STERN : face;
        return since >= 0 && since < FACE_TICKS ? face : FACE_CALM;
    }

    /**
     * The clip of the FULL form at an age: rise for the first {@link #FULL_RISE_TICKS}, sink for the last {@link #FULL_SINK_TICKS}, a pose the server asked for while it plays (cast, punch, slam,
     * shrug, guard, pound), one stomp at {@link #STOMP_AT}, idle otherwise.
     */
    public static int fullClip(int life, int maxLife, int pose, int since) {
        if (life < FULL_RISE_TICKS) return CLIP_RISE;
        if (maxLife - life <= FULL_SINK_TICKS) return CLIP_SINK;
        if (poseActive(pose, since) && pose != CLIP_KNEEL && pose != CLIP_DOZE) return pose;
        return life >= STOMP_AT && life < STOMP_AT + STOMP_CLIP_TICKS ? CLIP_STOMP : CLIP_IDLE;
    }

    /** The clip of the COMPANION: a pose the server asked for while it plays, walk while the master moves (a doze ends when it moves), idle otherwise. */
    public static int companionClip(boolean moving, int pose, int since) {
        if (poseActive(pose, since) && !(moving && pose == CLIP_DOZE)) return pose;
        return moving ? CLIP_WALK : CLIP_IDLE;
    }

    /** Seconds into a one-shot clip that started {@code ticks} ago with {@code partial} of a tick on top. */
    public static float secs(int ticks, float partial) { return (ticks + partial) / 20f; }

    /** The fade of the full form while it rises out of the ground and sinks back (0..1): it is solid from the first tick (it breaks the ground), fading only in its last ticks. */
    public static float fullFade(int life, int maxLife, float partial) {
        float out = (maxLife - life - partial) / 6f;
        return Math.max(0f, Math.min(1f, out));
    }

    /** Is this the tick at which a full form's clip lands a fist or a foot (the painter sends its dust then)? */
    public static boolean slamNow(int clip, int ticksIntoClip) {
        int c = contactTick(clip);
        return c >= 0 && c == ticksIntoClip;
    }

    // ================================================================================================ where it stands
    private static double[] axes(double yawDeg) {
        double r = Math.toRadians(yawDeg), s = Math.sin(r), c = Math.cos(r);
        return new double[]{-s, c, -c, -s};                                             // forward x z, right x z
    }

    /** The companion floats beside its master: to the right, a little behind, just above the ground. */
    public static double[] companion(double px, double py, double pz, double bodyYaw) {
        double[] o = new double[3];
        companionTo(o, px, py, pz, bodyYaw);
        return o;
    }

    /** {@link #companion} into a caller's array (the client draws every frame and allocates nothing). */
    public static void companionTo(double[] o, double px, double py, double pz, double bodyYaw) {
        double r = Math.toRadians(bodyYaw), s = Math.sin(r), c = Math.cos(r);
        o[0] = px + (-c) * 1.15 + (-s) * -0.35;
        o[1] = py + 0.10;
        o[2] = pz + (-s) * 1.15 + c * -0.35;
    }

    /** The companion's raised fist (where a spell leaves it): in front of it, at chest height. */
    public static double[] companionHand(double px, double py, double pz, double bodyYaw) {
        double[] c = companion(px, py, pz, bodyYaw), a = axes(bodyYaw);
        return new double[]{c[0] + a[0] * 0.40, c[1] + 0.95, c[2] + a[1] * 0.40};
    }

    /** Where the full form rises: in front of its master, on the ground, facing the way the master faces. */
    public static double[] full(double px, double py, double pz, double bodyYaw) {
        double[] a = axes(bodyYaw);
        return new double[]{px + a[0] * 4.8, py, pz + a[1] * 4.8};
    }

    /** The full form's fist (where a spell leaves it): out in front of it, low, between its two fists. */
    public static double[] fullHand(double fx, double fy, double fz, double yaw) {
        double[] a = axes(yaw);
        return new double[]{fx + a[0] * 2.4, fy + 1.3, fz + a[1] * 2.4};
    }

    // ================================================================================================ the emitter
    /** The most the emitter may sit away from the caster's eye (so a spell still goes where the caster aims). */
    public static final double EMITTER_MAX_SHIFT = 7.0;
    /** A spell start point within this many blocks of the caster's eye or feet counts as "the caster's own start" and moves to the emitter. */
    public static final double EMITTER_CAPTURE = 2.2;

    /**
     * The emitter a spell start is moved to: the start moves only when it was the caster's own (within {@link #EMITTER_CAPTURE} of the eye or the feet), and the emitter is kept within
     * {@link #EMITTER_MAX_SHIFT} of the eye. Returns the point to use (the start itself when it is not captured).
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

    // ================================================================================================ lore
    /** The canon grimoire ids that are the Gnome's host in the series: Feremena (spelled Feramina by the owner), a Shining General of the Diamond Kingdom. No such CanonBook exists in the mod yet. */
    public static boolean isHostCanon(String canonId) {
        if (canonId == null) return false;
        String c = canonId.toLowerCase(java.util.Locale.ROOT);
        return c.equals("feremena") || c.equals("feramina");
    }
}
