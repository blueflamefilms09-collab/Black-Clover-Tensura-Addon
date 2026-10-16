package com.newuniverse.nusmp.arena;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: going to the arena and coming home are the two moves that can lose a player, and they must be exactly symmetric: the way in
 * records where each traveller stood ({@link ReturnBook}, in the arena's saved data, so a restart does not forget), the way out takes that record
 * back and sets the player down there, and when there is no record (a command, an old save, a dimension that no longer exists) the way out
 * falls back to the player's own respawn point and then to the world spawn, never to the void. The courtyard is built on the first arrival
 * ({@link ArenaBuilder}), and the travellers are set down on a ring beside the dais, facing the middle, two blocks above the floor so they never
 * appear inside a block. The teleport is {@code ServerPlayer.teleportTo(ServerLevel, ...)}, the call the Labyrinth gate already relies on (it carries the player
 * across dimensions and keeps the inventory). Nothing here touches game rules or inventories.
 */
public final class ArenaTravel {
    private ArenaTravel() {}

    /** Takes 'user' and the 'companions' into the arena. Returns false (with a message to the user) if the arena cannot be reached. */
    public static boolean enter(ServerPlayer user, List<ServerPlayer> companions) {
        MinecraftServer server = user.getServer();
        ServerLevel arena = server == null ? null : server.getLevel(ZogratisArena.DIMENSION);
        if (arena == null) {
            user.sendSystemMessage(Component.literal("The Final Hour does not answer: its dimension is not loaded.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        ArenaSave save = ArenaSave.get(server);
        ArenaBuilder.ensureBuilt(arena, save);
        List<ServerPlayer> all = new ArrayList<>();
        all.add(user);
        for (ServerPlayer c : companions) if (c != user && !all.contains(c)) all.add(c);
        int[][] spots = ArenaLayout.arrivalSpots(all.size());
        for (int i = 0; i < all.size(); i++) {
            ServerPlayer p = all.get(i);
            if (!ZogratisArena.in(p)) {
                save.returns().put(p.getUUID(), new ReturnBook.Entry(p.level().dimension().location().toString(), p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot()));
                save.setDirty();
            }
            int[] s = spots[i % spots.length];
            float yaw = (float) Math.toDegrees(Math.atan2(s[0], -s[1]));
            p.teleportTo(arena, s[0] + 0.5, ArenaLayout.FLOOR_Y + ArenaLayout.ARRIVAL_DY, s[1] + 0.5, yaw, 0f);
            p.fallDistance = 0;
            p.sendSystemMessage(Component.literal(p == user ? "You step into the arena of the Final Hour." : user.getName().getString() + " carried you into the arena of the Final Hour.")
                    .withStyle(ChatFormatting.GOLD));
            ArenaDirector.welcome(server, arena, p);
        }
        return true;
    }

    /** Sends 'p' home: to the recorded place, or to their respawn point, or to the world spawn. Does nothing outside the arena. */
    public static boolean leave(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null || !ZogratisArena.in(p)) return false;
        ArenaSave save = ArenaSave.get(server);
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
            if (respawnLevel != null && rp != null && !ZogratisArena.in(respawnLevel)) {
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
        p.sendSystemMessage(Component.literal("You are carried home from the Final Hour.").withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** Where a player stands, as the hourglass rules see it. */
    public static HourglassRules.Place placeOf(Level level) {
        if (ZogratisArena.in(level)) return HourglassRules.Place.ARENA;
        if (com.newuniverse.nusmp.labyrinth.Labyrinth.in(level) || com.newuniverse.nusmp.dungeon.LivingDungeon.in(level)) return HourglassRules.Place.FORBIDDEN;
        return HourglassRules.Place.FREE;
    }
}
