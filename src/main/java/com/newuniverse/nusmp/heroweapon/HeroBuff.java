package com.newuniverse.nusmp.heroweapon;

/** 3.55.0: the hero weapon buffs as pure arithmetic; the config values come in through {@link HeroConfig}, the tests pass their own. */
public final class HeroBuff {
    private HeroBuff() {}

    public static boolean on() { return HeroConfig.get(HeroConfig.BUFF_ENABLED); }

    /** Factor on the damage a wielder of {@code kind} deals. */
    public static double damageFactor(HeroKind kind) { return on() ? damageFactor(kind, HeroConfig.get(HeroConfig.BUFF_DAMAGE), HeroConfig.get(HeroConfig.BUFF_HOLY_EXTRA)) : 1.0; }

    public static double damageFactor(HeroKind kind, double base, double holyExtra) { return kind != null && kind.isHoly() ? base * holyExtra : base; }

    /** 3.73.0: the damage factor at a given weapon level: the buff above times the anime factor, which grows with the level (all through the config). */
    public static double damageFactor(HeroKind kind, int level) {
        if (!on()) return 1.0;
        return damageFactor(kind) * anime(HeroConfig.get(HeroConfig.ANIME_DAMAGE), HeroConfig.get(HeroConfig.ANIME_LEVEL_BONUS), level);
    }

    /** The anime factor alone: {@code base * (1 + bonus * level / 100)}, the level held between 0 and 100. */
    public static double anime(double base, double bonus, int level) { return base * (1.0 + bonus * Math.max(0, Math.min(100, level)) / 100.0); }

    /** A technique cooldown after the buff (never below 1 tick; the dispatcher's floor applies after this). */
    public static int cooldown(int ticks) { return on() ? cooldown(ticks, HeroConfig.get(HeroConfig.BUFF_COOLDOWN)) : ticks; }

    public static int cooldown(int ticks, double factor) { return Math.max(1, (int) Math.round(Math.max(0, ticks) * factor)); }

    /** A technique cost (a share of the maximum magicules) after the buff. */
    public static double cost(double share) { return on() ? share * HeroConfig.get(HeroConfig.BUFF_COST) : share; }

    /** Damage a bound wielder of {@code kind} takes, after the buff. */
    public static float taken(HeroKind kind, float in) {
        if (!on()) return in;
        return (float) (taken(kind, in, HeroConfig.get(HeroConfig.BUFF_TAKEN), HeroConfig.get(HeroConfig.BUFF_SHIELD_TAKEN)) * HeroConfig.get(HeroConfig.ANIME_TAKEN));
    }

    public static float taken(HeroKind kind, float in, double general, double shield) { return (float) (in * (kind == HeroKind.SHIELD ? Math.min(general, shield) : general)); }

    /** What the Shield throws back for an incoming blow of {@code in}. */
    public static float reflect(float in) { return on() ? reflect(in, HeroConfig.get(HeroConfig.BUFF_REFLECT), HeroConfig.get(HeroConfig.BUFF_REFLECT_CAP)) : 0f; }

    public static float reflect(float in, double share, double cap) { return (float) Math.max(0.0, Math.min(cap, in * share)); }
}
