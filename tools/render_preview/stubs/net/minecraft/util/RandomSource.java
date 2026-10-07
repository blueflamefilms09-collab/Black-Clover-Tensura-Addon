package net.minecraft.util;

/**
 * Preview stub of Minecraft's RandomSource. {@code RandomSource.create(seed)} in the game is a LegacyRandomSource, which is the same
 * linear congruential generator as java.util.Random, so nextInt / nextFloat / nextDouble / nextBoolean / nextLong give the same sequence
 * for the same seed (the preview uses java.util.Random directly).
 */
public final class RandomSource {
    private final java.util.Random random;

    private RandomSource(long seed) { this.random = new java.util.Random(seed); }

    public static RandomSource create() { return new RandomSource(System.nanoTime()); }
    public static RandomSource create(long seed) { return new RandomSource(seed); }

    public RandomSource fork() { return new RandomSource(random.nextLong()); }
    public void setSeed(long seed) { random.setSeed(seed); }
    public int nextInt() { return random.nextInt(); }
    public int nextInt(int bound) { return random.nextInt(bound); }
    public int nextIntBetweenInclusive(int min, int max) { return random.nextInt(max - min + 1) + min; }
    public long nextLong() { return random.nextLong(); }
    public boolean nextBoolean() { return random.nextBoolean(); }
    public float nextFloat() { return random.nextFloat(); }
    public double nextDouble() { return random.nextDouble(); }
    public double nextGaussian() { return random.nextGaussian(); }
    public double triangle(double center, double maxDeviation) { return center + maxDeviation * (nextDouble() - nextDouble()); }
}
