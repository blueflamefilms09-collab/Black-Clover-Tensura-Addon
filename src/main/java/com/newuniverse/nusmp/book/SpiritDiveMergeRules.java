package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;

import java.util.ArrayList;
import java.util.List;

/**
 * Spirit Dive MERGE (the user merges with the spirit like the anime), the pure rules: the clock, the path of the spirit, the strengths of the flash, the glow of the body and the inner silhouette, the
 * budgets and the tables. Common code (no Minecraft class), so the tests, the self-test and the preview scripts count the same numbers the client painter {@code SpiritDiveMerge} draws.
 *
 * <p><b>The story (the anime):</b> a Spirit Dive is the mage taking the spirit's mana into the body. The spirit (Sylph, Undine, Salamander, Gnome) appears beside the mage, then flows INTO the chest;
 * the mage carries it. One clock {@code m} (0..{@link #ENTRY_TICKS}) drives everything: it runs forward over the first 22 ticks of the dive and backwards over the last {@link #EXIT_TICKS} ticks (the
 * same clock as the REEL look, so the exit is the entry backwards: the spirit separates from the chest, floats out and fades).</p>
 * <ul>
 *   <li><b>Phase 1, m 0..8:</b> the spirit materialises beside and above the wearer (a ghost of its model, in the colour of the element, fading in, rising into place).</li>
 *   <li><b>Phase 2, m 8..18:</b> it shrinks and spirals into the chest along a curved path with trailing strips of light; a flash at the moment of contact (m {@link #FLASH_AT}); the body takes the colour.</li>
 *   <li><b>Phase 3, m 15..22 and after:</b> a very faint silhouette of the spirit round the torso, breathing slowly (at most {@link #INNER_ALPHA} alpha).</li>
 * </ul>
 */
public final class SpiritDiveMergeRules {
    private SpiritDiveMergeRules() {}

    // ------------------------------------------------------------------ the clock
    public static final int ENTRY_TICKS = 22, EXIT_TICKS = 20;
    /** The contact: the spirit touches the chest. */
    public static final float FLASH_AT = 14.5f, APPEAR_END = 8f, FLOW_END = 18f;

    /** The merge clock, 0..22: forward over the first 22 ticks since the start, backwards over the last 20 ticks before the end (the smaller of the two; the same formula as the REEL look). */
    public static float clock(float t, float left) {
        float in = t < 0f ? 0f : Math.min(t, ENTRY_TICKS), out = left < 0f ? 0f : Math.min(left * ENTRY_TICKS / EXIT_TICKS, ENTRY_TICKS);
        return Math.min(in, out);
    }

    /** True while the exit plays (the clock is run down by the end of the dive, not by its start). */
    public static boolean exiting(float t, float left) { return left * ENTRY_TICKS / EXIT_TICKS < Math.min(t, ENTRY_TICKS); }

    /** True while anything of the entry / exit (the ghost, the strips, the flash) can be on screen: the clock is strictly inside the open range. */
    public static boolean flowing(float m) { return m > 0.05f && m < FLOW_END + 0.6f; }

    // ------------------------------------------------------------------ small maths
    public static float clamp01(float x) { return x < 0f ? 0f : Math.min(x, 1f); }

    public static float smooth(float x) { x = clamp01(x); return x * x * (3f - 2f * x); }

    // ------------------------------------------------------------------ budgets and caps
    /** The ghost may be drawn within this many blocks of the camera (squared below). */
    public static final double GHOST_RANGE = 32.0, GHOST_RANGE2 = GHOST_RANGE * GHOST_RANGE;
    /** At most this many merges a frame are drawn; the rest skip. */
    public static final int MERGES_PER_FRAME = 3;
    /** The ghost mesh (a whole spirit) is at most this many vertices; the inner silhouette at most {@link #INNER_VERTS}. */
    public static final int GHOST_VERTS = 700, INNER_VERTS = 300;
    /** The light strips: count per tier, segments per strip (4 vertices a segment). */
    public static final int STRIPS_STANDARD = 3, STRIPS_CHARACTER = 5, STRIP_SEGS = 6;
    /** The sprites of the contact: the flash, its core and the ring; the cover pass over the six body parts (24 vertices each). */
    public static final int FLASH_SPRITES = 3, COVER_VERTS = 6 * 24;
    /** The ghost's top alpha (additive) and the inner silhouette's top alpha (at most 12 percent). */
    public static final float GHOST_ALPHA = 0.8f, INNER_ALPHA = 0.12f;

    // ------------------------------------------------------------------ the strengths
    /** The ghost's alpha, 0..1 (times {@link #GHOST_ALPHA}): in over phase 1, full through phase 2, gone at the contact. */
    public static float ghostAlpha(float m) {
        if (m <= 0f) return 0f;
        if (m < APPEAR_END) return smooth(m / APPEAR_END);
        float w = clamp01((m - APPEAR_END) / (FLOW_END - APPEAR_END));
        return 1f - smooth((w - 0.78f) / 0.22f);
    }

