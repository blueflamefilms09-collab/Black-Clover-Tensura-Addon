package com.newuniverse.nusmp.relics.wave;

/**
 * Wave 18 (the Bountiful Bags and other charms): every id, flag and number in one place, so the defs, the handlers and the tests read the same
 * values. Pure: strings and numbers only.
 */
final class Wave18Tune {
    private Wave18Tune() {}

    // ---------------------------------------------------------------- relic ids (fixed by ids2.json)
    static final String ID_HOLDING = "bag_of_holding", ID_DEVOURING = "bag_of_devouring", ID_COLDING = "bag_of_colding", ID_GOLDING = "bag_of_golding",
            ID_MOLDING = "bag_of_molding", ID_SCOLDING = "bag_of_scolding", ID_UNFOLDING = "bag_of_unfolding", ID_SPELLBOOK = "old_masters_spellbook",
            ID_ORBIT = "tiny_orbit", ID_LOVERS = "the_lovers_divide", ID_COIN = "coin_of_fortune", ID_DIAL = "defenders_dial",
            ID_ELEPHANT = "elephant_of_the_room", ID_KITTENS = "bag_of_kittens", ID_CAMP = "the_pocket_campsite", ID_MIRROR = "mirror_of_spell_reflection",
            ID_DRAGON = "dragon_satchel";

    // ---------------------------------------------------------------- flags
    static final String F_ORBIT = "w18_orbit", F_LOVERS = "w18_lovers", F_SPELLBOOK = "w18_spellbook";

    // ---------------------------------------------------------------- bags: the pocket
    /** A bag is stuffed with a foe for three pulses of this many ticks ("a minute" of the card, a few seconds here). */
    static final int STUFF_PULSE = 60, STUFF_PULSES = 3;
    /** A foe this small, this light on health, fits through the mouth of a bag. */
    static final double STUFF_MAX_WIDTH = 1.4, STUFF_MAX_HEIGHT = 2.5, STUFF_MAX_HEALTH = 80.0, STUFF_REACH = 5.0;
    /** Chance a foe fails the bag's "saving throw" at a pulse (DC 12 on a d20 is 55%), lowered by up to half by knockback resistance. */
    static final double SAVE_FAIL = 0.55, SAVE_FLOOR = 0.05;
    static final int STUFF_COOLDOWN = 600, STUFF_MANA = 3;
    /** Damage of one failed pulse (the card's 1d6). */
    static final float PULSE_DAMAGE = 4.0f;
    /** The most items a bag changes in one day (gilding, composting). */
    static final int DAILY_ITEM_CAP = 64;
    /** One bone meal per this many composter levels of plant matter. */
    static final double COMPOST_LEVELS_PER_MEAL = 5.0;
    /** The Bag of Devouring: percent of maximum magicules each devoured item feeds, and how many items count per day. */
    static final double DEVOUR_FEED_PERCENT = 0.25;
    static final int DEVOUR_FEED_DAILY = 30;
    /** The Devouring bag's gulp: 40% of a foe's maximum health (8 at least, 60 at most), after a telegraphed three seconds. */
    static final double GULP_FRACTION = 0.40;
    static final float GULP_MIN = 8f, GULP_MAX = 60f;
    static final int GULP_DELAY = 60, GULP_COOLDOWN = 1200;
    static final double GULP_EP = 2.0;
    /** Unfolding: how far the unlatching reaches, and how many uses a day. */
    static final double UNLATCH_REACH = 6.0;
    static final int UNLATCH_PER_DAY = 8;

    // ---------------------------------------------------------------- the Lover's Divide
    static final int LINK_TICKS = 6000, LINK_COOLDOWN = 100;
    static final double LINK_REACH = 8.0, LINK_MAX_DISTANCE = 32.0;
    static final int LINK_CAP = 64;

    // ---------------------------------------------------------------- Coin of Fortune
    static final int COIN_PER_DAY = 3, COIN_LUCK_TICKS = 6000, COIN_COOLDOWN = 40;
    static final double COIN_GOOD_OUT = 0.70, COIN_GOOD_ATTACK = 1.5, COIN_BAD_OUT = 1.30, COIN_BAD_ATTACK = 0.6;

    // ---------------------------------------------------------------- Defender's Dial
    static final int DIAL_PER_DAY = 3, DIAL_TICKS = 6000;
    static final double DIAL_KEEP = 0.40;

    // ---------------------------------------------------------------- Mirror of Spell Reflection
    static final int MIRROR_PER_DAY = 3, MIRROR_WINDOW = 100, MIRROR_COOLDOWN = 300, MIRROR_MANA = 2;
    static final float MIRROR_CAP = 30f;

    // ---------------------------------------------------------------- Elephant of the Room
    static final int ELEPHANT_TICKS = 600, ELEPHANT_PULSE = 20;
    static final double ELEPHANT_MIN_R = 5.0, ELEPHANT_MAX_R = 12.0;

    // ---------------------------------------------------------------- Bag of Kittens
    static final int KITTENS_START = 1000, KITTEN_CAP_OWNED = 8, FLOOD_PULSES_MIN = 3, FLOOD_PULSES_DICE = 6, FLOOD_COOLDOWN = 1800;
    static final double FLOOD_EP = 2.0, FLOOD_R0 = 4.0, FLOOD_R_GROWTH = 0.8, FLOOD_R_MAX = 9.0;

    // ---------------------------------------------------------------- The Pocket Campsite
    static final int CAMP_TICKS = 12000, CAMP_COOLDOWN = 200, CAMP_MAX_BLOCKS = 200;
    static final double CAMP_RADIUS = 7.0;

    // ---------------------------------------------------------------- Dragon Satchel
    static final int SATCHEL_FILL_TICKS = 24000, SATCHEL_THROWS = 2, SATCHEL_COOLDOWN = 20;
    static final double SATCHEL_RANGE = 5.0, SATCHEL_COS = 0.82, SATCHEL_SAVE = 0.40;

    // ---------------------------------------------------------------- Tiny Orbit
    static final double ORBIT_RADIUS = 0.45;
    /** Block light under which the moon glows enough to see by. */
    static final int ORBIT_DARK = 4;

    // ---------------------------------------------------------------- Old Master's Spellbook
    static final int STUDY_PER_DAY = 2, STUDY_TICKS = 6000, STUDY_COOLDOWN = 400;
}
