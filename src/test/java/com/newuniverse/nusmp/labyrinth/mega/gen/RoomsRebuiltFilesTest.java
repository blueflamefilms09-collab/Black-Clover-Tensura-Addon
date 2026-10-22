package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the owner asked to KEEP the dungeon's size and rebuild the insides of its rooms, halls and hallways (docs/labyrinth/rooms_rebuilt.md). "Keep the size" is a promise that can be
 * checked on the files: every rebuilt template has exactly the footprint and height of the shipped template it replaces (so the module grid, the envelope of every room and the floor
 * height of 48 are unchanged), the same sockets (every jigsaw block at the same cell, pointing the same way, with the same name, pool, joint and priorities: the connectivity graph is made
 * of these), the same data markers (the encounters, spawns, patrols, seats ...), every container and mechanism of the room where it was, nothing but vanilla blocks and the mod's own
 * mechanism blocks, no weapon rack, no armour stand but the one slot of a stand room, and a roof that stays inside the envelope. It also pins the cost (the blocks per template) and that the
 * variants of one room really differ. Reads the real files; nothing here needs Minecraft.
 */
class RoomsRebuiltFilesTest {
    private static final Path MANIFEST = Path.of("src/main/resources/data/nusmp/mega/rooms_rebuilt.json");

    private record Entry(String shipped, int variants) {}

