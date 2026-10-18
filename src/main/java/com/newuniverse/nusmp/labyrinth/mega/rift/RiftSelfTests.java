package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing here can be played by its author, so each rule of the rifts has a SELF-TEST that the CI server smoke test runs in the real game ({@code /multiverse mega selftest}), and each
 * fails loudly with the reason. {@code rift.block}: every kind of rift opens as a block with a block entity of the right kind, and collapsing it removes the block. {@code rift.waypoint}: a
 * waypoint survives its compound round trip, a waypoint of a floor that does not exist is GONE and not an error, and the level of a floor is its decade. {@code rift.pocket.boss}: a
 * secret-boss pocket builds with its barred vault, three loot chests, an exit rift and a boss of the floor, the bars lift when the boss dies, and collapsing it clears everything and frees the
 * slot. {@code rift.pocket.puzzle}: a puzzle pocket builds with its four plates, a wrong plate resets the order, the right order lifts the bars. They use the last cell of the pocket field,
 * which nothing else ever uses, and clean it up whatever happens.
 */
public final class RiftSelfTests {
    private RiftSelfTests() {}

    private static final int TEST_SLOT = RiftRules.MAX_SLOTS - 1;

    public static void register() {
        SelfTests.register("rift.block", RiftSelfTests::block);
        SelfTests.register("rift.waypoint", RiftSelfTests::waypoint);
        SelfTests.register("rift.pocket.boss", s -> pocket(s, RiftRules.Variant.BOSS));
        SelfTests.register("rift.pocket.puzzle", s -> pocket(s, RiftRules.Variant.PUZZLE));
    }

    private static SelfTests.Result block(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return SelfTests.Result.fail("the Mega-Dungeon dimension is not loaded");
        int cx = RiftRules.pocketX(TEST_SLOT), cz = RiftRules.POCKET_Z;
        List<String> done = new ArrayList<>();
        try {
            for (RiftType t : RiftType.values()) {
                BlockPos at = new BlockPos(cx + 40, RiftRules.POCKET_Y + 40, cz + t.ordinal() * 4);
                mega.getChunk(at.getX() >> 4, at.getZ() >> 4);
                mega.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
                DimensionalRiftBlockEntity be = RiftService.open(mega, at, t, 12345L + t.ordinal());
                if (be == null) return SelfTests.Result.fail("a " + t.id() + " rift did not open at " + at.toShortString());
                if (be.type() != t) return SelfTests.Result.fail("the " + t.id() + " rift reports the type " + be.type().id());
                if (be.left() <= 0) return SelfTests.Result.fail("the " + t.id() + " rift opened with no life");
                if (!RiftService.openRifts().contains(at)) return SelfTests.Result.fail("the open list does not hold the " + t.id() + " rift");
                CompoundTag tag = be.saveWithoutMetadata(mega.registryAccess());
                if (!t.id().equals(tag.getString("Type"))) return SelfTests.Result.fail("the " + t.id() + " rift saved as " + tag.getString("Type"));
                RiftService.collapse(mega, at, be);
                if (mega.getBlockState(at).is(RiftRegistry.RIFT.get())) return SelfTests.Result.fail("the " + t.id() + " rift's block is still there after it collapsed");
                done.add(t.id());
            }
        } finally {
            for (RiftType t : RiftType.values()) mega.setBlock(new BlockPos(cx + 40, RiftRules.POCKET_Y + 40, cz + t.ordinal() * 4), Blocks.AIR.defaultBlockState(), 2);
        }
        return SelfTests.Result.ok("opened, saved and collapsed one rift of each kind: " + done);
    }

    private static SelfTests.Result waypoint(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return SelfTests.Result.fail("the Mega-Dungeon dimension is not loaded");
        Waypoint w = new Waypoint("nusmp:mega_dungeon", 7, 3, -25, 10.5, 64.0, -3.25, 90f, 12f, 777L);
        Waypoint r = Waypoint.load(w.save());
        if (!w.equals(r)) return SelfTests.Result.fail("the waypoint changed in a compound round trip: " + r);
        if (Waypoint.levelOf(1) != 1 || Waypoint.levelOf(10) != 1 || Waypoint.levelOf(11) != 2 || Waypoint.levelOf(-25) != 3 || Waypoint.levelOf(0) != 0)
            return SelfTests.Result.fail("levelOf is wrong: 1->" + Waypoint.levelOf(1) + " 10->" + Waypoint.levelOf(10) + " 11->" + Waypoint.levelOf(11) + " -25->" + Waypoint.levelOf(-25));
        if (Waypoint.load(new CompoundTag()) != null) return SelfTests.Result.fail("an empty compound loaded as a waypoint");
        FakePlayer p = SelfTests.player(mega, "rift_tester", 0, 70, 0);
        try {
            Waypoint nowhere = new Waypoint("nusmp:mega_dungeon", 999_999, 1, 1, 0, 70, 0, 0f, 0f, 1L);
            if (WaypointService.outcome(server, p, nowhere) != WaypointService.Outcome.GONE) return SelfTests.Result.fail("a waypoint on a floor that does not exist was not GONE");
            if (WaypointService.outcome(server, p, null) != WaypointService.Outcome.GONE) return SelfTests.Result.fail("no waypoint was not GONE");
        } finally {
            p.discard();
        }
        return SelfTests.Result.ok("the waypoint round-trips, levels follow decades, and a missing floor is GONE");
    }

