package com.newuniverse.nusmp.client.particle.rukh;

import com.newuniverse.nusmp.particle.rukh.RukhMath;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

/**
 * Why: Dark and Light Rukh share everything except how they move and glow, so the lifecycle lives here once: 15..25 tick lifetime (the
 * owner's lag rule: short-lived), the smooth scale in/out and the gentle alpha fade ({@link RukhMath} curves, tested), velocity clamping
 * every tick (a stray huge velocity from a careless call site cannot throw a bird across the map), no gravity, no collision (a bird is
 * decoration, physics would only cost CPU), and the wing-flap cycle: each particle starts at a random phase and steps through the six
 * wing-pose sprites twice during its life, so a stream of them reads as the flock of the reference art, every bird in a different pose. The
 * render type is the translucent particle sheet, which batches all Rukh into the vanilla particle draw call.
 */
abstract class RukhParticleBase extends TextureSheetParticle {
    protected final SpriteSet sprites;
    protected final float baseSize;
    private final int phase;

    RukhParticleBase(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.lifetime = 15 + this.random.nextInt(11);          // 15..25
        this.gravity = 0f;
        this.hasPhysics = false;
        this.friction = 0.99f;
        this.xd = RukhMath.clampSpeed(vx);
        this.yd = RukhMath.clampSpeed(vy);
        this.zd = RukhMath.clampSpeed(vz);
        this.baseSize = 0.16f + this.random.nextFloat() * 0.12f;
        this.quadSize = baseSize;
        this.phase = this.random.nextInt(this.lifetime);
        this.roll = this.oRoll = this.random.nextFloat() * 6.2831855f;
        this.pickFrame();
    }

    private void pickFrame() {
        // two flap cycles over the life, offset by the particle's own phase
        this.setSprite(sprites.get((this.age * 2 + phase) % this.lifetime, this.lifetime));
    }

    /** Subclass motion for one tick, applied to xd/yd/zd before the move. */
    protected abstract void steer();

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        float life = (float) this.age / (float) this.lifetime;
        this.alpha = RukhMath.alphaCurve(life);
        steer();
        this.xd = RukhMath.clampSpeed(this.xd);
        this.yd = RukhMath.clampSpeed(this.yd);
        this.zd = RukhMath.clampSpeed(this.zd);
        this.move(this.xd, this.yd, this.zd);
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;
        pickFrame();
    }

    @Override
    public float getQuadSize(float partialTick) {
        float life = Math.min(1f, ((float) this.age + partialTick) / (float) this.lifetime);
        return this.baseSize * (0.35f + 0.65f * RukhMath.scaleCurve(life));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
