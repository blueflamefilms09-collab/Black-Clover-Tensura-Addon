package com.newuniverse.nusmp.client.companion;

import com.newuniverse.nusmp.client.companion.gs.CompanionStyle;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: what only changes LOOKS of the companion system is a client setting ({@code nusmp-companions-client.toml}): whether speech bubbles are drawn over heads. (What changes how the game plays is the server
 * config {@code companions.*}.) A plain {@code define}, read through {@link #bubbles} which answers the default while the spec is not loaded yet.
 */
public final class CompanionClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue BUBBLES;
    public static final ModConfigSpec.ConfigValue<String> SCREEN_STYLE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("companions");
        BUBBLES = b.comment("Draw the speech bubbles over the heads of companions (at most 24 at a time, only within 24 blocks). Off: you still read everything in the talk screen.")
                .define("bubbles", true);
        SCREEN_STYLE = b.comment("The look of the talk screen and of the speech bubbles: 'GREAT_SPIRIT' (portrait with expressions, typed dialogue, rapport leaves, gift slots, stance icons, pouch ring, bond story; skin B for everybody, skin C for devil users, see ui.greatSpirit.skin), 'CLASSIC' (the plain dark screen) or 'AUTO' (the Great Spirit screen while ui.theme is great_spirit, the classic one otherwise).")
                .define("screenStyle", "AUTO", o -> o instanceof String s && (s.equalsIgnoreCase("AUTO") || s.equalsIgnoreCase("GREAT_SPIRIT") || s.equalsIgnoreCase("CLASSIC")));
        b.pop();
        SPEC = b.build();
    }

    private CompanionClientConfig() {}

    public static boolean bubbles() {
        try { return BUBBLES.get(); } catch (IllegalStateException notLoaded) { return BUBBLES.getDefault(); }
    }

    /** The configured choice (AUTO while the spec is not loaded yet). */
    public static CompanionStyle.Choice screenChoice() {
        try { return CompanionStyle.Choice.parse(SCREEN_STYLE.get()); } catch (IllegalStateException notLoaded) { return CompanionStyle.Choice.AUTO; }
    }
}