    /** The ghost's size, relative to its full size: grows a little while it appears, shrinks to a seventh when it reaches the chest. */
    public static float ghostScale(float m) {
        if (m < APPEAR_END) return 0.72f + 0.28f * smooth(m / APPEAR_END);
        float e = flowEase(m);
        return 1f - 0.86f * (float) Math.pow(e, 1.3);
    }

    /** The progress of the flow into the chest, 0..1 (smoothstep over m 8..18). */
    public static float flowEase(float m) { return smooth((m - APPEAR_END) / (FLOW_END - APPEAR_END)); }

    /** The flash at the contact, 0..1: a bell round {@link #FLASH_AT} (about 4 ticks wide). */
    public static float flash(float m) {
        float d = 1f - Math.abs(m - FLASH_AT) / 2.4f;
        return d <= 0f ? 0f : d * d;
    }

    /** The ring that runs out from the chest after the contact, 0..1 (0 outside m 14.5..20). */
    public static float ring(float m) { return clamp01((m - FLASH_AT) / 5.5f) * (m > FLASH_AT + 5.5f ? 0f : 1f); }

    /** The glow of the body in the colour of the spirit, 0..1: rises over m 13..14.5, fades out by m 21. */
    public static float bodyGlow(float m) {
        if (m < 13f) return 0f;
        if (m < FLASH_AT) return (m - 13f) / (FLASH_AT - 13f);
        float x = 1f - (m - FLASH_AT) / 6.5f;
        return x <= 0f ? 0f : x * (float) Math.sqrt(x);
    }

    /** The inner silhouette's strength, 0..1 (times {@link #INNER_ALPHA}): in over m 15..22, steady after; breathes with {@code age}. */
    public static float inner(float m, float age) { return smooth((m - 15f) / 7f) * (0.82f + 0.18f * (float) Math.sin(age * 0.07f)); }

    /** The strength of the trailing strips, 0..1: on while the spirit flows. */
    public static float strips(float m) {
        if (m <= APPEAR_END - 0.3f) return 0f;
        float w = clamp01((m - APPEAR_END) / (FLOW_END - APPEAR_END));
        return smooth((m - (APPEAR_END - 0.3f)) / 1.5f) * (1f - smooth((w - 0.82f) / 0.18f));
    }

    // ------------------------------------------------------------------ the path
    /** Layer space (model pixels, y down, origin at the neck line): the chest, where the spirit goes in. */
    public static final float CHEST_X = 0f, CHEST_Y = 4f, CHEST_Z = 0f;

    /** The side the spirit appears on: +1 or -1, by a player-specific number (so two divers do not mirror each other). */
    public static float side(int hash) { return (hash & 1) == 0 ? 1f : -1f; }

    /** The distance of the hovering spirit from the chest, and its height (y, down is positive), by tier. */
    public static float radius(boolean character) { return character ? 34f : 27f; }

    public static float hover(boolean character) { return character ? -1f : 1f; }

    /**
     * The centre of the spirit at the clock {@code m} as {@code out[0..3] = x, y, z, yaw}: phase 1 hovers beside the wearer rising into place, phase 2 spirals into the chest (a turn and a bit, the radius
     * closing, a small arc in height). {@code yaw} turns the spirit's front (-z) to the chest. Finite for every {@code m}.
     */
    public static void path(float m, float side, boolean character, float[] out) {
        float r0 = radius(character), y0 = hover(character), phi0 = side * 1.05f;
        float r, y, phi;
        if (m < APPEAR_END) {
            float a = smooth(m / APPEAR_END);
            r = r0; phi = phi0 + side * 0.18f * a; y = y0 + 9f * (1f - a);
        } else {
            float e = flowEase(m);
            r = r0 * (1f - e) + 3f * e;
            phi = phi0 + side * (0.18f + 2.3f * (float) Math.PI * e);
            y = y0 + (CHEST_Y - y0) * e - 8f * (float) Math.sin(Math.PI * e);
        }
        float x = r * (float) Math.sin(phi), z = -r * (float) Math.cos(phi);
        out[0] = x; out[1] = y; out[2] = z;
        out[3] = (float) Math.atan2(-(CHEST_X - x), -(CHEST_Z - z));
    }

    // ------------------------------------------------------------------ the tables
    /** The kinds of mesh of one element: the ghost of the standard and the character tier, the inner silhouette. */
    public static String ghostMesh(Element e, boolean character) {
        String base = name(e);
        return base + (character && hasCharacterMesh(e) ? "_char" : "_std");
    }

    public static String coreMesh(Element e) { return name(e) + "_core"; }

    /** Earth has no character tier (Gnome: the standard spirit only). */
    public static boolean hasCharacterMesh(Element e) { return e != Element.EARTH; }

