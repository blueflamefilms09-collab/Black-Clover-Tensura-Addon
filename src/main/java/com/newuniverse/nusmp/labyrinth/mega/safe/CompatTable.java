package com.newuniverse.nusmp.labyrinth.mega.safe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Why: the brief wants Farmer's Delight stoves, skillets and cutting boards in place of generic campfires, and MrCrayfish's Furniture Mod:
 * Refurbished sofas, tables and storage in place of stair-chairs and wool beds, when those mods are installed, and plain vanilla otherwise,
 * from ONE set of templates. The templates only say WHAT a block is ({@code nusmp:seat;as=chair}); this table says which block of which mod
 * can stand in for it. It is data, not code, because the block ids of other mods cannot be verified here and must be correctable by editing a
 * JSON file ({@code data/nusmp/mega/compat/<mod>.json}) rather than recompiling: each hint lists several plausible ids in order, a candidate is
 * used only if its mod is loaded AND the id exists in the block registry, so a wrong id silently falls through to the next one and finally to
 * the vanilla block. This class is the PURE half (no Minecraft types): the parsed files go in as {@link Source}s, the loader and the processor
 * supply "is this mod loaded" and "does this block exist" as predicates, so loaded / absent / unknown id / empty are all unit-tested.
 * The tokens {@code {wood}} and {@code {color}} let one entry cover a whole family ({@code refurbished_furniture:chair_{wood}}); they are
 * filled from the block being replaced ({@link #tokensFor}) and fall back to {@code oak} and {@code red}.
 */
public final class CompatTable {
    /** One parsed file: the mod it belongs to, the candidate ids per hint, the hints whose facing must be reversed, the name of the target's facing property. */
    public record Source(String mod, Map<String, List<String>> swaps, Set<String> flipFacing, String facingProperty) {}

    /** The chosen replacement: a real block id, whether the horizontal facing is reversed, and the name of the target's facing property. */
    public record Pick(String id, boolean flipFacing, String facingProperty) {}

    private record Entry(String mod, String template, boolean flip, String facingProperty) {}

    public static final String DEFAULT_WOOD = "oak", DEFAULT_COLOR = "red", DEFAULT_FACING_PROPERTY = "facing";
    public static final CompatTable EMPTY = new CompatTable(Map.of());

    /** The hints the shipped templates use; the data may name others (they simply never match a marker). */
    public static final Set<String> KNOWN_HINTS = Set.of("chair", "sofa", "table", "counter", "cutting_board", "storage", "stove", "skillet",
            "cooking_pot", "bed");

    private static final Pattern TOKEN = Pattern.compile("\\{([a-z_]+)\\}");
    private static final Pattern ID = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");
    private static final List<String> WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo",
            "crimson", "warped");
    /** Longest first, so {@code _fence_gate} wins over {@code _gate}. */
    private static final List<String> WOOD_SUFFIXES = List.of("_pressure_plate", "_hanging_sign", "_wall_sign", "_fence_gate", "_trapdoor", "_planks",
            "_stairs", "_button", "_hyphae", "_fence", "_stem", "_door", "_slab", "_sign", "_wood", "_log");
    /** Two-word colours first, so {@code light_blue_bed} is light blue and not blue. */
    private static final List<String> COLORS = List.of("light_blue", "light_gray", "white", "orange", "magenta", "yellow", "lime", "pink", "gray", "cyan",
            "purple", "blue", "brown", "green", "red", "black");

    private final Map<String, List<Entry>> byHint;

    private CompatTable(Map<String, List<Entry>> byHint) { this.byHint = byHint; }

    /**
     * Builds the table from parsed files, in the order given (the loader sorts by file name so the result does not depend on the pack order).
     * Entries with a blank hint or id are dropped; a null source is skipped; the same hint in two files concatenates in file order.
     */
    public static CompatTable of(List<Source> sources) {
        Map<String, List<Entry>> out = new LinkedHashMap<>();
        if (sources != null) {
            for (Source s : sources) {
                if (s == null || s.mod() == null || s.mod().isBlank() || s.swaps() == null) continue;
                String mod = s.mod().trim().toLowerCase(Locale.ROOT);
                String facing = s.facingProperty() == null || s.facingProperty().isBlank() ? DEFAULT_FACING_PROPERTY : s.facingProperty().trim();
                Set<String> flips = new LinkedHashSet<>();
                if (s.flipFacing() != null) for (String f : s.flipFacing()) if (f != null) flips.add(f.trim().toLowerCase(Locale.ROOT));
                for (Map.Entry<String, List<String>> e : s.swaps().entrySet()) {
                    if (e.getKey() == null || e.getKey().isBlank() || e.getValue() == null) continue;
                    String hint = e.getKey().trim().toLowerCase(Locale.ROOT);
                    for (String id : e.getValue()) {
                        if (id == null || id.isBlank()) continue;
                        out.computeIfAbsent(hint, k -> new ArrayList<>()).add(new Entry(mod, id.trim(), flips.contains(hint), facing));
                    }
                }
            }
        }
        Map<String, List<Entry>> frozen = new LinkedHashMap<>();
        for (Map.Entry<String, List<Entry>> e : out.entrySet()) frozen.put(e.getKey(), Collections.unmodifiableList(e.getValue()));
        return new CompatTable(Collections.unmodifiableMap(frozen));
    }

    /** True when no hint has any candidate (no compat mod loaded, or no data): the processor then has nothing to do. */
    public boolean isEmpty() { return byHint.isEmpty(); }

    public boolean hasHint(String hint) { return hint != null && byHint.containsKey(hint.trim().toLowerCase(Locale.ROOT)); }

    public Set<String> hints() { return byHint.keySet(); }

    /** How many candidate ids are listed for the hint (loaded or not); 0 for an unknown hint. */
    public int candidateCount(String hint) {
        List<Entry> l = hint == null ? null : byHint.get(hint.trim().toLowerCase(Locale.ROOT));
        return l == null ? 0 : l.size();
    }

    /**
     * The first candidate for {@code hint} whose mod is loaded, whose tokens all resolve to a well-formed id and whose block exists; null when
     * there is none (the caller keeps the vanilla block). {@code tokens} may be null (then only token-free candidates can match).
     */
    public Pick pick(String hint, Predicate<String> modLoaded, Predicate<String> blockExists, Map<String, String> tokens) {
        if (hint == null || modLoaded == null || blockExists == null) return null;
        List<Entry> l = byHint.get(hint.trim().toLowerCase(Locale.ROOT));
        if (l == null) return null;
        for (Entry e : l) {
            if (!modLoaded.test(e.mod())) continue;
            String id = expand(e.template(), tokens);
            if (id == null || !validId(id)) continue;
            if (!blockExists.test(id)) continue;
            return new Pick(id, e.flip(), e.facingProperty());
        }
        return null;
    }

    /** Fills {@code {token}}s from the map; null when one has no value (the candidate is then skipped, not guessed). */
    public static String expand(String template, Map<String, String> tokens) {
        if (template == null) return null;
        Matcher m = TOKEN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String v = tokens == null ? null : tokens.get(m.group(1));
            if (v == null || v.isBlank()) return null;
            m.appendReplacement(sb, Matcher.quoteReplacement(v));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Is this a well-formed {@code namespace:path} id (lower case, no spaces, no leftover braces)? */
    public static boolean validId(String id) { return id != null && ID.matcher(id).matches(); }

    /** The tokens for replacing the block with this path (the part after the colon): its wood and its colour, or the defaults. */
    public static Map<String, String> tokensFor(String blockPath) {
        Map<String, String> t = new LinkedHashMap<>();
        t.put("wood", woodOf(blockPath));
        t.put("color", colorOf(blockPath));
        return t;
    }

    /** The wood a block is made of ({@code dark_oak_stairs} gives {@code dark_oak}, {@code stripped_birch_log} gives {@code birch}), else {@code oak}. */
    public static String woodOf(String blockPath) {
        if (blockPath == null) return DEFAULT_WOOD;
        String p = blockPath.trim().toLowerCase(Locale.ROOT);
        if (p.startsWith("stripped_")) p = p.substring(9);
        for (String suffix : WOOD_SUFFIXES) {
            if (p.endsWith(suffix)) { p = p.substring(0, p.length() - suffix.length()); break; }
        }
        return WOODS.contains(p) ? p : DEFAULT_WOOD;
    }

    /** The dye colour at the start of a block's name ({@code light_blue_bed} gives {@code light_blue}), else {@code red}. */
    public static String colorOf(String blockPath) {
        if (blockPath == null) return DEFAULT_COLOR;
        String p = blockPath.trim().toLowerCase(Locale.ROOT);
        for (String c : COLORS) if (p.startsWith(c + "_")) return c;
        return DEFAULT_COLOR;
    }
}
