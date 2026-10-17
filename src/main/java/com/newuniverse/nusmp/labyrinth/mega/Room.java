package com.newuniverse.nusmp.labyrinth.mega;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Why: one placed piece of a sector (a room, a corridor, the Landing ...) as the rest of the mod sees it: which template it is, what
 * {@link RoomKind} that makes it, and where it stands. The encounter director picks spawn rooms from these, a wandering boss walks the
 * graph of them, the factions give each a native owner, the rest system asks whether a position is in a SAFE one. Door caps are never
 * stored (they are decoration of a socket, not places). The box is the piece's bounding box, wall layers included. The floor is the same
 * world row in every room ({@link MegaGrid#FLOOR_Y}), so a standing position needs no per-room height.
 */
public final class Room {
    public final int id;
    public final String template;
    public final RoomKind kind;
    public final BoundingBox box;
    /** The native faction (an id of {@code data/nusmp/mega/factions}); null until the ecology assigned one. Mutable on purpose: the eco builder sets it after generation. */
    public String faction;

    public Room(int id, String template, RoomKind kind, BoundingBox box) {
        this.id = id;
        this.template = template;
        this.kind = kind;
        this.box = box;
    }

    /** The standing cell in the middle of the room, one above the floor. */
    public BlockPos center() {
        return new BlockPos((box.minX() + box.maxX()) / 2, MegaGrid.FLOOR_Y + 1, (box.minZ() + box.maxZ()) / 2);
    }

    /** Is the position inside the box (all three axes)? */
    public boolean contains(BlockPos p) { return box.isInside(p); }

    /** Horizontal containment only (a position above a dome or below a pit is still "in" the room). */
    public boolean containsXZ(int x, int z) { return x >= box.minX() && x <= box.maxX() && z >= box.minZ() && z <= box.maxZ(); }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("tpl", template);
        t.putIntArray("box", new int[] {box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()});
        if (faction != null) t.putString("fac", faction);
        return t;
    }

    public static Room load(CompoundTag t) {
        int[] b = t.getIntArray("box");
        if (b.length != 6) b = new int[] {0, 0, 0, 0, 0, 0};
        Room r = new Room(t.getInt("id"), t.getString("tpl"), RoomKind.of(t.getString("tpl")), new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]));
        if (t.contains("fac")) r.faction = t.getString("fac");
        return r;
    }
}
