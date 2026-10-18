package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

/**
 * Why: a mechanic acts on "its area", and the three scopes mean three shapes: a FLOOR is the whole territory of a sector, a ROOM is the box of one placed piece, a ZONE is a disc
 * of a given radius round the mechanic's anchor (kept inside the room it stands in). One small class answers "is this position or entity in the area" and "what box is it" for
 * the triggers, the target selection of the primitives and the cleanup. Vertically a room spans from the pits under the floor to its dome; a zone is a flat cylinder round the
 * floor. A test or an admin command can make a zone anywhere.
 */
public final class Area {
    public enum Kind { FLOOR, ROOM, ZONE }

    public final Kind kind;
    /** The horizontal extent of the area (a room's box, a sector's region, or the disc's bounding square). */
    public final BoundingBox box;
    public final int cx, cz, radius;

    private Area(Kind kind, BoundingBox box, int cx, int cz, int radius) {
        this.kind = kind;
        this.box = box;
        this.cx = cx;
        this.cz = cz;
        this.radius = radius;
    }

    public static Area floor(BoundingBox region) {
        return new Area(Kind.FLOOR, region, (region.minX() + region.maxX()) / 2, (region.minZ() + region.maxZ()) / 2, Math.max(region.getXSpan(), region.getZSpan()) / 2);
    }

    public static Area room(BoundingBox roomBox) {
        return new Area(Kind.ROOM, roomBox, (roomBox.minX() + roomBox.maxX()) / 2, (roomBox.minZ() + roomBox.maxZ()) / 2, Math.max(roomBox.getXSpan(), roomBox.getZSpan()) / 2);
    }

    /** A disc of {@code radius} blocks round the position, clipped to {@code within} (a room's box) when it is not null. */
    public static Area zone(BlockPos centre, int radius, BoundingBox within) {
        BoundingBox b = new BoundingBox(centre.getX() - radius, MegaGrid.FLOOR_Y - 8, centre.getZ() - radius, centre.getX() + radius, MegaGrid.FLOOR_Y + 12, centre.getZ() + radius);
        return new Area(Kind.ZONE, b, centre.getX(), centre.getZ(), radius);
    }

    /** Is the point inside? A zone is round and short; a room spans every height of the piece; a floor ignores height. */
    public boolean contains(double x, double y, double z) {
        switch (kind) {
            case ZONE -> {
                double dx = x - (cx + 0.5), dz = z - (cz + 0.5);
                return dx * dx + dz * dz <= (radius + 0.5) * (radius + 0.5) && y >= MegaGrid.FLOOR_Y - 8 && y <= MegaGrid.FLOOR_Y + 14;
            }
            case ROOM -> {
                return x >= box.minX() && x < box.maxX() + 1 && z >= box.minZ() && z < box.maxZ() + 1 && y >= MegaGrid.FLOOR_Y - 9 && y <= box.maxY() + 2;
            }
            default -> {
                return x >= box.minX() && x < box.maxX() + 1 && z >= box.minZ() && z < box.maxZ() + 1;
            }
        }
    }

    public boolean contains(Entity e) { return contains(e.getX(), e.getY(), e.getZ()); }

    public boolean contains(BlockPos p) { return contains(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5); }

    /** The area's box as a world AABB (what the entity queries use). */
    public AABB aabb() {
        return new AABB(box.minX(), MegaGrid.FLOOR_Y - 9, box.minZ(), box.maxX() + 1, Math.max(box.maxY(), MegaGrid.FLOOR_Y + 14) + 1, box.maxZ() + 1);
    }

    /** The same box grown by {@code g} blocks horizontally (used so a player standing in a doorway does not make a room mechanic flicker on and off). */
    public boolean containsWithMargin(double x, double y, double z, int g) {
        if (contains(x, y, z)) return true;
        return x >= box.minX() - g && x < box.maxX() + 1 + g && z >= box.minZ() - g && z < box.maxZ() + 1 + g && kind != Kind.ZONE && y >= MegaGrid.FLOOR_Y - 9 && y <= box.maxY() + 2;
    }
}
