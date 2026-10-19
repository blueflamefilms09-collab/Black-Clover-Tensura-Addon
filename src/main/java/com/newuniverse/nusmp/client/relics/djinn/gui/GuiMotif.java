package com.newuniverse.nusmp.client.relics.djinn.gui;

import com.newuniverse.nusmp.relics.djinn.Affinity;

/**
 * The frame theme of a magic. One per Affinity; an Affinity this table does not know (a future one) still gets a theme through
 * {@link #of(Affinity)}'s fallback, so no Djinn can be left without a frame.
 */
public enum GuiMotif {
    /** Heat: flame tongues on the bottom edge, rising embers, heat haze. */
    EMBERS(15, 1.0f),
    /** Lightning: jagged arcs along the borders, sudden flashes. */
    ARCS(12, 1.3f),
    /** Water: a rippling bottom edge, rising bubbles, ripple waves, waves in the bars. */
    RIPPLE(14, 0.8f),
    /** Wind: swept streaks crossing the panel, drifting leaves, chevron notches. */
    STREAKS(14, 1.2f),
    /** Life: vines creeping along the border, falling petals, leaf notches in the bars. */
    VINES(13, 0.7f),
    /** Strength: cracked stone border, falling chips and dust, a heavy pulse. */
    CRACKS(12, 0.6f),
    /** Sound: concentric rings that leave the medallion, rising notes, rhythmic bars. */
    RINGS(11, 0.9f),
    /** Light: rotating rays, twinkling stars, a golden filigree. */
    RAYS(13, 0.8f);

    /** Ambient particles at full energy, and how fast this magic moves. */
    public final int particles;
    public final float tempo;

    GuiMotif(int particles, float tempo) {
        this.particles = particles;
        this.tempo = tempo;
    }

    public static GuiMotif of(Affinity a) {
        if (a == null) return RINGS;
        return switch (a) {
            case HEAT -> EMBERS;
            case LIGHTNING -> ARCS;
            case WATER -> RIPPLE;
            case WIND -> STREAKS;
            case LIFE -> VINES;
            case STRENGTH -> CRACKS;
            case SOUND -> RINGS;
            case LIGHT -> RAYS;
            default -> RINGS;
        };
    }
}
