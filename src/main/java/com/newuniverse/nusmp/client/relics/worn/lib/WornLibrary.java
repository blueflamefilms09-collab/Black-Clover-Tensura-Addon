package com.newuniverse.nusmp.client.relics.worn.lib;

import com.newuniverse.nusmp.client.relics.fx.FxTheme;
import com.newuniverse.nusmp.client.relics.fx.RelicFxOptions;
import com.newuniverse.nusmp.client.relics.fx.WornFxPlan;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The answer to "what does this relic look like on the body": the data-driven link between a relic def (its slot, look, palette, grade, id) and the models of the
 * library ({@link WornCatalog}: a signature entry by id, else the models of its slot and look). The result of a relic is built once and cached, so the render path
 * only reads it. Client thread only.
 */
public final class WornLibrary {
    private WornLibrary() {}

    /** One model on one body part. */
    public record Resolved(WornMesh mesh, WornAnchor anchor, boolean mirror) {}

    /**
     * Everything the draw loop needs about one worn thing: its models, its six colours, its grade tier, its costs and its particle theme. It is made for a relic
     * ({@link #of}), for an armour item of the mod ({@link #ofItem}) or for a mode overlay ({@link #ofKey}); {@code id} and {@code slotKey} are what the placement
     * allocator and the draw order use, whichever it is.
     */
    public static final class Ensemble {
        /** The relic this is for; null for an armour item or a mode. */
        public final RelicDef def;
        public final String id, slotKey;
        /** True for an armour item or a mode: its models are drawn at full size where the garment stands (no shrink, no place among slot-mates). */
        public final boolean exact;
        public final List<Resolved> pieces;
        public final int[] colors;
        public final int tier, baseVerts, shellVerts;
        public final FxTheme theme;

        Ensemble(RelicDef def, List<Resolved> pieces, FxTheme theme) {
            this(def, def.id(), def.slot().name(), false, WornPalette.slots(def.paletteMain(), def.paletteTrim(), def.paletteGlow()), def.grade().tier(), pieces, theme);
        }

        Ensemble(RelicDef def, String id, String slotKey, boolean exact, int[] colors, int tier, List<Resolved> pieces, FxTheme theme) {
            this.def = def;
            this.id = id;
            this.slotKey = slotKey;
            this.exact = exact;
            this.pieces = pieces;
            this.colors = colors;
            this.tier = tier;
            int b = 0, s = 0;
            for (Resolved r : pieces) {
                b += r.mesh().totalVertices();
                s += r.mesh().shellVertices();
            }
            this.baseVerts = b;
            this.shellVerts = s;
            this.theme = theme;
        }

        private float[] bounds;

        /** The box {minX, minY, minZ, maxX, maxY, maxZ} of all the pieces in the rest pose of the player model (layer space, model pixels), computed once. */
        public float[] bounds() {
            if (bounds != null) return bounds;
            float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
            for (Resolved r : pieces) {
                float ox = 0f, oy = 0f;
                switch (r.anchor()) {
                    case RIGHT_ARM -> { ox = -5f; oy = 2f; }
                    case LEFT_ARM -> { ox = 5f; oy = 2f; }
                    case RIGHT_LEG -> { ox = -1.9f; oy = 12f; }
                    case LEFT_LEG -> { ox = 1.9f; oy = 12f; }
                    default -> { }
                }
                float[] pos = r.mesh().pos;
                int n = r.mesh().quads * 12;
                for (int i = 0; i + 2 < n; i += 3) {
                    float x = (r.mirror() ? -pos[i] : pos[i]) + ox, y = pos[i + 1] + oy, z = pos[i + 2];
                    b[0] = Math.min(b[0], x); b[3] = Math.max(b[3], x);
                    b[1] = Math.min(b[1], y); b[4] = Math.max(b[4], y);
                    b[2] = Math.min(b[2], z); b[5] = Math.max(b[5], z);
                }
            }
            if (b[0] > b[3]) b = new float[]{-8f, -8f, -8f, 8f, 8f, 8f};
            bounds = b;
            return b;
        }

        /** Vertices the extras of a plan cost for this relic: the shell pass and the procedural motes, rings and halo. */
        public int extraVerts(WornFxPlan plan) { return (plan.usesShell() ? shellVerts : 0) + plan.fxVertices(); }
    }

    private static final Map<String, Optional<Ensemble>> CACHE = new HashMap<>();

