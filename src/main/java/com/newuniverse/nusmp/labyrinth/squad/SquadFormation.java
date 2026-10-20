package com.newuniverse.nusmp.labyrinth.squad;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Why: a squad that walks as a clump is a pack, and one that walks in a line is a queue; the formation is what makes the tanks arrive first and the healer last. A
 * slot is a pair (blocks ahead of the squad's centre, blocks to its right) in the squad's own frame, so the same table serves a squad facing any way and the
 * conversion to the world is {@link SquadGeometry#slot}. Tanks stand in an arc at the front, the leader just behind them, melee damage a step further back, flankers
 * on the wings, ranged damage behind the line and supports at the rear; each slot is nudged by a few tenths of a block drawn from the squad's seed so two squads of
 * the same plan do not stand on identical spots, and the same seed always gives the same table. {@link #offsets} guarantees no two slots closer than 1.3 blocks (a
 * mob is about 0.6 wide, and two mobs told to stand on one spot shove each other for ever).
 */
public final class SquadFormation {
    private SquadFormation() {}

    /** A place in the squad's own frame: {@code forward} blocks ahead of the centre, {@code lateral} blocks to its right (negative = left). */
    public record Offset(double forward, double lateral) {}

    /** Smallest distance between two slots, in blocks. */
    public static final double MIN_GAP = 1.3;

    /** The offsets of a plan's roles, in the order of the roles. */
    public static List<Offset> offsets(List<SquadRole> roles, long seed) {
        SplittableRandom r = new SplittableRandom(seed * 31 + 7);
        int n = roles.size();
        List<Offset> out = new ArrayList<>(n);
        int[] seen = new int[SquadRole.values().length];
        int[] total = new int[SquadRole.values().length];
        for (SquadRole role : roles) total[role.ordinal()]++;
        for (SquadRole role : roles) {
            int k = seen[role.ordinal()]++;
            int of = total[role.ordinal()];
            double[] base = base(role, k, of);
            double jf = (r.nextDouble() - 0.5) * 0.5, jl = (r.nextDouble() - 0.5) * 0.5;
            out.add(new Offset(base[0] + jf, base[1] + jl));
        }
        return spread(out);
    }

    /** The unjittered slot of the k-th of {@code of} members of a role: spread across the width of the role's rank, centred. */
    private static double[] base(SquadRole role, int k, int of) {
        double centred = k - (of - 1) / 2.0;                  // -0.5, +0.5 for two; -1, 0, +1 for three
        return switch (role) {
            case TANK -> new double[]{3.5 - Math.abs(centred) * 0.4, centred * 2.4};
            case LEADER -> new double[]{1.8, centred * 2.0};
            case DPS_MELEE -> new double[]{0.6, centred * 2.6 + (of == 1 ? 1.8 : 0.0)};
            case FLANKER -> new double[]{0.0, (k % 2 == 0 ? -1 : 1) * (4.8 + (k / 2) * 1.6)};
            case DPS_RANGED -> new double[]{-2.4, centred * 2.8 + (of == 1 ? -1.2 : 0.0)};
            case SUPPORT -> new double[]{-4.6, centred * 2.4};
        };
    }

    /** Pushes any two slots closer than {@link #MIN_GAP} apart along the line between them, a few passes at most (the slots are few, the cost is nothing). */
    private static List<Offset> spread(List<Offset> in) {
        double[][] p = new double[in.size()][2];
        for (int i = 0; i < p.length; i++) {
            p[i][0] = in.get(i).forward();
            p[i][1] = in.get(i).lateral();
        }
        for (int pass = 0; pass < 12; pass++) {
            boolean moved = false;
            for (int i = 0; i < p.length; i++) {
                for (int j = i + 1; j < p.length; j++) {
                    double dx = p[j][0] - p[i][0], dz = p[j][1] - p[i][1];
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d >= MIN_GAP + 0.02) continue;
                    if (d < 1e-6) {
                        dx = 0.0;
                        dz = 1.0;
                        d = 1.0;
                    }
                    double push = (MIN_GAP + 0.05 - d) / 2.0;
                    p[i][0] -= dx / d * push;
                    p[i][1] -= dz / d * push;
                    p[j][0] += dx / d * push;
                    p[j][1] += dz / d * push;
                    moved = true;
                }
            }
            if (!moved) break;
        }
        List<Offset> out = new ArrayList<>(p.length);
        for (double[] q : p) out.add(new Offset(q[0], q[1]));
        return out;
    }
}
