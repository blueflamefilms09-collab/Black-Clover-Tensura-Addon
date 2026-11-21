package com.newuniverse.nusmp.bossdrops2;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the ONE setting of the worthwhile boss drops (docs/boss_drops_worthwhile.md). SERVER config {@code nusmp-bossdrops2-server.toml}, {@code [bossDrops] worthwhile}: true (the default) pays the signature
 * drops, the personal rolls and the one-of-ones on top of the old drops; false gives exactly the drops the bosses had before this feature (the older {@code nusmp-bossdrops-server.toml} trophies and the Charter
 * Grimoire are separate and not affected). Every reader goes through {@link #worthwhile()}, which answers the default while the spec is not loaded yet.
 */
public final class SignatureConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue WORTHWHILE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("bossDrops");
        WORTHWHILE = b.comment("Worthwhile boss drops: every boss pays a signature drop of its own, every fighter rolls their own extras, and the strongest bosses pay a one-of-one relic (once per world; a second kill pays an echo). False: bosses drop what they dropped before this feature.")
                .define("worthwhile", true);
        b.pop();
        SPEC = b.build();
    }

    private SignatureConfig() {}

    public static boolean worthwhile() {
        try {
            return WORTHWHILE.get();
        } catch (IllegalStateException notLoaded) {
            return WORTHWHILE.getDefault();
        }
    }
}
