package com.newuniverse.nusmp.compat;

import com.newuniverse.nusmp.compat.CompatCodexModel.Item;
import com.newuniverse.nusmp.compat.CompatCodexModel.Type;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The Compatibility Codex screen's content and paging (pure), and its grimoire look. */
class CompatCodexModelTest {
    private static final Set<String> ALL = Set.of("neoforge", "tensura", "manascore_skill", "manascore_storage", "architectury", "geckolib",
            "curios", "bettercombat", "ftbteams", "trnightmare", "iceandfire", "tensuranoindex");

    @Test
    void everyCatalogEntryIsOneEntryRowWithItsStatusId() {
        List<Item> items = CompatCodexModel.items(ALL::contains);
        List<String> ids = new ArrayList<>();
        for (Item it : items) if (it.type() == Type.ENTRY) ids.add(it.modId());
        List<String> expected = new ArrayList<>();
        for (CompatCatalog.Entry e : CompatCatalog.entries()) if (e.kind() != CompatCatalog.Kind.BRIDGE) expected.add(e.modId());
        assertEquals(expected, ids);
        for (Item it : items) if (it.type() == Type.ENTRY) assertEquals(CompatCatalog.entry(it.modId()).name(), it.text());
    }

    @Test
    void textIsTheBooksNotACopy() {
        List<Item> items = CompatCodexModel.items(id -> false);
        StringBuilder all = new StringBuilder();
        for (Item it : items) all.append(it.text()).append('\n').append(it.detail()).append('\n');
        String s = all.toString().replaceAll("\\s+", " ");
        for (CompatBook.Block b : CompatBook.blocks(id -> false)) {
            for (String line : b.text().split("\n")) {
                String l = line.trim();
                if (l.isEmpty() || l.contains(": Not installed")) continue;      // the chip says that part
                if (b.newPage() && l.equals(CompatBook.ABSENT)) continue;
                assertTrue(s.contains(l.replaceAll("\\s+", " ")), "missing from the screen: " + l);
            }
        }
        assertTrue(s.contains("Not one line of this was run"), "the plain statement that nothing was run");
    }

    @Test
    void sectionsStartPagesAndTheBridgeHeadingCarriesTheChip() {
        List<Item> items = CompatCodexModel.items(ALL::contains);
        assertEquals(Type.HEADING, items.get(0).type());
        assertEquals(CompatBook.TITLE, items.get(0).text());
        Item bridge = null;
        for (Item it : items) if (it.type() == Type.HEADING && CompatCatalog.TNI_MODID.equals(it.modId())) bridge = it;
        assertNotNull(bridge);
        assertTrue(bridge.newPage());
        int idx = items.indexOf(bridge);
        assertFalse(items.get(idx + 1).text().startsWith(CompatBook.LOADED), "the status word is not repeated as text");
    }

    @Test
    void statusFollowsTheModListAndCountsLoaded() {
        assertEquals(CompatCatalog.entries().size(), CompatCodexModel.loadedCount(ALL::contains));
        assertEquals(0, CompatCodexModel.loadedCount(null));
        assertEquals(1, CompatCodexModel.loadedCount("tensura"::equals));
    }

    private static int fixed(Item it) { return it.type() == Type.HEADING ? 14 : it.type() == Type.ENTRY ? 30 : 60; }

    @Test
    void noPageOverflowsAndNothingIsLostOrReordered() {
        List<Item> items = CompatCodexModel.items(ALL::contains);
        for (int pageH : new int[]{120, 170, 200, 400}) {
            List<List<Item>> pages = CompatCodexModel.paginate(items, CompatCodexModelTest::fixed, pageH, 5);
            List<Item> flat = new ArrayList<>();
            for (List<Item> p : pages) {
                assertFalse(p.isEmpty(), "no empty page");
                int used = 0;
                for (Item it : p) { used += fixed(it) + (used == 0 ? 0 : 5); flat.add(it); }
                if (p.size() > 1) assertTrue(used <= pageH, "page overflows at " + pageH + ": " + used);
                for (int i = 1; i < p.size(); i++) assertFalse(p.get(i).newPage(), "a section start sits first on its page");
            }
            assertEquals(items, flat, "same items in the same order");
        }
    }

