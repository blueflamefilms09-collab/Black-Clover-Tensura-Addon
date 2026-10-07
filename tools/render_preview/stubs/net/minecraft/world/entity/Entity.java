package net.minecraft.world.entity;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Preview stub of Entity: position, rotation, size, id and the tick counter the painters read. In the game's Entity x / y / z are private
 * (use getX() ...), tickCount, xRotO, yRotO, xo, yo, zo are public fields.
 */
public class Entity {
    public int tickCount;
    public float xRotO, yRotO;
    public double xo, yo, zo;
    private double x, y, z;
    private float yRot, xRot;
    private int id;
    private float bbWidth = 0.6F, bbHeight = 1.8F;
    private final EntityType<?> type;
    private final Level level;
    private final UUID uuid = UUID.randomUUID();

    public Entity(EntityType<?> type, Level level) {
        this.type = type;
        this.level = level;
    }

    public EntityType<?> getType() { return type; }
    public final Level level() { return level; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public UUID getUUID() { return uuid; }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public Vec3 position() { return new Vec3(x, y, z); }
    public Vec3 getPosition(float partialTicks) { return new Vec3(xo + (x - xo) * partialTicks, yo + (y - yo) * partialTicks, zo + (z - zo) * partialTicks); }
    public void setPos(double x, double y, double z) { this.x = x; this.y = y; this.z = z; this.xo = x; this.yo = y; this.zo = z; }
    public void setPos(Vec3 pos) { setPos(pos.x, pos.y, pos.z); }

    public float getYRot() { return yRot; }
    public float getXRot() { return xRot; }
    public void setYRot(float yRot) { this.yRot = yRot; }
    public void setXRot(float xRot) { this.xRot = xRot; }

    public float getBbWidth() { return bbWidth; }
    public float getBbHeight() { return bbHeight; }
    /** Preview helper (not in the game): sets the hitbox size. */
    public void previewSetSize(float w, float h) { bbWidth = w; bbHeight = h; }

    public float getEyeHeight() { return bbHeight * 0.9F; }
    public double getEyeY() { return y + getEyeHeight(); }
    public Vec3 getEyePosition() { return new Vec3(x, getEyeY(), z); }
    public Vec3 getEyePosition(float partialTicks) { Vec3 p = getPosition(partialTicks); return new Vec3(p.x, p.y + getEyeHeight(), p.z); }

    public Vec3 getViewVector(float partialTicks) {
        float pitch = xRot * ((float) Math.PI / 180F), yaw = -yRot * ((float) Math.PI / 180F);
        float cy = (float) Math.cos(yaw), sy = (float) Math.sin(yaw), cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
        return new Vec3(sy * cp, -sp, cy * cp);
    }
    public final Vec3 getLookAngle() { return getViewVector(1.0F); }
    public Vec3 getDeltaMovement() { return Vec3.ZERO; }

    public boolean isCrouching() { return false; }
    public boolean isInvisible() { return false; }
    public boolean isSpectator() { return false; }
    public boolean isAlive() { return true; }
    public final boolean isRemoved() { return false; }
}
