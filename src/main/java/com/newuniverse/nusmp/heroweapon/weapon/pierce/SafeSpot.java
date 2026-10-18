package com.newuniverse.nusmp.heroweapon.weapon.pierce;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: three techniques of the Legendary Spear and Bow move the wielder to a place they did not walk to (the Time Reversal back to where they stood, the
 * Portal Spear to the point they aim at, a retreat hop), and a move that drops a person into a wall, a lava pool or the air over a pit is worse than no
 * move. The rule is the owner's: the spot must be air above a solid floor and not inside blocks; if the wanted spot is not like that, the nearest spot that
 * is, within a small radius; if there is none, the technique is refused. The search is pure (it asks a {@link Probe} whether a block position can be
 * stood in) so the rule is tested against hand-built worlds, and the game side only has to answer the one question.
 */
public final class SafeSpot {
    private SafeSpot() {}

    /** The world, reduced to the one question the search needs. */
    @FunctionalInterface
    public interface Probe {
        /** True when a person can stand with their feet in the block (x, y, z): room for their body, nothing harmful, a solid floor below. */
        boolean standable(int x, int y, int z);
    }

    /** A block position a person can stand in. */
    public record Spot(int x, int y, int z) {}

    private record Offset(int dx, int dy, int dz, int d2) {}

    /**
     * The standable block nearest to (cx, cy, cz) within {@code radius} blocks (distance between block centres), or null when there is none. Nearest first;
     * a tie goes to the smaller vertical difference, then to the smaller dx, then dz, so the answer never depends on the order the probe is asked in.
     */
    public static Spot nearest(Probe probe, int cx, int cy, int cz, double radius) {
        double r = Math.max(0.0, radius);
        int reach = (int) Math.floor(r);
        List<Offset> offsets = new ArrayList<>();
        for (int dy = -reach; dy <= reach; dy++)
            for (int dx = -reach; dx <= reach; dx++)
                for (int dz = -reach; dz <= reach; dz++) {
                    int d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 <= r * r) offsets.add(new Offset(dx, dy, dz, d2));
                }
        offsets.sort(Comparator.comparingInt(Offset::d2).thenComparingInt((Offset o) -> Math.abs(o.dy())).thenComparingInt(Offset::dx).thenComparingInt(Offset::dz));
        for (Offset o : offsets) if (probe.standable(cx + o.dx(), cy + o.dy(), cz + o.dz())) return new Spot(cx + o.dx(), cy + o.dy(), cz + o.dz());
        return null;
    }
}
