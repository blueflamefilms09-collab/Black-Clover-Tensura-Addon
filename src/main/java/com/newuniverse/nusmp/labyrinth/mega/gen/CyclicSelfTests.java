package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaConfig;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.CategoryBalance;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.LoopConfig;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.RoomGraph;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.Roles;
import com.newuniverse.nusmp.labyrinth.mega.floors.FloorsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;

/**
 * Why: the cyclic generator is a change to how every NEW floor is built, and nothing about it can be watched by its author, so the CI smoke test runs this
 * self-test ({@code /multiverse mega selftest gen.cyclic}) in the real game, on the real pools and templates, in three parts. (1) The generator: a handful of probe
 * floors (nothing is placed or saved) are planned once the old way and once the cyclic way from the same request and seed; both must pass the full audit without the
 * fallback layout, the cyclic plan must have no more dead ends and no fewer rooms on a cycle than the old one, must repeat exactly when planned again, must agree with
 * the game's own rotation maths for every piece ({@link GeometryCheck}, hallway pieces included) and must keep the category mix no worse than the old plan's.
 * (2) The save: {@code Sector.cyclic} round-trips through NBT, and a sector saved without it reads as not cyclic (so an old world keeps the old generator on
 * every regeneration). (3) The world: for every finished sector that was created cyclic, its plan is made again from the saved data, must equal what was saved, and
 * every doorway that joins a hallway piece (and every loop doorway) must be OPEN in the world, a 3 x 4 hole through all four wall layers with nothing solid in it:
 * no wall behind a door. The detail line carries the measured numbers.
 */
final class CyclicSelfTests {
    private CyclicSelfTests() {}

    static void register() {
        SelfTests.register("gen.cyclic", CyclicSelfTests::cyclic);
    }

    private static SelfTests.Result fail(String d) { return SelfTests.Result.fail(d); }

    private static SelfTests.Result ok(String d) { return SelfTests.Result.ok(d); }

    private static boolean floorModel() { return FloorMap.isFloorModel(FloorMap.floorsPerSector(FloorsConfig.get(FloorsConfig.PER_SECTOR))); }

