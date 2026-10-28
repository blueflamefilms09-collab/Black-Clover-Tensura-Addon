package com.newuniverse.nusmp.mesh;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** 3.32.0: the packed mesh form is lossless for what the loaders read, falls back to text for lines it cannot hold, and shrinks the shipped meshes. */
class MeshPackTest {
    static final Path RES = Path.of("src", "main", "resources", "assets", "nusmp");
    static final String[] PACKED_FOLDERS = {"weapon_meshes", "weapon_meshes_vu", "weapon_meshes_sz", "weapon_meshes_vu_sz", "djinn"};      // build.gradle meshFolders

    private static String qn(int layer, double x, double y) {
        return String.format(java.util.Locale.ROOT, "qn %d %.3f %.3f 0.500 10.250 20.500 0.000 -0.000 1.000 %.3f %.3f 0.500 11.250 20.500 0.000 0.000 1.000 "
                + "%.3f %.3f 1.500 11.250 21.500 0.000 0.000 1.000 %.3f %.3f 1.500 10.250 21.500 0.000 0.000 1.000", layer, x, y, x + 1, y, x + 1, y + 1, x, y + 1);
    }

    private static void assertRoundTrip(String text) throws IOException {
        byte[] bin = MeshPack.pack(text);
        assertTrue(MeshPack.isPacked(bin));
        String back = MeshPack.unpack(bin);
        assertNull(MeshPack.firstDifference(text, back), "round trip");
        assertEquals(back, MeshPack.text(bin), "text() unpacks");
    }

    @Test
    void qnQuadsAndOtherLinesSurviveInOrder() throws IOException {
        StringBuilder sb = new StringBuilder("# part blade 0 60\nplace -0.94768 -0.94768 45.000 0.052980 -28.500\nglow FF3A4A 0.14 0.45 1\nskin demon_slayer_sword\ng -\n");
        for (int i = 0; i < 30; i++) sb.append(qn(i % 3, i, i * 2)).append('\n');
        sb.append("g blade\n").append(qn(1, 3, 4)).append('\n').append(qn(1, 3, 4)).append('\n');
        assertRoundTrip(sb.toString());
    }

    @Test
    void theFlatNormalDialectRoundTrips() throws IOException {
        String q = "q 0 -6.000 0.000 0.700 1.00 56.00 0.000 0.000 1.500 7.00 56.00 0.000 11.750 1.500 7.00 44.00 -6.000 11.750 0.700 1.00 44.00 0.000 0.000 -1.000";
        assertRoundTrip("# x\n" + q + "\n" + q + "\n");
        String back = MeshPack.unpack(MeshPack.pack(q + "\n"));
        assertEquals(25, back.trim().split("\\s+").length, "25 tokens stay 25");
    }

    @Test
    void negativeZeroComesBackAsEqualZero() throws IOException {
        String line = qn(0, 1, 1);
        assertTrue(line.contains("-0.000"));
        String back = MeshPack.unpack(MeshPack.pack(line + "\n"));
        assertNull(MeshPack.firstDifference(line, back));
        for (String t : back.trim().split("\\s+")) assertFalse(t.startsWith("-0.0") && Float.parseFloat(t) == 0f && t.startsWith("-"), "no negative zero spelled: " + t);
    }

    @Test
    void linesItCannotHoldStayText() throws IOException {
        String tooPrecise = qn(0, 1, 1).replace("10.250", "10.25049");                 // five decimals
        String exponent = qn(0, 2, 2).replace("20.500", "2.05e1");
        String shortLine = "qn 0 1 2 3";
        String text = tooPrecise + "\n" + exponent + "\n" + shortLine + "\n" + qn(0, 3, 3) + "\n";
        byte[] bin = MeshPack.pack(text);
        String back = MeshPack.unpack(bin);
        assertTrue(back.contains("10.25049") && back.contains("2.05e1") && back.contains("qn 0 1 2 3"), "the odd lines are kept verbatim");
        assertNull(MeshPack.firstDifference(text, back));
    }

    @Test
    void windowsLineEndingsAndEmptyFilesAreFine() throws IOException {
        assertRoundTrip(qn(0, 1, 1) + "\r\nplace 1 2 3 4 5\r\n");
        assertRoundTrip("");
        assertRoundTrip("# only a comment");
    }

