package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Corundum Magic: the spells of CorundumBook that need their own logic (fracture, the bastion, the Ideal Closer armour, the star gem). */
public final class CorundumArts {
    private CorundumArts() {}

    private static float k(ManasSkillInstance i, ServerPlayer p) {
        return GrimoireBook.size(i, p) * EnergyBridge.scale(p);
    }

    /** Hardness on a target: Slowness for 'ticks'. */
    static void weigh(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp));
    }

    /** Cracks the target's defence: Weakness and the Tensura fragility (shortened on bosses). */
    static void crack(LivingEntity t, int ticks) {
        int dur = BalanceLaw.isBoss(t) ? Math.min(ticks, 60) : ticks;
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 1));
        EnergyBridge.effect(t, "fragility", dur, 0);
    }

    // ------------------------------------------------------------------ Corundum Fracture: a hard shard that cracks the defence
    static boolean fracture(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.CORUNDUM_FX1, start, start.add(dir.scale(24)), 14, 1.1f);
        SpellRuntime.bolt(p, start, dir.scale(1.8), 0.55 * s, 14, false, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 9f);
            crack(t, 160);
            Nullification.bleed(t, 0.03);
        }, (bolt, at) -> b.vfx(p, VfxShape.CORUNDUM_FX3, at, at, 20, 0.9f));
        return true;
    }

    // ------------------------------------------------------------------ Gem Wall: a wall of corundum with a flash
    static boolean gemWall(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!ElementBook.wall(Blocks.RED_CONCRETE.defaultBlockState()).cast(b, i, p, mode)) return false;
        Vec3 at = p.position().add(p.getViewVector(1f).multiply(1, 0, 1).normalize().scale(2.5));
        b.vfx(p, VfxShape.CORUNDUM_FX3, at, at.add(0, 1, 0), 24, 1.2f);
        return true;
    }

    // ------------------------------------------------------------------ Corundum Bastion: a fortress zone
    static boolean bastion(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        double r = 5.0 * s;
        int ticks = 200;
        Vec3 c = GrimoireBook.aim(p, 20);
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.CORUNDUM_FX2, c, c.add(0, 1, 0), ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 20, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                b.hurt(i, p, t, mode, 3.5f);
                weigh(t, 40, BalanceLaw.isBoss(t) ? 1 : 3);
                t.setSprinting(false);
            }
            for (LivingEntity f : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(r),
                    e -> e.isAlive() && (e == p || e.isAlliedTo(p)) && e.distanceToSqr(c) <= r * r))
                f.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1));
            if (age % 60 == 0) b.vfx(p, VfxShape.CORUNDUM_FX3, c, c.add(0, 1, 0), 20, (float) Math.max(1.0, r / 4));
        });
        return true;
    }

    // ------------------------------------------------------------------ Ideal Closer: the corundum armour with limbs
    static boolean idealCloser(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        int ticks = 400;
        double reach = 3.6 * s;
        b.castCircle(p, 1.4f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 2));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 3));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 1));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 0));
        var tag = i.getOrCreateTag();
        tag.putLong("EmpowerUntil", p.level().getGameTime() + ticks);
        tag.putFloat("EmpowerBonus", 5f);
        tag.putLong("ThornsUntil", p.level().getGameTime() + ticks);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.CORUNDUM_FX3, p, p.position().add(0, 1, 0), b.color, 40, 1.4f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.CORUNDUM_FX2, p, p.position(), b.color, ticks, (float) reach);
        SpellRuntime.zone(p.serverLevel(), ticks, 20, age -> {
            if (!p.isAlive()) return;
            boolean hit = false;
            for (LivingEntity t : GrimoireBook.around(p, p.position(), reach)) {
                b.hurt(i, p, t, mode, 6f);
                Vec3 d = t.position().subtract(p.position());
                if (d.lengthSqr() > 1e-4) t.knockback(0.6, -d.x, -d.z);
                hit = true;
            }
            if (hit) b.vfx(p, VfxShape.CORUNDUM_FX3, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 16, 0.8f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Star Corundum: the star gem mends the party
    static boolean starCorundum(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        double r = 8 * s;
        b.castCircle(p, 1.2f);
        Vec3 c = p.position();
        b.vfx(p, VfxShape.CORUNDUM_FX2, c, c.add(0, 1, 0), 60, (float) r);
        for (LivingEntity f : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(r),
                e -> e.isAlive() && (e == p || e.isAlliedTo(p)) && e.distanceToSqr(c) <= r * r)) {
            BalanceLaw.heal(f, f.getMaxHealth() * 0.3f);
            f.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            f.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1));
            f.removeEffect(MobEffects.WEAKNESS);
        }
        b.vfx(p, VfxShape.CORUNDUM_FX3, c, c.add(0, 1, 0), 28, 1.3f);
        return true;
    }
}
