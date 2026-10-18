package com.newuniverse.nusmp.heroweapon;

import java.util.Locale;

/**
 * Why: a form of a weapon gives its wielder up to five techniques, each on its own input, and the forms JSON names the input by a slot word. One enum maps
 * the input to the slot word so the item code, the keybinds, the forms data and the weapon builders agree. {@link #USE} is the right click (a blocking Shield
 * does not have it: the use key raises the shield), {@link #SNEAK_USE} is sneak + right click, {@link #KEY_1} to {@link #KEY_3} are the three hero keys
 * (config and keybind of the core; they work with the weapon summoned).
 */
public enum HeroTrigger {
    USE, SNEAK_USE, KEY_1, KEY_2, KEY_3;

    /** The slot word in the forms JSON: {@code use}, {@code sneak}, {@code key1}, {@code key2}, {@code key3}. */
    public String slot() {
        return switch (this) {
            case USE -> "use";
            case SNEAK_USE -> "sneak";
            default -> "key" + (ordinal() - 1);
        };
    }

    /** The trigger of a slot word, or null. */
    public static HeroTrigger bySlot(String slot) {
        if (slot == null) return null;
        String s = slot.toLowerCase(Locale.ROOT);
        for (HeroTrigger t : values()) if (t.slot().equals(s)) return t;
        return null;
    }
}
