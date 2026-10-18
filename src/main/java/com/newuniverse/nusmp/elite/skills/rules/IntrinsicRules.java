package com.newuniverse.nusmp.elite.skills.rules;

import com.newuniverse.nusmp.skill.rules.SkillMath;

/**
 * Why: the three Intrinsic skills (Kaioken, Mechanical Eye, Mechanical Hands) are body-bound: they come with the wearer and cost health or upkeep rather than a big
 * magicule bill. Their tables (Kaioken's multiplier ladder, the scan ranges, the reach bonuses) are Minecraft-free here so the ladder can be tested for monotonicity
 * and the "cannot kill yourself" guard can be checked without a server. Designed for this mod; the original addon's source was not available.
 */
public final class IntrinsicRules {
    private IntrinsicRules() {}

    // ======================================================================================= Kaioken
    /** A stackable multiplier toggle (x2, x3, x4, x10, x20) that burns health every second and leaves you spent when it ends. */
    public static final class Kaioken {
        private Kaioken() {}

        /** The classic ladder. Level index 0..4. */
        public static final int[] MULTIPLIER = {2, 3, 4, 10, 20};
        private static final double[] DAMAGE = {0.30, 0.60, 1.00, 2.00, 4.00};
        private static final double[] SPEED = {0.20, 0.35, 0.50, 0.80, 1.20};
        private static final double[] HEALTH_DRAIN = {0.010, 0.020, 0.030, 0.060, 0.100};   // share of max health per second
        private static final double[] MAGICULE_DRAIN = {20, 45, 80, 220, 500};

        public static int levels() { return MULTIPLIER.length; }

        /** How many rungs the wearer may climb: three, five once the skill is mastered. */
        public static int unlocked(boolean mastered) { return mastered ? 5 : 3; }

        public static int clampLevel(int level, boolean mastered) { return SkillMath.clamp(level, 0, unlocked(mastered) - 1); }

        public static int next(int level, boolean mastered, boolean reverse) {
            int n = unlocked(mastered);
            return Math.floorMod(level + (reverse ? -1 : 1), n);
        }

        public static int multiplier(int level) { return MULTIPLIER[SkillMath.clamp(level, 0, MULTIPLIER.length - 1)]; }

        /** Extra melee damage as a fraction (0.3 = +30%). */
        public static double damageBonus(int level) { return DAMAGE[SkillMath.clamp(level, 0, DAMAGE.length - 1)]; }

        public static double speedBonus(int level) { return SPEED[SkillMath.clamp(level, 0, SPEED.length - 1)]; }

        /** Hit points burnt per second at this rung. */
        public static double healthDrain(int level, double maxHealth) {
            return maxHealth * HEALTH_DRAIN[SkillMath.clamp(level, 0, HEALTH_DRAIN.length - 1)];
        }

        public static double magiculeDrain(int level) { return MAGICULE_DRAIN[SkillMath.clamp(level, 0, MAGICULE_DRAIN.length - 1)]; }

        /** The technique shuts itself off before it can kill: below 15% of max health or 4 hit points. */
        public static boolean canSustain(double health, double maxHealth) {
            return health > Math.max(4.0, maxHealth * 0.15);
        }

        /** After it ends: Weakness and Slowness for this long, longer from a higher rung. */
        public static int aftershockTicks(int level, int heldSeconds) {
            return Math.min(1200, (level + 1) * 40 + Math.min(heldSeconds, 30) * 4);
        }
    }

    // ======================================================================================= Mechanical Eye
    /** A scanning eye: reads the nearest foe out, and can mark one for extra damage. */
    public static final class MechanicalEye {
        private MechanicalEye() {}

        public static final double DRAIN_PER_SECOND = 25.0;
        public static final double MARK_COST = 200.0;
        public static final int MARK_COOLDOWN = 160;
        public static final int MARK_TICKS = 200;
        public static final double MARK_BONUS = 0.20;
        public static final int MAX_TRACKED = 16;

        public static double scanRange(boolean mastered) { return mastered ? 40.0 : 24.0; }

        public static double markRange(boolean mastered) { return mastered ? 48.0 : 32.0; }

        /** Damage multiplier against a marked target. */
        public static double markedMultiplier(boolean marked) { return marked ? 1.0 + MARK_BONUS : 1.0; }

        public static boolean markActive(long now, long until) { return now < until; }
    }

    // ======================================================================================= Mechanical Hands
    /** Arms that reach further, pull loot in, and mend a tool. */
    public static final class MechanicalHands {
        private MechanicalHands() {}

        public static final double EXTEND_COST = 250.0;
        public static final int EXTEND_COOLDOWN = 600;
        public static final double TELEKINESIS_COST = 120.0;
        public static final int TELEKINESIS_COOLDOWN = 100;
        public static final double MEND_COST = 150.0;
        public static final int MEND_COOLDOWN = 200;
        public static final int MAX_PULLED = 48;

        public static double blockReach(boolean mastered) { return mastered ? 4.5 : 3.0; }

        public static double entityReach(boolean mastered) { return mastered ? 2.25 : 1.5; }

        public static int buffTicks(boolean mastered) { return mastered ? 2400 : 1200; }

        public static double pullRadius(boolean mastered) { return mastered ? 18.0 : 12.0; }

        /** Speed of a pulled item toward the wearer, blocks per tick. */
        public static double pullSpeed(double distance) { return SkillMath.clamp(0.15 + distance * 0.03, 0.15, 0.6); }

        /** Durability points restored by one Mend on a tool with the given maximum. */
        public static int mendPoints(int maxDamage, int damage, boolean mastered) {
            if (maxDamage <= 0 || damage <= 0) return 0;
            int want = (int) Math.ceil(maxDamage * (mastered ? 0.25 : 0.15));
            return Math.min(damage, Math.max(1, want));
        }
    }
}
