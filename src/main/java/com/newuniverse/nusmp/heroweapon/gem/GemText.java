package com.newuniverse.nusmp.heroweapon.gem;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Why: every sentence the acquisition system shows is translatable but must still read correctly when the lang file has not been merged or lacks a key (the
 * lang fragment travels separately from the code), so each one is built with the English text of {@link GemMessages} as the fallback. One helper, so no call
 * site can forget the fallback or spell it differently from the lang file.
 */
final class GemText {
    private GemText() {}

    /** A translatable line with its English fallback; {@code args} fill the {@code %s} placeholders in order. */
    static MutableComponent msg(String key, Object... args) { return Component.translatableWithFallback(key, GemMessages.en(key), args); }
}
