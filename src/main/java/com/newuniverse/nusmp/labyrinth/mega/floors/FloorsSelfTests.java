package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.newuniverse.nusmp.labyrinth.mega.FloorListener;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: nothing in this package can be played by its author, so each part of it has a SELF-TEST that the CI server smoke test runs in the real game
 * ({@code /multiverse mega selftest}), against sector 0 that the smoke script has just generated (and, when it exists, the first sector of the other road,
 * so the negative floors of the Abyssal chain are walked too), and that skips with an OK line when the dimension or the sector is missing (a build without
 * the generator). {@code floors.bands} numbers the sectors and checks the result against the rules the brief states (the Landing is the first floor, the
 * arena the last, no number outside the sector's range, no floor falls along a doorway to a deeper room, the rule of the configured banding strategy holds:
 * equal runs of rooms for {@code even}, a bounded rise per doorway for {@code arena}, and a second run changes nothing); {@code floors.old_world} strips
 * sector 0 of its floors, the way a world saved by 1.54.0 has it, and checks that the start-up pass and the tracker number it again to the same floors and the
 * same length; {@code floors.alignment} walks a stand-in player through the story of the lock (step onto +1, the other road is still open, move on, the
 * other road is shut through the real {@code MegaDungeon.mayEnter} so the registration of the policy is tested too, reset); {@code floors.save} pushes the
 * alignment through the real NBT codec and reads an empty tag; {@code floors.sync} pushes the packet through a registry-aware buffer; {@code floors.tracker}
 * puts the stand-in at the centre of the Landing and then of the arena and checks the floor events, the clear and the lock that follow;
 * {@code floors.hard_lock} checks the guard's decision for a player on their own road, on the other one and not locked. The stand-in is a
 * {@code FakePlayer}, which is not in the level's player list, so the tests call the public entry points ({@code FloorTracker.check},
 * {@code HardLock.shouldCastOut}) directly; it has no connection, so nothing is sent to it and the guard's expulsion itself is never run on it (its DECISION
 * is). Every test restores what it touched: the stand-in's alignment is reset, the tracker forgets it, and sector 0's floors are put back, whatever happens.
 */
public final class FloorsSelfTests {
    private FloorsSelfTests() {}

    private static final String PLAYER = "mfl_tester";
    private static final Recorder RECORDER = new Recorder();

    /** Registers the tests and the recording listener; called once from {@code FloorsRegistry.init}. */
    public static void register() {
        MegaDungeon.addFloorListener(RECORDER);
        SelfTests.register("floors.bands", FloorsSelfTests::bands);
        SelfTests.register("floors.old_world", FloorsSelfTests::oldWorld);
        SelfTests.register("floors.alignment", FloorsSelfTests::alignment);
        SelfTests.register("floors.save", FloorsSelfTests::save);
        SelfTests.register("floors.sync", FloorsSelfTests::sync);
        SelfTests.register("floors.tracker", FloorsSelfTests::tracker);
        SelfTests.register("floors.hard_lock", FloorsSelfTests::hardLock);
    }

    private static SelfTests.Result ok(String detail) { return SelfTests.Result.ok(detail); }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static String firstFew(List<String> problems) {
        return problems.size() + " problem(s): " + String.join(" | ", problems.subList(0, Math.min(5, problems.size())));
    }

    /**
     * Why: the tracker test must hear exactly what the other modules would hear, so it registers as a real {@link FloorListener}; it is registered once for
     * the life of the game (listeners are never removed) and does nothing unless a test has set {@link #sink} for its own stand-in player.
     */
    private static final class Recorder implements FloorListener {
        volatile List<String> sink;
        volatile String only;

        @Override
        public void onFloorEntered(ServerPlayer player, int floor, int previous) { record(player, "enter " + FloorMap.label(previous) + "->" + FloorMap.label(floor)); }

        @Override
        public void onFloorCleared(ServerPlayer player, int floor) { record(player, "clear " + FloorMap.label(floor)); }

        private void record(ServerPlayer player, String what) {
            List<String> s = sink;
            String n = only;
            if (s != null && n != null && n.equals(player.getGameProfile().getName())) s.add(what);
        }
    }

