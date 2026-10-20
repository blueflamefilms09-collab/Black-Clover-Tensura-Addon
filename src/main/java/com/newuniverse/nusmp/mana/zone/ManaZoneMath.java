package com.newuniverse.nusmp.mana.zone;

/**
 * The maths of a Mana Zone, with no Minecraft types (unit tested). Nothing here keeps state: the radius is a function of the ticks since
 * the start tick, so a growing zone costs no per-tick bookkeeping on the server or on the client.
 */
public final class ManaZoneMath {
    private ManaZoneMath() {}

    /** The client draws at most this many vertices for one zone (the vertex buffer's own per-effect cap is the same 400). */
    public static final int VERTEX_BUDGET = 400;
    /** The caster's perception marks at most this many things at once (nearest first). */
    public static final int PERCEPTION_MAX = 24;
    /** How long the collapse finisher plays on the client. */
    public static final int COLLAPSE_TICKS = 14;

    // ---------------------------------------------------------------- the radius
    /** Ease-out cubic: fast at first, settling at the cap. */
    public static double ease(double x) {
        double c = x < 0 ? 0 : Math.min(x, 1);
        double k = 1 - c;
        return 1 - k * k * k;
    }

    /** The radius {@code elapsedTicks} after the start: 0 at the start, the cap after {@code growTicks}. No state. */
    public static double radiusAt(long elapsedTicks, int growTicks, double cap) {
        if (elapsedTicks <= 0 || cap <= 0) return 0;
        return cap * ease(elapsedTicks / (double) Math.max(1, growTicks));
    }

    /** The grown fraction 0..1 (what the power bonus ramps with). */
    public static double grown(long elapsedTicks, int growTicks) { return ease(elapsedTicks / (double) Math.max(1, growTicks)); }

    /**
     * The radius a caster can reach: {@code baseRadius} at the gate's EP, plus {@code radiusPerEpDecade} blocks for every factor of ten above
     * it, never over {@code maxRadius}. Under the gate EP it is the base (a Wizard King's forced zone).
     */
    public static double capRadius(double ep, double gateEp, ManaZoneRules.Zone z) {
        double decades = ep > 0 && gateEp > 0 ? Math.max(0, Math.log10(ep / gateEp)) : 0;
        return Math.min(z.maxRadius(), z.baseRadius() + z.radiusPerEpDecade() * decades);
    }

    public static boolean inside(double dx, double dy, double dz, double radius) { return dx * dx + dy * dy + dz * dz <= radius * radius; }

    // ---------------------------------------------------------------- costs
    /** Upkeep per second in percent of the caster's maximum magicule: it grows with the radius. */
    public static double upkeepPercent(double radius, ManaZoneRules.Zone z) { return z.upkeepBasePercent() + z.upkeepPerBlockPercent() * Math.max(0, radius); }

    /** Cooldown after the zone collapses, from the radius it had reached. */
    public static int cooldownTicks(double radius, ManaZoneRules.Zone z) { return (int) Math.min(72_000, z.cooldownBaseTicks() + z.cooldownPerBlockTicks() * Math.max(0, radius)); }

    /** The exhaustion after a collapse (slowness and weakness only: never hunger). Twice as long when the mana ran out. */
    public static int exhaustionTicks(long elapsedTicks, boolean manaSpent, ManaZoneRules.Zone z) {
        int t = z.exhaustionBaseTicks() + (int) Math.min(z.exhaustionMaxTicks(), Math.max(0, elapsedTicks) / 20 * 4);
        if (manaSpent) t *= 2;
        return Math.min(z.exhaustionMaxTicks() * 2, t);
    }

    /** Multiplier on a spell's mana cost for the caster inside the zone; worse while exhausted. */
    public static double costMult(ManaZoneRules.Zone z, boolean exhausted) { return exhausted ? 1.25 : z.costMult(); }

    // ---------------------------------------------------------------- offense
    /** Spell power multiplier: the bonus ramps in with the grown fraction and grows with the local mana density (1.0 ordinary, up to 3.0). */
    public static double powerMult(ManaZoneRules.Zone z, double grownFraction, double density) {
        double d = Math.max(0, Math.min(2.0, density - 1.0));
        return 1.0 + z.powerBonus() * Math.max(0, Math.min(1, grownFraction)) + z.powerDensityBonus() * d * 0.5 * Math.max(0, Math.min(1, grownFraction));
    }

