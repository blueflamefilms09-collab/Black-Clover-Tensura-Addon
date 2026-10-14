package com.newuniverse.nusmp.client.relics.worn;

import java.util.HashMap;
import java.util.Map;

/**
 * The registry of per-relic worn renderers. {@code RelicWornLayer} asks it for the renderer of each worn relic's id; a relic without one gets the
 * layer's generic palette model. {@link #init} registers the three boss relics' renderers (call it once from the client section, which
 * {@code RelicClient.init} does).
 */
public final class WornRenderers {
    private WornRenderers() {}

    private static final Map<String, WornRenderer> MAP = new HashMap<>();
    private static boolean inited;

    /** Registers (or replaces) the renderer of a relic id. Client thread only. */
    public static void register(String relicId, WornRenderer renderer) {
        if (relicId != null && renderer != null) MAP.put(relicId, renderer);
    }

    /** The renderer of a relic id, or null. */
    public static WornRenderer get(String relicId) { return MAP.get(relicId); }

    /** Registers the renderers this mod ships. Safe to call twice. */
    public static synchronized void init() {
        if (inited) return;
        inited = true;
        register("dante_gravity_crown", new DanteGravityCrownRenderer());
        register("zenon_spatial_ring", new ZenonSpatialRingRenderer());
        register("artifact_mimic", new ArtifactMimicRenderer());
    }
}
