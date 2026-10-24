package com.newuniverse.nusmp.client.relics.worn.lib;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** The generic fallback of the armour models, the item entries of the catalog, and the coverage of every armour item and armour relic of the mod. */
class ArmorLooksTest {
    static final Path ROOT = Path.of("src/main/resources/assets/nusmp/relic_worn");
    static final Path JAVA = Path.of("src/main/java/com/newuniverse/nusmp");

    static Set<String> modelNames() throws IOException {
        Set<String> out = new HashSet<>();
        try (Stream<Path> s = Files.list(ROOT)) {
            s.forEach(p -> {
                String n = p.getFileName().toString();
                if (n.endsWith(".txt") && !n.equals("catalog.txt")) out.add(n.substring(0, n.length() - 4));
            });
        }
        return out;
    }

    @Test
    void stylesComeFromTheWordsOfThePath() {
        assertEquals(ArmorLooks.Style.ROBE, ArmorLooks.styleOf("future_mage_robe"));
        assertEquals(ArmorLooks.Style.ROBE, ArmorLooks.styleOf("storm_cloak_leggings"));
        assertEquals(ArmorLooks.Style.ROBE, ArmorLooks.styleOf("mage_armor"), "a robe word wins over a plate word");
        assertEquals(ArmorLooks.Style.PLATE, ArmorLooks.styleOf("adamantite_helmet"));
        assertEquals(ArmorLooks.Style.PLATE, ArmorLooks.styleOf("knights_greaves"));
        assertEquals(ArmorLooks.Style.CLOTH, ArmorLooks.styleOf("traveller_boots"));
        assertEquals(ArmorLooks.Style.CLOTH, ArmorLooks.styleOf(null));
        assertEquals(ArmorLooks.Style.CLOTH, ArmorLooks.styleOf(""));
    }

    @Test
    void everySlotAndStyleHasPiecesThatExistAsModels() throws IOException {
        Set<String> files = modelNames();
        for (ArmorLooks.Slot slot : ArmorLooks.Slot.values()) {
            for (ArmorLooks.Style style : ArmorLooks.Style.values()) {
                List<String> p = ArmorLooks.pieces(slot, style);
                assertFalse(p.isEmpty(), slot + " " + style);
                for (String s : p) assertTrue(files.contains(WornCatalog.piece(s).model()), slot + " " + style + " names the missing model " + s);
            }
        }
        assertEquals(ArmorLooks.pieces(ArmorLooks.Slot.CHEST, ArmorLooks.Style.CLOTH), ArmorLooks.pieces(null, null),
                "a null slot is the chest, a null style is cloth");
    }

    @Test
    void thePaletteIsStableDistinctAndInRange() {
        int[] a = ArmorLooks.palette("future_mage_robe"), b = ArmorLooks.palette("future_mage_robe"), c = ArmorLooks.palette("future_war_helmet");
        assertArrayEquals(a, b, "the same path gives the same colours every time");
        assertFalse(java.util.Arrays.equals(a, c), "two paths do not look the same");
        for (int[] pal : new int[][]{a, c, ArmorLooks.palette(null), ArmorLooks.palette("")}) {
            assertEquals(3, pal.length);
            for (int col : pal) assertTrue(col >= 0 && col <= 0xFFFFFF);
            assertTrue(luma(pal[0]) < luma(pal[1]), "the trim is lighter than the body");
            assertTrue(luma(pal[2]) >= luma(pal[0]), "the glow is brighter than the body");
        }
        assertEquals(0xFF0000, ArmorLooks.hsv(0f, 1f, 1f));
        assertEquals(0x00FF00, ArmorLooks.hsv(1f / 3f, 1f, 1f));
        assertEquals(0x0000FF, ArmorLooks.hsv(2f / 3f, 1f, 1f));
        assertEquals(0xFF0000, ArmorLooks.hsv(1f, 1f, 1f), "the hue wraps");
        assertEquals(0xFFFFFF, ArmorLooks.hsv(0.4f, 0f, 1f));
    }

    static int luma(int rgb) { return (rgb >> 16 & 255) * 3 + (rgb >> 8 & 255) * 6 + (rgb & 255); }

