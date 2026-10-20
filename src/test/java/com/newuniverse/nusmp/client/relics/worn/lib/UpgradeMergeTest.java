package com.newuniverse.nusmp.client.relics.worn.lib;

import com.newuniverse.nusmp.client.relics.vu.VuOverrides;
import com.newuniverse.nusmp.relics.upgrade.UpgradeCaps;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The two upgrade slices that share the worn relic library (God worn, 1.102.0; Charms / Genesis, 1.104.0) were merged in 1.106.0 into one model: this test pins what must stay true. Every quad
 * flag has its own bit and the generators write the same numbers as the Java parser; no relic is in both override tables; the lookup order, the LOD rule, the caps and the single switch are
 * the ones documented in docs/visual_upgrade/worn_merge.md.
 */
class UpgradeMergeTest {
    static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    static final Path UPG = ASSETS.resolve("visual_upgrade");
    static final Path TOOLS = Path.of("tools/visual_upgrade");
    static final String[] FLAGS = {"EMIT", "SHELL", "HOT", "GLOWMAP", "STAR", "HALO", "LIT", "ADD"};

    static int flag(String name) throws ReflectiveOperationException {
        Field f = WornMesh.class.getField(name);
        return f.getInt(null);
    }

    @Test
    void everyQuadFlagHasItsOwnBitAndFitsAByte() throws ReflectiveOperationException {
        Set<Integer> seen = new HashSet<>();
        int all = 0;
        for (String n : FLAGS) {
            int v = flag(n);
            assertEquals(1, Integer.bitCount(v), n + " is a single bit");
            assertTrue(v <= 128, n + " fits the byte the mesh stores its flags in");
            assertTrue(seen.add(v), n + " shares its bit with another flag");
            all |= v;
        }
        assertEquals(255, all, "the eight flags use the eight bits of the byte, none twice");
        // the released values of the Charms / Genesis slice stay as they were
        assertEquals(8, WornMesh.GLOWMAP);
        assertEquals(16, WornMesh.STAR);
        assertEquals(32, WornMesh.HALO);
    }

    @Test
    void aHighBitFlagSurvivesTheByteOfTheMesh() {
        String q = "q 0 0 %d 0 0 0 0 0 0 -1 0 1 0 0 0 0 0 -1 0 1 1 0 0 0 0 -1 0 0 1 0 0 0 0 -1 0";
        WornMesh m = WornMesh.parse("t", List.of("model t", "anchor body", "skin demo", String.format(q, WornMesh.ADD), String.format(q, WornMesh.LIT), String.format(q, WornMesh.GLOWMAP),
                String.format(q, WornMesh.STAR), String.format(q, WornMesh.HALO), String.format(q, 0)));
        int add = 0, lit = 0;
        for (byte f : m.flags) {
            if ((f & WornMesh.ADD) != 0) add++;
            if ((f & WornMesh.LIT) != 0) lit++;
        }
        assertEquals(1, add, "the ADD bit (128) is a negative byte and must still be found");
        assertEquals(1, lit);
        assertEquals(2, m.emitQuads, "ADD and LIT quads draw in the lit-glow pass");
        assertEquals(1, m.glowMapQuads);
        assertEquals(1, m.starQuads);
        assertEquals(1, m.haloQuads);
    }

    @Test
    void theGeneratorsWriteTheSameBitsAsTheParser() throws IOException, ReflectiveOperationException {
        Pattern p = Pattern.compile("(?m)^(EMIT(?:, [A-Z]+)+) = ([0-9, ]+)");
        int files = 0;
        for (String dir : new String[]{"god_worn", "charms_genesis_worn"}) {
            try (Stream<Path> s = Files.list(TOOLS.resolve(dir))) {
                for (Path f : s.filter(x -> x.toString().endsWith(".py")).toList()) {
                    Matcher m = p.matcher(Files.readString(f));
                    while (m.find()) {
                        String[] names = m.group(1).split(", ");
                        String[] vals = m.group(2).trim().split(", ");
                        assertEquals(names.length, vals.length, f.toString());
                        for (int i = 0; i < names.length; i++) {
                            assertEquals(flag(names[i]), Integer.parseInt(vals[i].trim()), names[i] + " in " + f);
                        }
                        files++;
                    }
                }
            }
        }
        assertTrue(files >= 5, "the flag tables of both generators were found: " + files);
    }

