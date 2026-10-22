package com.newuniverse.nusmp.spiritspace;

/**
 * Why: every number and every small decision of the Spirit of Space lives here as pure functions (no game classes), so the caps, the ranges, the channel rule, the projectile selection and the
 * anchor points are unit tested once and the server code only asks. See docs/magic/spirit_space.md for the flows these numbers drive.
 *
 * <p>Ticks are game ticks (20 a second), distances are blocks, angles degrees. A Minecraft yaw of 0 faces +z; the forward vector of a yaw is (-sin, 0, cos) and the right of the body is (-cos, 0, -sin).
 * The hard caps below are the ceilings the server config can never lift past: a config value is always clamped to them.</p>
 */
public final class SpiritSpaceRules {
    private SpiritSpaceRules() {}

    // ================================================================================================ shared
    /** At most one key packet of one player is answered per this many ticks. */
    public static final int PACKET_GAP = 8;
    /** Sounds and effects of the spirit reach players within this many blocks only. */
    public static final double FX_RADIUS = 48.0;
    /** The same message to the same player is not repeated within this many ticks. */
    public static final int MESSAGE_GAP = 40;
    /** The upkeep runs once every this many ticks, and only for the players that have something live. */
    public static final int UPKEEP_EVERY = 4;

    /** True when {@code now} is at least {@code gap} ticks after {@code last}. */
    public static boolean gapOk(long now, long last, int gap) { return now - last >= gap; }

    /** A config value clamped into [lo, hi]. */
    public static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    public static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    // ================================================================================================ the prop word
    /** The spirit is one prop entity kind pair: SPIRITSPACE_1 is the spirit (orb or wisp), SPIRITSPACE_2 is one ring of a portal. The variant is the low four bits of the prop param. */
    public static final int V_ORB = 0, V_WISP = 1, V_PORTAL_IN = 2, V_PORTAL_OUT = 3, V_MASK = 15;
    /** Param bit: the orb is hidden (the wisp is out). Bits 12 and up carry a stamp (the game tick of the last cast, low 20 bits) for the orb and the wisp. */
    public static final int HIDDEN = 1 << 8, EXTRA_SHIFT = 12;
    public static final int EXTRA_MASK = (1 << 20) - 1;

    public static int param(int variant, int extra) { return (variant & V_MASK) | ((extra & EXTRA_MASK) << EXTRA_SHIFT); }

    public static int variant(int param) { return param & V_MASK; }

    public static int extra(int param) { return param >>> EXTRA_SHIFT; }

    public static boolean hidden(int param) { return (param & HIDDEN) != 0; }

    /** The ticks since the stamped tick {@code stamp} (low 20 bits of a game time), or a huge number when there is no stamp (0). */
    public static int sinceStamp(long now, int stamp) { return stamp == 0 ? Integer.MAX_VALUE : (int) ((now - stamp) & EXTRA_MASK); }

    /** The stamp to store for game time {@code now} (never 0, which means none). */
    public static int stampOf(long now) { int s = (int) (now & EXTRA_MASK); return s == 0 ? 1 : s; }

    // ================================================================================================ the abilities (the key packet)
    public static final int A_PORTAL = 0, A_GRAVITY = 1, A_CUT = 2, A_SET = 3, A_COUNT = 4;
    /** The packet actions: 0 toggles the link, 1 uses an ability (the second field says which and the third the waypoint slot). */
    public static final int ACT_TOGGLE = 0, ACT_USE = 1;

    public static boolean validAction(int action) { return action == ACT_TOGGLE || action == ACT_USE; }

    public static boolean validAbility(int ability) { return ability >= 0 && ability < A_COUNT; }

    /** The slot is 0-based; a waypoint book of {@code slots} slots accepts 0..slots-1. */
    public static boolean validSlot(int slot, int slots) { return slot >= 0 && slot < slots; }

    // ================================================================================================ waypoints and the portal
    /** Waypoints per player (config value is clamped to the hard cap), the longest name, the farthest a portal may reach (the config value is clamped to the hard cap). */
    public static final int WAYPOINTS_DEFAULT = 3, WAYPOINTS_HARD = 6, NAME_MAX = 20;
    public static final double RANGE_DEFAULT = 96.0, RANGE_MIN = 16.0, RANGE_HARD = 160.0;
    /** The channel before the portal opens (1.5 s), how long the two rings stay, the cooldown before the next portal (25 s). */
    public static final int CHANNEL_TICKS = 30, PORTAL_TICKS = 50, PORTAL_COOLDOWN_DEFAULT = 500, PORTAL_COOLDOWN_MIN = 300;
    /** How far the caster may drift while the channel runs. */
    public static final double CHANNEL_MOVE = 1.5;
    /** One ring pair per player; the server-wide cap of pairs. */
    public static final int PAIRS_PER_PLAYER = 1, PAIRS_DEFAULT = 12, PAIRS_HARD = 64;
    /** Mana of the abilities: percent of the maximum with a floor, through the balance law. */
    public static final double PORTAL_COST_PERCENT = 6.0, GRAVITY_COST_PERCENT = 4.0, CUT_COST_PERCENT = 5.0, COST_FLOOR = 40.0;

