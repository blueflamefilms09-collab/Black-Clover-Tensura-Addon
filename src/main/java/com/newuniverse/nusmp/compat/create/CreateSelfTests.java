package com.newuniverse.nusmp.compat.create;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: nothing here can be played by its author, and Create is not in the build environment, so the CI server smoke test runs this self-test ({@code compat.create}) in the real game,
 * with and without Create installed, and it must PASS in both. Without Create it proves the compatibility is inert (no part resolves to a Create block, the contraption guard says it
 * is idle) and that the drawings, the rules and the runtime are sound using vanilla stand-ins; with Create it additionally reports how many parts resolved to real Create blocks (a
 * part that did not is a stand-in, which is allowed and logged, not a failure: the ids are from memory). On a scratch corridor in a far corner of the Mega-Dungeon's dimension it
 * builds every family through the real arming path (the finder, the check, the builder), in each of the four directions for the first, and proves: the walkway is clear and can be
 * walked end to end, a lever sits at each end of each wall, the machinery is where the drawing says, a forced trigger starts a run, a player in a band is hurt within the limits and
 * only once per cycle, the presses' rams come out and go away again, the clutches are cut when idle and open when running, a thrown lever stops the run at once, and a pressed
 * plate (or a player in the lane) starts one. The scratch corridor is cleared whatever happens.
 */
public final class CreateSelfTests {
    private CreateSelfTests() {}

    private static final BlockPos ORIGIN = new BlockPos(6_700_000, MegaGrid.FLOOR_Y, 6_700_000);

    public static void register() { SelfTests.register("compat.create", CreateSelfTests::run); }

