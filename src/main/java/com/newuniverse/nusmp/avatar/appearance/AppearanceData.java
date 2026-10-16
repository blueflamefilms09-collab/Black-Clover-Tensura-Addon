package com.newuniverse.nusmp.avatar.appearance;

import net.minecraft.nbt.CompoundTag;

/**
 * Why: the look of an Avatar has to travel three ways (the creation screen's page 3, the entity's synced data, the save file) and every one of them
 * must hold the same, small, validated shape, so a hand-edited packet or an old save can never hand the renderer an id that has no model. It is a
 * record of plain numbers stored as a CompoundTag (keys skin_tone, hair_id, outfit_id, horn_id, wing_id, scale, aura_rgb, eye_rgb). Horns and wings
 * are optional: id -1 means none. The counts are mirrored by the asset generator (tools/avatar/gen_avatar_client_assets.py); a unit test reads the
 * generated files and compares. Colours are 0xRRGGBB. Pure: no Minecraft class except NBT.
 */
public record AppearanceData(int skinTone, int hairId, int outfitId, int hornId, int wingId, float scale, int auraRgb, int eyeRgb) {
    public static final int HAIR_COUNT = 16, OUTFIT_COUNT = 12, HORN_COUNT = 8, WING_COUNT = 6;
    /** The id of "no horns" / "no wings". */
    public static final int NONE = -1;
    public static final float MIN_SCALE = 0.5f, MAX_SCALE = 2.0f;
    public static final int DEFAULT_SKIN = 0xE8C8A8, DEFAULT_AURA = 0x66CCFF, DEFAULT_EYE = 0x3366FF;

    public static AppearanceData defaults(String raceId, float raceScale, long seed) {
        return DynamicAppearanceEngine.defaultsFor(raceId, raceScale, seed);
    }

    /** True when every field is inside its range (a sanitised record always is). */
    public boolean valid() {
        return hairId >= 0 && hairId < HAIR_COUNT && outfitId >= 0 && outfitId < OUTFIT_COUNT && hornId >= NONE && hornId < HORN_COUNT
                && wingId >= NONE && wingId < WING_COUNT && scale >= MIN_SCALE && scale <= MAX_SCALE && !Float.isNaN(scale)
                && (skinTone & 0xFF000000) == 0 && (auraRgb & 0xFF000000) == 0 && (eyeRgb & 0xFF000000) == 0;
    }

    public AppearanceData withSkin(int rgb) { return new AppearanceData(rgb, hairId, outfitId, hornId, wingId, scale, auraRgb, eyeRgb); }
    public AppearanceData withHair(int id) { return new AppearanceData(skinTone, id, outfitId, hornId, wingId, scale, auraRgb, eyeRgb); }
    public AppearanceData withOutfit(int id) { return new AppearanceData(skinTone, hairId, id, hornId, wingId, scale, auraRgb, eyeRgb); }
    public AppearanceData withHorn(int id) { return new AppearanceData(skinTone, hairId, outfitId, id, wingId, scale, auraRgb, eyeRgb); }
    public AppearanceData withWing(int id) { return new AppearanceData(skinTone, hairId, outfitId, hornId, id, scale, auraRgb, eyeRgb); }
    public AppearanceData withScale(float s) { return new AppearanceData(skinTone, hairId, outfitId, hornId, wingId, s, auraRgb, eyeRgb); }
    public AppearanceData withAura(int rgb) { return new AppearanceData(skinTone, hairId, outfitId, hornId, wingId, scale, rgb, eyeRgb); }
    public AppearanceData withEye(int rgb) { return new AppearanceData(skinTone, hairId, outfitId, hornId, wingId, scale, auraRgb, rgb); }

    public CompoundTag toTag() {
        CompoundTag t = new CompoundTag();
        t.putInt("skin_tone", skinTone); t.putInt("hair_id", hairId); t.putInt("outfit_id", outfitId); t.putInt("horn_id", hornId);
        t.putInt("wing_id", wingId); t.putFloat("scale", scale); t.putInt("aura_rgb", auraRgb); t.putInt("eye_rgb", eyeRgb);
        return t;
    }

    /**
     * Reads a tag, tolerating missing keys (an old save, an empty synced value): a missing key takes its default, so no entity ever shows horns it
     * never had. It does not clamp; {@link DynamicAppearanceEngine#sanitize} does (the server runs it on everything a client sends).
     */
    public static AppearanceData fromTag(CompoundTag t) {
        if (t == null || t.isEmpty()) return defaults("", 1.0f, 0L);
        return new AppearanceData(
                t.contains("skin_tone") ? t.getInt("skin_tone") : DEFAULT_SKIN,
                t.getInt("hair_id"), t.getInt("outfit_id"),
                t.contains("horn_id") ? t.getInt("horn_id") : NONE,
                t.contains("wing_id") ? t.getInt("wing_id") : NONE,
                t.contains("scale") ? t.getFloat("scale") : 1.0f,
                t.contains("aura_rgb") ? t.getInt("aura_rgb") : DEFAULT_AURA,
                t.contains("eye_rgb") ? t.getInt("eye_rgb") : DEFAULT_EYE);
    }
}
