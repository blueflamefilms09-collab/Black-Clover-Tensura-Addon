package com.newuniverse.nusmp.avatar.chatter;

import java.util.Random;

/**
 * Why: the chatter picks lines at random, but its tests must be deterministic and the pure classes must not import Minecraft; this tiny
 * interface is all the randomness they need. The game adapts {@code RandomSource} to it, tests pass {@link #seeded}.
 */
public interface Rng {
    /** Uniform in [0, bound). */
    int nextInt(int bound);

    /** Uniform in [0, 1). */
    double nextDouble();

    static Rng seeded(long seed) {
        Random r = new Random(seed);
        return new Rng() {
            @Override public int nextInt(int bound) { return r.nextInt(bound); }
            @Override public double nextDouble() { return r.nextDouble(); }
        };
    }
}
