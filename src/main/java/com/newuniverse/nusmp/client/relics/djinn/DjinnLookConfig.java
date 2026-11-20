package com.newuniverse.nusmp.client.relics.djinn;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the design audit (docs/djinn_design_audit.md) reworked the Djinn designs that were copies of another Djinn's (the Lesser Summon busts, two tiny Djinn). The old files are kept (owner rule 2),
 * so one client switch brings the old look back: {@code djinnLook.djinnLookClassic}, default false. It only changes what is drawn (a CLIENT config, own file {@code nusmp-djinn-look-client.toml}); nothing
 * the server decides depends on it. The paths are chosen when a texture is first asked for, so a change needs a restart of the client. A value read before the config is loaded (a unit test, a dedicated
 * server) gives the default false, which is the new look.
 */
public final class DjinnLookConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CLASSIC;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("djinnLook");
        CLASSIC = b.comment("true: the Djinn designs that the design audit reworked (the Lesser Summon busts, the tiny Vepar and Barbatos) are drawn as they were before. Applied when the game starts.")
                .define("djinnLookClassic", false);
        b.pop();
        SPEC = b.build();
    }

    private DjinnLookConfig() {}

    public static boolean classic() {
        try { return CLASSIC.get(); } catch (IllegalStateException unloaded) { return false; }
    }
}
