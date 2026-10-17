package com.newuniverse.nusmp.labyrinth.mega.travel;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel;
import com.newuniverse.nusmp.labyrinth.mega.MegaConfig;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.gen.ExpansionQueue;
import com.newuniverse.nusmp.labyrinth.mega.gen.LandingRule;
import com.newuniverse.nusmp.labyrinth.mega.gen.SectorGenerator;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: this is where a player goes when a Gate, a Dungeon Maw or the enter command sends them into the Mega-Dungeon ({@code LabyrinthTravel.enter} routes
 * here while {@code mega.layout} is {@code "mega"}). The dungeon is ONE place that exists only after somebody asks for it, so the first player through a
 * Gate wakes it: sector 0 is queued and the player is kept waiting (an action-bar line every two seconds, no teleport into a half-built void) until the
 * generator has finished it, then taken in automatically; a player who walks away from the gate meanwhile is released, because being pulled across the
 * world minutes later would be a surprise. Once the dungeon exists the player arrives on a Landing: the deepest one they ever stood in (so the climb is
 * not lost between play sessions), or the first when they sneak through the gate or {@code lastLanding} is off; a sector that is still being built is not
 * a place yet, so the rule falls back to the deepest finished one ({@link LandingRule}). The depth is a plain int in the player's persistent data
 * ({@code nusmp_mega_landing}), which survives death, logout and restart, and is raised by a slow check while the player stands in a Landing (reaching a
 * Landing by any road counts, not only by the Gate). When {@code legacyGates} is off the old Gate arches and Maws refuse and point at the Gateway. A Gate
 * calls {@link #enter} every tick the player overlaps it, so a refusal is told at most once every three seconds and a waiting player is not queued twice.
 * Server thread only; the waiting list and the message throttle are cleared when the server stops.
 */
public final class MegaTravel {
    private static final Logger LOG = LogUtils.getLogger();

    /** The deepest Landing (sector id) the player ever stood in; a persistent int in {@code getPersistentData()}. */
    public static final String LANDING_KEY = "nusmp_mega_landing";

    private static final int CHECK_EVERY = 10;
    private static final int TELL_EVERY = 40;
    private static final long GIVE_UP_TICKS = 20L * 60L * 10L;
    private static final double WANDER = 64.0;
    private static final long MESSAGE_GAP = 60L;

    /** A player waiting for sector 0 to be built: who, in which dimension and where they stood when they came to the Gate, and since when (server tick). */
    private record Waiting(UUID id, ResourceKey<Level> dimension, BlockPos from, long since) {}

    private static final Map<UUID, Waiting> WAITING = new LinkedHashMap<>();
    private static final Map<UUID, Long> LAST_TOLD = new HashMap<>();

    private MegaTravel() {}

    // ================================================================================================================ entering

    /** Takes the player into the Mega-Dungeon from a gate at {@code from}. False when it cannot (the message says why). Never throws. */
    public static boolean enter(ServerPlayer p, BlockPos from) {
        try {
            return enterNow(p, from);
        } catch (Throwable t) {
            LOG.error("[nusmp] The Mega-Dungeon entry failed for {}: {}", p == null ? "?" : p.getGameProfile().getName(), t.toString(), t);
            if (p != null) tell(p, "The Labyrinth does not answer.");
            return false;
        }
    }

    private static boolean enterNow(ServerPlayer p, BlockPos from) {
        MinecraftServer server = p.getServer();
        if (server == null) return false;
        ServerLevel mega = MegaDungeon.level(server);
        if (mega == null) {
            tell(p, "The Labyrinth does not answer: the Mega-Dungeon's dimension is not loaded.");
            return false;
        }
        if (WAITING.containsKey(p.getUUID())) return true;                                    // a Gate calls this every tick the player stands in it
        if (!Labyrinth.in(p) && !MegaConfig.get(MegaConfig.LEGACY_GATES) && !atGateway(p.serverLevel(), from)) {
            tell(p, "This old gate no longer opens onto the Labyrinth. Look for the Gateway: it is the one way in.");
            return false;
        }
        MegaData data = MegaData.get(server);
        Sector target = choose(data, p, p.isShiftKeyDown());
        if (target != null) return go(p, mega, target, true);

        Sector first = data.sector(0);
        if (first != null && first.state == Sector.State.FAILED) {
            tell(p, "The Labyrinth failed to wake. An admin can try again with /multiverse mega regen 0.");
            return false;
        }
        if (data.sectors().isEmpty()) {
            String why = ExpansionQueue.get().tryFirst(server);
            if (why != null) {
                tell(p, "The Labyrinth cannot wake: " + why + ".");
                return false;
            }
        }
        WAITING.put(p.getUUID(), new Waiting(p.getUUID(), p.level().dimension(), p.blockPosition(), server.getTickCount()));
        p.displayClientMessage(Component.translatable("message.nusmp.mega.core.waking", percent()), true);
        p.sendSystemMessage(Component.literal("The Labyrinth stirs. Stay by the gate: it will take you in when it is ready.").withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    /** The sector whose Landing this player should arrive at, or null when no sector is finished yet. */
    public static Sector choose(MegaData data, ServerPlayer p, boolean sneaking) {
        List<Sector> all = data.sectors();
        int[] playable = new int[all.size()];
        int n = 0;
        for (Sector s : all) if (SectorGenerator.playable(s)) playable[n++] = s.id;
        int id = LandingRule.choose(MegaConfig.get(MegaConfig.LAST_LANDING), sneaking, p.getPersistentData().getInt(LANDING_KEY), Arrays.copyOf(playable, n));
        return id < 0 ? null : data.sector(id);
    }

    /** Sets the player down on the Landing pad of {@code target}. False (with a message) when the sector has no pad. */
    public static boolean go(ServerPlayer p, ServerLevel mega, Sector target, boolean fromGate) {
        BlockPos pad = SectorGenerator.padOf(target);
        if (pad == null) {
            tell(p, "Sector " + target.id + " has no Landing to stand on. Its data is incomplete: see the server log.");
            return false;
        }
        String message = target.id == 0
                ? "The gate closes behind you. Lamps burn in a hall of dark stone: a Landing. Walk on, and the Labyrinth will find you."
                : "You step out onto the Landing of sector " + target.id + ".";
        LabyrinthTravel.arrive(p, mega, pad, message);
        CompoundTag data = p.getPersistentData();
        if (target.id > data.getInt(LANDING_KEY)) data.putInt(LANDING_KEY, target.id);
        if (fromGate) {
            NusmpEvents.fire(p, "labyrinth.entered");
            NusmpEvents.fire(p, "mega.entered", Integer.toString(target.id));
        }
        return true;
    }

    /** Is the position inside the Overworld Gateway structure (the one entrance that is always open)? */
    static boolean atGateway(ServerLevel level, BlockPos pos) {
        try {
            StructureStart start = level.structureManager().getStructureWithPieceAt(pos, Labyrinth.MEGA_GATEWAY_TAG);
            return start != null && start.isValid();
        } catch (Throwable t) {
            return false;
        }
    }

    // ================================================================================================================ waiting for sector 0

    /** Called every server tick: releases the players whose sector 0 is ready, tells the others how far it is, drops the ones who left. */
    public static void tick(MinecraftServer server) {
        if (WAITING.isEmpty()) return;
        int now = server.getTickCount();
        if (now % CHECK_EVERY != 0) return;
        try {
            ServerLevel mega = MegaDungeon.level(server);
            MegaData data = MegaData.get(server);
            for (Iterator<Waiting> it = WAITING.values().iterator(); it.hasNext(); ) {
                Waiting w = it.next();
                ServerPlayer p = server.getPlayerList().getPlayer(w.id());
                if (p == null || !p.isAlive() || mega == null) {
                    it.remove();
                    continue;
                }
                if (p.level().dimension() != w.dimension() || p.blockPosition().distSqr(w.from()) > WANDER * WANDER) {
                    it.remove();
                    p.sendSystemMessage(Component.literal("You step away from the gate. The Labyrinth will be ready when you come back.").withStyle(ChatFormatting.GRAY));
                    continue;
                }
                Sector target = choose(data, p, false);
                if (target != null) {
                    it.remove();
                    go(p, mega, target, true);
                    continue;
                }
                Sector first = data.sector(0);
                if (first != null && first.state == Sector.State.FAILED) {
                    it.remove();
                    p.sendSystemMessage(Component.literal("The Labyrinth failed to wake. An admin can try again with /multiverse mega regen 0.").withStyle(ChatFormatting.RED));
                    continue;
                }
                if (now - w.since() > GIVE_UP_TICKS) {
                    it.remove();
                    p.sendSystemMessage(Component.literal("The Labyrinth is taking too long to wake. Try the gate again later.").withStyle(ChatFormatting.GRAY));
                    continue;
                }
                if (now % TELL_EVERY == 0) p.displayClientMessage(Component.translatable("message.nusmp.mega.core.waking", percent()), true);
            }
        } catch (Throwable t) {
            LOG.error("[nusmp] The Mega-Dungeon waiting list failed: {}", t.toString(), t);
            WAITING.clear();
        }
    }

    private static int percent() {
        float f = ExpansionQueue.get().progress();
        return f < 0f ? 0 : Math.round(f * 100f);
    }

    /** Is this player waiting for the dungeon to wake (for the self-test and the status command)? */
    public static boolean isWaiting(UUID id) { return WAITING.containsKey(id); }

    // ================================================================================================================ remembering the deepest Landing

    /** Called for every player tick; every two seconds, raises the remembered depth while the player stands in a Landing. Cheap for players elsewhere. */
    public static void rememberLanding(ServerPlayer p) {
        if (p.tickCount % 40 != 0 || !MegaDungeon.in(p.level())) return;
        try {
            BlockPos at = p.blockPosition();
            Sector s = MegaDungeon.sectorAt(p.level(), at);
            if (s == null) return;
            Room r = s.roomAt(at);
            if (r == null || r.kind != RoomKind.LANDING) return;
            CompoundTag data = p.getPersistentData();
            if (s.id > data.getInt(LANDING_KEY)) data.putInt(LANDING_KEY, s.id);
        } catch (Throwable t) {
            LOG.debug("[nusmp] Landing check failed: {}", t.toString());
        }
    }

    // ================================================================================================================ housekeeping

    public static void forget(UUID id) {
        WAITING.remove(id);
        LAST_TOLD.remove(id);
    }

    public static void onServerStopped() {
        WAITING.clear();
        LAST_TOLD.clear();
    }

    /** A grey chat line to the player, at most once every three seconds (a Gate calls {@link #enter} every tick the player overlaps it). */
    private static void tell(ServerPlayer p, String message) {
        long now = p.level().getGameTime();
        Long last = LAST_TOLD.get(p.getUUID());
        if (last != null && now >= last && now - last < MESSAGE_GAP) return;
        LAST_TOLD.put(p.getUUID(), now);
        p.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.GRAY));
    }
}
