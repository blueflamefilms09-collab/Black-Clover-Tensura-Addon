package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * Why: nothing of the Dungeon Master can be played by its author, so its public entry points are exercised by the CI server smoke test against the
 * REAL generated sector 0, with a stand-in player: the tables and the roster resolve to live entity types (the part no unit test can check,
 * because it needs the registries of every installed mod), a forced roll puts monsters where the rules say (inside the sector, 24 to 48 blocks
 * from the party, out of its sight, never in a safe room, tagged, within the budget), a party in a safe room is not rolled for, noise raises a
 * player's meter and fades, a wandering boss walks the room graph without ever standing in a safe room, and the threat the dice read follows the floor
 * of the room the party stands in. Each test calls the same methods the event handlers and the admin command call, feeds time by hand (the monsters
 * are not ticked, so a walk is simulated by stepping the boss to the goal it was sent to), keeps to about a second or two, and removes everything it
 * made. They are registered with {@link SelfTests} and run by {@code /multiverse mega selftest}.
 */
final class EncSelfTests {
    private EncSelfTests() {}

    static void register() {
        SelfTests.register("enc.tables", EncSelfTests::tables);
        SelfTests.register("enc.roll", EncSelfTests::roll);
        SelfTests.register("enc.safe_zone", EncSelfTests::safeZone);
        SelfTests.register("enc.noise", EncSelfTests::noise);
        SelfTests.register("enc.wanderer", EncSelfTests::wanderer);
        SelfTests.register("enc.floor_threat", EncSelfTests::floorThreat);
    }

    // ---------------------------------------------------------------- helpers
    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    /** Loads the chunks within {@code reach} blocks of a point (a test world holds no players, so nothing keeps the sector loaded). */
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
        FakePlayer p = SelfTests.player(level, name, at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        EncounterDirector.forgetPlayer(p.getUUID());
        WandererDirector.forgetPlayer(p.getUUID());
        return p;
    }

    private static void cleanup(ServerLevel level, ServerPlayer p) {
        EncounterDirector.clear(level);
        WandererDirector.clear(level);
        NoiseEvents.BURSTS.clear();
        if (p != null) {
            EncounterDirector.forgetPlayer(p.getUUID());
            WandererDirector.forgetPlayer(p.getUUID());
        }
    }

    // ---------------------------------------------------------------- enc.tables
    private static SelfTests.Result tables(MinecraftServer server) {
        Set<String> names = EncData.tableNames();
        if (!names.contains("default")) return fail("the default encounter table did not load (loaded: " + names + ")");
        List<String> missing = new ArrayList<>(), broken = new ArrayList<>();
        for (RegionTheme t : RegionTheme.values()) if (!names.contains(t.id())) missing.add(t.id());
        if (!missing.isEmpty()) return fail("no encounter table for the themes " + missing);
        for (RegionTheme t : RegionTheme.values()) {
            for (Polarity pol : Polarity.values()) {
                String got = EncData.table(t, pol).id();
                if (!got.equals(t.id())) return fail("a " + t.id() + " sector on the " + pol.id() + " road reads the table '" + got + "' instead of its own");
            }
        }
        int entries = 0, dormantExtras = 0;
        Set<String> types = new TreeSet<>();
        Set<String> bands = new TreeSet<>();
        for (String name : names) {
            EncounterTable t = EncData.byName(name);
            if (t == null || t.entries().size() < 8) {
                broken.add(name + " has only " + (t == null ? 0 : t.entries().size()) + " entries");
                continue;
            }
            for (EncounterTable.Entry e : t.entries()) {
                entries++;
                bands.add(e.band().label());
                for (int i = 0; i < e.mobs().size(); i++) {
                    EncounterTable.MobSpec m = e.mobs().get(i);
                    boolean live;
                    String what;
                    if (m.kind() == EncounterTable.Kind.ENTITY) {
                        EntityType<?> type = Bestiary.type(m.entity());
                        live = Bestiary.usable(type);
                        what = m.entity();
                        if (live) types.add(m.entity());
                    } else {
                        String f = !m.faction().isEmpty() ? m.faction() : !e.faction().isEmpty() ? e.faction() : t.defaultFaction();
                        List<MobPicker.Candidate> members = f == null ? List.of() : Bestiary.members(f);
                        live = !members.isEmpty();
                        what = "faction " + f;
                        if (live) for (MobPicker.Candidate c : members) types.add(c.id());
                    }
                    if (!live) {
                        if (i == 0 && !fromMissingMod(m)) broken.add(name + "/" + e.id() + ": its first monster (" + what + ") does not resolve");
                        else dormantExtras++;
                    }
                }
            }
        }
        WandererRoster roster = EncData.roster();
        int kinds = 0;
        for (WandererRoster.Entry e : roster.entries()) {
            if (Bestiary.usable(Bestiary.type(e.entity()))) kinds++;
            else if (ModList.get().isLoaded(e.entity().substring(0, e.entity().indexOf(':')))) broken.add("wanderers.json: " + e.entity() + " is not a usable entity type");
        }
        if (kinds < 3) broken.add("only " + kinds + " usable kinds of wandering boss");
        if (!broken.isEmpty()) return fail(broken.size() + " problems: " + broken.subList(0, Math.min(10, broken.size())));
        return SelfTests.Result.ok(names.size() + " tables, " + entries + " entries (bands " + bands + "), " + types.size() + " entity types resolve, " + dormantExtras + " optional extras dormant, " + kinds + " wandering boss kinds");
    }

