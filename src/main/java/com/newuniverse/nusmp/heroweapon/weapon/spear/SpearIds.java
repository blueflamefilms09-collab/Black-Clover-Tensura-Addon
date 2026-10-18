package com.newuniverse.nusmp.heroweapon.weapon.spear;

import java.util.List;

/**
 * Why: the skill ids of the Legendary Spear are written in four places that must agree character for character (the skill objects, the forms file, the
 * language file, the self-test), and a typo in any of them is silent in the game (a form that lists an id nobody registered simply has an empty slot).
 * One pure class holds the ids and the passive ids, so the forms-file test, the numbers test and the self-test all read the same list. Ids are
 * {@code spear.<name>}, as the catalogue self-test requires.
 */
public final class SpearIds {
    private SpearIds() {}

    // ---- the ordinary skills (the tree forms)
    public static final String PIERCING_THRUST = "spear.piercing_thrust", LUNGE = "spear.lunge", VAULT = "spear.vault", JAVELIN = "spear.javelin",
            WHIRLWIND = "spear.whirlwind", RIPOSTE = "spear.riposte", CHAOS_SPEAR = "spear.chaos_spear", HOOKED_PIERCE = "spear.hooked_pierce",
            METEOR_SPEAR = "spear.meteor_spear", PORTAL_SPEAR = "spear.portal_spear", FILO_RUSH = "spear.filo_rush", TRIAINA = "spear.triaina",
            MINUTE_HAND = "spear.minute_hand", BRIONAC = "spear.brionac", TIME_REVERSAL = "spear.time_reversal";

    // ---- the Lust series
    public static final String TEMPTATION = "spear.temptation", RAPTURE = "spear.rapture", THORN_CHAINS = "spear.thorn_chains", LUSTS_EMBRACE = "spear.lusts_embrace";

    /** The skills of the ordinary forms, in the order the logic lists them. */
    public static final List<String> ORDINARY = List.of(PIERCING_THRUST, LUNGE, VAULT, JAVELIN, WHIRLWIND, RIPOSTE, CHAOS_SPEAR, HOOKED_PIERCE, METEOR_SPEAR,
            PORTAL_SPEAR, FILO_RUSH, TRIAINA, MINUTE_HAND, BRIONAC, TIME_REVERSAL);

    /** The skills of the Lust curse forms. */
    public static final List<String> CURSE = List.of(TEMPTATION, RAPTURE, THORN_CHAINS, LUSTS_EMBRACE);

    /** Every skill of the weapon. */
    public static final List<String> ALL = List.of(PIERCING_THRUST, LUNGE, VAULT, JAVELIN, WHIRLWIND, RIPOSTE, CHAOS_SPEAR, HOOKED_PIERCE, METEOR_SPEAR,
            PORTAL_SPEAR, FILO_RUSH, TRIAINA, MINUTE_HAND, BRIONAC, TIME_REVERSAL, TEMPTATION, RAPTURE, THORN_CHAINS, LUSTS_EMBRACE);

    // ---- the passives a form may list (the logic checks them with HeroForm.has)
    public static final String TIME_MEMORY = "time_memory", FEATHERLIGHT = "featherlight", LONGSHAFT = "longshaft", KILL_VIGOR = "kill_vigor",
            PORTAL_ECHO = "portal_echo", LUST_FERVOR = "lust_fervor", RAVENOUS_AIM = "ravenous_aim";

    public static final List<String> PASSIVES = List.of(TIME_MEMORY, FEATHERLIGHT, LONGSHAFT, KILL_VIGOR, PORTAL_ECHO, LUST_FERVOR, RAVENOUS_AIM);
}
