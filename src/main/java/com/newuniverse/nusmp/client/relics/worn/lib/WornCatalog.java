package com.newuniverse.nusmp.client.relics.worn.lib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Which models a worn relic wears, read from assets/nusmp/relic_worn/catalog.txt (written by tools/relics/gen_worn_all.py). Pure.
 * <pre>
 * look HEAD CROWN crown_spired            the models of every relic of that slot and look
 * relic tiny_duck_crown crown_duck        a signature relic's own models (they replace the look's)
 * fx tiny_duck_crown ember                the ambient particle theme of a relic
 * item black_bull_robe chest_mantle ...   an ARMOUR ITEM of the mod (by registry path, or a mode_&lt;name&gt;): the models it wears on the player
 * item anime:black_bull_robe anime_cloak_black_bull   the anime look of the same item (chosen when relicfx.robeAnime is on); the piece {@code none} draws nothing
 * ipal black_bull_robe 242424 E4B454 E4B454   its palette (main, trim, glow as hex); itier black_bull_robe 3  its grade tier 1..7
 * islot glutton_chalice CHARM            a Curios-worn item that is not a relic: the slot it is drawn in (placed and shrunk like a relic of that slot)
 * </pre>
 * A piece is {@code model[@anchor][~]}: {@code @anchor} attaches it to another body part than the one named in the file (head, body, rarm, larm, rleg, lleg),
 * {@code ~} mirrors it left to right (how the left arm gets the right arm's model).
 */
public final class WornCatalog {
    /** The registry-path prefix of the anime look of an armour item ({@code anime:black_bull_robe}); the catalog keeps both looks, the option {@code relicfx.robeAnime} picks one. */
    public static final String ANIME_PREFIX = "anime:";
    /** A piece that draws nothing but still makes the library "own" that armour piece (so the older renderer is silenced): the legs and boots of an anime robe set. */
    public static final String NONE = "none";

    /** The catalog key of the anime look of the item with this registry path. */
    public static String animeKey(String path) { return ANIME_PREFIX + path; }

    /** One model on one body part. {@code anchor} is null for the one in the model's file. */
    public record Piece(String model, WornAnchor anchor, boolean mirror) {}

    private final Map<String, List<Piece>> looks = new HashMap<>(), relics = new HashMap<>();
    private final Map<String, String> themes = new HashMap<>();
    private final Map<String, List<Piece>> items = new HashMap<>();
    private final Map<String, int[]> itemPalettes = new HashMap<>();
    private final Map<String, Integer> itemTiers = new HashMap<>();
    private final Map<String, String> itemSlots = new HashMap<>();

    public static WornCatalog parse(Iterable<String> lines) {
        WornCatalog c = new WornCatalog();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            String[] v = line.split("\\s+");
            switch (v[0]) {
                case "look" -> {
                    if (v.length >= 4) c.looks.put(key(v[1], v[2]), pieces(v, 3));
                }
                case "relic" -> {
                    if (v.length >= 3) c.relics.put(v[1], pieces(v, 2));
                }
                case "fx" -> {
                    if (v.length >= 3) c.themes.put(v[1], v[2].toLowerCase(Locale.ROOT));
                }
                case "item" -> {
                    if (v.length >= 3) c.items.put(v[1], pieces(v, 2));
                }
                case "ipal" -> {
                    if (v.length >= 5) {
                        int[] pal = {hex(v[2]), hex(v[3]), hex(v[4])};
                        if (pal[0] >= 0 && pal[1] >= 0 && pal[2] >= 0) c.itemPalettes.put(v[1], pal);
                    }
                }
                case "islot" -> {
                    if (v.length >= 3) c.itemSlots.put(v[1], v[2].toUpperCase(Locale.ROOT));
                }
                case "itier" -> {
                    if (v.length >= 3) {
                        try {
                            c.itemTiers.put(v[1], Math.min(7, Math.max(1, Integer.parseInt(v[2]))));
                        } catch (NumberFormatException ignored) { }
                    }
                }
                default -> { }
            }
        }
        return c;
    }

    /** A 0xRRGGBB colour from hex text, or -1 if it is not one. */
    static int hex(String s) {
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static String key(String slot, String look) { return slot.toUpperCase(Locale.ROOT) + ":" + look.toUpperCase(Locale.ROOT); }

    private static List<Piece> pieces(String[] v, int from) {
        List<Piece> out = new ArrayList<>();
        for (int i = from; i < v.length; i++) out.add(piece(v[i]));
        return List.copyOf(out);
    }

    /** Parses {@code model[@anchor][~]}. */
    public static Piece piece(String s) {
        boolean mirror = s.endsWith("~");
        if (mirror) s = s.substring(0, s.length() - 1);
        int at = s.indexOf('@');
        WornAnchor a = at < 0 ? null : WornAnchor.byKey(s.substring(at + 1));
        return new Piece(at < 0 ? s : s.substring(0, at), a, mirror);
    }

    /** The pieces a relic wears: its own entry if it has one, else the one of its slot and look; empty if neither exists. */
    public List<Piece> piecesFor(String relicId, String slot, String look) {
        List<Piece> p = relics.get(relicId);
        if (p != null) return p;
        p = looks.get(key(slot, look));
        if (p == null) p = looks.get(key(slot, defaultLook(slot)));      // 1.94.x: a slot/look pair the generator has no entry for wears the default look of its slot, never nothing
        return p == null ? List.of() : p;
    }

    /** The look a slot falls back to when its relic names a look the catalog has no entry for (mirrors {@code Look.defaultFor}); "" for a slot that is not worn. */
    public static String defaultLook(String slot) {
        if (slot == null) return "";
        return switch (slot.toUpperCase(Locale.ROOT)) {
            case "HEAD" -> "CIRCLET";
            case "CHEST" -> "PLATE";
            case "NECK" -> "AMULET";
            case "BACK" -> "CLOAK";
            case "BELT" -> "BELT";
            case "HANDS" -> "GAUNTLETS";
            case "CHARM" -> "CHARM";
            case "FEET" -> "BOOTS";
            default -> "";
        };
    }

    /** How a relic is worn according to the catalog: its own entry, the entry of its slot and look, the default look of its slot, or nothing at all. */
    public enum Source { SIGNATURE, LOOK, DEFAULT_LOOK, NONE }

    /** Where the models of this relic come from (see {@link #piecesFor}). */
    public Source sourceOf(String relicId, String slot, String look) {
        if (relics.containsKey(relicId)) return Source.SIGNATURE;
        if (looks.containsKey(key(slot, look))) return Source.LOOK;
        if (looks.containsKey(key(slot, defaultLook(slot)))) return Source.DEFAULT_LOOK;
        return Source.NONE;
    }

    /** The pieces an armour item (or mode) wears, by its registry path; empty if the catalog has no entry for it. */
    public List<Piece> itemPieces(String key) {
        List<Piece> p = items.get(key);
        return p == null ? List.of() : p;
    }

    /** The palette {main, trim, glow} the catalog names for an armour item, or null. */
    public int[] itemPalette(String key) { return itemPalettes.get(key); }

    /** The grade tier (1..7) the catalog names for an armour item, or {@code fallback}. */
    public int itemTier(String key, int fallback) { return itemTiers.getOrDefault(key, fallback); }

    /** The Curios slot name ("CHARM" ...) a non-relic worn item is drawn in, or null for an armour item or a mode. */
    public String itemSlot(String key) { return itemSlots.get(key); }

    /** Does the catalog have an {@code item} entry (of either look) under this key? */
    public boolean hasItem(String key) { return items.containsKey(key); }

    /** Is this list the explicit "draw nothing" entry (only {@link #NONE} pieces)? */
    public static boolean isNothing(List<Piece> pieces) {
        if (pieces.isEmpty()) return false;
        for (Piece p : pieces) if (!NONE.equals(p.model())) return false;
        return true;
    }

    public int itemCount() { return items.size(); }

    /** The theme named for a relic in the catalog ("ember" ...), or null. */
    public String themeOf(String relicId) { return themes.get(relicId); }

    public int lookCount() { return looks.size(); }

    public int relicCount() { return relics.size(); }
}
