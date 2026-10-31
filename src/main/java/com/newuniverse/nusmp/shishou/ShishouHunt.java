package com.newuniverse.nusmp.shishou;

/**
 * Why: the 2.1.0 hunting of Shishou in one pure place (no game classes): how often he looks for prey, how far, how many creatures one look may examine, which target he prefers, how fast and how hard
 * he charges, how fast he strikes, and how a blocked charge frees itself. The owner found the 2.0.0 hunter slow (a vanilla target scan of about one look every ten ticks that only saw four blocks up and
 * down, a 24 block range, a 24 tick strike cooldown, a charge that stalled on the first wall), so every one of those numbers is here, tested, and read by {@link ShishouGoals} and {@link ShishouEntity}.
 * The server cost stays bounded and is stated in docs/items/shishou.md: one box query per scan, at most {@link #MAX_EXAMINED} creatures examined, a scan every {@link #SCAN_TICKS} ticks while he has no target.
 */
public final class ShishouHunt {
    private ShishouHunt() {}

    /** Ticks between two looks for prey while he has no target (2.0.0: about 10 and often more), the most creatures one look examines, and the lowest / highest config values. */
    public static final int SCAN_TICKS = 6, MAX_EXAMINED = 64, SCAN_TICKS_MIN = 2, SCAN_TICKS_MAX = 40, EXAMINED_MIN = 8, EXAMINED_MAX = 256;
    /** The wild phase looks 40 blocks (2.0.0: 24) in a cube (2.0.0: 4 blocks up and down only); the config may set 8 to 96. A cube of 96 is the most a scan may cover. */
    public static final int WILD_RADIUS = 40, RADIUS_MIN = 8, RADIUS_MAX = 96, VERTICAL_MAX = 24;
    /** A thrown sword hunts everything within the leash of its wielder; its own pursuit may run this many blocks past the leash before it gives up. */
    public static final double LEASH_SLACK = 6.0;

    /** The scan gap for the body with this entity id: the configured gap plus a spread of 0..2 ticks, so two bodies never scan in the same tick (there is only one, but spawn commands exist). */
    public static int scanGap(int configured, int entityId) { return Math.max(SCAN_TICKS_MIN, Math.min(SCAN_TICKS_MAX, configured)) + Math.floorMod(entityId, 3); }

    /** The radius of a scan: the wild phase uses the configured radius, a minion the leash plus the slack; never above {@link #RADIUS_MAX}. */
    public static double scanRadius(boolean minion, int wildRadius, int leash) {
        return Math.min(RADIUS_MAX, minion ? leash + LEASH_SLACK : Math.max(RADIUS_MIN, wildRadius));
    }

    /** The vertical half height of a scan box: the radius, but never more than {@link #VERTICAL_MAX}. */
    public static double scanHeight(double radius) { return Math.min(VERTICAL_MAX, radius); }

    /**
     * The preference between two candidates, LOWER is better: the distance, minus a bonus for danger (a bigger creature is worth chasing first, up to 8 blocks of preference) and a bonus for a creature that
     * is attacking a player (up to 14). So he takes the nearest threat, and the one that is hurting somebody before a quiet one a little nearer.
     */
    public static double score(double distance, double maxHealth, boolean attackingPlayer) {
        double danger = Math.min(8.0, Math.max(0.0, maxHealth) / 10.0);
        return Math.max(0.0, distance) - danger - (attackingPlayer ? 14.0 : 0.0);
    }

    // ---------------------------------------------------------------- the charge
    /** Blocks per tick the charge accelerates by (a short curve: full speed in about eight ticks), and the speed it starts a charge with. */
    public static final double CHARGE_ACCEL = 0.16, CHARGE_START = 0.45, CHARGE_HARD_CAP = 1.9, ARRIVE_SLOW_RANGE = 4.0, ARRIVE_MIN = 0.45;

    /** The top speed of the charge in blocks per tick: 1.15 at level 10, +0.01 per level, never above 1.9 (about 23 blocks a second at the start, 38 at the cap); {@code percent} is the server's charge speed. */
    public static double topSpeed(int level, int percent) {
        double v = (1.05 + 0.01 * ShishouRules.clampLevel(level)) * Math.max(10, Math.min(300, percent)) / 100.0;
        return Math.min(CHARGE_HARD_CAP, v);
    }

    /** The speed of a charge {@code ticksCharging} ticks after it began: it accelerates from {@link #CHARGE_START} by {@link #CHARGE_ACCEL} a tick up to {@code top}. */
    public static double chargeSpeed(int ticksCharging, double top) { return Math.min(top, CHARGE_START + CHARGE_ACCEL * Math.max(0, ticksCharging)); }

    /** Near the target the charge slows so it does not shoot past it: full speed beyond {@link #ARRIVE_SLOW_RANGE} blocks, down to {@link #ARRIVE_MIN} of it at the target. */
    public static double arrive(double speed, double distance) {
        if (distance >= ARRIVE_SLOW_RANGE) return speed;
        double k = ARRIVE_MIN + (1.0 - ARRIVE_MIN) * Math.max(0.0, distance) / ARRIVE_SLOW_RANGE;
        return speed * k;
    }

    /** How much of the old velocity is kept when the new one is blended in (0.35 = he turns within about three ticks): the steering of the charge. */
    public static final double TURN = 0.35;

    /** One component of the blended velocity: {@code old + (wanted - old) * TURN}. */
    public static double blend(double old, double wanted) { return old + (wanted - old) * TURN; }

