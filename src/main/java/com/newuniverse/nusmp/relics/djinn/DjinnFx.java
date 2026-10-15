package com.newuniverse.nusmp.relics.djinn;

import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.function.Consumer;

/**
 * Small server-side helpers of the Djinn system: the particle, damage type and Tensura boost an affinity maps to, and bounded particle shapes.
 * Everything a shape sends is capped, because a telegraph is drawn every few ticks for three seconds.
 */
final class DjinnFx {
    private DjinnFx() {}

    /** The Tensura element boost that fits an affinity (Life and Strength have none), or null. */
    static Holder<Attribute> boost(Affinity a) {
        return switch (a) {
            case HEAT -> TensuraAttributes.FLAME_BOOST;
            case LIGHTNING -> TensuraAttributes.LIGHTNING_BOOST;
            case WATER -> TensuraAttributes.WATER_BOOST;
            case WIND -> TensuraAttributes.WIND_BOOST;
            case SOUND -> TensuraAttributes.SOUND_BOOST;
            case LIGHT -> TensuraAttributes.LIGHT_BOOST;
            default -> null;
        };
    }

    /** The damage type id a Djinn's spells hit with (unknown ids become a plain attack inside Fx). */
    static String damageType(Affinity a) {
        return switch (a) {
            case HEAT -> "minecraft:in_fire";
            case LIGHTNING -> "minecraft:lightning_bolt";
            case SOUND -> "minecraft:sonic_boom";
            case STRENGTH -> "minecraft:player_attack";
            default -> "minecraft:magic";
        };
    }

    /** Every affinity is a Light Rukh (the djinn are spirits of the elements); the dark djinn colours go through {@link #dust}. */
    static com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment align(Affinity a) {
        return com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT;
    }

    static ParticleOptions particle(Affinity a) {
        return com.newuniverse.nusmp.particle.rukh.RukhParticles.of(align(a));
    }

    static net.minecraft.core.particles.SimpleParticleType dust(int rgb, float size) {
        return com.newuniverse.nusmp.relics.Fx.dust(rgb, size);
    }

    /** A ring of dust lying on the ground: {@code points} particles (clamped to 8..96). */
    static void ring(ServerLevel l, Vec3 at, double radius, int rgb, int points, double lift) {
        int n = Mth.clamp(points, 8, 96);
        net.minecraft.core.particles.ParticleOptions d = dust(rgb, 1.3f);
        for (int i = 0; i < n; i++) {
            double a = i * (Math.PI * 2 / n);
            com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.Fx.alignOf(d), at.x + Math.cos(a) * radius, at.y + lift, at.z + Math.sin(a) * radius, 1, 0, 0.01, 0, 0);
        }
    }

    /** A vertical column of particles from y0 up to y1: at most 40 of them. */
    static void column(ServerLevel l, double x, double y0, double y1, double z, ParticleOptions o, int count) {
        int n = Mth.clamp(count, 2, 40);
        for (int i = 0; i < n; i++) com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.Fx.alignOf(o), x, y0 + (y1 - y0) * i / (n - 1), z, 1, 0.05, 0, 0.05, 0);
    }

    /** A line of particles between two points: at most 40 of them. */
    static void line(ServerLevel l, Vec3 a, Vec3 b, ParticleOptions o, int count) {
        int n = Mth.clamp(count, 2, 40);
        for (int i = 0; i < n; i++) {
            double t = i / (double) (n - 1);
            com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.Fx.alignOf(o), Mth.lerp(t, a.x, b.x), Mth.lerp(t, a.y, b.y), Mth.lerp(t, a.z, b.z), 1, 0, 0, 0, 0);
        }
    }

    /** A mob effect by id, or a fire for the pseudo id "nusmp:burn" (ticks/20 seconds). "" does nothing. */
    static void effect(LivingEntity t, String id, int ticks, int amp) {
        if (id == null || id.isEmpty() || ticks <= 0 || t == null || !t.isAlive()) return;
        if (id.equals("nusmp:burn")) {
            t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), Math.min(ticks, 200)));
            return;
        }
        com.newuniverse.nusmp.relics.Fx.addEffect(t, id, ticks, amp);
    }

    /** Runs {@code each} for every real player within {@code radius} of {@code p} (and p), for titles and action bars. */
    static void nearby(ServerPlayer p, double radius, Consumer<ServerPlayer> each) {
        double r2 = radius * radius;
        for (ServerPlayer o : p.serverLevel().players()) if (o == p || o.distanceToSqr(p) <= r2) each.accept(o);
    }

    static void bar(ServerPlayer p, double radius, String text, int rgb) {
        Component c = Component.literal(text).withColor(rgb & 0xFFFFFF);
        nearby(p, radius, o -> o.displayClientMessage(c, true));
    }
}
