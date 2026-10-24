package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the pure rules of the Wind pixel look (docs/vfx_remake/wind_mc_ingame.md) are what the painter, the vertex buffer and the CI self-test all read, so they are pinned here: which shapes the look owns
 * (never the Tornado or Spirit of Zephyr), the worst case of every effect against the peak of the code it replaced, the vertex budget against the activation limit, the stepped timing (on twos,
 * a flip-book frame every three ticks, four alpha steps), the choice between the pack palette and the tinted grey twin, the option text, and the texture names.
 */
class WindMcTest {
    @Test
    void theLookOwnsExactlyFiveShapesAndNeverTheTornadoOrZephyr() {
        assertEquals(Set.of(VfxShape.WIND_SLASH, VfxShape.WIND_RING, VfxShape.WIND_EMPEROR, VfxShape.WIND_GALE, VfxShape.FX_WIND_GUST), new HashSet<>(WindMc.SHAPES));
        assertEquals(5, WindMc.SHAPES.size());
        for (VfxShape s : WindMc.UNTOUCHED) assertFalse(WindMc.owns(s), s + " must keep its old layer in every setting");
        assertEquals(List.of(VfxShape.WIND_TORNADO, VfxShape.WIND_ZEPHYR), WindMc.UNTOUCHED);
        assertEquals(0, WindMc.worstQuads(VfxShape.WIND_TORNADO));
        assertEquals(0, WindMc.worstQuads(VfxShape.WIND_ZEPHYR));
    }

    @Test
    void everyEffectStaysUnderThePeakOfTheCodeItReplacedAndTheActivationLimit() {
        assertEquals(8, WindMc.worstQuads(VfxShape.WIND_SLASH));
        assertEquals(18, WindMc.worstQuads(VfxShape.WIND_EMPEROR));
        assertEquals(16, WindMc.worstQuads(VfxShape.WIND_GALE));
        assertEquals(16, WindMc.worstQuads(VfxShape.FX_WIND_GUST));
        assertEquals(13, WindMc.worstQuads(VfxShape.WIND_RING));
        for (VfxShape s : WindMc.SHAPES) {
            assertTrue(WindMc.worstQuads(s) > 0 && WindMc.worstQuads(s) <= WindMc.capQuads(s) / 3, s + ": the pixel look is a fraction of the old cap");
            assertEquals(WindMc.capQuads(s) * 4, WindMc.vertexBudget(s));
            assertTrue(WindMc.vertexBudget(s) <= 400, s + " budget over the activation limit");
        }
        assertEquals(72, WindMc.capQuads(VfxShape.WIND_SLASH));
        assertEquals(80, WindMc.capQuads(VfxShape.WIND_EMPEROR));
        assertEquals(100, WindMc.capQuads(VfxShape.WIND_RING));
    }

    @Test
    void theTimingIsSteppedOnTwosWithATwoFrameFlipBookEveryThreeTicks() {
        for (int age = 0; age < 40; age++) {
            assertEquals(0, WindMc.onTwos(age) % 2);
            assertTrue(WindMc.onTwos(age) == age || WindMc.onTwos(age) == age - 1);
        }
        int[] frames = new int[12];
        for (int i = 0; i < 12; i++) frames[i] = WindMc.frame(i);
        assertEquals("0,0,0,1,1,1,0,0,0,1,1,1", java.util.Arrays.stream(frames).mapToObj(String::valueOf).reduce((a, b) -> a + "," + b).orElse(""));
        assertEquals(0, WindMc.frame(-5));
        Set<Float> steps = new HashSet<>();
        for (float a = -0.2f; a <= 1.3f; a += 0.013f) steps.add(WindMc.step4(a));
        assertEquals(Set.of(0f, 0.25f, 0.5f, 0.75f, 1f), steps, "alpha has four steps and nothing in between");
        assertEquals(0.5f, WindMc.fade(0, 16, 2, 5));
        assertEquals(1f, WindMc.fade(8, 16, 2, 5));
        assertEquals(0f, WindMc.fade(16, 16, 2, 5));
        assertEquals(0xFF_FFFFFF, WindMc.white(1f));
        assertEquals(0x80_FFFFFF, WindMc.white(0.5f));
    }

