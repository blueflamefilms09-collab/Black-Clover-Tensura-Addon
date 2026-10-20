package com.newuniverse.nusmp.client.relics.hand;

import com.newuniverse.nusmp.client.relics.worn.lib.WornCatalog;
import com.newuniverse.nusmp.client.relics.worn.lib.WornLibrary;
import com.newuniverse.nusmp.client.relics.worn.lib.WornMesh;
import com.newuniverse.nusmp.client.relics.worn.lib.WornModels;
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

    private static final Map<String, Optional<Form>> CACHE = new HashMap<>(), CACHE_UP = new HashMap<>();

    /** The form of a relic, or null (its sprite is drawn). Built once per relic id. */
    public static Form of(RelicDef def) {
        boolean up = WornLibrary.upgradeOf(def.id());
        Map<String, Optional<Form>> cache = up ? CACHE_UP : CACHE;                           // 1.102.0: the visual upgrade option decides which form is cached
        Optional<Form> f = cache.get(def.id());
        if (f == null) {
            f = Optional.ofNullable(build(def, up));
            cache.put(def.id(), f);
        }
        return f.orElse(null);
    }

    private static Form build(RelicDef def, boolean upgraded) {
        HandTransforms xf = HandModels.transforms();
        if (!xf.complete()) return null;
        HandTransforms.Style style = xf.styleFor(def.id(), def.slot().name(), def.look().name());
        if (style == null) return null;
        List<WornCatalog.Piece> sig = HandModels.catalog().sigFor(def.id());
        if (upgraded) {                                                                   // 1.102.0: an upgraded relic is held as its held form (at most 2500 vertices), whatever it was held as before
            List<WornCatalog.Piece> held = WornModels.upgrade().held(def.id());
            if (!held.isEmpty()) sig = held;
        }
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

    /** Forgets every form (a resource reload). */
    public static void clear() {
        CACHE.clear();
        CACHE_UP.clear();
    }
}