    private static SelfTests.Result pocket(MinecraftServer server, RiftRules.Variant want) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return SelfTests.Result.fail("the Mega-Dungeon dimension is not loaded");
        PocketRealm realm = PocketRealm.get(server);
        if (realm.at(TEST_SLOT) != null) realm.collapseSlot(mega, TEST_SLOT);
        PocketRealm.Pocket p = new PocketRealm.Pocket();
        p.slot = TEST_SLOT;
        p.floor = 12;
        p.variant = want.id();
        p.seed = 424242L;
        p.createdAt = mega.getGameTime();
        p.expiresAt = p.createdAt + 20L * 600;
        FakePlayer fake = SelfTests.player(mega, "pocket_tester", RiftRules.pocketX(TEST_SLOT) - 19, RiftRules.POCKET_Y + 1, RiftRules.POCKET_Z);
        try {
            PocketBuilder.build(mega, p, RegionTheme.GRAVITY);
            realm.pockets().put(p.slot, p);
            if (p.bars.size() != 9) return SelfTests.Result.fail("the vault door has " + p.bars.size() + " bars, not 9");
            for (BlockPos b : p.bars) if (!mega.getBlockState(b).is(Blocks.IRON_BARS)) return SelfTests.Result.fail("no iron bars at " + b.toShortString());
            int chests = 0;
            for (int i = 0; i < 3; i++) {
                BlockEntity be = mega.getBlockEntity(new BlockPos(RiftRules.pocketX(TEST_SLOT) + 18, RiftRules.POCKET_Y, RiftRules.POCKET_Z - 2 + i * 2));
                if (be instanceof ChestBlockEntity) chests++;
            }
            if (chests != 3) return SelfTests.Result.fail("the vault has " + chests + " chests, not 3");
            if (!(mega.getBlockEntity(p.exit) instanceof DimensionalRiftBlockEntity ex) || ex.type() != RiftType.EXIT) return SelfTests.Result.fail("there is no exit rift at " + p.exit.toShortString());
            if (want == RiftRules.Variant.BOSS) {
                var bosses = mega.getEntitiesOfClass(LabyrinthGuardianEntity.class, PocketBuilder.bounds(TEST_SLOT), g -> g.getTags().contains(PocketBuilder.BOSS_TAG));
                if (bosses.size() != 1) return SelfTests.Result.fail("the arena holds " + bosses.size() + " secret bosses, not 1");
                // the boss dies: the same handler the game bus calls opens the vault
                PocketBuilder.openVault(mega, p);
                for (BlockPos b : p.bars) if (!mega.getBlockState(b).isAir()) return SelfTests.Result.fail("a bar stayed after the vault opened at " + b.toShortString());
            } else {
                if (p.plates.length != 4) return SelfTests.Result.fail("the puzzle has " + p.plates.length + " plates, not 4");
                for (BlockPos pl : p.plates) if (!mega.getBlockState(pl).is(Blocks.POLISHED_BLACKSTONE_BUTTON)) return SelfTests.Result.fail("no button at " + pl.toShortString());
                int[] order = RiftRules.puzzleOrder(p.seed);
                int wrong = order[1] == 0 ? 1 : 0;
                if (PocketEvents.press(mega, realm, p, fake, order[0], p.plates[order[0]]) != RiftRules.Press.PROGRESS) return SelfTests.Result.fail("the first right plate did not progress");
                if (PocketEvents.press(mega, realm, p, fake, wrong, p.plates[wrong]) != RiftRules.Press.WRONG) return SelfTests.Result.fail("a wrong plate was not WRONG");
                if (p.pressed != 0) return SelfTests.Result.fail("a wrong plate did not reset the order");
                RiftRules.Press last = RiftRules.Press.WRONG;
                for (int i = 0; i < 4; i++) last = PocketEvents.press(mega, realm, p, fake, order[i], p.plates[order[i]]);
                if (last != RiftRules.Press.SOLVED || !p.solved) return SelfTests.Result.fail("the right order did not solve the puzzle");
                for (BlockPos b : p.bars) if (!mega.getBlockState(b).isAir()) return SelfTests.Result.fail("a bar stayed after the puzzle was solved at " + b.toShortString());
            }
        } finally {
            fake.discard();
            realm.pockets().remove(p.slot);
            PocketBuilder.clear(mega, TEST_SLOT);
            for (var e : mega.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, PocketBuilder.bounds(TEST_SLOT))) e.discard();
            realm.setDirty();
        }
        if (!mega.getBlockState(p.bars.get(0)).isAir() || mega.getBlockState(p.exit).is(RiftRegistry.RIFT.get())) return SelfTests.Result.fail("the cell was not cleared after the pocket collapsed");
        return SelfTests.Result.ok("a " + want.id() + " pocket builds with a barred vault, three chests and an exit rift, opens as designed, and clears completely");
    }
}
