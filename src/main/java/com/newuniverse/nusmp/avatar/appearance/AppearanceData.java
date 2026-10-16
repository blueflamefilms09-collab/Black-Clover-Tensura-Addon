package com.newuniverse.nusmp.avatar.appearance;

import net.minecraft.nbt.CompoundTag;

/**
 * Contract stub (AV3 replaces the body, keeps every signature): the look of an Avatar, stored as a CompoundTag on the entity (synced) and chosen on page 3 of the
 * creation GUI. Keys: skin_tone (rgb int), hair_id, outfit_id, horn_id, wing_id, scale (float), aura_rgb (int), eye_rgb (int). Pure (no Minecraft except NBT).
 */
public record AppearanceData(int skinTone, int hairId, int outfitId, int hornId, int wingId, float scale, int auraRgb, int eyeRgb) {
    public static AppearanceData defaults(String raceId, float raceScale, long seed) {
        return new AppearanceData(0xE8C8A8, 0, 0, 0, 0, raceScale <= 0 ? 1.0f : raceScale, 0x66CCFF, 0x3366FF);
    }

    public CompoundTag toTag() {
        CompoundTag t = new CompoundTag();
        t.putInt("skin_tone", skinTone); t.putInt("hair_id", hairId); t.putInt("outfit_id", outfitId); t.putInt("horn_id", hornId);
        t.putInt("wing_id", wingId); t.putFloat("scale", scale); t.putInt("aura_rgb", auraRgb); t.putInt("eye_rgb", eyeRgb);
        return t;
    }

    public static AppearanceData fromTag(CompoundTag t) {
        if (t == null || t.isEmpty()) return defaults("", 1.0f, 0L);
        return new AppearanceData(t.getInt("skin_tone"), t.getInt("hair_id"), t.getInt("outfit_id"), t.getInt("horn_id"), t.getInt("wing_id"),
                t.contains("scale") ? t.getFloat("scale") : 1.0f, t.getInt("aura_rgb"), t.getInt("eye_rgb"));
    }
}
