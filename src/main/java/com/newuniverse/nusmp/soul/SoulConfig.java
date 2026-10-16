package com.newuniverse.nusmp.soul;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: Paladin Conversion changes how a whole creature behaves, so each of its knobs must be a switch the owner can turn without a
 * rebuild: the ceiling of the follower limit, the strength of the buffs, whether a released Paladin turns hostile again. Own spec
 * file ({@code nusmp-soul-server.toml}, SERVER) so it never touches another feature's config; the lead registers it with
 * {@code modContainer.registerConfig(ModConfig.Type.SERVER, SoulConfig.SPEC, "nusmp-soul-server.toml")}. Plain {@code define} and
 * {@code defineInRange} only. Reads fall back to the defaults before the config is loaded.
 */
public final class SoulConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, RELEASED_HOSTILE, PLAYER_SKINS;
    public static final ModConfigSpec.IntValue PALADIN_MAX, PALADIN_BASE, PALADIN_MASTERY_STEP;
    public static final ModConfigSpec.DoubleValue PALADIN_STRENGTH;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("soul");
        ENABLED = b.comment("Paladin Conversion (Lucius's Soul Magic) can purify creatures. false = the pages refuse to cast; existing Paladins keep serving.")
                .define("paladinEnabled", true);
        PALADIN_MAX = b.comment("The most Paladins one caster may hold, whatever their mastery (hard ceiling 6). The oldest lets go when a new one is made.")
                .defineInRange("paladinMax", 6, 1, 6);
        PALADIN_BASE = b.comment("Paladins a caster may hold at 0% mastery; one more per paladinMasteryStep percent of mastery, up to paladinMax.")
                .defineInRange("paladinBase", 3, 1, 6);
        PALADIN_MASTERY_STEP = b.comment("Mastery percent that adds one more Paladin to the limit.")
                .defineInRange("paladinMasteryStep", 25, 5, 100);
        PALADIN_STRENGTH = b.comment("Scales every Paladin buff at once (health +100%, attack +60%, armour 8, toughness 4, speed +15%, knockback resistance). 1.0 = as specified, 0 = none.")
                .defineInRange("paladinStrength", 1.0, 0.0, 3.0);
        RELEASED_HOSTILE = b.comment("A released Paladin (dismissed, owner died, evicted) gets its original target goals back and is hostile again. false = it stays neutral.")
                .define("releasedHostile", false);
        PLAYER_SKINS = b.comment("Draw the halo and wings on players that carry the Paladin effect too (a Paladin effect applied by a command or another mod).")
                .define("paladinOnPlayers", true);
        b.pop();
        SPEC = b.build();
    }

    private SoulConfig() {}

    public static boolean enabled() { return get(ENABLED); }

    public static int max() { return get(PALADIN_MAX); }

    public static int base() { return get(PALADIN_BASE); }

    public static int masteryStep() { return get(PALADIN_MASTERY_STEP); }

    public static double strength() { return PaladinStats.clampStrength(get(PALADIN_STRENGTH)); }

    public static boolean releasedHostile() { return get(RELEASED_HOSTILE); }

    public static boolean onPlayers() { return get(PLAYER_SKINS); }

    /** The follower limit for a caster at this mastery percent, with the configured base, step and ceiling. */
    public static int limit(double masteryPercent) { return PaladinRoster.limit(masteryPercent, base(), masteryStep(), max()); }

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
