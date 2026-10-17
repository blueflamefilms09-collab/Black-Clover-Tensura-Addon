package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaConfig;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.floors.FloorsConfig;
import com.newuniverse.nusmp.labyrinth.mega.party.PartyKey;
import com.newuniverse.nusmp.labyrinth.mega.party.PartyService;
import com.newuniverse.nusmp.labyrinth.mega.travel.MegaTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing the generator does can be watched by its author, so the CI smoke test runs these eight checks in the real game ({@code /multiverse mega
 * selftest core.*}) after it has made the dungeon grow with {@code /multiverse mega expand}. They are cheap (each well under two seconds, nothing is placed)
 * and each answers one question a person would otherwise have to find out by walking: {@code core.assemble} do the real pools and templates make sectors
 * that pass the audit (two on each road's lane); {@code core.determinism} does the same seed always give the same plan, and does the saved first sector of each road
 * equal the plan its seed gives now (so regeneration and the restart repair are safe); {@code core.geometry} does the game agree with the plan's rotation maths and with where the
 * blocks really stand ({@link GeometryCheck}); {@code core.seal} is each Way Onward really sealed, or really open with a tunnel floor behind it once the next sector
 * exists; {@code core.travel} does a player arrive on the right Landing and is the deepest Landing remembered; {@code core.expand} is the expansion machinery
 * registered, does it refuse what it must, and would the sector behind each road's newest free Way Onward plan and audit cleanly (a dry run: the real request,
 * the real solver, nothing saved). Since 1.55.0 the dungeon is two roads, so two more checks look at them: {@code core.roads} does each road's first sector stand
 * on its own lane (z 0 and {@code MegaGrid.LANE_Z}), are the saved chains whole (chain indices 0..n-1 in order, floors continuing from one sector to the next, the
 * threat of the floors, one length for the whole road, no region of one road near the other), and does a road that exists refuse a second first sector;
 * {@code core.chain_travel} does {@code MegaDungeon.enterChain} (the generator's {@code ChainTravel}) put a player on the first Landing of each road that exists,
 * and does the memory of the deepest Landing stay on its own road. The checks that need a built sector say so in their answer when there is none, instead of
 * passing silently.
 *
 * <p>The floor model (1.58.0, one floor per sector, joined by stairs): {@code core.assemble} probes floor 1 (two seeds) and the first boss floor of each road, with the stairs
 * in the fill; {@code core.seal} only asks a boss floor's seal to be whole until the next floor exists and gone after it (the grand stairwell stands behind it, there is no
 * tunnel); {@code core.expand} dry-runs the floor after each road's newest floor ({@link ExpansionQueue#draftFloor}: its slot, its decade theme, its plan, its audit with
 * the stairs); {@code core.roads} also checks that every floor stands in the slot of its place and that two floors of a road keep apart. A road saved with ten floors per
 * sector keeps being checked the old way.
 */
final class CoreSelfTests {
    private CoreSelfTests() {}

    static void register() {
        SelfTests.register("core.assemble", CoreSelfTests::assemble);
        SelfTests.register("core.determinism", CoreSelfTests::determinism);
        SelfTests.register("core.geometry", CoreSelfTests::geometry);
        SelfTests.register("core.seal", CoreSelfTests::seal);
        SelfTests.register("core.travel", CoreSelfTests::travel);
        SelfTests.register("core.expand", CoreSelfTests::expand);
        SelfTests.register("core.roads", CoreSelfTests::roads);
        SelfTests.register("core.chain_travel", CoreSelfTests::chainTravel);
    }

    private static final long PROBE_SALT = 0x434F52454C41424CL;

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static SelfTests.Result ok(String detail) { return SelfTests.Result.ok(detail); }

    private static List<Sector> playable(MegaData data) {
        List<Sector> out = new ArrayList<>();
        for (Sector s : data.sectors()) if (SectorGenerator.playable(s)) out.add(s);
        return out;
    }

    private static long probeSeed(MegaData data, int n) { return data.seed ^ PROBE_SALT ^ (n * 0x9E3779B97F4A7C15L); }

    /** The first sector of a road when it is finished, else null. */
    private static Sector firstPlayable(MegaData data, Polarity road) {
        Sector s = data.chainSector(road, 0);
        return s != null && SectorGenerator.playable(s) ? s : null;
    }

    /** The first sector of the Holy road when it is finished, else the first of the Abyssal road, else null: the sector the single-sector checks look at. */
    private static Sector firstOfAny(MegaData data) {
        Sector s = firstPlayable(data, Polarity.HOLY);
        return s != null ? s : firstPlayable(data, Polarity.ABYSSAL);
    }

