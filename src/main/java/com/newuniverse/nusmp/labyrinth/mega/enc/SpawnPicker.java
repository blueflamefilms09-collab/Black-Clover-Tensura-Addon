package com.newuniverse.nusmp.labyrinth.mega.enc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Why: where an encounter appears is half of what it feels like. Appear in front of the party and it is a jump-scare that looks like a bug (a
 * monster popping into existence); appear a hundred blocks away and nothing ever happens; appear in the safe room where the party is resting
 * and the dungeon has broken its own rule. The Dungeon Master's rule is therefore the one a game master uses behind the screen: the monsters
 * were already there, round the corner. Candidate cells (spawn markers, room floors) are filtered to those 24-48 blocks from the NEAREST member of
 * the party (never closer to anyone), outside the safe rooms, not too close to each other, and OUT OF SIGHT of every member; the survivors are
 * drawn by weight (a marker may ask to be preferred). The sight test is the one expensive thing here (a ray cast per member), so it is an
 * interface the game implements, is run in the order of the draw, and is capped per roll: the picker stops early rather than cast a hundred rays.
 * Pure (no Minecraft types) so every rule is tested with a fake line of sight.
 */
public final class SpawnPicker {
    private SpawnPicker() {}

    /** Can a player whose eyes are at (ex, ey, ez) see the cell? The game casts a ray from the eye to the cell's feet and head. */
    @FunctionalInterface
    public interface LineOfSight {
        boolean sees(double ex, double ey, double ez, int cx, int cy, int cz);
    }

    /** A place a monster could stand: the block it stands in (feet), how much it is preferred, the room it is in and whether that room is a safe one. */
    public record Cell(int x, int y, int z, double weight, int roomId, boolean safe) {}

    /** A party member's eyes. */
    public record Eye(double x, double y, double z) {}

    /** The rules of one pick: the distance band, the least distance between two picks, the cap on ray casts, and how many cells are wanted. */
    public record Params(double minDist, double maxDist, double minSeparation, int maxSightChecks, int want) {}

    /**
     * Up to {@code p.want()} cells that satisfy every rule, in the order they were drawn. {@code los} may be null (no sight rule: the tests of
     * the other rules, and a level with no light-of-sight to compute). An empty party gives an empty result: there is nobody to appear for.
     */
    public static List<Cell> pick(List<Cell> candidates, List<Eye> party, Params p, LineOfSight los, Random r) {
        List<Cell> out = new ArrayList<>();
        if (party.isEmpty() || p.want() <= 0) return out;
        List<double[]> keyed = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            Cell c = candidates.get(i);
            if (c.safe() || !(c.weight() > 0.0)) continue;
            double near = nearest(c, party);
            if (near < p.minDist() || near > p.maxDist()) continue;
            // weighted draw without replacement: the smallest of -ln(u) / weight comes first, so heavier cells tend to come earlier
            double key = -Math.log(1.0 - r.nextDouble()) / c.weight();
            keyed.add(new double[] {key, i});
        }
        keyed.sort(Comparator.comparingDouble(a -> a[0]));
        int checks = 0;
        for (double[] k : keyed) {
            if (out.size() >= p.want()) break;
            Cell c = candidates.get((int) k[1]);
            if (!separated(c, out, p.minSeparation())) continue;
            if (los != null) {
                boolean seen = false;
                for (Eye e : party) {
                    if (checks >= p.maxSightChecks()) return out;
                    checks++;
                    if (los.sees(e.x(), e.y(), e.z(), c.x(), c.y(), c.z())) {
                        seen = true;
                        break;
                    }
                }
                if (seen) continue;
            }
            out.add(c);
        }
        return out;
    }

    /** Distance from the middle of the cell to the nearest eye. */
    public static double nearest(Cell c, List<Eye> party) {
        double best = Double.MAX_VALUE;
        double cx = c.x() + 0.5, cy = c.y() + 0.5, cz = c.z() + 0.5;
        for (Eye e : party) {
            double dx = e.x() - cx, dy = e.y() - cy, dz = e.z() - cz;
            best = Math.min(best, Math.sqrt(dx * dx + dy * dy + dz * dz));
        }
        return best;
    }

    private static boolean separated(Cell c, List<Cell> picked, double minSeparation) {
        double s2 = minSeparation * minSeparation;
        for (Cell o : picked) {
            double dx = o.x() - c.x(), dy = o.y() - c.y(), dz = o.z() - c.z();
            if (dx * dx + dy * dy + dz * dz < s2) return false;
        }
        return true;
    }
}
