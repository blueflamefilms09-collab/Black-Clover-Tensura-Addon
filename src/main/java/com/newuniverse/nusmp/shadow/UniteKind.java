package com.newuniverse.nusmp.shadow;

/**
 * Why: Nacht Faust's four Devil Unite modes (Equus/Slotos, Canis/Gimodelo, Felis/Plumede, Gallus/Walgner) share one stat table, and balance
 * numbers must not be scattered through event handlers. This pure enum is that table: the attribute changes a mode applies, how much
 * physical damage it takes off, its dodge, its mob-stealth range and how long it lasts. The server code reads it; the tests pin it.
 *
 * <p>Order is append-only (the ordinal is stored in the player's data). The matching armour overlay is {@code ModeArmor.Mode.<overlay>}.
 */
public enum UniteKind {
    /** Slotos, the horse devil: a walking fortress. */
    EQUUS("equus", "Slotos", "DEVIL_EQUUS", 10, 6, 0.40, -0.25, 1.0, 0.0, 0.0, 0.0, 0.0, 0, 0),
    /** Gimodelo, the dog devil: a pack and spikes. */
    CANIS("canis", "Gimodelo", "DEVIL_CANIS", 0, 0, 0.0, 0.20, 0.0, 0.0, 0.0, 0.25, 0.0, 0, 0),
    /** Plumede, the cat devil: agility, sight and stealth. */
    FELIS("felis", "Plumede", "DEVIL_FELIS", 0, 0, 0.0, 0.30, 0.0, 0.25, 3.0, 0.0, 0.20, 8, 24),
    /** Walgner, the rooster devil: the stunning call. */
    GALLUS("gallus", "Walgner", "DEVIL_GALLUS", 0, 0, 0.0, 0.15, 0.0, 0.30, 6.0, 0.0, 0.0, 0, 0);

    /** Every mode lasts 30 seconds, like the quick Unite buffs. */
    public static final int TICKS = 600;
    /** No stat table entry may take off more physical damage than this, or dodge more than {@link #MAX_DODGE}, or slow by more than {@link #MIN_SPEED}. */
    public static final double MAX_REDUCTION = 0.60, MAX_DODGE = 0.25, MIN_SPEED = -0.50;

    public final String id, devil, overlay;
    /** Flat armour and armour toughness added. */
    public final int armor, toughness;
    /** Share of PHYSICAL damage removed (0..{@link #MAX_REDUCTION}). */
    public final double physicalReduction;
    /** Movement speed as a share of the base (-0.25 = 25 % slower), jump strength share, safe fall distance in blocks, attack damage share. */
    public final double speed, knockbackResistance, jump, safeFall, attack;
    /** Chance to cancel a hit. */
    public final double dodge;
    /** Mobs further than this (blocks) lose the wearer as a target; 0 = no stealth. */
    public final int lonerRange;
    /** Outline range (blocks) of living things shown to the wearer; 0 = none. */
    public final int sightRange;

    UniteKind(String id, String devil, String overlay, int armor, int toughness, double physicalReduction, double speed, double knockbackResistance,
              double jump, double safeFall, double attack, double dodge, int lonerRange, int sightRange) {
        this.id = id;
        this.devil = devil;
        this.overlay = overlay;
        this.armor = armor;
        this.toughness = toughness;
        this.physicalReduction = physicalReduction;
        this.speed = speed;
        this.knockbackResistance = knockbackResistance;
        this.jump = jump;
        this.safeFall = safeFall;
        this.attack = attack;
        this.dodge = dodge;
        this.lonerRange = lonerRange;
        this.sightRange = sightRange;
    }

    /** The mode stored as {@code ordinal}, or null for anything else (no mode, a value from the future). */
    public static UniteKind of(int ordinal) {
        UniteKind[] v = values();
        return ordinal >= 0 && ordinal < v.length ? v[ordinal] : null;
    }

    /** The damage left after the mode: only physical damage is reduced, never below zero. */
    public double afterReduction(double damage, boolean physical) {
        if (!physical || damage <= 0) return Math.max(0, damage);
        return damage * (1.0 - Math.max(0, Math.min(MAX_REDUCTION, physicalReduction)));
    }

    /** Does a hit from an attacker (a boss attacks at a reduced dodge chance) miss the wearer, given a roll in [0,1)? */
    public boolean dodges(double roll, boolean attackerIsBoss, double bossDodge) {
        double chance = Math.min(MAX_DODGE, dodge);
        if (attackerIsBoss) chance = Math.min(chance, Math.max(0, bossDodge));
        return chance > 0 && roll < chance;
    }

    /** Does a mob at {@code distance} blocks lose the wearer as a target? */
    public boolean mobLosesTarget(double distance) {
        return lonerRange > 0 && distance > lonerRange;
    }
}
