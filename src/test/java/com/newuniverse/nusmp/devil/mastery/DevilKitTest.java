package com.newuniverse.nusmp.devil.mastery;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.devil.WorldDevil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the kit of every devil (devil_kits.json, docs/devil_mastery.md) is data a server runs, so it must validate: every roster devil has a kit from its own anime and aspect, the shapes are the allowed ones, the numbers are inside their bounds, and a bad file is refused. */
class DevilKitTest {
    private static final Set<String> BLACK_CLOVER = Set.of("liebe", "lucifero", "beelzebub", "astaroth", "megicula", "lucifugus");

    @Test
    void theShippedTableHasNoProblem() {
        List<String> problems = DevilKit.problems(DevilKit.table());
        assertTrue(problems.isEmpty(), "devil_kits.json: " + problems);
    }

    @Test
    void everyDevilOfTheRosterHasAKitFromItsOwnAnimeAndAspect() {
        assertTrue(WorldDevil.count() > 0);
        for (int i = 0; i < WorldDevil.count(); i++) {
            WorldDevil d = WorldDevil.at(i);
            DevilKit.Kit k = DevilKit.of(d.id());
            assertNotNull(k, d.id() + " has no kit");
            assertEquals(d.world().label, k.anime(), d.id() + ": the kit is of another anime");
            assertEquals(d.attribute().key(), k.aspect(), d.id() + ": the kit is of another aspect");
            assertTrue(k.arts().size() >= 2 && k.arts().size() <= 4, d.id() + ": 2 to 4 arts");
            assertEquals(3, k.stages().size(), d.id() + ": three stage names");
            assertFalse(k.source().isBlank(), d.id() + ": the source must be said");
        }
        assertEquals(WorldDevil.count(), DevilKit.table().kits().size(), "a kit for a devil that is not in the roster");
    }

    @Test
    void theKitsOpenWithTheMasteryTiers() {
        for (DevilKit.Kit k : DevilKit.table().kits().values()) {
            assertEquals(0, k.arts().get(0).tier(), k.devil() + ": the first art is open from the start tier");
            int prev = 0;
            for (DevilKit.Art a : k.arts()) { assertTrue(a.tier() >= prev, k.devil() + ": arts open in order"); prev = a.tier(); }
            long awake = k.passives().stream().filter(p -> p.when().equals("awake")).count();
            assertTrue(awake == 2 || awake == 3, k.devil() + ": 2 or 3 passives");
            for (DevilKit.Passive p : k.passives()) if (p.when().equals("union")) assertEquals(MasteryRules.TIERS - 1, p.tier(), k.devil() + ": union passives open at full mastery");
        }
    }

    @Test
    void theBlackCloverDevilsAndTheirUnionsAreThere() {
        for (String id : BLACK_CLOVER) {
            DevilKit.Kit k = DevilKit.of(id);
            assertNotNull(k, id);
            assertEquals("Black Clover", k.anime(), id);
            assertFalse(k.passivesWhen("union").isEmpty(), id + ": a Devil Union gives passives (Asta-Liebe, the Dark Triad)");
            assertFalse(k.unionName().isBlank(), id + ": the union has its name");
        }
        assertFalse(DevilKit.table().fusions().isEmpty(), "the double unions are listed");
        for (DevilKit.Fusion f : DevilKit.table().fusions()) {
            assertNotNull(WorldDevil.byId(f.a()), f.a());
            assertNotNull(WorldDevil.byId(f.b()), f.b());
            assertFalse(f.note().isBlank(), "a fusion says where it comes from or that it is design");
        }
    }

    private static List<String> problemsOf(String devilJson) {
        JsonObject root = JsonParser.parseString("{\"devils\":{" + devilJson + "},\"fusions\":[]}").getAsJsonObject();
        return DevilKit.problems(DevilKit.parse(root));
    }

    @Test
    void aBadFileIsRefused() {
        assertFalse(problemsOf("\"not_a_devil\":{\"anime\":\"x\",\"aspect\":\"x\",\"source\":\"x\",\"arts\":[],\"passives\":[],\"stages\":[\"a\",\"b\",\"c\"]}").isEmpty(), "an unknown devil");
        // a real devil whose only art has a kind that is not allowed and a damage that is out of bounds
        String liebe = "\"liebe\":{\"anime\":\"Black Clover\",\"aspect\":\"anti_magic\",\"source\":\"s\",\"stages\":[\"a\",\"b\",\"c\"],\"passives\":[],"
                + "\"arts\":[{\"id\":\"x1\",\"name\":\"X\",\"kind\":\"summon\",\"dmg\":99,\"cd\":1,\"cost\":9,\"range\":99,\"fx\":[],\"special\":\"teleport\",\"self\":[],\"canon\":\"c\",\"tier\":0}]}";
        List<String> p = problemsOf(liebe);
        assertTrue(p.stream().anyMatch(s -> s.contains("kind")), p.toString());
        assertTrue(p.stream().anyMatch(s -> s.contains("special")), p.toString());
        assertTrue(p.stream().anyMatch(s -> s.contains("cooldown")), p.toString());
        assertTrue(p.stream().anyMatch(s -> s.contains("needs 2 to 4 arts")), p.toString());
        assertTrue(p.stream().anyMatch(s -> s.contains("has no kit")), "the other devils of the roster are missing: " + p);
    }

    @Test
    void theGeneratorAndTheFileAgree() throws Exception {
        Path gen = Path.of("tools/devil/gen_devil_kits.py");
        Assumptions.assumeTrue(Files.exists(gen), "no generator in this checkout");
        Process pr;
        try {
            pr = new ProcessBuilder("python3", "-B", gen.toString(), "--check").redirectErrorStream(true).start();
        } catch (java.io.IOException noPython) {
            Assumptions.assumeTrue(false, "python3 is not installed here");
            return;
        }
        String out = new String(pr.getInputStream().readAllBytes());
        assertEquals(0, pr.waitFor(), "devil_kits.json differs from tools/devil/gen_devil_kits.py: " + out);
    }
}
