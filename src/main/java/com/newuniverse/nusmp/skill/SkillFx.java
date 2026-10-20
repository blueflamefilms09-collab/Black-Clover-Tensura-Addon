package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;
import com.newuniverse.nusmp.particle.fx.FxRelic;

/**
 * Why: abilities spawn no raw vanilla particles: the mod's own custom particles ({@code particle.fx}, theme {@code skill}, light or dark flavour) go through {@link Fx} (which caps every
 * burst like the old Rukh emitters did, keeps the per-tick budgets and honours the {@code particles.custom} switch), and the big shapes come from the existing vfx package. This facade is the one place the skills touch
 * those helpers, so each effect call is a single guarded line (never throws, never sends on a bad argument) and no skill can reach for a
 * vanilla particle by accident. It does NOT touch the Time Magic effects: none of the shapes used here are Time shapes.
 */
final class SkillFx {
    private SkillFx() {}

    /** A burst of the skill theme's particles (light or dark flavour) at a point. */
    static void burst(ServerLevel level, Vec3 at, boolean dark, int count, double spread) {
        if (level == null || at == null) return;
        Fx.burst(level, FxThemes.SKILL, at, FxTone.of(dark), count, spread);
    }

    /** Particles sprayed in a box around a point, like the old raw particle calls. */
    static void spray(ServerLevel level, boolean dark, Vec3 at, int count, double dx, double dy, double dz, double speed) {
        if (level == null || at == null) return;
        Fx.spray(level, FxThemes.SKILL, FxTone.of(dark), at.x, at.y, at.z, count, dx, dy, dz, speed);
    }

    /** Motes gathered around an entity (a buff, a charge). */
    static void aura(ServerLevel level, Entity e, boolean dark) {
        if (level == null || e == null) return;
        Fx.aura(level, FxThemes.SKILL, e, FxTone.of(dark));
    }

    /** A line of particles from one point to another (a beam, a bolt), at most 24 samples so a long beam is cheap. */
    static void beam(ServerLevel level, Vec3 from, Vec3 to, boolean dark) {
        if (level == null || from == null || to == null) return;
        double len = from.distanceTo(to);
        if (len < 0.1) return;
        int n = Math.min(24, Math.max(2, (int) (len / 1.2)));
        Vec3 step = to.subtract(from).scale(1.0 / n);
        for (int i = 0; i <= n; i++) {
            Vec3 p = from.add(step.scale(i));
            Fx.spray(level, FxThemes.SKILL, FxTone.of(dark), p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.0);
        }
    }

    /** A ring of dust and a grimoire circle on the ground (0xRRGGBB). */
    static void ring(ServerLevel level, Vec3 at, double radius, int rgb) {
        if (level == null || at == null) return;
        FxRelic.ring(level, at, radius, rgb);
    }

    /** One of the mod's own effect shapes (ARGB colour). A failure to send is never the skill's failure. */
    static void shape(ServerLevel level, VfxShape shape, Vec3 from, Vec3 to, int argb, int duration, float power) {
        if (level == null || shape == null || from == null || to == null) return;
        try {
            VfxSpawn.send(level, shape, from, to, argb, duration, power);
        } catch (RuntimeException | LinkageError ignored) {
            // effects are cosmetic: a missing client definition or a closed connection must not break the cast
        }
    }

    /** A shape that follows an entity (an aura on the caster). */
    static void follow(ServerLevel level, VfxShape shape, Entity who, Vec3 to, int argb, int duration, float power) {
        if (level == null || shape == null || who == null || to == null) return;
        try {
            VfxSpawn.sendFollowing(level, shape, who, to, argb, duration, power);
        } catch (RuntimeException | LinkageError ignored) {
            // cosmetic only
        }
    }

    static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        if (level == null || at == null || sound == null) return;
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
