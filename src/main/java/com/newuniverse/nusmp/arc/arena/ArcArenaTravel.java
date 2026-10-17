package com.newuniverse.nusmp.arc.arena;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arena.ArenaTravel;
import com.newuniverse.nusmp.arena.ReturnBook;
import com.newuniverse.nusmp.arena.ZogratisArena;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: going to a story arena and coming home are the two moves that can lose a player, and they must be exactly symmetric, as the Final Hour's are: the way in
 * records where each traveller stood ({@code ReturnBook}, in the arena save, so a restart does not forget), the way out takes that record back and sets the
 * player down there, and when there is no record (a command, an old save, a dimension that no longer exists) the way out falls back to the player's own respawn
 * point and then to the world spawn, never to the void. The arena is built on the first arrival ({@link ArcArenaBuilder}), the travellers are set down on the
 * columns the shape names for them, turned towards the boss's place, one block above the surface so they never appear inside a block. The character the
 * Chronicle carries the party to is the one whose arena it is: a character that is fought in the Final Hour's courtyard (Lucius) is carried there through the
 * Hourglass's own travel, whose director, guard and way home are already built and tested.
 */
public final class ArcArenaTravel {
    private ArcArenaTravel() {}

    /** Takes {@code user} and the companions into the arena of {@code boss}. Returns false (with a message to the user) when it cannot be reached. */
    public static boolean enter(ServerPlayer user, List<ServerPlayer> companions, ArcCharacter boss) {
        MinecraftServer server = user.getServer();
        if (server == null) return false;
        if (ArenaShapes.usesFinalHour(boss)) {
            ArenaTravel.enter(user, companions);                                                // his courtyard, his director, his Return Sigils
            for (ServerPlayer p : everyone(user, companions)) NusmpEvents.fire(p, ARENA_KEY, boss.id());
            return true;
        }
        ServerLevel arena = server.getLevel(ArcArena.DIMENSION);
        if (arena == null) {
            user.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.arena.no_dimension", "The arena does not answer: its dimension is not loaded.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        ArcArenaSave save = ArcArenaSave.get(server);
        int slot = save.slotFor(boss.id());
        if (slot < 0) {
            user.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.arena.no_room", "Every site of the arenas is taken. An operator can free one with /multiverse arc arena status.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        ArenaShape shape = ArenaShapes.forCharacter(boss);
        ArcArenaBuilder.ensureBuilt(arena, save, shape, slot);
        List<ServerPlayer> all = everyone(user, companions);
        int[][] spots = shape.arrivalSpots(Math.min(all.size(), ArenaShape.MAX_ARRIVALS));
        for (int i = 0; i < all.size(); i++) {
            ServerPlayer p = all.get(i);
            if (!ArcArena.in(p) && !ZogratisArena.in(p)) {
                save.returns().put(p.getUUID(), new ReturnBook.Entry(p.level().dimension().location().toString(), p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot()));
                save.setDirty();
            }
            int[] s = spots[i % spots.length];
            var at = ArcArena.arrivalStand(shape, slot, s);
            p.teleportTo(arena, at.x, at.y, at.z, ArcArena.yawTowardsBoss(shape, s), 0f);
            p.fallDistance = 0;
            p.sendSystemMessage(Component.translatableWithFallback(p == user ? "message.nusmp.arc.arena.entered" : "message.nusmp.arc.arena.carried",
                    p == user ? "You step into %s." : "%s carried you into %s.", p == user ? new Object[] {shape.title()} : new Object[] {user.getName().getString(), shape.title()}).withStyle(ChatFormatting.GOLD));
            ArcArenaDirector.welcome(server, arena, p, slot, boss);
            NusmpEvents.fire(p, ARENA_KEY, boss.id());
        }
        return true;
    }

    /** The quest key fired for each player carried to a story arena (the argument is the boss's id). */
    public static final String ARENA_KEY = "arc.arena";

    private static List<ServerPlayer> everyone(ServerPlayer user, List<ServerPlayer> companions) {
        List<ServerPlayer> all = new ArrayList<>();
        all.add(user);
        for (ServerPlayer c : companions) if (c != user && !all.contains(c)) all.add(c);
        return all;
    }

    /** Sends {@code p} home from a story arena: to the recorded place, or to their respawn point, or to the world spawn. Does nothing outside the arenas' dimension. */
    public static boolean leave(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null) return false;
        if (ZogratisArena.in(p)) return ArenaTravel.leave(p);
        if (!ArcArena.in(p)) return false;
        ArcArenaSave save = ArcArenaSave.get(server);
        ReturnBook.Entry e = save.returns().take(p.getUUID());
        save.setDirty();
        ServerLevel to = null;
        double x = 0, y = 0, z = 0;
        float yaw = p.getYRot(), pitch = 0f;
        if (e != null) {
            ResourceLocation id = ResourceLocation.tryParse(e.dimension());
            if (id != null) to = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
            if (to != null) {
                x = e.x();
                y = e.y();
                z = e.z();
                yaw = e.yaw();
                pitch = e.pitch();
            }
        }
        if (to == null) {
            ServerLevel respawnLevel = server.getLevel(p.getRespawnDimension());
            BlockPos rp = p.getRespawnPosition();
            if (respawnLevel != null && rp != null && !ArcArena.in(respawnLevel) && !ZogratisArena.in(respawnLevel)) {
                to = respawnLevel;
                x = rp.getX() + 0.5;
                y = rp.getY();
                z = rp.getZ() + 0.5;
            } else {
                to = server.overworld();
                BlockPos sp = to.getSharedSpawnPos();
                x = sp.getX() + 0.5;
                y = to.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, sp).getY();
                z = sp.getZ() + 0.5;
            }
        }
        p.teleportTo(to, x, y, z, yaw, pitch);
        p.fallDistance = 0;
        p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.arena.home", "You are carried home from the arena.").withStyle(ChatFormatting.GOLD));
        return true;
    }
}
