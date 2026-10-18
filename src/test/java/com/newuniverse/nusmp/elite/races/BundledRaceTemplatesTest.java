package com.newuniverse.nusmp.elite.races;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.races.data.RaceAttributeKeys;
import com.newuniverse.nusmp.elite.races.data.RaceTemplate;
import com.newuniverse.nusmp.elite.races.data.RaceTemplateParser;
import com.newuniverse.nusmp.elite.races.data.RaceTemplates;
import com.newuniverse.nusmp.elite.races.rule.EvolutionRules;
import com.newuniverse.nusmp.elite.races.rule.RaceEvolutionRule;
import com.newuniverse.nusmp.elite.races.state.StateModifier;
import com.newuniverse.nusmp.elite.races.state.TransformSpec;
import com.newuniverse.nusmp.elite.races.state.TransformState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the five race files are the design. This reads the REAL files shipped in the jar (the same way the game does at registration) and checks the design rules the owner asked
 * for and the ones the lines need to make sense: all five load, the Saiyan line climbs by one tier at a time and ends, stats and gates grow with the tier, every natural skill is one
 * of the race's skills, every transformation can actually be reached, the Great Ape is moon-bound, the Divine state is the top tier's alone, and the Primordial race stands apart.
 */
class BundledRaceTemplatesTest {
    private static Map<String, RaceTemplate> load() {
        List<String> problems = RaceTemplates.loadBundled();
        assertTrue(problems.isEmpty(), "bundled race files: " + problems);
        return RaceTemplates.all();
    }

    @Test
    void allFiveRacesLoadAndTheLinesAreSound() {
        Map<String, RaceTemplate> t = load();
        assertEquals(List.of("lesser_saiyan", "medium_saiyan", "high_saiyan", "divine_saiyan", "primordial"), List.copyOf(t.keySet()));
        assertEquals(List.of(), RaceTemplates.checkLines(t));
        assertEquals("medium_saiyan", t.get("lesser_saiyan").evolvesTo());
        assertEquals("high_saiyan", t.get("medium_saiyan").evolvesTo());
        assertEquals("divine_saiyan", t.get("high_saiyan").evolvesTo());
        assertEquals("", t.get("divine_saiyan").evolvesTo());
        assertEquals("", t.get("primordial").evolvesTo());
        assertEquals("primordial", t.get("primordial").line());
        for (String s : List.of("lesser_saiyan", "medium_saiyan", "high_saiyan", "divine_saiyan")) assertEquals("saiyan", t.get(s).line());
    }

    @Test
    void statsAndGatesGrowWithTheTierOfTheSaiyanLine() {
        Map<String, RaceTemplate> t = load();
        List<RaceTemplate> line = List.of(t.get("lesser_saiyan"), t.get("medium_saiyan"), t.get("high_saiyan"), t.get("divine_saiyan"));
        for (int i = 1; i < line.size(); i++) {
            RaceTemplate a = line.get(i - 1), b = line.get(i);
            assertEquals(a.tier() + 1, b.tier());
            assertTrue(b.stats().auraMin() > a.stats().auraMax(), b.id() + " aura floor should clear the previous ceiling");
            assertTrue(b.stats().magiculeMin() > a.stats().magiculeMax(), b.id() + " magicule floor should clear the previous ceiling");
            assertTrue(b.stats().maxHealth() > a.stats().maxHealth());
            assertTrue(b.stats().attack() > a.stats().attack());
            assertTrue(b.stats().attackSpeed() > a.stats().attackSpeed());
            assertTrue(b.stats().movementSpeed() > a.stats().movementSpeed());
            assertTrue(b.stats().knockbackResistance() >= a.stats().knockbackResistance());
            assertTrue(b.intrinsic().size() >= a.intrinsic().size());
        }
        double lastGate = 0;
        for (RaceTemplate r : line) {
            if (r.evolution().isEmpty()) continue;
            double gate = 0;
            for (RaceEvolutionRule rule : r.evolution().rules()) if (rule instanceof EvolutionRules.EpGate g) gate = g.min();
            assertTrue(gate > lastGate, r.id() + " EP gate " + gate + " should exceed " + lastGate);
            lastGate = gate;
        }
    }

