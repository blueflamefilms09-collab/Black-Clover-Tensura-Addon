package com.newuniverse.nusmp.dungeon.sentient;

import java.util.ArrayList;
import java.util.List;

/**
 * Chooses where the walls of a room give birth: from the standing spots next to a wall, those between a least and a most distance from
 * the player, spaced apart from one another, preferring the ones behind the player's back (so the Dungeon closes in from where they came,
 * not from where they look). The preference is a weighted draw, not a sorting: the spots behind the player are likelier, the ones beside
 * and ahead are rarer but still possible, so the walls do not always give way in one line. Pure.
 */
public final class SpotPick {
    private SpotPick() {}

    /** A block position, kept apart from the game's so this stays pure. */
    public record Spot(int x, int y, int z) {}

    /**
     * The weight of a spot in the draw, by how much it lies in the player's look direction (dot: -1 straight behind, 0 beside, +1 straight
     * ahead): 1.15 straight behind, 0.4 beside, 0.15 straight ahead.
     */
    static double weight(double dot) {
        double t = 1.0 - Math.max(-1.0, Math.min(1.0, dot));
        return 0.15 + t * t / 4.0;
    }

    /**
     * @param lookX the horizontal part of the player's look direction (any length; 0, 0 means no preference)
     * @return at most {@code count} spots, each between {@code minDist} and {@code maxDist} of the player and at least {@code spacing} from every other
     */
    public static List<Spot> pick(List<Spot> candidates, double px, double py, double pz, double lookX, double lookZ, int count,
                                  double minDist, double maxDist, double spacing, Dice dice) {
        List<Spot> out = new ArrayList<>();
        if (count <= 0 || candidates.isEmpty()) return out;
        double lookLen = Math.sqrt(lookX * lookX + lookZ * lookZ);
        double min2 = minDist * minDist, max2 = maxDist * maxDist, gap2 = spacing * spacing;
        List<Scored> pool = new ArrayList<>();
        for (Spot s : candidates) {
            double dx = s.x() + 0.5 - px, dy = s.y() - py, dz = s.z() + 0.5 - pz;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < min2 || d2 > max2) continue;
            double flat = Math.sqrt(dx * dx + dz * dz);
            double dot = flat > 1.0e-6 && lookLen > 1.0e-6 ? (dx * lookX + dz * lookZ) / (flat * lookLen) : 0.0;
            pool.add(new Scored(s, -Math.log(1.0 - dice.next()) / weight(dot)));         // an exponential race: the lower the time, the earlier the spot; a heavier spot is faster
        }
        pool.sort((a, b) -> Double.compare(a.time, b.time));
        for (Scored c : pool) {
            boolean apart = true;
            for (Spot t : out) {
                double dx = t.x() - c.spot.x(), dy = t.y() - c.spot.y(), dz = t.z() - c.spot.z();
                if (dx * dx + dy * dy + dz * dz < gap2) {
                    apart = false;
                    break;
                }
            }
            if (!apart) continue;
            out.add(c.spot);
            if (out.size() >= count) break;
        }
        return out;
    }

    private record Scored(Spot spot, double time) {}
}