    /** The models of a relic, or null if the library has none for it (the layer then draws the old plain look). */
    public static Ensemble of(RelicDef def) {
        Optional<Ensemble> e = CACHE.get(def.id());
        if (e == null) {
            e = Optional.ofNullable(build(def));
            CACHE.put(def.id(), e);
        }
        return e.orElse(null);
    }

    private static Ensemble build(RelicDef def) {
        WornCatalog cat = WornModels.catalog();
        List<WornCatalog.Piece> wanted = cat.piecesFor(def.id(), def.slot().name(), def.look().name());
        if (wanted.isEmpty()) return null;
        List<Resolved> out = new ArrayList<>(wanted.size());
        for (WornCatalog.Piece p : wanted) {
            WornMesh m = WornModels.mesh(p.model());
            if (m == null) continue;
            WornAnchor at = p.anchor() != null ? p.anchor() : m.anchor;
            WornPlacer.Placement fit = WornPlacer.fit(at, p.model());
            m = m.clearOf(at, p.mirror(), fit.scale(), fit.dx(), fit.dy(), fit.dz());              // 1.71.0: never under the skin's second layer (hat, jacket, sleeves, pants)
            out.add(new Resolved(m, at, p.mirror()));
        }
        if (out.isEmpty()) return null;
        FxTheme theme = FxTheme.byName(cat.themeOf(def.id()));
        if (theme == null) theme = FxTheme.fromGlow(def.paletteGlow());
        return new Ensemble(def, List.copyOf(out), theme);
    }

    private static final Map<String, Optional<Ensemble>> ITEM_CACHE = new HashMap<>();

    /**
     * The models of an armour item of this mod ({@code path} is its registry path, {@code slot} the slot of the piece): the catalog entry of the item, else the generic look
     * ({@link ArmorLooks}) of its slot and name, so any armour item gets a decent 3D piece. Null only when the library cannot load a single model of it. Built once.
     */
    public static Ensemble ofItem(String path, ArmorLooks.Slot slot) {
        if (path == null || slot == null) return null;
        // 1.48.0: with relicfx.robeAnime on, an item that has an `anime:` entry wears that look (cloak only); the older look stays the fallback and the option's off state
        String key = WornCatalog.animeKey(path);
        boolean anime = RelicFxOptions.robeAnime() && WornModels.catalog().hasItem(key);
        String cacheKey = (anime ? "ANIME:" : "") + slot.key() + ":" + path;
        Optional<Ensemble> e = ITEM_CACHE.get(cacheKey);
        if (e == null) {
            e = Optional.ofNullable(buildKey(anime ? key : path, slot.key(), slot, true));
            ITEM_CACHE.put(cacheKey, e);
        }
        return e.orElse(null);
    }

    /** The models of a mode overlay ({@code mode_wind_spirit_dive} ...) from the catalog; null if it has no entry. */
    public static Ensemble ofKey(String key) {
        if (key == null) return null;
        Optional<Ensemble> e = ITEM_CACHE.get("KEY:" + key);
        if (e == null) {
            e = Optional.ofNullable(buildKey(key, "MODE", null, true));
            ITEM_CACHE.put("KEY:" + key, e);
        }
        return e.orElse(null);
    }

    /**
     * The models of a Curios-worn item of the mod that is not a relic (a dungeon relic, a charter grimoire ...): only when the catalog names an {@code item} entry and an
     * {@code islot} for it. It is drawn and placed like a relic of that slot. Null otherwise (a weapon worn in Curios has no worn model).
     */
    public static Ensemble ofCurio(String path) {
        if (path == null) return null;
        Optional<Ensemble> e = ITEM_CACHE.get("CURIO:" + path);
        if (e == null) {
            String slot = WornModels.catalog().itemSlot(path);
            e = Optional.ofNullable(slot == null ? null : buildKey(path, slot, null, false));
            ITEM_CACHE.put("CURIO:" + path, e);
        }
        return e.orElse(null);
    }

    /** The model of the last resort: a small floating charm. Used for a worn thing whose own models cannot be loaded, so nothing is ever invisible. */
    private static final String FALLBACK_MODEL = "charm_orbit";