    @Test
    void resolveAndSlotOfType() {
        ArmorLooks.Generic g = ArmorLooks.resolve(ArmorLooks.Slot.HEAD, "new_war_helm");
        assertEquals(ArmorLooks.Style.PLATE, g.style());
        assertEquals("helm_war", g.pieces().get(0));
        assertEquals(2, g.tier(), "an unknown item shows no grade effects beyond its model");
        assertEquals(ArmorLooks.Slot.HEAD, ArmorLooks.slotOfType("helmet"));
        assertEquals(ArmorLooks.Slot.CHEST, ArmorLooks.slotOfType("CHESTPLATE"));
        assertEquals(ArmorLooks.Slot.LEGS, ArmorLooks.slotOfType("leggings"));
        assertEquals(ArmorLooks.Slot.FEET, ArmorLooks.slotOfType("boots"));
        assertNull(ArmorLooks.slotOfType("body"));
        assertNull(ArmorLooks.slotOfType(null));
        assertEquals("ARMOR_FEET", ArmorLooks.Slot.FEET.key());
    }

    @Test
    void theCatalogParsesItemLinesAndIgnoresBrokenOnes() {
        WornCatalog c = WornCatalog.parse(List.of("item robe chest_x sleeve@rarm sleeve@larm~", "ipal robe 112233 AABBCC 00FF80", "itier robe 9", "itier bad x", "ipal bad zz 1 2", "item", "item lonely",
                "item mode_wind wing@body", "fx robe ember"));
        assertEquals(3, c.itemPieces("robe").size());
        assertTrue(c.itemPieces("robe").get(2).mirror());
        assertEquals(WornAnchor.LEFT_ARM, c.itemPieces("robe").get(2).anchor());
        assertArrayEquals(new int[]{0x112233, 0xAABBCC, 0x00FF80}, c.itemPalette("robe"));
        assertEquals(7, c.itemTier("robe", 2), "a tier above 7 is clamped");
        assertEquals(2, c.itemTier("bad", 2), "an unreadable tier is ignored");
        assertNull(c.itemPalette("bad"));
        assertTrue(c.itemPieces("lonely").isEmpty());
        assertEquals(1, c.itemPieces("mode_wind").size());
        assertEquals(2, c.itemCount());
        assertEquals("ember", c.themeOf("robe"));
        assertTrue(c.itemPieces("nothing").isEmpty());
        assertEquals(-1, WornCatalog.hex("xyz"));
        assertEquals(0xFF00FF, WornCatalog.hex("FF00FF"));
    }

    // ================================================================ the real catalog and the real item lists
    static WornCatalog real() throws IOException { return WornCatalog.parse(Files.readAllLines(ROOT.resolve("catalog.txt"))); }

    @Test
    void everyItemEntryNamesExistingModelsAndStaysUnderTheBudget() throws IOException {
        Set<String> files = modelNames();
        Map<String, Integer> verts = new HashMap<>();
        for (String n : files) verts.put(n, WornMesh.parse(n, Files.readAllLines(ROOT.resolve(n + ".txt"))).solidVertices());
        int items = 0;
        for (String line : Files.readAllLines(ROOT.resolve("catalog.txt"))) {
            String[] v = line.trim().split("\\s+");
            if (!v[0].equals("item") || v.length < 3) continue;
            int total = 0;
            for (int i = 2; i < v.length; i++) {
                String model = WornCatalog.piece(v[i]).model();
                if (WornCatalog.NONE.equals(model)) continue;                       // an anime robe's legs and boots draw nothing
                assertTrue(files.contains(model), line + ": " + model + " has no model file");
                total += verts.get(model);
            }
            assertTrue(total <= 2600, v[1] + " wears " + total + " vertices (budget 2600)");
            items++;
        }
        assertTrue(items >= 44 + 6, "the robe sets and the modes have entries: " + items);
        WornCatalog c = real();
        for (String key : new String[]{"black_bull_robe", "devil_bound_coat"}) {
            assertNotNull(c.itemPalette(key), key + " has a palette");
            assertTrue(c.itemTier(key, 0) >= 1);
        }
    }

