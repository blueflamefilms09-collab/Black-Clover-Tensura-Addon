package com.newuniverse.nusmp.client.particle.rukh;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Why: Light Rukh is the glowing cream-white bird of the reference render: it must read as a light source, so {@code getLightColor} returns
 * full-bright (15728880 = sky 15 / block 15) regardless of where it flies, and the texture carries its own soft yellow halo. Its motion is a
 * slow spiral: the horizontal velocity is rotated a few degrees each tick, which bends a straight stream into the curling river of birds of
 * the picture. Rotation is applied to the velocity vector (not the position) so the speed stays what the caller asked for and the clamp in
 * the base class still holds.
 */
public class LightRukhParticle extends RukhParticleBase {
    private final float turn;

    protected LightRukhParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz, sprites);
        this.turn = (this.random.nextBoolean() ? 1f : -1f) * (0.05f + this.random.nextFloat() * 0.07f);
        this.setColor(1f, 1f, 1f);
    }

    @Override
    protected void steer() {
        this.oRoll = this.roll;
        this.roll += this.turn * 0.6f;
        double c = Math.cos(this.turn), s = Math.sin(this.turn);
        double nx = this.xd * c - this.zd * s;
        double nz = this.xd * s + this.zd * c;
        this.xd = nx;
        this.zd = nz;
        this.yd += 0.003;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 15728880;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new LightRukhParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