    private static String json(SectorGenerator.Planned p) {
        return PlanJson.write(p.result(), p.prepared().request().start.frontCell(), p.prepared().request().start.growth(), List.of(), p.solved().tries(), p.solved().fallback(), "probe");
    }

    // ================================================================================================================ core.assemble

    private static SelfTests.Result assemble(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return fail("no mega dimension");
        VanillaPools pools = VanillaPools.get();
        pools.bind(level.getStructureManager());
        if (pools.poolIds().isEmpty()) return fail("no Mega pools are loaded (data/nusmp/worldgen/template_pool/mega)");
        MegaData data = MegaData.get(server);
        int pieces = 0, rooms = 0, loops = 0, tries = 0, probes = 0, bosses = 0;
        long nanos = 0;
        StringBuilder notes = new StringBuilder();
        boolean floors = floorModel();
        int perRoad = floors ? 3 : 2;                                             // floor model: floor 1 with two seeds, and the first boss floor
        for (Polarity road : Polarity.values()) {
            for (int n = 0; n < perRoad; n++) {
                boolean bossProbe = floors && n == 2;
                String who = "the " + road.id() + " probe " + n + (floors ? bossProbe ? " (the first boss floor)" : " (floor 1)" : "");
                SectorGenerator.Planned p = floors
                        ? SectorGenerator.probeFloor(pools, probeSeed(data, n), road, bossProbe ? FloorMap.BOSS_EVERY - 1 : 0, System.nanoTime())
                        : SectorGenerator.probe(level, probeSeed(data, n), road);
                if (!p.ok()) return fail(who + " cannot be assembled: " + p.failure());
                if (p.solved().fallback()) {
                    return fail(who + " needed the fallback layout after " + (p.solved().tries() - 1) + " tries: " + SectorSolver.summary(p.solved().failures(), 3));
                }
                Box region = p.prepared().request().region;
                SectorFill f = SectorFill.of(p.result(), region);
                int relics = p.prepared().request().relicCount;
                Request.Floor spec = p.prepared().request().floor;
                SectorView view = spec == null ? f.view(0, "ACTIVE", region, relics, -1) : f.view(0, "ACTIVE", region, relics, -1, true, spec.boss());
                List<String> bad = SavedAudit.audit(view, null, pools.recipe().requireMarkers);
                if (!bad.isEmpty()) return fail(who + " fails the saved-sector audit: " + String.join(", ", bad));
                if (spec != null && (f.stairBack == null || f.stairForward == null || f.stairForwardPad == null)) return fail(who + " has no stairs in its fill (back " + f.stairBack + ", forward " + f.stairForward + ")");
                for (String note : p.prepared().notes()) if (notes.indexOf(note) < 0) notes.append(" [").append(note).append("]");
                pieces += p.result().pieces.size();
                rooms += f.rooms.size();
                loops += p.result().loops.size();
                tries += p.solved().tries();
                nanos += p.nanos();
                if (spec != null && spec.boss()) bosses++;
                probes++;
            }
        }
        String what = floors ? " probe floors (floor 1 with two seeds and the first boss floor, on each road; " + bosses + " boss floors)" : " probe sectors (two per road)";
        return ok(probes + what + ": avg pieces=" + pieces / probes + " rooms=" + rooms / probes + " loops=" + loops / probes + " tries=" + tries + " total ms=" + nanos / 1_000_000L + notes);
    }

    /** Is the dungeon built one floor per sector (the 1.58.0 default)? Read from the same config the generator reads, so the probes are the floors a new road would get. */
    private static boolean floorModel() { return FloorMap.isFloorModel(FloorMap.floorsPerSector(FloorsConfig.get(FloorsConfig.PER_SECTOR))); }

    // ================================================================================================================ core.determinism

    private static SelfTests.Result determinism(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return fail("no mega dimension");
        MegaData data = MegaData.get(server);
        SectorGenerator.Planned a = SectorGenerator.probe(level, probeSeed(data, 0)), b = SectorGenerator.probe(level, probeSeed(data, 0)), c = SectorGenerator.probe(level, probeSeed(data, 1));
        if (!a.ok() || !b.ok() || !c.ok()) return fail("a probe cannot be assembled: " + (!a.ok() ? a.failure() : !b.ok() ? b.failure() : c.failure()));
        String ja = json(a);
        if (!ja.equals(json(b))) return fail("two plans of the same seed differ (the generator is not deterministic)");
        if (ja.equals(json(c))) return fail("two different seeds gave the same plan");
        StringBuilder compared = new StringBuilder();
        for (Polarity road : Polarity.values()) {
            Sector first = firstPlayable(data, road);
            if (first == null) continue;
            SectorGenerator.Planned again = SectorGenerator.plan(level, data, first);
            if (!again.ok()) return fail("sector " + first.id + " (the first of the " + road.id() + " road) cannot be planned again from its saved data: " + again.failure());
            String diff = compareSaved(first, again.result());
            if (diff != null) return fail("sector " + first.id + " (the first of the " + road.id() + " road) as saved differs from the plan its seed gives now: " + diff);
            compared.append(compared.length() == 0 ? "" : ", ").append("sector ").append(first.id).append(" (").append(road.id()).append(", ").append(first.rooms.size()).append(" rooms)");
        }
        if (compared.length() == 0) return ok("probe plans repeat exactly (no finished first sector yet to compare with its plan)");
        return ok("probe plans repeat exactly and equal the plan of their saved seed: " + compared);
    }