    @Test
    void windGreensKeepThePackPaletteAndEveryOtherColourIsTinted() {
        for (int c : new int[]{0xFF8CFFC2, 0xFFA6FFD6, 0xFF00FF00, 0xFF7CC46A, 0xFF55FF88}) assertTrue(WindMc.nativePalette(c), Integer.toHexString(c));
        for (int c : new int[]{0xFFD8E2FF, 0xFFFF8C1A, 0xFFE0451A, 0xFF1A1A1A, 0xFFFFFFFF, 0xFF808080, 0xFFC8D2E0, 0xFFFFD86B, 0xFF9C5AFF, 0xFF30406A})
            assertFalse(WindMc.nativePalette(c), Integer.toHexString(c) + " is a tint, not a wind green");
    }

    @Test
    void theOptionDefaultsToThePixelLookAndParsesV1() {
        assertEquals(WindMc.Style.MC_C, WindMc.DEFAULT_STYLE);
        assertEquals(WindMc.Style.MC_C, WindMc.parse(null));
        assertEquals(WindMc.Style.MC_C, WindMc.parse("nonsense"));
        assertEquals(WindMc.Style.V1, WindMc.parse("v1"));
        assertEquals(WindMc.Style.V1, WindMc.parse(" V1 "));
        assertEquals(WindMc.Style.MC_C, WindMc.parse("mc_c"));
        assertEquals("nusmp-wind-mc-client.toml", WindMc.CONFIG_FILE);
        assertEquals("vfx.element.wind", WindMc.CONFIG_PATH);
        WindMc.Style before = WindMc.style();
        try {
            WindMc.setStyle(WindMc.Style.V1);
            assertFalse(WindMc.pixelLook());
            WindMc.setStyle(null);
            assertTrue(WindMc.pixelLook(), "a missing value is the default");
        } finally {
            WindMc.setStyle(before);
        }
    }

    @Test
    void theTextureNamesAreTheThirtyTwoFilesAndOnlyWmcTexturesAreSampledHard() {
        List<String> all = WindMc.allTextures();
        assertEquals(32, all.size());
        assertEquals(32, new HashSet<>(all).size());
        assertTrue(all.contains("textures/particle/wmc_crescent_0.png") && all.contains("textures/particle/wmc_flash_1_t.png"));
        for (String p : all) assertTrue(WindMc.isPixelTexture(p), p);
        assertTrue(WindMc.isPixelTexture("nusmp:textures/particle/wmc_ring_0.png"));
        for (String p : new String[]{"textures/particle/wind_slash.png", "textures/particle/wind_swallow.png", "textures/particle/time_clock.png", "textures/entity/wmc_x.png", "", null})
            assertFalse(WindMc.isPixelTexture(p), String.valueOf(p));
        assertEquals("wmc_ribbon_1_t", WindMc.fileName("ribbon", 1, true));
        assertEquals("wmc_ribbon_1", WindMc.fileName("ribbon", 1, false));
        assertEquals(5, WindMc.PACK_PALETTE.length);
        assertEquals(5, WindMc.GREY_PALETTE.length);
    }

    @Test
    void checkTextureCatchesSoftAlphaOffPaletteColoursAndWrongSizes() {
        int[] px = new int[16 * 16];
        px[5 * 16 + 5] = 0xFF000000 | WindMc.PACK_PALETTE[0];
        assertEquals(null, WindMc.checkTexture("spark", false, 16, 16, px));
        px[6 * 16 + 6] = 0x80000000 | WindMc.PACK_PALETTE[1];
        assertTrue(WindMc.checkTexture("spark", false, 16, 16, px).contains("soft alpha"));
        px[6 * 16 + 6] = 0xFF123456;
        assertTrue(WindMc.checkTexture("spark", false, 16, 16, px).contains("outside its palette"));
        px[6 * 16 + 6] = 0;
        assertTrue(WindMc.checkTexture("spark", false, 8, 8, new int[64]).contains("expected 16x16"));
        px[0] = 0xFF000000 | WindMc.PACK_PALETTE[2];
        assertTrue(WindMc.checkTexture("spark", false, 16, 16, px).contains("opaque corner"));
        assertTrue(WindMc.checkTexture("spark", true, 16, 16, new int[256]).contains("empty"));
    }
}
