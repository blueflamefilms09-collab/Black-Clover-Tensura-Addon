package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Gel Magic: the spells of GelBook that need their own logic (sticky pools, bounce pads, the gel shield, the salamanders). */
public final class GelArts {
    private GelArts() {}

    private static final int COLOR = GelBook.COLOR;
    private static final String NOFALL = "nusmp_gel_nofall", SHIELD = "nusmp_gel_shield", BOUNCE = "nusmp_gel_bounce";
    private static final String[] POISONS = {"fatal_poison", "magicule_poison", "corrosion", "infection"};
    private static boolean hooked;

    /** Registers the damage hooks (the gel shield, the bounce landing). Called once from GelProps.init(). */
    public static synchronized void hooks() {
        if (hooked) return;
        hooked = true;
        AttributeEvents.incoming(e -> {
            LivingEntity victim = e.getEntity();
            long now = victim.level().getGameTime();
            var data = victim.getPersistentData();
            var src = e.getSource();
            if (src.is(DamageTypeTags.IS_FALL) && data.getLong(NOFALL) > now) { e.setCanceled(true); return; }
            if (data.getLong(SHIELD) <= now || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
            float before = e.getAmount();
            float kept = src.is(DamageTypeTags.IS_PROJECTILE) ? 0.3f : 0.5f;
            e.setAmount(before * kept);
            if (src.getDirectEntity() instanceof LivingEntity att && att != victim) stick(att, 60, 1);
            BalanceLaw.heal(victim, before * (1f - kept) * 0.15f);
        });
    }

    // ------------------------------------------------------------------ small helpers
    /** Gel on a target: Slowness for 'ticks' and its sprint stopped. */
    static void stick(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp));
        t.setSprinting(false);
        Vec3 v = t.getDeltaMovement();
        t.setDeltaMovement(v.x * 0.4, v.y, v.z * 0.4);
        t.hurtMarked = true;
    }

    /** Gel that holds: stuck fast, the Tensura web on top (shortened on bosses). */
    static void hold(LivingEntity t, int ticks, boolean hard) {
        boolean boss = BalanceLaw.isBoss(t);
        stick(t, ticks, boss ? 2 : 5);
        if (hard && !boss) com.newuniverse.nusmp.book.EnergyBridge.effect(t, "webbed", ticks, 0);
    }

    /** The gel neutralises poison: vanilla poison and the Tensura poisons come off. */
    static void cleanse(LivingEntity t) {
        t.removeEffect(MobEffects.POISON);
        t.removeEffect(MobEffects.WITHER);
        for (String id : POISONS)
            BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("tensura", id)).ifPresent(h -> t.removeEffect(h));
    }

    /** The caster and its friends within r of c (at most 12). */
    static List<LivingEntity> friends(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> out = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e.isAlive() && !e.isSpectator() && (e == p || e.isAlliedTo(p)) && e.distanceToSqr(c) <= r * r);
        return out.size() > 12 ? out.subList(0, 12) : out;
    }

    private static float k(GrimoireBook b, ManasSkillInstance i, ServerPlayer p) {
        return GrimoireBook.size(i, p) * EnergyBridge.scale(p);
    }

    // ------------------------------------------------------------------ Sticky Gel: a glob that slows and leaves a pool
    static boolean stickyGel(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(b, i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.GEL_FX1, start, start.add(dir.scale(22)), 14, 1.0f);
        SpellRuntime.bolt(p, start, dir.scale(1.5), 0.5, 15, false, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 6f);
            stick(t, 80, 2);
        }, (bolt, at) -> {
            b.vfx(p, VfxShape.GEL_FX3, at, at, 20, 0.9f);
            pool(b, i, p, mode, at, 2.4 * s, 80, 2, 1.5f);
        });
        return true;
    }

    /** A pool of sticky gel: slows everything inside, neutralises poison on friends. */
    static void pool(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, Vec3 c, double r, int ticks, int amp, float dmg) {
        b.vfx(p, VfxShape.GEL_FX2, c, c.add(0, 1, 0), ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (dmg > 0) b.hurt(i, p, t, mode, dmg);
                stick(t, 30, amp);
            }
            for (LivingEntity f : friends(p, c, r)) cleanse(f);
        });
    }

    // ------------------------------------------------------------------ Gel Burst: a blast of gel that throws everything back
    static boolean gelBurst(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(b, i, p);
        Vec3 c = p.position();
        double r = 4.5 * s;
        List<LivingEntity> hit = GrimoireBook.around(p, c, r);
        for (LivingEntity t : hit) {
            b.hurt(i, p, t, mode, 11f);
            Vec3 out = t.position().subtract(c).multiply(1, 0, 1);
            out = out.lengthSqr() < 1e-4 ? p.getViewVector(1f).multiply(1, 0, 1) : out.normalize();
            double power = BalanceLaw.isBoss(t) ? 0.35 : 1.5;
            t.setDeltaMovement(t.getDeltaMovement().add(out.scale(power)).add(0, 0.45 * (power > 1 ? 1 : 0.4), 0));
            t.hurtMarked = true;
            stick(t, 60, 1);
        }
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.GEL_FX3, c.add(0, 0.5, 0), c.add(0, 1.5, 0), 28, (float) (r / 4.5));
        b.vfx(p, VfxShape.GEL_FX2, c, c.add(0, 1, 0), 24, (float) r);
        return true;
    }

    // ------------------------------------------------------------------ Bouncy Gel Pad
    static boolean bouncyPad(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(b, i, p);
        Vec3 a = GrimoireBook.aim(p, 12);
        Vec3 c = a.distanceTo(p.getEyePosition()) > 11.5 ? p.position() : a;
        double r = 2.4 * s;
        int ticks = 200;
        b.castCircle(p, 0.9f);
        b.vfx(p, VfxShape.GEL_FX2, c, c.add(0, 1, 0), ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 2, age -> {
            long now = p.level().getGameTime();
            for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, 1.6, r),
                    x -> x.isAlive() && !x.isSpectator() && x.distanceToSqr(c.x, x.getY(), c.z) <= r * r && x.getY() - c.y < 1.6)) {
                var d = e.getPersistentData();
                if (d.getLong(BOUNCE) > now || (!e.onGround() && e.getDeltaMovement().y > 0)) continue;
                boolean friend = e == p || e.isAlliedTo(p);
                d.putLong(BOUNCE, now + 12);
                Vec3 v = e.getDeltaMovement();
                if (friend) {
                    e.setDeltaMovement(v.x * 1.2, 1.25, v.z * 1.2);
                    d.putLong(NOFALL, now + 100);
                } else {
                    e.setDeltaMovement(v.x * 0.3, BalanceLaw.isBoss(e) ? 0.4 : 1.0, v.z * 0.3);
                }
                e.hurtMarked = true;
                e.fallDistance = 0;
                b.vfx(p, VfxShape.GEL_FX3, e.position(), e.position().add(0, 1, 0), 14, 0.5f);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Gel Smother: a hard hold with gel over the face
    static boolean smother(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 14);
        if (t == null) { GrimoireBook.fail(p, "No one to smother."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 80);
        boolean boss = BalanceLaw.isBoss(t);
        hold(t, ticks, true);
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        if (!boss) t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.min(ticks, 40), 0));
        b.hurt(i, p, t, mode, 5f);
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.GEL_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), 12, 1.0f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.GEL_FX2, t, t.position(), COLOR, ticks, Math.max(1.2f, t.getBbWidth() * 1.6f));
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            if (!t.isAlive()) return;
            b.hurt(i, p, t, mode, boss ? 1.5f : 3f);
            Vec3 v = t.getDeltaMovement();
            t.setDeltaMovement(0, Math.min(0, v.y), 0);
            t.hurtMarked = true;
        });
        return true;
    }

    // ------------------------------------------------------------------ Gel Antidote
    static boolean antidote(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 7 * k(b, i, p);
        for (LivingEntity f : friends(p, p.position(), r)) {
            cleanse(f);
            BalanceLaw.heal(f, f.getMaxHealth() * 0.12f);
            f.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            b.vfx(p, VfxShape.GEL_FX3, f.position(), f.position().add(0, 1, 0), 18, 0.5f);
        }
        b.castCircle(p, 1.1f);
        b.vfx(p, VfxShape.GEL_FX2, p.position(), p.position().add(0, 1, 0), 40, (float) r);
        return true;
    }

    // ------------------------------------------------------------------ Absorbing Gel
    static boolean shield(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 200;
        long now = p.level().getGameTime();
        p.getPersistentData().putLong(SHIELD, now + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 0));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.GEL_FX2, p, p.position(), COLOR, ticks, 2.2f);
        b.vfx(p, VfxShape.GEL_FX3, p.position().add(0, 1, 0), p.position().add(0, 1.5, 0), 24, 1.0f);
        SpellRuntime.zone(p.serverLevel(), ticks, 2, age -> {
            if (!p.isAlive() || p.level().getGameTime() >= p.getPersistentData().getLong(SHIELD)) return;
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(2.4))) {
                if (pr.getOwner() == p || (pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p))) continue;
                b.vfx(p, VfxShape.GEL_FX3, pr.position(), pr.position(), 12, 0.4f);
                pr.discard();
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Gel Skin
    static boolean skin(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 600;
        long now = p.level().getGameTime();
        i.getOrCreateTag().putLong("GelSkinUntil", now + ticks);
        p.getPersistentData().putLong(NOFALL, now + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 1));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.GEL_FX3, p, p.position().add(0, 1, 0), COLOR, 30, 1.2f);
        return true;
    }

    // ------------------------------------------------------------------ Gel Mire: a field that drags everything to its middle
    static boolean mire(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(b, i, p);
        Vec3 c = GrimoireBook.aim(p, 20);
        double r = 6 * s;
        int ticks = 160;
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.GEL_FX2, c, c.add(0, 1, 0), ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 5, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (age % 2 == 0) b.hurt(i, p, t, mode, 2.5f);
                stick(t, 30, 3);
                Vec3 pull = c.subtract(t.position()).multiply(1, 0, 1);
                if (pull.lengthSqr() > 0.5) {
                    double f = BalanceLaw.isBoss(t) ? 0.03 : 0.1;
                    t.setDeltaMovement(t.getDeltaMovement().add(pull.normalize().scale(f)));
                    t.hurtMarked = true;
                }
            }
            for (LivingEntity f : friends(p, c, r)) cleanse(f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Sticky Salamander / Huge Sticky Salamander
    /** A salamander of gel hunts the nearest enemy (or follows your aim), holds what it touches, carries loose items back to you. */
    static boolean salamander(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, boolean huge) {
        float s = (huge ? 2.2f : 1f) * GrimoireBook.size(i, p);
        double reach = 1.7 * s + 0.4;
        int life = huge ? 240 : 200, maxHeld = huge ? 6 : 2;
        float dmg = huge ? 7f : 3.5f;
        boolean hard = BalanceLaw.beginControl(p);
        Vec3 look = p.getViewVector(1f).multiply(1, 0, 1);
        look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3[] pos = {p.position().add(look.scale(2))};
        List<ItemEntity> carried = new ArrayList<>();
        boolean[] over = {false};
        b.castCircle(p, huge ? 1.6f : 1f);
        b.vfx(p, VfxShape.GEL_FX3, pos[0], pos[0].add(0, 1, 0), 24, huge ? 1.6f : 0.9f);
        b.vfx(p, VfxShape.GEL_FX2, pos[0], pos[0].add(0, 1, 0), 20, (float) reach);
        SpellRuntime.zone(p.serverLevel(), life, 2, age -> {
            if (over[0]) return;
            if (!p.isAlive() || p.isRemoved()) { over[0] = true; return; }
            boolean last = age >= life - 2;
            Vec3 old = pos[0];
            List<LivingEntity> foes = GrimoireBook.around(p, old, 16);
            LivingEntity goal = null;
            double best = 1e9;
            for (LivingEntity t : foes) { double d = t.distanceToSqr(old); if (d < best) { best = d; goal = t; } }
            Vec3 want = goal != null ? goal.position() : GrimoireBook.aim(p, 18);
            if (goal == null && want.distanceTo(p.position()) > 20) want = p.position();
            Vec3 step = want.subtract(old).multiply(1, 0, 1);
            if (step.length() > 0.6) old = old.add(step.normalize().scale(huge ? 1.5 : 1.3));
            Vec3 now = new Vec3(old.x, goal != null ? goal.getY() : p.getY(), old.z);
            pos[0] = now;
            if (age % 4 == 0) b.vfx(p, VfxShape.GEL_FX1, now.add(0, 0.3 * s, 0), now.add(step.normalize().scale(2 * s)).add(0, 0.3 * s, 0), 10, 0.7f * s);
            int held = 0;
            for (LivingEntity t : GrimoireBook.around(p, now, reach)) {
                if (held >= maxHeld) break;
                held++;
                hold(t, 24, hard && held == 1);
                Vec3 toward = now.subtract(t.position()).multiply(1, 0, 1);
                if (toward.lengthSqr() > 0.2) t.setDeltaMovement(t.getDeltaMovement().add(toward.normalize().scale(BalanceLaw.isBoss(t) ? 0.02 : 0.12)));
                t.hurtMarked = true;
                if (age % 10 == 0) {
                    b.hurt(i, p, t, mode, dmg);
                    b.vfx(p, VfxShape.GEL_FX3, t.position().add(0, t.getBbHeight() / 2, 0), now, 14, 0.6f * Math.min(s, 1.6f));
                }
            }
            if (age % 10 == 0) for (LivingEntity f : friends(p, now, reach + 1)) cleanse(f);
            if (carried.size() < 16) for (ItemEntity it : p.serverLevel().getEntitiesOfClass(ItemEntity.class, new AABB(now, now).inflate(reach + 0.5), x -> x.isAlive() && !carried.contains(x)))
                if (carried.size() < 16) carried.add(it);
            for (ItemEntity it : carried) if (it.isAlive()) { it.setPos(now.x, now.y + 0.6 * s, now.z); it.setDeltaMovement(Vec3.ZERO); }
            if (last) {
                for (ItemEntity it : carried) if (it.isAlive()) it.setPos(p.getX(), p.getY() + 0.5, p.getZ());
                b.vfx(p, VfxShape.GEL_FX3, now, now.add(0, 1, 0), 28, huge ? 1.8f : 1.0f);
                over[0] = true;
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Jelly Cocoon (daily)
    static boolean cocoon(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        long now = p.level().getGameTime();
        cleanse(p);
        BalanceLaw.heal(p, p.getMaxHealth() * 0.4f);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 3));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1));
        p.getPersistentData().putLong(SHIELD, now + 160);
        p.getPersistentData().putLong(NOFALL, now + 160);
        Set<LivingEntity> shoved = new HashSet<>(GrimoireBook.around(p, p.position(), 4.5));
        for (LivingEntity t : shoved) {
            Vec3 out = t.position().subtract(p.position()).multiply(1, 0, 1);
            out = out.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : out.normalize();
            t.setDeltaMovement(t.getDeltaMovement().add(out.scale(BalanceLaw.isBoss(t) ? 0.3 : 1.0)).add(0, 0.3, 0));
            t.hurtMarked = true;
            stick(t, 80, 2);
        }
        b.castCircle(p, 1.4f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.GEL_FX2, p, p.position(), COLOR, 160, 2.6f);
        b.vfx(p, VfxShape.GEL_FX3, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 32, 1.6f);
        return true;
    }
}
