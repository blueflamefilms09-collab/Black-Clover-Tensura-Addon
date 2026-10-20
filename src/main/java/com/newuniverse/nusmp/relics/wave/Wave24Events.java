package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: the long-lived parts of the wave-24 relics: the Ring of Power's extra force, the Lucky Egg's reroll, the Ring of Endless Rage (one crushed
 * ruby, a protected thirty seconds, three death saves after), the Ring of Regeneration's pulse, the Ring of Memories' trail and trance, the pot
 * that pings, the Dryad's hover, Lumpy's remarks, the badge's halted creatures. Per-player work runs once a second, staggered by player id, and
 * looks at the cheap saved timers first; every timer lives in the player's persistent data, so nothing survives a world change in memory.
 */
final class Wave24Events {
    private Wave24Events() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(Wave24Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave24Events::onPre);
        NeoForge.EVENT_BUS.addListener(Wave24Events::onPost);
        NeoForge.EVENT_BUS.addListener(Wave24Events::onHeal);
        NeoForge.EVENT_BUS.addListener(Wave24Events::onDeath);
        NeoForge.EVENT_BUS.addListener(Wave24Events::onDrops);
        NeoForge.EVENT_BUS.addListener(Wave24Events::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(Wave24Events::onPlayerTick);
    }

    private static long now(LivingEntity e) { return e.level().getGameTime(); }

    // ---------------------------------------------------------------- damage
    /** Ring of Power: every weapon attack deals 1d8 more force damage (at most 4 more on a player). */
    static void onIncoming(LivingIncomingDamageEvent e) {
        float amount = e.getAmount();
        if (amount <= 0f) return;
        DamageSource src = e.getSource();
        if (src.getEntity() instanceof ServerPlayer a && src.getDirectEntity() == a && e.getEntity() != a && RelicFlags.has(a, Wave24.F_POWER)) {
            float extra = Wave24Rules.forceBonus(Wave24Rules.d(Wave24Util.dice(a), 8), e.getEntity() instanceof net.minecraft.world.entity.player.Player);
            e.setAmount(amount + extra);
            if (a.level() instanceof ServerLevel l) com.newuniverse.nusmp.particle.fx.Fx.burst(l, FxThemes.RELIC_WAVE_24, e.getEntity(), FxTone.DARK, 2);
        }
    }

    /** What reaches a player after armour: the egg's reroll, the ruby that is crushed at the last blow, the guard of the rage. */
    static void onPre(LivingDamageEvent.Pre e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isCreative() || p.isSpectator()) return;
        float dmg = e.getNewDamage();
        if (dmg <= 0f || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        CompoundTag d = Wave24Util.peek(p);
        long now = now(p);
        if (d != null && d.getLong(Wave24Acts.K_EGG_UNTIL) > now && dmg >= Wave24Rules.EGG_MIN_HIT && RelicFlags.has(p, Wave24.F_EGG)) {
            float again = Wave24Rules.rerolled(dmg, p.getRandom().nextDouble());
            d.remove(Wave24Acts.K_EGG_UNTIL);
            e.setNewDamage(again);
            dmg = again;
            if (p.level() instanceof ServerLevel l) com.newuniverse.nusmp.particle.fx.Fx.burst(l, FxThemes.RELIC_WAVE_24, p, FxTone.LIGHT, 6);
            Wave24Util.bar(p, "Lucky Egg: the blow is rolled again (" + String.format(java.util.Locale.ROOT, "%.1f", again) + ").", ChatFormatting.AQUA);
        }
        if (d != null && Wave24Acts.raging(p)) {
            e.setNewDamage(Wave24Rules.rageGuard(p.getHealth() + p.getAbsorptionAmount(), dmg));
            return;
        }
        if (RelicFlags.has(p, Wave24.F_RAGE) && Wave24Rules.lethal(p.getHealth(), p.getAbsorptionAmount(), dmg)) startRage(p, e, dmg);
    }

