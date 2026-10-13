package com.newuniverse.nusmp.dungeon.nexus.zone;

/**
 * The numbers of the Blood-Red Commander's fight, in one pure place so they can be tested: its health and how a blow is soaked, the three
 * phases (it turns at half its health and at a quarter), the timing of every move (the big moves warn for a second and a bit at least, the
 * three fast weapon arts for 12 to 20 ticks under a very readable mark), how much each hurts, when it calls its shadow soldiers and the caps
 * on them. The fight is about dodging area attacks and handling minions, so no number here makes a blow unavoidable: every hit takes a
 * bounded share of a victim's health, and nobody is struck twice inside {@link #HIT_GRACE} ticks.
 */
public final class MonarchRules {
    private MonarchRules() {}

    // ---------------------------------------------------------------- body
    /** The Commander's health attribute (the attribute stops at 1024) and how much a blow is divided by before it lands: 1000 x 4.8 = 4800, eight times Zagred's 600. */
    public static final double BASE_HEALTH = 1000.0, SOAK = 4.8;
    /** No single blow takes more than this share of the health bar, whatever hit it. */
    public static final float MAX_HIT = 0.10f;
    /** Its size, its attack damage, extra armour and the share of speed it gains over the mob it is made from. */
    public static final double SCALE = 1.6, ATTACK = 36.0, ARMOR = 8.0, SPEED_GAIN = 0.10;
    /** The share of health at which phase 2 and phase 3 begin: half the bar and a quarter of it, where the shadow soldiers are called. */
    public static final float PHASE_2 = 0.50f, PHASE_3 = 0.25f;
    /** A blow may not take the bar further than this under a phase's floor: the floor is a little below the share, so the phase really changes. */
    private static final float EDGE = 0.01f;

    /** 1, 2 or 3 by the share of health left. */
    public static int phaseOf(float health, float max) {
        if (max <= 0f) return 3;
        float f = health / max;
        return f > PHASE_2 ? 1 : f > PHASE_3 ? 2 : 3;
    }

    /** The health a blow cannot take it under while the phase is `phase`: it has to be seen to change phase first. 0 in the last phase. */
    public static float floorOf(int phase, float max) {
        return phase <= 1 ? max * PHASE_2 - EDGE : phase == 2 ? max * PHASE_3 - EDGE : 0f;
    }

    /** What a blow of `incoming` damage does to the bar: soaked, capped to a tenth of the bar. */
    public static float realDamage(float incoming, float max) {
        if (incoming <= 0f || max <= 0f) return 0f;
        return (float) Math.min(incoming / SOAK, max * MAX_HIT);
    }

    /** The damage that goes through: no more than takes the bar to the floor of its phase, never negative. */
    public static float gate(float health, float max, float damage, int phase) {
        float room = health - floorOf(phase, max);
        return Math.max(0f, Math.min(damage, room));
    }

    // ---------------------------------------------------------------- waking and sleeping
    /** A player this close (blocks across the floor, blocks up or down) to the middle of the dais wakes it. */
    public static final double WAKE_RADIUS = 18.0, WAKE_HEIGHT = 14.0;
    /** A foe is anyone inside the hall (this many blocks of the dais); the circles of the Shadow Rain fall within RAIN_REACH of it; it goes back to sleep when no foe has been in the hall for RESET_AFTER ticks. */
    public static final double ARENA_RADIUS = 46.0, RAIN_REACH = 38.0;
    public static final int RESET_AFTER = 200;
    /** The boss bar is shown to players this close. */
    public static final double BAR_RANGE = 80.0;

    public static boolean wakes(double dx, double dz, double dy) {
        return dx * dx + dz * dz <= WAKE_RADIUS * WAKE_RADIUS && Math.abs(dy) <= WAKE_HEIGHT;
    }

