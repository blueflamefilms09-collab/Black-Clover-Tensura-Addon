package com.newuniverse.nusmp.elite.skills.rules;

import com.newuniverse.nusmp.skill.rules.SkillMath;

/**
 * Why: the Ultimate skills are the strongest things a player can own, so their numbers must be readable in one place and testable: Basilisk Skin's hide and gaze, Beelzebuth's
 * hunger, Hephaestus' reforge, Ouroboros' rewind and cycle, Rapheal's prediction, Void Sovereign's domain. (Gaia, the biggest, has its own classes.) Designed for this mod
 * from the roles of these names in Tensura; the original addon's source was not available. {@code ownMastered} is true once the Ultimate ITSELF is mastered, which adds
 * the shared {@link com.newuniverse.nusmp.skill.rules.Tier#ULTIMATE_MASTERY_BONUS} on top of the table (callers apply it).
 */
public final class UltimateRules {
    private UltimateRules() {}

    // ======================================================================================= Basilisk Skin
    public static final class Basilisk {
        private Basilisk() {}

        public static final double GAZE_COST = 900.0;
        public static final int GAZE_COOLDOWN = 360;
        public static final double GAZE_RANGE = 14.0;
        public static final double GAZE_DOT = 0.85;
        public static final double GAZE_DAMAGE = 10.0;
        public static final double MOLT_COST = 1500.0;
        public static final int MOLT_COOLDOWN = 1800;

        /** Share of ordinary damage the scales turn aside. */
        public static double reduction(boolean ownMastered) { return ownMastered ? 0.42 : 0.35; }

        /** Share of a melee blow reflected to the attacker as poison damage. */
        public static double reflect(boolean ownMastered) { return ownMastered ? 0.25 : 0.18; }

        /** Petrified for this long (players get the shorter, capped time). */
        public static int stoneTicks(boolean player) { return player ? 40 : 100; }

        /** Absorption hearts' worth (share of max health) a molt leaves. */
        public static double moltShield(boolean ownMastered) { return ownMastered ? 0.5 : 0.4; }
    }

    // ======================================================================================= Beelzebuth
    public static final class Beelzebuth {
        private Beelzebuth() {}

        public static final double DEVOUR_COST = 700.0;
        public static final int DEVOUR_COOLDOWN = 160;
        public static final double DEVOUR_RANGE = 6.0;
        public static final double DEVOUR_DAMAGE = 24.0;
        public static final double MAW_COST = 1400.0;
        public static final int MAW_COOLDOWN = 600;
        public static final double MAW_RADIUS = 11.0;
        public static final double MAW_PULL = 0.55;
        public static final double MAW_DAMAGE = 14.0;
        public static final double DOMAIN_COST = 2200.0;
        public static final int DOMAIN_COOLDOWN = 1200;
        public static final double DOMAIN_RADIUS = 8.0;
        public static final int DOMAIN_PULSES = 6;
        /** Share of the target's MAX health eaten by one pulse of the Domain (capped for players by BalanceLaw). */
        public static final double DOMAIN_BITE = 0.04;
        public static final int STACK_CAP = 10;
        public static final int STACK_TICKS = 1200;

        /** Share of dealt damage returned as health by Devour. */
        public static double devourLifesteal(boolean ownMastered) { return ownMastered ? 0.6 : 0.5; }

        /** Share of max magicules restored by a Devour. */
        public static double devourMagicule(boolean ownMastered) { return ownMastered ? 0.08 : 0.05; }

        /** Hunger stacks gained by a kill (a boss feeds three). */
        public static int stacksForKill(boolean boss) { return boss ? 3 : 1; }

        /** Damage multiplier at this many Hunger stacks. */
        public static double hungerBonus(int stacks) { return 1.0 + 0.04 * SkillMath.clamp(stacks, 0, STACK_CAP); }

        /** Share of max health healed by a kill, and of max magicules returned. */
        public static double killHeal(boolean boss) { return boss ? 0.30 : 0.10; }

        public static double killMagicule(boolean boss) { return boss ? 0.15 : 0.05; }
    }

    // ======================================================================================= Hephaestus (Ultimate)
    public static final class Hephaestus {
        private Hephaestus() {}

        public static final double REFORGE_COST = 3500.0;
        public static final int REFORGE_COOLDOWN = 2400;
        public static final double TEMPER_COST = 1200.0;
        public static final int TEMPER_COOLDOWN = 1200;
        public static final double ANVIL_COST = 1600.0;
        public static final int ANVIL_COOLDOWN = 400;
        public static final double ANVIL_RANGE = 24.0;
        public static final double ANVIL_RADIUS = 3.5;
        public static final double ANVIL_DAMAGE = 30.0;

