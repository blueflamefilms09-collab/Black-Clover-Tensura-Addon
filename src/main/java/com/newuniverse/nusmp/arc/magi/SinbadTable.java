package com.newuniverse.nusmp.arc.magi;

import com.newuniverse.nusmp.entity.zogratis.MoveSpec;

import java.util.List;

/**
 * Why: every number of Sinbad's moves in one place and nothing else (the same split as {@code EscanorTable}): damage, wind-up, cooldown, range, power gate. Pure, so the tests read
 * it. Damage values are the raw base of a hit: {@code ZogratisBoss.strike} runs them through the power tuning, the victim's EP factor and the balance law (a hit never takes more
 * than the pvp share of a player's health). Times are ticks. The order of {@link #specs()} is the order of the boss's move list (the client reads move names by index): only append.
 */
final class SinbadTable {
    private SinbadTable() {}

    // --- base (no djinn): sword + Magoi Manipulation
    static final String SWORD_SLASH = "sword_slash", MAGOI_THRUST = "magoi_thrust";
    // --- the switch
    static final String DJINN_EQUIP = "djinn_equip";
    // --- Baal (lightning)
    static final String BARARAQ = "bararaq", BARARAQ_SAIQA = "bararaq_saiqa", INQERAD_SAIQA = "bararaq_inqerad_saiqa";   // the last is the Extreme Magic
    // --- Valefor (ice)
    static final String GARUFOR_ZAIRU = "garufor_zairu", GARUFOR_KIRESTAL = "garufor_kirestal", STAGNATION = "stagnation";
    // --- Zepar (sound)
    static final String COMMAND_SCREAM = "command_scream", RUKH_INTRUSION = "rukh_intrusion";
    // --- Focalor (wind)
    static final String FORAZ_ZORA = "foraz_zora", GALE_STEP = "gale_step";
    // --- Vepar (magnetism)
    static final String VEPAR_ISTELLA = "vepar_istella";
    // --- Crocell (saber), Furfur (darkness): invented moves, the lore gives no spells
    static final String SABER_FLURRY = "saber_flurry", RENDING_DARKNESS = "rending_darkness";
    // --- the final phase (invented, built on his canon rapid switching)
    static final String SEVEN_SEAS = "seven_seas_sovereign";
    // --- made for this boss (docs/boss_redesign_goku_sinbad.md), appended: they take the colour and the spark of whichever djinn is on him
    static final String RUKH_ORRERY = "rukh_orrery", KINGDOM_SIGIL = "kingdom_sigil";

    // damage (raw base)
    static final float SLASH_DAMAGE = 16f, THRUST_DAMAGE = 18f, BOLT_DAMAGE = 20f, SAIQA_DAMAGE = 30f, INQERAD_DAMAGE = 45f, ICE_PICK_DAMAGE = 9f, KIRESTAL_DAMAGE = 14f,
            SCREAM_DAMAGE = 6f, INTRUSION_DAMAGE = 8f, TORNADO_DAMAGE = 26f, GALE_DAMAGE = 12f, SWORD_RAIN_DAMAGE = 13f, SABER_DAMAGE = 11f, DARKNESS_DAMAGE = 20f,
            SEVEN_SEAS_STEP_DAMAGE = 18f, ORRERY_DAMAGE = 12f, SIGIL_DAMAGE = 24f;

    // equip rules
    static final int EQUIP_TICKS = 400;               // one djinn lasts 20 s
    static final int EQUIP_TICKS_GATES = 240;         // 12 s once the Gates are open
    static final int EQUIP_TICKS_PHASE2 = 320;        // 4.105.0: 16 s in the Djinn Master phase (from power 60)
    static final int SWITCH_WINDOW_PHASE2 = 120;      // 4.105.0: the last 6 s of an equip may already switch directly
    static final int PHASE2_POWER = 60;               // the power at which the second phase (and its second bar) begins
    static final int SWITCH_WINDOW = 60;              // the last 3 s of an equip: a direct switch to the next djinn may be chosen
    static final int FORESIGHT_COOLDOWN = 200;        // the "flow of destiny" sidestep at most every 10 s
    static final float FORESIGHT_CHANCE = 0.12f;
    static final float MAGOI_MAGIC_TAKEN = 0.75f;     // Magoi Manipulation: magic hits deal 75%

    /** The most spots any one move marks (the box queries and packets of a move are bounded by it). */
    static final int MAX_SPOTS = 12;

