package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.floors.FloorsConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Why: the rebuilt rooms ({@link RoomVariants}) are only believable if the GAME reads them as the same pieces the plan was made from, and nothing here can be watched by its author, so
 * the CI smoke test runs this self-test ({@code /multiverse mega selftest dungeon.rooms_rebuilt}) in the real game on the real files, in four parts. (1) The switch {@code gen.roomsRebuilt} is
 * readable and ON by default, and the table of variants is not empty. (2) Every rebuilt file named by the table is readable by the game's own template manager and agrees with the shipped
 * template it replaces: the same size, the same jigsaw blocks at the same cells pointing the same way, the same data markers at the same cells with the same text. (3) The rebuilt rooms
 * carry no armour stand (the stand is a rare prop, see {@code labyrinth/props/ArmourStandRarity}) except the one slot of the stand rooms. (4) Probe plans (both roads, an ordinary and a boss
 * floor, the cyclic generator when it is on; nothing is placed or saved) are built with the rebuilt rooms and run through the geometry check of the game ({@link GeometryCheck}: the game's own
 * boxes, jigsaw cells and marker cells of every piece against the plan), and a plan built twice gives the same pieces. The detail line carries the numbers.
 */
final class RoomsSelfTests {
    private RoomsSelfTests() {}

    static void register() {
        SelfTests.register("dungeon.rooms_rebuilt", RoomsSelfTests::rooms);
    }

    private static SelfTests.Result fail(String d) { return SelfTests.Result.fail(d); }

    private static SelfTests.Result ok(String d) { return SelfTests.Result.ok(d); }

    /** The size, the cells of the jigsaw blocks (with orientation and every data field) and of the data markers (with their text), and the number of armour stands of a template. */
    private record Face(int sx, int sy, int sz, TreeSet<String> jigsaws, TreeSet<String> markers, int stands) {}

