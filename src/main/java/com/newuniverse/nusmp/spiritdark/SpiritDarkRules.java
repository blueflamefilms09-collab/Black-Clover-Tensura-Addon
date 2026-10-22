package com.newuniverse.nusmp.spiritdark;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: every number and every small decision of the Spirit of Darkness lives here as pure functions and small pure classes (no game classes), so the life steal caps, the per target status
 * cooldown, the Shadow Step ranges and cooldowns, the prop word and the anchor points are unit tested once and the server code only asks. See docs/magic/spirit_dark.md for the flows these
 * numbers drive. No rule here touches hunger, none is a percentage of the target's maximum health, none kills outright: the spirit heals a small capped amount, adds a short status effect
 * once per target per cooldown and moves its owner a short validated distance.
 *
 * <p>Ticks are game ticks (20 a second), distances are blocks. A Minecraft yaw of 0 faces +z; the forward vector of a yaw is (-sin, 0, cos), the player's right is (-cos, 0, -sin), the left
 * (cos, 0, sin).</p>
 */
public final class SpiritDarkRules {
    private SpiritDarkRules() {}

    // ================================================================================================ shared
    /** The most any one player can ask the spirit's packet of the server in a row (a key press) before it is ignored, in ticks. */
    public static final int PACKET_GAP = 10;
    /** Sounds and effects of the spirit reach players within this many blocks only. */
    public static final double FX_RADIUS = 48.0;
    /** The same message to the same player is not repeated within this many ticks. */
    public static final int MESSAGE_GAP = 40;
    /** The three actions of the key packet. */
    public static final int ACT_LINK = 0, ACT_STEP = 1, ACT_POWER = 2, ACTIONS = 3;

    public static boolean gapOk(long now, long last, int gap) { return now - last >= gap; }

    public static boolean validAction(int action) { return action >= 0 && action < ACTIONS; }

    // ================================================================================================ the prop word
    /**
     * The spirit is one prop kind; its param (an int the server sets and every nearby client reads) is the whole state a client needs: bits 0..3 the form, bit 4 hides the companion while the powered
     * form is out, bits 5..8 the pose ({@link DarkMoodRules.Pose} ordinal), bits 9..11 the face ({@link DarkMoodRules.Face} ordinal), bits 12..31 the game tick (low 20 bits) the pose and face were set on
     * (0 = never set: the resting pose and the base face), so a client can tell how long ago and returns to rest by itself when the pose has run its length.
     */
    public static final int V_COMPANION = 0, V_POWERED = 1, V_MASK = 15;
    public static final int HIDDEN = 1 << 4, POSE_SHIFT = 5, POSE_MASK = 15, FACE_SHIFT = 9, FACE_MASK = 7, STAMP_SHIFT = 12, STAMP_MASK = 0xFFFFF;

    public static int param(int variant, boolean hidden, DarkMoodRules.Pose pose, DarkMoodRules.Face face, long stamp) {
        int st = (int) stamp & STAMP_MASK;
        if (st == 0) st = 1;                                                            // 0 is reserved for "never set"
        return (variant & V_MASK) | (hidden ? HIDDEN : 0) | ((pose.ordinal() & POSE_MASK) << POSE_SHIFT) | ((face.ordinal() & FACE_MASK) << FACE_SHIFT) | (st << STAMP_SHIFT);
    }

    /** The word of a form that has not been given a mood yet. */
    public static int plain(int variant, boolean hidden) { return (variant & V_MASK) | (hidden ? HIDDEN : 0); }

    public static int variant(int param) { return param & V_MASK; }

    public static boolean hidden(int param) { return (param & HIDDEN) != 0; }

    public static DarkMoodRules.Pose pose(int param) { return DarkMoodRules.Pose.of((param >>> POSE_SHIFT) & POSE_MASK); }

    public static DarkMoodRules.Face face(int param) { return DarkMoodRules.Face.of((param >>> FACE_SHIFT) & FACE_MASK); }

    public static int stamp(int param) { return (param >>> STAMP_SHIFT) & STAMP_MASK; }

    /** The same word with another face (the pose and the stamp stay). */
    public static int withFace(int param, DarkMoodRules.Face face) { return (param & ~(FACE_MASK << FACE_SHIFT)) | ((face.ordinal() & FACE_MASK) << FACE_SHIFT); }

    /** The same word with the hidden bit set or cleared. */
    public static int withHidden(int param, boolean hidden) { return hidden ? param | HIDDEN : param & ~HIDDEN; }

    /** Ticks since the stamp (never negative; the stamp is the low 20 bits of the tick, which wraps every ~14 hours, handled), or a huge number when it was never set. */
    public static int since(int param, long now) {
        if (stamp(param) == 0) return Integer.MAX_VALUE;
        return (int) ((now - stamp(param)) & STAMP_MASK);
    }

    // ================================================================================================ caps
    /** One companion and one powered form per player; the server-wide caps are config values (defaults below). */
    public static final int SPIRITS_PER_PLAYER = 1, POWERED_PER_PLAYER = 1, DEFAULT_MAX_SPIRITS = 48, DEFAULT_MAX_POWERED = 6;

    // ================================================================================================ the powered form
    /** The powered form stays this long (15 s) and may come again after this cooldown (60 s) counted from its call. */
    public static final int POWER_TICKS = 300, POWER_COOLDOWN = 1200;

    // ================================================================================================ life steal
    /** The share of the damage a melee strike dealt that comes back as health, and the caps per strike and per second (the powered form's caps in the second pair). */
    public static final float STEAL_FRACTION = 0.12f, STEAL_HIT_CAP = 1.5f, STEAL_SECOND_CAP = 3.0f, STEAL_HIT_CAP_POWERED = 2.0f, STEAL_SECOND_CAP_POWERED = 4.0f;
    public static final int STEAL_WINDOW = 20;

