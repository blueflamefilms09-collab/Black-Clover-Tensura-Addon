package net.minecraft.util;

/** Preview stub (java.util.Random; real Minecraft uses a different generator, so patterns differ but are as random). */
public final class RandomSource {
    private final java.util.Random r;
    private RandomSource(long seed) { r = new java.util.Random(seed); }
    public static RandomSource create(long seed) { return new RandomSource(seed); }
    public float nextFloat() { return r.nextFloat(); }
    public double nextDouble() { return r.nextDouble(); }
    public int nextInt(int n) { return r.nextInt(n); }
    public boolean nextBoolean() { return r.nextBoolean(); }
    public double nextGaussian() { return r.nextGaussian(); }
}
