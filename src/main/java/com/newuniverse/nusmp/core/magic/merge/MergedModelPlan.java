package com.newuniverse.nusmp.core.magic.merge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The layout of a merged magic's floating 3D emblem (pure): which pieces of which parents, where on the ring round the caster, how big.
 * A parent that has real geo models (bronze crest, corundum crown, a key ...) contributes up to two of them, shrunk to trinkets; every
 * parent also contributes procedural crystals in its colour; and one fusion gem in the blended colour sits in the middle. The plan is a
 * function of the merged id and the palette only, so every client builds the same emblem. The client bakes it once into a mesh
 * ({@code client.merge.MergedMesh}) and replays that every frame.
 */
public final class MergedModelPlan {
    private MergedModelPlan() {}

    public enum Kind { GEO, CRYSTAL, GEM }

    /**
     * @param parent  index of the parent in canonical order (-1 for the fusion gem)
     * @param geoKey  GeoSpec key ("bronze"), null unless GEO
     * @param geoName GeoSpec name ("crest"), null unless GEO
     * @param angle   degrees round the caster's back (0 = straight behind)
     * @param radius  blocks from the body axis
     * @param height  blocks above the feet
     * @param scale   size factor (crystal height in blocks, or the geo model's scale)
     * @param spin    degrees per second the piece turns on its own axis
     * @param lean    degrees the piece leans outward
     */
    public record Piece(Kind kind, int parent, String geoKey, String geoName, float angle, float radius, float height, float scale, float spin, float lean) {}

    /** The geo models each magic can lend (key = geo file prefix, name = the rest: {@code geo/entity/<key>_<name>.geo.json}). */
    private static final Map<String, String[][]> GEO = new HashMap<>();

    static {
        GEO.put("BRONZE", new String[][]{{"bronze", "crest"}, {"bronze", "pauldron_r"}});
        GEO.put("COPPER", new String[][]{{"copper", "crown"}, {"copper", "pauldrons"}});
        GEO.put("CORUNDUM", new String[][]{{"corundum", "crown"}, {"corundum", "gem_shard"}});
        GEO.put("KEY", new String[][]{{"key", "mini_key"}});
        GEO.put("BEAST", new String[][]{{"beast", "form_head"}});
    }

    /** True when the magic lends real geo models to a merged emblem. */
    public static boolean hasGeo(String parent) { return GEO.containsKey(parent); }

    /** The geo models a parent can lend ({key, name} pairs); empty for most magics. */
    public static List<String[]> geoOf(String parent) {
        List<String[]> out = new ArrayList<>();
        String[][] g = GEO.get(parent);
        if (g != null) for (String[] s : g) out.add(s.clone());
        return out;
    }

    /** Deterministic jitter in [-1, 1) from a key and an index. */
    static float jitter(int key, int i) {
        int h = key * 0x9E3779B1 + i * 0x85EBCA6B;
        h ^= h >>> 15; h *= 0x2C1B3C6D; h ^= h >>> 12;
        return ((h & 0xFFFF) / 32768f) - 1f;
    }

    /** Builds the plan: at most {@link #MAX_PIECES} pieces. */
    public static List<Piece> plan(MergedDef def) {
        List<Piece> out = new ArrayList<>();
        List<String> parents = def.parents();
        int n = parents.size();
        int key = def.key();
        // 1. what each parent lends, in turn so that no parent is hidden when the cap is reached
        List<Piece> pool = new ArrayList<>();
        int rounds = 3;
        for (int round = 0; round < rounds; round++) {
            for (int p = 0; p < n; p++) {
                List<String[]> geo = geoOf(parents.get(p));
                if (round < geo.size() && round < 2) pool.add(new Piece(Kind.GEO, p, geo.get(round)[0], geo.get(round)[1], 0, 0, 0, 0, 0, 0));
                else pool.add(new Piece(Kind.CRYSTAL, p, null, null, 0, 0, 0, 0, 0, 0));
            }
        }
        int count = Math.min(MAX_PIECES - 1, pool.size());
        // 2. place them on a ring, parents interleaved, with a small deterministic wobble
        for (int k = 0; k < count; k++) {
            Piece b = pool.get(k);
            float base = 360f * k / count;
            boolean geo = b.kind() == Kind.GEO;
            float ring = 0.62f + 0.10f * (k % 2) + 0.05f * jitter(key, k);
            float height = 1.05f + 0.55f * (((k * 5) % count) / (float) Math.max(1, count - 1)) + 0.06f * jitter(key, k + 31);
            float scale = geo ? 0.34f + 0.06f * jitter(key, k + 7) : 0.34f + 0.10f * (1f + jitter(key, k + 11));
            float spin = (k % 2 == 0 ? 1f : -1f) * (24f + 14f * (1f + jitter(key, k + 3)));
            float lean = 14f + 10f * (1f + jitter(key, k + 19));
            out.add(new Piece(b.kind(), b.parent(), b.geoKey(), b.geoName(), base + 14f * jitter(key, k + 5), ring, height, scale, spin, lean));
        }
        // 3. the fusion gem, low behind the shoulders
        out.add(new Piece(Kind.GEM, -1, null, null, 0f, 0.36f, 1.30f, 0.55f, 40f, 0f));
        return out;
    }

    /** At most this many pieces (including the gem): the baked mesh stays small. */
    public static final int MAX_PIECES = 9;
}
