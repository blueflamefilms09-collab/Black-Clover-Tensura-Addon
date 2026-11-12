package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.particle.fx.FxSlot;
import com.newuniverse.nusmp.particle.fx.FxTone;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import com.newuniverse.nusmp.particle.fx.TintedParticleOptions;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
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

    // ---------------------------------------------------------------- 3.75.0: each element in its own look (DjinnLook); typedVfx = false gives the Rukh back
    private static final ThemeKey[] KEYS = new ThemeKey[Affinity.values().length];
    private static final VfxShape[][] SHAPES = new VfxShape[Affinity.values().length][DjinnLook.Moment.values().length];

    static {
        for (Affinity a : Affinity.values()) {
            KEYS[a.ordinal()] = ThemeKey.of(DjinnLook.theme(a));
            for (DjinnLook.Moment m : DjinnLook.Moment.values()) {
                try {
                    SHAPES[a.ordinal()][m.ordinal()] = VfxShape.valueOf(DjinnLook.shape(a, m));
                } catch (IllegalArgumentException unknown) {
                    SHAPES[a.ordinal()][m.ordinal()] = null;                                 // a shape that was renamed: that moment simply has no mesh
                }
            }
        }
    }

    static boolean typed() { return DjinnBindConfig.typedVfx(); }

    /** The element's own mote (an ember, a spark, a droplet ...); the Light Rukh with typedVfx off. */
    static ParticleOptions particle(Affinity a) {
        if (a != null && typed()) {
            ParticleOptions o = com.newuniverse.nusmp.particle.fx.Fx.options(KEYS[a.ordinal()], FxSlot.MOTE);
            if (o != null) return o;
        }
        return com.newuniverse.nusmp.particle.rukh.RukhParticles.of(align(a));
    }

    /** A Djinn's colour as a particle: the accent particle of its element dyed with that colour; a Rukh for a colour no Djinn owns, or with typedVfx off. */
    static ParticleOptions dust(int rgb, float size) {
        Affinity a = typed() ? DjinnLook.affinityOf(rgb) : null;
        if (a != null && com.newuniverse.nusmp.particle.fx.Fx.options(KEYS[a.ordinal()], FxSlot.ACCENT) instanceof TintedParticleOptions t) return t.with(rgb & 0xFFFFFF, Mth.clamp(size, 0.5f, 2.5f));
        return com.newuniverse.nusmp.relics.Fx.dust(rgb, size);
    }

    /** Sends one of the options above: the themed ones through the particle facade (its switch and budgets), a Rukh through the Rukh emitter as before. */
    static void emit(ServerLevel l, ParticleOptions o, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        if (o instanceof TintedParticleOptions) com.newuniverse.nusmp.particle.fx.Fx.sprayOptions(l, o, x, y, z, count, dx, dy, dz, speed);
        else RukhFx.spray(l, com.newuniverse.nusmp.relics.Fx.alignOf(o), x, y, z, count, dx, dy, dz, speed);
    }

    /** Drop-in for {@code RukhFx.spray(l, align(affinity), ..)}: the element's motes, at most 12 a call like the Rukh. */
    static void themed(ServerLevel l, Affinity a, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        if (a != null && typed()) com.newuniverse.nusmp.particle.fx.Fx.spray(l, KEYS[a.ordinal()], FxTone.LIGHT, x, y, z, count, dx, dy, dz, speed);
        else RukhFx.spray(l, align(a), x, y, z, count, dx, dy, dz, speed);
    }

    /** Drop-in for {@code RukhFx.spray(l, Fx.align(rgb), ..)}: the colour's element, dyed; a Rukh for a colour no Djinn owns. */
    static void tinted(ServerLevel l, int rgb, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        Affinity a = typed() ? DjinnLook.affinityOf(rgb) : null;
        if (a != null) com.newuniverse.nusmp.particle.fx.Fx.sprayTinted(l, KEYS[a.ordinal()], FxSlot.ACCENT, rgb & 0xFFFFFF, 1.0f, x, y, z, Math.min(count, 12), dx, dy, dz, speed);
        else RukhFx.spray(l, com.newuniverse.nusmp.relics.Fx.align(rgb), x, y, z, count, dx, dy, dz, speed);
    }

    /**
     * The mesh effect of a Djinn's element for a moment of its spell (one packet to the players near, through the effect budget). {@code at} is where it happens; {@code to} is where a CAST
     * travels to (ignored by the others); {@code power} is the radius of a ZONE or a STRIKE and the size of an IMPACT or a CAST. Nothing with typedVfx off.
     */
    static void mesh(ServerLevel l, Djinn d, DjinnLook.Moment m, Vec3 at, Vec3 to, double power) {
        if (l == null || d == null || at == null || !typed()) return;
        VfxShape s = SHAPES[d.affinity().ordinal()][m.ordinal()];
        if (s == null) return;
        Vec3 from = at, end = to == null ? at.add(0, 1, 0) : to;
        if (m == DjinnLook.Moment.STRIKE && DjinnLook.fromSky(d.affinity())) {
            from = at.add(0, 14, 0);
            end = at;
        }
        VfxSpawn.send(l, s, from, end, 0xFF000000 | (d.color() & 0xFFFFFF), DjinnLook.ticks(m), (float) Mth.clamp(power, 0.5, 16.0));
    }

    /** A ring of dust lying on the ground: {@code points} particles (clamped to 8..96). */
    static void ring(ServerLevel l, Vec3 at, double radius, int rgb, int points, double lift) {
        int n = Mth.clamp(points, 8, 96);
        net.minecraft.core.particles.ParticleOptions d = dust(rgb, 1.3f);
        for (int i = 0; i < n; i++) {
            double a = i * (Math.PI * 2 / n);
            emit(l, d, at.x + Math.cos(a) * radius, at.y + lift, at.z + Math.sin(a) * radius, 1, 0, 0.01, 0, 0);
        }
    }

    /** A vertical column of particles from y0 up to y1: at most 40 of them. */
    static void column(ServerLevel l, double x, double y0, double y1, double z, ParticleOptions o, int count) {
        int n = Mth.clamp(count, 2, 40);
        for (int i = 0; i < n; i++) emit(l, o, x, y0 + (y1 - y0) * i / (n - 1), z, 1, 0.05, 0, 0.05, 0);
    }

    /** A line of particles between two points: at most 40 of them. */
    static void line(ServerLevel l, Vec3 a, Vec3 b, ParticleOptions o, int count) {
        int n = Mth.clamp(count, 2, 40);
        for (int i = 0; i < n; i++) {
            double t = i / (double) (n - 1);
            emit(l, o, Mth.lerp(t, a.x, b.x), Mth.lerp(t, a.y, b.y), Mth.lerp(t, a.z, b.z), 1, 0, 0, 0, 0);
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