    private static JsonObject manifest() throws IOException {
        return JsonParser.parseString(Files.readString(MANIFEST, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static List<Entry> entries() throws IOException {
        List<Entry> out = new ArrayList<>();
        for (Map.Entry<String, com.google.gson.JsonElement> e : manifest().getAsJsonObject("variants").entrySet()) out.add(new Entry(e.getKey(), e.getValue().getAsInt()));
        return out;
    }

    private static Set<String> vanilla() throws IOException {
        Set<String> out = new HashSet<>();
        for (String line : Files.readAllLines(Path.of("tools/labyrinth/vanilla_blocks_1_21_1.txt"), StandardCharsets.UTF_8)) {
            String t = line.strip();
            if (!t.isEmpty() && !t.startsWith("#")) out.add("minecraft:" + t);
        }
        return out;
    }

    /** The mod's own blocks a template may use (mega_lib.CUSTOM). */
    private static final Set<String> CUSTOM = Set.of("nusmp:gatekeeper_door", "nusmp:peak_altar", "nusmp:relic_pedestal", "nusmp:labyrinth_exit", "nusmp:labyrinth_gate", "nusmp:sealed_archway",
            "nusmp:magic_barrier", "nusmp:ward_rune", "nusmp:spawn_glyph", "nusmp:pitfall_tile", "nusmp:snuffer_glyph", "nusmp:sigil_plate", "nusmp:riddle_stone", "nusmp:labyrinth_bricks",
            "nusmp:labyrinth_tiles", "nusmp:labyrinth_pillar", "nusmp:labyrinth_glyph", "nusmp:labyrinth_lantern", "nusmp:magicule_vein", "nusmp:guardian_altar");

    private static final Set<String> FUNCTIONAL = Set.of("minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel", "minecraft:spawner", "minecraft:trial_spawner", "minecraft:vault",
            "minecraft:ender_chest", "minecraft:shulker_box", "minecraft:hopper", "minecraft:dispenser", "minecraft:dropper", "minecraft:crafter", "minecraft:brewing_stand", "minecraft:furnace",
            "minecraft:blast_furnace", "minecraft:smoker", "minecraft:lectern", "minecraft:chiseled_bookshelf", "minecraft:decorated_pot", "minecraft:jukebox", "minecraft:beacon",
            "minecraft:enchanting_table", "minecraft:conduit");

    @Test
    void theManifestAndTheFilesAgree() throws IOException {
        List<Entry> es = entries();
        assertTrue(es.size() >= 100, "the rebuilt rooms: " + es.size() + " templates");
        Set<String> listed = new TreeSet<>();
        for (Entry e : es) {
            assertTrue(e.variants() >= 4, e.shipped() + " has " + e.variants() + " variants (at least four per template)");
            assertNotNull(TemplateFile.load(e.shipped()), "the shipped template exists: " + e.shipped());
            for (int k = 1; k <= e.variants(); k++) {
                String id = RoomVariants.variantId(e.shipped(), k);
                assertNotNull(TemplateFile.load(id), "the rebuilt file exists: " + id);
                listed.add(id);
            }
        }
        Set<String> onDisk = new TreeSet<>();
        try (Stream<Path> w = Files.walk(TemplateFile.ROOT.resolve("mega_r"))) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".nbt"))::iterator) {
                onDisk.add(TemplateFile.ROOT.relativize(p).toString().replace('\\', '/').replace(".nbt", ""));
            }
        }
        Set<String> bare = new TreeSet<>();
        for (String s : listed) bare.add(RoomVariants.bare(s));
        assertEquals(bare, onDisk, "every file under structure/mega_r is in the manifest and the other way round");
        // the skipped list names shipped templates that exist and are not in the table
        for (Map.Entry<String, com.google.gson.JsonElement> e : manifest().getAsJsonObject("skipped").entrySet()) {
            assertNotNull(TemplateFile.load(e.getKey()), "skipped template exists: " + e.getKey());
            assertFalse(es.stream().anyMatch(x -> x.shipped().equals(e.getKey())), "a skipped template has no variants: " + e.getKey());
        }
    }

    @Test
    void everyRebuiltRoomKeepsTheFootprintTheHeightTheSocketsAndTheMarkers() throws IOException {
        int files = 0;
        for (Entry e : entries()) {
            TemplateFile shipped = TemplateFile.load(e.shipped());
            Map<String, String> sockets = shipped.jigsaws(), markers = shipped.markers();
            assertEquals(48, shipped.sy, e.shipped() + " height");
            for (int k = 1; k <= e.variants(); k++) {
                String id = RoomVariants.variantId(e.shipped(), k);
                TemplateFile v = TemplateFile.load(id);
                files++;
                assertEquals(shipped.sx, v.sx, id + " width (x)");
                assertEquals(shipped.sy, v.sy, id + " height: the floor height of 48 is unchanged");
                assertEquals(shipped.sz, v.sz, id + " depth (z)");
                assertEquals(sockets, v.jigsaws(), id + ": the sockets (the connectivity graph) are the shipped room's");
                assertEquals(markers, v.markers(), id + ": the data markers (spawns, patrols, seats ...) are the shipped room's");
                assertFalse(sockets.isEmpty(), id + " has sockets");
            }
        }
        assertTrue(files >= 400, "rebuilt files checked: " + files);
    }

    @Test
    void everyContainerAndMechanismStandsWhereItDid() throws IOException {
        int checked = 0;
        for (Entry e : entries()) {
            TemplateFile shipped = TemplateFile.load(e.shipped());
            for (int k = 1; k <= e.variants(); k++) {
                TemplateFile v = TemplateFile.load(RoomVariants.variantId(e.shipped(), k));
                boolean corridor = shipped.sx == 9 && shipped.sz % 9 == 0 || shipped.sz == 9 && shipped.sx % 9 == 0;
                for (TemplateFile.Block b : shipped.blocks.values()) {
                    boolean functional = b.id().startsWith("nusmp:") || FUNCTIONAL.contains(b.id());
                    if (!functional) continue;
                    TemplateFile.Block n = v.blocks.get(b.cell());
                    if (corridor && !b.id().startsWith("nusmp:") && !b.id().equals("minecraft:chest") && !b.id().equals("minecraft:barrel") && n == null) continue;   // furniture in a corridor lane: the 7 x 7 cut removes it anyway
                    assertNotNull(n, RoomVariants.variantId(e.shipped(), k) + ": " + b.id() + " at " + b.cell() + " is gone");
                    assertEquals(b.id(), n.id(), RoomVariants.variantId(e.shipped(), k) + " at " + b.cell());
                    assertEquals(b.props(), n.props(), RoomVariants.variantId(e.shipped(), k) + " at " + b.cell());
                    assertEquals(b.nbt(), n.nbt(), RoomVariants.variantId(e.shipped(), k) + " at " + b.cell() + " block entity data");
                    checked++;
                }
            }
        }
        assertTrue(checked > 500, "functional blocks checked: " + checked);
    }

    @Test
    void onlyVanillaBlocksAndTheModsMechanismBlocksAreUsedAndNoRackAndNoStandsButTheSlot() throws IOException {
        Set<String> vanilla = vanilla();
        assertTrue(vanilla.size() > 900, "the vanilla block list is read");
        RoomVariants.Table table = RoomVariants.table();
        int slots = 0;
        for (Entry e : entries()) {
            for (int k = 1; k <= e.variants(); k++) {
                String id = RoomVariants.variantId(e.shipped(), k);
                TemplateFile v = TemplateFile.load(id);
                Set<String> bad = new TreeSet<>();
                for (TemplateFile.Block b : v.blocks.values()) {
                    if (b.id().startsWith("minecraft:") ? !vanilla.contains(b.id()) : !CUSTOM.contains(b.id())) bad.add(b.id());
                }
                assertTrue(bad.isEmpty(), id + " uses blocks that are not vanilla and not an allowed mod block: " + bad);
                assertFalse(v.blocks.values().stream().anyMatch(b -> b.id().equals("nusmp:weapon_rack")), id + " places a weapon rack");
                assertTrue(v.maxRow() <= 46, id + " reaches row " + v.maxRow() + ": the roof stays inside the 48-block envelope");
                assertTrue(v.stands() <= 1, id + " carries " + v.stands() + " armour stands");
                if (v.stands() == 1) {
                    slots++;
                    assertTrue(table.standRoom(e.shipped()), id + " carries a stand but is not a stand room");
                    assertTrue(com.newuniverse.nusmp.labyrinth.props.ArmourStandRarity.eligibleTemplate(e.shipped()), id + " is a stand room that the rarity roll can never pick");
                }
            }
        }
        assertTrue(slots >= 10, "the stand rooms carry their slot: " + slots);
        for (String stand : table.standRooms()) assertTrue(com.newuniverse.nusmp.labyrinth.props.ArmourStandRarity.eligibleTemplate(stand), stand + " must be eligible");
    }

    @Test
    void everyEligibleRebuiltRoomHasAStandSlotAndNoOtherDoes() throws IOException {
        RoomVariants.Table table = RoomVariants.table();
        for (Entry e : entries()) {
            boolean eligible = com.newuniverse.nusmp.labyrinth.props.ArmourStandRarity.eligibleTemplate(e.shipped());
            if (eligible) {
                // a stand room: at least one variant carries the slot (a niche that could not be built in one variant is allowed, not in all)
                int with = 0;
                for (int k = 1; k <= e.variants(); k++) if (TemplateFile.load(RoomVariants.variantId(e.shipped(), k)).stands() == 1) with++;
                assertTrue(with >= 1 || !table.standRoom(e.shipped()), e.shipped() + " is listed as a stand room but no variant has the slot");
            } else {
                assertFalse(table.standRoom(e.shipped()), e.shipped() + " is not an eligible template but is listed as a stand room");
            }
        }
    }

    @Test
    void theShippedRoomsOfTheKindsThatAreNotRebuiltAreUntouched() throws IOException {
        // the boss chain, the safe rooms, stairs, trials, traps, puzzles, caverns, set pieces, landings and the Way Onward are KEPT: no variant exists for them
        Set<String> rebuilt = new HashSet<>();
        for (Entry e : entries()) rebuilt.add(e.shipped());
        for (String keep : List.of("mega/antechamber", "mega/arena", "mega/cap", "mega/cavern", "mega/landing", "mega/onward", "mega/puzzle", "mega/safe", "mega/set", "mega/stairwell", "mega/trap",
                "mega/trial", "mega/vault")) {
            for (String r : rebuilt) assertFalse(r.startsWith(keep + "/"), r + " is a kept piece");
        }
    }

    @Test
    void theVariantsOfOneRoomDifferFromEachOtherAndFromTheShippedRoom() throws IOException {
        for (Entry e : entries()) {
            Set<Long> prints = new HashSet<>();
            prints.add(TemplateFile.load(e.shipped()).fingerprint());
            for (int k = 1; k <= e.variants(); k++) {
                long f = TemplateFile.load(RoomVariants.variantId(e.shipped(), k)).fingerprint();
                assertTrue(prints.add(f), RoomVariants.variantId(e.shipped(), k) + " is the same room as another variant or as the shipped one");
            }
        }
    }

    @Test
    void theCostOfARebuiltRoomStaysNearTheShippedOne() throws IOException {
        double sumShipped = 0, sumRebuilt = 0, worst = 0;
        String worstName = "";
        TreeMap<String, double[]> byFolder = new TreeMap<>();
        for (Entry e : entries()) {
            int shipped = TemplateFile.load(e.shipped()).blocks.size();
            for (int k = 1; k <= e.variants(); k++) {
                int n = TemplateFile.load(RoomVariants.variantId(e.shipped(), k)).blocks.size();
                double ratio = n / (double) shipped;
                if (ratio > worst) {
                    worst = ratio;
                    worstName = RoomVariants.variantId(e.shipped(), k);
                }
                sumShipped += shipped;
                sumRebuilt += n;
                String folder = e.shipped().split("/")[1];
                double[] t = byFolder.computeIfAbsent(folder, x -> new double[2]);
                t[0] += shipped;
                t[1] += n;
                assertTrue(ratio <= 1.45, RoomVariants.variantId(e.shipped(), k) + " has " + n + " blocks, x" + String.format(Locale.ROOT, "%.2f", ratio) + " of the shipped " + shipped);
            }
        }
        double mean = sumRebuilt / sumShipped;
        StringBuilder line = new StringBuilder();
        for (Map.Entry<String, double[]> f : byFolder.entrySet()) line.append(f.getKey()).append(" x").append(String.format(Locale.ROOT, "%.2f", f.getValue()[1] / f.getValue()[0])).append("  ");
        System.out.println(String.format(Locale.ROOT, "ROOMS REBUILT cost: %.0f blocks shipped, %.0f rebuilt, mean x%.3f, worst x%.2f (%s); by folder: %s", sumShipped, sumRebuilt, mean, worst, worstName, line));
        assertTrue(mean <= 1.25, "generation cost grows at most about a quarter: x" + mean);
    }
}
