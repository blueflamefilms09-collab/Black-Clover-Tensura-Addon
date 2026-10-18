package com.newuniverse.nusmp.skilltree.line;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

/**
 * Why: passives are things that are simply true while a line is equipped, so they live where the game decides the thing they change: the incoming-damage event decides what a blow does (a dodge, a
 * resistance, a critical hit, a mark, a ward), the damage-applied event tells how much really landed (for a lifesteal), the death, experience and drop events decide what a kill leaves, and a per-player
 * half-second beat carries everything that acts over time (auras, regeneration, attribute bonuses, the stance). All of them are thin: they find the player involved and hand over to
 * {@link LinePassives}, which asks the gate; none of them keeps anything in a static field except the one boolean that stops a reflected blow from triggering another reflection. Every handler is wrapped in
 * a catch so a fault in one executor is logged and can never break combat, a tick, or a kill. The beat is staggered by entity id so that a server full of players does not do all its line work on one
 * tick. Registered once by {@code LineSystem.init}.
 */
public final class LineEvents {
    private LineEvents() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** Keys in a creature's own data: the mark an art puts on a foe, the ward an art puts on the caster. */
    static final String MARK_UNTIL = "mark.until", MARK_BONUS = "mark.bonus", WARD_UNTIL = "ward.until", WARD_PCT = "ward.pct";
    private static boolean wired;
    /** True while the engine itself hurts someone (a reflected blow): the passives skip it, so reflections can never chain. Server thread only. */
    private static boolean busy;

    public static synchronized void register() {
        if (wired) return;
        wired = true;
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(LineEvents::onIncomingDamage);
        bus.addListener(LineEvents::onDamaged);
        bus.addListener(LineEvents::onDeath);
        bus.addListener(LineEvents::onExperience);
        bus.addListener(LineEvents::onDrops);
        bus.addListener(LineEvents::onPlayerTick);
        bus.addListener(LineEvents::onLogout);
    }

    /** Hurts {@code victim} for {@code amount} on behalf of {@code owner} (thorns, a ward) without letting the passives react to it. */
    public static void reflect(ServerPlayer owner, LivingEntity victim, float amount) {
        if (busy || amount <= 0f || victim == null || !victim.isAlive()) return;
        busy = true;
        try {
            victim.hurt(owner.damageSources().thorns(owner), amount);
        } finally {
            busy = false;
        }
    }

    // ------------------------------------------------------------------ damage
    private static void onIncomingDamage(LivingIncomingDamageEvent e) {
        try {
            if (busy) return;
            LivingEntity victim = e.getEntity();
            if (victim.level().isClientSide) return;
            float amount = e.getAmount();
            DamageSource src = e.getSource();
            long now = victim.level().getGameTime();

            if (now < LineState.getLong(victim, MARK_UNTIL)) amount *= (float) (1.0 + LineState.getDouble(victim, MARK_BONUS));        // a marked foe takes more from everyone

            if (victim instanceof ServerPlayer vp) {
                LineState.markCombat(vp, now);
                amount = LinePassives.taken(vp, src, amount);
                if (amount <= 0f) {
                    e.setCanceled(true);
                    return;
                }
                if (now < LineState.getLong(vp, WARD_UNTIL) && src.getEntity() instanceof LivingEntity attacker && attacker != vp) {
                    reflect(vp, attacker, (float) (amount * LineState.getDouble(vp, WARD_PCT)));
                }
            }
            if (src.getEntity() instanceof ServerPlayer attacker && attacker != victim) {
                LineState.markCombat(attacker, now);
                amount = LinePassives.dealt(attacker, victim, src, amount);
            }
            if (amount != e.getAmount()) e.setAmount(amount);
        } catch (Throwable t) {
            LOG.warn("[nusmp] line passive failed on damage: {}", t.toString());
        }
    }

    private static void onDamaged(LivingDamageEvent.Post e) {
        try {
            if (busy) return;
            if (e.getSource().getEntity() instanceof ServerPlayer attacker && attacker != e.getEntity()) LinePassives.dealtPost(attacker, e.getEntity(), e.getNewDamage());
        } catch (Throwable t) {
            LOG.warn("[nusmp] line passive failed after damage: {}", t.toString());
        }
    }

    // ------------------------------------------------------------------ kills
    private static void onDeath(LivingDeathEvent e) {
        try {
            if (e.getSource().getEntity() instanceof ServerPlayer killer && killer != e.getEntity()) LinePassives.kill(killer, e.getEntity());
        } catch (Throwable t) {
            LOG.warn("[nusmp] line passive failed on a kill: {}", t.toString());
        }
    }

    private static void onExperience(LivingExperienceDropEvent e) {
        try {
            if (e.getAttackingPlayer() instanceof ServerPlayer killer) e.setDroppedExperience(LinePassives.xp(killer, e.getDroppedExperience()));
        } catch (Throwable t) {
            LOG.warn("[nusmp] line passive failed on experience: {}", t.toString());
        }
    }

    private static void onDrops(LivingDropsEvent e) {
        try {
            if (e.getSource().getEntity() instanceof ServerPlayer killer && killer != e.getEntity()) LinePassives.drops(killer, e);
        } catch (Throwable t) {
            LOG.warn("[nusmp] line passive failed on drops: {}", t.toString());
        }
    }

    // ------------------------------------------------------------------ the beat
    private static void onPlayerTick(PlayerTickEvent.Post e) {
        try {
            if (e.getEntity() instanceof ServerPlayer p && (p.tickCount + p.getId()) % PassiveExecutors.BEAT_TICKS == 0) LinePassives.beat(p);
        } catch (Throwable t) {
            LOG.warn("[nusmp] line beat failed: {}", t.toString());
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        try {
            if (e.getEntity() instanceof ServerPlayer p) {
                LineAttrs.clear(p);
                LineStances.clear(p);
            }
        } catch (Throwable t) {
            LOG.warn("[nusmp] line cleanup failed: {}", t.toString());
        }
    }
}
