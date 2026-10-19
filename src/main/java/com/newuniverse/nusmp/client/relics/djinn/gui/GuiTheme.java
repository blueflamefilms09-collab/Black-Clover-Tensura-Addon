package com.newuniverse.nusmp.client.relics.djinn.gui;

import com.newuniverse.nusmp.relics.djinn.Djinn;

/**
 * The look of one Djinn's panel, derived once from its {@link GuiSpec} (and whether it is a Dark Djinn) and then reused every frame: the frame theme of
 * its magic, its palette, its accent and hand tuned (or hash derived) traits, and its emblem. Immutable but for the text caches the painter fills on
 * first use.
 */
public final class GuiTheme {
    public final GuiSpec spec;
    public final boolean dark;
    public final GuiMotif motif;
    public final GuiPalette pal;
    public final GuiAccent.Tune tune;
    public final Emblem emblem;
    /** The halo shape, or NONE. */
    public final Djinn.Look.Halo halo;
    public final Djinn.Look.Tail tail;

    // text caches (filled by the painter)
    GuiLabels labelsFor;
    String nameLine, subLine;

    private GuiTheme(GuiSpec spec, boolean dark) {
        this.spec = spec;
        this.dark = dark;
        this.motif = GuiMotif.of(spec.affinity());
        this.pal = GuiPalette.of(spec.color(), spec.glow(), spec.tint(), dark);
        this.tune = GuiAccent.tuneOf(spec.id(), spec.affinity());
        this.emblem = Emblem.of(spec.look());
        this.halo = spec.look() == null ? Djinn.Look.Halo.NONE : spec.look().halo();
        this.tail = spec.look() == null ? Djinn.Look.Tail.NONE : spec.look().tail();
    }

    public static GuiTheme of(GuiSpec spec, boolean dark) { return new GuiTheme(spec, dark); }

    public static GuiTheme of(Djinn d, boolean dark) { return new GuiTheme(GuiSpec.of(d), dark); }
}
