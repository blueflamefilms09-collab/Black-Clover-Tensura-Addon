package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: where a sector goes is geometry and arithmetic, and a mistake in it is the one that cannot be repaired afterwards (two sectors that overlap
 * overwrite each other for ever), so it lives here, pure, with tests that run it over thousands of random expansions. A sector is a square REGION of
 * {@code REGION_MODULES} modules (19 x 9 = 171 blocks) whose near edge is the plane of its Landing's entry door and which is centred on that door, so
 * the Landing's own module grid is the region's grid. Consecutive regions are never closer than {@link #GAP} blocks: the Way Onward ends at a rubble
 * seal, and the next Landing hangs from the end of a straight TUNNEL of whole 9-block segments that starts at the seal and is long enough to cross the
 * rest of the old region, the keep-clear margin of the old sector's door caps and the gap. Why a tunnel and not the Landing right behind the seal: the old
 * sector's door caps may protrude past its region (up to {@code CAP_MARGIN}), and a Landing touching the seal would share its walls with caps that were
 * placed long before it existed; with the gap, no cap of one sector can ever reach a piece of another, and two sectors can be generated, repaired or
 * regenerated independently. The same functions choose, for a candidate seal, the SHORTEST tunnel that leaves free space ({@link #planNext}), which is
 * how the Way Onward's variant (straight, left, right) is picked "towards free space".
 */
public final class SectorPlanner {
    private SectorPlanner() {}

    /**
     * The least clear distance between two regions, in blocks: the contract's {@link MegaGrid#REGION_GAP}, but never less than twice the reach of a door
     * cap, so the caps of two neighbouring regions cannot overlap each other either (each may protrude {@code CAP_MARGIN}).
     */
    public static final int GAP = Math.max(MegaGrid.REGION_GAP, 2 * MegaGrid.CAP_MARGIN);

    public static final String POOL_STRAIGHT = MegaGrid.P_ONWARD + "_straight", POOL_LEFT = MegaGrid.P_ONWARD + "_left", POOL_RIGHT = MegaGrid.P_ONWARD + "_right";

    /** World y of the jigsaw cell of every door (the first row above the floor). */
    public static final int DOOR_Y = MegaGrid.ORIGIN_Y + MegaGrid.DOOR_ROW;

    /**
     * Something a new region or tunnel must stay {@code clearance} blocks away from (horizontally). {@code region} tells a sector's region (kept
     * {@link #GAP} away) from a tunnel between two sectors (kept the reach of a door cap away); the planner also uses the regions to tell inward from outward.
     */
    public record Obstacle(Box box, int clearance, boolean region) {
        public Box grown() { return box.inflateXZ(clearance); }

        /** The region of a sector. */
        public static Obstacle region(Box box) { return new Obstacle(box, GAP, true); }

        /** The tunnel between two sectors. */
        public static Obstacle tunnel(Box box) { return new Obstacle(box, MegaGrid.CAP_MARGIN, false); }
    }

    /** What the planner decided for a sector. */
    public record Plan(int id, long seed, Vec entry, Facing forward, Box region, int linkModules) {
        /** The jigsaw cell of the Landing's entry door. */
        public Vec entryJigsaw() { return entry; }

        /** The floor cell of the doorway (what {@code Sector.entry} stores): one below the jigsaw cell. */
        public Vec entryFloor() { return entry.add(0, -1, 0); }
    }

    /** The answer of {@link #planNext}: how long the tunnel is, where the next Landing's door is and the next region. */
    public record Next(int linkModules, Vec entry, Facing forward, Box region, Box tunnel) {}

    // ---------------------------------------------------------------- seeds

    /** The seed all of sector {@code id} derives from. */
    public static long sectorSeed(long dungeonSeed, int id) { return Rng.mix(dungeonSeed, 0x5EC70000L + id); }

    /** The seed of try number {@code attempt} of a sector (try 0 is the first). */
    public static long trySeed(long sectorSeed, int attempt) { return Rng.mix(sectorSeed, 0x7A110000L + attempt); }

    // ---------------------------------------------------------------- regions

    /**
     * The region of a sector whose Landing's entry door is the jigsaw cell {@code entryJigsaw} and which grows towards {@code forward}: the near edge is the
     * plane of the door, the region runs {@code REGION_MODULES} modules forward and is centred on the door sideways (the door is at a module centre, so
     * the 19 modules of the region sit on the Landing's own module grid). Height: the full 48 rows of a piece.
     */
    public static Box regionFor(Vec entryJigsaw, Facing forward) {
        int len = MegaGrid.blocks(MegaGrid.REGION_MODULES);
        int half = len / 2;
        int y1 = MegaGrid.ORIGIN_Y, y2 = MegaGrid.ORIGIN_Y + MegaGrid.HEIGHT - 1;
        return switch (forward) {
            case EAST -> new Box(entryJigsaw.x(), y1, entryJigsaw.z() - half, entryJigsaw.x() + len - 1, y2, entryJigsaw.z() + half);
            case WEST -> new Box(entryJigsaw.x() - len + 1, y1, entryJigsaw.z() - half, entryJigsaw.x(), y2, entryJigsaw.z() + half);
            case SOUTH -> new Box(entryJigsaw.x() - half, y1, entryJigsaw.z(), entryJigsaw.x() + half, y2, entryJigsaw.z() + len - 1);
            case NORTH -> new Box(entryJigsaw.x() - half, y1, entryJigsaw.z() - len + 1, entryJigsaw.x() + half, y2, entryJigsaw.z());
        };
    }

    /** The plan of the first sector of the Holy road (1.54.0's sector 0): its Landing's entry door is the fixed cell (0, door row, 0) and the sector grows east. */
    public static Plan first(long dungeonSeed) { return first(dungeonSeed, 0, Polarity.HOLY); }

    /**
     * The plan of the first sector of a road (1.55.0): the Landing's entry door is the fixed cell {@code (0, door row, laneZ)} of the road's lane
     * ({@link MegaGrid#laneZ}: z 0 for the Holy road, {@link MegaGrid#LANE_Z} for the Abyssal road) and the sector grows east, so the two roads start side by
     * side at x 0 and never come near each other. {@code id} is the sector's global id (it only labels the plan); the seed belongs to the place in the road
     * ({@link SectorDraft#seedKey}), not to the id.
     */
    public static Plan first(long dungeonSeed, int id, Polarity road) {
        Vec entry = new Vec(0, DOOR_Y, MegaGrid.laneZ(road));
        return new Plan(id, sectorSeed(dungeonSeed, SectorDraft.seedKey(road, 0)), entry, Facing.EAST, regionFor(entry, Facing.EAST), 0);
    }

    /** The tunnel (as a keep-clear box) of {@code modules} 9-block segments that starts in front of the seal and runs towards {@code facing}. */
    public static Box tunnelBox(Vec seal, Facing facing, int modules) {
        Vec front = seal.step(facing);
        Vec last = front.step(facing, MegaGrid.MODULE * modules - 1);
        int half = MegaGrid.MODULE / 2;
        int y1 = MegaGrid.ORIGIN_Y, y2 = MegaGrid.ORIGIN_Y + MegaGrid.HEIGHT - 1;
        int x1 = Math.min(front.x(), last.x()), x2 = Math.max(front.x(), last.x()), z1 = Math.min(front.z(), last.z()), z2 = Math.max(front.z(), last.z());
        if (facing.alongX()) { z1 = seal.z() - half; z2 = seal.z() + half; }
        else { x1 = seal.x() - half; x2 = seal.x() + half; }
        return new Box(x1, y1, z1, x2, y2, z2);
    }

    /** How many tunnel segments lie between a seal and the Landing door that hangs behind them (both jigsaw cells; the tunnel runs along {@code facing}). */
    public static int linkModulesBetween(Vec seal, Facing facing, Vec nextEntryJigsaw) {
        int span = nextEntryJigsaw.along(facing) - seal.step(facing).along(facing);
        return span <= 0 ? 0 : span / MegaGrid.MODULE;
    }

    // ---------------------------------------------------------------- the next sector

    /**
     * Where the sector behind a seal would go. {@code seal} is the jigsaw cell of the Way Onward's sealed socket and {@code facing} the way it points;
     * {@code prevRegion} is the region of the sector that owns the seal; {@code others} are the regions and tunnels of every OTHER sector. The tunnel is
     * the shortest one, at least long enough to cross the rest of {@code prevRegion} and the {@link #GAP}, that leaves the new region clear of every
     * obstacle and does not itself cross one. {@code maxLinkModules} limits how far the tunnel may be stretched beyond that minimum to get round an older
     * sector (the minimum bridge is always allowed). Null when no length works: the seal faces a wall of older sectors.
     */
    public static Next planNext(Vec seal, Facing facing, Box prevRegion, List<Obstacle> others, int maxLinkModules) {
        Vec front = seal.step(facing);
        int need = GAP + 1 + prevRegion.far(facing) - front.along(facing);
        int min = Math.max(1, Math.floorDiv(need + MegaGrid.MODULE - 1, MegaGrid.MODULE));
        int max = Math.max(min, maxLinkModules);
        Obstacle prev = Obstacle.region(prevRegion);
        for (int m = min; m <= max; m++) {
            Box tunnel = tunnelBox(seal, facing, m);
            for (Obstacle o : others) if (tunnel.overlaps(o.grown())) return null;       // a longer tunnel contains this one: it cannot get better
            Vec entry = front.step(facing, MegaGrid.MODULE * m);
            Box region = regionFor(entry, facing);
            if (region.overlaps(prev.grown())) continue;
            boolean hit = false;
            for (Obstacle o : others) if (region.overlaps(o.grown())) { hit = true; break; }
            if (!hit) return new Next(m, entry, facing, region, tunnel);
        }
        return null;
    }

    // ---------------------------------------------------------------- the Way Onward policy

    /** The three variants of the Way Onward in a seeded order (the order is the tie-break when two variants need an equally long tunnel). */
    public static List<String> variantOrder(long seed) {
        List<String> v = new ArrayList<>(List.of(POOL_STRAIGHT, POOL_LEFT, POOL_RIGHT));
        new Rng(Rng.mix(seed, 0x0A17L)).shuffle(v);
        return v;
    }

    /** The cost added to a Way Onward candidate for every way out of the next region that is already blocked (see {@link #exits}); larger than any tunnel. */
    public static final int EXIT_PENALTY = 100;
    /**
     * The cost added to a Way Onward candidate that does not lead away from the middle of the explored dungeon: 0 when the next region lies a full sector pitch
     * farther out than this one, {@code RADIAL_WEIGHT} when it is no farther out, twice that when it is a pitch nearer in (the walk is curling back on itself).
     */
    public static final int RADIAL_WEIGHT = 40;

    /**
     * The assembler's {@link OnwardPolicy} for one sector: the seeded variant order, and for a candidate seal the length of the shortest tunnel
     * {@link #planNext} finds behind it plus {@link #EXIT_PENALTY} for each of the three ways on (straight, left, right) that the NEXT region could not leave
     * by, plus up to twice {@link #RADIAL_WEIGHT} when the next region does not lie farther from the middle of the explored dungeon than this one. Why the
     * penalties: a dungeon that only ever takes the shortest tunnel curls into a spiral of sectors with an unreachable middle and then cannot grow; choosing the
     * seal whose next region still has exits and leads away from the explored middle keeps the walk moving outward, where there is always room. They are
     * preferences, never rejections: a sector must still be built when every candidate is poor.
     */
    public static OnwardPolicy policy(long seed, Box region, List<Obstacle> others, int maxLinkModules) {
        final List<String> variants = variantOrder(seed);
        final List<Obstacle> frozen = List.copyOf(others);
        return new OnwardPolicy() {
            @Override
            public List<String> variants() { return variants; }

            @Override
            public int cost(Vec sealCell, Facing facing) {
                Next n = planNext(sealCell, facing, region, frozen, maxLinkModules);
                if (n == null) return -1;
                return n.linkModules() + EXIT_PENALTY * (3 - exits(n, region, frozen, maxLinkModules)) + radial(n, region, frozen);
            }
        };
    }

    /** The penalty for not leading outward: see {@link #RADIAL_WEIGHT}. Distances are from the centroid of all the regions, this one included. */
    static int radial(Next n, Box thisRegion, List<Obstacle> others) {
        double sx = thisRegion.centerX(), sz = thisRegion.centerZ();
        int count = 1;
        for (Obstacle o : others) {
            if (!o.region()) continue;
            sx += o.box().centerX();
            sz += o.box().centerZ();
            count++;
        }
        double cx = sx / count, cz = sz / count;
        double here = Math.hypot(thisRegion.centerX() - cx, thisRegion.centerZ() - cz);
        double there = Math.hypot(n.region().centerX() - cx, n.region().centerZ() - cz);
        double pitch = MegaGrid.blocks(MegaGrid.REGION_MODULES) + GAP;
        double t = Math.max(0.0, Math.min(2.0, 1.0 - (there - here) / pitch));
        return (int) Math.round(RADIAL_WEIGHT * t);
    }

    /**
     * How many of the three ways on (straight, left, right of the way {@code n} points) a sector in {@code n.region()} could still leave by, judged with a
     * seal in the middle of the far half of that edge of the region. An estimate (the real seal stands wherever the boss chain ends up) but a cheap and
     * conservative one: a region walled in on three sides scores 0 whatever the chain does.
     */
    static int exits(Next n, Box prevRegion, List<Obstacle> others, int maxLinkModules) {
        List<Obstacle> all = new ArrayList<>(others);
        all.add(Obstacle.region(prevRegion));
        all.add(Obstacle.tunnel(n.tunnel()));
        Box r = n.region();
        Facing f = n.forward();
        int len = MegaGrid.blocks(MegaGrid.REGION_MODULES);
        Vec centre = new Vec((r.minX() + r.maxX()) / 2, SectorPlanner.DOOR_Y, (r.minZ() + r.maxZ()) / 2).step(f, len / 4);
        int count = 0;
        for (Facing d : new Facing[] {f, f.cw(), f.ccw()}) {
            Vec seal = switch (d) {
                case EAST -> new Vec(r.maxX(), centre.y(), centre.z());
                case WEST -> new Vec(r.minX(), centre.y(), centre.z());
                case SOUTH -> new Vec(centre.x(), centre.y(), r.maxZ());
                case NORTH -> new Vec(centre.x(), centre.y(), r.minZ());
            };
            if (planNext(seal, d, r, all, maxLinkModules) != null) count++;
        }
        return count;
    }

    /** The obstacles a new sector must keep out of for the assembler: every other region grown by the reach of a door cap, so no cap of the new sector meets an old one. */
    public static List<Box> exclusionsFor(Box region, List<Obstacle> others) {
        List<Box> out = new ArrayList<>();
        Box reach = region.inflateXZ(MegaGrid.CAP_MARGIN + MegaGrid.MODULE);
        for (Obstacle o : others) {
            Box g = o.box().inflateXZ(MegaGrid.CAP_MARGIN);
            if (g.overlaps(reach)) out.add(g);
        }
        return out;
    }
}
