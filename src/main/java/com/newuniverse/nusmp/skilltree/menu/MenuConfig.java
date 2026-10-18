package com.newuniverse.nusmp.skilltree.menu;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Why: the integration with Tensura's menu is built on things nobody can check without the game (where Tensura's frame sits, what its checkbox texture looks like), so a server owner or the mod's owner
 * must be able to change the visible choices without a rebuild: switch the whole integration off, choose where the nested panel docks, choose the checkbox skin, hide the admin button. It is a COMMON
 * config (its own file, {@code nusmp-tensura-menu-common.toml}), because the master switch is read by the server (which teaches the stand-in skills) and the looks by the client, each from its own copy. No
 * value uses {@code defineInList} (a list-validated value throws while the config is still loading, the reason {@code TreeConfig} avoids it): the two choice values are strings with a validator, and a
 * reader that gets an unknown word falls back to the default through {@link #placement()} / {@link #checkboxSkin()}. Every reader goes through {@link #get}, which answers the default while the spec is
 * not loaded yet (a client in the main menu, a unit test, an early event).
 */
public final class MenuConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, ADMIN_BUTTON;
    public static final ModConfigSpec.ConfigValue<String> PLACEMENT, CHECKBOX_SKIN;
    public static final ModConfigSpec.IntValue PANEL_WIDTH;

    /** The words {@link #PLACEMENT} accepts. */
    public static final List<String> PLACEMENTS = List.of("auto", "right", "left", "below", "inside");
    /** The words {@link #CHECKBOX_SKIN} accepts. */
    public static final List<String> SKINS = List.of("vanilla", "tensura");

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("tensuraMenu");
        ENABLED = b.comment("Master switch of the Tensura menu integration: when false no stand-in skill is taught to players and Tensura's Ability Selection screen is left alone. The stand-in skills stay registered.")
                .define("enabled", true);
        PLACEMENT = b.comment("Where the nested panel (arts, passives and their ON/OFF boxes) of the selected line docks: auto, right, left, below (of Tensura's frame) or inside (right of Tensura's list, over its own detail area). auto picks the first that fits.")
                .define("panelPlacement", "auto", o -> o instanceof String s && PLACEMENTS.contains(s));
        PANEL_WIDTH = b.comment("Width in GUI pixels the nested panel asks for (it shrinks to fit the window).")
                .defineInRange("panelWidth", 210, 150, 320);
        CHECKBOX_SKIN = b.comment("vanilla = the checkbox sprites of the game (always correct). tensura = try Tensura's own checkbox texture; used only when it is a vertical strip of square frames (off on top, on below), else vanilla.")
                .define("checkboxSkin", "vanilla", o -> o instanceof String s && SKINS.contains(s));
        ADMIN_BUTTON = b.comment("Show the ADMIN INSPECT switch on Tensura's Unique Skill list for operators and creative players (it lists all 283 lines, read-only unless owned).")
                .define("adminButton", true);
        b.pop();
        SPEC = b.build();
    }

    private MenuConfig() {}

    /** Registers the file on the mod container. Called once by {@code LineStandIns.init}. */
    public static void register(ModContainer container) { container.registerConfig(ModConfig.Type.COMMON, SPEC, "nusmp-tensura-menu-common.toml"); }

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try {
            return v.get();
        } catch (IllegalStateException notLoaded) {
            return v.getDefault();
        }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static boolean adminButton() { return get(ADMIN_BUTTON); }

    public static int panelWidth() { return get(PANEL_WIDTH); }

    /** The placement word, or {@code auto} when the file holds something else. */
    public static String placement() {
        String s = get(PLACEMENT);
        return PLACEMENTS.contains(s) ? s : "auto";
    }

    /** The checkbox skin word, or {@code vanilla} when the file holds something else. */
    public static String checkboxSkin() {
        String s = get(CHECKBOX_SKIN);
        return SKINS.contains(s) ? s : "vanilla";
    }
}
