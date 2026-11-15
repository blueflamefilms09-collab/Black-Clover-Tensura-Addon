package com.newuniverse.nusmp.greed;

import java.util.Locale;

/**
 * Why: every number of the Black Sword Greed (Berserk of Gluttony) in one pure place (no game classes), so the forms, the tier thresholds, the stat arithmetic, the starvation clock and the skill sacrifice values cannot drift
 * apart and are unit-testable. The sword exists once per world ({@link GreedData}); it has eight forms (eight item ids, one active at a time) and eight tiers (0..7) that open the forms; the wielder grows by devouring
 * (kills), starves when the burden is high and the stomach empty, and pays a share of the devoured stats for a form's secret technique. Entries of {@link Form} are only ever appended (the form number is saved).
 */
public final class GreedRules {
    private GreedRules() {}

    public static final int FORMS = 8, MAX_TIER = 7;
    public static final String ID_PREFIX = "black_sword_greed";

    /** The eight forms: the number is the saved form id and the tier that opens it; {@code share} is the part of the devoured stats a secret technique costs. */
    public enum Form {
        SWORD(0, "", 0.10, "Black Sword", "Bloody Ptarmigan Cross"),
        BOW(1, "_bow", 0.15, "Black Bow", "Piercing Meteor"),
        SCYTHE(2, "_scythe", 0.20, "Black Scythe", "Soul Reap"),
        SHIELD(3, "_shield", 0.20, "Black Shield", "Reflection Fortress"),
        STAFF(4, "_staff", 0.40, "Black Staff", "Twilight Healing"),
        GAUNTLETS(5, "_gauntlets", 0.25, "Black Gauntlets", "Dimension Destruction"),
        SPEAR(6, "_spear", 0.30, "Black Spear", "Revolt Brionac"),
        DUAL_BLADES(7, "_dual_blades", 0.35, "Dual Blades, Level Zero", "Infinity Divide");

        private final int id;
        private final String suffix, english, technique;
        private final double share;

        Form(int id, String suffix, double share, String english, String technique) {
            this.id = id;
            this.suffix = suffix;
            this.share = share;
            this.english = english;
            this.technique = technique;
        }

        public int id() { return id; }
        /** The registry path of the form's item. */
        public String itemId() { return ID_PREFIX + suffix; }
        public double share() { return share; }
        public String english() { return english; }
        public String techniqueEnglish() { return technique; }
        public String langKey() { return "greed.form." + name().toLowerCase(Locale.ROOT); }
        public String techniqueKey() { return "greed.technique." + name().toLowerCase(Locale.ROOT); }

        public static Form byId(int id) { return id < 0 || id >= values().length ? SWORD : values()[id]; }
    }

    /** Stored EP at which tier 0..7 opens (the sword starts at 0, Level Zero needs two million). */
    private static final double[] TIER_EP = {0, 2_500, 10_000, 35_000, 100_000, 300_000, 750_000, 2_000_000};

    public static double tierThreshold(int tier) { return TIER_EP[Math.max(0, Math.min(MAX_TIER, tier))]; }

    /** The highest tier the stored EP has reached (0..7). */
    public static int tierFor(double storedEp) {
        int t = 0;
        for (int i = 1; i <= MAX_TIER; i++) if (storedEp >= TIER_EP[i]) t = i;
        return t;
    }

    /** A form is open when the tier has reached its number. */
    public static boolean formOpen(int form, int tier) { return form >= 0 && form < FORMS && form <= tier; }

    // ---------------------------------------------------------------- devouring
    /** Bonus caps: no number of kills makes the wielder unbounded. */
    public static final double MAX_HP = 200.0, MAX_ATK = 60.0, MAX_ARMOR = 30.0;

    /** A kill absorbs this share of the victim's EP (double in Half-Starvation). */
    public static double absorbShare(boolean half) { return half ? 0.30 : 0.15; }

    /** Max-health, attack and armour gained from absorbing {@code ep} (square-root growth: a dragon is worth more than a zombie, not a million zombies). */
    public static double[] gains(double ep) {
        double r = Math.sqrt(Math.max(0.0, ep));
        return new double[] {Math.min(6.0, 0.06 * r), Math.min(2.5, 0.02 * r), Math.min(1.5, 0.01 * r)};
    }

    /** Satiation (0..100) restored by absorbing {@code ep}. */
    public static float feed(double ep) { return (float) Math.min(25.0, 3.0 + Math.sqrt(Math.max(0.0, ep)) * 0.05); }

    /** The weight of the devoured stats: what a secret technique's damage and the stored EP are measured in. */
    public static double power(double hp, double atk, double armor) { return hp * 0.5 + atk * 2.5 + armor * 1.5; }

    /** The EP a sacrificed share of the stats is worth. */
    public static double epOfPower(double power) { return power * 10.0; }

    public static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    // ---------------------------------------------------------------- hunger
    public static final float STARVING_BELOW = 20.0f;

    /** Satiation lost per second: the burden (devoured stats) makes the hunger worse; Half-Starvation makes it 2.5 times worse; the top tier (Level Zero) is never hungry. */
    public static double decayPerSecond(double hp, double atk, double armor, boolean half, int tier, double percent) {
        if (tier >= MAX_TIER) return 0.0;
        double burden = power(hp, atk, armor);
        double d = 0.03 * (1.0 + burden / 120.0) * (half ? 2.5 : 1.0);
        return Math.min(2.0, d) * percent / 100.0;
    }

    public static boolean starving(float satiation, int tier) { return tier < MAX_TIER && satiation < STARVING_BELOW; }

    // ---------------------------------------------------------------- the vault
    /** EP a sacrificed skill is worth, by its Tensura type name (INTRINSIC, COMMON, EXTRA, UNIQUE, ULTIMATE, anything else). */
    public static double sacrificeEp(String typeName) {
        return switch (typeName == null ? "" : typeName.toUpperCase(Locale.ROOT)) {
            case "INTRINSIC" -> 1_500.0;
            case "COMMON" -> 3_000.0;
            case "EXTRA" -> 12_000.0;
            case "UNIQUE" -> 75_000.0;
            case "ULTIMATE" -> 250_000.0;
            default -> 2_500.0;
        };
    }

    public static final int VAULT_MAX = 200;
    public static final float SACRIFICE_FEED = 25.0f;
    /** Chance that a kill steals a skill into the vault: always from a player, this share from a mob. */
    public static final double STEAL_MOB = 0.35;

    // ---------------------------------------------------------------- the world's lock
    public static final long INACTIVITY_MS = 14L * 24L * 60L * 60L * 1000L;
    public static final int SWITCH_GAP_TICKS = 40, TECHNIQUE_GAP_TICKS = 60, MAX_TARGETS = 16;

    /** True when the form is one of the item ids of the sword. */
    public static boolean isGreedPath(String path) { return path != null && path.startsWith(ID_PREFIX); }
}
