package com.newuniverse.nusmp.dragonballs;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** The few chat line shapes of the Dragon Balls (all text is a lang key under {@code anime_multiverse.dragonballs.}). */
final class DbMsg {
    private DbMsg() {}

    static final String P = "anime_multiverse.dragonballs.";

    /** The dragon speaking: "Shenron: ..." in the set's colour. */
    static MutableComponent say(BallSet set, String key, Object... args) {
        return Component.literal(set.dragonName() + ": ").withStyle(Style.EMPTY.withColor(set.color()).withBold(true)).append(Component.translatable(P + key, args).withStyle(ChatFormatting.WHITE));
    }

    /** A plain system line. */
    static MutableComponent info(String key, Object... args) { return Component.translatable(P + key, args).withStyle(ChatFormatting.YELLOW); }

    static MutableComponent bad(String key, Object... args) { return Component.translatable(P + key, args).withStyle(ChatFormatting.RED); }
}
