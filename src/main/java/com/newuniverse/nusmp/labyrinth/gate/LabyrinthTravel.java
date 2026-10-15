package com.newuniverse.nusmp.labyrinth.gate;

import com.newuniverse.nusmp.dungeon.LivingDungeon;
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
 * Why: the one place that moves players between the Overworld and the Labyrinth, so the Gate block, the legacy Dungeon Maw, the enter command
 * and the void rescue all take the same road and leave the same marks. The {@code labyrinth.engine} switch is honoured HERE and nowhere else:
 * {@link #enter} hands a "legacy" server straight to {@link LivingDungeon#enter}, which is how the old cave dungeon stays reachable without a
 * line of it being deleted. The way back is remembered in the player's persistent data ({@code nusmp_labyrinth_origin}: dimension id, x, y, z,
 * yaw, pitch; {@code nusmp_labyrinth_pad}: the arrival pad the void rescue returns to), the same NBT pattern {@link LivingDungeon} has used
 * since 1.17, because persistent data survives a relog and a server restart while a static map would not. The teleport itself is
 * {@code ServerPlayer.teleportTo(ServerLevel, x, y, z, yaw, pitch)}: the proven call of the legacy path, which moves the player across
 * dimensions, sends the level change to the client and keeps the inventory; the 1.21 alternative, {@code changeDimension(DimensionTransition)},
 * is built for portals that compute their own exit (the nether portal's search, the end's platform) and would only add a record round the
 * same numbers here. The gate cooldown stamps BOTH {@code nusmp_labyrinth_gate} and the legacy key {@code nusmp_dungeon_gate}, because a player
 * set down inside a legacy Maw (the way back from a Maw-entered Labyrinth) is checked by {@code DungeonBlocks.PortalBlock} against the legacy
 * key, and one set down inside a Labyrinth Gate after a legacy trip is checked against ours.
 */
public final class LabyrinthTravel {
    private LabyrinthTravel() {}

    static final String ORIGIN = "nusmp_labyrinth_origin", PAD = "nusmp_labyrinth_pad", GATE_USED = "nusmp_labyrinth_gate", RESCUED = "nusmp_labyrinth_rescued";
    /** The key {@code LivingDungeon.GATE_USED} reads (private there); written too so a legacy Maw or Root Gate does not send a returning player straight back. */
    static final String LEGACY_GATE_USED = "nusmp_dungeon_gate";

    // ---------------------------------------------------------------- in
    /**
     * Takes a player through a gate at {@code from}: into the Labyrinth's Spire nearest that column, or, when the engine is "legacy", down the
     * old road into the Living Dungeon. False if there is nowhere to go (the message says why).
     */
    public static boolean enter(ServerPlayer p, BlockPos from) {
        if (!Labyrinth.engineIsLabyrinth()) return LivingDungeon.enter(p, from);
        MinecraftServer server = p.getServer();
        ServerLevel labyrinth = server == null ? null : server.getLevel(Labyrinth.DIMENSION);
        if (labyrinth == null) {
            p.sendSystemMessage(Component.literal("The Labyrinth does not answer: its dimension is not loaded.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        BlockPos pad = com.newuniverse.nusmp.labyrinth.gen.LabyrinthLocate.arrivalPad(labyrinth, new BlockPos(from.getX(), 40, from.getZ()));
        if (pad == null) {
            p.sendSystemMessage(Component.literal("The Labyrinth does not answer here.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        labyrinth.getChunk(pad.getX() >> 4, pad.getZ() >> 4);                  // generate the pad's chunk first, so nobody arrives in an ungenerated void and drops out of the world
        if (!Labyrinth.in(p)) rememberOrigin(p);
        p.getPersistentData().putLong(PAD, pad.asLong());
        stamp(p, labyrinth);
        p.teleportTo(labyrinth, pad.getX() + 0.5, pad.getY(), pad.getZ() + 0.5, p.getYRot(), 0f);
        labyrinth.playSound(null, pad, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5f, 0.6f);
        p.sendSystemMessage(Component.literal("The gate closes behind you. Above the sea of clouds the Spire waits; the golden gate beside you is the way back.").withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    /** The legacy Dungeon Maw's entry when the engine is the Labyrinth: the same road as a gate, from the Maw's position. */
    public static boolean enterFromMaw(ServerPlayer p, BlockPos pos) { return enter(p, pos); }

    // ---------------------------------------------------------------- out
    /** Takes a player of the Labyrinth back to where they came in from, or to their bed / the world spawn if that is lost. */
    public static void leave(ServerPlayer p) {
        if (!Labyrinth.in(p)) return;
        MinecraftServer server = p.getServer();
        if (server == null) return;
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

    /** Leaves whichever dungeon the player is in: the Labyrinth by {@link #leave}, the legacy caves by {@link LivingDungeon#leave}; nothing elsewhere. */
    public static void leaveAny(ServerPlayer p) {
        if (Labyrinth.in(p)) leave(p);
        else if (LivingDungeon.in(p)) LivingDungeon.leave(p);
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
    /** Has this player just used a gate, Maw or Root Gate (so the block they were set down in does not send them straight back)? */
    public static boolean justTravelled(ServerPlayer p) {
        CompoundTag data = p.getPersistentData();
        return GateRules.cooldownHolds(p.level().getGameTime(), data.getLong(GATE_USED), data.getLong(LEGACY_GATE_USED));
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
        long until = GateRules.cooldownUntil(level.getGameTime());
        p.getPersistentData().putLong(GATE_USED, until);
        p.getPersistentData().putLong(LEGACY_GATE_USED, until);
    }
}
