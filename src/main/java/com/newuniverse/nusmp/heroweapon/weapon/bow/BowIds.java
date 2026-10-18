package com.newuniverse.nusmp.heroweapon.weapon.bow;

import java.util.List;

/**
 * Why: the skill ids of the Legendary Bow are written in four places that must agree character for character (the skill objects, the forms file, the
 * language file, the self-test), and a typo in any of them is silent in the game (a form that lists an id nobody registered simply has an empty slot).
 * One pure class holds the ids and the passive ids, so the forms-file test, the numbers test and the self-test all read the same list. Ids are
 * {@code bow.<name>}, as the catalogue self-test requires.
 */
public final class BowIds {
    private BowIds() {}

    // ---- the arming techniques: they change the NEXT drawn shot
    public static final String PIERCING_SHOT = "bow.piercing_shot", NET_ARROW = "bow.net_arrow", MARK_ARROW = "bow.mark_arrow", EXPLOSIVE_ARROW = "bow.explosive_arrow",
            TRACKING_SHOT = "bow.tracking_shot", SECOND_ARROW = "bow.second_arrow";

    // ---- the direct techniques
    public static final String ARROW_RAIN = "bow.arrow_rain", ARROW_SQUALL = "bow.arrow_squall", BIRD_HUNTING = "bow.bird_hunting", RETREAT_SHOT = "bow.retreat_shot",
            METEOR_BOW = "bow.meteor_bow", FROZEN_RAIN = "bow.frozen_rain", STARFALL_VOLLEY = "bow.starfall_volley";

    // ---- the Pride series
    public static final String LAW_FANATIC = "bow.law_fanatic", BULL_OF_PHALARIS = "bow.bull_of_phalaris", PRIDE_COMMAND = "bow.pride_command",
            AUTOCRACY_JUSTICE = "bow.autocracy_justice";

    /** The skills of the ordinary forms, in the order the logic lists them. */
    public static final List<String> ORDINARY = List.of(PIERCING_SHOT, NET_ARROW, MARK_ARROW, EXPLOSIVE_ARROW, TRACKING_SHOT, SECOND_ARROW, ARROW_RAIN, ARROW_SQUALL,
            BIRD_HUNTING, RETREAT_SHOT, METEOR_BOW, FROZEN_RAIN, STARFALL_VOLLEY);

    /** The skills of the Pride curse forms. */
    public static final List<String> CURSE = List.of(LAW_FANATIC, BULL_OF_PHALARIS, PRIDE_COMMAND, AUTOCRACY_JUSTICE);

    /** Every skill of the weapon. */
    public static final List<String> ALL = List.of(PIERCING_SHOT, NET_ARROW, MARK_ARROW, EXPLOSIVE_ARROW, TRACKING_SHOT, SECOND_ARROW, ARROW_RAIN, ARROW_SQUALL,
            BIRD_HUNTING, RETREAT_SHOT, METEOR_BOW, FROZEN_RAIN, STARFALL_VOLLEY, LAW_FANATIC, BULL_OF_PHALARIS, PRIDE_COMMAND, AUTOCRACY_JUSTICE);

    // ---- the passives a form may list (the logic checks them with HeroForm.has)
    public static final String QUICKDRAW = "quickdraw", FARSIGHT = "farsight", PIERCING_ARROWS = "piercing_arrows", EAGLE_EYE = "eagle_eye", PRIDE_ARROWS = "pride_arrows";

    public static final List<String> PASSIVES = List.of(QUICKDRAW, FARSIGHT, PIERCING_ARROWS, EAGLE_EYE, PRIDE_ARROWS);
}
