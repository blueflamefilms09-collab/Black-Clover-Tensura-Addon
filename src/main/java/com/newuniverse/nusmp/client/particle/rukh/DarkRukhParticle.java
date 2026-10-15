package com.newuniverse.nusmp.client.particle.rukh;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Why: Dark Rukh is the black ink-brush bird of the reference drawing: it drifts and tumbles. A low-frequency sideways wobble (a different
 * phase per bird) turns a straight stream into the loose V-shaped flock of the picture, the roll spins slowly one way or the other, and a
 * faint upward bias keeps it from looking like falling soot. The texture is solid black-violet with hard edges, so nothing is lit: it uses
 * the world light like an ordinary particle (a dark bird in a dark room is nearly invisible, which suits it).
 */
public class DarkRukhParticle extends RukhParticleBase {
    private final float spin;
    private final float wobble;

    protected DarkRukhParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz, sprites);
        this.spin = (this.random.nextFloat() - 0.5f) * 0.18f;
        this.wobble = this.random.nextFloat() * 6.2831855f;
        this.setColor(1f, 1f, 1f);
    }

    @Override
    protected void steer() {
        this.oRoll = this.roll;
        this.roll += this.spin;
        double w = Math.sin(this.age * 0.45 + this.wobble) * 0.006;
        this.xd += w;
        this.zd -= w;
        this.yd += 0.0025;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new DarkRukhParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
