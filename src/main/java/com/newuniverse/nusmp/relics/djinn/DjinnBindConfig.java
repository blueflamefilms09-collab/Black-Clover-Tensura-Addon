package com.newuniverse.nusmp.relics.djinn;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config {@code serverconfig/nusmp-djinn-bind-server.toml} (1.101.0): the two ways to bind a Djinn that do not depend on an item's use hook.
 * Both default ON so that no other mod (Better Combat's two-handed rule hides the off hand) can ever stop a player binding a Seal.
 */
public final class DjinnBindConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue COMMAND_BIND, GUI_BIND;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("djinnBind");
        COMMAND_BIND = b.comment("Players may bind a Seal with /multiverse djinn bind (also /djinn bind) and with the Djinn key (J) while no Djinn is bound.")
                .define("commandBind", true);
        GUI_BIND = b.comment("Players may bind a Seal in an inventory screen: Seal on the cursor, right-click the vessel (or the other way round).")
                .define("guiBind", true);
        b.pop();
        SPEC = b.build();
    }

    private DjinnBindConfig() {}

    public static boolean command() {
        try { return COMMAND_BIND.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static boolean gui() {
        try { return GUI_BIND.get(); } catch (RuntimeException notLoaded) { return true; }
    }
}
