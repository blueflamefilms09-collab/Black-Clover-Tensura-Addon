package com.newuniverse.nusmp.mesh;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Build step of the jar task (build.gradle, task packMeshes): reads the plain text meshes under the given folders of {@code src/main/resources} and writes
 * the packed copies of the big ones to a separate folder; the jar takes those instead of the text. The source tree is never changed.
 *
 * <pre>java com.newuniverse.nusmp.mesh.MeshPackMain &lt;resources root&gt; &lt;out root&gt; &lt;folder relative to the root&gt;...</pre>
 *
 * Every packed file is unpacked again before it is written and compared with its text; a difference fails the build.
 */
public final class MeshPackMain {
    /** A file with fewer packable quads than this stays text (nothing to gain, and the small catalogs stay readable). */
    static final int MIN_QUADS = 8;

    private MeshPackMain() {}

    public static void main(String[] args) throws IOException {
        if (args.length < 3) throw new IllegalArgumentException("usage: <resources root> <out root> <folder>...");
        Path root = Paths.get(args[0]), out = Paths.get(args[1]);
        long before = 0, after = 0;
        int packed = 0, kept = 0, capped = 0, quadsDropped = 0;
        double worstLoss = 0;
        String worst = "";
        int cap = Integer.getInteger("nusmp.meshQuadCap", 0);                           // build.gradle -PmeshQuadCap (default 3000, 0 = no cut)
        for (int i = 2; i < args.length; i++) {
            Path dir = root.resolve(args[i]);
            if (!Files.isDirectory(dir)) continue;
            List<Path> files;
            try (Stream<Path> s = Files.walk(dir)) {
                files = s.filter(p -> p.toString().endsWith(".txt")).sorted().toList();
            }
            for (Path f : files) {
                byte[] raw = Files.readAllBytes(f);
                if (MeshPack.isPacked(raw)) { kept++; continue; }
                String text = new String(raw, StandardCharsets.UTF_8);
                if (MeshPack.packableQuads(text) < MIN_QUADS) { kept++; continue; }
                if (cap > 0) {
                    MeshCap.Result cut = MeshCap.cap(text, cap);
                    if (cut.changed()) {
                        if (!java.util.Arrays.equals(MeshCap.bounds(text), MeshCap.bounds(cut.text())))
                            throw new IllegalStateException(root.relativize(f) + ": the cut changed the extent of a layer");
                        capped++;
                        quadsDropped += cut.quadsBefore() - cut.quadsAfter();
                        if (cut.areaLost() > worstLoss) {
                            worstLoss = cut.areaLost();
                            worst = root.relativize(f) + " " + cut.quadsBefore() + " -> " + cut.quadsAfter() + " quads";
                        }
                        text = cut.text();
                        String dump = System.getProperty("nusmp.meshCapDump");           // tools/mesh_cap/preview_cap.py: the cut meshes as plain text, to look at
                        if (dump != null) {
                            Path d = Paths.get(dump).resolve(root.relativize(f).toString());
                            Files.createDirectories(d.getParent());
                            Files.writeString(d, text);
                        }
                    }
                }
                byte[] bin = MeshPack.pack(text);
                String diff = MeshPack.firstDifference(text, MeshPack.unpack(bin));
                if (diff != null) throw new IllegalStateException(root.relativize(f) + " does not survive packing: " + diff);
                Path to = out.resolve(root.relativize(f).toString());
                Files.createDirectories(to.getParent());
                Files.write(to, bin);
                before += raw.length;
                after += bin.length;
                packed++;
            }
        }
        System.out.println("[nusmp] packMeshes: " + packed + " meshes packed (" + (before >> 20) + " MB of text -> " + (after >> 20) + " MB), " + kept + " left as text");
        if (cap > 0)
            System.out.println("[nusmp] packMeshes: detail limit " + cap + " quads (" + 2 * cap + " triangles): " + capped + " meshes cut, " + quadsDropped + " small quads left out"
                    + (capped > 0 ? ", most surface lost " + Math.round(worstLoss * 1000) / 10.0 + "% (" + worst + ")" : ""));
    }
}
