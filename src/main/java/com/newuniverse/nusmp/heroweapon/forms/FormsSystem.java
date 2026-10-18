package com.newuniverse.nusmp.heroweapon.forms;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Why: the one entry point of the forms system (the data loader, the absorption, the Weapon Book packets, the config {@code nusmp-heroweapon-forms-server.toml}, the
 * self-tests {@code hero.forms} and {@code hero.absorb}), called once from {@code HeroWeaponSystem.init} so the shared start-up never changes when this system
 * grows. It registers the reload listener for {@code data/nusmp/hero/forms}, the packets, and the few events that keep the loadout true: on login, respawn, dimension
 * change and every data reload the attribute modifiers are brought up to date and the weapon's forms are sent to the client; a bind teaches the base form; a release
 * takes everything off. Every handler is guarded, because an event handler that throws would break the event for every other mod.
 */
public final class FormsSystem {
    private FormsSystem() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean done;

    /** Once a second every bonded player's loadout is made to match the bond again: {@code refresh} changes nothing that already matches, and this catches a weapon put into the hand after the last call. */
    private static final int RECONCILE_TICKS = 20;

    public static synchronized void init(IEventBus modBus, ModContainer modContainer) {
        if (done) return;
        done = true;
        modContainer.registerConfig(ModConfig.Type.SERVER, FormsConfig.SPEC, "nusmp-heroweapon-forms-server.toml");
        modBus.addListener(FormsNet::registerPayloads);
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new FormsLoader()));
        NeoForge.EVENT_BUS.addListener(FormsSystem::onDatapackSync);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> sync(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> refresh(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> refresh(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> forget(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> stopped());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            try {
                FormsSync.tick(e.getServer());
                if (e.getServer().getTickCount() % RECONCILE_TICKS == 0) reconcile(e.getServer());
                FormsFeedback.flush(e.getServer());
            } catch (RuntimeException ex) {
                LOG.warn("[nusmp] hero forms: the periodic tick failed", ex);
            }
        });
        HeroBonds.addListener(new HeroBonds.Listener() {
            @Override
            public void onBound(ServerPlayer player, HeroBond bond) { bound(player, bond); }

            @Override
            public void onReleased(ServerPlayer player, HeroKind was) { released(player); }
        });
        FormsSelfTests.register();                          // keys hero.forms and hero.absorb, expected by the server smoke test (prefix "hero")
    }

    private static void reconcile(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) if (HeroBonds.has(p)) FormLoadout.refresh(p);
    }

    private static void onDatapackSync(OnDatapackSyncEvent e) {
        try {
            e.getRelevantPlayers().forEach(FormsSystem::sync);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] hero forms: the data pack sync hook failed", ex);
        }
    }

    /** Login or reload: bring the loadout up to date, catch up on forms the new data makes available, and send the forms to the client. */
    private static void sync(Object entity) {
        if (!(entity instanceof ServerPlayer p)) return;
        try {
            if (HeroBonds.has(p)) {
                FormLearning.learnAvailable(p, null, false);
                FormsSync.syncDefs(p);
            }
            HeroForms.refresh(p);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] hero forms: login sync failed for {}", p.getGameProfile().getName(), ex);
        }
    }

    private static void refresh(Object entity) {
        if (entity instanceof ServerPlayer p) HeroForms.refresh(p);
    }

    private static void forget(Object entity) {
        if (!(entity instanceof ServerPlayer p)) return;
        FormsSync.forget(p.getUUID());
        HeroAbsorb.forget(p.getUUID());
        FormLocks.forget(p.getUUID());
        FormsFeedback.forget(p.getUUID());
    }

    private static void stopped() {
        FormsStore.clearLoaded();
        FormsSync.clear();
        HeroAbsorb.clear();
        FormLocks.clear();
        FormsFeedback.clear();
    }

    /** A weapon was bound: the base form is learned and active (the bond already did that), the logic hears about it, the loadout and the client's copy are set up. */
    private static void bound(ServerPlayer player, HeroBond bond) {
        bond.unlock(HeroForm.BASE_ID);
        bond.setForm(HeroForm.BASE_ID);
        HeroForms.refresh(player);
        FormsSync.bondChanged(player);
        try {
            HeroCtx c = HeroCtx.of(player);
            if (c != null) {
                HeroForm base = HeroForms.active(bond);
                HeroLogics.of(bond.kind()).onFormChanged(c, base, base);
            }
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] hero forms: onFormChanged at bind failed", ex);
        }
    }

    private static void released(ServerPlayer player) {
        FormLoadout.clear(player);
        FormsSync.released(player);
        FormLocks.forget(player.getUUID());
    }
}
