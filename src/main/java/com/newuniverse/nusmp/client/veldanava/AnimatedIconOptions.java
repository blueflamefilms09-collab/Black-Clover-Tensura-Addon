package com.newuniverse.nusmp.client.veldanava;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client option of the Ultimate skill icons: config/nusmp-icons-client.toml, key {@code icons.ultimateStyle}. ANIMATED (default): the new painted, animated icons everywhere (skill lists and the
 * Will of the Creator cards); STILL: the new art as one still frame, no ticking texture; CLASSIC: the pre-3.17.0 32 px sprites (Sandalphon, Israfil, Jibril; Solomon and Hercules had none, so they show the still).
 * It only changes how the icons look, so it is a CLIENT config; the server never reads it.
 */
public final class AnimatedIconOptions {
    public enum Style { ANIMATED, STILL, CLASSIC }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<Style> STYLE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("icons");
        STYLE = b.comment("How the Ultimate skill icons (Sandalphon, Israfil, Jibril, Solomon, Hercules) are drawn: ANIMATED = the animated icons, STILL = the new art as one still frame, CLASSIC = the old 32 px sprites.")
                .defineEnum("ultimateStyle", Style.ANIMATED);
        b.pop();
        SPEC = b.build();
    }

    private AnimatedIconOptions() {}

    /** The chosen style; ANIMATED when the config is not loaded yet. */
    public static Style style() {
        try {
            return STYLE.get();
        } catch (IllegalStateException notLoaded) {
            return Style.ANIMATED;
        }
    }
}