    // ---------------------------------------------------------------- timing (ticks)
    /** The warning before each big move lands: a ring on the floor, the marks of the rain, the line, the field, the gathering of shadows, the roar between phases. */
    public static final int SLAM_WINDUP = 30, RAIN_MARK = 32, BEAM_WINDUP = 36, AUTHORITY_WINDUP = 30, SUMMON_CHANNEL = 44, TRANSITION = 50;
    /** No big move is warned for less than this (a second and a bit). */
    public static final int MIN_WARNING = 24;
    /** The warning before each fast art lands: the red cross, the red lane, the red arc. Fast, but never under 12 ticks and never over 20, and always with the whole mark on the floor. */
    public static final int CROSS_WINDUP = 18, LUNGE_WINDUP = 18, SWEEP_WINDUP = 20;
    /** The shortest and the longest warning a fast art may give. */
    public static final int MIN_ART_WARNING = 12, MAX_ART_WARNING = 20;
    /** How long it stands still after a move lands, open to being hit. */
    public static final int RECOVERY = 16;
    /** How long a player is spared after being hit by one of its area attacks. */
    public static final int HIT_GRACE = 12;

    /** The ticks the Commander stands still for a move, from the moment it starts. */
    public static int freeze(MonarchMove m, int phase) {
        return switch (m) {
            case SLAM -> SLAM_WINDUP + (int) Math.ceil(slamRadius(phase) / SLAM_SPEED) + RECOVERY;
            case RAIN -> 24;
            case SUMMON -> SUMMON_CHANNEL + RECOVERY;
            case BEAM -> BEAM_WINDUP + RECOVERY;
            case AUTHORITY -> AUTHORITY_WINDUP + 10;
            case CROSS_SLASH -> CROSS_WINDUP + (crosses(phase) - 1) * CROSS_GAP + CROSS_CUT + RECOVERY;
            case LUNGE -> LUNGE_WINDUP + lungeTicks(LUNGE_MAX) + RECOVERY;
            case SWEEP -> SWEEP_WINDUP + (sweepCuts(phase) - 1) * SWEEP_BACK + SWEEP_CUT + RECOVERY;
            default -> MIN_WARNING + RECOVERY;
        };
    }

    /**
     * The last tick, counted from the start of the move, on which a fast art still does anything (a mark is drawn, a blade passes, the Commander
     * dashes). The Commander stands still for at least {@link #RECOVERY} ticks after it, open to attack; for the other moves, their warning.
     */
    public static int activeUntil(MonarchMove m, int phase) {
        return switch (m) {
            case CROSS_SLASH -> CROSS_WINDUP + (crosses(phase) - 1) * CROSS_GAP;
            case LUNGE -> LUNGE_WINDUP + lungeTicks(LUNGE_MAX);
            case SWEEP -> SWEEP_WINDUP + (sweepCuts(phase) - 1) * SWEEP_BACK + SWEEP_CUT - 1;
            default -> warning(m);
        };
    }

    /** The ticks between the end of a move and the choice of the next, by phase. */
    public static int gap(int phase) { return phase <= 1 ? 80 : phase == 2 ? 58 : 42; }

    /** The ticks before a move may be used again, by phase. The arts come round oftener than the big moves: they are the bulk of the fight. */
    public static int cooldown(MonarchMove m, int phase) {
        int i = clampPhase(phase) - 1;
        return switch (m) {
            case SLAM -> new int[]{150, 120, 96}[i];
            case RAIN -> new int[]{190, 160, 130}[i];
            case SUMMON -> new int[]{560, 480, 400}[i];
            case BEAM -> new int[]{210, 180, 150}[i];
            case AUTHORITY -> new int[]{360, 300, 260}[i];
            case CROSS_SLASH -> new int[]{120, 104, 90}[i];
            case LUNGE -> new int[]{130, 112, 96}[i];
            case SWEEP -> new int[]{120, 104, 90}[i];
            default -> 240;
        };
    }