    /** The first difference between the saved rooms and the rooms of a plan, or null when they are the same rooms in the same places. */
    static String compareSaved(Sector s, Result r) {
        List<Placed> rooms = r.rooms();
        if (rooms.size() != s.rooms.size()) return s.rooms.size() + " rooms saved but " + rooms.size() + " planned";
        for (int i = 0; i < rooms.size(); i++) {
            Placed p = rooms.get(i);
            Room saved = s.rooms.get(i);
            if (!saved.template.equals(p.shape.id)) return "room " + i + " is " + saved.template + " but the plan has " + p.shape.id;
            Box sb = SectorGenerator.box(saved.box);
            if (!sb.equals(p.box)) return "room " + i + " (" + saved.template + ") stands at " + sb + " but the plan puts it at " + p.box;
        }
        return null;
    }

    // ================================================================================================================ core.geometry

    private static SelfTests.Result geometry(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return fail("no mega dimension");
        MegaData data = MegaData.get(server);
        Sector s0 = firstOfAny(data);
        boolean placed = s0 != null;
        SectorGenerator.Planned p = placed ? SectorGenerator.plan(level, data, s0) : SectorGenerator.probe(level, probeSeed(data, 0));
        if (!p.ok()) return fail("the plan cannot be made: " + p.failure());
        SectorGenerator.Built built = SectorGenerator.build(level, p);
        GeometryCheck.Report r = GeometryCheck.run(level, built, placed ? 8 : 0);
        if (!r.ok()) return fail(String.join(", ", r.failures()));
        if (!placed) return ok("plan only (no placed first sector to read the world of): " + r.pieces() + " pieces, boxes " + r.boxes() + ", jigsaws " + r.jigsaws() + ", markers " + r.markers()
                + " agree with the game");
        String diff = compareSaved(s0, p.result());
        if (diff != null) return fail("sector " + s0.id + " as saved differs from its plan, so the world was not compared: " + diff);
        return ok("sector " + s0.id + ": " + r.pieces() + " pieces: boxes " + r.boxes() + ", jigsaws " + r.jigsaws() + ", markers " + r.markers() + " agree with the game; world " + r.found() + "/" + r.sampled()
                + " sampled blocks stand where planned in " + r.worldPieces() + " pieces, jigsaws left " + r.jigsawsLeft());
    }

    // ================================================================================================================ core.seal

    private static SelfTests.Result seal(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return fail("no mega dimension");
        MegaData data = MegaData.get(server);
        List<Sector> all = playable(data);
        StringBuilder detail = new StringBuilder();
        int checked = 0;
        for (int i = all.size() - 1; i >= 0 && checked < 3; i--) {
            Sector s = all.get(i);
            if (s.expansionPos == null || s.expansionFacing == null) continue;
            checked++;
            List<BlockPos> cells = SectorPlacer.sealCells(s);
            int solid = 0;
            for (BlockPos p : cells) if (!level.getBlockState(p).isAir()) solid++;
            Sector next = s.nextId >= 0 ? data.sector(s.nextId) : null;
            BlockPos front = s.expansionPos.relative(s.expansionFacing);
            // 1.58.0: behind the seal of a boss floor stands the grand stairwell, not a tunnel, so the cells outside are the stairwell's own (floor under them, but not
            // necessarily air); what has to hold is the seal itself: whole while the next floor is not there, gone once it is
            boolean stairsBehind = s.isFloor();
            if (next != null && SectorGenerator.playable(next)) {
                if (solid != 0) return fail("sector " + s.id + " leads to sector " + next.id + " but " + solid + " of " + cells.size() + " seal cells are still solid");
                if (level.getBlockState(front.below()).isAir()) return fail("sector " + s.id + "'s seal is open but there is no " + (stairsBehind ? "stairwell" : "tunnel") + " floor outside it at " + front.below().toShortString());
                detail.append("sector ").append(s.id).append(" open into sector ").append(next.id).append("; ");
            } else {
                if (solid < cells.size() * 0.75) return fail("sector " + s.id + "'s Way Onward is not sealed: only " + solid + " of " + cells.size() + " seal cells hold blocks");
                if (!stairsBehind) {
                    for (int k = 0; k < 3; k++) {
                        BlockPos outside = front.relative(s.expansionFacing, k);
                        if (!level.getBlockState(outside).isAir()) return fail("sector " + s.id + " has a block " + (k + 1) + " cells outside its seal at " + outside.toShortString() + ", where the tunnel to the next sector must run");
                    }
                }
                detail.append("sector ").append(s.id).append(" sealed (").append(solid).append("/").append(cells.size()).append(" cells)").append(stairsBehind ? ", the stairs behind it" : ", clear outside").append("; ");
            }
        }
        if (checked == 0) return ok("no finished sector has a Way Onward yet");
        return ok(detail.toString().trim());
    }

