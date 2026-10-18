package com.newuniverse.nusmp.elite.nations.world;

import com.newuniverse.nusmp.elite.api.EliteHooks;
import com.newuniverse.nusmp.elite.nations.core.Nation;
import com.newuniverse.nusmp.elite.nations.core.Permission;
import com.newuniverse.nusmp.elite.nations.core.Res;
import com.newuniverse.nusmp.elite.nations.econ.InterestPolicy;
import com.newuniverse.nusmp.elite.nations.econ.Ledger;
import com.newuniverse.nusmp.elite.nations.econ.TreasuryBridge;
import com.newuniverse.nusmp.elite.nations.econ.UbsBridge;
import com.newuniverse.nusmp.elite.nations.event.TreasuryChangedEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.UUID;

/**
 * Why: all money movement of a nation goes through here, so the ledger, the cap, the event and the dirty-marking are never forgotten: player deposits (from the wallet hook or from UBS cash), withdrawals
 * (to the wallet), rewards and spoils, tax, and the periodic interest (built-in simple rate, the UBS bridge, or a custom {@link InterestPolicy} set through the API). It is also the implementation of
 * the public {@link TreasuryBridge}. Amounts are copper coins. Server thread only.
 */
public final class Treasury implements TreasuryBridge {
    public static final Treasury BRIDGE = new Treasury();

    private static volatile InterestPolicy custom;

    private Treasury() {}

    public static void setCustomInterest(InterestPolicy p) { custom = p; }

    static long stamp() { return System.currentTimeMillis(); }

    // ---------------------------------------------------------------- low level (no wallet)
    /** Credits a nation's ledger (clamped to the cap), marks it, posts the event and refreshes the level. Returns what was accepted. */
    public static long credit(MinecraftServer s, Nation n, Ledger.Kind kind, UUID actor, long amount, String note) {
        if (s == null || n == null || amount <= 0) return 0L;
        long accepted = n.ledger.credit(stamp(), kind, actor, amount, note, NationPerks.treasuryCap(n));
        if (accepted > 0) changed(s, n, accepted, note);
        return accepted;
    }

    public static long debit(MinecraftServer s, Nation n, Ledger.Kind kind, UUID actor, long amount, String note) {
        if (s == null || n == null || amount <= 0) return 0L;
        long taken = n.ledger.debit(stamp(), kind, actor, amount, note);
        if (taken > 0) changed(s, n, -taken, note);
        return taken;
    }

    private static void changed(MinecraftServer s, Nation n, long delta, String note) {
        NationService.refresh(s, n);
        NationService.data(s).touch(n.id);
        post(new TreasuryChangedEvent(n.id, delta, n.ledger.balance(), note == null ? "" : note));
    }

    static void post(net.neoforged.bus.api.Event e) {
        try { NeoForge.EVENT_BUS.post(e); } catch (RuntimeException ex) { com.mojang.logging.LogUtils.getLogger().warn("[nusmp] a nation event listener threw: {}", ex.toString()); }
    }

    // ---------------------------------------------------------------- players
    /** Pays coins from the player's wallet (or UBS cash) into the treasury. Needs the TREASURY permission? No: any member may donate; only withdrawing needs the permission. */
    public static Res deposit(ServerPlayer p, long coins, boolean fromUbs) {
        MinecraftServer s = p.getServer();
        if (!NationsConfig.enabled()) return Res.fail("nusmp.nation.m.disabled");
        NationsData d = NationService.data(s);
        Nation n = d.nationOf(p.getUUID());
        if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (coins <= 0) return Res.fail("nusmp.nation.m.bad_amount");
        long room = n.ledger.room(NationPerks.treasuryCap(n));
        if (room <= 0) return Res.fail("nusmp.nation.m.treasury_full");
        long amount = Math.min(coins, room);
        boolean paid = fromUbs ? UbsBridge.take(p.getUUID(), amount) : EliteHooks.wallet.charge(p, amount);
        if (!paid) return Res.fail(fromUbs ? "nusmp.nation.m.no_ubs_cash" : "nusmp.nation.m.no_coins", amount);
        long accepted = credit(s, n, Ledger.Kind.DEPOSIT, p.getUUID(), amount, "deposit by " + p.getGameProfile().getName());
        if (accepted < amount) refund(p, amount - accepted, fromUbs);
        return Res.ok("nusmp.nation.m.deposited", accepted, n.name);
    }