    /** The ticks the move needs its warning to last. */
    public static int warning(MonarchMove m) {
        return switch (m) {
            case SLAM -> SLAM_WINDUP;
            case RAIN -> RAIN_MARK;
            case SUMMON -> SUMMON_CHANNEL;
            case BEAM -> BEAM_WINDUP;
            case AUTHORITY -> AUTHORITY_WINDUP;
            case CROSS_SLASH -> CROSS_WINDUP;
            case LUNGE -> LUNGE_WINDUP;
            case SWEEP -> SWEEP_WINDUP;
            default -> MIN_WARNING;
        };
    }

    static int clampPhase(int phase) { return Math.max(1, Math.min(3, phase)); }

    // ---------------------------------------------------------------- damage
    /** The share of a victim's health a move takes, by phase; the soldiers and the field do not hurt directly. For the arts it is the share of ONE cut (a cross, a sweep): see {@link #totalShare}. */
    public static float hitShare(MonarchMove m, int phase) {
        int i = clampPhase(phase) - 1;
        return switch (m) {
            case SLAM -> new float[]{0.20f, 0.24f, 0.28f}[i];
            case RAIN -> new float[]{0.16f, 0.20f, 0.24f}[i];
            case BEAM -> new float[]{0.30f, 0.34f, 0.38f}[i];
            case SUMMON, AUTHORITY -> 0f;
            case CROSS_SLASH -> new float[]{0.10f, 0.12f, 0.14f}[i];
            case LUNGE -> new float[]{0.20f, 0.24f, 0.28f}[i];
            case SWEEP -> new float[]{0.16f, 0.19f, 0.22f}[i];
            default -> 0f;
        };
    }

    /** How many cuts of a move can land on one victim in one casting (a cross per X, a sweep and its reverse). */
    public static int cuts(MonarchMove m, int phase) {
        return switch (m) {
            case CROSS_SLASH -> crosses(phase);
            case SWEEP -> sweepCuts(phase);
            default -> 1;
        };
    }

    /** The most of a victim's health one casting can take if every cut lands (the arts are quick, so they come in several cuts): kept well under the whole bar. */
    public static float totalShare(MonarchMove m, int phase) { return hitShare(m, phase) * cuts(m, phase); }

    /** The most one of its area attacks may take: well under half, so even a full bar survives two of them. */
    public static final float SHARE_CEILING = 0.45f;

    /** The damage of a hit of that share on a victim with that much health: at least a few points, never more than the ceiling. */
    public static float hit(float share, float victimMax) {
        if (share <= 0f || victimMax <= 0f) return 0f;
        return Math.min(Math.max(3f, share * victimMax), victimMax * SHARE_CEILING);
    }

    /** The most a blow of the Commander's own hand, or of one of its soldiers, may take of a player's health: its strength is large, but no single blow kills. */
    public static final float MELEE_SHARE = 0.30f, SOLDIER_SHARE = 0.18f;

    /** What a melee blow of `incoming` damage does to a victim with that much health, struck by the Commander (boss) or by a soldier. */
    public static float capMelee(float incoming, float victimMax, boolean boss) {
        if (incoming <= 0f) return incoming;
        return Math.min(incoming, hit(boss ? MELEE_SHARE : SOLDIER_SHARE, victimMax));
    }

    /** May a victim be struck now, when it was last struck at `lastHit`? */
    public static boolean mayHit(long now, long lastHit) { return now - lastHit >= HIT_GRACE; }

    // ---------------------------------------------------------------- the moves
    /** Domain Slam: how far the ring spreads (blocks), how fast (blocks a tick), how thick its band is (half), and how far above or below the Commander's floor a body on the ground can stand and still be caught. */
    public static double slamRadius(int phase) { return new double[]{14.0, 16.0, 18.0}[clampPhase(phase) - 1]; }

    public static final double SLAM_SPEED = 0.75, SLAM_BAND = 1.0, SLAM_REACH = 4.5;

