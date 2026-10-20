package com.newuniverse.nusmp.client.particle.fx;

import com.newuniverse.nusmp.particle.fx.FxCatalog;
import com.newuniverse.nusmp.particle.fx.FxMath;
import com.newuniverse.nusmp.particle.fx.FxProfile;
import com.newuniverse.nusmp.particle.fx.FxTint;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

/**
 * Why: the ONE particle class behind all 70 custom particles; what makes an ember an ember and a leaf a leaf is the immutable {@link FxProfile} read from the catalog, not a subclass. Per
 * particle it keeps only a few primitives (its random frame phase, sway phase, size jitter, the tint multiplier and the fixed lifetime picked from the profile's range), so spawning one
 * allocates the particle and nothing else. Physics per tick: gravity (negative rises), drag, swirl (the horizontal velocity turns), sway (a sine push), jitter (small random noise), optional
 * ground collision; the quad spins by {@code spin} per tick; size and alpha follow the profile's curves ({@link FxMath}); the animation frame follows the profile's mode (spread over the life,
 * looping, or fixed at birth). A full-bright profile ignores the world light; an additive profile uses {@link FxRenderTypes#ADDITIVE}.
 */
final class FxParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final FxProfile profile;
    private final int frames;
    private final int phase;
    private final float swayPhase;
    private final float sizeJitter;
    private final float scale;
    private final float spinSign;

    FxParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, FxCatalog.Def def, FxTint tint) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.profile = def.profile();
        this.frames = def.frames();
        this.lifetime = profile.lifeMin() + (profile.lifeMax() > profile.lifeMin() ? this.random.nextInt(profile.lifeMax() - profile.lifeMin() + 1) : 0);
        this.phase = this.random.nextInt(Math.max(1, frames));
        this.swayPhase = this.random.nextFloat() * 6.2831855f;
        this.sizeJitter = 0.88f + this.random.nextFloat() * 0.24f;
        this.scale = tint.scale();
        this.spinSign = this.random.nextBoolean() ? 1f : -1f;
        this.xd = FxMath.clampSpeed(vx);
        this.yd = FxMath.clampSpeed(vy);
        this.zd = FxMath.clampSpeed(vz);
        this.hasPhysics = profile.collide();
        this.gravity = 0f;
        this.friction = 1f;
        if (profile.spin() != 0f) this.roll = this.oRoll = this.random.nextFloat() * 6.2831855f;
        if (tint.tinted()) this.setColor(tint.red(), tint.green(), tint.blue());
        this.alpha = 0f;
        this.quadSize = 0.5f * profile.sizeStart() * scale * sizeJitter;
        this.setFrame();
    }

    private void setFrame() {
        if (frames <= 1) {
            this.setSprite(sprites.get(0, 1));
            return;
        }
        int f = FxMath.frameAt(profile.anim(), this.age, this.lifetime, frames, profile.loopTicks(), phase);
        this.setSprite(sprites.get(f, frames - 1));
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        FxProfile p = profile;
        this.yd -= 0.04 * p.gravity();
        if (p.swirl() != 0f) {
            double c = Math.cos(p.swirl()), s = Math.sin(p.swirl());
            double nx = this.xd * c - this.zd * s;
            double nz = this.xd * s + this.zd * c;
            this.xd = nx;
            this.zd = nz;
        }
        if (p.sway() != 0f) {
            this.xd += Math.sin(this.age * 0.31f + swayPhase) * p.sway();
            this.zd += Math.cos(this.age * 0.27f + swayPhase) * p.sway();
        }
        if (p.jitter() != 0f) {
            this.xd += (this.random.nextFloat() - 0.5f) * 2f * p.jitter();
            this.yd += (this.random.nextFloat() - 0.5f) * 2f * p.jitter();
            this.zd += (this.random.nextFloat() - 0.5f) * 2f * p.jitter();
        }
        this.xd = FxMath.clampSpeed(this.xd);
        this.yd = FxMath.clampSpeed(this.yd);
        this.zd = FxMath.clampSpeed(this.zd);
        if (this.xd != 0.0 || this.yd != 0.0 || this.zd != 0.0) this.move(this.xd, this.yd, this.zd);
        this.xd *= p.drag();
        this.yd *= p.drag();
        this.zd *= p.drag();
        if (this.onGround) {
            this.xd *= 0.7;
            this.zd *= 0.7;
        }
        if (p.spin() != 0f) {
            this.oRoll = this.roll;
            this.roll += p.spin() * spinSign;
        }
        float t = (float) this.age / (float) this.lifetime;
        this.alpha = FxMath.alphaAt(p.fadeIn(), p.fadeOut(), t);
        setFrame();
    }

    @Override
    public float getQuadSize(float partialTick) {
        float t = Math.min(1f, ((float) this.age + partialTick) / (float) this.lifetime);
        return 0.5f * FxMath.sizeAt(profile.curve(), profile.sizeStart(), profile.sizeEnd(), t) * scale * sizeJitter;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return profile.light() == FxProfile.Light.FULL ? 15728880 : super.getLightColor(partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return profile.blend() == FxProfile.Blend.ADD ? FxRenderTypes.ADDITIVE : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
