package net.minecraft.core;

import org.joml.Vector3f;

/** Preview stub of Minecraft's Direction: the six faces with their unit normals (the player model's polygons carry one each). */
public enum Direction {
    DOWN(0, -1, 0),
    UP(0, 1, 0),
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    EAST(1, 0, 0);

    private final int stepX, stepY, stepZ;

    Direction(int x, int y, int z) {
        this.stepX = x;
        this.stepY = y;
        this.stepZ = z;
    }

    public int getStepX() { return stepX; }
    public int getStepY() { return stepY; }
    public int getStepZ() { return stepZ; }
    public Vector3f step() { return new Vector3f((float) stepX, (float) stepY, (float) stepZ); }
}
