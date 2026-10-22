package com.newuniverse.nusmp.spiritlight;

import java.util.Arrays;

/**
 * Why: every number and every small decision of the Spirit of Light (docs/magic/spirit_light.md) lives here as pure functions (no game classes), so the caps, the beam and the glow pulse
 * rules, the prop words and the anchor points are unit tested once and the server code only asks. The Spirit of Light is a holy spirit of the Light Magic caster: a glowing lion cub that
 * floats beside the owner's shoulder while linked, and a miniature seraph (the powered form) that rises behind the owner for a short, cooldown-bound time.
 *
 * <p>Ticks are game ticks (20 a second), distances are blocks, angles degrees. A Minecraft yaw of 0 faces +z; the forward vector of a yaw is (-sin, 0, cos) and the owner's right is (-cos, 0, -sin).
 * Every damage number is a RAW light damage that still goes through the Light Magic path ({@code LightBook.lightHit}, the balance law and its caps): nothing here lifts a cap, nothing touches
 * the food bar, nothing is a percentage of a target's health and nothing kills outright.</p>
 */
public final class SpiritLightRules {
    private SpiritLightRules() {}

    // ================================================================================================ shared
    /** At most one key packet of one player is answered per this many ticks. */
    public static final int PACKET_GAP = 6;
    /** The same message to the same player is not repeated within this many ticks. */
    public static final int MESSAGE_GAP = 40;
    /** Effects and sounds of the spirit reach players within this many blocks only. */
    public static final double FX_RADIUS = 48.0;
    /** The key actions the client may send (anything else is ignored): link toggle, beam, glow pulse, powered form toggle. */
    public static final int ACT_LINK = 0, ACT_BEAM = 1, ACT_PULSE = 2, ACT_POWER = 3, ACTIONS = 4;

    /** True when {@code now} is at least {@code gap} ticks after {@code last}. */
    public static boolean gapOk(long now, long last, int gap) { return now - last >= gap; }

    public static boolean validAction(int action) { return action >= 0 && action < ACTIONS; }

    // ================================================================================================ the prop word
    /** The variant is the low four bits of the prop param: 0 cub, 1 seraph (prop kind SPIRIT_LIGHT_1), 2 beam, 3 pulse ring (prop kind SPIRIT_LIGHT_2). */
    public static final int V_CUB = 0, V_SERAPH = 1, V_BEAM = 2, V_PULSE = 3, V_MASK = 15;
    /** Param bits: the cub is hidden while the seraph is out; the powered flag of a beam or a pulse. */
    public static final int HIDDEN = 1 << 8, POWERED = 1 << 4;
    /** The game time (low 20 bits) the pose began sits in the bits from here up in a form's param: the client plays the pose's clip from that moment. */
    public static final int STAMP_SHIFT = 12, STAMP_MASK = 0xFFFFF;

    public static int variant(int param) { return param & V_MASK; }

    public static boolean hidden(int param) { return (param & HIDDEN) != 0; }

    public static boolean powered(int param) { return (param & POWERED) != 0; }

    /** The pose of a form (0..15, see {@link LightMoodRules}) lives in bits 4..7 of a form's param (the powered flag of a beam is the same bit of another variant). */
    public static final int POSE_SHIFT = 4, POSE_MASK = 0xF;

    /** A cub's or a seraph's param: the variant, the hidden bit (cub), the pose and the tick it began (0 = never). */
    public static int formParam(int variant, boolean hidden, int pose, long poseTick) {
        int stamp = poseTick <= 0 ? 0 : (int) (poseTick & STAMP_MASK);
        if (poseTick > 0 && stamp == 0) stamp = 1;
        return (variant & V_MASK) | ((pose & POSE_MASK) << POSE_SHIFT) | (hidden ? HIDDEN : 0) | (stamp << STAMP_SHIFT);
    }

    /** The pose a form's param holds. */
    public static int pose(int param) { return (param >> POSE_SHIFT) & POSE_MASK; }

    public static int stamp(int param) { return (param >>> STAMP_SHIFT) & STAMP_MASK; }

