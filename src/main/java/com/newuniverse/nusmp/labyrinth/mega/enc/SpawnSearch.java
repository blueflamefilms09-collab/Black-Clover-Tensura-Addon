package com.newuniverse.nusmp.labyrinth.mega.enc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Why: a ring of 24 to 48 blocks round the party, out of sight, is the right place for an encounter and, on a real floor, often holds nothing: the party stands
 * in a corner of the floor, or in a hall whose neighbours are all within sight, or the markers of the nearby halls were all in line of sight. Giving up there
 * (the first version did, silently) turns a rolled encounter into nothing, which is indistinguishable from "encounters do not trigger". So the search is a ladder:
 * the configured ring first (exactly the old behaviour); then a wider ring; then every cell of every room that is not a safe room, corridors and doorways
 * included; and finally, only after the ladder has failed {@code relaxFromTier} times, the sight rule is eased to "not seen from nearer than three quarters of the
 * ring's far edge" (a monster that appears at the far end of a long hall is not a monster that pops into existence in front of the party). Nothing ever appears
 * nearer than {@link #HARD_MIN} blocks, and a safe room is never a candidate at any rung. The result says which rung worked and carries a trace of what each rung
 * saw, which is what {@code /multiverse mega dm why} prints. Pure.
 */
public final class SpawnSearch {
    private SpawnSearch() {}

    /** No encounter appears nearer than this to any member of the party, on any rung. */
    public static final double HARD_MIN = 8.0;
    /** The least distance between two chosen cells. */
    static final double SEPARATION = 4.0;
    /** Extra cells asked of the picker beyond those wanted: the spawner falls back on them when a chosen cell turns out to be unusable in the world. */
    static final int RESERVE = 6;

    /**
     * One rung of the ladder. {@code relaxSightFrom} is 0 when the full sight rule holds; otherwise a cell that a member can see is accepted when it is at least
     * that far from the nearest member.
     */
    public record Tier(int index, String name, double min, double max, CellFinder.Mode mode, double relaxSightFrom) {
        public String range() { return String.format(Locale.ROOT, "%.0f-%.0f", min, max); }
    }

    /** What the search found: the cells, the reserve cells, the rung that worked (null when none did) and one line per rung tried. */
    public record Found(List<SpawnPicker.Cell> cells, List<SpawnPicker.Cell> reserve, Tier tier, List<String> trace) {
        public boolean ok() { return !cells.isEmpty(); }
    }

    /** The rungs for a ring of {@code min} to {@code max} blocks. {@code widen = false} gives only the configured ring. {@code relaxFromTier} 4 or more never eases the sight rule. */
    public static List<Tier> tiers(double min, double max, boolean widen, int relaxFromTier) {
        double lo = Math.max(HARD_MIN, Math.min(min, max));
        double hi = Math.max(lo + 4.0, max);
        double wideLo = Math.max(HARD_MIN, lo * 0.6);
        double relax = Math.max(wideLo + 4.0, hi * 0.75);
        List<Tier> out = new ArrayList<>(4);
        out.add(new Tier(0, "ring", lo, hi, CellFinder.Mode.LEGACY, relaxFromTier <= 0 ? relax : 0.0));
        if (!widen) return out;
        out.add(new Tier(1, "wide ring", wideLo, hi * 1.5, CellFinder.Mode.LEGACY, relaxFromTier <= 1 ? relax : 0.0));
        out.add(new Tier(2, "every room", wideLo, hi * 1.5, CellFinder.Mode.DENSE, relaxFromTier <= 2 ? relax : 0.0));
        out.add(new Tier(3, "far and in view", wideLo, hi * 2.0, CellFinder.Mode.DENSE, relaxFromTier <= 3 ? relax : 0.0));
        return out;
    }

    /**
     * Climbs the ladder until a rung gives at least one cell. {@code checksPerEye} bounds the ray casts of each rung (it is multiplied by the number of members).
     * {@code los} may be null (no sight rule: the tests of the other rules).
     */
    public static Found find(SpawnLayout layout, List<SpawnPicker.Eye> eyes, double min, double max, int want, SpawnPicker.LineOfSight los, int checksPerEye,
                             boolean widen, int relaxFromTier, Random r) {
        List<String> trace = new ArrayList<>();
        if (eyes.isEmpty() || want <= 0) return new Found(List.of(), List.of(), null, List.of("nobody to appear for"));
        List<Integer> partyRooms = new ArrayList<>();
        for (SpawnPicker.Eye e : eyes) {
            SpawnLayout.RoomBox room = layout.roomAt((int) Math.floor(e.x()), (int) Math.floor(e.y()), (int) Math.floor(e.z()));
            if (room == null) room = layout.roomAtXZ((int) Math.floor(e.x()), (int) Math.floor(e.z()));
            if (room != null && !partyRooms.contains(room.id())) partyRooms.add(room.id());
        }
        Map<Integer, Integer> hops = partyRooms.isEmpty() ? null : layout.hopsFrom(partyRooms);
        int checks = Math.max(1, checksPerEye) * eyes.size();
        for (Tier t : tiers(min, max, widen, relaxFromTier)) {
            List<SpawnPicker.Cell> cands = CellFinder.candidates(layout, eyes, t.max() + 16.0, t.mode(), hops, r);
            SpawnPicker.LineOfSight sight = los;
            if (los != null && t.relaxSightFrom() > 0.0) {
                double from = t.relaxSightFrom();
                sight = (ex, ey, ez, cx, cy, cz) -> {
                    double dx = ex - (cx + 0.5), dy = ey - (cy + 0.5), dz = ez - (cz + 0.5);
                    return Math.sqrt(dx * dx + dy * dy + dz * dz) < from && los.sees(ex, ey, ez, cx, cy, cz);        // far enough away, a seen cell is allowed
                };
            }
            List<SpawnPicker.Cell> got = SpawnPicker.pick(cands, eyes, new SpawnPicker.Params(t.min(), t.max(), SEPARATION, checks, want + RESERVE), sight, r);
            trace.add(t.name() + " " + t.range() + (t.relaxSightFrom() > 0.0 ? " (seen cells allowed from " + (int) t.relaxSightFrom() + ")" : "") + ": " + cands.size() + " candidates, " + got.size() + " usable");
            if (!got.isEmpty()) {
                List<SpawnPicker.Cell> cells = new ArrayList<>(got.subList(0, Math.min(want, got.size())));
                List<SpawnPicker.Cell> reserve = new ArrayList<>(got.subList(cells.size(), got.size()));
                return new Found(cells, reserve, t, trace);
            }
        }
        return new Found(List.of(), List.of(), null, trace);
    }
}