    @Test
    void everyEvolutionAsksForEpAndAnotherThingAndTheNaturalSkillIsOwned() {
        for (RaceTemplate r : load().values()) {
            assertTrue(r.intrinsic().contains(r.natural()) || r.resistances().contains(r.natural()), r.id() + " natural skill is not one of its skills");
            RaceTemplate.Stats s = r.stats();
            assertTrue(s.auraMin() <= s.auraMax() && s.magiculeMin() <= s.magiculeMax());
            if (!r.evolvesTo().isEmpty()) {
                assertFalse(r.evolution().isEmpty(), r.id() + " evolves for free");
                assertTrue(r.evolution().rules().size() >= 3, r.id() + " should ask for more than one thing");
                assertTrue(r.evolution().rules().stream().anyMatch(x -> x instanceof EvolutionRules.EpGate), r.id() + " has no EP gate");
            }
            double share = 0;
            for (RaceEvolutionRule x : r.evolution().rules()) share += r.evolution().sharePercent(x);
            assertTrue(r.evolution().isEmpty() || Math.abs(share - 100.0) < 1e-6, r.id() + " shares add up to " + share);
        }
    }

    @Test
    void theHighestSaiyansEvolutionHasAWayThatDoesNotNeedTheRecordsSystem() {
        RaceTemplate high = load().get("high_saiyan");
        boolean hasAnyOfWithKills = false;
        for (RaceEvolutionRule r : high.evolution().rules()) {
            if (r instanceof EvolutionRules.RecordUnlocked) assertTrue(false, "a bare record rule would block the evolution while the records system is absent");
            if (r instanceof EvolutionRules.AnyOf a && a.rules().stream().anyMatch(x -> x instanceof EvolutionRules.Kills) && a.rules().stream().anyMatch(x -> x instanceof EvolutionRules.RecordUnlocked))
                hasAnyOfWithKills = true;
        }
        assertTrue(hasAnyOfWithKills);
    }