    // ================================================================================================================ core.travel

    private static SelfTests.Result travel(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return fail("no mega dimension");
        MegaData data = MegaData.get(server);
        List<Sector> holy = new ArrayList<>();
        for (Sector s : data.chain(Polarity.HOLY)) if (SectorGenerator.playable(s)) holy.add(s);
        if (holy.isEmpty()) return fail("no playable sector on the Holy road: run /multiverse mega expand and wait for its first sector");
        Sector first = holy.get(0), newest = holy.get(holy.size() - 1);
        FakePlayer p = SelfTests.player(level, "mega_travel_test", 0.5, 70.0, 0.5);
        CompoundTag mem = p.getPersistentData();
        clearMemory(mem);
        MegaTravel.forget(p.getUUID());
        PartyKey pinned = PartyService.pin(server, p, 0);                      // 1.58.0: a stand-in is a party of its own; this test is about the shared chain (party 0), so it joins it
        try {
            Sector a = MegaTravel.choose(data, p, false);
            if (a == null || a.id != first.id) return fail("a newcomer should be offered sector " + first.id + " but got " + (a == null ? "nothing" : Integer.toString(a.id)));
            boolean last = com.newuniverse.nusmp.labyrinth.mega.MegaConfig.get(com.newuniverse.nusmp.labyrinth.mega.MegaConfig.LAST_LANDING);
            int want = last ? newest.id : first.id;
            mem.putInt(MegaTravel.LANDING_HOLY, 9999);
            Sector deep = MegaTravel.choose(data, p, false);
            if (deep == null || deep.id != want) return fail("a player who reached a very deep Landing should be offered sector " + want + " but got " + (deep == null ? "nothing" : Integer.toString(deep.id)));
            Sector sneak = MegaTravel.choose(data, p, true);
            if (sneak == null || sneak.id != first.id) return fail("a sneaking player should be offered sector " + first.id);
            clearMemory(mem);
            mem.putInt(MegaTravel.LANDING_KEY, newest.id);                       // the 1.54.0 memory (a sector id; the rest anchors write it too) still counts
            Sector legacy = MegaTravel.choose(data, p, false);
            if (legacy == null || legacy.id != want) return fail("the 1.54.0 key naming sector " + newest.id + " should offer sector " + want + " but gave " + (legacy == null ? "nothing" : Integer.toString(legacy.id)));

            clearMemory(mem);
            LabyrinthTravel.forget(p);
            p.setPos(0.5, 70.0, 0.5);
            boolean entered;
            try {
                entered = MegaTravel.enter(p, p.blockPosition());
            } catch (Throwable t) {
                return fail("MegaTravel.enter threw " + t);
            }
            if (!entered) return fail("MegaTravel.enter refused the player (see the server log)");
            BlockPos pad = SectorGenerator.padOf(first);
            BlockPos sent = LabyrinthTravel.arrivalPad(p);
            if (!pad.equals(sent)) {
                return fail("after entering, the player was sent to " + (sent == null ? "nowhere" : sent.toShortString()) + " instead of the Landing pad of sector " + first.id
                        + " at " + pad.toShortString());
            }
            String remembered = "";
            if (newest.id != first.id) {
                BlockPos npad = SectorGenerator.padOf(newest);
                if (npad == null) return fail("sector " + newest.id + " has no Landing pad");
                p.setPos(npad.getX() + 0.5, npad.getY(), npad.getZ() + 0.5);
                clearMemory(mem);
                p.tickCount = 40;
                MegaTravel.rememberLanding(p);
                if (mem.getInt(MegaTravel.LANDING_HOLY) != newest.chainIndex || mem.getInt(MegaTravel.LANDING_KEY) != newest.id) {
                    return fail("standing on the Landing of sector " + newest.id + " (chain index " + newest.chainIndex + ") did not raise the remembered depth (road key " + mem.getInt(MegaTravel.LANDING_HOLY)
                            + ", 1.54.0 key " + mem.getInt(MegaTravel.LANDING_KEY) + ")");
                }
                remembered = ", depth " + newest.chainIndex + " remembered";
            }
            return ok("newcomer to sector " + first.id + ", deep memory to sector " + want + ", the 1.54.0 key honoured, sneaking to sector " + first.id + ", entered onto the pad" + remembered);
        } finally {
            PartyService.unpin(server, pinned);
            p.getPersistentData().remove(PartyService.KEY);
            clearMemory(mem);
            MegaTravel.forget(p.getUUID());
        }
    }

