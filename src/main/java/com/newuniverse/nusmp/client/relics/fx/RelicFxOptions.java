package com.newuniverse.nusmp.client.relics.fx;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client options of the worn relic visuals: config/nusmp-relicfx-client.toml. {@code relicfx.quality} (low, medium, high) sets the vertex budget of the worn
 * models and their grade effects and how often the ambient particles spawn ({@link FxQuality}); {@code relicfx.particles} off stops the ambient particles
 * (the models and their glow stay); {@code relicfx.models} off hands every worn relic back to the old plain boxes; {@code relicfx.robeAnime} off wears the robe sets as the older lofted garments instead of the anime cloaks.
 * Registered once from the client section of the mod constructor:
 * {@code modContainer.registerConfig(ModConfig.Type.CLIENT, RelicFxOptions.SPEC, "nusmp-relicfx-client.toml")}.
 */
public final class RelicFxOptions {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> QUALITY;
    public static final ModConfigSpec.BooleanValue PARTICLES;
    public static final ModConfigSpec.BooleanValue MODELS;
    public static final ModConfigSpec.BooleanValue HIDE_ARMOUR_RELICS;
    public static final ModConfigSpec.BooleanValue ROBE_ANIME;
    public static final ModConfigSpec.BooleanValue ROBE_ANIME_BARE;
    public static final ModConfigSpec.BooleanValue HAND_MODELS;
    public static final ModConfigSpec.BooleanValue SHOW_WORN;
    public static final ModConfigSpec.BooleanValue CONTACT_FIT;
    public static final ModConfigSpec.ConfigValue<String> MODEL_DETAIL;
    public static final ModConfigSpec.BooleanValue CLASSIC_WORN;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("relicfx");
        QUALITY = b.comment("How much the worn relics show: 'low' (models and a faint rim only, at most about 2400 vertices per player), 'medium' (the default: rim, shimmer, orbiting motes, about 4200), "
                        + "'high' (rune rings, halos and denser particles, about 6000).")
                .define("quality", "medium", o -> o instanceof String s && (s.equals("low") || s.equals("medium") || s.equals("high")));   // not defineInList(List.of(..)): List.of(..).contains(null) throws when the file does not exist yet and aborts every config load
        PARTICLES = b.comment("Ambient particles drifting off worn relics (embers, snow, sparks, leaves, wisps, stars). Off = none.")
                .define("particles", true);
        MODELS = b.comment("Draw the worn relics as 3D models. Off = the plain boxes the mod had before.")
                .define("models", true);
        HIDE_ARMOUR_RELICS = b.comment("Hide the worn visuals (3D models, glow, particles) of relics worn as head, chest or feet armour. Curios relics have the eye icon of the Curios screen for this; "
                        + "armour has none, so this is its switch (also the 'Toggle relic visuals' key, unbound by default).")
                .define("hideArmourRelics", false);
        ROBE_ANIME = b.comment("Wear the Magic Knight robe sets as in the Black Clover anime: only the squad cloak (with its emblem, clasp and collar) on the chest piece, at most one small head piece, "
                        + "and nothing on the legs and feet. Off = the older lofted garments (hood, robe, sleeves, hip band, legs, boots).")
                .define("robeAnime", true);
        ROBE_ANIME_BARE = b.comment("Only with robeAnime on: true = the legs, boots and bare-headed pieces of a robe set draw nothing (the 1.48.0 look, only the cloak shows). "
                        + "false (default) = those pieces wear their older model (hip sash, legs, boots, hood), so helmet, leggings and boots show next to the cloak.")
                .define("robeAnimeBare", false);
        HAND_MODELS = b.comment("Show relics that are not weapons (rings, necklaces, hoods, boots, flasks, scrolls ...) as 3D objects in the hand, on the ground and in item frames. Off = the flat sprite, as before. "
                        + "The inventory always shows the sprite.")
                .define("handModels", true);
        SHOW_WORN = b.comment("Show the relics worn on the body (Curios and armour) on every player, yourself in third person and the inventory preview included. On by default. Off hides all of them at once; "
                        + "the Curios eye icon of one slot and relicfx.hideArmourRelics still switch single groups off while this is on. Also /multiverse relic show <on|off> and the 'Show or hide all worn relics' key.")
                .define("showWorn", true);
        CONTACT_FIT = b.comment("3.9.0: the smaller body fit of worn relics scales every model about its own middle instead of about the joint of its body part, so boots stay on the soles, gloves on the hands "
                        + "and a hem on the hips. Off = the 3.8 placement (models shrink toward the hip, shoulder or neck).")
                .define("contactFit", true);
        MODEL_DETAIL = b.comment("3.9.0: 'x3' = relics that have a blocky detailed model (relic_worn_x3) wear it when you are near and one of the nearest four wearers; 'classic' = always the 3.8 models. "
                        + "Capes, cloaks and mantles are never changed.")
                .define("modelDetail", "x3", o -> o instanceof String s && (s.equals("x3") || s.equals("classic")));
        CLASSIC_WORN = b.comment("3.29.0: draw the worn relics the way Multiverse 1.41.0 (build 213) did, where every worn relic showed on the player: no per-relic plan, every relic in a Curios or armour slot "
                        + "is drawn with its library model (else its plain box). The eye icon of a Curios slot, relicfx.hideArmourRelics and relicfx.showWorn still switch relics off. Off = the newer plan-driven layer.")
                .define("classicWorn", true);
        b.pop();
        SPEC = b.build();
    }

    private RelicFxOptions() {}

    public static FxQuality quality() {
        try { return FxQuality.byName(QUALITY.get(), FxQuality.MEDIUM); } catch (IllegalStateException notLoaded) { return FxQuality.MEDIUM; }
    }

    public static boolean particles() { return get(PARTICLES); }

    public static boolean models() { return get(MODELS); }

    /** True when the worn relics are drawn the 1.41.0 way ({@code relicfx.classicWorn}, default on). */
    public static boolean classicWorn() { return get(CLASSIC_WORN); }

    /**
     * The one client switch {@code visual.upgrade} of the whole visual upgrade (default on; true when the config is not loaded): the God worn relics (1.102.0) and the Charms / Genesis
     * relics (1.104.0) both read it, from {@code config/nusmp-visual-charms-client.toml} (the file the Charms slice released first). Off = every relic is worn and held as in 1.101.
     */
    public static boolean upgrade() { return com.newuniverse.nusmp.client.relics.vu.VuCharmsOptions.upgrade(); }

    /** True when the robe sets are worn as anime cloaks ({@code relicfx.robeAnime}, default on); true when the config is not loaded. */
    public static boolean robeAnime() { return get(ROBE_ANIME); }
    /** True when the legs, boots and bare-head pieces of an anime robe set draw nothing ({@code relicfx.robeAnimeBare}, default false). */
    public static boolean robeAnimeBare() { return get(ROBE_ANIME_BARE); }
    /** Relics that are not weapons as 3D objects in hand, on the ground and in frames ({@code relicfx.handModels}). */
    public static boolean handModels() { return get(HAND_MODELS); }

    /** True (default) when worn models are scaled about their own middle so they stay in contact with the body ({@code relicfx.contactFit}). */
    public static boolean contactFit() { return get(CONTACT_FIT); }

    /** True (default) when relics with a blocky detailed model wear it near the camera ({@code relicfx.modelDetail} = x3). */
    public static boolean detailX3() {
        try { return "x3".equals(MODEL_DETAIL.get()); } catch (IllegalStateException notLoaded) { return true; }
    }

    /** True when the worn visuals of armour relics are switched off ({@code relicfx.hideArmourRelics}); false when the config is not loaded. */
    public static boolean hideArmourRelics() {
        try { return HIDE_ARMOUR_RELICS.get(); } catch (IllegalStateException notLoaded) { return false; }
    }

    /** Flips {@code relicfx.hideArmourRelics} and saves it; returns the new value (false if the config cannot be written). */
    public static boolean toggleHideArmourRelics() {
        try {
            boolean now = !HIDE_ARMOUR_RELICS.get();
            HIDE_ARMOUR_RELICS.set(now);
            SPEC.save();
            return now;
        } catch (RuntimeException notLoaded) {
            return false;
        }
    }

    /** True (the default, and when the config is not loaded) unless the player switched all worn relics off ({@code relicfx.showWorn}). */
    public static boolean showWorn() { return get(SHOW_WORN); }

    /** Sets {@code relicfx.showWorn} and saves it; returns the value now in force (the old one if the config cannot be written). */
    public static boolean setShowWorn(boolean on) {
        try {
            SHOW_WORN.set(on);
            SPEC.save();
            return on;
        } catch (RuntimeException notLoaded) {
            return showWorn();
        }
    }

    /** Flips {@code relicfx.showWorn}; returns the new value. */
    public static boolean toggleShowWorn() { return setShowWorn(!showWorn()); }

    private static boolean get(ModConfigSpec.BooleanValue v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return true; }
    }
}
