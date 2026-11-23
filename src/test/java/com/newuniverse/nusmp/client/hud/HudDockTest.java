package com.newuniverse.nusmp.client.hud;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 5.0.0: the panel column's arithmetic, and that every panel the owner listed asks the dock (and can be switched back). */
class HudDockTest {
    @Test
    void panelsStackUpwardFromTheBottomWithAGapAndNeverLeaveTheScreen() {
        assertEquals(240 - 6 - 40, HudDock.slot(240, 0, 40));
        int used = 40 + HudDock.GAP;
        assertEquals(240 - 6 - used - 30, HudDock.slot(240, used, 30));
        assertEquals(HudDock.TOP_MIN, HudDock.slot(240, 500, 100));
    }

    @Test
    void leftIsTheMarginAndRightEndsAtTheMargin() {
        assertEquals(HudDock.MARGIN, HudDock.edge(HudDock.Side.LEFT, 400, 170));
        assertEquals(400 - HudDock.MARGIN - 170, HudDock.edge(HudDock.Side.RIGHT, 400, 170));
    }

    @Test
    void everyListedPanelUsesTheDockAndTheFrameStartsEmpty() throws Exception {
        String root = "src/main/java/com/newuniverse/nusmp/client/";
        for (String f : new String[]{"heroweapon/HeroHud.java", "greed/GreedClient.java", "lords/LordClient.java", "beastwill/BeastWillClient.java", "saiyan/SaiyanClient.java",
                "shishou/ShishouHud.java", "devil/DevilClient.java", "relics/RelicActKeys.java"})
            assertTrue(Files.readString(Path.of(root + f)).contains("HudDock.take("), f + " does not use the dock");
        assertTrue(Files.readString(Path.of(root + "heroweapon/HeroClient.java")).contains("HudDock.begin()"), "the dock is never reset");
        assertTrue(Files.readString(Path.of(root + "heroweapon/HeroClientConfig.java")).contains("\"unifiedHud\", true"), "the setting is missing or not the default");
    }
}