    private static void clearMemory(CompoundTag mem) {
        mem.remove(MegaTravel.LANDING_KEY);
        mem.remove(MegaTravel.LANDING_HOLY);
        mem.remove(MegaTravel.LANDING_ABYSSAL);
    }

    // ================================================================================================================ core.expand

    private static SelfTests.Result expand(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return fail("no mega dimension");
        if (!(MegaDungeon.expander() instanceof ExpansionQueue)) return fail("the Expander registered with MegaDungeon is not the generator's queue");
        ExpansionQueue queue = ExpansionQueue.get();
        MegaData data = MegaData.get(server);
        StringBuilder detail = new StringBuilder();
        for (Sector s : data.sectors()) {
            if (s.nextId < 0) continue;
            String why = queue.tryNext(level, s);
            if (why == null || !why.startsWith("sector " + s.id + " already leads to sector " + s.nextId)) return fail("a second expansion off sector " + s.id + " was not refused properly: " + why);
            detail.append("refuses a second expansion off sector ").append(s.id).append("; ");
            break;
        }
        for (Polarity road : Polarity.values()) {
            Sector tail = data.chainTail(road);
            if (tail != null && tail.isFloor()) {                                // 1.58.0: floors are joined by stairs; the dry run plans the floor after the newest one
                if (!SectorGenerator.playable(tail)) {
                    detail.append("the newest floor of the ").append(road.id()).append(" road (sector ").append(tail.id).append(") is ").append(tail.state.name()).append(", so no dry run; ");
                    continue;
                }
                String bad = dryRunFloor(level, data, tail, detail);
                if (bad != null) return fail(bad);
                continue;
            }
            Sector free = null;
            for (Sector s : data.chain(road)) if (SectorGenerator.playable(s) && s.expansionPos != null && s.expansionFacing != null && s.nextId < 0) free = s;
            if (free == null) {
                detail.append("the ").append(road.id()).append(" road has no free Way Onward to plan from; ");
                continue;
            }
            SectorPlanner.Next n = ExpansionQueue.planBehind(data, free);
            if (n == null) {
                detail.append("sector ").append(free.id).append(" (").append(road.id()).append(") is a dead end: older sectors fill the space behind its seal; ");
                continue;
            }
            Box region = n.region();
            for (Sector o : data.chain(road)) {
                if (region.inflateXZ(SectorPlanner.GAP - 1).overlaps(SectorGenerator.box(o.region))) return fail("the next region " + region + " would be closer than " + SectorPlanner.GAP + " blocks to sector " + o.id);
            }
            Sector draft = ExpansionQueue.draft(data, free, n);
            String bad = checkDraft(draft, free, data.seed);
            if (bad != null) return fail("the sector behind sector " + free.id + " would be wrong: " + bad);
            SectorGenerator.Planned p = SectorGenerator.plan(level, data, draft, free);
            if (!p.ok()) return fail("the sector behind sector " + free.id + " cannot be planned: " + p.failure());
            SectorFill f = SectorFill.of(p.result(), region);
            List<String> audit = SavedAudit.audit(f.view(draft.id, "ACTIVE", region, draft.relicCount, -1), null, VanillaPools.get().recipe().requireMarkers);
            if (!audit.isEmpty()) return fail("the sector behind sector " + free.id + " fails the saved-sector audit: " + String.join(", ", audit));
            detail.append("sector ").append(draft.id).append(" behind sector ").append(free.id).append(" would be a ").append(draft.theme.id()).append(" sector of the ").append(road.id())
                    .append(" road (floors ").append(FloorMap.label(draft.firstFloor())).append("..").append(FloorMap.label(draft.lastFloor())).append(", threat ").append(draft.threat).append("): ")
                    .append(n.linkModules()).append(" tunnel segments, ").append(p.result().pieces.size()).append(" pieces, ").append(f.rooms.size()).append(" rooms, tries ").append(p.solved().tries())
                    .append(p.solved().fallback() ? " (fallback)" : "").append(", ").append(p.nanos() / 1_000_000L).append(" ms; ");
        }
        return ok(detail.length() == 0 ? "no sectors yet" : detail.toString().trim());
    }

