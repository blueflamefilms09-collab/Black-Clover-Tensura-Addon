package com.newuniverse.nusmp.relics.wave;

/**
 * Why: wave 25 (ten charms and two helmets from the Mines of Dimlodir to the Death Knight) keeps every id, flag and number in one place, so the defs,
 * the handlers and the tests read the same values. Pure: strings and numbers only. Time scale: a card hour is five minutes, a card minute a few seconds,
 * a long rest a Minecraft day, a round six seconds, five feet one block.
 */
final class Wave25Tune {
    private Wave25Tune() {}

    // ---------------------------------------------------------------- relic ids (fixed by the lead)
    static final String ID_ROCK = "ring_of_rock_eating", ID_GAME = "ring_of_the_game", ID_MIMIC = "ring_of_the_mimic", ID_ROSE = "rose_petal",
            ID_LYRE = "silver_tongued_lyre", ID_SPOON = "soup_spoon_of_improbability", ID_LANTERN = "spirit_lantern", ID_FORGOTTEN = "the_forgotten_ring",
            ID_WITCH = "witchs_ring", ID_BUCKET = "bucket_of_disguise", ID_MASK = "corpse_mask", ID_DKH = "death_knights_helmet";

    // ---------------------------------------------------------------- flags
    static final String F_ROCK = "w25_rock", F_GAME = "w25_game", F_MIMIC = "w25_mimic", F_ROSE = "w25_rose", F_SPOON = "w25_spoon",
            F_LANTERN = "w25_lantern", F_FORGOTTEN = "w25_forgotten", F_WITCH = "w25_witch", F_BUCKET = "w25_bucket", F_MASK = "w25_mask", F_DKH = "w25_dkh";

    /** A card hour, in ticks (five minutes), and a card minute (a few seconds). */
    static final int HOUR = 6000, MINUTE = 100, DAY = 24000;

    // ---------------------------------------------------------------- Ring of Rock Eating
    static final int ROCK_EAT_MAX = 8, ROCK_COOLDOWN = 40, ROCK_AUTO_PERIOD = 100, ROCK_AUTO_HUNGRY = 8, ROCK_AUTO_ITEMS = 2;
    static final float ROCK_SATURATION_PER_FOOD = 0.5f;

    // ---------------------------------------------------------------- Ring of the Game
    /** The Game's thinking comes at most once per this many ticks (the card's 30 minutes), then within a few seconds. */
    static final int GAME_INTERVAL = 3000;
    static final int GAME_DC = 15, GAME_WIS_BONUS = 2, GAME_CURSE_RANGE = 16, GAME_CURSE_MOBS = 12, GAME_ECHO_RANGE = 32;
    static final double GAME_THINK_CHANCE = 0.10, GAME_CAP = 0.30;
    static final int PASS_COOLDOWN = 3600;
    static final double PASS_RANGE = 10.0;
    static final float PASS_DAMAGE = 2.5f;
    static final int PASS_CONFUSE = 100;

    // ---------------------------------------------------------------- Ring of the Mimic
    static final double MIMIC_RANGE = 10.0;
    static final int MIMIC_SCAN = 20, MIMIC_GLOW = 80, MIMIC_MSG_GAP = 200;
    static final int GUIDANCE_COOLDOWN = 600, GUIDANCE_TICKS = 1200, GUIDANCE_FUMBLE_SIDES = 20;
    static final int FINGER_TICKS = 12000;
    static final double FINGER_BREAK = -0.25, FINGER_SPEED = -0.15;

    // ---------------------------------------------------------------- Rose Petal
    static final int ROSE_MAX = 12, ENSNARE_COST = 1, GRASP_COST = 4, GUARDIAN_COST = 4;
    static final int ENSNARE_WINDOW = 600, ENSNARE_ROOT = 120, GRASP_ROOT = 100, GUARDIAN_TICKS = 1200;
    static final double GRASP_RANGE = 9.0, HALBERD_ATTACK = 2.0, HALBERD_REACH = 1.5;
    static final float ENSNARE_BONUS = 3.5f, GRASP_DAMAGE = 5.0f, GUARDIAN_SHIELD = 10f, THORN_DAMAGE = 2.0f;

    // ---------------------------------------------------------------- Silver Tongued Lyre
    static final double LYRE_RANGE = 18.0, LAUGH_RANGE = 18.0, LAUGH_COS = 0.93;
    static final int LAUGH_COOLDOWN = 400, LAUGH_MANA = 2, LAUGH_TICKS = 120, DISSONANCE_MANA = 3, DISSONANCE_COOLDOWN = 600, DISSONANCE_WEAK = 240;
    static final double LYRE_SAVE = 0.45;

    // ---------------------------------------------------------------- Soup Spoon of Improbability
    static final double SPOON_DAMAGE = 1.0, SPOON_SPEED = 0.2, SPOON_SIP_SUCCESS = 0.60;
    static final int SPOON_STEADY = HOUR;

    // ---------------------------------------------------------------- Spirit Lantern
    static final int SHINE_HOLD = 100, SHINE_COOLDOWN = 200, REMAINS_MAX = 48, REMAINS_AGE = 24000, REMAINS_NEAR_PLAYER = 64, REPLAY_TICKS = 100;
    static final double SHINE_RANGE = 6.0, LANTERN_GLOW_RANGE = 6.0;
    static final int LANTERN_SCAN = 40, LANTERN_DARK = 4;

    // ---------------------------------------------------------------- The Forgotten Ring
    static final int VANISH_COOLDOWN = 100, VANISH_MANA = 2;
    static final double VANISH_AMBUSH = 1.5, VANISH_SEEN_RANGE = 6.0;

    // ---------------------------------------------------------------- Witch's Ring
    static final int FORM_ALLOWANCE = 6000, FORM_COOLDOWN = 60;

    // ---------------------------------------------------------------- Bucket of Disguise
    static final int BUCKET_PERIOD = 100;
    static final double BUCKET_RANGE = 12.0, BUCKET_WARN_RANGE = 16.0;

    // ---------------------------------------------------------------- Corpse Mask
    static final int MASK_FACES = 5, MASK_PER_DAY = 2, MASK_TICKS = HOUR, MASK_WITNESS_RANGE = 24, MASK_REPLACE_GAP = 1200;

    // ---------------------------------------------------------------- Death Knight's Helmet
    static final int DKH_CON_MAX = 4, DKH_BASE_DC = 12, DKH_DC_STEP = 2, DKH_WIS_BONUS = 2;
    static final double DKH_CON_HP = 2.0, DKH_RESIST_WITHER = 0.5, DKH_CAP = 0.30;
}
