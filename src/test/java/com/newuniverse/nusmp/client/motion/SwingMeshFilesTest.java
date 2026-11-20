package com.newuniverse.nusmp.client.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Why: the swing rigs (tools/weaponmotion/build_swing.py) are text lines in the mesh files; a rig that does not fit its mesh makes the renderer draw the weapon rigid, silently.
 * Every mesh with a {@code bone} line must have a rig that is valid for its own vertex counts, every mesh named in tools/weaponmotion/motion_specs.json must have one, the
 * {@code # part} comments must still cover the quads exactly once after the tool reordered them, and the worst-case cost of every rig is within the documented budget.
 */
class SwingMeshFilesTest {
    private static final Path MESHES = Path.of("src/main/resources/assets/anime_multiverse/weapon_meshes");
    private static final Path SPECS = Path.of("tools/weaponmotion/motion_specs.json");
    /** Skinned vertices per frame for one weapon (quads x 4): docs/vassal_forms_v2.md. */
    static final int MAX_SKINNED_VERTICES = 4 * 4500;

    record Parsed(SwingRig rig, int[] counts, int quads, List<int[]> parts) {}

    static Parsed parse(Path file) throws IOException {
        SwingRig rig = new SwingRig();
        int[] counts = new int[3];
        int quads = 0;
        List<int[]> parts = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isEmpty()) continue;
            String[] v = line.trim().split("\\s+");
            switch (v[0]) {
                case "q", "qn" -> {
                    counts[Integer.parseInt(v[1])] += 4;
                    quads++;
                }
                case "bone" -> rig.addBone(v);
                case "skin" -> rig.addSkin(v);
                case "#" -> {
                    if (v.length >= 5 && v[1].equals("part")) parts.add(new int[]{Integer.parseInt(v[3]), Integer.parseInt(v[4])});
                }
                default -> { }
            }
        }
        return new Parsed(rig, counts, quads, parts);
    }

    static List<String> rigged() throws IOException {
        List<String> ids = new ArrayList<>();
        try (Stream<Path> s = Files.list(MESHES)) {
            for (Path p : (Iterable<Path>) s.sorted()::iterator) {
                String n = p.getFileName().toString();
                if (!n.endsWith(".txt")) continue;
                try (Stream<String> lines = Files.lines(p, StandardCharsets.UTF_8)) {
                    if (lines.anyMatch(l -> l.startsWith("bone "))) ids.add(n.substring(0, n.length() - 4));
                }
            }
        }
        return ids;
    }

    @Test
    void everyRiggedMeshHasAValidRigForItsOwnVertices() throws IOException {
        List<String> ids = rigged();
        assertTrue(ids.size() >= 15, "the whips, flails and morning stars: " + ids.size());
        List<String> bad = new ArrayList<>();
        for (String id : ids) {
            Parsed p = parse(MESHES.resolve(id + ".txt"));
            if (!p.rig().valid(p.counts())) bad.add(id + ": rig does not fit " + java.util.Arrays.toString(p.counts()));
            else if (p.rig().skinnedVertices() > MAX_SKINNED_VERTICES) bad.add(id + ": " + p.rig().skinnedVertices() + " skinned vertices, over the budget");
            else if (p.rig().n > 31) bad.add(id + ": " + p.rig().n + " bones");
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void everySpecMeshHasItsRig() throws IOException {
        String json = Files.readString(SPECS, StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("\"([a-z0-9_]+)\"\\s*:\\s*\\{\"kind\"").matcher(json);
        List<String> riggedIds = rigged();
        int n = 0;
        while (m.find()) {
            n++;
            assertTrue(riggedIds.contains(m.group(1)), m.group(1) + " is in motion_specs.json but its mesh has no rig (run tools/weaponmotion/build_swing.py)");
        }
        assertTrue(n >= 15, "specs read: " + n);
    }

    @Test
    void thePartCommentsStillCoverEveryQuadOnce() throws IOException {
        for (String id : rigged()) {
            Parsed p = parse(MESHES.resolve(id + ".txt"));
            int covered = 0, next = 0;
            for (int[] r : p.parts()) {
                assertTrue(r[0] >= 0 && r[0] + r[1] <= p.quads(), id + ": a part run is outside the quads");
                covered += r[1];
                next = r[0] + r[1];
            }
            assertEquals(p.quads(), covered, id + ": the part runs must cover the quads exactly once");
            assertTrue(next <= p.quads());
        }
    }

    @Test
    void theRigidHeadComesFirstAndTheTailIsWhatMoves() throws IOException {
        for (String id : rigged()) {
            Parsed p = parse(MESHES.resolve(id + ".txt"));
            for (int l = 0; l < 3; l++) {
                if (p.rig().skin[l] == null) continue;
                assertEquals(p.counts()[l], p.rig().skinFrom[l] + p.rig().skin[l].length, id + " layer " + l);
                assertTrue(p.rig().skinFrom[l] > 0 || l == 2, id + " layer " + l + ": a weapon with nothing rigid (no handle) is not a weapon");
            }
        }
    }
}
