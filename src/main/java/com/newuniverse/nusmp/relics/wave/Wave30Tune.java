package com.newuniverse.nusmp.relics.wave;

/**
 * Why: the wave is split over several classes (Vecna, Deck, Stones, Paradox, Key, events); the shared numbers live here so a balance change is one edit
 * and the pure tests check the very figures the game uses. Pure: strings and numbers only.
 * <p>
 * Wave 30 (the special wave: Hand and Eye of Vecna, the Deck of Many Things, the Infinity Gauntlet with its six Stones, Paradox and Warren's Key):
 * every id, flag and number in one place, so the defs, the handlers and the tests read the same values.
 */
final class Wave30Tune {
    private Wave30Tune() {}

    // ---------------------------------------------------------------- relic ids (fixed by the lead: ids4.json / wave30_cards.json)
    static final String ID_HAND = "hand_of_vecna", ID_EYE = "eye_of_vecna", ID_DECK = "deck_of_many_things", ID_GAUNTLET = "the_infinity_gauntlet",
            ID_SPACE = "the_space_stone_or_the_tesseract", ID_MIND = "the_mind_stone", ID_POWER = "the_power_stone",
            ID_TIME = "the_time_stone_or_the_eye_of_agamotto", ID_REALITY = "the_reality_stone", ID_SOUL = "the_soul_stone",
            ID_PARADOX = "paradox", ID_KEY = "warrens_key";

    /** The six stones in the order of the gauntlet's knuckles (also the order of its acts). */
    static final String[] STONES = {ID_SPACE, ID_MIND, ID_POWER, ID_TIME, ID_REALITY, ID_SOUL};

    // ---------------------------------------------------------------- flags (read with RelicFlags.has)
    static final String F_EYE = "w30_eye", F_HAND = "w30_hand", F_GAUNTLET = "w30_gauntlet", F_PARADOX = "w30_paradox", F_SOUL = "w30_soul",
            F_POWER = "w30_power";

    // ---------------------------------------------------------------- Tensura ids (all in clean.json; gated at run time by the skill / effect registries)
    static final String SK_EYE_OF_TRUTH = "tensura:eye_of_truth", SK_FARSIGHT = "tensura:farsight", SK_DANGER_SENSE = "tensura:danger_sense",
            SK_POISON_RES = "tensura:poison_resistance", SK_PARALYSIS_RES = "tensura:paralysis_resistance", SK_SPATIAL_MOTION = "tensura:spatial_motion",
            SK_THOUGHT_COMM = "tensura:thought_communication", SK_ANALYTICAL = "tensura:analytical_appraisal";
    static final String EF_FEAR = "tensura:fear", EF_FROST = "tensura:frost", EF_FUTURE_VISION = "tensura:future_vision",
            EF_PRESENCE_CONCEAL = "tensura:presence_concealment", EF_CONFUSION = "tensura:confusion";
    static final String AT_MAX_MAGICULE = "tensura:max_magicule", AT_MAX_AURA = "tensura:max_aura", AT_MAGICULE_REGEN = "tensura:magicule_regeneration_multiplier",
            AT_HEIGHT = "tensura:height_multiplier", AT_WIDTH = "tensura:width_multiplier";
    static final String MC_ATTACK = "minecraft:generic.attack_damage", MC_ARMOR = "minecraft:generic.armor", MC_TOUGHNESS = "minecraft:generic.armor_toughness",
            MC_MAX_HEALTH = "minecraft:generic.max_health";

