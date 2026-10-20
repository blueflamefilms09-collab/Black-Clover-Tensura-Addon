package com.newuniverse.nusmp.client.playermodel;

import com.newuniverse.nusmp.labyrinth.mega.guard.AbyssalLooks;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Look resolution of phase 2, with no game: entity id to look id to a skin that exists, for every entity the phase converts, every Foundry mob of the asset index, every abyssal boss look in all
 * three phases and all four decay stages; the fallback flag's rule; the loader of looks_abyssal.json and the add-only merge of the tables.
 */
class DungeonLooksTest {
    private static final String ASSETS = "src/main/resources/assets/nusmp/";

    private static InputStream open(String path) {
        try {
            File f = new File(ASSETS + path);
            return f.isFile() ? Files.newInputStream(f.toPath()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, PlayerLook> tables() { return PlayerLookTables.load(DungeonLooksTest::open).looks(); }

    private static boolean skinExists(PlayerLook l) { return new File(ASSETS + SkinPath.resolve(l.skin())).isFile(); }

    @Test
    void everyConvertedEntityIsARegisteredEntityId() throws Exception {
        StringBuilder all = new StringBuilder();
        try (var walk = Files.walk(new File("src/main/java/com/newuniverse/nusmp").toPath())) {
            for (var p : (Iterable<java.nio.file.Path>) walk.filter(f -> f.toString().endsWith(".java") && !f.toString().contains("playermodel"))::iterator) all.append(new String(Files.readAllBytes(p)));
        }
        for (String id : DungeonLooks.ENTITIES) {
            boolean named = all.indexOf("\"" + id + "\"") >= 0 || (id.startsWith("corpse_") && all.indexOf("\"" + id.substring(7) + "\"") >= 0) || id.equals("foundry_mob") && all.indexOf("\"foundry_mob\"") >= 0;
            assertTrue(named, id + " is not an entity id of the mod");
        }
        assertEquals(8, DungeonLooks.ENTITIES.size());
    }

    @Test
    void theFixedLooksOfTheArmouredBodiesGoliathAndPuppetsAreInTheTable() {
        Map<String, PlayerLook> looks = tables();
        for (String id : new String[]{DungeonLooks.LIVING_ARMOR, DungeonLooks.FLOOR_WARDEN, DungeonLooks.GOLIATH, DungeonLooks.GOLIATH_FORM2}) {
            assertTrue(looks.containsKey(id), id);
            assertTrue(skinExists(looks.get(id)), id + " skin");
        }
        assertEquals(DungeonLooks.LIVING_ARMOR, DungeonLooks.lookFor("living_armor", "", 0));
        assertEquals(DungeonLooks.FLOOR_WARDEN, DungeonLooks.lookFor("floor_warden", "", 0));
        assertEquals(DungeonLooks.GOLIATH, DungeonLooks.lookFor("goliath", "", 0));
        assertEquals(DungeonLooks.GOLIATH_FORM2, DungeonLooks.lookFor("goliath", "", 1), "the Gates open");
        assertEquals("", DungeonLooks.lookFor("zombie", "", 0), "an entity the phase does not convert");
        assertEquals("", DungeonLooks.lookFor(null, "", 0));
    }

    @Test
    void everyPuppetHasAFreshAndARottenSkinInEveryStage() {
        Map<String, PlayerLook> looks = tables();
        for (String puppet : new String[]{"corpse_jimmy", "corpse_alfredo", "corpse_carl"}) {
            String previous = null;
            for (int stage = 0; stage <= 3; stage++) {
                String id = DungeonLooks.lookFor(puppet, "", stage);
                assertEquals(puppet + "_s" + stage, id);
                assertTrue(looks.containsKey(id), id);
                assertTrue(skinExists(looks.get(id)), id);
                assertFalse(id.equals(previous));
                previous = id;
            }
            assertEquals(puppet + "_s0", DungeonLooks.corpse(puppet, -4), "a stage below 0 is 0");
            assertEquals(puppet + "_s3", DungeonLooks.corpse(puppet, 99), "a stage above 3 is 3");
        }
    }

    @Test
    void everyFoundryMobIsEitherAPersonWithASkinOrAListedCreature() throws Exception {
        assertTrue(DungeonAudit.run(DungeonLooksTest::open).isEmpty(), DungeonAudit.run(DungeonLooksTest::open).toString());
        Map<String, PlayerLook> looks = tables();
        int people = 0;
        for (String id : indexIds()) {
            String look = DungeonLooks.lookFor("foundry_mob", id, 0);
            assertEquals("foundry_" + id, look);
            if (looks.containsKey(look)) {
                people++;
                assertTrue(skinExists(looks.get(look)), look + " skin");
            }
        }
        assertTrue(people >= 250, "people on the player model: " + people);
        assertEquals("", DungeonLooks.lookFor("foundry_mob", "", 0), "a blank mob is not drawn");
        assertSame(DungeonLooks.foundry("tunnel_kobold"), DungeonLooks.foundry("tunnel_kobold"), "the id's look string is made once");
    }

    private static java.util.Set<String> indexIds() throws Exception {
        try (InputStream in = open("foundry/index.json")) {
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("mobs").keySet();
        }
    }

    @Test
    void theGuardianWearsTheLookItsProfileNamesInEveryPhase() {
        Map<String, PlayerLook> looks = tables();
        int humanoids = 0, creatures = 0;
        for (AbyssalLooks.Look al : AbyssalLooks.all().values()) {
            for (int phase = 1; phase <= 3; phase++) {
                String id = DungeonLooks.lookFor("labyrinth_guardian", al.id(), phase);
                if (al.humanoid()) {
                    assertEquals(AbyssalLooks.kitId(al.id(), phase), id);
                    assertTrue(looks.containsKey(id), id + " is the loader's answer and must be in the merged table");
                    assertTrue(skinExists(looks.get(id)), id);
                } else {
                    assertEquals("", id, al.id() + " is a creature: it keeps its own model");
                }
            }
            if (al.humanoid()) humanoids++;
            else creatures++;
        }
        assertEquals(9, humanoids, "the nine humanoid bosses of the abyssal table");
        assertTrue(creatures >= 10);
        assertEquals(DungeonLooks.FLOOR_WARDEN, DungeonLooks.lookFor("labyrinth_guardian", "", 1), "no look: the Warden's body, as it always was");
        assertEquals(DungeonLooks.FLOOR_WARDEN, DungeonLooks.lookFor("labyrinth_guardian", "a_look_nobody_drew", 3));
        assertEquals(DungeonLooks.guardian("lich", 3), DungeonLooks.guardian("lich", 9), "a phase above 3 is 3");
        assertEquals(DungeonLooks.guardian("lich", 1), DungeonLooks.guardian("lich", -2), "a phase below 1 is 1");
    }

    @Test
    void theFallbackFlagGivesEverythingBackToTheOldRenderersWhenOff() {
        assertFalse(DungeonLooks.usesKit(false, true));
        assertFalse(DungeonLooks.usesKit(false, false));
        assertFalse(DungeonLooks.usesKit(true, false), "an entity without a look is never drawn by the kit");
        assertTrue(DungeonLooks.usesKit(true, true));
    }

    @Test
    void theLoaderMergesTheThreeTablesAddOnlyAndTheNinePhasesOfTheAbyssalBossesComeThrough() {
        PlayerLookTables.Merged m = PlayerLookTables.load(DungeonLooksTest::open);
        assertTrue(m.problems().isEmpty(), m.problems().toString());
        Map<String, PlayerLook> looks = m.looks();
        assertTrue(looks.containsKey("asta"), "phase 1 is kept");
        assertTrue(looks.containsKey("abyssal_lich_p1") && looks.containsKey("abyssal_zariel_p3"), "the abyssal table is loaded");
        assertTrue(looks.containsKey("dg_goblin_scrapper") && looks.containsKey("foundry_tunnel_kobold"), "the dungeon table is loaded");
        int abyssal = 0;
        for (String id : looks.keySet()) if (id.startsWith("abyssal_")) abyssal++;
        assertEquals(27, abyssal, "nine humanoid bosses in three phases");
        assertEquals(1, PlayerLookTables.FILES.indexOf("playermodel/looks_abyssal.json"));
        assertNotNull(looks.get("abyssal_vecna_p1").extras());

        java.util.Map<String, PlayerLook> into = new java.util.LinkedHashMap<>();
        java.util.List<String> problems = new java.util.ArrayList<>();
        PlayerLook a = looks.get("asta"), b = looks.get("dg_lich");
        into.put("x", a);
        PlayerLookTables.merge(into, java.util.Map.of("x", b, "y", b), "looks_later.json", problems);
        assertSame(a, into.get("x"), "an earlier look is never replaced");
        assertSame(b, into.get("y"));
        assertEquals(1, problems.size(), "the duplicate is reported");
        assertTrue(problems.get(0).contains("looks_later.json"));
    }

    @Test
    void theStanceOfALookIsReadFromTheTable() {
        Map<String, PlayerLook> looks = tables();
        assertEquals(Stances.HUNCH, looks.get("dg_goblin_scrapper").stance());
        assertEquals(Stances.ZOMBIE, looks.get("dg_zombie_labourer").stance());
        assertEquals("", looks.get("dg_paladin").stance());
        assertEquals("", looks.get("asta").stance(), "an older table has no stance");
    }
}
