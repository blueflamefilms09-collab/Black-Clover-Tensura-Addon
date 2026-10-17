package com.newuniverse.nusmp.arc.early;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Lotus's smoke gardens, the veil and the full bloom are all "a disc of smoke somewhere, for a while, that does something to whoever stands in it".
 * Keeping them as a list of live discs in one pure class means the glue asks one question ("how thick is the smoke where this foe stands?") and the rules
 * about them are tested once: at most {@code cap} discs exist at a time (the oldest is dropped, so the arena can never fill with smoke), a disc is gone the
 * tick its time is up, a disc is flat (a foe far above or below it is not in it), and where discs overlap the thickest one counts, never the sum (so
 * stacking gardens cannot be made into a faster poison than the veil).
 */
public final class CloudField {
    /** One disc of smoke: centre, radius, the tick it thins out at, and the dose it gives per second. Its depth is {@value #BELOW} blocks below the centre and {@value #ABOVE} above. */
    public record Cloud(double x, double y, double z, double radius, long until, int gain) {}

    public static final double BELOW = 1.5, ABOVE = 3.5;

    private final int cap;
    private final List<Cloud> clouds = new ArrayList<>();

    public CloudField(int cap) { this.cap = Math.max(1, cap); }

    /** Adds a disc; the ones already out of time go first, then the oldest if there are still too many. */
    public void add(Cloud c, long now) {
        clouds.removeIf(o -> now >= o.until());
        clouds.add(c);
        while (clouds.size() > cap) clouds.remove(0);
    }

    /** The dose per second at a point: the thickest disc that contains it, 0 when none does. */
    public int gainAt(double x, double y, double z, long now) {
        int best = 0;
        for (Cloud c : clouds) {
            if (now >= c.until() || c.gain() <= best) continue;
            if (Math.hypot(x - c.x(), z - c.z()) <= c.radius() && y >= c.y() - BELOW && y <= c.y() + ABOVE) best = c.gain();
        }
        return best;
    }

    /** The discs still standing at {@code now}. */
    public List<Cloud> live(long now) {
        List<Cloud> out = new ArrayList<>();
        for (Cloud c : clouds) if (now < c.until()) out.add(c);
        return out;
    }

    public void clear() { clouds.clear(); }
}
