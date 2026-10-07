package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Key Magic: the custom spells of its grimoire (KeyBook lists them). Keys lock and unlock: a locked mage cannot cast, a locked body
 * cannot move, a door opens anywhere. FX1 = a key in flight, FX2 = a door / keyhole zone (power = radius), FX3 = the key turning.
 */
final class KeyArts {
    private KeyArts() {}

    static final String STOLEN = "KeyStolen", MASTER = "KeyMasterUntil";

    private static float safe(LivingEntity t, float raw) { return BalanceLaw.isBoss(t) ? raw * 0.5f : raw; }

    private static MobEffectInstance slow(int ticks, int amp) { return new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp); }

    private static Vec3 centre(LivingEntity t) { return t.getBoundingBox().getCenter(); }

    /** Locks the grimoire of a mage target (players cannot chant for the time; mobs lose their magic through the jam). */
    private static void lock(LivingEntity t, int ticks) {
        if (t instanceof Player) t.getPersistentData().putLong("nusmp_sealed_until", t.level().getGameTime() + ticks);
        EnergyBridge.effect(t, "silence", ticks, 0);
    }

    // ================================================================ Janus Baptism
    /** Janus Baptism: a key locks the target's magic (5 s, 1 s on bosses) and the caster draws some of it out as a stolen spell charge. */
    static boolean janusBaptism(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "No mage to lock."); return false; }
        boolean boss = BalanceLaw.isBoss(t);
        int ticks = boss ? 20 : 100;
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.KEY_FX1, p.getEyePosition(), centre(t), 12, 1f);
        b.vfx(p, VfxShape.KEY_FX3, centre(t), centre(t).add(0, 1, 0), 30, 1f);
        b.hurt(i, p, t, mode, safe(t, 5f));
        lock(t, ticks);
        if (boss) Nullification.interfere(t, 2, 0.3f, 0.3f);
        else Nullification.jamAll(t, 5, 0.6f);
        t.addEffect(slow(ticks, 1));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0));
        EnergyBridge.drain(t, p, 0.06);
        var tag = i.getOrCreateTag();
        tag.putInt(STOLEN, Math.min(4, tag.getInt(STOLEN) + 1));
        i.markDirty();
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("Stolen magic stored: " + tag.getInt(STOLEN) + "/4"), true);
        return true;
    }

    // ================================================================ Keyhole Prison
    /** Locks one target away behind a keyhole: frozen, unseen and silent, then the key turns and it is thrown out hurt. */
    static boolean keyholePrison(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "No target to lock away."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 70);
        TimeStop.freeze(t, ticks);
        t.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, true, false));
        EnergyBridge.effect(t, "silence", ticks, 0);
        b.castCircle(p, 1f);
        Vec3 at = t.position();
        b.vfx(p, VfxShape.KEY_FX1, p.getEyePosition(), centre(t), 10, 1f);
        b.vfx(p, VfxShape.KEY_FX2, at, at.add(0, 1, 0), ticks, Math.max(1.2f, t.getBbWidth() * 1.4f));
        ServerLevel level = p.serverLevel();
        SpellRuntime.later(level, ticks + 2, () -> {
            if (!t.isAlive()) return;
            b.hurt(i, p, t, mode, safe(t, 14f));
            Vec3 away = t.position().subtract(p.position()).multiply(1, 0, 1).normalize();
            t.knockback(0.9, -away.x, -away.z);
            b.vfx(p, VfxShape.KEY_FX3, centre(t), centre(t).add(0, 1, 0), 28, 1.2f);
        });
        return true;
    }

    // ================================================================ Door Step
    /** Opens a door ahead and steps through it (up to 14 blocks, through thin walls); the door slams on whoever stands at the exit. */
    static boolean doorStep(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f), from = p.position();
        double max = 14 * GrimoireBook.size(i, p);
        Vec3 dest = null;
        for (double d = max; d >= 2; d -= 0.5) {
            Vec3 c = eye.add(dir.scale(d)).subtract(0, p.getEyeHeight(), 0);
            if (!p.serverLevel().isLoaded(BlockPos.containing(c))) continue;
            if (p.level().noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
        }
        if (dest == null) { GrimoireBook.fail(p, "No room for a door there."); return false; }
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.KEY_FX2, from, from.add(0, 1, 0), 24, 1.3f);
        b.vfx(p, VfxShape.KEY_FX2, dest, dest.add(0, 1, 0), 28, 1.3f);
        b.vfx(p, VfxShape.KEY_FX1, from.add(0, 1, 0), dest.add(0, 1, 0), 10, 0.8f);
        p.teleportTo(dest.x, dest.y, dest.z);
        p.fallDistance = 0;
        for (LivingEntity t : GrimoireBook.around(p, dest, 3)) {
            b.hurt(i, p, t, mode, safe(t, 4f));
            t.addEffect(slow(40, 1));
            Vec3 away = t.position().subtract(dest).multiply(1, 0, 1).normalize();
            t.knockback(0.8, -away.x, -away.z);
        }
        return true;
    }

    // ================================================================ Master Key
    /** Master Key: the key itself in your hand: faster hands, +4 damage on melee hits that also lock the foe's legs and magic (see KeyBook). */
    static boolean masterKey(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        i.getOrCreateTag().putLong(MASTER, p.level().getGameTime() + 360);
        i.markDirty();
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 360, 0));
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 360, 1));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.KEY_FX3, p, p.position().add(0, 1, 0), b.color, 30, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.KEY_FX2, p, p.position(), b.color, 360, 1.4f);
        return true;
    }

    // ================================================================ Bolted Door
    /** Bolted Door: a locked door around you for 8 s: Resistance, a thick absorption shell, enemy shots stop and foes are pushed off. */
    static boolean boltedDoor(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 3.5f * GrimoireBook.size(i, p);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1));
        p.setAbsorptionAmount(Math.max(p.getAbsorptionAmount(), 6f * (float) BalanceLaw.epScale(p) * 0.5f + 4f));
        b.castCircle(p, 1.2f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.KEY_FX2, p, p.position(), b.color, 160, r);
        SpellRuntime.zone(p.serverLevel(), 160, 5, age -> {
            if (!p.isAlive()) return;
            for (Projectile pr : WikiSpells.enemyShots(p, p.getBoundingBox().inflate(r))) pr.discard();
            for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
                Vec3 away = t.position().subtract(p.position()).multiply(1, 0, 1).normalize();
                t.knockback(BalanceLaw.isBoss(t) ? 0.2 : 0.6, -away.x, -away.z);
            }
            if (age % 40 == 0) b.vfx(p, VfxShape.KEY_FX3, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 20, 0.8f);
        });
        return true;
    }

    // ================================================================ Unlock
    /** Unlock: turns the key on every lock that holds you and your allies (6 blocks): debuffs, ice, stopped time and a sealed grimoire fall away. */
    static boolean unlock(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int n = 0;
        float r = 6 * GrimoireBook.size(i, p);
        for (ServerPlayer a : p.serverLevel().getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(r),
                e -> e.isAlive() && WikiSpells.ally(p, e))) {
            if (n++ >= 12) break;
            for (MobEffectInstance e : new ArrayList<>(a.getActiveEffects()))
                if (!e.getEffect().value().isBeneficial()) a.removeEffect(e.getEffect());
            a.setTicksFrozen(0);
            a.getPersistentData().putLong("nusmp_sealed_until", 0L);
            TimeStop.immune(a, 20);
            BalanceLaw.heal(a, a.getMaxHealth() * 0.12f);
            b.vfx(p, VfxShape.KEY_FX3, a.position().add(0, 1, 0), a.position().add(0, 2, 0), 26, 0.9f);
        }
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.KEY_FX2, p.position(), p.position().add(0, 1, 0), 30, r * 0.5f);
        return true;
    }

    // ================================================================ Locked Domain
    /** Locked Domain: a sealed room at the aim point for 7 s: foes inside crawl, cannot cast (players' grimoires lock) and are cut by the walls. */
    static boolean lockedDomain(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 20);
        float r = 5.5f * GrimoireBook.size(i, p);
        b.castCircle(p, 1.3f);
        b.vfx(p, VfxShape.KEY_FX2, c, c.add(0, 1, 0), 140, r);
        b.vfx(p, VfxShape.KEY_FX3, c, c.add(0, 1, 0), 30, 1.3f);
        SpellRuntime.zone(p.serverLevel(), 140, 10, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                t.addEffect(slow(30, BalanceLaw.isBoss(t) ? 1 : 3));
                EnergyBridge.effect(t, "silence", 30, 0);
                if (t instanceof Player) t.getPersistentData().putLong("nusmp_sealed_until", t.level().getGameTime() + 30);
                if (age % 20 == 0) b.hurt(i, p, t, mode, safe(t, 2.5f));
            }
            if (age % 40 == 0) Nullification.shatterBarriers(p.serverLevel(), c, r, p);
        });
        return true;
    }

    // ================================================================ Key Armory
    /** Key Armory: for 8 s golden key-spears rise behind you and fly at the nearest foe within 16 blocks, one every 0.6 s. */
    static boolean keyArmory(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (GrimoireBook.around(p, p.position(), 16).isEmpty()) { GrimoireBook.fail(p, "No foe for the armory."); return false; }
        b.castCircle(p, 1.2f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.KEY_FX2, p, p.position(), b.color, 160, 2.5f);
        SpellRuntime.zone(p.serverLevel(), 160, 12, age -> {
            if (!p.isAlive()) return;
            LivingEntity near = null;
            for (LivingEntity t : GrimoireBook.around(p, p.position(), 16)) if (near == null || t.distanceToSqr(p) < near.distanceToSqr(p)) near = t;
            if (near == null) return;
            final LivingEntity tgt = near;
            Vec3 start = p.position().add((p.getRandom().nextDouble() - 0.5) * 3, 2.4, (p.getRandom().nextDouble() - 0.5) * 3);
            Vec3 vel = centre(tgt).subtract(start).normalize().scale(1.8);
            b.vfx(p, VfxShape.KEY_FX1, start, centre(tgt), 10, 0.6f);
            SpellRuntime.bolt(p, start, vel, 0.5, 14, false, tgt, (bolt, t) -> {
                b.hurt(i, p, t, mode, safe(t, 4f));
                t.addEffect(slow(20, 1));
            }, (bolt, at) -> b.vfx(p, VfxShape.KEY_FX3, at, at, 14, 0.5f));
        });
        return true;
    }

    // ================================================================ Janus Abigail
    /** Janus Abigail: the magical space opens: 6 doors (+1 per stolen charge, up to 10) appear around you and each looses a different stolen spell. */
    static boolean janusAbigail(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity first = GrimoireBook.target(p, 40);
        Vec3 aim = first != null ? centre(first) : GrimoireBook.aim(p, 40);
        var tag = i.getOrCreateTag();
        int doors = 6 + Math.min(4, tag.getInt(STOLEN));
        tag.putInt(STOLEN, 0);
        i.markDirty();
        Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1).normalize();
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(0, 0, 1);
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        ServerLevel level = p.serverLevel();
        b.castCircle(p, 1.5f);
        b.vfx(p, VfxShape.KEY_FX2, p.position(), p.position().add(0, 1, 0), 60, 3.5f);
        for (int k = 0; k < doors; k++) {
            double ang = Math.PI * (k + 0.5) / doors;                       // a half ring from left to right, above and around
            Vec3 pos = p.position().add(side.scale(-Math.cos(ang) * 3.2)).add(dir.scale(Math.sin(ang) * 1.5 - 0.5)).add(0, 2.2 + 0.5 * Math.sin(ang), 0);
            final int kind = k % 4;
            b.vfx(p, VfxShape.KEY_FX2, pos, pos.add(0, 0.5, 0), 36, 0.8f);
            SpellRuntime.later(level, 6 + k * 3, () -> {
                if (!p.isAlive()) return;
                Vec3 v = aim.subtract(pos).normalize().scale(1.6);
                b.vfx(p, VfxShape.KEY_FX1, pos, aim, 18, 0.9f);
                SpellRuntime.bolt(p, pos, v, 0.7, 30, false, null, (bolt, t) -> {
                    float raw = safe(t, 7f);
                    switch (kind) {
                        case 0 -> { b.hurtAs(i, p, t, mode, raw, TensuraDamageTypes.FIRE_ELEMENTAL); t.igniteForSeconds(4); }
                        case 1 -> { b.hurtAs(i, p, t, mode, raw, TensuraDamageTypes.ICE_ELEMENTAL); t.setTicksFrozen(t.getTicksRequiredToFreeze() + 60); t.addEffect(slow(50, 1)); }
                        case 2 -> { b.hurtAs(i, p, t, mode, raw, TensuraDamageTypes.LIGHTNING_ELEMENTAL); EnergyBridge.effect(t, "silence", 40, 0); t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0)); }
                        default -> { b.hurtAs(i, p, t, mode, raw, TensuraDamageTypes.WIND_ELEMENTAL); t.setDeltaMovement(t.getDeltaMovement().add(0, 0.6, 0)); t.hurtMarked = true; }
                    }
                }, (bolt, at) -> b.vfx(p, VfxShape.KEY_FX3, at, at, 16, 0.7f));
            });
        }
        return true;
    }

    // ================================================================ Doom's Gate
    /** Doom's Gate: pays most of your remaining life and a quarter of your magic; after 2.5 s a great door looses one gargantuan beam. */
    static boolean doomsGate(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!p.isCreative() && p.getHealth() < 10f) { GrimoireBook.fail(p, "You have too little life left to pay the gate."); return false; }
        float s = GrimoireBook.size(i, p) * EnergyBridge.scale(p);
        Vec3 dir = p.getViewVector(1f).normalize(), door = p.getEyePosition().add(dir.scale(5));
        Vec3 end = door.add(dir.scale(44 * s));
        if (!p.isCreative()) p.setHealth(Math.max(2f, p.getHealth() * 0.4f));
        EnergyBridge.drain(p, null, 0.25);
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 1));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 1));
        ServerLevel level = p.serverLevel();
        b.castCircle(p, 2f);
        b.vfx(p, VfxShape.KEY_FX2, door, door, 70, 3.2f);
        SpellRuntime.later(level, 50, () -> {
            if (!p.isAlive()) return;
            b.vfx(p, VfxShape.KEY_FX1, door, end, 24, 1.8f);
            b.vfx(p, VfxShape.KEY_FX3, door, end, 30, 1.5f);
            b.vfx(p, VfxShape.KEY_FX3, end, end, 40, 2f);
            for (LivingEntity t : GrimoireBook.along(p, door, end, 3.0 * s)) b.hurt(i, p, t, mode, safe(t, 30f));
            double r = 7 * s;
            for (LivingEntity t : GrimoireBook.around(p, end, r)) {
                b.hurt(i, p, t, mode, safe(t, 18f));
                Vec3 away = t.position().subtract(end).multiply(1, 0, 1).normalize();
                t.knockback(BalanceLaw.isBoss(t) ? 0.4 : 1.6, -away.x, -away.z);
            }
            Nullification.shatterBarriers(level, door, 6, p);
            Nullification.shatterBarriers(level, door.add(dir.scale(22 * s)), 8, p);
            Nullification.shatterBarriers(level, end, r, p);
        });
        return true;
    }
}