    // ---------------------------------------------------------------- the strike
    /** The strike cooldown in ticks: 15 at level 10 down to 6 at level 100 (2.0.0: 24 always). */
    public static int strikeCooldown(int level) { return Math.max(6, 15 - ShishouRules.clampLevel(level) / 11); }

    // ---------------------------------------------------------------- the flurry (3.2.0)
    /** After a strike he bounces off the target: this many ticks he flies free on the bounce, at {@link #BOUNCE_SPEED} blocks a tick away from it (a little up), then the charge takes over again. */
    public static final int BOUNCE_TICKS = 3;
    public static final double BOUNCE_SPEED = 0.55, BOUNCE_LIFT = 0.12;

    /** The bounce velocity away from the target along {@code (dx, dy, dz)} (sword minus target); zero length bounces straight up. */
    public static double[] bounce(double dx, double dy, double dz) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-4) return new double[]{0.0, BOUNCE_SPEED, 0.0};
        return new double[]{dx / len * BOUNCE_SPEED, dy / len * BOUNCE_SPEED + BOUNCE_LIFT, dz / len * BOUNCE_SPEED};
    }

    // ---------------------------------------------------------------- the unstick rule
    /** Ticks of a blocked charge before he slides upward, and before he gives the target up for a while (so a creature behind a wall that he cannot reach does not hold him forever). */
    public static final int STUCK_NUDGE = 3, STUCK_GIVE_UP = 40, IGNORE_TICKS = 160;

    /** What a blocked charge does after {@code stuckTicks} ticks of no progress: 0 nothing yet, 1 nudge up and aside, 2 give the target up. */
    public static int unstick(int stuckTicks) { return stuckTicks >= STUCK_GIVE_UP ? 2 : stuckTicks >= STUCK_NUDGE ? 1 : 0; }

    /** Is the charge making progress? Moving more than a tenth of a block in the tick counts. */
    public static boolean progressed(double movedSqr) { return movedSqr > 0.01; }

    // ---------------------------------------------------------------- patrol (the wild sword goes where the monsters are)
    /** A wild sword with no prey in range flies toward the nearest player if one is within this many blocks but farther than {@link #PATROL_MIN}: that is where monsters spawn. */
    public static final double PATROL_MAX = 160.0, PATROL_MIN = 40.0, PATROL_STAND_OFF = 24.0;
    public static final int PATROL_PERIOD = 20, PATROL_SPEED_PERCENT = 150;

    /** The distance a patrol flies for: toward a player at {@code distance}, ending {@link #PATROL_STAND_OFF} short of them; 0 when the player is out of the patrol band. */
    public static double patrolLeg(double distance) { return distance > PATROL_MAX || distance < PATROL_MIN ? 0.0 : distance - PATROL_STAND_OFF; }

    /** Worst case, for the docs and the tests: scans a second and creatures examined a second while he hunts (one box query per scan). */
    public static double scansPerSecond(int scanGap) { return 20.0 / Math.max(1, scanGap); }

    public static int examinedPerSecond(int scanGap, int maxExamined) { return (int) Math.ceil(scansPerSecond(scanGap) * maxExamined); }

    // ---------------------------------------------------------------- 3.42.0 audit
    /** The best candidates of a scan whose line of sight is checked, best first: at most this many clip rays per scan (a charge cannot pass a wall, so a creature he can see beats a nearer one he cannot). */
    public static final int LOS_CHECKS = 3;

    /** Worst case of the line-of-sight rays a second while he hunts: one scan every {@code scanGap} ticks, {@link #LOS_CHECKS} rays each. */
    public static double raysPerSecond(int scanGap) { return scansPerSecond(scanGap) * LOS_CHECKS; }

    /** A fixed-size list of the {@code k} lowest scores seen (lower is better), best first; offering is O(k) and nothing grows. */
    public static final class Top {
        private final int[] index;
        private final double[] score;
        private int n;

        public Top(int k) {
            index = new int[Math.max(1, k)];
            score = new double[index.length];
        }

        public void offer(int i, double s) {
            if (n == index.length && s >= score[n - 1]) return;
            int at = Math.min(n, index.length - 1);
            while (at > 0 && score[at - 1] > s) {
                index[at] = index[at - 1];
                score[at] = score[at - 1];
                at--;
            }
            index[at] = i;
            score[at] = s;
            if (n < index.length) n++;
        }

        public int size() { return n; }

        /** The candidate index at rank {@code r} (0 = best). */
        public int index(int r) { return index[r]; }
    }

    // The follow flight of a thrown sword (it used to ask the move control, whose air speed is a vanilla default; now it steers like the charge and the patrol do).
    public static final double FOLLOW_MIN = 0.12, FOLLOW_EASE = 6.0, FOLLOW_EASE_TOP = 0.6, FOLLOW_STEP = 0.05, FOLLOW_MAX = 1.6, DORMANT_SPEED = 0.55;

    /** Blocks per tick a thrown sword flies to its wielder's shoulder at {@code distance} blocks: it eases in over the last {@link #FOLLOW_EASE} blocks, and beyond that speeds up to {@link #FOLLOW_MAX}. */
    public static double followSpeed(double distance) {
        double d = Math.max(0.0, distance);
        if (d <= FOLLOW_EASE) return FOLLOW_MIN + (FOLLOW_EASE_TOP - FOLLOW_MIN) * d / FOLLOW_EASE;
        return Math.min(FOLLOW_MAX, FOLLOW_EASE_TOP + FOLLOW_STEP * (d - FOLLOW_EASE));
    }
}
