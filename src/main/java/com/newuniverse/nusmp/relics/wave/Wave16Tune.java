package com.newuniverse.nusmp.relics.wave;

/**
 * Wave 16: the ids, flags, data keys and numbers of the seventeen necklace pieces and charms (pure, no Minecraft types). The Minecraft glue reads
 * these; {@link Wave16Rules} holds the small formulas and the tests pin them. The tabletop translation of each card is in docs/doc_wave16.md.
 */
public final class Wave16Tune {
    private Wave16Tune() {}

    // ---------------------------------------------------------------- ids (one string per line: tools/relics/gen_curios_tags.py reads them)
    public static final String ID_SIGIL = "sigil_of_the_faint_flame";
    public static final String ID_CLASP = "true_heart_clasp";
    public static final String ID_POEUR = "brooch_of_poeur";
    public static final String ID_GRENADE = "holy_handgrenade";
    public static final String ID_DOLL = "snugglable_doll";
    public static final String ID_PERFUME = "paralyzing_perfume";
    public static final String ID_SATCHEL = "satchel_of_unmet_needs";
    public static final String ID_SUNSCREEN = "drow_sunscreen";
    public static final String ID_GOD = "portable_god";
    public static final String ID_BATS = "jar_of_bats";
    public static final String ID_BLUE = "simple_blue_runechalk";
    public static final String ID_DRYAD = "dryads_wedding_band";
    public static final String ID_WEAVER = "weaversong";
    public static final String ID_COIN = "coin_of_draconic_luck";
    public static final String ID_WHITE = "alabaster_white_runechalk";
    public static final String ID_IDOL = "strange_idol";
    public static final String ID_WALLET = "wallet_of_the_slayer";

    // ---------------------------------------------------------------- flags (read with RelicFlags.has)
    public static final String F_FLAME = "w16_flame", F_POEUR = "w16_poeur", F_DRYAD = "w16_dryad", F_WALLET = "w16_wallet", F_SATCHEL = "w16_satchel",
            F_GOD = "w16_god", F_SUN = "w16_sun";

    // ---------------------------------------------------------------- keys on the wearer (player persistent data) and on the stack
    public static final String K_REPRISAL = "w16_reprisal_at", K_POEUR = "w16_poeur_at", K_TAINT = "w16_taint_until", K_SUN = "w16_sun_until",
            K_GOD_SPIRIT = "w16_god_spirit", K_GOD_SPIRIT_ROLL = "w16_god_spirit_roll", K_GOD_REVENGE = "w16_god_revenge", K_GOD_AVATAR = "w16_god_avatar",
            K_DRYAD_BOUND = "w16_dryad_bound", K_DRYAD_OFF = "w16_dryad_off";
    public static final String S_COIN_STREAK = "w16_coin_streak", S_COIN_DAY = "w16_coin_day", S_COIN_CRACKED = "w16_coin_cracked_until", S_WALLET = "w16_wallet_gp",
            S_SAT_SEL = "w16_sat_sel", S_SAT_OUT = "w16_sat_out", S_SAT_GONE = "w16_sat_gone";
    /** Marks an object the Satchel produced: which entry it is and when it vanishes. */
    public static final String T_SAT_IDX = "w16_sat_idx", T_SAT_EXP = "w16_sat_exp";
    /** The keys the core's summon bookkeeping reads (Fx.OWNER and Fx.UNTIL); a creature that carries them is nobody's foe and never targets its owner. */
    public static final String OWNER = "nusmp_relic_owner", UNTIL = "nusmp_relic_until";
    /** Marks Snuggles and the Weaverlings: creatures with hostile goals of their own that must not pick a player their owner may not harm. */
    public static final String ALLY = "w16_ally";

    // ---------------------------------------------------------------- Sigil of the Faint Flame
    public static final int SIGIL_MAX = 10;                       // the card: 1d8+2 charges a dawn, none left at sunset
    public static final long SUNSET = 12000L;
    public static final int REPRISAL_COOLDOWN = 40;
    public static final double FLAME_FOCUS = 1.25;                // "+2 to attack rolls and DCs" of fire spells: +25% fire damage

