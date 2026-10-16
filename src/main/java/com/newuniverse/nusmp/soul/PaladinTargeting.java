package com.newuniverse.nusmp.soul;

/**
 * Why: a Paladin has to defend its owner and nobody else's quarrel, and it must never wander off, never hit the owner, an ally or
 * another Paladin of the same owner. Which target is fair game and what the Paladin does about its distance from the owner are pure
 * decisions, so they are written once here (no Minecraft types) and used by all three drivers: the target goal for goal-driven mobs,
 * the per-tick drive for brain-driven ones, and the target-change event that redirects either. They are tested on their own.
 */
public final class PaladinTargeting {
    private PaladinTargeting() {}

    /** Walk back to the owner from this far when idle; always walk back from {@link #LEASH}; teleport from {@link #TELEPORT}. */
    public static final double FOLLOW_START = 8.0, LEASH = 24.0, TELEPORT = 32.0;
    /** A target farther than this from the owner is not worth chasing (it would pull the Paladin over the leash). */
    public static final double MAX_TARGET_FROM_OWNER = 28.0;
    /** A foe the owner struck or was struck by stays the Paladin's concern for this many ticks. */
    public static final int GRUDGE_TICKS = 160;

    /** What a Paladin may do about a candidate target. */
    public record Candidate(boolean alive, boolean isOwner, boolean allyOfOwner, boolean paladinOfSameOwner, boolean petOfOwner,
                            boolean creativeOrSpectator, boolean family, double distanceToOwner) {}

    public static boolean fairTarget(Candidate c) {
        if (!c.alive() || c.isOwner() || c.allyOfOwner() || c.paladinOfSameOwner() || c.petOfOwner() || c.creativeOrSpectator() || c.family()) return false;
        return c.distanceToOwner() <= MAX_TARGET_FROM_OWNER;
    }

    public enum Follow { NONE, WALK, RETURN, TELEPORT }

    /** What to do about the distance to the owner (blocks). RETURN also drops any target: the Paladin comes home first. */
    public static Follow follow(double distanceToOwner, boolean hasTarget, boolean sameLevel) {
        if (!sameLevel || distanceToOwner > TELEPORT) return Follow.TELEPORT;
        if (distanceToOwner > LEASH) return Follow.RETURN;
        if (!hasTarget && distanceToOwner > FOLLOW_START) return Follow.WALK;
        return Follow.NONE;
    }

    /**
     * Picks the target out of what the owner did lately: the foe that hurt the owner counts if the owner was hurt within the grudge
     * time, else the creature the owner last hit within it. Times are game ticks; a negative or zero stamp means never.
     *
     * @return 0 = no target, 1 = the one who attacked the owner, 2 = the one the owner attacked
     */
    public static int pick(long now, long hurtByStamp, boolean hurtByFair, long hurtStamp, boolean hurtFair) {
        if (hurtByFair && hurtByStamp > 0 && now - hurtByStamp <= GRUDGE_TICKS) return 1;
        if (hurtFair && hurtStamp > 0 && now - hurtStamp <= GRUDGE_TICKS) return 2;
        return 0;
    }
}
