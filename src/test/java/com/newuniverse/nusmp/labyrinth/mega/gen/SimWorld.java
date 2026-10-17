package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: a sector is only half of the problem; the other half is that sectors must fit together for ever. This grows a whole simulated dungeon the way the
 * game does (plan the first sector, solve it, take the seal of its Way Onward, plan the next sector behind it with the tunnel in between, and so on) using
 * the same pure functions as the generator, so the tests and the simulator can check, over many sectors and many seeds, that regions never come closer than the
 * gap, that no piece of one sector touches a piece of another, and that the dungeon can keep growing. Since 1.55.0 it grows two roads in one world, like the game:
 * a sector belongs to a road and a place in it ({@link Entry#road}, {@link Entry#chainIndex}), ids are global (the number of sectors built so far), the first
 * sector of each road stands on that road's lane, and a sector keeps out of the sectors of its own road only ({@link WorldObstacles}). The single-road calls
 * ({@link #first()}, {@link #next()}, {@link #grow}) are the Holy road, which is what every 1.54.0 test grows. It is test code and holds no game state.
 */
final class SimWorld {
    /** One simulated sector; {@code prevId} is the id of the sector it hangs from (-1 for the first of a road). */
    record Entry(int id, Polarity road, int chainIndex, int prevId, SectorPlanner.Plan plan, RegionTheme theme, int relics, SectorRequests.Prepared prepared,
                 SectorSolver.Solved solved, Vec prevSeal, Facing prevFacing, SectorPlanner.Obstacle tunnel) {
        Result result() { return solved.result(); }
    }

    final PoolSource source;
    final long dungeonSeed;
    final Recipe recipe;
    final int maxPieces;
    final int maxLinkModules;
    final int tries;
    final List<Entry> sectors = new ArrayList<>();
    String stopped;

    SimWorld(PoolSource source, long dungeonSeed, Recipe recipe, int maxPieces, int maxLinkModules, int tries) {
        this.source = source;
        this.dungeonSeed = dungeonSeed;
        this.recipe = recipe;
        this.maxPieces = maxPieces;
        this.maxLinkModules = maxLinkModules;
        this.tries = tries;
    }

    /** The sectors of one road in chain order. */
    List<Entry> chain(Polarity road) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : sectors) if (e.road() == road) out.add(e);
        return out;
    }

    /** The newest sector of a road, or null when the road has none. */
    Entry tail(Polarity road) {
        Entry best = null;
        for (Entry e : sectors) if (e.road() == road) best = e;
        return best;
    }

    /** The regions and tunnels of the sectors of one road planned so far. */
    List<SectorPlanner.Obstacle> obstacles(Polarity road) {
        List<SectorPlanner.Obstacle> out = new ArrayList<>();
        for (Entry e : sectors) {
            if (e.road() != road) continue;
            out.add(SectorPlanner.Obstacle.region(e.plan().region()));
            if (e.tunnel() != null) out.add(e.tunnel());
        }
        return out;
    }

    /** The regions and tunnels of the sectors of the Holy road planned so far. */
    List<SectorPlanner.Obstacle> obstacles() { return obstacles(Polarity.HOLY); }

    /** Plans and solves the first sector of the Holy road. */
    boolean first() { return first(Polarity.HOLY); }

    /** Plans and solves the first sector of a road, at that road's lane origin. */
    boolean first(Polarity road) {
        int id = sectors.size();
        SectorPlanner.Plan plan = SectorPlanner.first(dungeonSeed, id, road);
        return add(road, 0, plan, null, null, null, null);
    }

    /** Plans and solves the sector behind the newest sector's Way Onward on the Holy road; false (and {@link #stopped} says why) when it cannot. */
    boolean next() { return next(Polarity.HOLY); }

    /** Plans and solves the sector behind the Way Onward of the newest sector of a road; false (and {@link #stopped} says why) when it cannot. */
    boolean next(Polarity road) {
        Entry prev = tail(road);
        if (prev == null) {
            stopped = "the " + road.id() + " road has no sector";
            return false;
        }
        if (prev.solved().result() == null || prev.result().expansion == null) {
            stopped = "sector " + prev.id() + " has no Way Onward";
            return false;
        }
        Result.Expansion ex = prev.result().expansion;
        if (ex.deadEnd()) {
            stopped = "dead end: the seal of sector " + prev.id() + " faces older sectors";
            return false;
        }
        List<SectorPlanner.Obstacle> others = new ArrayList<>();
        for (Entry e : sectors) {
            if (e.road() != road || e == prev) continue;
            others.add(SectorPlanner.Obstacle.region(e.plan().region()));
            if (e.tunnel() != null) others.add(e.tunnel());
        }
        if (prev.tunnel() != null) others.add(prev.tunnel());
        SectorPlanner.Next n = SectorPlanner.planNext(ex.seal(), ex.facing(), prev.plan().region(), others, maxLinkModules);
        if (n == null) {
            stopped = "no room behind the seal of sector " + prev.id();
            return false;
        }
        int id = sectors.size();
        int chainIndex = prev.chainIndex() + 1;
        SectorPlanner.Plan plan = new SectorPlanner.Plan(id, SectorPlanner.sectorSeed(dungeonSeed, SectorDraft.seedKey(road, chainIndex)), n.entry(), n.forward(), n.region(), n.linkModules());
        return add(road, chainIndex, plan, prev, ex.seal(), ex.facing(), prev.plan().region());
    }

    private boolean add(Polarity road, int chainIndex, SectorPlanner.Plan plan, Entry prev, Vec prevSeal, Facing prevFacing, Box prevRegion) {
        int id = plan.id();
        SectorDraft draft = SectorDraft.of(dungeonSeed, id, road, chainIndex, FloorMap.DEFAULT_PER_REGION, prev == null ? null : prev.theme(), null);
        List<SectorPlanner.Obstacle> others = obstacles(road);      // the tunnel INTO this sector is built by prepare(); every older tunnel of the road is an obstacle
        SectorRequests.Inputs in = new SectorRequests.Inputs(source, recipe, maxPieces, maxLinkModules, plan.seed(), plan.entry(), plan.forward(), plan.region(),
                prevSeal, prevFacing, prevRegion, others, draft.relicCount());
        SectorRequests.Prepared prep = SectorRequests.prepare(in);
        if (!prep.ok()) {
            stopped = "sector " + id + ": " + prep.failure();
            return false;
        }
        SectorSolver.Solved solved = SectorSolver.solve(prep.request(), tries);
        sectors.add(new Entry(id, road, chainIndex, prev == null ? -1 : prev.id(), plan, draft.theme(), draft.relicCount(), prep, solved, prevSeal, prevFacing, prep.tunnelObstacle()));
        if (!solved.ok()) {
            stopped = "sector " + id + " could not be built: " + SectorSolver.summary(solved.failures(), 3);
            return false;
        }
        return true;
    }

    /** Grows the Holy road to {@code count} sectors; false as soon as one cannot be built. */
    boolean grow(int count) {
        if (!first()) return false;
        for (int i = 1; i < count; i++) if (!next()) return false;
        return true;
    }

    /**
     * Grows both roads to {@code count} sectors each, the way players open them: the first sectors of both, then one sector more on each road in turn, so the global ids
     * interleave (Holy 0, Abyssal 1, Holy 2, Abyssal 3 ...). False as soon as one cannot be built.
     */
    boolean growRoads(int count) {
        if (!first(Polarity.HOLY) || !first(Polarity.ABYSSAL)) return false;
        for (int i = 1; i < count; i++) if (!next(Polarity.HOLY) || !next(Polarity.ABYSSAL)) return false;
        return true;
    }
}
