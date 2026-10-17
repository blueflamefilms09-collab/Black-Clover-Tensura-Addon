package com.newuniverse.nusmp.labyrinth.mega.macro;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing here can be played by the author, so the CI smoke test boots the real server and runs these four checks of the Hub's generator in the real
 * game: {@code macro.model} (the pure model of the LIVE world seed: the gates are where the layout says, the doors are open, a walking search reaches both gates),
 * {@code macro.wfc} (both Wave Function Collapse problems re-solved for the live seed: no fallback, within a time budget), {@code macro.generate} (the real generator,
 * through the real chunk pipeline, produces what the model promised at the plaza, at the Chasm's centre and at the Spire's door) and {@code macro.structures}
 * (no chunk of the Hub has a structure start). Each skips with an OK line when the sanctuary level does not exist (a build without the hub module) and with
 * landmarks switched off in the config it checks what is left. They take a few seconds in total and unload what they loaded.
 */
final class MacroSelfTests {
    private MacroSelfTests() {}

    static void register() {
        SelfTests.register("macro.model", MacroSelfTests::model);
        SelfTests.register("macro.wfc", MacroSelfTests::wfc);
        SelfTests.register("macro.generate", MacroSelfTests::generate);
        SelfTests.register("macro.structures", MacroSelfTests::structures);
    }

    private static ServerLevel level(MinecraftServer server) { return server.getLevel(Labyrinth.SANCTUARY_DIMENSION); }

    private static SelfTests.Result skipped() { return SelfTests.Result.ok("skipped: this build has no nusmp:sanctuary level"); }

    private static SelfTests.Result fail(String s) { return SelfTests.Result.fail(s); }

    private static String at(int x, int y, int z) { return x + " " + y + " " + z; }

    // ------------------------------------------------------------------------------------------------ the pure model
    private static SelfTests.Result model(MinecraftServer server) {
        ServerLevel level = level(server);
        if (level == null) return skipped();
        MacroParams params = MacroConfig.params();
        HubModel m = HubModel.of(SanctuaryGenerator.hubSeed(level.getChunkSource().randomState()), params);
        int hx = HubLayout.HOLY_GATE_X, hy = HubLayout.HOLY_GATE_Y, hz = HubLayout.HOLY_GATE_Z, ax = HubLayout.ABYSS_GATE_X, ay = HubLayout.ABYSS_GATE_Y, az = HubLayout.ABYSS_GATE_Z;
        if (m.kindAt(hx, hy, hz) != BlockKind.GATE) return fail("the Holy gate centre " + at(hx, hy, hz) + " is " + m.kindAt(hx, hy, hz) + ", not GATE");
        if (m.kindAt(ax, ay, az) != BlockKind.GATE) return fail("the Abyssal gate centre " + at(ax, ay, az) + " is " + m.kindAt(ax, ay, az) + ", not GATE");
        if (!params.landmarks()) return SelfTests.Result.ok("landmarks are off in the config: both gates stand at their coordinates on their small platforms");
        for (int dy = 0; dy < 3; dy++) {
            if (m.kindAt(HubLayout.SPIRE_DOOR_X, HubLayout.SURFACE_Y + dy, HubLayout.SPIRE_DOOR_Z) != BlockKind.AIR) return fail("the Spire's doorway is blocked at height +" + dy);
            if (m.kindAt(HubLayout.CHASM_DOOR_X, HubLayout.SURFACE_Y + dy, HubLayout.CHASM_DOOR_Z) != BlockKind.AIR) return fail("the Chasm's rim door is blocked at height +" + dy);
        }
        long t0 = System.nanoTime();
        Walkability.Result a = Walkability.reach(m::fillColumn, WalkRoutes.spireBox(), WalkRoutes.SPIRE_START[0], WalkRoutes.SPIRE_START[1], WalkRoutes.SPIRE_START[2],
                WalkRoutes.HOLY_TARGET[0], WalkRoutes.HOLY_TARGET[1], WalkRoutes.HOLY_TARGET[2]);
        if (!a.reached()) return fail("a walking search from the Spire's door does not reach the Holy gate (visited " + a.visited() + " states)");
        Walkability.Result b = Walkability.reach(m::fillColumn, WalkRoutes.chasmBox(), WalkRoutes.CHASM_START[0], WalkRoutes.CHASM_START[1], WalkRoutes.CHASM_START[2],
                WalkRoutes.ABYSS_TARGET[0], WalkRoutes.ABYSS_TARGET[1], WalkRoutes.ABYSS_TARGET[2]);
        if (!b.reached()) return fail("a walking search from the Chasm's rim does not reach the Abyssal gate (visited " + b.visited() + " states)");
        return SelfTests.Result.ok("gates in place, doors open, routes on foot: Holy " + a.steps() + " steps, Abyssal " + b.steps() + " steps (" + (System.nanoTime() - t0) / 1_000_000 + " ms)");
    }

