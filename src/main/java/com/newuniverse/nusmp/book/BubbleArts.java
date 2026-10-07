package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Bubble Magic: the spells of BubbleBook that need their own logic (floating traps, bursting bubbles, mines, the shield, the refresher). */
public final class BubbleArts {
    private BubbleArts() {}

    private static final int COLOR = BubbleBook.COLOR;
    private static final String SHIELD = "nusmp_bubble_shield", LAYERS = "nusmp_bubble_layers", NOFALL = "nusmp_bubble_nofall";
    private static final String[] POISONS = {"fatal_poison", "magicule_poison", "corrosion", "infection"};
    private static boolean hooked;

    /** Registers the damage hook (the bubble barrier pops one layer per blow, the fall is cushioned). Called once from BubbleProps.init(). */
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
            boolean shot = src.is(DamageTypeTags.IS_PROJECTILE);
            if (shot) e.setCanceled(true);
            else e.setAmount(e.getAmount() * 0.5f);
            if (!shot && src.getDirectEntity() instanceof LivingEntity att && att != victim) knockUp(att, 0.55);
            int left = data.getInt(LAYERS) - 1;
            data.putInt(LAYERS, left);
            if (left <= 0) data.putLong(SHIELD, 0);
            if (victim.level() instanceof ServerLevel sl) VfxSpawn.send(sl, VfxShape.BUBBLE_FX3, victim.position().add(0, 1, 0), victim.position().add(0, 1.5, 0), COLOR, 16, 0.7f);
        });
    }

    // ------------------------------------------------------------------ small helpers
    private static float k(ManasSkillInstance i, ServerPlayer p) {
        return GrimoireBook.size(i, p) * EnergyBridge.scale(p);
    }

    /** A bubble pops under the target and throws it up (bosses only hop). */
    static void knockUp(LivingEntity t, double y) {
        Vec3 v = t.getDeltaMovement();
        t.setDeltaMovement(v.x * 0.5, Math.max(v.y, BalanceLaw.isBoss(t) ? y * 0.3 : y), v.z * 0.5);
        t.hurtMarked = true;
    }

    /** Lifts the target inside a bubble: Levitation and slowed limbs (bosses are only slowed). */
    static void floatIn(LivingEntity t, int ticks) {
        if (BalanceLaw.isBoss(t)) {
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2));
            return;
        }
        t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, ticks, 0));
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 3));
        t.setSprinting(false);
    }

    /** Keeps a floating target from drifting sideways. */
    static void still(LivingEntity t) {
        Vec3 v = t.getDeltaMovement();
        t.setDeltaMovement(v.x * 0.1, v.y, v.z * 0.1);
        t.hurtMarked = true;
    }

    /** Clean water takes the poison out: vanilla poison and the Tensura poisons come off. */
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

    /** Raises the barrier on a friend: 'layers' blows (or shots) are softened, then it pops. */
    static void barrier(LivingEntity t, int ticks, int layers) {
        t.getPersistentData().putLong(SHIELD, t.level().getGameTime() + ticks);
        t.getPersistentData().putInt(LAYERS, layers);
    }

    // ------------------------------------------------------------------ Foam Blind: a cone of soap suds
    static final ElementBook.Rider FOAM = (t, p) -> {
        boolean boss = BalanceLaw.isBoss(t);
        t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, boss ? 30 : 80, 0));
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, boss ? 30 : 60, 1));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, boss ? 40 : 100, 0));
    };

    // ------------------------------------------------------------------ Bubble Trap: one target hangs in a bubble, then it pops
    static boolean trap(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "No one to trap."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 80);
        floatIn(t, ticks);
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.BUBBLE_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), 12, 1.0f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BUBBLE_FX2, t, t.position(), COLOR, ticks, Math.max(1.2f, t.getBbWidth() * 1.6f));
        SpellRuntime.zone(p.serverLevel(), ticks, 2, age -> { if (t.isAlive()) still(t); });
        SpellRuntime.later(p.serverLevel(), ticks, () -> {
            if (!t.isAlive()) return;
            t.removeEffect(MobEffects.LEVITATION);
            b.hurt(i, p, t, mode, 7f);
            b.vfx(p, VfxShape.BUBBLE_FX3, t.position().add(0, t.getBbHeight() / 2, 0), t.position().add(0, 1, 0), 22, 1.0f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Bubble Burst: bubbles swell under their feet and pop twice
    static boolean burst(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 c = GrimoireBook.aim(p, 18);
        double r = 4 * s;
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.BUBBLE_FX2, c, c.add(0, 1, 0), 16, (float) r);
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            b.hurt(i, p, t, mode, 9f);
            knockUp(t, 0.95);
        }
        b.vfx(p, VfxShape.BUBBLE_FX3, c, c.add(0, 1, 0), 26, 1.0f * s);
        SpellRuntime.later(p.serverLevel(), 12, () -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r + 1.5)) {
                if (t.onGround()) continue;
                b.hurt(i, p, t, mode, 6f);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                b.vfx(p, VfxShape.BUBBLE_FX3, t.position().add(0, 1, 0), t.position().add(0, 1.5, 0), 16, 0.6f);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Bubble Ride: a bubble carries you up and forward
    static boolean ride(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position(), look = p.getViewVector(1f);
        Vec3 flat = look.multiply(1, 0, 1);
        flat = flat.lengthSqr() < 1e-4 ? Vec3.ZERO : flat.normalize();
        float s = k(i, p);
        p.setDeltaMovement(flat.scale(1.1 * s).add(0, 0.75, 0));
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.getPersistentData().putLong(NOFALL, p.level().getGameTime() + 140);
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0));
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.BUBBLE_FX3, from, from.add(0, 1, 0), 22, 1.0f);
        b.vfx(p, VfxShape.BUBBLE_FX1, from.add(0, 1, 0), from.add(flat.scale(8)).add(0, 5, 0), 16, 0.8f);
        return true;
    }

    // ------------------------------------------------------------------ Bubble Barrier
    static boolean shield(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 200;
        barrier(p, ticks, 4);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 0));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BUBBLE_FX2, p, p.position(), COLOR, ticks, 1.8f);
        b.vfx(p, VfxShape.BUBBLE_FX3, p.position().add(0, 1, 0), p.position().add(0, 1.5, 0), 22, 1.0f);
        return true;
    }

    // ------------------------------------------------------------------ Bubble Skin
    static boolean skin(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 600;
        long now = p.level().getGameTime();
        i.getOrCreateTag().putLong("BubbleSkinUntil", now + ticks);
        p.getPersistentData().putLong(NOFALL, now + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks, 0));
        p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, ticks, 0));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 1));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BUBBLE_FX3, p, p.position().add(0, 1, 0), COLOR, 30, 1.2f);
        return true;
    }

    // ------------------------------------------------------------------ Bubble Healing Magic: Bubble Refresher
    static boolean refresher(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 20);
        Vec3 c = t != null && (t == p || t.isAlliedTo(p)) ? t.position() : p.position();
        double r = 4 * k(i, p);
        int ticks = 160;
        b.castCircle(p, 1.1f);
        b.vfx(p, VfxShape.BUBBLE_FX3, c, c.add(0, 1, 0), 26, 1.2f);
        b.vfx(p, VfxShape.BUBBLE_FX2, c, c.add(0, 1, 0), ticks, (float) r);
        for (LivingEntity f : friends(p, c, r)) BalanceLaw.heal(f, f.getMaxHealth() * 0.08f);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            for (LivingEntity f : friends(p, c, r)) {
                cleanse(f);
                f.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1));
                f.removeEffect(MobEffects.CONFUSION);
                f.removeEffect(MobEffects.BLINDNESS);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Bubble Mines: hidden bubbles that pop when someone brushes them
    static boolean mines(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 c = GrimoireBook.aim(p, 18);
        double r = 4.5 * s;
        int n = 5, ticks = 200;
        Vec3[] pts = new Vec3[n];
        boolean[] live = new boolean[n];
        double base = p.getRandom().nextDouble() * Math.PI * 2;
        b.castCircle(p, 1f);
        for (int m = 0; m < n; m++) {
            double ang = base + m * Math.PI * 2 / n, d = r * (0.35 + 0.65 * p.getRandom().nextDouble());
            pts[m] = c.add(Math.cos(ang) * d, 0.6, Math.sin(ang) * d);
            live[m] = true;
            b.vfx(p, VfxShape.BUBBLE_FX2, pts[m].add(0, -0.6, 0), pts[m], ticks, 0.9f);
        }
        SpellRuntime.zone(p.serverLevel(), ticks, 4, age -> {
            for (int m = 0; m < n; m++) {
                if (!live[m] || GrimoireBook.around(p, pts[m], 1.5).isEmpty()) continue;
                live[m] = false;
                for (LivingEntity t : GrimoireBook.around(p, pts[m], 2.4)) {
                    b.hurt(i, p, t, mode, 8f);
                    knockUp(t, 0.95);
                    if (!BalanceLaw.isBoss(t)) t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0));
                }
                b.vfx(p, VfxShape.BUBBLE_FX3, pts[m], pts[m].add(0, 1, 0), 20, 1.0f);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Great Bubble (signature): everyone inside floats, then it bursts
    static boolean greatBubble(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 c = GrimoireBook.aim(p, 20);
        double r = 6 * s;
        List<LivingEntity> caught = new ArrayList<>(GrimoireBook.around(p, c, r));
        if (caught.isEmpty()) { GrimoireBook.fail(p, "No one inside the bubble."); return false; }
        if (!GrimoireBook.control(p)) return false;
        if (caught.size() > 10) caught = new ArrayList<>(caught.subList(0, 10));
        final List<LivingEntity> held = caught;
        int ticks = 70;
        b.castCircle(p, 1.5f);
        b.vfx(p, VfxShape.BUBBLE_FX2, c, c.add(0, 1, 0), ticks, (float) r);
        int shown = 0;
        for (LivingEntity t : held) {
            floatIn(t, BalanceLaw.controlTicks(t, ticks));
            if (shown++ < 6) VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BUBBLE_FX2, t, t.position(), COLOR, ticks, Math.max(1.2f, t.getBbWidth() * 1.6f));
        }
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            for (LivingEntity t : held) if (t.isAlive()) { still(t); b.hurt(i, p, t, mode, 2.5f); }
        });
        SpellRuntime.later(p.serverLevel(), ticks, () -> {
            for (LivingEntity t : held) {
                if (!t.isAlive()) continue;
                t.removeEffect(MobEffects.LEVITATION);
                b.hurt(i, p, t, mode, 18f);
                Vec3 out = t.position().subtract(c).multiply(1, 0, 1);
                out = out.lengthSqr() < 1e-4 ? Vec3.ZERO : out.normalize();
                double power = BalanceLaw.isBoss(t) ? 0.2 : 0.9;
                t.setDeltaMovement(t.getDeltaMovement().add(out.scale(power)).add(0, 0.5 * (power > 0.5 ? 1 : 0.3), 0));
                t.hurtMarked = true;
                b.vfx(p, VfxShape.BUBBLE_FX3, t.position().add(0, t.getBbHeight() / 2, 0), c, 20, 0.8f);
            }
            b.vfx(p, VfxShape.BUBBLE_FX3, c.add(0, 1, 0), c.add(0, 2, 0), 34, 1.6f * s);
        });
        return true;
    }

    // ------------------------------------------------------------------ Rainbow Cocoon (daily)
    static boolean cocoon(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        double r = 6 * s;
        for (LivingEntity f : friends(p, p.position(), r)) {
            cleanse(f);
            BalanceLaw.heal(f, f.getMaxHealth() * 0.35f);
            f.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 2));
            f.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            barrier(f, 160, 8);
            b.vfx(p, VfxShape.BUBBLE_FX3, f.position(), f.position().add(0, 1, 0), 22, 0.8f);
        }
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0));
        p.getPersistentData().putLong(NOFALL, p.level().getGameTime() + 160);
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 4.5 * s)) {
            knockUp(t, 1.0);
            if (!BalanceLaw.isBoss(t)) t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0));
        }
        b.castCircle(p, 1.4f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BUBBLE_FX2, p, p.position(), COLOR, 160, 2.4f);
        b.vfx(p, VfxShape.BUBBLE_FX3, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 30, 1.5f);
        b.vfx(p, VfxShape.BUBBLE_FX2, p.position(), p.position().add(0, 1, 0), 40, (float) r);
        return true;
    }
}