    /** An explicit monster of a mod that is not installed is dormant by design (a table may name optional mods), not a broken entry. */
    private static boolean fromMissingMod(EncounterTable.MobSpec m) {
        if (m.kind() != EncounterTable.Kind.ENTITY) return false;
        int colon = m.entity().indexOf(':');
        String ns = colon < 0 ? "" : m.entity().substring(0, colon);
        return !ns.isEmpty() && !ns.equals("minecraft") && !ns.equals("nusmp") && !ModList.get().isLoaded(ns);
    }

    // ---------------------------------------------------------------- enc.roll
    private static SelfTests.Result roll(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("no mega dimension");
        Sector s = MegaDungeon.data(server).sector(0);
        if (s == null || s.state == Sector.State.GENERATING || s.state == Sector.State.FAILED) return fail("sector 0 is not ready (run /multiverse mega expand first)");
        List<Room> rooms = ordinaryRooms(s);
        if (rooms.isEmpty()) return fail("sector 0 has no ordinary room to stand in");
        EncounterDirector.Outcome last = null;
        int tried = 0;
        for (Room room : rooms) {
            if (tried++ >= 8) break;
            BlockPos at = room.center();
            loadAround(mega, at, 72);
            FakePlayer p = stand(mega, "enc_roll", at);
            try {
                cleanup(mega, p);
                last = EncounterDirector.roll(mega, List.of(p), EncounterDirector.Force.band(Band.STANDARD).peacefulOk());
                if (!last.spawned()) continue;
                return verifyRoll(mega, s, p, last);
            } finally {
                cleanup(mega, p);
            }
        }
        return fail("no forced roll could place a monster in " + Math.min(tried, 8) + " rooms; the last said: " + (last == null ? "nothing" : last.text()));
    }

