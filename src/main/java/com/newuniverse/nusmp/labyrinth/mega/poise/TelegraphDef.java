package com.newuniverse.nusmp.labyrinth.mega.poise;

import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryAbility;

/**
 * Why: one row of the telegraph table ({@code data/nusmp/mega/poise/telegraphs.json}): HOW an existing Foundry ability warns before it lands. The ability itself (damage, cooldown, radius,
 * wind-up) stays in {@code _abilities.json}; this row only adds what the ability did not have: the shape drawn on the floor, whether the mob has super armor while it winds up, the shortest
 * wind-up that is fair to read, and whether the shape sits on the TARGET (a leap's landing, a curse) instead of on the mob. Numbers the ability already has win over the row's own
 * fallbacks: the radius is the ability's {@code r} (the row's only if the ability has none), a cleave's cone is the ability's {@code amp} degrees, a dash is as long as it travels.
 *
 * @param shape     what is drawn
 * @param armor     super armor during the wind-up and the swing
 * @param minWind   the least wind-up (ticks) the attack gets
 * @param atTarget  the shape is centred on the target's position, not on the mob
 * @param radius    fallback radius when the ability has none
 * @param angle     fallback cone angle (degrees) when the ability is not a cleave
 * @param halfWidth half the width of a line
 */
public record TelegraphDef(TelegraphShape shape, boolean armor, int minWind, boolean atTarget, double radius, double angle, double halfWidth) {

    public TelegraphDef {
        if (shape == null) shape = TelegraphShape.NONE;
        minWind = Math.max(0, Math.min(FoundryAbility.MAX_WIND, minWind));
        radius = Math.max(0.5, Math.min(FoundryAbility.MAX_RADIUS, radius));
        angle = Math.max(10.0, Math.min(360.0, angle));
        halfWidth = Math.max(0.3, Math.min(4.0, halfWidth));
    }

    /** The radius of the drawing for this ability. */
    public double radiusFor(FoundryAbility a) { return a.r() > 0.0 ? a.r() : radius; }

    /** The cone angle for this ability: a cleave's own arc, else the row's. */
    public double angleFor(FoundryAbility a) { return a.kind() == FoundryAbility.Kind.CLEAVE && a.amp() > 0 ? a.amp() : angle; }

    /** The length of a line for this ability: how far a dash or dive travels (about one block per tick), never past the ability's own range. */
    public double lengthFor(FoundryAbility a) {
        double travel = a.dur() > 0 ? a.dur() * 1.05 : a.max() > 0.0 ? a.max() : 3.0;      // a dash is as long as it travels; a beam (a pull) as long as its range
        travel = Math.max(3.0, travel);
        return a.max() > 0.0 ? Math.min(travel, Math.max(3.0, a.max())) : travel;
    }

    /** Does a point at local {@code (f, s)} fall on the ground this attack hits? (The same test that draws it.) */
    public boolean hits(FoundryAbility a, double f, double s) {
        return TelegraphGeometry.inside(shape, f, s, radiusFor(a), angleFor(a), lengthFor(a), halfWidth);
    }
}