    /** The generic floating charm for a relic with no loadable worn model, tinted with its palette; null only when even the generic model cannot be loaded. Built once. */
    public static Ensemble fallbackCharm(RelicDef def) {
        if (def == null) return null;
        Optional<Ensemble> e = ITEM_CACHE.get("FB:" + def.id());
        if (e == null) {
            WornMesh m = WornModels.mesh(FALLBACK_MODEL);
            Ensemble made = null;
            if (m != null) {
                FxTheme theme = FxTheme.fromGlow(def.paletteGlow());
                made = new Ensemble(def, List.of(new Resolved(m, m.anchor, false)), theme);
            }
            e = Optional.ofNullable(made);
            ITEM_CACHE.put("FB:" + def.id(), e);
        }
        return e.orElse(null);
    }

    /** The same for a Curios-worn item of the mod that is not a relic and has no catalog entry (palette from its name). */
    public static Ensemble fallbackCharm(String path) {
        if (path == null) return null;
        Optional<Ensemble> e = ITEM_CACHE.get("FBP:" + path);
        if (e == null) {
            WornMesh m = WornModels.mesh(FALLBACK_MODEL);
            Ensemble made = null;
            if (m != null) {
                int[] pal = ArmorLooks.palette(path);
                made = new Ensemble(null, path, "CHARM", false, pal, 2, List.of(new Resolved(m, m.anchor, false)), FxTheme.fromGlow(pal[2]));
            }
            e = Optional.ofNullable(made);
            ITEM_CACHE.put("FBP:" + path, e);
        }
        return e.orElse(null);
    }

    private static Ensemble buildKey(String key, String slotKey, ArmorLooks.Slot slot, boolean exact) {
        WornCatalog cat = WornModels.catalog();
        List<WornCatalog.Piece> wanted = cat.itemPieces(key);
        int[] pal = cat.itemPalette(key);
        int tier = cat.itemTier(key, -1);
        if (wanted.isEmpty() && slot != null) {                                           // the generic look of a future item
            ArmorLooks.Generic g = ArmorLooks.resolve(slot, key);
            List<WornCatalog.Piece> gp = new ArrayList<>(g.pieces().size());
            for (String s : g.pieces()) gp.add(WornCatalog.piece(s));
            wanted = gp;
            if (pal == null) pal = g.palette();
            if (tier < 0) tier = g.tier();
        }
        if (wanted.isEmpty()) return null;
        if (pal == null) pal = ArmorLooks.palette(key);
        if (tier < 0) tier = 2;
        boolean nothing = WornCatalog.isNothing(wanted);                                  // an anime robe's legs and boots: the library owns the piece but draws no model
        List<Resolved> out = new ArrayList<>(wanted.size());
        for (WornCatalog.Piece p : wanted) {
            if (WornCatalog.NONE.equals(p.model())) continue;
            WornMesh m = WornModels.mesh(p.model());
            if (m == null) continue;
            out.add(new Resolved(m, p.anchor() != null ? p.anchor() : m.anchor, p.mirror()));
        }
        if (out.isEmpty() && !nothing) return null;
        FxTheme theme = FxTheme.byName(cat.themeOf(key));
        if (theme == null) theme = FxTheme.fromGlow(pal[2]);
        return new Ensemble(null, key, slotKey, exact, WornPalette.slots(pal[0], pal[1], pal[2]), tier, List.copyOf(out), theme);
    }

    /**
     * An ensemble of the given pieces coloured and graded like this relic, built fresh and not cached: the hand library ({@code client.relics.hand}) uses it for the models a
     * relic is held as (a signature hand model, or one glove of the worn pair) so they draw through the same code as the worn ones.
     */
    public static Ensemble ofPieces(RelicDef def, List<Resolved> pieces) {
        return new Ensemble(def, List.copyOf(pieces), FxTheme.fromGlow(def.paletteGlow()));
    }

    /** Does this relic have worn models? */
    public static boolean hasModel(RelicDef def) { return of(def) != null; }

    /**
     * Does the 3D model replace the painted armour box? True for a head or feet relic with models: the plain painted helmet or boot box would hide the model
     * (armour sits one pixel outside the body), so its texture is left clear.
     */
    public static boolean hidesArmor(RelicDef def) {
        return (def.slot() == RelicSlot.HEAD || def.slot() == RelicSlot.FEET) && hasModel(def);
    }

    /** Forgets everything built (a resource reload). */
    public static void clear() {
        CACHE.clear();
        ITEM_CACHE.clear();
    }
}