    /** Ticks since the cast the param remembers, or {@link Integer#MAX_VALUE} when it was never cast (the 20 bit stamp wraps after about 14 hours; an old cast then shows once as a clip of 24 ticks). */
    public static int sinceCast(int param, long now) {
        int s = stamp(param);
        return s == 0 ? Integer.MAX_VALUE : (int) ((now - s) & STAMP_MASK);
    }

    /** A beam's param: powered flag, the foes it hit (0..7), the pitch (degrees, -90..90 stored as 0..180) and the length in half blocks (0..255). */
    public static int beamParam(boolean powered, int hits, double pitchDeg, double lengthBlocks) {
        int p = (int) Math.round(Math.max(-90.0, Math.min(90.0, pitchDeg))) + 90;
        int len = (int) Math.round(Math.max(0.0, Math.min(127.0, lengthBlocks)) * 2.0);
        return V_BEAM | (powered ? POWERED : 0) | (Math.max(0, Math.min(7, hits)) << 5) | (p << 8) | (len << 16);
    }

    public static int beamHits(int param) { return (param >> 5) & 7; }

    public static double beamPitch(int param) { return ((param >> 8) & 0xFF) - 90.0; }

    public static double beamLength(int param) { return ((param >> 16) & 0xFF) / 2.0; }

    /** A pulse ring's param: powered flag and the radius in whole blocks (0..63). */
    public static int pulseParam(boolean powered, double radius) {
        return V_PULSE | (powered ? POWERED : 0) | (Math.max(0, Math.min(63, (int) Math.round(radius))) << 8);
    }

    public static double pulseRadius(int param) { return (param >> 8) & 63; }

    // ================================================================================================ the link and Swiftness III
    /** One spirit per player; the server-wide caps are server config values. */
    public static final int SPIRITS_PER_PLAYER = 1;
    public static final int DEFAULT_MAX_SPIRITS = 48, DEFAULT_MAX_POWERED = 6, DEFAULT_MAX_EFFECT_PROPS = 24;
    /** Swiftness III (amplifier 2) while linked: a short effect refreshed by the upkeep, so it is gone about three seconds after the link ends even if every cleanup hook were missed. */
    public static final int SWIFT_AMPLIFIER = 2, SWIFT_TICKS = 60, SWIFT_REFRESH_BELOW = 40;

    /** Does the owner's speed effect need a refresh? (none, or less than {@link #SWIFT_REFRESH_BELOW} ticks left). */
    public static boolean swiftnessDue(boolean has, int remainingTicks) { return !has || remainingTicks < SWIFT_REFRESH_BELOW; }

    /** Is a speed effect instance the spirit's own (so the cleanup removes it and never someone else's potion or beacon)? Ambient, no particles, amplifier III, at most the spirit's own duration. */
    public static boolean ownSwiftness(boolean ambient, boolean visible, int amplifier, int durationTicks) {
        return ambient && !visible && amplifier == SWIFT_AMPLIFIER && durationTicks <= SWIFT_TICKS;
    }

    // ================================================================================================ the powered form (the seraph)
    /** How long the seraph stays (60 seconds), how long before it may rise again, and how many one player may have. */
    public static final int POWER_TICKS = 1200, POWER_COOLDOWN = 600, POWERED_PER_PLAYER = 1;
    /** The most any one form lives (cub: while linked; seraph: its time plus a grace). */
    public static final int FORM_LIFE = 24000, SERAPH_LIFE = POWER_TICKS + 40;

    public static boolean powerReady(long now, long readyAt) { return now >= readyAt; }

