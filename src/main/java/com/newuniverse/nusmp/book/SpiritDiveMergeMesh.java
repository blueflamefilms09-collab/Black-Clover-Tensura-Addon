package com.newuniverse.nusmp.book;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spirit Dive MERGE: the light meshes of the spirits (the ghost that flows into the diver, the faint silhouette it leaves), read once from {@code assets/nusmp/sdm_meshes/<name>.txt} (written by
 * {@code tools/gen_sdm_models.py}) and shared by every diver. The quad line format and the parser are those of {@link SpiritDiveWind3Mesh}; every quad of these meshes is on layer 1 (the additive
 * layer). Pure and server safe (no Minecraft class).
 */
public final class SpiritDiveMergeMesh {
    private SpiritDiveMergeMesh() {}

    public static final String DIR = "/assets/nusmp/sdm_meshes/";
    private static final Map<String, SpiritDiveWind3Mesh> CACHE = new ConcurrentHashMap<>();

    /** The mesh of that name (read once, shared); an empty mesh when the file is missing (nothing is drawn; the self-test reports it). */
    public static SpiritDiveWind3Mesh get(String name) {
        SpiritDiveWind3Mesh m = CACHE.get(name);
        if (m != null) return m;
        List<String> lines = read(name);
        m = SpiritDiveWind3Mesh.parse(name, lines == null ? List.of() : lines);
        CACHE.put(name, m);
        return m;
    }

    public static boolean exists(String name) { return read(name) != null; }

    private static List<String> read(String name) {
        try (InputStream in = SpiritDiveMergeMesh.class.getResourceAsStream(DIR + name + ".txt")) {
            if (in != null) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) { return r.lines().toList(); }
            }
        } catch (IOException ignored) { }
        try {
            Path p = Path.of("src", "main", "resources", "assets", "nusmp", "sdm_meshes", name + ".txt");
            if (Files.isRegularFile(p)) return Files.readAllLines(p, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException ignored) { }
        return null;
    }
}
