package com.newuniverse.nusmp.labyrinth.mega.hub;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.quests.NusmpEvents;
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
import net.minecraft.world.level.Level;

/**
 * Why: Floor 0 has three doors and one arrival, and this is the one class that knows how a player moves through them, so the lead's routing in
 * {@code LabyrinthTravel.enter} is three lines and the Gate block, the legacy Maw and the commands all behave the same. IN: {@link #enterFromOverworld}
 * (the Gateway) remembers where the player came from in their persistent data under {@link HubKeys#ORIGIN}, makes sure the plaza exists, and puts them
 * on the arrival cell facing south, with a short gate cooldown so the gate they were set down near does not send them straight back. ACROSS: {@link
 * #useGate} reads WHICH gate the block is from its position ({@link HubLayout#gateNear}: the three gates are plain {@code nusmp:labyrinth_gate} blocks,
 * so no block property tells them apart) and sends the player home, or up the Holy road, or down the Abyssal road, asking the Directional Lock first
 * ({@link MegaDungeon#mayEnter}, which tells the player why it refuses). OUT: {@link #leaveToOverworld} returns to the remembered origin, or to the
 * respawn point or the Overworld spawn when it is lost or is itself a Labyrinth or Sanctuary dimension; {@link #returnToPlaza} is how the Mega-Dungeon
 * brings someone back to Floor 0. The teleport is {@code ServerPlayer.teleportTo(level, x, y, z, yaw, pitch)}, the call {@code LabyrinthTravel} has used
 * since 1.35.0 (it moves a player across dimensions and keeps the inventory), and the cooldown keys are the ones the Labyrinth gate block reads, so a
 * refused or completed passage cannot repeat on the next tick (a gate block calls its handler every tick the player overlaps it, and a refusal that
 * speaks would otherwise speak twenty times a second). Server thread only.
 */
public final class HubTravel {
    private HubTravel() {}

    /** Is Floor 0 switched on ({@code hub.enabled})? False = the 1.54.0 behaviour: the lead's routing keeps the old road. */
    public static boolean enabled() { return HubConfig.get(HubConfig.ENABLED); }

    /** The Sanctuary's level, or null when the server does not have the dimension. */
    public static ServerLevel level(MinecraftServer server) { return server == null ? null : server.getLevel(Labyrinth.SANCTUARY_DIMENSION); }

    /** Does the server have the Sanctuary dimension (so the routing may send people there)? */
    public static boolean available(MinecraftServer server) { return level(server) != null; }

    /** The arrival cell: where a player stands when they enter Floor 0. */
    public static BlockPos arrivalPos() { return new BlockPos(HubLayout.SPAWN_X, HubLayout.SPAWN_Y, HubLayout.SPAWN_Z); }

    /** Makes sure the plaza and the way home exist (see {@link HubPlaza#ensure}); lazy, so a world nobody enters never builds it. */
    public static boolean ensurePlaza(ServerLevel level) { return HubPlaza.ensure(level); }

    // ---------------------------------------------------------------- in
    /**
     * The Overworld Gateway (or any gate outside the Labyrinth and the Sanctuary) leads here. False when the server has no Sanctuary (the message says so).
     * The player keeps their old origin if they already are in the Sanctuary or a Labyrinth dimension, so a second trip never overwrites the way home.
     */
    public static boolean enterFromOverworld(ServerPlayer p, BlockPos from) {
        ServerLevel hub = level(p.getServer());
        if (hub == null) {
            tell(p, "travel.no_level", "The Sanctuary does not answer: its dimension is not loaded.", ChatFormatting.GRAY);
            return false;
        }
        if (!Labyrinth.inSanctuary(p.level()) && !Labyrinth.in(p)) rememberOrigin(p);
        arrive(p, hub, "travel.arrive",
                "Floor 0, the Sanctuary. Nothing here wishes you harm. The Spire to the east climbs, the Chasm to the west descends; the gate behind you leads home.");
        return true;
    }

    /** Takes the player to the plaza's arrival cell (the Mega-Dungeon calls this when somebody leaves it for Floor 0). The way back to the Overworld is not touched. */
    public static void returnToPlaza(ServerPlayer p) {
        ServerLevel hub = level(p.getServer());
        if (hub == null) {
            tell(p, "travel.no_level", "The Sanctuary does not answer: its dimension is not loaded.", ChatFormatting.GRAY);
            return;
        }
        arrive(p, hub, "travel.plaza", "You are back on the Sanctuary's square.");
    }

