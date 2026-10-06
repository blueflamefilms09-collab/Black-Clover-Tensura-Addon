package net.minecraft.world.phys;

public class Vec3 {
    public static final Vec3 ZERO = new Vec3(0, 0, 0);
    public final double x, y, z;
    public Vec3(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    public Vec3 add(double a, double b, double c) { return new Vec3(x + a, y + b, z + c); }
    public Vec3 add(Vec3 o) { return add(o.x, o.y, o.z); }
    public Vec3 subtract(Vec3 o) { return new Vec3(x - o.x, y - o.y, z - o.z); }
    public Vec3 subtract(double a, double b, double c) { return new Vec3(x - a, y - b, z - c); }
    public Vec3 scale(double s) { return new Vec3(x * s, y * s, z * s); }
    public Vec3 multiply(double a, double b, double c) { return new Vec3(x * a, y * b, z * c); }
    public double length() { return Math.sqrt(x * x + y * y + z * z); }
    public double lengthSqr() { return x * x + y * y + z * z; }
    public double horizontalDistance() { return Math.sqrt(x * x + z * z); }
    public Vec3 normalize() { double l = length(); return l < 1e-4 ? ZERO : new Vec3(x / l, y / l, z / l); }
    public double dot(Vec3 o) { return x * o.x + y * o.y + z * o.z; }
    public Vec3 cross(Vec3 o) { return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
    public Vec3 lerp(Vec3 o, double t) { return new Vec3(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t); }
    public double distanceTo(Vec3 o) { return subtract(o).length(); }
    public double distanceToSqr(Vec3 o) { return subtract(o).lengthSqr(); }
}