    @Test
    void theTransformationStatesAreReachableAndThemedAsTheOwnerAsked() {
        Map<String, RaceTemplate> t = load();
        for (RaceTemplate r : t.values()) {
            for (TransformSpec sp : r.transformations().values()) {
                assertNotEquals(TransformState.BASE, sp.state());
                for (TransformState from : sp.from()) {
                    assertTrue(from == TransformState.BASE || r.transformations().containsKey(from), r.id() + "." + sp.state().id + " is entered from " + from.id + ", which the race does not have");
                }
                assertFalse(sp.entries().isEmpty());
                assertTrue(sp.durationTicks() > 0, "every state must end by itself");
                assertTrue(sp.cooldownTicks() > 0 || sp.state() == TransformState.BOOSTED, r.id() + "." + sp.state().id + " has no rest");
                assertTrue(sp.auraDrainPerSecond() >= 0 && sp.auraDrainPerSecond() <= 0.05);
                for (StateModifier m : sp.modifiers()) assertTrue(RaceAttributeKeys.known(m.attribute()));
            }
        }
        for (String id : List.of("lesser_saiyan", "medium_saiyan", "high_saiyan", "divine_saiyan")) {
            RaceTemplate r = t.get(id);
            TransformSpec boosted = r.spec(TransformState.BOOSTED), ape = r.spec(TransformState.GREAT_APE);
            assertNotNull(boosted, id + " has no ki boost");
            assertTrue(boosted.kiScaled(), id + ": the ki boost must scale down beside Kaioken");
            assertNotNull(ape, id + " has no Great Ape");
            assertTrue(ape.endsAtDawn(), "the ape ends with the night");
            assertTrue(ape.darkRukh());
            assertTrue(ape.modifiers().stream().anyMatch(m -> m.attribute().equals("scale") && m.amount() > 0), "the ape is bigger");
            assertTrue(ape.entries().stream().anyMatch(e -> e.auto() && e.moon() == com.newuniverse.nusmp.elite.races.state.MoonRule.FULL_MOON), id + ": the moon raises the ape");
            assertTrue(ape.entries().stream().allMatch(e -> e.auto() ? e.moon() != com.newuniverse.nusmp.elite.races.state.MoonRule.ANY : e.epMin() > 0), id + ": an at-will ape needs a power gate");
        }
        // the ascension opens at tier 2 and is first reached in a rage
        assertNull(t.get("lesser_saiyan").spec(TransformState.ASCENDED));
        for (String id : List.of("medium_saiyan", "high_saiyan", "divine_saiyan")) {
            TransformSpec asc = t.get(id).spec(TransformState.ASCENDED);
            assertNotNull(asc, id);
            assertTrue(asc.entries().stream().anyMatch(e -> e.auto() && e.hpBelow() < 1.0 && e.epMin() > 0), id + ": the rage awakening needs low health and an EP gate");
        }
        // the Divine state belongs to the top tier alone and is the strongest, rarest form
        for (String id : List.of("lesser_saiyan", "medium_saiyan", "high_saiyan", "primordial")) assertNull(t.get(id).spec(TransformState.DIVINE), id);
        TransformSpec divine = t.get("divine_saiyan").spec(TransformState.DIVINE);
        TransformSpec ascended = t.get("divine_saiyan").spec(TransformState.ASCENDED);
        assertNotNull(divine);
        assertEquals(4, divine.minTier());
        assertTrue(divine.cooldownTicks() > ascended.cooldownTicks());
        assertTrue(divine.durationTicks() < ascended.durationTicks());
        assertTrue(amount(divine, "attack_damage") > amount(ascended, "attack_damage"));
        assertTrue(amount(ascended, "attack_damage") > amount(t.get("divine_saiyan").spec(TransformState.BOOSTED), "attack_damage"));
        // every tier's boost is stronger than the previous tier's
        double last = 0;
        for (String id : List.of("lesser_saiyan", "medium_saiyan", "high_saiyan", "divine_saiyan")) {
            double a = amount(t.get(id).spec(TransformState.BOOSTED), "attack_damage");
            assertTrue(a > last, id);
            last = a;
        }
    }

    private static double amount(TransformSpec s, String attribute) {
        for (StateModifier m : s.modifiers()) if (m.attribute().equals(attribute)) return m.amount();
        return 0.0;
    }

    @Test
    void thePrimordialRaceIsASeparateMagiculeRichLineWithItsOwnRite() {
        RaceTemplate p = load().get("primordial");
        assertEquals("primordial", p.line());
        assertTrue(p.evolution().isEmpty(), "nothing to evolve into");
        assertFalse(p.entry().isEmpty(), "the Primordial Rite");
        assertTrue(p.entry().rules().stream().anyMatch(x -> x instanceof EvolutionRules.Item it && it.consume()), "the rite offers an item");
        RaceTemplate top = load().get("divine_saiyan");
        assertTrue(p.stats().magiculeMin() > top.stats().magiculeMax() * 10, "pure mana: far more magicule than any Saiyan");
        assertTrue(p.stats().auraMax() < top.stats().auraMax(), "a mana being has less ki than the top Saiyan");
        assertTrue(p.stats().spiritualHealth() > top.stats().spiritualHealth());
        assertNull(p.spec(TransformState.GREAT_APE));
        assertNotNull(p.spec(TransformState.BOOSTED));
        assertFalse(p.spec(TransformState.BOOSTED).kiScaled(), "mana is not ki: Kaioken does not scale it");
        assertTrue(p.intrinsic().contains("tensura:mana_manipulation"));
        assertTrue(p.traits().isEmpty());
        // the Saiyan line has no rite
        for (String id : List.of("lesser_saiyan", "medium_saiyan", "high_saiyan", "divine_saiyan")) assertTrue(load().get(id).entry().isEmpty(), id);
    }

