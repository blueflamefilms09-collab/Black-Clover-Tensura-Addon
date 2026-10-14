package com.newuniverse.nusmp.client.relics.worn.lib;

import java.util.List;
import java.util.Locale;

/**
 * The generic fallback of the worn armour models: what an armour item of this mod wears when the catalog ({@link WornCatalog}) has no {@code item} entry for it. It is the
 * per-game safety net for every FUTURE armour item: from the slot of the piece and a few words of its registry path it picks one of the library's existing models, and
 * from the path alone a stable palette, so a new item is never drawn as nothing and two new items do not look the same. Pure: no Minecraft types, every rule is a test.
 */
public final class ArmorLooks {
    private ArmorLooks() {}

    /** The four armour slots (the body slot of a horse or wolf armour is not worn by a player). */
    public enum Slot {
        HEAD, CHEST, LEGS, FEET;

        /** The key a placement slot name uses for this armour slot ("ARMOR_HEAD" ...): never equal to a {@code RelicSlot} name. */
        public String key() { return "ARMOR_" + name(); }
    }

    /** The silhouette family a path suggests: soft cloth, a long robe or coat, or plate. */
    public enum Style { CLOTH, ROBE, PLATE }

    /** The answer for one item: the pieces (as catalog text), the palette {main, trim, glow} and the grade tier. */
    public record Generic(List<String> pieces, int[] palette, int tier, Style style) {}

    private static final String[] PLATE_WORDS = {"plate", "armor", "armour", "helm", "mail", "greave", "sabaton", "knight", "steel", "iron", "golem", "guard", "war", "gauntlet", "cuirass"};
    private static final String[] ROBE_WORDS = {"robe", "cloak", "mantle", "coat", "cowl", "hood", "hat", "dress", "uniform", "mage", "wizard", "cape", "vestment", "tunic"};

    /** The style of a registry path: plate words win over robe words only when no robe word is present ("mage_armor" is a robe). */
    public static Style styleOf(String path) {
        String p = path == null ? "" : path.toLowerCase(Locale.ROOT);
        for (String w : ROBE_WORDS) if (p.contains(w)) return Style.ROBE;
        for (String w : PLATE_WORDS) if (p.contains(w)) return Style.PLATE;
        return Style.CLOTH;
    }

    /** The catalog pieces of the generic look of a slot and style (model names that exist in the library). */
    public static List<String> pieces(Slot slot, Style style) {
        Style st = style == null ? Style.CLOTH : style;
        return switch (slot == null ? Slot.CHEST : slot) {
            case HEAD -> List.of(st == Style.PLATE ? "helm_war" : "hood_cowl");
            case CHEST -> switch (st) {
                case PLATE -> List.of("chest_tunic", "sleeve_plated@rarm", "sleeve_plated@larm~");
                case ROBE -> List.of("chest_robe", "sleeve_wide@rarm", "sleeve_wide@larm~");
                default -> List.of("chest_tunic", "sleeve_plain@rarm", "sleeve_plain@larm~");
            };
            case LEGS -> switch (st) {
                case PLATE -> List.of("hip_plated", "legs_plated@rleg", "legs_plated@lleg");
                case ROBE -> List.of("hip_sash", "legs_wrapped@rleg", "legs_wrapped@lleg");
                default -> List.of("hip_sash", "legs_cloth@rleg", "legs_cloth@lleg");
            };
            case FEET -> st == Style.PLATE ? List.of("boots_war@rleg", "boots_war@lleg") : List.of("boots_tall@rleg", "boots_tall@lleg");
        };
    }

    /** A stable palette {main, trim, glow} (0xRRGGBB) from the path: a dark body colour, a light trim of a neighbouring hue, a bright glow. */
    public static int[] palette(String path) {
        int h = path == null ? 0 : path.hashCode();
        float hue = ((h & 0x7FFFFFFF) % 360) / 360f;
        return new int[]{hsv(hue, 0.45f, 0.46f), hsv(hue + 0.09f, 0.32f, 0.86f), hsv(hue + 0.02f, 0.55f, 1.0f)};
    }

    /** Grade tier of an item the catalog does not know: a plain model with a faint rim (2 shows no rim, so none of the grade effects). */
    public static int tier(String path) { return 2; }

    /** The generic look of a slot and path. */
    public static Generic resolve(Slot slot, String path) {
        Style st = styleOf(path);
        return new Generic(pieces(slot, st), palette(path), tier(path), st);
    }

    /** The slot of a piece by the name of the vanilla armour type ("helmet", "chestplate", "leggings", "boots"), or null for the body slot and anything else. */
    public static Slot slotOfType(String typeName) {
        if (typeName == null) return null;
        return switch (typeName.toLowerCase(Locale.ROOT)) {
            case "helmet" -> Slot.HEAD;
            case "chestplate" -> Slot.CHEST;
            case "leggings" -> Slot.LEGS;
            case "boots" -> Slot.FEET;
            default -> null;
        };
    }

    /** HSV (h wraps, s and v 0..1) to 0xRRGGBB. */
    static int hsv(float h, float s, float v) {
        float hh = (h - (float) Math.floor(h)) * 6f;
        int i = (int) Math.floor(hh);
        float f = hh - i, p = v * (1f - s), q = v * (1f - s * f), t = v * (1f - s * (1f - f));
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (Math.round(r * 255f) << 16) | (Math.round(g * 255f) << 8) | Math.round(b * 255f);
    }
}
