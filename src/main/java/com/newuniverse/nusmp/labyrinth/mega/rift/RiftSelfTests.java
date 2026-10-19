package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.gen.SectorGenerator;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

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
        SelfTests.register("rift.e2e", RiftSelfTests::e2e);                                       // 1.94.0: spawn, step in, arrive, clear, reward, exit, for both pocket kinds and the shortcut
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
                BlockPos at = new BlockPos(cx + 40, RiftRules.POCKET_Y + 20, cz + t.ordinal() * 4);
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
            for (RiftType t : RiftType.values()) mega.setBlock(new BlockPos(cx + 40, RiftRules.POCKET_Y + 20, cz + t.ordinal() * 4), Blocks.AIR.defaultBlockState(), 2);
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

    // ================================================================ 1.94.0: the whole journey, in the real game
    /**
     * {@code rift.e2e}: what a delver does, end to end, with a stand-in for the delver. For each pocket kind (puzzle and boss): a rift opens, its block entity ticks, a player outside the tear is left alone and
     * one inside it is taken (the very code the tick runs), the hidden realm is built with its barred vault, three loot chests and an exit rift, the player arrives in the antechamber with the way back
     * remembered, the realm is CLEARED (the plates pressed in order / the death of the secret boss posted on the game bus), the bars lift, the chests give loot (the reward), the exit rift is held back by
     * its short cooldown and then takes the player out beside the rift they came in by and NOT inside it, and the realm collapses and frees its slot. Then a SHORTCUT: a steady one carries the player to the
     * arrival pad of the floor it was aimed at and is used up, one aimed past the last built floor holds the player back, and after the patience it settles for the farthest built floor and carries them there. Last the SPAWNER: a
     * natural roll on a real floor opens a rift near the player, aimed forward, announced, and a second roll finds the neighbourhood crowded. Parts that need a finished dungeon say so in the detail and are
     * skipped when this world has none; every other part is an assertion. Everything it builds is cleaned up whatever happens.
     */
    private static SelfTests.Result e2e(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return SelfTests.Result.fail("the Mega-Dungeon dimension is not loaded");
        List<String> notes = new ArrayList<>();
        for (RiftRules.Variant v : RiftRules.Variant.values()) {
            SelfTests.Result r = e2ePocket(server, mega, v, notes);
            if (!r.ok()) return r;
        }
        SelfTests.Result r = e2eShortcut(server, mega, notes);
        if (!r.ok()) return r;
        r = e2eSpawn(server, mega, notes);
        if (!r.ok()) return r;
        return SelfTests.Result.ok(String.join("; ", notes));
    }

    private static SelfTests.Result e2ePocket(MinecraftServer server, ServerLevel mega, RiftRules.Variant want, List<String> notes) {
        String k = "[" + want.id() + " pocket] ";
        double share = RiftConfig.get(RiftConfig.BOSS_SHARE);
        long seed = 90_000L;
        for (int tries = 0; RiftRules.variantFor(seed, share) != want && tries < 512; tries++) seed++;
        if (RiftRules.variantFor(seed, share) != want) {
            notes.add(k + "skipped: bossShare=" + share + " allows none");
            return SelfTests.Result.ok("");
        }
        PocketRealm realm = PocketRealm.get(server);
        if (realm.at(TEST_SLOT) != null) realm.collapseSlot(mega, TEST_SLOT);
        List<Integer> held = new ArrayList<>();
        List<BlockPos> platform = new ArrayList<>();
        BlockPos site = new BlockPos(RiftRules.POCKET_X0 + 20, RiftRules.POCKET_Y + 20, RiftRules.POCKET_Z + 400);       // outside the pocket field's cells, inside the build height
        FakePlayer p = SelfTests.standIn(mega, "rift_e2e_" + want.id(), site.getX() + 0.5, site.getY(), site.getZ() + 0.5);
        PocketRealm.Pocket pk = null;
        try {
            for (int i = 0; i < TEST_SLOT; i++)
                if (realm.at(i) == null) {                                                  // the first free slot must be the test cell: the others are held for the length of the test
                    PocketRealm.Pocket ph = new PocketRealm.Pocket();
                    ph.slot = i;
                    ph.expiresAt = Long.MAX_VALUE;
                    realm.pockets().put(i, ph);
                    held.add(i);
                }
            mega.getChunk(site.getX() >> 4, site.getZ() >> 4);
            for (int dx = -3; dx <= 3; dx++)
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos floor = site.offset(dx, -1, dz);
                    mega.setBlock(floor, Blocks.STONE.defaultBlockState(), 2);
                    platform.add(floor);
                    for (int dy = 0; dy <= 3; dy++) mega.setBlock(site.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                }
            // 1. the rift opens and ticks
            DimensionalRiftBlockEntity be = RiftService.open(mega, site, RiftType.POCKET, seed);
            if (be == null) return SelfTests.Result.fail(k + "a pocket rift did not open at " + site.toShortString());
            if (!RiftService.openRifts(mega).contains(site)) return SelfTests.Result.fail(k + "the open list of the dimension does not hold the rift");
            int life = be.left();
            for (int i = 0; i < 40; i++) DimensionalRiftBlockEntity.tick(mega, site, mega.getBlockState(site), be);
            if (be.age() != 40 || be.left() != life - 40) return SelfTests.Result.fail(k + "the rift's tick did not age it: age " + be.age() + ", life " + life + " -> " + be.left());
            // 2. outside the tear nothing happens, inside it the player is taken
            p.setPos(site.getX() + 5.5, site.getY(), site.getZ() + 0.5);
            RiftService.step(mega, be, List.of(p));
            if (realm.at(TEST_SLOT) != null || RiftRules.slotAt(p.getX(), p.getZ()) >= 0) return SelfTests.Result.fail(k + "a player five blocks from the tear was taken in");
            p.setPos(site.getX() + 0.5, site.getY(), site.getZ() + 0.5);
            RiftService.step(mega, be, List.of(p));
            pk = realm.at(TEST_SLOT);
            if (pk == null) return SelfTests.Result.fail(k + "stepping into the tear built no hidden realm" + (be.failures() > 0 ? " (the rift logged: " + be.lastError() + ")" : ""));
            if (pk.seed != seed || !want.id().equals(pk.variant)) return SelfTests.Result.fail(k + "the realm is a " + pk.variant + " pocket of seed " + pk.seed + ", not a " + want.id() + " pocket of seed " + seed);
            if (be.slot() != TEST_SLOT) return SelfTests.Result.fail(k + "the rift does not know its realm (slot " + be.slot() + ")");
            // 3. arrival
            if (RiftRules.slotAt(p.getX(), p.getZ()) != TEST_SLOT) return SelfTests.Result.fail(k + "the player is not in the realm: " + p.position());
            if (p.position().distanceTo(Vec3.atBottomCenterOf(pk.entry)) > 1.5) return SelfTests.Result.fail(k + "the player is not in the antechamber: " + p.position() + " instead of " + pk.entry.toShortString());
            CompoundTag back = p.getPersistentData().getCompound(PocketRealm.RETURN);
            if (back.isEmpty() || back.getInt("RX") != site.getX() || back.getInt("RZ") != site.getZ()) return SelfTests.Result.fail(k + "the way back was not remembered: " + back);
            if (pk.bars.size() != 9) return SelfTests.Result.fail(k + "the vault door has " + pk.bars.size() + " bars");
            for (BlockPos b : pk.bars) if (!mega.getBlockState(b).is(Blocks.IRON_BARS)) return SelfTests.Result.fail(k + "the vault is not barred at " + b.toShortString());
            if (!(mega.getBlockEntity(pk.exit) instanceof DimensionalRiftBlockEntity exit) || exit.type() != RiftType.EXIT) return SelfTests.Result.fail(k + "there is no exit rift at " + pk.exit.toShortString());
            if (pk.entry.distSqr(pk.exit) > 16) return SelfTests.Result.fail(k + "the exit rift is out of reach of the arrival: " + pk.entry.toShortString() + " / " + pk.exit.toShortString());
            // 4. clear it
            if (want == RiftRules.Variant.BOSS) {
                var bosses = mega.getEntitiesOfClass(LabyrinthGuardianEntity.class, PocketBuilder.bounds(TEST_SLOT), g -> g.getTags().contains(PocketBuilder.BOSS_TAG));
                if (bosses.size() != 1) return SelfTests.Result.fail(k + "the arena holds " + bosses.size() + " secret bosses, not 1");
                NeoForge.EVENT_BUS.post(new LivingDeathEvent(bosses.get(0), mega.damageSources().genericKill()));         // the death goes through the game bus, as it does in play
                if (!pk.bossDead) return SelfTests.Result.fail(k + "the death of the secret boss did not clear the realm: its handler is not on the game bus");
                bosses.get(0).discard();
            } else {
                int[] order = RiftRules.puzzleOrder(pk.seed);
                RiftRules.Press last = null;
                for (int i = 0; i < 4; i++) last = PocketEvents.press(mega, realm, pk, p, order[i], pk.plates[order[i]]);
                if (last != RiftRules.Press.SOLVED || !pk.solved) return SelfTests.Result.fail(k + "the plates in the right order did not clear the realm");
            }
            for (BlockPos b : pk.bars) if (!mega.getBlockState(b).isAir()) return SelfTests.Result.fail(k + "a bar stayed after the realm was cleared at " + b.toShortString());
            if (pk.clearedAt < 0) return SelfTests.Result.fail(k + "clearing did not stamp the time");
            if (realm.clear(mega, pk, "again")) return SelfTests.Result.fail(k + "clearing a cleared realm opened the vault a second time");
            // 5. the reward
            int stacks = 0;
            for (int i = 0; i < 3; i++) {
                BlockPos at = new BlockPos(RiftRules.pocketX(TEST_SLOT) + 18, RiftRules.POCKET_Y, RiftRules.POCKET_Z - 2 + i * 2);
                if (!(mega.getBlockEntity(at) instanceof ChestBlockEntity chest)) return SelfTests.Result.fail(k + "no chest at " + at.toShortString());
                if (chest.getLootTable() == null) return SelfTests.Result.fail(k + "the chest at " + at.toShortString() + " has no loot table");
                chest.unpackLootTable(p);
                for (int s = 0; s < chest.getContainerSize(); s++) if (!chest.getItem(s).isEmpty()) stacks++;
            }
            if (stacks == 0) return SelfTests.Result.fail(k + "the three chests of the vault gave nothing");
            // 6. the way out: held back by the cooldown, then out beside the rift and not inside it
            p.setPos(pk.exit.getX() + 0.5, pk.exit.getY(), pk.exit.getZ() + 0.5);
            RiftService.step(mega, exit, List.of(p));
            if (RiftRules.slotAt(p.getX(), p.getZ()) != TEST_SLOT) return SelfTests.Result.fail(k + "the exit took the player although the tear's cooldown still held");
            p.getPersistentData().remove(RiftService.TOUCH);
            RiftService.step(mega, exit, List.of(p));
            if (RiftRules.slotAt(p.getX(), p.getZ()) >= 0) return SelfTests.Result.fail(k + "the exit rift did not take the player out of the realm: " + p.position() + (exit.failures() > 0 ? " (the exit logged: " + exit.lastError() + ")" : ""));
            if (RiftRules.inTear(site.getX(), site.getY(), site.getZ(), p.getX(), p.getY(), p.getZ()))
                return SelfTests.Result.fail(k + "the player came out INSIDE the tear they went in by (" + p.position() + "), which takes them in again");
            if (p.position().distanceTo(Vec3.atBottomCenterOf(site)) > 4.0) return SelfTests.Result.fail(k + "the player came out far from the rift: " + p.position());
            if (!p.getPersistentData().getCompound(PocketRealm.RETURN).isEmpty()) return SelfTests.Result.fail(k + "the way back was not forgotten after the exit");
            // 7. the realm collapses and frees its slot
            BlockPos exitPos = pk.exit, bar0 = pk.bars.get(0);
            realm.collapseSlot(mega, TEST_SLOT);
            if (realm.at(TEST_SLOT) != null) return SelfTests.Result.fail(k + "the realm did not free its slot");
            if (!mega.getBlockState(bar0).isAir() || mega.getBlockState(exitPos).is(RiftRegistry.RIFT.get()) || RiftService.openRifts(mega).contains(exitPos)) return SelfTests.Result.fail(k + "the cell was not cleared");
            notes.add(k + "opened, entered, arrived, cleared, " + stacks + " stacks of loot, left beside the rift, collapsed");
        } finally {
            if (realm.at(TEST_SLOT) != null) realm.collapseSlot(mega, TEST_SLOT);
            for (int i : held) realm.pockets().remove(i);
            realm.setDirty();
            if (mega.getBlockState(site).is(RiftRegistry.RIFT.get())) mega.setBlock(site, Blocks.AIR.defaultBlockState(), 2);
            RiftService.noteGone(mega, site);
            for (BlockPos b : platform) mega.setBlock(b, Blocks.AIR.defaultBlockState(), 2);
            p.getPersistentData().remove(PocketRealm.RETURN);
            p.getPersistentData().remove(RiftService.TOUCH);
        }
        return SelfTests.Result.ok("");
    }

    private static SelfTests.Result e2eShortcut(MinecraftServer server, ServerLevel mega, List<String> notes) {
        String k = "[shortcut] ";
        MegaData data = MegaData.get(server);
        Sector low = null, high = null;
        search:
        for (int party : data.parties())
            for (Polarity road : Polarity.values()) {
                Sector prev = null;
                for (Sector s : data.chain(party, road)) {
                    boolean good = s.isFloor() && SectorGenerator.playable(s) && SectorGenerator.padOf(s) != null;
                    if (good && prev != null && s.chainIndex == prev.chainIndex + 1) {
                        low = prev;
                        high = s;
                        break search;
                    }
                    prev = good ? s : null;
                }
            }
        if (low == null) {
            notes.add(k + "skipped: this world has no two finished floors in a row");
            return SelfTests.Result.ok("");
        }
        BlockPos lowPad = SectorGenerator.padOf(low);
        BlockPos site = findSite(mega, low, lowPad);
        if (site == null) {
            notes.add(k + "skipped: no free cell on floor " + FloorMap.label(low.firstFloor()));
            return SelfTests.Result.ok("");
        }
        FakePlayer p = SelfTests.standIn(mega, "rift_e2e_shortcut", site.getX() + 0.5, site.getY(), site.getZ() + 0.5);
        DimensionalRiftBlockEntity be = null;
        try {
            if (!MegaDungeon.mayEnter(p, low.polarity)) {
                notes.add(k + "skipped: the Directional Lock refuses the stand-in on the " + low.polarity.id() + " road");
                return SelfTests.Result.ok("");
            }
            // 1. a steady shortcut to the next floor
            be = RiftService.open(mega, site, RiftType.SHORTCUT, 4242L);
            if (be == null) return SelfTests.Result.fail(k + "a shortcut rift did not open at " + site.toShortString());
            be.aimShortcut(low.party, low.polarity, low.chainIndex, high.chainIndex);
            RiftService.refreshStability(mega, be, false);
            if (!be.stable()) return SelfTests.Result.fail(k + "a shortcut to a finished floor is not steady");
            LabyrinthTravel.forget(p);
            p.getPersistentData().remove(RiftService.TOUCH);
            RiftService.step(mega, be, List.of(p));
            BlockPos sent = LabyrinthTravel.arrivalPad(p), want = SectorGenerator.padOf(high);
            if (!want.equals(sent)) return SelfTests.Result.fail(k + "the shortcut sent the player to " + (sent == null ? "nowhere" : sent.toShortString()) + " instead of the arrival of floor " + FloorMap.label(high.firstFloor()) + " at " + want.toShortString());
            if (be.left() > 1) return SelfTests.Result.fail(k + "a used shortcut did not close");
            RiftService.collapse(mega, site, be);
            if (mega.getBlockState(site).is(RiftRegistry.RIFT.get())) return SelfTests.Result.fail(k + "the used shortcut's block is still there");
            // 2. a shortcut aimed past the last built floor holds the player back, then settles
            Sector tail = data.chainTail(low.party, low.polarity);
            int far = tail.chainIndex + 3;
            p.setPos(site.getX() + 0.5, site.getY(), site.getZ() + 0.5);
            be = RiftService.open(mega, site, RiftType.SHORTCUT, 4243L);
            if (be == null) return SelfTests.Result.fail(k + "a second shortcut rift did not open");
            be.aimShortcut(low.party, low.polarity, low.chainIndex, far);
            RiftService.refreshStability(mega, be, false);
            if (be.stable()) return SelfTests.Result.fail(k + "a shortcut aimed at chain " + far + " (not built) is steady");
            LabyrinthTravel.forget(p);
            p.getPersistentData().remove(RiftService.TOUCH);
            RiftService.step(mega, be, List.of(p));
            if (LabyrinthTravel.arrivalPad(p) != null) return SelfTests.Result.fail(k + "a shortcut to a floor that is not built carried the player");
            be.waited(RiftRules.FORM_PATIENCE_TICKS);
            RiftService.refreshStability(mega, be, false);
            if (tail.chainIndex > low.chainIndex) {
                if (!be.stable() || be.targetChain() <= low.chainIndex || be.targetChain() > tail.chainIndex)
                    return SelfTests.Result.fail(k + "after its patience the shortcut did not settle for a built floor (stable " + be.stable() + ", target " + be.targetChain() + ", last built " + tail.chainIndex + ")");
                p.getPersistentData().remove(RiftService.TOUCH);
                RiftService.step(mega, be, List.of(p));
                BlockPos settledPad = SectorGenerator.padOf(data.chainSector(low.party, low.polarity, be.targetChain()));
                if (!settledPad.equals(LabyrinthTravel.arrivalPad(p))) return SelfTests.Result.fail(k + "the settled shortcut did not carry the player to its floor");
                notes.add(k + "steady jump, held while forming, settled for chain " + be.targetChain());
            } else {
                notes.add(k + "steady jump and held while forming (no floor beyond this one, so settling was not exercised)");
            }
        } finally {
            if (be != null && mega.getBlockState(site).is(RiftRegistry.RIFT.get())) RiftService.collapse(mega, site, be);
            LabyrinthTravel.forget(p);
            p.getPersistentData().remove(RiftService.TOUCH);
        }
        return SelfTests.Result.ok("");
    }

    /** A free cell for a test rift on floor {@code s}: an open spot near its arrival pad, else the pad's own cell when that is free. */
    private static BlockPos findSite(ServerLevel mega, Sector s, BlockPos pad) {
        for (int i = 0; i < 4; i++) {
            BlockPos at = RiftService.findSpotNear(mega, s, pad.getX(), pad.getY(), pad.getZ());
            if (at != null) return at;
        }
        return mega.getBlockState(pad).isAir() ? pad : null;
    }

    private static SelfTests.Result e2eSpawn(MinecraftServer server, ServerLevel mega, List<String> notes) {
        String k = "[spawner] ";
        MegaData data = MegaData.get(server);
        Sector floor = null;
        for (Sector s : data.sectors()) if (s.isFloor() && SectorGenerator.playable(s) && !s.isBossFloor() && !s.rooms.isEmpty()) {
            floor = s;
            break;
        }
        if (floor == null) {
            notes.add(k + "skipped: this world has no finished floor that is not a boss floor");
            return SelfTests.Result.ok("");
        }
        FakePlayer p = SelfTests.standIn(mega, "rift_e2e_spawner", 0, 70, 0);
        DimensionalRiftBlockEntity found = null;
        try {
            boolean placed = false;
            for (Room room : floor.rooms) {                                                      // a room where the spawner's conditions hold: not a safe room, an open spot within reach
                if (room.kind.isSafe()) continue;
                BlockPos c = room.center();
                p.setPos(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
                if (RiftService.gateFor(mega, p, true) == RiftRules.RollGate.OK && RiftService.findSpotNear(mega, floor, p.getX(), p.getY(), p.getZ()) != null) {
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                notes.add(k + "skipped: no room of floor " + FloorMap.label(floor.firstFloor()) + " has an open spot for a rift");
                return SelfTests.Result.ok("");
            }
            String why = "no_spot";
            for (int i = 0; i < 6 && why != null && why.equals("no_spot"); i++) why = RiftService.rollNow(mega, p, RiftType.SHORTCUT);
            if ("dead_end".equals(why)) {
                notes.add(k + "the roll refused a dead end (nothing beyond this floor and expansion is off)");
                return SelfTests.Result.ok("");
            }
            if (why != null) return SelfTests.Result.fail(k + "a roll for a shortcut on floor " + FloorMap.label(floor.firstFloor()) + " opened nothing: " + why);
            found = RiftService.nearest(mega, p.blockPosition(), 30);
            if (found == null || found.type() != RiftType.SHORTCUT) return SelfTests.Result.fail(k + "the roll reported success but no shortcut rift stands within 30 blocks");
            if (found.targetChain() <= found.fromChain()) return SelfTests.Result.fail(k + "the spawned shortcut leads nowhere (chain " + found.fromChain() + " to " + found.targetChain() + ")");
            if (found.fromChain() != floor.chainIndex || found.party() != floor.party) return SelfTests.Result.fail(k + "the spawned shortcut was aimed from the wrong floor");
            if (RiftConfig.get(RiftConfig.MAX_NEAR) == 1 && !"crowded".equals(RiftService.rollNow(mega, p, RiftType.SHORTCUT))) return SelfTests.Result.fail(k + "a second roll next to an open rift was not refused as crowded");
            BlockPos at = found.getBlockPos();
            RiftService.collapse(mega, at, found);
            found = null;
            notes.add(k + "a roll on floor " + FloorMap.label(floor.firstFloor()) + " opened a shortcut aimed forward");
        } finally {
            if (found != null) RiftService.collapse(mega, found.getBlockPos(), found);
        }
        return SelfTests.Result.ok("");
    }
}