    private static SelfTests.Result cyclic(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return fail("no mega dimension");
        VanillaPools pools = VanillaPools.get();
        pools.bind(level.getStructureManager());
        if (pools.poolIds().isEmpty()) return fail("no Mega pools are loaded");
        MegaData data = MegaData.get(server);

        // ---- (2) the saved flag
        Sector t = new Sector(-9_001);
        t.cyclic = true;
        if (!Sector.load(t.save()).cyclic) return fail("Sector.cyclic does not survive a save and load");
        Sector u = new Sector(-9_002);
        if (u.save().contains("cyc")) return fail("a sector that is not cyclic writes a 'cyc' tag (an old save would not be byte for byte the same)");
        if (Sector.load(u.save()).cyclic) return fail("a sector saved without 'cyc' reads as cyclic (an old world would change generator)");

        // ---- (1) the generator: old and cyclic from the same request
        LoopConfig cfg = CyclicSettings.loopConfig();
        int tries = MegaConfig.get(MegaConfig.ASSEMBLE_TRIES);
        boolean floors = floorModel();
        int probes = 0, hallways = 0, links = 0, pruned = 0;
        double deadOld = 0, deadNew = 0, cycOld = 0, cycNew = 0, roomsOld = 0, roomsNew = 0;
        for (Polarity road : Polarity.values()) {
            for (int n = 0; n < 2; n++) {
                long seed = data.seed ^ 0x43594C4943L ^ (n * 0x9E3779B97F4A7C15L);
                String who = "the " + road.id() + " probe " + n;
                SectorGenerator.Planned base = floors ? SectorGenerator.probeFloor(pools, seed, road, n * (FloorMap.BOSS_EVERY - 1), System.nanoTime()) : SectorGenerator.probe(level, seed, road);
                if (!base.ok()) return fail(who + " cannot be assembled: " + base.failure());
                Request off = base.prepared().request().withSeed(base.prepared().request().seed, false);
                off.cyclic = null;
                Request on = off.withSeed(off.seed, false);
                on.cyclic = cfg;
                SectorSolver.Solved a = SectorSolver.solve(off, tries), b = SectorSolver.solve(on, tries);
                if (!a.ok() || a.fallback()) return fail(who + ": the old generator cannot build it any more: " + SectorSolver.summary(a.failures(), 3));
                if (!b.ok() || b.fallback()) return fail(who + ": the cyclic generator needed the fallback layout or failed after " + b.tries() + " tries: " + SectorSolver.summary(b.failures(), 3));
                SectorSolver.Solved again = SectorSolver.solve(on, tries);
                if (!again.ok() || !fingerprint(again.result()).equals(fingerprint(b.result()))) return fail(who + ": the cyclic plan is not deterministic (planning it again gave another plan)");
                List<String> audit = SectorAudit.audit(b.result(), on.withSeed(b.result().seed, false), true);
                if (!audit.isEmpty()) return fail(who + ": the cyclic plan fails the audit: " + String.join("; ", audit));
                RoomGraph ga = CyclicGraphs.of(a.result()), gb = CyclicGraphs.of(b.result());
                RoomGraph.Report ra = ga.report(Roles.TERMINAL), rb = gb.report(Roles.TERMINAL);
                if (!gb.connected()) return fail(who + ": the cyclic plan's room graph is not connected");
                if (rb.deadEndRatio() > ra.deadEndRatio() + 1e-9) return fail(who + ": the cyclic plan has MORE dead ends than the old one (" + pct(rb.deadEndRatio()) + " against " + pct(ra.deadEndRatio()) + ")");
                if (rb.cycleShare() + 1e-9 < ra.cycleShare()) return fail(who + ": fewer rooms are on a cycle than in the old plan (" + pct(rb.cycleShare()) + " against " + pct(ra.cycleShare()) + ")");
                double da = CategoryBalance.distanceToTarget(ga.categoryCounts()), db = CategoryBalance.distanceToTarget(gb.categoryCounts());
                if (db > da + 0.08) return fail(who + ": the category mix is further from the target than the old plan's (" + String.format(Locale.ROOT, "%.2f against %.2f", db, da) + ")");
                GeometryCheck.Report geo = GeometryCheck.run(level, SectorGenerator.build(level, new SectorGenerator.Planned(base.prepared(), b, 0L, null)), 0);
                if (!geo.ok()) return fail(who + ": the game disagrees with the cyclic plan: " + String.join(", ", geo.failures()));
                int through = 0;
                for (Placed p : b.result().pieces) if (CyclicGraphs.throughHallway(b.result(), p)) through++;
                int made = b.result().stats.getOrDefault("cyclic.hallways", 0);
                if (through < made) return fail(who + ": " + made + " hallway rooms were built but only " + through + " pass-through corridor pieces join two rooms");
                probes++;
                hallways += made;
                links += b.result().stats.getOrDefault("cyclic.links", 0);
                pruned += b.result().stats.getOrDefault("cyclic.pruned", 0);
                deadOld += ra.deadEndRatio();
                deadNew += rb.deadEndRatio();
                cycOld += ra.cycleShare();
                cycNew += rb.cycleShare();
                roomsOld += ra.rooms();
                roomsNew += rb.rooms();
            }
        }

        // ---- (3) the world: cyclic sectors that exist
        int sectors = 0, checkedDoorways = 0, openedLoops = 0, oldSectors = 0;
        for (Sector s : data.sectors()) {
            if (!SectorGenerator.playable(s)) continue;
            if (!s.cyclic) {
                oldSectors++;
                continue;
            }
            if (sectors >= 3) continue;
            SectorGenerator.Planned p = SectorGenerator.plan(level, data, s);
            if (!p.ok()) return fail("sector " + s.id + " (cyclic) cannot be planned again from its saved data: " + p.failure());
            if (p.prepared().request().cyclic == null) return fail("sector " + s.id + " is saved as cyclic but its plan is not");
            String diff = CoreSelfTests.compareSaved(s, p.result());
            if (diff != null) return fail("sector " + s.id + " (cyclic) as saved differs from the plan its seed gives now: " + diff);
            sectors++;
            Result r = p.result();
            for (Result.Loop l : r.loops) {
                Placed pa = r.pieces.get(l.pieceA()), pb = r.pieces.get(l.pieceB());
                int solid = solidInDoorway(level, pa.socketCell(l.socketA()), pa.socketFacing(l.socketA()));
                boolean ours = CyclicGraphs.throughHallway(r, pa) || CyclicGraphs.throughHallway(r, pb);
                if (ours) {
                    checkedDoorways++;
                    if (solid > 0) return fail("sector " + s.id + ": a loop doorway at " + pa.socketCell(l.socketA()) + " into a hallway has " + solid + " solid blocks in its 3 x 4 opening: a wall behind a door");
                } else if (solid == 0) openedLoops++;
            }
            for (Placed c : r.pieces) {
                if (c.parent < 0 || c.shape.isCap() || r.pieces.get(c.parent).shape.isCap()) continue;
                Placed par = r.pieces.get(c.parent);
                if (!CyclicGraphs.throughHallway(r, c) && !CyclicGraphs.throughHallway(r, par)) continue;
                checkedDoorways++;
                int solid = solidInDoorway(level, par.socketCell(c.parentSocket), par.socketFacing(c.parentSocket));
                if (solid > 0) return fail("sector " + s.id + ": the doorway between " + par.shape.id + " and the hallway piece " + c.shape.id + " at " + par.socketCell(c.parentSocket) + " has " + solid + " solid blocks in its opening");
            }
        }
        String world = sectors == 0 ? "no finished cyclic sector yet to read the world of (" + oldSectors + " older sector(s) are untouched)"
                : sectors + " cyclic sector(s): " + checkedDoorways + " hallway doorways and " + openedLoops + " other loop doorways open in the world; " + oldSectors + " older sector(s) untouched";
        return ok(String.format(Locale.ROOT, "%d probe plans old vs cyclic: dead ends %.1f%% -> %.1f%%, rooms on a cycle %.1f%% -> %.1f%%, rooms %.1f -> %.1f, %.1f links, %.1f hallway rooms, %.1f trimmed per plan; deterministic, audited, agree with the game; flag round-trips; %s; switch generation.cyclic=%s",
                probes, deadOld / probes * 100, deadNew / probes * 100, cycOld / probes * 100, cycNew / probes * 100, roomsOld / probes, roomsNew / probes, links / (double) probes,
                hallways / (double) probes, pruned / (double) probes, world, CyclicSettings.enabled()));
    }