    // ================================================================================================ the piercing light beam
    /** Raw light damage of one hit, how many foes one beam pierces, how far it reaches, ticks between two beams: the cub's first, the seraph's second. Each has a hard cap that no config lifts. */
    public static final float BEAM_RAW_CAP = 5.0f;
    public static final int MAX_PIERCE = 5, MIN_BEAM_COOLDOWN = 16;
    public static final double MAX_RANGE = 32.0;
    private static final float[] BEAM_RAW = {3.0f, 4.5f};
    private static final int[] BEAM_PIERCE = {2, 4}, BEAM_COOLDOWN = {30, 24};
    private static final double[] BEAM_RANGE = {22.0, 30.0};
    /** The beam's hit half width around its line (blocks), and how long a beam prop lives. */
    public static final double BEAM_WIDTH = 0.55;
    public static final int BEAM_LIFE = 6;
    /** Alive effect props (beams and pulse rings) of one player at most; the server-wide cap is a config value. */
    public static final int EFFECT_PROPS_PER_PLAYER = 3;
    /** The most entities one beam looks at (the box of the segment is searched once per shot). */
    public static final int MAX_BEAM_CANDIDATES = 256;

    private static int form(boolean powered) { return powered ? 1 : 0; }

    /** The raw damage of one beam hit; {@code scale} is the server's damage scale (0..1, never above 1: the cap stays). */
    public static float beamRaw(boolean powered, double scale) { return (float) Math.min(BEAM_RAW_CAP, BEAM_RAW[form(powered)] * Math.max(0.0, Math.min(1.0, scale))); }

    public static int beamPierce(boolean powered) { return Math.min(MAX_PIERCE, BEAM_PIERCE[form(powered)]); }

    public static double beamRange(boolean powered) { return Math.min(MAX_RANGE, BEAM_RANGE[form(powered)]); }

    public static int beamCooldown(boolean powered) { return Math.max(MIN_BEAM_COOLDOWN, BEAM_COOLDOWN[form(powered)]); }

    /**
     * The foes a beam hits: from candidates given as the distance along the beam (blocks, any order; a negative value is a miss) the {@code max} nearest, nearest first. Returns their indices: at most
     * {@code max}, and at most {@link #MAX_BEAM_CANDIDATES} candidates are read.
     */
    public static int[] pierce(double[] along, int n, int max) {
        return nearest(along, n, Math.min(max, MAX_PIERCE), MAX_BEAM_CANDIDATES);
    }

    /** Where along the segment a-b a point c lies and whether it is within {@code width} of the line: the distance along in blocks, or -1 for a miss. */
    public static double alongHit(double ax, double ay, double az, double bx, double by, double bz, double cx, double cy, double cz, double width) {
        double dx = bx - ax, dy = by - ay, dz = bz - az, len2 = dx * dx + dy * dy + dz * dz;
        if (len2 < 1e-9) return -1.0;
        double t = ((cx - ax) * dx + (cy - ay) * dy + (cz - az) * dz) / len2;
        t = Math.max(0.0, Math.min(1.0, t));
        double px = ax + dx * t - cx, py = ay + dy * t - cy, pz = az + dz * t - cz;
        if (px * px + py * py + pz * pz > width * width) return -1.0;
        return t * Math.sqrt(len2);
    }

    /** The unit direction from the emitter to the aim point (the way the owner looks when the aim point is too close to tell). */
    public static double[] beamDirection(double ex, double ey, double ez, double ax, double ay, double az, double lx, double ly, double lz) {
        double dx = ax - ex, dy = ay - ey, dz = az - ez, n = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (n < 0.5) { dx = lx; dy = ly; dz = lz; n = Math.sqrt(dx * dx + dy * dy + dz * dz); }
        if (n < 1e-9) return new double[]{0, 0, 1};
        return new double[]{dx / n, dy / n, dz / n};
    }

    /** The pitch in degrees (-90 up .. 90 down, Minecraft's sign) of a direction. */
    public static double pitchOf(double dx, double dy, double dz) {
        double h = Math.sqrt(dx * dx + dz * dz);
        return -Math.toDegrees(Math.atan2(dy, h));
    }

    /** The yaw in degrees of a direction (Minecraft's: 0 faces +z). */
    public static double yawOf(double dx, double dz) { return Math.toDegrees(Math.atan2(-dx, dz)); }

