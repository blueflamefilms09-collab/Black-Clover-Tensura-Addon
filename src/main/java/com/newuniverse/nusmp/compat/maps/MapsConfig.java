package com.newuniverse.nusmp.compat.maps;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the server side of the map-mod compatibility, {@code world/serverconfig/nusmp-compat-maps-server.toml}, section {@code [compat.maps]}. Client independent: the server
 * cannot know which map mod a client has, so the only switches are whether it prints waypoint lines at all, in which formats, and how often. Every reader goes through
 * {@link #get}, which answers the default while the spec is not loaded (a unit test, the first ticks).
 */
public final class MapsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHARE_WAYPOINTS, RAW_XAERO_STRING, SHARE_JOURNEYMAP_FORM, WARN_OPS;
    public static final ModConfigSpec.IntValue MAX_LINES_PER_MINUTE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("compat").push("maps");
        SHARE_WAYPOINTS = b.comment("The server prints clickable waypoint lines at the moments a place matters (a Gate found, a Return Waypoint saved, a rift opened, a safe room found). "
                        + "false = none are printed on their own (the /multiverse waypoint share commands still work). Each player can also switch it off with /multiverse waypoint share off.")
                .define("shareWaypoints", true);
        RAW_XAERO_STRING = b.comment("Put the raw Xaero's waypoint string (xaero-waypoint:...) after the readable text, in dark grey: a client with Xaero's Minimap / World Map turns it into an 'add waypoint' "
                        + "button. false = only the readable, click-to-copy-coordinates text (players with Xaero's then have to type the waypoint).")
                .define("rawXaeroString", true);
        SHARE_JOURNEYMAP_FORM = b.comment("Also print the JourneyMap chat form [name:\"..\", x:.., y:.., z:.., dim:..] (off by default: it is a second line for everybody).")
                .define("shareJourneyMapForm", false);
        MAX_LINES_PER_MINUTE = b.comment("The most automatic waypoint lines one player receives in a minute (more are dropped, not delayed).")
                .defineInRange("maxLinesPerMinute", 4, 1, 30);
        WARN_OPS = b.comment("Tell operators, once per login, when FTB Chunks is installed and its claim_dimension_blacklist does not cover this mod's dimensions.")
                .define("warnOps", true);
        b.pop(2);
        SPEC = b.build();
    }

    private MapsConfig() {}

    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException | NullPointerException notLoaded) { return v.getDefault(); }
    }
}