    // ---------------------------------------------------------------- Vecna: the Eye
    /** How long a pulse of X-ray (every second) glows, and how far it reaches; a little longer than the pulse so it does not flicker. */
    static final int XRAY_GLOW = 50;
    static final double XRAY_RADIUS = 24.0;
    /** Food exhaustion one X-ray pulse costs when the pair is not complete (the card's exhaustion: "safe X-ray vision without exhaustion" is a set bonus). */
    static final float XRAY_EXHAUSTION = 0.12f;
    /** Clairvoyance: the five nearest, within 96 blocks (the card's scrying), glowing for ten seconds. */
    static final int CLAIR_COUNT = 5, CLAIR_GLOW = 200, CLAIR_COOLDOWN = 1200;
    static final double CLAIR_RADIUS = 96.0;
    /** Crown of Madness: a mob fights its allies for ten seconds. */
    static final int CROWN_TICKS = 200, CROWN_COOLDOWN = 900;
    static final double CROWN_REACH = 24.0, CROWN_ALLY_RADIUS = 14.0;
    /** Disintegrate: a hitscan beam; heavy damage on a mob, capped at 45% of a player's health and never lethal (a player is left on a heart at least). */
    static final double DISINTEGRATE_REACH = 48.0;
    static final float DISINTEGRATE_DAMAGE = 26f;
    static final int DISINTEGRATE_COOLDOWN = 1200;
    static final double DISINTEGRATE_EP = 3.0;
    /** Dominate Monster: a mob is an ally for a minute. */
    static final int DOMINATE_TICKS = 1200, DOMINATE_COOLDOWN = 2400;
    static final double DOMINATE_REACH = 24.0, DOMINATE_MANA = 6.0;
    /** Eyebite: a cone twelve blocks long. */
    static final double EYEBITE_RANGE = 12.0, EYEBITE_COS = 0.72;
    static final int EYEBITE_TICKS = 200, EYEBITE_PLAYER_TICKS = 80, EYEBITE_COOLDOWN = 600;
    /** The 5% chance each Eye act has of Vecna's Hunger; the episode lasts 20 seconds and the hand that hunts the bearer lives for as long. */
    static final double HUNGER_CHANCE = 0.05;
    static final int HUNGER_TICKS = 400, HUNGER_HAND_HEALTH = 14;
    static final float HUNGER_HAND_DAMAGE = 3.0f;
    /** A hostile summon of the Deck or of the Hunger never leaves a player below this much health (one heart). */
    static final float LEAVE_HEALTH = 2.0f;

    // ---------------------------------------------------------------- Vecna: the Hand
    /** "Strength 20": +7 attack damage (nine with the player's own two), +3 armor, +2 toughness while it is worn. */
    static final double HAND_ATTACK = 7.0, HAND_ARMOR = 3.0, HAND_TOUGHNESS = 2.0;
    /** The cold of the Hand's blows: extra damage and the frost on the foe. */
    static final float HAND_COLD_DAMAGE = 3.0f;
    static final int HAND_FROST_TICKS = 100;
    /** Coercion: the chance a Hand act is followed by Vecna's Whisper, and what the Whisper does (a few seconds of Hunger and Mining Fatigue). */
    static final double WHISPER_CHANCE = 0.20;
    static final int WHISPER_TICKS = 160;
    /** Touch of Death (the Finger of Death): wither and heavy damage, capped. */
    static final double TOUCH_REACH = 4.5;
    static final float TOUCH_DAMAGE = 20f;
    static final int TOUCH_COOLDOWN = 1200;
    static final double TOUCH_EP = 3.0;
    /** Rimed Grasp: freezes the foes around. */
    static final double GRASP_RADIUS = 6.0;
    static final float GRASP_DAMAGE = 7f;
    static final int GRASP_TICKS = 160, GRASP_COOLDOWN = 600;
    static final double GRASP_MANA = 5.0;
    /** Skeleton to Jelly (the pair's touch): 30% of the foe's maximum health, 10% on a boss, 45% cap and a heart left on a player. */
    static final double JELLY_FRACTION = 0.30, JELLY_BOSS_FRACTION = 0.10, JELLY_PLAYER_CAP = 0.45;
    static final int JELLY_TICKS = 300, JELLY_COOLDOWN = 6000;
    static final double JELLY_EP = 4.0;

    // ---------------------------------------------------------------- Vecna: the pair
    /** Cannot be surprised: the first blow from an attacker in each 20 seconds is turned aside. */
    static final int SURPRISE_COOLDOWN = 400;
    /** Regeneration: one health every two seconds; poison and disease are cleansed once a second. */
    static final int REGEN_EVERY = 40;
    static final float REGEN_AMOUNT = 1.0f;
    /** The Wish: once in 30 real days (the card), per player. */
    static final long WISH_COOLDOWN_MILLIS = 30L * 24L * 60L * 60L * 1000L;
    static final int WISH_RESILIENCE_TICKS = 24000;

