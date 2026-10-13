package com.newuniverse.nusmp.relics.order;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.relics.order.OrderPlan.Beat;
import com.newuniverse.nusmp.relics.order.OrderPlan.Phase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runs the visits of the God of Order. Scans report duplicates ({@link #report}); the director keeps them on a waiting list ({@link CaseBook}: one
 * place per relic, a five minute cooldown per relic), and takes them one case at a time, so at most one God of Order lives.
 * <p>One case is an {@link OrderPlan}: a six second telegraph at a standing spot eight blocks from the holder of the duplicate, the God appearing, its
 * reading, its beam, the taking of the duplicates (every duplicate at that place, never the valid copy: they are judged again at that moment), the
 * Notice of Order, and the bow; or, if the holder struck the God, the fight until its Erasure. A visit is called off, without taking anything, when the
 * holder leaves the world or the dimension. Everything runs on the server thread; the director holds no level or entity beyond a case.
 */
public final class OrderDirector {
    private OrderDirector() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    private static final CaseBook<Sighting> BOOK = new CaseBook<>(OrderTable.QUEUE_CAPACITY);
    private static OrderCase active;

    /** One visit. */
    static final class OrderCase {
        final UUID id = UUID.randomUUID();
        final Sighting sighting;
        final ServerLevel level;
        final UUID bearer;
        final boolean creative;
        final OrderPlan plan;
        Vec3 godSpot, target;
        UUID god;
        UUID provoker;
        boolean combatStarted, godSeen;
        int godMissing;
        List<String> taken = new ArrayList<>();

        OrderCase(Sighting sighting, ServerLevel level, ServerPlayer bearer, boolean creative) {
            this.sighting = sighting;
            this.level = level;
            this.bearer = bearer == null ? null : bearer.getUUID();
            this.creative = creative;
            this.plan = new OrderPlan(creative);
        }

        GodOfOrder god() {
            return god != null && level.getEntity(god) instanceof GodOfOrder g && g.isAlive() ? g : null;
        }
    }

    // ================================================================ reports
    /** A scan met a duplicate. It is put on the waiting list unless its relic is cooling down. */
    static void report(MinecraftServer server, Sighting s) {
        if (s == null || s.relicId() == null || s.relicId().isEmpty()) return;
        UniqueLedger ledger = UniqueLedger.get(server);
        long now = server.overworld().getGameTime();
        if (active != null && active.sighting.relicId().equals(s.relicId())) return;
        BOOK.offer(s.relicId(), s, LedgerRules.visitAllowed(ledger.lastVisit(s.relicId()), now));
    }

    // ================================================================ the tick
    /** Every server tick (post). */
    public static void onServerTick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (active != null) {
            try {
                tickCase(server, active, now);
            } catch (RuntimeException ex) {
                LOG.warn("[nusmp] The Order's visit failed and was ended: {}", ex.toString());
                finish(server, active);
            }
        } else if (BOOK.size() > 0 && now % 10 == 0) {
            UniqueLedger ledger = UniqueLedger.get(server);
            Sighting s = BOOK.poll(id -> LedgerRules.visitAllowed(ledger.lastVisit(id), now));
            if (s != null) start(server, s, false);
        }
        if (now % 20 == 0) housekeeping(server, now);
    }

    private static void housekeeping(MinecraftServer server, long now) {
        UniqueLedger ledger = UniqueLedger.get(server);
        if (server.getPlayerCount() > 0 && ledger.startCensusIfPending(now)) {
            RelicScanner.resetCensus();
            OrderChat.toOps(server, OrderChat.plain(OrderText.censusStart(LedgerRules.CENSUS_TICKS / 1200)));
        }
        if (ledger.censusJustEnded(now)) {
            OrderChat.toOps(server, OrderChat.plain(OrderText.censusEnd(ledger.adoptedInCensus(), RelicScanner.censusGroups())));
        }
        if (now % 1200 == 0) {
            for (String id : ledger.sweep(server, now)) {
                LOG.info("[nusmp] The Order declares {} lost (not seen)", OrderText.titleCase(id));
                BOOK.forget(id);
            }
        }
    }

    // ================================================================ starting a case
    /**
     * Begins a visit. 'show' = an admin asked for it: the duplicate is not required to exist (the God comes and finds nothing, if so). False if the
     * Order is busy, the place is gone, or the duplicate no longer exists.
     */
    static boolean start(MinecraftServer server, Sighting s, boolean show) {
        if (active != null) return false;
        ServerLevel level = RelicScanner.levelOf(server, s.spot().dim());
        if (level == null) return false;
        long now = server.overworld().getGameTime();
        if (!show && UniqueLedger.get(server).mode(now) != LedgerRules.Mode.ENFORCE) return false;        // the census only counts
        if (!show && OrderStrip.count(server, s, now) <= 0) return false;                 // it put itself right in the meantime
        ServerPlayer bearer = bearerOf(server, s.spot());
        boolean creative = bearer != null && (bearer.isCreative() || bearer.isSpectator());
        OrderCase c = new OrderCase(s, level, bearer, creative);
        c.target = targetOf(level, s, bearer);
        c.godSpot = OrderSpots.find(level, c.target, level.random.nextDouble() * Math.PI * 2.0);
        UniqueLedger ledger = UniqueLedger.get(server);
        if (!s.relicId().isEmpty()) ledger.recordVisit(s.relicId(), now);
        if (c.godSpot == null) {                                                          // nowhere to stand: the duplicate is taken without the show
            OrderStrip.Result r = OrderStrip.strip(server, s, now);
            deliver(server, c, r.distinct(), bearer);
            return true;
        }
        active = c;
        announce(server, c, bearer);
        OrderFx.telegraphStart(level, c.godSpot);
        LOG.info("[nusmp] The Order comes for a second {} {}", OrderText.titleCase(s.relicId()), s.spot().describe());
        return true;
    }

    private static ServerPlayer bearerOf(MinecraftServer server, Spot spot) {
        if ((spot.kind() == Spot.Kind.PLAYER || spot.kind() == Spot.Kind.CONTAINER) && spot.holder() != null)
            return server.getPlayerList().getPlayer(spot.holder());
        return null;
    }

    private static Vec3 targetOf(ServerLevel level, Sighting s, ServerPlayer bearer) {
        if (s.spot().kind() == Spot.Kind.PLAYER && bearer != null) return bearer.position();
        if ((s.spot().kind() == Spot.Kind.GROUND || s.spot().kind() == Spot.Kind.MOB) && s.spot().holder() != null) {
            Entity e = level.getEntity(s.spot().holder());
            if (e != null) return e.position();
        }
        return Vec3.atBottomCenterOf(s.spot().blockPos());
    }

    private static void announce(MinecraftServer server, OrderCase c, ServerPlayer bearer) {
        Component line = OrderChat.announce(c.sighting.relicId());
        if (bearer != null) bearer.sendSystemMessage(line);
        for (ServerPlayer op : OrderChat.ops(server)) if (op != bearer) op.sendSystemMessage(line);
    }

    // ================================================================ one tick of a case
    private static void tickCase(MinecraftServer server, OrderCase c, long now) {
        ServerPlayer bearer = c.bearer == null ? null : server.getPlayerList().getPlayer(c.bearer);
        boolean holderBound = c.sighting.spot().kind() == Spot.Kind.PLAYER;
        if (holderBound && !c.plan.stripped() && (bearer == null || bearer.level() != c.level || !bearer.isAlive())) {
            c.plan.abort();                                                                // the holder is gone: nothing is taken, the God does not come
            finish(server, c);
            return;
        }
        GodOfOrder g = c.god();
        if (c.godSeen && g == null) {
            if (++c.godMissing > 40) {                                                     // killed, or unloaded: the visit is over
                c.plan.lost();
                finish(server, c);
                return;
            }
        } else {
            c.godMissing = 0;
        }
        if (g != null && g.provokedBy() != null && c.provoker == null) {
            c.provoker = g.provokedBy();
            c.plan.provoke();
        }
        if (c.plan.phase() == Phase.TELEGRAPH) followHolder(c, bearer);
        Beat beat = c.plan.tick();
        Phase phase = c.plan.phase();
        if (g != null) g.setOrderPhase(phase);
        switch (beat) {
            case APPEAR -> spawn(server, c);
            case BEAM_BEGIN -> { if (g != null) g.say(OrderText.beam(c.level.random.nextInt(8))); }
            case STRIP -> strip(server, c, g, bearer, now);
            case LEAVE -> { finish(server, c); return; }
            default -> { }
        }
        g = c.god();
        if (g != null && g.orderPhase() != phase) g.setOrderPhase(phase);
        int in = c.plan.ticksInPhase();
        switch (phase) {
            case TELEGRAPH -> { if (in % 4 == 0) OrderFx.telegraph(c.level, c.godSpot, c.plan.progress()); }
            case READING -> {
                if (g != null) {
                    if (in == 2) g.say(OrderText.reading(c.level.random.nextInt(8)));
                    if (in % 3 == 0) OrderFx.reading(c.level, g.position(), in);
                    g.getLookControl().setLookAt(c.target.x, c.target.y + 1.0, c.target.z);
                }
            }
            case BEAM -> {
                if (g != null) {
                    g.getLookControl().setLookAt(c.target.x, c.target.y + 1.0, c.target.z);
                    if (in % 2 == 0) OrderFx.beam(c.level, g.staffTip(), c.target.add(0, 1.0, 0), c.plan.progress());
                }
            }
            case COMBAT -> {
                if (!c.combatStarted && g != null) {
                    c.combatStarted = true;
                    ServerPlayer p = c.provoker == null ? null : server.getPlayerList().getPlayer(c.provoker);
                    g.beginCombat(p);
                }
            }
            default -> { }
        }
    }

    /** During the telegraph the spot follows a holder who walks away, so the God always comes eight blocks from where they stand. */
    private static void followHolder(OrderCase c, ServerPlayer bearer) {
        if (bearer == null || c.sighting.spot().kind() != Spot.Kind.PLAYER) return;
        c.target = bearer.position();
        if (c.plan.ticksInPhase() % 10 != 0) return;
        double d = Math.hypot(c.godSpot.x - c.target.x, c.godSpot.z - c.target.z);
        if (d < 5.0 || d > 11.0) {
            Vec3 next = OrderSpots.find(c.level, c.target, c.level.random.nextDouble() * Math.PI * 2.0);
            if (next != null) c.godSpot = next;
        }
    }

    private static void spawn(MinecraftServer server, OrderCase c) {
        GodOfOrder g = OrderEntities.GOD_OF_ORDER.get().create(c.level);
        if (g == null) {
            c.plan.abort();
            finish(server, c);
            return;
        }
        float yaw = OrderGeometry.yawToward(c.target.x - c.godSpot.x, c.target.z - c.godSpot.z);
        g.moveTo(c.godSpot.x, c.godSpot.y, c.godSpot.z, yaw, 0f);
        g.setYHeadRot(yaw);
        g.setArena(BlockPos.containing(c.godSpot));
        g.bind(c.id);
        g.setOrderPhase(Phase.READING);
        c.level.addFreshEntity(g);
        c.god = g.getUUID();
        c.godSeen = true;
        OrderFx.appear(c.level, c.godSpot);
    }

    private static void strip(MinecraftServer server, OrderCase c, GodOfOrder g, ServerPlayer bearer, long now) {
        OrderStrip.Result r = OrderStrip.strip(server, c.sighting, now);
        List<String> ids = r.distinct();
        if (g != null) {
            if (ids.isEmpty()) g.say("There is nothing left to correct.");
            else if (c.plan.phase() == Phase.COMBAT) g.say(OrderText.provoked(c.level.random.nextInt(8)));
            else g.say(OrderText.farewell(c.level.random.nextInt(8)));
        }
        deliver(server, c, ids, bearer);
    }

    /** The duplicates are gone: dust, a word in chat, the Notice, and the cooldown for each relic taken. */
    private static void deliver(MinecraftServer server, OrderCase c, List<String> ids, ServerPlayer bearer) {
        long now = server.overworld().getGameTime();
        UniqueLedger ledger = UniqueLedger.get(server);
        c.taken = ids;
        if (ids.isEmpty()) return;
        OrderFx.dust(c.level, c.target.add(0, 1.0, 0));
        for (String id : ids) {
            ledger.recordVisit(id, now);
            BOOK.forget(id);
        }
        if (bearer != null) {
            bearer.sendSystemMessage(OrderChat.taken(ids));
            ItemStack notice = NoticeOfOrderItem.make(OrderEntities.NOTICE.get(), ids);
            if (!bearer.getInventory().add(notice)) bearer.drop(notice, false);
        }
    }

    private static void finish(MinecraftServer server, OrderCase c) {
        if (active != c) return;
        active = null;
        GodOfOrder g = c.god();
        if (g != null) {
            OrderFx.leave(c.level, g.position());
            g.discard();
        }
    }

    // ================================================================ calls from the God
    /** Is this the id of the visit that is going on? (A God from an earlier session, or whose visit ended, leaves.) */
    public static boolean knows(UUID caseId) { return active != null && active.id.equals(caseId); }

    /** The God's Erasure: it takes the duplicates of everyone who fought it (during the census it only counts) and the visit is over. */
    static void erased(MinecraftServer server, GodOfOrder god, List<ServerPlayer> fighters) {
        long now = server.overworld().getGameTime();
        boolean enforce = UniqueLedger.get(server).mode(now) == LedgerRules.Mode.ENFORCE;
        if (enforce) {
            for (ServerPlayer p : fighters) {
                OrderStrip.Result r = OrderStrip.stripPlayer(server, p, now);
                List<String> ids = r.distinct();
                if (ids.isEmpty()) continue;
                OrderFx.dust((ServerLevel) p.level(), p.position().add(0, 1.0, 0));
                UniqueLedger ledger = UniqueLedger.get(server);
                for (String id : ids) {
                    ledger.recordVisit(id, now);
                    BOOK.forget(id);
                }
                p.sendSystemMessage(OrderChat.taken(ids));
                ItemStack notice = NoticeOfOrderItem.make(OrderEntities.NOTICE.get(), ids);
                if (!p.getInventory().add(notice)) p.drop(notice, false);
            }
        }
        OrderCase c = active;
        if (c != null && c.god != null && c.god.equals(god.getUUID())) {
            c.plan.erased();
            finish(server, c);
        } else if (god.isAlive()) {
            OrderFx.leave((ServerLevel) god.level(), god.position());
            god.discard();
        }
    }

    /** The God died: the visit is over (the duplicate was taken before the fight began). */
    static void godDied(GodOfOrder god) {
        OrderCase c = active;
        if (c != null && c.god != null && c.god.equals(god.getUUID())) c.plan.lost();
    }

    /** The God was removed from the world without dying (its chunk unloaded, a command): the visit is over, unless the director itself sent it away. */
    static void godRemoved(GodOfOrder god) {
        OrderCase c = active;
        if (c != null && c.god != null && c.god.equals(god.getUUID())) c.plan.lost();
    }

    // ================================================================ commands and the stop
    /** An admin sends the Order to a player for a relic (the God comes whether or not a duplicate exists). False if it is busy. */
    static boolean summon(MinecraftServer server, ServerPlayer target, String relicId) {
        if (active != null) return false;
        long now = server.overworld().getGameTime();
        return start(server, new Sighting(relicId, Spot.player(target), null, now), true);
    }

    static boolean busy() { return active != null; }

    static int waiting() { return BOOK.size(); }

    static List<String> waitingIds() { return BOOK.ids(); }

    static void forget(String relicId) { BOOK.forget(relicId); }

    /** The describing line of the running case, or null. */
    static String describeActive() {
        OrderCase c = active;
        if (c == null) return null;
        return OrderText.titleCase(c.sighting.relicId()) + " " + c.sighting.spot().describe() + ", " + c.plan.phase().name().toLowerCase(java.util.Locale.ROOT)
                + " (" + c.plan.totalTicks() / 20 + " s)";
    }

    /** The server is stopping: the God leaves, the list is forgotten (the next scan finds what is still there). */
    public static void clear() {
        OrderCase c = active;
        active = null;
        if (c != null) {
            GodOfOrder g = c.god();
            if (g != null) g.discard();
        }
        BOOK.clear();
    }
}
