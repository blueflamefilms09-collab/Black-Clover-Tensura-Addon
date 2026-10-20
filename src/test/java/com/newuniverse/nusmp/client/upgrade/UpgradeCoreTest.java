package com.newuniverse.nusmp.client.upgrade;

import com.newuniverse.nusmp.relics.upgrade.UpgradeItems;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the pure parts of the visual upgrade: the list of upgraded weapons and the signature id of an act (the server and the client must agree on it without a table), the seed
 * packing, the base id of a draw state, the spec parser (defaults, colours, bad input), and the rule that the hot paths of the renderer and the effect tick allocate nothing per call.
 */
class UpgradeCoreTest {
    static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    @Test
    void theListIsTheTwelveOfTheSliceWithNoDuplicates() {
        assertEquals(12, UpgradeItems.GOD_MELEE_A.size());
        assertEquals(UpgradeItems.ALL.size(), UpgradeItems.ALL.stream().distinct().count());
        assertTrue(UpgradeItems.has("staff_of_the_magi"));
        assertFalse(UpgradeItems.has("blackrazor"));
        assertFalse(UpgradeItems.has(null));
    }

    @Test
    void theSignatureIdIsTheCrcOfItemAndActAndSurvivesTheSeed() {
        int a = UpgradeItems.signatureId("staff_of_the_magi", "arcane_tempest");
        assertTrue(a >= 0, "31 bits: never negative");
        assertEquals(a, UpgradeItems.signatureId("staff_of_the_magi", "arcane_tempest"));
        assertNotEquals(a, UpgradeItems.signatureId("staff_of_the_magi", "spell_turning"));
        assertNotEquals(a, UpgradeItems.signatureId("hammer_of_thunderbolts", "arcane_tempest"));
        // a value the Python generator writes into the specs (zlib.crc32 of the same text, masked to 31 bits)
        java.util.zip.CRC32 c = new java.util.zip.CRC32();
        c.update("staff_of_the_magi:arcane_tempest".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals((int) (c.getValue() & 0x7FFFFFFFL), a);
        for (int random : new int[]{0, 1, -1, 123456789, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            long seed = UpgradeItems.packSeed(a, random);
            assertEquals(a, UpgradeItems.signatureOf(seed));
        }
    }

    @Test
    void aDrawStateSharesTheBaseIdOfItsWeapon() {
        assertEquals("dawnpiercer_crossbow", UpgradeItems.baseId("dawnpiercer_crossbow_pull2"));
        assertEquals("dawnpiercer_crossbow", UpgradeItems.baseId("dawnpiercer_crossbow_charged"));
        assertEquals("dawnpiercer_crossbow", UpgradeItems.baseId("dawnpiercer_crossbow"));
        assertEquals("staff_of_the_magi", UpgradeItems.baseId("staff_of_the_magi"));
        assertNull(UpgradeItems.baseId(null));
    }

    @Test
    void theSpecParserReadsColoursDefaultsAndRecipes() {
        String json = "{\"id\":\"x\",\"version\":1,\"palette\":{\"primary\":[255,0,16],\"secondary\":[1,2,3]},\"maps\":{\"skin\":\"up/x.png\",\"glow\":\"up/x_glow.png\",\"veins\":\"up/x_veins.png\"},"
                + "\"lod\":{\"mesh\":\"up/x_lod\",\"near\":10,\"glow_off\":20,\"skin\":\"up/x_lod.png\",\"glow\":\"up/x_lod_glow.png\"},\"passes\":[\"skin\",\"glow_core\"],"
                + "\"pass_cfg\":{\"veins\":{\"rgb\":[10,20,30],\"speed\":0.5,\"wavelength\":12,\"min\":0.2,\"max\":0.9}},\"idle\":[\"glow_breath\",\"rune_flicker\"],"
                + "\"ambient\":[{\"type\":\"motes\",\"rgb\":[300,-5,16]}],\"hand\":{\"length\":2.0,\"tilt\":10},\"swing\":{\"pattern\":\"arc\",\"count\":4},"
                + "\"acts\":[{\"act\":\"a\",\"id\":77,\"recipe\":[{\"t\":\"ring\",\"dur\":10,\"delay\":4},{\"t\":\"flash\",\"dur\":6}]}],\"ambient_sound\":\"nusmp:s\"}";
        UpgradeSpec s = UpgradeSpec.parse(new StringReader(json));
        assertEquals("x", s.id);
        assertEquals(0xFF0010, s.primary);
        assertEquals(0x0A141E, s.veinsRgb);
        assertEquals(0.9f, s.veinsMax, 1e-6f);
        assertEquals(10f, s.lodNear, 1e-6f);
        assertTrue(s.idles("rune_flicker"));
        assertFalse(s.idles("orbit_parts"));
        assertEquals(1, s.ambient.size());
        assertEquals(0xFF0010, s.ambient.get(0).rgb(), "colours are clamped to 0..255");
        assertEquals("end_rod".equals(s.ambient.get(0).particle()) ? "end_rod" : "dust", s.ambient.get(0).particle());
        assertEquals(4, s.swing.count());
        UpgradeSpec.Act act = s.actByKey("a");
        assertNotNull(act);
        assertEquals(77, act.id());
        assertEquals(14, act.maxDur(), "the last component ends at delay + dur");
        assertNull(s.actByKey("b"));
    }

    @Test
    void anEmptySpecIsStillASpec() {
        UpgradeSpec s = UpgradeSpec.parse(new StringReader("{}"));
        assertEquals("", s.id);
        assertTrue(s.ambient.isEmpty());
        assertNull(s.swing);
        assertTrue(s.acts.isEmpty());
        assertEquals(12f, s.lodNear, 1e-6f);
        assertEquals(24f, s.lodGlowOff, 1e-6f);
    }

    // ---------------------------------------------------------------- no allocation per frame / tick
    /** The body of a method of a source file (from its signature line to the matching closing brace). */
    static String body(String file, String signature) throws IOException {
        String src = Files.readString(SRC.resolve(file));
        int i = src.indexOf(signature);
        assertTrue(i >= 0, signature + " is in " + file);
        int open = src.indexOf('{', i);
        int depth = 0, k = open;
        for (; k < src.length(); k++) {
            char c = src.charAt(k);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) break;
        }
        return src.substring(open, k + 1);
    }

    static String strip(String code) {
        return code.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
    }

    @Test
    void theRenderPathsAllocateNothing() throws IOException {
        for (String sig : new String[]{"static void draw(", "private static void emitQuads(", "private static void emitWave(", "static WeaponRenderer.Mesh pick(", "static float flicker("}) {
            String code = strip(body("client/WeaponUpgrade.java", sig));
            assertFalse(code.matches("(?s).*\\bnew\\s+[A-Z].*"), sig + " allocates");
        }
    }

    @Test
    void theEffectTickAllocatesNothingButParticles() throws IOException {
        for (String sig : new String[]{"public static void onClientTick(", "private static void frame(", "private static void layer(", "private static void swing("}) {
            String code = strip(body("client/upgrade/UpgradeFx.java", sig));
            assertFalse(code.matches("(?s).*\\bnew\\s+[A-Z].*"), sig + " allocates");
        }
    }

    @Test
    void theParticleBudgetIsTakenBeforeAnythingIsSpawned() throws IOException {
        String layer = strip(body("client/upgrade/UpgradeFx.java", "private static void layer("));
        assertTrue(layer.indexOf("PerfBudgets.clientParticles") > 0 && layer.indexOf("PerfBudgets.clientParticles") < layer.indexOf("level.addParticle"));
        String swing = strip(body("client/upgrade/UpgradeFx.java", "private static void swing("));
        assertTrue(swing.indexOf("PerfBudgets.clientParticles") > 0 && swing.indexOf("PerfBudgets.clientParticles") < swing.indexOf("level.addParticle"));
        List<String> must = List.of("MAX_HOLDERS", "RANGE");
        String src = Files.readString(SRC.resolve("client/upgrade/UpgradeFx.java"));
        for (String m : must) assertTrue(src.contains(m), m);
    }

    @Test
    void theShippedLookIsKeptAsTheFallback() throws IOException {
        String src = Files.readString(SRC.resolve("client/WeaponRenderer.java"));
        assertTrue(src.contains("static Mesh plainMesh(String id)"), "the shipped mesh path is still there");
        assertTrue(src.contains("WeaponUpgrade.mesh(id)"), "the renderer asks the upgrade first");
        String up = Files.readString(SRC.resolve("client/WeaponUpgrade.java"));
        assertTrue(up.contains("if (!UpgradeConfig.enabled()) return null;"), "the option switches the upgrade off");
    }
}
