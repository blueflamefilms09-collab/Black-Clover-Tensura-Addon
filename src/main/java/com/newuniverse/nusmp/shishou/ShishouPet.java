package com.newuniverse.nusmp.shishou;

import java.util.Locale;

/**
 * Why: the PET MODE of the thrown Shishou (server setting {@code pet.mode}, default on): the thrown sword behaves like a tamed pet. This is the pure brain (no game classes): the three orders the wielder can give,
 * how far he hunts in each, when he retreats to his wielder (low mana), when he guards (the wielder is hurt), when he gives a chase up (too far) and how a flight that meets a wall turns away from it. The glue
 * ({@link ShishouEntity}, {@link ShishouPetGoals}) only asks and acts. Everything is bounded: ranges are capped, and nothing here allocates more than a few numbers.
 */
public final class ShishouPet {
    private ShishouPet() {}

    /** What the wielder told him to do (cycled by the Telekinesis mode with sneak, or {@code /multiverse shishou order}). Only ever appended. */
    public enum Order {
        /** Follows, and attacks any hostile near the wielder, what the wielder hits and what hits the wielder (the default). */
        FOLLOW,
        /** Hovers where he is and only strikes what comes within {@link #STAY_GUARD} blocks of him. */
        STAY,
        /** Hunts farther ({@link #ATTACK_EXTRA} blocks past the leash) and chases past the usual follow range. */
        ATTACK;

        public String id() { return name().toLowerCase(Locale.ROOT); }

        public String langKey() { return "shishou.order." + id(); }

        public Order next() { return values()[(ordinal() + 1) % values().length]; }

        public static Order byId(String id) {
            if (id == null) return null;
            for (Order o : values()) if (o.id().equals(id.toLowerCase(Locale.ROOT))) return o;
            return null;
        }

        public static Order byOrdinal(int i) { return i >= 0 && i < values().length ? values()[i] : FOLLOW; }
    }

    /** What his brain says this second. */
    public enum State { HUNT, GUARD, RETREAT, STAY, RECALL }

    public static final double STAY_GUARD = 6.0, ATTACK_EXTRA = 16.0, SCAN_CAP = 48.0, GUARD_RADIUS = 8.0;
    /** The wielder at or under this share of their health: he guards them (stays within {@link #GUARD_RADIUS}). */
    public static final double HURT_OWNER_SHARE = 0.35;
    /** Below this share of the sword's mana he retreats to the wielder and stops hunting; he resumes above {@link #RESUME_SHARE} (a gap, so he does not flicker). */
    public static final double RETREAT_SHARE = ShishouRules.LOW_MANA_SHARE, RESUME_SHARE = 0.30;
    /** A chase is given up when he is this many blocks past the leash (the old leash was a hard line; now it is a soft one he comes back from). */
    public static final double FAR_SLACK = 16.0;
    /** Follow distance: he comes to the wielder's shoulder when farther than this (a retreating sword comes closer). */
    public static final double FOLLOW_START = 9.0, RETREAT_FOLLOW_START = 4.0;
    /** A follow flight that makes no progress for this many ticks, with the wielder at least {@link #BLINK_MIN} blocks away, blinks to the shoulder (no stalling on a wall). */
    public static final int STUCK_BLINK_TICKS = 40;
    public static final double BLINK_MIN = 10.0;
    /** Flying: a probe this far ahead (in ticks of flight) looks for a wall; once every {@link #PROBE_GAP} ticks. */
    public static final double PROBE_TICKS = 4.0;
    public static final int PROBE_GAP = 2;

    /**
     * The decision. {@code retreating} is last second's answer (the gap between {@link #RETREAT_SHARE} and {@link #RESUME_SHARE} keeps a retreating sword retreating until he has mana again). An empty pool is
     * RECALL (the service puts him back in the hand, as before).
     */
    public static State decide(Order order, double manaShare, double ownerHealthShare, boolean retreating) {
        if (manaShare <= 0.0) return State.RECALL;
        if (manaShare < RETREAT_SHARE || retreating && manaShare < RESUME_SHARE) return State.RETREAT;
        if (ownerHealthShare <= HURT_OWNER_SHARE && order != Order.STAY) return State.GUARD;
        if (order == Order.STAY) return State.STAY;
        return State.HUNT;
    }

