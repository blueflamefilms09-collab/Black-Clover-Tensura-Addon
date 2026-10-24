package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the pure rules of the Fire V2 look (docs/vfx_remake/fire_ingame.md) are what the painter, the CI self-test and the preview all read, so they are pinned here: which shapes the look owns (never the Calderos
 * eruption or the Demon Fire family), the cel / glow mix the owner chose per effect, the worst case of every effect against the peak of the code it replaced, the vertex budget against the activation limit, the
 * flip-book, the reduced-flashes rules (no hard flash, a soft fade that only falls, at most three frame changes a second, no pulse, no spin), the choice between the baked textures and the tinted grey twin, the
 * option text and the texture names.
 */
class FireV2Test {
    @Test
    void theLookOwnsExactlyTheElevenOldFireShapesAndNeverTheEruptionOrDemonFire() {
        assertEquals(Set.of(VfxShape.FIRE_LION, VfxShape.FIRE_SPEAR, VfxShape.FIRE_PILLAR, VfxShape.FIRE_BURST, VfxShape.FIRE_SPIRAL, VfxShape.FIRE_WILD, VfxShape.FLAMEFX_CAST, VfxShape.FLAMEFX_ZONE,
                VfxShape.FLAMEFX_BURST, VfxShape.FLAME_TRAIL, VfxShape.FLAME_EXPLOSION), new HashSet<>(FireV2.SHAPES));
        assertEquals(11, FireV2.SHAPES.size());
        for (VfxShape s : FireV2.UNTOUCHED) {
            assertFalse(FireV2.owns(s), s + " must keep its own layer in every setting");
            assertEquals(0, FireV2.worstQuads(s));
        }
        assertTrue(FireV2.UNTOUCHED.contains(VfxShape.FX_FIRE_ERUPTION) && FireV2.UNTOUCHED.contains(VfxShape.DEMON_FIRE_FX1));
    }

    @Test
    void theMixIsCelForTheHeroShapesAndGlowForSpiralCastAndTrail() {
        Set<VfxShape> glow = new HashSet<>();
        for (VfxShape s : FireV2.SHAPES) if (!FireV2.isCel(s)) glow.add(s);
        assertEquals(Set.of(VfxShape.FIRE_SPIRAL, VfxShape.FLAMEFX_CAST, VfxShape.FLAME_TRAIL), glow);
        assertTrue(FireV2.isCel(VfxShape.FIRE_LION) && FireV2.isCel(VfxShape.FIRE_WILD) && FireV2.isCel(VfxShape.FIRE_PILLAR) && FireV2.isCel(VfxShape.FLAMEFX_ZONE));
    }

    @Test
    void everyEffectStaysUnderThePeakOfTheCodeItReplacedAndTheActivationLimit() {
        assertEquals(22, FireV2.worstQuads(VfxShape.FIRE_LION));
        assertEquals(17, FireV2.worstQuads(VfxShape.FIRE_SPEAR));
        assertEquals(33, FireV2.worstQuads(VfxShape.FIRE_PILLAR));
        assertEquals(49, FireV2.worstQuads(VfxShape.FIRE_SPIRAL));
        assertEquals(69, FireV2.worstQuads(VfxShape.FIRE_WILD));
        assertEquals(31, FireV2.worstQuads(VfxShape.FLAMEFX_ZONE));
        for (VfxShape s : FireV2.SHAPES) {
            assertTrue(FireV2.worstQuads(s) > 0 && FireV2.worstQuads(s) <= FireV2.capQuads(s), s + ": " + FireV2.worstQuads(s) + " quads against the old peak " + FireV2.capQuads(s));
            assertEquals(FireV2.capQuads(s) * 4, FireV2.vertexBudget(s));
            assertTrue(FireV2.vertexBudget(s) <= 400, s + " budget over the activation limit");
        }
        assertEquals(66, FireV2.capQuads(VfxShape.FIRE_LION));
        assertEquals(93, FireV2.capQuads(VfxShape.FIRE_WILD));
        assertEquals(73, FireV2.capQuads(VfxShape.FLAME_EXPLOSION));
        assertEquals(0, FireV2.capQuads(VfxShape.WIND_SLASH));
    }

    @Test
    void theFlipBookHoldsAFrameTwoTicksAndWithReducedFlashesSevenAndNeverChangesMoreThanThreeTimesASecond() {
        assertEquals(2, FireV2.framePeriod(false));
        assertEquals(7, FireV2.framePeriod(true));
        StringBuilder sb = new StringBuilder();
        for (int a = 0; a < 8; a++) sb.append(FireV2.frame(a, 0, 3, false));
        assertEquals("00112200", sb.toString());
        assertEquals(1, FireV2.frame(0, 1, 3, false), "an offset staggers the sprites");
        assertEquals(0, FireV2.frame(-9, 0, 3, false), "a negative age is frame 0");
        assertEquals(2, FireV2.frame(0, -1, 3, false), "a negative offset wraps");
        assertEquals(0, FireV2.frame(6, 0, 3, true));
        assertEquals(1, FireV2.frame(7, 0, 3, true));
        assertTrue(FireV2.framesPerSecond(true) <= 3f, "at most three frame changes a second");
        assertEquals(10f, FireV2.framesPerSecond(false));
    }

