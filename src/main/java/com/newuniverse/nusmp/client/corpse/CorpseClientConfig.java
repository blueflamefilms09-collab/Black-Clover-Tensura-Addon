package com.newuniverse.nusmp.client.corpse;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: Rades's numbered undead (Jimmy, Alfredo, Carl) got new custom looks that follow the series art; the old looks are replaced, not deleted, so one client switch brings them back
 * ({@code corpse.classicLooks}, default false). It only changes what is drawn (a CLIENT config, own file {@code nusmp-corpse-client.toml}); the stats, the moves and the saves do not depend on it.
 * Read once when the renderers are registered, so a change needs a restart of the client. Reads fall back to false before the config is loaded.
 */
public final class CorpseClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CLASSIC_LOOKS, FALLBACK_LOOK;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("corpse");
        CLASSIC_LOOKS = b.comment("true: Jimmy, Alfredo and Carl keep their first (1.43.0) models instead of the new looks that follow the series art. Applied when the game starts.")
                .define("classicLooks", false);
        FALLBACK_LOOK = b.comment("true (default, 3.7.1): when the geo model or the texture of a numbered undead (No.4 to No.0) fails to load, draw a tinted zombie stand-in (with the name) so the body is never invisible,",
                "and write one line to the client log saying which file failed. false = the old behaviour (draw nothing).").define("fallbackLook", true);
        b.pop();
        SPEC = b.build();
    }

    private CorpseClientConfig() {}

    public static boolean fallbackLook() {
        try { return FALLBACK_LOOK.get(); } catch (IllegalStateException unloaded) { return true; }
    }

    public static boolean classicLooks() {
        try { return CLASSIC_LOOKS.get(); } catch (IllegalStateException unloaded) { return false; }
    }
}
