package com.newuniverse.nusmp.elite.nations.world;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.elite.nations.core.GuardRules;
import com.newuniverse.nusmp.elite.nations.core.Nation;
import com.newuniverse.nusmp.elite.nations.core.Permission;
import com.newuniverse.nusmp.elite.nations.core.Res;
import com.newuniverse.nusmp.elite.nations.net.NationNet;
import com.newuniverse.nusmp.elite.nations.war.PvpRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: every game-bus handler of the Nations system in one place, each doing one small thing and delegating to a service: kills (war score, contract progress, bounties, guard penalty), blocks (capital
 * protection and the banner rules, contract mining), explosions (capital area), damage (the PvP rules: no friendly fire inside or between allied nations, war PvP only while ACTIVE, guards spare their
 * own), crafting and exploring (contract progress), guards joining or leaving a level, login and logout (names, caches), and the reload listener and command registration. All of them return at once when the
 * master switch is off and never act inside the dungeon dimensions (the dungeon has its own guard). A handler that throws is caught, so a bug here never kills a tick.
 */
public final class NationEvents {
    private NationEvents() {}

    private static final String BANNER_STAMP = "nusmp_nation_msg";

    public static void register(IEventBus gameBus) {
        gameBus.addListener(NationEvents::onReloadListeners);
        gameBus.addListener(NationEvents::onCommands);
        gameBus.addListener(NationEvents::onLogin);
        gameBus.addListener(NationEvents::onLogout);
        gameBus.addListener(NationEvents::onStopped);
        gameBus.addListener(NationEvents::onDeath);
        gameBus.addListener(NationEvents::onPlace);
        gameBus.addListener(EventPriority.HIGH, NationEvents::onBreak);
        gameBus.addListener(EventPriority.LOWEST, true, NationEvents::onBreakDone);
        gameBus.addListener(NationEvents::onExplosion);
        gameBus.addListener(NationEvents::onDamage);
        gameBus.addListener(NationEvents::onCraft);
        gameBus.addListener(NationEvents::onPlayerTick);
        gameBus.addListener(NationEvents::onJoin);
        gameBus.addListener(NationEvents::onLeave);
        gameBus.addListener(NationTicker::onServerTick);
    }

    private static void onReloadListeners(AddReloadListenerEvent e) { e.addListener(new NationsLoader()); }