    private static Face face(StructureTemplate t) {
        CompoundTag nbt = t.save(new CompoundTag());
        ListTag palette = nbt.getList("palette", Tag.TAG_COMPOUND);
        if (palette.isEmpty() && nbt.contains("palettes", Tag.TAG_LIST)) {
            ListTag all = nbt.getList("palettes", Tag.TAG_LIST);
            if (!all.isEmpty()) palette = all.getList(0);
        }
        ListTag size = nbt.getList("size", Tag.TAG_INT);
        TreeSet<String> jig = new TreeSet<>(), mk = new TreeSet<>();
        ListTag blocks = nbt.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag b = blocks.getCompound(i);
            int state = b.getInt("state");
            if (state < 0 || state >= palette.size()) continue;
            CompoundTag p = palette.getCompound(state);
            String name = p.getString("Name");
            ListTag pos = b.getList("pos", Tag.TAG_INT);
            String at = pos.getInt(0) + "," + pos.getInt(1) + "," + pos.getInt(2);
            if (name.equals("minecraft:jigsaw")) {
                String o = p.contains("Properties") ? p.getCompound("Properties").getString("orientation") : "";
                CompoundTag d = b.contains("nbt") ? b.getCompound("nbt") : new CompoundTag();
                jig.add(at + " " + o + " " + d.getString("name") + " " + d.getString("target") + " " + d.getString("pool") + " " + d.getString("joint") + " " + d.getString("final_state")
                        + " " + d.getInt("selection_priority") + " " + d.getInt("placement_priority"));
            } else if (name.equals("minecraft:structure_block")) {
                CompoundTag d = b.contains("nbt") ? b.getCompound("nbt") : new CompoundTag();
                mk.add(at + " " + d.getString("metadata"));
            }
        }
        int stands = 0;
        ListTag ents = nbt.getList("entities", Tag.TAG_COMPOUND);
        for (int i = 0; i < ents.size(); i++) if (ents.getCompound(i).getCompound("nbt").getString("id").equals("minecraft:armor_stand")) stands++;
        return new Face(size.getInt(0), size.getInt(1), size.getInt(2), jig, mk, stands);
    }

    private static SelfTests.Result rooms(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return fail("no mega dimension");

        // ---- (1) the switch and the table
        if (!RoomsSettings.enabled()) return fail("gen.roomsRebuilt is not on by default");
        RoomVariants.Table table = RoomVariants.table();
        if (table.size() < 40) return fail("the table of rebuilt rooms is empty or unreadable (" + table.size() + " templates; " + table.problem() + ")");

        // ---- (2) + (3) every rebuilt file against the shipped template
        StructureTemplateManager mgr = level.getStructureManager();
        int files = 0, stands = 0;
        List<String> bad = new ArrayList<>();
        for (Map.Entry<String, Integer> e : table.counts().entrySet()) {
            Optional<StructureTemplate> shipped = mgr.get(ResourceLocation.fromNamespaceAndPath("nusmp", RoomVariants.bare(e.getKey())));
            if (shipped.isEmpty()) {
                bad.add(e.getKey() + " is named by the table but the game cannot read it");
                continue;
            }
            Face s = face(shipped.get());
            for (int k = 1; k <= e.getValue(); k++) {
                String id = RoomVariants.variantId("nusmp:" + e.getKey(), k);
                Optional<StructureTemplate> t = mgr.get(ResourceLocation.parse(id));
                if (t.isEmpty()) {
                    bad.add(id + " cannot be read");
                    continue;
                }
                files++;
                Face f = face(t.get());
                if (f.sx() != s.sx() || f.sy() != s.sy() || f.sz() != s.sz()) bad.add(id + " is " + f.sx() + "x" + f.sy() + "x" + f.sz() + ", the shipped room is " + s.sx() + "x" + s.sy() + "x" + s.sz());
                else if (!f.jigsaws().equals(s.jigsaws())) bad.add(id + " has other sockets than " + e.getKey());
                else if (!f.markers().equals(s.markers())) bad.add(id + " has other data markers than " + e.getKey());
                stands += f.stands();
                if (f.stands() > 1) bad.add(id + " carries " + f.stands() + " armour stands (one slot at most)");
                if (f.stands() == 1 && !table.standRoom(e.getKey())) bad.add(id + " carries an armour stand but is not a stand room");
            }
            if (bad.size() >= 4) break;
        }
        if (!bad.isEmpty()) return fail(String.join("; ", bad.subList(0, Math.min(4, bad.size()))));

        // ---- (4) probe plans built with the rebuilt rooms
        VanillaPools pools = VanillaPools.get();
        pools.bind(mgr);
        if (pools.poolIds().isEmpty()) return fail("no Mega pools are loaded");
        MegaData data = MegaData.get(server);
        boolean floors = FloorMap.isFloorModel(FloorMap.floorsPerSector(FloorsConfig.get(FloorsConfig.PER_SECTOR)));
        int plans = 0, pieces = 0, swapped = 0, checked = 0;
        for (Polarity road : Polarity.values()) {
            for (int n = 0; n < 2; n++) {
                long seed = data.seed ^ 0x5252424C54L ^ (n * 0x9E3779B97F4A7C15L);
                SectorGenerator.Planned planned = floors ? SectorGenerator.probeFloor(pools, seed, road, n * (FloorMap.BOSS_EVERY - 1), System.nanoTime()) : SectorGenerator.probe(level, seed, road);
                if (!planned.ok()) return fail("the " + road.id() + " probe " + n + " cannot be assembled: " + planned.failure());
                SectorGenerator.Built a = SectorGenerator.build(level, planned, true), b = SectorGenerator.build(level, planned, true);
                if (!a.templates().equals(b.templates())) return fail("the rebuilt rooms of the " + road.id() + " probe " + n + " are not the same twice");
                List<String> ships = new ArrayList<>();
                for (Placed p : planned.result().pieces) ships.add(p.shape.id);
                for (int i = 0; i < ships.size(); i++) if (!ships.get(i).equals(a.templates().get(i))) swapped++;
                GeometryCheck.Report rep = GeometryCheck.run(level, a, 0);
                if (!rep.ok()) return fail("the geometry of the " + road.id() + " probe " + n + " built with the rebuilt rooms is wrong: " + String.join("; ", rep.failures()));
                checked += rep.pieces();
                pieces += ships.size();
                plans++;
            }
        }
        if (swapped == 0) return fail("no piece of " + plans + " probe plans was built from a rebuilt room");
        return ok("table " + table.size() + " templates, " + files + " rebuilt files agree with the shipped ones (size, sockets, markers), " + stands + " stand slots, " + plans + " probe plans: "
                + swapped + " of " + pieces + " pieces rebuilt, geometry of " + checked + " pieces checked by the game");
    }
}
