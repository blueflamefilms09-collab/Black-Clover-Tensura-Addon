package com.newuniverse.nusmp.avatar;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.avatar.dnd.Stat;
import com.newuniverse.nusmp.avatar.race.AvatarRace;
import com.newuniverse.nusmp.avatar.race.AvatarRaceParser;
import com.newuniverse.nusmp.avatar.race.ConditionEvaluator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Why: races are hand-written datapack JSON, so the parser is checked on bad input and every shipped file is parsed; the connection tags are checked with a fake player. */
class AvatarRaceTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/avatar_races");

    private static Map<String, AvatarRace> shipped() throws IOException {
        Map<String, AvatarRace> out = new HashMap<>();
        try (Stream<Path> files = Files.list(DIR)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                String id = f.getFileName().toString().replace(".json", "");
                out.put(id, AvatarRaceParser.parse(id, JsonParser.parseString(Files.readString(f)).getAsJsonObject()));
            }
        }
        return out;
    }

    @Test
    void atLeastTenRacesShipAndAllParse() throws IOException {
        Map<String, AvatarRace> races = shipped();
        assertTrue(races.size() >= 10, "shipped " + races.size());
        for (String must : new String[]{"demon_host", "human", "elf", "dwarf", "ogre", "goblin", "lizardman", "beastman", "fallen_angel", "spirit", "devil_vessel"})
            assertTrue(races.containsKey(must), must);
    }

    @Test
    void everyEvolutionTargetExistsAndIsNotSelectableUnlessItIsAlsoAStart() throws IOException {
        Map<String, AvatarRace> races = shipped();
        for (AvatarRace r : races.values()) {
            if (r.evolves()) assertTrue(races.containsKey(r.evolvesTo()), r.id() + " evolves into a missing race " + r.evolvesTo());
            assertNotEquals(r.id(), r.evolvesTo(), "a race must not evolve into itself");
        }
    }

    @Test
    void noEvolutionChainLoops() throws IOException {
        Map<String, AvatarRace> races = shipped();
        for (AvatarRace start : races.values()) {
            AvatarRace r = start;
            for (int i = 0; i < 20 && r != null && r.evolves(); i++) r = races.get(r.evolvesTo());
            assertFalse(r != null && r.evolves(), "loop from " + start.id());
        }
    }

    @Test
    void everyShippedConditionIsKnown() throws IOException {
        for (AvatarRace r : shipped().values())
            for (String c : r.connectionConditions()) {
                List<String> warnings = new ArrayList<>();
                ConditionEvaluator.test(new FakePlayer(), c, warnings::add);
                assertTrue(warnings.isEmpty(), r.id() + ": " + c + " -> " + warnings);
            }
    }

    @Test
    void demonHostIsExactlyTheOwnersExample() throws IOException {
        AvatarRace r = shipped().get("demon_host");
        assertEquals("Demon Host", r.displayName());
        assertEquals(1.25f, r.defaultScale(), 1e-6);
        assertTrue(r.requiresDndRoll());
        assertEquals("devil_vessel", r.evolvesTo());
        assertTrue(r.connectionConditions().contains("black_clover:devil_contract"));
        assertEquals(1.15, r.multipliers().get(Stat.STR), 1e-9);
        assertTrue(r.baseEp() > 0 && r.baseMagiculeCost() > 0);
    }

    @Test
    void defaultsFillAMinimalFile() {
        AvatarRace r = AvatarRaceParser.parse("plain", JsonParser.parseString("{}").getAsJsonObject());
        assertEquals(1.0f, r.defaultScale());
        assertTrue(r.requiresDndRoll() && r.selectable());
        assertTrue(r.connectionConditions().isEmpty() && r.allowedPersonalities().isEmpty());
        assertFalse(r.evolves());
        assertTrue(r.allows(null));
    }

    @Test
    void namespacedEvolutionIdsLoseTheNamespace() {
        JsonObject o = JsonParser.parseString("{\"evolves_to_race_id\":\"nusmp:ogre\"}").getAsJsonObject();
        assertEquals("ogre", AvatarRaceParser.parse("goblin", o).evolvesTo());
    }

    @Test
    void badFilesAreRejected() {
        for (String bad : new String[]{
                "{\"default_scale\": 9}", "{\"default_scale\": \"big\"}", "{\"stat_multipliers\":{\"luck\":1.0}}", "{\"stat_multipliers\":{\"str\":0}}",
                "{\"stat_multipliers\":{\"str\":-1}}", "{\"base_ep\": 0}", "{\"allowed_personalities\":[\"GRUMPY\"]}", "{\"appearance\":{\"skin_tone\":\"red\"}}"}) {
            JsonObject o = JsonParser.parseString(bad).getAsJsonObject();
            assertThrows(IllegalArgumentException.class, () -> AvatarRaceParser.parse("x", o), bad);
        }
        assertThrows(IllegalArgumentException.class, () -> AvatarRaceParser.parse("", new JsonObject()));
    }

    @Test
    void appearanceColoursParseFromHexStringsAndNumbers() {
        JsonObject o = JsonParser.parseString("{\"appearance\":{\"skin_tone\":\"#FF8000\",\"horn_id\":2,\"aura_rgb\":\"0x112233\"}}").getAsJsonObject();
        AvatarRace r = AvatarRaceParser.parse("x", o);
        assertEquals(0xFF8000, (int) r.appearanceHints().get("skin_tone"));
        assertEquals(2, (int) r.appearanceHints().get("horn_id"));
        assertEquals(0x112233, (int) r.appearanceHints().get("aura_rgb"));
    }

    // ---------------------------------------------------------------- the connection checker
    /** A fake player: holds a genesis core, a devil contract, 5000 EP, lives in the overworld. */
    static final class FakePlayer implements ConditionEvaluator.Ctx {
        boolean contract = true, falna = false, relic = false, grimoire = true;
        double ep = 5000;
        String dim = "minecraft:overworld";
        @Override public boolean hasItem(String id) { return id.equals("nusmp:genesis_core"); }
        @Override public boolean hasSkill(String id) { return id.equals("tensura:sage"); }
        @Override public boolean hasGrimoire() { return grimoire; }
        @Override public boolean hasMagic(String id) { return id.equalsIgnoreCase("flame"); }
        @Override public boolean hasDevilContract() { return contract; }
        @Override public boolean hasFalna() { return falna; }
        @Override public boolean hasAttunedRelic() { return relic; }
        @Override public double ep() { return ep; }
        @Override public String dimension() { return dim; }
    }

    private static boolean t(ConditionEvaluator.Ctx c, String tag) { return ConditionEvaluator.test(c, tag, w -> {}); }

    @Test
    void parameterisedTagsSplitAtTheKeyEvenWithANamespacedArgument() {
        FakePlayer p = new FakePlayer();
        assertTrue(t(p, "has_item:nusmp:genesis_core"));
        assertFalse(t(p, "has_item:minecraft:stick"));
        assertTrue(t(p, "has_skill:tensura:sage"));
        assertTrue(t(p, "has_magic:flame"));
        assertFalse(t(p, "has_magic:water"));
        assertTrue(t(p, "dimension:minecraft:overworld"));
        assertFalse(t(p, "dimension:minecraft:the_nether"));
    }

    @Test
    void namespacedKeysWithoutAnArgumentWork() {
        FakePlayer p = new FakePlayer();
        assertTrue(t(p, "black_clover:devil_contract"));
        assertFalse(t(p, "danmachi:has_falna"));
        assertFalse(t(p, "relics:requires_curios_attunement"));
        p.falna = true; p.relic = true;
        assertTrue(t(p, "danmachi:has_falna"));
        assertTrue(t(p, "relics:requires_curios_attunement"));
        assertTrue(t(p, "has_grimoire"));
    }

    @Test
    void minEpComparesAndBadNumbersFail() {
        FakePlayer p = new FakePlayer();
        assertTrue(t(p, "min_ep:5000"));
        assertFalse(t(p, "min_ep:5000.5"));
        assertFalse(t(p, "min_ep:lots"));
    }

    @Test
    void unknownTagsFailClosedAndWarn() {
        List<String> warnings = new ArrayList<>();
        assertFalse(ConditionEvaluator.test(new FakePlayer(), "has_wings:true", warnings::add));
        assertFalse(ConditionEvaluator.test(new FakePlayer(), "", warnings::add));
        assertFalse(ConditionEvaluator.test(new FakePlayer(), null, warnings::add));
        assertEquals(1, warnings.size());
    }

    @Test
    void aLeadingBangNegates() {
        FakePlayer p = new FakePlayer();
        assertFalse(t(p, "!black_clover:devil_contract"));
        assertTrue(t(p, "!danmachi:has_falna"));
        assertTrue(t(p, "!!has_grimoire"));
        assertFalse(t(p, "!no_such_tag"), "negating an unknown tag must not open the door");
    }

    @Test
    void evaluateCollectsEveryFailure() {
        FakePlayer p = new FakePlayer();
        ConditionEvaluator.Result r = ConditionEvaluator.evaluate(p, List.of("has_grimoire", "danmachi:has_falna", "min_ep:99999", "has_item:nusmp:genesis_core"), w -> {});
        assertFalse(r.ok());
        assertEquals(List.of("danmachi:has_falna", "min_ep:99999"), r.failed());
        assertTrue(ConditionEvaluator.evaluate(p, List.of(), w -> {}).ok());
        assertTrue(ConditionEvaluator.evaluate(p, null, w -> {}).ok());
    }

    @Test
    void aThrowingPredicateFailsClosed() {
        ConditionEvaluator.register("test_boom", (c, a) -> { throw new IllegalStateException("boom"); });
        List<String> warnings = new ArrayList<>();
        assertFalse(ConditionEvaluator.test(new FakePlayer(), "test_boom", warnings::add));
        assertEquals(1, warnings.size());
    }

    @Test
    void anAddOnCanRegisterItsOwnTag() {
        ConditionEvaluator.register("test_always", (c, a) -> true);
        assertTrue(ConditionEvaluator.known("test_always"));
        assertTrue(t(new FakePlayer(), "test_always"));
    }
}