    /** What is wrong with a draft of the sector behind {@code from} (it must continue the same road, one place on, with the same floors per sector, the threat of its floors and its own seed), or null. */
    private static String checkDraft(Sector d, Sector from, long dungeonSeed) {
        if (d.polarity != from.polarity) return "it is on the " + d.polarity.id() + " road but sector " + from.id + " is on the " + from.polarity.id() + " road";
        if (d.chainIndex != from.chainIndex + 1) return "its chain index is " + d.chainIndex + " but should be " + (from.chainIndex + 1);
        if (d.perRegion != from.perRegion) return "it has " + d.perRegion + " floors per sector but its road has " + from.perRegion;
        if (d.firstFloor() != from.lastFloor() + from.polarity.sign()) return "its first floor " + FloorMap.label(d.firstFloor()) + " does not follow floor " + FloorMap.label(from.lastFloor());
        if (d.threat != SectorThreat.of(d.polarity, d.chainIndex, d.perRegion)) return "its threat is " + d.threat + " but its floors say " + SectorThreat.of(d.polarity, d.chainIndex, d.perRegion);
        if (d.party != from.party) return "it is in party " + d.party + " but sector " + from.id + " is in party " + from.party;
        if (d.seed != SectorPlanner.sectorSeed(SectorDraft.partySeed(dungeonSeed, d.party), SectorDraft.seedKey(d.polarity, d.chainIndex))) return "its seed is not the seed of its place";
        return null;
    }

    /**
     * The dry run of the floor after {@code from} (a playable floor of the floor model, the newest of its road): the draft is the one {@code ExpansionQueue} would save, it must
     * stand in the next slot of the road's lane, keep the decade's theme unless it opens a decade, stay clear of every floor of its road, plan with the real pools and pass the
     * saved-sector audit with the stairs. Nothing is placed and nothing is saved. Returns what is wrong, or null (and adds a line to {@code detail}).
     */
    private static String dryRunFloor(ServerLevel level, MegaData data, Sector from, StringBuilder detail) {
        Sector draft = ExpansionQueue.draftFloor(data, from);
        String who = "the floor after sector " + from.id + " (" + from.polarity.id() + " road)";
        String bad = checkDraft(draft, from, data.seed);
        if (bad != null) return who + " would be wrong: " + bad;
        if (!draft.isFloor()) return who + " would not be a floor of the floor model (floors per sector " + draft.perRegion + ")";
        Vec slot = FloorPlan.entry(draft.polarity, draft.chainIndex, draft.party);
        if (draft.entry.getX() != slot.x() || draft.entry.getZ() != slot.z()) return who + " would stand at " + draft.entry.toShortString() + " but its slot is x " + slot.x() + ", z " + slot.z();
        if (!FloorMap.startsDecade(draft.chainIndex, draft.perRegion) && draft.theme != from.theme) {
            return who + " is not the first floor of a decade, so it keeps theme " + from.theme.id() + ", but it has " + draft.theme.id();
        }
        Box region = SectorGenerator.box(draft.region);
        for (Sector o : data.chain(from.party, from.polarity)) {
            if (region.inflateXZ(SectorPlanner.GAP - 1).overlaps(SectorGenerator.box(o.region))) return who + " would be closer than " + SectorPlanner.GAP + " blocks to sector " + o.id;
        }
        SectorGenerator.Planned p = SectorGenerator.plan(level, data, draft, from);
        if (!p.ok()) return who + " cannot be planned: " + p.failure();
        SectorFill f = SectorFill.of(p.result(), region);
        if (f.stairBack == null || f.stairForward == null || f.stairForwardPad == null) return who + " has no stairs in its fill (back " + f.stairBack + ", forward " + f.stairForward + ")";
        List<String> audit = SavedAudit.audit(f.view(draft.id, "ACTIVE", region, draft.relicCount, -1, true, draft.isBossFloor()), null, VanillaPools.get().recipe().requireMarkers);
        if (!audit.isEmpty()) return who + " fails the saved-sector audit: " + String.join(", ", audit);
        detail.append("floor ").append(FloorMap.label(draft.firstFloor())).append(" (sector ").append(draft.id).append(") after sector ").append(from.id).append(" would be a ").append(draft.theme.id())
                .append(draft.isBossFloor() ? " boss floor" : " floor").append(" of the ").append(from.polarity.id()).append(" road, threat ").append(draft.threat).append(": ")
                .append(p.result().pieces.size()).append(" pieces, ").append(f.rooms.size()).append(" rooms, tries ").append(p.solved().tries()).append(p.solved().fallback() ? " (fallback)" : "")
                .append(", ").append(p.nanos() / 1_000_000L).append(" ms; ");
        return null;
    }

    // ================================================================================================================ core.roads