    private static void arrive(ServerPlayer p, ServerLevel hub, String key, String english) {
        ensurePlaza(hub);
        BlockPos spawn = arrivalPos();
        hub.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);              // generate the arrival chunk first, so nobody arrives in an ungenerated void
        stamp(p, hub);
        p.teleportTo(hub, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0.0f, 0.0f);          // yaw 0 faces south, away from the gate and towards the notice board
        hub.playSound(null, spawn, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5f, 0.6f);
        tell(p, key, english, ChatFormatting.DARK_PURPLE);
        NusmpEvents.fire(p, "mega.hub");
    }

    // ---------------------------------------------------------------- across
    /**
     * A player stepped into the gate block at {@code gate} inside the Sanctuary. Home gate: out to the Overworld. Holy or Abyssal gate: the Directional Lock
     * decides, then the player enters that road's chain. Any other position: a short refusal. The gate cooldown is stamped FIRST, on every outcome, so a
     * refusal is spoken once per cooldown and not once per tick. True when the player left the Sanctuary.
     */
    public static boolean useGate(ServerPlayer p, BlockPos gate) {
        HubLayout.GateKind kind = HubLayout.gateNear(gate.getX(), gate.getY(), gate.getZ());
        stamp(p, p.serverLevel());
        if (kind == null) {
            tell(p, "gate.unknown", "This gate answers to nothing here.", ChatFormatting.GRAY);
            return false;
        }
        if (kind == HubLayout.GateKind.HOME) {
            leaveToOverworld(p);
            return true;
        }
        Polarity road = HubLayout.polarityOf(kind);
        if (!MegaDungeon.mayEnter(p, road)) return false;                // the lock says why
        return MegaDungeon.enterChain(p, road);
    }

    // ---------------------------------------------------------------- out
    /**
     * Back to where the player entered Floor 0, or, when that is lost (a player who was sent here by a command, a dimension that no longer exists, or an
     * origin that is itself a Labyrinth or Sanctuary dimension), to their respawn point or the Overworld's spawn. Forgets the origin, stamps the cooldown on
     * arrival so the gate they are set down at does not take them back, and tells them.
     */
    public static void leaveToOverworld(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        CompoundTag o = p.getPersistentData().getCompound(HubKeys.ORIGIN);
        ServerLevel to = o.isEmpty() ? null : levelOf(server, o.getString(HubKeys.DIM));
        if (to == null || Labyrinth.in(to) || Labyrinth.inSanctuary(to)) {
            ServerLevel home = server.getLevel(p.getRespawnDimension());
            if (home == null || Labyrinth.in(home) || Labyrinth.inSanctuary(home)) home = server.overworld();
            BlockPos s = home.dimension() == p.getRespawnDimension() && p.getRespawnPosition() != null ? p.getRespawnPosition() : home.getSharedSpawnPos();
            to = home;
            p.teleportTo(home, s.getX() + 0.5, s.getY(), s.getZ() + 0.5, p.getYRot(), p.getXRot());
        } else {
            p.teleportTo(to, o.getDouble(HubKeys.X), o.getDouble(HubKeys.Y), o.getDouble(HubKeys.Z), o.getFloat(HubKeys.YAW), o.getFloat(HubKeys.PITCH));
        }
        forget(p);
        stamp(p, to);
        to.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5f, 0.8f);
        tell(p, "travel.leave", "The Sanctuary lets you go. The gate stands quiet behind you.", ChatFormatting.GRAY);
    }

    // ---------------------------------------------------------------- marks in the player's persistent data
    /** Forgets the way back to the Overworld. */
    public static void forget(ServerPlayer p) { p.getPersistentData().remove(HubKeys.ORIGIN); }

    /** Does this player have a remembered way back? */
    public static boolean hasOrigin(ServerPlayer p) { return p.getPersistentData().contains(HubKeys.ORIGIN); }

    private static void rememberOrigin(ServerPlayer p) {
        CompoundTag o = new CompoundTag();
        o.putString(HubKeys.DIM, p.level().dimension().location().toString());
        o.putDouble(HubKeys.X, p.getX());
        o.putDouble(HubKeys.Y, p.getY());
        o.putDouble(HubKeys.Z, p.getZ());
        o.putFloat(HubKeys.YAW, p.getYRot());
        o.putFloat(HubKeys.PITCH, p.getXRot());
        p.getPersistentData().put(HubKeys.ORIGIN, o);
    }

    private static ServerLevel levelOf(MinecraftServer server, String dimensionId) {
        ResourceLocation id = ResourceLocation.tryParse(dimensionId);
        return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }

    /** Both gate keys until {@code now + hub.gateCooldownTicks} (never shortening a longer stamp), so a gate does not take the player again while they walk out of it. */
    static void stamp(ServerPlayer p, Level level) {
        long until = level.getGameTime() + HubConfig.get(HubConfig.GATE_COOLDOWN);
        CompoundTag data = p.getPersistentData();
        data.putLong(HubKeys.GATE_USED, Math.max(until, data.getLong(HubKeys.GATE_USED)));
        data.putLong(HubKeys.LEGACY_GATE_USED, Math.max(until, data.getLong(HubKeys.LEGACY_GATE_USED)));
    }

    private static void tell(ServerPlayer p, String key, String english, ChatFormatting colour) {
        p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.mega.mhb." + key, english).withStyle(colour));
    }
}
