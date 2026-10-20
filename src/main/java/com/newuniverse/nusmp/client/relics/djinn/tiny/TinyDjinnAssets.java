package com.newuniverse.nusmp.client.relics.djinn.tiny;

import com.newuniverse.nusmp.relics.djinn.Djinn;

/**
 * Where the files of a Tiny Djinn live (paths under {@code assets/nusmp/}, as strings so the data test can read them without Minecraft) and what each must hold.
 * {@code tools/gen_tiny_djinn.py} writes exactly these; {@link TinyDjinnLayer} turns them into resource locations.
 */
public final class TinyDjinnAssets {
    private TinyDjinnAssets() {}

    /** The one animation file all 36 models share. */
    public static final String ANIMATION = "animations/entity/tinydjinn_anim.animation.json";

    /** The clips every model plays ({@link TinyDjinnActor#clip()} asks for these names). */
    public static final String[] CLIPS = {"idle", "cast", "attack", "hop", "poof", "exit"};

    /** The 25 bones every model has (so one animation file moves them all); empty ones are fine. */
    public static final String[] BONES = {"root", "body", "head", "lids", "hair", "crown", "brow", "ear_l", "ear_r", "horn_l", "horn_r", "dark", "arm_l", "hand_l", "arm_r", "hand_r",
            "weapon", "cape", "scarf", "back", "acc", "tail1", "tail2", "tail3", "tail4"};

    public static final int TEXTURE_SIZE = 64;
    /** A model has 55 to 175 cubes (the brief says about 60 to 150; the extra room is for the busiest). */
    public static final int MIN_CUBES = 55, MAX_CUBES = 175;

    public static String key(Djinn d) { return "tinydjinn_" + d.id(); }

    public static String model(Djinn d) { return "geo/entity/" + key(d) + ".geo.json"; }

    public static String texture(Djinn d, boolean dark) { return "textures/entity/" + key(d) + (dark ? "_dark" : "") + ".png"; }

    public static String glow(Djinn d, boolean dark) { return "textures/entity/" + key(d) + (dark ? "_dark" : "") + "_glow.png"; }

    /** The name floating over the figure: the Djinn's own name ("Alloces"). */
    public static String label(Djinn d) { return d.displayName(); }
}
