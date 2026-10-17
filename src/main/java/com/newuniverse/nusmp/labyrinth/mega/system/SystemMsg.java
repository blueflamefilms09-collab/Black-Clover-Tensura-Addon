package com.newuniverse.nusmp.labyrinth.mega.system;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Why: every line the server says (an action-bar hint, a chat line, a command reply, a tooltip) must be a TRANSLATABLE component, so each player reads it in
 * their own language, and must still read properly before the lang fragment is merged or when a key is missing. {@code Component.translatableWithFallback}
 * does both, if it is given the English text; this helper looks that text up in {@link SystemText}, the one map the lang fragment is also written from, so no
 * call site repeats an English string. Format arguments use the numbered {@code %1$s} style of the texts.
 */
final class SystemMsg {
    private SystemMsg() {}

    static MutableComponent text(String key, Object... args) {
        return Component.translatableWithFallback(key, SystemText.english(key), args);
    }
}
