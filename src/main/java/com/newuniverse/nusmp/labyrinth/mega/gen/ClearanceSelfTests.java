package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaConfig;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.floors.FloorsConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Why: the 7 x 7 minimum clearance ({@link Clearance7}) changes the finish of every NEW piece of the Mega-Dungeon, and nothing about it can be watched by its author, so
 * the CI smoke test runs this self-test ({@code /multiverse mega selftest dungeon.clearance7}) in the real game on the real templates, in four parts. (1) The rule itself
 * behaves on made-up spaces (a 5-wide doorway, a 6-high one and a pillar fail; a free 7 x 7 passes) and the switch {@code gen.clearance7} is readable. (2) Probe plans (both
 * roads, an ordinary and a boss floor, the cyclic generator when it is on; nothing is placed or saved): cutting must keep every module connected ({@link Clearance7#connectivity}:
 * every join and loop has a cut on both sides and the two line up, no cut leaves its piece or reaches the outer wall, the room graph stays connected). (3) The templates the
 * game really loaded for those plans (from the data pack, not from the catalog dump the unit test uses) are cut exactly as the placer cuts them and measured with the rule: a
 * transition that is still below 7 x 7 must be blocked by a block the cut is not allowed to remove (a loot chest), or the cutting logic is wrong. (4) The world: for the
 * finished sectors that were made with the switch on, the pieces read from the world are measured with the same rule ({@link GeometryCheck#clearance7}); older sectors are
 * counted, not judged. The detail line carries the measured numbers.
 */
final class ClearanceSelfTests {
    private ClearanceSelfTests() {}

    static void register() {
        SelfTests.register("dungeon.clearance7", ClearanceSelfTests::clearance7);
    }

    private static SelfTests.Result fail(String d) { return SelfTests.Result.fail(d); }

    private static SelfTests.Result ok(String d) { return SelfTests.Result.ok(d); }

    private static SelfTests.Result clearance7(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return fail("no mega dimension");

        // ---- (1) the rule and the switch
        List<String> rule = Clearance7.ruleSelfCheck();
        if (!rule.isEmpty()) return fail("the rule is wrong: " + String.join("; ", rule));
        boolean on = ClearanceSettings.enabled();

        // ---- (1b) 1.107.0, the systems that CLOSE doorways must close a cut one completely: the lockdown barrier (inner and outer wall layer, the outer columns at the corner, the limits for
        // twelve doorways), the Sealed Archway's columns (7 wide, two layers, 7 high) and the seal_doors bar (7 rows); the same checks run on the real sector in dungeon.lockdown
        List<String> closing = new ArrayList<>(com.newuniverse.nusmp.labyrinth.mega.lockdown.OpeningScan.cutDoorwayCheck());
        closing.addAll(com.newuniverse.nusmp.labyrinth.trial.ArchwayDoorways.selfCheck());
        closing.addAll(com.newuniverse.nusmp.labyrinth.mega.mechanics.DoorSealing.selfCheck());
        if (!closing.isEmpty()) return fail("a closing system does not cover a cut 7 x 7 doorway: " + String.join("; ", closing.subList(0, Math.min(3, closing.size()))));

        VanillaPools pools = VanillaPools.get();
        pools.bind(level.getStructureManager());
        if (pools.poolIds().isEmpty()) return fail("no Mega pools are loaded");
        MegaData data = MegaData.get(server);

        // ---- (2) + (3) probe plans
        int tries = MegaConfig.get(MegaConfig.ASSEMBLE_TRIES);
        boolean floors = FloorMap.isFloorModel(FloorMap.floorsPerSector(FloorsConfig.get(FloorsConfig.PER_SECTOR)));
        int plans = 0, pieces = 0, doorways = 0, corridors = 0, chambers = 0, stairColumns = 0, blocked = 0, cleared = 0, kept = 0, capped = 0, templatesRead = 0, templatesMissing = 0;
        for (Polarity road : Polarity.values()) {
            for (int n = 0; n < 2; n++) {
                long seed = data.seed ^ 0x434C524E43L ^ (n * 0x9E3779B97F4A7C15L);
                String who = "the " + road.id() + " probe " + n;
                SectorGenerator.Planned base = floors ? SectorGenerator.probeFloor(pools, seed, road, n * (FloorMap.BOSS_EVERY - 1), System.nanoTime()) : SectorGenerator.probe(level, seed, road);
                if (!base.ok()) return fail(who + " cannot be assembled: " + base.failure());
                Result r = base.result();
                List<String> conn = Clearance7.connectivity(r);
                if (!conn.isEmpty()) return fail(who + ": cutting would break the connections: " + String.join("; ", conn.subList(0, Math.min(3, conn.size()))));
                boolean[][] connected = Clearance7.connected(r);
                for (Placed p : r.pieces) {
                    if (p.shape.isCap()) continue;
                    pieces++;
                    Optional<StructureTemplate> t = ResourceLocation.tryParse(p.shape.id) == null ? Optional.empty() : level.getStructureManager().get(ResourceLocation.tryParse(p.shape.id));
                    if (t.isEmpty()) {
                        templatesMissing++;
                        continue;
                    }
                    Placed flat = new Placed(0, p.shape, p.processors, 0, Vec.ZERO, 0, -1, -1, -1, false);
                    ClearanceGrid g = grid(t.get());
                    templatesRead++;
                    List<Clearance7.Cut> cuts = new ArrayList<>(Clearance7.cuts(flat, connected[p.index]));
                    if (p.shape.kind == RoomKind.STAIRWELL) cuts.addAll(Clearance7.stairCuts(0, g.stairStands(), p.shape.id));
                    ClearanceGrid.Stats st = g.cutAll(cuts);
                    cleared += st.cleared();
                    kept += st.kept();
                    capped += st.capped();
                    for (Clearance7.Cut c : cuts) {
                        switch (c.kind()) {
                            case DOORWAY -> doorways++;
                            case CORRIDOR -> corridors++;
                            case CHAMBER -> chambers++;
                            case STAIR -> stairColumns++;
                        }
                        Clearance7.Clearance m = Clearance7.measure(g, c);
                        if (m.ok()) continue;
                        String why = blocker(g, c);
                        if (why == null) return fail(who + ": " + p.shape.id + " " + c.kind() + " is " + m + " after the cut and nothing the cut must leave stands in it: the cutting logic is wrong");
                        blocked++;
                    }
                }
                plans++;
            }
        }

        // ---- (4) the world
        int sectors = 0, older = 0, checkedPieces = 0;
        for (Sector s : data.sectors()) {
            if (!SectorGenerator.playable(s)) continue;
            if (!ClearanceCarver.wasCut(server, s.id)) {
                older++;
                continue;
            }
            if (sectors >= 2) continue;
            SectorGenerator.Planned p = SectorGenerator.plan(level, data, s);
            if (!p.ok()) return fail("sector " + s.id + " cannot be planned again from its saved data: " + p.failure());
            String diff = CoreSelfTests.compareSaved(s, p.result());
            if (diff != null) return fail("sector " + s.id + " as saved differs from the plan its seed gives now, so the world was not read: " + diff);
            List<Integer> pick = GeometryCheck.pick(p.result(), 40);
            List<String> bad = GeometryCheck.clearance7(level, p.result(), pick::contains);
            if (!bad.isEmpty()) return fail("sector " + s.id + " was cut to 7 x 7 but the world disagrees: " + String.join("; ", bad.subList(0, Math.min(3, bad.size()))));
            checkedPieces += pick.size();
            sectors++;
        }
        String world = sectors == 0 ? "no finished sector made with the switch on yet to read the world of (" + older + " older sector(s) are untouched)"
                : sectors + " finished sector(s) read in the world, " + checkedPieces + " pieces all at least 7x7 (" + older + " older sector(s) untouched)";
        return ok(String.format(Locale.ROOT, "rule behaves; the lockdown barrier, the Sealed Archway columns and the seal_doors bar cover a cut 7x7 doorway completely; %d probe plans keep every module connected; %d real templates cut: %d doorways, %d corridor strips, %d chambers, %d stair head-room columns all measure 7x7 or better (%d blocked only by a block that is never cut: %d blocks left standing); %d blocks removed, %d roof blocks; %s; switch gen.clearance7=%s",
                plans, templatesRead, doorways, corridors, chambers, stairColumns, blocked, kept, cleared, capped, world, on)
                + (templatesMissing > 0 ? "; " + templatesMissing + " template(s) not loadable" : ""));
    }

    /** The first block still in a cut's box that the cut is not allowed to remove, or null when the blocker is something that could have been cut. */
    private static String blocker(ClearanceGrid g, Clearance7.Cut c) {
        Box b = c.box();
        for (int y = b.minY(); y <= b.maxY(); y++) {
            for (int x = b.minX(); x <= b.maxX(); x++) {
                for (int z = b.minZ(); z <= b.maxZ(); z++) {
                    if (g.free(x, y, z)) continue;
                    String id = g.id(x, y, z);
                    if (id.equals("minecraft:bedrock")) continue;                      // outside the template: a cut that leaves its piece (the connectivity check catches that)
                    return Clearance7.carvable(id, g.blockEntity(x, y, z)) ? null : id + " at " + x + "," + y + "," + z;
                }
            }
        }
        return null;
    }

    /** A template as a grid of block ids (the same fields {@code GeometryCheck.world} reads). */
    static ClearanceGrid grid(StructureTemplate t) {
        CompoundTag nbt = t.save(new CompoundTag());
        ListTag size = nbt.getList("size", Tag.TAG_INT);
        ClearanceGrid g = new ClearanceGrid(size.getInt(0), size.getInt(1), size.getInt(2));
        ListTag palette = nbt.getList("palette", Tag.TAG_COMPOUND);
        if (palette.isEmpty() && nbt.contains("palettes", Tag.TAG_LIST)) {
            ListTag all = nbt.getList("palettes", Tag.TAG_LIST);
            if (!all.isEmpty()) palette = all.getList(0);
        }
        String[] names = new String[palette.size()];
        boolean[] stairBottom = new boolean[palette.size()];
        for (int i = 0; i < names.length; i++) {
            CompoundTag e = palette.getCompound(i);
            names[i] = e.getString("Name");
            stairBottom[i] = names[i].endsWith("_stairs") && e.getCompound("Properties").getString("half").equals("bottom");
        }
        ListTag blocks = nbt.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag b = blocks.getCompound(i);
            int state = b.getInt("state");
            ListTag pos = b.getList("pos", Tag.TAG_INT);
            if (state < 0 || state >= names.length || pos.size() != 3) continue;
            g.set(pos.getInt(0), pos.getInt(1), pos.getInt(2), names[state], b.contains("nbt"), stairBottom[state]);
        }
        return g;
    }
}