    private static SelfTests.Result verifyRoll(ServerLevel mega, Sector s, FakePlayer p, EncounterDirector.Outcome o) {
        if (o.mobs().isEmpty()) return fail("a spawn with no monsters");
        Vec3 eye = p.getEyePosition();
        List<SpawnPicker.Eye> eyes = List.of(new SpawnPicker.Eye(eye.x, eye.y, eye.z));
        LevelSight sight = new LevelSight(mega, p);
        double nearest = Double.MAX_VALUE, farthest = 0.0;
        Set<String> kinds = new TreeSet<>();
        for (Mob m : o.mobs()) {
            BlockPos bp = m.blockPosition();
            String who = m.getType().toString() + " at " + bp.getX() + " " + bp.getY() + " " + bp.getZ();
            if (!m.isAlive()) return fail(who + " is not alive");
            if (m.level() != mega) return fail(who + " is in the wrong level");
            if (!s.containsRegion(bp)) return fail(who + " is outside sector 0");
            Room r = s.roomAt(bp);
            if (r != null && Sanctuary.is(r.kind)) return fail(who + " stands in a " + r.kind.name().toLowerCase(Locale.ROOT) + " room");
            if (!m.getTags().contains(EncounterSpawner.TAG)) return fail(who + " lacks the " + EncounterSpawner.TAG + " tag");
            if (!o.entry().equals(m.getPersistentData().getString(EncounterSpawner.ENTRY))) return fail(who + " does not name its table entry");
            double near = SpawnPicker.nearest(new SpawnPicker.Cell(bp.getX(), bp.getY(), bp.getZ(), 1.0, 0, false), eyes);
            if (near < o.min() - 0.01 || near > o.max() + 3.0) return fail(who + " is " + String.format(Locale.ROOT, "%.1f", near) + " blocks from the party, outside " + o.min() + "-" + o.max());
            if (sight.sees(eye.x, eye.y, eye.z, bp.getX(), bp.getY(), bp.getZ())) return fail(who + " is in sight of the party");
            nearest = Math.min(nearest, near);
            farthest = Math.max(farthest, near);
            kinds.add(Bestiary.id(m.getType()));
        }
        if (o.spent() > o.budget() + 1e-6) return fail("spent " + o.spent() + " of a budget of " + o.budget());
        if (o.mobs().size() > EncConfig.get(EncConfig.MAX_PER_ENCOUNTER)) return fail(o.mobs().size() + " monsters, more than the cap");
        if (EncounterDirector.tracked() < o.mobs().size()) return fail("the Director tracks " + EncounterDirector.tracked() + " of " + o.mobs().size() + " monsters");
        return SelfTests.Result.ok("entry " + o.entry() + " (" + o.band().label() + "): " + o.mobs().size() + " x " + kinds + ", spent " + String.format(Locale.ROOT, "%.1f of %.1f", o.spent(), o.budget())
                + ", " + String.format(Locale.ROOT, "%.0f-%.0f", nearest, farthest) + " blocks away, none in sight, none in a safe room");
    }

