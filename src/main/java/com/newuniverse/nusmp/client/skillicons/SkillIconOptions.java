package com.newuniverse.nusmp.client.skillicons;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client options of the repainted skill icons: config/nusmp-skillicons-client.toml. {@code skillIcons.newStyle} (default true): the icons painted in the Tensura house style; false restores the icons of
 * 4.4.0 (textures/skill/classic/v440). {@code skillIcons.animate} (default true): the Ultimates play their short loop; false shows the still frame and nothing ticks. Both only change looks, so
 * they are CLIENT configs; the server never reads them.
 */
public final class SkillIconOptions {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue NEW_STYLE;
    public static final ModConfigSpec.BooleanValue ANIMATE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("skillIcons");
        NEW_STYLE = b.comment("true = the skill icons painted in the Tensura house style (class frame, shaded symbol); false = the icons of 4.4.0.")
                .define("newStyle", true);
        ANIMATE = b.comment("true = Ultimate skill icons play a short 8-frame loop (glow, glint, sparkles); false = they stay still. Ignored when newStyle is false.")
                .define("animate", true);
        b.pop();
        SPEC = b.build();
    }

    private SkillIconOptions() {}

    public static boolean newStyle() {
        try {
            return NEW_STYLE.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    public static boolean animate() {
        try {
            return ANIMATE.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }
}
