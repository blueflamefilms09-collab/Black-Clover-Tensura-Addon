package com.newuniverse.nusmp.client.relics.djinn;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The armour meshes of the Djinn (assets/nusmp/djinn/&lt;id&gt;/&lt;piece&gt;.txt, written by tools/relics/gen_djinn_art.py with mesh_lib): one file per piece,
 * lines {@code qn <layer> (x y z u v nx ny nz) x4} in the player model's own pixel space (y down, -z the front), UV in 128 units. Parsed once per
 * resource reload into flat float arrays (8 floats a vertex, 4 vertices a quad); layer 0 is drawn lit, layer 2 additive. A missing or broken file
 * is logged once and draws nothing, never an exception on the render thread.
 */
public final class DjinnMeshes {
    private DjinnMeshes() {}

    /** The pieces, with the file name each is stored under. */
    public enum Piece {
        HELM, HORN_L, HORN_R, EARS, CREST, CHEST, BELT, PAULDRON_L, PAULDRON_R, VAMBRACE_L, VAMBRACE_R, GAUNTLET_L, GAUNTLET_R, GREAVE_L, GREAVE_R,
        WING_IN_L, WING_IN_R, WING_OUT_L, WING_OUT_R, TAIL_SEG, TAIL_TIP, HALO, SHEATH;

        public String file() { return name().toLowerCase(); }
    }

    /** One piece's quads: {@code body} (lit) and {@code glow} (additive), each 32 floats a quad. */
    public static final class Data {
        public final float[] body, glow;
        Data(float[] body, float[] glow) { this.body = body; this.glow = glow; }
        static final Data EMPTY = new Data(new float[0], new float[0]);
    }

    /** [djinn ordinal][piece ordinal], filled on first use (nothing is parsed at resource reload, and never twice per reload). */
    private static final Data[][] CACHE = new Data[Djinn.values().length][Piece.values().length];
    private static final ResourceLocation[] SKIN = new ResourceLocation[Djinn.values().length], GLOW = new ResourceLocation[Djinn.values().length],
            RUNE = new ResourceLocation[Djinn.values().length], BUST = new ResourceLocation[Djinn.values().length];
    private static final Map<String, Boolean> WARNED = new HashMap<>();

    public static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    public static ResourceLocation skin(Djinn d) {
        ResourceLocation r = SKIN[d.ordinal()];
        return r != null ? r : (SKIN[d.ordinal()] = rl("textures/entity/djinn/" + d.id() + "_armor.png"));
    }

    public static ResourceLocation glowSkin(Djinn d) {
        ResourceLocation r = GLOW[d.ordinal()];
        return r != null ? r : (GLOW[d.ordinal()] = rl("textures/entity/djinn/" + d.id() + "_armor_glow.png"));
    }

    /** The 64x64 body-art overlay of the Maso. */
    public static ResourceLocation rune(Djinn d) {
        ResourceLocation r = RUNE[d.ordinal()];
        return r != null ? r : (RUNE[d.ordinal()] = rl("textures/entity/djinn/" + d.id() + "_rune.png"));
    }

    /** The Lesser Summon's bust. */
    public static ResourceLocation bust(Djinn d) {
        ResourceLocation r = BUST[d.ordinal()];
        return r != null ? r : (BUST[d.ordinal()] = rl("textures/entity/djinn/" + d.id() + "_bust.png"));
    }

    public static void clear() {
        for (Data[] row : CACHE) java.util.Arrays.fill(row, null);
        WARNED.clear();
    }

    public static Data get(Djinn d, Piece p) {
        Data cached = CACHE[d.ordinal()][p.ordinal()];
        if (cached != null) return cached;
        Data data = load(d, p, d.id() + "/" + p.file());
        CACHE[d.ordinal()][p.ordinal()] = data;
        return data;
    }

    private static Data load(Djinn d, Piece p, String key) {
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl("djinn/" + key + ".txt"));
            if (res.isEmpty()) return Data.EMPTY;
            List<float[]> body = new ArrayList<>(), glow = new ArrayList<>();
            try (BufferedReader in = res.get().openAsReader()) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (!line.startsWith("qn ")) continue;
                    String[] v = line.trim().split("\\s+");
                    int layer = Integer.parseInt(v[1]);
                    float[] q = new float[32];
                    for (int k = 0; k < 4; k++) {
                        int o = 2 + k * 8, w = k * 8;
                        for (int j = 0; j < 8; j++) q[w + j] = Float.parseFloat(v[o + j]);
                        q[w + 3] /= 128f;
                        q[w + 4] /= 128f;
                    }
                    if (layer == 0) body.add(q);
                    else if (layer == 2) glow.add(q);
                }
            }
            return new Data(flatten(body), flatten(glow));
        } catch (Exception ex) {
            if (WARNED.put(key, Boolean.TRUE) == null) LogUtils.getLogger().error("[nusmp] Djinn armour piece {} failed to load; it is not drawn", key, ex);
            return Data.EMPTY;
        }
    }

    private static float[] flatten(List<float[]> quads) {
        float[] out = new float[quads.size() * 32];
        for (int i = 0; i < quads.size(); i++) System.arraycopy(quads.get(i), 0, out, i * 32, 32);
        return out;
    }
}