    /** Squared distance between two points. */
    public static double distSq(double ax, double ay, double az, double bx, double by, double bz) {
        double dx = ax - bx, dy = ay - by, dz = az - bz;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Is the destination within the range of the portal (true for a distance exactly the range)? */
    public static boolean inRange(double ax, double ay, double az, double bx, double by, double bz, double range) {
        return distSq(ax, ay, az, bx, by, bz) <= range * range;
    }

    /** Did the caster drift away from where the channel began (strictly more than {@link #CHANNEL_MOVE})? */
    public static boolean channelBroken(double sx, double sy, double sz, double cx, double cy, double cz) {
        return distSq(sx, sy, sz, cx, cy, cz) > CHANNEL_MOVE * CHANNEL_MOVE;
    }

    /** Has the channel run its course at {@code now}? */
    public static boolean channelDone(long now, long began) { return now - began >= CHANNEL_TICKS; }

    // ================================================================================================ the powered form
    public static final int WISP_TICKS = 80, WISP_COOLDOWN = 100, WISPS_PER_PLAYER = 1, WISPS_DEFAULT = 6, WISPS_HARD = 32;
    public static final int SPIRITS_PER_PLAYER = 1, SPIRITS_DEFAULT = 48, SPIRITS_HARD = 256;

    // ================================================================================================ gravity
    public static final double GRAVITY_RADIUS_DEFAULT = 8.0, GRAVITY_RADIUS_HARD = 16.0;
    public static final int GRAVITY_TARGETS_DEFAULT = 6, GRAVITY_TARGETS_HARD = 12;
    /** Levitation: the base ticks asked of the balance law's control cap (which shortens it for bosses and for a target hit again), the pulse's cooldown (15 s). */
    public static final int LEVITATION_TICKS = 40, GRAVITY_COOLDOWN = 300;

    // ================================================================================================ the spatial cut
    public static final double CUT_RADIUS_DEFAULT = 10.0, CUT_RADIUS_HARD = 16.0, CUT_REACH = 24.0;
    public static final int CUT_PROJECTILES_DEFAULT = 12, CUT_PROJECTILES_HARD = 32, CUT_COOLDOWN = 200;
    /** The raw damage of the cut at the default, the ceiling the config cannot pass, and the absolute ceiling of one hit after every multiplier (then the balance law's player cap still applies). */
    public static final double CUT_DAMAGE_DEFAULT = 10.0, CUT_DAMAGE_HARD = 60.0, CUT_HIT_CEILING = 400.0;

    /** The damage of one cut: raw damage times the caster's scaling, the game rule and the mana law's power, never above the absolute ceiling and never below zero. */
    public static double cutDamage(double raw, double epScale, double gameRule, double power) {
        double d = Math.max(0.0, raw) * Math.max(0.0, epScale) * Math.max(0.0, gameRule) * Math.max(0.0, power);
        return Math.min(CUT_HIT_CEILING, d);
    }

    /** How many projectiles of {@code found} one cast deletes: at most {@code cap}, and never a negative number. */
    public static int deleteCount(int found, int cap) { return Math.max(0, Math.min(found, cap)); }

    /**
     * Picks the nearest {@code cap} entries of {@code distSq[0..n)}: writes their indices into {@code out} (nearest first) and returns how many. No allocation, at most {@code cap} passes over {@code n}
     * entries (the cap is small), so the scan of a cast stays bounded however many projectiles are in the air.
     */
    public static int nearest(double[] distSq, int n, int cap, int[] out) {
        int k = Math.max(0, Math.min(Math.min(cap, n), out.length)), got = 0;
        for (int round = 0; round < k; round++) {
            int best = -1;
            double bd = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (distSq[i] >= bd) continue;
                boolean taken = false;
                for (int j = 0; j < got; j++) if (out[j] == i) { taken = true; break; }
                if (!taken) { best = i; bd = distSq[i]; }
            }
            if (best < 0) break;
            out[got++] = best;
        }
        return got;
    }

    // ================================================================================================ anchor points (server and client use the same maths)
    /** The orb's orbit round the owner: radius, height of the centre above the feet, bob, angular speed (radians a tick). */
    public static final double ORBIT_RADIUS = 0.80, ORBIT_HEIGHT = 1.95, ORBIT_BOB = 0.10, ORBIT_SPEED = 0.05;

    /** The orb's place at {@code age} ticks for an owner at (px, py, pz) whose body is turned {@code bodyYaw} degrees; written into {@code o} (x, y, z). */
    public static void orbitTo(double[] o, double px, double py, double pz, double bodyYaw, double age) {
        double a = Math.toRadians(bodyYaw) + age * ORBIT_SPEED;
        o[0] = px + Math.cos(a) * ORBIT_RADIUS;
        o[1] = py + ORBIT_HEIGHT + Math.sin(age * 0.11) * ORBIT_BOB;
        o[2] = pz + Math.sin(a) * ORBIT_RADIUS;
    }

    /** The powered wisp's anchor, its body's middle: above and behind the owner, facing the way the owner faces. */
    public static void wispTo(double[] o, double px, double py, double pz, double bodyYaw) {
        double r = Math.toRadians(bodyYaw), s = Math.sin(r), c = Math.cos(r);
        o[0] = px - (-s) * 1.7;
        o[1] = py + 3.0;
        o[2] = pz - c * 1.7;
    }

    /** The point the cut and the pulse are drawn from: the wisp's mouth, out in front of its anchor. */
    public static void mouthTo(double[] o, double px, double py, double pz, double bodyYaw) {
        double r = Math.toRadians(bodyYaw), s = Math.sin(r), c = Math.cos(r);
        wispTo(o, px, py, pz, bodyYaw);
        o[0] += -s * 2.3;
        o[1] += 0.2;
        o[2] += c * 2.3;
    }
}
