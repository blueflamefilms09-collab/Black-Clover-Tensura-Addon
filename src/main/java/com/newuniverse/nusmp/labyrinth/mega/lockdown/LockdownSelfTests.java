package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.enc.EncounterDirector;
import com.newuniverse.nusmp.labyrinth.mega.enc.Sanctuary;
import com.newuniverse.nusmp.labyrinth.mega.lock.EscapeRules;
import com.newuniverse.nusmp.labyrinth.mega.lock.LockBook;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Why: nothing in the anti-speedrun layer can be played by its author, so the CI server smoke test runs {@code dungeon.lockdown} for real, on the sector the server
 * generated. It checks, in the running game: the shipped room data parses without a single problem and says what the doc says; the barrier block and its block entity are
 * registered; a stand-in party in a real challenge room of the generated dungeon makes the director lock it (the doorway cells really become barriers, the first of them a
 * master that is a lock of the escape rules, the wave stands inside the room, a listener of {@link RoomLockdownEvent} is called and can cancel it); the whole life of the
 * room is driven by hand: the wave dies, the room clears, every cell holds the block it held before, the cleared mark is saved and the verdict for the room is CLEARED; a
 * second lockdown is opened by the escape rules the way the release rune opens it, and the room is then NOT cleared and cools down; and the ambush director, given enough
 * heat for a stand-in in an ordinary room, places a squad AHEAD of it (alive, tagged, in the sector, in no safe room, at least the minimum distance away, not behind), is
 * then refused by its own cooldown, and is refused outright in a safe room. A fake player is not in the level's player list, so the passes of the directors are not what is
 * tested here but the decisions and the placements they make, called directly; and a Peaceful world, which removes monsters at once, skips the live parts. Everything it
 * makes is removed.
 */
public final class LockdownSelfTests {
    private LockdownSelfTests() {}

    public static void register() {
        SelfTests.register("dungeon.lockdown", LockdownSelfTests::run);
    }

    private static SelfTests.Result ok(String d) { return SelfTests.Result.ok(d); }

    private static SelfTests.Result fail(String d) { return SelfTests.Result.fail(d); }

    private static void loadAround(ServerLevel level, Room r) {
        for (int cx = (r.box.minX() - 16) >> 4; cx <= (r.box.maxX() + 16) >> 4; cx++)
            for (int cz = (r.box.minZ() - 16) >> 4; cz <= (r.box.maxZ() + 16) >> 4; cz++) level.getChunk(cx, cz);
    }

    private static SelfTests.Result run(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("no mega dimension");
        StringBuilder done = new StringBuilder();

        // ---- the shipped room data
        String text = readData(server, LockdownRooms.FILE);
        if (text == null) return fail(LockdownRooms.FILE + " is missing from the data pack");
        LockdownTable.Parsed parsed = LockdownTable.parse(LockdownRooms.FILE, text);
        if (!parsed.clean()) return fail("the room data has problems: " + parsed.problems());
        LockdownRule treasure = parsed.table().ruleFor("nusmp:mega/treasure/holy_church_reliquary", RoomKind.TREASURE);
        LockdownRule trial = parsed.table().ruleFor("nusmp:mega/trial/trial_hall_a", RoomKind.TRIAL);
        LockdownRule puzzle = parsed.table().ruleFor("nusmp:mega/puzzle/sigil_hall_a", RoomKind.PUZZLE);
        if (!treasure.lock()) return fail("a treasure room should lock");
        if (trial.lock() || puzzle.lock()) return fail("a trial hall and a puzzle room lock themselves and must not be locked twice");
        done.append("room data clean (").append(parsed.table().kinds().size()).append(" kinds, ").append(parsed.table().patterns().size()).append(" patterns); ");

        // ---- the block and the block entity
        if (LockdownRegistry.LOCKDOWN_BARRIER.get().defaultBlockState().isAir()) return fail("the Lockdown Barrier is not registered");
        if (LockdownRegistry.LOCKDOWN_BARRIER_BE.get() == null) return fail("the Lockdown Barrier's block entity type is not registered");
        done.append("block and block entity registered; ");

        // ---- the pure rules, as the doc states them
        AmbushRules.Config cfg = AmbushRules.Config.DEFAULT;
        AmbushRules.Facts hot = new AmbushRules.Facts(true, false, true, true, false, false, 0L, 0, 0, cfg.threshold());
        if (AmbushRules.decide(hot, cfg) != AmbushRules.Why.FIRE) return fail("a hot, free player in an ordinary room should be ambushed");
        if (AmbushRules.decide(new AmbushRules.Facts(true, false, true, true, true, false, 0L, 0, 0, cfg.threshold()), cfg) != AmbushRules.Why.SAFE) return fail("a safe room must never be ambushed");
        if (AmbushRules.decide(new AmbushRules.Facts(true, false, true, true, false, true, 0L, 0, 0, cfg.threshold()), cfg) != AmbushRules.Why.LOCKED_IN) return fail("a party locked in a room must not be ambushed on top");
        done.append("pure rules hold; ");

        if (mega.getDifficulty() == Difficulty.PEACEFUL) return ok(done + "the live parts were skipped: the world is Peaceful (monsters would vanish at once)");
        Sector sector = MegaDungeon.data(server).sector(0);
        if (sector == null || sector.state == Sector.State.GENERATING || sector.state == Sector.State.FAILED) return fail("sector 0 is not ready (run /multiverse mega expand first)");

        String lock = lockLife(mega, sector);
        if (lock.startsWith("FAIL ")) return fail(done + lock.substring(5));
        done.append(lock).append("; ");
        String amb = ambush(mega, sector);
        if (amb.startsWith("FAIL ")) return fail(done + amb.substring(5));
        done.append(amb);
        return ok(done.toString());
    }

