package com.newuniverse.nusmp.arc.sds;

/**
 * Why: two of Diane's moves are waves that travel: the rings of Drole's Dance leave her feet and race out (a low ring, so a jump at the right moment clears it), and Queen
 * Embrace is a wall of earth that runs down a lane. "Did the wave touch that body" is the question for every foe on every tick of both, and the answer has to be the same for
 * a body next to her feet and one at the far edge, for a fast wave and a slow one, and for a body the front steps over in a single tick. This pure class answers it from
 * numbers (the front before and after the tick, the body's distance and half-width), so the rule is tested and the glue only supplies the numbers.
 */
public final class WaveMath {
    private WaveMath() {}

    /** The radius of a ring that started at tick 0 and moves {@code speed} blocks a tick, at {@code age} ticks. */
    public static double front(int age, double speed) { return Math.max(0, age) * speed; }

    /**
     * Does the front, moving from {@code before} to {@code after} this tick, touch a body whose centre is {@code distance} from the origin and whose half-width is
     * {@code halfWidth}? True on the one tick the front crosses the body (and on the first tick for a body the front starts inside).
     */
    public static boolean touches(double distance, double halfWidth, double before, double after) {
        return before <= distance + halfWidth && after >= distance - halfWidth;
    }

    /** Is the body clear of a low ring: its feet at least {@code ringHeight} above the floor? */
    public static boolean clears(double heightAboveFloor, double ringHeight) { return heightAboveFloor >= ringHeight; }

    /** The ticks a ring takes to reach {@code radius}. */
    public static int ticksToReach(double radius, double speed) { return speed > 0 ? (int) Math.ceil(radius / speed) : Integer.MAX_VALUE; }

    /**
     * Does a wall of earth running down a lane touch a body? {@code along} is how far down the lane the body is, {@code across} how far to the side of its axis; the wall
     * is {@code laneHalfWidth} either side and its front moves from {@code before} to {@code after} this tick.
     */
    public static boolean laneTouches(double along, double across, double halfWidth, double laneHalfWidth, double before, double after) {
        return Math.abs(across) <= laneHalfWidth + halfWidth && touches(along, halfWidth, before, after) && along >= -halfWidth;
    }
}
