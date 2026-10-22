package com.newuniverse.nusmp.fairytail.storm;

/**
 * Why: the spell table of the Stormcaller Staff, in one place and with no game class in it, so it can be unit tested and read by the self-test, the tooltip, the docs and the
 * client alike. Storm Magic is a Fairy Tail Caster magic: summoning storm clouds, rain, hail, thunder and lightning out of the cloud, gales and tornado-like winds, and surges
 * of storm water. Each spell is a storm: a mask of weather parts ({@link StormComp}) with a size, a time, a slow drift and the numbers the server runs them with. The ordinal is stored
 * in the staff and sent to clients: ONLY APPEND NEW SPELLS AT THE END.
 *
 * <p>The raw damage numbers go through the balance law like every other spell of the mod (EP scaling, the PvP hit cap); they are tuned against the Fairy Tail skills of this mod
 * (a roar 11, a Brilliant Flame 18, the Raging Bolt 26), and a storm can never stack more than the per-target limits of {@link StormMath}.
 */
public enum StormSpell {
    //            id                 name                         mask                                                              cost  cd  aim radius dur drift boltRaw hailRaw tickRaw push bolts every
    GATHERING_CLOUDS("gathering_clouds", "Storm Cloud: Gathering", StormComp.RAIN | StormComp.BOLTS,                                 5.0, 200, 26, 7.0, 160, 1.6, 9.0, 0.0, 1.5, 0.0, 5, 30,
            "A storm cell gathers over the place you look at: a dark cloud rolls in, rain curtains fall and forked bolts strike from it. It drifts slowly the way you faced."),
    DOWNPOUR("downpour", "Downpour", StormComp.RAIN,                                                                                  4.0, 160, 24, 8.0, 140, 1.0, 0.0, 0.0, 2.2, 0.0, 0, 0,
            "A heavy rain falls on the area: foes are soaked, slowed and weakened, fires are put out, friends are cooled."),
    HAILSTORM("hailstorm", "Hailstorm", StormComp.HAIL,                                                                               6.0, 240, 24, 6.5, 120, 1.2, 0.0, 3.4, 0.0, 0.0, 0, 0,
            "Hail stones as big as fists fall in volleys on the area; every stone that lands hurts what stands there."),
    THUNDERHEAD("thunderhead", "Thunderhead", StormComp.BOLTS | StormComp.RAIN,                                                       8.0, 300, 26, 6.0, 140, 1.0, 9.0, 0.0, 0.0, 0.0, 7, 14,
            "A tall black thunderhead builds and throws bolt after bolt at the foes under it, each bolt leaping to the foes standing near its mark."),
    GALE_FRONT("gale_front", "Gale Front", StormComp.FRONT,                                                                           4.0, 140, 20, 5.5, 40, 0.0, 0.0, 0.0, 5.5, 1.15, 0, 0,
            "A wall of storm wind sweeps out in front of you and shoves everything on its line before it, rain streaming in its wake."),
    STORM_SURGE("storm_surge", "Storm Surge", StormComp.SURGE,                                                                        7.0, 260, 18, 3.0, 40, 0.0, 0.0, 0.0, 11.0, 0.95, 0, 0,
            "A roll of storm water breaks from a low cloud and runs along the ground, throwing foes back and soaking them."),
    TWISTER("twister", "Twister", StormComp.FUNNEL,                                                                                   9.0, 320, 22, 3.4, 120, 3.2, 0.0, 0.0, 2.4, 0.14, 0, 0,
            "A funnel drops from the cloud and walks across the ground, drawing in and lifting what it meets, then throws it out."),
    EYE_OF_THE_STORM("eye_of_the_storm", "Eye of the Storm", StormComp.RAIN | StormComp.HAIL | StormComp.BOLTS | StormComp.GUST | StormComp.EYE, 16.0, 900, 0, 12.0, 240, 0.6, 12.0, 3.0, 1.2, 0.9, 8, 22,
            "The whole sky turns on the place you stand: rain, hail, bolts and gust rings in a wide cell, and in its middle a calm eye where you and your friends are looked after.");

    public final String id, displayName, description;
    /** Weather parts, see {@link StormComp}. */
    public final int mask;
    /** Cost as a share of the caster's maximum Aura (at least {@link StormMath#MIN_COST}). */
    public final double costPercent;
    public final int cooldownTicks;
    /** How far the cell is placed from the caster (0 = on the caster). For a line spell: the length. */
    public final int aimRange;
    /** Cell radius; for a line spell the half width. */
    public final double radius;
    public final int durationTicks;
    /** Slow drift of the whole cell, blocks per second, along the way the caster faced. */
    public final double driftPerSecond;
    public final double boltRaw, hailRaw, tickRaw, push;
    public final int maxBolts, boltEvery;

    StormSpell(String id, String name, int mask, double costPercent, int cooldownTicks, int aimRange, double radius, int durationTicks, double driftPerSecond,
               double boltRaw, double hailRaw, double tickRaw, double push, int maxBolts, int boltEvery, String description) {
        this.id = id;
        this.displayName = name;
        this.mask = mask;
        this.costPercent = costPercent;
        this.cooldownTicks = cooldownTicks;
        this.aimRange = aimRange;
        this.radius = radius;
        this.durationTicks = durationTicks;
        this.driftPerSecond = driftPerSecond;
        this.boltRaw = boltRaw;
        this.hailRaw = hailRaw;
        this.tickRaw = tickRaw;
        this.push = push;
        this.maxBolts = maxBolts;
        this.boltEvery = boltEvery;
        this.description = description;
    }

    public boolean has(int part) { return StormComp.has(mask, part); }

    /** True for the two spells that run along a line (a front, a surge) instead of standing as a cell. */
    public boolean isLine() { return has(StormComp.FRONT) || has(StormComp.SURGE); }

    public static StormSpell byId(String id) {
        if (id != null) for (StormSpell s : values()) if (s.id.equals(id)) return s;
        return null;
    }

    /** The spell at an ordinal, and the first spell for anything out of range (an old or damaged stack never crashes a cast). */
    public static StormSpell at(int ordinal) {
        StormSpell[] v = values();
        return ordinal < 0 || ordinal >= v.length ? v[0] : v[ordinal];
    }

    public StormSpell next() {
        StormSpell[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public static int count() { return values().length; }
}
