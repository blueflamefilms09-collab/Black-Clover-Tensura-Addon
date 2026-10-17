package com.newuniverse.nusmp.arc.spade;

/**
 * Why: Morris's ultimate, the Dissection Theatre, is a cage of tendrils that closes round the party like a shrinking circle with doors. A closing circle
 * is only a fair mechanic if the doors really are there and really lead somewhere: this class decides the radius at every tick, which angles are doors,
 * how wide a door is, and who is touching the wall, as plain arithmetic, so the tests can pin that the cage always has doors, that the doors are spread
 * round it, that the wall is thin, and that a foe standing in a door is never touched. The game side only samples the ground beyond the ring (is there
 * somewhere to stand out there) and feeds it in, so a door never opens onto a wall of the arena when another direction is free.
 */
public final class CageMath {
    private CageMath() {}

    /** The ring starts this wide, ends this wide, and its wall is this thick (blocks). */
    public static final double START = 16, END = 5, WALL = 1.25;
    /** Half the angular width of a door, radians (20 degrees: a 40 degree door; 11 blocks wide at the start, 3.5 at the end). */
    public static final double DOOR_HALF = Math.toRadians(20);
    /** Doors in a cage. */
    public static final int DOORS = 3;
    /** How far (radians, 50 degrees) a door may be moved from its ideal angle to open onto standing room. */
    private static final double SNAP = Math.toRadians(50);

    /** The ring radius {@code age} ticks into a closing that lasts {@code closeTicks}: linear from {@link #START} to {@link #END}, then it stays. */
    public static double radius(int age, int closeTicks) {
        double t = Math.max(0, Math.min(1, age / (double) Math.max(1, closeTicks)));
        return START + (END - START) * t;
    }

    /** The angle (radians, 0..2pi) of the point (dx, dz) as seen from the cage centre. */
    public static double angle(double dx, double dz) {
        double a = Math.atan2(dz, dx);
        return a < 0 ? a + Math.PI * 2 : a;
    }

    /** The smallest difference between two angles, 0..pi. */
    public static double angularDistance(double a, double b) {
        double d = Math.abs(a - b) % (Math.PI * 2);
        return d > Math.PI ? Math.PI * 2 - d : d;
    }

    /**
     * Picks the door angles. {@code open[i]} says whether the direction {@code i * 2pi / open.length} has standing room beyond the ring. The doors are
     * spread evenly round the circle, starting at the first open direction (or at 0 when none is open); each ideal angle is moved to the nearest open
     * direction if one lies within 50 degrees of it, so a door opens onto somewhere to stand whenever that is possible without bunching the doors up.
     * The result always has {@code want} doors, however little of the arena is open. Deterministic.
     */
    public static double[] doors(boolean[] open, int want) {
        int n = open == null ? 0 : open.length;
        int k = Math.max(1, want);
        double base = 0;
        for (int i = 0; i < n; i++) if (open[i]) { base = i * Math.PI * 2 / n; break; }
        double[] out = new double[k];
        for (int j = 0; j < k; j++) {
            double ideal = (base + j * Math.PI * 2 / k) % (Math.PI * 2);
            double best = ideal, bestDist = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (!open[i]) continue;
                double a = i * Math.PI * 2 / n, d = angularDistance(a, ideal);
                if (d <= SNAP && d < bestDist) { best = a; bestDist = d; }
            }
            out[j] = best;
        }
        return out;
    }

    /** Is the angle inside any door? */
    public static boolean inDoor(double angle, double[] doors) {
        if (doors == null) return false;
        for (double d : doors) if (angularDistance(angle, d) <= DOOR_HALF) return true;
        return false;
    }

    /** Is a foe at {@code dist} and {@code angle} from the centre touching the wall of a ring of {@code radius}: on the wall and not in a door? */
    public static boolean touches(double dist, double angle, double radius, double[] doors) {
        return Math.abs(dist - radius) <= WALL && !inDoor(angle, doors);
    }

    /** Width in blocks of a door at the given ring radius. */
    public static double doorWidth(double radius) { return 2 * radius * Math.sin(DOOR_HALF); }
}
