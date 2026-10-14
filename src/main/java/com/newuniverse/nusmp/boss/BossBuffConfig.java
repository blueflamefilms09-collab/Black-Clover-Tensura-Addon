package com.newuniverse.nusmp.boss;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The two server-config knobs of the boss overhaul, read through NUConfig by name so this package compiles before the lead adds them:
 * {@code NUConfig.BOSS_GLOBAL_BUFFS} (boolean, default true: "bosses.globalBuffs") and {@code NUConfig.BOSS_BUFF_MULTIPLIER} (double
 * 0..3, default 1.0: "bosses.buffMultiplier"). Until the fields exist, or while the config is not loaded, the defaults apply.
 */
public final class BossBuffConfig {
    private BossBuffConfig() {}

    public static boolean globalBuffs() {
        Object v = field("BOSS_GLOBAL_BUFFS");
        try {
            if (v instanceof ModConfigSpec.BooleanValue b) return b.get();
        } catch (Throwable notLoaded) { /* the default */ }
        return true;
    }

    /** The multiplier on the engine's effect levels and on Lucius's damage cuts, clamped 0..{@link BossBuffTable#MAX_MULTIPLIER}. */
    public static double multiplier() {
        Object v = field("BOSS_BUFF_MULTIPLIER");
        try {
            if (v instanceof ModConfigSpec.DoubleValue d) return BossBuffTable.clampMultiplier(d.get());
        } catch (Throwable notLoaded) { /* the default */ }
        return 1.0;
    }

    private static final java.util.Map<String, Object> CACHE = new java.util.HashMap<>();

    /** The config value object NUConfig holds under this name (looked up once), or null while the field does not exist. */
    private static Object field(String name) {
        if (CACHE.containsKey(name)) return CACHE.get(name);
        Object v = null;
        try {
            v = com.newuniverse.nusmp.NUConfig.class.getField(name).get(null);
        } catch (Throwable absent) { /* the lead has not added it yet */ }
        CACHE.put(name, v);
        return v;
    }
}
