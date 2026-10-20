package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import com.newuniverse.nusmp.particle.fx.FxRelic;

/**
 * Why: the 1.70.0 relic waves (31 and up) are generated from tables ({@code tools/relics/relic_rows_*.py}, written to Java by {@code gen_relic_waves.py}), and a table of hundreds of relics can only
 * describe an active ability as a choice from a small, well-tested vocabulary, not as hundreds of hand-written bodies. This class is that vocabulary: each method is one ability shape
 * (heal, regeneration, an effect on the wearer, a shield, an area hit with an effect, a bolt at the foe in front, a dash, a blink, a lent Tensura skill, a cleanse, an effect on allies, marking
 * foes, a second wind, a life drain, a gale and a pull) built only on {@link Fx}, the relic core's own helpers, so every ability inherits what the core already guarantees: damage goes through the
 * balance law and the caps on players, foes are only hostile non-allied creatures, effects and radii are clamped, and the engine has paid the cost and started the cooldown before the body runs.
 * A relic's flavour is the table's name and text for it; its numbers are the table's. Nothing here keeps state.
 */
public final class Wave31Acts {
    private Wave31Acts() {}

    public static void say(ActCtx c, String text) { Fx.say(c.player(), text); }

    public static void heal(ActCtx c, float amount) { Fx.heal(c.player(), amount); }

    public static void regen(ActCtx c, int seconds, int amp) { Fx.addEffect(c.player(), "minecraft:regeneration", seconds * 20, amp); }

    public static void buff(ActCtx c, String effectId, int seconds, int amp) { Fx.addEffect(c.player(), effectId, seconds * 20, amp); }

    public static void shield(ActCtx c, float absorb, int seconds) { Fx.shield(c.player(), absorb, seconds * 20); }

    public static void aoe(ActCtx c, double radius, float damage, String type, String effectId, int seconds, int amp) {
        ServerPlayer p = c.player();
        FxRelic.ring(p.serverLevel(), p.position(), Math.min(radius, 12.0), c.def().paletteGlow());
        Fx.aoe(p, radius, damage, type, effectId, seconds * 20, amp);
    }

    /** The foe nearest to the look direction within {@code range} (a narrow cone), or null. */
    private static LivingEntity inFront(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        LivingEntity best = null;
        double bestDot = 0.93;
        for (LivingEntity t : Fx.foesAround(p, range)) {
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            if (to.lengthSqr() < 1.0e-4) continue;
            double dot = to.normalize().dot(look);
            if (dot > bestDot) {
                bestDot = dot;
                best = t;
            }
        }
        return best;
    }

    public static void bolt(ActCtx c, double range, float damage, String type, String effectId, int seconds, int amp) {
        ServerPlayer p = c.player();
        LivingEntity t = inFront(p, range);
        if (t == null) {
            Fx.say(p, "Nothing in front of you to strike.");
            return;
        }
        Vec3 a = p.getEyePosition(), b = t.getBoundingBox().getCenter();
        for (int i = 0; i <= 10; i++) {
            Vec3 at = a.lerp(b, i / 10.0);
            FxRelic.burst(p.serverLevel(), at, c.def().paletteGlow());
        }
        Fx.damage(p, t, damage, type);
        if (effectId != null) Fx.addEffect(t, effectId, seconds * 20, amp);
    }

    public static void dash(ActCtx c, double blocks) { Fx.dash(c.player(), blocks); }

    public static void blink(ActCtx c, double range) {
        if (!Fx.teleportLook(c.player(), range)) Fx.say(c.player(), "There is no safe place to step to.");
    }

    public static void lend(ActCtx c, String skillId, int seconds) { Fx.lendSkill(c.player(), skillId, seconds * 20); }

