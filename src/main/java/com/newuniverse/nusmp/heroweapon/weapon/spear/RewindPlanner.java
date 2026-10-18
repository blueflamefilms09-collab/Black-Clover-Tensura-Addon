package com.newuniverse.nusmp.heroweapon.weapon.spear;

import com.newuniverse.nusmp.heroweapon.weapon.pierce.SafeSpot;
import com.newuniverse.nusmp.heroweapon.weapon.spear.RewindBuffer.Snap;

/**
 * Why: the rule of the Legendary Spear's time reversal is the owner's, and it is easy to get subtly wrong: the wielder goes back to where they stood up to
 * eight seconds ago, but only to a spot that is safe (air above a solid floor, not inside blocks, the same dimension), else the nearest safe spot within three
 * blocks, else the technique is refused; they regain the health they had then but never more than a quarter of their maximum above what they have now. This
 * class makes those decisions with no game type in it, so each case is tested: a snapshot too young or too old, in another dimension, in a wall, over a pit.
 */
public final class RewindPlanner {
    private RewindPlanner() {}

    /** Where to go and from which moment: {@code exact} is true when the wielder's own block of that moment is still fine to stand in. */
    public record Plan(Snap snap, SafeSpot.Spot spot, boolean exact) {}

    /** True when some snapshot is old enough and young enough to go back to, before anyone asks whether its place is safe (so the refusal can say why). */
    public static boolean hasPast(RewindBuffer buf, long now, int window, int minAge) {
        for (Snap s : buf.snapshots()) {
            long age = now - s.tick();
            if (age >= minAge && age <= window) return true;
        }
        return false;
    }

    /**
     * The oldest snapshot that is at least {@code minAge} and at most {@code window} ticks old, in dimension {@code dim}, whose place (or a place within
     * {@code radius} blocks of it) can be stood in, or null when there is none. Oldest first because the point of the technique is to go as far back as
     * is safe; a snapshot taken in mid-air (a fall) is skipped in favour of a later one that was on the ground.
     */
    public static Plan choose(RewindBuffer buf, long now, int window, int minAge, String dim, SafeSpot.Probe probe, double radius) {
        for (Snap s : buf.snapshots()) {
            long age = now - s.tick();
            if (age > window || age < minAge) continue;
            if (!s.dim().equals(dim)) continue;
            int bx = (int) Math.floor(s.x()), by = (int) Math.floor(s.y()), bz = (int) Math.floor(s.z());
            if (probe.standable(bx, by, bz)) return new Plan(s, new SafeSpot.Spot(bx, by, bz), true);
            SafeSpot.Spot near = SafeSpot.nearest(probe, bx, by, bz, radius);
            if (near != null) return new Plan(s, near, false);
        }
        return null;
    }

    /**
     * The health to give back: what the wielder had then minus what they have now, never below 0 and never above {@code cap} of their maximum
     * ({@code cap} 0.25 is a quarter). Never takes health away.
     */
    public static float healBack(float current, float then, float max, double cap) {
        if (!(max > 0f) || Float.isNaN(current) || Float.isNaN(then)) return 0f;
        float want = then - current;
        if (want <= 0f) return 0f;
        return (float) Math.min(want, max * Math.max(0.0, cap));
    }
}