    /** The health one strike of {@code dealt} damage would give back, before the per second cap (never negative, never above the per strike cap). */
    public static float stealOfHit(float dealt, boolean powered) {
        if (!(dealt > 0f) || Float.isInfinite(dealt)) return 0f;
        return Math.min(dealt * STEAL_FRACTION, powered ? STEAL_HIT_CAP_POWERED : STEAL_HIT_CAP);
    }

    /** Per player: the health already given back in the current one-second window. */
    public static final class StealWindow {
        private long windowStart = Long.MIN_VALUE / 4;
        private float given;

        /** The part of {@code wanted} that fits under the per second cap at tick {@code now}; it is counted. */
        public float grant(long now, float wanted, boolean powered) {
            if (!(wanted > 0f)) return 0f;
            if (now - windowStart >= STEAL_WINDOW) { windowStart = now; given = 0f; }
            float cap = powered ? STEAL_SECOND_CAP_POWERED : STEAL_SECOND_CAP;
            float give = Math.max(0f, Math.min(wanted, cap - given));
            given += give;
            return give;
        }

        public float given() { return given; }
    }

    // ================================================================================================ the status effect on a strike
    /** Wither lasts this long (3 s; the powered form adds 1 s), Blindness this long (2 s), a target is marked again only after this cooldown (5 s); at most this many targets are remembered per player. */
    public static final int WITHER_TICKS = 60, WITHER_TICKS_POWERED = 80, BLIND_TICKS = 40, TARGET_COOLDOWN = 100, MAX_TRACKED_TARGETS = 16;
    public static final int FX_NONE = 0, FX_WITHER = 1, FX_BLIND = 2;

    /** Which effect the n-th mark of a player gets: they alternate, Wither first. */
    public static int effectOf(int markIndex) { return (markIndex & 1) == 0 ? FX_WITHER : FX_BLIND; }

    /** Per player: when each recent target was last marked (a bounded list, oldest dropped, expired entries pruned). */
    public static final class TargetLedger {
        private final List<UUID> ids = new ArrayList<>();
        private final List<Long> times = new ArrayList<>();
        private int marks;

        /** May this target be marked now? If yes it is recorded and the mark index (0, 1, 2, ...) of this mark is returned; else -1. */
        public int tryMark(UUID target, long now) {
            if (target == null) return -1;
            for (int i = 0; i < ids.size(); i++) {
                if (ids.get(i).equals(target)) {
                    if (now - times.get(i) < TARGET_COOLDOWN) return -1;
                    times.set(i, now);
                    return marks++;
                }
            }
            for (int i = ids.size() - 1; i >= 0; i--) if (now - times.get(i) >= TARGET_COOLDOWN) { ids.remove(i); times.remove(i); }    // expired entries go
            while (ids.size() >= MAX_TRACKED_TARGETS) { ids.remove(0); times.remove(0); }                                             // still full: the oldest goes
            ids.add(target);
            times.add(now);
            return marks++;
        }

        public int size() { return ids.size(); }
    }

    // ================================================================================================ Shadow Step
    /** The furthest and the shortest step (blocks), the cooldown (9 s; 7 s while the powered form is out), the pause before a refused step may be asked again, the light level at or below which a spot counts as dark. */
    public static final double STEP_RANGE_MAX = 14.0, STEP_RANGE_MIN = 3.0;
    public static final int STEP_COOLDOWN = 180, STEP_COOLDOWN_POWERED = 140, STEP_RETRY_GAP = 20, DARK_LIGHT_MAX = 7;

    public static int stepCooldown(boolean powered) { return powered ? STEP_COOLDOWN_POWERED : STEP_COOLDOWN; }

    // ================================================================================================ where the forms hover
    private static double[] axes(double yawDeg) {
        double r = Math.toRadians(yawDeg), s = Math.sin(r), c = Math.cos(r);
        return new double[]{-s, c, -c, -s};                                             // forward x z, right x z
    }

    /** The companion: above the owner's left shoulder, a little behind. */
    public static double[] companion(double px, double py, double pz, double bodyYaw) {
        double[] o = new double[3];
        companionTo(o, px, py, pz, bodyYaw);
        return o;
    }

    /** {@link #companion} into a caller's array (the client draws every frame and allocates nothing). */
    public static void companionTo(double[] o, double px, double py, double pz, double bodyYaw) {
        double r = Math.toRadians(bodyYaw), s = Math.sin(r), c = Math.cos(r);
        o[0] = px + c * 0.62 + (-s) * -0.18;
        o[1] = py + 1.95;
        o[2] = pz + s * 0.62 + c * -0.18;
    }

    /** The powered form: behind the owner and a little to the right, hovering just above the ground, facing the way the owner faces. */
    public static double[] powered(double px, double py, double pz, double bodyYaw) {
        double[] o = new double[3];
        poweredTo(o, px, py, pz, bodyYaw);
        return o;
    }

    public static void poweredTo(double[] o, double px, double py, double pz, double bodyYaw) {
        double r = Math.toRadians(bodyYaw), s = Math.sin(r), c = Math.cos(r);
        o[0] = px - (-s) * 1.9 + (-c) * 0.7;
        o[1] = py + 0.45;
        o[2] = pz - c * 1.9 + (-s) * 0.7;
    }

    /** The point on a form's quads the other numbers measure from (used by the self-test): the forms never sit farther from their owner than this. */
    public static final double MAX_ANCHOR_DISTANCE = 3.2;

    public static double anchorDistance(double[] anchor, double px, double py, double pz) {
        double dx = anchor[0] - px, dy = anchor[1] - py, dz = anchor[2] - pz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    static double[] forwardRight(double yaw) { return axes(yaw); }
}
