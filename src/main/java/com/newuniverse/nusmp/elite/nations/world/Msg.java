package com.newuniverse.nusmp.elite.nations.world;

import com.newuniverse.nusmp.elite.nations.core.Nation;
import com.newuniverse.nusmp.elite.nations.core.NationText;
import com.newuniverse.nusmp.elite.nations.core.Res;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Why: one place that turns a message key into a chat line. Every line is {@code Component.translatableWithFallback(key, english, args)}: the English text of {@link NationText} is the fallback, so the
 * system reads correctly even before the lang fragment {@code tools/elite/nations_lang.json} is merged into en_us.json, and a resource pack can still translate it.
 */
public final class Msg {
    private Msg() {}

    public static Component of(String key, Object... args) { return Component.translatableWithFallback(key, NationText.en(key), args); }

    public static Component of(Res r) { return of(r.key(), r.args()); }

    public static Component colored(Res r) { return of(r).copy().withStyle(r.ok() ? ChatFormatting.GREEN : ChatFormatting.RED); }

    public static void tell(ServerPlayer p, Res r) { if (p != null) p.sendSystemMessage(colored(r)); }

    public static void tell(ServerPlayer p, String key, Object... args) { if (p != null) p.sendSystemMessage(of(key, args)); }

    /** A line to every ONLINE member of a nation. */
    public static void broadcast(MinecraftServer server, Nation n, String key, Object... args) {
        if (server == null || n == null) return;
        Component c = of(key, args);
        for (UUID id : n.members.keySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) p.sendSystemMessage(c);
        }
    }

    /** A line to everyone on the server (declarations of war, end of a war). */
    public static void everyone(MinecraftServer server, String key, Object... args) {
        if (server == null) return;
        server.getPlayerList().broadcastSystemMessage(of(key, args), false);
    }
}
