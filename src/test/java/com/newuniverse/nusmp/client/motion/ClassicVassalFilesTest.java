package com.newuniverse.nusmp.client.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Why: the v2 remodel of the vassal axe, book and carriage forms (docs/vassal_forms_v2.md) replaced their meshes and skins, and the owner's rule is to keep the old ones: every remodelled form must
 * have its classic mesh and both classic skins (body and glow), the classic mesh must still be a valid mesh, and both versions must stay inside the budgets (5000 quad lines, 350 KB of skins).
 */
class ClassicVassalFilesTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/anime_multiverse");
    private static final Path MESHES = ASSETS.resolve("weapon_meshes");
    private static final Path CLASSIC = MESHES.resolve("classic_vassal");
    private static final Path SKINS = ASSETS.resolve("textures/entity/weapon");
    private static final Path CLASSIC_SKINS = SKINS.resolve("classic_vassal");

    private static List<String> remodelled() throws IOException {
        List<String> ids = new ArrayList<>();
        try (Stream<Path> s = Files.list(MESHES)) {
            for (Path p : (Iterable<Path>) s.sorted()::iterator) {
                String n = p.getFileName().toString();
                if (n.endsWith(".txt") && (n.startsWith("vassal_axe__") || n.startsWith("vassal_book__") || n.startsWith("vassal_carriage__"))) ids.add(n.substring(0, n.length() - 4));
            }
        }
        return ids;
    }

    private static int[] lines(Path mesh) throws IOException {
        int body = 0, glow = 0;
        for (String l : Files.readAllLines(mesh)) {
            if (l.startsWith("q ") || l.startsWith("qn ")) {
                if (l.split("\\s+")[1].equals("2")) glow++;
                else body++;
            }
        }
        return new int[]{body, glow};
    }

    @Test
    void twelveFormsWereRemodelledAndEachKeepsItsClassicCopy() throws IOException {
        List<String> ids = remodelled();
        assertEquals(12, ids.size(), "4 axes + 4 books + 4 carriages: " + ids);
        List<String> missing = new ArrayList<>();
        for (String id : ids) {
            for (Path p : new Path[]{CLASSIC.resolve(id + ".txt"), CLASSIC_SKINS.resolve(id + ".png"), CLASSIC_SKINS.resolve(id + "_glow.png")}) {
                if (!Files.isRegularFile(p) || Files.size(p) == 0) missing.add(p.toString());
            }
        }
        assertEquals(List.of(), missing, "the classic copy of a remodelled form is gone");
    }

    @Test
    void theClassicAndTheNewFormsAreBothInsideTheBudgets() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : remodelled()) {
            for (Path mesh : new Path[]{CLASSIC.resolve(id + ".txt"), MESHES.resolve(id + ".txt")}) {
                int[] c = lines(mesh);
                if (c[0] + c[1] > 5000) bad.add(mesh + ": " + (c[0] + c[1]) + " quad lines");
                if (c[0] < 500) bad.add(mesh + ": only " + c[0] + " body quads, not a model");
            }
            long newKb = (Files.size(SKINS.resolve(id + ".png")) + Files.size(SKINS.resolve(id + "_glow.png"))) / 1024;
            if (newKb > 350) bad.add(id + ": skins " + newKb + " KB");
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void theNewFormIsReallyANewMesh() throws IOException {
        for (String id : remodelled()) {
            assertTrue(!Files.readString(MESHES.resolve(id + ".txt")).equals(Files.readString(CLASSIC.resolve(id + ".txt"))), id + ": the new mesh is identical to the classic one");
        }
    }
}