    @Test
    void aHeadingIsNeverAloneAtTheFootOfAPage() {
        List<Item> items = CompatCodexModel.items(ALL::contains);
        for (int pageH : new int[]{90, 111, 150, 187}) {
            List<List<Item>> pages = CompatCodexModel.paginate(items, CompatCodexModelTest::fixed, pageH, 5);
            for (List<Item> p : pages) {
                assertFalse(p.size() > 1 && p.get(p.size() - 1).type() == Type.HEADING, "heading at the foot at " + pageH);
                if (pageH >= 150) assertFalse(p.size() == 1 && p.get(0).type() == Type.HEADING, "heading alone on a page at " + pageH);
            }
        }
    }

    @Test
    void oversizedItemGetsItsOwnPageAndEmptyInputGivesOnePage() {
        Item big = new Item(Type.TEXT, "x", "", null, false), small = new Item(Type.ENTRY, "y", "", "m", false);
        List<List<Item>> pages = CompatCodexModel.paginate(List.of(small, big, small), it -> it == big ? 500 : 10, 100, 5);
        assertEquals(3, pages.size());
        assertEquals(List.of(big), pages.get(1));
        assertEquals(1, CompatCodexModel.paginate(List.of(), CompatCodexModelTest::fixed, 100, 5).size());
    }

    @Test
    void spreadsAreTwoPagesEach() {
        assertEquals(1, CompatCodexModel.spreads(0));
        assertEquals(1, CompatCodexModel.spreads(2));
        assertEquals(2, CompatCodexModel.spreads(3));
        assertEquals(3, CompatCodexModel.spreads(6));
    }

    @Test
    void theCodexLookUsesTheGrimoireCoreAndItsTexturesExist() {
        var look = CompatCodexLook.LOOK;
        assertEquals(com.newuniverse.nusmp.grimoire.BookMotif.COMPAT, look.motif());
        assertEquals(CompatCodexLook.COVER, look.tint(0));
        assertEquals(CompatCodexLook.TRIM, look.tint(1));
        assertEquals(CompatCodexLook.EMBLEM, look.tint(2));
        assertEquals(0xFF000000 | CompatCodexLook.COVER, CompatCodexLook.argb(0));
        assertEquals(-1, CompatCodexLook.argb(7));
        assertFalse(look.tattered());
        var plan = com.newuniverse.nusmp.grimoire.GrimoireBookPlan.build(look.key(true));
        assertFalse(plan.isEmpty());
        assertTrue(com.newuniverse.nusmp.grimoire.GrimoireBookPlan.textures().contains("emblem_compat"));
        boolean emblem = false, art = false, base = false;
        for (var q : plan) {
            emblem |= q.texture().equals("emblem_compat");
            art |= q.texture().equals("cover_art_compat");
            base |= q.texture().equals("cover_base_compat");
            assertTrue(com.newuniverse.nusmp.grimoire.GrimoireBookPlan.textures().contains(q.texture()), "unknown texture " + q.texture());
        }
        assertTrue(emblem && art && base, "the rings emblem and the cover design are on the book");
        File dir = new File("src/main/resources/assets/nusmp/textures/item/grimoire_book");
        for (String t : new String[]{"emblem_compat", "cover_art_compat", "cover_base_compat"})
            assertTrue(new File(dir, t + ".png").isFile(), "missing texture " + t);
        assertTrue(new File("src/main/resources/assets/nusmp/textures/item/compat_codex.png").isFile());
        assertTrue(new File("src/main/resources/assets/nusmp/models/item/compat_codex.json").isFile());
    }
}
