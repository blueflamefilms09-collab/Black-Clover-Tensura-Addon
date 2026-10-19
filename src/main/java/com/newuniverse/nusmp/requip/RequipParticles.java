package com.newuniverse.nusmp.requip;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Why: the themed particles of Requip: The Knight (the flash of light of a swap, the dust of the armour's colours, the trail of the flying armours) in one place. The skill package keeps to the mod's own
 * effect shapes ({@code SkillAssetsTest} forbids a raw particle there), so the particles of this skill, which are the armour's own colours and kinds ({@link Armour.Particle}), live here and the skill's
 * effect class calls them.
 */
public final class RequipParticles {
    private RequipParticles() {}

    public static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), size);
    }

    private static ParticleOptions primary(Armour.Particle k) {
        return switch (k) {
            case HEART -> ParticleTypes.HEART;
            case SWORD -> ParticleTypes.END_ROD;
            case FLAME -> ParticleTypes.FLAME;
            case SPARK -> ParticleTypes.ELECTRIC_SPARK;
            case WATER -> ParticleTypes.SPLASH;
            case DARK -> ParticleTypes.SQUID_INK;
            case CLOUD -> ParticleTypes.CLOUD;
            case RUNE -> ParticleTypes.ENCHANT;
            case GLINT -> ParticleTypes.GLOW;
        };
    }

    /** The armour's theme particles (its own kind and its colour dust) around a point. */
    public static void themed(ServerLevel level, Armour a, Vec3 at, int count, double spread) {
        level.sendParticles(primary(a.particle()), at.x, at.y, at.z, count, spread, spread * 1.2, spread, 0.05);
        level.sendParticles(dust(a.theme(), 1.4f), at.x, at.y, at.z, count, spread, spread * 1.2, spread, 0.02);
        level.sendParticles(dust(a.glow(), 1.0f), at.x, at.y, at.z, count / 2 + 1, spread, spread * 1.2, spread, 0.02);
    }

    /** The flash of light of a swap: a white flash, a rising ring of the theme colours and the theme particles. */
    public static void flash(ServerLevel level, ServerPlayer p, Armour a) {
        Vec3 c = p.position().add(0, 1.0, 0);
        level.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        for (int i = 0; i < 36; i++) {
            double ang = i / 36.0 * Mth.TWO_PI, r = 0.9;
            level.sendParticles(dust(i % 2 == 0 ? a.theme() : a.glow(), 1.5f), c.x + Math.cos(ang) * r, p.getY() + 0.1 + i * 0.05, c.z + Math.sin(ang) * r, 1, 0, 0.05, 0, 0.0);
        }
        themed(level, a, c, 40, 0.6);
    }

    /** The trail of a flying armour. */
    public static void trail(ServerLevel level, ServerPlayer p, Armour a) {
        level.sendParticles(primary(a.particle()), p.getX(), p.getY() + 0.9, p.getZ(), 2, 0.3, 0.3, 0.3, 0.01);
        level.sendParticles(dust(a.glow(), 1.0f), p.getX(), p.getY() + 0.9, p.getZ(), 2, 0.3, 0.3, 0.3, 0.0);
    }
}