    @Test
    void eachFamilyOfModelFilesUsesOnlyItsOwnFlags() throws IOException {
        int own = WornMesh.EMIT | WornMesh.SHELL | WornMesh.HOT;
        int godOnly = own | WornMesh.LIT | WornMesh.ADD, charmsOnly = own | WornMesh.GLOWMAP | WornMesh.STAR | WornMesh.HALO;
        int godFiles = 0, charmsFiles = 0;
        try (Stream<Path> s = Files.list(ASSETS.resolve("relic_worn"))) {
            for (Path f : s.filter(x -> x.getFileName().toString().startsWith("gw_")).toList()) {
                for (String line : Files.readAllLines(f)) {
                    if (!line.startsWith("q ")) continue;
                    int fl = Integer.parseInt(line.split("\\s+")[3]);
                    assertEquals(0, fl & ~godOnly, f + " sets a flag of the other slice: " + fl);
                }
                godFiles++;
            }
        }
        try (Stream<Path> s = Files.list(UPG.resolve("models"))) {
            for (Path f : s.filter(x -> x.toString().endsWith(".txt")).toList()) {
                for (String line : Files.readAllLines(f)) {
                    if (!line.startsWith("q ")) continue;
                    int fl = Integer.parseInt(line.split("\\s+")[3]);
                    assertEquals(0, fl & ~charmsOnly, f + " sets a flag of the other slice: " + fl);
                }
                charmsFiles++;
            }
        }
        assertTrue(godFiles >= 100 && charmsFiles >= 100, godFiles + " God worn files, " + charmsFiles + " Charms / Genesis files");
    }

    @Test
    void aSkinIsAPathOrABareIdAndTheTwoKindsResolveToTheirTextures() {
        WornMesh god = WornMesh.parse("g", List.of("model g", "anchor head", "skin demo"));
        assertTrue(god.custom() && god.tinted());
        assertEquals("relic_worn/god/demo.png", god.skinPath());
        assertEquals("relic_worn/god/demo_glow.png", god.glowPath());
        WornMesh charms = WornMesh.parse("c", List.of("model c", "anchor head", "skin visual_upgrade/c.png", "glowtex visual_upgrade/c_glow.png"));
        assertTrue(charms.custom() && !charms.tinted());
        assertEquals("visual_upgrade/c.png", charms.skinPath());
        assertEquals("visual_upgrade/c_glow.png", charms.glowPath());
        WornMesh old = WornMesh.parse("o", List.of("model o", "anchor head"));
        assertFalse(old.custom() || old.tinted());
        assertNull(old.skinPath());
        assertNull(old.glowPath());
    }

    @Test
    void noRelicIsInBothOverrideTablesAndEveryModelTheyNameParses() throws IOException {
        VuOverrides vu = VuOverrides.parse(Files.readAllLines(UPG.resolve("overrides.txt")));
        assertTrue(vu.problems().isEmpty(), vu.problems().toString());
        UpgradeCatalog god = UpgradeCatalog.parse(Files.readAllLines(UPG.resolve("catalog.txt")));
        Set<String> charmsIds = new HashSet<>();
        vu.handIds().forEach(charmsIds::add);
        vu.wornIds().forEach(charmsIds::add);
        assertTrue(charmsIds.size() >= 27, "the Charms / Genesis override table is read: " + charmsIds.size());
        assertTrue(god.size() >= 24, "the God worn catalog is read: " + god.size());
        List<String> both = new ArrayList<>();
        for (String id : god.ids()) if (charmsIds.contains(id)) both.add(id);
        assertTrue(both.isEmpty(), "relics in both tables: " + both);
    }

    @Test
    void theLookupOrderPutsTheOverridesFirstAndTheSwitchOffMeansNothing() {
        assertEquals(UpgradeSource.NONE, UpgradeSource.choose(false, true, true));
        assertEquals(UpgradeSource.NONE, UpgradeSource.choose(true, false, false));
        assertEquals(UpgradeSource.OVERRIDES, UpgradeSource.choose(true, true, false));
        assertEquals(UpgradeSource.GOD_CATALOG, UpgradeSource.choose(true, false, true));
        assertEquals(UpgradeSource.OVERRIDES, UpgradeSource.choose(true, true, true), "a relic in both tables (a mistake) takes the override");
    }

