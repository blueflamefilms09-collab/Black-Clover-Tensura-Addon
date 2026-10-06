package com.newuniverse.nusmp.multiverse;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/** Server config for the Multiverse systems: world/serverconfig/nusmp-multiverse-server.toml */
public final class MultiverseConfig {
    public static final ModConfigSpec SPEC;

    // ceremony
    public static final ModConfigSpec.IntValue CEREMONY_MONTH;
    public static final ModConfigSpec.BooleanValue CEREMONY_REAL_DATE;
    public static final ModConfigSpec.IntValue CEREMONY_TOWER_RADIUS;
    public static final ModConfigSpec.BooleanValue EVERYONE_ELIGIBLE;
    public static final ModConfigSpec.BooleanValue LEGACY_SOUL_AUTO_ROLL;
    public static final ModConfigSpec.DoubleValue WEIGHT_COMMON, WEIGHT_UNCOMMON, WEIGHT_RARE, WEIGHT_BLACK, WEIGHT_GOD;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> STARTER_MAGICS;
    // pages
    public static final ModConfigSpec.IntValue PAGE_SLOTS_FLOOR, PAGE_SLOTS_CEILING, PAGE_SLOTS_CONTRACT_BONUS, PAGE_SLOTS_CAP;
    public static final ModConfigSpec.DoubleValue PAGE_MASTERY_SCALE;
    public static final ModConfigSpec.ConfigValue<String> GATE_ZONE, GATE_SIGNATURE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PAGE_GATES;
    public static final ModConfigSpec.IntValue ALTAR_TRAINING_MASTERY, ALTAR_TRAINING_COOLDOWN, CHANNEL_MASTERY_TICKS;
    public static final ModConfigSpec.DoubleValue MASTERY_PER_GOLD_STAR;
    // ranks & stars
    public static final ModConfigSpec.BooleanValue AUTO_PROMOTE;
    public static final ModConfigSpec.IntValue STARS_PER_CLASS;
    // squads
    public static final ModConfigSpec.ConfigValue<String> SQUAD_MIN_RANK_CREATE, SQUAD_MIN_RANK_JOIN;
    public static final ModConfigSpec.IntValue SQUAD_MAX_SIZE;
    public static final ModConfigSpec.BooleanValue SQUAD_CAPTAIN_PROMOTES_VICE, SQUAD_GOLD_FEEDS_SCORE, SQUAD_BLACK_REDUCES_SCORE, SQUAD_OFFLINE_COUNT;
    // spirit lord
    public static final ModConfigSpec.DoubleValue SPIRIT_PHYSICAL_REDUCTION;
    public static final ModConfigSpec.BooleanValue SPIRIT_FALL_IMMUNE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("ceremony");
        CEREMONY_MONTH = b.comment("Month (1-12) of the annual Grimoire Acceptance Ceremony. Default March.").defineInRange("month", 3, 1, 12);
        CEREMONY_REAL_DATE = b.comment("Run the ceremony automatically during that real-world month (otherwise only /multiverse ceremony start).")
                .define("useRealDate", true);
        CEREMONY_TOWER_RADIUS = b.comment("How close (blocks) an eligible player must be to a Grimoire Tower to be chosen.").defineInRange("towerRadius", 48, 8, 256);
        EVERYONE_ELIGIBLE = b.comment("Every player without a grimoire is eligible (otherwise only players flagged eligible by admins).").define("everyoneEligible", true);
        LEGACY_SOUL_AUTO_ROLL = b.comment("Old behaviour: roll a grimoire automatically when a soul type is assigned (kept, off by default).").define("legacySoulAutoRoll", false);
        WEIGHT_COMMON = b.comment("Cover rarity weights when a grimoire chooses you. Common: Three-Leaf / basic suit covers.").defineInRange("weightCommon", 80.0, 0, 1000);
        WEIGHT_UNCOMMON = b.comment("Four-Leaf / Double Spade / Two-Heart / Five-Sided Diamond.").defineInRange("weightUncommon", 16.0, 0, 1000);
        WEIGHT_RARE = b.comment("Five-Leaf (devil-inhabited).").defineInRange("weightRare", 3.5, 0, 1000);
        WEIGHT_BLACK = b.comment("Black Magic cover (black book, Anti-Magic, the devil Liebe).").defineInRange("weightBlack", 0.5, 0, 1000);
        WEIGHT_GOD = b.comment("God-Tier cover (the rarest: an ivory-and-gold book with a starter attribute).").defineInRange("weightGod", 0.1, 0, 1000);
        STARTER_MAGICS = b.comment("Attributes a grimoire can choose at the ceremony (Anti-Magic comes with the Black Magic cover or an empty soul).")
                .defineListAllowEmpty("starterMagics", java.util.List.of("FLAME", "WATER", "WIND", "EARTH"), () -> "FLAME", o -> o instanceof String);
        b.pop();

