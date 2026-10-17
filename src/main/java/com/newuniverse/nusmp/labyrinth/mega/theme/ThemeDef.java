package com.newuniverse.nusmp.labyrinth.mega.theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Why: what a Region Theme MEANS is data ({@code data/nusmp/mega/themes/<id>.json}), not code, so the owner or a datapack can retune a sector's look,
 * its relic pool and its catastrophes without a mod update; {@code RegionTheme} only names the nine themes (it must exist at class-load time in saved
 * data and in tests). This record is that data after parsing: the block families the {@code nusmp:region_theme} processor swaps in
 * ({@link ThemeSwap}), the per-block overrides, the keywords that say which relics belong to the theme ({@link RelicThemes} reads them to decide the
 * theme of the NEXT region from the relic that was chosen), the weights of the three catastrophes and the ambient particle. It is parsed from a plain tree of maps, lists, strings and numbers ({@link #parse}) instead of from Gson's
 * {@code JsonObject} so the parser, and the nine shipped files with it, are tested without Minecraft or a JSON library on the class path: the game
 * glue ({@code ThemeDefs}) only converts {@code JsonElement}s into that tree. A broken field never throws: it becomes a warning line and the field
 * keeps a safe default, because a typo in a datapack must not stop a sector from generating.
 * <p>
 * Besides the palette, a theme carries the three things that make a sector FEEL different before a block is read: its light ({@code light}: a shift,
 * from -8 to +8, of the invisible light blocks the templates use for window glow and glowing growths, so a Radiant sector is brighter and an Abyss one dimmer
 * than the baseline; the lanterns themselves change through the {@code explicit} map, warm for cold and cold for warm), its mood ({@code sound}: one
 * vanilla ambience, played now and then somewhere near each player, and {@code ambient}: the drifting particle) and its danger ({@code danger}: how much
 * of the sector's threat the Crucible's catastrophes count, 0.5 to 2). All three are optional; a file without them is the baseline's light, silence and
 * danger 1.
 */
public record ThemeDef(String id, String name, int color, Map<String, String> families, Map<String, String> explicit, List<String> keywords,
                       Map<String, Integer> catastrophes, String ambient, int lightShift, Mood mood, double danger) {
    /** Largest shift of the light blocks, either way. */
    public static final int MAX_LIGHT_SHIFT = 8;
    /** The bounds of a theme's danger multiplier. */
    public static final double MIN_DANGER = 0.5, MAX_DANGER = 2.0;

    /**
     * The ambience of a theme: a vanilla sound event id, how loud (0.05 to 1) and how high (0.5 to 2) it is played, and how rarely: once in {@code every}
     * two-second pulses per player (1 to 60), at a random place five to nine blocks away so it comes from "somewhere in the stone". An empty id is silence.
     */
    public record Mood(String sound, float volume, float pitch, int every) {
        public static final Mood NONE = new Mood("", 0.5f, 1.0f, 6);

        public boolean any() { return !sound.isEmpty(); }
    }

    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern COLOR = Pattern.compile("#?[0-9a-fA-F]{6}");

    public ThemeDef {
        families = Collections.unmodifiableMap(new LinkedHashMap<>(families));
        explicit = Collections.unmodifiableMap(new LinkedHashMap<>(explicit));
        keywords = List.copyOf(keywords);
        catastrophes = Collections.unmodifiableMap(new LinkedHashMap<>(catastrophes));
        ambient = ambient == null ? "" : ambient;
        lightShift = Math.max(-MAX_LIGHT_SHIFT, Math.min(MAX_LIGHT_SHIFT, lightShift));
        mood = mood == null ? Mood.NONE : mood;
        danger = Double.isNaN(danger) ? 1.0 : Math.max(MIN_DANGER, Math.min(MAX_DANGER, danger));
    }

    /** The weight of a catastrophe ({@code collapse}, {@code flood}, {@code spirit}); 0 when the theme does not list it. */
    public int weight(String catastrophe) { return Math.max(0, catastrophes.getOrDefault(catastrophe, 0)); }

    /** The baseline: the deepslate palette itself, so every block maps to itself (the processor does nothing for it). */
    public static ThemeDef gravity() {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("brick", "minecraft:deepslate_bricks");
        f.put("tile", "minecraft:deepslate_tiles");
        f.put("polished", "minecraft:polished_deepslate");
        f.put("chiseled", "minecraft:chiseled_deepslate");
        f.put("cobbled", "minecraft:cobbled_deepslate");
        f.put("cracked_brick", "minecraft:cracked_deepslate_bricks");
        f.put("cracked_tile", "minecraft:cracked_deepslate_tiles");
        f.put("rock", "minecraft:deepslate");
        f.put("wood", "minecraft:dark_oak_planks");
        Map<String, Integer> c = new LinkedHashMap<>();
        c.put("collapse", 4);
        c.put("flood", 1);
        c.put("spirit", 1);
        return new ThemeDef("gravity", "Gravity", 0x6B6F7A, f, Map.of(), List.of("gravity", "weight", "anchor", "hammer", "stone", "mountain", "maul", "heavy", "earth"), c, "minecraft:ash", 0, Mood.NONE, 1.0);
    }

    /** A theme with the deepslate palette and the given id: what a theme without a file looks like (no swap, a plain catastrophe table). */
    public static ThemeDef plain(String id) {
        ThemeDef g = gravity();
        return new ThemeDef(id, capitalise(id), g.color, g.families, Map.of(), List.of(), g.catastrophes, "", 0, Mood.NONE, 1.0);
    }

    /** The result of {@link #parse}: the definition (null only when the tree is not an object) and the warnings found on the way. */
    public record Parsed(ThemeDef def, List<String> warnings) {}

    /**
     * Reads a theme from a tree of {@code Map<String,Object>}, {@code List<Object>}, {@code String}, {@code Number} and {@code Boolean}.
     * @param fallbackId the id to use when the file has none (the file name)
     */
    public static Parsed parse(String fallbackId, Object tree) {
        List<String> warn = new ArrayList<>();
        if (!(tree instanceof Map<?, ?> root)) {
            warn.add("a theme file must be a JSON object");
            return new Parsed(null, warn);
        }
        String id = text(root.get("id"));
        if (id.isEmpty() || !id.matches("[a-z0-9_]+")) {
            if (!id.isEmpty()) warn.add("id '" + id + "' is not lower case letters, digits and underscores; using '" + fallbackId + "'");
            id = fallbackId == null ? "" : fallbackId.toLowerCase(Locale.ROOT);
        }
        if (id.isEmpty()) {
            warn.add("a theme needs an id");
            return new Parsed(null, warn);
        }
        String name = text(root.get("name"));
        if (name.isEmpty()) name = capitalise(id);
        int color = 0xAAAAAA;
        String col = text(root.get("color"));
        if (!col.isEmpty()) {
            if (COLOR.matcher(col).matches()) color = Integer.parseInt(col.startsWith("#") ? col.substring(1) : col, 16);
            else warn.add("color '" + col + "' is not #RRGGBB");
        }
        Map<String, String> families = new LinkedHashMap<>();
        if (root.get("families") instanceof Map<?, ?> fam) {
            for (Map.Entry<?, ?> x : fam.entrySet()) {
                String key = String.valueOf(x.getKey());
                if (ThemeSwap.Slot.byKey(key) == null) { warn.add("families." + key + " is not a slot (" + slotKeys() + ")"); continue; }
                String block = blockId(x.getValue());
                if (block == null) { warn.add("families." + key + " is not a block id: " + x.getValue()); continue; }
                families.put(key.trim().toLowerCase(Locale.ROOT), block);
            }
        } else if (root.get("families") != null) {
            warn.add("families must be an object");
        }
        Map<String, String> explicit = new LinkedHashMap<>();
        if (root.get("explicit") instanceof Map<?, ?> ex) {
            for (Map.Entry<?, ?> x : ex.entrySet()) {
                String from = blockId(x.getKey()), to = blockId(x.getValue());
                if (from == null || to == null) { warn.add("explicit " + x.getKey() + " -> " + x.getValue() + " is not a pair of block ids"); continue; }
                explicit.put(from, to);
            }
        } else if (root.get("explicit") != null) {
            warn.add("explicit must be an object");
        }
        List<String> keywords = new ArrayList<>();
        if (root.get("relics") instanceof Map<?, ?> relics) {
            if (relics.get("keywords") instanceof List<?> list) {
                for (Object k : list) {
                    String w = text(k).toLowerCase(Locale.ROOT);
                    if (!w.isEmpty() && !keywords.contains(w)) keywords.add(w);
                }
            } else if (relics.get("keywords") != null) {
                warn.add("relics.keywords must be a list of words");
            }
        } else if (root.get("relics") != null) {
            warn.add("relics must be an object");
        }
        Map<String, Integer> cats = new LinkedHashMap<>();
        if (root.get("catastrophes") instanceof Map<?, ?> cm) {
            for (Map.Entry<?, ?> x : cm.entrySet()) {
                String key = String.valueOf(x.getKey()).toLowerCase(Locale.ROOT);
                if (!(key.equals("collapse") || key.equals("flood") || key.equals("spirit"))) { warn.add("catastrophes." + x.getKey() + " is not collapse, flood or spirit"); continue; }
                if (x.getValue() instanceof Number n) cats.put(key, Math.max(0, Math.min(100, n.intValue())));
                else warn.add("catastrophes." + key + " must be a whole number");
            }
        } else if (root.get("catastrophes") != null) {
            warn.add("catastrophes must be an object");
        }
        if (cats.isEmpty()) {
            cats.putAll(gravity().catastrophes);
            warn.add("no catastrophe weights: using collapse 4, flood 1, spirit 1");
        }
        String ambient = text(root.get("ambient")).toLowerCase(Locale.ROOT);
        if (!ambient.isEmpty()) {
            if (!ambient.contains(":")) ambient = "minecraft:" + ambient;
            if (!ID.matcher(ambient).matches()) {
                warn.add("ambient '" + ambient + "' is not a particle id");
                ambient = "";
            }
        }
        int light = 0;
        if (root.get("light") instanceof Number n) {
            light = n.intValue();
            if (light < -MAX_LIGHT_SHIFT || light > MAX_LIGHT_SHIFT) {
                warn.add("light " + light + " is outside -" + MAX_LIGHT_SHIFT + " to " + MAX_LIGHT_SHIFT + ": clamped");
                light = Math.max(-MAX_LIGHT_SHIFT, Math.min(MAX_LIGHT_SHIFT, light));
            }
        } else if (root.get("light") != null) {
            warn.add("light must be a whole number (a shift of the light blocks)");
        }
        double danger = 1.0;
        if (root.get("danger") instanceof Number n) {
            danger = n.doubleValue();
            if (!(danger >= MIN_DANGER && danger <= MAX_DANGER)) {
                warn.add("danger " + danger + " is outside " + MIN_DANGER + " to " + MAX_DANGER + ": clamped");
                danger = Double.isNaN(danger) ? 1.0 : Math.max(MIN_DANGER, Math.min(MAX_DANGER, danger));
            }
        } else if (root.get("danger") != null) {
            warn.add("danger must be a number");
        }
        Mood mood = Mood.NONE;
        if (root.get("sound") instanceof Map<?, ?> sm) {
            String sid = text(sm.get("id")).toLowerCase(Locale.ROOT);
            if (!sid.isEmpty() && !sid.contains(":")) sid = "minecraft:" + sid;
            if (sid.isEmpty() || !ID.matcher(sid).matches()) {
                warn.add("sound.id '" + sid + "' is not a sound event id: no mood sound");
            } else {
                float volume = number(sm.get("volume"), 0.5, 0.05, 1.0, "sound.volume", warn);
                float pitch = number(sm.get("pitch"), 1.0, 0.5, 2.0, "sound.pitch", warn);
                int every = Math.round(number(sm.get("every"), 6, 1, 60, "sound.every", warn));
                mood = new Mood(sid, volume, pitch, every);
            }
        } else if (root.get("sound") != null) {
            warn.add("sound must be an object {id, volume, pitch, every}");
        }
        return new Parsed(new ThemeDef(id, name, color, families, explicit, keywords, cats, ambient, light, mood, danger), warn);
    }

    /** A number field with a default and bounds; a value outside the bounds, or not a number, is clamped (or defaulted) with one warning. */
    private static float number(Object o, double def, double lo, double hi, String what, List<String> warn) {
        if (o == null) return (float) def;
        if (!(o instanceof Number n)) {
            warn.add(what + " must be a number: using " + def);
            return (float) def;
        }
        double v = n.doubleValue();
        if (!(v >= lo && v <= hi)) {
            warn.add(what + " " + v + " is outside " + lo + " to " + hi + ": clamped");
            v = Double.isNaN(v) ? def : Math.max(lo, Math.min(hi, v));
        }
        return (float) v;
    }

    private static String slotKeys() {
        StringBuilder b = new StringBuilder();
        for (ThemeSwap.Slot s : ThemeSwap.Slot.values()) b.append(b.length() == 0 ? "" : ", ").append(s.key());
        return b.toString();
    }

    private static String text(Object o) { return o instanceof String s ? s.trim() : ""; }

    /** A block id with its namespace made explicit ({@code stone} gives {@code minecraft:stone}); null when it is not an id. */
    private static String blockId(Object o) {
        String s = text(o).toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return null;
        if (!s.contains(":")) s = "minecraft:" + s;
        return ID.matcher(s).matches() ? s : null;
    }

    private static String capitalise(String id) {
        if (id == null || id.isEmpty()) return "";
        return Character.toUpperCase(id.charAt(0)) + id.substring(1).replace('_', ' ');
    }
}
