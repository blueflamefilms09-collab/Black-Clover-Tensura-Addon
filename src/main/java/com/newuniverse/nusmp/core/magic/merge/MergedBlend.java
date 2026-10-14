package com.newuniverse.nusmp.core.magic.merge;

import java.util.List;
import java.util.function.Function;

/**
 * Everything a merged magic derives from its parents, as plain data (pure): the shared palette, the weights, the soul type (the dominant
 * parent's), and the two hit effects (the dominant parent's is the primary, the first DIFFERENT one of the others rides along as the
 * secondary). The profile, the effects, the armour and the model all read this one object, so they agree.
 */
public final class MergedBlend {
    /** What the blend needs to know of one parent. */
    public record Part(String id, String soulType, String hit, int color) {}

    public final MergedDef def;
    public final int[] colors;
    public final float[] weights;
    /** The weighted mix of the palette: the colour a merged effect is drawn in when it has to be one colour. */
    public final int primary;
    public final String soulType;
    public final String hitPrimary;
    /** The first hit effect of another parent that differs from the primary, or null. */
    public final String hitSecondary;
    /** The index (canonical order) of the parent whose hit effect is the secondary, -1 if none. */
    public final int hitSecondaryParent;

    private MergedBlend(MergedDef def, int[] colors, float[] weights, int primary, String soul, String hit1, String hit2, int hit2Parent) {
        this.def = def; this.colors = colors; this.weights = weights; this.primary = primary;
        this.soulType = soul; this.hitPrimary = hit1; this.hitSecondary = hit2; this.hitSecondaryParent = hit2Parent;
    }

    /** Derives the blend. {@code parts} resolves a canonical parent name to its data (it may return null: the parent gets a neutral part). */
    public static MergedBlend of(MergedDef def, Function<String, Part> parts) {
        List<String> ps = def.parents();
        int n = ps.size();
        int[] colors = new int[n];
        String soul = "EMPTY", hit1 = "PIERCE", hit2 = null;
        int hit2Parent = -1;
        for (int i = 0; i < n; i++) {
            Part p = parts == null ? null : parts.apply(ps.get(i));
            if (p == null) p = new Part(ps.get(i), "EMPTY", "PIERCE", 0xFFB8D8FF);
            colors[i] = p.color() | 0xFF000000;
            if (i == 0) { soul = p.soulType() == null || p.soulType().isBlank() ? "EMPTY" : p.soulType(); hit1 = p.hit() == null ? "PIERCE" : p.hit(); }
            else if (hit2 == null && p.hit() != null && !p.hit().equals(hit1)) { hit2 = p.hit(); hit2Parent = i; }
        }
        float[] w = ColorBlend.weights(n);
        return new MergedBlend(def, colors, w, ColorBlend.mix(colors, w), soul, hit1, hit2, hit2Parent);
    }

    public int size() { return colors.length; }

    /** The palette colour at a phase (0..1 walks round all parents). */
    public int at(float phase) { return ColorBlend.cycle(colors, phase); }

    /** The colour of parent i's own effect during a merged cast: pulled 35 % toward the shared mix, so every effect shares the palette. */
    public int tinted(int parent) { return ColorBlend.tint(colors[Math.floorMod(parent, colors.length)], primary, 0.35f); }
}
