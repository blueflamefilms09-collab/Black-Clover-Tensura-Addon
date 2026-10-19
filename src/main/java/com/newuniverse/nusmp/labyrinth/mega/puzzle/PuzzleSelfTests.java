package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Why: nothing here can be played by its author, so every puzzle has a SELF-TEST that the CI server smoke test runs in the real game ({@code /multiverse mega selftest}, keys
 * {@code puzzle.<id>}). Each one lays a flat scratch room in a far corner of the Mega-Dungeon's dimension, arms the puzzle there through the SAME entry point the admin command and the Director
 * use, checks that the blocks the world shows are the blocks the rule's faces name, then plays it to the end with a stand-in player through the SAME paths a real player uses (a right click on
 * the cell's block with the right token in hand, a step onto a floor tile read from the player's position, a chosen answer, time passing) following the rule's own hint, and asserts: it was
 * solved, the reward was paid, nothing threw, and after ending the room is block for block what it was. {@code puzzle.catalog} runs the run-time check of all rules, texts and weights,
 * {@code puzzle.plan} checks the planner against the loaded weights file, {@code puzzle.restore} proves that a crash in the middle of a puzzle is repaired at the next start. The scratch room
 * is cleaned up whatever happens.
 */
public final class PuzzleSelfTests {
    private PuzzleSelfTests() {}

    private static final BlockPos ORIGIN = new BlockPos(6_300_000, MegaGrid.FLOOR_Y, 6_300_000);
    private static final int HALF = 22;
    private static final int TOP = 16;

    public static void register() {
        SelfTests.register("puzzle.catalog", PuzzleSelfTests::catalog);
        SelfTests.register("puzzle.plan", PuzzleSelfTests::plan);
        SelfTests.register("puzzle.restore", PuzzleSelfTests::restore);
        for (String id : PuzzleCatalog.ids()) SelfTests.register("puzzle." + id, server -> one(id, server));
    }

    // ---------------------------------------------------------------- the scratch room
    private static void build(ServerLevel level) {
        for (int cx = (ORIGIN.getX() - HALF - 2) >> 4; cx <= (ORIGIN.getX() + HALF + 2) >> 4; cx++) for (int cz = (ORIGIN.getZ() - HALF - 2) >> 4; cz <= (ORIGIN.getZ() + HALF + 2) >> 4; cz++) level.getChunk(cx, cz);
        DungeonGuard.internal(() -> {
            BlockState air = Blocks.AIR.defaultBlockState(), stone = Blocks.STONE_BRICKS.defaultBlockState();
            for (int x = ORIGIN.getX() - HALF; x <= ORIGIN.getX() + HALF; x++) for (int z = ORIGIN.getZ() - HALF; z <= ORIGIN.getZ() + HALF; z++) {
                for (int y = MegaGrid.FLOOR_Y - 4; y <= MegaGrid.FLOOR_Y + TOP; y++) {
                    BlockState want = y <= MegaGrid.FLOOR_Y ? stone : air;
                    BlockPos p = new BlockPos(x, y, z);
                    if (level.getBlockState(p) != want) level.setBlock(p, want, 2);
                }
            }
        });
        wipeEntities(level);
    }

    private static void wipeEntities(ServerLevel level) {
        AABB box = new AABB(ORIGIN.getX() - HALF - 4, MegaGrid.FLOOR_Y - 6, ORIGIN.getZ() - HALF - 4, ORIGIN.getX() + HALF + 4, MegaGrid.FLOOR_Y + TOP + 4, ORIGIN.getZ() + HALF + 4);
        for (Entity e : level.getEntitiesOfClass(ItemEntity.class, box)) e.discard();
        for (Entity e : level.getEntitiesOfClass(ExperienceOrb.class, box)) e.discard();
        for (Mob m : level.getEntitiesOfClass(Mob.class, box, x -> x.getTags().contains(PuzzleMobs.TAG))) m.discard();
    }

