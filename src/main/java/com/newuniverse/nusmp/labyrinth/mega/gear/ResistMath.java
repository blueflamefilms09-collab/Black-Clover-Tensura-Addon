package com.newuniverse.nusmp.labyrinth.mega.gear;

/**
 * Why: the magic resistance of Rare gear is a few lines of arithmetic that decide how much damage a player takes, so they are written once, with no
 * Minecraft type, and pinned by tests: WHICH pieces count (armour only worn in its own slot, a weapon or shield only held; otherwise a chestplate carried in
 * the hand would resist magic, and a sword stuck on the head by a command would count twice), HOW MUCH they add up to (the pieces' percentages are summed,
 * then held to the configured ceiling so a full set of Rare gear cannot make a player immune to magic, which would make every spell of every monster
 * and boss pointless), and how the percentage turns into a smaller hit (a straight multiplier, never below zero, never above the incoming amount).
 * The game part ({@code GearEvents}) only walks the entity's equipment and asks these.
 */
public final class ResistMath {
    private ResistMath() {}

    /** The hardest ceiling the config may ask for: even the owner cannot make the gear grant more than 90 percent. */
    public static final int HARD_CAP = 90;

    /**
     * Does a piece of this slot count when found in an equipment position? {@code wornIndex} is 0..3 for the helmet, chestplate, leggings and boots
     * positions and any negative number for a hand. Armour counts only in the position of its own kind; a weapon or shield only in a hand.
     */
    public static boolean counts(GearSlot slot, int wornIndex) {
        if (slot.isArmour()) return wornIndex == slot.armourIndex();
        return wornIndex < 0;
    }

    /** The sum held to the ceiling (the ceiling itself is held to 0..{@link #HARD_CAP}); 0 or less gives 0. */
    public static int effective(int sum, int ceiling) {
        int cap = Math.max(0, Math.min(HARD_CAP, ceiling));
        return Math.max(0, Math.min(sum, cap));
    }

    /** The amount after the resistance: {@code amount * (100 - percent) / 100}; unchanged for a percent of 0 or less, and never negative. */
    public static float reduce(float amount, int percent) {
        if (percent <= 0 || amount <= 0f) return amount;
        int p = Math.min(100, percent);
        return Math.max(0f, amount * (100 - p) / 100f);
    }
}