    // ------------------------------------------------------------------------------------------------ the Wave Function Collapse
    private static SelfTests.Result wfc(MinecraftServer server) {
        ServerLevel level = level(server);
        if (level == null) return skipped();
        MacroParams params = MacroConfig.params();
        if (!params.landmarks()) return SelfTests.Result.ok("landmarks are off in the config: nothing to solve");
        long seed = SanctuaryGenerator.hubSeed(level.getChunkSource().randomState());
        long t0 = System.nanoTime();
        HubModel fresh = HubModel.fresh(seed, params);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        long[] s = fresh.wfcStats();
        if (s[1] != 0) return fail(s[1] + " of the problems fell back to the plain tile after " + s[0] + " restarts");
        if (ms > 3000) return fail("solving both landmarks took " + ms + " ms, over the 3 s budget");
        return SelfTests.Result.ok("Spire " + fresh.spire().galleryCount() + " galleries and the Chasm's " + fresh.chasm().sectors() + " x " + fresh.chasm().levels() + " x " + ChasmModel.RINGS
                + " grid solved: " + s[0] + " restarts, " + s[2] + " collapses, " + s[3] + " backtracks, no fallback, " + ms + " ms");
    }

    // ------------------------------------------------------------------------------------------------ the real generator
    /** The chunk columns (centre chunk and the ring of eight round it) of the three places the tests look at. */
    private static List<int[]> squares() {
        List<int[]> chunks = new ArrayList<>();
        int[][] centres = {{HubLayout.PLAZA_X, HubLayout.PLAZA_Z + 28}, {HubLayout.SPIRE_DOOR_X, HubLayout.SPIRE_DOOR_Z}, {HubLayout.CHASM_X, HubLayout.CHASM_Z}};
        for (int[] c : centres)
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) chunks.add(new int[] {(c[0] >> 4) + dx, (c[1] >> 4) + dz});
        return chunks;
    }

    private static SelfTests.Result generate(MinecraftServer server) {
        ServerLevel level = level(server);
        if (level == null) return skipped();
        List<int[]> chunks = squares();
        try {
            for (int[] c : chunks) level.setChunkForced(c[0], c[1], true);
            for (int[] c : chunks) level.getChunk(c[0], c[1]);
            // the plaza's disc is flat grass at SURFACE_Y - 1 (a tree or the plaza template may stand on part of it, so look at a point outside the template)
            int px = HubLayout.PLAZA_X + 5, pz = HubLayout.PLAZA_Z + 28;
            BlockState g = level.getBlockState(new BlockPos(px, HubLayout.SURFACE_Y - 1, pz));
            if (!g.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) && !g.is(net.minecraft.world.level.block.Blocks.DIRT)) return fail("the plaza disc at " + at(px, HubLayout.SURFACE_Y - 1, pz) + " is " + g.getBlock().getDescriptionId() + ", not grass");
            if (!MacroConfig.get(MacroConfig.ENABLED)) return SelfTests.Result.ok("landmarks are off: the plaza disc is grass at SURFACE_Y - 1, nothing more to check");
            // the Chasm: its centre column (beside the gate plane) is open air from the floor up to the surface
            int cx = HubLayout.CHASM_X, cz = HubLayout.CHASM_Z + 3;
            for (int y = ChasmModel.FLOOR_STAND; y < HubLayout.SURFACE_Y; y++) {
                BlockState s = level.getBlockState(new BlockPos(cx, y, cz));
                if (!s.isAir()) return fail("the Chasm's centre column is not open at " + at(cx, y, cz) + ": " + s.getBlock().getDescriptionId());
            }
            // the Spire's door is open
            for (int dy = 0; dy < 3; dy++) {
                BlockPos p = new BlockPos(HubLayout.SPIRE_DOOR_X, HubLayout.SURFACE_Y + dy, HubLayout.SPIRE_DOOR_Z);
                if (!level.getBlockState(p).isAir()) return fail("the Spire's doorway is blocked at " + at(p.getX(), p.getY(), p.getZ()) + ": " + level.getBlockState(p).getBlock().getDescriptionId());
            }
            // and the gates exist as blocks of the world
            BlockState holy = level.getBlockState(new BlockPos(HubLayout.HOLY_GATE_X, HubLayout.HOLY_GATE_Y, HubLayout.HOLY_GATE_Z));
            BlockState abyss = level.getBlockState(new BlockPos(HubLayout.ABYSS_GATE_X, HubLayout.ABYSS_GATE_Y, HubLayout.ABYSS_GATE_Z));
            if (holy.isAir() || abyss.isAir()) return fail("a polarity gate is missing in the generated world (Holy: " + holy.getBlock().getDescriptionId() + ", Abyssal: " + abyss.getBlock().getDescriptionId() + ")");
            return SelfTests.Result.ok("plaza grass, Chasm centre open from " + ChasmModel.FLOOR_STAND + " to the rim, Spire doorway open, both gates placed (" + chunks.size() + " chunks generated)");
        } finally {
            for (int[] c : chunks) level.setChunkForced(c[0], c[1], false);
        }
    }

    private static SelfTests.Result structures(MinecraftServer server) {
        ServerLevel level = level(server);
        if (level == null) return skipped();
        List<int[]> chunks = squares();
        try {
            for (int[] c : chunks) level.setChunkForced(c[0], c[1], true);
            int withStarts = 0;
            for (int[] c : chunks) {
                ChunkAccess chunk = level.getChunk(c[0], c[1]);
                if (!chunk.getAllStarts().isEmpty()) withStarts++;
            }
            if (withStarts > 0) return fail(withStarts + " of " + chunks.size() + " chunks of the Hub have a structure start");
            return SelfTests.Result.ok("no structure start in " + chunks.size() + " chunks (plaza, Spire, Chasm)");
        } finally {
            for (int[] c : chunks) level.setChunkForced(c[0], c[1], false);
        }
    }
}
