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
 * </pre>
 * A piece is {@code model[@anchor][~]}: {@code @anchor} attaches it to another body part than the one named in the file (head, body, rarm, larm, rleg, lleg),
 * {@code ~} mirrors it left to right (how the left arm gets the right arm's model).
 */
public final class WornCatalog {
    /** One model on one body part. {@code anchor} is null for the one in the model's file. */
    public record Piece(String model, WornAnchor anchor, boolean mirror) {}

    private final Map<String, List<Piece>> looks = new HashMap<>(), relics = new HashMap<>();
    private final Map<String, String> themes = new HashMap<>();

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
                default -> { }
            }
        }
        return c;
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
        return p == null ? List.of() : p;
    }

    /** The theme named for a relic in the catalog ("ember" ...), or null. */
    public String themeOf(String relicId) { return themes.get(relicId); }

    public int lookCount() { return looks.size(); }

    public int relicCount() { return relics.size(); }
}
