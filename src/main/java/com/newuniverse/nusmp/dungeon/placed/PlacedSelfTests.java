package com.newuniverse.nusmp.dungeon.placed;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Why: nothing about the player-placed exemption can be played by its author, so each rule has a SELF-TEST the CI server smoke test runs in the real game
 * ({@code /multiverse mega selftest dungeon.guard_exempt.<name>}). They use a far-away cell of the Mega-Dungeon's own dimension, under the REAL guard, and a
 * fake player. {@code placed}: a generated block is held, a player's placement is remembered, a mob's is not, the guard lets exactly the marked block go, a
 * builder's write drops the mark, and a box clear drops the marks in it. {@code pickup}: a real player-mode break (the very path a left click takes) takes a
 * placed torch and a placed crafting table with their drops, and still refuses a generated crafting table next to them. {@code persist}: the saved data
 * round-trips. {@code sync}: the client packet encodes, decodes and fills the mirror. {@code cap}: the per-chunk cap drops the OLDEST marks. Every block
 * and entity a test makes is removed at the end whatever happens.
 */
public final class PlacedSelfTests {
    private PlacedSelfTests() {}

    /** A cell far from everything the dungeon builds (the pockets use the field the rift package documents; this one is on the other side of the world). */
    private static final BlockPos PROBE = new BlockPos(1_234_560, 100, -1_234_544);

    public static void register() {
        SelfTests.register("dungeon.guard_exempt.placed", PlacedSelfTests::placed);
        SelfTests.register("dungeon.guard_exempt.pickup", PlacedSelfTests::pickup);
        SelfTests.register("dungeon.guard_exempt.persist", PlacedSelfTests::persist);
        SelfTests.register("dungeon.guard_exempt.sync", PlacedSelfTests::sync);
        SelfTests.register("dungeon.guard_exempt.cap", PlacedSelfTests::cap);
    }

    private static SelfTests.Result fail(String s) { return SelfTests.Result.fail(s); }

    private static void put(ServerLevel level, BlockPos pos, BlockState s) { DungeonGuard.internal(() -> level.setBlock(pos, s, 2)); }

    private static void tidy(ServerLevel level, BlockPos... cells) {
        for (BlockPos c : cells) {
            PlayerPlaced.unmark(level, c);
            put(level, c, Blocks.AIR.defaultBlockState());
        }
        for (ItemEntity i : level.getEntitiesOfClass(ItemEntity.class, new AABB(PROBE).inflate(12.0))) i.discard();
    }

