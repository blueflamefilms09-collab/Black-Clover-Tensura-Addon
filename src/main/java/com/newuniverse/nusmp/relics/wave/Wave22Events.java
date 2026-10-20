package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicEngine;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: the abilities that are not a click (the sky's double and half damage of the Double Moon, the Lich Wand's no-food-no-sleep body, the wisps'
 * fighting, the flame's burning, the rainbow road) need a listener rather than an act.
 * <p>
 * Wave 22: the game-bus listeners of the staves and wands. Each starts with a cheap test (does the attacker or the healed carry the flag) and does its
 * real work only for the wielders; the player tick is staggered by entity id and nothing scans the world per tick. Everything that lasts (the wisps, the
 * ember flame, the faerie mark) lives in persistent data with an end time, so there is no static state to clean up.
 */
final class Wave22Events {
    private Wave22Events() {}

    private static boolean hooked;

    static void register() {
        if (hooked) return;
        hooked = true;
        NeoForge.EVENT_BUS.addListener(Wave22Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, Wave22Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave22Events::onHeal);
        NeoForge.EVENT_BUS.addListener(Wave22Events::onPost);
    }

    private static boolean has(LivingEntity e, String flag) { return RelicFlags.has(e, flag); }

    private static boolean due(ServerPlayer p, int period) { return (p.tickCount + p.getId()) % period == 0; }

    /** Day, night or twilight where the player stands. */
    static Wave22Rules.Sky skyOf(ServerPlayer p) {
        boolean none = p.level().dimensionType().hasFixedTime() || !p.level().dimensionType().hasSkyLight();
        return Wave22Rules.sky(none, p.level().getDayTime());
    }

    // ================================================================ damage in
    private static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        DamageSource src = e.getSource();
        // Wand-o-Wisp: a foe outlined in faerie fire takes 15% more from everything
        CompoundTag vd = victim.getPersistentData();
        if (vd.contains(Wave22Acts.K_FAERIE) && vd.getLong(Wave22Acts.K_FAERIE) > victim.level().getGameTime()) e.setAmount(e.getAmount() * Wave22Rules.FAERIE_BONUS);
        // Scepter of the Double Moon: the sky doubles or halves the element of the wielder's spells
        if (!Wave0Util.busy() && src.getEntity() instanceof ServerPlayer p && victim != p && !src.is(DamageTypes.PLAYER_ATTACK) && has(p, Wave22.F_MOON)) {
            String id = src.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse("");
            Wave22Rules.Element el = Wave22Rules.element(id);
            double scale = Wave22Rules.moonScale(skyOf(p), el);
            if (scale != 1.0) {
                float amount = Wave22Rules.moonScaled(e.getAmount(), scale);
                if (victim instanceof net.minecraft.world.entity.player.Player) amount = Wave22Rules.capVsPlayer(amount, victim.getMaxHealth());
                e.setAmount(amount);
            }
        }
    }

    private static void onHeal(LivingHealEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !has(p, Wave22.F_LICH)) return;
        e.setAmount(e.getAmount() * Wave22Rules.LICH_HEAL_BONUS);
        long now = p.level().getGameTime();
        CompoundTag d = Wave0Util.persisted(p);
        if (now - d.getLong("nusmp_w22_gaunt") >= Wave22Rules.LICH_GAUNT_GAP) {
            d.putLong("nusmp_w22_gaunt", now);
            Fx.addEffect(p, "minecraft:weakness", Wave22Rules.LICH_GAUNT_TICKS, 0);          // flesh regrows withered and gaunt
        }
    }

    /** Serpent Head Staff: with Venom Strike on, each melee hit costs a charge and a snake head lashes the nearest foe (or the one hit). */
    private static void onPost(LivingDamageEvent.Post e) {
        if (Wave0Util.busy()) return;
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer p) || !src.is(DamageTypes.PLAYER_ATTACK) || e.getEntity() == p) return;
        if (!has(p, Wave22.F_SERPENT) || !RelicEngine.isOn(p, "serpant_head_staff", "venom_strike")) return;
        ItemStack hand = p.getMainHandItem();
        RelicDef def = Relics.defOf(hand);
        if (def == null || !"serpant_head_staff".equals(def.id())) return;
        LivingEntity hit = e.getEntity();
        if (!Wave22Charges.spend(p, hand, Wave22Rules.Kind.SERPENT, 1, true)) return;
        LivingEntity bite = hit;
        double best = 4.5 * 4.5;
        for (LivingEntity t : Fx.foesAround(p, 4.5)) {
            if (t != hit && t.distanceToSqr(p) < best) {
                best = t.distanceToSqr(p);
                bite = t;
            }
        }
        if (!bite.isAlive()) return;
        Wave0Util.extra(p, bite, 3.5f, "minecraft:magic");
        Fx.addEffect(bite, "minecraft:poison", 100, 0);
        if (p.level() instanceof ServerLevel l) com.newuniverse.nusmp.particle.fx.Fx.stream(l, FxThemes.RELIC_WAVE_22, p.getEyePosition().add(p.getLookAngle().scale(0.8)), bite.getBoundingBox().getCenter(), FxTone.DARK, 4);
    }

    // ================================================================ the player tick
    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isSpectator() || !p.isAlive()) return;
        if (due(p, 10) && has(p, Wave22.F_BIFROST)) rainbow(p);
        if (!due(p, 20)) return;
        CompoundTag d = Wave0Util.persisted(p);
        long now = p.level().getGameTime();
        if (d.getLong(Wave22Acts.K_WISP_UNTIL) > now) wisps(p, d.getInt(Wave22Acts.K_WISPS));
        if (d.getLong(Wave22Acts.K_EMBER_UNTIL) > now) ember(p, Wave22Rules.Form.of(d.getInt(Wave22Acts.K_EMBER_FORM)));
        if (has(p, Wave22.F_LICH)) lich(p);
        if (has(p, Wave22.F_WIND)) windwheel(p, d, now);
        if (has(p, Wave22.F_SUPER)) {
            Fx.addEffect(p, "minecraft:night_vision", 260, 0);
            purgeSilence(p);
        }
    }

    private static void purgeSilence(ServerPlayer p) {
        for (String id : new String[]{"tensura:silence"}) {
            net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(id);
            if (rl != null) net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(rl).ifPresent(p::removeEffect);
        }
    }

    /** Bifrost: rainbows round the body while it is wielded; footprints on the ground while Rainbow Road is on (and a little speed for the walker). */
    private static void rainbow(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.aura(l, FxThemes.RELIC_WAVE_22, p, FxTone.LIGHT);
        if (RelicEngine.isOn(p, "the_bifrost", "rainbow_road")) {
            if (p.onGround() && p.getDeltaMovement().horizontalDistanceSqr() > 0.004) {
                Vec3 at = p.position();
                com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_22, FxTone.LIGHT, at.x, at.y + 0.05, at.z, 3, 0.25, 0.0, 0.25, 0.0);
                com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_22, FxTone.DARK, at.x, at.y + 0.05, at.z, 1, 0.2, 0.0, 0.2, 0.0);
            }
            Fx.addEffect(p, "minecraft:speed", 40, 0);
        }
    }

    /** The wisps: each second every wisp strikes one foe (the nearest ones, one each) within 12 blocks. */
    private static void wisps(ServerPlayer p, int count) {
        List<LivingEntity> foes = Fx.foesAround(p, 12.0);
        ServerLevel l = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.aura(l, FxThemes.RELIC_WAVE_22, p, FxTone.LIGHT);
        for (int i = 0; i < Math.min(count, Wave22Rules.WISP_MAX) && !foes.isEmpty(); i++) {
            LivingEntity t = foes.get(Math.min(i, foes.size() - 1));
            Vec3 from = p.position().add(Math.cos(i * Math.PI) * 1.2, 2.0, Math.sin(i * Math.PI) * 1.2);
            com.newuniverse.nusmp.particle.fx.Fx.stream(l, FxThemes.RELIC_WAVE_22, from, t.getBoundingBox().getCenter(), FxTone.LIGHT, 4);
            Fx.damage(p, t, Wave22Rules.WISP_DAMAGE, "minecraft:magic");
        }
    }

    /** The ember flame: it burns what is near it (humanoid), ranges wider and quickens you (beast), or lights your foes (hummingbird). */
    private static void ember(ServerPlayer p, Wave22Rules.Form f) {
        ServerLevel l = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.aura(l, FxThemes.RELIC_WAVE_22, p, FxTone.LIGHT);
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_22, FxTone.LIGHT, p.getX(), p.getY() + 2.2, p.getZ(), 2, 0.3, 0.1, 0.3, 0.02);
        List<LivingEntity> foes = Fx.foesAround(p, f.radius());
        if (f == Wave22Rules.Form.BIRD) {
            for (LivingEntity t : foes) Fx.addEffect(t, "minecraft:glowing", 60, 0);
            Fx.addEffect(p, "minecraft:speed", 60, 0);
            return;
        }
        if (f == Wave22Rules.Form.BEAST) Fx.addEffect(p, "minecraft:speed", 60, 0);
        for (int i = 0; i < Math.min(2, foes.size()); i++) {
            LivingEntity t = foes.get(i);
            Fx.damage(p, t, f.damage(), "minecraft:on_fire");
            t.setRemainingFireTicks(40);
        }
    }

    /** Lich Wand: no air, no hunger, no sleep. Water breathing is a passive; here the belly never empties below a floor and the night never catches up. */
    private static void lich(ServerPlayer p) {
        if (p.getFoodData().getFoodLevel() < Wave22Rules.LICH_FOOD_FLOOR) p.getFoodData().setFoodLevel(Wave22Rules.LICH_FOOD_FLOOR);
        p.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));                    // phantoms never come for a sleepless wielder
        if (due(p, 80)) com.newuniverse.nusmp.particle.fx.Fx.aura(p.serverLevel(), FxThemes.RELIC_WAVE_22, p, FxTone.DARK);                      // eyes of pale fire
    }

    /**
     * Windwheel Staff: every spell the wielder casts (a drop of their magicule pool of at least 1% of the maximum between two checks) stores charges
     * equal to its level; the charges last a minute from the last one gained.
     */
    private static void windwheel(ServerPlayer p, CompoundTag d, long now) {
        var ex = io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(p);
        if (ex == null) return;
        double max = io.github.manasmods.tensura.util.EnergyHelper.getMaxMagicule(p);
        double cur = ex.getMagicule();
        double prev = d.contains(Wave22Acts.K_WIND_PREV) ? d.getDouble(Wave22Acts.K_WIND_PREV) : cur;
        d.putDouble(Wave22Acts.K_WIND_PREV, cur);
        if (!Wave22Rules.windAlive(now, d.getLong(Wave22Acts.K_WIND_UNTIL))) d.putInt(Wave22Acts.K_WIND, 0);
        if (max <= 0) return;
        int stored = Wave22Rules.windStored((prev - cur) / max);
        if (stored <= 0) return;
        int total = Wave22Rules.windTotal(d.getInt(Wave22Acts.K_WIND), stored);
        d.putInt(Wave22Acts.K_WIND, total);
        d.putLong(Wave22Acts.K_WIND_UNTIL, now + Wave22Rules.WIND_TICKS);
        com.newuniverse.nusmp.particle.fx.Fx.gather(p.serverLevel(), FxThemes.RELIC_WAVE_22, p, FxTone.LIGHT, 4 + Math.min(8, total));
        Wave0Util.note(p, "The wind wheel spins: " + total + " charge(s).", net.minecraft.ChatFormatting.YELLOW);
    }
}
