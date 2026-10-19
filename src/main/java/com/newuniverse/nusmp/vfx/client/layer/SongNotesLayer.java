package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;

import java.util.EnumSet;
import java.util.Set;

/**
 * Why: a song has to be SEEN as well as heard, or a quiet pulse looks like nothing happened. 1.93.0: the drawing moved into {@link SongFxLayer} (real note glyph sprites with
 * halos, a clef crowning the spiral, the Song textures); this class stays the registered layer of the older SONG_NOTES shape (Kahono's singing): 'from' = the followed
 * body's position, 'to' = the point the notes rise from (followed too), power = size (1 = a normal pulse, 1.6 = the Crescendo burst, which also sends more notes).
 */
public class SongNotesLayer extends SongFxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SONG_NOTES); }
}