    // ---------------------------------------------------------------- enc.safe_zone
    private static SelfTests.Result safeZone(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("no mega dimension");
        Sector s = MegaDungeon.data(server).sector(0);
        if (s == null || s.state == Sector.State.GENERATING || s.state == Sector.State.FAILED) return fail("sector 0 is not ready (run /multiverse mega expand first)");
        Room safe = null;
        for (Room r : s.rooms) if (r.kind == RoomKind.LANDING) safe = r;
        if (safe == null) for (Room r : s.rooms) if (r.kind.isSafe()) safe = r;
        if (safe == null) return fail("sector 0 has no Landing or safe room");
        List<Room> rooms = ordinaryRooms(s);
        if (rooms.isEmpty()) return fail("sector 0 has no ordinary room");
        FakePlayer inside = stand(mega, "enc_safe", safe.center());
        try {
            cleanup(mega, inside);
            loadAround(mega, safe.center(), 72);
            int before = EncounterDirector.tracked();
            for (int i = 0; i < 20; i++) {
                EncounterDirector.Outcome o = EncounterDirector.roll(mega, List.of(inside), null);
                if (o.kind() != EncounterDirector.Kind.SAFE_ZONE) return fail("roll " + i + " in the " + safe.kind.name().toLowerCase(Locale.ROOT) + " said " + o.kind() + ": " + o.text());
            }
            if (EncounterDirector.tracked() != before) return fail("monsters appeared while the party rested");
            // the control: the same player in an ordinary room is rolled for, so the refusal above was the safe zone and nothing else
            BlockPos away = rooms.get(0).center();
            loadAround(mega, away, 72);
            inside.setPos(away.getX() + 0.5, away.getY(), away.getZ() + 0.5);
            EncounterDirector.Outcome control = EncounterDirector.roll(mega, List.of(inside), null);
            if (control.kind() == EncounterDirector.Kind.SAFE_ZONE || control.kind() == EncounterDirector.Kind.OUTSIDE || control.kind() == EncounterDirector.Kind.NO_PARTY)
                return fail("the control roll in an ordinary room still said " + control.kind() + ": " + control.text());
            EncounterDirector.clear(mega);
            // no safe cell is ever picked, on the real markers and rooms of the sector
            Random rnd = new Random(7);
            List<SpawnPicker.Cell> cells = CellSource.candidates(s, safe.center().getX(), safe.center().getZ(), 400.0, rnd);
            int flagged = 0;
            for (SpawnPicker.Cell c : cells) {
                Room r = c.roomId() < 0 ? null : s.room(c.roomId());
                if (r != null && Sanctuary.is(r.kind) && !c.safe()) return fail("a cell in the " + r.kind + " room is not marked safe");
                if (c.safe()) flagged++;
            }
            List<SpawnPicker.Cell> picked = SpawnPicker.pick(cells, List.of(new SpawnPicker.Eye(safe.center().getX() + 0.5, safe.center().getY() + 0.6, safe.center().getZ() + 0.5)),
                    new SpawnPicker.Params(1.0, 400.0, 1.0, 0, 200), null, rnd);
            for (SpawnPicker.Cell c : picked) {
                Room r = s.roomAt(new BlockPos(c.x(), c.y(), c.z()));
                if (r != null && Sanctuary.is(r.kind)) return fail("the picker chose a cell in a " + r.kind + " room");
            }
            return SelfTests.Result.ok("20 rolls in the " + safe.kind.name().toLowerCase(Locale.ROOT) + " refused; the control said " + control.kind() + "; " + cells.size() + " candidate cells, " + flagged + " in safe rooms, "
                    + picked.size() + " picked, none safe");
        } finally {
            cleanup(mega, inside);
        }
    }

    // ---------------------------------------------------------------- enc.noise
    private record EventCase(Holder<GameEvent> event, NoiseKind kind) {}

