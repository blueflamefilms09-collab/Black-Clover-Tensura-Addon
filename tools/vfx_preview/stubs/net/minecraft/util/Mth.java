package net.minecraft.util;

/** Preview stub of Minecraft's Mth (same maths, exact trig instead of the sin table). */
public final class Mth {
    public static final float PI = (float) Math.PI, TWO_PI = (float) (Math.PI * 2), HALF_PI = (float) (Math.PI / 2);
    private Mth() {}
    public static float sin(float a) { return (float) Math.sin(a); }
    public static float cos(float a) { return (float) Math.cos(a); }
    public static float sin(double a) { return (float) Math.sin(a); }
    public static float cos(double a) { return (float) Math.cos(a); }
    public static float sqrt(float a) { return (float) Math.sqrt(a); }
    public static float clamp(float v, float a, float b) { return v < a ? a : Math.min(v, b); }
    public static double clamp(double v, double a, double b) { return v < a ? a : Math.min(v, b); }
    public static int clamp(int v, int a, int b) { return v < a ? a : Math.min(v, b); }
    public static float lerp(float t, float a, float b) { return a + t * (b - a); }
    public static double lerp(double t, double a, double b) { return a + t * (b - a); }
    public static float abs(float a) { return Math.abs(a); }
    public static int floor(float a) { return (int) Math.floor(a); }
    public static int floor(double a) { return (int) Math.floor(a); }
    public static float frac(float a) { return a - (float) Math.floor(a); }
    public static float atan2(double y, double x) { return (float) Math.atan2(y, x); }
}
