package com.newuniverse.nusmp.client.relics.hand;

import com.newuniverse.nusmp.client.relics.worn.lib.WornCatalog;
import com.newuniverse.nusmp.client.relics.worn.lib.WornMesh;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: every signature hand model is a file the generator wrote and the game parses once; a file that does not parse, a catalog line that names a model that is not there, or a
 * model over the vertex budget would be a held relic that falls back to its sprite or costs too much to draw. This test parses the real catalog and every real model file with
 * the same parser the game uses, checks the budget the worn models keep (2600 vertices for everything one relic is drawn with), and checks the catalog parser on sample text.
 */
class HandCatalogTest {
    static final Path HAND = Path.of("src/main/resources/assets/nusmp/relic_hand");
    static final Path WORN = Path.of("src/main/resources/assets/nusmp/relic_worn");
    static final int BUDGET = 2600;

    @Test
    void parsesSigLinesWithAnchorsAndMirrors() {
        HandCatalog c = HandCatalog.parse(List.of("# c", "", "sig a stone_space", "sig b chest_collar pauldron_lion@rarm pauldron_lion@larm~", "sig bad", "other x y", "sig c  x   y"));
        assertEquals(3, c.sigCount());
        assertEquals(1, c.sigFor("a").size());
        List<WornCatalog.Piece> b = c.sigFor("b");
        assertEquals(3, b.size());
        assertEquals("pauldron_lion", b.get(2).model());
        assertTrue(b.get(2).mirror());
        assertNotNull(b.get(1).anchor());
        assertTrue(c.sigFor("bad").isEmpty());
        assertTrue(c.sigFor("nobody").isEmpty());
        assertEquals(2, c.sigFor("c").size());
    }

    static Set<String> handModels() throws IOException {
        Set<String> out = new HashSet<>();
        try (Stream<Path> s = Files.list(HAND)) {
            for (Path p : s.toList()) {
                String n = p.getFileName().toString();
                if (n.endsWith(".txt") && !n.equals("catalog.txt") && !n.equals("transforms.txt")) out.add(n.substring(0, n.length() - 4));
            }
        }
        return out;
    }

    static int vertices(String model) throws IOException {
        Path p = HAND.resolve(model + ".txt");
        if (!Files.exists(p)) p = WORN.resolve(model + ".txt");
        return WornMesh.parse(model, Files.readAllLines(p)).solidVertices();
    }

    @Test
    void everyHandModelFileParsesAndStaysUnderTheBudget() throws IOException {
        Set<String> models = handModels();
        assertTrue(models.size() >= 100, "hand models found: " + models.size());
        for (String n : models) {
            WornMesh m = WornMesh.parse(n, Files.readAllLines(HAND.resolve(n + ".txt")));
            assertTrue(m.quads >= 4, n + " has no geometry");
            assertTrue(m.solidVertices() <= BUDGET, n + " has " + m.solidVertices() + " vertices (budget " + BUDGET + ")");
            for (int i = 0; i < m.pos.length; i++) assertTrue(Float.isFinite(m.pos[i]) && Math.abs(m.pos[i]) < 60f, n + " has a vertex far away: " + m.pos[i]);
        }
    }

    @Test
    void everySigLineNamesExistingModelsAndTheirSumIsUnderTheBudget() throws IOException {
        HandCatalog c = HandCatalog.parse(Files.readAllLines(HAND.resolve("catalog.txt")));
        assertTrue(c.sigCount() >= 120, "sig lines: " + c.sigCount());
        for (String id : c.sigIds()) {
            int total = 0;
            for (WornCatalog.Piece p : c.sigFor(id)) {
                assertTrue(Files.exists(HAND.resolve(p.model() + ".txt")) || Files.exists(WORN.resolve(p.model() + ".txt")), id + " names the missing model " + p.model());
                total += vertices(p.model());
            }
            assertTrue(total <= BUDGET, id + " is drawn with " + total + " vertices (budget " + BUDGET + ")");
        }
    }

    @Test
    void everyHandModelIsHeldByAtLeastOneRelic() throws IOException {
        HandCatalog c = HandCatalog.parse(Files.readAllLines(HAND.resolve("catalog.txt")));
        Set<String> used = new HashSet<>();
        for (String id : c.sigIds()) for (WornCatalog.Piece p : c.sigFor(id)) used.add(p.model());
        Set<String> orphans = new HashSet<>(handModels());
        orphans.removeAll(used);
        assertTrue(orphans.isEmpty(), "hand models no relic uses: " + orphans);
    }
}