    public static Res withdraw(ServerPlayer p, long coins, boolean toUbs) {
        MinecraftServer s = p.getServer();
        if (!NationsConfig.enabled()) return Res.fail("nusmp.nation.m.disabled");
        NationsData d = NationService.data(s);
        Nation n = d.nationOf(p.getUUID());
        if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (!n.can(p.getUUID(), Permission.TREASURY)) return Res.fail("nusmp.nation.m.no_permission", Permission.TREASURY.id());
        if (coins <= 0) return Res.fail("nusmp.nation.m.bad_amount");
        if (toUbs && !UbsBridge.available()) return Res.fail("nusmp.nation.m.ubs_unavailable");
        long taken = debit(s, n, Ledger.Kind.WITHDRAW, p.getUUID(), coins, "withdrawal by " + p.getGameProfile().getName());
        if (taken <= 0) return Res.fail("nusmp.nation.m.treasury_short", n.ledger.balance());
        refund(p, taken, toUbs);
        return Res.ok("nusmp.nation.m.withdrawn", taken, n.name);
    }

    private static void refund(ServerPlayer p, long coins, boolean ubs) {
        if (ubs && UbsBridge.give(p.getUUID(), coins)) return;
        EliteHooks.wallet.give(p, coins);
    }

    /** The leader's tax: a member's contract coins are split by the nation's tax rate. */
    public static Res setTax(ServerPlayer p, int percent) {
        NationsData d = NationService.data(p.getServer());
        Nation n = d.nationOf(p.getUUID());
        if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (!n.can(p.getUUID(), Permission.TREASURY)) return Res.fail("nusmp.nation.m.no_permission", Permission.TREASURY.id());
        int max = NationsConfig.get(NationsConfig.MAX_TAX_PERCENT);
        if (percent < 0 || percent > max) return Res.fail("nusmp.nation.m.bad_tax", max);
        n.taxPercent = percent;
        d.touch(n.id);
        return Res.ok("nusmp.nation.m.tax_set", percent);
    }

    // ---------------------------------------------------------------- interest
    /** The policy in force: a custom one (API) wins unless interest is off; else UBS if asked and answering; else the built-in rate. */
    public static InterestPolicy policy() {
        String mode = NationsConfig.lower(NationsConfig.INTEREST_MODE);
        if (mode.equals("off")) return InterestPolicy.NONE;
        InterestPolicy builtin = new InterestPolicy.Simple(NationsConfig.get(NationsConfig.INTEREST_PER_MILLE), NationsConfig.get(NationsConfig.INTEREST_MIN_BALANCE), NationsConfig.get(NationsConfig.INTEREST_MAX_PER_PERIOD));
        InterestPolicy c = custom;
        if (c != null) return (id, bal) -> safe(c, id, bal);
        if (mode.equals("ubs")) {
            InterestPolicy ubs = UbsBridge.policy();
            return (id, bal) -> {
                long v = safe(ubs, id, bal);
                return v >= 0 ? v : builtin.interest(id, bal);
            };
        }
        return builtin;
    }

    private static long safe(InterestPolicy p, String id, long bal) {
        try { return p.interest(id, bal); } catch (RuntimeException e) { return 0L; }
    }

    /** Pays the interest of every nation that is due. Called from the throttled loop. */
    public static void runInterest(MinecraftServer s, long now) {
        long period = NationsConfig.minutes(NationsConfig.INTEREST_PERIOD_MINUTES);
        InterestPolicy pol = policy();
        NationsData d = NationService.data(s);
        for (Nation n : d.nations.values()) {
            if (n.lastInterest == 0L) { n.lastInterest = now; d.setDirty(); continue; }
            if (now - n.lastInterest < period) continue;
            n.lastInterest = now;
            d.setDirty();
            long i = Math.max(0L, safe(pol, n.id, n.ledger.balance()));
            if (i > 0) credit(s, n, Ledger.Kind.INTEREST, null, i, "interest");
        }
    }

    // ---------------------------------------------------------------- TreasuryBridge
    private static NationsData dataOrNull() {
        MinecraftServer s = NationService.server();
        return s == null ? null : NationService.data(s);
    }

    @Override public long balance(String nationId) {
        NationsData d = dataOrNull();
        Nation n = d == null || nationId == null ? null : d.nations.get(nationId);
        return n == null ? 0L : n.ledger.balance();
    }

    @Override public long deposit(String nationId, long coins, String reason) {
        NationsData d = dataOrNull();
        if (d == null || nationId == null || !NationsConfig.enabled()) return 0L;
        Nation n = d.nations.get(nationId);
        return n == null ? 0L : credit(NationService.server(), n, Ledger.Kind.REWARD, null, coins, reason);
    }

    @Override public boolean withdraw(String nationId, long coins, String reason) {
        NationsData d = dataOrNull();
        if (d == null || nationId == null || !NationsConfig.enabled()) return false;
        Nation n = d.nations.get(nationId);
        return n != null && debit(NationService.server(), n, Ledger.Kind.WITHDRAW, null, coins, reason) > 0;
    }

    @Override public long cap(String nationId) {
        NationsData d = dataOrNull();
        Nation n = d == null || nationId == null ? null : d.nations.get(nationId);
        return n == null ? 0L : NationPerks.treasuryCap(n);
    }
}
