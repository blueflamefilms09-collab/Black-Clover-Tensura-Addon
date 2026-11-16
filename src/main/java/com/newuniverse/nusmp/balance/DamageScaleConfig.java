package com.newuniverse.nusmp.balance;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the damage scale-down changes gameplay, so every number is a SERVER setting of its own, {@code nusmp-damage-server.toml}, section {@code [damage]} (docs/damage_scale_down.md). Off
 * ({@code scaleDownEnabled = false}) brings back the old numbers exactly. Readers answer the default while the spec is not loaded (unit tests, early startup).
 */
public final class DamageScaleConfig {
    private DamageScaleConfig() {}

    public static final String NAMESPACE = "anime_multiverse";
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, DJINN_FLOOR, EXTREME_TYPES;
    public static final ModConfigSpec.DoubleValue WEAPON_CAP, GLOBAL_CAP, DJINN_MIN, DJINN_MAX, APEX_CAP;
    public static final ModConfigSpec.ConfigValue<String> APEX_ID;

    static {
        double top = DamageScale.ABSOLUTE_MAX;
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("damage");
        ENABLED = b.comment("Master switch of the damage scale-down. false = the old damage exactly (no per-hit ceiling, the old Extreme Magic damage types).").define("scaleDownEnabled", true);
        GLOBAL_CAP = b.comment("The most one hit that comes from the mod may deal (weapons, spells, skills, ultimates, bosses' hits on players). Never above 15,000 (the hard maximum).")
                .defineInRange("globalHitCap", DamageScale.DEFAULT_GLOBAL, 1.0, top);
        WEAPON_CAP = b.comment("The most one melee blow with a held weapon of the mod may deal (also under globalHitCap). Never above 15,000.")
                .defineInRange("weaponHitCap", DamageScale.DEFAULT_WEAPON, 1.0, top);
        DJINN_MIN = b.comment("The Djinn band: the weapon hit of the weakest Djinn (rank 36), and the floor of every Djinn weapon swing.").defineInRange("djinnMin", DamageScale.DEFAULT_DJINN_MIN, 1.0, top);
        DJINN_MAX = b.comment("The Djinn band: the weapon hit of the strongest Djinn (rank 1). It is also the ceiling of that Djinn's Extreme Magic per hit.").defineInRange("djinnMax", DamageScale.DEFAULT_DJINN_MAX, 1.0, top);
        DJINN_FLOOR = b.comment("A full swing with a Djinn weapon never lands under djinnMin (against mobs; players keep the PvP hit cap). false = the band is only a ceiling.").define("djinnFloor", true);
        APEX_ID = b.comment("The ONE weapon allowed above globalHitCap: its item id. Default anime_multiverse:elsdocia (the first Wizard King's sword; the owner's 'Escadosa'). Empty = no apex weapon.")
                .define("apexWeaponId", DamageScale.DEFAULT_APEX_ID);
        APEX_CAP = b.comment("The per-hit ceiling of the apex weapon (and of what its wielder deals while holding it). Clamped to 15,000 in code, the hard maximum of the whole mod.")
                .defineInRange("apexHitCap", DamageScale.DEFAULT_APEX, 1.0, top);
        EXTREME_TYPES = b.comment("Each Djinn's Extreme Magic hits with the damage type of its own element or concept (fire, thunder, ice, water, wind, life, light ...). false = the old types (fire, lightning, sonic, magic).")
                .define("extremeElementTypes", true);
        b.pop();
        SPEC = b.build();
    }

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** The settings now, as the pure record. */
    public static DamageScale.Limits limits() {
        return new DamageScale.Limits(get(ENABLED), get(GLOBAL_CAP), get(WEAPON_CAP), get(DJINN_MIN), get(DJINN_MAX), get(APEX_CAP), get(DJINN_FLOOR));
    }

    public static boolean enabled() { return get(ENABLED); }

    /** The widest per-hit ceiling now (BalanceLaw.cap): the larger of the global and the apex ceiling, never above 15,000; no limit when the scale-down is off. */
    public static double softCeiling() { return DamageScale.softCeiling(limits()); }

    public static boolean extremeTypes() { return get(ENABLED) && get(EXTREME_TYPES); }

    public static String apexId() { return get(APEX_ID); }
}
