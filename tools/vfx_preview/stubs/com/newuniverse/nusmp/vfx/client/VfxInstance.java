package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Preview stub of a running effect (no entity following: from/to stay fixed). */
public final class VfxInstance {
    public final VfxPayload payload;
    public final VfxShape shape;
    public final AbstractVfxLayer layer;
    public final int color, duration;
    public final float power;
    public final long seed;
    private int age;

    public VfxInstance(VfxPayload payload, AbstractVfxLayer layer, int color, int duration) {
        this.payload = payload;
        this.shape = payload.shapeType();
        this.layer = layer;
        this.color = color;
        this.duration = Math.max(1, duration);
        this.power = payload.power() <= 0 ? 1 : payload.power();
        this.seed = payload.seed();
    }

    public int age() { return age; }
    public void setAge(int a) { age = a; }
    public void tick() { age++; }
    public boolean done() { return age >= duration; }
    public float progress(float partialTick) { return Math.min(1f, (age + partialTick) / duration); }
    public float ageTicks(float partialTick) { return age + partialTick; }
    public RandomSource random() { return RandomSource.create(seed); }
    public Vec3 from(VfxRenderContext ctx) { return payload.from(); }
    public Vec3 to(VfxRenderContext ctx) { return payload.to(); }
    public float followYaw(VfxRenderContext ctx) { return Float.NaN; }
}
