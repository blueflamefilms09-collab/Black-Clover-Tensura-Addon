package com.newuniverse.nusmp.heroweapon.rules;

/**
 * Why (3.63.0): the use key of a Legendary weapon in the main hand raised the guard, drew the string or fired a technique and always "consumed" the click, so vanilla never got to try the OFF hand and a
 * food or potion there could not be used. This is the whole decision, with no Minecraft type so it is a unit test: the weapon lets the click pass to the off hand when the off hand holds something the
 * player can use right now (a drink, or a food they are hungry enough to eat) and they are not sneaking (sneak + use stays the weapon's own gesture). Settings off restores the old behaviour.
 */
public final class ConsumablePass {
    private ConsumablePass() {}

    public enum Kind { NONE, FOOD, DRINK }

    /** True when the weapon should return PASS so that the off-hand consumable is used instead. */
    public static boolean offhandFirst(boolean setting, boolean sneaking, Kind offhand, boolean canEatNow) {
        if (!setting || sneaking || offhand == null) return false;
        return offhand == Kind.DRINK || (offhand == Kind.FOOD && canEatNow);
    }
}