    // ================================================================================================ the Glowing pulse
    /** The pulse: radius, how many foes it marks, how long they glow, ticks between two pulses; each with a hard cap. */
    public static final double MAX_PULSE_RADIUS = 24.0;
    public static final int MAX_PULSE_TARGETS = 24, MAX_PULSE_TICKS = 200, MIN_PULSE_COOLDOWN = 200, MAX_PULSE_CANDIDATES = 32, MAX_PULSE_SCAN = 512;
    private static final double[] PULSE_RADIUS = {12.0, 18.0};
    private static final int[] PULSE_TARGETS = {12, 20}, PULSE_TICKS = {100, 140}, PULSE_COOLDOWN = {400, 400};
    /** How long the pulse ring prop lives. */
    public static final int PULSE_LIFE = 16;

    public static double pulseRadius(boolean powered) { return Math.min(MAX_PULSE_RADIUS, PULSE_RADIUS[form(powered)]); }

    public static int pulseTargets(boolean powered) { return Math.min(MAX_PULSE_TARGETS, PULSE_TARGETS[form(powered)]); }

    public static int pulseTicks(boolean powered) { return Math.min(MAX_PULSE_TICKS, PULSE_TICKS[form(powered)]); }

    public static int pulseCooldown(boolean powered) { return Math.max(MIN_PULSE_COOLDOWN, PULSE_COOLDOWN[form(powered)]); }

    /**
     * The first step of a pulse: from the squared distances of every hostile found in the box (at most {@link #MAX_PULSE_SCAN} are read; a value past the radius is left out) the
     * {@link #MAX_PULSE_CANDIDATES} nearest, nearest first. Only these are checked for a wall between them and the owner, so a crowd costs at most that many line of sight tests.
     */
    public static int[] pulseShortlist(double[] distSq, int n, double radius) {
        double r2 = radius * radius;
        n = Math.max(0, Math.min(Math.min(n, distSq.length), MAX_PULSE_SCAN));
        double[] key = new double[n];
        for (int i = 0; i < n; i++) key[i] = distSq[i] < 0 || distSq[i] > r2 ? -1.0 : distSq[i];
        return nearest(key, n, MAX_PULSE_CANDIDATES, MAX_PULSE_SCAN);
    }

    /**
     * The second step: of the shortlist (squared distances and whether a wall hides the foe from the owner) the {@code cap} that are marked: foes the owner cannot see ({@code hidden} true) come
     * first, because those are what the pulse is for, then the nearest; a negative value or one past the radius is left out. Returns indices into the shortlist.
     */
    public static int[] pulsePick(double[] distSq, boolean[] hidden, int n, int cap, double radius) {
        n = Math.max(0, Math.min(Math.min(n, distSq.length), MAX_PULSE_CANDIDATES));
        double[] key = new double[n];
        double r2 = radius * radius;
        for (int i = 0; i < n; i++) {
            double d = distSq[i];
            key[i] = d < 0 || d > r2 ? -1.0 : (hidden != null && i < hidden.length && hidden[i] ? d : d + r2 * 2.0 + 1.0);        // a hidden foe sorts before every visible one
        }
        return nearest(key, n, Math.min(cap, MAX_PULSE_TARGETS), MAX_PULSE_CANDIDATES);
    }

    /** The indices of the {@code max} smallest non negative keys, smallest first (ties keep their order). */
    private static int[] nearest(double[] key, int n, int max, int readCap) {
        n = Math.max(0, Math.min(Math.min(n, key.length), readCap));
        int[] idx = new int[n];
        int c = 0;
        for (int i = 0; i < n; i++) {
            if (key[i] < 0) continue;
            int pos = c;
            while (pos > 0 && key[idx[pos - 1]] > key[i]) { idx[pos] = idx[pos - 1]; pos--; }
            idx[pos] = i;
            c++;
        }
        return Arrays.copyOf(idx, Math.max(0, Math.min(max, c)));
    }

