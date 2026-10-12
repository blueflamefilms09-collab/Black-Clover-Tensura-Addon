package com.newuniverse.nusmp.dungeon.nexus.relic;

/** The words of the relics' tooltips: what each does and what it costs (docs/living_dungeon.md section 7). */
final class RelicText {
    private RelicText() {}

    static String effect(Relic r) {
        return switch (r) {
            case GLUTTONS_CHALICE -> "Digests toxic monster meat into a rich stew: no sickness, immense saturation, and for a few minutes the Dungeon's thin air, "
                    + "scorched floors and magicule radiation cannot touch you.";
            case MANTLE_OF_THE_SCAVENGER -> "Registers you as a Tier 1 Scavenger: the apex predators of the Dungeon pay you no mind at all.";
            case BAND_OF_THE_SPATIAL_ANCHOR -> "Holds you fast in space: the Dungeon's spatial traps and gravity pulses cannot move you, and you may place blocks "
                    + "in its volatile zones.";
            case DEVIL_BOUND_SHEATH -> "Drinks the magicules of magic you parry or block. Once full, your next melee strike releases a cross-dimensional "
                    + "Anti-Magic slash that ignores armour and magic nullification.";
            case GAMBLERS_GRIMOIRE_PAGE -> "Held in the off-hand, use it to cast a random, overpowered spell. It rests for a minute after each use.";
        };
    }

    static String curse(Relic r) {
        return switch (r) {
            case GLUTTONS_CHALICE -> "It is alive. Unfed for three in-game days, it drains your hunger and your magicules until it is fed.";
            case MANTLE_OF_THE_SCAVENGER -> "Your physical blows do nothing to tier 2 and 3 creatures. Taking it off leaves the Pheromone Mark for 30 s: "
                    + "every predator within 48 blocks comes for you.";
            case BAND_OF_THE_SPATIAL_ANCHOR -> null;
            case DEVIL_BOUND_SHEATH -> "Left fully charged for more than a minute, it discharges into you: Magicule Poisoning and damage.";
            case GAMBLERS_GRIMOIRE_PAGE -> "Each use rolls a hidden d20. On a 1 the spell backfires: a tenth of your mana pool is lost for the rest of the day, "
                    + "and inside the Dungeon its structure collapses.";
        };
    }

    static String hint(Relic r) {
        return switch (r) {
            case GLUTTONS_CHALICE -> "Eat raw monster meat while it is worn, or use it in your hand with raw meat in the off-hand, to feed it.";
            case GAMBLERS_GRIMOIRE_PAGE -> "Works only in the off-hand.";
            default -> null;
        };
    }
}
