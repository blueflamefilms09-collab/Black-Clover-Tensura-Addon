package com.newuniverse.nusmp.labyrinth.mega.poise;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the warning must show EXACTLY the ground the attack will hit (a party that dodges what the floor shows must not be hit anyway, and one that stands outside it must not be), so the
 * outline the particles trace and the "is this point hit" test are the same arithmetic in one pure class. Points are in a LOCAL frame: {@code f} is the distance along the attack's
 * direction, {@code s} the distance to its left; the world side rotates them by the attack's yaw. The hit test of a cone is the one {@code FoundryAbilityRunner.cleave} uses (the angle
 * between the direction and the target, on the horizontal plane), so the cleave and its cone agree.
 */
public final class TelegraphGeometry {
    private TelegraphGeometry() {}

    /** Hard cap on the points of one outline, whatever the data says (the particle budget is shared by every telegraph in the room). */
    public static final int MAX_POINTS = 48;

    /** The points of the outline of a shape, as {f, s} pairs. {@code points} is the wished density (clamped to 6..48); a cone of 360 degrees or more is a circle. */
    public static List<double[]> outline(TelegraphShape shape, double radius, double angleDeg, double length, double halfWidth, int points) {
        List<double[]> out = new ArrayList<>();
        if (shape == null || shape == TelegraphShape.NONE) return out;
        int n = Math.max(6, Math.min(MAX_POINTS, points));
        double r = Math.max(0.5, radius);
        switch (shape) {
            case CIRCLE -> circle(out, r, n);
            case RING -> {
                circle(out, r, Math.max(6, n * 2 / 3));
                circle(out, r * 0.55, Math.max(6, n / 3));
            }
            case CONE -> {
                if (angleDeg >= 359.0) {
                    circle(out, r, n);
                    break;
                }
                double half = Math.toRadians(Math.max(10.0, Math.min(358.0, angleDeg)) / 2.0);
                int arc = Math.max(4, n / 2), edge = Math.max(2, (n - arc) / 2);
                for (int i = 0; i <= arc; i++) {
                    double a = -half + 2.0 * half * i / arc;
                    out.add(new double[] {Math.cos(a) * r, Math.sin(a) * r});
                }
                for (int i = 1; i <= edge; i++) {
                    double d = r * i / (edge + 1.0);
                    out.add(new double[] {Math.cos(half) * d, Math.sin(half) * d});
                    out.add(new double[] {Math.cos(-half) * d, Math.sin(-half) * d});
                }
            }
            case LINE -> {
                double len = Math.max(1.0, length), hw = Math.max(0.3, halfWidth);
                int along = Math.max(3, n / 2);
                for (int i = 0; i <= along; i++) {
                    double f = len * i / along;
                    out.add(new double[] {f, hw});
                    out.add(new double[] {f, -hw});
                }
                out.add(new double[] {len, 0.0});
            }
            default -> { }
        }
        return out;
    }

    private static void circle(List<double[]> out, double r, int n) {
        for (int i = 0; i < n; i++) {
            double a = i * (Math.PI * 2.0 / n);
            out.add(new double[] {Math.cos(a) * r, Math.sin(a) * r});
        }
    }

    /** Is the local point {@code (f, s)} on the ground the attack hits? */
    public static boolean inside(TelegraphShape shape, double f, double s, double radius, double angleDeg, double length, double halfWidth) {
        if (shape == null || shape == TelegraphShape.NONE) return false;
        switch (shape) {
            case CIRCLE, RING:
                return f * f + s * s <= radius * radius;
            case CONE: {
                double d2 = f * f + s * s;
                if (d2 > radius * radius) return false;
                if (angleDeg >= 359.0) return true;
                if (d2 < 1.0E-9) return true;
                double cos = f / Math.sqrt(d2);
                return Math.acos(Math.max(-1.0, Math.min(1.0, cos))) <= Math.toRadians(angleDeg / 2.0);
            }
            case LINE:
                return f >= 0.0 && f <= length && Math.abs(s) <= halfWidth;
            default:
                return false;
        }
    }

    /** The world position of a local point for an attack at {@code (ox, oz)} facing {@code yawRad} (0 = +x, counter-clockwise): {x, z}. */
    public static double[] toWorld(double ox, double oz, double yawRad, double f, double s) {
        double c = Math.cos(yawRad), sn = Math.sin(yawRad);
        return new double[] {ox + f * c - s * sn, oz + f * sn + s * c};
    }
}
