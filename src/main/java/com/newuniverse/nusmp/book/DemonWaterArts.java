package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Demon Water Magic: the spells that need more than a shared shape (drowning, whirlpool, healing mist, the Hydra, rebirth). */
final class DemonWaterArts {
    private DemonWaterArts() {}

    /** Spiritual damage on top of a hit (bypasses armour; bosses are spared). */
    static ElementBook.Rider spirit(double amount) {
        return (t, p) -> { if (!BalanceLaw.isBoss(t)) EnergyBridge.spirit(t, amount); };
    }

    /** Slowness as a hit rider. */
    static ElementBook.Rider slow(int ticks, int amp) {
        return ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp));
    }

    /** Dash through the water: a pushing hit along the path, then water breathing and dolphin speed for a while. */
    static BookPage.Cast surge() {
        BookPage.Cast dash = ElementBook.dash(6, 9, false, VfxShape.DEMON_WATER_FX1, ElementBook.all(ElementBook.knock(0.8), slow(40, 1)));
        return (b, i, p, mode) -> {
            if (!dash.cast(b, i, p, mode)) return false;
            p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 400, 0));
            p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 120, 0));
            b.vfx(p, VfxShape.DEMON_WATER_FX3, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 24, 1f);
            return true;
        };
    }

    /** Drowning Grasp: a water lung fills the target; slowed, weakened, its air drained, hurt every half second. */
    static boolean drowningGrasp(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "No one to drown."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 80);
        float dmg = 3f * (float) EnergyBridge.scale(p);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 4));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks + 40, 1));
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.DEMON_WATER_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), 14, 1f);
        b.vfx(p, VfxShape.DEMON_WATER_FX2, t.position(), t.position(), ticks, 1.6f);
        b.hurt(i, p, t, mode, dmg);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            if (!t.isAlive()) return;
            t.setAirSupply(Math.max(-20, t.getAirSupply() - 100));
            b.hurt(i, p, t, mode, dmg);
            if (!BalanceLaw.isBoss(t)) EnergyBridge.spirit(t, 1.0);
            b.vfx(p, VfxShape.DEMON_WATER_FX3, t.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), 14, 0.5f);
        });
        return true;
    }

    /** Healing Mist: a cool teal fog heals you and your allies, washes out poison and slows, and gives regeneration. */
    static boolean healingMist(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 8 * GrimoireBook.size(i, p);
        float frac = 0.15f * (float) EnergyBridge.scale(p);
        List<ServerPlayer> who = new ArrayList<>(p.serverLevel().getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(r),
                a -> a != p && a.isAlive() && a.isAlliedTo(p)));
        who.add(p);
        for (ServerPlayer a : who) {
            BalanceLaw.heal(a, a.getMaxHealth() * frac);
            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            a.removeEffect(MobEffects.POISON);
            a.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            a.clearFire();
        }
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.DEMON_WATER_FX2, p.position(), p.position(), 70, (float) r);
        return true;
    }

    /** Abyssal Whirlpool: a black whirlpool at the aim point drags everything in, slows it and tears at it. */
    static boolean whirlpool(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 24);
        double r = 5 * GrimoireBook.size(i, p);
        int ticks = 100;
        float dmg = 3f * (float) EnergyBridge.scale(p);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.DEMON_WATER_FX2, c, c, ticks, (float) r);
        int[] n = {0};
        SpellRuntime.zone(p.serverLevel(), ticks, 4, age -> {
            n[0]++;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                Vec3 pull = c.subtract(t.position());
                double d = Math.max(0.5, pull.length());
                double k = BalanceLaw.isBoss(t) ? 0.04 : 0.14;
                Vec3 spin = new Vec3(-pull.z, 0, pull.x).normalize().scale(0.05);
                t.setDeltaMovement(t.getDeltaMovement().scale(0.6).add(pull.scale(k / d)).add(spin));
                t.hurtMarked = true;
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
                if (n[0] % 5 == 0) b.hurt(i, p, t, mode, dmg);
            }
        });
        return true;
    }

    /** Hydra of Darkneros: a water sphere around you, serpents of black water burst from it and crush what they find. */
    static boolean hydra(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float sz = GrimoireBook.size(i, p);
        int heads = 3 + EnergyBridge.modifier(p) / 2;
        float dmg = 7f * (float) EnergyBridge.scale(p);
        Vec3 centre = p.position().add(0, 1, 0);
        List<LivingEntity> foes = GrimoireBook.around(p, p.position(), 18);
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.DEMON_WATER_FX2, p.position(), p.position(), 50, 3f * sz);
        for (int k = 0; k < heads; k++) {
            int head = k;
            SpellRuntime.later(p.serverLevel(), 6 + k * 4, () -> {
                if (!p.isAlive()) return;
                double a = head * Math.PI * 2 / heads;
                Vec3 start = p.position().add(Math.cos(a) * 2.5, 1.2 + 0.4 * (head % 2), Math.sin(a) * 2.5);
                LivingEntity home = foes.isEmpty() ? null : foes.get(head % foes.size());
                Vec3 dir = home != null ? home.getBoundingBox().getCenter().subtract(start).normalize()
                        : new Vec3(Math.cos(a), 0.1, Math.sin(a)).add(p.getViewVector(1f)).normalize();
                b.vfx(p, VfxShape.DEMON_WATER_FX1, start, start.add(dir.scale(30)), 22, 1f * sz);
                SpellRuntime.bolt(p, start, dir.scale(1.3), 0.9 * sz, 24, false, home, (bolt, t) -> {
                    b.hurt(i, p, t, mode, dmg);
                    if (!BalanceLaw.isBoss(t)) EnergyBridge.spirit(t, 3.0);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
                    t.setAirSupply(Math.max(-20, t.getAirSupply() - 150));
                }, (bolt, at) -> b.vfx(p, VfxShape.DEMON_WATER_FX3, at, at, 22, 1f));
            });
        }
        return true;
    }

    /** Tide Rebirth: the water takes every ailment away and closes your wounds; allies close by are mended too. */
    static boolean rebirth(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float sc = (float) EnergyBridge.scale(p);
        List<ServerPlayer> who = new ArrayList<>(p.serverLevel().getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(6),
                a -> a != p && a.isAlive() && a.isAlliedTo(p)));
        who.add(p);
        for (ServerPlayer a : who) {
            for (MobEffectInstance e : new ArrayList<>(a.getActiveEffects()))
                if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) a.removeEffect(e.getEffect());
            BalanceLaw.heal(a, a.getMaxHealth() * (a == p ? 0.5f : 0.25f) * Math.min(1.2f, sc));
            a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1));
            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            a.clearFire();
        }
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.DEMON_WATER_FX3, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 40, 1.5f);
        b.vfx(p, VfxShape.DEMON_WATER_FX2, p.position(), p.position(), 50, 4f);
        return true;
    }
}