    /** Sector 0 with its rooms, or null with {@code why[0]} set to the reason a test should skip. */
    private static Sector sector0(MinecraftServer server, String[] why) {
        if (SelfTests.mega(server) == null) { why[0] = "skipped: this build has no mega dimension"; return null; }
        Sector s = MegaData.get(server).sector(0);
        if (s == null) { why[0] = "skipped: sector 0 does not exist yet (/multiverse mega expand creates it)"; return null; }
        if (s.state == Sector.State.GENERATING || s.rooms.isEmpty()) { why[0] = "skipped: sector 0 is still being generated"; return null; }
        return s;
    }

    /** A position inside a room, as the stand-in player's coordinates: the middle of the standing cell. */
    private static double[] stand(Room r) {
        BlockPos c = r.center();
        return new double[] {c.getX() + 0.5, c.getY(), c.getZ() + 0.5};
    }

    /**
     * The sectors a numbering or tracking test looks at: sector 0 (the contract's first sector) and the first sector of each road when it exists, is finished
     * and has rooms (so a world that opened the Abyssal road also walks the negative floors). Empty, with the reason in {@code why[0]}, when sector 0 is not ready.
     */
    private static List<Sector> testSectors(MinecraftServer server, String[] why) {
        Sector s0 = sector0(server, why);
        if (s0 == null) return List.of();
        List<Sector> out = new ArrayList<>();
        out.add(s0);
        for (Polarity road : Polarity.values()) {
            List<Sector> chain = MegaData.get(server).chain(road);
            if (chain.isEmpty()) continue;
            Sector first = chain.get(0);
            if (first != s0 && first.state != Sector.State.GENERATING && first.state != Sector.State.FAILED && !first.rooms.isEmpty()) out.add(first);
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------------ the numbering
    private static SelfTests.Result bands(MinecraftServer server) {
        String[] why = new String[1];
        List<Sector> sectors = testSectors(server, why);
        if (sectors.isEmpty()) return ok(why[0]);
        MegaData data = MegaData.get(server);
        List<String> bad = new ArrayList<>();
        List<String> said = new ArrayList<>();
        for (Sector s : sectors) {
            String line = checkBands(data, s, bad);
            if (line != null) said.add(line);
        }
        return bad.isEmpty() ? ok(String.join("; ", said)) : fail(firstFew(bad));
    }

    /** Numbers one sector and checks the result; problems go to {@code bad}, the one-line summary is returned (null when the sector could not be checked at all). */
    private static String checkBands(MegaData data, Sector s, List<String> bad) {
        String name = "sector " + s.id + " (" + s.polarity.id() + ")";
        FloorBands.Strategy strategy = FloorAssigner.strategy();
        FloorAssigner.assign(s, data);
        data.setDirty();
        int first = Math.abs(s.firstFloor()), last = Math.abs(s.lastFloor());
        Room landing = FloorAssigner.landingOf(s), arena = FloorAssigner.arenaOf(s);
        if (landing == null) {
            bad.add(name + " has no Landing room (no room holds its arrival marker and none is of kind LANDING)");
            return null;
        }
        if (arena == null) {
            bad.add(name + " has no room of kind ARENA, so the Guardian's floor cannot be checked");
            return null;
        }
        if (landing.floor != s.firstFloor()) bad.add(name + ": the Landing (room #" + landing.id + ") is floor " + FloorMap.label(landing.floor) + ", expected " + FloorMap.label(s.firstFloor()));
        if (arena.floor != s.lastFloor()) bad.add(name + ": the arena (room #" + arena.id + ") is floor " + FloorMap.label(arena.floor) + ", expected " + FloorMap.label(s.lastFloor()));
        List<FloorBands.Node> nodes = new ArrayList<>();
        for (Room r : s.rooms) nodes.add(new FloorBands.Node(r.id, r.kind));
        Map<Integer, Integer> depth = FloorBands.depths(nodes, landing.id, s.edges);
        int arenaDepth = depth.get(arena.id);
        int spread = arenaDepth <= 0 ? Math.max(1, s.perRegion - 1) : (s.perRegion - 1 + arenaDepth - 1) / arenaDepth;       // the most one doorway can raise a band under the arena rule
        int[] perBand = new int[s.perRegion + 1];
        Map<Integer, Room> byId = new HashMap<>();
        for (Room r : s.rooms) {
            byId.put(r.id, r);
            if (r.floor == 0) { bad.add(name + ": room #" + r.id + " (" + r.kind + ") has no floor"); continue; }
            if (Polarity.ofFloor(r.floor) != s.polarity) bad.add(name + ": room #" + r.id + " is floor " + FloorMap.label(r.floor) + ", on the wrong road for a " + s.polarity.id() + " sector");
            int d = Math.abs(r.floor);
            if (d < first || d > last) bad.add(name + ": room #" + r.id + " is floor " + FloorMap.label(r.floor) + ", outside the sector's " + FloorMap.label(s.firstFloor()) + ".." + FloorMap.label(s.lastFloor()));
            else perBand[FloorMap.band(r.floor, s.perRegion)]++;
            if (FloorBands.isBeyondArena(r.kind) && r.floor != s.lastFloor()) bad.add(name + ": room #" + r.id + " (" + r.kind + ") is behind the arena but is floor " + FloorMap.label(r.floor));
        }
        for (int[] e : s.edges) {
            Room a = byId.get(e[0]), b = byId.get(e[1]);
            if (a == null || b == null || a == landing || b == landing || a.floor == 0 || b.floor == 0) continue;
            if (!plain(a) || !plain(b)) continue;
            int da = depth.get(a.id), db = depth.get(b.id);
            if (da < 0 || db < 0 || da == db) continue;                                  // rooms at one depth may sit on neighbouring floors under the even rule
            Room near = da < db ? a : b, far = near == a ? b : a;
            int rise = Math.abs(far.floor) - Math.abs(near.floor);
            if (rise < 0) bad.add(name + ": floors fall along the doorway #" + near.id + " -> #" + far.id + " (" + FloorMap.label(near.floor) + " to " + FloorMap.label(far.floor) + ")");
            else if (strategy == FloorBands.Strategy.ARENA && rise > spread) bad.add(name + ": floors jump by " + rise + " along the doorway #" + near.id + " -> #" + far.id + " (more than the " + spread + " the arena rule allows)");
        }
        if (strategy == FloorBands.Strategy.EVEN && s.perRegion >= 2) {
            int open = s.perRegion - 1;
            int[] runs = new int[open + 1];
            int plainRooms = 0;
            for (Room r : s.rooms) {
                if (r == landing || !plain(r) || depth.get(r.id) < 0 || r.floor == 0) continue;
                int b = FloorMap.band(r.floor, s.perRegion);
                plainRooms++;
                if (b < 1 || b > open) bad.add(name + ": room #" + r.id + " is floor " + FloorMap.label(r.floor) + " but only the boss chain belongs on the last floor");
                else runs[b]++;
            }
            int lo = Integer.MAX_VALUE, hi = 0;
            for (int b = 1; b <= open; b++) {
                lo = Math.min(lo, runs[b]);
                hi = Math.max(hi, runs[b]);
            }
            if (plainRooms > 0 && hi - lo > 1) bad.add(name + ": the even rule must cut equal runs but the rooms per floor are " + Arrays.toString(Arrays.copyOfRange(runs, 1, open + 1)));
        }
        if (FloorAssigner.assign(s, data) != 0) bad.add(name + ": a second numbering changed rooms: the assigner is not idempotent");
        int usedCount = 0;
        for (int b = 1; b <= s.perRegion; b++) if (perBand[b] > 0) usedCount++;
        if (usedCount < 2) bad.add(name + ": only " + usedCount + " distinct floor(s) are used");
        return name + ": " + s.rooms.size() + " rooms numbered " + FloorMap.label(s.firstFloor()) + ".." + FloorMap.label(s.lastFloor()) + " by the " + strategy.id() + " rule, " + usedCount + " distinct floors, rooms per floor "
                + Arrays.toString(Arrays.copyOfRange(perBand, 1, perBand.length)) + ", arena depth " + arenaDepth + ", Landing " + FloorMap.label(landing.floor) + ", arena " + FloorMap.label(arena.floor) + ", a second run changed nothing";
    }

    /** A room whose floor comes from its depth alone (not a link and not the boss chain; the Landing is excluded by the caller). */
    private static boolean plain(Room r) {
        return r.kind != RoomKind.LINK && !FloorBands.isBeyondArena(r.kind);
    }

    // ------------------------------------------------------------------------------------------------ a world saved before floors existed
    private static SelfTests.Result oldWorld(MinecraftServer server) {
        String[] why = new String[1];
        Sector s = sector0(server, why);
        if (s == null) return ok(why[0]);
        ServerLevel mega = SelfTests.mega(server);
        MegaData data = MegaData.get(server);
        FloorAssigner.assign(s, data);
        Room landing = FloorAssigner.landingOf(s);
        if (landing == null) return fail("sector 0 has no Landing room");
        int[] saved = new int[s.rooms.size()];
        for (int i = 0; i < saved.length; i++) saved[i] = s.rooms.get(i).floor;
        int perBefore = s.perRegion;
        List<String> bad = new ArrayList<>();
        try {
            for (Room r : s.rooms) r.floor = 0;                                      // what a sector saved by 1.54.0 looks like: rooms without floors
            if (FloorAssigner.isBanded(s)) bad.add("a sector whose rooms all have floor 0 reads as numbered");
            if (MegaDungeon.floorAt(mega, landing.center()) != s.firstFloor()) bad.add("an unnumbered Landing should read the sector's first floor, it reads " + FloorMap.label(MegaDungeon.floorAt(mega, landing.center())));
            int numbered = FloorAssigner.numberOldSectors(server);
            if (numbered < 1) bad.add("the start-up pass numbered no sector although one had no floors");
            for (int i = 0; i < saved.length && bad.isEmpty(); i++) {
                Room r = s.rooms.get(i);
                if (r.floor != saved[i]) bad.add("room #" + r.id + " came out floor " + FloorMap.label(r.floor) + " after the old-world numbering, it was " + FloorMap.label(saved[i]) + " before");
            }
            if (s.perRegion != perBefore) bad.add("the length of the sector changed from " + perBefore + " to " + s.perRegion + " when it was numbered again");
            for (Room r : s.rooms) r.floor = 0;
            int viaTracker = FloorTracker.floorAt(mega, landing.center());            // the tracker numbers an unnumbered sector on first contact
            if (viaTracker != s.firstFloor()) bad.add("the tracker read floor " + FloorMap.label(viaTracker) + " at the Landing of an unnumbered sector, expected " + FloorMap.label(s.firstFloor()));
            if (!FloorAssigner.isBanded(s)) bad.add("the tracker did not number the sector it was asked about");
            if (FloorAssigner.numberOldSectors(server) != 0) bad.add("a second start-up pass numbered something again");
        } finally {
            for (int i = 0; i < saved.length; i++) s.rooms.get(i).floor = saved[i];       // leave the numbers as they were
            s.perRegion = perBefore;
            data.setDirty();
        }
        return bad.isEmpty() ? ok("sector 0 stripped of its floors was numbered again by the start-up pass and by the tracker, to the same floors and the same length (" + perBefore + "); a second pass did nothing") : fail(firstFew(bad));
    }

    // ------------------------------------------------------------------------------------------------ the lock
    private static SelfTests.Result alignment(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return ok("skipped: this build has no mega dimension");
        ServerPlayer p = SelfTests.player(mega, PLAYER, 0.5, 70, 0.5);
        if (LockPolicy.bypasses(p)) return ok("skipped: the stand-in player counts as an operator here and passes the lock");
        boolean lockOn = FloorsConfig.get(FloorsConfig.LOCK_ENABLED);
        PlayerDungeonAlignment a = AlignmentAttachments.get(p);
        List<String> bad = new ArrayList<>();
        try {
            AlignmentRules.reset(a);
            if (AlignmentAttachments.get(p) != a) bad.add("the attachment is not the same live object on a second read");
            if (!MegaDungeon.mayEnter(p, Polarity.ABYSSAL) || !MegaDungeon.mayEnter(p, Polarity.HOLY)) bad.add("a fresh player was refused a road");
            AlignmentRules.onFloorEntered(a, 1);
            if (a.chosen() != Polarity.HOLY || a.locked()) bad.add("stepping onto +1 should choose the Holy road without locking (road " + a.chosen() + ", locked " + a.locked() + ")");
            if (!MegaDungeon.mayEnter(p, Polarity.ABYSSAL)) bad.add("before the lock the Abyssal road must still be open");
            boolean clears = AlignmentRules.clearsPrevious(a, 1, 2);
            AlignmentRules.onFloorEntered(a, 2);
            boolean locked = clears && AlignmentRules.onFloorCleared(a, 1, 1, mega.getGameTime());
            if (!clears || !locked || !a.locked()) bad.add("moving on from +1 should clear it and lock (clears " + clears + ", newly locked " + locked + ", locked " + a.locked() + ")");
            if (AlignmentAttachments.get(p).lockedAt() != mega.getGameTime()) bad.add("the lock time was not stored");
            boolean abyssal = MegaDungeon.mayEnter(p, Polarity.ABYSSAL), holy = MegaDungeon.mayEnter(p, Polarity.HOLY);
            if (lockOn && abyssal) bad.add("MegaDungeon.mayEnter let a player locked to the Holy road take the Abyssal road: the policy is not registered or does not obey the lock");
            if (!lockOn && !abyssal) bad.add("the lock is switched off in the config but the Abyssal road was refused");
            if (!holy) bad.add("a player locked to the Holy road was refused the Holy road");
            AlignmentRules.reset(a);
            if (!a.isDefault() || !MegaDungeon.mayEnter(p, Polarity.ABYSSAL)) bad.add("after a reset the state is not fresh or the Abyssal road is still shut");
        } finally {
            AlignmentRules.reset(a);
            FloorTracker.forget(p.getUUID());
        }
        return bad.isEmpty() ? ok("fresh player free; +1 chose Holy without locking; moving on locked them" + (lockOn ? " and shut the Abyssal road through MegaDungeon.mayEnter" : " (the lock is off in the config, so nothing was shut)") + "; reset opened it again")
                : fail(firstFew(bad));
    }

    // ------------------------------------------------------------------------------------------------ the save and the packet
    private static SelfTests.Result save(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        PlayerDungeonAlignment[] samples = {
                new PlayerDungeonAlignment(),
                PlayerDungeonAlignment.of("holy", false, 1, 0, 0),
                PlayerDungeonAlignment.of("holy", true, 12, 3, 4_000_000_000L),
                PlayerDungeonAlignment.of("abyssal", true, 0, 25, 77)
        };
        for (PlayerDungeonAlignment s : samples) {
            Tag tag = AlignmentAttachments.CODEC.encodeStart(NbtOps.INSTANCE, s).result().orElse(null);
            if (tag == null) { bad.add("the codec cannot write " + s); continue; }
            PlayerDungeonAlignment back = AlignmentAttachments.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null);
            if (!s.equals(back)) bad.add("the codec read back " + back + " from " + tag + ", expected " + s);
        }
        PlayerDungeonAlignment empty = AlignmentAttachments.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).result().orElse(null);
        if (empty == null || !empty.isDefault()) bad.add("an empty tag should load as the fresh state, got " + empty);
        CompoundTag damaged = new CompoundTag();
        damaged.putString("chosen", "sideways");
        damaged.putBoolean("locked", true);
        damaged.putInt("holy", -4);
        PlayerDungeonAlignment repaired = AlignmentAttachments.CODEC.parse(NbtOps.INSTANCE, damaged).result().orElse(null);
        if (repaired == null || repaired.chosen() != null || repaired.locked() || repaired.deepestHoly() != 0) bad.add("a damaged tag should load as a valid state, got " + repaired);
        return bad.isEmpty() ? ok(samples.length + " states survive the NBT codec; an empty tag is the fresh state; a damaged tag is repaired") : fail(firstFew(bad));
    }

