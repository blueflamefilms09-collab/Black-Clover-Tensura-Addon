package com.newuniverse.nusmp.labyrinth.mega.theme;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Why: the Region Theme processor has to turn a deepslate brick into a nether brick AND a deepslate brick stair into a nether brick stair, a
 * dark oak fence gate into a crimson fence gate, and so on for every shape of the template palette, without a hand-written table per theme
 * (nine themes times thirty-one palette blocks would be 279 JSON lines that must all be right; one typo is a stair that stays grey). So a theme
 * names ONE base block per slot ({@link Slot}: brick, tile, polished, chiseled, cobbled, cracked_brick, cracked_tile, rock, wood) and this class
 * DERIVES the target of every palette block from it, by id suffix, the way {@code labyrinth.gen.PaletteSwap} does for the biome palette. Suffix
 * rules rather than a table of vanilla families because vanilla is not regular ({@code nether_bricks} has {@code nether_brick_stairs}, purpur has
 * {@code purpur_stairs} but no wall, quartz bricks have no stairs at all), and the exceptions are few enough to be spelled out here once and
 * tested, instead of being discovered in game as a stair that did not change.
 * <p>
 * The derivation rule, in order, for a palette block of slot S and shape H (plain, stairs, slab, wall, ...):
 * <ol>
 *   <li>the theme's {@code explicit} map wins when it names the template id and the target exists (any block, palette or not: how a theme turns
 *       {@code minecraft:lantern} into {@code minecraft:soul_lantern});</li>
 *   <li>the plain block is the family base of S itself;</li>
 *   <li>a shape is the base plus its suffix ({@code polished_diorite} + {@code _stairs}); when that id does not exist, the SINGULAR of a trailing
 *       {@code bricks} / {@code tiles} ({@code nether_bricks} gives {@code nether_brick_stairs}), then the base without a trailing {@code _block}
 *       ({@code purpur_block} gives {@code purpur_stairs});</li>
 *   <li>wood shapes use the stem of the planks ({@code crimson_planks} gives {@code crimson}); a log is {@code _log}, else {@code _stem} (crimson,
 *       warped), else {@code _block} (bamboo); wood NEVER falls back to a stone family;</li>
 *   <li>a STONE shape the family does not have (purpur has no wall, polished diorite has no wall, netherrack has nothing) is taken from the first
 *       FALLBACK slot that has it (brick, then tile, polished, cobbled: see {@link #FALLBACK}), so a wall in a purpur room becomes an end stone brick
 *       wall rather than staying grey;</li>
 *   <li>when nothing exists the answer is {@link How#NONE} and the processor leaves the template block alone (one deepslate stair in a nether room
 *       is a better failure than a purple-and-black cube).</li>
 * </ol>
 * Pure: no Minecraft types, the registry check is a {@link Predicate} the caller supplies (the game passes a lookup in {@code BuiltInRegistries.BLOCK},
 * the unit tests pass the list of vanilla block ids), so every rule is tested without a game.
 */
public final class ThemeSwap {
    private ThemeSwap() {}

    /** The slots of a theme's block families; {@link #key()} is the field name in the theme JSON ({@code "families": {"brick": ...}}). */
    public enum Slot {
        BRICK("brick"), TILE("tile"), POLISHED("polished"), CHISELED("chiseled"), COBBLED("cobbled"),
        CRACKED_BRICK("cracked_brick"), CRACKED_TILE("cracked_tile"), ROCK("rock"), WOOD("wood");

        private final String key;

        Slot(String key) { this.key = key; }

        public String key() { return key; }

        /** The slot named by a JSON key, or null. */
        public static Slot byKey(String key) {
            if (key == null) return null;
            String k = key.trim().toLowerCase(Locale.ROOT);
            for (Slot s : values()) if (s.key.equals(k)) return s;
            return null;
        }
    }

    /** The shape of a palette block inside its family; {@link #suffix()} is the id suffix a stone or wooden shape adds to the family stem. */
    public enum Shape {
        PLAIN(""), STAIRS("_stairs"), SLAB("_slab"), WALL("_wall"), FENCE("_fence"), FENCE_GATE("_fence_gate"), TRAPDOOR("_trapdoor"),
        DOOR("_door"), BUTTON("_button"), PRESSURE_PLATE("_pressure_plate"),
        /** A log: the stem plus {@code _log}, else {@code _stem} (crimson, warped), else {@code _block} (bamboo). */
        LOG(""),
        /** A stripped log: {@code stripped_} plus the same three endings. */
        STRIPPED_LOG("");

        private final String suffix;

        Shape(String suffix) { this.suffix = suffix; }

        public String suffix() { return suffix; }
    }

    /** One block of the template palette: its id, the slot of the family it belongs to, and its shape. */
    public record Entry(String id, Slot slot, Shape shape) {}

    private static Entry e(String id, Slot slot, Shape shape) { return new Entry(id, slot, shape); }

    /**
     * The 31 blocks the templates are drawn in, exactly {@code mega_lib.PALETTE} (a unit test reads the Python file and fails when the two differ).
     * Everything that is not in this list (glass, lanterns, candles, iron bars, chains, skulls, banners, gold, copper rods ...) is left alone by a theme
     * unless its {@code explicit} map names it.
     */
    public static final List<Entry> PALETTE = List.of(
            e("minecraft:deepslate_bricks", Slot.BRICK, Shape.PLAIN), e("minecraft:deepslate_brick_stairs", Slot.BRICK, Shape.STAIRS),
            e("minecraft:deepslate_brick_slab", Slot.BRICK, Shape.SLAB), e("minecraft:deepslate_brick_wall", Slot.BRICK, Shape.WALL),
            e("minecraft:deepslate_tiles", Slot.TILE, Shape.PLAIN), e("minecraft:deepslate_tile_stairs", Slot.TILE, Shape.STAIRS),
            e("minecraft:deepslate_tile_slab", Slot.TILE, Shape.SLAB), e("minecraft:deepslate_tile_wall", Slot.TILE, Shape.WALL),
            e("minecraft:polished_deepslate", Slot.POLISHED, Shape.PLAIN), e("minecraft:polished_deepslate_stairs", Slot.POLISHED, Shape.STAIRS),
            e("minecraft:polished_deepslate_slab", Slot.POLISHED, Shape.SLAB), e("minecraft:polished_deepslate_wall", Slot.POLISHED, Shape.WALL),
            e("minecraft:chiseled_deepslate", Slot.CHISELED, Shape.PLAIN),
            e("minecraft:cobbled_deepslate", Slot.COBBLED, Shape.PLAIN), e("minecraft:cobbled_deepslate_stairs", Slot.COBBLED, Shape.STAIRS),
            e("minecraft:cobbled_deepslate_slab", Slot.COBBLED, Shape.SLAB), e("minecraft:cobbled_deepslate_wall", Slot.COBBLED, Shape.WALL),
            e("minecraft:cracked_deepslate_bricks", Slot.CRACKED_BRICK, Shape.PLAIN), e("minecraft:cracked_deepslate_tiles", Slot.CRACKED_TILE, Shape.PLAIN),
            e("minecraft:deepslate", Slot.ROCK, Shape.PLAIN),
            e("minecraft:dark_oak_planks", Slot.WOOD, Shape.PLAIN), e("minecraft:dark_oak_stairs", Slot.WOOD, Shape.STAIRS),
            e("minecraft:dark_oak_slab", Slot.WOOD, Shape.SLAB), e("minecraft:dark_oak_fence", Slot.WOOD, Shape.FENCE),
            e("minecraft:dark_oak_fence_gate", Slot.WOOD, Shape.FENCE_GATE), e("minecraft:dark_oak_trapdoor", Slot.WOOD, Shape.TRAPDOOR),
            e("minecraft:dark_oak_door", Slot.WOOD, Shape.DOOR), e("minecraft:dark_oak_log", Slot.WOOD, Shape.LOG),
            e("minecraft:stripped_dark_oak_log", Slot.WOOD, Shape.STRIPPED_LOG), e("minecraft:dark_oak_button", Slot.WOOD, Shape.BUTTON),
            e("minecraft:dark_oak_pressure_plate", Slot.WOOD, Shape.PRESSURE_PLATE));

    /**
     * Where a slot looks when its own family lacks a shape (or the slot is missing from the theme): the first listed slot that has the shape wins.
     * Brick first for everything because the walls are the face of a sector, so a stray stair should match the wall; wood has no fallback at all.
     */
    public static final Map<Slot, List<Slot>> FALLBACK = new EnumMap<>(Slot.class);

    static {
        FALLBACK.put(Slot.BRICK, List.of(Slot.TILE, Slot.POLISHED, Slot.COBBLED));
        FALLBACK.put(Slot.TILE, List.of(Slot.BRICK, Slot.POLISHED, Slot.COBBLED));
        FALLBACK.put(Slot.POLISHED, List.of(Slot.BRICK, Slot.TILE, Slot.COBBLED));
        FALLBACK.put(Slot.CHISELED, List.of(Slot.POLISHED, Slot.BRICK, Slot.TILE));
        FALLBACK.put(Slot.COBBLED, List.of(Slot.BRICK, Slot.POLISHED, Slot.TILE));
        FALLBACK.put(Slot.CRACKED_BRICK, List.of(Slot.BRICK, Slot.TILE));
        FALLBACK.put(Slot.CRACKED_TILE, List.of(Slot.TILE, Slot.BRICK));
        FALLBACK.put(Slot.ROCK, List.of(Slot.COBBLED, Slot.BRICK));
        FALLBACK.put(Slot.WOOD, List.of());
    }

    /** How a palette block's target was found; {@link #NONE} means the processor leaves the block as the template drew it. */
    public enum How {
        /** The target is the template block itself (the baseline theme, or a slot that keeps the deepslate block). */
        SAME,
        /** The theme's {@code explicit} map named it. */
        EXPLICIT,
        /** The plain family base, or the base plus the shape's suffix (nothing cleverer was needed). */
        DIRECT,
        /** The singular of bricks/tiles, the stem without {@code _block}, a stem's {@code _stem} / {@code _block} log: still the theme's own family. */
        DERIVED,
        /** Taken from another slot's family because the theme's own family has no such shape (see {@link #FALLBACK}). */
        FALLBACK,
        /** Nothing exists: no swap. */
        NONE
    }

    /** The answer for one template block: the target id (null for {@link How#NONE}) and how it was found. */
    public record Resolved(String target, How how) {
        /** Does the processor have to change the block at all? */
        public boolean swaps() { return target != null && how != How.NONE && how != How.SAME; }
    }

    // ---------------------------------------------------------------- candidates
    /** The ids a shape of {@code baseId} may have, best first. A plain block is the base itself. */
    static List<String> candidates(String baseId, Slot slot, Shape shape) {
        List<String> out = new ArrayList<>(4);
        if (baseId == null || baseId.isBlank()) return out;
        String id = baseId.trim();
        int colon = id.indexOf(':');
        String ns = colon < 0 ? "minecraft:" : id.substring(0, colon + 1);
        String path = colon < 0 ? id : id.substring(colon + 1);
        if (shape == Shape.PLAIN) {
            out.add(ns + path);
            return out;
        }
        if (slot == Slot.WOOD) {
            String stem = path.endsWith("_planks") ? path.substring(0, path.length() - "_planks".length()) : path;
            switch (shape) {
                case LOG -> {
                    out.add(ns + stem + "_log");
                    out.add(ns + stem + "_stem");
                    out.add(ns + stem + "_block");
                }
                case STRIPPED_LOG -> {
                    out.add(ns + "stripped_" + stem + "_log");
                    out.add(ns + "stripped_" + stem + "_stem");
                    out.add(ns + "stripped_" + stem + "_block");
                }
                default -> out.add(ns + stem + shape.suffix());
            }
            return out;
        }
        String suffix = shape.suffix();
        out.add(ns + path + suffix);
        if (path.endsWith("bricks") || path.endsWith("tiles")) out.add(ns + path.substring(0, path.length() - 1) + suffix);
        if (path.endsWith("_block")) out.add(ns + path.substring(0, path.length() - "_block".length()) + suffix);
        return out;
    }

    // ---------------------------------------------------------------- the rule
    /** The target of one palette block under a theme (see the class comment for the order). */
    public static Resolved resolve(Entry entry, Map<String, String> families, Map<String, String> explicit, Predicate<String> exists) {
        String ex = explicit == null ? null : explicit.get(entry.id());
        if (ex != null && !ex.isBlank() && exists.test(ex.trim())) return new Resolved(ex.trim(), ex.trim().equals(entry.id()) ? How.SAME : How.EXPLICIT);
        List<Slot> order = new ArrayList<>(4);
        order.add(entry.slot());
        order.addAll(FALLBACK.getOrDefault(entry.slot(), List.of()));
        for (int s = 0; s < order.size(); s++) {
            Slot slot = order.get(s);                                                      // wood has no fallback slots, so a wooden entry only ever sees WOOD here
            List<String> cands = candidates(families == null ? null : families.get(slot.key()), slot, entry.shape());
            for (int c = 0; c < cands.size(); c++) {
                String target = cands.get(c);
                if (!exists.test(target)) continue;
                if (target.equals(entry.id())) return new Resolved(target, How.SAME);
                How how = s > 0 ? How.FALLBACK : (c == 0 ? How.DIRECT : How.DERIVED);
                return new Resolved(target, how);
            }
        }
        return new Resolved(null, How.NONE);
    }

    /**
     * Every palette block's target, plus the {@code explicit} entries that are not palette blocks (a lantern becoming a soul lantern). Insertion order
     * is the palette's, so a log or a test sees a stable listing.
     */
    public static Map<String, Resolved> table(Map<String, String> families, Map<String, String> explicit, Predicate<String> exists) {
        Map<String, Resolved> out = new LinkedHashMap<>();
        for (Entry e : PALETTE) out.put(e.id(), resolve(e, families, explicit, exists));
        if (explicit != null) {
            for (Map.Entry<String, String> x : explicit.entrySet()) {
                String from = x.getKey(), to = x.getValue();
                if (from == null || to == null || out.containsKey(from) || from.equals(to) || !exists.test(to.trim())) continue;
                out.put(from, new Resolved(to.trim(), How.EXPLICIT));
            }
        }
        return out;
    }

    /** A theme's light shift never takes a light block below this level (or below its own level when it was lower): the templates were lit to stay clear of the dungeon's gloom limit. */
    public static final int LIGHT_FLOOR = 6;

    /**
     * The level an invisible {@code minecraft:light} block takes in a sector whose theme shifts the light by {@code shift} (the window glow and the glowing growths
     * of the templates): moved by the shift, never above 15 and never below {@link #LIGHT_FLOOR} (a block that was already lower keeps its own level as the floor,
     * and a block of level 0, which gives no light, stays 0).
     */
    public static int shiftLight(int level, int shift) {
        if (level <= 0) return level;
        int floor = Math.min(level, LIGHT_FLOOR);
        return Math.max(floor, Math.min(15, level + shift));
    }

    /** True when the id is one of the 31 palette blocks. */
    public static boolean isPalette(String id) {
        for (Entry e : PALETTE) if (e.id().equals(id)) return true;
        return false;
    }

    /**
     * Everything wrong with a theme's block data, as human-readable lines (empty = fine): a family id that is not a registered block, a slot that is
     * missing, an explicit pair whose source or target does not exist, and every palette block that ends in {@link How#NONE}. The game logs each line
     * ONCE per theme; the data test fails the build on any line for the nine shipped themes.
     */
    public static List<String> problems(Map<String, String> families, Map<String, String> explicit, Predicate<String> exists) {
        List<String> out = new ArrayList<>();
        for (Slot s : Slot.values()) {
            String base = families == null ? null : families.get(s.key());
            if (base == null || base.isBlank()) out.add("family slot '" + s.key() + "' is missing (its blocks fall back to " + FALLBACK.get(s) + ")");
            else if (!exists.test(base.trim())) out.add("family slot '" + s.key() + "' names " + base + ", which is not a registered block (no swap for it)");
        }
        if (explicit != null) {
            for (Map.Entry<String, String> x : explicit.entrySet()) {
                if (x.getKey() == null || !exists.test(x.getKey().trim())) out.add("explicit source " + x.getKey() + " is not a registered block");
                if (x.getValue() == null || !exists.test(x.getValue().trim())) out.add("explicit target " + x.getValue() + " (for " + x.getKey() + ") is not a registered block");
            }
        }
        for (Entry e : PALETTE) {
            if (resolve(e, families, explicit, exists).how() == How.NONE) out.add("palette block " + e.id() + " has no target: it stays as the template draws it");
        }
        return out;
    }
}