    /** Shadow Rain: circles in one casting, how wide each is, and the most that may be asked of one player's side. */
    public static int rainCircles(int phase, int foes) { return new int[]{5, 8, 11}[clampPhase(phase) - 1] + Math.min(Math.max(foes, 1), 4) - 1; }

    public static final double RAIN_RADIUS = 2.6, RAIN_GAP = 3.0;

    /** Soul Rend: lines in one casting, half the width of a line, its length, the spread of the fan (radians). */
    public static int beams(int phase) { return clampPhase(phase); }

    public static final double BEAM_HALF = 1.3, BEAM_LENGTH = 44.0, BEAM_FAN = 0.5;

    /** Monarch's Authority: the radius of the field, how long it holds (ticks), and the slowness it lays on (an amplifier); a Mana Zone holds it back by a level and halves the time it lingers. */
    public static final double AUTHORITY_RADIUS = 12.0;
    public static final int AUTHORITY_TICKS = 80, AUTHORITY_REFRESH = 10;

    public static int authoritySlow(int phase, boolean manaZone) {
        int base = phase >= 3 ? 2 : 1;
        return manaZone ? base - 1 : base;
    }

    public static int authorityDuration(boolean manaZone) { return manaZone ? AUTHORITY_REFRESH + 10 : AUTHORITY_REFRESH + 30; }

    // ---------------------------------------------------------------- the arts: Crimson Cross
    /**
     * Crimson Cross: how many crosses one casting draws (one in the first phase, a second one turned 45 degrees and begun 20 ticks later in the
     * second, three in a row in the third), the ticks from the start of one cross's mark to the start of the next, how long the cut lasts, the
     * turn between one cross and the next (45 degrees), half the length of a line, half its width, and how far above or below the floor of the
     * mark a body standing on the ground is still cut. Every cross is centred on where the target stands when ITS mark begins, never on the boss.
     */
    public static int crosses(int phase) { return clampPhase(phase); }

    public static final int CROSS_GAP = 20, CROSS_CUT = 2;
    public static final double CROSS_TURN = Math.PI / 4, CROSS_HALF_LEN = 13.0, CROSS_HALF_WIDTH = 1.25, CROSS_REACH = 2.0;

    // ---------------------------------------------------------------- the arts: Blood-Red Lunge
    /**
     * Blood-Red Lunge: how fast it dashes (blocks a tick), the longest lane it will ever mark (a hard cap), the shortest run worth a lunge
     * (a wall right in front of it and the move is given up), how far past the target the lane reaches, half the width of the lane.
     */
    public static final double LUNGE_SPEED = 2.0, LUNGE_MAX = 32.0, LUNGE_MIN_RUN = 6.0, LUNGE_OVERSHOOT = 8.0, LUNGE_HALF = 1.6;

    /**
     * A lane cannot be jumped, only left, so it is never thrown at someone too slow to leave it in time (even when the field of Authority has made
     * the arts likelier): the strongest Slowness (an amplifier, -1 for none) a target may have for the lunge to be marked for them. Slowness III
     * (amplifier 2), which the field lays on in the last phase, is too much, and a cross is thrown instead; the crosses and the arc can be jumped,
     * so they have no such limit.
     */
    public static final int LUNGE_MAX_SLOW = 1;

    public static boolean lungeMayTarget(int slownessAmplifier) { return slownessAmplifier <= LUNGE_MAX_SLOW; }

    /** How long a lane to mark towards a target `toTarget` blocks away: it reaches past them, at least a short run, never more than the cap. */
    public static double lungeLength(double toTarget) {
        return Math.min(LUNGE_MAX, Math.max(LUNGE_MIN_RUN + 4.0, toTarget + LUNGE_OVERSHOOT));
    }

    /** The ticks a dash of that length takes. */
    public static int lungeTicks(double length) { return (int) Math.ceil(Math.max(0.0, length) / LUNGE_SPEED); }

