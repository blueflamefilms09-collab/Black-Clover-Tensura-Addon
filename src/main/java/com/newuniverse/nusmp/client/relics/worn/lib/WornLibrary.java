package com.newuniverse.nusmp.client.relics.worn.lib;

import com.newuniverse.nusmp.client.relics.fx.FxTheme;
import com.newuniverse.nusmp.client.relics.fx.RelicFxOptions;
import com.newuniverse.nusmp.client.relics.fx.WornFxPlan;
import com.newuniverse.nusmp.client.relics.vu.VuModels;
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
        /**
         * 1.102.0 (visual upgrade): true for a relic worn as its upgraded models; {@code lodPieces} are its low detail models (at most 15% of the vertices, drawn beyond
         * {@link #LOD_DISTANCE} blocks and at the low quality), {@code lodVerts} their vertex count. Empty and 0 for every other ensemble.
         */
        public final boolean upgraded;
        public final List<Resolved> lodPieces;
        public final int lodVerts;
        /** 3.9.0: true when the pieces are the blocky x3 models of an armour relic (the classic ones are {@code lodPieces}); set once by the builder. */
        public boolean x3;

        /** Blocks beyond which an upgraded relic is drawn as its low detail models, and past which (see {@link #ADDITIVE_CUTOFF}) its additive passes are skipped. */
        public static final double LOD_DISTANCE = com.newuniverse.nusmp.relics.upgrade.UpgradeCaps.LOD_DISTANCE, ADDITIVE_CUTOFF = com.newuniverse.nusmp.relics.upgrade.UpgradeCaps.ADDITIVE_CUTOFF;

        Ensemble(RelicDef def, List<Resolved> pieces, FxTheme theme) {
            this(def, def.id(), def.slot().name(), false, WornPalette.slots(def.paletteMain(), def.paletteTrim(), def.paletteGlow()), def.grade().tier(), pieces, theme);
        }

        Ensemble(RelicDef def, String id, String slotKey, boolean exact, int[] colors, int tier, List<Resolved> pieces, FxTheme theme) {
            this(def, id, slotKey, exact, colors, tier, pieces, theme, false, List.of());
        }

        Ensemble(RelicDef def, String id, String slotKey, boolean exact, int[] colors, int tier, List<Resolved> pieces, FxTheme theme, boolean upgraded, List<Resolved> lodPieces) {
            this.upgraded = upgraded;
            this.lodPieces = lodPieces;
            int lv = 0;
            for (Resolved r : lodPieces) lv += r.mesh().totalVertices();
            this.lodVerts = lv;
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
                byte[] flags = r.mesh().flags;
                int n = r.mesh().quads * 12;
                for (int i = 0; i + 2 < n; i += 3) {
                    if ((flags[i / 12] & WornMesh.HALO) != 0) continue;                      // the soft light shapes of a visual upgrade model (rings, flares, shafts) do not size the object
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

        /** Does any of the models carry its own skin (a visual upgrade model)? */
        public boolean custom() {
            for (Resolved r : pieces) if (r.mesh().custom()) return true;
            return false;
        }

        /** Vertices the extras of a plan cost for this relic: the shell pass and the procedural motes, rings and halo. */
        public int extraVerts(WornFxPlan plan) { return (plan.usesShell() ? shellVerts : 0) + plan.fxVertices(); }
    }

    private static final Map<String, Optional<Ensemble>> CACHE = new HashMap<>();
    private static int seenGeneration = -1;
    private static boolean seenX3 = true, seenContact = true;

    /** The models of a relic, or null if the library has none for it (the layer then draws the old plain look). */
    public static Ensemble of(RelicDef def) {
        int gen = VuModels.generation();                                               // the visual upgrade switched on or off, or the resources reloaded: build again
        boolean x3 = RelicFxOptions.detailX3(), contact = RelicFxOptions.contactFit();
        if (gen != seenGeneration || x3 != seenX3 || contact != seenContact) {                // 3.9.0: a changed detail or fit setting builds again
            CACHE.clear();
            seenGeneration = gen;
            seenX3 = x3;
            seenContact = contact;
        }
        Optional<Ensemble> e = CACHE.get(def.id());
        if (e == null) {
            Ensemble built = build(def, true);
            if (built == null && !WornModels.catalogLoaded()) return null;                  // 1.99.0: the catalog was not readable just now: do not remember "no model" for the session
            e = Optional.ofNullable(built);
            CACHE.put(def.id(), e);
        }
        return e.orElse(null);
    }

    /** Where this relic's upgraded look comes from right now (NONE: the 1.101 look). */
    public static UpgradeSource sourceOf(String relicId) {
        if (!VuModels.enabled()) return UpgradeSource.NONE;
        return UpgradeSource.choose(true, VuModels.wornPieces(relicId) != null, WornModels.upgrade().has(relicId));
    }

    /** Is this relic worn as its God worn upgrade models right now (the shared switch {@code visual.upgrade} is on and the God catalog has it, and no override beats it)? */
    public static boolean upgradeOf(String relicId) { return sourceOf(relicId) == UpgradeSource.GOD_CATALOG; }

    /** Resolves pieces the way the library always did: the anchor of the piece, the fit of its body part, pushed out of the skin's second layer. */
    private static List<Resolved> resolve(List<WornCatalog.Piece> wanted) {
        List<Resolved> out = new ArrayList<>(wanted.size());
        for (WornCatalog.Piece p : wanted) {
            WornMesh m = WornModels.mesh(p.model());
            if (m == null) continue;
            WornAnchor at = p.anchor() != null ? p.anchor() : m.anchor;
            WornPlacer.Placement fit = WornPlacer.fit(at, p.model(), m.centreY(), RelicFxOptions.contactFit());
            m = m.clearOf(at, p.mirror(), fit.scale(), fit.dx(), fit.dy(), fit.dz());              // 1.71.0: never under the skin's second layer (hat, jacket, sleeves, pants)
            out.add(new Resolved(m, at, p.mirror()));
        }
        return out;
    }

    /**
     * 3.9.0: the pieces with each model swapped for its x3 version where the index lists one (same anchor, mirror and fit); null when none of them has one, or when an x3 file cannot be loaded,
     * or when the whole would pass the x3 ensemble cap (the relic then keeps its classic models).
     */
    private static List<Resolved> resolveX3(List<WornCatalog.Piece> wanted) {
        java.util.Set<String> index = WornModels.x3Models();
        if (index.isEmpty()) return null;
        List<WornCatalog.Piece> swapped = new ArrayList<>(wanted.size());
        boolean any = false;
        for (WornCatalog.Piece p : wanted) {
            if (index.contains(p.model()) && WornModels.mesh("x3/" + p.model()) != null) {
                swapped.add(new WornCatalog.Piece("x3/" + p.model(), p.anchor(), p.mirror()));
                any = true;
            } else {
                swapped.add(p);
            }
        }
        if (!any) return null;
        List<Resolved> out = resolve(swapped);
        int verts = 0;
        for (Resolved r : out) verts += r.mesh().totalVertices();
        return out.size() == wanted.size() && verts <= com.newuniverse.nusmp.relics.upgrade.UpgradeCaps.X3_ENSEMBLE_VERTICES ? out : null;
    }

    private static Ensemble build(RelicDef def, boolean allowUpgrade) {
        WornCatalog cat = WornModels.catalog();
        UpgradeSource src = allowUpgrade ? sourceOf(def.id()) : UpgradeSource.NONE;
        List<WornCatalog.Piece> wanted = switch (src) {
            case OVERRIDES -> VuModels.wornPieces(def.id());
            case GOD_CATALOG -> WornModels.upgrade().up(def.id());
            case NONE -> null;
        };
        if (wanted == null) wanted = cat.piecesFor(def.id(), def.slot().name(), def.look().name());
        if (wanted.isEmpty()) return null;
        List<Resolved> out = resolve(wanted);
        if (out.isEmpty()) {
            if (src == UpgradeSource.NONE) return null;
            return build(def, false);                                                        // an upgraded model that cannot be loaded: the 1.101 look, never nothing
        }
        FxTheme theme = FxTheme.byName(cat.themeOf(def.id()));
        if (theme == null) theme = FxTheme.fromGlow(def.paletteGlow());
        List<Resolved> lod = src == UpgradeSource.GOD_CATALOG ? resolve(WornModels.upgrade().lod(def.id())) : List.of();   // an override model names its own lod in its file (WornMesh#lod)
        if (src == UpgradeSource.NONE && def.slot() == com.newuniverse.nusmp.relics.RelicSlot.FEET && RelicFxOptions.detailX3()) {   // 3.9.0: a feet relic wears the blocky x3 version of each model that has one
            List<Resolved> blocky = resolveX3(wanted);
            if (blocky != null) {
                Ensemble e = new Ensemble(def, def.id(), def.slot().name(), false, WornPalette.slots(def.paletteMain(), def.paletteTrim(), def.paletteGlow()), def.grade().tier(), List.copyOf(blocky), theme,
                        true, List.copyOf(out));
                e.x3 = true;
                return e;
            }
        }
        return new Ensemble(def, def.id(), def.slot().name(), false, WornPalette.slots(def.paletteMain(), def.paletteTrim(), def.paletteGlow()), def.grade().tier(), List.copyOf(out), theme,
                src != UpgradeSource.NONE, List.copyOf(lod));
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
        boolean anime = WornModels.catalog().wearsAnime(path, RelicFxOptions.robeAnime(), RelicFxOptions.robeAnimeBare());   // 3.1.2: a legs / boots / bare-head anime entry (`none`) falls back to the older model
        String cacheKey = (anime ? "ANIME:" : "") + slot.key() + ":" + path;
        Optional<Ensemble> e = ITEM_CACHE.get(cacheKey);
        if (e == null) {
            Ensemble built = buildKey(anime ? key : path, slot.key(), slot, true);
            if (built == null && !WornModels.catalogLoaded()) return null;                  // 1.99.0: not read yet: do not remember "nothing"
            e = Optional.ofNullable(built);
            ITEM_CACHE.put(cacheKey, e);
        }
        return e.orElse(null);
    }

    /** The models of a mode overlay ({@code mode_wind_spirit_dive} ...) from the catalog; null if it has no entry. */
    public static Ensemble ofKey(String key) {
        if (key == null) return null;
        Optional<Ensemble> e = ITEM_CACHE.get("KEY:" + key);
        if (e == null) {
            Ensemble built = buildKey(key, "MODE", null, true);
            if (built == null && !WornModels.catalogLoaded()) return null;
            e = Optional.ofNullable(built);
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
            Ensemble built = slot == null ? null : buildKey(path, slot, null, false);
            if (built == null && !WornModels.catalogLoaded()) return null;
            e = Optional.ofNullable(built);
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
