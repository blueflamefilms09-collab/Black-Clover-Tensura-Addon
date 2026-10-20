package com.newuniverse.nusmp.labyrinth.mega.admin;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: one place builds every message of the admin tree, so the colour code is the same everywhere (GREEN = it worked, RED = it was refused or failed, YELLOW = it worked with a
 * warning or an instruction, GRAY = a note) and every sentence is a translation key with its English text from {@link AdminText} as the fallback. The arguments handed to a
 * component are only strings, numbers or other components, which is all the game can send to a client. A message to a player who has no connection (a self-test's stand-in) is
 * dropped instead of failing.
 */
final class AdminMsg {
    private AdminMsg() {}

    static MutableComponent text(String key, Object... args) { return Component.translatableWithFallback(key, AdminText.english(key), args); }

    static MutableComponent green(String key, Object... args) { return text(key, args).withStyle(ChatFormatting.GREEN); }

    static MutableComponent red(String key, Object... args) { return text(key, args).withStyle(ChatFormatting.RED); }

    static MutableComponent yellow(String key, Object... args) { return text(key, args).withStyle(ChatFormatting.YELLOW); }

    static MutableComponent gray(String key, Object... args) { return text(key, args).withStyle(ChatFormatting.GRAY); }

    /** Replies to the command's sender with a success line (not echoed to the other operators) and returns 1. */
    static int ok(CommandSourceStack src, MutableComponent c) {
        src.sendSuccess(() -> c, false);
        return 1;
    }

    /** Replies with a failure line (red) and returns 0. */
    static int fail(CommandSourceStack src, MutableComponent c) {
        src.sendFailure(c);
        return 0;
    }

    /** A chat line to a player who can read it (a stand-in has no connection and nobody to tell). */
    static void tell(ServerPlayer p, Component c) {
        if (p != null && p.connection != null) p.sendSystemMessage(c);
    }

    /** A line above the hotbar. */
    static void bar(ServerPlayer p, Component c) {
        if (p != null && p.connection != null) p.displayClientMessage(c, true);
    }
}