    private static SelfTests.Result sync(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        if (!"nusmp:alignment_sync".equals(AlignmentNet.Sync.TYPE.id().toString())) bad.add("the packet id is " + AlignmentNet.Sync.TYPE.id() + ", the contract says nusmp:alignment_sync");
        AlignmentNet.Sync[] samples = {
                AlignmentNet.Sync.of(new PlayerDungeonAlignment()),
                AlignmentNet.Sync.of(PlayerDungeonAlignment.of("holy", true, 12, 3, 99)),
                AlignmentNet.Sync.of(PlayerDungeonAlignment.of("abyssal", false, 0, 300, 0))
        };
        for (AlignmentNet.Sync out : samples) {
            RegistryFriendlyByteBuf buf = RegistryFriendlyByteBuf.decorator(server.registryAccess()).apply(Unpooled.buffer());
            try {
                AlignmentNet.Sync.STREAM_CODEC.encode(buf, out);
                AlignmentNet.Sync back = AlignmentNet.Sync.STREAM_CODEC.decode(buf);
                if (!out.equals(back)) bad.add("the packet came back as " + back + ", expected " + out);
                if (buf.readableBytes() != 0) bad.add("the packet left " + buf.readableBytes() + " unread byte(s)");
            } finally {
                buf.release();
            }
        }
        return bad.isEmpty() ? ok(samples.length + " packets round-trip through a registry-aware buffer with nothing left over") : fail(firstFew(bad));
    }

