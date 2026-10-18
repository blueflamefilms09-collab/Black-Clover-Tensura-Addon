package com.newuniverse.nusmp.elite.races.runtime;

import com.newuniverse.nusmp.boss.BossEntities;
import com.newuniverse.nusmp.elite.races.EliteRacesConfig;
import com.newuniverse.nusmp.elite.races.data.RaceTemplate;
import com.newuniverse.nusmp.elite.races.race.EliteRace;
import com.newuniverse.nusmp.elite.races.rule.KillKeys;
import com.newuniverse.nusmp.elite.races.state.Zenkai;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: the game-bus listeners of the elite races, kept thin: each one only finds the player and hands over to the class that owns the rule ({@link RaceTransforms} for the
 * form, {@link RaceLedger} for the counters, {@link Zenkai} for the trait). The tick listener costs one cache lookup for a player who is not an elite race; the kill listener is a
 * few comparisons; the damage listener returns at once unless the victim is a player of an elite race with the trait. Every listener is a no-op while the master switch is off.
 */
public final class RaceRuntime {
    private RaceRuntime() {}

    public static void register(IEventBus game) {
        game.addListener(RaceRuntime::onTick);
        game.addListener(RaceRuntime::onDeath);
        game.addListener(RaceRuntime::onDamaged);
        game.addListener(RaceRuntime::onLogin);
        game.addListener(RaceRuntime::onLogout);
        game.addListener(RaceRuntime::onRespawn);
        game.addListener(RaceRuntime::onClone);
    }

    private static void onTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            try {
                RaceTransforms.tick(p);
            } catch (RuntimeException ex) {
                // never let a race tick kill the server loop; the next tick tries again
                com.mojang.logging.LogUtils.getLogger().warn("[nusmp] elite races: tick failed for {}", p.getGameProfile().getName(), ex);
            }
        }
    }

    // ---------------------------------------------------------------- kills
    private static boolean isBoss(LivingEntity e) {
        return e instanceof EnderDragon || e instanceof WitherBoss || BossEntities.isModBoss(e);
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!EliteRacesConfig.countKills()) return;
        LivingEntity dead = e.getEntity();
        if (!(e.getSource().getEntity() instanceof ServerPlayer killer) || killer == dead) return;
        for (String key : KillKeys.classify(dead instanceof Player, dead instanceof Enemy, isBoss(dead), dead.getMaxHealth(), EliteRacesConfig.killMinHealth())) {
            RaceLedger.addKill(killer, key);
        }
    }

    // ---------------------------------------------------------------- Zenkai
    private static void onDamaged(LivingDamageEvent.Post e) {
        if (!EliteRacesConfig.zenkai() || !(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        EliteRace race = RaceTransforms.raceOf(p);
        RaceTemplate t = race == null ? null : race.template();
        if (t == null || !t.hasTrait("zenkai")) return;
        double after = p.getMaxHealth() <= 0 ? 1.0 : p.getHealth() / p.getMaxHealth();
        Zenkai.State before = RaceLedger.zenkai(p);
        Zenkai.State now = Zenkai.onSurvivedHit(before, after, p.serverLevel().getGameTime(), t.tier());
        if (now != before) {
            RaceLedger.setZenkai(p, now);
            RaceTransforms.invalidate(p);
            if (EliteRacesConfig.announce())
                p.displayClientMessage(RaceMessages.msg("zenkai", "Your body remembers the brink: Zenkai %s.", now.stacks()), true);
        }
    }

    // ---------------------------------------------------------------- lifecycle
    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) RaceTransforms.onLogin(p);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) RaceTransforms.onLogout(p);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) RaceTransforms.onRespawn(p);
    }

    private static void onClone(PlayerEvent.Clone e) {
        RaceLedger.copy(e.getOriginal(), e.getEntity());
    }
}
