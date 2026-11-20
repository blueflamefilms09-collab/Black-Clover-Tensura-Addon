package com.newuniverse.nusmp.client.weaponpage;

import com.newuniverse.nusmp.client.weaponpage.MasteryTracks.Track;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the "Other masteries" list of the Weapon page (Codex, hub tab and Status-screen tab) is built from these pure rules, so they are pinned: fractions stay in 0..1, the list is bounded, the open weapon is not
 * listed twice, the skills in training are picked highest first and never more than asked, and the source-level promises of the change hold (a pure model with no game imports, the Status tab appended after the
 * Beast Will tab, both client settings default on, the hero's extra bars bounded by the packet limit). The working directory is the project root.
 */
class MasteryTracksTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private static List<Track> list(int globals, int[] owners) {
        List<Track> t = new ArrayList<>();
        for (int i = 0; i < globals; i++) t.add(new Track("g" + i, "1%", 0.1f, MasteryTracks.GLOBAL));
        for (int o : owners) t.add(new Track("w" + o, "2%", 0.2f, o));
        return t;
    }

    @Test
    void fractionsAreAlwaysInsideZeroToOne() {
        assertEquals(0f, MasteryTracks.frac(5, 0));
        assertEquals(0f, MasteryTracks.frac(-3, 10));
        assertEquals(1f, MasteryTracks.frac(99, 10));
        assertEquals(0.5f, MasteryTracks.frac(5, 10));
        assertEquals(0f, new Track("x", "", Float.NaN, 0).frac());
        assertEquals(1f, new Track("x", "", 7f, 0).frac());
        assertEquals(40, MasteryTracks.pct(0.4f));
        assertEquals(100, MasteryTracks.pct(5f));
    }

    @Test
    void theOpenWeaponIsNotListedTwiceAndTheListIsBounded() {
        List<Track> all = list(2, new int[] {0, 1, 2, 0, 1});
        Track[] t = MasteryTracks.forSelection(all, 1);
        assertEquals(5, t.length, "7 minus the two rows of weapon 1");
        for (Track x : t) assertFalse(x.owner() == 1);
        assertEquals(7, MasteryTracks.forSelection(all, -1).length, "nothing selected: nothing left out");
        assertEquals(7, MasteryTracks.forSelection(all, 3).length, "a weapon with no rows leaves everything");
        List<Track> many = list(0, new int[0]);
        for (int i = 0; i < 100; i++) many.add(new Track("t" + i, "", 0.5f, i % 6));
        assertEquals(MasteryTracks.MAX_TRACKS, MasteryTracks.forSelection(many, 2).length, "never more than MAX_TRACKS rows");
        assertEquals(0, MasteryTracks.forSelection(List.of(), 0).length);
    }

    @Test
    void globalTracksComeFirstAndKeepTheirOrder() {
        Track[] t = MasteryTracks.forSelection(list(3, new int[] {0, 1}), 0);
        assertEquals("g0", t[0].label());
        assertEquals("g2", t[2].label());
        assertEquals("w1", t[3].label());
    }

    @Test
    void skillsInTrainingAreTheHighestNotFullOnesAtMostN() {
        double[] have = {50, 10, 100, 0, 30, 90, 45};
        int[] max = {100, 100, 100, 0, 100, 100, 100};            // index 2 is full, index 3 has no mastery at all
        assertArrayEquals(new int[] {5, 0, 6, 4}, MasteryTracks.inTraining(have, max, 4));
        assertArrayEquals(new int[] {5, 0}, MasteryTracks.inTraining(have, max, 2));
        assertArrayEquals(new int[] {5, 0, 6, 4, 1}, MasteryTracks.inTraining(have, max, 99), "all that are in training, still ordered");
        assertEquals(0, MasteryTracks.inTraining(have, max, 0).length);
        assertEquals(0, MasteryTracks.inTraining(new double[0], new int[0], 4).length);
        assertArrayEquals(new int[] {0, 1}, MasteryTracks.inTraining(new double[] {5, 5, 5}, new int[] {10, 10, 10}, 2), "a tie keeps the lower index");
    }

    // ---------------------------------------------------------------- source pins
    private static String src(String rel) throws IOException {
        return Files.readString(ROOT.resolve("src/main/java/com/newuniverse/nusmp/" + rel), StandardCharsets.UTF_8);
    }

    @Test
    void theModelIsPureAndTheStatusTabIsAppendedLast() throws IOException {
        String model = src("client/weaponpage/MasteryTracks.java");
        assertFalse(model.contains("net.minecraft") || model.contains("com.mojang") || model.contains("net.neoforged"), "the model has no game imports");
        String screen = src("client/multiverse/MultiverseStatusScreen.java");
        assertTrue(screen.indexOf("beastView = !beastView") > 0 && screen.indexOf("toggleWeapons()") > screen.indexOf("beastView = !beastView"), "the Weapon tab comes after the Beast Will tab");
        assertTrue(screen.contains("WEAPON_STATUS_PAGE"), "the tab is behind its client setting");
        String cfg = src("client/multiverse/MultiverseClientConfig.java");
        assertTrue(cfg.contains("\"moreMastery\", true") && cfg.contains("\"statusPage\", true"), "both looks-only settings default on");
        String page = src("client/multiverse/WeaponStatusPage.java");
        assertTrue(page.contains("HubWeaponsBody"), "the status tab draws the hub's body, it does not copy it");
        assertFalse(page.contains("drawString"), "no second page drawing in the status tab");
    }

    @Test
    void theHeroExtraBarsStayInsideThePacketLimitAndThePageStaysClientSideOptional() throws IOException {
        String hero = src("heroweapon/forms/HeroPageView.java");
        assertTrue(hero.contains("bars.size() >= WeaponPageRules.MAX_BARS"), "extra form bars are capped by MAX_BARS");
        String client = src("client/weaponpage/WeaponPageClient.java");
        assertTrue(client.contains("WEAPON_MORE_MASTERY"), "the extra tracks and bars are behind the setting");
        assertFalse(client.contains("onClientTick") || client.contains("RenderGuiEvent") || client.contains("ClientTickEvent"), "no per-tick or per-frame work in the page client");
    }
}
