package com.newuniverse.nusmp.labyrinth.mega.decennial.arena;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: a thematic boss arena is HARDCODED (the owner's word: "a specific, hardcoded BossArenaStructure that matches the boss's theme"), and the only way to make a hardcoded build testable without a
 * game is to make it a PLAN: a pure object that holds, for a square footprint of {@code size} blocks, which block (as text in the form the game's own commands use, so a test can check it against
 * the table of the game's real blocks) stands in every cell, plus everything the fight needs to know about the hall besides its blocks: the INTERACTIVE ELEMENTS (the tombstones that spawn adds
 * when broken, the pillars that can be brought down, the root nodes of the Underworld Root ...), the HAZARD ZONES the lair actions aim at, the spots adds come from, the doors, and the cells that
 * seal the entry until the fight is over. The game side ({@code ArenaPlacer}) only turns a plan into block writes inside the Dungeon Guard's internal scope and the runtime
 * ({@code DecennialArenas}) only reads the plan's element list; nothing about WHAT a hall is lives anywhere but in the plan builders ({@code ArenaThemes}), so the same plan is what the
 * tests walk, what the previews draw and what the game builds.
 *
 * <p>Coordinates are local: {@code x} east and {@code z} south in {@code 0..size-1}, {@code y} up with {@code 0} the walking surface (the floor blocks), {@code -FOUNDATION..-1} the foundation under
 * it and {@code 1..height-1} the room, the ceiling at {@code height-1}. The hall faces NORTH: the sealed Gatekeeper is in the north wall (z 0..1) and the entry in the south wall (z size-2..size-1), both
 * on the middle column {@code c = size / 2}; the Guardian Altar stands on the middle of the hall. {@code size} is odd. When the hall is built into a floor of the Mega-Dungeon the plan is rotated
 * to the way that floor's own Gatekeeper faces, and the cells the floor's template already owns (the doorways, the altar) are RESERVED: a plan never writes them in that mode.
 */
public final class BossArenaStructure {
    public static final int MIN_SIZE = 41, MAX_SIZE = 61, FOUNDATION = 4;
    /** The footprint of the Guardian's hall in a floor of the Mega-Dungeon (five modules of nine); in a floor the plan is built at this size. */
    public static final int SECTOR_SIZE = 45;

    /** What an interactive element is, for the previews, the lang and the lair actions that look for one. */
    public enum Kind { TOMBSTONE, PILLAR, COFFIN, ROOT_NODE, BRAZIER, CRYSTAL, MIRROR, STATUE, EGG_SAC, SHELF, VALVE, CHANDELIER, URN, SHROOM, TOTEM, GATE, BLOOM }

    /** What breaking an element does: {@code ADDS} spawns the element's adds, {@code TOPPLE} brings the pillar down on a strip of floor, {@code SHATTER} bursts it (a small hurt in a ring). */
    public enum Effect { ADDS, TOPPLE, SHATTER }

    /** A block at a cell (the extras of a standalone hall, the rubble of a fallen pillar). */
    public record Placed(int x, int y, int z, String spec) {}

    /** A hazard zone a lair action can aim at: its kind (grave, pillar, geyser, beam, root, nest ...), centre column and radius. */
    public record Zone(String kind, int x, int z, int r) {}

    /** A column adds step out of. */
    public record Spot(int x, int z) {}

    /**
     * An interactive element: a cluster of cells that is explicitly allowed to break (the Dungeon Guard holds every other block of the hall) and what its breaking does. {@code rubble} is placed when it
     * topples and taken away again when the hall resets. {@code strip} is the floor strip a toppled pillar crushes: {@code {x0, z0, x1, z1}}.
     */
    public static final class Element {
        public final int id;
        public final Kind kind;
        public final Effect effect;
        public final int adds;
        public final List<Placed> cells = new ArrayList<>();
        public final List<Placed> rubble = new ArrayList<>();
        public int[] strip = new int[0];
        /** A weak point: breaking it hurts the boss by this share of its health (0 for none). */
        public double bossShare;
        /** A locked element is never breakable by a player or a blow: only the hall itself removes it (the great gate of the Underworld Root opens when the boss falls). */
        public boolean locked;

        Element(int id, Kind kind, Effect effect, int adds) {
            this.id = id;
            this.kind = kind;
            this.effect = effect;
            this.adds = adds;
        }

        /** The column and height of the first (lowest) cell: where an effect starts. */
        public int x() { return cells.isEmpty() ? 0 : cells.get(0).x(); }

        public int y() { return cells.isEmpty() ? 0 : cells.get(0).y(); }

        public int z() { return cells.isEmpty() ? 0 : cells.get(0).z(); }

        public boolean contains(int x, int y, int z) {
            for (Placed p : cells) if (p.x() == x && p.y() == y && p.z() == z) return true;
            return false;
        }
    }

    public final String theme;
    public final int size, height, c;
    private final short[] cells;
    private final BitSet reserved, elementCells;
    private final List<String> palette = new ArrayList<>();
    private final Map<String, Integer> index = new HashMap<>();
    private final List<Element> elements = new ArrayList<>();
    private final List<Zone> zones = new ArrayList<>();
    private final List<Spot> spots = new ArrayList<>();
    private final List<Placed> extras = new ArrayList<>();
    private final List<Placed> seal = new ArrayList<>();
    private int outOfBounds, blocks;
    private Element open;

    /** The air block: written to clear a cell (a plan that must empty the old hall says so with this). */
    public static final String AIR = "minecraft:air";

    public BossArenaStructure(String theme, int size, int height) {
        if (size < MIN_SIZE || size > MAX_SIZE || (size & 1) == 0) throw new IllegalArgumentException("arena size must be odd and within " + MIN_SIZE + ".." + MAX_SIZE + ", not " + size);
        if (height < 16 || height > 40) throw new IllegalArgumentException("arena height must be 16..40, not " + height);
        this.theme = theme;
        this.size = size;
        this.height = height;
        this.c = size / 2;
        this.cells = new short[size * size * (height + FOUNDATION)];
        this.reserved = new BitSet(cells.length);
        this.elementCells = new BitSet(cells.length);
        palette.add("");                         // index 0: untouched
        palette.add(AIR);                        // index 1: air
        index.put(AIR, 1);
    }

    // ================================================================ cells
    private int at(int x, int y, int z) { return ((y + FOUNDATION) * size + z) * size + x; }

    public boolean inside(int x, int y, int z) { return x >= 0 && x < size && z >= 0 && z < size && y >= -FOUNDATION && y < height; }

    /** Writes a block. Out-of-bounds writes are counted (a test asserts there are none) and ignored; a reserved cell is silently kept. Returns whether it was written. */
    public boolean set(int x, int y, int z, String spec) {
        if (!inside(x, y, z)) {
            outOfBounds++;
            return false;
        }
        int i = at(x, y, z);
        if (reserved.get(i)) return false;
        Integer p = index.get(spec);
        if (p == null) {
            if (palette.size() >= Short.MAX_VALUE) throw new IllegalStateException("palette overflow");
            p = palette.size();
            palette.add(spec);
            index.put(spec, p);
        }
        if (cells[i] == 0 && p != 0) blocks++;
        cells[i] = (short) (int) p;
        if (open != null && p != 1) {
            open.cells.add(new Placed(x, y, z, spec));
            elementCells.set(i);
        }
        return true;
    }

    public boolean air(int x, int y, int z) { return set(x, y, z, AIR); }

    /** The block text at a cell, or null when the plan leaves it alone (also outside the plan). */
    public String get(int x, int y, int z) {
        if (!inside(x, y, z)) return null;
        short p = cells[at(x, y, z)];
        return p == 0 ? null : palette.get(p);
    }

    /** True when the plan writes something other than air here. */
    public boolean solidAt(int x, int y, int z) {
        String s = get(x, y, z);
        return s != null && Solidity.solid(s);
    }

    public boolean writes(int x, int y, int z) { return inside(x, y, z) && cells[at(x, y, z)] != 0; }

    /** Marks a cell as owned by the floor's own template: the plan never writes it. Must be called before the cell is written. */
    public void reserve(int x, int y, int z) {
        if (inside(x, y, z)) reserved.set(at(x, y, z));
    }

    public boolean isReserved(int x, int y, int z) { return inside(x, y, z) && reserved.get(at(x, y, z)); }

    public int outOfBounds() { return outOfBounds; }

    /** How many cells the plan writes (air included). */
    public int writtenCells() {
        int n = 0;
        for (short s : cells) if (s != 0) n++;
        return n;
    }

    /** How many cells the plan fills with something other than air. */
    public int solidCells() {
        int n = 0;
        for (short s : cells) if (s > 1 && Solidity.solid(palette.get(s))) n++;
        return n;
    }

    public List<String> palette() { return Collections.unmodifiableList(palette); }

    /** The palette index of every written cell in the order y, z, x (0 = untouched): what the previews and the placer walk. */
    public short[] raw() { return cells; }

    public int rawIndex(int x, int y, int z) { return at(x, y, z); }

    // ================================================================ elements, zones, spots
    /** Starts an element: every block written until {@link #endElement} (air excluded) becomes one of its cells. */
    public Element beginElement(Kind kind, Effect effect, int adds) {
        if (open != null) throw new IllegalStateException("an element is already open");
        open = new Element(elements.size(), kind, effect, adds);
        return open;
    }

    public Element endElement() {
        Element e = open;
        if (e == null) throw new IllegalStateException("no element is open");
        open = null;
        if (e.cells.isEmpty()) return e;                       // an element whose every cell was refused (reserved) is dropped
        elements.add(e);
        return e;
    }

    public List<Element> elements() { return Collections.unmodifiableList(elements); }

    public List<Element> elements(Kind kind) {
        List<Element> out = new ArrayList<>();
        for (Element e : elements) if (e.kind == kind) out.add(e);
        return out;
    }

    /** Does an element own this cell (O(1))? */
    public boolean isElementCell(int x, int y, int z) { return inside(x, y, z) && elementCells.get(at(x, y, z)); }

    /** The element that owns the cell, or null. */
    public Element elementAt(int x, int y, int z) {
        for (Element e : elements) if (e.contains(x, y, z)) return e;
        return null;
    }

    public void zone(String kind, int x, int z, int r) { zones.add(new Zone(kind, x, z, r)); }

    public List<Zone> zones() { return Collections.unmodifiableList(zones); }

    public List<Zone> zones(String kind) {
        List<Zone> out = new ArrayList<>();
        for (Zone z : zones) if (z.kind().equals(kind)) out.add(z);
        return out;
    }

    public void spot(int x, int z) { spots.add(new Spot(x, z)); }

    public List<Spot> spots() { return Collections.unmodifiableList(spots); }

    // ================================================================ doors, seal and standalone extras
    /** Extras placed only when the hall stands alone (the admin's arena): the Gatekeeper slab, the open entry, the altar and its pedestal. */
    public void extra(int x, int y, int z, String spec) { extras.add(new Placed(x, y, z, spec)); }

    public List<Placed> extras() { return Collections.unmodifiableList(extras); }

    /** A cell of the entry seal (closed with iron bars while the fight lasts). */
    public void sealCell(int x, int y, int z, String spec) { seal.add(new Placed(x, y, z, spec)); }

    public List<Placed> sealCells() { return Collections.unmodifiableList(seal); }

    /** The column a party arrives on (inside the south door) and the column in front of the Gatekeeper. */
    public int[] entry() { return new int[] {c, size - 3}; }

    public int[] gate() { return new int[] {c, 2}; }

    /** The column the boss stands on: the middle of the hall. */
    public int[] bossSpot() { return new int[] {c, c}; }

    /** Is this cell part of a doorway the floor's template keeps (the 3 x 4 x 2 holes of the entry and the Gatekeeper)? */
    public static boolean inDoorway(int x, int y, int z, int size) {
        int c = size / 2;
        return Math.abs(x - c) <= 1 && y >= 1 && y <= 4 && (z <= 1 || z >= size - 2);
    }

    // ================================================================ rotation (for a floor whose Gatekeeper faces another way)
    /**
     * Turns a local cell {@code quarterTurns} quarter turns clockwise (seen from above, north to east) about the middle of the hall; {@code out[0], out[1]} are the new x and z.
     */
    public static void rotate(int x, int z, int size, int quarterTurns, int[] out) {
        int c = size / 2, dx = x - c, dz = z - c;
        switch (((quarterTurns % 4) + 4) % 4) {
            case 1 -> { int t = dx; dx = -dz; dz = t; }
            case 2 -> { dx = -dx; dz = -dz; }
            case 3 -> { int t = dx; dx = dz; dz = -t; }
            default -> { }
        }
        out[0] = c + dx;
        out[1] = c + dz;
    }

    public int blockCount() { return blocks; }
}
