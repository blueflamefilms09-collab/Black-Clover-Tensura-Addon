package com.newuniverse.nusmp.arc.spade;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Why: Kneel Before the King can only be fair if there is somewhere to stand. The pockets of still air must be near enough that a party can reach them
 * in the wind-up (5 to 9 blocks from the foe they are laid for), far enough from the King that nobody has to hug him, and apart from each other so that
 * a party has to spread out and not pile into one. Planning that is arithmetic on positions, so it is pure and seeded: the same seed gives the same
 * pockets, and the tests can check every rule on thousands of seeds. The game side snaps each candidate to the floor and drops the ones that have no
 * standing room, which is why it asks for a few more candidates than it needs.
 */
public final class PocketMath {
    private PocketMath() {}

    /** Nearest a pocket may be to the King's middle beyond the pocket's own radius, and the pocket's distance from the foe it is laid for. */
    public static final double MIN_FROM_BOSS = 4.0, NEAR = 5.0, FAR = 9.0;

    /**
     * Plans up to {@code count} pockets (x, z). Pocket {@code i} is laid for foe {@code i mod foes}: a random angle and a distance of {@link #NEAR} to
     * {@link #FAR} blocks from that foe, tried up to 16 times, accepted when it is at least {@code MIN_FROM_BOSS + radius} from the King and
     * {@code 2 * radius + 1.5} from every pocket already accepted. A pocket that cannot be placed after the tries is left out, so the result can be
     * shorter than {@code count} (never longer).
     */
    public static List<double[]> plan(double[][] foes, double bossX, double bossZ, int count, double radius, long seed) {
        List<double[]> out = new ArrayList<>();
        if (foes == null || foes.length == 0 || count <= 0) return out;
        Random rng = new Random(seed);
        double minBoss = MIN_FROM_BOSS + radius, minApart = 2 * radius + 1.5;
        for (int i = 0; i < count; i++) {
            double[] f = foes[i % foes.length];
            for (int tries = 0; tries < 16; tries++) {
                double a = rng.nextDouble() * Math.PI * 2, d = NEAR + rng.nextDouble() * (FAR - NEAR);
                double x = f[0] + Math.cos(a) * d, z = f[1] + Math.sin(a) * d;
                if (Math.hypot(x - bossX, z - bossZ) < minBoss) continue;
                boolean apart = true;
                for (double[] p : out) if (Math.hypot(x - p[0], z - p[1]) < minApart) { apart = false; break; }
                if (!apart) continue;
                out.add(new double[]{x, z});
                break;
            }
        }
        return out;
    }
}
