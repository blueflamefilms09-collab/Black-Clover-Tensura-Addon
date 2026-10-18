package com.newuniverse.nusmp.heroweapon.rules;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the third restriction of the Holy Weapons says the heroes gain no experience when they stand too close to one another. Who is "too close" is plain
 * geometry (another Holy wielder in the same dimension within the repel radius), and it is the part of the rule most likely to be got wrong at the edges
 * (exactly at the radius, in another dimension, three heroes in a row, a hero alone), so it lives apart from the game: the game-side {@code HeroInterference}
 * lists the Holy wielders once a second and asks here which of them are blocked. Two heroes block each other; a third in range of only one of them is blocked
 * too, and the one in the middle of a chain is blocked whichever way it is read. Pure, tested.
 */
public final class InterferenceGeometry {
    private InterferenceGeometry() {}

    /** One Holy wielder: a stable id (the uuid text), the dimension's id, and the position. */
    public record Wielder(String id, String dimension, double x, double y, double z) {}

    /** The ids of the wielders that have another wielder (a different id) within {@code radius} blocks, in the same dimension; a distance of exactly the radius counts. */
    public static Set<String> blocked(List<Wielder> wielders, double radius) {
        Set<String> out = new LinkedHashSet<>();
        if (wielders == null || !(radius > 0.0)) return out;
        double r2 = radius * radius;
        for (int i = 0; i < wielders.size(); i++) {
            Wielder a = wielders.get(i);
            for (int j = i + 1; j < wielders.size(); j++) {
                Wielder b = wielders.get(j);
                if (a.id().equals(b.id()) || !a.dimension().equals(b.dimension())) continue;
                double dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
                if (dx * dx + dy * dy + dz * dz <= r2) {
                    out.add(a.id());
                    out.add(b.id());
                }
            }
        }
        return out;
    }
}
