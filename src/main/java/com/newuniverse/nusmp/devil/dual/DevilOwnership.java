package com.newuniverse.nusmp.devil.dual;

import com.newuniverse.nusmp.devil.DevilConfig;
import com.newuniverse.nusmp.devil.DevilContract;
import com.newuniverse.nusmp.devil.DevilContracts;
import com.newuniverse.nusmp.devil.DevilStore;
import com.newuniverse.nusmp.devil.WorldDevil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.UUID;

/**
 * Why: One of One on the server (docs/devil_double_magic.md). Each devil of the roster has only one contract holder in the world; the table is {@link DevilHolders} (world saved data, by player UUID, so an
 * offline holder counts) and the decisions are the pure {@link HolderRules}. This class is the glue: it refuses a caller when the devil is held ({@link #refuses}), writes the claim when a contract is signed
 * ({@link #claim}), and keeps the table true at the few moments that matter: a holder dies (the grace clock starts), lets the devil in again (it stops), logs out (the absence clock starts) or in (it stops
 * and the table is reconciled with the contract), and a bound duel ends in a kill. No tick work, no scan: every path is a lookup in a table of at most 256 rows, on an event.
 *
 * <p>The three ways to take a held devil, all server decided and switchable ({@code devilDual.oneOfOne.*}): the holder's death lapse (they died and did not let the devil in again for 72 in-game hours),
 * the absence lapse (not seen for 30 real days), and the bound duel ({@code /devil duel <holder>}: kill them within five minutes and the devil is reserved for you for a day). An operator can always release or
 * hand over a devil. A contract older than this system is never dissolved by it ({@link DevilContract#registered}): it is adopted if its devil is free, tolerated if not.
 */
public final class DevilOwnership {
    private DevilOwnership() {}

