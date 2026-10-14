package com.newuniverse.nusmp.client.relics.worn.lib;

import java.util.Locale;

/**
 * The anchor allocator of the worn relics: when several relics of the same slot are worn at once (three charms, two necklaces, two head items, two pairs of
 * gloves) each gets its own deterministic place so none hides another. {@link #place} turns a slot name and the index of the relic among the relics of that slot
 * into an offset (model pixels, y down, front is -z), a turn about the vertical axis, a scale and a left-right flip; {@link #fit} is the size and the closeness to the
 * body every worn model gets. Index 0 is always the untouched look the model files were made for. Pure: no Minecraft types.
 */
public final class WornPlacer {
    private WornPlacer() {}

    /** The global shrink of the on-body models (head items keep their size). */
    public static final float BODY_SCALE = 0.85f;

    /** @param dx,dy,dz offset in model pixels; rotY degrees about the vertical axis; scale uniform; mirror flips left and right */
    public record Placement(float dx, float dy, float dz, float rotY, float scale, boolean mirror) {
        public static final Placement NONE = new Placement(0f, 0f, 0f, 0f, 1f, false);

        /** This placement followed by {@code o} (offsets add, scales multiply, flips cancel). */
        public Placement then(Placement o) {
            return new Placement(dx + o.dx, dy + o.dy, dz + o.dz, rotY + o.rotY, scale * o.scale, mirror ^ o.mirror);
        }
    }

    /** Where the {@code index}-th relic (0-based, in a fixed order) of this slot ("NECK", "CHARM", "HEAD" ...) stands. Negative indexes count as 0. */
    public static Placement place(String slot, int index) {
        int i = Math.max(0, index);
        if (i == 0 || slot == null) return Placement.NONE;
        switch (slot.toUpperCase(Locale.ROOT)) {
            case "CHARM": {                                         // hip left, hip right, back right, back left, then hanging lower on alternating sides
                int ring = (i - 1) % 4, tier = (i - 1) / 4;
                float drop = tier * 2.4f;
                float sc = (float) Math.pow(0.9, tier);
                return switch (ring) {
                    case 0 -> new Placement(0f, drop, 0f, 0f, sc, true);              // the right hip
                    case 1 -> new Placement(0f, drop, 0f, 180f, sc, false);           // the back, right
                    case 2 -> new Placement(0f, drop, 0f, 180f, sc, true);            // the back, left
                    default -> new Placement(0f, 2.4f + drop, 0f, 0f, sc * 0.95f, false);   // lower on the left hip
                };
            }
            case "NECK":                                            // layered chains: each lower, a little further from the chest, a little smaller
                return new Placement(0f, 2.2f * i, -0.35f * i, 0f, Math.max(0.6f, 1f - 0.08f * i), false);
            case "HEAD": {                                          // a crown, then the next one above it and slightly smaller, then to the sides
                if (i == 1) return new Placement(0f, -1.4f, 0f, 0f, 0.92f, false);
                int side = (i % 2 == 0) ? 1 : -1;
                int k = i / 2;                                     // 2,3 the first pair, 4,5 the next one further out
                return new Placement(3.2f * side * k, -0.8f, 0f, 0f, Math.max(0.6f, 0.8f - 0.05f * (k - 1)), false);
            }
            case "BACK":                                            // capes and mantles in layers, each further out
                return new Placement(0f, 0.4f * i, 0.9f * i, 0f, 1f + 0.04f * i, false);
            case "BELT":
                return new Placement(0f, 2.4f * i, 0f, 0f, Math.max(0.7f, 1f - 0.06f * i), false);
            case "HANDS": {                                         // wrist, then toward the elbow and toward the fingers
                int step = (i + 1) / 2, dir = (i % 2 == 1) ? -1 : 1;
                return new Placement(0f, dir * step * 2.6f, 0f, 0f, 1f, false);
            }
            case "FEET":
                return new Placement(0f, -1.2f * i, 0f, 0f, 1f, false);
            case "CHEST":
                return new Placement(0f, 0f, -0.4f * i, 0f, 1f + 0.03f * i, false);
            default:
                return new Placement(0f, 1.6f * i, 0f, 0f, Math.max(0.6f, 1f - 0.05f * i), false);
        }
    }

    /** Shoulder, pauldron, bracer and gauntlet pieces sit on the arms: they are pulled toward the body. */
    public static boolean hugsBody(String model) {
        if (model == null) return false;
        String m = model.toLowerCase(Locale.ROOT);
        return m.contains("pauldron") || m.contains("shoulder") || m.contains("bracer") || m.contains("gauntlet");
    }

    /**
     * The size of a worn model and, for the arm pieces, how far toward the body it moves (in arm-local pixels: +x is toward the body for the right arm, -x for the
     * left). A head item is not touched. The left arm's pieces are mirrored copies, so the sign is the arm's, not the piece's.
     */
    public static Placement fit(WornAnchor anchor, String model) {
        if (anchor == null || anchor == WornAnchor.HEAD) return Placement.NONE;
        if (!hugsBody(model) || (anchor != WornAnchor.RIGHT_ARM && anchor != WornAnchor.LEFT_ARM)) return new Placement(0f, 0f, 0f, 0f, BODY_SCALE, false);
        float in = anchor == WornAnchor.RIGHT_ARM ? 0.7f : -0.7f;
        return new Placement(in, -0.3f, 0f, 0f, BODY_SCALE * 0.94f, false);
    }

    /** The index of each entry among the entries of the same slot, in list order (["CHARM","NECK","CHARM"] gives [0,0,1]). */
    public static int[] indexes(java.util.List<String> slots) {
        int[] out = new int[slots.size()];
        java.util.Map<String, Integer> seen = new java.util.HashMap<>();
        for (int k = 0; k < out.length; k++) {
            String key = slots.get(k) == null ? "" : slots.get(k).toUpperCase(Locale.ROOT);
            int n = seen.getOrDefault(key, 0);
            out[k] = n;
            seen.put(key, n + 1);
        }
        return out;
    }
}
