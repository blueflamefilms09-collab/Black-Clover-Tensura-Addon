package com.newuniverse.nusmp.relics.wave;

import java.util.Locale;
import java.util.function.IntUnaryOperator;

/**
 * Wave 18: the maths and tables of the bags and charms, with no Minecraft types, so they are unit-tested. The tabletop's dice are passed in as
 * {@code roll(sides) -> 0..sides-1} (the way {@code RandomSource.nextInt} answers).
 */
public final class Wave18Pure {
    private Wave18Pure() {}

    // ---------------------------------------------------------------- the bags
    /** The seven Bountiful Bags: relic id, rows of nine slots, and the hazard the card puts on a creature inside. */
    public enum Bag {
        HOLDING(Wave18Tune.ID_HOLDING, 6, Hazard.NONE),
        DEVOURING(Wave18Tune.ID_DEVOURING, 3, Hazard.GULP),
        COLDING(Wave18Tune.ID_COLDING, 6, Hazard.COLD),
        GOLDING(Wave18Tune.ID_GOLDING, 1, Hazard.NONE),
        MOLDING(Wave18Tune.ID_MOLDING, 6, Hazard.POISON),
        SCOLDING(Wave18Tune.ID_SCOLDING, 6, Hazard.PSYCHIC),
        UNFOLDING(Wave18Tune.ID_UNFOLDING, 6, Hazard.NONE);

        private final String id;
        private final int rows;
        private final Hazard hazard;

        Bag(String id, int rows, Hazard hazard) { this.id = id; this.rows = rows; this.hazard = hazard; }

        public String id() { return id; }

        public int rows() { return rows; }

        public int slots() { return rows * 9; }

        public Hazard hazard() { return hazard; }

        /** The bag with this relic id, or null. */
        public static Bag byId(String id) {
            if (id == null) return null;
            for (Bag b : values()) if (b.id.equals(id)) return b;
            return null;
        }

        /** Is this relic id one of the seven bags (nothing of theirs may go inside another)? */
        public static boolean isBag(String id) { return byId(id) != null || Wave18Tune.ID_KITTENS.equals(id); }
    }

    /** What a creature stuffed in a bag suffers: nothing, cold, poison, a scolding, or the Devouring bag's gulp. */
    public enum Hazard { NONE, COLD, POISON, PSYCHIC, GULP }

    /** Does a foe fail the bag's save at this pulse? {@code roll01} is 0 to 1; knockback resistance (0..1) lowers the failure chance by up to half. */
    public static boolean failsSave(double roll01, double knockbackResistance) {
        return roll01 < failChance(knockbackResistance);
    }

    public static double failChance(double knockbackResistance) {
        double kb = Math.max(0.0, Math.min(1.0, knockbackResistance));
        return Math.max(Wave18Tune.SAVE_FLOOR, Wave18Tune.SAVE_FAIL * (1.0 - 0.5 * kb));
    }

    /** Does a creature of this size and health fit through the mouth of a bag? */
    public static boolean fitsBag(double width, double height, double maxHealth) {
        return width <= Wave18Tune.STUFF_MAX_WIDTH && height <= Wave18Tune.STUFF_MAX_HEIGHT && maxHealth <= Wave18Tune.STUFF_MAX_HEALTH;
    }

    /** The Devouring bag's gulp on a foe of this maximum health. */
    public static float gulp(double maxHealth) {
        return (float) Math.max(Wave18Tune.GULP_MIN, Math.min(Wave18Tune.GULP_MAX, maxHealth * Wave18Tune.GULP_FRACTION));
    }

    /** Does the Devouring bag manage to pull a creature in ("50 percent", less for a creature that braces: knockback resistance)? */
    public static boolean pulledIn(double roll01, double knockbackResistance) {
        double kb = Math.max(0.0, Math.min(1.0, knockbackResistance));
        return roll01 < 0.5 * (1.0 - kb);
    }

