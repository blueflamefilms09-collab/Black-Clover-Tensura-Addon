package com.newuniverse.nusmp.labyrinth.mega.party;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the doors send every party to its own floors, but not everything goes through a door: an operator's teleport, a relic, a command block or a bug in some future door
 * would put a player on the floors of ANOTHER party, and then the instancing would be a suggestion (a sector with players of one party only is the rule). This is the backstop,
 * the way {@code HardLock} is for the Directional Lock: once a second every player in the Mega-Dungeon is looked at, and a player whose recorded party is not the party of the
 * floor under their feet is told why and sent out with the existing {@link LabyrinthTravel#leaveAny} (which takes someone home from any Labyrinth dimension: the Hub's plaza).
 * A player with no record adopts the party of the floor they stand in, so a world saved before parties, a login after an update and a teleport by command never throw anyone
 * out of a floor they were already in. Operators and spectators are never moved ({@code party.opsBypass}); a stand-in player of a self-test has no connection and is only
 * judged, never moved. The decision is the pure {@link PartyPolicy#verdict}; the expulsion is rate-limited per player (a teleport that fails must not repeat its message every
 * second) and any exception is logged once: the guard must never take the server tick down.
 */
public final class PartyGuard {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int EVERY = 20;
    /** Ticks before the same player is sent out again: long enough that a teleport that fails does not repeat its message every look. */
    private static final long COOLDOWN = 40L;
    private static final Map<UUID, Long> LAST = new HashMap<>();
    private static boolean warned;

    private PartyGuard() {}

    public static void register(IEventBus bus) {
        bus.addListener(PartyGuard::onServerTick);
        bus.addListener(PartyGuard::onLogout);
        bus.addListener(PartyGuard::onServerStopped);
    }

    /** What the guard would do with this player right now. No side effects. */
    public static PartyPolicy.Verdict verdict(ServerPlayer p) {
        if (p == null || !MegaDungeon.in(p.level())) return PartyPolicy.Verdict.OK;
        boolean exempt = p.isSpectator() || (PartyConfig.get(PartyConfig.OPS_BYPASS) && p.hasPermissions(2));
        Sector s = MegaDungeon.sectorAt(p.level(), p.blockPosition());
        return PartyPolicy.verdict(PartyService.instanced(), PartyConfig.get(PartyConfig.GUARD), exempt, PartyService.assigned(p), s == null ? null : s.party);
    }

    /** Looks at one player and acts on the verdict. True when the player was sent out. */
    public static boolean check(ServerPlayer p) {
        try {
            PartyPolicy.Verdict v = verdict(p);
            if (v == PartyPolicy.Verdict.OK) return false;
            Sector s = MegaDungeon.sectorAt(p.level(), p.blockPosition());
            if (s == null) return false;
            if (v == PartyPolicy.Verdict.ADOPT) {
                PartyService.store(p, s.party);
                return false;
            }
            return evict(p);
        } catch (RuntimeException e) {
            if (!warned) {
                warned = true;
                LOG.warn("[nusmp] The party guard failed for {} (further failures are not logged): {}", p == null ? "?" : p.getGameProfile().getName(), e.toString());
            }
            return false;
        }
    }

    /** Sends a player out of the dungeon, with the reason, at most once every {@link #COOLDOWN} ticks. A stand-in player is not moved. */
    private static boolean evict(ServerPlayer p) {
        long now = p.level().getGameTime();
        Long last = LAST.get(p.getUUID());
        if (last != null && now >= last && now - last < COOLDOWN) return false;
        LAST.put(p.getUUID(), now);
        if (!PartyText.real(p)) return true;                                    // a stand-in player is judged, not moved
        p.sendSystemMessage(PartyText.text("evicted", "These floors belong to another party. The dungeon sends you back to the Hub; go in again by your own gate.").withStyle(ChatFormatting.DARK_PURPLE));
        LabyrinthTravel.leaveAny(p);
        return true;
    }

    private static void onServerTick(ServerTickEvent.Post e) {
        try {
            MinecraftServer server = e.getServer();
            if (server.getTickCount() % EVERY != 7 || !PartyService.instanced() || !PartyConfig.get(PartyConfig.GUARD)) return;
            ServerLevel mega = MegaDungeon.level(server);
            if (mega == null || mega.players().isEmpty()) return;
            for (ServerPlayer p : new ArrayList<>(mega.players())) check(p);          // a copy: the guard takes a player out of the dimension while we walk the list
        } catch (RuntimeException ex) {
            if (!warned) {
                warned = true;
                LOG.warn("[nusmp] The party guard failed (further failures are not logged): {}", ex.toString());
            }
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { LAST.remove(e.getEntity().getUUID()); }

    private static void onServerStopped(ServerStoppedEvent e) {
        LAST.clear();
        warned = false;
        PartyService.onServerStopped();
    }
}