    private static ServerLevel prepared(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega != null) mega.getChunk(PROBE.getX() >> 4, PROBE.getZ() >> 4);
        return mega;
    }

    // ================================================================ placed
    private static SelfTests.Result placed(MinecraftServer server) {
        ServerLevel mega = prepared(server);
        if (mega == null) return fail("the Mega-Dungeon dimension is not loaded");
        if (!PlayerPlaced.active(mega)) return SelfTests.Result.ok("the exemption is switched off in this config (placed.exempt = false); nothing to check");
        BlockPos a = PROBE, b = PROBE.east(), c = PROBE.east(2), far = PROBE.offset(64, 0, 0);
        Entity zombie = EntityType.ZOMBIE.create(mega);
        FakePlayer player = SelfTests.player(mega, "guard_exempt_tester", PROBE.getX() + 0.5, PROBE.getY() + 2, PROBE.getZ() + 0.5);
        try {
            put(mega, a, Blocks.STONE.defaultBlockState());
            put(mega, b, Blocks.STONE.defaultBlockState());
            put(mega, c, Blocks.STONE.defaultBlockState());
            if (PlayerPlaced.isPlaced(mega, a)) return fail("a generated block reads as player-placed");
            boolean guardOn = DungeonGuard.on(mega);
            if (guardOn && !DungeonGuard.refusesSet(mega, a, Blocks.AIR.defaultBlockState())) return fail("the guard is on but does not refuse a generated block turned to air: the premise does not hold in this build");
            // a mob's placement is not remembered, a player's is
            if (PlacedEvents.markPlacement(mega, zombie, List.of(a)) != 0 || PlayerPlaced.isPlaced(mega, a)) return fail("a mob's placement was remembered");
            if (PlacedEvents.markPlacement(mega, player, List.of(a, b)) != 2 || !PlayerPlaced.isPlaced(mega, a) || !PlayerPlaced.isPlaced(mega, b)) return fail("a player's placement (two blocks, like a bed) was not remembered");
            if (PlayerPlaced.isPlaced(mega, c)) return fail("an unmarked neighbour reads as player-placed");
            // the guard: the marked block may go through a REAL write (and its mark goes with it), the generated neighbour may not
            if (DungeonGuard.refusesSet(mega, a, Blocks.AIR.defaultBlockState())) return fail("the guard refuses a player-placed block turned to air");
            if (!mega.setBlock(a, Blocks.AIR.defaultBlockState(), 3) || !mega.getBlockState(a).isAir()) return fail("a real write could not take a player-placed block away");
            if (PlayerPlaced.isPlaced(mega, a)) return fail("the mark of a block that was taken away is still there");
            if (guardOn && (mega.setBlock(c, Blocks.AIR.defaultBlockState(), 3) || !mega.getBlockState(c).is(Blocks.STONE))) return fail("the guard let a GENERATED block go: the exemption is too wide");
            if (DungeonGuard.refusesRemoval(mega, b)) return fail("the guard refuses to remove a player-placed block");
            if (!mega.removeBlock(b, false) || !mega.getBlockState(b).isAir()) return fail("removeBlock could not take a player-placed block away");
            if (PlayerPlaced.isPlaced(mega, b)) return fail("the mark of a removed block is still there");
            if (guardOn && (mega.removeBlock(c, false) || !mega.getBlockState(c).is(Blocks.STONE))) return fail("removeBlock took a generated block");
            put(mega, a, Blocks.STONE.defaultBlockState());
            put(mega, b, Blocks.STONE.defaultBlockState());
            // a builder's write drops the mark
            PlayerPlaced.mark(mega, a);
            put(mega, a, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            if (PlayerPlaced.isPlaced(mega, a)) return fail("the dungeon's own builder wrote over a marked block and the mark stayed");
            if (guardOn && !DungeonGuard.refusesSet(mega, a, Blocks.AIR.defaultBlockState())) return fail("a block the builder rewrote is still breakable");
            // a box clear drops the marks inside it
            PlayerPlaced.mark(mega, a);
            PlayerPlaced.mark(mega, b);
            PlayerPlaced.mark(mega, far);
            int cleared = PlayerPlaced.clearBox(mega, a.offset(-1, -1, -1), c.offset(1, 1, 1));
            if (cleared != 2 || PlayerPlaced.isPlaced(mega, a) || PlayerPlaced.isPlaced(mega, b) || !PlayerPlaced.isPlaced(mega, far)) return fail("clearBox dropped " + cleared + " marks (2 expected) or the wrong ones");
            int chunk = PlayerPlaced.clearChunk(mega, far.getX() >> 4, far.getZ() >> 4);
            if (chunk != 1 || PlayerPlaced.isPlaced(mega, far)) return fail("clearChunk dropped " + chunk + " marks (1 expected)");
            return SelfTests.Result.ok("guard " + (guardOn ? "ON" : "off in this world") + ": generated blocks held, a player's two-block placement remembered, a mob's not, a marked block let go once, a builder's write and the box / chunk clears drop marks");
        } finally {
            if (zombie != null) zombie.discard();
            tidy(mega, a, b, c, far);
        }
    }

    // ================================================================ pickup
    private static SelfTests.Result pickup(MinecraftServer server) {
        ServerLevel mega = prepared(server);
        if (mega == null) return fail("the Mega-Dungeon dimension is not loaded");
        if (!PlayerPlaced.active(mega)) return SelfTests.Result.ok("the exemption is switched off in this config; nothing to check");
        BlockPos torch = PROBE.above(8), table = PROBE.above(8).east(2), wall = PROBE.above(8).east(4), floor = PROBE.above(7);
        FakePlayer player = SelfTests.player(mega, "guard_exempt_taker", PROBE.getX() + 0.5, PROBE.getY() + 12, PROBE.getZ() + 0.5);
        try {
            put(mega, floor.below(), Blocks.STONE.defaultBlockState());
            put(mega, floor, Blocks.STONE.defaultBlockState());
            put(mega, torch, Blocks.TORCH.defaultBlockState());
            put(mega, table, Blocks.CRAFTING_TABLE.defaultBlockState());
            put(mega, wall, Blocks.CRAFTING_TABLE.defaultBlockState());
            PlacedEvents.markPlacement(mega, player, List.of(torch, table));
            boolean guardOn = DungeonGuard.on(mega);
            if (guardOn && player.gameMode.destroyBlock(wall)) return fail("a GENERATED crafting table was broken by a player: the exemption is too wide");
            if (guardOn && !mega.getBlockState(wall).is(Blocks.CRAFTING_TABLE)) return fail("a generated crafting table is gone after a refused break");
            if (!player.gameMode.destroyBlock(torch) || !mega.getBlockState(torch).isAir()) return fail("a player could not pick up their placed torch");
            if (!player.gameMode.destroyBlock(table) || !mega.getBlockState(table).isAir()) return fail("a player could not pick up their placed crafting table");
            if (PlayerPlaced.isPlaced(mega, torch) || PlayerPlaced.isPlaced(mega, table)) return fail("the marks of the two picked-up blocks are still there");
            int drops = 0;
            for (ItemEntity i : mega.getEntitiesOfClass(ItemEntity.class, new AABB(PROBE).inflate(14.0))) {
                Block b = net.minecraft.world.level.block.Block.byItem(i.getItem().getItem());
                if (b == Blocks.TORCH || b == Blocks.CRAFTING_TABLE) drops++;
            }
            if (drops < 2) return fail("picking the two blocks up dropped " + drops + " matching items (2 expected)");
            return SelfTests.Result.ok("guard " + (guardOn ? "ON" : "off in this world") + ": a real player-mode break took a placed torch and a placed crafting table with their drops and refused the generated crafting table beside them");
        } finally {
            tidy(mega, torch, table, wall, floor, floor.below());
        }
    }

    // ================================================================ persist
    private static SelfTests.Result persist(MinecraftServer server) {
        ServerLevel mega = prepared(server);
        if (mega == null) return fail("the Mega-Dungeon dimension is not loaded");
        PlacedSet set = new PlacedSet();
        long[] keys = {PlacedSet.pack(5, 70, -9), PlacedSet.pack(-30_000_000, -64, 29_999_999), PlacedSet.pack(1_234_560, 319, -1_234_544)};
        for (long k : keys) set.addKey(k);
        CompoundTag tag = new PlacedBlocksData(set).save(new CompoundTag(), mega.registryAccess());
        PlacedBlocksData back = PlacedBlocksData.load(tag, mega.registryAccess());
        if (back.set().size() != 3) return fail("the saved data came back with " + back.set().size() + " marks (3 expected)");
        for (long k : keys) if (!back.set().containsKey(k)) return fail("a mark was lost in the save and load");
        if (PlacedSet.unpackX(keys[1]) != -30_000_000 || PlacedSet.unpackY(keys[1]) != -64 || PlacedSet.unpackZ(keys[1]) != 29_999_999) return fail("the packing does not round-trip a far negative position");
        BlockPos p = new BlockPos(-30_000_000, -64, 29_999_999);
        if (PlacedSet.pack(p.getX(), p.getY(), p.getZ()) != p.asLong()) return fail("the packing no longer matches BlockPos.asLong, which the guard hooks rely on");
        try {
            if (!PlayerPlaced.mark(mega, PROBE)) return fail("mark() refused a fresh position");
            if (!PlacedBlocksData.get(mega).isDirty()) return fail("the world's saved data was not marked dirty by a placement, it would not be written");
            return SelfTests.Result.ok("3 marks (one at the world's far corner) round-trip through the save, the packing matches BlockPos, and a placement marks the level's data dirty");
        } finally {
            PlayerPlaced.unmark(mega, PROBE);
        }
    }

    // ================================================================ sync
    private static SelfTests.Result sync(MinecraftServer server) {
        String dim = "nusmp:selftest";
        long k1 = PlacedSet.pack(100, 64, 200), k2 = PlacedSet.pack(101, 64, 200);
        try {
            PlacedMirror.clear();
            PlacedNet.PlacedPayload sent = new PlacedNet.PlacedPayload(dim, PlacedNet.REPLACE, 6, 12, new long[]{k1});
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            PlacedNet.PlacedPayload.CODEC.encode(buf, sent);
            PlacedNet.PlacedPayload got = PlacedNet.PlacedPayload.CODEC.decode(buf);
            if (!got.dim().equals(dim) || got.op() != PlacedNet.REPLACE || got.cx() != 6 || got.cz() != 12 || got.keys().length != 1 || got.keys()[0] != k1) return fail("the packet did not survive encode and decode");
            PlacedNet.apply(got);
            if (!PlacedMirror.contains(dim, 100, 64, 200)) return fail("a chunk packet did not fill the client mirror");
            PlacedNet.apply(new PlacedNet.PlacedPayload(dim, PlacedNet.ADD, 6, 12, new long[]{k2}));
            if (!PlacedMirror.contains(dim, 101, 64, 200)) return fail("an add packet did not reach the mirror");
            PlacedNet.apply(new PlacedNet.PlacedPayload(dim, PlacedNet.REMOVE, 6, 12, new long[]{k1}));
            if (PlacedMirror.contains(dim, 100, 64, 200) || !PlacedMirror.contains(dim, 101, 64, 200)) return fail("a remove packet removed the wrong mark");
            PlacedNet.apply(new PlacedNet.PlacedPayload(dim, PlacedNet.REPLACE, 6, 12, new long[0]));
            if (PlacedMirror.size(dim) != 0) return fail("an empty chunk packet did not clear the chunk");
            return SelfTests.Result.ok("the packet encodes and decodes, and the replace / add / remove / forget operations fill and empty the client mirror");
        } finally {
            PlacedMirror.clear();
        }
    }

    // ================================================================ cap
    private static SelfTests.Result cap(MinecraftServer server) {
        ServerLevel mega = prepared(server);
        if (mega == null) return fail("the Mega-Dungeon dimension is not loaded");
        if (!PlayerPlaced.active(mega)) return SelfTests.Result.ok("the exemption is switched off in this config; nothing to check");
        int per = PlacedConfig.get(PlacedConfig.PER_CHUNK);
        if (per > 5000) return SelfTests.Result.ok("the per-chunk cap is " + per + ", too high to exercise in a test; the pure cap logic is covered by the unit tests");
        int cx = PROBE.getX() >> 4, cz = PROBE.getZ() >> 4;
        BlockPos base = new BlockPos(cx << 4, 150, cz << 4);
        int total = per + 3;
        try {
            PlayerPlaced.clearChunk(mega, cx, cz);
            BlockPos first = null, last = null;
            for (int i = 0; i < total; i++) {
                BlockPos p = base.offset(i % 16, (i / 256) + 1, (i / 16) % 16);
                if (i == 0) first = p;
                last = p;
                PlayerPlaced.mark(mega, p);
            }
            PlacedBlocksData d = PlacedBlocksData.peek(mega);
            int now = d == null ? 0 : d.set().chunkSize(cx, cz);
            if (now != per) return fail("the chunk holds " + now + " marks after " + total + " placements (the cap " + per + " expected)");
            if (PlayerPlaced.isPlaced(mega, first)) return fail("the OLDEST mark survived the cap");
            if (!PlayerPlaced.isPlaced(mega, last)) return fail("the NEWEST mark was dropped by the cap");
            return SelfTests.Result.ok(total + " placements in one chunk kept exactly " + per + " marks, dropping the oldest and keeping the newest");
        } finally {
            PlayerPlaced.clearChunk(mega, cx, cz);
        }
    }
}
