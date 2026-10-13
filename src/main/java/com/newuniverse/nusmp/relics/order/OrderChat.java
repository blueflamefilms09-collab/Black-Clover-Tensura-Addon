package com.newuniverse.nusmp.relics.order;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** The Order's lines in chat, as components (relic names are translated by the client). The words are in {@link OrderText}. */
final class OrderChat {
    private OrderChat() {}

    private static MutableComponent order(String text) { return Component.literal(text).withStyle(ChatFormatting.GOLD); }

    /** "[Order] Two of <relic> cannot be." */
    static Component announce(String relicId) {
        return Component.literal(OrderText.PREFIX).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(order(OrderText.TWO_OF)).append(OrderRelics.name(relicId).copy().withStyle(ChatFormatting.YELLOW)).append(order(OrderText.CANNOT_BE));
    }

    /** "[Order] <relic> exists more than once. In N minutes ..." */
    static Component censusWarning(String relicId, int minutes) {
        return Component.literal(OrderText.PREFIX).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(OrderRelics.name(relicId).copy().withStyle(ChatFormatting.YELLOW))
                .append(order(minutes > 0 ? " exists more than once. In " + minutes + " minutes the Order will come for the extra copy."
                        : " exists more than once. The Order will come for the extra copy when the count ends."));
    }

    static Component plain(String text) { return Component.literal(text).withStyle(ChatFormatting.GOLD); }

    /** What a bearer reads when a duplicate was taken from them. */
    static Component taken(List<String> relicIds) {
        MutableComponent c = Component.literal(OrderText.PREFIX).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD).append(order("Struck from the ledger: "));
        for (int i = 0; i < relicIds.size(); i++) {
            if (i > 0) c.append(order(", "));
            c.append(OrderRelics.name(relicIds.get(i)).copy().withStyle(ChatFormatting.YELLOW));
        }
        return c.append(order("."));
    }

    /** Players with the rights of an operator. */
    static List<ServerPlayer> ops(MinecraftServer server) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) if (p.hasPermissions(2)) out.add(p);
        return out;
    }

    static void toOps(MinecraftServer server, Component c) {
        for (ServerPlayer p : ops(server)) p.sendSystemMessage(c);
    }
}