    private static SelfTests.Result noise(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("no mega dimension");
        Sector s = MegaDungeon.data(server).sector(0);
        BlockPos at = s == null || s.rooms.isEmpty() ? new BlockPos(0, 65, 0) : s.rooms.get(0).center();
        FakePlayer p = stand(mega, "enc_noise", at);
        try {
            NoiseEvents.forget(p.getUUID());
            NoiseEvents.BURSTS.clear();
            long now = mega.getGameTime();
            // the real registry names map to the right kinds
            List<EventCase> expect = List.of(new EventCase(GameEvent.STEP, NoiseKind.STEP_WALK), new EventCase(GameEvent.EXPLODE, NoiseKind.EXPLOSION), new EventCase(GameEvent.BLOCK_DESTROY, NoiseKind.BLOCK),
                    new EventCase(GameEvent.BLOCK_PLACE, NoiseKind.BLOCK), new EventCase(GameEvent.BLOCK_OPEN, NoiseKind.DOOR), new EventCase(GameEvent.PROJECTILE_LAND, NoiseKind.IMPACT),
                    new EventCase(GameEvent.HIT_GROUND, NoiseKind.LAND), new EventCase(GameEvent.ENTITY_DAMAGE, NoiseKind.SILENT), new EventCase(GameEvent.EAT, NoiseKind.SILENT));
            for (EventCase c : expect) {
                NoiseKind k = NoiseEvents.kindOf(c.event());
                if (k != c.kind()) return fail("game event " + c.event().unwrapKey().map(x -> x.location().toString()).orElse("?") + " mapped to " + k + ", expected " + c.kind());
            }
            double start = NoiseEvents.reading(p.getUUID(), now);
            if (start != 0.0) return fail("a fresh player already has noise " + start);
            // a blast at the player raises the meter by its full amount; a step of someone who is not a player adds nothing
            Vec3 where = p.position();
            if (NoiseEvents.handle(mega, GameEvent.STEP, null, where, List.of(p), now) != NoiseKind.SILENT) return fail("a footstep of no player counted");
            NoiseKind kind = NoiseEvents.handle(mega, GameEvent.EXPLODE, null, where, List.of(p), now);
            double after = NoiseEvents.reading(p.getUUID(), now);
            if (kind != NoiseKind.EXPLOSION || after < 5.9 * EncConfig.get(EncConfig.NOISE_SCALE)) return fail("a blast at the player gave " + kind + " and a reading of " + after);
            if (NoiseEvents.BURSTS.size() < 1) return fail("the blast was not remembered for the bosses");
            if (NoiseMeter.mod(after, 4) < 2 && EncConfig.get(EncConfig.NOISE_SCALE) >= 1.0) return fail("a blast reads as a modifier of " + NoiseMeter.mod(after, 4));
            // sprinting steps count as sprint steps and add to the level
            p.setSprinting(true);
            NoiseKind step = NoiseEvents.handle(mega, GameEvent.STEP, p, where, List.of(p), now);
            p.setSprinting(false);
            if (step != NoiseKind.STEP_SPRINT) return fail("a sprinting step counted as " + step);
            double stepped = NoiseEvents.reading(p.getUUID(), now);
            if (!(stepped > after)) return fail("a sprinting step did not raise the level (" + after + " -> " + stepped + ")");
            // it fades: ten seconds later well under half, a minute later nothing
            double later = NoiseEvents.reading(p.getUUID(), now + 200), gone = NoiseEvents.reading(p.getUUID(), now + 1200);
            if (!(later < stepped * 0.2) || gone != 0.0) return fail("the level did not fade: " + stepped + " -> " + later + " after 10 s -> " + gone + " after a minute");
            // far away it is quieter: a blast thirty blocks off adds less than one at the player
            NoiseEvents.forget(p.getUUID());
            NoiseEvents.handle(mega, GameEvent.EXPLODE, null, where.add(30, 0, 0), List.of(p), now);
            double far = NoiseEvents.reading(p.getUUID(), now);
            if (!(far > 0.0 && far < after * 0.5)) return fail("a blast 30 blocks away read " + far + " against " + after + " at the player");
            return SelfTests.Result.ok("game events map to kinds; a blast reads " + String.format(Locale.ROOT, "%.1f", after) + " (+" + NoiseMeter.mod(after, 4) + " on the d20), 30 blocks off " + String.format(Locale.ROOT, "%.1f", far)
                    + ", fades to " + String.format(Locale.ROOT, "%.2f", later) + " in 10 s and 0 in a minute");
        } finally {
            NoiseEvents.forget(p.getUUID());
            NoiseEvents.BURSTS.clear();
        }
    }