    private static SelfTests.Result roads(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return fail("no mega dimension");
        if (!(MegaDungeon.expander() instanceof ExpansionQueue)) return fail("the Expander registered with MegaDungeon is not the generator's queue");
        MegaData data = MegaData.get(server);
        StringBuilder detail = new StringBuilder();

        // 1. each road's first sector stands on its own lane: the real pools and templates, planned at both lane origins
        for (Polarity road : Polarity.values()) {
            SectorGenerator.Planned p = SectorGenerator.probe(level, probeSeed(data, 7), road);
            if (!p.ok()) return fail("the " + road.id() + " lane cannot be planned: " + p.failure());
            int laneZ = MegaGrid.laneZ(road);
            Vec door = p.prepared().request().start.frontCell();
            if (door.x() != 0 || door.z() != laneZ) return fail("the first sector of the " + road.id() + " road starts at " + door.x() + "," + door.z() + " instead of the lane origin 0," + laneZ);
            Box region = p.prepared().request().region;
            if (region.minZ() > laneZ || region.maxZ() < laneZ) return fail("the region of the " + road.id() + " lane does not contain its lane z " + laneZ);
            for (Placed piece : p.result().pieces) {
                if (Math.abs(piece.box.centerZ() - laneZ) > MegaGrid.blocks(MegaGrid.REGION_MODULES)) return fail("a piece of the " + road.id() + " lane stands at z " + (int) piece.box.centerZ() + ", far from the lane at z " + laneZ);
            }
        }
        detail.append("both lanes plan at their origins (z 0 and ").append(MegaGrid.LANE_Z).append("); ");

        // 2. the saved chains are whole
        for (Polarity road : Polarity.values()) {
            List<Sector> chain = data.chain(road);
            if (chain.isEmpty()) {
                detail.append("the ").append(road.id()).append(" road is not opened; ");
                continue;
            }
            for (int i = 0; i < chain.size(); i++) {
                Sector s = chain.get(i);
                String who = "sector " + s.id + " of the " + road.id() + " road";
                if (s.chainIndex != i) return fail(who + " has chain index " + s.chainIndex + " but stands at place " + i + " of its road (the chain has a hole or a duplicate)");
                if (s.threat != SectorThreat.of(road, s.chainIndex, s.perRegion)) return fail(who + " has threat " + s.threat + " but its floors say " + SectorThreat.of(road, s.chainIndex, s.perRegion));
                int midZ = (s.region.minZ() + s.region.maxZ()) / 2;
                if (Math.abs(midZ - MegaGrid.laneZ(road)) > MegaGrid.LANE_Z / 2) return fail(who + " lies at z " + midZ + ", outside its lane");
                if (s.isFloor()) {                                                   // 1.58.0: a floor stands in the slot of its place and its door is inside its own region
                    Vec slot = FloorPlan.entry(road, s.chainIndex);
                    if (s.entry == null || s.entry.getX() != slot.x() || s.entry.getZ() != slot.z()) {
                        return fail(who + " (floor " + FloorMap.label(s.firstFloor()) + ") has its door at " + (s.entry == null ? "nowhere" : s.entry.toShortString()) + " but its slot is x " + slot.x() + ", z " + slot.z());
                    }
                    if (s.entry.getX() < s.region.minX() || s.entry.getX() > s.region.maxX() || s.entry.getZ() < s.region.minZ() || s.entry.getZ() > s.region.maxZ()) {
                        return fail(who + " has its door at " + s.entry.toShortString() + " outside its own region");
                    }
                }
                if (i > 0) {
                    Sector prev = chain.get(i - 1);
                    if (prev.nextId != s.id) return fail("sector " + prev.id + " does not lead to sector " + s.id + " (it says " + prev.nextId + ") though they follow each other on the " + road.id() + " road");
                    if (s.perRegion != prev.perRegion) return fail(who + " has " + s.perRegion + " floors per sector but the one before it has " + prev.perRegion);
                    if (s.firstFloor() != prev.lastFloor() + road.sign()) return fail("the floors of " + who + " start at " + FloorMap.label(s.firstFloor()) + " but the sector before ends at " + FloorMap.label(prev.lastFloor()));
                    if (s.isFloor() && prev.isFloor()) {                             // two floors of a road never touch (their slots are one pitch apart)
                        long gap = SectorGenerator.box(prev.region).gapXZ(SectorGenerator.box(s.region));
                        if (gap < SectorPlanner.GAP) return fail("floor " + FloorMap.label(prev.firstFloor()) + " (sector " + prev.id + ") and floor " + FloorMap.label(s.firstFloor()) + " (sector " + s.id + ") are only " + gap + " blocks apart");
                    }
                }
            }
            Sector head = chain.get(0), tail = chain.get(chain.size() - 1);
            detail.append("the ").append(road.id()).append(" road: ").append(chain.size()).append(" sector(s), floors ").append(FloorMap.label(head.firstFloor())).append("..").append(FloorMap.label(tail.lastFloor())).append("; ");
        }

        // 3. the roads never come near each other
        for (Sector a : data.chain(Polarity.HOLY)) {
            for (Sector b : data.chain(Polarity.ABYSSAL)) {
                long gap = SectorGenerator.box(a.region).gapXZ(SectorGenerator.box(b.region));
                if (gap < SectorPlanner.GAP) return fail("sector " + a.id + " of the Holy road and sector " + b.id + " of the Abyssal road are only " + gap + " blocks apart");
            }
        }

        // 4. a road that exists refuses a second first sector (and nothing is created by asking)
        for (Polarity road : Polarity.values()) {
            if (data.chainTail(road) == null) continue;
            int before = data.sectors().size();
            if (MegaDungeon.expander().requestFirst(server, road) || data.sectors().size() != before) return fail("the " + road.id() + " road already has a sector, but a second first sector was accepted");
        }
        return ok(detail.toString().trim());
    }

