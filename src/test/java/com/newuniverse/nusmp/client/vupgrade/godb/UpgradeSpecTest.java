package com.newuniverse.nusmp.client.vupgrade.godb;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The shipped data tables (assets/nusmp/visual_upgrade/*.json) parse and meet the rules every table must meet; a broken table is refused with a reason. */
class UpgradeSpecTest {
    static final Path DIR = Path.of("src", "main", "resources", "assets", "nusmp", "visual_upgrade");
    static final List<String> IDS = List.of("aurliels_seraph_halberd", "chronarchs_pendulum_sabre", "deepwatch_harpoon", "orials_starfall_staff", "reflected_kings_mirrorblade",
            "rimeheart_greatblade", "sanguine_wardens_cleaver", "valkarins_stormlance", "paradox");

    static JsonObject read(String id) throws IOException {
        return JsonParser.parseString(Files.readString(DIR.resolve(id + ".json"))).getAsJsonObject();
    }

    @Test
    void everyShippedTableParsesAndValidates() throws IOException {
        for (String id : IDS) {
            UpgradeSpec s = UpgradeSpec.parse(read(id));
            assertEquals(id, s.item());
            assertEquals("up/" + id, s.mesh());
            assertEquals(UpgradeSpec.LAYER_KINDS.size(), s.layers().size(), id + ": seven particle layers");
            for (String k : UpgradeSpec.LAYER_KINDS) assertNotNull(s.layer(k), id + " layer " + k);
            assertTrue(s.orbitPieces() >= 4 && s.orbitPieces() <= 12, id + " orbiting pieces " + s.orbitPieces());
            assertTrue(s.trail().ghosts() >= 3, id + " swing ghosts");
            assertTrue(s.passes().size() >= 5, id + " passes " + s.passes());
            assertTrue(s.ambientLayers().size() >= 8, id + " ambient layers " + s.ambientLayers());
            assertTrue(s.animations().size() >= 6, id + " idle animations " + s.animations());
        }
    }

    @Test
    void theListedLayersAreTheOnesTheCodeRuns() throws IOException {
        for (String id : IDS) {
            UpgradeSpec s = UpgradeSpec.parse(read(id));
            for (String k : UpgradeSpec.LAYER_KINDS) assertTrue(s.ambientLayers().contains(k), id + " lists " + k);
            assertTrue(s.ambientLayers().contains("orbit_pieces") && s.ambientLayers().contains("grade_shimmer"), id + " lists the orbit pieces and the old shimmer");
            for (String pass : List.of("body", "core_glow", "vein_wave", "halo_shell", "orbit_pieces")) assertTrue(s.passes().contains(pass), id + " pass " + pass);
        }
    }

    @Test
    void aBrokenTableIsRefusedWithAReason() throws IOException {
        JsonObject o = read("rimeheart_greatblade");
        o.addProperty("mesh", "rimeheart_greatblade");
        try {
            UpgradeSpec.parse(o);
            fail("a mesh id outside up/ must be refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("mesh ids"), expected.getMessage());
        }
        JsonObject p = read("rimeheart_greatblade");
        p.getAsJsonObject("halo").addProperty("scale", 2.5);
        try {
            UpgradeSpec.parse(p);
            fail("a halo of 2.5 must be refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("halo"), expected.getMessage());
        }
        JsonObject q = read("paradox");
        q.getAsJsonObject("particles").getAsJsonArray("layers").remove(0);
        try {
            UpgradeSpec.parse(q);
            fail("a table with six layers must be refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("seven"), expected.getMessage());
        }
    }

    private static void fail(String m) {
        throw new AssertionError(m);
    }
}
