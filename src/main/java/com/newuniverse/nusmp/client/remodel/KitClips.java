package com.newuniverse.nusmp.client.remodel;

import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.zogratis.ZogratisPose;

/**
 * Why: the pose clock ({@link ZogratisPose}) needs the length and looping of the clips of the model that is drawn, and the family's own reader of them is not reachable from this package
 * (the arc renderers' copy is package-private too), so this is the same small reader again: it asks the animation file every time (so a resource reload is picked up) and says "no such
 * clip" (length 0) for a missing file or clip, never an exception.
 */
final class KitClips implements ZogratisPose.Clips {
    private final GeoSpec spec;

    KitClips(GeoSpec spec) { this.spec = spec; }

    private GeoAnim.Clip find(String clip) {
        GeoAnim a = GeoModels.animations(spec.animations());
        return a == null ? null : a.clip(clip);
    }

    @Override
    public float length(String clip) {
        GeoAnim.Clip c = find(clip);
        return c == null ? 0f : c.length;
    }

    @Override
    public boolean loops(String clip) {
        GeoAnim.Clip c = find(clip);
        return c == null || c.loop;
    }

    /**
     * The geo spec of an art-kit model: {@code arc_<model>}, optionally wearing the texture of a skin: {@code arc_<base>_<skin>}, where base is the model without its {@code _form2}
     * (the skin of a second form is named {@code <skin>_form2}, as tools/remodel/gen_remodel.py writes it); the glow layer follows the skin.
     */
    static GeoSpec spec(String model, String skin, boolean glow) {
        GeoSpec base = GeoSpec.of("arc", model);
        String stem = model.endsWith(RemodelTable.FORM2) ? model.substring(0, model.length() - RemodelTable.FORM2.length()) : model;
        GeoSpec s = skin == null || skin.isEmpty() ? base : base.skin(RemodelTable.skinStem(stem, skin));
        return glow ? s : s.noGlow();
    }
}