    @Test
    void oneDrawRuleForBothSlices() {
        double near = 11.9 * 11.9, mid = 12.1 * 12.1, far = 24.1 * 24.1;
        assertFalse(UpgradeCaps.useLod(near, false));
        assertTrue(UpgradeCaps.useLod(mid, false), "beyond 12 blocks");
        assertTrue(UpgradeCaps.useLod(0.0, true), "at the low quality");
        assertTrue(UpgradeCaps.additive(mid));
        assertFalse(UpgradeCaps.additive(far), "beyond 24 blocks");
        assertTrue(UpgradeCaps.rich(near, false));
        assertFalse(UpgradeCaps.rich(near, true), "the nebula and the halos are left out at the low quality");
        assertFalse(UpgradeCaps.rich(far, false));
    }

    @Test
    void theCapsLookupMatchesTheTwoTableFiles() throws IOException {
        // Charms / Genesis: assets/nusmp/visual_upgrade/caps_charms_genesis_worn.txt, rows "GRADE mesh drawn lod_percent"
        int rows = 0;
        for (String l : Files.readAllLines(UPG.resolve("caps_charms_genesis_worn.txt"))) {
            if (l.isBlank() || l.startsWith("#")) continue;
            String[] v = l.trim().split("\\s+");
            UpgradeCaps.Caps c = UpgradeCaps.lookup(UpgradeCaps.SLICE_CHARMS_GENESIS, v[0]);
            assertEquals(Integer.parseInt(v[1]), c.modelVertices(), v[0]);
            assertEquals(Integer.parseInt(v[2]), c.ensembleVertices(), v[0]);
            assertEquals(Integer.parseInt(v[3]) / 100.0, c.lodFraction(), 1e-9, v[0]);
            rows++;
        }
        assertEquals(2, rows);
        // God worn: the GOD row of tools/visual_upgrade/god_worn/caps.py
        String py = Files.readString(TOOLS.resolve("god_worn/caps.py"));
        Matcher m = Pattern.compile("\"GOD\":\\s*\\{([^}]*)\\}").matcher(py);
        assertTrue(m.find());
        UpgradeCaps.Caps g = UpgradeCaps.lookup(UpgradeCaps.SLICE_GOD_WORN, "GOD");
        assertTrue(m.group(1).contains("\"model_vertices\": " + g.modelVertices()), m.group(1));
        assertTrue(m.group(1).contains("\"ensemble_vertices\": " + g.ensembleVertices()), m.group(1));
        // an unknown pair is never allowed more than before the upgrade
        UpgradeCaps.Caps none = UpgradeCaps.lookup("nobody", "GOD");
        assertEquals(UpgradeCaps.LEGACY_MODEL_VERTICES, none.modelVertices());
        assertEquals(UpgradeCaps.LEGACY_ENSEMBLE_VERTICES, none.ensembleVertices());
    }

    @Test
    void thereIsOneSharedSwitchForTheWholeVisualUpgrade() throws IOException {
        String charms = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/relics/vu/VuCharmsOptions.java"));
        String fx = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/relics/fx/RelicFxOptions.java"));
        assertTrue(charms.contains(".define(\"upgrade\", true)"), "the shared switch lives in VuCharmsOptions");
        assertFalse(fx.contains(".define(\"upgrade\""), "RelicFxOptions defines no second switch");
        assertTrue(fx.contains("VuCharmsOptions.upgrade()"), "RelicFxOptions.upgrade() reads the shared switch");
    }

    @Test
    void theMergedCodeHasOneLibraryOneCacheAndOneReloadPath() throws IOException {
        Path lib = Path.of("src/main/java/com/newuniverse/nusmp/client/relics/worn/lib");
        String draw = Files.readString(lib.resolve("WornLibDraw.java")), library = Files.readString(lib.resolve("WornLibrary.java"));
        String forms = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/relics/hand/HandForms.java"));
        assertFalse(draw.contains("SKINS"), "one texture cache in the draw class (tex), cleared by clearCustom");
        assertFalse(library.contains("CACHE_UP") || forms.contains("CACHE_UP"), "one cache per library, rebuilt when VuModels.generation() changes");
        assertTrue(library.contains("VuModels.generation()") && forms.contains("VuModels.generation()"));
        Map<String, Integer> hooks = new HashMap<>();
        String engine = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/relics/RelicEngine.java"));
        for (String h : new String[]{"VuActVfx.fire(", "UpgradeActFx.after(", "UpgradeSignatures.afterAct("}) hooks.put(h, engine.split(Pattern.quote(h), -1).length - 1);
        for (var e : hooks.entrySet()) assertEquals(1, e.getValue().intValue(), e.getKey().toLowerCase(Locale.ROOT) + " is called once after an act");
        String sys = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/relics/RelicSystem.java"));
        assertTrue(sys.contains("UpgradeSelfTests.register()"));
    }
}
