package com.newuniverse.nusmp.client.relics.worn.lib;

import com.newuniverse.nusmp.client.relics.fx.FxTheme;
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

    /** Everything the draw loop needs about one relic: its models, its six colours, its grade tier, its costs and its particle theme. */
    public static final class Ensemble {
        public final RelicDef def;
        public final List<Resolved> pieces;
        public final int[] colors;
        public final int tier, baseVerts, shellVerts;
        public final FxTheme theme;

        Ensemble(RelicDef def, List<Resolved> pieces, FxTheme theme) {
            this.def = def;
            this.pieces = pieces;
            this.colors = WornPalette.slots(def.paletteMain(), def.paletteTrim(), def.paletteGlow());
            this.tier = def.grade().tier();
            int b = 0, s = 0;
            for (Resolved r : pieces) {
                b += r.mesh().totalVertices();
                s += r.mesh().shellVertices();
            }
            this.baseVerts = b;
            this.shellVerts = s;
            this.theme = theme;
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
            out.add(new Resolved(m, p.anchor() != null ? p.anchor() : m.anchor, p.mirror()));
        }
        if (out.isEmpty()) return null;
        FxTheme theme = FxTheme.byName(cat.themeOf(def.id()));
        if (theme == null) theme = FxTheme.fromGlow(def.paletteGlow());
        return new Ensemble(def, List.copyOf(out), theme);
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
    public static void clear() { CACHE.clear(); }
}