    // ================================================================================================ sounds
    /** Ticks a player's sound of this kind may repeat at the soonest (the sounds go to players nearby only, through the effect packet budget). */
    public static final int S_LINK = 0, S_UNLINK = 1, S_BEAM = 2, S_PULSE = 3, S_POWER = 4, S_PURR = 5, S_MEW = 6, S_CUB_ROAR = 7, S_YAWN = 8, S_CHIRP = 9, S_WHIMPER = 10, SOUND_KINDS = 11;
    /** The event ids of the sound kinds, in order (sounds.json keys; the files are sounds/spirit_light/*.ogg). The last five are the lion cub's creature sounds (no spoken words). */
    public static final String[] SOUND_IDS = {"spirit_light.link", "spirit_light.unlink", "spirit_light.beam", "spirit_light.pulse", "spirit_light.power", "spirit_light.purr",
            "spirit_light.cub_mew", "spirit_light.cub_roar", "spirit_light.cub_yawn", "spirit_light.cub_chirp", "spirit_light.cub_whimper"};
    private static final int[] SOUND_GAPS = {10, 10, 8, 20, 20, 400, 60, 40, 200, 40, 60};

    public static int soundGap(int kind) { return kind < 0 || kind >= SOUND_KINDS ? Integer.MAX_VALUE : SOUND_GAPS[kind]; }

    // ================================================================================================ anchor points (the server and the painter use the same)
    private static double[] axes(double yawDeg) {
        double r = Math.toRadians(yawDeg), s = Math.sin(r), c = Math.cos(r);
        return new double[]{-s, c, -c, -s};                                             // forward x z, right x z
    }

    /** Where the cub floats: beside the right shoulder, a little behind, at the head height of the owner (a body at (px, py, pz) turned {@code bodyYaw} degrees). */
    public static double[] companion(double px, double py, double pz, double bodyYaw) {
        double[] o = new double[3];
        companionTo(o, px, py, pz, bodyYaw);
        return o;
    }

    /** {@link #companion} into a caller's array (the client draws every frame and allocates nothing). */
    public static void companionTo(double[] o, double px, double py, double pz, double bodyYaw) {
        double r = Math.toRadians(bodyYaw), s = Math.sin(r), c = Math.cos(r);
        o[0] = px + (-c) * 0.66 + (-s) * -0.22;
        o[1] = py + 1.34;
        o[2] = pz + (-s) * 0.66 + c * -0.22;
    }

    /** The cub's mouth: in front of its sitting place, a little up (where the beam starts for the companion form). */
    public static double[] companionMouth(double px, double py, double pz, double bodyYaw) {
        double[] s = companion(px, py, pz, bodyYaw), a = axes(bodyYaw);
        return new double[]{s[0] + a[0] * 0.28, s[1] + 0.36, s[2] + a[1] * 0.28};
    }

    /** Where the seraph stands: behind the owner and a little above, facing the way the owner faces. */
    public static double[] seraph(double px, double py, double pz, double bodyYaw) {
        double[] o = new double[3];
        seraphTo(o, px, py, pz, bodyYaw);
        return o;
    }

    /** {@link #seraph} into a caller's array. */
    public static void seraphTo(double[] o, double px, double py, double pz, double bodyYaw) {
        double r = Math.toRadians(bodyYaw), s = Math.sin(r), c = Math.cos(r);
        o[0] = px - (-s) * 1.55;
        o[1] = py + 0.35;
        o[2] = pz - c * 1.55;
    }

    /** The seraph's core (its faceless head, where the beam starts for the powered form): high above its feet and a little forward. */
    public static double[] seraphCore(double px, double py, double pz, double bodyYaw) {
        double[] d = seraph(px, py, pz, bodyYaw), a = axes(bodyYaw);
        return new double[]{d[0] + a[0] * 0.15, d[1] + 1.72, d[2] + a[1] * 0.15};
    }

    /** The most the beam's start may sit away from the owner's eye (so a beam still goes where the owner aims). */
    public static final double EMITTER_MAX_SHIFT = 4.2;

    /** The emitter moved to within {@link #EMITTER_MAX_SHIFT} of the eye (a point farther away is pulled back along the line to the eye). */
    public static double[] clampEmitter(double ex, double ey, double ez, double mx, double my, double mz) {
        double dx = mx - ex, dy = my - ey, dz = mz - ez, n = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (n <= EMITTER_MAX_SHIFT || n < 1e-9) return new double[]{mx, my, mz};
        double k = EMITTER_MAX_SHIFT / n;
        return new double[]{ex + dx * k, ey + dy * k, ez + dz * k};
    }
}