    /** Takes every harmful effect off the wearer. */
    public static void cleanse(ActCtx c) {
        ServerPlayer p = c.player();
        for (MobEffectInstance i : new ArrayList<>(p.getActiveEffects())) {
            MobEffect e = i.getEffect().value();
            if (e.getCategory() == MobEffectCategory.HARMFUL) p.removeEffect(i.getEffect());
        }
        p.setTicksFrozen(0);
        p.clearFire();
    }

    /** An effect on the wearer and every player within the radius (the caster's party, in the sense of the game: anyone who can stand beside them). */
    public static void allies(ActCtx c, double radius, String effectId, int seconds, int amp) {
        ServerPlayer p = c.player();
        Fx.addEffect(p, effectId, seconds * 20, amp);
        for (Player o : p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(Math.min(radius, 24.0)), o -> o != p && o.isAlive() && !o.isSpectator() && o.isAlliedTo(p))) {
            Fx.addEffect(o, effectId, seconds * 20, amp);
        }
    }

    /** Makes every foe within the radius glow (so it can be seen through walls) for a while. */
    public static void mark(ActCtx c, double radius, int seconds) {
        for (LivingEntity t : Fx.foesAround(c.player(), radius)) Fx.addEffect(t, "minecraft:glowing", seconds * 20, 0);
    }

    /** A second wind: the wearer is healed {@code fraction} of the health they are missing and resists for a moment. */
    public static void secondWind(ActCtx c, double fraction, int resistSeconds) {
        ServerPlayer p = c.player();
        float missing = p.getMaxHealth() - p.getHealth();
        Fx.heal(p, (float) (missing * Math.max(0.0, Math.min(1.0, fraction))));
        Fx.addEffect(p, "minecraft:resistance", resistSeconds * 20, 1);
    }

    /** Hurts the foes around the wearer and returns a share of the damage dealt to them as healing. */
    public static void drain(ActCtx c, double radius, float damage, String type) {
        ServerPlayer p = c.player();
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, radius)) {
            Fx.damage(p, t, damage, type);
            n++;
        }
        if (n > 0) Fx.heal(p, Math.min(20f, damage * 0.4f * n));
        FxRelic.ring(p.serverLevel(), p.position(), Math.min(radius, 12.0), c.def().paletteGlow());
    }

    /** Throws every foe within the radius away from the wearer. */
    public static void gale(ActCtx c, double radius, double strength) {
        ServerPlayer p = c.player();
        for (LivingEntity t : Fx.foesAround(p, radius)) {
            Vec3 d = t.position().subtract(p.position());
            double len = Math.max(0.5, Math.hypot(d.x, d.z));
            t.setDeltaMovement(t.getDeltaMovement().add(d.x / len * strength, 0.3, d.z / len * strength));
            t.hurtMarked = true;
        }
        FxRelic.ring(p.serverLevel(), p.position(), Math.min(radius, 12.0), c.def().paletteGlow());
    }

    /** Drags every foe within the radius towards the wearer. */
    public static void pull(ActCtx c, double radius, double strength) {
        ServerPlayer p = c.player();
        for (LivingEntity t : Fx.foesAround(p, radius)) {
            Vec3 d = p.position().subtract(t.position());
            double len = Math.max(0.5, Math.hypot(d.x, d.z));
            t.setDeltaMovement(t.getDeltaMovement().add(d.x / len * strength, 0.2, d.z / len * strength));
            t.hurtMarked = true;
        }
        FxRelic.ring(p.serverLevel(), p.position(), Math.min(radius, 12.0), c.def().paletteGlow());
    }

    /** Sets the foes around the wearer alight for a number of seconds. */
    public static void ignite(ActCtx c, double radius, int seconds) {
        for (LivingEntity t : Fx.foesAround(c.player(), radius)) t.setRemainingFireTicks(seconds * 20);
    }

    /** Whether an id names a registered mob effect (the generator's tests use it through the lang check). */
    public static boolean knowsEffect(String id) {
        var rl = net.minecraft.resources.ResourceLocation.tryParse(id);
        return rl != null && BuiltInRegistries.MOB_EFFECT.containsKey(rl);
    }
}