    // ---------------------------------------------------------------- True Heart Clasp, Brooch of Po'eur
    public static final double CLASP_RADIUS = 24.0;
    public static final int POEUR_BONUS = 4, POEUR_COOLDOWN = 600;
    public static final double POEUR_MIN_COST = 5.0;

    // ---------------------------------------------------------------- Holy Handgrenade
    public static final int GRENADE_HOLD = 60;                    // "count to three"
    public static final double GRENADE_THROW = 9.0, GRENADE_RADIUS = 8.0, BACKFIRE_RADIUS = 5.0;
    public static final float GRENADE_SMITE = 99f, GRENADE_HEAL = 33f, BACKFIRE_DAMAGE = 33f;
    public static final int GRENADE_MAX_TARGETS = 40;
    public static final long TAINT_TICKS = 72000L;                // three game days

    // ---------------------------------------------------------------- Snugglable Doll
    public static final int SNUGGLES_TICKS = 6000;
    public static final double SNUGGLES_HEALTH = 30.0, SNUGGLES_ATTACK = 3.0, SNUGGLES_ARMOR = 4.0;

    // ---------------------------------------------------------------- Paralyzing Perfume
    public static final int PERFUME_MAX = 10, PERFUME_TICKS = 360, PERFUME_PLAYER_TICKS = 60;
    public static final double PERFUME_RANGE = 5.0, PERFUME_CONE = 0.5;
    public static final int PERFUME_DC = 17, PERFUME_MAX_TARGETS = 8;

    // ---------------------------------------------------------------- Satchel of Unmet Needs
    public static final long SAT_OUT_TICKS = 24000L;
    public static final int SAT_MISSING_DAYS = 3;

    // ---------------------------------------------------------------- Drow Sunscreen
    public static final long SUN_TICKS = 3000L;
    public static final double SUN_BURN_FACTOR = 0.5;

    // ---------------------------------------------------------------- Portable God
    public static final int GOD_MAX = 12, GOD_GAIN = 3;
    public static final int GOD_PUNISH = 1, GOD_RESCUE = 2, GOD_SPIRIT = 3, GOD_REVENGE = 4, GOD_AVATAR = 5, GOD_WONDER = 7;
    public static final int GOD_ARM_TICKS = 1200;

    // ---------------------------------------------------------------- Jar of Bats, Weaversong
    public static final int BATS = 6, BATS_TICKS = 1200;
    public static final double BATS_RANGE = 14.0;
    public static final float BAT_BITE = 0.5f;
    public static final int WEAVERLINGS = 3, WEAVER_RESPAWN = 40, WEAVER_LIFE = 6000;
    public static final double WEAVER_LEASH = 8.0;

    // ---------------------------------------------------------------- Runechalks
    public static final int CHALK_MAX = 20, CHALK_GAIN = 2;
    public static final int ALARM_TICKS = 3600, MAX_ALARMS = 2, WARD_TICKS = 1200, SEAL_TICKS = 3600, MAX_SEALS = 4, MAX_WARDS = 48, MAX_SEAL_TABLE = 40;
    public static final double ALARM_RADIUS = 6.0, CIRCLE_RADIUS = 3.0, PURIFY_RADIUS = 5.0, GLYPH_REACH = 1.2;
    public static final float GLYPH_DAMAGE = 6.5f;                 // 1d8+2 radiant

    // ---------------------------------------------------------------- Dryad's Wedding Band
    public static final double DRYAD_FIRE = 1.25;
    public static final long DRYAD_FREE_AFTER = 72000L;

    // ---------------------------------------------------------------- Coin of Draconic Luck, Wallet of the Slayer
    public static final int COIN_BUFF_TICKS = 600, COIN_CRACK_DAYS = 3;
    public static final int WALLET_CAP = 1_000_000, WALLET_TITHE = 300, GOLD_COIN_GP = 100;
}