    /** One row of the table; the record's accessors are the {@link MoveSpec} methods. */
    record Spec(String id, String label, SinbadDjinn djinn, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange, int weight,
                boolean ultimate, boolean crowd, double manaCost, int recovery) implements MoveSpec {}

    //                                 id                  label                      djinn                 pow wind  cd   min   max   w  ult    crowd  mana rec
    private static final List<Spec> SPECS = List.of(
            new Spec(SWORD_SLASH,       "Sword Slash",             SinbadDjinn.NONE,     0, 14,  60, 0.0,  6.0, 6, false, false,  6,   8),
            new Spec(MAGOI_THRUST,      "Magoi Thrust",            SinbadDjinn.NONE,     0, 18, 120, 3.0, 10.0, 4, false, false,  8,  10),
            new Spec(DJINN_EQUIP,       "Djinn Equip",             SinbadDjinn.NONE,    10, 20,  40, 0.0, 48.0, 9, false, false, 10,   6),

            new Spec(BARARAQ,           "Bararaq",                 SinbadDjinn.BAAL,    10, 30, 140, 4.0, 32.0, 5, false, true,  14,  10),
            new Spec(BARARAQ_SAIQA,     "Bararaq Saiqa",           SinbadDjinn.BAAL,    30, 34, 200, 5.0, 28.0, 4, false, false, 20,  14),
            new Spec(INQERAD_SAIQA,     "Bararaq Inqerad-Saiqa",   SinbadDjinn.BAAL,    80, 80, 900, 0.0, 40.0, 3, true,  true,  60,  60),

            new Spec(GARUFOR_ZAIRU,     "Garufor Zairu",           SinbadDjinn.VALEFOR, 10, 22, 120, 4.0, 26.0, 5, false, false, 12,   8),
            new Spec(GARUFOR_KIRESTAL,  "Garufor Kirestal",        SinbadDjinn.VALEFOR, 30, 28, 260, 0.0, 18.0, 3, false, false, 18,  12),
            new Spec(STAGNATION,        "Stagnation",              SinbadDjinn.VALEFOR, 20, 24, 300, 0.0,  9.0, 3, false, true,  14,  10),

            new Spec(COMMAND_SCREAM,    "Command Scream",          SinbadDjinn.ZEPAR,   20, 36, 320, 0.0, 16.0, 4, false, true,  18,  14),
            new Spec(RUKH_INTRUSION,    "Rukh Intrusion",          SinbadDjinn.ZEPAR,   10, 20, 160, 0.0, 20.0, 4, false, false, 12,   8),

            new Spec(FORAZ_ZORA,        "Foraz Zora",              SinbadDjinn.FOCALOR, 20, 40, 260, 4.0, 30.0, 5, false, true,  24,  14),
            new Spec(GALE_STEP,         "Gale Step",               SinbadDjinn.FOCALOR, 10, 12, 120, 4.0, 20.0, 4, false, false, 10,   6),

            new Spec(VEPAR_ISTELLA,     "Vepar Istella",           SinbadDjinn.VEPAR,   40, 30, 240, 0.0, 30.0, 6, false, true,  22,  12),

            new Spec(SABER_FLURRY,      "Crocell Saber Flurry",    SinbadDjinn.CROCELL, 10, 14, 100, 0.0,  6.5, 7, false, false, 10,   8),

            new Spec(RENDING_DARKNESS,  "Rending Darkness",        SinbadDjinn.FURFUR,  60, 26, 180, 3.0, 22.0, 6, false, false, 16,  10),

            new Spec(SEVEN_SEAS,        "Seven Seas Sovereign",    SinbadDjinn.NONE,    90, 60, 1200, 0.0, 30.0, 3, true,  true,  80, 150),

            new Spec(RUKH_ORRERY,       "Rukh Orrery",             SinbadDjinn.NONE,    20, 44, 300, 4.0, 30.0, 3, false, false, 20,  16),
            new Spec(KINGDOM_SIGIL,     "Kingdom Sigil",           SinbadDjinn.NONE,    50, 56, 420, 0.0, 24.0, 3, false, true,  30,  20));

    static List<Spec> specs() { return SPECS; }

    static Spec byId(String id) {
        for (Spec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }

    /** The power Sinbad needs before he may equip a djinn (the power rises through the fight). */
    static int unlockPower(SinbadDjinn d) {
        return switch (d) {
            case BAAL, FOCALOR -> 10;
            case VALEFOR, ZEPAR, CROCELL -> 25;
            case VEPAR -> 40;
            case FURFUR -> 60;
            case NONE -> 0;
        };
    }
}
