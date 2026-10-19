package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.PuzzleBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the world half of a {@link Layout}: where each cell of a puzzle stands in a real room. {@link PuzzleGeometry} says where the cells stand relative to each other; this class puts that
 * arrangement round an anchor and checks it against the real blocks: the floor under every cell must be a solid, flat, block-entity-free floor of the same height as the others, there must be air
 * (and headroom) where a tile's visitors stand and where a pillar's column rises, no creature may stand in a pillar's column, and everything must lie inside the room (two blocks clear of its
 * walls). If the anchor does not fit, a few other anchors are tried (and the other stacking direction); if none does, there is no stage and the puzzle is simply not armed in that room - a puzzle
 * that cannot be placed never blocks a room. Nothing is placed here: the stage only decides; the instance draws through a journal. Pillars stand two blocks apart, so there is always a
 * block-wide gap to walk through, and tiles are floor: a stage never walls a room in.
 */
final class PuzzleStage {
    /** One cell: its role, the position of its first block (a tile: the floor block; a column: the lowest block, above the floor) and its height. */
    record Cell(int index, Layout.Role role, BlockPos base, int height, boolean tile, String palette) {}

    final List<Cell> cells = new ArrayList<>();
    private final Map<Long, Integer> byPos = new HashMap<>();
    private final Map<Long, Integer> tileByXZ = new HashMap<>();
    BlockPos lectern;
    int floorY;
    int minX, maxX, minZ, maxZ;

    int cellAt(BlockPos p) {
        Integer i = byPos.get(p.asLong());
        return i == null ? -1 : i;
    }

    /** The tile under a point, or -1. */
    int tileAt(int x, int z) {
        Integer i = tileByXZ.get(((long) x << 32) ^ (z & 0xFFFFFFFFL));
        return i == null ? -1 : i;
    }

    /** The position a delver stands in to use the cell (a tile: the cell above it). */
    BlockPos standing(int cell) {
        Cell c = cells.get(cell);
        return c.tile() ? c.base().above() : c.base();
    }

    private void add(Cell c) {
        cells.add(c);
        if (c.tile()) {
            byPos.put(c.base().asLong(), c.index());
            tileByXZ.put(((long) c.base().getX() << 32) ^ (c.base().getZ() & 0xFFFFFFFFL), c.index());
        } else {
            for (int y = 0; y < c.height(); y++) byPos.put(c.base().above(y).asLong(), c.index());
        }
    }

    // ---------------------------------------------------------------- the check of the real blocks
    private static boolean valid(ServerLevel level, int x, int fy, int z, int h, boolean tile) {
        BlockPos floor = new BlockPos(x, fy, z);
        if (!level.hasChunksAt(floor.offset(-1, -2, -1), floor.offset(1, h + 3, 1))) return false;
        BlockState fs = level.getBlockState(floor);
        if (fs.isAir() || fs.getCollisionShape(level, floor).isEmpty() || !level.getFluidState(floor).isEmpty()) return false;
        if (level.getBlockEntity(floor) != null || fs.getDestroySpeed(level, floor) < 0.0f) return false;
        BlockPos below = floor.below();
        if (level.getBlockState(below).getCollisionShape(level, below).isEmpty()) return false;
        if (tile) {
            for (int y = fy + 1; y <= fy + 2; y++) {
                BlockPos q = new BlockPos(x, y, z);
                if (!level.getBlockState(q).getCollisionShape(level, q).isEmpty() || !level.getFluidState(q).isEmpty()) return false;
            }
            return true;
        }
        for (int y = fy + 1; y <= fy + h; y++) {
            BlockPos q = new BlockPos(x, y, z);
            if (!level.getBlockState(q).isAir() || PuzzleBridge.occupied(level, q)) return false;
        }
        BlockPos top = new BlockPos(x, fy + h + 1, z);
        return level.getBlockState(top).getCollisionShape(level, top).isEmpty();
    }

    private static boolean standable(ServerLevel level, int x, int fy, int z) {
        BlockPos floor = new BlockPos(x, fy, z);
        if (!level.hasChunkAt(floor)) return false;
        BlockState fs = level.getBlockState(floor);
        if (fs.getCollisionShape(level, floor).isEmpty() || !level.getFluidState(floor).isEmpty()) return false;
        for (int y = fy + 1; y <= fy + 2; y++) {
            BlockPos q = new BlockPos(x, y, z);
            if (!level.getBlockState(q).isAir()) return false;
        }
        return true;
    }

