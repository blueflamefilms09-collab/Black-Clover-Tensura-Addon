package com.newuniverse.nusmp.client.relics.worn;

import com.newuniverse.nusmp.client.relics.worn.lib.WornCatalog;
import com.newuniverse.nusmp.client.relics.worn.lib.WornMesh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Why: the worn models, the catalog and the textures are plain files of the jar, so the question "does this relic's model exist, parse, and sit on a body part" can be answered from the
 * jar itself, on a dedicated server and in a test, with no resource manager and no game. {@code /multiverse relic debug} (server half) and the self-test {@code relic.worn_render2} use this;
 * the client half asks the real resource manager the same questions. Pure Java (class path reads only), cached after the first read.
 */
public final class WornAssets {
    private WornAssets() {}

    public static final String BASE = "/assets/nusmp/relic_worn/";
    public static final String ATLAS = "/assets/nusmp/textures/relic_worn/atlas.png", SHIMMER = "/assets/nusmp/textures/relic_worn/shimmer.png";

    /** The relics that have a worn renderer of their own ({@code WornRenderers.init}); a test pins this list to that method. */
    public static final Set<String> CUSTOM_RENDERERS = Set.of("dante_gravity_crown", "zenon_spatial_ring", "artifact_mimic");

    private static WornCatalog catalog;
    private static final Map<String, Optional<WornMesh>> MESHES = new HashMap<>();
    private static final Map<String, String> PROBLEMS = new HashMap<>();

    /** Does the jar hold this file? ({@code path} starts with a slash.) */
    public static boolean exists(String path) { return WornAssets.class.getResource(path) != null; }

    /** The catalog, read once; an empty one (and {@link #catalogProblem}) if the file is not there. */
    public static synchronized WornCatalog catalog() {
        if (catalog != null) return catalog;
        List<String> lines = new ArrayList<>();
        try (InputStream in = WornAssets.class.getResourceAsStream(BASE + "catalog.txt")) {
            if (in != null) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String l;
                    while ((l = r.readLine()) != null) lines.add(l);
                }
            } else {
                PROBLEMS.put("catalog.txt", "not in the jar");
            }
        } catch (IOException ex) {
            PROBLEMS.put("catalog.txt", ex.toString());
        }
        catalog = WornCatalog.parse(lines);
        return catalog;
    }

    /** Why the catalog could not be read, or null. */
    public static synchronized String catalogProblem() { return PROBLEMS.get("catalog.txt"); }

    /** The parsed model of this name, or empty (the reason is in {@link #problemOf}). */
    public static synchronized Optional<WornMesh> mesh(String name) {
        Optional<WornMesh> cached = MESHES.get(name);
        if (cached != null) return cached;
        WornMesh mesh = null;
        try (InputStream in = WornAssets.class.getResourceAsStream(BASE + name + ".txt")) {
            if (in == null) {
                PROBLEMS.put(name, "file " + name + ".txt is not in the jar");
            } else {
                List<String> lines = new ArrayList<>();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String l;
                    while ((l = r.readLine()) != null) lines.add(l);
                }
                mesh = WornMesh.parse(name, lines);
                if (mesh.quads == 0) {
                    PROBLEMS.put(name, "model " + name + " has no quads");
                    mesh = null;
                }
            }
        } catch (IOException | RuntimeException ex) {
            PROBLEMS.put(name, "model " + name + " cannot be parsed: " + ex.getMessage());
            mesh = null;
        }
        Optional<WornMesh> made = Optional.ofNullable(mesh);
        MESHES.put(name, made);
        return made;
    }

    /** Why a model could not be loaded, or null. */
    public static synchronized String problemOf(String name) { return PROBLEMS.get(name); }

    /** What a relic's worn models are, from the jar: one line per piece, and whether at least one of them loads. */
    public record Dress(String source, List<String> lines, boolean loads) {}

    /** Resolves a relic (slot name, look name) through the catalog and the files, the way the client library does. */
    public static Dress dress(String id, String slot, String look) {
        WornCatalog cat = catalog();
        WornCatalog.Source src = cat.sourceOf(id, slot, look);
        List<String> lines = new ArrayList<>();
        boolean any = false;
        if (src == WornCatalog.Source.NONE) {
            lines.add("no catalog entry for " + slot + "/" + look + " and no default look for the slot");
            return new Dress(src.name(), lines, false);
        }
        for (WornCatalog.Piece p : cat.piecesFor(id, slot, look)) {
            if (WornCatalog.NONE.equals(p.model())) {
                lines.add("piece none (draws nothing on purpose)");
                continue;
            }
            Optional<WornMesh> m = mesh(p.model());
            if (m.isEmpty()) {
                lines.add("model " + p.model() + ": MISSING OR BROKEN (" + problemOf(p.model()) + ")");
                continue;
            }
            any = true;
            WornMesh w = m.get();
            lines.add("model " + p.model() + ": " + w.quads + " quads, bone " + (p.anchor() != null ? p.anchor() : w.anchor) + (p.mirror() ? " (mirrored)" : "")
                    + ", " + Math.max(0, w.groups.length - 1) + " animation groups, " + w.emitQuads + " glowing quads");
        }
        return new Dress(src.name(), lines, any);
    }

    /** Does a relic have at least one loadable worn model (from the jar)? */
    public static boolean hasModel(String id, String slot, String look) { return dress(id, slot, look).loads(); }
}
