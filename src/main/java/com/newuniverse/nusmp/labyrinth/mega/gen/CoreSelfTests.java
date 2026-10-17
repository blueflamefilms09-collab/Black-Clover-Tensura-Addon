package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.travel.MegaTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing the generator does can be watched by its author, so the CI smoke test runs these six checks in the real game ({@code /multiverse mega
 * selftest core.*}) after it has made the dungeon grow with {@code /multiverse mega expand}. They are cheap (each well under two seconds, nothing is placed)
 * and each answers one question a person would otherwise have to find out by walking: {@code core.assemble} do the real pools and templates make sectors
 * that pass the audit; {@code core.determinism} does the same seed always give the same plan, and does the saved sector 0 equal the plan its seed gives now
 * (so regeneration and the restart repair are safe); {@code core.geometry} does the game agree with the plan's rotation maths and with where the
 * blocks really stand ({@link GeometryCheck}); {@code core.seal} is each Way Onward really sealed, or really open with a tunnel floor behind it once the next sector
 * exists; {@code core.travel} does a player arrive on the right Landing and is the deepest Landing remembered; {@code core.expand} is the expansion machinery
 * registered, does it refuse what it must, and would the sector behind the newest free Way Onward plan and audit cleanly (a dry run: the real request, the
 * real solver, nothing saved). The checks that need a built sector say so in their answer when there is none, instead of passing silently.
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
        int pieces = 0, rooms = 0, loops = 0, tries = 0;
        long nanos = 0;
        for (int n = 0; n < 3; n++) {
            SectorGenerator.Planned p = SectorGenerator.probe(level, probeSeed(data, n));
            if (!p.ok()) return fail("probe " + n + " cannot be assembled: " + p.failure());
            if (p.solved().fallback()) {
                return fail("probe " + n + " needed the fallback layout after " + (p.solved().tries() - 1) + " tries: " + SectorSolver.summary(p.solved().failures(), 3));
            }
            Box region = p.prepared().request().region;
            SectorFill f = SectorFill.of(p.result(), region);
            List<String> bad = SavedAudit.audit(f.view(0, "ACTIVE", region, 1, -1), null, pools.recipe().requireMarkers);
            if (!bad.isEmpty()) return fail("probe " + n + " fails the saved-sector audit: " + String.join(", ", bad));
            pieces += p.result().pieces.size();
            rooms += f.rooms.size();
            loops += p.result().loops.size();
            tries += p.solved().tries();
            nanos += p.nanos();
        }
        return ok("3 probe sectors: avg pieces=" + pieces / 3 + " rooms=" + rooms / 3 + " loops=" + loops / 3 + " tries=" + tries + " total ms=" + nanos / 1_000_000L);
    }

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
        Sector s0 = data.sector(0);
        if (s0 == null || !SectorGenerator.playable(s0)) return ok("probe plans repeat exactly (no sector 0 yet to compare with its plan)");
        SectorGenerator.Planned again = SectorGenerator.plan(level, data, s0);
        if (!again.ok()) return fail("sector 0 cannot be planned again from its saved data: " + again.failure());
        String diff = compareSaved(s0, again.result());
        if (diff != null) return fail("sector 0 as saved differs from the plan its seed gives now: " + diff);
        return ok("probe plans repeat exactly and sector 0 (" + s0.rooms.size() + " rooms) equals the plan of its saved seed");
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
        Sector s0 = data.sector(0);
        boolean placed = s0 != null && SectorGenerator.playable(s0);
        SectorGenerator.Planned p = placed ? SectorGenerator.plan(level, data, s0) : SectorGenerator.probe(level, probeSeed(data, 0));
        if (!p.ok()) return fail("the plan cannot be made: " + p.failure());
        SectorGenerator.Built built = SectorGenerator.build(level, p);
        GeometryCheck.Report r = GeometryCheck.run(level, built, placed ? 8 : 0);
        if (!r.ok()) return fail(String.join(", ", r.failures()));
        if (!placed) return ok("plan only (no placed sector 0 to read the world of): " + r.pieces() + " pieces, boxes " + r.boxes() + ", jigsaws " + r.jigsaws() + ", markers " + r.markers()
                + " agree with the game");
        String diff = compareSaved(s0, p.result());
        if (diff != null) return fail("sector 0 as saved differs from its plan, so the world was not compared: " + diff);
        return ok(r.pieces() + " pieces: boxes " + r.boxes() + ", jigsaws " + r.jigsaws() + ", markers " + r.markers() + " agree with the game; world " + r.found() + "/" + r.sampled()
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
            if (next != null && SectorGenerator.playable(next)) {
                if (solid != 0) return fail("sector " + s.id + " leads to sector " + next.id + " but " + solid + " of " + cells.size() + " seal cells are still solid");
                if (level.getBlockState(front.below()).isAir()) return fail("sector " + s.id + "'s seal is open but there is no tunnel floor outside it at " + front.below().toShortString());
                detail.append("sector ").append(s.id).append(" open into sector ").append(next.id).append("; ");
            } else {
                if (solid < cells.size() * 0.75) return fail("sector " + s.id + "'s Way Onward is not sealed: only " + solid + " of " + cells.size() + " seal cells hold blocks");
                for (int k = 0; k < 3; k++) {
                    BlockPos outside = front.relative(s.expansionFacing, k);
                    if (!level.getBlockState(outside).isAir()) return fail("sector " + s.id + " has a block " + (k + 1) + " cells outside its seal at " + outside.toShortString() + ", where the tunnel to the next sector must run");
                }
                detail.append("sector ").append(s.id).append(" sealed (").append(solid).append("/").append(cells.size()).append(" cells), clear outside; ");
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
        List<Sector> playable = playable(data);
        if (playable.isEmpty()) return fail("no playable sector: run /multiverse mega expand and wait for sector 0");
        Sector first = playable.get(0), newest = playable.get(playable.size() - 1);
        FakePlayer p = SelfTests.player(level, "mega_travel_test", 0.5, 70.0, 0.5);
        CompoundTag mem = p.getPersistentData();
        mem.remove(MegaTravel.LANDING_KEY);
        MegaTravel.forget(p.getUUID());
        try {
            Sector a = MegaTravel.choose(data, p, false);
            if (a == null || a.id != first.id) return fail("a newcomer should be offered sector " + first.id + " but got " + (a == null ? "nothing" : Integer.toString(a.id)));
            mem.putInt(MegaTravel.LANDING_KEY, 9999);
            Sector deep = MegaTravel.choose(data, p, false);
            boolean last = com.newuniverse.nusmp.labyrinth.mega.MegaConfig.get(com.newuniverse.nusmp.labyrinth.mega.MegaConfig.LAST_LANDING);
            int want = last ? newest.id : first.id;
            if (deep == null || deep.id != want) return fail("a player who reached a very deep Landing should be offered sector " + want + " but got " + (deep == null ? "nothing" : Integer.toString(deep.id)));
            Sector sneak = MegaTravel.choose(data, p, true);
            if (sneak == null || sneak.id != first.id) return fail("a sneaking player should be offered sector " + first.id);

            mem.remove(MegaTravel.LANDING_KEY);
            p.setPos(0.5, 70.0, 0.5);
            boolean entered;
            try {
                entered = MegaTravel.enter(p, p.blockPosition());
            } catch (Throwable t) {
                return fail("MegaTravel.enter threw " + t);
            }
            if (!entered) return fail("MegaTravel.enter refused the player (see the server log)");
            BlockPos pad = SectorGenerator.padOf(first);
            double dx = p.getX() - (pad.getX() + 0.5), dz = p.getZ() - (pad.getZ() + 0.5), dy = p.getY() - pad.getY();
            if (dx * dx + dz * dz > 1.0 || Math.abs(dy) > 1.5) {
                return fail("after entering, the player stands at " + (int) p.getX() + "," + (int) p.getY() + "," + (int) p.getZ() + " instead of the Landing pad of sector " + first.id
                        + " at " + pad.toShortString());
            }
            String remembered = "";
            if (newest.id > 0) {
                BlockPos npad = SectorGenerator.padOf(newest);
                if (npad == null) return fail("sector " + newest.id + " has no Landing pad");
                p.setPos(npad.getX() + 0.5, npad.getY(), npad.getZ() + 0.5);
                mem.remove(MegaTravel.LANDING_KEY);
                p.tickCount = 40;
                MegaTravel.rememberLanding(p);
                if (mem.getInt(MegaTravel.LANDING_KEY) != newest.id) {
                    return fail("standing on the Landing of sector " + newest.id + " did not raise the remembered depth (it is " + mem.getInt(MegaTravel.LANDING_KEY) + ")");
                }
                remembered = ", depth " + newest.id + " remembered";
            }
            return ok("newcomer to sector " + first.id + ", deep memory to sector " + want + ", sneaking to sector " + first.id + ", entered onto the pad" + remembered);
        } finally {
            mem.remove(MegaTravel.LANDING_KEY);
            MegaTravel.forget(p.getUUID());
        }
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
        Sector free = null;
        for (Sector s : playable(data)) if (s.expansionPos != null && s.expansionFacing != null && s.nextId < 0) free = s;
        if (free == null) return ok(detail + "no free Way Onward to plan from (" + data.sectors().size() + " sectors)");
        SectorPlanner.Next n = ExpansionQueue.planBehind(data, free);
        if (n == null) return ok(detail + "sector " + free.id + " is a dead end (older sectors fill the space behind its seal)");
        Box region = n.region();
        for (Sector o : data.sectors()) {
            if (region.inflateXZ(SectorPlanner.GAP - 1).overlaps(SectorGenerator.box(o.region))) return fail("the next region " + region + " would be closer than " + SectorPlanner.GAP + " blocks to sector " + o.id);
        }
        Sector draft = ExpansionQueue.draft(data, free, n);
        SectorGenerator.Planned p = SectorGenerator.plan(level, data, draft, free);
        if (!p.ok()) return fail("the sector behind sector " + free.id + " cannot be planned: " + p.failure());
        SectorFill f = SectorFill.of(p.result(), region);
        List<String> bad = SavedAudit.audit(f.view(draft.id, "ACTIVE", region, draft.relicCount, -1), null, VanillaPools.get().recipe().requireMarkers);
        if (!bad.isEmpty()) return fail("the sector behind sector " + free.id + " fails the saved-sector audit: " + String.join(", ", bad));
        return ok(detail + "sector " + draft.id + " behind sector " + free.id + " would be a " + draft.theme.id() + " sector: " + n.linkModules() + " tunnel segments, " + p.result().pieces.size()
                + " pieces, " + f.rooms.size() + " rooms, tries " + p.solved().tries() + (p.solved().fallback() ? " (fallback)" : "") + ", " + p.nanos() / 1_000_000L + " ms");
    }
}
