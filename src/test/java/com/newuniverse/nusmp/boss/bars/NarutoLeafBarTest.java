package com.newuniverse.nusmp.boss.bars;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the second Naruto bar ({@code naruto_leaf}, docs/bosses/kakashi.md) is appended next to the first, never instead of it, and nobody can see it in a game from here. What a machine can prove is
 * proved here: both styles ship with their art, the server still names {@code naruto} (the client swaps the new look in behind a setting, so an old client or server still meets a known id), the
 * three new emblems and the new fill effect are appended to the lists (the old cells and rows did not move), the frame has its three mounting plates exactly under the phase notches the
 * painter draws, the painter reads the emblem sheet height from the list instead of the old literal, and the setting is a CLIENT option that defaults on.
 */
class NarutoLeafBarTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/anime_multiverse");
    private static final Path TEX = ASSETS.resolve("textures/gui/boss_bar");
    private static final Path CLIENT = Path.of("src/main/java/com/newuniverse/nusmp/client/gui/bossbar");

    @Test
    void bothNarutoBarsShipAndTheServerStillNamesTheOldOne() throws IOException {
        assertTrue(BarStyleIds.ALL.contains(BarStyleIds.NARUTO) && BarStyleIds.ALL.contains(BarStyleIds.NARUTO_LEAF));
        assertEquals(BarStyleIds.NARUTO_LEAF, BarStyleIds.ALL.get(BarStyleIds.ALL.size() - 1), "appended last");
        assertEquals(java.util.List.of(BarStyleIds.NARUTO_LEAF), BarStyleIds.VARIANTS);
        assertEquals(BarStyleIds.NARUTO, BarStyleResolver.forEntityPath("kakashi_hatake"), "the server's id is unchanged: the client swaps the look");
        assertEquals(BarStyleIds.NARUTO, BarStyleResolver.forArc("allies", "kakashi_hatake"));
        BarStyle old = BarStyle.parse(Files.readString(ASSETS.resolve("boss_bars/naruto.json")));
        BarStyle neu = BarStyle.parse(Files.readString(ASSETS.resolve("boss_bars/naruto_leaf.json")));
        assertEquals("sharingan", old.emblem(), "the first bar keeps its art");
        assertEquals("sharingan_v2", neu.emblem());
        assertEquals("chidori", neu.fx());
        assertEquals("dissolve", neu.death());
        assertTrue(Files.size(TEX.resolve("frame_naruto.png")) > 100 && Files.size(TEX.resolve("frame_naruto_leaf.png")) > 100);
    }

    @Test
    void theNewEmblemsAndTheNewFillEffectAreAppendedAndTheOldOnesDidNotMove() {
        int n = BarStyle.EMBLEMS.size();
        assertEquals("sharingan", BarStyle.EMBLEMS.get(36));
        assertEquals("mangekyo", BarStyle.EMBLEMS.get(37));
        assertEquals("dragon_ball", BarStyle.EMBLEMS.get(38));
        assertEquals("vessel_star", BarStyle.EMBLEMS.get(39));
        assertEquals(java.util.List.of("sharingan_v2", "mangekyo_v2", "tomoe"), BarStyle.EMBLEMS.subList(40, n));
        assertEquals(43, n);
        assertEquals("chidori", BarStyle.FX.get(BarStyle.FX.size() - 1));
        assertEquals("shield", BarStyle.FX.get(12), "the older rows keep their numbers");
    }

    @Test
    void theFrameHasAMountingPlateUnderEachNotchAndTheWindowAndSocketAreClear() throws IOException {
        BufferedImage im = ImageIO.read(TEX.resolve("frame_naruto_leaf.png").toFile());
        assertEquals(BarLayoutMath.ATLAS_W, im.getWidth());
        assertEquals(BarLayoutMath.ATLAS_H, im.getHeight());
        for (int percent : new int[]{25, 50, 75}) {
            int mx = BarLayoutMath.WIN_X + BarLayoutMath.markerX(percent) + 1;                 // the middle of the notch's two columns, where the painter centres the tomoe
            int y = BarLayoutMath.LAYER_Y[1] + 39 * 2;
            assertTrue((im.getRGB(mx * 2, y) >>> 24) > 200, "no mounting plate under the " + percent + "% notch");
            assertTrue((im.getRGB(mx * 2 - 5, y) >>> 24) > 100 && (im.getRGB(mx * 2 + 5, y) >>> 24) > 100, "the plate is wider than a pixel at " + percent);
        }
        for (int x = BarLayoutMath.WIN_X * 2 + 4; x < (BarLayoutMath.WIN_X + BarLayoutMath.WIN_W) * 2 - 4; x += 5)
            assertEquals(0, im.getRGB(x, BarLayoutMath.LAYER_Y[1] + (BarLayoutMath.WIN_Y + BarLayoutMath.WIN_H / 2) * 2) >>> 24, "window covered at " + x);
        assertEquals(0, im.getRGB(BarLayoutMath.SOCK_X * 2, BarLayoutMath.LAYER_Y[1] + BarLayoutMath.SOCK_Y * 2) >>> 24);
    }

    @Test
    void theChidoriRowDrawsOnlyWhereThePainterLooksAndIsTileable() throws IOException {
        BufferedImage fx = ImageIO.read(TEX.resolve("fillfx.png").toFile());
        int row = BarStyle.FX.indexOf("chidori") * BarLayoutMath.FX_ROW_H;
        assertEquals(BarLayoutMath.FX_ROW_H * BarStyle.FX.size(), fx.getHeight());
        int lit = 0, outside = 0, wrap = 0;
        for (int y = 0; y < BarLayoutMath.FX_ROW_H; y++) {
            for (int x = 0; x < BarLayoutMath.FX_W; x++) {
                int a = fx.getRGB(x, row + y) >>> 24;
                if (a > 40) {
                    if (y >= 3 && y <= 12) lit++;
                    else outside++;
                }
            }
        }
        for (int y = 3; y <= 12; y++) if ((fx.getRGB(0, row + y) >>> 24) > 40 || (fx.getRGB(BarLayoutMath.FX_W - 1, row + y) >>> 24) > 40) wrap++;
        assertTrue(lit > 60, "the arcs are drawn: " + lit);
        assertEquals(0, outside, "the painter shows rows 3..12 of the tile only");
        assertTrue(wrap > 0, "the arcs reach both edges, so the tile joins up");
    }

    @Test
    void thePainterReadsTheSheetHeightFromTheListAndTheSettingIsAClientOptionThatDefaultsOn() throws IOException {
        String painter = Files.readString(CLIENT.resolve("CustomBarPainter.java"));
        assertFalse(painter.contains("256, 160)"), "the emblem sheet grew: no literal height may be left");
        assertTrue(painter.contains("BarStyles.SHEET_H") && painter.contains("ls.tomoeMarkers") && painter.contains("ALT_PHASE"));
        String styles = Files.readString(CLIENT.resolve("BarStyles.java"));
        assertTrue(styles.contains("((BarStyle.EMBLEMS.size() + 7) / 8) * 32"));
        assertTrue(styles.contains("BarStyleIds.NARUTO.equals(id) && BossBarOptions.narutoLeafBar()"), "the swap is behind the client setting");
        assertTrue(Files.readString(CLIENT.resolve("CustomBarOverlay.java")).contains("BarStyles.effective(st.s.style())"));
        String opts = Files.readString(CLIENT.resolve("BossBarOptions.java"));
        assertTrue(opts.contains(".define(\"narutoLeafBar\", true)"));
        assertFalse(opts.contains("narutoLeafBar\", false"));
        String lang = Files.readString(ASSETS.resolve("lang/en_us.json"));
        assertTrue(lang.contains("\"anime_multiverse.configuration.narutoLeafBar\"") && lang.contains("\"anime_multiverse.configuration.narutoLeafBar.tooltip\""));
    }

    @Test
    void theServerSideOfTheBarDoesNotTouchTheClientClasses() throws IOException {
        for (String f : new String[]{"BarStyleIds.java", "BarStyle.java", "BarStyleResolver.java"}) {
            String s = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/boss/bars", f));
            assertFalse(s.contains("net.minecraft.client") || s.contains("com.newuniverse.nusmp.client"), f);
        }
    }
}
