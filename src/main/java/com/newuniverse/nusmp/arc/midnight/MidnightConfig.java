package com.newuniverse.nusmp.arc.midnight;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: nothing about the four Midnight Sun bosses has ever run in a game, so a server owner needs a few honest dials that do not need a data pack: the
 * damage of the whole group (the one number most likely to be wrong), the three mechanics that decide how a fight feels (the Third Eye bond, the
 * crowd-control effects, the turning back of beams and fireballs), the size of Rades's army, and the two "break it" windows (Fana's Evil Heart and
 * Patolli's Heaven's Verdict). {@code nusmp-midnight-server.toml}, section {@code [midnight]}. Every value is a plain {@code define} / {@code defineInRange}
 * (never {@code defineInList}: a list-validated value throws while the server is still loading) and every reader goes through {@link #get}, which
 * answers the default while the spec is not loaded yet. The lead registers the spec (see docs/arc/midnight_sun.md).
 */
public final class MidnightConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue BOND_RAGE, CONTROL_EFFECTS, REFLECT_BACKFIRE;
    public static final ModConfigSpec.DoubleValue DAMAGE_SCALE, EVIL_HEART_BREAK_PERCENT, VERDICT_BREAK_PERCENT;
    public static final ModConfigSpec.IntValue THRALL_LIMIT, STAGGER_LOCKOUT_TICKS, EVIL_HEART_TICKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("midnight");
        DAMAGE_SCALE = b.comment("Multiplier on the damage of every move of Fana, Patolli, Rades Spirito and Rhya (1.0 = as designed). The numbers were never played; this is the first dial to turn.")
                .defineInRange("damageScale", 1.0, 0.1, 3.0);
        BOND_RAGE = b.comment("The Third Eye bond: when one of Vetto, Fana and Rhya falls, the others that are still standing nearby take one rage step (+10% power).")
                .define("thirdEyeBond", true);
        CONTROL_EFFECTS = b.comment("Allow the crowd-control effects of these bosses (Patolli's blinding flash, the roots of Fana, Rades and Rhya, Rhya's copied blindness). They always obey the balance law's shared cooldown and caps.")
                .define("controlEffects", true);
        REFLECT_BACKFIRE = b.comment("Patolli's Light Shaft and Fana's fireball can be turned back by Full Reflection, a mirror array or (fireball only) an anti-magic swing, and then strike their caster.")
                .define("reflectBackfire", true);
        THRALL_LIMIT = b.comment("Most risen corpses of Rades Spirito alive at once (his power step may allow fewer). 0 = he raises none.")
                .defineInRange("thrallLimit", 6, 0, 12);
        STAGGER_LOCKOUT_TICKS = b.comment("After one of these bosses has been left open (overreach, a turned-back attack, a broken Evil Heart or Verdict), ticks before they can be left open again.")
                .defineInRange("staggerLockoutTicks", 160, 0, 2400);
        EVIL_HEART_TICKS = b.comment("Fana's Evil Heart (100% power only): ticks from the sphere's birth to its detonation.")
                .defineInRange("evilHeartTicks", 150, 60, 600);
        EVIL_HEART_BREAK_PERCENT = b.comment("Percent of Fana's maximum health that must be taken from her during the Evil Heart to break it (the Third Eye shatters and the sphere dies).")
                .defineInRange("evilHeartBreakPercent", 6.0, 1.0, 40.0);
        VERDICT_BREAK_PERCENT = b.comment("Percent of Patolli's maximum health that must be taken from him while he casts Heaven's Verdict to break the incantation.")
                .defineInRange("verdictBreakPercent", 5.0, 1.0, 40.0);
        b.pop();
        SPEC = b.build();
    }

    private MidnightConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static float damageScale() { return get(DAMAGE_SCALE).floatValue(); }

    public static boolean bondRage() { return get(BOND_RAGE); }

    public static boolean controls() { return get(CONTROL_EFFECTS); }

    public static boolean reflects() { return get(REFLECT_BACKFIRE); }

    public static int thrallLimit() { return get(THRALL_LIMIT); }

    public static int lockout() { return get(STAGGER_LOCKOUT_TICKS); }

    public static int evilHeartTicks() { return get(EVIL_HEART_TICKS); }

    /** As a fraction of maximum health (0.06 for the default 6.0). */
    public static double evilHeartBreak() { return get(EVIL_HEART_BREAK_PERCENT) / 100.0; }

    /** As a fraction of maximum health (0.05 for the default 5.0). */
    public static double verdictBreak() { return get(VERDICT_BREAK_PERCENT) / 100.0; }
}
