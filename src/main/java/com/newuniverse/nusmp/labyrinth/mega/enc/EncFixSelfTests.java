package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Why: the owner's report was "the Dungeon Master's encounters do not trigger, and when they do they spawn badly", and the older self-tests could not have
 * caught either: they force the band (so the dice and the cooldown never run), they never ask whether the tick handler runs, and they check a spawn against the
 * rules of the picker but not against the ground. These two run the Director the way the game does. {@code enc.trigger}: with the default configuration, on the
 * generated sector, a stand-in party that does nothing but stand in an ordinary room is met by an encounter within a bounded number of passes (the dice, the
 * pity timer, the entry choice, the place search and the spawn all take part), the tick handler has been running, and the very next pass is refused by the
 * cooldown. {@code enc.spawn}: the monsters of forced encounters in several rooms really stand on solid ground in loaded chunks, in a body-sized gap, in no fluid,
 * out of sight (unless the rung that found the place is the one that allows it, which the test then holds to its distance), in the sector, in no safe room, and
 * when the ring is made impossible the search still finds a place. Both use a stand-in that answers its true position, remove what they made and never rely on
 * a player being online.
 */
final class EncFixSelfTests {
    private EncFixSelfTests() {}

    /** The passes one room gets in {@code enc.trigger}: at the default roll interval of 100 ticks that is 2000 ticks, 100 seconds, of standing still. */
    private static final int PASSES = 20, ROOMS = 6;

    static void register() {
        SelfTests.register("enc.trigger", EncFixSelfTests::trigger);
        SelfTests.register("enc.spawn", EncFixSelfTests::spawn);
    }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static void loadAround(ServerLevel level, BlockPos c, int reach) {
        for (int cx = (c.getX() - reach) >> 4; cx <= (c.getX() + reach) >> 4; cx++)
            for (int cz = (c.getZ() - reach) >> 4; cz <= (c.getZ() + reach) >> 4; cz++) level.getChunk(cx, cz);
    }

    /** Ordinary rooms (not a rest room, a boss room or a corridor), the most central first. */
    private static List<Room> ordinaryRooms(Sector s) {
        List<Room> out = new ArrayList<>();
        for (Room r : s.rooms) if (!Sanctuary.is(r.kind) && !r.kind.isCirculation()) out.add(r);
        double mx = (s.bounds.minX() + s.bounds.maxX()) * 0.5, mz = (s.bounds.minZ() + s.bounds.maxZ()) * 0.5;
        out.sort(Comparator.comparingDouble((Room r) -> {
            BlockPos c = r.center();
            return (c.getX() - mx) * (c.getX() - mx) + (c.getZ() - mz) * (c.getZ() - mz);
        }));
        return out;
    }

