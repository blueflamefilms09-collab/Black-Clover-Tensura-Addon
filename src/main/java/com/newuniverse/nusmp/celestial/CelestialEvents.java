package com.newuniverse.nusmp.celestial;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.gear.GearDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Why: the game-bus listeners of Celestial Spirit Magic: once a second the carried keys are noticed and the twelfth gold key hands over the gate key, a worn Star Dress is taken
 * off when its minute is over, the King's permanent buff is put back; a login or respawn sends the client its read-only copy; a logout takes the dress off; and a monster killed
 * in the Mega-Dungeon may drop a key ({@link CelestialRules#dropChance}: a boss, a mob of the celestial_court theme, or any other mob at a small chance). Never throws out of an event.
 */
public final class CelestialEvents {
    private CelestialEvents() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    public static void register(IEventBus gameBus) {
        gameBus.addListener(CelestialEvents::onPlayerTick);
        gameBus.addListener(CelestialEvents::onLogin);
        gameBus.addListener(CelestialEvents::onRespawn);
        gameBus.addListener(CelestialEvents::onLogout);
        gameBus.addListener(net.neoforged.bus.api.EventPriority.LOW, CelestialEvents::onDrops);
        SpiritWorld.register(gameBus);
    }

    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 3) return;
        try {
            boolean fresh = !CelestialLedger.scan(p).isEmpty();
            CelestialDress.tick(p);
            KingRewards.refresh(p);
            if (fresh) CelestialNet.sync(p);
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] celestial tick failed once and is skipped", ex);
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { CelestialDress.clear(p); KingRewards.refresh(p); CelestialNet.sync(p); }
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { KingRewards.refresh(p); CelestialNet.sync(p); }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) CelestialDress.clear(p);
    }

    /** A key may fall from a monster killed by a player in the Mega-Dungeon. */
    private static void onDrops(LivingDropsEvent e) {
        try {
            if (!(e.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level) || !MegaDungeon.in(level)) return;
            ServerPlayer killer = e.getSource().getEntity() instanceof ServerPlayer sp ? sp : (mob.getKillCredit() instanceof ServerPlayer lp ? lp : null);
            if (killer == null || GearDrops.isServant(mob)) return;
            boolean boss = GearDrops.isBoss(mob);
            if (!boss && !GearDrops.hostile(mob)) return;
            BlockPos pos = mob.blockPosition();
            int floor = MegaDungeon.floorAt(level, pos);
            Sector sector = MegaData.get(level.getServer()).sectorAt(pos);
            boolean court = sector != null && CelestialRules.isCourt(sector.theme.id());
            CelestialRules.Source src = boss ? CelestialRules.Source.BOSS_MOB : court ? CelestialRules.Source.COURT_MOB : CelestialRules.Source.MOB;
            double chance = CelestialRules.dropChance(src, floor, (int) Math.round(killer.getLuck()));
            if (level.getRandom().nextDouble() >= chance) return;
            CelestialKey key = CelestialRules.pickKey(level.getRandom().nextDouble(), CelestialAttachments.of(killer).owned(), floor);
            if (key == null) return;
            ItemStack stack = CelestialItems.stack(key);
            e.getDrops().add(new ItemEntity(level, mob.getX(), mob.getY() + 0.3, mob.getZ(), stack));
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] celestial key drop failed once and is skipped", ex);
        }
    }
}
