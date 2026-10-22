package com.newuniverse.nusmp.gnome;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the server owner decides whether the Gnome (the Earth Spirit, docs/magic/gnome.md) exists as a companion and a stone giant ({@code magic.gnome}, default true; false = no Gnome link, every earth
 * page is cast the old way and the old Earth spirit body is all there is), and how many of them may stand in the world at once. These change gameplay, so they are SERVER settings. File:
 * nusmp-gnome-server.toml (registered by {@link GnomeBoot}).
 */
public final class GnomeConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue GNOME;
    public static final ModConfigSpec.IntValue MAX_SPIRITS, MAX_FULL, FULL_COOLDOWN;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("magic");
        GNOME = b.comment("The Gnome, the Earth Spirit: a small stone companion that walks beside an Earth Magic caster (the Gnome key links and unlinks it), the earth spells leave its fist, and the big pages call",
                "its giant form out of the ground. false = no Gnome link; every earth page is cast the old generic way. The pages, their numbers and their cooldowns are the same either way.")
                .define("gnome", true);
        b.pop();
        b.push("caps");
        MAX_SPIRITS = b.comment("Most Gnome companions linked across the whole server (one per caster).").defineInRange("maxSpirits", GnomeRules.DEFAULT_MAX_SPIRITS, 1, 256);
        MAX_FULL = b.comment("Most full-size Gnomes standing across the whole server (one per caster, a few seconds each).").defineInRange("maxFull", GnomeRules.DEFAULT_MAX_FULL, 1, 64);
        FULL_COOLDOWN = b.comment("Ticks before a caster may call the full-size Gnome again (20 = one second).").defineInRange("fullCooldownTicks", GnomeRules.DEFAULT_FULL_COOLDOWN, 100, 24000);
        b.pop();
        SPEC = b.build();
    }

    private GnomeConfig() {}

    /** Reads a value; before the config is loaded (tests, early startup) the default stands in. */
    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(GNOME); }
    public static int maxSpirits() { return get(MAX_SPIRITS); }
    public static int maxFull() { return get(MAX_FULL); }
    public static int fullCooldown() { return get(FULL_COOLDOWN); }
}