    private static int[] snapshot(ServerLevel level) {
        List<Integer> out = new ArrayList<>();
        for (int x = ORIGIN.getX() - HALF; x <= ORIGIN.getX() + HALF; x++) for (int z = ORIGIN.getZ() - HALF; z <= ORIGIN.getZ() + HALF; z++) {
            for (int y = MegaGrid.FLOOR_Y - 4; y <= MegaGrid.FLOOR_Y + TOP; y++) out.add(net.minecraft.world.level.block.Block.getId(level.getBlockState(new BlockPos(x, y, z))));
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    private static ServerPlayer stand(ServerLevel level, String name) {
        return SelfTests.standIn(level, name, ORIGIN.getX() - HALF + 1.5, MegaGrid.FLOOR_Y + 1, ORIGIN.getZ() - HALF + 1.5);
    }

    // ---------------------------------------------------------------- one puzzle, end to end
    private static SelfTests.Result one(String id, MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return SelfTests.Result.fail("the Mega-Dungeon dimension is not loaded");
        build(level);
        ServerPlayer p = stand(level, "puzzle_" + id);
        int[] before = snapshot(level);
        long t = level.getGameTime() + 5000;
        String[] why = {""};
        PuzzleInstance in = PuzzleDirector.applyAt(level, id, ORIGIN.above(), 24680L + id.hashCode(), List.of(p), t, why);
        if (in == null) return SelfTests.Result.fail("could not be placed in an empty hall: " + why[0]);
        String detail;
        try {
            for (PuzzleStage.Cell c : in.stage.cells) {
                int layers = c.tile() ? 1 : c.height();
                for (int l = 0; l < layers; l++) {
                    BlockState want = PuzzlePalette.state(c.palette(), in.rule.face(c.index(), l), in.flavour);
                    if (in.shown(c.index(), l) != want) return SelfTests.Result.fail("cell " + c.index() + " layer " + l + " shows " + in.shown(c.index(), l) + " but the rule says " + want);
                }
            }
            int steps = 0;
            while (!in.rule.solved() && in.state != PuzzleInstance.State.ENDED && steps < 1500) {
                Act s = in.rule.suggest(t);
                steps++;
                if (s == null) {
                    t += 10;
                    in.update(List.of(p), t);
                    continue;
                }
                while (t < s.tick()) {
                    t = Math.min(s.tick(), t + 5);
                    in.update(List.of(p), t);
                }
                Act a = s.at(t);
                in.perform(p, a, t);
                if (a.stepsOn()) {
                    long until = t + a.hold();
                    while (t < until) {
                        t = Math.min(until, t + 5);
                        in.update(List.of(p), t);
                    }
                    in.perform(p, Act.stepOff(a.actor(), a.cell(), t), t);
                }
            }
            if (!in.rule.solved()) return SelfTests.Result.fail("not solved by its own hint after " + steps + " steps: " + in.describe());
            if (!in.rewardPaid || in.state != PuzzleInstance.State.SOLVED) return SelfTests.Result.fail("solved but the reward was not paid (" + in.state + ")");
            if (in.errors != 0) return SelfTests.Result.fail(in.errors + " error(s) while it ran");
            detail = id + " solved in " + steps + " steps, " + in.rule.layout().total() + " cells, fails " + in.rule.fails();
        } finally {
            PuzzleDirector.end(in);
            wipeEntities(level);
        }
        int[] after = snapshot(level);
        for (int i = 0; i < before.length; i++) if (before[i] != after[i]) return SelfTests.Result.fail("the room was not restored (block number " + i + ")");
        return SelfTests.Result.ok(detail + ", room restored");
    }

    // ---------------------------------------------------------------- the others
    private static SelfTests.Result catalog(MinecraftServer server) {
        List<String> bad = PuzzleVerify.run();
        if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problem(s): " + bad.get(0));
        PuzzleWeights w = PuzzleWeights.get();
        if (w.types.size() < PuzzleCatalog.ids().size()) return SelfTests.Result.fail("the weights file names only " + w.types.size() + " puzzles");
        if (PuzzleCatalog.ids().size() < 24) return SelfTests.Result.fail("only " + PuzzleCatalog.ids().size() + " puzzles");
        return SelfTests.Result.ok(PuzzleCatalog.ids().size() + " puzzles, all solvable by their hint, palettes and texts complete, weights loaded (max " + w.maxPerFloor + " a floor)");
    }

    private static SelfTests.Result plan(MinecraftServer server) {
        PuzzleWeights w = PuzzleWeights.get();
        List<PuzzlePlanner.RoomInfo> rooms = new ArrayList<>();
        for (int i = 0; i < 14; i++) rooms.add(new PuzzlePlanner.RoomInfo(i, i % 3 == 0 ? "puzzle" : "hall", i % 3 == 0 ? "nusmp:mega/puzzle/gallery_a" : "nusmp:mega/hall/crypt_a", 20, true, false, false, ""));
        rooms.add(new PuzzlePlanner.RoomInfo(20, "puzzle", "nusmp:mega/puzzle/sigil_hall_a", 20, true, false, false, ""));
        rooms.add(new PuzzlePlanner.RoomInfo(21, "hall", "nusmp:mega/hall/crypt_b", 20, true, true, false, ""));
        for (int floor : new int[] {1, 250, 500, 750, 999}) {
            PuzzlePlanner.FloorInfo f = new PuzzlePlanner.FloorInfo(floor, floor % 2 == 0, "necropolis", 77L + floor, false, rooms);
            List<PuzzlePlanner.Slot> a = PuzzlePlanner.plan(f, w), b = PuzzlePlanner.plan(f, w);
            if (!a.equals(b)) return SelfTests.Result.fail("the plan of floor " + floor + " is not repeatable");
            if (a.size() > w.maxPerFloor) return SelfTests.Result.fail("floor " + floor + " plans " + a.size() + " puzzles, the cap is " + w.maxPerFloor);
            for (PuzzlePlanner.Slot s : a) {
                if (s.roomId() == 20 || s.roomId() == 21) return SelfTests.Result.fail("a protected room was planned");
                if (!PuzzleCatalog.has(s.type())) return SelfTests.Result.fail("unknown puzzle " + s.type());
            }
        }
        SplittableRandom r = new SplittableRandom(5);
        int planned = 0;
        for (int i = 0; i < 400; i++) planned += PuzzlePlanner.plan(new PuzzlePlanner.FloorInfo(1 + r.nextInt(1000), r.nextBoolean(), "frost", r.nextLong(), false, rooms), w).size();
        if (planned == 0) return SelfTests.Result.fail("400 floors planned no puzzle at all");
        return SelfTests.Result.ok("plans are repeatable and capped, protected rooms are left alone, " + planned + " puzzles over 400 sample floors");
    }

    private static SelfTests.Result restore(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return SelfTests.Result.fail("the Mega-Dungeon dimension is not loaded");
        build(level);
        ServerPlayer p = stand(level, "puzzle_restore");
        int[] before = snapshot(level);
        long t = level.getGameTime() + 5000;
        PuzzleInstance in = PuzzleDirector.applyAt(level, "lights_out", ORIGIN.above(), 4242L, List.of(p), t, new String[] {""});
        if (in == null) return SelfTests.Result.fail("could not place the probe puzzle");
        try {
            if (in.journal().isEmpty()) return SelfTests.Result.fail("the puzzle changed no block");
            int[] during = snapshot(level);
            boolean differs = false;
            for (int i = 0; i < before.length; i++) differs |= before[i] != during[i];
            if (!differs) return SelfTests.Result.fail("the room did not change while the puzzle was armed");
            PuzzleData live = PuzzleData.get(server);
            CompoundTag saved = live.save(new CompoundTag(), server.registryAccess());
            PuzzleData reloaded = PuzzleData.load(saved, server.registryAccess());
            if (reloaded.leftoverCount() == 0) return SelfTests.Result.fail("the saved data holds no journal");
            int n = reloaded.recover(level);                                          // what the next start does after a crash
            int[] healed = snapshot(level);
            for (int i = 0; i < before.length; i++) if (before[i] != healed[i]) return SelfTests.Result.fail("the crash recovery did not restore block number " + i);
            return SelfTests.Result.ok("a crash in the middle of a puzzle is repaired at the next start (" + n + " journal)");
        } finally {
            PuzzleDirector.end(in);
            wipeEntities(level);
        }
    }
}
