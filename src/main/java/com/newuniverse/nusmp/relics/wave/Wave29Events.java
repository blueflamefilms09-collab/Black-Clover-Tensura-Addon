package com.newuniverse.nusmp.relics.wave;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: the handlers of wave 29: the spider's bite and climb, the vampire's doubled holy damage and paused regeneration, the pack called by a wound, the
 * sleeper who wakes when hurt and cannot act while asleep, and the portal sweep. Per-player work runs once a second, staggered by player id; each damage
 * handler first checks the cheap things (a timer, a flag) before it reads the damage type.
 */
final class Wave29Events {
    private Wave29Events() {}

    // ---------------------------------------------------------------- damage
    static void onIncoming(LivingIncomingDamageEvent e) {
        float amount = e.getAmount();
        if (amount <= 0f) return;
        DamageSource src = e.getSource();
        LivingEntity victim = e.getEntity();
        Entity attacker = src.getEntity();
        // the spider's bite: a melee blow by the player themself
        if (attacker instanceof ServerPlayer a && src.getDirectEntity() == a && victim != a && Wave29Util.running(a, Wave29Brews.K_SPIDER)) {
            float extra = Wave29Brews.bite(a, victim);
            if (extra > 0f) {
                e.setAmount(amount + extra);
                amount = e.getAmount();
            }
        }
        // the vampire's weakness to holy light
        if (victim instanceof ServerPlayer p) {
            CompoundTag d = Wave29Util.peek(p);
            if (d == null) return;
            boolean cursed = d.getLong(Wave29Brews.K_CURSE) > p.level().getGameTime();
            if (!cursed && !d.getBoolean(Wave29Brews.K_REGEN)) return;
            String path = src.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse("");
            if (!Wave29Rules.isRadiantPath(path)) return;
            if (cursed) e.setAmount(amount * Wave29Rules.RADIANT_MULT);
            d.putLong(Wave29Brews.K_REGEN_HOLD, p.level().getGameTime() + Wave29Rules.REGEN_HOLD);
        }
    }

    /** Damage wakes a sleeper; a wound from a hostile creature calls the Children of the Night. */
    static void onDamagePost(LivingDamageEvent.Post e) {
        LivingEntity victim = e.getEntity();
        Wave29Brews.wake(victim);
        if (victim instanceof ServerPlayer p && e.getSource().getEntity() instanceof LivingEntity a) Wave29Brews.woundedBy(p, a);
    }

    /** A sleeping creature picks no target. */
    static void onChangeTarget(LivingChangeTargetEvent e) {
        if (e.getNewAboutToBeSetTarget() != null && Wave29Util.running(e.getEntity(), Wave29Brews.K_SLEEP)) e.setCanceled(true);
    }

    // ---------------------------------------------------------------- a sleeper does nothing
    static void onUseItem(PlayerInteractEvent.RightClickItem e) {
        if (e.getEntity() instanceof ServerPlayer p && Wave29Util.running(p, Wave29Brews.K_SLEEP)) e.setCanceled(true);
    }

    static void onUseBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getEntity() instanceof ServerPlayer p && Wave29Util.running(p, Wave29Brews.K_SLEEP)) e.setCanceled(true);
    }

    // ---------------------------------------------------------------- the player's second
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        if (p.horizontalCollision) Wave29Brews.climb(p);
        if ((p.tickCount + p.getId()) % 20 != 11) return;
        CompoundTag d = Wave29Util.peek(p);
        if (d == null) return;
        Wave29Brews.vampirePass(p, d);
        Wave29Brews.sleepPass(p, d);
        Wave29Brews.spiderPass(p, d);
    }

    // ---------------------------------------------------------------- life and death
    /** Death ends the curse, the sleep and the spider body; the Children of the Night and Regeneration are for good. */
    static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        CompoundTag d = Wave29Util.peek(p);
        if (d != null) {
            d.remove(Wave29Brews.K_CURSE);
            d.remove(Wave29Brews.K_SLEEP);
            d.remove(Wave29Brews.K_PACK_AT);
        }
        Wave29Brews.endSpider(p);
        Wave29Portal.forget(p);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Wave29Portal.forget(p);
    }

    static void onServerTick(ServerTickEvent.Post e) { Wave29Portal.tick(e.getServer()); }

    static void onStopping(ServerStoppingEvent e) { Wave29Portal.clear(); }
}
