package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wave 12's game-bus listeners. Each starts with a cheap test (is the attacker a player with a relic in the main hand) and does its work only
 * for the ten weapons; periodic work runs once a second per player, staggered by entity id, and the climb only while a wielder is pushed against a
 * wall. Nothing keeps a static reference to a level, entity or player.
 */
final class Wave12Hooks {
    private Wave12Hooks() {}

    private static final Logger LOG = LoggerFactory.getLogger("nusmp");
    private static boolean registered;

    static synchronized void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(Wave12Hooks::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave12Hooks::onDamagePost);
        NeoForge.EVENT_BUS.addListener(Wave12Hooks::onDeath);
        NeoForge.EVENT_BUS.addListener(Wave12Hooks::onPlayerTick);
    }

    /** The player who struck this blow by hand (not a spell or a thrown thing), or null. */
    private static ServerPlayer striker(DamageSource src) {
        if (!(src.getEntity() instanceof ServerPlayer p) || src.getDirectEntity() != p || !src.is(DamageTypes.PLAYER_ATTACK)) return null;
        return p;
    }

    // ================================================================ damage
    private static void onIncoming(LivingIncomingDamageEvent e) {
        if (Wave0Util.busy()) return;
        ServerPlayer atk = striker(e.getSource());
        LivingEntity victim = e.getEntity();
        if (atk == null || victim == atk || e.getAmount() <= 0f) return;
        String id = Wave12Data.heldId(atk);
        if (id.isEmpty()) return;
        try {
            switch (id) {
                case Wave12Heavy.JAW -> {
                    if (RelicFlags.has(atk, Wave12Heavy.JAW_FLAG)) e.setAmount(Wave12Heavy.jawBlow(atk, victim, e.getAmount()));
                }
                case Wave12Magic.WARMAGE -> {
                    if (RelicFlags.has(atk, Wave12Magic.WARMAGE_FLAG)) e.setAmount(e.getAmount() + 2.0f);                  // the +2 force
                }
                case Wave12Magic.CRYSTAL -> {
                    if (RelicFlags.has(atk, Wave12Magic.CRYSTAL_FLAG) && Wave12Magic.armActive(atk)) e.setAmount(e.getAmount() + 2.0f);   // the +2 sword
                }
                case Wave12Magic.BOTTLE -> {
                    if (RelicFlags.has(atk, Wave12Magic.BOTTLE_FLAG) && Wave12Magic.dry(atk.getMainHandItem())) e.setAmount(e.getAmount() * Wave12Rules.EMPTY_BLOW);
                }
                case Wave12Magic.SHROOM -> {
                    if (RelicFlags.has(atk, Wave12Magic.SHROOM_FLAG)) e.setAmount(Wave12Magic.shroomRoll(atk, victim, e.getAmount()));
                }
                default -> { }
            }
        } catch (RuntimeException | LinkageError failed) {
            LOG.error("[nusmp] wave 12 damage hook failed", failed);
        }
    }

    private static void onDamagePost(LivingDamageEvent.Post e) {
        if (Wave0Util.busy()) return;
        ServerPlayer atk = striker(e.getSource());
        LivingEntity victim = e.getEntity();
        if (atk == null || victim == atk || !victim.isAlive()) return;
        String id = Wave12Data.heldId(atk);
        if (id.isEmpty()) return;
        try {
            switch (id) {
                case Wave12Heavy.JAW -> Wave12Heavy.jawAfter(atk, victim);
                case Wave12Light.WHIP -> {
                    if (RelicFlags.has(atk, Wave12Light.WHIP_FLAG) && Wave12Light.wet(atk)) Wave12Light.humiliate(atk, victim);
                }
                case Wave12Light.BLADES -> {
                    if (RelicFlags.has(atk, Wave12Light.BLADES_FLAG)) Wave12Light.bladeBlow(atk, victim);
                }
                case Wave12Magic.CRYSTAL -> {
                    if (RelicFlags.has(atk, Wave12Magic.CRYSTAL_FLAG) && Wave12Magic.armActive(atk)) Wave12Magic.crystalAfter(atk, victim);
                }
                case Wave12Magic.SHROOM -> {
                    if (RelicFlags.has(atk, Wave12Magic.SHROOM_FLAG)) Wave12Magic.shroomAfter(atk, victim);
                }
                default -> { }
            }
        } catch (RuntimeException | LinkageError failed) {
            LOG.error("[nusmp] wave 12 hit hook failed", failed);
        }
    }

    /** The Disintegration Dagger: a fatal strike of the wielder's hand leaves a body that blows apart. */
    private static void onDeath(LivingDeathEvent e) {
        if (Wave0Util.busy()) return;
        ServerPlayer atk = striker(e.getSource());
        if (atk == null || e.getEntity() == atk) return;
        try {
            if (Wave12Magic.DAGGER.equals(Wave12Data.heldId(atk)) && RelicFlags.has(atk, Wave12Magic.DAGGER_FLAG)) Wave12Magic.daggerKill(atk, e.getEntity());
        } catch (RuntimeException | LinkageError failed) {
            LOG.error("[nusmp] wave 12 death hook failed", failed);
        }
    }

    // ================================================================ ticks
    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        try {
            if (p.onGround()) {
                Wave12Light.landed(p);
            } else if (p.horizontalCollision && Wave12Light.HOOKS.equals(Wave12Data.heldId(p)) && RelicFlags.has(p, Wave12Light.HOOKS_FLAG)) {
                Wave12Light.climb(p);
            }
            if ((p.tickCount + p.getId()) % 20 == 3) slow(p);
        } catch (RuntimeException | LinkageError failed) {
            LOG.error("[nusmp] wave 12 tick failed", failed);
        }
    }

    /** Once a second per player. */
    private static void slow(ServerPlayer p) {
        if (!Wave12Data.hasPlayerData(p) && Wave12Data.heldId(p).isEmpty()) return;
        if (Wave12Light.WHIP.equals(Wave12Data.heldId(p)) && RelicFlags.has(p, Wave12Light.WHIP_FLAG)) Wave12Light.whipTick(p);
        if (Wave12Data.hasPlayerData(p) || Wave12Magic.CRYSTAL.equals(Wave12Data.heldId(p))) Wave12Magic.crystalTick(p);
    }
}
