package com.newuniverse.nusmp.beastwill;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.multiverse.MultiverseProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: getting a Beast Will (3.45.0) in one place, so the two ways in (absorbing a core, and a beast's direct inheritance) go through the SAME checks and the same preview. Nothing is consumed on the way:
 * {@link #open} only checks and shows (the player sees the beast, the rank, the affinity, what the host needs and how well they fit), {@link #confirm} checks AGAIN on the server (a forged or old
 * confirm is dropped, nothing is trusted from the client) and only then takes the core and binds the will. A refused core stays in the inventory. Pending asks are kept per player UUID, one each, for a
 * minute, and dropped at logout. The same entry ({@link #open} with source OFFER) is the reusable integration point for future beasts and quests: {@link BeastWillApi}.
 */
public final class BeastWillAcquire {
    private BeastWillAcquire() {}

    public static final int CORE = 0, OFFER = 1;
    public static final int PENDING_TICKS = 1200;

    /** What is waiting for the player's yes: the will, the beast, the rank, where it comes from, the ledger key and when it lapses. */
    record Pending(String will, String beast, BeastRank rank, int source, String key, long expires) {}

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    /** The answer of the eligibility check: ok, or the lang suffix of the reason ({@code nusmp.beastwill.no.<reason>}). */
    public record Verdict(boolean ok, String reason, double compat, double havEp, double needEp) {
        static Verdict no(String reason, double compat, double have, double need) { return new Verdict(false, reason, compat, have, need); }
    }

    // ------------------------------------------------------------------ the eligibility check (nothing changes)
    public static Verdict check(ServerPlayer p, BeastWillDef def, BeastRank rank, String key) {
        if (!BeastWillConfig.enabled()) return Verdict.no("disabled", 0, 0, 0);
        if (def == null) return Verdict.no("unknown_will", 0, 0, 0);
        double have = TensuraBridge.maxEp(p), need = def.requireEp();
        String race = MultiverseProfile.race(p).name();
        boolean listed = def.hostRace(race) || BeastWillConfig.extraHost(race);
        double compat = BeastWillRules.compat(listed, def.otherHostCompat() * BeastWillConfig.otherRaceScale());
        if (rank == null || !rank.atLeast(def.minRank())) return Verdict.no("rank", compat, have, need);
        BeastWillState s = BeastWillStore.peek(p);
        if (s != null && s.has(def.id())) return Verdict.no("already", compat, have, need);
        if (s != null && s.wills.size() >= BeastWillState.MAX_WILLS) return Verdict.no("full", compat, have, need);
        if (!listed && !BeastWillConfig.otherRacesMayHost()) return Verdict.no("race", compat, have, need);
        if (have < need) return Verdict.no("ep", compat, have, need);
        if (key != null && !key.isEmpty() && p.getServer() != null && BeastWillLedger.get(p.getServer()).has(key)) return Verdict.no("used", compat, have, need);
        return new Verdict(true, "", compat, have, need);
    }

    // ------------------------------------------------------------------ the preview (the ask)
    /** Starts the ask: remembers it and sends the preview. Returns false when the will does not exist (nothing is sent). A refused ask is still shown, with its reason. */
    public static boolean open(ServerPlayer p, String willId, String beast, BeastRank rank, int source, String key) {
        BeastWillDef def = BeastWillCatalog.get(willId);
        if (def == null || !BeastWillConfig.enabled()) return false;
        Verdict v = check(p, def, rank, key);
        long now = p.level().getGameTime();
        if (v.ok()) PENDING.put(p.getUUID(), new Pending(def.id(), beast == null ? def.beast() : beast, rank, source, key == null ? "" : key, now + PENDING_TICKS));
        else PENDING.remove(p.getUUID());
        BeastWillNet.send(p, new BeastWillNet.Preview(def.id(), beast == null || beast.isBlank() ? def.beast() : beast, def.affinity(), rank.ordinal(), source, v.ok(), v.reason(), (int) Math.round(v.compat() * 100), v.havEp(), v.needEp()));
        return true;
    }

    /** The player said no (or closed the screen). */
    public static void decline(ServerPlayer p) { PENDING.remove(p.getUUID()); }

    public static void forget(UUID player) { PENDING.remove(player); }

    // ------------------------------------------------------------------ the yes
    /** Checks again and, only if everything still holds, takes the core and gives the will. Returns true when the will was given. */
    public static boolean confirm(ServerPlayer p) {
        Pending ask = PENDING.remove(p.getUUID());
        if (ask == null || p.level().getGameTime() > ask.expires()) return false;
        BeastWillDef def = BeastWillCatalog.get(ask.will());
        Verdict v = check(p, def, ask.rank(), ask.key());
        if (!v.ok()) {
            BeastWillFeedback.say(p, "no." + v.reason(), true);
            return false;
        }
        ItemStack held = null;
        if (ask.source() == CORE) {
            held = holdingCore(p, ask.key());
            if (held == null) {
                BeastWillFeedback.say(p, "no.gone", true);
                return false;
            }
        }
        if (p.getServer() == null || !BeastWillLedger.get(p.getServer()).consume(ask.key().isEmpty() ? "free:" + UUID.randomUUID() : ask.key())) {
            BeastWillFeedback.say(p, "no.used", true);
            return false;
        }
        if (held != null && !p.getAbilities().instabuild) held.shrink(1);
        grant(p, def, true);
        return true;
    }

    /** The core in a hand with this serial, or null. */
    private static ItemStack holdingCore(ServerPlayer p, String serial) {
        for (InteractionHand h : InteractionHand.values()) {
            ItemStack st = p.getItemInHand(h);
            BeastWillComponents.Core c = BeastWillComponents.of(st);
            if (c != null && c.serial().equals(serial) && st.getItem() == BeastWillItems.CORE.get()) return st;
        }
        return null;
    }

    // ------------------------------------------------------------------ the gift itself
    /** Binds the will (keeping the progress it had), makes it the active one when the player has none, and tells them. No check here: callers check first (or are an operator command). */
    public static boolean grant(ServerPlayer p, BeastWillDef def, boolean announce) {
        BeastWillState s = BeastWillStore.of(p);
        if (!s.bind(def.id())) return false;
        if (s.active.isEmpty()) s.activate(def.id(), 0L);
        BeastWillManager.markDirty(p);
        if (announce) {
            BeastWillFeedback.say(p, "absorbed", true, BeastWillText.name(def));
            BeastWillFeedback.say(p, "assimilation_begins", true);
        }
        return true;
    }
}
