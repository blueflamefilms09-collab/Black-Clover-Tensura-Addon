package com.newuniverse.nusmp.dungeon.nexus.vault;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntPredicate;

/**
 * A made-up world for the vault tests: solid stone everywhere except what a test opens, plus a record of what ops placed. It answers
 * {@link VaultTerrain} (so the planner can search it) and applies ops the way the game's sink does (CARVE to air, SOFT only where the cell is
 * not solid, BOX always).
 */
final class SimWorld implements VaultTerrain {
    interface Cell { int code(int x, int y, int z); }

    private final Map<Long, Integer> over = new HashMap<>();
    private final Map<Long, VaultOp.Pal> placed = new HashMap<>();
    private final Cell base;
    int reads;

    SimWorld(Cell base) { this.base = base; }

    /** Solid stone (deepslate below y=0) everywhere. */
    static SimWorld solid() { return new SimWorld((x, y, z) -> y < 0 ? DEEPSLATE : STONE); }

    static long key(int x, int y, int z) { return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF); }

    @Override
    public int at(int x, int y, int z) {
        reads++;
        Integer o = over.get(key(x, y, z));
        return o != null ? o : base.code(x, y, z);
    }

    void set(int x, int y, int z, int code) { over.put(key(x, y, z), code); }

    void fill(int x1, int y1, int z1, int x2, int y2, int z2, int code) {
        for (int x = x1; x <= x2; x++) for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) set(x, y, z, code);
    }

    VaultOp.Pal palAt(int x, int y, int z) { return placed.get(key(x, y, z)); }

    /** Applies one cell of an op; returns whether the world changed. */
    boolean place(VaultOp op, int x, int y, int z) {
        int cur = at(x, y, z);
        switch (op.kind()) {
            case CARVE -> {
                if (cur == AIR) return false;
                set(x, y, z, AIR);
                placed.remove(key(x, y, z));
                return true;
            }
            case SOFT -> {
                if (VaultTerrain.solid(cur)) return false;
                set(x, y, z, OTHER);
                placed.put(key(x, y, z), op.pal());
                return true;
            }
            default -> {
                if (placed.get(key(x, y, z)) == op.pal()) return false;
                set(x, y, z, op.pal() == VaultOp.Pal.AIR ? AIR : OTHER);
                placed.put(key(x, y, z), op.pal());
                return true;
            }
        }
    }

    void apply(VaultLayout layout) {
        VaultCursor c = new VaultCursor(layout, 0, 0);
        c.run((op, x, y, z) -> place(op, x, y, z), Long.MAX_VALUE);
    }

    int count(IntPredicate on, int x1, int y1, int z1, int x2, int y2, int z2) {
        int n = 0;
        for (int x = x1; x <= x2; x++) for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) if (on.test(at(x, y, z))) n++;
        return n;
    }
}
