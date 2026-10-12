package com.newuniverse.nusmp.dungeon.nexus.zone;

/**
 * The numbers of the Monarch's fight, in one pure place so they can be tested: its health and how a blow is soaked, the three phases, the
 * timing of every move (and the rule that none is quicker than a second's warning), how much each hurts, and the caps on its soldiers. The
 * fight is about dodging area attacks and handling minions, so no number here makes a blow unavoidable: every hit takes a bounded share of
 * a victim's health, and nobody is struck twice inside {@link #HIT_GRACE} ticks.
 */
public final class MonarchRules {
    private MonarchRules() {}

    // ---------------------------------------------------------------- body
    /** The Monarch's health attribute (the attribute stops at 1024) and how much a blow is divided by before it lands: 1000 x 4.8 = 4800, eight times Zagred's 600. */
    public static final double BASE_HEALTH = 1000.0, SOAK = 4.8;
    /** No single blow takes more than this share of the health bar, whatever hit it. */
    public static final float MAX_HIT = 0.10f;
    /** Its size, its attack damage, extra armour and the share of speed it gains over the mob it is made from. */
    public static final double SCALE = 1.6, ATTACK = 36.0, ARMOR = 8.0, SPEED_GAIN = 0.10;
    /** The share of health at which phase 2 and phase 3 begin. */
    public static final float PHASE_2 = 0.66f, PHASE_3 = 0.33f;
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
    /** The warning before each move lands: a ring on the floor, the marks of the rain, the line, the field, the gathering of shadows, the roar between phases. */
    public static final int SLAM_WINDUP = 30, RAIN_MARK = 32, BEAM_WINDUP = 36, AUTHORITY_WINDUP = 30, SUMMON_CHANNEL = 44, TRANSITION = 50;
    /** No warning is shorter than this (a second and a bit). */
    public static final int MIN_WARNING = 24;
    /** How long it stands still after a move lands, open to being hit. */
    public static final int RECOVERY = 16;
    /** How long a player is spared after being hit by one of its area attacks. */
    public static final int HIT_GRACE = 12;

    /** The ticks the Monarch stands still for a move, from the moment it starts. */
    public static int freeze(MonarchMove m, int phase) {
        return switch (m) {
            case SLAM -> SLAM_WINDUP + (int) Math.ceil(slamRadius(phase) / SLAM_SPEED) + RECOVERY;
            case RAIN -> 24;
            case SUMMON -> SUMMON_CHANNEL + RECOVERY;
            case BEAM -> BEAM_WINDUP + RECOVERY;
            case AUTHORITY -> AUTHORITY_WINDUP + 10;
        };
    }

    /** The ticks between the end of a move and the choice of the next, by phase. */
    public static int gap(int phase) { return phase <= 1 ? 80 : phase == 2 ? 58 : 42; }

    /** The ticks before a move may be used again, by phase. */
    public static int cooldown(MonarchMove m, int phase) {
        int i = clampPhase(phase) - 1;
        return switch (m) {
            case SLAM -> new int[]{150, 120, 96}[i];
            case RAIN -> new int[]{190, 160, 130}[i];
            case SUMMON -> new int[]{560, 480, 400}[i];
            case BEAM -> new int[]{210, 180, 150}[i];
            case AUTHORITY -> new int[]{360, 300, 260}[i];
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
        };
    }

    static int clampPhase(int phase) { return Math.max(1, Math.min(3, phase)); }

    // ---------------------------------------------------------------- damage
    /** The share of a victim's health a move takes, by phase; the soldiers and the field do not hurt directly. */
    public static float hitShare(MonarchMove m, int phase) {
        int i = clampPhase(phase) - 1;
        return switch (m) {
            case SLAM -> new float[]{0.20f, 0.24f, 0.28f}[i];
            case RAIN -> new float[]{0.16f, 0.20f, 0.24f}[i];
            case BEAM -> new float[]{0.30f, 0.34f, 0.38f}[i];
            case SUMMON, AUTHORITY -> 0f;
        };
    }

    /** The most one of its area attacks may take: well under half, so even a full bar survives two of them. */
    public static final float SHARE_CEILING = 0.45f;

    /** The damage of a hit of that share on a victim with that much health: at least a few points, never more than the ceiling. */
    public static float hit(float share, float victimMax) {
        if (share <= 0f || victimMax <= 0f) return 0f;
        return Math.min(Math.max(3f, share * victimMax), victimMax * SHARE_CEILING);
    }

    /** The most a blow of the Monarch's own hand, or of one of its soldiers, may take of a player's health: its strength is large, but no single blow kills. */
    public static final float MELEE_SHARE = 0.30f, SOLDIER_SHARE = 0.18f;

    /** What a melee blow of `incoming` damage does to a victim with that much health, struck by the Monarch (boss) or by a soldier. */
    public static float capMelee(float incoming, float victimMax, boolean boss) {
        if (incoming <= 0f) return incoming;
        return Math.min(incoming, hit(boss ? MELEE_SHARE : SOLDIER_SHARE, victimMax));
    }

    /** May a victim be struck now, when it was last struck at `lastHit`? */
    public static boolean mayHit(long now, long lastHit) { return now - lastHit >= HIT_GRACE; }

    // ---------------------------------------------------------------- the moves
    /** Domain Slam: how far the ring spreads (blocks), how fast (blocks a tick), how thick its band is (half), and how far above or below the Monarch's floor a body on the ground can stand and still be caught. */
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

    /** The experience the Monarch gives when it falls: it drops nothing else. */
    public static final int XP = 3000;
}