    // ---------------------------------------------------------------- planning
    /**
     * Finds a place for every cell of the rule's layout round {@code anchor} (a standing cell) inside {@code room} (null for an admin zone), or null when there is none. {@code height(cell)} of
     * the rule gives each column's height; {@code seed} fixes the scatter positions.
     */
    static PuzzleStage plan(ServerLevel level, PuzzleRule rule, BlockPos anchor, Room room, long seed) {
        Layout lay = rule.layout();
        int fy = anchor.getY() - 1;
        for (boolean alongX : new boolean[] {false, true}) {
            PuzzleGeometry.Arrangement arr = PuzzleGeometry.arrange(lay, seed, alongX);
            for (int[] sh : shifts()) {
                PuzzleStage st = tryPlace(level, rule, lay, arr, anchor.getX() + sh[0], anchor.getZ() + sh[1], fy, room);
                if (st != null) return st;
            }
        }
        return null;
    }

    private static int[][] shifts() {
        List<int[]> out = new ArrayList<>();
        out.add(new int[] {0, 0});
        for (int r = 3; r <= 12; r += 3) {
            out.add(new int[] {r, 0});
            out.add(new int[] {-r, 0});
            out.add(new int[] {0, r});
            out.add(new int[] {0, -r});
            out.add(new int[] {r, r});
            out.add(new int[] {-r, -r});
            out.add(new int[] {r, -r});
            out.add(new int[] {-r, r});
        }
        return out.toArray(new int[0][]);
    }

    private static PuzzleStage tryPlace(ServerLevel level, PuzzleRule rule, Layout lay, PuzzleGeometry.Arrangement arr, int ax, int az, int fy, Room room) {
        int left = ax - arr.w() / 2, top = az - arr.d() / 2;
        PuzzleStage st = new PuzzleStage();
        st.floorY = fy;
        st.minX = Integer.MAX_VALUE;
        st.minZ = Integer.MAX_VALUE;
        st.maxX = Integer.MIN_VALUE;
        st.maxZ = Integer.MIN_VALUE;
        Set<Long> used = new HashSet<>();
        for (int gi = 0; gi < lay.groups().size(); gi++) {
            Layout.Group g = lay.groups().get(gi);
            int first = lay.start(gi);
            for (int i = 0; i < g.count(); i++) {
                int cell = first + i;
                int x = left + arr.x()[cell], z = top + arr.z()[cell];
                int h = Math.max(1, rule.height(cell));
                boolean tile = g.role() == Layout.Role.PLATE || (g.role() == Layout.Role.DISPLAY && h == 1);
                if (!used.add(((long) x << 32) ^ (z & 0xFFFFFFFFL))) return null;
                if (room != null && (x < room.box.minX() + 2 || x > room.box.maxX() - 2 || z < room.box.minZ() + 2 || z > room.box.maxZ() - 2)) return null;
                st.minX = Math.min(st.minX, x);
                st.maxX = Math.max(st.maxX, x);
                st.minZ = Math.min(st.minZ, z);
                st.maxZ = Math.max(st.maxZ, z);
                if (!valid(level, x, fy, z, h, tile)) return null;
                st.add(new Cell(cell, g.role(), tile ? new BlockPos(x, fy, z) : new BlockPos(x, fy + 1, z), h, tile, g.palette()));
            }
        }
        st.lectern = lectern(level, st, fy, room);
        return st;
    }

    private static BlockPos lectern(ServerLevel level, PuzzleStage st, int fy, Room room) {
        int cx = (st.minX + st.maxX) / 2, cz = (st.minZ + st.maxZ) / 2;
        int[][] spots = {{st.minX - 2, cz}, {st.maxX + 2, cz}, {cx, st.minZ - 2}, {cx, st.maxZ + 2}, {st.minX - 2, st.minZ - 2}, {st.maxX + 2, st.maxZ + 2}, {st.minX - 2, st.maxZ + 2}, {st.maxX + 2, st.minZ - 2}};
        for (int[] s : spots) {
            if (room != null && (s[0] < room.box.minX() + 1 || s[0] > room.box.maxX() - 1 || s[1] < room.box.minZ() + 1 || s[1] > room.box.maxZ() - 1)) continue;
            if (standable(level, s[0], fy, s[1])) return new BlockPos(s[0], fy + 1, s[1]);
        }
        return null;
    }
}
