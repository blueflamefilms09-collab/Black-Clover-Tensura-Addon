package com.newuniverse.nusmp.avatar.appearance;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/** The Avatar's look: the tag round trip, what the server clamps, the seeded default, and that the id ranges match the assets the generator wrote. */
class AppearanceTest {
    @Test
    void aLookSurvivesTheTagRoundTrip() {
        AppearanceData a = new AppearanceData(0x112233, 5, 7, 3, 2, 1.25f, 0xABCDEF, 0x123456);
        assertEquals(a, AppearanceData.fromTag(a.toTag()));
    }

    @Test
    void missingKeysTakeDefaultsAndNeverInventHornsOrWings() {
        CompoundTag t = new CompoundTag();
        t.putInt("hair_id", 3);
        AppearanceData a = AppearanceData.fromTag(t);
        assertEquals(AppearanceData.NONE, a.hornId());
        assertEquals(AppearanceData.NONE, a.wingId());
        assertEquals(1.0f, a.scale());
        assertEquals(3, a.hairId());
    }

    @Test
    void anEmptyTagGivesTheDefaultLook() {
        assertTrue(AppearanceData.fromTag(new CompoundTag()).valid());
        assertTrue(AppearanceData.fromTag(null).valid());
    }

    @Test
    void sanitizeClampsEveryField() {
        AppearanceData wild = new AppearanceData(0xFF123456, 99, -4, 40, -9, 40f, 0x7F000000 | 0xAABBCC, 0xFFFFFFFF);
        AppearanceData ok = DynamicAppearanceEngine.sanitize(wild, 1.0f);
        assertTrue(ok.valid(), ok.toString());
        assertEquals(AppearanceData.HAIR_COUNT - 1, ok.hairId());
        assertEquals(0, ok.outfitId());
        assertEquals(AppearanceData.HORN_COUNT - 1, ok.hornId());
        assertEquals(AppearanceData.NONE, ok.wingId());
        assertEquals(1.15f, ok.scale(), 1e-6);
        assertEquals(0x123456, ok.skinTone());
        assertEquals(0xAABBCC, ok.auraRgb());
        assertEquals(0xFFFFFF, ok.eyeRgb());
    }

    @Test
    void theScaleStaysNearTheRaceSize() {
        AppearanceData big = new AppearanceData(0, 0, 0, -1, -1, 5f, 0, 0), small = big.withScale(0.1f);
        assertEquals(1.5f * 1.15f, DynamicAppearanceEngine.sanitize(big, 1.5f).scale(), 1e-5);
        assertEquals(1.5f * 0.85f, DynamicAppearanceEngine.sanitize(small, 1.5f).scale(), 1e-5);
        assertEquals(0.85f * 0.85f, DynamicAppearanceEngine.sanitize(small, 0.85f).scale(), 1e-5);
        assertEquals(1.0f, DynamicAppearanceEngine.scaleFor(Float.NaN, 0f, 9f));
        assertEquals(1.0f, DynamicAppearanceEngine.scaleFor(-3f, 0f, 9f));
    }

    @Test
    void nanScaleIsRepaired() {
        AppearanceData a = new AppearanceData(0, 0, 0, -1, -1, Float.NaN, 0, 0);
        assertTrue(DynamicAppearanceEngine.sanitize(a, 1.0f).valid());
        assertTrue(DynamicAppearanceEngine.sanitize(null, 1.0f).valid());
    }

    @Test
    void sanitizeIsIdempotent() {
        AppearanceData once = DynamicAppearanceEngine.sanitize(new AppearanceData(1, 20, 20, 20, 20, 9f, -1, -1), 0.95f);
        assertEquals(once, DynamicAppearanceEngine.sanitize(once, 0.95f));
    }

    @Test
    void seededDefaultsAreStableAndValid() {
        for (long seed = 0; seed < 200; seed++) {
            AppearanceData a = DynamicAppearanceEngine.defaultsFor("ogre", 1.4f, seed);
            assertTrue(a.valid());
            assertEquals(a, DynamicAppearanceEngine.defaultsFor("ogre", 1.4f, seed), "same seed, same Avatar");
            assertEquals(1.4f, a.scale(), 1e-6);
            assertEquals(AppearanceData.NONE, a.hornId());
        }
        boolean differs = false;
        for (long seed = 1; seed < 50; seed++) differs |= !DynamicAppearanceEngine.defaultsFor("elf", 0.95f, 0).equals(DynamicAppearanceEngine.defaultsFor("elf", 0.95f, seed));
        assertTrue(differs, "seeds must vary the look");
    }

    @Test
    void raceGroupsFollowTheRaceName() {
        assertEquals(AvatarRaceGroup.BULKY, AvatarRaceGroup.of("nusmp:ogre"));
        assertEquals(AvatarRaceGroup.BULKY, AvatarRaceGroup.of("Orc_Disaster"));
        assertEquals(AvatarRaceGroup.SMALL, AvatarRaceGroup.of("goblin"));
        assertEquals(AvatarRaceGroup.HUMAN, AvatarRaceGroup.of("demon_host"));
        assertEquals(AvatarRaceGroup.HUMAN, AvatarRaceGroup.of(null));
    }

    /** The counts in AppearanceData are the contract with tools/avatar/gen_avatar_client_assets.py: every id has its model and texture. */
    @Test
    void everyIdHasItsAsset() {
        File root = new File("src/main/resources/assets/nusmp");
        if (!root.isDirectory()) return;                       // run from another folder: nothing to compare against
        for (int i = 0; i < AppearanceData.HAIR_COUNT; i++) assertAsset(root, "avatar_hair_" + i);
        for (int i = 0; i < AppearanceData.HORN_COUNT; i++) assertAsset(root, "avatar_horn_" + i);
        for (int i = 0; i < AppearanceData.WING_COUNT; i++) assertAsset(root, "avatar_wing_" + i);
        for (int i = 0; i < AppearanceData.OUTFIT_COUNT; i++) assertTrue(new File(root, "textures/entity/avatar/outfit_" + i + ".png").isFile(), "outfit " + i);
        for (AvatarRaceGroup g : AvatarRaceGroup.values()) {
            assertTrue(new File(root, "textures/entity/avatar/base_" + g.file + ".png").isFile(), g.file);
            assertTrue(new File(root, "textures/entity/avatar/base_" + g.file + "_mask.png").isFile(), g.file + " mask");
        }
        assertAsset(root, "avatar_aura_ring");
        assertFalse(new File(root, "geo/entity/avatar_hair_" + AppearanceData.HAIR_COUNT + ".geo.json").exists(), "more hair files than HAIR_COUNT");
    }

    private static void assertAsset(File root, String id) {
        assertTrue(new File(root, "geo/entity/" + id + ".geo.json").isFile(), id + " geo");
        assertTrue(new File(root, "textures/entity/" + id + ".png").isFile(), id + " texture");
        assertTrue(new File(root, "textures/entity/" + id + "_glow.png").isFile(), id + " glow");
    }
}
