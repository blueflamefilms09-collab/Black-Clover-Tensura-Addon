package com.newuniverse.nusmp.client.relics.djinn;

import com.newuniverse.nusmp.mesh.MeshText;
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
 * The armour meshes of the Djinn (assets/nusmp/djinn/&lt;set&gt;/&lt;piece&gt;.txt, written by tools/relics/gen_djinn_art.py and gen_djinn_unique.py with
 * mesh_lib): one file per piece, lines {@code qn <layer> (x y z u v nx ny nz) x4}, UV in 128 units. Parsed once per resource reload into flat float
 * arrays (8 floats a vertex, 4 vertices a quad); layer 0 is drawn lit, layer 2 additive. A missing or broken file is logged once and draws nothing,
 * never an exception on the render thread.
 *
 * <p>Every Djinn has up to three sets ({@link DjinnLook}): its unique look {@code u_<id>}, its Dark Djinn look {@code d_<id>} and the shared rig it
 * had first ({@code <id>}, kept as the fallback). {@link #setOf} picks ONE set per Djinn and variant (the first of the preferred chain whose marker
 * piece exists) and {@link #get} reads the pieces of that set only. All pieces are in the player model's pixel space (y down, -z the front), except
 * the WEAPON, which is in the held item's space (the grip at the origin, the blade along +y).
 */
public final class DjinnMeshes {
    private DjinnMeshes() {}

    /** The pieces, with the file name each is stored under. New pieces are only ever appended. */
    public enum Piece {
        HELM, HORN_L, HORN_R, EARS, CREST, CHEST, BELT, PAULDRON_L, PAULDRON_R, VAMBRACE_L, VAMBRACE_R, GAUNTLET_L, GAUNTLET_R, GREAVE_L, GREAVE_R,
        WING_IN_L, WING_IN_R, WING_OUT_L, WING_OUT_R, TAIL_SEG, TAIL_TIP, HALO, SHEATH,
        /** The Half Equip weapon of the unique look (held, item space). */
        WEAPON,
        /** The Dark Djinn's eye crest on the brow. */
        EYE,
        /** The unique piece standing behind the back: a ring, an arch, a pair of extra arms. */
        BACKDROP,
        /** The mantle: a cape, scarves, a ruff. */
        MANTLE;

        public String file() { return name().toLowerCase(); }
    }

    /** One piece's quads: {@code body} (lit) and {@code glow} (additive), each 32 floats a quad, and the set ({@link DjinnLook#BASE} ...) they came from (which skin they use). */
    public static final class Data {
        public final float[] body, glow;
        public final int kind;
        Data(float[] body, float[] glow, int kind) { this.body = body; this.glow = glow; this.kind = kind; }
        static final Data EMPTY = new Data(new float[0], new float[0], DjinnLook.BASE);
        public boolean isEmpty() { return body.length == 0 && glow.length == 0; }
    }

    private static final int N = Djinn.values().length;
    /** [dark ? 1 : 0][djinn ordinal][piece ordinal], filled on first use (nothing is parsed at resource reload, and never twice per reload). */
    private static final Data[][][] CACHE = new Data[2][N][Piece.values().length];
    /** The set each [dark][djinn] is drawn from, -1 until decided. */
    private static final int[][] SET = new int[2][N];
    private static final ResourceLocation[][] SKIN = new ResourceLocation[3][N], GLOW = new ResourceLocation[3][N];
    private static final ResourceLocation[][] RUNE = new ResourceLocation[2][N], BUST = new ResourceLocation[2][N];
    private static final ResourceLocation[] ART = new ResourceLocation[N];
    private static final Map<String, Boolean> WARNED = new HashMap<>(), EXISTS = new HashMap<>();

    static { clearSets(); }

    public static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    private static void clearSets() { for (int[] row : SET) java.util.Arrays.fill(row, -1); }

    /** The armour skin of a set. */
    public static ResourceLocation skin(Djinn d, int kind) {
        ResourceLocation r = SKIN[kind][d.ordinal()];
        return r != null ? r : (SKIN[kind][d.ordinal()] = rl(DjinnLook.skinPath(d.id(), kind, false)));
    }

    public static ResourceLocation glowSkin(Djinn d, int kind) {
        ResourceLocation r = GLOW[kind][d.ordinal()];
        return r != null ? r : (GLOW[kind][d.ordinal()] = rl(DjinnLook.skinPath(d.id(), kind, true)));
    }

    /** The first look's skin (kept for callers that know no sets). */
    public static ResourceLocation skin(Djinn d) { return skin(d, DjinnLook.BASE); }

    public static ResourceLocation glowSkin(Djinn d) { return glowSkin(d, DjinnLook.BASE); }

    /** The 64x64 body-art overlay of the Maso: the dark one, the Djinn's own pattern if it exists, else the shared rune. */
    public static ResourceLocation rune(Djinn d, boolean dark) {
        int i = dark ? 1 : 0;
        ResourceLocation r = RUNE[i][d.ordinal()];
        if (r != null) return r;
        boolean unique = !dark && exists(DjinnLook.runePath(d.id(), false, true));
        return RUNE[i][d.ordinal()] = rl(DjinnLook.runePath(d.id(), dark, unique));
    }

    public static ResourceLocation rune(Djinn d) { return rune(d, false); }

    /** The Lesser Summon's bust (the dark one for a dark Djinn if the file exists). */
    public static ResourceLocation bust(Djinn d, boolean dark) {
        int i = dark ? 1 : 0;
        ResourceLocation r = BUST[i][d.ordinal()];
        if (r != null) return r;
        return BUST[i][d.ordinal()] = rl(DjinnLook.bustPath(d.id(), dark, dark && exists(DjinnLook.bustPath(d.id(), true, true))));
    }

    public static ResourceLocation bust(Djinn d) { return bust(d, false); }

    /** The black veins drawn over a dark Djinn's skin, or null if the file is missing. */
    public static ResourceLocation art(Djinn d) {
        ResourceLocation r = ART[d.ordinal()];
        if (r != null) return r;
        String path = DjinnLook.artPath(d.id());
        return exists(path) ? (ART[d.ordinal()] = rl(path)) : null;
    }

    private static boolean exists(String path) {
        Boolean b = EXISTS.get(path);
        if (b == null) {
            Minecraft mc = Minecraft.getInstance();
            b = mc != null && mc.getResourceManager().getResource(rl(path)).isPresent();
            EXISTS.put(path, b);
        }
        return b;
    }

    public static void clear() {
        for (Data[][] sets : CACHE) for (Data[] row : sets) java.util.Arrays.fill(row, null);
        clearSets();
        for (ResourceLocation[] row : RUNE) java.util.Arrays.fill(row, null);
        for (ResourceLocation[] row : BUST) java.util.Arrays.fill(row, null);
        java.util.Arrays.fill(ART, null);
        WARNED.clear();
        EXISTS.clear();
    }

    /** The set a Djinn is drawn from: the first of {@link DjinnLook#chain} whose marker piece exists ({@link DjinnLook#BASE} if none does). */
    public static int setOf(Djinn d, boolean dark) {
        int i = dark ? 1 : 0, cached = SET[i][d.ordinal()];
        if (cached >= 0) return cached;
        int found = DjinnLook.pickSet(dark, kind -> exists("djinn/" + DjinnLook.dir(d.id(), kind) + "/" + DjinnLook.MARKER_PIECE + ".txt"));
        return SET[i][d.ordinal()] = found;
    }

    public static Data get(Djinn d, Piece p) { return get(d, p, false); }

    public static Data get(Djinn d, Piece p, boolean dark) {
        Data[] row = CACHE[dark ? 1 : 0][d.ordinal()];
        Data cached = row[p.ordinal()];
        if (cached != null) return cached;
        int kind = setOf(d, dark);
        Data data = load(DjinnLook.dir(d.id(), kind) + "/" + p.file(), kind);
        row[p.ordinal()] = data;
        return data;
    }

    private static Data load(String key, int kind) {
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl("djinn/" + key + ".txt"));
            if (res.isEmpty()) return Data.EMPTY;
            List<float[]> body = new ArrayList<>(), glow = new ArrayList<>();
            try (BufferedReader in = MeshText.reader(res.get())) {
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
            return new Data(flatten(body), flatten(glow), kind);
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
