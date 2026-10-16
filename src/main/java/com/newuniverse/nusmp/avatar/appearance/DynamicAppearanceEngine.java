package com.newuniverse.nusmp.avatar.appearance;

import com.newuniverse.nusmp.avatar.race.AvatarRace;

import java.util.Random;

/**
 * Why: the creation screen sends a look chosen on the client, and a client can send anything (a scale of 40, a hair id with no model, a colour with
 * stray high bits). This is the one place that turns whatever arrived into a look the renderer can draw, clamped to the race (an ogre is tall, a goblin
 * small, nobody is more than 15 percent off their race's size), and the one place that makes the pleasant seeded default a new Avatar starts from.
 * Pure: the server calls it on every request, the screen calls it for its live preview, tests call it directly.
 */
public final class DynamicAppearanceEngine {
    private DynamicAppearanceEngine() {}

    /** How far a creator may move the size from the race's own, as a fraction. */
    public static final float SCALE_SPREAD = 0.15f;

    /** The curated colours a default Avatar picks from (so a default never looks like random noise). */
    public static final int[] SKIN_PALETTE = {0xF6DCC8, 0xE8C8A8, 0xD9AE86, 0xC08A60, 0x8E5E3E, 0x5E3C28, 0xF0D0D8, 0xC8E0C0, 0x9CC0A0, 0x8AA0C8,
            0xB8A0D8, 0xD88888, 0xA0A0A8, 0xE8E0C0, 0x70A878, 0x68A0B8};
    public static final int[] AURA_PALETTE = {0x66CCFF, 0xFFD060, 0xFF6A6A, 0x9A7CFF, 0x7CF0B0, 0xFFFFFF, 0xFF8AD8, 0xFF9A40};
    public static final int[] EYE_PALETTE = {0x3366FF, 0x22AA66, 0xCC3344, 0xDDAA22, 0x9955DD, 0x44CCDD, 0x884422, 0xCCCCDD};

    /** The race's own size clamped into [min, max]; a non-positive or NaN size is 1.0. */
    public static float scaleFor(float raceScale, float minScale, float maxScale) {
        float s = Float.isNaN(raceScale) || raceScale <= 0f ? 1.0f : raceScale;
        float lo = Math.max(AppearanceData.MIN_SCALE, minScale), hi = Math.min(AppearanceData.MAX_SCALE, Math.max(lo, maxScale));
        return Math.min(hi, Math.max(lo, s));
    }

    public static float minScale(float raceScale) { return scaleFor(raceScale, 0f, 99f) * (1f - SCALE_SPREAD); }

    public static float maxScale(float raceScale) { return scaleFor(raceScale, 0f, 99f) * (1f + SCALE_SPREAD); }

    /** Clamps every field; the scale to the race's size plus or minus 15 percent. */
    public static AppearanceData sanitize(AppearanceData in, float raceScale) {
        if (in == null) return defaultsFor("", raceScale, 0L);
        float lo = Math.max(AppearanceData.MIN_SCALE, minScale(raceScale)), hi = Math.min(AppearanceData.MAX_SCALE, maxScale(raceScale));
        float sc = Float.isNaN(in.scale()) ? scaleFor(raceScale, lo, hi) : Math.min(hi, Math.max(lo, in.scale()));
        return new AppearanceData(in.skinTone() & 0xFFFFFF, clamp(in.hairId(), 0, AppearanceData.HAIR_COUNT - 1),
                clamp(in.outfitId(), 0, AppearanceData.OUTFIT_COUNT - 1), clamp(in.hornId(), AppearanceData.NONE, AppearanceData.HORN_COUNT - 1),
                clamp(in.wingId(), AppearanceData.NONE, AppearanceData.WING_COUNT - 1), sc, in.auraRgb() & 0xFFFFFF, in.eyeRgb() & 0xFFFFFF);
    }

    /** The same for a race record (what the server holds). */
    public static AppearanceData sanitize(AppearanceData in, AvatarRace race) {
        return sanitize(in, race == null ? 1.0f : race.defaultScale());
    }

    /** A pleasant look from a seed: the same seed always gives the same Avatar; no horns or wings, a plain outfit. */
    public static AppearanceData defaultsFor(String raceId, float raceScale, long seed) {
        Random r = new Random(seed * 0x9E3779B97F4A7C15L + (raceId == null ? 0 : raceId.hashCode()));
        float sc = scaleFor(raceScale, 0f, 99f);
        return new AppearanceData(SKIN_PALETTE[r.nextInt(SKIN_PALETTE.length)], r.nextInt(AppearanceData.HAIR_COUNT), r.nextInt(3), AppearanceData.NONE,
                AppearanceData.NONE, sc, AURA_PALETTE[r.nextInt(AURA_PALETTE.length)], EYE_PALETTE[r.nextInt(EYE_PALETTE.length)]);
    }

    private static int clamp(int v, int lo, int hi) { return Math.min(hi, Math.max(lo, v)); }
}