    // ---------------------------------------------------------------- gilding, composting, feeding (a day inside)
    /** What a worked metal becomes after a day in the Bag of Golding: a registry id, or null when the item is not gilded. Gold-coloured: gold nuggets. */
    public static String gildedOf(String itemId) {
        if (itemId == null) return null;
        return switch (itemId) {
            case "minecraft:iron_nugget" -> "minecraft:gold_nugget";
            case "minecraft:copper_ingot" -> "minecraft:gold_nugget";
            default -> null;
        };
    }

    /** How many gold nuggets one of these becomes (a copper ingot is nine nuggets of copper, three of gold). */
    public static int gildedCount(String itemId) { return "minecraft:copper_ingot".equals(itemId) ? 3 : 1; }

    /** Does the Bag of Golding hold this (a metal bar, nugget or coin)? Path words are checked by the glue; this is the name test for modded coins. */
    public static boolean coinLike(String path) {
        if (path == null) return false;
        String p = path.toLowerCase(Locale.ROOT);
        return p.contains("coin") || p.endsWith("_nugget") || p.endsWith("_ingot");
    }

    /** Bone meal a bag of rotted plant matter yields: one per {@link Wave18Tune#COMPOST_LEVELS_PER_MEAL} composter levels. */
    public static int boneMealFor(double levels) { return (int) Math.floor(Math.max(0.0, levels) / Wave18Tune.COMPOST_LEVELS_PER_MEAL); }

    /** The percent of maximum magicules a devoured stack feeds, under the day's cap. */
    public static double feedPercent(int itemsDevoured, int alreadyFedToday) {
        int room = Math.max(0, Wave18Tune.DEVOUR_FEED_DAILY - Math.max(0, alreadyFedToday));
        return Math.min(Math.max(0, itemsDevoured), room) * Wave18Tune.DEVOUR_FEED_PERCENT;
    }

    /** How many of the devoured items count towards the day's cap. */
    public static int feedCounted(int itemsDevoured, int alreadyFedToday) {
        return Math.min(Math.max(0, itemsDevoured), Math.max(0, Wave18Tune.DEVOUR_FEED_DAILY - Math.max(0, alreadyFedToday)));
    }

    /** At least one full day passed since the bag's contents last changed. */
    public static boolean dayPassed(long lastDay, long today) { return today > lastDay; }

    // ---------------------------------------------------------------- the Lover's Divide
    /** What the partner takes of a blow: half, but never the last of their health. */
    public static float partnerShare(float amount, float partnerHealth) {
        float half = Math.max(0f, amount) / 2f;
        return Math.max(0f, Math.min(half, partnerHealth - 1f));
    }

    // ---------------------------------------------------------------- Coin of Fortune
    /** A d20: even is the gold face (good fortune), odd the dark one. */
    public static boolean goldFace(int d20) { return d20 % 2 == 0; }

    /** The next blow dealt: x1.5 under good fortune, x0.6 under bad. */
    public static double fortuneOut(int fortune) { return fortune > 0 ? Wave18Tune.COIN_GOOD_ATTACK : fortune < 0 ? Wave18Tune.COIN_BAD_ATTACK : 1.0; }

    /** The next blow taken: x0.7 under good fortune, x1.3 under bad. */
    public static double fortuneIn(int fortune) { return fortune > 0 ? Wave18Tune.COIN_GOOD_OUT : fortune < 0 ? Wave18Tune.COIN_BAD_OUT : 1.0; }

    // ---------------------------------------------------------------- Defender's Dial
    /** The dial shows fire, then frost, then lightning, and starts over. */
    public static int dialType(int index) { return Math.floorMod(index, 3); }

    public static String dialDamageTag(int type) {
        return switch (dialType(type)) {
            case 0 -> "minecraft:is_fire";
            case 1 -> "minecraft:is_freezing";
            default -> "minecraft:is_lightning";
        };
    }

