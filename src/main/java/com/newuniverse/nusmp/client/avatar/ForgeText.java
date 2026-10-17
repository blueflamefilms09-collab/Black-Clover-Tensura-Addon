package com.newuniverse.nusmp.client.avatar;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Why: every line of the Soul Forge screen is written once, in the code, as a translation key together with its English words, and this is the only way the
 * screen makes a text. {@code translatableWithFallback} shows the English words when the language file does not (yet) hold the key, so the screen is never
 * full of raw keys, and the same pair is what {@code tools/avatar/gen_soulforge_lang.py} reads to write the language fragment, so the file and the code cannot
 * drift apart. Keep each call on one line with both strings as plain literals, or the tool will not see it.
 */
final class ForgeText {
    private ForgeText() {}

    static MutableComponent t(String key, String fallback, Object... args) { return Component.translatableWithFallback(key, fallback, args); }

    static String s(String key, String fallback, Object... args) { return t(key, fallback, args).getString(); }
}
