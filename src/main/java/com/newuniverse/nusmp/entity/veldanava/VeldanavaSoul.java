package com.newuniverse.nusmp.entity.veldanava;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: Soul Seizing is the Creator God's anti-Totem. A player who would die to him or to the Dark Avatar is not killed (so no Totem of Undying can answer
 * it and no death screen follows); their soul is taken and they watch the rest of the fight in spectator mode. It must never leave anyone stuck there, so the
 * way back is built three times: the boss frees everybody when the fight ends, when he is removed and when the fight is reset; a player who logs in with the
 * marker and no fight holding them is freed at once (a server restart in the middle of a fight); and the marker is kept on the player (their persistent data),
 * so it survives a restart. Per-player state is keyed by UUID; nothing here touches the client.
 */
public final class VeldanavaSoul {
    private VeldanavaSoul() {}

    /** The game mode a seized player had, kept on the player. */
    public static final String TAG_MODE = "nusmp_veldanava_prev_mode";

    /** player -> the boss holding them (server thread only; cleared when the server stops). */
    private static final Map<UUID, UUID> SEIZED = new HashMap<>();

    public static boolean isSeized(UUID player) { return SEIZED.containsKey(player); }

    public static int count() { return SEIZED.size(); }

    /** Takes the soul of 'p' for 'boss': false if they cannot be (creative, spectator, already seized). */
    public static boolean seize(ServerPlayer p, UUID boss) {
        if (p.isCreative() || p.isSpectator() || SEIZED.containsKey(p.getUUID())) return false;
        p.getPersistentData().putString(TAG_MODE, p.gameMode.getGameModeForPlayer().getName());
        if (p.getHealth() < 1f) p.setHealth(1f);
        p.setGameMode(GameType.SPECTATOR);
        SEIZED.put(p.getUUID(), boss);
        p.displayClientMessage(Component.literal("Your soul is seized. You will return when the Creator falls.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        return true;
    }

    /** Gives 'p' back their body (their old game mode, survival if it is unknown). */
    public static void restore(ServerPlayer p) {
        String mode = p.getPersistentData().getString(TAG_MODE);
        p.getPersistentData().remove(TAG_MODE);
        SEIZED.remove(p.getUUID());
        if (p.isSpectator()) p.setGameMode(GameType.byName(mode, GameType.SURVIVAL));
        if (p.getHealth() < 1f) p.setHealth(1f);
    }

    /** Frees everybody this boss holds. Returns how many were put back (the offline ones are put back when they log in). */
    public static int release(MinecraftServer server, UUID boss) {
        List<UUID> mine = new ArrayList<>();
        for (Map.Entry<UUID, UUID> e : SEIZED.entrySet()) if (e.getValue().equals(boss)) mine.add(e.getKey());
        int back = 0;
        for (UUID id : mine) {
            ServerPlayer p = server == null ? null : server.getPlayerList().getPlayer(id);
            SEIZED.remove(id);
            if (p != null) { restore(p); back++; }
        }
        return back;
    }

    /** A player who logs in with the marker and no fight holding them. */
    public static void onLogin(ServerPlayer p) {
        if (!p.getPersistentData().contains(TAG_MODE) || SEIZED.containsKey(p.getUUID())) return;
        restore(p);
    }

    public static void clear() { SEIZED.clear(); }
}