    // ------------------------------------------------------------------------------------------------ the tracker
    private static SelfTests.Result tracker(MinecraftServer server) {
        String[] why = new String[1];
        List<Sector> sectors = testSectors(server, why);
        if (sectors.isEmpty()) return ok(why[0]);
        List<String> bad = new ArrayList<>();
        List<String> said = new ArrayList<>();
        for (Sector s : sectors) {
            String line = trackSector(server, s, bad);
            if (line == null) return ok("skipped: the stand-in player counts as an operator here");
            said.add(line);
        }
        return bad.isEmpty() ? ok(String.join("; ", said)) : fail(firstFew(bad));
    }

    /** Walks the stand-in from the centre of a sector's Landing to the centre of its arena; problems go to {@code bad}; null when the stand-in is an operator (the lock does not bind it). */
    private static String trackSector(MinecraftServer server, Sector s, List<String> bad) {
        String name = "sector " + s.id + " (" + s.polarity.id() + ")";
        ServerLevel mega = SelfTests.mega(server);
        MegaData data = MegaData.get(server);
        if (!FloorAssigner.isBanded(s)) {
            FloorAssigner.assign(s, data);
            data.setDirty();
        }
        Room landing = FloorAssigner.landingOf(s), arena = FloorAssigner.arenaOf(s);
        if (landing == null || arena == null) {
            bad.add(name + " needs a Landing and an arena for this test (landing " + (landing != null) + ", arena " + (arena != null) + ")");
            return name + ": not walked";
        }
        if (landing.floor == arena.floor) {
            bad.add(name + ": the Landing and the arena are both floor " + FloorMap.label(landing.floor));
            return name + ": not walked";
        }
        double[] at = stand(landing);
        ServerPlayer p = SelfTests.player(mega, PLAYER, at[0], at[1], at[2]);
        if (LockPolicy.bypasses(p)) return null;
        PlayerDungeonAlignment a = AlignmentAttachments.get(p);
        List<String> heard = new ArrayList<>();
        boolean lockOn = FloorsConfig.get(FloorsConfig.LOCK_ENABLED);
        boolean locksAtOne = lockOn && FloorsConfig.get(FloorsConfig.LOCK_AT_FLOOR) <= 1;
        try {
            AlignmentRules.reset(a);
            FloorTracker.forget(p.getUUID());
            RECORDER.only = p.getGameProfile().getName();
            RECORDER.sink = heard;

            FloorTracker.check(p);
            expect(heard, List.of("enter 0->" + FloorMap.label(landing.floor)), name + " at the centre of the Landing", bad);
            if (FloorTracker.currentFloor(p) != landing.floor) bad.add(name + ": the tracker holds floor " + FloorMap.label(FloorTracker.currentFloor(p)) + " at the Landing, expected " + FloorMap.label(landing.floor));
            if (a.chosen() != s.polarity || a.locked()) bad.add(name + ": arriving at the Landing should choose the " + s.polarity.id() + " road without locking (road " + a.chosen() + ", locked " + a.locked() + ")");
            FloorTracker.check(p);
            expect(heard, List.of("enter 0->" + FloorMap.label(landing.floor)), name + " after a second look at the same place (no new event)", bad);

            double[] to = stand(arena);
            p.setPos(to[0], to[1], to[2]);
            FloorTracker.check(p);
            List<String> expected = new ArrayList<>(List.of("enter 0->" + FloorMap.label(landing.floor), "enter " + FloorMap.label(landing.floor) + "->" + FloorMap.label(arena.floor)));
            boolean sameRoad = Math.abs(arena.floor) > Math.abs(landing.floor);
            if (sameRoad) expected.add("clear " + FloorMap.label(landing.floor));
            expect(heard, expected, name + " after walking from the Landing to the arena", bad);
            if (a.deepest(s.polarity) != Math.abs(arena.floor)) bad.add(name + ": the deepest floor on the " + s.polarity.id() + " road is " + a.deepest(s.polarity) + ", expected " + Math.abs(arena.floor));
            if (sameRoad && Math.abs(landing.floor) >= FloorsConfig.get(FloorsConfig.LOCK_AT_FLOOR) && lockOn && (!a.locked() || a.chosen() != s.polarity)) {
                bad.add(name + ": clearing the Landing's floor should lock the player to the " + s.polarity.id() + " road (locked " + a.locked() + ", road " + a.chosen() + ")");
            }
            if (!lockOn && a.locked()) bad.add(name + ": the lock is off in the config but the player was locked");

            int before = heard.size();
            p.setPos(to[0] + 100000, to[1], to[2] + 100000);                       // far outside every sector: no room, so the floor must be kept
            FloorTracker.check(p);
            if (heard.size() != before || FloorTracker.currentFloor(p) != arena.floor) bad.add(name + ": a position in no room changed the floor (events " + heard + ", tracker " + FloorMap.label(FloorTracker.currentFloor(p)) + ")");
        } finally {
            RECORDER.sink = null;
            RECORDER.only = null;
            AlignmentRules.reset(a);
            FloorTracker.forget(p.getUUID());
        }
        return name + ": Landing " + FloorMap.label(landing.floor) + " then arena " + FloorMap.label(arena.floor) + " announced in order" + (locksAtOne ? ", the Landing's floor cleared and the player locked" : ", no lock expected at this config")
                + ", no event for the same place or for a position in no room";
    }

