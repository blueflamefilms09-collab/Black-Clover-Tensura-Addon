package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: a sector is only half of the problem; the other half is that sectors must fit together for ever. This grows a whole simulated dungeon the way the
 * game does (plan the first sector, solve it, take the seal of its Way Onward, plan the next sector behind it with the tunnel in between, and so on) using
 * the same pure functions as the generator, so the tests and the simulator can check, over many sectors and many seeds, that regions never come closer than the
 * gap, that no piece of one sector touches a piece of another, and that the dungeon can keep growing. It is test code and holds no game state.
 */
final class SimWorld {
    /** One simulated sector. */
    record Entry(int id, SectorPlanner.Plan plan, RegionTheme theme, int relics, SectorRequests.Prepared prepared, SectorSolver.Solved solved,
                 Vec prevSeal, Facing prevFacing, SectorPlanner.Obstacle tunnel) {
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

    /** The regions and tunnels of the sectors planned so far, except the tunnel into sector {@code except} (which is that sector's own). */
    List<SectorPlanner.Obstacle> obstacles() {
        List<SectorPlanner.Obstacle> out = new ArrayList<>();
        for (Entry e : sectors) {
            out.add(SectorPlanner.Obstacle.region(e.plan().region()));
            if (e.tunnel() != null) out.add(e.tunnel());
        }
        return out;
    }

    /** Plans and solves the first sector. */
    boolean first() {
        SectorPlanner.Plan plan = SectorPlanner.first(dungeonSeed);
        return add(plan, null, null, null);
    }

    /** Plans and solves the sector behind the newest sector's Way Onward; false (and {@link #stopped} says why) when it cannot. */
    boolean next() {
        Entry prev = sectors.get(sectors.size() - 1);
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
        for (int i = 0; i < sectors.size() - 1; i++) {
            Entry e = sectors.get(i);
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
        SectorPlanner.Plan plan = new SectorPlanner.Plan(id, SectorPlanner.sectorSeed(dungeonSeed, id), n.entry(), n.forward(), n.region(), n.linkModules());
        return add(plan, ex.seal(), ex.facing(), prev.plan().region());
    }

    private boolean add(SectorPlanner.Plan plan, Vec prevSeal, Facing prevFacing, Box prevRegion) {
        int id = plan.id();
        RegionTheme prevTheme = id == 0 ? null : sectors.get(id - 1).theme();
        RegionTheme theme = RegionTheme.pick(dungeonSeed, id, prevTheme);
        int relics = theme.relicCount(dungeonSeed, id);
        List<SectorPlanner.Obstacle> others = new ArrayList<>();
        for (Entry e : sectors) {                          // the tunnel INTO this sector is built by prepare(); every older tunnel is an obstacle
            others.add(SectorPlanner.Obstacle.region(e.plan().region()));
            if (e.tunnel() != null) others.add(e.tunnel());
        }
        SectorRequests.Inputs in = new SectorRequests.Inputs(source, recipe, maxPieces, maxLinkModules, plan.seed(), plan.entry(), plan.forward(), plan.region(),
                prevSeal, prevFacing, prevRegion, others, relics);
        SectorRequests.Prepared prep = SectorRequests.prepare(in);
        if (!prep.ok()) {
            stopped = "sector " + id + ": " + prep.failure();
            return false;
        }
        SectorSolver.Solved solved = SectorSolver.solve(prep.request(), tries);
        sectors.add(new Entry(id, plan, theme, relics, prep, solved, prevSeal, prevFacing, prep.tunnelObstacle()));
        if (!solved.ok()) {
            stopped = "sector " + id + " could not be built: " + SectorSolver.summary(solved.failures(), 3);
            return false;
        }
        return true;
    }

    /** Grows the dungeon to {@code count} sectors; false as soon as one cannot be built. */
    boolean grow(int count) {
        if (!first()) return false;
        for (int i = 1; i < count; i++) if (!next()) return false;
        return true;
    }
}