    /** Game ticks between two duels a challenger opens (10 minutes). */
    public static final long DUEL_COOLDOWN = 12000L;

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DevilOwnership::onLogin);
        gameBus.addListener(DevilOwnership::onLogout);
        gameBus.addListener(DevilOwnership::onDeath);
        gameBus.addListener(DevilOwnership::onKill);
    }

    private static long now(MinecraftServer s) { return s.overworld().getGameTime(); }

    // ---------------------------------------------------------------- the verdict
    public static HolderRules.Verdict verdict(MinecraftServer server, String devilId, UUID who) {
        if (server == null || devilId == null || devilId.isEmpty()) return HolderRules.Verdict.FREE;
        if (!com.newuniverse.nusmp.devil.phase2.Phase2.oneOfOne(devilId)) return HolderRules.Verdict.FREE;                  // phase 2: a lesser devil is shared (its cap is checked by the gate), not held by one
        return HolderRules.check(DevilHolders.get(server).slot(devilId), who, now(server), System.currentTimeMillis(), DualConfig.holderParams());
    }

    /** True (with a line) when the caller may not call this devil because somebody else holds it. Called when a ritual asks and again when the contract is signed. */
    public static boolean refuses(ServerPlayer p, WorldDevil d) {
        HolderRules.Verdict v = verdict(p.getServer(), d.id(), p.getUUID());
        if (v.mayTake) return false;
        p.sendSystemMessage(Component.translatableWithFallback("anime_multiverse.devil.dual.taken." + v.name().toLowerCase(java.util.Locale.ROOT), HolderRules.refusal(v)).withStyle(ChatFormatting.RED));
        return true;
    }

    // ---------------------------------------------------------------- claiming
    /**
     * The contract with {@code d} was just signed by {@code p}: enter it in the table. A previous holder whose bond lapsed loses the contract now if online (an offline one loses it at login). Returns false
     * when the devil may not be taken after all (the table says held: another player signed in the meantime).
     */
    public static boolean claim(ServerPlayer p, DevilContract c, WorldDevil d) {
        MinecraftServer server = p.getServer();
        HolderRules.Verdict v = verdict(server, d.id(), p.getUUID());
        if (!v.mayTake) return false;
        if (!DualConfig.oneOfOne() || !com.newuniverse.nusmp.devil.phase2.Phase2.oneOfOne(d.id())) return true;      // the old behaviour: no table at all; phase 2: a shared devil has no row
        DevilHolders h = DevilHolders.get(server);
        UUID old = h.holderOf(d.id());
        if (com.newuniverse.nusmp.devil.phase2.Phase2.stacking()) h.claimKeep(d.id(), p.getUUID(), now(server)); else h.claim(d.id(), p.getUUID(), now(server));      // phase 2: a player who stacks keeps their other Supreme devils
        c.registered = true;
        if (old != null && !old.equals(p.getUUID())) {
            ServerPlayer was = server.getPlayerList().getPlayer(old);
            DevilContract oc = was == null ? null : DevilStore.peek(was);
            if (oc != null && d.id().equals(oc.devil)) {
                DevilContracts.dissolve(was, oc);
                was.sendSystemMessage(Component.translatableWithFallback("anime_multiverse.devil.dual.lost", "Another has taken your devil. The bond is gone.").withStyle(ChatFormatting.DARK_RED));
            }
        }
        if (old != null && !old.equals(p.getUUID()) && com.newuniverse.nusmp.devil.phase2.Phase2.on()) com.newuniverse.nusmp.devil.phase2.Phase2.lose(server, old, d.id(), false);      // phase 2: a held-but-not-active contract goes from the ledger too
        return true;
    }

    /** An operator cleared this player's contract: the devil they held is free again. */
    public static void release(ServerPlayer p, String devilId) {
        if (devilId == null || devilId.isEmpty()) return;
        DevilHolders h = DevilHolders.get(p.getServer());
        if (p.getUUID().equals(h.holderOf(devilId))) h.free(devilId);
    }

    /** The holder let the devil in again after a death: the lapse clock stops. */
    public static void renewed(ServerPlayer p) {
        if (DualConfig.oneOfOne()) DevilHolders.get(p.getServer()).renewed(p.getUUID());
    }

    // ---------------------------------------------------------------- events
    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !DualConfig.oneOfOne()) return;
        DevilContract c = DevilStore.peek(p);
        if (c == null || !c.hasContract() || c.devil.isEmpty()) return;
        MinecraftServer server = p.getServer();
        DevilHolders h = DevilHolders.get(server);
        h.online(p.getUUID());
        if (!com.newuniverse.nusmp.devil.phase2.Phase2.oneOfOne(c.devil)) return;                // phase 2: a shared devil has no holder row to reconcile
        UUID cur = h.holderOf(c.devil);
        if (cur == null) {
            if (h.slot(c.devil) == null) { h.claim(c.devil, p.getUUID(), now(server)); c.registered = true; }      // an older contract is adopted when its devil is free
        } else if (cur.equals(p.getUUID())) {
            c.registered = true;
        } else if (c.registered) {                                                            // somebody else holds it and this contract was entered: the bond was lost while offline
            DevilContracts.dissolve(p, c);
            p.sendSystemMessage(Component.translatableWithFallback("anime_multiverse.devil.dual.lost", "Another has taken your devil. The bond is gone.").withStyle(ChatFormatting.DARK_RED));
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.getServer() == null) return;
        DevilHolders h = DevilHolders.get(p.getServer());
        h.seen(p.getUUID(), System.currentTimeMillis());
        h.dropDuels(p.getUUID(), now(p.getServer()));
    }

    /** A player kills something: the corruption of their price eases (one attachment lookup per death event). */
    private static void onKill(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || e.getEntity() == p) return;
        DevilContract c = DevilStore.peek(p);
        if (c == null) return;
        c.corruption = CorruptionRules.onKill(c.corruption, DualConfig.corruption());
        c.lastKill = DevilContracts.gameTime(p);
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.getServer() == null || !DualConfig.oneOfOne()) return;
        MinecraftServer server = p.getServer();
        DevilHolders h = DevilHolders.get(server);
        long now = now(server);
        h.died(p.getUUID(), now);
        DevilHolders.Duel duel = h.duelOfHolder(p.getUUID());
        UUID killer = e.getSource().getEntity() instanceof ServerPlayer k ? k.getUUID() : null;
        if (duel != null && HolderRules.duelWon(duel.holder(), duel.challenger(), p.getUUID(), killer, duel.until(), now)) won(server, h, p, duel, now);
        h.dropDuels(p.getUUID(), now);
    }

    /** The challenger killed the holder inside the duel: the holder loses the contract, the devil is reserved for the victor. */
    private static void won(MinecraftServer server, DevilHolders h, ServerPlayer loser, DevilHolders.Duel d, long now) {
        DevilContract lc = DevilStore.peek(loser);
        if (lc != null && d.devil().equals(lc.devil)) DevilContracts.dissolve(loser, lc);
        if (com.newuniverse.nusmp.devil.phase2.Phase2.on()) com.newuniverse.nusmp.devil.phase2.Phase2.lose(server, loser.getUUID(), d.devil(), false);      // phase 2: and from the ledger, if it was held but not active
        h.reserve(d.devil(), d.challenger(), now + HolderRules.reserveTicks(DualConfig.reserveHours()));
        WorldDevil w = WorldDevil.byId(d.devil());
        String name = w == null ? d.devil() : w.name();
        loser.sendSystemMessage(Component.translatableWithFallback("anime_multiverse.devil.dual.duel_lost", "You lost the duel. %s is no longer yours.", name).withStyle(ChatFormatting.DARK_RED));
        ServerPlayer victor = server.getPlayerList().getPlayer(d.challenger());
        if (victor != null) victor.sendSystemMessage(Component.translatableWithFallback("anime_multiverse.devil.dual.duel_won", "You won the duel. %s is reserved for you: call it within %s in-game hours.", name, DualConfig.reserveHours()).withStyle(ChatFormatting.GOLD));
    }

    // ---------------------------------------------------------------- the bound duel
    /** {@code /devil duel <holder>}. Returns the line to show (success or why not) and whether it opened. */
    public static boolean duel(ServerPlayer challenger, ServerPlayer holder) {
        MinecraftServer server = challenger.getServer();
        long now = now(server);
        DevilHolders h = DevilHolders.get(server);
        String devil = h.devilOf(holder.getUUID());
        DevilContract mine = DevilStore.of(challenger);
        String why = null;
        if (!DualConfig.oneOfOne() || !DualConfig.duelEnabled() || !DevilConfig.enabled()) why = "off";
        else if (devil.isEmpty() || WorldDevil.byId(devil) == null) why = "no_devil";
        else if (mine.hasContract()) why = "have_contract";
        else if (now - mine.lastDuel < DUEL_COOLDOWN) why = "cooldown";
        else if (holder.isCreative() || holder.isSpectator() || !challenger.canHarmPlayer(holder)) why = "no_pvp";
        else if (!HolderRules.duelMayStart(true, challenger.level() == holder.level(), challenger.distanceTo(holder), DualConfig.duelRange(), challenger == holder,
                h.duelOfHolder(holder.getUUID()) != null || h.duelOfChallenger(challenger.getUUID()) != null, h.duelCount(), DevilHolders.MAX_DUELS)) why = "cannot_start";
        if (why != null) {
            challenger.sendSystemMessage(Component.translatableWithFallback("anime_multiverse.devil.dual.duel_no." + why, DUEL_NO.getOrDefault(why, "No duel.")).withStyle(ChatFormatting.RED));
            return false;
        }
        long until = now + HolderRules.duelTicks(DualConfig.duelSeconds());
        if (!h.openDuel(new DevilHolders.Duel(challenger.getUUID(), holder.getUUID(), devil, until))) return false;
        mine.lastDuel = now;
        WorldDevil w = WorldDevil.byId(devil);
        challenger.sendSystemMessage(Component.translatableWithFallback("anime_multiverse.devil.dual.duel_open", "The duel is bound. Kill %s within %s seconds to take %s.", holder.getName(), DualConfig.duelSeconds(), w.name()).withStyle(ChatFormatting.GOLD));
        holder.sendSystemMessage(Component.translatableWithFallback("anime_multiverse.devil.dual.duel_warn", "%s challenges you for %s. If they kill you within %s seconds the devil is theirs.", challenger.getName(), w.name(), DualConfig.duelSeconds()).withStyle(ChatFormatting.DARK_RED));
        return true;
    }

    private static final java.util.Map<String, String> DUEL_NO = java.util.Map.of(
            "off", "Duels are off on this server.",
            "no_devil", "That player holds no devil of the roster.",
            "have_contract", "You already hold a devil contract: only one without a bond can take a devil.",
            "cooldown", "You opened a duel a moment ago. Wait ten minutes.",
            "no_pvp", "You may not fight that player.",
            "cannot_start", "A duel needs both of you in the same world, close together, and neither already in a duel.");

    /** An operator hands the devil to a player (or frees it with {@code to == null}): the table only; the contract is made by the ritual or {@code /devil grant}. Returns false for an unknown devil. */
    public static boolean adminSet(MinecraftServer server, String devilId, ServerPlayer to) {
        if (WorldDevil.byId(devilId) == null) return false;
        DevilHolders h = DevilHolders.get(server);
        h.free(devilId);
        if (to != null) h.reserve(devilId, to.getUUID(), now(server) + HolderRules.reserveTicks(DualConfig.reserveHours()));
        return true;
    }
}