    @Test
    void reducedFlashesDropTheHardFlashAndReplaceItWithASoftFadeThatOnlyFalls() {
        for (int age = 0; age < 12; age++) {
            assertEquals(0f, FireV2.hardFlash(age, 7, true), "no hard flash with reduced flashes");
            assertEquals(0f, FireV2.softFlash(age, 7, false), "no soft flash in the normal look");
            assertEquals(age < 7 ? 1f : 0f, FireV2.hardFlash(age, 7, false));
        }
        float prev = 1f;
        for (int age = 0; age < 30; age++) {
            float s = FireV2.softFlash(age, 7, true);
            assertTrue(s <= 0.5f && s >= 0f && s <= prev, "the soft fade starts at 0.5 and never rises (age " + age + ")");
            prev = s;
        }
        assertEquals(0.5f, FireV2.softFlash(0, 7, true));
        assertEquals(0f, FireV2.softFlash(-3, 7, true));
        assertEquals(0f, FireV2.spin(2.4f, true));
        assertEquals(2.4f, FireV2.spin(2.4f, false));
        for (float age = 0; age < 40; age += 0.37f) assertEquals(0.8f, FireV2.pulse(age, 0.8f, 0.4f, true), 1e-6, "no pulsing light");
        float lo = 9, hi = -9;
        for (float age = 0; age < 40; age += 0.1f) { float v = FireV2.pulse(age, 0.8f, 0.4f, false); lo = Math.min(lo, v); hi = Math.max(hi, v); }
        assertTrue(lo >= 0.6f - 1e-3 && hi <= 1f + 1e-3 && hi - lo > 0.35f, "the normal pulse swings 0.6 .. 1");
    }

    @Test
    void fireColoursKeepTheBakedTexturesAndEveryOtherColourIsTinted() {
        for (int c : new int[]{0xFFFF8A2A, 0xFFFF6A1E, 0xFFFFD86B, 0xFFE5401A, 0xFFFF3A1A, 0xFFFFB02E, 0xFFFFE05A, 0xFFFF2020}) assertTrue(FireV2.nativeFire(c), Integer.toHexString(c));
        for (int c : new int[]{0xFF5599FF, 0xFF55FF88, 0xFF1A1A1A, 0xFFFFFFFF, 0xFF808080, 0xFF9C5AFF, 0xFF30406A, 0xFFB8B8FF, 0xFF8CFFC2, 0xFFFF55D8})
            assertFalse(FireV2.nativeFire(c), Integer.toHexString(c) + " is a tint, not a fire colour");
    }

    @Test
    void theOptionsDefaultToTheNewLookWithFlashesOnAndParseV1() {
        assertEquals(FireV2.Style.V2, FireV2.DEFAULT_STYLE);
        assertFalse(FireV2.DEFAULT_REDUCED_FLASHES);
        assertEquals(FireV2.Style.V2, FireV2.parse(null));
        assertEquals(FireV2.Style.V2, FireV2.parse("nonsense"));
        assertEquals(FireV2.Style.V1, FireV2.parse("v1"));
        assertEquals(FireV2.Style.V1, FireV2.parse(" V1 "));
        assertEquals("nusmp-fire-v2-client.toml", FireV2.CONFIG_FILE);
        assertEquals("vfx.element.fire", FireV2.CONFIG_PATH);
        assertEquals("vfx.element.fireReducedFlashes", FireV2.CONFIG_PATH_REDUCED);
        FireV2.Style before = FireV2.style();
        boolean red = FireV2.reducedFlashes();
        try {
            FireV2.setStyle(FireV2.Style.V1);
            assertFalse(FireV2.v2Look());
            FireV2.setStyle(null);
            assertTrue(FireV2.v2Look(), "a missing value is the default");
            FireV2.setReducedFlashes(true);
            assertTrue(FireV2.reducedFlashes());
        } finally {
            FireV2.setStyle(before);
            FireV2.setReducedFlashes(red);
        }
    }

    @Test
    void theTextureNamesAreThirtyFourFilesAndThirtyTwoTwins() {
        List<String> all = FireV2.allTextures();
        assertEquals(34, FireV2.TEXTURE_NAMES.length);
        assertEquals(34, new HashSet<>(List.of(FireV2.TEXTURE_NAMES)).size());
        assertEquals(66, all.size());
        assertEquals(66, new HashSet<>(all).size());
        assertTrue(all.contains("textures/particle/fnw_a_tongue_a.png") && all.contains("textures/particle/fnw_b_halo_t.png") && all.contains("textures/particle/fnw_a_smoke_a.png"));
        assertFalse(all.contains("textures/particle/fnw_a_smoke_a_t.png"), "the smoke has no twin");
        assertEquals("fnw_a_orb_b_t", FireV2.fileName("a_orb_b", true));
        assertEquals("fnw_a_orb_b", FireV2.fileName("a_orb_b", false));
        assertTrue(FireV2.noTwin("a_smoke_b") && !FireV2.noTwin("b_halo"));
    }

    @Test
    void checkTextureCatchesWrongSizesVisibleCornersNonGreyTwinsAndEmptyFiles() {
        int[] px = new int[32 * 32];
        px[16 * 32 + 16] = 0xFFFFFFFF;
        assertNull(FireV2.checkTexture("b_ember", false, 32, 32, px));
        px[0] = 0x80FFFFFF;
        assertTrue(FireV2.checkTexture("b_ember", false, 32, 32, px).contains("corner"));
        px[0] = 0;
        assertTrue(FireV2.checkTexture("b_ember", false, 16, 16, new int[256]).contains("expected 32x32"));
        px[17 * 32 + 17] = 0xFFFF8040;
        assertTrue(FireV2.checkTexture("b_ember", true, 32, 32, px).contains("not grey"));
        assertTrue(FireV2.checkTexture("b_ember", false, 32, 32, new int[1024]).contains("empty"));
        assertNotNull(FireV2.size("a_lion_a"));
        assertEquals(512, FireV2.size("a_lion_a")[0]);
    }
}
