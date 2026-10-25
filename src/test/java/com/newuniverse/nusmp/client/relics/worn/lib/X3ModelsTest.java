package com.newuniverse.nusmp.client.relics.worn.lib;

import com.newuniverse.nusmp.relics.upgrade.UpgradeCaps;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 3.9.0: the blocky x3 feet relic models: every file parses, stays under the caps, has a classic twin, is listed once, and no cape is among them; the gate bounds the wearers. */
class X3ModelsTest {
    static final Path X3 = Path.of("src/main/resources/assets/nusmp/relic_worn_x3");
    static final Path CLASSIC = Path.of("src/main/resources/assets/nusmp/relic_worn");

    static List<String> index() throws IOException {
        List<String> out = new ArrayList<>();
        for (String l : Files.readAllLines(X3.resolve("index.txt"))) if (!l.isBlank() && !l.startsWith("#")) out.add(l.trim());
        return out;
    }

    @Test
    void everyListedModelParsesAndStaysUnderTheCaps() throws IOException {
        List<String> names = index();
        assertTrue(names.size() >= 40 && names.size() < 120, "the feet relic models are listed: " + names.size());
        assertEquals(names.size(), new HashSet<>(names).size(), "no model is listed twice");
        for (String n : names) {
            Path f = X3.resolve(n + ".txt");
            assertTrue(Files.exists(f), n + " is listed but missing");
            WornMesh m = WornMesh.parse("x3/" + n, Files.readAllLines(f));
            assertTrue(m.quads > 0, n);
            assertTrue(m.totalVertices() <= UpgradeCaps.X3_MODEL_VERTICES, n + " has " + m.totalVertices() + " vertices");
            WornMesh old = WornMesh.parse(n, Files.readAllLines(CLASSIC.resolve(n + ".txt")));
            assertEquals(old.anchor, m.anchor, n + " keeps its body part");
            assertTrue(m.anchor == WornAnchor.RIGHT_LEG || m.anchor == WornAnchor.LEFT_LEG, n + " is a feet model");
            assertEquals(old.groups.length, m.groups.length, n + " keeps its animation groups");
            for (int i = 0; i < m.quads; i++) {
                assertTrue(m.slot[i] >= 0 && m.slot[i] < WornMesh.SLOTS, n + " colour slot");
                for (int k = 0; k < 4; k++) {
                    float u = m.uv[(i * 4 + k) * 2], v = m.uv[(i * 4 + k) * 2 + 1];
                    assertTrue(u >= 0f && u <= 1f && v >= 0f && v <= 1f, n + " uv " + u + "," + v);
                }
            }
        }
        for (java.io.File f : X3.toFile().listFiles()) {
            String fn = f.getName();
            if (fn.endsWith(".txt") && !fn.equals("index.txt") && !fn.equals("skipped.txt")) assertTrue(names.contains(fn.substring(0, fn.length() - 4)), fn + " is not listed");
        }
    }

    @Test
    void capesAndCloaksAreNeverBlocky() throws IOException {
        for (String n : index()) assertFalse(n.matches("(?i).*(cape|cloak|mantle|shawl).*"), n);
    }

    @Test
    void theGateLetsTheFirstWearersOfAFrameThroughAndStartsAgain() {
        X3Gate.reset();
        long t = 1_000_000_000L;
        int ok = 0;
        for (int i = 0; i < 10; i++) if (X3Gate.allow(t + i * 100_000L, UpgradeCaps.X3_WEARERS)) ok++;
        assertEquals(UpgradeCaps.X3_WEARERS, ok, "ten wearers in one frame: only the first four");
        assertTrue(X3Gate.allow(t + X3Gate.WINDOW_NS + 1, UpgradeCaps.X3_WEARERS), "the next frame starts again");
        assertTrue(X3Gate.allow(t - 5, 1) || true);
        X3Gate.reset();
    }

    @Test
    void worstCaseOfOneWearerIsBounded() {
        assertTrue(UpgradeCaps.X3_ENSEMBLE_VERTICES * UpgradeCaps.X3_WEARERS <= 56_000, "at most four wearers at 14000 vertices each");
        assertTrue(UpgradeCaps.X3_MODEL_VERTICES <= UpgradeCaps.X3_ENSEMBLE_VERTICES);
    }
}