    private static void onCommands(RegisterCommandsEvent e) { NationCommand.register(e.getDispatcher()); }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && !(p instanceof FakePlayer) && NationsConfig.enabled()) guard(() -> NationService.onLogin(p));
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            NationNet.forget(p.getUUID());
            NationActions.forget(p.getUUID());
            NationTicker.forget(p.getUUID());
        }
    }

    private static void onStopped(ServerStoppedEvent e) {
        NationNet.forgetAll();
        NationActions.forgetAll();
        NationTicker.reset();
        GuardService.forgetAll();
    }

    private static void guard(Runnable r) {
        try { r.run(); } catch (RuntimeException ex) { com.mojang.logging.LogUtils.getLogger().warn("[nusmp] nations handler failed: {}", ex.toString()); }
    }

    private static boolean inDungeon(net.minecraft.world.level.Level level) { return DungeonGuard.on(level); }

    // ---------------------------------------------------------------- kills
    private static void onDeath(LivingDeathEvent e) {
        if (!NationsConfig.enabled() || e.getEntity().level().isClientSide) return;
        Entity src = e.getSource().getEntity();
        if (!(src instanceof ServerPlayer killer)) return;
        LivingEntity victim = e.getEntity();
        guard(() -> {
            if (victim instanceof ServerPlayer vp) {
                WarService.onPlayerKill(killer, vp);
            } else if (GuardService.isGuard(victim)) {
                String nid = GuardService.nationOf(victim);
                NationsData d = NationService.data(killer.getServer());
                if (!d.nationIdOf(killer.getUUID()).equals(nid)) WarService.onGuardKilled(killer, nid);
            } else {
                ContractService.onKill(killer, victim);
            }
            ContractService.claim(killer, victim);
        });
    }

    // ---------------------------------------------------------------- blocks
    private static boolean bypass(ServerPlayer p) { return p.isCreative() && p.hasPermissions(2); }

    private static void notify(ServerPlayer p, String key) {
        long now = p.level().getGameTime();
        long last = p.getPersistentData().getLong(BANNER_STAMP);
        if (last != 0L && now >= last && now - last < 40L) return;
        p.getPersistentData().putLong(BANNER_STAMP, now);
        p.displayClientMessage(Msg.of(key), true);
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (!NationsConfig.enabled() || !(e.getLevel() instanceof ServerLevel lv)) return;
        BlockState st = e.getPlacedBlock();
        boolean banner = st.is(NationBlocks.BANNER.get());
        guard(() -> {
            if (banner) {
                if (!(e.getEntity() instanceof ServerPlayer p)) { e.setCanceled(true); return; }
                Res r = NationService.canPlaceCapital(p, lv, e.getPos());
                if (!r.ok()) { e.setCanceled(true); notifyRes(p, r); }
                return;
            }
            if (!NationsConfig.get(NationsConfig.PROTECT_CAPITAL) || inDungeon(lv)) return;
            if (!(e.getEntity() instanceof ServerPlayer p)) return;
            Nation z = zoneAt(lv, e.getPos());
            if (z != null && !bypass(p) && !z.can(p.getUUID(), Permission.BUILD)) { e.setCanceled(true); notify(p, "nusmp.nation.m.capital_protected"); }
        });
    }

    private static void notifyRes(ServerPlayer p, Res r) {
        long now = p.level().getGameTime();
        long last = p.getPersistentData().getLong(BANNER_STAMP);
        if (last != 0L && now >= last && now - last < 20L) return;
        p.getPersistentData().putLong(BANNER_STAMP, now);
        p.displayClientMessage(Msg.colored(r), true);
    }

    private static Nation zoneAt(ServerLevel lv, BlockPos pos) {
        return NationService.data(lv.getServer()).capitalZoneAt(lv.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ(), NationsConfig.get(NationsConfig.CAPITAL_RADIUS), 64);
    }

    private static void onBreak(BlockEvent.BreakEvent e) {
        if (!NationsConfig.enabled() || !(e.getLevel() instanceof ServerLevel lv) || !(e.getPlayer() instanceof ServerPlayer p)) return;
        guard(() -> {
            if (inDungeon(lv)) return;
            NationsData d = NationService.data(lv.getServer());
            BlockPos pos = e.getPos();
            if (e.getState().is(NationBlocks.BANNER.get())) {
                Nation n = d.capitalAt(lv.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ());
                if (n == null) return;
                if (n.leader.equals(p.getUUID()) || bypass(p)) NationService.clearCapital(lv.getServer(), n);
                else { e.setCanceled(true); notify(p, "nusmp.nation.m.banner_protected"); }
                return;
            }
            if (!NationsConfig.get(NationsConfig.PROTECT_CAPITAL)) return;
            Nation z = zoneAt(lv, pos);
            if (z != null && !bypass(p) && !z.can(p.getUUID(), Permission.BUILD)) { e.setCanceled(true); notify(p, "nusmp.nation.m.capital_protected"); }
        });
    }

    /** Runs even when another handler cancelled: only counts the block when the break really happens. */
    private static void onBreakDone(BlockEvent.BreakEvent e) {
        if (e.isCanceled() || !NationsConfig.enabled() || !(e.getPlayer() instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        guard(() -> ContractService.onMine(p, e.getState()));
    }

    private static void onExplosion(ExplosionEvent.Detonate e) {
        if (!NationsConfig.enabled() || !NationsConfig.get(NationsConfig.PROTECT_EXPLOSIONS) || !(e.getLevel() instanceof ServerLevel lv) || inDungeon(lv)) return;
        guard(() -> {
            NationsData d = NationService.data(lv.getServer());
            if (d.nations.isEmpty()) return;
            String dim = lv.dimension().location().toString();
            int radius = NationsConfig.get(NationsConfig.CAPITAL_RADIUS);
            e.getAffectedBlocks().removeIf(pos -> d.capitalZoneAt(dim, pos.getX(), pos.getY(), pos.getZ(), radius, 64) != null);
        });
    }

    // ---------------------------------------------------------------- damage
    private static void onDamage(LivingIncomingDamageEvent e) {
        if (!NationsConfig.enabled() || e.getEntity().level().isClientSide) return;
        Entity src = e.getSource().getEntity();
        if (!(src instanceof ServerPlayer attacker)) return;
        LivingEntity victim = e.getEntity();
        guard(() -> {
            if (victim instanceof ServerPlayer vp) {
                PvpRules.Verdict v = WarService.pvp(attacker, vp);
                if (PvpRules.denies(v)) {
                    e.setCanceled(true);
                    notify(attacker, "nusmp.nation.m.pvp_" + v.name().toLowerCase(java.util.Locale.ROOT));
                }
            } else if (GuardService.isGuard(victim)) {
                String nid = GuardService.nationOf(victim);
                NationsData d = NationService.data(attacker.getServer());
                String an = d.nationIdOf(attacker.getUUID());
                if (an.equals(nid) || (!an.isEmpty() && d.alliances.areAllied(an, nid))) e.setCanceled(true);
            }
        });
    }

    // ---------------------------------------------------------------- contracts
    private static void onCraft(PlayerEvent.ItemCraftedEvent e) {
        if (!NationsConfig.enabled() || !(e.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        guard(() -> ContractService.onCraft(p, e.getCrafting()));
    }

    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer || p.tickCount % 100 != 7 || !NationsConfig.enabled()) return;
        guard(() -> ContractService.onBiome(p));
    }

    // ---------------------------------------------------------------- guards
    private static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide || !(e.getLevel() instanceof ServerLevel lv) || !GuardService.isGuard(e.getEntity())) return;
        try {
            if (!GuardService.onJoin(lv.getServer(), e.getEntity())) e.setCanceled(true);
        } catch (RuntimeException ex) {
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] nation guard adoption failed: {}", ex.toString());
        }
    }

    private static void onLeave(EntityLeaveLevelEvent e) {
        if (!e.getLevel().isClientSide && GuardService.isGuard(e.getEntity())) GuardService.onLeave(e.getEntity());
    }

    /** Capital zone helper for others (self-tests). */
    static boolean inZone(Nation n, BlockPos pos) { return n.hasCapital && GuardRules.inZone(pos.getX(), pos.getY(), pos.getZ(), n.cx, n.cy, n.cz, NationsConfig.get(NationsConfig.CAPITAL_RADIUS), 64); }
}
