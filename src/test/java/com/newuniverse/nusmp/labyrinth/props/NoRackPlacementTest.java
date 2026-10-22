package com.newuniverse.nusmp.labyrinth.props;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the owner asked for the weapon racks to be deleted from the dungeon, and deleting the registered block would break existing worlds, so the block, its item, its block entity and its
 * renderer STAY registered and working while the generator never places one. This pins both halves on the sources and the data: with the defaults no rack placement path is reachable (the
 * old keys default to false, the converter is guarded by the legacy path, nothing else in the mod places a rack, no template and no room loot table names the rack), the rack is still
 * registered, and the armour stand has the config keys the owner asked for with sane defaults.
 */
class NoRackPlacementTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) throws IOException { return Files.readString(SRC.resolve(rel), StandardCharsets.UTF_8); }

    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", ""); }

    @Test
    void theOldRackKeysDefaultToOffAndTheNewStandKeysExistWithSmallDefaults() throws IOException {
        String cfg = code(read("labyrinth/props/PropsConfig.java"));
        assertTrue(cfg.contains(".define(\"rackInsteadOfArmourStand\", false)"), "the old flag must default to false");
        assertTrue(cfg.contains(".define(\"legacyRackConversion\", false)"), "the legacy rack path must default to false");
        assertTrue(cfg.contains(".defineInRange(\"armourStandRarity\", 6.0, 0.0, 100.0)"), "the stand chance is a percent with a small default");
        assertTrue(cfg.contains(".defineInRange(\"armourStandFloorCap\", 2, 0, 8)"), "the per-floor cap is small by default");
        assertTrue(cfg.contains("get(LEGACY_RACK_CONVERSION) && get(RACK_INSTEAD_OF_STAND)"), "both keys must be on for a rack");
    }

    @Test
    void theConverterIsGuardedByTheLegacyPathAndSkipsTheRareStand() throws IOException {
        String conv = code(read("labyrinth/props/RackConverter.java"));
        assertTrue(conv.contains("RackPlacement.shouldConvert(PropsConfig.legacyRackConversion()"), "onJoin must ask the legacy path");
        assertFalse(conv.contains("shouldConvert(PropsConfig.rackInsteadOfStand()"), "the old flag alone must not convert");
        assertTrue(conv.contains("ArmourStandGate.RARE_TAG"), "the rare stand is never converted");
    }

    @Test
    void nothingButThePropsPackagePlacesARack() throws IOException {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> w = Files.walk(SRC)) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String rel = SRC.relativize(p).toString().replace('\\', '/');
                if (rel.startsWith("labyrinth/props/") || rel.startsWith("client/labyrinth/props/")) continue;
                String c = code(Files.readString(p, StandardCharsets.UTF_8));
                if (c.contains("PropsRegistry.RACK") || c.contains("RackConverter.") || c.contains("WeaponRackBlock")) bad.add(rel);
            }
        }
        assertTrue(bad.isEmpty(), "code outside the props package touches the weapon rack: " + bad);
    }

    @Test
    void inThePropsPackageOnlyTheConverterAndTheSelfTestsPlaceARack() throws IOException {
        List<String> placers = new ArrayList<>();
        try (Stream<Path> w = Files.list(SRC.resolve("labyrinth/props"))) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String c = code(Files.readString(p, StandardCharsets.UTF_8));
                if (c.contains("RACK.get().defaultBlockState()")) placers.add(p.getFileName().toString());
            }
        }
        placers.sort(String::compareTo);
        assertTrue(placers.equals(List.of("PropsSelfTests.java", "RackConverter.java")), "files that build a rack: " + placers);
    }

    @Test
    void theRackBlockItemEntityAndRendererStayRegisteredForOldWorlds() throws IOException {
        String reg = code(read("labyrinth/props/PropsRegistry.java"));
        assertTrue(reg.contains("BLOCKS.register(\"weapon_rack\""), "the block stays registered");
        assertTrue(reg.contains("NUItems.ITEMS.register(\"weapon_rack\""), "the item stays registered");
        assertTrue(reg.contains("BLOCK_ENTITIES.register(\"weapon_rack\""), "the block entity stays registered");
        assertTrue(Files.exists(SRC.resolve("client/labyrinth/props/WeaponRackRenderer.java")), "the renderer stays");
        assertTrue(Files.exists(Path.of("src/main/resources/data/nusmp/loot_table/racks/armoury.json")), "the rack loot tables stay");
    }

    @Test
    void noTemplateAndNoRoomLootTablePlacesOrNamesTheRack() throws IOException {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> w = Files.walk(Path.of("src/main/resources/data/nusmp/structure"))) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".nbt"))::iterator) {
                byte[] raw = Files.readAllBytes(p);
                try (java.util.zip.GZIPInputStream in = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(raw))) {
                    String s = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
                    if (s.contains("nusmp:weapon_rack")) bad.add(p.toString());
                }
            }
        }
        assertTrue(bad.isEmpty(), "templates that contain the weapon rack: " + bad);
        List<String> loot = new ArrayList<>();
        try (Stream<Path> w = Files.walk(Path.of("src/main/resources/data/nusmp/loot_table"))) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".json"))::iterator) {
                String rel = Path.of("src/main/resources/data/nusmp/loot_table").relativize(p).toString().replace('\\', '/');
                if (rel.startsWith("racks/") || rel.equals("blocks/weapon_rack.json")) continue;     // the rack's own tables and the drop of the rack block (old racks and crafted racks use them)
                if (Files.readString(p, StandardCharsets.UTF_8).contains("weapon_rack")) loot.add(rel);
            }
        }
        assertTrue(loot.isEmpty(), "loot tables outside racks/ that name the weapon rack: " + loot);
    }
}
