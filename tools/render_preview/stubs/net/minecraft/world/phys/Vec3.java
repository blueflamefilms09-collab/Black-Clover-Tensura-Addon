package net.minecraft.world.phys;

import org.joml.Vector3f;

/** Preview stub of Minecraft 1.21.1's Vec3 (immutable double vector): the arithmetic the mod and the painters use. */
public class Vec3 {
    public static final Vec3 ZERO = new Vec3(0.0, 0.0, 0.0);
    public final double x;
    public final double y;
    public final double z;

    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3(Vector3f vector) { this(vector.x(), vector.y(), vector.z()); }

    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }

    public Vec3 vectorTo(Vec3 vec) { return new Vec3(vec.x - x, vec.y - y, vec.z - z); }
    public Vec3 normalize() {
        double d = Math.sqrt(x * x + y * y + z * z);
        return d < 1.0E-5 ? ZERO : new Vec3(x / d, y / d, z / d);
    }
    public double dot(Vec3 vec) { return x * vec.x + y * vec.y + z * vec.z; }
    public Vec3 cross(Vec3 vec) { return new Vec3(y * vec.z - z * vec.y, z * vec.x - x * vec.z, x * vec.y - y * vec.x); }
    public Vec3 subtract(Vec3 vec) { return subtract(vec.x, vec.y, vec.z); }
    public Vec3 subtract(double x, double y, double z) { return add(-x, -y, -z); }
    public Vec3 add(Vec3 vec) { return add(vec.x, vec.y, vec.z); }
    public Vec3 add(double x, double y, double z) { return new Vec3(this.x + x, this.y + y, this.z + z); }
    public double distanceTo(Vec3 vec) {
        double a = vec.x - x, b = vec.y - y, c = vec.z - z;
        return Math.sqrt(a * a + b * b + c * c);
    }
    public double distanceToSqr(Vec3 vec) {
        double a = vec.x - x, b = vec.y - y, c = vec.z - z;
        return a * a + b * b + c * c;
    }
    public double distanceToSqr(double x, double y, double z) {
        double a = x - this.x, b = y - this.y, c = z - this.z;
        return a * a + b * b + c * c;
    }
    public Vec3 scale(double factor) { return multiply(factor, factor, factor); }
    public Vec3 reverse() { return scale(-1.0); }
    public Vec3 multiply(Vec3 vec) { return multiply(vec.x, vec.y, vec.z); }
    public Vec3 multiply(double x, double y, double z) { return new Vec3(this.x * x, this.y * y, this.z * z); }
    public double length() { return Math.sqrt(x * x + y * y + z * z); }
    public double lengthSqr() { return x * x + y * y + z * z; }
    public double horizontalDistance() { return Math.sqrt(x * x + z * z); }
    public double horizontalDistanceSqr() { return x * x + z * z; }
    public Vec3 lerp(Vec3 to, double delta) { return new Vec3(x + (to.x - x) * delta, y + (to.y - y) * delta, z + (to.z - z) * delta); }
    public Vec3 xRot(float pitch) {
        float f = (float) Math.cos(pitch), f1 = (float) Math.sin(pitch);
        return new Vec3(x, y * (double) f + z * (double) f1, z * (double) f - y * (double) f1);
    }
    public Vec3 yRot(float yaw) {
        float f = (float) Math.cos(yaw), f1 = (float) Math.sin(yaw);
        return new Vec3(x * (double) f + z * (double) f1, y, z * (double) f - x * (double) f1);
    }
    public Vec3 zRot(float roll) {
        float f = (float) Math.cos(roll), f1 = (float) Math.sin(roll);
        return new Vec3(x * (double) f + y * (double) f1, y * (double) f - x * (double) f1, z);
    }
    public final Vector3f toVector3f() { return new Vector3f((float) x, (float) y, (float) z); }

    @Override public boolean equals(Object o) { return this == o || (o instanceof Vec3 v && v.x == x && v.y == y && v.z == z); }
    @Override public int hashCode() { return java.util.Objects.hash(x, y, z); }
    @Override public String toString() { return "(" + x + ", " + y + ", " + z + ")"; }
}