    // ---------------------------------------------------------------- Deck of Many Things
    static final int DECK_ACT_COOLDOWN = 40;
    static final int DONJON_TICKS = 3600, VOID_TICKS = 2400, IDIOT_TICKS = 12000, EURYALE_TICKS = 36000, RUIN_UNLUCK_TICKS = 12000, FATES_TICKS = 12000,
            MOON_TICKS = 12000, THRONE_TICKS = 24000;
    static final int FOE_LIFE_TICKS = 1200, KNIGHT_LIFE_TICKS = 6000;
    static final int FOOL_LEVELS = 3, JESTER_LEVELS = 4, COMET_LEVELS = 3, SUN_LEVELS = 6, GEM_COUNT = 25, RUIN_MAX = 64, TALONS_MAX = 32;
    /** The Star's permanent gift: +2 maximum health and +1 attack damage each, at most three stars over a life. */
    static final int STAR_CAP = 3;
    static final double STAR_HEALTH = 2.0, STAR_ATTACK = 1.0;

    // ---------------------------------------------------------------- Infinity Stones and the Gauntlet
    static final double STONE_BOOST_EACH = 0.12;
    static final int SPACE_MAX_PEOPLE = 10, SPACE_COOLDOWN = 1200;
    static final double SPACE_GATHER = 12.0, SPACE_MANA = 3.0;
    static final int MIND_COOLDOWN = 600, MIND_LIST = 12;
    static final double MIND_RADIUS = 200.0, MIND_READ_REACH = 32.0;
    static final int POWER_COOLDOWN = 900;
    static final double POWER_RADIUS = 6.0, POWER_EP = 2.0;
    static final float POWER_DAMAGE = 14f;
    static final int FORESIGHT_TICKS = 12000, FORESIGHT_HOLD = 100;
    static final int REALITY_COOLDOWN = 600, POLY_TICKS = 600, SEEMING_TICKS = 3600, MIRAGE_TICKS = 200;
    static final double REALITY_REACH = 24.0, MIRAGE_RADIUS = 14.0, POLY_SCALE = 0.4;
    static final int SOUL_RADIUS = 64, SOUL_MAX = 12, SOUL_BLESS_TICKS = 600;
    static final int SNAP_RADIUS = 48;
    static final double SNAP_FRACTION = 0.5, SNAP_BOSS_FRACTION = 0.10, SNAP_EP = 10.0;
    static final int SNAP_TOLL_TICKS = 600;
    static final int GREAT_WISH_BLESS_TICKS = 12000, WARD_TICKS = 24000, WISH_RADIUS = 48;
    static final int UNDO_WINDOW = 120;
    static final int TREASURE_DIAMONDS = 32, TREASURE_EMERALDS = 32;

    // ---------------------------------------------------------------- Paradox
    static final int PARADOX_MAX_CHARGES = 20;
    static final String POOL_PARADOX = "paradox";
    static final int PARADOX_HASTE = 3, PARADOX_LOCK = 5, PARADOX_STASIS = 3, PARADOX_SLOW = 3, PARADOX_STOP = 9, PARADOX_ALT = 3, PARADOX_CLOCK = 1, PARADOX_MISSING = 3;
    static final int PARADOX_SAVE_DC = 17, PARADOX_SAVE_BONUS = 0, PARADOX_MISSING_DC = 15;
    static final double PARADOX_BONUS_FLAT = 3.0;
    static final int PARADOX_HASTE_TICKS = 300, PARADOX_HOLD_TICKS = 100, PARADOX_SLOW_TICKS = 200, PARADOX_STOP_TICKS = 100, PARADOX_ALT_TICKS = 100;
    static final double PARADOX_RADIUS = 18.0, PARADOX_REACH = 24.0;
    /** The wielder's path is sampled every two seconds and this many samples are kept: a failed Paradox throws them back about ten seconds. */
    static final int PATH_SAMPLES = 5;

    // ---------------------------------------------------------------- Warren's Key
    static final int KEY_POOL = 5, KEY_REACH = 6, KEY_KNOWN_MAX = 12, KEY_PORTAL_TICKS = 72000, KEY_PORTAL_MAX = 8, KEY_LOCK_MAX = 16;
    static final double KEY_PORTAL_STEP = 1.4;
}
