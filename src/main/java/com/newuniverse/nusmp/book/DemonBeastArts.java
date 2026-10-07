package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Demon Beast Magic: chimera and hydra spells (claws, stingers, roars, fangs, terror, the hunting ground, the many heads, regrowth). */
final class DemonBeastArts {
    private DemonBeastArts() {}

    private static final String BLEED = "nusmp_db_bleed";

    /** Fear from the Tensura effect (bosses only shiver for a second). */
    static void fear(LivingEntity t, int ticks) {
        EnergyBridge.effect(t, "fear", BalanceLaw.isBoss(t) ? Math.min(ticks, 20) : ticks, 1);
    }

    /** A bleeding wound: bleeds magicules once and hurts every half second; a target that already bleeds is not stacked. */
    static void bleed(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, LivingEntity t, int mode, float dmg, int ticks) {
        long now = t.level().getGameTime();
        if (t.getPersistentData().getLong(BLEED) > now) return;
        t.getPersistentData().putLong(BLEED, now + ticks);
        Nullification.bleed(t, 0.02);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            if (!t.isAlive()) return;
            b.hurt(i, p, t, mode, dmg);
            Vec3 c = t.getBoundingBox().getCenter();
            b.vfx(p, VfxShape.DEMON_BEAST_FX3, c, c, 12, 0.4f);
        });
    }

    /** Chimera Claw Rend: three claw swipes in a row on one target within reach, then it bleeds. */
    static boolean clawRend(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 5);
        if (t == null) { GrimoireBook.fail(p, "No one within claw reach."); return false; }
        b.castCircle(p, 0.6f);
        for (int k = 0; k < 3; k++) {
            final int n = k;
            SpellRuntime.later(p.serverLevel(), k * 4, () -> {
                if (!t.isAlive() || !p.isAlive()) return;
                b.hurt(i, p, t, mode, 4f);
                b.vfx(p, VfxShape.DEMON_BEAST_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), 8, 0.8f);
                if (n == 2) {
                    bleed(b, i, p, t, mode, 1.5f, 100);
                    Vec3 c = t.getBoundingBox().getCenter();
                    b.vfx(p, VfxShape.DEMON_BEAST_FX3, c, c, 20, 0.8f);
                }
            });
        }
        return true;
    }

    /** Demon Porcupine's Stingers: a fan of poison quills; a target hit by three or more also bleeds. */
    static boolean stingers(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f), side = new Vec3(-dir.z, 0, dir.x).normalize();
        Map<LivingEntity, Integer> hits = new HashMap<>();
        int n = 7;
        b.castCircle(p, 0.9f);
        b.vfx(p, VfxShape.DEMON_BEAST_FX3, start.add(dir), start.add(dir.scale(2)), 14, 0.8f);
        for (int k = 0; k < n; k++) {
            double off = (k - (n - 1) / 2.0) * 0.09;
            Vec3 d = dir.add(side.scale(off)).normalize();
            if (k % 2 == 0) b.vfx(p, VfxShape.DEMON_BEAST_FX1, start, start.add(d.scale(20)), 12, 0.5f);
            SpellRuntime.bolt(p, start, d.scale(1.5), 0.4, 14, false, null, (bolt, t) -> {
                b.hurt(i, p, t, mode, 3f);
                t.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
                if (hits.merge(t, 1, Integer::sum) == 3) bleed(b, i, p, t, mode, 1.5f, 80);
            }, (bolt, at) -> b.vfx(p, VfxShape.DEMON_BEAST_FX3, at, at, 8, 0.35f));
        }
        return true;
    }

    /** Chimera's Roar: a horned lion's head blasts a cone; it hurts, throws back, terrifies, weakens and shreds defence. */
    static boolean roar(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        double r = 11 * GrimoireBook.size(i, p);
        Vec3 tip = eye.add(look.scale(r));
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.DEMON_BEAST_FX1, eye, tip, 16, 1.2f);
        b.vfx(p, VfxShape.DEMON_BEAST_FX3, tip, tip, 24, 1f);
        int hit = 0;
        for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
            if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.7) continue;
            if (hit++ >= 12) break;
            b.hurt(i, p, t, mode, 10f);
            ElementBook.knock(1.2).apply(t, p);
            fear(t, 100);
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            EnergyBridge.effect(t, "fragility", 100, 0);
        }
        return true;
    }

    /** Chimera Fang Lunge: leap to a target and bite; it is slowed, bleeds and loses defence, and the bite heals you. */
    static boolean fangLunge(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 14);
        if (t == null) { GrimoireBook.fail(p, "Nothing to sink your fangs into."); return false; }
        Vec3 from = p.position(), tp = t.position();
        Vec3 flat = tp.subtract(from).multiply(1, 0, 1);
        b.castCircle(p, 0.7f);
        if (flat.length() > 2.2) {
            Vec3 dest = tp.subtract(flat.normalize().scale(1.6));
            if (!p.level().noCollision(p, p.getBoundingBox().move(dest.subtract(from)))) { GrimoireBook.fail(p, "No room to lunge."); return false; }
            b.vfx(p, VfxShape.DEMON_BEAST_FX1, from.add(0, 1, 0), dest.add(0, 1, 0), 10, 1f);
            p.teleportTo(dest.x, dest.y, dest.z);
            p.fallDistance = 0;
        }
        Vec3 c = t.getBoundingBox().getCenter();
        b.hurt(i, p, t, mode, 11f);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        EnergyBridge.effect(t, "fragility", 100, 0);
        BalanceLaw.heal(p, 2f * (float) EnergyBridge.scale(p));
        bleed(b, i, p, t, mode, 1.5f, 100);
        b.vfx(p, VfxShape.DEMON_BEAST_FX3, c, c, 20, 0.9f);
        return true;
    }

    /** Terror Howl: everything near you is gripped by fear, loses its target, glows for the pack and is shoved away (hard control). */
    static boolean terrorHowl(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 9 * GrimoireBook.size(i, p);
        List<LivingEntity> ts = GrimoireBook.around(p, p.position(), r);
        if (ts.isEmpty()) { GrimoireBook.fail(p, "No one to terrify."); return false; }
        if (!GrimoireBook.control(p)) return false;
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.DEMON_BEAST_FX2, p.position(), p.position(), 30, (float) r);
        b.vfx(p, VfxShape.DEMON_BEAST_FX3, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 28, 1.1f);
        int n = 0;
        for (LivingEntity t : ts) {
            if (n++ >= 16) break;
            fear(t, BalanceLaw.controlTicks(t, 120));
            if (t instanceof Mob m) m.setTarget(null);
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
            b.hurt(i, p, t, mode, 3f);
            ElementBook.knock(0.9).apply(t, p);
        }
        return true;
    }

    /** Horned Spirit Beast Rush: a charge that tosses what it hits, then a short burst of speed and thick hide. */
    static BookPage.Cast rush() {
        BookPage.Cast dash = ElementBook.dash(9, 10, false, VfxShape.DEMON_BEAST_FX1,
                ElementBook.all(ElementBook.knock(1.6), ElementBook.lift(0.4),
                        ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1))));
        return (b, i, p, mode) -> {
            if (!dash.cast(b, i, p, mode)) return false;
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1));
            b.vfx(p, VfxShape.DEMON_BEAST_FX3, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 22, 1f);
            return true;
        };
    }

    /** Chimera Hunting Ground: claws rake the ground at the aim point; enemies inside are slowed, scared once and kept bleeding. */
    static boolean huntingGround(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 20);
        double r = 7 * GrimoireBook.size(i, p);
        int ticks = 140;
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.DEMON_BEAST_FX2, c, c, ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (n++ >= 10) break;
                b.hurt(i, p, t, mode, 2.5f);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                if (age == 0) fear(t, 40);
                bleed(b, i, p, t, mode, 1.2f, 60);
                Vec3 tc = t.getBoundingBox().getCenter();
                b.vfx(p, VfxShape.DEMON_BEAST_FX3, tc, tc, 12, 0.5f);
            }
        });
        return true;
    }

    /** Demon Beast Mantle: the beast aura wraps you; strength, speed and jump, and your melee hits carry extra damage (30 s). */
    static boolean mantle(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 600;
        var tag = i.getOrCreateTag();
        tag.putLong("EmpowerUntil", p.level().getGameTime() + ticks);
        tag.putFloat("EmpowerBonus", 4f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 1));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 1));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.DEMON_BEAST_FX3, p, p.position().add(0, 1, 0), b.color, 40, 1.2f);
        return true;
    }

    /** Underworld Hide: a thick beast hide; absorption and resistance, and attackers are torn by thorns (15 s). */
    static boolean hide(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 300;
        i.getOrCreateTag().putLong("ThornsUntil", p.level().getGameTime() + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks + 100, EnergyBridge.modifier(p) + 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.DEMON_BEAST_FX3, p, p.position().add(0, 1, 0), b.color, 40, 1f);
        return true;
    }

    /** Hydra's Many Heads: three spectral heads circle you for 5 s and strike the nearest enemies (poison, slow and weakness, bleed). */
    static boolean manyHeads(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 100;
        b.castCircle(p, 1.6f);
        b.vfx(p, VfxShape.DEMON_BEAST_FX2, p.position(), p.position(), ticks, 10f);
        b.vfx(p, VfxShape.DEMON_BEAST_FX3, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 28, 1.2f);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            if (!p.isAlive()) return;
            List<LivingEntity> ts = new ArrayList<>(GrimoireBook.around(p, p.position(), 12));
            ts.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
            for (int h = 0; h < 3 && h < ts.size(); h++) {
                LivingEntity t = ts.get(h);
                double ang = age * 0.05 + h * 2.0944;
                Vec3 head = p.position().add(Math.cos(ang) * 2.2, 2.6, Math.sin(ang) * 2.2);
                Vec3 c = t.getBoundingBox().getCenter();
                b.vfx(p, VfxShape.DEMON_BEAST_FX1, head, c, 8, 0.9f);
                b.hurt(i, p, t, mode, 5f);
                if (h == 0) t.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
                else if (h == 1) {
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                    t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                } else bleed(b, i, p, t, mode, 1.5f, 80);
                if (h == 0) BalanceLaw.heal(p, 1f * (float) EnergyBridge.scale(p));
                b.vfx(p, VfxShape.DEMON_BEAST_FX3, c, c, 14, 0.6f);
            }
        });
        return true;
    }

    /** Whether the Tensura Magic Jamming effect has cut the magic currents of this player. */
    private static boolean severed(ServerPlayer p) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("tensura", "silence")).map(h -> p.hasEffect(h)).orElse(false);
    }

    /** Hydra's Regrowth: heals from any injury (up to 40% of your health, the usual healing decay applies) and washes out wounds; jammed currents stop it. */
    static boolean regrowth(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (severed(p)) { GrimoireBook.fail(p, "Your magic currents are severed."); return false; }
        BalanceLaw.heal(p, p.getMaxHealth() * 0.4f);
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
        p.removeEffect(MobEffects.POISON);
        p.removeEffect(MobEffects.WITHER);
        p.removeEffect(MobEffects.WEAKNESS);
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        p.removeEffect(MobEffects.DIG_SLOWDOWN);
        p.removeEffect(MobEffects.BLINDNESS);
        p.getPersistentData().putLong(BLEED, 0L);
        p.clearFire();
        b.castCircle(p, 1.2f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.DEMON_BEAST_FX3, p, p.position().add(0, 1, 0), b.color, 40, 1.2f);
        b.vfx(p, VfxShape.DEMON_BEAST_FX2, p.position(), p.position(), 50, 3f);
        return true;
    }
}