    // ---------------------------------------------------------------- enc.floor_threat
    private static SelfTests.Result floorThreat(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("no mega dimension");
        Sector s = MegaDungeon.data(server).sector(0);
        if (s == null || s.state == Sector.State.GENERATING || s.state == Sector.State.FAILED || s.rooms.isEmpty()) return fail("sector 0 is not ready (run /multiverse mega expand first)");
        int first = s.firstFloor(), last = s.lastFloor();
        int lo = Math.min(Math.abs(first), Math.abs(last)), hi = Math.max(Math.abs(first), Math.abs(last));
        Set<Integer> floors = new TreeSet<>();
        Room landing = null, deepest = null;
        int deepestFloor = 0;
        for (Room r : s.rooms) {
            int floor = MegaDungeon.floorAt(mega, r.center());
            String who = "room " + r.id + " (" + r.kind.name().toLowerCase(Locale.ROOT) + ")";
            if (floor == 0) return fail(who + " answers floor 0 inside its own sector");
            if (Polarity.ofFloor(floor) != s.polarity) return fail(who + " is floor " + floor + ", which is not on the " + s.polarity.id() + " road of its sector");
            if (Math.abs(floor) < lo || Math.abs(floor) > hi) return fail(who + " is floor " + floor + ", outside the sector's floors " + first + " to " + last);
            if (ThreatInput.of(s.threat, floor, true) != Math.abs(floor)) return fail(who + ": the threat of floor " + floor + " is " + ThreatInput.of(s.threat, floor, true));
            floors.add(floor);
            if (r.kind == RoomKind.LANDING || r.kind == RoomKind.STAIRWELL) landing = r;
            if (deepest == null || Math.abs(floor) > Math.abs(deepestFloor)) {
                deepest = r;
                deepestFloor = floor;
            }
        }
        if (landing == null) return fail("sector 0 has no Landing");
        int landingFloor = MegaDungeon.floorAt(mega, landing.center());
        if (landingFloor != first) return fail("the Landing is floor " + landingFloor + " but the sector begins at floor " + first);
        boolean byFloor = EncConfig.get(EncConfig.FLOOR_THREAT);
        FakePlayer near = stand(mega, "enc_floor_a", landing.center());
        FakePlayer far = stand(mega, "enc_floor_b", deepest.center());
        try {
            int alone = FloorThreat.of(mega, s, List.of(near)), pair = FloorThreat.of(mega, s, List.of(near, far));
            int wantAlone = byFloor ? Math.abs(first) : s.threat, wantPair = byFloor ? Math.abs(deepestFloor) : s.threat;
            if (alone != wantAlone) return fail("a player in the Landing reads threat " + alone + ", expected " + wantAlone);
            if (pair != wantPair) return fail("a party with a member on floor " + deepestFloor + " reads threat " + pair + ", expected " + wantPair);
            EncounterTable table = EncData.table(s.theme, s.polarity);
            if (!table.id().equals(s.theme.id())) return fail("the " + s.theme.id() + " sector reads the table '" + table.id() + "'");
            return SelfTests.Result.ok("sector 0 (" + s.polarity.id() + ", " + s.theme.id() + ", sector threat " + s.threat + ") has " + floors.size() + " different floors between " + first + " and " + last
                    + "; the Landing reads threat " + alone + ", a party reaching floor " + deepestFloor + " reads " + pair + (byFloor ? " (from the floor)" : " (the floor rule is off)"));
        } finally {
            cleanup(mega, near);
            cleanup(mega, far);
        }
    }