    /** May he start or keep hunting in this state? */
    public static boolean mayHunt(State s) { return s == State.HUNT || s == State.GUARD || s == State.STAY; }

    /** The radius of his hunt around the wielder (or around himself for STAY) in blocks, capped at {@link #SCAN_CAP}. */
    public static double huntRadius(State s, Order o, int leash) {
        double r = switch (s) {
            case STAY -> STAY_GUARD;
            case GUARD -> GUARD_RADIUS;
            case RETREAT, RECALL -> 0.0;
            case HUNT -> o == Order.ATTACK ? leash + ATTACK_EXTRA : leash;
        };
        return Math.min(SCAN_CAP, Math.max(0.0, r));
    }

    /** Does the hunt measure from the wielder (true) or from the sword himself (false: STAY)? */
    public static boolean aroundOwner(State s) { return s != State.STAY; }

    /** Is this chase too far from the wielder to go on with? The leash is a soft line: slack past it, then he comes back. */
    public static boolean tooFar(double distanceToOwner, int leash, Order o) {
        double line = leash + FAR_SLACK + (o == Order.ATTACK ? ATTACK_EXTRA : 0.0);
        return distanceToOwner > line;
    }

    public static double followStart(State s) { return s == State.RETREAT || s == State.GUARD ? RETREAT_FOLLOW_START : FOLLOW_START; }

    /**
     * A flight that meets a wall: the velocity loses its part along the wall's normal and gains a lift, so he slides up and along the wall instead of stalling on it. {@code (nx, ny, nz)} is the unit normal of the
     * face that was hit. Returns {x, y, z}.
     */
    public static double[] deflect(double vx, double vy, double vz, double nx, double ny, double nz, double lift) {
        double dot = vx * nx + vy * ny + vz * nz;
        if (dot >= 0.0) return new double[]{vx, vy, vz};                                      // already moving away from it
        double ox = vx - dot * nx, oy = vy - dot * ny, oz = vz - dot * nz;
        boolean sideWall = Math.abs(ny) < 0.5;
        return new double[]{ox, sideWall ? oy + lift : oy, oz};
    }

    /** Worst cases for the docs: wall probes a second of one flying body, and goal evaluations of the pet goals. */
    public static double probesPerSecond() { return 20.0 / PROBE_GAP; }

    // ---------------------------------------------------------------- who may a pet attack
    /** Why a creature is a target. */
    public enum Reason { HOSTILE, OWNER_HIT, OWNER_ATTACKER }

    /**
     * May the pet attack this creature for this reason? A hostile creature, yes. What the wielder hit: only a hostile or a creature that is going for the wielder at the moment (never a peaceful animal the wielder
     * punched). What attacked the wielder: any creature that is not protected (a neutral one that hurt them has made itself an enemy). NEVER a player, a tamed creature, an ally or an NPC ({@code protectedKind}).
     */
    public static boolean mayAttack(Reason r, boolean hostile, boolean targetsWielder, boolean protectedKind) {
        if (protectedKind) return false;
        return switch (r) {
            case HOSTILE -> hostile;
            case OWNER_HIT -> hostile || targetsWielder;
            case OWNER_ATTACKER -> true;
        };
    }

    /** How long a target given by the wielder's blow or by an attacker stays his target, in ticks (the wolf's own window is 100). */
    public static final int OWNER_TARGET_TICKS = 160;

    /** Is a wielder's "last hurt" event still fresh? {@code stamp} and {@code now} are the wielder's tick counts. */
    public static boolean fresh(int stamp, int now) { return stamp > 0 && now - stamp >= 0 && now - stamp < OWNER_TARGET_TICKS; }
}
