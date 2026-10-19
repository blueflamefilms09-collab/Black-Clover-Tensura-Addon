package com.newuniverse.nusmp.compat.create;

/**
 * Why: a trap is drawn once, in the frame of the lane it guards, and the world turns it. In that frame {@link #FWD} is the way the lane runs, {@link #RIGHT} is the
 * clockwise neighbour of it and {@link #UP} is up; the six directions are the only thing a part's facing or axis can be. The Minecraft layer maps them to a
 * {@code Direction} with the lane's forward (the only place a world direction appears), so the same drawing serves all four ways a corridor can run and both ways
 * the trap can be entered from. Pure (no Minecraft types).
 */
public enum Dir6 {
    UP, DOWN, FWD, BACK, RIGHT, LEFT;

    public Dir6 opposite() {
        return switch (this) {
            case UP -> DOWN;
            case DOWN -> UP;
            case FWD -> BACK;
            case BACK -> FWD;
            case RIGHT -> LEFT;
            case LEFT -> RIGHT;
        };
    }

    public boolean horizontal() { return this != UP && this != DOWN; }

    /** The local axis: 'x' for RIGHT / LEFT (across the lane), 'y' for UP / DOWN, 'z' for FWD / BACK (along the lane). */
    public char axis() {
        return switch (this) {
            case RIGHT, LEFT -> 'x';
            case UP, DOWN -> 'y';
            case FWD, BACK -> 'z';
        };
    }

    /** The unit step of this direction in the lane frame as {dx, dy, dz} (x across to the right, y up, z along the lane). */
    public int[] step() {
        return switch (this) {
            case UP -> new int[] {0, 1, 0};
            case DOWN -> new int[] {0, -1, 0};
            case FWD -> new int[] {0, 0, 1};
            case BACK -> new int[] {0, 0, -1};
            case RIGHT -> new int[] {1, 0, 0};
            case LEFT -> new int[] {-1, 0, 0};
        };
    }

    /**
     * The value of the Create state property {@code axis_along_first} that makes a block that faces along {@code facingAxis} rotate about
     * {@code rotationAxis}. Create's rule for such a block (the mechanical saw, the mechanical piston) as I remember it: facing X rotates about Y when the flag is set
     * and about Z when it is not; facing Y about X or Z; facing Z about X or Y. Axes are WORLD axes ('x', 'y', 'z'). Returns false when the pair is impossible (the
     * rotation axis equals the facing axis).
     */
    public static boolean alongFirst(char facingAxis, char rotationAxis) {
        return switch (facingAxis) {
            case 'x' -> rotationAxis == 'y';
            case 'y', 'z' -> rotationAxis == 'x';
            default -> false;
        };
    }
}