    public static String dialName(int type) { return dialType(type) == 0 ? "fire" : dialType(type) == 1 ? "frost" : "lightning"; }

    // ---------------------------------------------------------------- Bag of Kittens
    /** The kittens in a bag from the stored count (an unused bag holds a thousand). */
    public static int kittensIn(Integer stored) { return stored == null ? Wave18Tune.KITTENS_START : Math.max(0, Math.min(Wave18Tune.KITTENS_START, stored)); }

    /** The turns the flood lasts: 3d6 turns, as printed, but at least the minimum and (for the game's pace) at most 12 pulses. */
    public static int floodPulses(IntUnaryOperator roll) {
        int sum = 0;
        for (int i = 0; i < 3; i++) sum += 1 + roll.applyAsInt(6);
        return Math.max(Wave18Tune.FLOOD_PULSES_MIN, Math.min(12, sum));
    }

    /** Radius of the sea of kittens at pulse {@code n} (0-based): it grows each turn, to a size set by how many kittens are left in the bag. */
    public static double floodRadius(int kittens, int pulse) {
        double fill = Math.max(0.1, Math.min(1.0, kittens / (double) Wave18Tune.KITTENS_START));
        double cap = Wave18Tune.FLOOD_R0 + (Wave18Tune.FLOOD_R_MAX - Wave18Tune.FLOOD_R0) * fill;
        return Math.min(cap, Wave18Tune.FLOOD_R0 + Math.max(0, pulse) * Wave18Tune.FLOOD_R_GROWTH);
    }

    // ---------------------------------------------------------------- Dragon Satchel
    /** The powders: 0 none, 1 ash (fire), 2 sand (lightning), 3 grass (acid), 4 snow (cold). */
    public static int powderOf(String itemId) {
        if (itemId == null) return 0;
        return switch (itemId) {
            case "minecraft:charcoal", "minecraft:coal", "minecraft:soul_sand" -> 1;
            case "minecraft:sand", "minecraft:red_sand" -> 2;
            case "minecraft:short_grass", "minecraft:fern", "minecraft:tall_grass", "minecraft:large_fern", "minecraft:vine" -> 3;
            case "minecraft:snowball", "minecraft:snow_block", "minecraft:snow" -> 4;
            default -> 0;
        };
    }

    public static String powderName(int kind) {
        return switch (kind) { case 1 -> "ash"; case 2 -> "sand"; case 3 -> "grass"; case 4 -> "snow"; default -> "nothing"; };
    }

    public static String powderDamageType(int kind) {
        return switch (kind) { case 1 -> "minecraft:on_fire"; case 2 -> "minecraft:lightning_bolt"; case 3 -> "minecraft:indirect_magic"; case 4 -> "minecraft:freeze"; default -> "minecraft:magic"; };
    }

    /** The colour of a powder's cloud (0xRRGGBB): red, yellow, green, blue. */
    public static int powderColour(int kind) {
        return switch (kind) { case 1 -> 0xD8412B; case 2 -> 0xE8C93A; case 3 -> 0x6CCB3A; case 4 -> 0x6CB6F2; default -> 0xCCCCCC; };
    }

    /** {@code dice} d{@code sides}: 3d4 for a breath of powder. */
    public static int dice(int dice, int sides, IntUnaryOperator roll) {
        int sum = 0;
        for (int i = 0; i < dice; i++) sum += 1 + Math.max(0, Math.min(sides - 1, roll.applyAsInt(sides)));
        return sum;
    }

    /** Is a point {@code (dx, dz)} away from the thrower inside a cone along the unit direction {@code (lx, lz)}? */
    public static boolean inCone(double dx, double dz, double lx, double lz, double range, double cosHalf) {
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len > range) return false;
        if (len < 1.0e-6) return true;
        double ll = Math.sqrt(lx * lx + lz * lz);
        if (ll < 1.0e-6) return false;
        return (dx * lx + dz * lz) / (len * ll) >= cosHalf;
    }
}
