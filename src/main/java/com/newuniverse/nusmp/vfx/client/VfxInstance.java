package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** One running effect on the client. */
public final class VfxInstance {
    public final VfxPayload payload;
    public final VfxShape shape;
    public final AbstractVfxLayer layer;
    public final int color;
    public final int duration;
    public final float power;
    public final long seed;
    private final Vec3 followOffsetFrom, followOffsetTo;
    private int age;

    VfxInstance(VfxPayload payload, AbstractVfxLayer layer, int color, int duration, ClientLevel level) {
        this.payload = payload;
        this.shape = payload.shapeType();
        this.layer = layer;
        this.color = color;
        this.duration = Math.max(1, duration);
        this.power = payload.power() <= 0 ? 1 : payload.power();
        this.seed = payload.seed();
        Entity e = payload.followEntity() >= 0 ? level.getEntity(payload.followEntity()) : null;
        this.followOffsetFrom = e == null ? null : payload.from().subtract(e.position());
        this.followOffsetTo = e == null ? null : payload.to().subtract(e.position());
    }

    public int age() { return age; }
    void tick() { age++; }
    public boolean done() { return age >= duration; }

    /** 0..1 progress, smooth between ticks. */
    public float progress(float partialTick) { return Math.min(1f, (age + partialTick) / duration); }
    public float ageTicks(float partialTick) { return age + partialTick; }

    /** A fresh random with this effect's seed (same pattern on every client). */
    public RandomSource random() { return RandomSource.create(seed); }

    /** Start point; if following an entity, moves with it. */
    public Vec3 from(VfxRenderContext ctx) { return follow(ctx, payload.from(), followOffsetFrom); }
    public Vec3 to(VfxRenderContext ctx) { return follow(ctx, payload.to(), followOffsetTo); }

    /** 0.45: the body yaw (degrees) of the followed living entity, or NaN when nothing living is followed. */
    public float followYaw(VfxRenderContext ctx) {
        if (payload.followEntity() < 0 || ctx.level == null) return Float.NaN;
        Entity e = ctx.level.getEntity(payload.followEntity());
        if (!(e instanceof net.minecraft.world.entity.LivingEntity le)) return Float.NaN;
        return net.minecraft.util.Mth.lerp(ctx.partialTick, le.yBodyRotO, le.yBodyRot);
    }

    private Vec3 follow(VfxRenderContext ctx, Vec3 fixed, Vec3 offset) {
        if (offset == null) return fixed;
        Entity e = ctx.level.getEntity(payload.followEntity());
        return e == null ? fixed : e.getPosition(ctx.partialTick).add(offset);
    }
}
