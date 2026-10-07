package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Bronze Magic: the spells of BronzeBook that need their own logic (the lizard, the cannonballs, the bicycle, the statues, molten bronze). */
public final class BronzeArts {
    private BronzeArts() {}

    private static final int COLOR = BronzeBook.COLOR;
    private static final String GUARD = "nusmp_bronze_guard", GUARD_F = "nusmp_bronze_guard_f", GUARD_T = "nusmp_bronze_guard_t",
            NOFALL = "nusmp_bronze_nofall";
    private static boolean hooked;

    /** Registers the damage hook (bronze armour softens blows and cuts attackers, the fall is cushioned). Called once from BronzeProps.init(). */
    public static synchronized void hooks() {
        if (hooked) return;
        hooked = true;
        AttributeEvents.incoming(e -> {
            LivingEntity victim = e.getEntity();
            var data = victim.getPersistentData();
            long now = victim.level().getGameTime();
            if (e.getSource().is(DamageTypeTags.IS_FALL) && data.getLong(NOFALL) > now) { e.setCanceled(true); return; }
            if (data.getLong(GUARD) <= now || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
            e.setAmount(e.getAmount() * (1f - data.getFloat(GUARD_F)));
            if (e.getSource().getDirectEntity() instanceof LivingEntity att && att != victim && !e.getSource().is(DamageTypes.THORNS)
                    && data.getFloat(GUARD_T) > 0) att.hurt(victim.damageSources().thorns(victim), data.getFloat(GUARD_T));
        });
    }

    // ------------------------------------------------------------------ small helpers
    private static float k(ManasSkillInstance i, ServerPlayer p) {
        return GrimoireBook.size(i, p) * EnergyBridge.scale(p);
    }

    /** Bronze armour: 'cut' of every blow is softened and attackers take 'thorns' for the duration. */
    static void guard(LivingEntity t, int ticks, float cut, float thorns) {
        var d = t.getPersistentData();
        d.putLong(GUARD, t.level().getGameTime() + ticks);
        d.putFloat(GUARD_F, cut);
        d.putFloat(GUARD_T, thorns);
    }

    static void push(LivingEntity t, Vec3 from, double power, double up) {
        Vec3 out = t.position().subtract(from).multiply(1, 0, 1);
        out = out.lengthSqr() < 1e-4 ? Vec3.ZERO : out.normalize();
        double s = BalanceLaw.isBoss(t) ? 0.25 : 1.0;
        t.setDeltaMovement(t.getDeltaMovement().add(out.scale(power * s)).add(0, up * s, 0));
        t.hurtMarked = true;
    }

    private static void slow(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.isBoss(t) ? ticks / 3 : ticks, amp));
    }

    // ------------------------------------------------------------------ Verdigris Spit: tarnish eats into the flesh
    static final ElementBook.Rider VERDIGRIS = (t, p) -> {
        boolean boss = BalanceLaw.isBoss(t);
        t.addEffect(new MobEffectInstance(MobEffects.POISON, boss ? 50 : 100, 1));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, boss ? 50 : 120, 0));
        EnergyBridge.effect(t, "corrosion", boss ? 40 : 100, 0);
    };

    // ------------------------------------------------------------------ Shrapnel: bronze splinters that shove their target back
    static final ElementBook.Rider SHRAPNEL = (t, p) -> {
        push(t, p.position(), 0.45, 0.12);
        com.newuniverse.nusmp.antimagic.Nullification.bleed(t, 0.01);
    };

    // ------------------------------------------------------------------ Sekke Poison Lizard: a bronze lizard crawls to the target
    static boolean lizard(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 28);
        if (t == null) { GrimoireBook.fail(p, "No one for the lizard to hunt."); return false; }
        float s = k(i, p);
        Vec3 start = p.getEyePosition().add(p.getViewVector(1f).scale(0.8)).add(0, -0.5, 0);
        b.castCircle(p, 0.9f);
        b.vfx(p, VfxShape.BRONZE_FX1, start, t.getBoundingBox().getCenter(), 30, 1.0f);
        SpellRuntime.bolt(p, start, t.getBoundingBox().getCenter().subtract(start).normalize().scale(0.75), 0.8, 40, false, t, (bolt, hit) -> {
            b.hurtAs(i, p, hit, mode, 9f * s, TensuraDamageTypes.CURSE);
            boolean boss = BalanceLaw.isBoss(hit);
            hit.addEffect(new MobEffectInstance(MobEffects.POISON, boss ? 80 : 160, 1));
            hit.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, boss ? 60 : 160, 1));
            EnergyBridge.effect(hit, "fatal_poison", boss ? 40 : 100, 0);
            slow(hit, 60, 1);
        }, (bolt, at) -> b.vfx(p, VfxShape.BRONZE_FX3, at, at.add(0, 1, 0), 22, 0.9f));
        return true;
    }

    // ------------------------------------------------------------------ Sekke Magnum Cannonball: a spiked bronze sphere around the caster
    static boolean cannonball(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        double r = 3.5 * s;
        int ticks = 120;
        Vec3 c = p.position();
        b.castCircle(p, 1.1f);
        b.vfx(p, VfxShape.BRONZE_FX3, c.add(0, 1, 0), c.add(0, 2, 0), 26, 1.0f * s);
        for (LivingEntity t : GrimoireBook.around(p, c, r + 1)) {
            b.hurtAs(i, p, t, mode, 6f, TensuraDamageTypes.EARTH_ELEMENTAL);
            push(t, c, 1.0, 0.3);
        }
        guard(p, ticks, 0.35f, 3f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BRONZE_FX2, p, p.position(), COLOR, ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            if (!p.isAlive()) return;
            for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
                b.hurtAs(i, p, t, mode, 2.5f, TensuraDamageTypes.EARTH_ELEMENTAL);
                push(t, p.position(), 0.35, 0.05);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Super Sekke Magnum Cannonball: the caster is the cannonball
    static boolean superCannonball(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 look = p.getViewVector(1f), flat = look.multiply(1, 0, 1);
        flat = flat.lengthSqr() < 1e-4 ? Vec3.ZERO : flat.normalize();
        Vec3 from = p.position();
        p.setDeltaMovement(flat.scale(2.1 * s).add(0, 0.28, 0));
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.getPersistentData().putLong(NOFALL, p.level().getGameTime() + 160);
        guard(p, 40, 0.6f, 0f);
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.BRONZE_FX3, from.add(0, 1, 0), from.add(flat.scale(4)).add(0, 1, 0), 24, 1.2f);
        b.vfx(p, VfxShape.BRONZE_FX1, from.add(0, 1, 0), from.add(flat.scale(14)).add(0, 1, 0), 14, 1.3f);
        Set<LivingEntity> rammed = new HashSet<>();
        SpellRuntime.zone(p.serverLevel(), 16, 1, age -> {
            if (!p.isAlive()) return;
            for (LivingEntity t : GrimoireBook.around(p, p.position().add(0, 1, 0), 2.6)) {
                if (!rammed.add(t) || rammed.size() > 12) continue;
                b.hurtAs(i, p, t, mode, 14f, TensuraDamageTypes.EARTH_ELEMENTAL);
                push(t, p.position(), 0.9, 0.35);
                slow(t, 50, 2);
            }
        });
        SpellRuntime.later(p.serverLevel(), 16, () -> {
            if (!p.isAlive()) return;
            Vec3 at = p.position();
            for (LivingEntity t : GrimoireBook.around(p, at, 4.5 * s)) {
                b.hurtAs(i, p, t, mode, 12f, TensuraDamageTypes.EARTH_ELEMENTAL);
                push(t, at, 0.8, 0.5);
            }
            b.vfx(p, VfxShape.BRONZE_FX2, at, at.add(0, 1, 0), 30, (float) (4.5 * s));
            b.vfx(p, VfxShape.BRONZE_FX3, at.add(0, 0.5, 0), at.add(0, 1.5, 0), 30, 1.5f * s);
        });
        return true;
    }

    // ------------------------------------------------------------------ Sekke Shooting Star: the winged bronze bicycle
    static boolean shootingStar(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 300;
        long now = p.level().getGameTime();
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 2));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 1));
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks, 0));
        p.getPersistentData().putLong(NOFALL, now + ticks + 40);
        guard(p, ticks, 0.2f, 0f);
        b.castCircle(p, 1.2f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BRONZE_FX3, p, p.position().add(0, 1, 0), COLOR, 40, 1.2f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BRONZE_FX1, p, p.position().add(0, 1, 0), COLOR, ticks, 0.8f);
        Map<LivingEntity, Long> last = new HashMap<>();
        SpellRuntime.zone(p.serverLevel(), ticks, 3, age -> {
            if (!p.isAlive()) return;
            Vec3 v = p.getDeltaMovement();
            if (v.horizontalDistanceSqr() < 0.04) return;
            long t0 = p.level().getGameTime();
            for (LivingEntity t : GrimoireBook.around(p, p.position().add(0, 0.9, 0), 1.9)) {
                if (last.size() > 24 || t0 - last.getOrDefault(t, -100L) < 20) continue;
                last.put(t, t0);
                b.hurtAs(i, p, t, mode, 6f, TensuraDamageTypes.EARTH_ELEMENTAL);
                push(t, p.position(), 0.8, 0.3);
                b.vfx(p, VfxShape.BRONZE_FX3, t.position().add(0, 1, 0), p.position().add(0, 1, 0), 14, 0.6f);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Bronze Statue: the mage turns to bronze
    static boolean statue(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 200;
        guard(p, ticks, 0.5f, 2f);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 1));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 0));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BRONZE_FX3, p, p.position().add(0, 1, 0), COLOR, 40, 1.1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BRONZE_FX2, p, p.position(), COLOR, ticks, 1.5f);
        return true;
    }

    // ------------------------------------------------------------------ Statue Garrison: three bronze statues guard the spot
    static boolean sentinels(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 c = GrimoireBook.aim(p, 20);
        int ticks = 240;
        Vec3[] pts = new Vec3[3];
        double base = p.getRandom().nextDouble() * Math.PI * 2;
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.BRONZE_FX2, c, c.add(0, 1, 0), ticks, (float) (6 * s));
        for (int m = 0; m < 3; m++) {
            double a = base + m * Math.PI * 2 / 3;
            pts[m] = c.add(Math.cos(a) * 2.4, 0, Math.sin(a) * 2.4);
            b.vfx(p, VfxShape.BRONZE_FX3, pts[m], pts[m].add(0, 2, 0), 26, 0.8f);
        }
        SpellRuntime.zone(p.serverLevel(), ticks, 12, age -> {
            for (Vec3 at : pts) {
                LivingEntity best = null;
                double bd = Double.MAX_VALUE;
                for (LivingEntity t : GrimoireBook.around(p, at, 6 * s)) {
                    double d = t.distanceToSqr(at);
                    if (d < bd) { bd = d; best = t; }
                }
                if (best == null) continue;
                b.hurtAs(i, p, best, mode, 4.5f, TensuraDamageTypes.EARTH_ELEMENTAL);
                slow(best, 30, 1);
                b.vfx(p, VfxShape.BRONZE_FX1, at.add(0, 1.6, 0), best.getBoundingBox().getCenter(), 8, 0.6f);
                for (LivingEntity t : GrimoireBook.around(p, at, 1.4)) push(t, at, 0.4, 0.05);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Molten Bronze: a pool of liquid metal
    static boolean molten(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 c = GrimoireBook.aim(p, 20);
        double r = 4 * s;
        int ticks = 140;
        b.castCircle(p, 1.1f);
        b.vfx(p, VfxShape.BRONZE_FX2, c, c.add(0, 1, 0), ticks, (float) r);
        b.vfx(p, VfxShape.BRONZE_FX3, c, c.add(0, 1, 0), 24, 1.1f);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                b.hurtAs(i, p, t, mode, 3f, TensuraDamageTypes.FIRE_ELEMENTAL);
                t.igniteForSeconds(3);
                slow(t, 30, 2);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Bronze Colossus (daily): a towering bronze giant
    static boolean colossus(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        int ticks = 600;
        Vec3 c = p.position();
        guard(p, ticks, 0.55f, 4f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 2));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 0));
        for (LivingEntity t : GrimoireBook.around(p, c, 5 * s)) {
            b.hurtAs(i, p, t, mode, 10f, TensuraDamageTypes.EARTH_ELEMENTAL);
            push(t, c, 1.1, 0.5);
            slow(t, 60, 1);
        }
        b.castCircle(p, 1.6f);
        b.vfx(p, VfxShape.BRONZE_FX2, c, c.add(0, 1, 0), 50, (float) (5 * s));
        b.vfx(p, VfxShape.BRONZE_FX3, c.add(0, 1, 0), c.add(0, 2, 0), 34, 1.6f * s);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BRONZE_FX3, p, p.position().add(0, 1, 0), COLOR, 60, 1.4f);
        return true;
    }
}
