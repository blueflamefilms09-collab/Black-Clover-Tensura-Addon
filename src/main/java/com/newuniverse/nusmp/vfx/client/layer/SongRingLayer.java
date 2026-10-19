package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;

import java.util.EnumSet;
import java.util.Set;

/**
 * Why: each pulse of a song reaches a known radius, and the players need to see that edge to stand inside it. 1.93.0: the drawing moved into {@link SongFxLayer} (two staff
 * rings and sound arcs running out to the radius, a flash under the singer, note sparks on the leading edge); this class stays the registered layer of the older SONG_RING
 * shape: 'from' = the centre on the ground, power = the final radius in blocks.
 */
public class SongRingLayer extends SongFxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SONG_RING); }
}
