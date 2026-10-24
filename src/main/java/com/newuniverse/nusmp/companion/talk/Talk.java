package com.newuniverse.nusmp.companion.talk;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.avatar.net.RollLimiter;
import com.newuniverse.nusmp.companion.CompanionConfig;
import com.newuniverse.nusmp.companion.growth.Panelable;
import com.newuniverse.nusmp.companion.net.CompanionNet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the server's half of every conversation, in one place, so that a companion cannot get the checks wrong and a client cannot skip them. {@link #open} starts a talk (from {@code mobInteract}); the client
 * then only ever sends two asks, a PICK and a CLOSE, and every PICK is checked here before the entity hears of it: the sender has a conversation open with exactly that entity, the entity is alive, in the same
 * level and within {@link TalkRules#REACH} blocks, the player is not sending faster than every {@link TalkRules#PICK_GAP_TICKS} ticks, and the id was on the last screen the server sent (so a forged id never reaches
 * the entity). A companion that throws is logged and the conversation ends; a player is never kicked for a line of text. State is ONE record per player, keyed by UUID (a conversation is: which entity, which ids
 * were offered), dropped on logout, death, dimension change, when the entity dies or goes out of reach (swept once a second) and at server stop, so it cannot grow beyond the number of players.
 */
public final class Talk {
    private Talk() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** A talk is only possible this close (blocks). */
    public static final double REACH = TalkRules.REACH;

    private record Session(UUID entity, int entityId, Set<String> offered) {}

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final RollLimiter PICKS = new RollLimiter(), OPENS = new RollLimiter();

    // ================================================================ opening
    /** Starts a conversation between the player and the entity (call from {@code mobInteract}, server side). Returns whether the screen was opened. */
    public static boolean open(ServerPlayer player, Talkable talkable) {
        if (!CompanionConfig.enabled() || player == null || !(talkable instanceof Entity e) || !e.isAlive() || e.level() != player.level()) return false;
        if (!player.isAlive() || player.isSpectator() || !TalkRules.inReach(player.distanceToSqr(e))) return false;       // (a stand-in player with no connection can talk: CompanionNet.send skips it)
        long now = player.level().getGameTime();
        if (!OPENS.allow(player.getUUID(), now, TalkRules.OPEN_GAP_TICKS)) return false;
        Session before = SESSIONS.remove(player.getUUID());
        if (before != null && !before.entity().equals(e.getUUID())) told(player, before);
        TalkView view = safeOpen(talkable, player);
        if (view == null || view.isClosed()) return false;
        SESSIONS.put(player.getUUID(), new Session(e.getUUID(), e.getId(), view.ids()));
        CompanionNet.send(player, new CompanionNet.Show(e.getId(), view));
        return true;
    }

    // ================================================================ the asks from the client
    /** The player picked a choice. Every check is here; the entity only hears of a pick that passed them all. */
    public static void onPick(ServerPlayer player, int entityId, String id) {
        if (player == null) return;
        Session s = SESSIONS.get(player.getUUID());
        if (s == null || s.entityId() != entityId) return;                                  // no conversation with that entity: an old or forged packet
        Entity e = player.serverLevel().getEntity(entityId);
        if (!CompanionConfig.enabled() || e == null || !e.getUUID().equals(s.entity()) || !(e instanceof Talkable t) || !e.isAlive() || e.level() != player.level()
                || !player.isAlive() || !TalkRules.inReach(player.distanceToSqr(e))) {
            end(player, true);
            return;
        }
        if (!PICKS.allow(player.getUUID(), player.level().getGameTime(), TalkRules.PICK_GAP_TICKS)) return;     // too fast: ignored
        if (!TalkRules.acceptPick(id, s.offered())) return;                                      // not on the last screen, or not a well formed id: ignored
        if (Panelable.PANEL_ID.equals(id)) {
            if (t instanceof Panelable p && p.panelAvailable(player)) {
                try {
                    p.panelOpen(player);
                } catch (RuntimeException ex) {
                    LOG.warn("[nusmp] the panel of {} failed: {}", e.getType().getDescriptionId(), ex.toString());
                }
            }
            end(player, true);
            return;
        }
        TalkView next = safeChoose(t, player, id);
        if (next == null || next.isClosed()) {
            end(player, true);
            return;
        }
        SESSIONS.put(player.getUUID(), new Session(s.entity(), s.entityId(), next.ids()));
        CompanionNet.send(player, new CompanionNet.Show(entityId, next));
    }

    /** The player closed the screen. */
    public static void onClose(ServerPlayer player, int entityId) {
        if (player == null) return;
        Session s = SESSIONS.get(player.getUUID());
        if (s == null || s.entityId() != entityId) return;
        SESSIONS.remove(player.getUUID());
        told(player, s);
    }

    // ================================================================ ending
    /** Ends the player's conversation (if any), tells the entity, and (when {@code tellClient}) closes the screen. */
    public static void end(ServerPlayer player, boolean tellClient) {
        if (player == null) return;
        Session s = SESSIONS.remove(player.getUUID());
        if (s == null) return;
        told(player, s);
        if (tellClient) CompanionNet.send(player, new CompanionNet.End(s.entityId()));
    }

    private static void told(ServerPlayer player, Session s) {
        try {
            if (player.serverLevel().getEntity(s.entity()) instanceof Talkable t) t.talkClosed(player);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] talkClosed failed: {}", ex.toString());
        }
    }

    /** A player logged out, died or changed dimension: nothing of theirs is kept. */
    public static void forget(ServerPlayer player) {
        if (player == null) return;
        end(player, false);
        PICKS.forget(player.getUUID());
        OPENS.forget(player.getUUID());
    }

    /** Once a second: closes the conversations whose player is gone or whose entity died, left the level or is out of reach. Bounded by the number of players. */
    public static void sweep(MinecraftServer server) {
        if (SESSIONS.isEmpty()) return;
        List<UUID> done = new ArrayList<>();
        for (Map.Entry<UUID, Session> en : SESSIONS.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            if (p == null) {
                done.add(en.getKey());
                continue;
            }
            Entity e = p.serverLevel().getEntity(en.getValue().entity());
            if (e == null || !e.isAlive() || !p.isAlive() || p.distanceToSqr(e) > (TalkRules.REACH + 2.0) * (TalkRules.REACH + 2.0)) done.add(en.getKey());
        }
        for (UUID id : done) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) end(p, true);
            else SESSIONS.remove(id);
        }
    }

    /** Forgets the rate limits (self-test only: the tests run inside one tick). */
    public static void clearLimits() {
        PICKS.clear();
        OPENS.clear();
    }

    /** Server stopped: drop everything. */
    public static void clear() {
        SESSIONS.clear();
        PICKS.clear();
        OPENS.clear();
    }

    /** How many conversations are open (self-test). */
    public static int openCount() { return SESSIONS.size(); }

    /** Is a conversation open for this player (self-test)? */
    public static boolean isOpen(UUID player) { return SESSIONS.containsKey(player); }

    /** The ids the player's current screen offers, or an empty set (self-test). */
    public static Set<String> offered(UUID player) {
        Session s = SESSIONS.get(player);
        return s == null ? Set.of() : s.offered();
    }

    // ================================================================ calling the entity safely
    private static TalkView safeOpen(Talkable t, ServerPlayer p) {
        try {
            return t.talkOpen(p);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] a companion could not open a talk: {}", ex.toString());
            return null;
        }
    }

    private static TalkView safeChoose(Talkable t, ServerPlayer p, String id) {
        try {
            return t.talkChoose(p, id);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] a companion could not answer the pick {}: {}", id, ex.toString());
            return null;
        }
    }
}
