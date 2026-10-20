package com.newuniverse.nusmp.client.relics.hand;

import com.newuniverse.nusmp.client.relics.vu.VuModels;
import com.newuniverse.nusmp.client.relics.vu.VuOverrides;
import com.newuniverse.nusmp.client.relics.worn.lib.WornCatalog;
import com.newuniverse.nusmp.client.relics.worn.lib.WornLibrary;
import com.newuniverse.nusmp.client.relics.worn.lib.WornMesh;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Why: the renderer must not decide, every frame, which models a relic is held as and how: that is answered once per relic here and cached. A relic is held as its own
 * hand model when the hand catalog names one (the signature objects and every consumable), else as the model it wears (the worn library: a ring, a necklace, a hood, a
 * glove ...), shown with the hold style its id, slot and look select in transforms.txt. A relic with neither has no form and keeps its flat sprite. Client thread only.
 */
public final class HandForms {
    private HandForms() {}

    /** What one relic is held as: the models (an ensemble the worn library can draw) and the style that places them. */
    public record Form(WornLibrary.Ensemble ensemble, HandTransforms.Style style, boolean signature) {}

    private static final Map<String, Optional<Form>> CACHE = new HashMap<>();
    private static int seenGeneration = -1;

    /** The form of a relic, or null (its sprite is drawn). Built once per relic id. */
    public static Form of(RelicDef def) {
        int gen = VuModels.generation();                                               // the visual upgrade switched on or off, or the resources reloaded: build the forms again
        if (gen != seenGeneration) {
            CACHE.clear();
            seenGeneration = gen;
        }
        Optional<Form> f = CACHE.get(def.id());
        if (f == null) {
            f = Optional.ofNullable(build(def));
            CACHE.put(def.id(), f);
        }
        return f.orElse(null);
    }

    private static Form build(RelicDef def) {
        HandTransforms xf = HandModels.transforms();
        if (!xf.complete()) return null;
        HandTransforms.Style style = xf.styleFor(def.id(), def.slot().name(), def.look().name());
        if (style == null) return null;
        VuOverrides.Entry upgrade = VuModels.handEntry(def.id());                      // the visual upgrade: the override table of assets/nusmp/visual_upgrade (null: the older catalog)
        List<WornCatalog.Piece> sig = VuModels.handPieces(def.id());
        if (sig == null) sig = HandModels.catalog().sigFor(def.id());
        else style = upgradeStyle(xf, style, upgrade);
        boolean signature = !sig.isEmpty();
        List<WornLibrary.Resolved> pieces = new ArrayList<>();
        if (signature) {
            for (WornCatalog.Piece p : sig) {
                WornMesh m = HandModels.mesh(p.model());
                if (m != null) pieces.add(new WornLibrary.Resolved(m, p.anchor() != null ? p.anchor() : m.anchor, p.mirror()));
            }
            if (pieces.isEmpty()) signature = false;
        }
        if (!signature) {
            if (def.slot() == RelicSlot.CONSUMABLE || def.slot() == RelicSlot.MAIN_HAND) return null;     // a consumable without its flask is a sprite; a weapon has its own renderer
            WornLibrary.Ensemble worn = WornLibrary.of(def);
            if (worn == null) return null;
            pieces.addAll(worn.pieces);
        }
        int only = style.fit().only();
        if (only > 0 && pieces.size() > only) pieces = new ArrayList<>(pieces.subList(0, only));
        return new Form(WornLibrary.ofPieces(def, pieces), style, signature);
    }

    /** The style of an upgraded relic: the style the override names (if any) and its own size (the upgraded model is bigger and more ornate than the one it replaces). */
    private static HandTransforms.Style upgradeStyle(HandTransforms xf, HandTransforms.Style style, VuOverrides.Entry e) {
        if (e == null) return style;
        String named = e.options().get("style");
        HandTransforms.Style s = named == null ? null : xf.style(named);
        if (s == null) s = style;
        float size = e.number("size", s.fit().size());
        return size == s.fit().size() ? s : s.withSize(size);
    }

    /** Forgets every form (a resource reload). */
    public static void clear() { CACHE.clear(); }
}