    /** Mana Strike's reach in the zone, never beyond the zone's own radius plus the usual reach. */
    public static double strikeRange(double baseRange, double radius, ManaZoneRules.Zone z) { return Math.min(baseRange * z.rangeMult(), Math.max(baseRange, radius * 2.0)); }

    /** How many targets Mana Strike may pierce in the zone. */
    public static int strikeTargets(int base, ManaZoneRules.Zone z) { return base + z.extraTargets(); }

    /**
     * Where the {@code index}-th of {@code count} bolts of an Omni Volley starts, around a target at (tx,ty,tz) hit by a caster at (cx,cz):
     * bolt 0 comes from behind the target (on the line from the caster through it, {@code reach} past it); the rest are spread evenly round
     * the target, alternating head height and overhead. Writes x,y,z into {@code out} (no allocation).
     */
    public static void omniOrigin(int index, int count, double tx, double ty, double tz, double cx, double cz, double reach, double[] out) {
        double dx = tx - cx, dz = tz - cz;
        double base = Math.atan2(dz, dx);
        double a = base + 2 * Math.PI * index / Math.max(1, count);
        out[0] = tx + Math.cos(a) * reach;
        out[1] = ty + 1.0 + (index % 2) * 1.5;
        out[2] = tz + Math.sin(a) * reach;
    }

    // ---------------------------------------------------------------- defense
    /** Share of an incoming hit the caster foresees (0..1): the base, rising to the reinforced value with Reinforcement mastery; capped against bosses. */
    public static double predictReduction(ManaZoneRules.Zone z, double reinforcementMastery, boolean bossAttacker) {
        double m = Math.max(0, Math.min(1, reinforcementMastery));
        double r = z.predictBase() + (z.predictReinforced() - z.predictBase()) * m;
        return bossAttacker ? Math.min(r, z.predictBossCap()) : r;
    }

    /** Damage factor for an ally standing in the zone (the zone's own mana presses the blow aside), 1.0 = unchanged. */
    public static double allyFactor(ManaZoneRules.Zone z) { return 1.0 - z.allyPressure(); }

    /** Damage factor inside a pressurised space; a boss's blow is only cut by half as much. */
    public static double bubbleFactor(ManaZoneRules.Zone z, boolean bossAttacker) { return 1.0 - z.bubbleReduction() * (bossAttacker ? 0.5 : 1.0); }

    /** Speed factor of a foreign projectile that is a boss's: slowed, never removed. */
    public static double bossProjectileFactor(ManaZoneRules.Zone z) { return z.bossProjectileSlow(); }

    /** Movement speed bonus as an attribute multiplier (added as a fraction of the base). */
    public static double speedBonus(ManaZoneRules.Zone z, double grownFraction) { return z.speedBonus() * Math.max(0, Math.min(1, grownFraction)); }

    // ---------------------------------------------------------------- budgets (server)
    /** The next tick on which a periodic job of {@code interval} ticks runs, staggered by an id so zones do not all scan on the same tick. */
    public static long nextDue(long now, int interval, int stagger) {
        int i = Math.max(1, interval);
        long base = now - (now % i);
        long due = base + Math.floorMod(stagger, i);
        return due > now ? due : due + i;
    }

    // ---------------------------------------------------------------- budgets (client)
    /** Segments of the shell and rings by camera distance: full inside and near the zone, fewer as it gets farther; 0 = not drawn at all. */
    public static int lodSegments(double camDistance, double radius, int full, int min) {
        double r = Math.max(1.0, radius);
        if (camDistance > r * 8 + 96) return 0;
        double f = camDistance <= r ? 1.0 : camDistance <= r * 2 ? 0.75 : camDistance <= r * 4 ? 0.5 : 0.35;
        return Math.max(min, (int) Math.round(full * f));
    }

    /** Quads the shell spends of the 400-vertex budget (4 vertices each) given the segments in use. */
    public static int shellQuads(int lon, int lat) { return lon * lat; }

    /** Visibility fade of the zone's boundary as the camera nears it: the shell goes see-through when the camera is on it so it never fills the screen. */
    public static double shellAlpha(double camDistanceFromCentre, double radius) {
        double edge = Math.abs(camDistanceFromCentre - radius);
        return Math.max(0.25, Math.min(1.0, edge / 3.0));
    }
}
