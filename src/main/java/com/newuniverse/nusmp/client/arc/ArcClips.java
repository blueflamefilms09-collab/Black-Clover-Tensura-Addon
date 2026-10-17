package com.newuniverse.nusmp.client.arc;

import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.zogratis.ZogratisPose;

/**
 * Why: the pose clock ({@link ZogratisPose}) needs the length and looping of the clips of the model that is drawn. The family's own reader of them is not reachable from
 * this package (its constructor is protected), so this is the same small reader: it asks the animation file every time (so a resource reload is picked up) and says
 * "no such clip" (length 0) for a missing file or clip, never an exception.
 */
final class ArcClips implements ZogratisPose.Clips {
    private final GeoSpec spec;

    ArcClips(GeoSpec spec) { this.spec = spec; }

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
}
