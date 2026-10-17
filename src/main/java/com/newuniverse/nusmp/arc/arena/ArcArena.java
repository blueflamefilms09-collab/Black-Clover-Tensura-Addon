package com.newuniverse.nusmp.arc.arena;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the story arenas live in one dimension of their own, {@code nusmp:arc_arena} (the pattern of the Final Hour's {@code nusmp:zogratis_arena}), and the
 * builder, the director, the travel, the events, the dungeon guard and the commands all compare against the same key and convert between a SLOT of the grid
 * and a world position the same way. This class is that one place: the dimension key, {@code in(Level)} and {@code in(Entity)} (null safe, both sides), the
 * world positions an arena's formula maps to (the floor centre, the boss's footing, the travellers' footing), and the operator's edit switch that lets the
 * dungeon guard stand aside so a creative operator can change an arena by hand (it is off at every start and is not saved).
 */
public final class ArcArena {
    private ArcArena() {}

    /** The dimension of the story arenas (data/nusmp/dimension/arc_arena.json). */
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "arc_arena"));

    /** The persistent-data key (an int) that marks a boss as the one fighting in a slot's arena; the value is the slot. */
    public static final String SLOT_KEY = "nusmp_arc_arena_slot";

    private static volatile boolean editing;

    /** Is this world the story arenas' dimension? Null-safe, both sides (the key is compared, not the level instance). */
    public static boolean in(Level level) { return level != null && level.dimension() == DIMENSION; }

    public static boolean in(Entity e) { return e != null && in(e.level()); }

    /** Is an operator editing the arenas now? While true the dungeon guard lets blocks change in this dimension. Not saved: every start begins closed. */
    public static boolean editing() { return editing; }

    public static void setEditing(boolean on) { editing = on; }

    /** The block in the middle of a slot's floor. */
    public static BlockPos floorCentre(int slot) { return new BlockPos(ArenaGrid.centreX(slot), ArenaShape.FLOOR_Y, ArenaGrid.centreZ(slot)); }

    /** World x of an arena column (a column is an offset {@code dx} from the slot's centre). */
    public static int worldX(int slot, int dx) { return ArenaGrid.centreX(slot) + dx; }

    public static int worldZ(int slot, int dz) { return ArenaGrid.centreZ(slot) + dz; }

    /** World y of the cell a creature stands in on this column: one above the surface (the floor is 0 when a column has no ground). */
    public static int standY(ArenaShape shape, int dx, int dz) {
        int top = shape.surface(dx, dz);
        return ArenaShape.FLOOR_Y + (top == Integer.MIN_VALUE ? 0 : top) + 1;
    }

    /** The point the boss's feet are set on. */
    public static Vec3 bossStand(ArenaShape shape, int slot) {
        int[] b = shape.bossSpot();
        return new Vec3(worldX(slot, b[0]) + 0.5, standY(shape, b[0], b[1]), worldZ(slot, b[1]) + 0.5);
    }

    /** The point a traveller's feet are set on for an arrival column. */
    public static Vec3 arrivalStand(ArenaShape shape, int slot, int[] column) {
        return new Vec3(worldX(slot, column[0]) + 0.5, standY(shape, column[0], column[1]), worldZ(slot, column[1]) + 0.5);
    }

    /** The yaw (degrees, game convention) that turns a traveller on a column towards the boss. */
    public static float yawTowardsBoss(ArenaShape shape, int[] column) {
        int[] b = shape.bossSpot();
        double dx = b[0] - column[0], dz = b[1] - column[1];
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }
}
