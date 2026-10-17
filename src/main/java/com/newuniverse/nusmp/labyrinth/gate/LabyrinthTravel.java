package com.newuniverse.nusmp.labyrinth.gate;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.LabyrinthConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the one place that moves players between the Overworld and the Labyrinth, so the Gate block, the enter command and the void rescue all
 * take the same road and leave the same marks. A way in is the Hub's or the Mega-Dungeon's ({@link #enter}); since 1.58.0 the Mega-Dungeon is the
 * only dungeon (the Spire of 1.35.0 and the Living Dungeon's caves were removed, and with them the {@code labyrinth.engine} and
 * {@code mega.layout} switches that chose between them). The way back is remembered in the player's persistent data
 * ({@code nusmp_labyrinth_origin}: dimension id, x, y, z, yaw, pitch; {@code nusmp_labyrinth_pad}: the arrival pad the void rescue returns to),
 * because persistent data survives a relog and a server restart while a static map would not. The teleport itself is
 * {@code ServerPlayer.teleportTo(ServerLevel, x, y, z, yaw, pitch)}: the proven call, which moves the player across dimensions, sends the level
 * change to the client and keeps the inventory; the 1.21 alternative, {@code changeDimension(DimensionTransition)}, is built for portals that
 * compute their own exit and would only add a record round the same numbers here. The gate cooldown stamps {@code nusmp_labyrinth_gate}.
 * A player found in the dimension of the removed Living Dungeon (a world that remembers it keeps it registered so it still decodes) is carried
 * out by {@link #evacuate}.
 */
public final class LabyrinthTravel {
    private LabyrinthTravel() {}

    static final String ORIGIN = "nusmp_labyrinth_origin", PAD = "nusmp_labyrinth_pad", GATE_USED = "nusmp_labyrinth_gate", RESCUED = "nusmp_labyrinth_rescued";
    /** Where the removed Living Dungeon remembered the way back (dimension id, x, y, z, yaw, pitch): read only to carry a player home from its dimension. */
    static final String LIVING_ORIGIN = "nusmp_dungeon_origin";
    /** The stamp the removed Dungeon Maw read. Nothing here reads it any more; the Hub still writes it ({@code HubKeys.LEGACY_GATE_USED}) and its test pins this spelling. */
    static final String LEGACY_GATE_USED = "nusmp_dungeon_gate";

    // ---------------------------------------------------------------- in
    /**
     * Takes a player through a gate at {@code from}: a gate of Floor 0 leads onto its road, the Gateway of the Overworld leads to Floor 0 (the Hub)
     * or, when the Hub is switched off, straight into the Mega-Dungeon's first road. False if there is nowhere to go (the message says why).
     */
    public static boolean enter(ServerPlayer p, BlockPos from) {
        if (Labyrinth.inSanctuary(p.level())) return com.newuniverse.nusmp.labyrinth.mega.hub.HubTravel.useGate(p, from);      // 1.55.0: a gate of Floor 0 (the Sanctuary Hub) leads onto its road
        if (com.newuniverse.nusmp.labyrinth.mega.hub.HubTravel.enabled() && com.newuniverse.nusmp.labyrinth.mega.hub.HubTravel.available(p.getServer()))
            return com.newuniverse.nusmp.labyrinth.mega.hub.HubTravel.enterFromOverworld(p, from);                               // 1.55.0: the Gateway leads to Floor 0 first (hub.enabled=false restores the 1.54.0 road straight into sector 0)
        return com.newuniverse.nusmp.labyrinth.mega.travel.MegaTravel.enter(p, from);
    }

    /**
     * 1.54.0: the common tail of a way in that already knows its pad (the Mega-Dungeon's Landings): generate the pad's chunk first so nobody
     * arrives in an ungenerated void, remember where the player came from unless they are already inside a Labyrinth dimension (a trip
     * between two Landings must not overwrite the way home), store the pad for the void rescue, stamp the gate cooldown, teleport and tell
     * them. Public because {@code MegaTravel} lives in another package and the origin / pad keys are package-private here.
     */
    public static void arrive(ServerPlayer p, ServerLevel level, BlockPos pad, String message) {
        level.getChunk(pad.getX() >> 4, pad.getZ() >> 4);
        if (!Labyrinth.in(p)) rememberOrigin(p);
        p.getPersistentData().putLong(PAD, pad.asLong());
        stamp(p, level);
        p.teleportTo(level, pad.getX() + 0.5, pad.getY(), pad.getZ() + 0.5, p.getYRot(), 0f);
        level.playSound(null, pad, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5f, 0.6f);
        if (message != null) p.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.DARK_PURPLE));
    }

    // ---------------------------------------------------------------- out
    /** Takes a player of the Labyrinth back to where they came in from, or to their bed / the world spawn if that is lost. */
    public static void leave(ServerPlayer p) {
        if (!Labyrinth.in(p)) return;
        MinecraftServer server = p.getServer();
        if (server == null) return;
        if (Labyrinth.inMega(p.level()) && com.newuniverse.nusmp.labyrinth.mega.hub.HubTravel.enabled() && com.newuniverse.nusmp.labyrinth.mega.hub.HubTravel.available(server)) {
            com.newuniverse.nusmp.labyrinth.mega.hub.HubTravel.returnToPlaza(p);       // 1.55.0: leaving a road of the Mega-Dungeon brings the player back to Floor 0 (it reads the stored origin, so it runs BEFORE forget)
            forget(p);
            return;
        }
        CompoundTag data = p.getPersistentData();
        CompoundTag o = data.getCompound(ORIGIN);
        ServerLevel to = o.isEmpty() ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(o.getString("Dim"))));
        if (GateRules.goesHome(to != null, to != null && Labyrinth.in(to))) {     // the origin is lost, or was the Labyrinth itself: home
            ServerLevel home = server.getLevel(p.getRespawnDimension());
            if (home == null || Labyrinth.in(home)) home = server.overworld();
            BlockPos s = p.getRespawnPosition() != null ? p.getRespawnPosition() : home.getSharedSpawnPos();
            to = home;
            p.teleportTo(home, s.getX() + 0.5, s.getY(), s.getZ() + 0.5, p.getYRot(), p.getXRot());
        } else {
            p.teleportTo(to, o.getDouble("X"), o.getDouble("Y"), o.getDouble("Z"), o.getFloat("Yaw"), o.getFloat("Pitch"));
        }
        forget(p);
        stamp(p, to);
        to.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5f, 0.8f);
        p.sendSystemMessage(Component.literal("The Labyrinth lets you go. The arch stands quiet behind you.").withStyle(ChatFormatting.GRAY));
    }

    /** Leaves the dungeon the player is in (the Labyrinth, by {@link #leave}); nothing elsewhere. */
    public static void leaveAny(ServerPlayer p) {
        if (Labyrinth.in(p)) leave(p);
    }

    /**
     * Carries a player out of the dimension of the removed Living Dungeon (1.58.0): back to where the Maw took them from if that is remembered and
     * still there, else to their bed or the world spawn. A world that remembers the dimension keeps it registered so it still decodes, but no gate
     * leads out of it any more. False when the player is not in it.
     */
    public static boolean evacuate(ServerPlayer p) {
        if (!Labyrinth.inRemovedDungeon(p.level())) return false;
        MinecraftServer server = p.getServer();
        if (server == null) return false;
        CompoundTag data = p.getPersistentData();
        CompoundTag o = data.getCompound(LIVING_ORIGIN);
        ServerLevel to = o.isEmpty() ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(o.getString("Dim"))));
        if (to == null || Labyrinth.inRemovedDungeon(to)) {
            to = server.getLevel(p.getRespawnDimension());
            if (to == null || Labyrinth.inRemovedDungeon(to)) to = server.overworld();
            BlockPos s = p.getRespawnPosition() != null && to.dimension() == p.getRespawnDimension() ? p.getRespawnPosition() : to.getSharedSpawnPos();
            p.teleportTo(to, s.getX() + 0.5, s.getY(), s.getZ() + 0.5, p.getYRot(), p.getXRot());
        } else {
            p.teleportTo(to, o.getDouble("X"), o.getDouble("Y"), o.getDouble("Z"), o.getFloat("Yaw"), o.getFloat("Pitch"));
        }
        data.remove(LIVING_ORIGIN);
        p.resetFallDistance();
        p.sendSystemMessage(Component.literal("The old Dungeon below the world is gone. You are carried back up.").withStyle(ChatFormatting.GRAY));
        return true;
    }

    // ---------------------------------------------------------------- the void
    /**
     * Catches a player who fell off the Spire: back onto the arrival pad with {@link GateRules#RESCUE_DAMAGE}, or out of the Labyrinth altogether
     * if the pad is unknown or let them fall twice within {@link GateRules#RESCUE_RETRY_TICKS}. False when the rescue is switched off (the void
     * then kills as usual) or the player is not in the Labyrinth.
     */
    public static boolean rescue(ServerPlayer p) {
        if (!Labyrinth.in(p) || !LabyrinthConfig.get(LabyrinthConfig.VOID_RESCUE)) return false;
        CompoundTag data = p.getPersistentData();
        long now = p.level().getGameTime();
        boolean out = GateRules.rescueTakesOut(data.contains(PAD), data.contains(RESCUED), now, data.getLong(RESCUED));
        data.putLong(RESCUED, now);
        p.resetFallDistance();
        p.setDeltaMovement(Vec3.ZERO);
        if (out) {
            leave(p);
            return true;
        }
        BlockPos pad = BlockPos.of(data.getLong(PAD));
        p.teleportTo(p.serverLevel(), pad.getX() + 0.5, pad.getY(), pad.getZ() + 0.5, p.getYRot(), 0f);
        p.hurt(p.damageSources().magic(), GateRules.RESCUE_DAMAGE);
        p.sendSystemMessage(Component.literal("The Gate refuses to let the fall end you.").withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    // ---------------------------------------------------------------- marks in the player's persistent data
    /** Has this player just used a gate (so the block they were set down in does not send them straight back)? */
    public static boolean justTravelled(ServerPlayer p) {
        return GateRules.cooldownHolds(p.level().getGameTime(), p.getPersistentData().getLong(GATE_USED));
    }

    /** The pad {@link #arrive} last set this player down on, or null when there is none (read by the self-tests: a connection-less stand-in does not move on a same-level teleport). */
    public static BlockPos arrivalPad(ServerPlayer p) {
        CompoundTag data = p.getPersistentData();
        return data.contains(PAD) ? BlockPos.of(data.getLong(PAD)) : null;
    }

    /** Forgets the way back and the pad (on leaving, and on a death inside: the respawn is a normal one). */
    public static void forget(ServerPlayer p) {
        CompoundTag data = p.getPersistentData();
        data.remove(ORIGIN);
        data.remove(PAD);
        data.remove(RESCUED);
    }

    private static void rememberOrigin(ServerPlayer p) {
        CompoundTag o = new CompoundTag();
        o.putString("Dim", p.level().dimension().location().toString());
        o.putDouble("X", p.getX());
        o.putDouble("Y", p.getY());
        o.putDouble("Z", p.getZ());
        o.putFloat("Yaw", p.getYRot());
        o.putFloat("Pitch", p.getXRot());
        p.getPersistentData().put(ORIGIN, o);
    }

    private static void stamp(ServerPlayer p, ServerLevel level) {
        p.getPersistentData().putLong(GATE_USED, GateRules.cooldownUntil(level.getGameTime()));
    }
}
