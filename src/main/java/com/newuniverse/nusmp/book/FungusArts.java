package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Fungus Magic: the spells of FungusBook that need their own logic (spore clouds, roots, the heavy and towering mushrooms). */
public final class FungusArts {
    private FungusArts() {}

    static final String ARMOR = "SporeArmorUntil";
    private static final BlockState STEM = Blocks.MUSHROOM_STEM.defaultBlockState();
    private static final BlockState BROWN = Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState();
    private static final BlockState RED = Blocks.RED_MUSHROOM_BLOCK.defaultBlockState();

    // ------------------------------------------------------------------ small helpers
    private static float k(ManasSkillInstance i, ServerPlayer p) {
        return GrimoireBook.size(i, p) * EnergyBridge.scale(p);
    }

    /** Spores in the lungs: poison and weakness (half as long on bosses, no amplifier). */
    static void infect(LivingEntity t, int ticks, int amp) {
        boolean boss = BalanceLaw.isBoss(t);
        int d = boss ? ticks / 2 : ticks;
        t.addEffect(new MobEffectInstance(MobEffects.POISON, d, boss ? 0 : amp));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, d, boss ? 0 : Math.min(amp, 1)));
        EnergyBridge.effect(t, "magicule_poison", d, 0);
    }

    /** Mycelium around the legs: Slowness, the sprint stopped, the horizontal speed cut (weaker on bosses). */
    static void root(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, BalanceLaw.isBoss(t) ? Math.min(amp, 2) : amp));
        t.setSprinting(false);
        Vec3 v = t.getDeltaMovement();
        t.setDeltaMovement(v.x * 0.2, Math.min(v.y, 0.0), v.z * 0.2);
        t.hurtMarked = true;
    }

    /** The fungus feeds: takes frac of the target's magicules and gives them to the caster. */
    static void siphon(ServerPlayer p, LivingEntity t, double frac) {
        EnergyBridge.drain(t, p, frac);
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

    /** A self-restoring mushroom block, only where nobody stands. */
    private static boolean put(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        if (!level.isLoaded(pos)) return false;
        if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) return false;
        return SpellRuntime.tempBlock(level, pos, state, ticks);
    }

    /** The air block standing on solid ground nearest to the aim point, or null. */
    private static BlockPos ground(ServerLevel level, Vec3 at) {
        BlockPos q = BlockPos.containing(at);
        if (!level.isLoaded(q)) return null;
        for (int up = 0; up < 3 && !level.getBlockState(q).isAir(); up++) q = q.above();
        for (int down = 0; down < 14 && level.getBlockState(q.below()).isAir(); down++) q = q.below();
        return level.getBlockState(q).isAir() && !level.getBlockState(q.below()).isAir() ? q : null;
    }

    private static Vec3 forward(ServerPlayer p) {
        double yaw = Math.toRadians(p.getYRot());
        return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    // ------------------------------------------------------------------ Spore Burst: a pod that bursts into a small cloud
    static boolean sporeBurst(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.FUNGUS_FX1, start, start.add(dir.scale(1.3 * 18)), 18, 1.0f);
        SpellRuntime.bolt(p, start, dir.scale(1.3), 0.5, 18, false, null, (bolt, t) -> {}, (bolt, at) -> {
            double r = 2.6 * s;
            b.vfx(p, VfxShape.FUNGUS_FX3, at, at.add(0, 1, 0), 24, 1.0f);
            b.vfx(p, VfxShape.FUNGUS_FX2, at, at.add(0, 1, 0), 60, (float) r);
            for (LivingEntity t : foes(p, at, r)) { b.hurt(i, p, t, mode, 5f); infect(t, 100, 0); }
            SpellRuntime.zone(p.serverLevel(), 60, 20, age -> {
                if (age == 0) return;
                for (LivingEntity t : foes(p, at, r)) { infect(t, 40, 0); siphon(p, t, 0.005); }
            });
        });
        return true;
    }

    // ------------------------------------------------------------------ Mycelium Roots: a row of roots that grips the legs
    static boolean roots(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 dir = forward(p), start = p.position(), end = start.add(dir.scale(10 * s));
        List<LivingEntity> hit = GrimoireBook.along(p, start.add(0, 0.5, 0), end.add(0, 0.5, 0), 1.2);
        if (hit.size() > 16) hit = hit.subList(0, 16);
        List<LivingEntity> held = List.copyOf(hit);
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.FUNGUS_FX1, start.add(0, 0.4, 0), end.add(0, 0.4, 0), 14, 1.0f);
        for (double f : new double[]{0.25, 0.55, 0.85}) {
            Vec3 at = start.add(end.subtract(start).scale(f));
            b.vfx(p, VfxShape.FUNGUS_FX2, at, at.add(0, 1, 0), 70, 1.6f);
        }
        for (LivingEntity t : held) { b.hurt(i, p, t, mode, 3f); root(t, 80, 3); }
        SpellRuntime.zone(p.serverLevel(), 60, 5, age -> {
            for (LivingEntity t : held) if (t.isAlive()) root(t, 20, 3);
        });
        return true;
    }

    // ------------------------------------------------------------------ Heavy Mr. Mushroom: spores on the target, a mushroom that grows heavier until it is pinned
    static boolean heavy(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 18);
        if (t == null || t.isAlliedTo(p)) { GrimoireBook.fail(p, "No one to put spores on."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 90);
        ServerLevel level = p.serverLevel();
        infect(t, 60, 0);
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.FUNGUS_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), 14, 0.8f);
        b.vfx(p, VfxShape.FUNGUS_FX3, t.position(), t.position().add(0, 1, 0), 20, 0.7f);
        SpellRuntime.later(level, 20, () -> {
            if (!t.isAlive()) return;
            b.vfx(p, VfxShape.FUNGUS_FX2, t.position(), t.position().add(0, 1, 0), ticks, 1.6f);
            b.vfx(p, VfxShape.FUNGUS_FX3, t.position(), t.position().add(0, 1, 0), 28, 1.0f);
            t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 2));
            SpellRuntime.zone(level, ticks, 5, age -> {
                if (!t.isAlive()) return;
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, Math.min(9, 1 + age / 5)));
                Vec3 v = t.getDeltaMovement();
                t.setDeltaMovement(v.x * 0.5, t.onGround() ? v.y : Math.min(v.y, 0.0) - 0.1, v.z * 0.5);
                t.hurtMarked = true;
            });
            SpellRuntime.later(level, ticks, () -> {
                if (!t.isAlive()) return;
                b.hurt(i, p, t, mode, 12f);
                b.vfx(p, VfxShape.FUNGUS_FX3, t.position(), t.position().add(0, 1, 0), 24, 1.2f);
            });
        });
        return true;
    }

    // ------------------------------------------------------------------ Running Mr. Mushroom: a sprint on a mycelium trail
    static boolean running(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position();
        BookPage.Cast dash = ElementBook.dash(5f, 9, false, VfxShape.FUNGUS_FX1, (t, pl) -> { infect(t, 60, 0); root(t, 40, 2); });
        if (!dash.cast(b, i, p, mode)) return false;
        Vec3 to = p.position();
        Vec3 mid = from.add(to.subtract(from).scale(0.5));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1));
        b.vfx(p, VfxShape.FUNGUS_FX2, mid, mid.add(0, 1, 0), 60, (float) Math.max(1.5, from.distanceTo(to) / 2));
        b.vfx(p, VfxShape.FUNGUS_FX3, to, to.add(0, 1, 0), 20, 0.8f);
        SpellRuntime.zone(p.serverLevel(), 60, 10, age -> {
            for (LivingEntity t : GrimoireBook.along(p, from.add(0, 0.5, 0), to.add(0, 0.5, 0), 1.3)) { infect(t, 30, 0); root(t, 20, 1); }
        });
        return true;
    }

    // ------------------------------------------------------------------ Talking Mr. Mushroom: the mushroom tells where everyone is and where they are soft
    static boolean talking(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 22 * k(i, p);
        List<LivingEntity> all = foes(p, p.position(), r);
        if (all.isEmpty()) { GrimoireBook.fail(p, "The mushroom has nothing to say."); return false; }
        double nearest = r;
        for (LivingEntity t : all) {
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
            EnergyBridge.effect(t, "fragility", BalanceLaw.isBoss(t) ? 40 : 100, 0);
            nearest = Math.min(nearest, t.distanceTo(p));
        }
        p.displayClientMessage(Component.literal("The mushroom whispers: " + all.size() + " nearby, the closest " + (int) nearest + " blocks away.")
                .withStyle(ChatFormatting.GOLD), true);
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.FUNGUS_FX2, p.position(), p.position().add(0, 1, 0), 40, (float) r);
        b.vfx(p, VfxShape.FUNGUS_FX3, p.position(), p.position().add(0, 1, 0), 24, 0.8f);
        return true;
    }

    // ------------------------------------------------------------------ Spore Armor: a coat of spores that poisons whoever strikes you
    static boolean armor(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        i.getOrCreateTag().putLong(ARMOR, p.level().getGameTime() + 400);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 0));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.FUNGUS_FX3, p, p.position().add(0, 1, 0), b.color, 40, 1.0f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.FUNGUS_FX2, p, p.position().add(0, 1, 0), b.color, 400, 1.8f);
        return true;
    }

    // ------------------------------------------------------------------ Mushroom Wall: a row of giant mushrooms that spits spores at those who press it
    static boolean wall(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 dir = forward(p), side = new Vec3(-dir.z, 0, dir.x), base = p.position().add(dir.scale(2.5));
        ServerLevel level = p.serverLevel();
        int placed = 0;
        for (int w = -2; w <= 2; w++) for (int h = 0; h < 3; h++) {
            BlockPos pos = BlockPos.containing(base.add(side.scale(w)).add(0, h, 0));
            if (put(level, pos, h == 0 ? STEM : h == 1 ? BROWN : RED, 200)) placed++;
        }
        if (placed == 0) { GrimoireBook.fail(p, "No room for the mushrooms."); return false; }
        for (LivingEntity t : foes(p, base, 3.5)) {
            Vec3 d = t.position().subtract(p.position()).normalize();
            t.knockback(0.8, -d.x, -d.z);
            infect(t, 60, 0);
        }
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.FUNGUS_FX2, base, base.add(0, 1, 0), 60, 2.5f);
        b.vfx(p, VfxShape.FUNGUS_FX3, base, base.add(0, 1, 0), 24, 1.0f);
        return true;
    }

    // ------------------------------------------------------------------ Mycelium Mending: the mycelium knits wounds and draws the poison out
    static boolean mending(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 7 * k(i, p);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.FUNGUS_FX2, p.position(), p.position().add(0, 1, 0), 40, (float) r);
        for (LivingEntity f : friends(p, p.position(), r)) {
            BalanceLaw.heal(f, f.getMaxHealth() * 0.18f);
            f.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            f.removeEffect(MobEffects.POISON);
            b.vfx(p, VfxShape.FUNGUS_FX3, f.position(), f.position().add(0, 1, 0), 20, 0.6f);
        }
        return true;
    }

    // ------------------------------------------------------------------ Spore Cloud: a dense cloud that poisons, weakens and feeds on magicules
    static boolean cloud(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 5 * k(i, p);
        Vec3 c = GrimoireBook.aim(p, 20);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.FUNGUS_FX1, p.getEyePosition(), c, 14, 1.0f);
        b.vfx(p, VfxShape.FUNGUS_FX2, c, c.add(0, 1, 0), 160, (float) r);
        b.vfx(p, VfxShape.FUNGUS_FX3, c, c.add(0, 1, 0), 28, 1.0f);
        SpellRuntime.zone(p.serverLevel(), 160, 10, age -> {
            boolean feed = age % 20 == 0;
            for (LivingEntity t : foes(p, c, r)) {
                b.hurt(i, p, t, mode, 1.5f);
                infect(t, 40, 1);
                if (feed) siphon(p, t, 0.01);
            }
            if (age > 0 && age % 40 == 0) b.vfx(p, VfxShape.FUNGUS_FX3, c, c.add(0, 1, 0), 20, 1.0f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Towering Mr. Mushroom: a giant mushroom with arms that grows from the ground and slams
    static boolean tower(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        ServerLevel level = p.serverLevel();
        BlockPos g = ground(level, GrimoireBook.aim(p, 24));
        if (g == null) { GrimoireBook.fail(p, "No ground to grow from."); return false; }
        Vec3 c = Vec3.atBottomCenterOf(g);
        double rise = 3.5 * s, slam = 4.5 * s;
        b.castCircle(p, 1.3f);
        b.vfx(p, VfxShape.FUNGUS_FX1, p.getEyePosition(), c.add(0, 1, 0), 12, 1.0f);
        b.vfx(p, VfxShape.FUNGUS_FX2, c, c.add(0, 1, 0), 24, (float) rise);
        SpellRuntime.later(level, 24, () -> {
            b.vfx(p, VfxShape.FUNGUS_FX3, c, c.add(0, 2, 0), 36, 1.3f);
            b.vfx(p, VfxShape.FUNGUS_FX2, c, c.add(0, 1, 0), 120, (float) slam);
            for (LivingEntity t : foes(p, c, rise)) {
                b.hurt(i, p, t, mode, 16f);
                infect(t, 100, 1);
                root(t, 60, 2);
                if (!BalanceLaw.isBoss(t)) { t.setDeltaMovement(t.getDeltaMovement().add(0, 1.0, 0)); t.hurtMarked = true; }
            }
            SpellRuntime.later(level, 6, () -> {
                for (int y = 0; y < 5; y++) put(level, g.above(y), STEM, 140);
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) put(level, g.offset(x, 5, z), BROWN, 140);
            });
            SpellRuntime.zone(level, 120, 20, age -> {
                for (LivingEntity t : foes(p, c, slam)) {
                    b.hurt(i, p, t, mode, 6f);
                    Vec3 d = t.position().subtract(c).normalize();
                    t.knockback(1.0, -d.x, -d.z);
                    infect(t, 60, 1);
                    siphon(p, t, 0.01);
                }
                b.vfx(p, VfxShape.FUNGUS_FX3, c, c.add(0, 1, 0), 18, 1.0f);
            });
        });
        return true;
    }

    // ------------------------------------------------------------------ Mycelium Bloom (daily): the ground itself turns to a fungal forest
    static boolean bloom(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 9 * k(i, p);
        Vec3 c = p.position();
        b.castCircle(p, 1.5f);
        b.vfx(p, VfxShape.FUNGUS_FX2, c, c.add(0, 1, 0), 400, (float) r);
        b.vfx(p, VfxShape.FUNGUS_FX3, c, c.add(0, 2, 0), 36, 1.4f);
        SpellRuntime.zone(p.serverLevel(), 400, 20, age -> {
            for (LivingEntity t : foes(p, c, r)) {
                b.hurt(i, p, t, mode, 3f);
                infect(t, 50, 1);
                root(t, 30, 2);
                siphon(p, t, 0.02);
            }
            for (LivingEntity f : friends(p, c, r)) BalanceLaw.heal(f, f.getMaxHealth() * 0.03f);
            if (age % 80 == 0) b.vfx(p, VfxShape.FUNGUS_FX3, c, c.add(0, 1, 0), 24, 1.0f);
        });
        return true;
    }
}