    @Test
    void theExtraAttributesNeverRepeatWhatTheStatsSet() {
        for (RaceTemplate r : load().values()) {
            for (RaceTemplate.Extra e : r.attributes()) {
                assertTrue(RaceAttributeKeys.known(e.attribute()), e.attribute());
                assertFalse(RaceAttributeKeys.RESERVED_BY_STATS.contains(e.attribute()), r.id() + " repeats " + e.attribute());
            }
        }
    }

    // ---------------------------------------------------------------- the parser's refusals, on a copy of a real file
    private static JsonObject copyOfLesser() {
        try (var in = RaceTemplates.class.getResourceAsStream(RaceTemplates.DIR + "lesser_saiyan.json")) {
            return JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }

    private static void refuses(JsonObject o, String fragment) {
        try {
            RaceTemplateParser.parse("lesser_saiyan", o);
            assertTrue(false, "accepted a bad file (expected a problem with '" + fragment + "')");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(fragment), "message '" + e.getMessage() + "' should contain '" + fragment + "'");
        }
    }

    @Test
    void theParserRefusesBrokenFiles() {
        JsonObject o = copyOfLesser();
        assertNotNull(RaceTemplateParser.parse("lesser_saiyan", o));
        try { RaceTemplateParser.parse("Bad Id", o); assertTrue(false, "bad id accepted"); } catch (IllegalArgumentException e) { assertTrue(e.getMessage().contains("lowercase")); }

        o = copyOfLesser();
        o.getAsJsonObject("stats").addProperty("aura_min", 5000);
        refuses(o, "aura_min is above aura_max");

        o = copyOfLesser();
        o.addProperty("natural_skill", "tensura:other");
        refuses(o, "natural_skill");

        o = copyOfLesser();
        o.addProperty("tier", 9);
        refuses(o, "outside");

        o = copyOfLesser();
        o.getAsJsonObject("transformations").getAsJsonObject("boosted").getAsJsonArray("modifiers").get(0).getAsJsonObject().addProperty("attribute", "not_an_attribute");
        refuses(o, "unknown attribute");

        o = copyOfLesser();
        o.getAsJsonObject("transformations").add("base", new JsonObject());
        refuses(o, "not a transformation state");

        o = copyOfLesser();
        o.getAsJsonObject("transformations").getAsJsonObject("great_ape").getAsJsonArray("entries").get(0).getAsJsonObject().addProperty("moon", "any");
        o.getAsJsonObject("transformations").getAsJsonObject("great_ape").getAsJsonArray("entries").get(0).getAsJsonObject().addProperty("sky", false);
        refuses(o, "auto");

        o = copyOfLesser();
        o.getAsJsonObject("transformations").getAsJsonObject("boosted").addProperty("rukh", "red");
        refuses(o, "rukh");

        o = copyOfLesser();
        o.getAsJsonObject("transformations").getAsJsonObject("boosted").addProperty("color", "gold");
        refuses(o, "color");

        o = copyOfLesser();
        o.addProperty("evolves_to", "lesser_saiyan");
        refuses(o, "itself");

        o = copyOfLesser();
        com.google.gson.JsonArray traits = new com.google.gson.JsonArray();
        traits.add("flying");
        o.add("traits", traits);
        refuses(o, "unknown trait");

        o = copyOfLesser();
        com.google.gson.JsonArray attrs = new com.google.gson.JsonArray();
        JsonObject dup = new JsonObject();
        dup.addProperty("attribute", "max_health");
        dup.addProperty("amount", 5);
        dup.addProperty("op", "add");
        attrs.add(dup);
        o.add("attributes", attrs);
        refuses(o, "set by 'stats'");
    }
}
