package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Body Magic: the user condenses, hardens and drives their own flesh. Strikes are bullet-fast and armour-ignoring, the
 * defensive pages harden the frame (they cost Aura on top of the page), and the healing pages knit tissue back together.
 */
final class BodyArts {
    private BodyArts() {}

    private static final VfxShape CAST = VfxShape.BODY_FX1, ZONE = VfxShape.BODY_FX2, BURST = VfxShape.BODY_FX3;
    static final String FRAME = "BodyFrameUntil", TITAN = "BodyTitanUntil";

    private static Vec3 hand(ServerPlayer p) { return p.getEyePosition().add(0, -0.35, 0); }
    private static Vec3 mid(LivingEntity t) { return t.getBoundingBox().getCenter(); }

    private static void slow(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.isBoss(t) ? Math.max(20, ticks / 2) : ticks, amp));
    }

    private static List<LivingEntity> cap(List<LivingEntity> l, int n) { return new ArrayList<>(l.subList(0, Math.min(n, l.size()))); }

    /** Pushes the target away from 'from' along the ground. */
    static void shove(Vec3 from, LivingEntity t, double k) {
        Vec3 d = t.position().subtract(from);
        double len = Math.sqrt(d.x * d.x + d.z * d.z);
        if (len < 1e-3) return;
        t.knockback(k, -d.x / len, -d.z / len);
    }

    private static void lift(LivingEntity t, double y) {
        t.setDeltaMovement(t.getDeltaMovement().add(0, y, 0));
        t.hurtMarked = true;
    }

    /** Self buffs draw on Aura (magicules when it is empty). */
    private static boolean aura(ServerPlayer p, double frac, double floor) {
        if (EnergyBridge.aura(p, frac, floor)) return true;
        GrimoireBook.fail(p, "Not enough aura to push the body that far.");
        return false;
    }

    /** Removes every harmful potion effect (a copy of the list, so nothing changes under the loop). */
    private static void cleanse(LivingEntity t) {
        for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects()))
            if (!e.getEffect().value().isBeneficial()) t.removeEffect(e.getEffect());
    }

    private static List<Player> circle(ServerPlayer p, double r) {
        return p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(r),
                q -> (q == p || q.isAlliedTo(p)) && q.isAlive() && !q.isSpectator() && q.distanceToSqr(p) <= r * r);
    }

    // ------------------------------------------------------------------ Bullet Fist
    /** The legs fire the whole body forward like a shot: everything on the way is struck, knocked off its feet and slowed. */
    static boolean bulletFist(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1e-4) { GrimoireBook.fail(p, "Look ahead to strike."); return false; }
        dir = dir.normalize();
        final Vec3 start = p.position().add(0, 1, 0);
        final double reach = 1.7 * GrimoireBook.size(i, p);
        final Set<LivingEntity> struck = new HashSet<>();
        p.setDeltaMovement(dir.x * 2.1, 0.12, dir.z * 2.1);
        p.hurtMarked = true;
        p.fallDistance = 0;
        b.castCircle(p, 0.7f);
        b.vfx(p, CAST, start, start.add(dir.scale(9)), 8, 1.0f);
        SpellRuntime.zone(p.serverLevel(), 7, 1, age -> {
            if (!p.isAlive()) return;
            p.fallDistance = 0;
            for (LivingEntity t : cap(GrimoireBook.around(p, p.position().add(0, 1, 0), reach), 6)) {
                if (!struck.add(t)) continue;
                float raw = 8f;
                raw *= EnergyBridge.armourBypass(t, p.damageSources().mobAttack(p), raw, 0.35f);
                b.hurt(i, p, t, mode, raw);
                shove(p.position(), t, 1.0);
                lift(t, 0.2);
                slow(t, 30, 1);
                b.vfx(p, BURST, mid(t), mid(t), 16, 0.7f);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Rapid Barrage
    /** Five jabs in under a second into the same pressure points; the last one is the heavy one and leaves the foe weakened. */
    static boolean rapidBarrage(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        final LivingEntity t = GrimoireBook.target(p, 6);
        if (t == null) { GrimoireBook.fail(p, "No one within reach."); return false; }
        b.castCircle(p, 0.6f);
        for (int k = 0; k < 5; k++) {
            final int n = k;
            SpellRuntime.later(p.serverLevel(), k * 3, () -> {
                if (!t.isAlive() || !p.isAlive() || t.distanceToSqr(p) > 100) return;
                float raw = n == 4 ? 6f : 2.8f;
                raw *= EnergyBridge.armourBypass(t, p.damageSources().mobAttack(p), raw, 0.4f);
                t.invulnerableTime = 0;                                  // every jab lands, not only the first
                b.hurt(i, p, t, mode, raw);
                b.vfx(p, CAST, hand(p), mid(t), 4, 0.5f);
                b.vfx(p, BURST, mid(t), mid(t), 10, n == 4 ? 0.8f : 0.4f);
                if (n == 4) {
                    shove(p.position(), t, 1.2);
                    t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BalanceLaw.isBoss(t) ? 30 : 80, 0));
                }
            });
        }
        return true;
    }

    // ------------------------------------------------------------------ Body Compression
    /** The wiki spell: the forearm is condensed into a gun barrel and fires three compressed rounds, the last packed double. */
    static boolean compression(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 0.9f);
        b.vfx(p, BURST, hand(p), hand(p), 14, 0.8f);                      // the barrel forms
        for (int k = 0; k < 3; k++) {
            final int n = k;
            SpellRuntime.later(p.serverLevel(), 6 + k * 4, () -> {
                if (!p.isAlive()) return;
                Vec3 start = hand(p).add(p.getViewVector(1f).scale(0.6)), dir = p.getViewVector(1f);
                float raw = n == 2 ? 11f : 6.5f;
                b.vfx(p, CAST, start, start.add(dir.scale(34)), 9, n == 2 ? 1.1f : 0.8f);
                SpellRuntime.bolt(p, start, dir.scale(3.4), 0.45 * GrimoireBook.size(i, p), 10, true, null, (bolt, t) -> {
                    b.hurt(i, p, t, mode, raw);
                    shove(bolt.pos.subtract(bolt.vel), t, n == 2 ? 0.9 : 0.3);
                }, (bolt, at) -> b.vfx(p, BURST, at, at, 14, n == 2 ? 0.9f : 0.5f));
                p.setDeltaMovement(p.getDeltaMovement().add(dir.scale(-0.12)));   // the recoil
                p.hurtMarked = true;
            });
        }
        return true;
    }

    // ------------------------------------------------------------------ Tendon Sever
    /** A precise strike at the tendons: armour is mostly ignored, the foe bleeds magicules and can barely move, work or swing. */
    static boolean tendonSever(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 8);
        if (t == null) { GrimoireBook.fail(p, "No tendons in sight."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 70);
        float raw = 5f;
        raw *= EnergyBridge.armourBypass(t, p.damageSources().mobAttack(p), raw, 0.6f);
        b.castCircle(p, 0.8f);
        b.hurt(i, p, t, mode, raw);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 5));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 1));
        Nullification.bleed(t, 0.04);
        b.vfx(p, CAST, hand(p), mid(t), 8, 0.9f);
        b.vfx(p, BURST, mid(t), mid(t), 20, 0.9f);
        return true;
    }

    // ------------------------------------------------------------------ Hardened Frame
    /** For 20 s the bones and muscle are reinforced: physical blows are cut by 30%, magic by 15%, and 4 hearts of tissue are added. */
    static boolean hardenedFrame(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!aura(p, 0.08, 40)) return false;
        i.getOrCreateTag().putLong(FRAME, p.level().getGameTime() + 400);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1));
        PlayerAuras.set(p, Aura.BODY, 400, 0);
        b.castCircle(p, 1.1f);
        VfxSpawn.sendFollowing(p.serverLevel(), BURST, p, p.position().add(0, 1, 0), b.color, 28, 1.0f);
        return true;
    }

    // ------------------------------------------------------------------ Tissue Mend
    /** Torn flesh knits shut: you and allies within 5 blocks heal a quarter of max health, regenerate and shed every harmful effect. */
    static boolean tissueMend(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1f);
        b.vfx(p, ZONE, p.position(), p.position().add(0, 1, 0), 40, 5f);
        for (Player a : circle(p, 5)) {
            BalanceLaw.heal(a, a.getMaxHealth() * 0.25f);
            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
            cleanse(a);
            a.clearFire();
            b.vfx(p, BURST, a.position().add(0, 1, 0), a.position().add(0, 1, 0), 18, 0.7f);
        }
        return true;
    }

    // ------------------------------------------------------------------ Leg Spring
    /** The legs coil and throw you high and forward; where you land the ground takes the whole weight. No fall damage. */
    static boolean legSpring(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!aura(p, 0.04, 30)) return false;
        Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1);
        dir = dir.lengthSqr() < 1e-4 ? Vec3.ZERO : dir.normalize();
        final double r = 4.2 * GrimoireBook.size(i, p);
        final boolean[] landed = {false};
        p.setDeltaMovement(dir.x * 1.1, 1.15, dir.z * 1.1);
        p.hurtMarked = true;
        p.fallDistance = 0;
        b.castCircle(p, 0.8f);
        b.vfx(p, BURST, p.position(), p.position().add(0, 1, 0), 16, 0.8f);
        SpellRuntime.zone(p.serverLevel(), 50, 1, age -> {
            if (landed[0] || !p.isAlive()) return;
            p.fallDistance = 0;
            if (age < 5 || !p.onGround()) return;
            landed[0] = true;
            Vec3 at = p.position();
            b.vfx(p, ZONE, at, at.add(0, 1, 0), 22, (float) r);
            b.vfx(p, BURST, at, at.add(0, 1, 0), 24, 1.2f);
            for (LivingEntity t : cap(GrimoireBook.around(p, at, r), 10)) {
                b.hurt(i, p, t, mode, 9f);
                shove(at, t, 0.8);
                lift(t, 0.3);
                slow(t, 40, 1);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Quake Stomp
    /** A stomp that sends a ring of broken ground outward: five waves, each one hitting what it reaches, staggering and slowing it. */
    static boolean quakeStomp(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        final Vec3 c = p.position();
        final double r = 6 * GrimoireBook.size(i, p);
        final int waves = 5;
        final Set<LivingEntity> struck = new HashSet<>();
        b.castCircle(p, 1.3f);
        b.vfx(p, ZONE, c, c.add(0, 1, 0), 30, (float) r);
        b.vfx(p, BURST, c, c.add(0, 1, 0), 26, 1.3f);
        SpellRuntime.zone(p.serverLevel(), waves * 3, 3, age -> {
            double inner = r * (age / 3) / waves, outer = r * (age / 3 + 1) / waves;
            for (LivingEntity t : cap(GrimoireBook.around(p, c, outer), 12)) {
                double dx = t.getX() - c.x, dz = t.getZ() - c.z;
                if (Math.sqrt(dx * dx + dz * dz) < inner || !struck.add(t)) continue;
                b.hurt(i, p, t, mode, 7f);
                shove(c, t, 0.9);
                lift(t, 0.35);
                slow(t, 60, 2);
                t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, BalanceLaw.isBoss(t) ? 20 : 60, 0));
                b.vfx(p, BURST, t.position(), t.position(), 12, 0.5f);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Titan Form
    /** The signature: for 20 s the body swells with condensed muscle. Strength, Resistance and +6 hearts; melee blows hit harder and shake the foes beside the target. The strain costs a short weakness at the end. */
    static boolean titanForm(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!aura(p, 0.15, 100)) return false;
        final long until = p.level().getGameTime() + 400;
        i.getOrCreateTag().putLong(TITAN, until);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1));
        p.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 400, 2));
        PlayerAuras.set(p, Aura.BODY, 400, 1);
        b.castCircle(p, 1.6f);
        b.vfx(p, ZONE, p.position(), p.position().add(0, 1, 0), 40, 4f);
        VfxSpawn.sendFollowing(p.serverLevel(), BURST, p, p.position().add(0, 1, 0), b.color, 30, 1.6f);
        for (LivingEntity t : cap(GrimoireBook.around(p, p.position(), 4 * GrimoireBook.size(i, p)), 8)) {   // the swelling body throws them back
            b.hurt(i, p, t, mode, 6f);
            shove(p.position(), t, 1.2);
        }
        SpellRuntime.zone(p.serverLevel(), 400, 40, age -> {              // steam rolls off the hot muscle
            if (p.isAlive() && p.level().getGameTime() < until)
                VfxSpawn.sendFollowing(p.serverLevel(), BURST, p, p.position().add(0, 1, 0), b.color, 20, 0.5f);
        });
        SpellRuntime.later(p.serverLevel(), 400, () -> {
            if (!p.isAlive() || i.getOrCreateTag().getLong(TITAN) != until) return;   // a recast has its own ending
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            PlayerAuras.clear(p, Aura.BODY);
        });
        return true;
    }

    // ------------------------------------------------------------------ Flesh Regrowth (daily)
    /** Once a day: half of the missing body is regrown at once, the rest follows as strong regeneration, every harmful effect is shed and allies within 8 blocks are mended too. */
    static boolean regrowth(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.6f);
        b.vfx(p, ZONE, p.position(), p.position().add(0, 1, 0), 60, 8f);
        for (Player a : circle(p, 8)) {
            boolean self = a == p;
            BalanceLaw.heal(a, a.getMaxHealth() * (self ? 0.5f : 0.3f));
            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, self ? 240 : 140, self ? 2 : 1));
            a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, self ? 2 : 1));
            cleanse(a);
            a.clearFire();
            b.vfx(p, BURST, a.position().add(0, 1, 0), a.position().add(0, 1, 0), 24, self ? 1.2f : 0.8f);
        }
        return true;
    }
}
