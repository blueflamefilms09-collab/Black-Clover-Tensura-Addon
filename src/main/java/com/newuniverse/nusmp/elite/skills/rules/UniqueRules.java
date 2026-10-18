package com.newuniverse.nusmp.elite.skills.rules;

import com.newuniverse.nusmp.skill.rules.SkillMath;

/**
 * Why: the three Unique skills (Crocodile Skin, Hecate, Void Edge) each have a rule that is worth testing on its own: Crocodile's armour that thickens in water, Hecate's
 * moon-phase power for three forms, and Void Edge's armour-ignoring share of a blow. Pure functions here, game calls in the skill classes. Designed for this mod; the original
 * addon's source was not available.
 */
public final class UniqueRules {
    private UniqueRules() {}

    // ======================================================================================= Crocodile Skin
    public static final class Crocodile {
        private Crocodile() {}

        public static final double ROLL_COST = 500.0;
        public static final int ROLL_COOLDOWN = 400;
        public static final double ROLL_RANGE = 6.0;
        public static final int ROLL_HITS = 3;
        public static final int ROLL_INTERVAL = 10;
        public static final double ROLL_DAMAGE = 9.0;
        public static final double ROLL_HEAL = 0.25;

        /** Share of ordinary (not magic, not void) damage that bounces off the hide. */
        public static double reduction(boolean mastered, boolean inWater) {
            double r = mastered ? 0.30 : 0.20;
            return inWater ? r + 0.15 : r;
        }

        /** In water the croc hits harder: melee multiplier. */
        public static double waterDamage(boolean inWater, boolean mastered) { return inWater ? (mastered ? 1.35 : 1.25) : 1.0; }

        public static int bindTicks(boolean mastered) { return mastered ? 50 : 40; }
    }

    // ======================================================================================= Hecate
    public static final class Hecate {
        private Hecate() {}

        /** The three faces: the Maiden waxes, the Mother is full, the Crone wanes into the new moon. */
        public enum Form { MAIDEN, MOTHER, CRONE }

        public static final int FORMS = 3;

        public static Form form(int mode) { return Form.values()[SkillMath.clamp(mode, 0, FORMS - 1)]; }

        public static double cost(Form f) {
            return switch (f) { case MAIDEN -> 450.0; case MOTHER -> 800.0; case CRONE -> 650.0; };
        }

        public static int cooldown(Form f) {
            return switch (f) { case MAIDEN -> 120; case MOTHER -> 400; case CRONE -> 300; };
        }

        /**
         * Power multiplier of a form for the vanilla moon phase (0 full, 1 waning gibbous, 2 last quarter, 3 waning crescent, 4 new, 5 waxing crescent, 6 first quarter,
         * 7 waxing gibbous). Each form peaks at its own phase; the moon is weaker by day.
         */
        public static double moonPower(Form f, int phase, boolean night) {
            int p = Math.floorMod(phase, 8);
            int peak = switch (f) { case MAIDEN -> 6; case MOTHER -> 0; case CRONE -> 4; };
            int dist = Math.min(Math.floorMod(p - peak, 8), Math.floorMod(peak - p, 8));
            double base = switch (dist) { case 0 -> 1.4; case 1 -> 1.2; case 2 -> 1.0; default -> 0.85; };
            return night ? base : base * 0.7;
        }

        /** Maiden: raw damage of the Moonbolt. */
        public static double boltDamage(boolean mastered) { return mastered ? 22.0 : 16.0; }

        public static double boltRange(boolean mastered) { return mastered ? 48.0 : 36.0; }

        /** Mother: share of max health healed. */
        public static double healShare(boolean mastered) { return mastered ? 0.40 : 0.30; }

        public static double blessRadius(boolean mastered) { return mastered ? 14.0 : 10.0; }

        /** Crone: the hex's radius and the length of the lost-in-the-crossroads state. */
        public static double hexRadius(boolean mastered) { return mastered ? 12.0 : 9.0; }

        public static int hexTicks(boolean mastered) { return mastered ? 120 : 80; }

        public static int veilTicks() { return 80; }
    }

    // ======================================================================================= Void Edge
    public static final class VoidEdge {
        private VoidEdge() {}

        public static final double IMBUE_COST = 450.0;
        public static final int IMBUE_COOLDOWN = 600;
        public static final double SLASH_COST = 350.0;
        public static final int SLASH_COOLDOWN = 120;
        public static final double SLASH_RANGE = 10.0;
        public static final double SLASH_WIDTH = 1.6;
        public static final double SLASH_DAMAGE = 14.0;
        public static final int SHIELD_BREAK_TICKS = 100;
        public static final int SHRED_TICKS = 120;
        public static final double SHRED = 0.30;

        public static int imbueTicks(boolean mastered) { return mastered ? 600 : 400; }

        /**
         * Share of a blow that is dealt again as void damage, which ignores armour. The more armour the target wears, the more the edge adds (so it is a counter to
         * defences, not a flat boost): 15% base, +2% per armour point, at most 60%.
         */
        public static double voidShare(double armor, boolean mastered) {
            double s = 0.15 + 0.02 * Math.max(0.0, armor);
            if (mastered) s += 0.05;
            return SkillMath.clamp(s, 0.0, 0.60);
        }

        public static boolean active(long now, long until) { return now < until; }
    }
}
