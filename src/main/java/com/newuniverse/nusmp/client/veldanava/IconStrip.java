package com.newuniverse.nusmp.client.veldanava;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;

/**
 * Why: the pure arithmetic of the animated Ultimate skill icons (3.17.0), kept free of game classes so a plain unit test can check it. An icon strip is one PNG with the frames stacked
 * top to bottom (as many frames as the height divides by the width); the {@code .mcmeta} next to it says how many game ticks one frame lasts (the vanilla {@code animation.frametime} key, default 2).
 */
public final class IconStrip {
    private IconStrip() {}

    /** The five Ultimate skills, in the order of the Will of the Creator cards. */
    public static final List<String> IDS = List.of("sandalphon", "israfil", "jibril", "solomon", "hercules");
    /** The skills that had an icon before 3.17.0 (their old sprite is kept as {@code <id>_classic.png}); Solomon and Hercules had none. */
    public static final List<String> WITH_CLASSIC = List.of("sandalphon", "israfil", "jibril");

    public static final int DEFAULT_FRAMETIME = 2;
    /** More frames than this is not a strip we draw (the shipped ones have 32). */
    public static final int MAX_FRAMES = 128;

    private static final String BASE = "textures/skill/veldanava/";

    /** The still hero frame; the path the skill itself names as its icon, and the one the game swaps for the animation. */
    public static String stillPath(String id) { return BASE + id + ".png"; }

    /** The 32 px strip (32 x 32N). */
    public static String stripPath(String id) { return BASE + id + "_anim.png"; }

    /** The 128 px strip (128 x 128N), drawn on the card screen. */
    public static String hdStripPath(String id) { return BASE + id + "_anim_hd.png"; }

    /** The pre-3.17.0 sprite. */
    public static String classicPath(String id) { return BASE + id + "_classic.png"; }

    /** How many square frames a strip of this size holds; 0 when it is not a strip (not a whole number of frames, wider than tall, or too many). */
    public static int frames(int width, int height) {
        if (width <= 0 || height < width || height % width != 0) return 0;
        int n = height / width;
        return n > MAX_FRAMES ? 0 : n;
    }

    /** The frame to show after {@code ticks} ticks of a loop of {@code frames} frames of {@code frameTime} ticks each. */
    public static int frameAt(long ticks, int frames, int frameTime) {
        if (frames <= 1) return 0;
        int ft = Math.max(1, frameTime);
        return (int) ((Math.max(0L, ticks) / ft) % frames);
    }

    /** {@code animation.frametime} of a vanilla {@code .mcmeta}, clamped to 1..40; the default (2) when the text is missing or not that shape. */
    public static int frameTime(String mcmeta) {
        if (mcmeta == null || mcmeta.isBlank()) return DEFAULT_FRAMETIME;
        try {
            JsonObject root = JsonParser.parseString(mcmeta).getAsJsonObject();
            JsonObject anim = root.getAsJsonObject("animation");
            if (anim == null || !anim.has("frametime")) return DEFAULT_FRAMETIME;
            int v = anim.get("frametime").getAsInt();
            return Math.max(1, Math.min(40, v));
        } catch (RuntimeException notJson) {
            return DEFAULT_FRAMETIME;
        }
    }

    /** True for the five ids (the packet-free whitelist of what this client code touches). */
    public static boolean known(String id) { return id != null && IDS.contains(id); }
}
