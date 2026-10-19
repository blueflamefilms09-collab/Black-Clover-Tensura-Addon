package com.newuniverse.nusmp.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the one place where a trap's lane frame (x across to the right, y up, z along the lane) meets the world. A frame is the world cell of local (0, 0, 0) (the lane's centre
 * line at the floor row, at the start of the strip) and the direction the lane runs; everything else (a part's cell and facing, a band's box, which local cell a player stands
 * in) is computed from those two, the same way for all four directions, using the integer maths of {@link LaneFinder#toWorld} so the tests that pin the turning also pin this.
 */
public record Frame(BlockPos origin, Direction forward) {
    public Frame {
        if (forward == null || !forward.getAxis().isHorizontal()) forward = Direction.SOUTH;
    }

    /** The forward index of {@link LaneFinder}: 0 north, 1 east, 2 south, 3 west. */
    public static int index(Direction d) {
        return switch (d) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            default -> 3;
        };
    }

    public static Direction direction(int index) {
        return switch (Math.floorMod(index, 4)) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }

    /** The world cell of a local cell. */
    public BlockPos pos(int dx, int dy, int dz) {
        int[] xz = LaneFinder.toWorld(origin.getX(), origin.getZ(), index(forward), dx, dz);
        return new BlockPos(xz[0], origin.getY() + dy, xz[1]);
    }

    /** The world direction of a local direction. */
    public Direction dir(Dir6 d) {
        return switch (d) {
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
            case FWD -> forward;
            case BACK -> forward.getOpposite();
            case RIGHT -> forward.getClockWise();
            case LEFT -> forward.getCounterClockWise();
        };
    }

    /** The world axis letter ('x', 'y', 'z') of a local direction. */
    public char axis(Dir6 d) {
        Direction w = dir(d);
        return w.getAxis() == Direction.Axis.X ? 'x' : w.getAxis() == Direction.Axis.Y ? 'y' : 'z';
    }

    /** The world box of the local box (continuous: cell {@code x} spans {@code x - 0.5 .. x + 0.5} across the lane, and the same along it; y runs from {@code yMin} to {@code yMax + 1} above the floor row). */
    public AABB box(double xMin, double xMax, int yMin, int yMax, double zMin, double zMax) {
        Vec3 a = point(xMin - 0.5, zMin - 0.5), b = point(xMax + 0.5, zMax + 0.5);
        return new AABB(Math.min(a.x, b.x), origin.getY() + yMin, Math.min(a.z, b.z), Math.max(a.x, b.x), origin.getY() + yMax + 1, Math.max(a.z, b.z));
    }

    /** The world point (x and z; y is 0) of a continuous local point across / along the lane, measured from the centre of the origin cell. */
    public Vec3 point(double lx, double lz) {
        int fx = LaneFinder.fx(index(forward)), fz = LaneFinder.fz(index(forward));
        int rx = -fz, rz = fx;
        return new Vec3(origin.getX() + 0.5 + rx * lx + fx * lz, 0.0, origin.getZ() + 0.5 + rz * lx + fz * lz);
    }

    /** The local coordinates (across, up from the floor row, along) of a world point; across and along are continuous, measured from the centre of the origin cell. */
    public double[] local(Vec3 w) {
        int fx = LaneFinder.fx(index(forward)), fz = LaneFinder.fz(index(forward));
        int rx = -fz, rz = fx;
        double dx = w.x - (origin.getX() + 0.5), dz = w.z - (origin.getZ() + 0.5);
        return new double[] {dx * rx + dz * rz, w.y - origin.getY(), dx * fx + dz * fz};
    }

    /** The local cell (across, up, along) a world point is in. */
    public int[] cell(Vec3 w) {
        double[] l = local(w);
        return new int[] {(int) Math.round(l[0]), (int) Math.floor(l[1]), (int) Math.round(l[2])};
    }

    /** The unit vector of travel in the world. */
    public Vec3 forwardVec() { return new Vec3(forward.getStepX(), 0, forward.getStepZ()); }
}
