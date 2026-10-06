package com.newuniverse.nusmp.vfx.fx;

import java.util.List;

/** A complete effect: fixed life, ordered phases, pieces, a 3-6 colour palette and timed sound/shake cues. */
public record EffectSpec(String id, String family, int[] palette, int life, List<Phase> phases, List<Piece> pieces, List<Cue> cues, String additiveTextures) {
    public record Phase(String name, int start, int end) {}
    /** A sound and/or camera shake, fired on the first frame of the phase it belongs to. */
    public record Cue(String phase, int tick, String sound, float volume, float pitch, double shake) {}

    public int vertices() { return pieces.stream().mapToInt(Piece::vertices).sum(); }

    public Phase phase(String name) { for (Phase p : phases) if (p.name().equals(name)) return p; return null; }
}
