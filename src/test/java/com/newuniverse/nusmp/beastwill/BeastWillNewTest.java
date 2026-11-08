package com.newuniverse.nusmp.beastwill;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.vfx.VfxShape;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (docs/beastwill_new.md): the 18 later Beast Wills are data files, a tint each, a look kind each, a lang name and ability names, and a picture table. A file the validator rejects would silently leave a
 * will out of the game, two wills with one colour or kind would look the same, and an ability without a name would show its raw id. This reads the real files from the repo and pins what the owner asked for:
 * every will passes the validator, ranks and balance sit between the first nine and the dragon, the three legendary ones are rarer, slower and capped harder than the dragon, every ability is named, every
 * kind of the later wills has a picture that is never a Time shape. Pure (file reads); the working directory is the project root.
 */
class BeastWillNewTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/beast_will");
    private static final List<String> LEGENDARY = List.of("typhon_will", "yamata_no_orochi_will", "apophis_will");

    private static BeastWillDef load(String id) throws Exception { return BeastWillSchemaTest.load(id); }

    @Test
    void everyLaterWillPassesTheValidatorAndHasItsOwnTintAndKind() throws Exception {
        Set<Integer> tints = new HashSet<>(), kinds = new HashSet<>();
        for (String f : DIR.toFile().list((d, n) -> n.endsWith(".json"))) tints.add(JsonParser.parseString(Files.readString(DIR.resolve(f))).getAsJsonObject().get("tint").getAsInt());
        assertEquals(27, tints.size(), "27 wills, 27 colours");
        assertEquals(18, BeastWillSchemaTest.LATER.size());
        for (String id : BeastWillSchemaTest.LATER) {
            BeastWillDef d = load(id);
            assertNull(d.problem(), id);
            assertEquals(d.tint(), BeastCoreLook.tint(id), id + ": the core colour is the file's tint");
            int k = BeastWillLook.kindOf(id);
            assertTrue(k >= BeastWillLook.FENRIR && k < BeastWillLook.COUNT, id + " has a look kind of the later set");
            assertTrue(kinds.add(k), id + " shares its kind");
            assertFalse(d.keywords().isEmpty(), id + " names the Tensura creatures that can give it");
            assertEquals(2, d.acquire().abilities().size(), id);
            assertEquals(2, d.integrate().abilities().size(), id);
            assertTrue(d.legendary() == LEGENDARY.contains(id), id);
        }
    }

    @Test
    void theLegendaryThreeAreRarerSlowerAndCappedHarderThanTheDragonAndStrongerInIntegrate() throws Exception {
        BeastWillDef dragon = load("dragon_will");
        for (String id : LEGENDARY) {
            BeastWillDef d = load(id);
            assertEquals(BeastRank.SS, d.minRank(), id + " (3.60.0 remake: the legendary three sit with the dragon)");
            assertTrue(d.dropChance() < dragon.dropChance(), id + " drops more rarely");
            assertTrue(d.assimilationSeconds() > dragon.assimilationSeconds() && d.masterySeconds() > 0.8 * dragon.masterySeconds(), id + " is slower");
            assertTrue(d.assimilationDayCap() < dragon.assimilationDayCap(), id + " is capped harder per day");
            assertTrue(d.requireEp() >= dragon.requireEp(), id + " asks more");
            assertTrue(d.integrate().drainPct() >= dragon.integrate().drainPct(), id + " drains at least as much");
        }
    }

    @Test
    void rankFollowsTheRemadeBrief() throws Exception {
        // 3.60.0 (docs/beastwill_remake.md): the rank each remade will asks of its holder
        for (String id : List.of("minotaur_will", "hydra_will", "cu_sith_will", "scylla_charybdis_will", "pegasus_will", "nue_will")) assertEquals(BeastRank.A, load(id).minRank(), id);
        for (String id : List.of("fenrir_will", "cerberus_will", "kraken_will", "sphinx_will", "kitsune_will", "ammit_will", "jormungandr_will", "nidhoggr_will", "hafgufa_will")) {
            assertEquals(BeastRank.S, load(id).minRank(), id);
            assertFalse(load(id).legendary(), id);
        }
    }

    @Test
    void theFenrirWillIsTheChainedWolfOfTheSoulAndSeals() throws Exception {
        BeastWillDef d = load("fenrir_will");
        assertEquals("Fenrir, the chained wolf", d.beast());
        assertEquals("Soul", d.affinity());
        assertEquals("restraining_vines", d.ability(BeastWillRules.INTEGRATE, 2).kind(), "the soul seal");
        assertEquals("shadow_dash", d.ability(BeastWillRules.ACQUIRE, 1).kind(), "the borrowed dash of the Acquire stage");
        assertFalse(JsonParser.parseString(Files.readString(DIR.resolve("fenrir_will.json"))).getAsJsonObject().getAsJsonObject("acquire").get("equipPiece").getAsString().isBlank(), "every remade will names the one piece worn in Acquire");
    }

    @Test
    void noLaterWillNeedsTheRuntimeCapToHoldItBackAtFullStrength() throws Exception {
        for (String id : BeastWillSchemaTest.LATER) {
            BeastWillDef d = load(id);
            for (BeastWillDef.Phase ph : List.of(d.acquire(), d.integrate())) {
                java.util.Map<String, Double> sum = new java.util.HashMap<>();
                for (BeastWillDef.Stat s : d.passive()) sum.merge(s.attribute() + "|" + s.op(), s.amount(), Double::sum);
                for (BeastWillDef.Stat s : ph.stats()) sum.merge(s.attribute() + "|" + s.op(), s.amount(), Double::sum);
                for (var e : sum.entrySet()) {
                    BeastWillRules.AttrCap cap = BeastWillRules.ATTR_CAPS.get(e.getKey().substring(0, e.getKey().indexOf('|')));
                    double max = e.getKey().endsWith("|ADD") ? cap.maxAdd() : cap.maxMult();
                    assertTrue(e.getValue() <= max + 1e-9, id + " passes the cap on " + e.getKey() + ": " + e.getValue() + " > " + max);
                }
            }
        }
    }

    @Test
    void theNewKindsAreBoundedLikeTheOldOnes() {
        for (String k : List.of("venom_spit", "maelstrom", "dread_gaze")) {
            BeastWillRules.KindLimit l = BeastWillRules.KINDS.get(k);
            assertNotNull(l, k);
            assertTrue(l.maxDuration() <= 160 && l.maxTargets() <= 6 && l.maxRadius() <= 16 && l.minCooldown() >= 100, k);
        }
    }

    @Test
    void everyAbilityOfTheLaterWillsHasAName() throws Exception {
        String lang = Files.readString(Path.of("tools/lang/beastwill_new.json"));
        String en = Files.readString(Path.of("src/main/resources/assets/nusmp/lang/en_us.json"));
        JsonObject frag = JsonParser.parseString(lang).getAsJsonObject();
        for (String id : BeastWillSchemaTest.LATER) {
            assertTrue(frag.has("nusmp.beastwill." + id + ".name") || en.contains("\"nusmp.beastwill." + id + ".name\""), id + " has no lang name");
            BeastWillDef d = load(id);
            assertEquals(d.name(), JsonParser.parseString(en).getAsJsonObject().get(d.langKey("name")).getAsString(), id + ": the lang name differs from the file name");
            for (var ph : List.of(d.acquire(), d.integrate())) for (var a : ph.abilities()) {
                String key = "nusmp.beastwill.ability." + a.id();
                assertTrue(frag.has(key) || en.contains("\"" + key + "\""), a.id() + " has no lang name");
            }
        }
        for (String k : frag.keySet()) assertTrue(k.startsWith("nusmp.beastwill."), k);
    }

    @Test
    void everyLaterKindHasAPictureAndNeverATimeShape() {
        for (int k = BeastWillLook.FENRIR; k < BeastWillLook.COUNT; k++) {
            BeastWillPictures.Pic p = BeastWillPictures.of(k);
            assertNotNull(p, "kind " + k + " has no picture");
            assertNotNull(p.burst(), "kind " + k + " has no burst picture");
            assertFalse(p.burst().name().startsWith("TIME_") || (p.travel() != null && p.travel().name().startsWith("TIME_")), "no Time Magic shape in kind " + k);
            assertTrue(p.max() > 0 && p.max() <= 4f && p.scale() > 0 && p.lift() >= 0 && p.lift() <= 2f, "kind " + k + " has a bounded picture");
        }
        for (int k = 0; k <= BeastWillLook.CHIMERA; k++) assertNull(BeastWillPictures.of(k), "the first nine keep their own switches");
        assertNotNull(VfxShape.valueOf("SOUL_VORTEX"));
    }
}
