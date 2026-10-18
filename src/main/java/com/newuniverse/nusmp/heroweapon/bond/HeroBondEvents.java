package com.newuniverse.nusmp.heroweapon.bond;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.heroweapon.rules.HeroInterference;
import com.newuniverse.nusmp.heroweapon.rules.HeroRules;
import com.newuniverse.nusmp.heroweapon.skill.HeroCombat;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroKeys;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import com.newuniverse.nusmp.heroweapon.skill.HeroUse;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Why: this is where the bond becomes a living thing. It registers the core's gates and listeners with {@link HeroBonds} (the bind is refused when the system is
 * off or another player holds a unique weapon; after a bind or release the register, the quest hook, the loan and the client are told; the forms system is asked
 * to look again at once after a bind and one tick after a release, because the release listeners run before the bond is cleared), and it follows the
 * player through the moments that threaten the loan: login, respawn, dimension change, logout, the server stopping. Every four ticks the loan is made whole and the
 * open menu is checked; every five the weapon's {@code onTick} runs while it is out; every second the client's copy of the bond is refreshed if it changed and the
 * interference set is rebuilt. Everything is guarded so a fault in one weapon's logic is logged once and never reaches the game. A weapon that was out when the
 * player left is taken out of the inventory that is saved and put back at the next login.
 */
public final class HeroBondEvents {
    private HeroBondEvents() {}

    /** Registers the gates and listeners with {@link HeroBonds} and the events on the game bus. */
    public static void register(IEventBus bus) {
        HeroBonds.addGate(HeroBondEvents::gate);
        HeroBonds.addListener(new HeroBonds.Listener() {
            @Override
            public void onBound(ServerPlayer player, HeroBond bond) { bound(player, bond); }

            @Override
            public void onReleased(ServerPlayer player, HeroKind was) { released(player, was); }
        });
        bus.addListener(HeroBondEvents::onLogin);
        bus.addListener(HeroBondEvents::onRespawn);
        bus.addListener(HeroBondEvents::onDimension);
        bus.addListener(HeroBondEvents::onLogout);
        bus.addListener(HeroBondEvents::onPlayerTick);
        bus.addListener(HeroBondEvents::onServerTick);
        bus.addListener(HeroBondEvents::onStopping);
        bus.addListener(HeroBondEvents::onStopped);
    }

    // ================================================================ the gate and the listeners of the bond
    /** The core's gate: the system must be on, a bond must be allowed, and a unique weapon must be free. */
    static String gate(ServerPlayer p, HeroKind kind) {
        if (!HeroConfig.enabled()) return "the hero weapons are switched off on this server";
        if (HeroConfig.maxBonds() < 1) return "no hero weapon can be bound on this server at the moment";
        boolean unique = kind.isHoly() ? HeroConfig.holyUnique() : HeroConfig.vassalUnique();
        return unique ? HeroRegistry.refusal(p, kind) : null;
    }

    private static void bound(ServerPlayer p, HeroBond bond) {
        HeroKind kind = bond.kind();
        HeroRegistry.claim(p, kind);
        NusmpEvents.fire(p, "hero.bound", kind.itemId());
        SafeCast.tick("hero.bound.forms", () -> HeroForms.refresh(p));
        HeroSync.sendNow(p);
    }

    private static void released(ServerPlayer p, HeroKind was) {
        HeroLoan.onReleased(p);
        HeroUse.clearCooldowns(p.getUUID());
        HeroRegistry.release(p, was);
        HeroSync.sendEmpty(p);
        // the listeners run before HeroBonds clears the bond, so the forms system is asked to look again one tick later, when the player has no weapon any more
        SpellRuntime.later(p.serverLevel(), 1, () -> SafeCast.tick("hero.release.forms", () -> {
            if (!p.hasDisconnected()) HeroForms.refresh(p);
        }));
    }

    // ================================================================ the moments of a player's life
    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) restore(p, false);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) restore(p, false);
    }

    private static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) restore(p, true);
    }

    /** The register agrees with the bond, the loan is whole, the forms are applied, the client has the bond. */
    private static void restore(ServerPlayer p, boolean dimension) {
        SafeCast.tick("hero.restore", () -> {
            if (dimension) HeroUse.cancelPending(p, true);
            HeroRegistry.reconcile(p);
            HeroLoan.reconcile(p);
            HeroForms.refresh(p);
            if (HeroBonds.has(p)) HeroSync.sendNow(p);
        });
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        SafeCast.tick("hero.logout", () -> HeroLoan.shutdown(p));
        UUID id = p.getUUID();
        HeroUse.forget(id);
        HeroKeys.forget(id);
        HeroSync.forget(id);
        HeroCombat.forget(id);
        HeroRules.forget(id);
        HeroInterference.forget(id);
        HeroLoan.forget(id);
    }

    private static void onStopping(ServerStoppingEvent e) {
        for (ServerPlayer p : new ArrayList<>(e.getServer().getPlayerList().getPlayers())) SafeCast.tick("hero.stop", () -> HeroLoan.shutdown(p));
    }

    private static void onStopped(ServerStoppedEvent e) {
        HeroUse.clear();
        HeroKeys.clear();
        HeroSync.clear();
        HeroCombat.clear();
        HeroRules.clear();
        HeroInterference.clear();
        HeroLoan.clear();
    }

    // ================================================================ the beats
    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        int t = p.tickCount;
        SafeCast.tick("hero.tick", () -> {
            boolean dirty = HeroLoan.takeDirty(p.getUUID());
            if (dirty || (t & 3) == 1) {
                HeroLoan.reconcile(p);
                HeroLoan.scanMenu(p);
            }
            HeroLoan.processAdoption(p);
            if (t % 5 == 0) tickLogic(p);
            if (t % 20 == 3) HeroSync.poll(p);
        });
    }

    /** The weapon's own tick, every five ticks while it is out. */
    private static void tickLogic(ServerPlayer p) {
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound() || !b.summoned() || !HeroConfig.enabled()) return;
        HeroCtx c = HeroCtx.of(p);
        if (c != null) HeroLogics.of(c.kind).onTick(c);
    }

    private static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 20 == 5) SafeCast.tick("hero.interference", () -> HeroInterference.refresh(server.getPlayerList().getPlayers()));
    }
}