    private static String readData(MinecraftServer server, String path) {
        try {
            var res = server.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("nusmp", path));
            if (res.isEmpty()) return null;
            try (InputStream in = res.get().open()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (java.io.IOException | RuntimeException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- the life of a locked room
    private static String lockLife(ServerLevel mega, Sector sector) {
        List<Room> candidates = new ArrayList<>();
        for (Room r : sector.rooms) {
            LockdownRule rule = LockdownRooms.ruleFor(r.template, r.kind);
            if (rule.lock() && !Sanctuary.is(r.kind) && LockdownMath.area(r.box.minX(), r.box.minZ(), r.box.maxX(), r.box.maxZ()) >= Math.max(81, rule.minArea())) candidates.add(r);
        }
        if (candidates.isEmpty()) return "FAIL sector 0 has no room that the room data locks";
        Room room = null;
        List<OpeningScan.Cell> cells = null;
        List<String> why = new ArrayList<>();
        for (Room r : candidates) {
            if (why.size() >= 14) break;
            loadAround(mega, r);
            if (LockdownDirector.ownSeal(mega, r)) {
                why.add(r.template + ": has a seal of its own");
                continue;
            }
            List<OpeningScan.Cell> found = LockdownDirector.scanOpenings(mega, r);
            OpeningScan.Result j = OpeningScan.judge(found, LockdownConfig.limits());
            if (j.ok()) {
                room = r;
                cells = found;
                break;
            }
            why.add(r.template + ": " + j.why());
        }
        if (room == null) return "FAIL none of " + candidates.size() + " candidate rooms of sector 0 has doorways the director can close: " + why;
        LockdownRule rule = LockdownRooms.ruleFor(room.template, room.kind).withWaves(1);
        BlockPos at = room.center();
        FakePlayer p = SelfTests.standIn(mega, "dungeon_lockdown", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        LockdownData data = LockdownData.get(mega.getServer());
        Map<BlockPos, BlockState> before = new HashMap<>();
        for (OpeningScan.Cell c : cells) {
            BlockPos bp = new BlockPos(c.x(), c.y(), c.z());
            before.put(bp, mega.getBlockState(bp));
        }
        List<RoomLockdownEvent> seen = new ArrayList<>();
        Consumer<RoomLockdownEvent> spy = seen::add;
        Consumer<RoomLockdownEvent> veto = e -> e.setCanceled(true);
        try {
            data.forget(sector.id, room.id);
            LockdownDirector.forgetRefusals();
            EncounterDirector.forgetPlayer(p.getUUID());

            // a listener can cancel the lock
            NeoForge.EVENT_BUS.addListener(veto);
            LockdownDirector.ArmResult vetoed;
            try {
                vetoed = LockdownDirector.arm(mega, sector, room, rule, List.of(p), mega.getGameTime());
            } finally {
                NeoForge.EVENT_BUS.unregister(veto);
            }
            if (vetoed.armed() || LockdownDirector.masterOf(sector.id, room.id) != null) return "FAIL a cancelled RoomLockdownEvent still locked room " + room.id;
            for (var e : before.entrySet()) if (!mega.getBlockState(e.getKey()).equals(e.getValue())) return "FAIL a cancelled lockdown changed the block at " + e.getKey().toShortString();
            if (LockdownDirector.verdict(mega, sector, room, List.of(p), mega.getGameTime()) == LockdownMath.Verdict.ARM) return "FAIL a cancelled lockdown would be asked again at once: it should cool down";

            // the real lock
            LockdownDirector.forgetRefusals();
            NeoForge.EVENT_BUS.addListener(spy);
            LockdownDirector.ArmResult armed;
            try {
                armed = LockdownDirector.arm(mega, sector, room, rule, List.of(p), mega.getGameTime());
            } finally {
                NeoForge.EVENT_BUS.unregister(spy);
            }
            if (!armed.armed()) return "FAIL room " + room.id + " (" + room.template + ") did not lock: " + armed.why();
            if (seen.size() != 1 || seen.get(0).getRoom() != room || seen.get(0).getSource() != RoomLockdownEvent.Source.DIRECTOR) return "FAIL RoomLockdownEvent was posted " + seen.size() + " time(s), not once for the room";
            BlockPos master = LockdownDirector.masterOf(sector.id, room.id);
            if (master == null || !(mega.getBlockEntity(master) instanceof LockdownBarrierBlockEntity be) || !be.isMaster()) return "FAIL the master barrier is missing";
            int barriers = 0;
            for (var e : before.entrySet()) if (mega.getBlockState(e.getKey()).is(LockdownRegistry.LOCKDOWN_BARRIER.get())) barriers++;
            if (barriers != before.size() || be.cellCount() != before.size()) return "FAIL " + barriers + " of " + before.size() + " doorway cells are barriers (the master holds " + be.cellCount() + ")";
            if (armed.mobs() < 1 || be.trackedMobs() < 1) return "FAIL the room locked with no wave";
            List<Mob> wave = taggedMobs(mega, room);
            if (wave.size() < 1) return "FAIL no monster carries the lockdown tag inside the room";
            for (Mob m : wave) if (!room.contains(m.blockPosition())) return "FAIL a wave monster stands outside the room at " + m.blockPosition().toShortString();
            be.tickNow(mega);
            if (com.newuniverse.nusmp.labyrinth.mega.lock.LockConfig.enabled() && !LockBook.isLive(be.lockKey())) return "FAIL the locked room is not in the escape rules' book";
            if (LockdownDirector.verdict(mega, sector, room, List.of(p), mega.getGameTime()) == LockdownMath.Verdict.ARM) return "FAIL a locked room would be locked again";

            // the wave dies, the room clears
            for (Mob m : wave) m.discard();
            for (int i = 0; i < TO_CLEAR; i++) {
                if (!(mega.getBlockEntity(master) instanceof LockdownBarrierBlockEntity live)) break;
                live.tickNow(mega);
            }
            for (var e : before.entrySet()) {
                if (!mega.getBlockState(e.getKey()).equals(e.getValue())) return "FAIL after the wave died the block at " + e.getKey().toShortString() + " is " + mega.getBlockState(e.getKey()).getBlock() + ", not what it was";
            }
            if (!data.isCleared(sector.id, room.id)) return "FAIL the cleared mark was not saved";
            if (LockBook.isLive("lockdown:" + master.asLong())) return "FAIL the cleared room is still in the escape rules' book";
            if (LockdownDirector.masterOf(sector.id, room.id) != null) return "FAIL the cleared room is still listed as locked";
            LockdownMath.Verdict again = LockdownDirector.verdict(mega, sector, room, List.of(p), mega.getGameTime());
            if (again == LockdownMath.Verdict.ARM) return "FAIL a cleared room would lock again";

            // an escape: the same room locked again, opened by the escape rules the way the rune opens it
            data.forget(sector.id, room.id);
            LockdownDirector.forgetRefusals();
            LockdownDirector.ArmResult second = LockdownDirector.arm(mega, sector, room, rule, List.of(p), mega.getGameTime());
            if (!second.armed()) return "FAIL the second lockdown of room " + room.id + " did not lock: " + second.why();
            BlockPos master2 = LockdownDirector.masterOf(sector.id, room.id);
            if (master2 == null || !(mega.getBlockEntity(master2) instanceof LockdownBarrierBlockEntity be2)) return "FAIL the second master is missing";
            be2.open(null, EscapeRules.Why.IDLE);
            for (var e : before.entrySet()) {
                if (!mega.getBlockState(e.getKey()).equals(e.getValue())) return "FAIL after an escape the block at " + e.getKey().toShortString() + " is " + mega.getBlockState(e.getKey()).getBlock();
            }
            if (data.isCleared(sector.id, room.id)) return "FAIL an escaped room was marked cleared";
            if (LockdownDirector.masterOf(sector.id, room.id) != null) return "FAIL an escaped room is still listed as locked";
            for (Mob m : taggedMobs(mega, room)) if (m.isAlive()) return "FAIL a wave monster survived the opening of its room";
            LockdownMath.Verdict cooling = LockdownDirector.verdict(mega, sector, room, List.of(p), mega.getGameTime());
            if (cooling == LockdownMath.Verdict.ARM) return "FAIL an escaped room would lock again at once";
            return "room " + room.id + " (" + room.template + ") locked " + before.size() + " doorway cells in " + armed.cells() + " barriers with a wave of " + armed.mobs() + ", a cancelling listener kept it open, the wave's death cleared it and every cell was given back, an escape through the rules opened it uncleared (verdict then " + cooling + ")";
        } finally {
            NeoForge.EVENT_BUS.unregister(spy);
            NeoForge.EVENT_BUS.unregister(veto);
            BlockPos m = LockdownDirector.masterOf(sector.id, room.id);
            if (m != null && mega.getBlockEntity(m) instanceof LockdownBarrierBlockEntity leftover) leftover.open(null, EscapeRules.Why.NONE);
            for (Mob mob : taggedMobs(mega, room)) mob.discard();
            data.forget(sector.id, room.id);
            LockdownDirector.forgetRefusals();
            EncounterDirector.clear(mega);
            EncounterDirector.forgetPlayer(p.getUUID());
            DynamicAmbushDirector.forget(p.getUUID());
        }
    }

    /** How many manual looks the cleared-room check gives the room: three to count the dead wave out, one to clear (the rest is slack). */
    private static final int TO_CLEAR = 6;

    private static List<Mob> taggedMobs(ServerLevel level, Room room) {
        List<Mob> out = new ArrayList<>();
        net.minecraft.world.phys.AABB bb = new net.minecraft.world.phys.AABB(room.box.minX(), room.box.minY(), room.box.minZ(), room.box.maxX() + 1.0, room.box.maxY() + 1.0, room.box.maxZ() + 1.0);
        for (Mob m : level.getEntitiesOfClass(Mob.class, bb, e -> e.getTags().contains(LockdownRegistry.MOB_TAG))) out.add(m);
        return out;
    }

    // ---------------------------------------------------------------- the ambush
    private static String ambush(ServerLevel mega, Sector sector) {
        Room from = null;
        for (Room r : sector.rooms) {
            if (Sanctuary.is(r.kind) || r.kind.isCirculation()) continue;
            from = r;
            if (r.kind == RoomKind.HALL) break;
        }
        if (from == null) return "FAIL sector 0 has no ordinary room to stand in";
        loadAround(mega, from);
        BlockPos at = from.center();
        FakePlayer p = SelfTests.standIn(mega, "dungeon_ambush", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        AmbushRules.Config cfg = AmbushRules.Config.DEFAULT;
        AmbushLedger ledger = new AmbushLedger();
        List<String> misses = new ArrayList<>();
        try {
            EncounterDirector.clear(mega);
            EncounterDirector.forgetPlayer(p.getUUID());
            long now = mega.getGameTime();
            ledger.add(AmbushRules.Signal.TELEPORT, 0, now, cfg);
            ledger.add(AmbushRules.Signal.ESCAPED_LOCKDOWN, 0, now + 40, cfg);
            if (ledger.heat(now + 40, cfg) < cfg.threshold()) return "FAIL a teleport and an escaped lockdown should reach the ambush threshold, heat " + ledger.heat(now + 40, cfg);
            now += 40;
            double[][] headings = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            DynamicAmbushDirector.Result fired = null;
            double[] way = null;
            for (double[] h : headings) {
                DynamicAmbushDirector.Result r = DynamicAmbushDirector.attempt(mega, p, ledger, cfg, now, h[0], h[1], false);
                if (r.fired()) {
                    fired = r;
                    way = h;
                    break;
                }
                misses.add((int) h[0] + "," + (int) h[1] + " " + r.why() + ": " + r.text());
                if (r.why() != AmbushRules.Why.NO_CELLS) break;
            }
            if (fired == null) return "FAIL no ambush was placed ahead of a hot player in room " + from.id + " (" + from.template + "): " + misses;
            List<Mob> squad = new ArrayList<>();
            for (Entity e : mega.getAllEntities()) if (e instanceof Mob m && m.getTags().contains(LockdownRegistry.AMBUSH_TAG)) squad.add(m);
            if (squad.size() != fired.mobs() || squad.isEmpty()) return "FAIL the ambush says " + fired.mobs() + " monsters, the world holds " + squad.size();
            double min = LockdownConfig.get(LockdownConfig.AMBUSH_MIN_DIST);
            for (Mob m : squad) {
                BlockPos bp = m.blockPosition();
                String who = m.getType() + " at " + bp.toShortString();
                if (!m.isAlive() || m.level() != mega) return "FAIL " + who + " is not alive in the Mega-Dungeon";
                Room in = sector.roomAt(bp);
                if (in != null && Sanctuary.is(in.kind)) return "FAIL " + who + " stands in a " + in.kind.name().toLowerCase(Locale.ROOT) + " room";
                double dx = m.getX() - p.getX(), dz = m.getZ() - p.getZ(), d = Math.hypot(dx, dz);
                if (d < min - 1.0) return "FAIL " + who + " is only " + String.format(Locale.ROOT, "%.1f", d) + " blocks away (minimum " + min + ")";
                if ((dx * way[0] + dz * way[1]) / Math.max(d, 1.0e-6) < -0.5) return "FAIL " + who + " is behind the player (heading " + (int) way[0] + "," + (int) way[1] + ")";
            }
            DynamicAmbushDirector.Result second = DynamicAmbushDirector.attempt(mega, p, ledger, cfg, now + 20, way[0], way[1], false);
            if (second.fired()) return "FAIL a second ambush fired at once, inside its cooldown and with its heat spent";
            // a safe room is never ambushed
            Room safe = null;
            for (Room r : sector.rooms) if (r.kind.isSafe()) { safe = r; break; }
            String safeNote = "no safe room in sector 0 to test";
            if (safe != null) {
                loadAround(mega, safe);
                BlockPos sp = safe.center();
                FakePlayer q = SelfTests.standIn(mega, "dungeon_ambush_safe", sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5);
                AmbushLedger hot = new AmbushLedger();
                hot.add(AmbushRules.Signal.TELEPORT, 0, now, cfg);
                hot.add(AmbushRules.Signal.ESCAPED_LOCKDOWN, 0, now + 40, cfg);
                DynamicAmbushDirector.Result refused = DynamicAmbushDirector.attempt(mega, q, hot, cfg, now + 40, 1.0, 0.0, true);
                if (refused.fired() || refused.why() != AmbushRules.Why.SAFE) return "FAIL a safe room was not refused: " + refused.why();
                safeNote = "a safe room refused it";
            }
            return "a hot stand-in in room " + from.id + " (" + from.template + ") met a " + fired.band().label() + " squad of " + squad.size() + " placed ahead, alive, tagged, in no safe room and " + (int) min + " blocks or more away; the next attempt right after was refused; " + safeNote;
        } finally {
            List<Mob> left = new ArrayList<>();
            for (Entity e : mega.getAllEntities()) if (e instanceof Mob m && m.getTags().contains(LockdownRegistry.AMBUSH_TAG)) left.add(m);
            for (Mob m : left) m.discard();
            EncounterDirector.clear(mega);
            EncounterDirector.forgetPlayer(p.getUUID());
            DynamicAmbushDirector.forget(p.getUUID());
        }
    }
}
