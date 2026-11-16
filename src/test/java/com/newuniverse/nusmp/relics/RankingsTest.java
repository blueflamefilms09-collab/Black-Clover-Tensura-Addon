package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.DjinnExtremeTypes;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the generic ranking table (assets/anime_multiverse/rankings.json, docs/rankings.md) must load, rank all 36 Djinn exactly once and in a stable order, cover every Genesis main-hand weapon of the
 * relic waves, resolve by explicit entry, Djinn order, rule and the fixed weapon table, and the Djinn Extreme Magic table must cover the same 36 with damage types that have a data file. Reads the repo's files
 * (the working directory is the project root).
 */
class RankingsTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path RES = Path.of("src/main/resources");

    @Test
    void theTableLoadsAndRanksEveryDjinnOnce() {
        Rankings.Table t = Rankings.table();
        assertEquals(Djinn.values().length, t.djinnCount());
        Set<String> seen = new HashSet<>();
        for (Djinn d : Djinn.values()) {
            assertTrue(t.djinnRank(d.id()) > 0, d.id() + " is not ranked");
            assertTrue(seen.add(String.valueOf(t.djinnRank(d.id()))), d.id() + " shares a rank");
            assertNotNull(t.djinn(d.id()), d.id());
        }
        assertEquals(1, t.djinnRank("baal"), "Baal is the strongest");
        assertEquals(36, t.djinnRank("murmur"));
    }

    @Test
    void djinnTiersAreGradesWithPlacesAndNeverRiseWithTheRank() {
        Rankings.Table t = Rankings.table();
        int last = Integer.MAX_VALUE;
        for (String id : t.djinnOrder()) {
            Rankings.Rank r = t.djinn(id);
            assertTrue(r.place() >= 1 && r.place() <= r.of(), id);
            assertTrue(r.grade().ordinal() <= last, id + " is above the Djinn before it");
            last = r.grade().ordinal();
        }
        assertEquals(Grade.GENESIS, t.djinn("baal").grade());
        assertEquals(Grade.UNIQUE, t.djinn("murmur").grade());
        assertEquals("God Grade #5", t.djinn("furcas").label(), "Furcas is the 8th of 36: the 5th of the nine God places");
    }

    @Test
    void djinnWeaponsAndSealsResolveThroughTheOrderLightAndDark() {
        Rankings.Table t = Rankings.table();
        assertEquals(t.djinn("amon").label(), t.of("djinn_weapon_amon").label());
        assertEquals(t.djinn("amon").label(), t.of("djinn_weapon_amon_dark").label());
        assertEquals(t.djinn("glasya_labolas").label(), t.of("djinn_seal_glasya_labolas").label());
        assertNull(t.djinnIdOfItem("djinn_weapon_nobody"));
    }

    @Test
    void anExplicitEntryBeatsARuleAndARuleBeatsTheWeaponTable() {
        Rankings.Table t = Rankings.parse("{\"items\":{\"vassal_hammer\":\"god:2\"},\"rules\":[{\"prefix\":\"vassal_\",\"rank\":\"rare\"},{\"suffix\":\"_core\",\"rank\":\"unique\"}]}");
        assertEquals(Grade.GOD, t.of("vassal_hammer").grade());
        assertEquals(2, t.of("vassal_hammer").place());
        assertEquals(Grade.RARE, t.of("vassal_claw").grade());
        assertEquals(Grade.UNIQUE, t.of("mana_core").grade());
        assertEquals("weapon", t.of("legendary_sword").source());
        assertEquals(Grade.LEGEND, t.of("legendary_sword").grade());
        assertNull(t.of("some_block"));
    }

    @Test
    void aBrokenTableNeverThrows() {
        Rankings.Table t = Rankings.parse("{\"items\":{\"a\":\"nonsense\",\"b\":\"rare:x\"},\"rules\":[{\"nothing\":1},{\"prefix\":\"p\",\"rank\":\"nope\"}],\"djinnTiers\":[[\"nope\",3]]}");
        assertNull(t.of("a"));
        assertEquals(Grade.RARE, t.of("b").grade());
        assertEquals(0, t.of("b").place());
        assertEquals(0, t.rules());
    }

    @Test
    void everyGenesisMainHandWeaponOfTheWavesHasARankPlace() throws IOException {
        Pattern p = Pattern.compile("RelicDef\\.of\\(\\s*\"([a-z0-9_]+)\"\\s*,\\s*\"[^\"]*\"\\s*,\\s*Grade\\.GENESIS\\s*,\\s*RelicSlot\\.MAIN_HAND");
        List<String> ids = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SRC.resolve("relics/wave"))) {
            for (Path f : files.filter(x -> x.toString().endsWith(".java")).toList()) {
                Matcher m = p.matcher(Files.readString(f, StandardCharsets.UTF_8));
                while (m.find()) ids.add(m.group(1));
            }
        }
        ids.add("demon_slayer_sword");
        ids.add("elsdocia");
        assertTrue(ids.size() >= 15, "the scan found " + ids);
        List<String> bad = new ArrayList<>();
        Set<Integer> places = new HashSet<>();
        for (String id : ids) {
            Rankings.Rank r = Rankings.of(id);
            if (r == null || r.grade() != Grade.GENESIS || r.place() < 1 || !places.add(r.place())) bad.add(id);
        }
        assertTrue(bad.isEmpty(), "Genesis weapons without a Genesis rank place in rankings.json: " + bad);
    }

    @Test
    void theExtremeMagicTableCoversTheDjinnWithRealDamageTypes() throws IOException {
        for (Djinn d : Djinn.values()) assertNotNull(DjinnExtremeTypes.typeId(d.id()), d.id() + " has no Extreme Magic damage type");
        assertEquals("minecraft:lightning_bolt", DjinnExtremeTypes.typeId("baal"));
        assertEquals("minecraft:in_fire", DjinnExtremeTypes.typeId("amon"));
        assertEquals("minecraft:freeze", DjinnExtremeTypes.typeId("vinea"));
        for (String id : DjinnExtremeTypes.concepts().values()) {
            if (!id.startsWith("anime_multiverse:")) continue;
            String path = id.substring("anime_multiverse:".length());
            assertTrue(Files.exists(RES.resolve("data/anime_multiverse/damage_type/" + path + ".json")), id + " has no data file");
            String json = Files.readString(RES.resolve("data/anime_multiverse/damage_type/" + path + ".json"), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"exhaustion\": 0.0"), id + ": the hunger ban, a damage type drains no food");
        }
        for (String t : new String[]{"water", "wind", "life", "light"})
            assertTrue(Files.readString(RES.resolve("data/anime_multiverse/tags/damage_type/is_djinn_" + t + ".json"), StandardCharsets.UTF_8).contains("anime_multiverse:djinn_" + t));
    }
}
