package com.newuniverse.nusmp.dungeon.nexus.collapse;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

/** What the collapse shows a player: a line of chat, a line over the hotbar, a title. Each call is safe to make for a player who just left. */
final class CollapseUi {
    private CollapseUi() {}

    static void chat(ServerPlayer p, String text, ChatFormatting... style) {
        if (!text.isEmpty()) p.sendSystemMessage(Component.literal(text).withStyle(style));
    }

    static void bar(ServerPlayer p, String text, ChatFormatting... style) {
        if (!text.isEmpty()) p.displayClientMessage(Component.literal(text).withStyle(style), true);
    }

    /** A title with a smaller line under it; `stay` is in ticks. */
    static void title(ServerPlayer p, String title, String subtitle, int stay, ChatFormatting... style) {
        if (p.connection == null) return;
        p.connection.send(new ClientboundSetTitlesAnimationPacket(4, stay, 12));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle).withStyle(ChatFormatting.GRAY)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title).withStyle(style)));
    }
}
