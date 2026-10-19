package com.newuniverse.nusmp.compat.create;

/**
 * Why: what a band of a trap does to someone caught in it, apart from the family that dresses it. Each kind has its own sound and particles in the runtime, its damage
 * type (a Create damage type when the mod has one, a vanilla one otherwise), a base damage scale and how hard it throws the victim out of the band. Pure.
 */
public enum HazardKind {
    /** A mechanical saw lashing across the lane. */
    SLASH("slash", 1.0, 0.55, false),
    /** The press: two rams closing from the walls. */
    CRUSH("crush", 1.5, 0.75, false),
    /** A wall of drills lunging. */
    BORE("bore", 1.2, 0.5, false),
    /** A spout pouring molten metal. Sets the victim alight. */
    SCALD("scald", 0.8, 0.2, true),
    /** The sequence's discharge between the nixie tubes. */
    SHOCK("shock", 0.9, 0.6, false);

    private final String id;
    private final double damageScale, throwStrength;
    private final boolean fire;

    HazardKind(String id, double damageScale, double throwStrength, boolean fire) {
        this.id = id;
        this.damageScale = damageScale;
        this.throwStrength = throwStrength;
        this.fire = fire;
    }

    public String id() { return id; }

    /** Multiplier of the depth-scaled base damage. */
    public double damageScale() { return damageScale; }

    /** Horizontal speed (blocks per tick) the victim is thrown out of the band with. */
    public double throwStrength() { return throwStrength; }

    /** Does a hit set the victim on fire for a few seconds? */
    public boolean fire() { return fire; }
}