    // ---------------------------------------------------------------- enc.wanderer
    private static SelfTests.Result wanderer(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("no mega dimension");
        Sector s = MegaDungeon.data(server).sector(0);
        if (s == null || s.state == Sector.State.GENERATING || s.state == Sector.State.FAILED) return fail("sector 0 is not ready (run /multiverse mega expand first)");
        PatrolGraph graph = WandererDirector.graphOf(s);
        if (graph.size() < 3) return fail("sector 0 has only " + graph.size() + " rooms a wandering boss may enter");
        Room safe = null;
        for (Room r : s.rooms) if (r.kind == RoomKind.LANDING) safe = r;
        if (safe == null) for (Room r : s.rooms) if (r.kind.isSafe()) safe = r;
        if (safe == null) return fail("sector 0 has no safe room for the party to stand in");
        // the boss is stood on every goal it is sent to, and a boss moved into a chunk that is not loaded may leave the level's entity lookup, so the whole sector is loaded first
        int loaded = 0;
        for (Room r : s.rooms) {
            if (loaded++ >= 200) break;
            loadAround(mega, r.center(), 20);
        }
        loadAround(mega, safe.center(), 72);
        FakePlayer p = stand(mega, "enc_wanderer", safe.center());
        try {
            cleanup(mega, p);
            WandererDirector.Spawned sp = WandererDirector.spawn(mega, s, List.of(p), new Random(12345));
            if (sp.mob() == null) return fail(sp.text());
            Mob boss = sp.mob();
            if (!boss.getTags().contains(WandererDirector.TAG)) return fail("the boss lacks the " + WandererDirector.TAG + " tag");
            // the boss walks the graph: every pass it is sent to a goal and the test stands it there, as a walk that worked would
            Set<Integer> rooms = new HashSet<>();
            long now = mega.getGameTime();
            int passes = 0, moves = 0;
            for (int i = 0; i < 200; i++, passes++) {
                WandererMind.Order o = WandererDirector.think(mega, boss.getUUID(), List.of(p), now + i * 10L);
                if (o == null) return fail("pass " + i + ": the boss gave no order (it was removed or has nobody to think about)");
                if (o.state() != WandererRules.State.PATROL) return fail("pass " + i + ": the boss left its patrol (" + o.state() + ") although the party is in a safe room");
                if (o.move()) {
                    loadAround(mega, BlockPos.containing(o.gx(), o.gy(), o.gz()), 8);
                    boss.teleportTo(o.gx(), o.gy(), o.gz());
                    moves++;
                }
                Room here = s.roomAt(boss.blockPosition());
                if (here != null && Sanctuary.is(here.kind)) return fail("pass " + i + ": the boss stands in a " + here.kind.name().toLowerCase(Locale.ROOT) + " room");
                if (!s.containsRegion(boss.blockPosition())) return fail("pass " + i + ": the boss left the sector");
                if (here != null) rooms.add(here.id);
            }
            if (rooms.size() < 2) return fail("the boss visited only " + rooms.size() + " rooms in " + passes + " passes");
            if (moves < 100) return fail("the boss was sent to walk only " + moves + " times in " + passes + " passes");
            // a player in plain sight next to it is noticed and hunted; one who goes to the safe room is let go
            String hunted = "not tried (no clear spot beside the boss)";
            for (int[] d : new int[][] {{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {3, 0}, {-3, 0}}) {
                p.setPos(boss.getX() + d[0], boss.getY(), boss.getZ() + d[1]);
                if (!boss.hasLineOfSight(p)) continue;
                WandererMind.Order seen = WandererDirector.think(mega, boss.getUUID(), List.of(p), now + 3000);
                if (seen == null || seen.state() != WandererRules.State.CHASE || !p.getUUID().equals(seen.chase())) return fail("a player two blocks away in plain sight was not hunted: " + (seen == null ? "no order" : seen.state() + " " + seen.event()));
                p.setPos(safe.center().getX() + 0.5, safe.center().getY(), safe.center().getZ() + 0.5);
                WandererMind.Order gone = WandererDirector.think(mega, boss.getUUID(), List.of(p), now + 3010);
                if (gone == null || gone.event() != WandererMind.Event.REFUGE || gone.state() != WandererRules.State.SEARCH) return fail("the player reached a safe room but the chase did not end: " + (gone == null ? "no order" : gone.state() + " " + gone.event()));
                if (boss.getTarget() != null) return fail("the boss kept a target after the player reached a safe room");
                hunted = "noticed at 2 blocks, let go at the safe room";
                break;
            }
            for (int i = 0; i < 40; i++) {
                WandererMind.Order o = WandererDirector.think(mega, boss.getUUID(), List.of(p), now + 4000 + i * 10L);
                if (o == null) break;
                if (o.move()) {
                    loadAround(mega, BlockPos.containing(o.gx(), o.gy(), o.gz()), 8);
                    boss.teleportTo(o.gx(), o.gy(), o.gz());
                }
                Room here = s.roomAt(boss.blockPosition());
                if (here != null && Sanctuary.is(here.kind)) return fail("after the chase the boss stands in a " + here.kind.name().toLowerCase(Locale.ROOT) + " room");
            }
            return SelfTests.Result.ok(sp.text() + "; " + passes + " passes, " + rooms.size() + " rooms visited, never in a safe room, never out of the sector; " + hunted);
        } finally {
            cleanup(mega, p);
        }
    }
}
