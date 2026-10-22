package com.newuniverse.nusmp.sylph;

/**
 * Why: every number and every small decision of the Sylph (docs/magic/sylph.md) lives here as pure functions (no game classes), so the forms, the caps, the cooldown, the emitter points and
 * the sound budget are unit tested once and the server code only asks. The Sylph is Yuno's wind Spirit Lord given a small body: a fairy girl that floats beside the caster's head while the link
 * is on (the SMALL form) and, for a short while after a big wind page, a bigger POWERED form that hangs above and behind the caster. Every wind page of the caster's grimoire leaves her hands.
 *
 * <p>Ticks are game ticks (20 a second), distances are blocks, angles degrees. A Minecraft yaw of 0 faces +z, the forward vector of a yaw is (-sin, 0, cos) and the player's right is (-cos, 0, -sin).</p>
 */
public final class SylphRules {
    private SylphRules() {}

    // ================================================================================================ the prop word
    /** The Sylph is a prop entity; its variant is the low four bits of the prop param. */
    public static final int V_SMALL = 0, V_POWERED = 1, V_MASK = 15;
    /** Param bit: the small form is hidden (the powered form is out). Bits 12 and up carry the tick of the last cast (the cast clip plays for a moment after it). */
    public static final int HIDDEN = 1 << 8, EXTRA_SHIFT = 12;
    /** The cast clip plays this many ticks after the stamped cast tick. */
    public static final int CAST_CLIP_TICKS = 24;

    public static int param(int variant, int extra) { return (variant & V_MASK) | (extra << EXTRA_SHIFT); }

    public static int variant(int param) { return param & V_MASK; }

    public static int extra(int param) { return param >>> EXTRA_SHIFT; }

    public static boolean hidden(int param) { return (param & HIDDEN) != 0; }

    /** The cast stamp for a game time: the low 20 bits, never 0 (0 means "no cast yet"). */
    public static int stamp(long gameTime) {
        int s = (int) (gameTime & 0xFFFFF);
        return s == 0 ? 1 : s;
    }

    /** Ticks since a stamped cast (wraps correctly over the 20 bits), or {@link Integer#MAX_VALUE} when there was none. */
    public static int sinceStamp(int stamp, long gameTime) { return stamp == 0 ? Integer.MAX_VALUE : (int) ((gameTime - stamp) & 0xFFFFF); }

    // ================================================================================================ rules of the link
    /** At least this many ticks between two asks of the link key (a packet gap), validated on the server. */
    public static final int PACKET_GAP = 10;
    /** One small Sylph and one powered form per player. */
    public static final int SPIRITS_PER_PLAYER = 1, POWERED_PER_PLAYER = 1;
    /** The default server-wide caps (server config): 48 small Sylphs, 6 powered forms. */
    public static final int DEFAULT_MAX_SPIRITS = 48, DEFAULT_MAX_POWERED = 6;
    /** The powered form: how long it stays, how long before it may come again, the cost tier of the pages that call it, and how long it stays for the Spirit of Zephyr. */
    public static final int POWERED_TICKS = 80, POWERED_ZEPHYR_TICKS = 200, POWERED_COOLDOWN = 300, POWERED_CAST_TICK = 6;
    public static final double POWERED_COST_PERCENT = 20.0;
    /** Effects and sounds of a Sylph reach players within this many blocks only. */
    public static final double FX_RADIUS = 48.0, SOUND_RADIUS = 32.0;
    /** How long the powered form lasts for a page. */
    public static int poweredTicks(boolean zephyr) { return zephyr ? POWERED_ZEPHYR_TICKS : POWERED_TICKS; }

    /** Does a page of this cost tier call the powered form (the zone and signature pages)? */
    public static boolean callsPowered(double costPercent) { return costPercent >= POWERED_COST_PERCENT; }

    /** True when {@code now} is at least {@code gap} ticks after {@code last}. */
    public static boolean gapOk(long now, long last, int gap) { return now - last >= gap; }

    // ================================================================================================ where she is
    /** The hit boxes (width, height) of the props, in blocks: the small fairy is a little more than half a block tall, the powered form about a block and a half. */
    public static final float SMALL_W = 0.4f, SMALL_H = 0.6f, POWERED_W = 0.9f, POWERED_H = 1.7f;

