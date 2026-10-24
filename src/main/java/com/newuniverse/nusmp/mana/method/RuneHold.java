package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The server side of hold-to-charge. It uses the grimoire's OWN hold (the cast key of the Tensura skill: {@code GrimoireBook.onHeld} every tick while it is down, {@code onRelease} with the
 * ticks held when it is let go); nothing new is bound. The SERVER measures the hold: the clients are only told "held", "released after N" and "cancelled" ({@link RuneNet.HoldPayload}) so
 * they can write the rings while the player holds.
 *
 * <p>State, all keyed by the player's UUID and at most one entry per player and kind: {@code HOLDING} (a Mana Method page is down) and {@code RELEASED} (the charge of a release, waiting
 * for the cast that follows it a few ticks later, taken by {@link #take}). Both are dropped on logout, death, dimension change and server stop, and swept by {@link #tick}: a hold nobody
 * keeps feeding goes after {@value #STALE} ticks (the page was changed, the grimoire was put away), a release nobody takes after {@value #RELEASED_TTL} ticks.
 *
 * <p>Cost: one map lookup and two int compares per held tick of a player holding a Mana Method page; one packet at the start and one at the release (nearby players, through the effect
 * packet budget); an idle server pays one {@code isEmpty()} pair per tick.
 */
public final class RuneHold {
    private RuneHold() {}

    /** Ticks without a held tick after which a hold is considered abandoned. */
    public static final int STALE = 6;
    /** Ticks a release waits for its cast (the wind-up is 4 to 7 ticks). */
    public static final int RELEASED_TTL = 60;
    private static final int SWEEP_EVERY = 5;

    private static final class Holding {
        int mode, lastHeld = -1;
        long lastSeen;
        boolean ready;
        RuneSpec spec;
    }

    private record Released(int mode, Charge charge, long at) {}

    private static final Map<UUID, Holding> HOLDING = new HashMap<>();
    private static final Map<UUID, Released> RELEASED = new HashMap<>();

    public static int holding() { return HOLDING.size(); }

    public static int released() { return RELEASED.size(); }

    /** Is hold-to-charge on (the setting, and the Mana Method itself)? */
    public static boolean active() { return ManaMethodConfig.holdToCharge() && ManaMethodConfig.enabled(); }

    // ---------------------------------------------------------------- the grimoire's hooks
    /** One held tick of a Mana Method page (called by {@code GrimoireBook.onHeld} for those pages only). */
    public static void onHeld(GrimoireBook book, ManasSkillInstance inst, ServerPlayer p, int heldTicks, int mode) {
        if (!active()) return;
        long now = p.level().getGameTime();
        Holding h = HOLDING.get(p.getUUID());
        if (h == null || h.mode != mode || heldTicks < h.lastHeld) {                      // a new hold (first tick, another page, or the counter started over)
            if (h != null && h.ready) cancel(p, h);
            h = new Holding();
            h.mode = mode;
            h.ready = book.chargeReady(inst, p, mode);
            if (h.ready) {
                h.spec = ManaMethodPages.specFor(book.page(mode) == null ? null : book.page(mode).id(), inst, p, Charge.LEGACY);
                if (h.spec != null) RuneNet.sendHold(p, RuneNet.PHASE_HOLD, h.spec, 0, ManaMethodConfig.minHold(), ManaMethodConfig.maxHold());
            }
            HOLDING.put(p.getUUID(), h);
        }
        h.lastHeld = heldTicks;
        h.lastSeen = now;
    }

    /** The release of a Mana Method page after {@code heldTicks} (called by {@code GrimoireBook.onRelease}, once the cast is going to follow). */
    public static void onRelease(GrimoireBook book, ManasSkillInstance inst, ServerPlayer p, int mode, int heldTicks) {
        if (!active()) return;
        long now = p.level().getGameTime();
        Holding h = HOLDING.remove(p.getUUID());
        if (h != null && h.mode != mode) { if (h.ready) cancel(p, h); h = null; }
        int held = Math.max(heldTicks, h == null ? 0 : h.lastHeld);
        int min = ManaMethodConfig.minHold(), max = ManaMethodConfig.maxHold();
        Charge c = RuneCharge.of(held, min, max);
        RELEASED.put(p.getUUID(), new Released(mode, c, now));
        RuneSpec spec = h == null ? null : h.spec;
        boolean ready = h != null && h.ready;
        if (h == null) {                                                                  // a tap shorter than a tick: no held tick came, the release tells the clients everything
            ready = book.chargeReady(inst, p, mode);
            if (ready) spec = ManaMethodPages.specFor(book.page(mode) == null ? null : book.page(mode).id(), inst, p, Charge.LEGACY);
        }
        if (ready && spec != null) RuneNet.sendHold(p, RuneNet.PHASE_RELEASE, spec, c.held(), min, max);
    }

    /** The charge waiting for this player's cast of this page, or null when none (then the cast is the old fixed one). Consumed. */
    public static Charge take(UUID owner, int mode, long now) {
        Released r = RELEASED.remove(owner);
        if (r == null || r.mode() != mode || now - r.at() > RELEASED_TTL) return null;
        return r.charge();
    }

    private static void cancel(ServerPlayer p, Holding h) {
        if (h.spec != null) RuneNet.sendHold(p, RuneNet.PHASE_CANCEL, h.spec, 0, ManaMethodConfig.minHold(), ManaMethodConfig.maxHold());
    }

    // ---------------------------------------------------------------- the tick
    /** The sweep: nothing while no one holds; otherwise one pass every {@value #SWEEP_EVERY} ticks over at most one entry per player. */
    public static void tick(MinecraftServer server, long now) {
        if ((HOLDING.isEmpty() && RELEASED.isEmpty()) || now % SWEEP_EVERY != 0) return;
        for (Iterator<Map.Entry<UUID, Holding>> it = HOLDING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Holding> e = it.next();
            if (now - e.getValue().lastSeen <= STALE) continue;
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p != null && e.getValue().ready) cancel(p, e.getValue());
            it.remove();
        }
        RELEASED.values().removeIf(r -> now - r.at() > RELEASED_TTL);
    }

    // ---------------------------------------------------------------- clean-up
    public static void cancelOwner(UUID owner) {
        HOLDING.remove(owner);
        RELEASED.remove(owner);
    }

    public static void clearAll() {
        HOLDING.clear();
        RELEASED.clear();
    }

    // ---------------------------------------------------------------- test hooks
    /** Puts a release in the queue as {@link #onRelease} would, without a player or a packet (tests only). */
    static void releaseForTest(UUID owner, int mode, int held, long now) {
        RELEASED.put(owner, new Released(mode, RuneCharge.of(held, ManaMethodConfig.minHold(), ManaMethodConfig.maxHold()), now));
    }
}