    private static FakePlayer stand(ServerLevel level, String name, BlockPos at) {
        FakePlayer p = SelfTests.standIn(level, name, at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        EncounterDirector.forgetPlayer(p.getUUID());
        WandererDirector.forgetPlayer(p.getUUID());
        return p;
    }

    private static void cleanup(ServerLevel level, FakePlayer p) {
        EncounterDirector.clear(level);
        if (p != null) {
            EncounterDirector.forgetPlayer(p.getUUID());
            WandererDirector.forgetPlayer(p.getUUID());
        }
    }

    // ---------------------------------------------------------------- enc.trigger
    private static SelfTests.Result trigger(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("no mega dimension");
        if (!EncConfig.get(EncConfig.ENABLED)) return fail("enc.enabled is false in this world's config: the Dungeon Master is switched off");
        // 1. the handler is wired: the registered listener has run, recently
        long now = mega.getGameTime();
        if (EncounterDirector.ticksSeen() == 0L) return fail("the Director's tick handler has never run in this session (is EncRegistry.register called, is the Mega-Dungeon loaded?)");
        if (now - EncounterDirector.lastTickAt() > 200L) return fail("the Director's tick handler last ran " + (now - EncounterDirector.lastTickAt()) + " ticks ago");
        if (mega.getDifficulty() == Difficulty.PEACEFUL) return SelfTests.Result.ok("skipped the spawn: the world is Peaceful (the handler has run " + EncounterDirector.ticksSeen() + " ticks)");
        Sector s = MegaDungeon.data(server).sector(0);
        if (s == null || s.state == Sector.State.GENERATING || s.state == Sector.State.FAILED) return fail("sector 0 is not ready (run /multiverse mega expand first)");
        List<Room> rooms = ordinaryRooms(s);
        if (rooms.isEmpty()) return fail("sector 0 has no ordinary room to stand in");
        Map<EncounterDirector.Kind, Integer> seen = new EnumMap<>(EncounterDirector.Kind.class);
        String lastText = "";
        int tried = 0;
        for (Room room : rooms) {
            if (tried++ >= ROOMS) break;
            BlockPos at = room.center();
            loadAround(mega, at, 80);
            FakePlayer p = stand(mega, "enc_trigger", at);
            try {
                cleanup(mega, p);
                Random rnd = new Random(0xC0FFEEL + tried);
                for (int pass = 1; pass <= PASSES; pass++) {
                    EncounterDirector.Outcome o = EncounterDirector.pass(mega, List.of(p), rnd);
                    seen.merge(o.kind(), 1, Integer::sum);
                    lastText = o.text();
                    if (!o.spawned()) continue;
                    if (o.mobs().isEmpty()) return fail("a spawn with no monsters");
                    for (Mob m : o.mobs()) if (!m.isAlive() || m.level() != mega) return fail("a monster of the encounter is not alive in the Mega-Dungeon");
                    // the cooldown arithmetic: the pass right after a spawn must be refused for lack of time, not roll again
                    EncounterDirector.Outcome again = EncounterDirector.pass(mega, List.of(p), rnd);
                    if (again.kind() != EncounterDirector.Kind.COOLDOWN) return fail("the pass after a spawn said " + again.kind() + " instead of COOLDOWN: " + again.text());
                    return SelfTests.Result.ok("a stand-in party was met on pass " + pass + " (" + pass * EncConfig.get(EncConfig.ROLL_EVERY) + " ticks) in room " + room.id + " of sector 0: " + o.text()
                            + "; the handler has run " + EncounterDirector.ticksSeen() + " ticks; the next pass: cooldown");
                }
            } finally {
                cleanup(mega, p);
            }
        }
        return fail("no encounter in " + PASSES + " passes in each of " + Math.min(tried, ROOMS) + " rooms with the default configuration; outcomes " + seen + "; the last said: " + lastText);
    }

    // ---------------------------------------------------------------- enc.spawn
    private static SelfTests.Result spawn(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("no mega dimension");
        Sector s = MegaDungeon.data(server).sector(0);
        if (s == null || s.state == Sector.State.GENERATING || s.state == Sector.State.FAILED) return fail("sector 0 is not ready (run /multiverse mega expand first)");
        List<Room> rooms = ordinaryRooms(s);
        if (rooms.isEmpty()) return fail("sector 0 has no ordinary room to stand in");
        int roomsWithSpawn = 0, monsters = 0, best = 0;
        List<String> misses = new ArrayList<>();
        int tried = 0;
        for (Room room : rooms) {
            if (tried++ >= 8) break;
            BlockPos at = room.center();
            loadAround(mega, at, 80);
            FakePlayer p = stand(mega, "enc_spawn", at);
            try {
                cleanup(mega, p);
                EncounterDirector.Outcome o = EncounterDirector.roll(mega, List.of(p), EncounterDirector.Force.band(Band.STANDARD).peacefulOk(), new Random(0xBEEFL + tried));
                if (!o.spawned()) {
                    misses.add("room " + room.id + ": " + o.text());
                    continue;
                }
                String bad = verify(mega, s, p, o);
                if (bad != null) return fail("room " + room.id + ": " + bad);
                roomsWithSpawn++;
                monsters += o.mobs().size();
                best = Math.max(best, o.mobs().size());
            } finally {
                cleanup(mega, p);
            }
        }
        if (roomsWithSpawn < Math.min(3, Math.min(tried, 8))) return fail("only " + roomsWithSpawn + " of " + Math.min(tried, 8) + " rooms got an encounter; " + misses.subList(0, Math.min(3, misses.size())));
        String ladder = ladder(mega, s, rooms.get(0));
        if (ladder.startsWith("FAIL ")) return fail(ladder.substring(5));
        return SelfTests.Result.ok(roomsWithSpawn + " of " + Math.min(tried, 8) + " rooms got an encounter, " + monsters + " monsters (most in one: " + best + "), every one on solid ground in a loaded chunk, in a clear gap, out of the water, out of sight, in the sector and in no safe room; " + ladder);
    }

    /** Null when every monster of the outcome is placed as the rules say, else what is wrong. */
    private static String verify(ServerLevel mega, Sector s, FakePlayer p, EncounterDirector.Outcome o) {
        if (o.mobs().isEmpty()) return "a spawn with no monsters";
        Vec3 eye = p.getEyePosition();
        LevelSight sight = new LevelSight(mega, p);
        for (Mob m : o.mobs()) {
            BlockPos bp = m.blockPosition();
            String who = m.getType().toString() + " at " + bp.getX() + " " + bp.getY() + " " + bp.getZ();
            if (!m.isAlive()) return who + " is not alive";
            if (m.level() != mega) return who + " is in the wrong level";
            if (!mega.hasChunkAt(bp) || !mega.getChunkSource().hasChunk(bp.getX() >> 4, bp.getZ() >> 4)) return who + " stands in a chunk that is not loaded";
            if (!s.containsRegion(bp)) return who + " is outside sector 0";
            Room r = s.roomAt(bp);
            if (r != null && Sanctuary.is(r.kind)) return who + " stands in a " + r.kind.name().toLowerCase(Locale.ROOT) + " room";
            BlockPos below = bp.below();
            if (mega.getBlockState(below).getCollisionShape(mega, below).isEmpty()) return who + " has no solid ground under its feet (" + mega.getBlockState(below).getBlock() + ")";
            if (!mega.getFluidState(bp).isEmpty() || !mega.getFluidState(bp.above()).isEmpty()) return who + " stands in a fluid";
            if (!mega.noCollision(m)) return who + " is inside a block or another body";
            double near = SpawnPicker.nearest(new SpawnPicker.Cell(bp.getX(), bp.getY(), bp.getZ(), 1.0, 0, false), List.of(new SpawnPicker.Eye(eye.x, eye.y, eye.z)));
            if (near < SpawnSearch.HARD_MIN - 0.01) return who + " is " + String.format(Locale.ROOT, "%.1f", near) + " blocks from the party, nearer than the hard minimum";
            if (near < o.min() - 0.01 || near > o.max() + 3.0) return who + " is " + String.format(Locale.ROOT, "%.1f", near) + " blocks from the party, outside " + o.min() + "-" + o.max();
            boolean seen = sight.sees(eye.x, eye.y, eye.z, bp.getX(), bp.getY(), bp.getZ());
            // only the last rung of the search (the ring doubled, "rung 'far and in view'") may accept a place in view, and then only three eighths of its far edge away or more
            boolean lastRung = o.text().contains("rung 'far and in view'");
            if (seen && !(lastRung && near >= 0.37 * o.max() - 0.5)) return who + " is in sight of the party at " + String.format(Locale.ROOT, "%.1f", near) + " blocks";
        }
        return null;
    }

    /** The search with a ring nothing can satisfy still finds a place (a rung further down the ladder). "FAIL ..." on failure. */
    private static String ladder(ServerLevel mega, Sector s, Room room) {
        BlockPos at = room.center();
        List<SpawnPicker.Eye> eyes = List.of(new SpawnPicker.Eye(at.getX() + 0.5, at.getY() + 0.62, at.getZ() + 0.5));
        FakePlayer p = stand(mega, "enc_spawn_ladder", at);
        try {
            SpawnSearch.Found tight = SpawnSearch.find(CellSource.layoutOf(s), eyes, 24.0, 48.0, 2, new LevelSight(mega, p), 48, true, 3, new Random(5));
            SpawnSearch.Found impossible = SpawnSearch.find(CellSource.layoutOf(s), eyes, 150.0, 160.0, 2, new LevelSight(mega, p), 48, true, 3, new Random(5));
            SpawnSearch.Found refused = SpawnSearch.find(CellSource.layoutOf(s), eyes, 150.0, 160.0, 2, new LevelSight(mega, p), 48, false, 3, new Random(5));
            if (!refused.cells().isEmpty()) return "FAIL a ring of 150-160 blocks found cells with the ladder switched off: " + refused.trace();
            if (!impossible.ok()) return "FAIL a ring nothing satisfies found no place on any rung: " + impossible.trace();
            return "the ring 24-48 is met on rung " + (tight.ok() ? tight.tier().index() : -1) + ", the impossible ring 150-160 on rung " + impossible.tier().index() + " ('" + impossible.tier().name() + "')";
        } finally {
            EncounterDirector.forgetPlayer(p.getUUID());
        }
    }
}
