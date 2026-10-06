package com.newuniverse.nusmp.vfx.fx;

/** Tiny immutable vector (pure Java, so specs and checks run headless). */
public record Vec(double x, double y, double z) {
    public static final Vec ZERO = new Vec(0, 0, 0), UP = new Vec(0, 1, 0);
    public Vec add(Vec o) { return new Vec(x + o.x, y + o.y, z + o.z); }
    public Vec sub(Vec o) { return new Vec(x - o.x, y - o.y, z - o.z); }
    public Vec mul(double s) { return new Vec(x * s, y * s, z * s); }
    public double dot(Vec o) { return x * o.x + y * o.y + z * o.z; }
    public Vec cross(Vec o) { return new Vec(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
    public double length() { return Math.sqrt(x * x + y * y + z * z); }
    public Vec norm() { double l = length(); return l < 1e-9 ? UP : mul(1 / l); }
}