    /** The last blow lands: the ruby is crushed (the ring is gone), the blow leaves one half heart, the rage begins. */
    private static void startRage(ServerPlayer p, LivingDamageEvent.Pre e, float dmg) {
        ItemStack ring = Wave24Util.stackOf(p, "ring_of_endless_rage");
        if (ring.isEmpty()) return;
        ring.shrink(1);
        Relics.invalidate(p);
        Wave24Util.data(p).putLong(Wave24Acts.K_RAGE_UNTIL, now(p) + Wave24Rules.RAGE_TICKS);
        e.setNewDamage(Wave24Rules.rageGuard(p.getHealth() + p.getAbsorptionAmount(), dmg));
        Wave24Util.give(p, "minecraft:strength", Wave24Rules.RAGE_TICKS, 1);
        Wave24Util.give(p, "minecraft:speed", Wave24Rules.RAGE_TICKS, 0);
        if (p.level() instanceof ServerLevel l) {
            com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_24, FxTone.DARK, p.getX(), p.getY() + 1.0, p.getZ(), 12, 0.6, 0.8, 0.6, 0.15);
            l.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.RAVAGER_ROAR, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.7f);
        }
        Wave24Util.say(p, "Ring of Endless Rage: you crush the ruby. You cannot die for 30 seconds, and nothing can heal you.", ChatFormatting.DARK_RED);
    }

    /** A raging bearer cannot be healed. */
    static void onHeal(LivingHealEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && Wave24Acts.raging(p)) e.setCanceled(true);
    }

    /** Lumpy's remark on a hard blow; a trance broken by pain. */
    static void onPost(LivingDamageEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        CompoundTag d = Wave24Util.peek(p);
        if (d != null && d.getLong(Wave24Acts.K_TRANCE_UNTIL) > 0 && e.getNewDamage() > 0f) Wave24Acts.endTrance(p, d, false);
        if (e.getNewDamage() >= 3.0f && RelicFlags.has(p, Wave24.F_LUMPY)) lumpySpeak(p, Wave24Rules.Mood.HURT);
    }

    static void onDeath(LivingDeathEvent e) {
        Entity killer = e.getSource().getEntity();
        if (killer instanceof ServerPlayer k && e.getEntity() instanceof Enemy && RelicFlags.has(k, Wave24.F_LUMPY)) lumpySpeak(k, Wave24Rules.Mood.KILL);
        if (e.getEntity() instanceof ServerPlayer p && RelicFlags.has(p, Wave24.F_LUMPY)) lumpySpeak(p, Wave24Rules.Mood.DEATH);
    }

    /** A creature Disintegrate killed leaves dust: no drops (the experience still falls). */
    static void onDrops(LivingDropsEvent e) {
        long at = e.getEntity().getPersistentData().getLong(Wave24Acts.K_DUST);
        if (at > 0 && now(e.getEntity()) - at <= 4) e.setCanceled(true);
    }

    /** The badge: a creature it halted does not turn on the bearer again until the time is up. */
    static void onChangeTarget(LivingChangeTargetEvent e) {
        if (e.getNewAboutToBeSetTarget() instanceof ServerPlayer tp && Wave24Acts.halted(e.getEntity(), tp)) e.setCanceled(true);
    }

    // ---------------------------------------------------------------- Lumpy
    static void lumpySpeak(ServerPlayer p, Wave24Rules.Mood mood) {
        CompoundTag d = Wave24Util.data(p);
        long now = now(p);
        if (!Wave24Rules.lumpyMayTalk(now, d.getLong(Wave24Acts.K_LUMPY_AT))) return;
        d.putLong(Wave24Acts.K_LUMPY_AT, now);
        Wave24Util.say(p, "Lumpy: \"" + Wave24Rules.lumpyLine(mood, p.getRandom().nextInt(64)) + "\"", ChatFormatting.AQUA);
    }

    // ---------------------------------------------------------------- the player's second
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive() || p.isSpectator()) return;
        if ((p.tickCount + p.getId()) % 20 != 5) return;
        long now = now(p);
        CompoundTag d = Wave24Util.peek(p);
        if (d != null) {
            Wave24Acts.finishCooking(p, d);
            if (d.getLong(Wave24Acts.K_RAGE_UNTIL) > 0 && d.getLong(Wave24Acts.K_RAGE_UNTIL) <= now) endRage(p, d);
            Wave24Acts.trancePass(p, d);
            Wave24Acts.dryadPass(p, d);
        }
        if (RelicFlags.has(p, Wave24.F_REGEN)) regenPass(p, now);
        if (RelicFlags.has(p, Wave24.F_MEMORIES) && !Wave24Acts.trancing(p)) Wave24Acts.sampleTrail(p, Wave24Util.data(p));
        if (RelicFlags.has(p, Wave24.F_DRYAD) && Wave24Acts.on(p, "orb_of_the_dryad", "light")) Wave24Util.give(p, "minecraft:night_vision", Wave24Rules.DRYAD_LIGHT_TICKS, 0);
        if (RelicFlags.has(p, Wave24.F_COMPASS) && Wave24Acts.on(p, "monster_compass", "lid")) Wave24Acts.readCompass(p);
        if (RelicFlags.has(p, Wave24.F_LUMPY) && Wave24Rules.lowHealth(p.getHealth(), p.getMaxHealth())) lumpySpeak(p, Wave24Rules.Mood.LOW);
    }

    // ---------------------------------------------------------------- Ring of Regeneration
    /** Every twenty seconds a hurt wearer regains 1d6 health; every five seconds each crippling effect is cut in half (the body regrows). Only while at least a half heart remains. */
    private static void regenPass(ServerPlayer p, long now) {
        if (p.getHealth() < 1.0f) return;
        CompoundTag d = Wave24Util.data(p);
        long last = d.getLong(Wave24Acts.K_REGEN_AT);
        if (p.getHealth() < p.getMaxHealth() && (now - last >= Wave24Rules.REGEN_EVERY_TICKS || now < last)) {
            d.putLong(Wave24Acts.K_REGEN_AT, now);
            Fx.heal(p, Wave24Rules.regenAmount(Wave24Rules.d(Wave24Util.dice(p), 6)));
            if (p.level() instanceof ServerLevel l) com.newuniverse.nusmp.particle.fx.Fx.aura(l, FxThemes.RELIC_WAVE_24, p, FxTone.LIGHT);
        }
        long lastRegrow = d.getLong(Wave24Acts.K_REGROW_AT);
        if (now - lastRegrow >= Wave24Rules.REGROW_EVERY_TICKS || now < lastRegrow) {
            d.putLong(Wave24Acts.K_REGROW_AT, now);
            for (String id : Wave24Rules.CRIPPLING) {
                var h = Wave24Util.effect(id);
                if (h.isEmpty()) continue;
                MobEffectInstance inst = p.getEffect(h.get());
                if (inst == null || inst.isInfiniteDuration()) continue;
                int left = Wave24Rules.regrown(inst.getDuration());
                p.removeEffect(h.get());
                if (left > 0) p.addEffect(new MobEffectInstance(h.get(), left, inst.getAmplifier(), inst.isAmbient(), inst.isVisible(), inst.showIcon()));
            }
        }
    }

    // ---------------------------------------------------------------- Ring of Endless Rage
    /** The rage ends: three death saves decide what you wake with. Nothing here kills; the worst result is half a heart and heavy weakness. */
    private static void endRage(ServerPlayer p, CompoundTag d) {
        d.remove(Wave24Acts.K_RAGE_UNTIL);
        if (!p.isAlive()) return;
        int ok = Wave24Rules.deathSaves(Wave24Util.dice(p));
        Wave24Rules.Aftermath a = Wave24Rules.aftermath(ok);
        float hp = (float) Math.max(1.0, p.getMaxHealth() * Wave24Rules.aftermathHealth(a));
        p.setHealth(Math.min(p.getMaxHealth(), Math.max(p.getHealth(), hp)));
        switch (a) {
            case STABLE_GOOD, STABLE -> Wave24Util.give(p, "minecraft:regeneration", 100, 0);
            case BARELY -> {
                Wave24Util.give(p, "minecraft:weakness", 300, 0);
                Wave24Util.give(p, "minecraft:slowness", 300, 0);
            }
            case EXHAUSTED -> {
                Wave24Util.give(p, "minecraft:weakness", 600, 1);
                Wave24Util.give(p, "minecraft:slowness", 600, 1);                          // 1.99.0, the hunger ban: the Hunger effect it also gave is gone
            }
        }
        Vec3 c = p.position();
        if (p.level() instanceof ServerLevel l) com.newuniverse.nusmp.particle.fx.Fx.burst(l, FxThemes.RELIC_WAVE_24, c.add(0, 1.0, 0), FxTone.DARK, 6);
        Wave24Util.say(p, "Ring of Endless Rage: the rage ends. Death saves: " + ok + " of 3 held. " + switch (a) {
            case STABLE_GOOD -> "You wake stable, bruised but whole.";
            case STABLE -> "You stabilise.";
            case BARELY -> "You cling on by a thread.";
            case EXHAUSTED -> "The ruby's price comes due: you are barely alive and spent.";
        }, ChatFormatting.DARK_RED);
    }
}
