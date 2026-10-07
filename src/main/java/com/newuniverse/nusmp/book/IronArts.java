package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/** Iron Magic: bullets, falling blades, shackles, the guardian bell, rust and the iron god of war. */
final class IronArts {
    private IronArts() {}

    private static final VfxShape CAST = VfxShape.IRON_FX1, ZONE = VfxShape.IRON_FX2, BURST = VfxShape.IRON_FX3;

    private static Vec3 hand(ServerPlayer p) { return p.getEyePosition().add(0, -0.35, 0); }
    private static Vec3 mid(LivingEntity t) { return t.getBoundingBox().getCenter(); }

    static void slow(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp));
    }

    /** Rider: the hit target is weighed down with iron (Slowness and Burden). */
    static ElementBook.Rider heavy(int ticks, int amp) {
        return (t, p) -> { slow(t, ticks, amp); EnergyBridge.effect(t, "burden", ticks, 0); };
    }

    /** Rider for the shrapnel: the splinters bleed magicules and slow. */
    static ElementBook.Rider splinters() { return (t, p) -> { Nullification.bleed(t, 0.015); slow(t, 40, 1); }; }

    /** Iron Sword Rain: iron swords fall one after another around the aim point and pin whatever they hit. */
    static boolean swordRain(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 22);
        double r = 4 * GrimoireBook.size(i, p);
        Random rnd = new Random(p.level().getGameTime());
        b.castCircle(p, 1f);
        b.vfx(p, ZONE, c, c.add(0, 1, 0), 40, (float) r);
        for (int n = 0; n < 8; n++) {
            double a = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * r;
            Vec3 at = c.add(Math.cos(a) * d, 0, Math.sin(a) * d);
            SpellRuntime.later(p.serverLevel(), 2 + n * 3, () -> {
                b.vfx(p, CAST, at.add(0, 9, 0), at, 6, 0.8f);
                SpellRuntime.later(p.serverLevel(), 5, () -> {
                    for (LivingEntity t : GrimoireBook.around(p, at, 1.8)) {
                        b.hurt(i, p, t, mode, 6f);
                        slow(t, 40, 1);
                    }
                    b.vfx(p, BURST, at, at, 14, 0.5f);
                });
            });
        }
        return true;
    }

    /** Iron Shackles: iron cuffs close on the target's limbs and weigh it down. */
    static boolean shackles(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "Nothing to shackle."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 90);
        b.castCircle(p, 0.8f);
        b.vfx(p, CAST, hand(p), mid(t), 8, 0.9f);
        b.vfx(p, ZONE, t.position(), t.position().add(0, 1, 0), ticks, (float) Math.max(1.4, t.getBbWidth() + 1));
        b.hurt(i, p, t, mode, 5f);
        slow(t, ticks, 9);
        EnergyBridge.effect(t, "burden", ticks, 1);
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0));
        SpellRuntime.zone(p.serverLevel(), ticks, 4, age -> {
            if (!t.isAlive()) return;
            t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
            t.hurtMarked = true;
        });
        return true;
    }

    /** Iron Wall: a wall of iron blocks that vanishes after 8 seconds. */
    static boolean wall(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1).normalize(), side = new Vec3(-dir.z, 0, dir.x);
        Vec3 base = p.position().add(dir.scale(2.5));
        int placed = 0;
        for (int w = -2; w < 2; w++) for (int h = 0; h < 3; h++)
            if (SpellRuntime.tempBlock(p.serverLevel(), BlockPos.containing(base.add(side.scale(w + 0.5)).add(0, h, 0)), Blocks.IRON_BLOCK.defaultBlockState(), 160)) placed++;
        if (placed == 0) { GrimoireBook.fail(p, "No room for a wall."); return false; }
        b.castCircle(p, 0.8f);
        b.vfx(p, ZONE, base, base.add(0, 1, 0), 40, 2.5f);
        b.vfx(p, BURST, base.add(0, 1, 0), base.add(dir), 18, 1f);
        return true;
    }

    /** Immovable Guardian Deity: a great bell of iron closes over the caster and allies; enemy shots break on it, foes are thrown off it. */
    static boolean guardian(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 3.5 * GrimoireBook.size(i, p);
        int ticks = 160;
        b.castCircle(p, 1.4f);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 1));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 0));
        VfxSpawn.sendFollowing(p.serverLevel(), ZONE, p, p.position(), b.color, ticks, (float) r);
        b.vfx(p, BURST, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 24, 1.2f);
        SpellRuntime.zone(p.serverLevel(), ticks, 5, age -> {
            if (!p.isAlive()) return;
            for (Player a : p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(r), x -> x == p || x.isAlliedTo(p)))
                a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 2));
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(r))) {
                if (pr.getOwner() == p || (pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p))) continue;
                pr.discard();
            }
            for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
                Vec3 away = t.position().subtract(p.position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1e-4) {
                    Vec3 v = away.normalize().scale(BalanceLaw.isBoss(t) ? 0.15 : 0.5);
                    t.setDeltaMovement(v.x, t.getDeltaMovement().y, v.z);
                    t.hurtMarked = true;
                }
                if (age % 20 == 0) b.hurt(i, p, t, mode, 2f);
            }
        });
        return true;
    }

    /** Rust Field: the ground rusts. Foes inside are slowed, weakened and weighed down, their armour corrodes and they lose magicules. */
    static boolean rustField(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 20);
        double r = 5 * GrimoireBook.size(i, p);
        int ticks = 140;
        b.castCircle(p, 1.2f);
        b.vfx(p, ZONE, c, c.add(0, 1, 0), ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                slow(t, 30, 1);
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
                EnergyBridge.effect(t, "burden", 30, 0);
                EnergyBridge.effect(t, "fragility", 30, 0);
                if (age % 20 == 0) {
                    b.hurt(i, p, t, mode, 3f);
                    Nullification.bleed(t, 0.01);
                }
            }
            if (age % 40 == 0) b.vfx(p, BURST, c, c.add(0, 1, 0), 16, (float) (r / 4));
        });
        return true;
    }

    /** Iron God of War: the caster is armoured in a giant iron war god; every second its stomp shakes the ground. */
    static boolean godOfWar(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 5 * GrimoireBook.size(i, p);
        int ticks = 240;
        b.castCircle(p, 1.8f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 2));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 1));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 2));
        VfxSpawn.sendFollowing(p.serverLevel(), ZONE, p, p.position(), b.color, ticks, (float) r);
        b.vfx(p, BURST, p.position().add(0, 1, 0), p.position().add(0, 3, 0), 30, 1.6f);
        for (LivingEntity t : GrimoireBook.around(p, p.position(), r + 1)) {
            b.hurt(i, p, t, mode, 14f);
            slow(t, 60, 2);
        }
        SpellRuntime.zone(p.serverLevel(), ticks, 20, age -> {
            if (!p.isAlive() || age == 0) return;
            for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
                b.hurt(i, p, t, mode, 5f);
                slow(t, 40, 2);
                Vec3 away = t.position().subtract(p.position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1e-4) {
                    Vec3 v = away.normalize().scale(BalanceLaw.isBoss(t) ? 0.1 : 0.4);
                    t.setDeltaMovement(v.x, 0.25, v.z);
                    t.hurtMarked = true;
                }
            }
            b.vfx(p, BURST, p.position().add(0, 0.2, 0), p.position().add(0, 1, 0), 18, (float) (r / 4));
        });
        return true;
    }

    /** Colossus Anvil: a mass of iron the size of a house is dropped on the aim point. */
    static boolean anvil(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 at = GrimoireBook.aim(p, 24);
        double r = 5 * GrimoireBook.size(i, p);
        b.castCircle(p, 1.6f);
        b.vfx(p, ZONE, at, at.add(0, 1, 0), 24, (float) r);
        b.vfx(p, CAST, at.add(0, 16, 0), at, 20, 2f);
        SpellRuntime.later(p.serverLevel(), 20, () -> {
            List<LivingEntity> hit = GrimoireBook.around(p, at, r);
            for (LivingEntity t : hit) {
                b.hurt(i, p, t, mode, 24f);
                slow(t, 100, 3);
                EnergyBridge.effect(t, "burden", 100, 1);
                Vec3 v = t.getDeltaMovement();
                t.setDeltaMovement(v.x * 0.2, 0.35, v.z * 0.2);
                t.hurtMarked = true;
            }
            b.vfx(p, BURST, at, at.add(0, 1, 0), 34, 2f);
        });
        return true;
    }
}
