package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Briar Magic: the spells of BriarBook that need their own logic (rooting vines, thorn walls, the corpse-hunting tree, the queen). */
public final class BriarArts {
    private BriarArts() {}

    static final String REFLECT = "ThornDressUntil";
    static final String QUEEN = "QueenOfBriarsUntil";
    private static final BlockState THORNS = Blocks.MANGROVE_ROOTS.defaultBlockState();

    // ------------------------------------------------------------------ small helpers
    private static float k(ManasSkillInstance i, ServerPlayer p) {
        return GrimoireBook.size(i, p) * EnergyBridge.scale(p);
    }

    /** Prickle wounds: poison from the thorns and a small cut in the magicule pool (half as long on bosses). */
    static void wound(LivingEntity t, int ticks, int amp) {
        boolean boss = BalanceLaw.isBoss(t);
        t.addEffect(new MobEffectInstance(MobEffects.POISON, boss ? ticks / 2 : ticks, boss ? 0 : amp));
        Nullification.bleed(t, 0.004);
    }

    /** Vines around the legs: Slowness, the sprint stopped, the horizontal speed cut (weaker on bosses). */
    static void root(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, BalanceLaw.isBoss(t) ? Math.min(amp, 2) : amp));
        t.setSprinting(false);
        Vec3 v = t.getDeltaMovement();
        t.setDeltaMovement(v.x * 0.2, Math.min(v.y, 0.0), v.z * 0.2);
        t.hurtMarked = true;
    }

    /** Enemies within r of c (at most 16, so a crowd cannot stall a pulse). */
    static List<LivingEntity> foes(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> l = GrimoireBook.around(p, c, r);
        return l.size() > 16 ? l.subList(0, 16) : l;
    }

    /** The caster and its friends within r of c (at most 12). */
    static List<LivingEntity> friends(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> out = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e.isAlive() && !e.isSpectator() && (e == p || e.isAlliedTo(p)) && e.distanceToSqr(c) <= r * r);
        return out.size() > 12 ? out.subList(0, 12) : out;
    }

    /** A self-restoring thorn block, only where nobody stands. */
    private static boolean put(ServerLevel level, BlockPos pos, int ticks) {
        if (!level.isLoaded(pos)) return false;
        if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) return false;
        return SpellRuntime.tempBlock(level, pos, THORNS, ticks);
    }

    private static Vec3 forward(ServerPlayer p) {
        double yaw = Math.toRadians(p.getYRot());
        return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    // ------------------------------------------------------------------ Thorn Lash: a briar whip that cuts and drags
    static boolean lash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 a = p.getEyePosition(), dir = p.getViewVector(1f), end = a.add(dir.scale(8 * s));
        List<LivingEntity> hit = GrimoireBook.along(p, a, end, 0.9);
        if (hit.size() > 8) hit = hit.subList(0, 8);
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.BRIAR_FX1, a, end, 10, 1.0f);
        for (LivingEntity t : hit) {
            b.hurt(i, p, t, mode, 6f);
            wound(t, 60, 0);
            if (!BalanceLaw.isBoss(t)) {
                Vec3 pull = p.position().subtract(t.position()).normalize().scale(0.45);
                t.setDeltaMovement(t.getDeltaMovement().add(pull.x, 0.1, pull.z));
                t.hurtMarked = true;
            }
            b.vfx(p, VfxShape.BRIAR_FX3, t.position(), t.position().add(0, 1, 0), 18, 0.7f);
        }
        return true;
    }

    // ------------------------------------------------------------------ Creeping Vines: a patch of ground that grabs, twice
    static boolean vines(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        double r = 3.2 * s;
        Vec3 c = GrimoireBook.aim(p, 18);
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.BRIAR_FX1, p.getEyePosition(), c, 12, 0.9f);
        b.vfx(p, VfxShape.BRIAR_FX2, c, c.add(0, 1, 0), 70, (float) r);
        SpellRuntime.zone(p.serverLevel(), 50, 20, age -> {
            for (LivingEntity t : foes(p, c, r)) {
                b.hurt(i, p, t, mode, 3f);
                root(t, 60, 4);
                wound(t, 40, 0);
            }
            b.vfx(p, VfxShape.BRIAR_FX3, c, c.add(0, 1, 0), 18, 0.8f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Bleeding Thorn Wall: a barrier that tears at whoever leans on it
    static boolean wall(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 dir = forward(p), side = new Vec3(-dir.z, 0, dir.x), base = p.position().add(dir.scale(2.5));
        ServerLevel level = p.serverLevel();
        int placed = 0;
        for (int w = -3; w <= 3; w++) for (int h = 0; h < 3; h++)
            if (put(level, BlockPos.containing(base.add(side.scale(w)).add(0, h, 0)), 220)) placed++;
        if (placed == 0) { GrimoireBook.fail(p, "No room for the thorns."); return false; }
        Vec3 left = base.add(side.scale(-3.5)).add(0, 1, 0), right = base.add(side.scale(3.5)).add(0, 1, 0);
        for (LivingEntity t : foes(p, base, 4.5)) {
            Vec3 d = t.position().subtract(p.position()).normalize();
            t.knockback(0.8, -d.x, -d.z);
            b.hurt(i, p, t, mode, 3f);
            wound(t, 60, 0);
        }
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.BRIAR_FX2, base, base.add(0, 1, 0), 120, 3.5f);
        b.vfx(p, VfxShape.BRIAR_FX3, base, base.add(0, 1, 0), 24, 1.0f);
        SpellRuntime.zone(level, 200, 10, age -> {
            for (LivingEntity t : GrimoireBook.along(p, left, right, 1.7)) {
                b.hurt(i, p, t, mode, 1.5f);
                wound(t, 40, 0);
                Nullification.bleed(t, 0.004);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Rose Scent: the perfume that inhibits the senses
    static boolean scent(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 7 * k(i, p);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.BRIAR_FX2, p.position(), p.position().add(0, 1, 0), 60, (float) r);
        b.vfx(p, VfxShape.BRIAR_FX3, p.position(), p.position().add(0, 1, 0), 24, 1.0f);
        for (LivingEntity t : foes(p, p.position(), r)) {
            boolean boss = BalanceLaw.isBoss(t);
            t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, boss ? 30 : 80, 0));
            t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, boss ? 40 : 100, 0));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, boss ? 60 : 120, 0));
            EnergyBridge.effect(t, "silence", boss ? 40 : 100, 0);
        }
        return true;
    }

    // ------------------------------------------------------------------ Thorn Reflect: a dress of briars that bites whoever strikes
    static boolean dress(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        i.getOrCreateTag().putLong(REFLECT, p.level().getGameTime() + 400);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 0));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BRIAR_FX3, p, p.position().add(0, 1, 0), b.color, 40, 1.0f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BRIAR_FX2, p, p.position().add(0, 1, 0), b.color, 400, 1.6f);
        return true;
    }

    // ------------------------------------------------------------------ Briar Leap: a vine thrown at the aim point that hauls you there
    static boolean leap(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position(), to = GrimoireBook.aim(p, 22);
        Vec3 d = to.subtract(from);
        double dist = d.length();
        if (dist < 3) { GrimoireBook.fail(p, "Too close to throw a vine."); return false; }
        Vec3 flat = new Vec3(d.x, 0, d.z).normalize();
        double sp = Math.min(1.9, 0.5 + dist * 0.09);
        p.setDeltaMovement(flat.x * sp, 0.55 + Math.min(0.3, d.y * 0.02), flat.z * sp);
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 50, 0));
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.BRIAR_FX1, from.add(0, 1, 0), to, 12, 1.0f);
        b.vfx(p, VfxShape.BRIAR_FX3, from, from.add(0, 1, 0), 20, 0.8f);
        for (LivingEntity t : foes(p, from, 3)) { b.hurt(i, p, t, mode, 4f); root(t, 40, 2); }
        SpellRuntime.later(p.serverLevel(), 16, () -> {
            if (!p.isAlive()) return;
            Vec3 at = p.position();
            b.vfx(p, VfxShape.BRIAR_FX3, at, at.add(0, 1, 0), 22, 1.0f);
            b.vfx(p, VfxShape.BRIAR_FX2, at, at.add(0, 1, 0), 30, 3f);
            for (LivingEntity t : foes(p, at, 3.2)) { b.hurt(i, p, t, mode, 5f); wound(t, 50, 0); root(t, 30, 2); }
        });
        return true;
    }

    // ------------------------------------------------------------------ Briar Garden: a patch of wild roses that cuts and drags
    static boolean garden(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 5.5 * k(i, p);
        Vec3 c = GrimoireBook.aim(p, 20);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.BRIAR_FX1, p.getEyePosition(), c, 14, 1.0f);
        b.vfx(p, VfxShape.BRIAR_FX2, c, c.add(0, 1, 0), 200, (float) r);
        b.vfx(p, VfxShape.BRIAR_FX3, c, c.add(0, 1, 0), 28, 1.0f);
        SpellRuntime.zone(p.serverLevel(), 200, 10, age -> {
            for (LivingEntity t : foes(p, c, r)) {
                b.hurt(i, p, t, mode, 1.5f);
                wound(t, 30, 0);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24, 2));
                if (age % 20 == 0) Nullification.bleed(t, 0.006);
            }
            if (age > 0 && age % 50 == 0) b.vfx(p, VfxShape.BRIAR_FX3, c, c.add(0, 1, 0), 20, 1.0f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Corpse-Hunting Briar Tree: vines hunt, converge and rise as a tree
    static boolean tree(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        List<LivingEntity> prey = new ArrayList<>(GrimoireBook.around(p, p.position(), 20 * s));
        prey.sort(Comparator.comparingDouble(t -> t.distanceToSqr(p)));
        if (prey.isEmpty()) { GrimoireBook.fail(p, "The vines find nothing to hunt."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int n = Math.min(prey.size(), 3 + Math.round(s));
        Vec3 start = p.getEyePosition();
        b.castCircle(p, 1.3f);
        for (int k = 0; k < n; k++) {
            LivingEntity t = prey.get(k);
            AtomicBoolean caught = new AtomicBoolean(false);
            Vec3 dir = t.getBoundingBox().getCenter().subtract(start).normalize();
            b.vfx(p, VfxShape.BRIAR_FX1, start, t.getBoundingBox().getCenter(), 16, 1.0f);
            SpellRuntime.bolt(p, start, dir.scale(1.3), 0.7, 30, false, t, (bolt, hit) -> {
                if (!caught.compareAndSet(false, true)) return;
                trap(b, i, p, mode, hit, s);
            }, (bolt, at) -> {});
        }
        return true;
    }

    /** The vines have caught 'hit': it is held, then the tree rises through it. */
    private static void trap(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, LivingEntity hit, float s) {
        ServerLevel level = p.serverLevel();
        int hold = BalanceLaw.controlTicks(hit, 60);
        root(hit, hold, 9);
        b.vfx(p, VfxShape.BRIAR_FX2, hit.position(), hit.position().add(0, 1, 0), 60, 1.8f);
        SpellRuntime.zone(level, 20, 4, age -> { if (hit.isAlive()) root(hit, 10, 9); });
        SpellRuntime.later(level, 22, () -> {
            if (!hit.isAlive()) return;
            BlockPos g = hit.blockPosition();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                for (int y = 0; y < 4; y++) put(level, g.offset(dx, y, dz), 100);
            }
            b.vfx(p, VfxShape.BRIAR_FX3, hit.position(), hit.position().add(0, 2, 0), 34, 1.3f);
            b.hurt(i, p, hit, mode, 14f * Math.min(1.4f, s));
            wound(hit, 100, 1);
            if (!BalanceLaw.isBoss(hit)) { hit.setDeltaMovement(hit.getDeltaMovement().add(0, 0.7, 0)); hit.hurtMarked = true; }
            SpellRuntime.zone(level, 50, 10, age -> {
                if (!hit.isAlive()) return;
                b.hurt(i, p, hit, mode, 2.5f);
                wound(hit, 30, 1);
                Nullification.bleed(hit, 0.006);
                root(hit, 12, 5);
            });
        });
    }

    // ------------------------------------------------------------------ Queen of Briars: the dress and crown, the red whip, the blue rose bind
    static boolean queen(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        double r = 8 * s;
        List<LivingEntity> foes = foes(p, p.position(), r);
        if (!foes.isEmpty() && !GrimoireBook.control(p)) return false;
        i.getOrCreateTag().putLong(QUEEN, p.level().getGameTime() + 1200);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 0));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
        b.castCircle(p, 1.6f);
        b.vfx(p, VfxShape.BRIAR_FX3, p.position(), p.position().add(0, 2, 0), 40, 1.5f);
        b.vfx(p, VfxShape.BRIAR_FX2, p.position(), p.position().add(0, 1, 0), 60, (float) r);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BRIAR_FX2, p, p.position().add(0, 1, 0), b.color, 1200, 2.0f);
        LivingEntity strongest = null;
        for (LivingEntity t : foes) if (strongest == null || EnergyBridge.power(t) > EnergyBridge.power(strongest)) strongest = t;
        for (LivingEntity t : foes) {
            b.vfx(p, VfxShape.BRIAR_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), 12, 1.0f);
            if (t == strongest) {
                // blue roses: the stronger the victim, the tighter the vines
                int amp = (int) Math.min(8, 2 + Math.round(EnergyBridge.power(t) * 2));
                int ticks = BalanceLaw.controlTicks(t, 120);
                b.hurt(i, p, t, mode, 6f);
                root(t, ticks, amp);
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
                EnergyBridge.drain(t, p, 0.02);
            } else {
                // red roses: the whip lashes
                b.hurt(i, p, t, mode, 8f);
                wound(t, 100, 1);
                Nullification.bleed(t, 0.008);
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ Rose Garden Bloom (daily): the ground becomes a briar field that feeds its owner's side
    static boolean bloom(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 9 * k(i, p);
        Vec3 c = p.position();
        b.castCircle(p, 1.5f);
        b.vfx(p, VfxShape.BRIAR_FX2, c, c.add(0, 1, 0), 400, (float) r);
        b.vfx(p, VfxShape.BRIAR_FX3, c, c.add(0, 2, 0), 36, 1.4f);
        SpellRuntime.zone(p.serverLevel(), 400, 20, age -> {
            for (LivingEntity t : foes(p, c, r)) {
                b.hurt(i, p, t, mode, 3f);
                wound(t, 50, 1);
                root(t, 30, 2);
                EnergyBridge.effect(t, "silence", 40, 0);
                EnergyBridge.drain(t, p, 0.01);
            }
            for (LivingEntity f : friends(p, c, r)) BalanceLaw.heal(f, f.getMaxHealth() * 0.03f);
            if (age % 80 == 0) b.vfx(p, VfxShape.BRIAR_FX3, c, c.add(0, 1, 0), 24, 1.0f);
        });
        return true;
    }
}
