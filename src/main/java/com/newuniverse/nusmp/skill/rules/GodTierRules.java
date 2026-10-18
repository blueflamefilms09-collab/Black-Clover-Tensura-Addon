package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Instant Death, The Almighty and The Visionary are standalone reality-bending Ultimates (admin and event rewards). Their balance
 * knobs (costs, cooldowns, radii, who may be instantly killed) are in one pure class so the god-tier balance is readable at a glance and
 * covered by tests, while the config [god_tier] section decides bosses and players.
 */
public final class GodTierRules {
    private GodTierRules() {}

    // ---- Instant Death (Yogiri) ----
    public static final int DIE = 0, MASS = 1;
    public static double dieCost() { return 3000; }
    public static int dieCooldown(boolean mastered) { return mastered ? 200 : 400; }
    public static double dieRange() { return 48; }
    public static double massCost() { return 8000; }
    public static int massCooldown() { return 3600; }
    public static double massRadius(boolean mastered) { return mastered ? 24 : 16; }

    /** Outcome of striking a target: killed outright, or only a share of max health removed. */
    public static boolean kills(boolean fullyAffected) { return fullyAffected; }

    // ---- The Almighty (Yhwach) ----
    public static double almightyCost() { return 5000; }
    public static int almightyCooldown() { return 3000; }
    public static int rewriteTicks(boolean mastered) { return mastered ? 160 : 100; }
    /** The configured dodge chance, one and a half times as high when mastered, never above 0.95 (a fraction of futures must still hit). */
    public static double dodgeChance(double configured, boolean mastered) {
        return SkillMath.clamp(configured * (mastered ? 1.5 : 1.0), 0.0, 0.95);
    }

    // ---- The Visionary (Gremmy) ----
    public static final int METEOR = 0, VOID = 1, BODY = 2, MODES = 3;
    public static double meteorCost() { return 4000; }
    public static int meteorCooldown() { return 600; }
    public static double meteorRadius(boolean m) { return m ? 8 : 6; }
    public static double meteorDamage(boolean m) { return m ? 50 : 35; }
    public static float meteorBlast(boolean m) { return m ? 6.0F : 4.0F; }
    public static double voidCost() { return 3500; }
    public static int voidCooldown() { return 700; }
    public static double voidRadius(boolean m) { return m ? 9 : 6; }
    public static int voidWitherTicks() { return 160; }
    public static double bodyCost() { return 3000; }
    public static int bodyCooldown() { return 3600; }
    public static int bodyTicks(boolean m) { return m ? 2400 : 1200; }
}
