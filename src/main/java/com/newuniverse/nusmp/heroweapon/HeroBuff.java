package com.newuniverse.nusmp.heroweapon;

/** 3.55.0: the hero weapon buffs as pure arithmetic; the config values come in through {@link HeroConfig}, the tests pass their own. */
public final class HeroBuff {
    private HeroBuff() {}

    public static boolean on() { return HeroConfig.get(HeroConfig.BUFF_ENABLED); }

    /** Factor on the damage a wielder of {@code kind} deals. */
    public static double damageFactor(HeroKind kind) { return on() ? damageFactor(kind, HeroConfig.get(HeroConfig.BUFF_DAMAGE), HeroConfig.get(HeroConfig.BUFF_HOLY_EXTRA)) : 1.0; }

    public static double damageFactor(HeroKind kind, double base, double holyExtra) { return kind != null && kind.isHoly() ? base * holyExtra : base; }

    /** A technique cooldown after the buff (never below 1 tick; the dispatcher's floor applies after this). */
    public static int cooldown(int ticks) { return on() ? cooldown(ticks, HeroConfig.get(HeroConfig.BUFF_COOLDOWN)) : ticks; }

    public static int cooldown(int ticks, double factor) { return Math.max(1, (int) Math.round(Math.max(0, ticks) * factor)); }

    /** A technique cost (a share of the maximum magicules) after the buff. */
    public static double cost(double share) { return on() ? share * HeroConfig.get(HeroConfig.BUFF_COST) : share; }

    /** Damage a bound wielder of {@code kind} takes, after the buff. */
    public static float taken(HeroKind kind, float in) {
        if (!on()) return in;
        return taken(kind, in, HeroConfig.get(HeroConfig.BUFF_TAKEN), HeroConfig.get(HeroConfig.BUFF_SHIELD_TAKEN));
    }

    public static float taken(HeroKind kind, float in, double general, double shield) { return (float) (in * (kind == HeroKind.SHIELD ? Math.min(general, shield) : general)); }

    /** What the Shield throws back for an incoming blow of {@code in}. */
    public static float reflect(float in) { return on() ? reflect(in, HeroConfig.get(HeroConfig.BUFF_REFLECT), HeroConfig.get(HeroConfig.BUFF_REFLECT_CAP)) : 0f; }

    public static float reflect(float in, double share, double cap) { return (float) Math.max(0.0, Math.min(cap, in * share)); }
}
