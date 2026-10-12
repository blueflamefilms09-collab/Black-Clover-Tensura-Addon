package com.newuniverse.nusmp.entity.zogratis;

/**
 * The numbers of a Dark Disciple (pure). A disciple is infused with up to 40% of its devil's power: its share is
 * {@code 0.4 x the owner's power fraction at spawn} ({@link PowerCurve#discipleShare}), and every stat grows from that share.
 */
public final class DiscipleStats {
    private DiscipleStats() {}

    public static final double MAX_SHARE = 0.4;

    /** 0..1: how much of the maximum infusion this share is. */
    public static double fill(double share) { return Math.max(0, Math.min(1, share / MAX_SHARE)); }

    public static double health(double share) { return 60 + 240 * fill(share); }
    public static double attack(double share) { return 4 + 10 * fill(share); }
    public static double speed(double share) { return 0.24 + 0.06 * fill(share); }
    public static double armor(double share) { return 2 + 10 * fill(share); }

    /** Ticks between two uses of a disciple's devil ability. */
    public static int abilityCooldown(double share) { return (int) Math.round(160 - 70 * fill(share)); }

    /** Damage of the ability, in multiples of the disciple's attack. */
    public static double abilityDamage(double share) { return 1.2 + 0.8 * fill(share); }
}
