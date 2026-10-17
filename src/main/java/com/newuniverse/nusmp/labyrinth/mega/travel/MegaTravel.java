package com.newuniverse.nusmp.labyrinth.mega.travel;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaConfig;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.gen.ExpansionQueue;
import com.newuniverse.nusmp.labyrinth.mega.gen.LandingRule;
import com.newuniverse.nusmp.labyrinth.mega.gen.SectorGenerator;
import com.newuniverse.nusmp.labyrinth.mega.party.PartyService;
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
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: this is where a player goes when a Gate or the enter command sends them into the Mega-Dungeon ({@code LabyrinthTravel.enter} routes
 * here). The dungeon is ONE place that exists only after somebody asks for it, so the first player through a
 * Gate wakes it: sector 0 is queued and the player is kept waiting (an action-bar line every two seconds, no teleport into a half-built void) until the
 * generator has finished it, then taken in automatically; a player who walks away from the gate meanwhile is released, because being pulled across the
 * world minutes later would be a surprise. Once the dungeon exists the player arrives on a Landing: the deepest one they ever stood in (so the climb is
 * not lost between play sessions), or the first when they sneak through the gate or {@code lastLanding} is off; a sector that is still being built is not
 * a place yet, so the rule falls back to the deepest finished one ({@link LandingRule}). The depth is a plain int in the player's persistent data
 * ({@code nusmp_mega_landing}), which survives death, logout and restart, and is raised by a slow check while the player stands in a Landing (reaching a
 * Landing by any road counts, not only by the Gate). A Gate
 * calls {@link #enter} every tick the player overlaps it, so a refusal is told at most once every three seconds and a waiting player is not queued twice.
 * Server thread only; the waiting list and the message throttle are cleared when the server stops.
 *
 * <p>Two roads (1.55.0): the Hub's gates take a player up the Holy road or down the Abyssal road through {@link #enterChain}, which is the {@code ChainTravel} the
 * generator registers with the facade ({@code MegaDungeon.enterChain}). It asks the Directional Lock first ({@code MegaDungeon.mayEnter}), wakes the road's first sector
 * when the road does not exist yet (and keeps the player waiting exactly as for the first sector of 1.54.0), and picks the deepest Landing the player ever reached ON THAT
 * ROAD, or the road's first one. The memory is per road: a persistent int {@code nusmp_mega_landing_holy} / {@code nusmp_mega_landing_abyssal} holding the deepest CHAIN
 * INDEX reached (sector ids are global and interleave the two roads, so an id says nothing about depth; the chain index does). The 1.54.0 key
 * {@code nusmp_mega_landing} (a sector id; the rest anchors write it too) is still written and still read, as a hint for the road its sector belongs to. The Gates ({@link #enter}) lead to the first road, the Holy one, and obey the lock like every other door.
 *
 * <p>Parties (1.58.0): the dungeon is instanced, so a player goes into THEIR party's chain of the road, not the road's one chain. The party is decided here, at the gate,
 * after the lock has agreed ({@link PartyService#enter}: their team's lane, handed out the first time), and everything below it is asked of that party's chain: the Landing to
 * offer (the deepest the player reached, never deeper than the party's newest finished floor, so a teammate who comes later joins the chain that exists), the first floor of a
 * road the party has not opened yet (queued on the party's own lane), and the wait for it. With one shared party (parties switched off) it is the same code on party 0, which
 * is the chain it always was. A party that cannot be placed (the book of lanes is full) is not let in: there is no other place to put it but among strangers.
 */
public final class MegaTravel {
    private static final Logger LOG = LogUtils.getLogger();

    /** The 1.54.0 memory: the deepest Landing (a sector id) the player ever stood in; a persistent int in {@code getPersistentData()}. The rest anchors write it too. */
    public static final String LANDING_KEY = "nusmp_mega_landing";
    /** 1.55.0: the deepest Landing the player reached on the Holy / the Abyssal road, as a chain index (persistent ints). */
    public static final String LANDING_HOLY = "nusmp_mega_landing_holy", LANDING_ABYSSAL = "nusmp_mega_landing_abyssal";

    private static final int CHECK_EVERY = 10;
    private static final int TELL_EVERY = 40;
    private static final long GIVE_UP_TICKS = 20L * 60L * 10L;
    private static final double WANDER = 64.0;
    private static final long MESSAGE_GAP = 60L;

    /** A player waiting for the first sector of a road to be built: who, in which dimension and where they stood when they came to the Gate, since when (server tick), for which road and for which party's chain. */
    private record Waiting(UUID id, ResourceKey<Level> dimension, BlockPos from, long since, Polarity road, int party) {}

    private static final Map<UUID, Waiting> WAITING = new LinkedHashMap<>();
    private static final Map<UUID, Long> LAST_TOLD = new HashMap<>();

    private MegaTravel() {}

    // ================================================================================================================ entering

    /**
     * Takes the player into the Mega-Dungeon from a Gate at {@code from}: up the first road, the Holy one (what 1.54.0's single chain was). False when it
     * cannot (the message says why). Never throws.
     */
    public static boolean enter(ServerPlayer p, BlockPos from) { return enterSafely(p, from, Polarity.HOLY); }

    /**
     * 1.55.0: the {@code ChainTravel} of the Hub's gates: takes the player into the chain of a polarity, waking its first sector when the road does not exist yet. The
     * Directional Lock is asked first and tells the player why when it refuses. False when it cannot (the message says why). Never throws.
     */
    public static boolean enterChain(ServerPlayer p, Polarity road) {
        if (p == null || road == null) return false;
        return enterSafely(p, p.blockPosition(), road);
    }

    private static boolean enterSafely(ServerPlayer p, BlockPos from, Polarity road) {
        try {
            return enterNow(p, from, road);
        } catch (Throwable t) {
            LOG.error("[nusmp] The Mega-Dungeon entry failed for {}: {}", p == null ? "?" : p.getGameProfile().getName(), t.toString(), t);
            if (p != null) tell(p, "The Labyrinth does not answer.");
            return false;
        }
    }

    private static boolean enterNow(ServerPlayer p, BlockPos from, Polarity road) {
        MinecraftServer server = p.getServer();
        if (server == null) return false;
        ServerLevel mega = MegaDungeon.level(server);
        if (mega == null) {
            tell(p, "The Labyrinth does not answer: the Mega-Dungeon's dimension is not loaded.");
            return false;
        }
        Waiting waiting = WAITING.get(p.getUUID());
        if (waiting != null && waiting.road() == road) return true;                          // a Gate calls this every tick the player stands in it
        if (!MegaDungeon.mayEnter(p, road)) return false;                                    // the Directional Lock says why
        int party = PartyService.enter(p);                                                   // the party is decided at the gate: the lane of their team (or of the player alone)
        if (party < 0) {
            tell(p, "The Labyrinth has no free lane for your party. An admin can raise party.maxParties.");
            return false;
        }
        MegaData data = MegaData.get(server);
        Sector target = choose(data, party, p, road, p.isShiftKeyDown());
        if (target != null) return go(p, mega, target, true);

        Sector first = data.chainSector(party, road, 0);
        if (first != null && first.state == Sector.State.FAILED) {
            tell(p, "The " + road.id() + " road failed to wake. An admin can try again with /multiverse mega regen " + first.id + ".");
            return false;
        }
        if (data.chain(party, road).isEmpty()) {
            String why = ExpansionQueue.get().tryFirst(server, road, party);
            if (why != null) {
                tell(p, "The Labyrinth cannot wake: " + why + ".");
                return false;
            }
        }
        WAITING.put(p.getUUID(), new Waiting(p.getUUID(), p.level().dimension(), p.blockPosition(), server.getTickCount(), road, party));
        p.displayClientMessage(Component.translatable("message.nusmp.mega.core.waking", percent()), true);
        p.sendSystemMessage(Component.literal("The Labyrinth stirs. Stay by the gate: it will take you in when it is ready.").withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    /** The sector of the Holy road whose Landing this player should arrive at, or null when none is finished yet (the 1.54.0 question, for the one road it had). The shared chain (party 0). */
    public static Sector choose(MegaData data, ServerPlayer p, boolean sneaking) { return choose(data, p, Polarity.HOLY, sneaking); }

    /** {@link #choose(MegaData, int, ServerPlayer, Polarity, boolean)} on the shared chain (party 0), the way it was asked before parties. */
    public static Sector choose(MegaData data, ServerPlayer p, Polarity road, boolean sneaking) { return choose(data, 0, p, road, sneaking); }

    /**
     * The sector of {@code road} in the chain of {@code party} whose Landing this player should arrive at, or null when none is finished yet: the deepest Landing the player
     * reached on that road (clamped down to the newest finished one OF THE PARTY'S CHAIN, so a player who comes to a party's chain late joins what it has built), the
     * chain's first when they sneak or {@code lastLanding} is off ({@link LandingRule}, applied to chain indices).
     */
    public static Sector choose(MegaData data, int party, ServerPlayer p, Polarity road, boolean sneaking) {
        List<Sector> chain = data.chain(party, road);
        int[] playable = new int[chain.size()];
        int n = 0;
        for (Sector s : chain) if (SectorGenerator.playable(s)) playable[n++] = s.chainIndex;
        int idx = LandingRule.choose(MegaConfig.get(MegaConfig.LAST_LANDING), sneaking, deepest(data, p, road), Arrays.copyOf(playable, n));
        return idx < 0 ? null : data.chainSector(party, road, idx);
    }

    /** The persistent key of the per-road memory. */
    public static String landingKey(Polarity road) { return road == Polarity.ABYSSAL ? LANDING_ABYSSAL : LANDING_HOLY; }

    /**
     * The deepest chain index the player ever reached on {@code road}: the per-road int, and the 1.54.0 key when the sector id it holds belongs to this road (that key
     * is a sector id written by the old travel and by the rest anchors; a sector of the other road says nothing about this one).
     */
    public static int deepest(MegaData data, ServerPlayer p, Polarity road) {
        CompoundTag t = p.getPersistentData();
        int best = Math.max(0, t.getInt(landingKey(road)));
        if (t.contains(LANDING_KEY)) {
            Sector s = data.sector(t.getInt(LANDING_KEY));
            if (s != null && s.polarity == road) best = Math.max(best, s.chainIndex);
        }
        return best;
    }

    /** Raises the player's memory of the deepest Landing: the per-road chain index and the 1.54.0 sector-id key (never lowers either). */
    static void remember(ServerPlayer p, Sector s) {
        CompoundTag t = p.getPersistentData();
        String key = landingKey(s.polarity);
        if (!t.contains(key) || s.chainIndex > t.getInt(key)) t.putInt(key, s.chainIndex);
        if (s.id > t.getInt(LANDING_KEY)) t.putInt(LANDING_KEY, s.id);
    }

    /** Sets the player down on the Landing pad of {@code target}. False (with a message) when the sector has no pad. */
    public static boolean go(ServerPlayer p, ServerLevel mega, Sector target, boolean fromGate) {
        BlockPos pad = SectorGenerator.padOf(target);
        if (pad == null) {
            tell(p, "Sector " + target.id + " has no Landing to stand on. Its data is incomplete: see the server log.");
            return false;
        }
        String floor = FloorMap.label(target.firstFloor());
        String way = target.polarity == Polarity.HOLY ? "up" : "down";
        String message = target.isFloor()
                ? (target.chainIndex == 0
                        ? "The gate closes behind you. You stand at the foot of the stairs of floor " + floor + ". Find the stairs that lead " + way + "."
                        : "You come out at the foot of the stairs of floor " + floor + ".")
                : target.chainIndex == 0
                ? "The gate closes behind you. Lamps burn in a hall of dark stone: a Landing, floor " + floor + ". Walk on, and the Labyrinth will find you."
                : "You step out onto the Landing of floor " + floor + ".";
        LabyrinthTravel.arrive(p, mega, pad, message);
        remember(p, target);
        if (fromGate) {
            NusmpEvents.fire(p, "labyrinth.entered");
            NusmpEvents.fire(p, "mega.entered", Integer.toString(target.id));
            announceParty(p, target);
        }
        return true;
    }

    /**
     * The gate's word about the party, after the arrival: which party the player walks with and how many of it are inside already (nothing when the dungeon is shared), and the
     * quest hook {@code mega.party} (argument: how many of the party are inside now, the player included) when there is somebody to delve with.
     */
    private static void announceParty(ServerPlayer p, Sector target) {
        try {
            Component note = PartyService.entryNote(p, target.party);
            if (note != null && p.connection != null) p.sendSystemMessage(note.copy().withStyle(ChatFormatting.GRAY));
            if (PartyService.instanced() && p.getServer() != null) {
                int inside = PartyService.onlineIn(p.getServer(), target.party).size();
                if (inside >= 2) NusmpEvents.fire(p, "mega.party", Integer.toString(inside));
            }
        } catch (RuntimeException e) {
            LOG.debug("[nusmp] The party line of the gate failed: {}", e.toString());
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
                Sector target = choose(data, w.party(), p, w.road(), false);
                if (target != null) {
                    it.remove();
                    if (MegaDungeon.mayEnter(p, w.road())) go(p, mega, target, true);          // the lock may have closed this road while the player waited
                    continue;
                }
                Sector first = data.chainSector(w.party(), w.road(), 0);
                if (first != null && first.state == Sector.State.FAILED) {
                    it.remove();
                    p.sendSystemMessage(Component.literal("The " + w.road().id() + " road failed to wake. An admin can try again with /multiverse mega regen " + first.id + ".").withStyle(ChatFormatting.RED));
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
            if (r == null || (r.kind != RoomKind.LANDING && r.kind != RoomKind.STAIRWELL)) return;
            remember(p, s);
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