    /** The robe sets registered in NUItems: id of each {@code robe("id", ...)} call. */
    static List<String> robeSetIds() throws IOException {
        List<String> ids = new ArrayList<>();
        Matcher m = Pattern.compile("robe\\(\"([a-z0-9_]+)\",\\s*RobeItem\\.Kind").matcher(Files.readString(JAVA.resolve("item/NUItems.java")));
        while (m.find()) ids.add(m.group(1));
        return ids;
    }

    @Test
    void everyPieceOfEveryRobeSetHasACatalogEntry() throws IOException {
        List<String> ids = robeSetIds();
        assertTrue(ids.size() >= 11, "found the robe sets: " + ids);
        WornCatalog c = real();
        for (String id : ids) {
            for (String suffix : new String[]{"_hood", "", "_leggings", "_boots"}) {
                assertFalse(c.itemPieces(id + suffix).isEmpty(), id + suffix + " has worn models");
                assertNotNull(c.itemPalette(id + suffix), id + suffix + " has a palette");
            }
            assertEquals(1, c.itemPieces(id + "_hood").size(), "a hood is one head piece");
            assertTrue(c.itemPieces(id).size() >= 3, "a robe has a body and two sleeves");
            assertEquals(3, c.itemPieces(id + "_leggings").size(), "a hip piece and two legs");
            assertEquals(2, c.itemPieces(id + "_boots").size(), "two boots");
        }
    }

    @Test
    void theAnimeLookCoversEveryPieceOfEveryRobeSet() throws IOException {
        WornCatalog c = real();
        Set<String> files = modelNames();
        for (String id : robeSetIds()) {
            for (String suffix : new String[]{"_hood", "", "_leggings", "_boots"}) {
                String key = WornCatalog.animeKey(id + suffix);
                assertTrue(c.hasItem(key), key + " has an anime entry");
                assertFalse(c.itemPieces(key).isEmpty(), key);
                assertNotNull(c.itemPalette(key), key + " has a palette");
                assertTrue(c.hasItem(id + suffix), "the older look of " + id + suffix + " is kept");
                for (WornCatalog.Piece p : c.itemPieces(key)) assertTrue(WornCatalog.NONE.equals(p.model()) || files.contains(p.model()), key + " names " + p.model());
            }
            WornCatalog.Piece cloak = c.itemPieces(WornCatalog.animeKey(id)).get(0);
            assertEquals(1, c.itemPieces(WornCatalog.animeKey(id)).size(), "the chest piece is the cloak and nothing else (no sleeves, no hip band)");
            assertTrue(cloak.model().startsWith("anime_cloak_"), id + " wears an anime cloak");
            assertTrue(WornCatalog.isNothing(c.itemPieces(WornCatalog.animeKey(id + "_leggings"))), "the player's own legs show");
            assertTrue(WornCatalog.isNothing(c.itemPieces(WornCatalog.animeKey(id + "_boots"))), "the player's own boots show");
            assertTrue(c.itemPieces(WornCatalog.animeKey(id + "_hood")).size() == 1, "at most one small head piece");
        }
    }

    /** 3.1.2 regression: since 1.48.0 only the robe (chest) showed, because the anime entries of leggings, boots and most helmets are `none`. */
    @Test
    void everyPieceOfARobeSetShowsAModelNotOnlyTheChest() throws IOException {
        WornCatalog c = real();
        for (String id : robeSetIds()) {
            assertTrue(c.wearsAnime(id, true, false), id + ": the chest piece keeps the anime cloak");
            for (String suffix : new String[]{"_leggings", "_boots"}) {
                assertFalse(c.wearsAnime(id + suffix, true, false), id + suffix + " must wear its older model, not the empty anime entry");
                assertFalse(WornCatalog.isNothing(c.itemPieces(id + suffix)), id + suffix + " older model draws something");
                assertTrue(c.wearsAnime(id + suffix, true, true), id + suffix + ": option relicfx.robeAnimeBare restores the 1.48.0 bare legs");
                assertFalse(c.wearsAnime(id + suffix, false, true), "robeAnime off: older look");
            }
            String hood = id + "_hood";
            boolean animeHead = !WornCatalog.isNothing(c.itemPieces(WornCatalog.animeKey(hood)));
            assertEquals(animeHead, c.wearsAnime(hood, true, false), hood + ": a real anime head piece stays, a `none` one falls back to the older hood");
            assertFalse(WornCatalog.isNothing(c.itemPieces(animeHead ? WornCatalog.animeKey(hood) : hood)), hood + " draws something");
        }
    }

