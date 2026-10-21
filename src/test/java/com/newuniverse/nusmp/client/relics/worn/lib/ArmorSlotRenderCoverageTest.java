package com.newuniverse.nusmp.client.relics.worn.lib;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression guard for "relics that go in regular armour slots are not showing on the player": EVERY item that can be worn in a vanilla armour slot has a way onto the body, derived from
 * the registration sources (the relic defs of the waves, the robe sets of NUItems, the dungeon armour sets) and the real asset files. A relic needs a worn model that loads and hangs on
 * the right body part (the painted layer under it is covered by ArmorPainterV2Test); a robe or dungeon piece needs library models whose files exist (or a layer_1 / layer_2 pair).
 */
class ArmorSlotRenderCoverageTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path ARMOR = ASSETS.resolve("textures/models/armor");

    static WornAnchor anchorOf(String model) {
        Path f = model.startsWith("vu/") ? ASSETS.resolve("visual_upgrade/models/" + model.substring(3) + ".txt") : ASSETS.resolve("relic_worn/" + model + ".txt");
        if (!Files.exists(f)) return null;
        try {
            for (String l : Files.readAllLines(f)) {
                if (l.startsWith("anchor ")) return WornAnchor.byKey(l.substring(7).trim());
                if (l.startsWith("q ")) break;
            }
        } catch (IOException e) {
            return null;
        }
        return WornAnchor.HEAD;                                                       // WornMesh.parse starts with the head anchor
    }

    static List<WornCoverage.Entry> armourRelics() throws IOException {
        List<WornCoverage.Entry> out = new ArrayList<>();
        for (WornCoverage.Entry e : WornCoverageTest.relicsFromSources()) if (ArmorSlotCoverage.isArmourSlot(e.slot())) out.add(e);
        return out;
    }

    @Test
    void theSourcesListManyArmourRelicsOfAllThreeSlots() throws IOException {
        List<WornCoverage.Entry> all = armourRelics();
        assertTrue(all.size() >= 100, "armour-slot relics found in the wave sources: " + all.size());
        for (String slot : new String[]{"HEAD", "CHEST", "FEET"}) assertTrue(all.stream().anyMatch(e -> e.slot().equals(slot)), "no " + slot + " relic found");
    }

    @Test
    void everyArmourSlotRelicHasAWornModelOnTheRightBodyPart() throws IOException {
        WornCatalog cat = WornCoverageTest.catalog();
        UpgradeCatalog up = UpgradeCatalog.parse(Files.readAllLines(ASSETS.resolve("visual_upgrade/catalog.txt")));
        List<String> bad = new ArrayList<>();
        int upgraded = 0;
        for (WornCoverage.Entry e : armourRelics()) {
            ArmorSlotCoverage.Resolution r = ArmorSlotCoverage.resolve(e.id(), e.slot(), e.look(), cat, up, ArmorSlotRenderCoverageTest::anchorOf);
            if (!r.ok()) bad.add(e.id() + " (" + e.slot() + "/" + e.look() + "): " + r.detail());
            if (r.route() == ArmorSlotCoverage.Route.UPGRADED_MODEL) upgraded++;
        }
        assertTrue(bad.isEmpty(), "armour-slot relics whose worn model never reaches the body: " + bad);
        assertTrue(upgraded > 0, "the visual upgrade catalog should dress at least one armour-slot relic");
    }

    @Test
    void theCheckNamesAModelOnTheWrongBodyPartAndAMissingFile() {
        WornCatalog cat = WornCatalog.parse(List.of("look HEAD CROWN crown_spired", "look FEET BOOTS boots_cuffed@rleg boots_cuffed@lleg", "relic bad_helm boots_cuffed", "relic gone_helm missing_model"));
        java.util.function.Function<String, WornAnchor> a = n -> n.equals("missing_model") ? null : n.startsWith("boots") ? WornAnchor.RIGHT_LEG : WornAnchor.HEAD;
        assertTrue(ArmorSlotCoverage.resolve("x", "HEAD", "CROWN", cat, UpgradeCatalog.EMPTY, a).ok());
        assertTrue(ArmorSlotCoverage.resolve("x", "FEET", "BOOTS", cat, UpgradeCatalog.EMPTY, a).ok());
        assertTrue(!ArmorSlotCoverage.resolve("bad_helm", "HEAD", "CROWN", cat, UpgradeCatalog.EMPTY, a).ok(), "boots on a helmet");
        assertTrue(!ArmorSlotCoverage.resolve("gone_helm", "HEAD", "CROWN", cat, UpgradeCatalog.EMPTY, a).ok(), "missing file");
        assertTrue(!ArmorSlotCoverage.resolve("x", "BELT", "BELT", cat, UpgradeCatalog.EMPTY, a).ok(), "no entry for the slot");
    }

    @Test
    void everyRobeAndDungeonArmourPieceHasLibraryModelsAndItsLayerFiles() throws IOException {
        String nu = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/item/NUItems.java"));
        Matcher m = Pattern.compile("= robe\\(\"([a-z0-9_]+)\"").matcher(nu);
        List<String> ids = new ArrayList<>();
        while (m.find()) ids.add(m.group(1));
        assertTrue(ids.size() >= 10, "robe sets found in NUItems: " + ids.size());
        WornCatalog cat = WornCoverageTest.catalog();
        List<String> bad = new ArrayList<>();
        for (String id : ids) {
            if (!Files.exists(ARMOR.resolve(id + "_layer_1.png"))) bad.add(id + "_layer_1.png missing");
            checkLibrary(cat, id + "_hood", ArmorLooks.Slot.HEAD, bad);
            checkLibrary(cat, id, ArmorLooks.Slot.CHEST, bad);
            checkLibrary(cat, id + "_leggings", ArmorLooks.Slot.LEGS, bad);
            checkLibrary(cat, id + "_boots", ArmorLooks.Slot.FEET, bad);
        }
        for (String set : new String[]{"drakescale", "shadowsteel"}) {
            for (int layer = 1; layer <= 2; layer++) if (!Files.exists(ARMOR.resolve("loot_" + set + "_layer_" + layer + ".png"))) bad.add("loot_" + set + "_layer_" + layer + ".png missing");
        }
        assertTrue(bad.isEmpty(), "armour items without a way onto the body: " + bad);
    }

    private static void checkLibrary(WornCatalog cat, String path, ArmorLooks.Slot slot, List<String> bad) {
        List<String> names = new ArrayList<>();
        List<WornCatalog.Piece> own = cat.itemPieces(path);
        if (!own.isEmpty()) for (WornCatalog.Piece p : own) names.add(p.model());
        else for (String s : ArmorLooks.resolve(slot, path).pieces()) names.add(WornCatalog.piece(s).model());
        if (names.isEmpty()) bad.add(path + " (" + slot + "): no library piece");
        for (String n : names) if (!WornCatalog.NONE.equals(n) && anchorOf(n) == null) bad.add(path + ": model file " + n + " missing");
    }
}
