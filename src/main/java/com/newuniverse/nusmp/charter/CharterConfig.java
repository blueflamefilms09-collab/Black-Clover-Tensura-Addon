package com.newuniverse.nusmp.charter;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config of the Charter Grimoires: world/serverconfig/nusmp-charter-server.toml. */
public final class CharterConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, MIRROR;
    public static final ModConfigSpec.DoubleValue DROP_CHANCE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("charter");
        ENABLED = b.comment("The first-kill Canon Grimoire drops. false = no boss leaves one and nothing is locked (grimoires can still be given with /multiverse charter give).")
                .define("enabled", true);
        DROP_CHANCE = b.comment("Multiplies each boss's chance to leave its Canon Grimoire (25% for the four Zogratis, 40% for the floor bosses). It only rolls while the grimoire is unclaimed in this world; 0 = never, 4 = four times as likely (capped at 100%).")
                .defineInRange("dropChance", 1.0, 0.0, 10.0);
        MIRROR = b.comment("A held Canon Grimoire lends its boss's skills for as long as it is held. false = the grimoires lend nothing.")
                .define("mirror", true);
        b.pop();
        SPEC = b.build();
    }

    private CharterConfig() {}

    public static boolean enabled() { return get(ENABLED); }

    public static boolean mirror() { return get(MIRROR); }

    public static double dropChance() { return get(DROP_CHANCE); }

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
