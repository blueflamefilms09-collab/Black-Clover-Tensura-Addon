package com.newuniverse.nusmp.heroweapon.curse;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.Sin;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Why: the Curse Series lives on the game bus, and this is the whole list of what it listens to, so a reader sees the curse's footprint on the game in one place.
 * Every ten ticks the server looks at each bonded player ({@link CurseService#tick}); a creature dying (the wielder's defeat, an ally's death, a kill by a wielder)
 * and a wielder being hurt (a betrayal) feed despair; a heal passes through Wrath's flames and Pride's refusal; a vanilla experience orb is left on the ground while Gluttony is
 * worn; a login, a respawn and a change of dimension tell the client the state and put the tax back in force; a logout and the server stopping clear the small
 * caches. The bond listener tells the client when a weapon is bound and takes the curse's modifiers off when it is released. NOTHING here can throw out into the
 * game: each handler catches and logs once. The tuning object is rebuilt from the config at most once a second (a death event can fire hundreds of times a minute).
 */
final class CurseEvents {
    private CurseEvents() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean warned;
    private static CurseTuning cache;
    private static int cacheTick = Integer.MIN_VALUE;

    /** Attaches every handler to the game bus. Called once from {@link CurseSystem#init}. */
    static void register(IEventBus bus) {
        bus.addListener(CurseEvents::onServerTick);
        bus.addListener(EventPriority.LOWEST, CurseEvents::onDeath);
        bus.addListener(CurseEvents::onDamage);
        bus.addListener(CurseEvents::onHeal);
        bus.addListener(CurseEvents::onXp);
        bus.addListener(CurseEvents::onLogin);
        bus.addListener(CurseEvents::onRespawn);
        bus.addListener(CurseEvents::onDimension);
        bus.addListener(CurseEvents::onLogout);
        bus.addListener(CurseEvents::onStopped);
    }

    /** The bond listener: a new bond starts with a quiet screen, a released one loses the curse's modifiers. */
    static final HeroBonds.Listener BOND_LISTENER = new HeroBonds.Listener() {
        @Override
        public void onBound(ServerPlayer player, HeroBond bond) {
            try {
                CurseSync.reset(player, CurseService.now(player));
            } catch (RuntimeException e) {
                failed("bind", e);
            }
        }

        @Override
        public void onReleased(ServerPlayer player, HeroKind was) {
            try {
                CurseService.onReleased(player, CurseService.now(player));
            } catch (RuntimeException e) {
                failed("release", e);
            }
        }
    };

    /** The current tuning, rebuilt from the config when the cached one is a second old. Callers must not change it. */
    static CurseTuning tuning(MinecraftServer server) {
        int now = server == null ? 0 : server.getTickCount();
        if (cache == null || now < cacheTick || now - cacheTick >= 20) {
            cache = CurseConfig.tuning();
            cacheTick = now;
        }
        return cache;
    }

    private static void failed(String what, RuntimeException e) {
        if (warned) return;
        warned = true;
        LOG.warn("[nusmp] The Curse Series failed in its {} handler and carried on (further failures are not logged): {}", what, e.toString(), e);
    }

    // ------------------------------------------------------------------ the clock
    static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        int tick = server.getTickCount();
        if (tick % 10 != 0) return;
        boolean second = tick % 20 == 0;
        boolean enabled = CurseConfig.enabled();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            try {
                if (!HeroBonds.has(p)) continue;
                if (!enabled) {
                    if (second) CurseEffects.clearModifiers(p);
                    continue;
                }
                if (KeyThree.available()) CurseService.pollKeyThree(p, CurseService.now(p), tuning(server));
                CurseService.tick(p, CurseService.now(p), second, tuning(server));
            } catch (RuntimeException ex) {
                failed("tick", ex);
            }
        }
    }

    // ------------------------------------------------------------------ what happens to people
    static void onDeath(LivingDeathEvent e) {
        try {
            LivingEntity dead = e.getEntity();
            if (dead.level().isClientSide || !CurseConfig.enabled()) return;
            CurseService.onDeath(dead, e.getSource(), dead.level().getGameTime(), tuning(dead.getServer()));
        } catch (RuntimeException ex) {
            failed("death", ex);
        }
    }

    static void onDamage(LivingDamageEvent.Post e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p) || !HeroBonds.has(p) || !CurseConfig.enabled()) return;
            CurseService.onDamaged(p, e.getSource(), e.getNewDamage(), CurseService.now(p), tuning(p.getServer()));
        } catch (RuntimeException ex) {
            failed("damage", ex);
        }
    }

    /** Wrath's curse flames cut every heal, Pride refuses help (a heal over the wielder's own recovery); the cuts only ever reduce. */
    static void onHeal(LivingHealEvent e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p)) return;
            HeroBond b = HeroBonds.of(p);
            if (!b.isBound() || !CurseConfig.enabled()) return;
            HeroForm f = CurseService.wornForm(b);
            if (f == null || f.sin() == null) return;
            CurseTuning t = tuning(p.getServer());
            float scale = SinMath.healScale(f.sin(), e.getAmount(), t.intensity(f.sin()), t.wrathHealCut, t.prideHelpCut, t.prideSelfHealMax);
            if (scale >= 1f) return;
            if (scale <= 0f) e.setCanceled(true);
            else e.setAmount(e.getAmount() * scale);
        } catch (RuntimeException ex) {
            failed("heal", ex);
        }
    }

    /** Gluttony turns experience away: a vanilla orb is not picked up while the sin is worn, it stays on the ground for later (the config can switch this off). */
    static void onXp(PlayerXpEvent.PickupXp e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p) || !CurseConfig.enabled()) return;
            HeroBond b = HeroBonds.of(p);
            if (!b.isBound()) return;
            HeroForm f = CurseService.wornForm(b);
            if (f == null || f.sin() != Sin.GLUTTONY) return;
            CurseTuning t = tuning(p.getServer());
            if (t.gluttonyBlocksXp && t.intensity(Sin.GLUTTONY) > 0.0) e.setCanceled(true);
        } catch (RuntimeException ex) {
            failed("experience", ex);
        }
    }

    // ------------------------------------------------------------------ the player's session
    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        try {
            if (e.getEntity() instanceof ServerPlayer p && CurseConfig.enabled()) CurseService.onEnter(p, CurseService.now(p), tuning(p.getServer()));
        } catch (RuntimeException ex) {
            failed("login", ex);
        }
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        try {
            if (e.getEntity() instanceof ServerPlayer p && CurseConfig.enabled()) CurseService.onEnter(p, CurseService.now(p), tuning(p.getServer()));
        } catch (RuntimeException ex) {
            failed("respawn", ex);
        }
    }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        try {
            if (e.getEntity() instanceof ServerPlayer p && CurseConfig.enabled()) CurseService.onEnter(p, CurseService.now(p), tuning(p.getServer()));
        } catch (RuntimeException ex) {
            failed("dimension", ex);
        }
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() == null) return;
        CurseSync.forget(e.getEntity().getUUID());
        CurseEffects.forget(e.getEntity().getUUID());
        KeyThree.forget(e.getEntity().getUUID());
    }

    static void onStopped(ServerStoppedEvent e) {
        CurseSync.clear();
        CurseEffects.clear();
        KeyThree.clear();
        cache = null;
        cacheTick = Integer.MIN_VALUE;
        warned = false;
    }
}
