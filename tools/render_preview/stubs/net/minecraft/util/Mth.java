package net.minecraft.util;

/**
 * Preview stub of Minecraft 1.21.1's Mth: the members the mod and the render painters use, with the game's signatures (exact trig
 * instead of the game's sine table, so values differ by about 1e-4). atan2 returns a double like the game's.
 */
public final class Mth {
    public static final float PI = (float) Math.PI;
    public static final float HALF_PI = PI / 2.0F;
    public static final float TWO_PI = PI * 2.0F;
    public static final float DEG_TO_RAD = PI / 180.0F;
    public static final float RAD_TO_DEG = 180.0F / PI;
    public static final float EPSILON = 1.0E-5F;
    public static final float SQRT_OF_TWO = (float) Math.sqrt(2.0);

    private Mth() {}

    public static float sin(float value) { return (float) Math.sin(value); }
    public static float cos(float value) { return (float) Math.cos(value); }
    public static float sqrt(float value) { return (float) Math.sqrt(value); }

    public static int floor(float value) { int i = (int) value; return value < (float) i ? i - 1 : i; }
    public static int floor(double value) { int i = (int) value; return value < (double) i ? i - 1 : i; }
    public static long lfloor(double value) { long l = (long) value; return value < (double) l ? l - 1L : l; }
    public static int ceil(float value) { int i = (int) value; return value > (float) i ? i + 1 : i; }
    public static int ceil(double value) { int i = (int) value; return value > (double) i ? i + 1 : i; }

    public static int clamp(int value, int min, int max) { return Math.min(Math.max(value, min), max); }
    public static long clamp(long value, long min, long max) { return Math.min(Math.max(value, min), max); }
    public static float clamp(float value, float min, float max) { return value < min ? min : Math.min(value, max); }
    public static double clamp(double value, double min, double max) { return value < min ? min : Math.min(value, max); }

    public static double clampedLerp(double start, double end, double delta) {
        if (delta < 0.0) return start;
        return delta > 1.0 ? end : lerp(delta, start, end);
    }

    public static float lerp(float delta, float start, float end) { return start + delta * (end - start); }
    public static double lerp(double delta, double start, double end) { return start + delta * (end - start); }
    public static float inverseLerp(float value, float start, float end) { return (value - start) / (end - start); }
    public static double inverseLerp(double value, double start, double end) { return (value - start) / (end - start); }

    public static float abs(float value) { return Math.abs(value); }
    public static int abs(int value) { return Math.abs(value); }
    public static float square(float value) { return value * value; }
    public static double square(double value) { return value * value; }
    public static int square(int value) { return value * value; }

    public static float frac(float number) { return number - (float) floor(number); }
    public static double frac(double number) { return number - (double) lfloor(number); }

    public static int wrapDegrees(int angle) {
        int i = angle % 360;
        if (i >= 180) i -= 360;
        if (i < -180) i += 360;
        return i;
    }

    public static float wrapDegrees(float value) {
        float f = value % 360.0F;
        if (f >= 180.0F) f -= 360.0F;
        if (f < -180.0F) f += 360.0F;
        return f;
    }

    public static double wrapDegrees(double value) {
        double d = value % 360.0;
        if (d >= 180.0) d -= 360.0;
        if (d < -180.0) d += 360.0;
        return d;
    }

    public static float degreesDifference(float start, float end) { return wrapDegrees(end - start); }

    public static float rotLerp(float delta, float start, float end) { return start + delta * wrapDegrees(end - start); }
    public static double rotLerp(double delta, double start, double end) { return start + delta * wrapDegrees(end - start); }

    public static double atan2(double y, double x) { return Math.atan2(y, x); }

    public static double smoothstep(double x) { return x * x * x * (x * (x * 6.0 - 15.0) + 10.0); }

    public static int sign(double x) { return x == 0.0 ? 0 : (x > 0.0 ? 1 : -1); }

    public static boolean equal(float x, float y) { return Math.abs(y - x) < 1.0E-5F; }
    public static boolean equal(double x, double y) { return Math.abs(y - x) < 1.0E-5F; }

    public static float length(float x, float y) { return (float) Math.sqrt(x * x + y * y); }
    public static double length(double x, double y) { return Math.sqrt(x * x + y * y); }
    public static float lengthSquared(float x, float y) { return x * x + y * y; }
    public static double lengthSquared(double x, double y) { return x * x + y * y; }

    public static int nextInt(RandomSource random, int min, int max) { return min >= max ? min : random.nextInt(max - min + 1) + min; }
    public static float nextFloat(RandomSource random, float min, float max) { return min >= max ? min : random.nextFloat() * (max - min) + min; }
    public static double nextDouble(RandomSource random, double min, double max) { return min >= max ? min : random.nextDouble() * (max - min) + min; }
    public static float randomBetween(RandomSource random, float min, float max) { return random.nextFloat() * (max - min) + min; }

    public static float positiveModulo(float numerator, float denominator) { return (numerator % denominator + denominator) % denominator; }
    public static double positiveModulo(double numerator, double denominator) { return (numerator % denominator + denominator) % denominator; }
    public static int positiveModulo(int x, int y) { return Math.floorMod(x, y); }

    public static float triangleWave(float input, float period) { return (Math.abs(input % period - period * 0.5F) - period * 0.25F) / (period * 0.25F); }

    /** h, s, v in 0..1; returns 0xRRGGBB. */
    public static int hsvToRgb(float hue, float saturation, float value) {
        int i = (int) (hue * 6.0F) % 6;
        float f = hue * 6.0F - (float) i;
        float f1 = value * (1.0F - saturation);
        float f2 = value * (1.0F - f * saturation);
        float f3 = value * (1.0F - (1.0F - f) * saturation);
        float r, g, b;
        switch (i) {
            case 0 -> { r = value; g = f3; b = f1; }
            case 1 -> { r = f2; g = value; b = f1; }
            case 2 -> { r = f1; g = value; b = f3; }
            case 3 -> { r = f1; g = f2; b = value; }
            case 4 -> { r = f3; g = f1; b = value; }
            case 5 -> { r = value; g = f1; b = f2; }
            default -> throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + hue + ", " + saturation + ", " + value);
        }
        return clamp((int) (r * 255.0F), 0, 255) << 16 | clamp((int) (g * 255.0F), 0, 255) << 8 | clamp((int) (b * 255.0F), 0, 255);
    }
}