    // ================================================================================================================ core.chain_travel

    private static SelfTests.Result chainTravel(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return fail("no mega dimension");
        MegaData data = MegaData.get(server);
        StringBuilder detail = new StringBuilder();
        int tested = 0;
        for (Polarity road : Polarity.values()) {
            Sector first = firstPlayable(data, road);
            if (first == null) {
                detail.append("the ").append(road.id()).append(" road has no finished sector to enter; ");
                continue;
            }
            FakePlayer p = SelfTests.player(level, "mega_chain_test_" + road.id(), 0.5, 70.0, 0.5);
            CompoundTag mem = p.getPersistentData();
            clearMemory(mem);
            MegaTravel.forget(p.getUUID());
            LabyrinthTravel.forget(p);
            PartyKey pinned = PartyService.pin(server, p, 0);                  // 1.58.0: the stand-in joins the shared chain (party 0), which is what this test reads
            try {
                Sector chosen = MegaTravel.choose(data, p, road, false);
                if (chosen == null || chosen.id != first.id) return fail("a newcomer should be offered the first sector of the " + road.id() + " road (sector " + first.id + ") but got " + (chosen == null ? "nothing" : Integer.toString(chosen.id)));
                // the facade: this is the call the Hub's gates make, so it proves the generator registered its ChainTravel
                if (!MegaDungeon.enterChain(p, road)) return fail("MegaDungeon.enterChain refused a player for the " + road.id() + " road (is the ChainTravel registered? see the server log)");
                BlockPos pad = SectorGenerator.padOf(first);
                BlockPos sent = LabyrinthTravel.arrivalPad(p);
                if (!pad.equals(sent)) {
                    return fail("after enterChain(" + road.id() + ") the player was sent to " + (sent == null ? "nowhere" : sent.toShortString()) + " instead of the pad of sector " + first.id + " at " + pad.toShortString());
                }
                // the memory of one road never leaks into the other
                List<Sector> chain = data.chain(road);
                Sector newest = first;
                for (Sector s : chain) if (SectorGenerator.playable(s)) newest = s;
                boolean last = com.newuniverse.nusmp.labyrinth.mega.MegaConfig.get(com.newuniverse.nusmp.labyrinth.mega.MegaConfig.LAST_LANDING);
                mem.putInt(MegaTravel.landingKey(road), 9999);
                Sector deep = MegaTravel.choose(data, p, road, false);
                int want = last ? newest.id : first.id;
                if (deep == null || deep.id != want) return fail("a deep memory on the " + road.id() + " road should offer sector " + want + " but gave " + (deep == null ? "nothing" : Integer.toString(deep.id)));
                Sector other = firstPlayable(data, road.opposite());
                if (other != null) {
                    Sector there = MegaTravel.choose(data, p, road.opposite(), false);
                    if (there == null || there.id != other.id) return fail("the memory of the " + road.id() + " road changed what the " + road.opposite().id() + " road offers (sector " + (there == null ? "none" : Integer.toString(there.id)) + " instead of " + other.id + ")");
                }
                Sector sneak = MegaTravel.choose(data, p, road, true);
                if (sneak == null || sneak.id != first.id) return fail("a sneaking player should be offered the first sector of the " + road.id() + " road");
                detail.append("the ").append(road.id()).append(" road: entered onto the pad of sector ").append(first.id).append(", deep memory offers sector ").append(want).append(", the other road is not affected; ");
                tested++;
            } finally {
                PartyService.unpin(server, pinned);
                p.getPersistentData().remove(PartyService.KEY);
                clearMemory(mem);
                MegaTravel.forget(p.getUUID());
            }
        }
        if (tested == 0) return fail("no road has a finished sector: run /multiverse mega expand and wait for it");
        return ok(detail.toString().trim());
    }
}