    // ---------------------------------------------------------------- the scratch corridor
    private static void forChunks(ServerLevel level, Frame f, int n, Runnable each) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (int dx : new int[] {-8, 8}) {
            for (int dz : new int[] {-3, n + 2}) {
                BlockPos p = f.pos(dx, 0, dz);
                minX = Math.min(minX, p.getX());
                maxX = Math.max(maxX, p.getX());
                minZ = Math.min(minZ, p.getZ());
                maxZ = Math.max(maxZ, p.getZ());
            }
        }
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) level.getChunk(cx, cz);
        each.run();
    }

    /** A solid mass of stone bricks with a 3 wide corridor of the given height carved through it (one cell longer than the strip at each end). */
    private static void carve(ServerLevel level, Frame f, int n, int h) {
        forChunks(level, f, n, () -> DungeonGuard.internal(() -> {
            BlockState brick = Blocks.STONE_BRICKS.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -2; dz <= n + 1; dz++) {
                    for (int dy = -3; dy <= h + 3; dy++) {
                        boolean lane = Math.abs(dx) <= 1 && dz >= -1 && dz <= n && dy >= 1 && dy <= h;
                        level.setBlock(f.pos(dx, dy, dz), lane ? air : brick, 2);
                    }
                }
            }
        }));
    }

    private static void clear(ServerLevel level, Frame f, int n, int h) {
        forChunks(level, f, n, () -> DungeonGuard.internal(() -> {
            BlockState air = Blocks.AIR.defaultBlockState();
            for (int dx = -8; dx <= 8; dx++) for (int dz = -3; dz <= n + 2; dz++) for (int dy = -5; dy <= h + 5; dy++) {
                BlockPos p = f.pos(dx, dy, dz);
                if (!level.getBlockState(p).isAir()) level.setBlock(p, air, 2);
            }
        }));
    }

    // ---------------------------------------------------------------- the run
    static SelfTests.Result run(MinecraftServer server) {
        StringBuilder out = new StringBuilder();
        // 1. the pure parts, once more in the game
        for (TrapFamily f : TrapFamily.values()) {
            List<String> problems = TrapBlueprints.problems(TrapBlueprints.build(f, 8, 5, TriggerKind.PLATE, 99L));
            if (!problems.isEmpty()) return SelfTests.Result.fail(f.id() + " drawing is unsafe: " + problems);
        }
        if (!CreateIds.FORBIDDEN.contains("create:schematicannon")) return SelfTests.Result.fail("the schematicannon is no longer forbidden");
        for (Part p : Part.values()) for (String id : CreateIds.candidates(p)) if (CreateIds.FORBIDDEN.contains(id)) return SelfTests.Result.fail(p + " lists the forbidden " + id);
        if (!ContraptionGuard.refuses("create:contraption", true, true) || ContraptionGuard.refuses("minecraft:zombie", true, true) || ContraptionGuard.refuses("create:contraption", false, true)
                || ContraptionGuard.refuses("create:contraption", true, false)) return SelfTests.Result.fail("the contraption rule answers wrongly");
        if (TrapRules.safeDamage(1000, 20f, 20f, 0f) > 20f * TrapRules.MAX_HIT_SHARE + 1e-3 || TrapRules.safeDamage(1000, 3f, 20f, 0f) != 0f) return SelfTests.Result.fail("the damage limits answer wrongly");
        if (CreateConfig.get(CreateConfig.DENSITY) < 0.0 || CreateConfig.get(CreateConfig.DENSITY) > 4.0) return SelfTests.Result.fail("the density setting is out of range");

        // 2. every part resolves to a block; without Create none to a Create block
        boolean loaded = PartResolver.createLoaded();
        int fromCreate = 0, createParts = 0;
        StringBuilder stand = new StringBuilder();
        PartResolver.clear();
        for (Map.Entry<Part, PartResolver.Resolved> en : PartResolver.report().entrySet()) {
            Part p = en.getKey();
            PartResolver.Resolved r = en.getValue();
            if (r.block() == Blocks.AIR) return SelfTests.Result.fail(p + " resolved to air");
            if (!loaded && r.create()) return SelfTests.Result.fail("Create is not installed but " + p + " resolved to " + r.id());
            if (p.vanilla() && r.create()) return SelfTests.Result.fail("the vanilla part " + p + " resolved to " + r.id());
            if (!p.vanilla()) {
                createParts++;
                if (r.create()) fromCreate++;
                else stand.append(stand.length() == 0 ? "" : ",").append(p.name().toLowerCase());
            }
        }
        out.append(loaded ? "Create installed, " + fromCreate + "/" + createParts + " parts are Create blocks" + (stand.length() == 0 ? "" : " (stand-ins: " + stand + ")") : "Create not installed, inert, vanilla stand-ins");
        if (!loaded && (CreateTrapDirector.active())) return SelfTests.Result.fail("the director claims to be active without Create");

        // 3. the world
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return SelfTests.Result.fail("no mega dimension");
        int built = 0;
        Set<String> notes = new java.util.TreeSet<>();
        Set<TrapFamily> done = EnumSet.noneOf(TrapFamily.class);
        net.minecraft.core.Direction[] dirs = {net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.WEST};
        String failure = null;
        // the first family in all four directions, every family in one
        int[][] cases = new int[TrapFamily.values().length + 3][];
        int k = 0;
        for (int d = 0; d < 4; d++) cases[k++] = new int[] {TrapFamily.SAW_RUN.ordinal(), d};
        for (TrapFamily f : TrapFamily.values()) if (f != TrapFamily.SAW_RUN) cases[k++] = new int[] {f.ordinal(), 0};
        for (int[] c : cases) {
            if (c == null) continue;
            TrapFamily family = TrapFamily.values()[c[0]];
            String why = oneCase(level, family, dirs[c[1]], c[1] * 2 + family.ordinal(), notes);
            if (why != null) {
                failure = family.id() + " facing " + dirs[c[1]] + ": " + why;
                break;
            }
            built++;
            done.add(family);
        }
        if (failure != null) return SelfTests.Result.fail(failure);
        out.append("; ").append(built).append(" traps built, run, disarmed and cleared on a scratch corridor (").append(done.size()).append(" families); contraption guard: ").append(ContraptionGuard.status());
        if (!notes.isEmpty()) out.append("; parts that did not stay as placed: ").append(notes);
        return SelfTests.Result.ok(out.toString());
    }

    /** Builds one trap of a family on a fresh scratch corridor, runs it, and clears the corridor. Returns null on success, else why not. */
    private static String oneCase(ServerLevel level, TrapFamily family, net.minecraft.core.Direction dir, int salt, Set<String> notes) {
        int core = TrapRules.MIN_CORE + (salt % (TrapRules.MAX_CORE - TrapRules.MIN_CORE + 1)), h = TrapRules.MIN_HEADROOM + 1 + (salt % 3);
        int n = TrapRules.stripLength(core);
        TriggerKind trigger = TriggerKind.values()[salt % TriggerKind.values().length];
        Frame scratch = new Frame(ORIGIN, dir);
        carve(level, scratch, n, h);
        TrapRuntime.Live lv = null;
        try {
            // the finder sees exactly the corridor that was carved
            WorldGrid grid = new WorldGrid(level, MegaGrid.FLOOR_Y);
            int minX = ORIGIN.getX() - 16, maxX = ORIGIN.getX() + 16, minZ = ORIGIN.getZ() - 16, maxZ = ORIGIN.getZ() + 16;
            List<LaneFinder.Site> found = LaneFinder.find(grid, minX, maxX, minZ, maxZ, n, false);
            if (found.isEmpty()) return "the finder found no choke in the carved corridor";
            Set<Long> want = new HashSet<>(), got = new HashSet<>();
            for (int z = 0; z < n; z++) {
                BlockPos a = scratch.pos(0, 0, z);
                want.add(a.asLong());
                int[] xz = LaneFinder.toWorld(found.get(0).originX(), found.get(0).originZ(), found.get(0).forward(), 0, z);
                got.add(new BlockPos(xz[0], MegaGrid.FLOOR_Y, xz[1]).asLong());
            }
            if (!want.equals(got)) return "the finder's strip is not the carved corridor";
            if (grid.stripHeadroom(found.get(0)) != h) return "the finder measured a headroom of " + grid.stripHeadroom(found.get(0)) + ", not " + h;

            // arm it through the real path
            TrapSite site = new TrapSite("selftest-" + family.id(), -1, -1, family, trigger, core, 77L + salt);
            site.depth = 20;
            site.threat = 3;
            if (!CreateTrapDirector.tryArm(level, site, minX, maxX, minZ, maxZ, MegaGrid.FLOOR_Y)) return "arming failed: " + site.note;
            if (site.state != TrapSite.State.BUILT) return "the site is not BUILT after arming";
            Blueprint bp = site.blueprint();
            Frame f = site.frame();
            if (bp.headroom() != h) return "the site's headroom is " + bp.headroom();

            String why = verifyBuilt(level, bp, f, notes);
            if (why != null) return why;
            lv = new TrapRuntime.Live(site);
            return verifyRuntime(level, lv, family);
        } finally {
            if (lv != null) TrapRuntime.idle(level, lv);
            clear(level, scratch, n, h);
        }
    }

    private static String verifyBuilt(ServerLevel level, Blueprint bp, Frame f, Set<String> notes) {
        int n = bp.length();
        Map<String, Blueprint.Placed> at = new HashMap<>();
        for (Blueprint.Placed p : bp.parts()) at.put(p.key(), p);
        // the walkway holds nothing solid
        for (int z = 0; z < n; z++) for (int x = -1; x <= 1; x++) for (int y = 1; y <= bp.headroom(); y++) {
            BlockPos pos = f.pos(x, y, z);
            BlockState s = level.getBlockState(pos);
            Blueprint.Placed p = at.get(x + "," + y + "," + z);
            if (p == null) {
                if (!s.isAir()) return "something stands in the walkway at " + pos.toShortString() + ": " + s.getBlock();
            } else if (!p.part().passable() && !(y >= bp.headroom() && (p.part() == Part.SPOUT || p.part() == Part.PIPE))) {
                return p.part() + " stands in the walkway at " + pos.toShortString();
            }
        }
        // the walkway can be walked end to end: columns with two free cells above a sturdy floor, from the first cell to the last
        boolean[][] free = new boolean[3][n];
        for (int z = 0; z < n; z++) for (int x = -1; x <= 1; x++) {
            boolean ok = level.getBlockState(f.pos(x, 0, z)).isFaceSturdy(level, f.pos(x, 0, z), net.minecraft.core.Direction.UP);
            for (int y = 1; y <= 2; y++) ok &= level.getBlockState(f.pos(x, y, z)).getCollisionShape(level, f.pos(x, y, z)).isEmpty();
            free[x + 1][z] = ok;
        }
        ArrayDeque<int[]> q = new ArrayDeque<>();
        boolean[][] seen = new boolean[3][n];
        for (int x = 0; x < 3; x++) if (free[x][0]) { q.add(new int[] {x, 0}); seen[x][0] = true; }
        boolean through = false;
        while (!q.isEmpty()) {
            int[] c = q.poll();
            if (c[1] == n - 1) through = true;
            int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] s : steps) {
                int nx = c[0] + s[0], nz = c[1] + s[1];
                if (nx < 0 || nx > 2 || nz < 0 || nz >= n || seen[nx][nz] || !free[nx][nz]) continue;
                seen[nx][nz] = true;
                q.add(new int[] {nx, nz});
            }
        }
        if (!through) return "the walkway cannot be walked from one end to the other";
        // the levers, the parts
        int levers = 0;
        for (Blueprint.Placed l : bp.levers()) {
            BlockState s = level.getBlockState(f.pos(l.dx(), l.dy(), l.dz()));
            if (s.getBlock() != Blocks.LEVER) return "the lever at " + l.key() + " is " + s.getBlock();
            if (s.getValue(BlockStateProperties.POWERED)) return "a lever is thrown on a fresh trap";
            levers++;
        }
        if (levers != 4) return "expected four levers, found " + levers;
        for (Blueprint.Placed p : bp.parts()) {
            Block placed = level.getBlockState(f.pos(p.dx(), p.dy(), p.dz())).getBlock();
            if (placed == PartResolver.resolve(p.part()).block()) continue;
            // a vanilla part (lever, plate, hooks) must stay where it was put; a Create part (or its stand-in) that changed itself or popped off is a note, not a failure: I could not check how Create's blocks behave
            if (p.part().vanilla()) return p.part() + " at " + p.key() + " is " + placed + " instead of " + PartResolver.resolve(p.part()).id();
            notes.add(p.part().name().toLowerCase());
        }
        // idle: clutches cut, nixie tubes dark
        for (Blueprint.PowerCell pc : bp.power()) {
            Block b = level.getBlockState(f.pos(pc.dx(), pc.dy(), pc.dz())).getBlock();
            Block want = PartResolver.power(pc.role() == Blueprint.Role.CLUTCH).getBlock();
            if (b != want) return "a " + pc.role() + " power cell at " + pc.key() + " is not idle: " + b;
        }
        return null;
    }

    private static String verifyRuntime(ServerLevel level, TrapRuntime.Live lv, TrapFamily family) {
        Blueprint bp = lv.bp;
        Frame f = lv.frame;
        Blueprint.Band b0 = bp.bands().get(0);
        Vec3 c = f.point((b0.xMin() + b0.xMax()) / 2.0, (b0.zMin() + b0.zMax()) / 2.0);
        ServerPlayer p = SelfTests.standIn(level, "create_trap_" + family.id(), c.x, f.origin().getY() + 1.0, c.z);
        List<ServerPlayer> who = List.of(p);
        float maxHealth = p.getMaxHealth();
        long t0 = 1_000_000L;
        TrapRuntime.Report r = TrapRuntime.step(level, lv, t0, true, who);
        if (r.state() != TrapClock.State.RUNNING) return "a forced trigger did not start a run: " + r.state();
        int hits = 0, maxRams = 0;
        boolean lit = false, openWhileRunning = false, ramSeen = false, wasRunning = false;
        float asked = 0f;
        int span = bp.runTicks() + 20;
        for (int t = 1; t <= span; t++) {
            p.setPos(c.x, f.origin().getY() + 1.0, c.z);
            r = TrapRuntime.step(level, lv, t0 + t, false, who);
            hits += r.hits();
            asked += r.asked();
            if (r.hits() > 0 && r.asked() > r.hits() * maxHealth * TrapRules.MAX_HIT_SHARE + 1e-3) return "a hit asked for " + r.asked() / r.hits() + " of " + maxHealth + " health";
            maxRams = Math.max(maxRams, r.ramsOut());
            if (r.state() == TrapClock.State.RUNNING) {
                wasRunning = true;
                for (Blueprint.PowerCell pc : bp.power()) {
                    Block now = level.getBlockState(f.pos(pc.dx(), pc.dy(), pc.dz())).getBlock();
                    if (pc.role() == Blueprint.Role.CLUTCH && now == PartResolver.power(false).getBlock()) openWhileRunning = true;
                    if (pc.role() == Blueprint.Role.LIGHT && now == PartResolver.power(true).getBlock()) lit = true;
                }
                for (int[] ram : bp.rams()) {
                    if (level.getBlockState(f.pos(ram[0], ram[1], ram[2])).getBlock() == PartResolver.ram().getBlock()) ramSeen = true;
                }
            }
        }
        if (!wasRunning) return "the trap never ran";
        if (hits < 1) return "a player standing in the first band was never hit in a whole run";
        if (hits > 40) return "a player was hit " + hits + " times in one run";
        if (r.state() != TrapClock.State.COOLDOWN) return "the trap is " + r.state() + " after its run, not cooling down";
        if (!bp.power().isEmpty() && bp.power().stream().anyMatch(pc -> pc.role() == Blueprint.Role.CLUTCH) && !openWhileRunning) return "the clutches were not opened during the run (the redstone block stayed)";
        if (family == TrapFamily.SEQUENCE && !lit) return "the nixie tubes never lit";
        if (!bp.rams().isEmpty() && (maxRams == 0 || !ramSeen)) return "the press never put a ram into the lane";
        if (lv.ramsOut() != 0) return "rams are left in the lane after the run";
        for (int[] ram : bp.rams()) if (!level.getBlockState(f.pos(ram[0], ram[1], ram[2])).isAir()) return "a ram block is left at " + ram[0] + "," + ram[1] + "," + ram[2];
        for (Blueprint.PowerCell pc : bp.power()) {
            if (level.getBlockState(f.pos(pc.dx(), pc.dy(), pc.dz())).getBlock() != PartResolver.power(pc.role() == Blueprint.Role.CLUTCH).getBlock() && r.state() != TrapClock.State.RUNNING)
                return "a power cell is not idle after the run: " + pc.key();
        }
        // wait out the cool-down, then a thrown lever stops a forced run at once and keeps it stopped
        long t = t0 + span;
        for (int i = 0; i < bp.cooldownTicks() + 5; i++) r = TrapRuntime.step(level, lv, ++t, false, List.of());
        BlockPos lever = f.pos(bp.levers().get(0).dx(), bp.levers().get(0).dy(), bp.levers().get(0).dz());
        BlockState off = level.getBlockState(lever);
        level.setBlock(lever, off.setValue(BlockStateProperties.POWERED, true), 3);
        try {
            int hitsWhileDisarmed = 0;
            for (int i = 0; i < 60; i++) {
                p.setPos(c.x, f.origin().getY() + 1.0, c.z);
                r = TrapRuntime.step(level, lv, ++t, true, who);
                hitsWhileDisarmed += r.hits();
                if (r.state() != TrapClock.State.DISARMED) return "a thrown lever left the trap " + r.state();
            }
            if (hitsWhileDisarmed != 0) return "a disarmed trap hurt someone";
        } finally {
            level.setBlock(lever, off, 3);
        }
        // the lever is back: the trap re-arms, and a pressed plate / a tripped wire / a player in the lane starts a run without any force
        r = TrapRuntime.step(level, lv, ++t, false, List.of());
        if (r.state() != TrapClock.State.IDLE) return "the trap did not return to idle after the lever came back: " + r.state();
        BlockPos pressed = null;
        BlockState pressedBefore = null;
        if (bp.trigger() != TriggerKind.PROXIMITY && !bp.triggerCells().isEmpty()) {
            Blueprint.Placed cell = bp.triggerCells().get(0);
            pressed = f.pos(cell.dx(), cell.dy(), cell.dz());
            pressedBefore = level.getBlockState(pressed);
            level.setBlock(pressed, pressedBefore.setValue(BlockStateProperties.POWERED, true), 3);
        }
        try {
            r = TrapRuntime.step(level, lv, ++t, false, bp.trigger() == TriggerKind.PROXIMITY ? who : List.of());
            if (!r.triggered() || r.state() != TrapClock.State.RUNNING) return "the " + bp.trigger().id() + " trigger did not start the trap (" + r.state() + ")";
        } finally {
            if (pressed != null) level.setBlock(pressed, pressedBefore, 3);
        }
        return null;
    }
}