    private static void expect(List<String> heard, List<String> expected, String when, List<String> bad) {
        if (!heard.equals(expected)) bad.add(when + ": heard " + heard + ", expected " + expected);
    }

    // ------------------------------------------------------------------------------------------------ the hard guard
    private static SelfTests.Result hardLock(MinecraftServer server) {
        String[] why = new String[1];
        Sector s = sector0(server, why);
        if (s == null) return ok(why[0]);
        ServerLevel mega = SelfTests.mega(server);
        Room landing = FloorAssigner.landingOf(s);
        if (landing == null) return fail("sector 0 has no Landing to stand in");
        double[] at = stand(landing);
        ServerPlayer p = SelfTests.player(mega, PLAYER, at[0], at[1], at[2]);
        if (LockPolicy.bypasses(p)) return ok("skipped: the stand-in player counts as an operator here and the guard never touches operators");
        PlayerDungeonAlignment a = AlignmentAttachments.get(p);
        boolean armed = FloorsConfig.get(FloorsConfig.HARD_LOCK) && FloorsConfig.get(FloorsConfig.LOCK_ENABLED);
        Polarity here = s.polarity, other = here.opposite();
        List<String> bad = new ArrayList<>();
        try {
            AlignmentRules.reset(a);
            if (MegaDungeon.polarityAt(mega, p.blockPosition()) != here) bad.add("the sector under the stand-in is not read as a " + here.id() + " sector");
            if (HardLock.shouldCastOut(p)) bad.add("a player with no road was to be cast out");
            AlignmentRules.choose(a, other);
            if (HardLock.shouldCastOut(p)) bad.add("a player who chose the " + other.id() + " road but is not locked was to be cast out");
            AlignmentRules.reset(a);
            AlignmentRules.choose(a, here);
            AlignmentRules.lock(a, mega.getGameTime());
            if (HardLock.shouldCastOut(p)) bad.add("a player locked to the " + here.id() + " road, standing on it, was to be cast out");
            if (HardLock.check(p)) bad.add("the guard cast out a player locked to the road they stand on");
            if (p.serverLevel() != mega) bad.add("the player is no longer in the Mega-Dungeon");
            AlignmentRules.reset(a);
            AlignmentRules.choose(a, other);
            AlignmentRules.lock(a, mega.getGameTime());
            boolean decision = HardLock.shouldCastOut(p);
            if (decision != armed) bad.add("a player locked to the " + other.id() + " road, standing on a " + here.id() + " sector, " + (decision ? "was" : "was not") + " to be cast out but the config says " + (armed ? "yes" : "no"));
            if (decision) {
                FloorTracker.forget(p.getUUID());
                FloorTracker.check(p);                                                 // a player the guard is about to send out must not be tracked on the closed road
                if (FloorTracker.currentFloor(p) != 0) bad.add("a player about to be cast out was tracked on floor " + FloorMap.label(FloorTracker.currentFloor(p)));
            }
        } finally {
            AlignmentRules.reset(a);
            FloorTracker.forget(p.getUUID());
        }
        return bad.isEmpty() ? ok("no road, an open road and the own road keep the player; a lock to the " + other.id() + " road on a " + here.id() + " sector " + (armed ? "casts out and is not tracked" : "is left alone because the guard or the lock is off in the config") + " (decision only: a stand-in cannot be sent away)")
                : fail(firstFew(bad));
    }
}