    public static String name(Element e) {
        return switch (e) { case WIND -> "wind"; case WATER -> "water"; case FIRE -> "fire"; case EARTH -> "earth"; };
    }

    /** The spirit that merges: Sylph (Wind), Undine (Water), Salamander (Fire), Gnome (Earth). */
    public static String spirit(Element e) {
        return switch (e) { case WIND -> "Sylph"; case WATER -> "Undine"; case FIRE -> "Salamander"; case EARTH -> "Gnome"; };
    }

    /** The size of the ghost relative to its mesh (the meshes are 32 px on their longest side): the character tier is larger. */
    public static float ghostSize(Element e, boolean character) {
        return switch (e) {
            case WIND -> character ? 1.25f : 0.95f;
            case WATER -> character ? 1.25f : 0.95f;
            case FIRE -> character ? 1.45f : 1.05f;
            case EARTH -> 0.95f;
        };
    }

    /** The size and the spot (y, z px) of the inner silhouette about the torso. */
    public static float coreSize(Element e) {
        return switch (e) { case WIND -> 1.06f; case WATER -> 1.0f; case FIRE -> 1.18f; case EARTH -> 0.92f; };
    }

    public static final float CORE_Y = 8f, CORE_Z = 2f;

    /** The colour of the spirit's light (RGB), the same as the dive's glow. */
    public static int rgb(Element e) {
        return switch (e) { case WIND -> 0x98FFD0; case WATER -> 0xA8DCFF; case FIRE -> 0xFF9A3A; case EARTH -> 0xFFC060; };
    }

    /** The colour of the flash: the spirit's colour mixed halfway with white. */
    public static int flashRgb(Element e) {
        int c = rgb(e);
        return ((((c >> 16 & 255) + 255) / 2) << 16) | ((((c >> 8 & 255) + 255) / 2) << 8) | (((c & 255) + 255) / 2);
    }

    public static final String SKIN = "aura/sdm_skin", TRAIL = "aura/sdm_trail", FLASH = "particle/glow";

    public static List<String> textures() { return List.of(SKIN, TRAIL); }

    public static List<String> meshes() {
        List<String> l = new ArrayList<>();
        for (Element e : Element.values()) {
            l.add(ghostMesh(e, false));
            if (hasCharacterMesh(e)) l.add(ghostMesh(e, true));
            l.add(coreMesh(e));
        }
        return l;
    }

    // ------------------------------------------------------------------ the character tier
    /** The character tier of the merge: Yuno on Wind (Sylph), Fuegoleon on Fire (Salamander), Noelle on Water (Undine); never Earth. The same rule as the dive's own tier. */
    public static boolean character(Element e, Grade g) { return SpiritDiveTier.character(e, g); }

    public static int strips(boolean character) { return character ? STRIPS_CHARACTER : STRIPS_STANDARD; }

    // ------------------------------------------------------------------ the cost
    /**
     * The vertices the merge draws for one diver at the clock {@code m} and the level of detail {@code lod} (a quad is four vertices): the ghost mesh and the strips and the contact sprites and the cover
     * while it flows (only inside {@link #GHOST_RANGE}, which the painter tests by distance: {@code ghostRange}), the inner silhouette once it has started (FULL only).
     */
    public static int vertices(Element e, boolean character, Lod lod, float m, boolean ghostRange) {
        if (lod == Lod.CULLED) return 0;
        int v = 0;
        if (ghostRange && flowing(m)) {
            v += SpiritDiveMergeMesh.get(ghostMesh(e, character)).totalVerts();
            v += strips(character) * STRIP_SEGS * 4;
            if (flash(m) > 0.01f || ring(m) > 0.01f) v += FLASH_SPRITES * 4;
            if (bodyGlow(m) > 0.01f) v += COVER_VERTS;
        } else if (ghostRange && (flash(m) > 0.01f || bodyGlow(m) > 0.01f)) {                    // the end of the cover after the ghost is gone
            if (flash(m) > 0.01f || ring(m) > 0.01f) v += FLASH_SPRITES * 4;
            if (bodyGlow(m) > 0.01f) v += COVER_VERTS;
        }
        if (lod == Lod.FULL && m > 15.05f) v += SpiritDiveMergeMesh.get(coreMesh(e)).totalVerts();
        return v;
    }

    /** The worst case for one diver over the whole clock, counted by stepping it. */
    public static int worst(Element e, boolean character) {
        int w = 0;
        for (float m = 0f; m <= ENTRY_TICKS; m += 0.25f) w = Math.max(w, vertices(e, character, Lod.FULL, m, true));
        return w;
    }

    /** The steady state (clock at its end): the inner silhouette alone, FULL only. */
    public static int steady(Element e, Lod lod) { return vertices(e, false, lod, ENTRY_TICKS, true); }

    // ------------------------------------------------------------------ the option
    /** The client option {@code spiritDive.merge}: default true. */
    public static final boolean DEFAULT_MERGE = true;
}
