package com.newuniverse.nusmp.client.relics.djinn.tiny;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * The little effects of a Tiny Djinn, all plain client particles and quiet sounds: the poof of its entrance (a ring of smoke and a flash), the spell burst of a
 * cast, the spark where it strikes, the hop's glitter, the spiral and the pop of its exit. Called a few times per event, never per vertex; the dust options of a
 * Djinn are made once ({@link #dust}).
 */
final class TinyDjinnFx {
    private TinyDjinnFx() {}

    /** The Djinn's light as a dust particle option (made once per figure). */
    static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), size);
    }

    /** The entrance: a ring of smoke round the spot, a flash, a burst of the Djinn's dust. */
    static void poofIn(ClientLevel level, double x, double y, double z, DustParticleOptions light) {
        RandomSource r = level.random;
        Fx.add(level, FxThemes.RELIC, FxOld.FLASH, x, y, z, 0, 0, 0);
        for (int i = 0; i < 14; i++) {
            double a = i * Math.PI * 2 / 14;
            Fx.add(level, FxThemes.RELIC, FxOld.POOF, x + Math.cos(a) * 0.12, y, z + Math.sin(a) * 0.12, Math.cos(a) * 0.07, 0.01, Math.sin(a) * 0.07);
        }
        for (int i = 0; i < 10; i++) level.addParticle(light, x + (r.nextDouble() - 0.5) * 0.3, y + (r.nextDouble() - 0.5) * 0.3, z + (r.nextDouble() - 0.5) * 0.3,
                (r.nextDouble() - 0.5) * 0.06, r.nextDouble() * 0.05, (r.nextDouble() - 0.5) * 0.06);
        level.playLocalSound(x, y, z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.35f, 1.7f, false);
    }

    /** The exit's last puff, at the vessel. */
    static void poofOut(ClientLevel level, double x, double y, double z, DustParticleOptions light) {
        RandomSource r = level.random;
        for (int i = 0; i < 6; i++) Fx.add(level, FxThemes.RELIC, FxOld.POOF, x, y, z, (r.nextDouble() - 0.5) * 0.06, r.nextDouble() * 0.04, (r.nextDouble() - 0.5) * 0.06);
        for (int i = 0; i < 8; i++) level.addParticle(light, x, y, z, (r.nextDouble() - 0.5) * 0.08, (r.nextDouble() - 0.5) * 0.08, (r.nextDouble() - 0.5) * 0.08);
        level.playLocalSound(x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.3f, 1.9f, false);
    }

    /** One mote of the exit spiral (called every other frame while the figure turns into the vessel). */
    static void spiralMote(ClientLevel level, double x, double y, double z, DustParticleOptions light) {
        level.addParticle(light, x, y, z, 0, 0.01, 0);
    }

    /** The spell burst at the figure's pointing hand: sparks fly out along {@code (dx, dz)}, a ring of its light stands round the hand. */
    static void cast(ClientLevel level, double x, double y, double z, double dx, double dz, DustParticleOptions light) {
        RandomSource r = level.random;
        for (int i = 0; i < 6; i++) Fx.add(level, FxThemes.RELIC, FxOld.END_ROD, x, y, z, dx * 0.08 + (r.nextDouble() - 0.5) * 0.03, (r.nextDouble() - 0.3) * 0.03, dz * 0.08 + (r.nextDouble() - 0.5) * 0.03);
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI * 2 / 10;
            level.addParticle(light, x + Math.cos(a) * 0.1, y + Math.sin(a) * 0.1, z, dx * 0.03, 0, dz * 0.03);
        }
        level.playLocalSound(x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.4f, 1.5f, false);
    }

    /** Where the dash reaches the foe. */
    static void strike(ClientLevel level, double x, double y, double z, DustParticleOptions light) {
        RandomSource r = level.random;
        for (int i = 0; i < 6; i++) Fx.add(level, FxThemes.RELIC, FxOld.CRIT, x, y, z, (r.nextDouble() - 0.5) * 0.3, (r.nextDouble() - 0.2) * 0.3, (r.nextDouble() - 0.5) * 0.3);
        for (int i = 0; i < 8; i++) level.addParticle(light, x, y, z, (r.nextDouble() - 0.5) * 0.16, (r.nextDouble() - 0.5) * 0.16, (r.nextDouble() - 0.5) * 0.16);
    }

    /** The happy hop after a kill. */
    static void hop(ClientLevel level, double x, double y, double z, DustParticleOptions light) {
        RandomSource r = level.random;
        for (int i = 0; i < 5; i++) Fx.add(level, FxThemes.RELIC, FxOld.HAPPY_VILLAGER, x + (r.nextDouble() - 0.5) * 0.3, y + r.nextDouble() * 0.3, z + (r.nextDouble() - 0.5) * 0.3, 0, 0.02, 0);
        for (int i = 0; i < 8; i++) level.addParticle(light, x, y, z, (r.nextDouble() - 0.5) * 0.12, r.nextDouble() * 0.1, (r.nextDouble() - 0.5) * 0.12);
        level.playLocalSound(x, y, z, SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.PLAYERS, 0.35f, 1.8f, false);
    }
}
