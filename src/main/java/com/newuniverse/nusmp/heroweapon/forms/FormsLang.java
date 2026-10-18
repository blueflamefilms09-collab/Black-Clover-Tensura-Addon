package com.newuniverse.nusmp.heroweapon.forms;

import net.minecraft.network.chat.Component;

/**
 * Why: every line the forms system says (a chat line, an action-bar line, a title, a label in the Weapon Book) is a translatable key with its English text
 * carried along as the fallback, so a lang file that was not merged yet, or a player whose game has no entry, still reads as plain English instead of a raw key.
 * The keys are listed in the lang fragment {@code hforms.lang.json}; the lead merges it into {@code en_us.json}. One tiny class keeps the habit in one place.
 */
public final class FormsLang {
    private FormsLang() {}

    /** A translatable component whose fallback is the English text ({@code %s} placeholders work in both). */
    public static Component t(String key, String english, Object... args) { return Component.translatableWithFallback(key, english, args); }
}
