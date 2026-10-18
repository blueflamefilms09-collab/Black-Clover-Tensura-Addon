package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.skill.rules.SkillMath;

/**
 * Why: Gaia is the strongest earth skill in the pack, so what it costs must be one readable table: the price to raise the domain, the upkeep per second while it stands (it grows with the
 * domain's size and with how many pillars and blooms are being paid for), and the war surcharge when enemies of the owner's nation stand inside. Minecraft-free so JUnit pins the
 * relations ("a bigger domain costs more", "war costs more", "nothing is ever free or negative"). Designed for this mod; the original addon's source was not available.
 */
public final class GaiaCost {
    private GaiaCost() {}

    public static final double RAISE_BASE = 2500.0;
    public static final double UPKEEP_BASE = 30.0;
    public static final double PILLARS_BASE = 900.0;
    public static final double BLOOM_COST = 1500.0;
    public static final double WAR_SURCHARGE = 0.5;
    public static final int RAISE_COOLDOWN = 900;
    public static final int PILLARS_COOLDOWN = 300;
    public static final int BLOOM_COOLDOWN = 200;

    /** Radius of the domain in blocks: 14, or 20 once the Ultimate itself is mastered. */
    public static double radius(boolean ownMastered) { return ownMastered ? 20.0 : 14.0; }

    /** How long a raised domain stands before it fades (ticks). */
    public static int durationTicks(boolean ownMastered) { return ownMastered ? 4800 : 2400; }

    /** Price to raise a domain of this radius. */
    public static double raise(double radius) { return RAISE_BASE + 3.0 * Math.max(0.0, radius) * Math.max(0.0, radius); }

    /** Magicules per second while the domain stands. {@code war} adds the surcharge. */
    public static double upkeepPerSecond(double radius, int livePillars, boolean blooming, boolean war) {
        double u = UPKEEP_BASE + 3.0 * Math.max(0.0, radius) + 4.0 * Math.max(0, livePillars) + (blooming ? 20.0 : 0.0);
        return war ? u * (1.0 + WAR_SURCHARGE) : u;
    }

    /** Price of one pillar volley of {@code count} pillars. */
    public static double pillars(int count) { return PILLARS_BASE + 120.0 * Math.max(0, count - 3); }

    /** The whole price of a bloom (magicules); the ledger points are charged separately. */
    public static double bloom() { return BLOOM_COST; }

    /** Whatever the inputs, a price is at least 1 and finite. */
    public static double sane(double price) { return Double.isFinite(price) ? SkillMath.clamp(price, 1.0, 1.0e9) : 1.0; }
}