    @Test
    void theAnimeKeysAndTheNothingPieceAreParsedAsData() {
        assertEquals("anime:black_bull_robe", WornCatalog.animeKey("black_bull_robe"));
        WornCatalog c = WornCatalog.parse(List.of("item anime:x_boots none", "item anime:x cloak_a", "item x chest_old"));
        assertTrue(c.hasItem("anime:x_boots"));
        assertTrue(WornCatalog.isNothing(c.itemPieces("anime:x_boots")));
        assertFalse(WornCatalog.isNothing(c.itemPieces("anime:x")));
        assertFalse(WornCatalog.isNothing(List.of()), "no entry is not the same as an entry that draws nothing");
        assertFalse(c.hasItem("anime:y"));
        assertEquals(1, c.itemPieces("x").size(), "the older look is stored apart from the anime one");
    }

    /** The ids of the armour relics (HEAD, CHEST, FEET) from the Java sources, with their look (null: the default of the slot). */
    static Map<String, String[]> armourRelics() throws IOException {
        Map<String, String> consts = new HashMap<>();
        List<Path> files = new ArrayList<>();
        try (Stream<Path> s = Files.walk(JAVA.resolve("relics"))) {
            s.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
        }
        Pattern constant = Pattern.compile("\\b(ID_\\w+)\\s*=\\s*\"([a-z0-9_]+)\"");
        for (Path f : files) {
            Matcher m = constant.matcher(Files.readString(f));
            while (m.find()) consts.put(m.group(1), m.group(2));
        }
        Pattern def = Pattern.compile("RelicDef\\.of\\(\\s*(?:\"([a-z0-9_]+)\"|[\\w.]*?(ID_\\w+))\\s*,\\s*\"[^\"]*\"\\s*,\\s*Grade\\.\\w+\\s*,\\s*RelicSlot\\.(HEAD|CHEST|FEET)\\s*\\)");
        Pattern look = Pattern.compile("\\.look\\(Look\\.(\\w+)\\)");
        Map<String, String[]> out = new HashMap<>();
        for (Path f : files) {
            String src = Files.readString(f);
            Matcher m = def.matcher(src);
            while (m.find()) {
                String id = m.group(1) != null ? m.group(1) : consts.get(m.group(2));
                int end = src.indexOf(';', m.end());
                Matcher lm = look.matcher(src.substring(m.end(), end < 0 ? src.length() : end));
                out.put(id, new String[]{m.group(3), lm.find() ? lm.group(1) : null});
            }
        }
        return out;
    }

    @Test
    void everyArmourRelicHasASignatureModelOrItsLookHasOne() throws IOException {
        WornCatalog c = real();
        Map<String, String[]> defs = armourRelics();
        assertTrue(defs.size() >= 40, "found the armour relics: " + defs.size());
        Map<String, String> dflt = Map.of("HEAD", "CIRCLET", "CHEST", "PLATE", "FEET", "BOOTS");
        List<String> generic = new ArrayList<>();
        for (Map.Entry<String, String[]> e : defs.entrySet()) {
            String slot = e.getValue()[0], look = e.getValue()[1] != null ? e.getValue()[1] : dflt.get(slot);
            assertFalse(c.piecesFor(e.getKey(), slot, look).isEmpty(), e.getKey() + " (" + slot + " " + look + ") has no worn model at all");
            if (c.piecesFor(e.getKey(), "NO", "NO").isEmpty()) generic.add(e.getKey());
        }
        // the boss crown has its own renderer (WornRenderers); every other armour relic has a signature entry
        generic.remove("dante_gravity_crown");
        assertTrue(generic.isEmpty(), "armour relics that only have the model of their look: " + generic);
    }
}
