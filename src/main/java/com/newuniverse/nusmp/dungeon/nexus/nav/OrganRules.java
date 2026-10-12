package com.newuniverse.nusmp.dungeon.nexus.nav;

/**
 * What a Butcher Cleaver finds in the organs and in the hide of a Carcass (docs/living_dungeon.md section 6). The carcass only remembers
 * its kind (CarcassBlock.KIND: 0 slime, 1 Armorsaurus, 2 Basilisk, 3 Direwolf, 4 anything else, 5 spider): a <b>Venom Gland</b> comes from
 * a Basilisk or a spider, a <b>Magic Stone</b> from the carcass of anything above tier 1 (a slime never has one; kind 4 is unknown, so it has
 * a smaller chance). Hide and bone give a very little, and a slime, which is jelly all through, has none. Pure.
 */
public final class OrganRules {
    private OrganRules() {}

    public static final int SLIME = 0, BASILISK = 2, ARACHNID = 5;
    /** The chances, per organ cut: a gland where there is one, a magic stone from a known tier 2+ creature, and from the unknown "anything else". */
    public static final double GLAND_CHANCE = 0.6, STONE_CHANCE = 0.35, STONE_UNKNOWN = 0.2;

    /** What the organs of one cut held. */
    public record Haul(boolean gland, boolean stone) {
        public boolean empty() { return !gland && !stone; }
    }

    /** What the hide and bone of one cut gave. */
    public enum Hide { BONE_MEAL_1, BONE_MEAL_2, LEATHER }

    public static boolean hasGland(int kind) { return kind == BASILISK || kind == ARACHNID; }

    /** Is there anything in the organs of a carcass of this kind at all? (A slime's are not worth cutting: the cleaver cuts meat there as before.) */
    public static boolean hasOrgans(int kind) { return hasGland(kind) || stoneChance(kind) > 0.0; }

    /** Is there hide and bone to cut on a carcass of this kind? Everything but the jelly of a slime. */
    public static boolean hasHide(int kind) { return kind != SLIME; }

    /**
     * What a cut really takes from a carcass of this kind when the click said {@code zone}: organs where there are none, and hide where there
     * is none, are the meat cut that the cleaver always made. Anything else stays as it was.
     */
    public static CutZone settle(CutZone zone, int kind) {
        if (zone == CutZone.ORGANS && !hasOrgans(kind)) return CutZone.MEAT;
        if (zone == CutZone.HIDE && !hasHide(kind)) return CutZone.MEAT;
        return zone;
    }

    /** The chance of a Magic Stone from the organs of a carcass of this kind. */
    public static double stoneChance(int kind) {
        return switch (kind) {
            case 1, 2, 3, ARACHNID -> STONE_CHANCE;
            case 4 -> STONE_UNKNOWN;
            default -> 0.0;                                      // a slime (0), or nothing known
        };
    }

    /** One organ cut: each roll is in [0, 1). */
    public static Haul organs(int kind, double glandRoll, double stoneRoll) {
        return new Haul(hasGland(kind) && glandRoll < GLAND_CHANCE, stoneRoll < stoneChance(kind));
    }

    /** One hide cut: a little bone meal (a second one sometimes) or a scrap of leather. */
    public static Hide hide(double roll) { return roll < 0.45 ? Hide.BONE_MEAL_1 : roll < 0.65 ? Hide.BONE_MEAL_2 : Hide.LEATHER; }
}