    @Test
    void sharedCornersAreStoredOnce() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 400; i++) sb.append(qn(0, i % 20, i / 20)).append('\n');            // neighbouring quads of a grid repeat their corners in the text
        String text = sb.toString();
        assertTrue(MeshPack.pack(text).length * 4 < text.length(), "a 400 quad grid is under a quarter of the text");
    }

    @Test
    void plainTextPassesThroughTheReader() throws IOException {
        String text = "place 1 2 3 4 5\n" + qn(0, 1, 1) + "\n";
        assertEquals(text, MeshText.text(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))));
        assertEquals(MeshPack.unpack(MeshPack.pack(text)).lines().toList(), MeshText.reader(new ByteArrayInputStream(MeshPack.pack(text))).lines().toList());
    }

    @Test
    void damagedFilesAreRefusedNotMisread() {
        byte[] bin = MeshPack.pack(qn(0, 1, 1) + "\n" + qn(0, 2, 2) + "\n");
        assertThrows(IOException.class, () -> MeshPack.unpack(new byte[]{'N', 'M', 'S', 'H', 1, 0x55, 0x66}));
        assertThrows(IOException.class, () -> MeshPack.unpack(java.util.Arrays.copyOf(bin, bin.length - 4)));
        assertThrows(IOException.class, () -> MeshPack.unpack("plain".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void thePackerLeavesSmallFilesAsTextAndVerifiesBigOnes() throws IOException {
        Path dir = Files.createTempDirectory("meshpack");
        Path in = dir.resolve("in/weapon_meshes"), out = dir.resolve("out");
        Files.createDirectories(in);
        StringBuilder big = new StringBuilder("place 1 2 3 4 5\n");
        for (int i = 0; i < 20; i++) big.append(qn(0, i, i)).append('\n');
        Files.writeString(in.resolve("big.txt"), big);
        Files.writeString(in.resolve("small.txt"), qn(0, 1, 1) + "\n");
        Files.writeString(in.resolve("catalog.txt"), "relic a b c\n");
        MeshPackMain.main(new String[]{dir.resolve("in").toString(), out.toString(), "weapon_meshes", "missing_folder"});
        assertTrue(MeshPack.isPacked(Files.readAllBytes(out.resolve("weapon_meshes/big.txt"))));
        assertFalse(Files.exists(out.resolve("weapon_meshes/small.txt")), "too few quads: not packed");
        assertFalse(Files.exists(out.resolve("weapon_meshes/catalog.txt")));
        assertEquals(big.toString(), Files.readString(in.resolve("big.txt")), "the source stays text");
    }

    /** Every shipped mesh of the packed folders survives packing (this is also what the jar task checks, file by file) and the result is far smaller. */
    @Test
    void everyShippedMeshSurvivesPacking() throws IOException {
        List<Path> files = new ArrayList<>();
        for (String f : PACKED_FOLDERS) {
            Path dir = RES.resolve(f);
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> s = Files.walk(dir)) { s.filter(p -> p.toString().endsWith(".txt")).forEach(files::add); }
        }
        assertFalse(files.isEmpty(), "the shipped meshes are there");
        long[] sizes = new long[2];
        List<String> bad = java.util.Collections.synchronizedList(new ArrayList<>());
        files.parallelStream().forEach(p -> {
            try {
                String text = Files.readString(p);
                if (MeshPack.isPacked(text.getBytes(StandardCharsets.UTF_8))) { bad.add(p + ": the source tree must hold plain text"); return; }
                byte[] bin = MeshPack.pack(text);
                String diff = MeshPack.firstDifference(text, MeshPack.unpack(bin));
                if (diff != null) bad.add(p + ": " + diff);
                synchronized (sizes) { sizes[0] += text.length(); sizes[1] += bin.length; }
            } catch (IOException e) {
                bad.add(p + ": " + e);
            }
        });
        assertTrue(bad.isEmpty(), () -> bad.size() + " meshes do not survive packing, first: " + bad.get(0));
        assertTrue(sizes[1] * 8 < sizes[0], "packed " + sizes[1] + " bytes of " + sizes[0] + " characters of text");
    }
}