    // ---------------------------------------------------------------- the arts: Executioner's Arc
    /**
     * Executioner's Arc: the radius of the fan by phase (9 to 13 blocks), half its angle (100 degrees, so the fan is 200), how many ticks one cut
     * takes to sweep across it, how many ticks after one cut begins the reverse cut begins, how many cuts a casting has (the reverse sweep comes
     * from the second phase on) and how far above or below the Commander's floor a body on the ground is still caught.
     */
    public static double sweepRadius(int phase) { return new double[]{9.0, 11.0, 13.0}[clampPhase(phase) - 1]; }

    public static final double SWEEP_HALF_ANGLE = Math.toRadians(100.0), SWEEP_REACH = 4.5;
    public static final int SWEEP_CUT = 5, SWEEP_BACK = 14;

    public static int sweepCuts(int phase) { return clampPhase(phase) >= 2 ? 2 : 1; }

    // ---------------------------------------------------------------- soldiers
    /** The most shadow soldiers alive at once, soldiers per summoning, how long a soldier lasts (ticks). */
    public static final int HARD_CAP = 8;
    public static final int SOLDIER_LIFE = 1800;

    public static int minionCap(int phase) { return Math.min(HARD_CAP, new int[]{4, 6, 8}[clampPhase(phase) - 1]); }

    public static int summonBatch(int phase) { return new int[]{2, 3, 4}[clampPhase(phase) - 1]; }

    /** How many soldiers one summoning brings, given how many are alive: the batch, but never past the cap. */
    public static int summonCount(int phase, int alive) { return Math.max(0, Math.min(summonBatch(phase), minionCap(phase) - alive)); }

    /** A soldier's health and damage as multiples of the daemon it is made from, by phase, and the greater daemons (stronger) that may stand with them. */
    public static double soldierHealth(int phase) { return new double[]{1.5, 2.0, 2.5}[clampPhase(phase) - 1]; }

    public static double soldierDamage(int phase) { return new double[]{1.2, 1.4, 1.6}[clampPhase(phase) - 1]; }

    public static int greaterAllowed(int phase) { return phase >= 3 ? 2 : 0; }

    /** The most summonings one fight has: the one at half its health, the one at a quarter, and one refill. */
    public static final int MAX_SUMMONINGS = 3;
    /** The ticks (25 seconds) after a summoning before an empty field is refilled, in the last phase only. */
    public static final int REFILL_AFTER = 500;

    /**
     * Is a summoning due? Yes the moment the phase turns to 2 (the first summoning, at half its health) or to 3 (the second, at a quarter),
     * whatever stands about. Otherwise only the refill: in the last phase, when every soldier is dead and {@link #REFILL_AFTER} ticks have passed
     * since the last summoning, so the fight does not stall on an empty field. (How many refills there may be is {@link #mayRefill}.)
     */
    public static boolean summonDue(int prevPhase, int newPhase, int soldiersAlive, long ticksSinceLast) {
        if (newPhase > prevPhase) return newPhase == 2 || newPhase == 3;
        return newPhase >= 3 && soldiersAlive <= 0 && ticksSinceLast >= REFILL_AFTER;
    }

    /** Is there a summoning left in this fight's allowance, when `summonsSoFar` have been made? Bounds the refill; the two threshold summonings are always made. */
    public static boolean mayRefill(int summonsSoFar) { return summonsSoFar < MAX_SUMMONINGS; }

    /** Is the one refill due now: the last phase, the allowance not spent, every soldier dead, 25 seconds since the last summoning? */
    public static boolean refillDue(int phase, int summonsSoFar, int soldiersAlive, long ticksSinceLast) {
        return phase >= 3 && mayRefill(summonsSoFar) && summonDue(phase, phase, soldiersAlive, ticksSinceLast);
    }

    /** The experience the Commander gives when it falls: it drops nothing else. */
    public static final int XP = 3000;
}
