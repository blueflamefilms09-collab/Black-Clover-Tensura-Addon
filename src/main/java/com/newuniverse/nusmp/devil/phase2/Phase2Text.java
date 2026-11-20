package com.newuniverse.nusmp.devil.phase2;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: every line the contract rules of phase 2 say to a player is a translatable line with its English as the fallback ({@code anime_multiverse.devil.phase2.<key>}); the English texts are merged append-only
 * into en_us.json from the calls of this class by {@code tools/lang/gen_devil_phase2.py}, so the lang file and the code can not drift apart. Every call must be written as {@code Phase2Text.t("key", "English %s", args)}
 * with two string literals (the generator reads them).
 */
final class Phase2Text {
    private Phase2Text() {}

    static MutableComponent t(String key, String fallback, Object... args) { return Component.translatableWithFallback("anime_multiverse.devil.phase2." + key, fallback, args); }

    static void say(ServerPlayer p, ChatFormatting color, MutableComponent c) { p.sendSystemMessage(c.withStyle(color)); }
}