        b.push("pages");
        PAGE_SLOTS_FLOOR = b.defineInRange("slotsFloor", 6, 1, 64);
        PAGE_SLOTS_CEILING = b.comment("Slots grow from the floor to this ceiling with mastery.").defineInRange("slotsCeiling", 12, 1, 64);
        PAGE_SLOTS_CONTRACT_BONUS = b.comment("Extra slots with a spirit or devil contract.").defineInRange("slotsContractBonus", 4, 0, 64);
        PAGE_SLOTS_CAP = b.defineInRange("slotsCap", 16, 1, 64);
        PAGE_MASTERY_SCALE = b.comment("A page's mastery cost: the larger of its place in the book (k/n) and its spell cost (cost% / 35), times this.").defineInRange("masteryScale", 1.0, 0.05, 2.0);
        GATE_ZONE = b.comment("Minimum Magic Knight rank for zone pages (rank or rank:class, e.g. INTERMEDIATE:5).").define("gateZone", "INTERMEDIATE:5");
        GATE_SIGNATURE = b.comment("Minimum rank for signature (ultimate) pages.").define("gateSignature", "SENIOR:5");
        PAGE_GATES = b.comment("Per-page overrides: book_id:page_id=RANK[:class][;RACE|RACE]. Example: book_time:chrono_anastasis=GRAND")
                .defineListAllowEmpty("pageGates", List.of("book_time:chrono_anastasis=SENIOR:3", "book_fire:leo_rugiens=SENIOR:5"), () -> "", o -> o instanceof String);
        ALTAR_TRAINING_MASTERY = b.comment("Mastery points per altar training session (sneak + use your grimoire on an altar).").defineInRange("altarTrainingMastery", 4, 0, 1000);
        ALTAR_TRAINING_COOLDOWN = b.comment("Seconds between altar training sessions.").defineInRange("altarTrainingCooldown", 300, 0, 86400);
        CHANNEL_MASTERY_TICKS = b.comment("One mastery point per this many ticks of Spirit Channeling.").defineInRange("channelMasteryTicks", 100, 1, 72000);
        MASTERY_PER_GOLD_STAR = b.comment("Mastery points per gold star earned (missions).").defineInRange("masteryPerGoldStar", 2.0, 0, 1000);
        b.pop();

        b.push("ranks");
        AUTO_PROMOTE = b.comment("Promote one class per STARS_PER_CLASS net stars (Junior 5th -> ... -> Senior 1st). Grand / Wizard King are admin-only.")
                .define("autoPromote", true);
        STARS_PER_CLASS = b.defineInRange("starsPerClass", 10, 1, 10000);
        b.pop();

        b.push("squads");
        SQUAD_MIN_RANK_CREATE = b.define("minRankCreate", "INTERMEDIATE:5");
        SQUAD_MIN_RANK_JOIN = b.define("minRankJoin", "JUNIOR:5");
        SQUAD_MAX_SIZE = b.defineInRange("maxSize", 10, 1, 1000);
        SQUAD_CAPTAIN_PROMOTES_VICE = b.define("captainCanPromoteVice", true);
        SQUAD_GOLD_FEEDS_SCORE = b.define("goldStarsFeedScore", true);
        SQUAD_BLACK_REDUCES_SCORE = b.define("blackStarsReduceScore", true);
        SQUAD_OFFLINE_COUNT = b.define("offlineMembersCount", true);
        b.pop();

        b.push("spirit_lord");
        SPIRIT_PHYSICAL_REDUCTION = b.comment("Physical damage reduction for Spirit Lords.").defineInRange("physicalReduction", 0.30, 0, 0.95);
        SPIRIT_FALL_IMMUNE = b.define("fallImmune", true);
        b.pop();
        SPEC = b.build();
    }

    private MultiverseConfig() {}

    /** Config values can be read before the server config loads (e.g. on a client); fall back to the default then. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