    private static String pct(double v) { return String.format(Locale.ROOT, "%.1f%%", v * 100); }

    private static String fingerprint(Result r) {
        StringBuilder b = new StringBuilder();
        for (Placed p : r.pieces) b.append(p.index).append(' ').append(p.shape.id).append(" r").append(p.rot).append(' ').append(p.origin).append(" p").append(p.parent).append(':').append(p.parentSocket).append('\n');
        return b.toString();
    }

    /**
     * How many blocks in the doorway are solid. The doorway runs through four wall layers (two of each room); the jigsaw cell {@code cell} is the bottom-centre of the OUTER
     * layer of the piece it belongs to, so the line is: the cell behind it, the cell, the cell in front, the cell after that, each 3 wide and 4 tall.
     */
    static int solidInDoorway(ServerLevel level, Vec cell, Facing facing) {
        int solid = 0;
        Facing lateral = facing.cw();
        for (int along = -1; along <= 2; along++) {
            for (int side = -1; side <= 1; side++) {
                for (int up = 0; up < MegaGrid.DOOR_H; up++) {
                    BlockPos pos = new BlockPos(cell.x() + facing.dx * along + lateral.dx * side, cell.y() + up, cell.z() + facing.dz * along + lateral.dz * side);
                    BlockState bs = level.getBlockState(pos);
                    if (!bs.getCollisionShape(level, pos).isEmpty()) solid++;
                }
            }
        }
        return solid;
    }
}
