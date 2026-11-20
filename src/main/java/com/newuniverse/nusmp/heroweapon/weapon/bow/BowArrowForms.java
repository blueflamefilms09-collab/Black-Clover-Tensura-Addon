package com.newuniverse.nusmp.heroweapon.weapon.bow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: every bow form gives its real arrows an effect, a magic share, a colour and a texture, and a form added to the forms file later must not silently
 * shoot a plain arrow. This pure table is the one place that says it; {@code BowArrowFormsTest} reads the forms file and fails when a form has no row or no
 * texture. The row ORDER is the number the arrow tells the client (the entity data), so rows are only ever appended. A form id with no row (a test form, a
 * hand-edited file) reads as the base arrow.
 */
public final class BowArrowForms {
    private BowArrowForms() {}

    /** The look of one form's arrows: the effect, the magic hit as a share of the physical hit, the trail colour (0xRRGGBB) and the texture file name (no extension). */
    public record Look(String form, ArrowKind kind, double magicShare, int rgb, String texture) {}

    private static final Map<String, Look> TABLE = new LinkedHashMap<>();

    private static void row(String form, ArrowKind kind, double share, int rgb) { TABLE.put(form, new Look(form, kind, share, rgb, form)); }

    static {
        // ---- the ten ordinary forms (the order of legendary_bow.json)
        row("base", ArrowKind.LIGHT, 0.30, 0xFFE8A0);
        row("hunters_bow", ArrowKind.BLEED, 0.30, 0x7DAA45);
        row("scouts_bow", ArrowKind.MARK, 0.25, 0x6FD6E0);
        row("bind_arrow", ArrowKind.BIND, 0.30, 0xE6E6E6);
        row("bird_hunting", ArrowKind.PIERCE, 0.30, 0x7FB0FF);
        row("arrow_shower", ArrowKind.SPLIT, 0.25, 0xFFC040);
        row("meteor_bow", ArrowKind.FIRE, 0.40, 0xFF6A20);
        row("frozen_rain", ArrowKind.FROST, 0.35, 0x9FE8FF);
        row("eagle_piercer", ArrowKind.PIERCE, 0.30, 0xD8C060);
        row("sagittarius", ArrowKind.STAR, 0.40, 0xB8A0FF);
        // ---- the three Pride (curse) forms
        row("bow_of_pride_1", ArrowKind.PRIDE, 0.35, 0x7A3FB5);
        row("bow_of_pride_2", ArrowKind.PRIDE, 0.35, 0x7A3FB5);
        row("bow_of_pride_3", ArrowKind.PRIDE, 0.35, 0x7A3FB5);
    }

    /** The form ids with a row, in table order. */
    public static List<String> ids() { return new ArrayList<>(TABLE.keySet()); }

    /** The look of this form; the base look for an id with no row. */
    public static Look of(String formId) {
        Look l = formId == null ? null : TABLE.get(formId);
        return l != null ? l : TABLE.get("base");
    }

    /** True when the form has its own row. */
    public static boolean has(String formId) { return formId != null && TABLE.containsKey(formId); }

    /** The number the arrow tells the client for this form (0 for an unknown one). */
    public static int index(String formId) {
        int i = 0;
        for (String k : TABLE.keySet()) {
            if (k.equals(formId)) return i;
            i++;
        }
        return 0;
    }

    /** The look with this number; the base look for a number out of range. */
    public static Look byIndex(int index) {
        int i = 0;
        for (Look l : TABLE.values()) {
            if (i++ == index) return l;
        }
        return TABLE.get("base");
    }

    /** How many rows there are (the client keeps one texture location per row). */
    public static int count() { return TABLE.size(); }

    /** The texture path under {@code assets/anime_multiverse/} (the client turns it into a location). */
    public static String texturePath(Look l) { return "textures/entity/bow_arrow/" + l.texture() + ".png"; }

    /** The soft glow of the trail, one texture for every form (tinted by the form's colour). */
    public static final String GLOW_PATH = "textures/entity/bow_arrow/glow.png";
}