        public static int temperTicks(boolean ownMastered) { return ownMastered ? 24000 : 12000; }

        /** Extra melee damage of a tempered weapon. */
        public static double temperBonus(boolean ownMastered) { return ownMastered ? 0.30 : 0.20; }

        /** Durability points a Reforge restores on a stack: all of it. */
        public static int reforgePoints(int damage) { return Math.max(0, damage); }

        public static boolean tempered(long now, long until) { return now < until; }
    }

    // ======================================================================================= Ouroboros
    public static final class Ouroboros {
        private Ouroboros() {}

        public static final int RING_CAPACITY = 16;
        public static final int SAMPLE_GAP = 20;
        public static final double REWIND_COST = 2000.0;
        public static final int REWIND_COOLDOWN = 2400;
        public static final double CYCLE_COST = 3000.0;
        public static final int CYCLE_COOLDOWN = 12000;
        /** The automatic rewind when a killing blow lands. */
        public static final int AUTO_COOLDOWN = 6000;

        /** How far back the rewind goes. */
        public static int rewindTicks(boolean ownMastered) { return ownMastered ? 300 : 200; }

        /** Health after the rewind: the remembered health, but never below this share of max (a rewind from near death must not just return you to near death). */
        public static double restoredHealth(double remembered, double max) { return Math.max(remembered, max * 0.5); }

        /** Magicules after a Cycle: refilled to this share of the maximum, if lower. */
        public static double cycleMagiculeShare(boolean ownMastered) { return ownMastered ? 0.75 : 0.5; }

        /** Share of every other skill's remaining cooldown a Cycle wipes. */
        public static double cycleCooldownCut() { return 1.0; }
    }

    // ======================================================================================= Rapheal
    public static final class Rapheal {
        private Rapheal() {}

        public static final double ANALYZE_COST = 600.0;
        public static final int ANALYZE_COOLDOWN = 160;
        public static final double ANALYZE_RANGE = 40.0;
        public static final int MARK_TICKS = 400;
        public static final double MARK_BONUS = 0.15;
        public static final double ACCEL_COST = 1800.0;
        public static final int ACCEL_COOLDOWN = 1200;
        public static final int ACCEL_TICKS = 200;
        public static final double DISSECT_COST = 1500.0;
        public static final int DISSECT_COOLDOWN = 500;
        public static final double DISSECT_RANGE = 20.0;
        public static final int PREDICT_GAP_TICKS = 20;

        /** Chance that an incoming blow is foreseen and turned aside (never against void / kill damage). */
        public static double predictChance(boolean ownMastered, boolean accelerated) {
            double c = ownMastered ? 0.25 : 0.18;
            return accelerated ? c + 0.15 : c;
        }

        /** True if the prediction fires: roll in [0,1), at most one per {@link #PREDICT_GAP_TICKS}. */
        public static boolean predicts(double roll, double chance, long now, long lastPrediction) {
            return roll < chance && now - lastPrediction >= PREDICT_GAP_TICKS;
        }

        /** Share of every other skill's cooldown that runs down per second while accelerated. */
        public static double accelCooldownRate() { return 0.25; }
    }

    // ======================================================================================= Void Sovereign
    public static final class VoidSovereign {
        private VoidSovereign() {}

        public static final double DOMAIN_COST = 2500.0;
        public static final int DOMAIN_COOLDOWN = 1200;
        public static final double STEP_COST = 800.0;
        public static final int STEP_COOLDOWN = 160;
        public static final double STEP_RANGE = 20.0;
        public static final double STEP_WIDTH = 1.6;
        public static final double STEP_DAMAGE = 12.0;
        public static final double COLLAPSE_COST = 3200.0;
        public static final int COLLAPSE_COOLDOWN = 1600;
        public static final double COLLAPSE_RADIUS = 7.0;
        /** Share of max health a Collapse takes from a foe, with a floor; bosses lose only a fifth of it. */
        public static final double COLLAPSE_SHARE = 0.12;
        public static final double COLLAPSE_FLOOR = 20.0;

        public static double domainRadius(boolean ownMastered) { return ownMastered ? 12.0 : 10.0; }

        public static int domainTicks(boolean ownMastered) { return ownMastered ? 300 : 200; }

        public static double collapseDamage(double maxHealth, boolean boss) {
            double d = Math.max(COLLAPSE_FLOOR, maxHealth * COLLAPSE_SHARE);
            return boss ? d * 0.2 : d;
        }

        public static boolean inside(double dx, double dy, double dz, double radius) {
            return dx * dx + dy * dy + dz * dz <= radius * radius;
        }
    }
}
