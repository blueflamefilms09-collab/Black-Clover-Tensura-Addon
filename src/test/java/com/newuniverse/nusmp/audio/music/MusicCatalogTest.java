package com.newuniverse.nusmp.audio.music;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the table that says which music belongs to which theme is used by four things (the server, the client, the sound registry, the generator), so its rules are pinned here:
 * every one of the Region Themes has a family, every family has both loops, the track and event names follow the one scheme the generator mirrors, an unknown (datapack) theme
 * always gets the same family, and the layer codes on the wire never move.
 */
class MusicCatalogTest {
    @Test
    void everyRegionThemeHasAFamilyInTheTable() {
        for (RegionTheme t : RegionTheme.values()) {
            assertTrue(MusicCatalog.themeTable().containsKey(t.id()), "theme " + t.id() + " has no musical family");
            assertTrue(MusicCatalog.FAMILIES.contains(MusicCatalog.familyOf(t.id())), t.id());
        }
        assertEquals(RegionTheme.values().length, MusicCatalog.themeTable().size(), "the table names a theme that does not exist, or misses one");
    }

    @Test
    void everyFamilyIsUsedAndHasBothLoops() {
        Set<String> used = new HashSet<>(MusicCatalog.themeTable().values());
        assertEquals(new HashSet<>(MusicCatalog.FAMILIES), used);
        List<String> tracks = MusicCatalog.trackNames();
        for (String f : MusicCatalog.FAMILIES) {
            assertTrue(tracks.contains(f + "_explore"), f);
            assertTrue(tracks.contains(f + "_battle"), f);
        }
    }

    @Test
    void theTrackListIsTwentyFiveUniqueNamesWithUniqueEvents() {
        List<String> tracks = MusicCatalog.trackNames();
        assertEquals(25, tracks.size());
        assertEquals(25, new HashSet<>(tracks).size());
        assertEquals(25, new HashSet<>(MusicCatalog.eventIds()).size());
        assertEquals("boss_holy", tracks.get(20));
        assertEquals("rift", tracks.get(24));
    }

    @Test
    void eventAndFileNamesFollowOneScheme() {
        assertEquals("music.stone.explore", MusicCatalog.eventId("stone_explore"));
        assertEquals("music.crypt.battle", MusicCatalog.eventId("crypt_battle"));
        assertEquals("music.boss.holy", MusicCatalog.eventId("boss_holy"));
        assertEquals("music.boss.abyssal", MusicCatalog.eventId("boss_abyssal"));
        assertEquals("music.safe", MusicCatalog.eventId("safe"));
        assertEquals("music/hub.ogg", MusicCatalog.filePath("hub"));
        assertEquals("music.nusmp.track.rift", MusicCatalog.titleKey("rift"));
    }

    @Test
    void layersPickTheRightTrack() {
        assertEquals("holy_explore", MusicCatalog.trackFor(MusicLayer.EXPLORE, "holy_church", 1));
        assertEquals("deep_battle", MusicCatalog.trackFor(MusicLayer.BATTLE, "drowned_crypt", -1));
        assertEquals("boss_holy", MusicCatalog.trackFor(MusicLayer.BOSS, "flame", 1));
        assertEquals("boss_abyssal", MusicCatalog.trackFor(MusicLayer.BOSS, "flame", -1));
        assertEquals("safe", MusicCatalog.trackFor(MusicLayer.SAFE, "frost", 1));
        assertEquals("hub", MusicCatalog.trackFor(MusicLayer.HUB, "", 1));
        assertEquals("rift", MusicCatalog.trackFor(MusicLayer.RIFT, "shadow", 1));
        assertNull(MusicCatalog.trackFor(MusicLayer.OFF, "stone", 1));
        assertNull(MusicCatalog.trackFor(null, "stone", 1));
    }

    @Test
    void anUnknownThemeIsStableAndAnEmptyOneIsTheDefault() {
        assertEquals(MusicCatalog.DEFAULT_FAMILY, MusicCatalog.familyOf(""));
        assertEquals(MusicCatalog.DEFAULT_FAMILY, MusicCatalog.familyOf(null));
        String a = MusicCatalog.familyOf("some_datapack_theme");
        for (int i = 0; i < 5; i++) assertEquals(a, MusicCatalog.familyOf("SOME_DATAPACK_THEME"), "case and repetition must not change it");
        assertTrue(MusicCatalog.FAMILIES.contains(a));
        Set<String> spread = new HashSet<>();
        for (int i = 0; i < 40; i++) spread.add(MusicCatalog.familyOf("custom_theme_" + i));
        assertTrue(spread.size() >= 5, "unknown themes should spread over the families, got " + spread);
    }

    @Test
    void layerCodesAreStableAndTheStateIsSanitised() {
        assertEquals(0, MusicLayer.OFF.code());
        assertEquals(1, MusicLayer.EXPLORE.code());
        assertEquals(2, MusicLayer.SAFE.code());
        assertEquals(3, MusicLayer.HUB.code());
        assertEquals(4, MusicLayer.RIFT.code());
        assertEquals(5, MusicLayer.BATTLE.code());
        assertEquals(6, MusicLayer.BOSS.code());
        assertEquals(MusicLayer.OFF, MusicLayer.fromCode(99));
        assertEquals(MusicLayer.OFF, MusicLayer.fromCode(-3));
        assertTrue(MusicLayer.BOSS.priority() > MusicLayer.BATTLE.priority());
        assertTrue(MusicLayer.BATTLE.priority() > MusicLayer.EXPLORE.priority());
        MusicState s = new MusicState(null, null, -7);
        assertEquals(MusicLayer.OFF, s.layer());
        assertEquals("", s.themeId());
        assertEquals(-1, s.polarity());
        assertNull(s.track());
        assertEquals(MusicState.MAX_THEME, new MusicState(MusicLayer.EXPLORE, "x".repeat(200), 1).themeId().length());
        assertFalse(MusicState.OFF.layer() != MusicLayer.OFF);
    }
}