    private static double[] axes(double yawDeg) {
        double r = Math.toRadians(yawDeg), s = Math.sin(r), c = Math.cos(r);
        return new double[]{-s, c, -c, -s};                                             // forward x z, right x z
    }

    /** Where the small Sylph floats: beside the caster's right shoulder, level with the head, a little forward (so she is seen next to the face). {@code bodyYaw} is the caster's body yaw. */
    public static double[] small(double px, double py, double pz, double bodyYaw) {
        double[] o = new double[3];
        smallTo(o, px, py, pz, bodyYaw);
        return o;
    }

    /** {@link #small} into a caller's array (the client draws every frame and allocates nothing). */
    public static void smallTo(double[] o, double px, double py, double pz, double bodyYaw) {
        double r = Math.toRadians(bodyYaw), s = Math.sin(r), c = Math.cos(r);
        o[0] = px + (-c) * 0.70 + (-s) * 0.20;
        o[1] = py + 1.95;
        o[2] = pz + (-s) * 0.70 + c * 0.20;
    }

    /** The small Sylph's hand: out in front of her when she casts. */
    public static double[] smallHand(double px, double py, double pz, double bodyYaw) {
        double[] s = small(px, py, pz, bodyYaw), a = axes(bodyYaw);
        return new double[]{s[0] + a[0] * 0.30, s[1] - 0.02, s[2] + a[1] * 0.30};
    }

    /** Where the powered form hangs: above and behind the caster, facing the way the caster faces. */
    public static double[] powered(double px, double py, double pz, double bodyYaw) {
        double[] o = new double[3];
        poweredTo(o, px, py, pz, bodyYaw);
        return o;
    }

    /** {@link #powered} into a caller's array. */
    public static void poweredTo(double[] o, double px, double py, double pz, double bodyYaw) {
        double r = Math.toRadians(bodyYaw), s = Math.sin(r), c = Math.cos(r);
        o[0] = px - (-s) * 1.5;
        o[1] = py + 2.9;
        o[2] = pz - c * 1.5;
    }

    /** The powered form's hands: held out in front of her body at her chest height, as she casts. */
    public static double[] poweredHand(double px, double py, double pz, double bodyYaw) {
        double[] d = powered(px, py, pz, bodyYaw), a = axes(bodyYaw);
        return new double[]{d[0] + a[0] * 0.95, d[1] + 0.15, d[2] + a[1] * 0.95};
    }

    // ================================================================================================ the emitter
    /** The most the emitter may sit away from the caster's eye (so a spell still goes where the caster aims). */
    public static final double EMITTER_MAX_SHIFT = 4.6;
    /** A spell start point within this many blocks of the caster's eye or feet counts as "the caster's own start" and moves to the emitter. */
    public static final double EMITTER_CAPTURE = 2.2;

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

    // ================================================================================================ the clip of the moment
    /** The clip names of the small form's animation file (animations/entity/sylph_small.animation.json) and the powered form's. */
    public static final String[] SMALL_CLIPS = {"idle", "laugh", "point", "cast", "fly"};
    public static final String[] POWERED_CLIPS = {"hover", "powered_cast", "shout"};

    /**
     * The clip the small form plays: the cast clip for {@link #CAST_CLIP_TICKS} after a cast, the fly clip while the owner moves, otherwise idle, with a laugh now and then (a few seconds of
     * every half minute) and, rarer, the pointing pose of the second picture. {@code seed} makes two Sylphs differ, {@code ticks} is a client clock.
     */
    public static String smallClip(int sinceCast, boolean moving, long ticks, int seed) {
        if (sinceCast < CAST_CLIP_TICKS) return "cast";
        if (moving) return "fly";
        long t = Math.floorMod(ticks + (seed & 0x3FF), 600L);
        if (t < 80) return "laugh";
        if (t >= 300 && t < 360) return "point";
        return "idle";
    }

    /** The clip the powered form plays: the cast clip for a moment after a page, otherwise the hover. */
    public static String poweredClip(int sinceCast, int age) {
        if (sinceCast < 30) return "powered_cast";
        return age < 16 ? "shout" : "hover";
    }
}
