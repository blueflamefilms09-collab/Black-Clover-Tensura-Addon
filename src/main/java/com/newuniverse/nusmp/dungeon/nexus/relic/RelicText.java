package com.newuniverse.nusmp.dungeon.nexus.relic;

/** The words of the relics' tooltips: what each does and what it costs (docs/living_dungeon.md section 7). */
final class RelicText {
    private RelicText() {}

    /** Where a relic goes: every one is worn in one of the two Dungeon Relic slots (the Page, being an artifact, is called one). */
    static String where(Relic r) {
        return (r == Relic.GAMBLERS_GRIMOIRE_PAGE ? "Artifact" : "Wondrous item") + " - worn in a Dungeon Relic slot (you have two)";
    }

    static String effect(Relic r) {
        return switch (r) {
            case GLUTTONS_CHALICE -> "Digests toxic monster meat into a rich stew: no sickness, immense saturation, and for a few minutes the Dungeon's thin air, "
                    + "scorched floors and magicule radiation cannot touch you.";
            case MANTLE_OF_THE_SCAVENGER -> "Registers you as a Tier 1 Scavenger: the apex predators of the Dungeon pay you no mind at all.";
            case BAND_OF_THE_SPATIAL_ANCHOR -> "Holds you fast in space: the Dungeon's spatial traps and gravity pulses cannot move you, and you may place blocks "
                    + "in its volatile zones.";
            case DEVIL_BOUND_SHEATH -> "Drinks the magicules of magic you parry or block. Once full, your next melee strike releases a cross-dimensional "
                    + "Anti-Magic slash that ignores armour and magic nullification.";
            case GAMBLERS_GRIMOIRE_PAGE -> "Worn in a Dungeon Relic slot, press the Use Relic key (or hold it in the off-hand and use it) to cast a random, "
                    + "overpowered spell. It rests for a minute after each use.";
            case AMULET_OF_THE_JUGGERNAUTS_BLINDNESS -> "Masks your mana signature: the massive spells you cast no longer trigger the Dungeon's Juggernaut immune response.";
            default -> "";
        };
    }

    static String curse(Relic r) {
        return switch (r) {
            case GLUTTONS_CHALICE -> "It is alive. Unfed for three in-game days, it drains your hunger and your magicules until it is fed.";
            case MANTLE_OF_THE_SCAVENGER -> "Your physical blows do nothing to tier 2 and 3 creatures. Taking it off leaves the Pheromone Mark for 30 s: "
                    + "every predator within 48 blocks comes for you.";
            case BAND_OF_THE_SPATIAL_ANCHOR -> null;
            case DEVIL_BOUND_SHEATH -> "Left fully charged for more than a minute, it discharges into you: Magicule Poisoning and damage.";
            case GAMBLERS_GRIMOIRE_PAGE -> "Each use rolls a hidden d20. On a 1 the spell backfires: a tenth of your mana pool is drained for the day, "
                    + "and inside the Dungeon its immune system is set on you.";
            case AMULET_OF_THE_JUGGERNAUTS_BLINDNESS -> "Your natural mana regeneration is halved while it is attuned and worn.";
            default -> null;
        };
    }

    static String hint(Relic r) {
        return switch (r) {
            case GLUTTONS_CHALICE -> "Eat raw monster meat while it is worn, or use it in your hand with raw meat in the off-hand, to feed it.";
            case GAMBLERS_GRIMOIRE_PAGE -> "Works from a Dungeon Relic slot (the Use Relic key) or from the off-hand (use it).";
            default -> null;
        };
    }

    /** What the action bar says when an attunement starts over. */
    static String reset(AttuneClock.Why why) {
        return switch (why) {
            case MOVED -> "You moved: the attunement slips away. Stand still, sneaking, and begin again.";
            case HURT -> "A blow broke the attunement. Stand still, sneaking, and begin again.";
            default -> "The attunement slips away. Stand still, sneaking, and begin again.";
        };
    }
}
