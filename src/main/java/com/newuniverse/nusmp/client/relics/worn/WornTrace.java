package com.newuniverse.nusmp.client.relics.worn;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: "was the worn layer even reached for this wearer, and what did it decide" is a fact of the render thread that nothing recorded. Each worn layer calls {@link #reached} once per frame
 * per wearer (one small mutable record per wearer, no allocation after the first frame), and {@code /multiverse relic debug} reads it back through {@code WornDebugClient}. No Minecraft types.
 * Client render thread only.
 */
public final class WornTrace {
    private WornTrace() {}

    /** What the layer last did for one wearer. */
    public static final class Seen {
        /** Game time of the last frame the layer ran for this wearer, and the number of frames since the world was joined. */
        public long tick;
        public int frames;
        /** "player" (the vanilla player renderers) or "kit" (the player-model characters). */
        public String layer = "";
        public boolean slim;
        public WornPlan.View view = WornPlan.View.OTHER;
        /** How many worn relics the layer found, and how many of them it drew (library, boxes, charms, own renderers). */
        public int found, drawn;
        /** The last plan (null when the wearer had no relic at all). */
        public WornPlan.Plan plan;
        /** Ensembles handed to the model library on the last frame (relics and the mod's own armour). */
        public int ensembles;
    }

    private static final Map<UUID, Seen> SEEN = new HashMap<>();

    /** Records that the layer ran for this wearer this frame. */
    public static void reached(UUID wearer, long tick, String layer, boolean slim, WornPlan.View view, int found, WornPlan.Plan plan, int ensembles) {
        Seen s = SEEN.get(wearer);
        if (s == null) {
            if (SEEN.size() > 256) SEEN.clear();
            s = new Seen();
            SEEN.put(wearer, s);
        }
        s.tick = tick;
        s.frames++;
        s.layer = layer;
        s.slim = slim;
        s.view = view;
        s.found = found;
        s.plan = plan;
        s.drawn = plan == null ? 0 : plan.drawnCount();
        s.ensembles = ensembles;
    }

    /** What was last recorded for this wearer, or null if the layer never ran for it. */
    public static Seen of(UUID wearer) { return SEEN.get(wearer); }

    public static void clear() { SEEN.clear(); }
}
