package com.newuniverse.nusmp.labyrinth.mega.eco;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the faction layer is mostly DATA (eleven faction files, eleven entity-type tags, two item tags, the cooking recipes it relies on), and a wrong
 * letter in data fails SILENTLY in the game: a tag entry with a typo is just a creature that never joins its faction, a rival that names no faction is
 * ignored, a raw cut without a cooking recipe is meat nobody can eat. This test reads the shipped files the way the game would and fails the build
 * on a mistake: the eleven contract ids are present and parse with no complaint, every rivalry is written on both sides, every theme has a mix of
 * factions, every entity id exists (vanilla from the dumped id list, Tensura from the owner's id reference, our own from the registrations, Ice and
 * Fire from the ids the Living Dungeon tags already trust), no boss and no excluded creature is a member, every creature of the toughness tiers has a
 * faction (or is a grazer), every raw cut cooks on a campfire and in a smoker, and the marker strings of {@link TurfMarks} and the stone ids of
 * {@link FactionLoot} still match the code they were copied from. Pure (java.nio, regular expressions, the tiny reader), working directory = project root.
 */
class FactionDataTest {
    private static final Path DATA = Path.of("src/main/resources/data");
    private static final Path FACTIONS = DATA.resolve("nusmp/mega/factions");
    private static final Path TAGS = DATA.resolve("nusmp/tags/entity_type/mega");
    private static final Path DUNGEON = DATA.resolve("nusmp/tags/entity_type/dungeon");
    private static final Path RECIPES = DATA.resolve("nusmp/recipe");
    private static final Path JAVA = Path.of("src/main/java");
    private static final Pattern LOCATION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final List<String> THEMES = new ArrayList<>();

    static {
        for (RegionTheme t : RegionTheme.values()) THEMES.add(t.id());
    }

    // ---------------------------------------------------------------- reading
    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static Map<String, Object> json(Path p) throws IOException {
        try {
            return MiniJson.object(read(p));
        } catch (IllegalArgumentException e) {
            throw new AssertionError(p + " is not valid JSON: " + e.getMessage());
        }
    }

    private static List<String> lines(String resource) throws IOException {
        try (InputStream in = FactionDataTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "test resource " + resource + " is missing");
            List<String> out = new ArrayList<>();
            for (String l : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) if (!l.isBlank()) out.add(l.trim());
            return out;
        }
    }

    /** id -> definition, parsed with the real parser (complaints fail the test). */
    private static Map<String, FactionDef> defs() throws IOException {
        Map<String, FactionDef> out = new TreeMap<>();
        try (Stream<Path> files = Files.list(FACTIONS)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json")).sorted()::iterator) {
                String id = f.getFileName().toString().replace(".json", "");
                FactionParser.Result r = FactionParser.parse(id, json(f));
                assertTrue(r.problems().isEmpty(), id + ".json: " + r.problems());
                out.put(id, r.def());
            }
        }
        return out;
    }

    private static FactionGraph graph() throws IOException { return FactionGraph.of(defs().values()); }

    /** Faction id -> the entity ids of its tag file, in file order. */
    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> members() throws IOException {
        Map<String, List<String>> out = new TreeMap<>();
        for (String id : FactionIds.CONTRACT) {
            Map<String, Object> tag = json(TAGS.resolve(id + ".json"));
            assertEquals(Boolean.FALSE, tag.get("replace"), id + ": replace must be false so other packs can add to the tag");
            List<Object> values = (List<Object>) tag.get("values");
            assertNotNull(values, id + ": no values");
            List<String> ids = new ArrayList<>();
            for (Object o : values) {
                Map<String, Object> entry = (Map<String, Object>) o;
                assertEquals(Boolean.FALSE, entry.get("required"), id + ": " + entry.get("id") + " must be required:false so a missing mod never breaks the tag");
                assertEquals(2, entry.size(), id + ": a tag entry is exactly id and required: " + entry);
                ids.add((String) entry.get("id"));
            }
            out.put(id, ids);
        }
        return out;
    }

    private static Set<String> dungeonTag(String name) throws IOException {
        Set<String> out = new TreeSet<>();
        for (Object o : (List<?>) json(DUNGEON.resolve(name + ".json")).get("values")) out.add((String) ((Map<?, ?>) o).get("id"));
        return out;
    }

    // ---------------------------------------------------------------- the faction files
    @Test void theElevenContractFactionsAreShippedAndNothingElse() throws IOException {
        Set<String> found = new TreeSet<>();
        try (Stream<Path> files = Files.list(FACTIONS)) {
            files.forEach(p -> found.add(p.getFileName().toString()));
        }
        Set<String> expected = new TreeSet<>();
        for (String id : FactionIds.CONTRACT) expected.add(id + ".json");
        assertEquals(expected, found);
    }

    @Test void everyFileParsesWithNoComplaintAndAgreesWithItsName() throws IOException {
        for (Map.Entry<String, FactionDef> e : defs().entrySet()) {
            FactionDef d = e.getValue();
            assertEquals(e.getKey(), d.id());
            assertEquals("nusmp:mega/" + e.getKey(), d.tag(), "the tag of " + e.getKey() + " is the contract's name");
            assertTrue(Files.exists(TAGS.resolve(e.getKey() + ".json")), "the entity-type tag file of " + e.getKey() + " is missing");
            assertTrue(d.name().length() >= 6, e.getKey() + " needs a real display name");
            Map<String, Object> raw = json(FACTIONS.resolve(e.getKey() + ".json"));
            assertEquals(e.getKey(), raw.get("id"), "the id field must repeat the file name");
            assertTrue(raw.get("why") instanceof String why && why.length() >= 80, e.getKey() + ".json needs a 'why' of at least a sentence or two (the owner's WHY rule)");
            assertTrue(d.range() != FactionDef.UNSET && d.nativeBonus() != FactionDef.UNSET, e.getKey() + " should state its aggro numbers");
        }
    }

    @Test void everyRivalryAndAllianceIsWrittenOnBothSides() throws IOException {
        Map<String, FactionDef> defs = defs();
        for (FactionDef d : defs.values()) {
            for (String r : d.rivals()) {
                assertTrue(defs.containsKey(r), d.id() + " lists unknown rival " + r);
                assertTrue(defs.get(r).rivals().contains(d.id()), d.id() + " lists " + r + " as a rival but " + r + ".json does not list " + d.id() + " back");
            }
            for (String a : d.allies()) {
                assertTrue(defs.containsKey(a), d.id() + " lists unknown ally " + a);
                assertTrue(defs.get(a).allies().contains(d.id()), d.id() + " lists " + a + " as an ally but " + a + ".json does not list " + d.id() + " back");
                assertFalse(d.rivals().contains(a), d.id() + " lists " + a + " as both rival and ally");
            }
            assertFalse(d.oneWay(), d.id() + ": the shipped factions mirror their rivalries");
        }
    }

    @Test void theGraphIsTheDesignsTable() throws IOException {
        FactionGraph g = graph();
        assertTrue(g.problems().isEmpty(), g.problems().toString());
        assertEquals(11, g.size());
        for (String[] pair : new String[][] {{"undead", "goblin"}, {"undead", "beast"}, {"ogre", "goblin"}, {"aquatic", "beast"}, {"insect", "beast"}, {"elemental", "construct"}}) {
            assertTrue(g.hostile(pair[0], pair[1]), pair[0] + " vs " + pair[1]);
            assertTrue(g.hostile(pair[1], pair[0]), pair[1] + " vs " + pair[0]);
        }
        assertTrue(g.allied("cultist", "demon"), "cultists summon demons: allies");
        assertTrue(g.alliesOf("cultist").equals(Set.of("demon")) && g.alliesOf("demon").equals(Set.of("cultist")));
        assertTrue(g.rivalsOf("slime").isEmpty(), "slimes tolerate everyone");
        for (String other : g.ids()) assertFalse(g.hostile(other, "slime"), "nobody hunts a slime: " + other);
        assertEquals(Set.of("elemental"), g.rivalsOf("construct"), "constructs ignore everyone but the elementals that unmake them");
        assertEquals(Set.of("construct"), g.rivalsOf("elemental"), "elementals ignore all but constructs");
        assertTrue(g.def("construct").territorial(), "constructs defend their own room");
        for (String id : g.ids()) if (!id.equals("construct")) assertFalse(g.def(id).territorial(), id + " is not territorial");
        for (String id : g.ids()) assertFalse(g.hostile(id, id) || g.allied(id, id));
    }

    @Test void everyFactionIsInAtLeastOneRelationExceptTheTolerantSlimes() throws IOException {
        FactionGraph g = graph();
        for (String id : g.ids()) if (!id.equals("slime")) assertTrue(!g.rivalsOf(id).isEmpty() || !g.alliesOf(id).isEmpty(), id + " has no relation to anyone");
    }

    @Test void everyThemeNamedByAFileIsARealThemeAndEveryThemeIsWeighed() throws IOException {
        for (FactionDef d : defs().values()) {
            assertEquals(new TreeSet<>(THEMES), new TreeSet<>(d.themeWeights().keySet()), d.id() + " should give a weight to each of the nine themes");
            double total = 0;
            for (double w : d.themeWeights().values()) {
                assertTrue(w >= 0 && w <= FactionParser.MAX_WEIGHT, d.id() + " weight " + w);
                total += w;
            }
            assertTrue(total >= 12, d.id() + " should like some theme (total weight " + total + ")");
        }
    }

    @Test void everyThemeHasAMixOfFactionsInThisPackage() throws IOException {
        FactionGraph g = graph();
        for (String theme : THEMES) {
            int liked = 0;
            for (String id : g.ids()) if (g.themeWeight(id, theme) > 0) liked++;
            assertTrue(liked >= 6, "the " + theme + " theme lets only " + liked + " factions in");
            Map<String, Integer> count = new HashMap<>();
            int rooms = 600;
            for (int room = 0; room < rooms; room++) count.merge(g.pickNative(0x51ED270B0B0CL ^ room, theme), 1, Integer::sum);
            assertTrue(count.size() >= 5, "600 rooms of the " + theme + " theme used only " + count);
            for (Map.Entry<String, Integer> e : count.entrySet()) assertTrue(e.getValue() <= rooms * 0.45, theme + ": " + e.getKey() + " takes " + e.getValue() + " of " + rooms + " rooms");
        }
    }

    @Test void theThemesFlavourTheFactionsTheWayTheDesignSays() throws IOException {
        FactionGraph g = graph();
        assertTrue(g.themeWeight("undead", "blood") > g.themeWeight("undead", "verdant"));
        assertTrue(g.themeWeight("demon", "flame") > g.themeWeight("demon", "frost"));
        assertTrue(g.themeWeight("beast", "frost") > g.themeWeight("beast", "flame"));
        assertTrue(g.themeWeight("insect", "verdant") > g.themeWeight("insect", "astral"));
        assertTrue(g.themeWeight("elemental", "storm") > g.themeWeight("elemental", "blood"));
        assertTrue(g.themeWeight("construct", "astral") > g.themeWeight("construct", "verdant"));
        assertTrue(g.themeWeight("cultist", "spatial") > g.themeWeight("cultist", "gravity"));
    }

    @Test void theDropBlocksAreSane() throws IOException {
        for (FactionDef d : defs().values()) {
            assertTrue(FactionDef.CUTS.contains(d.meat()), d.id() + ": " + d.meat());
            assertTrue(d.meatChance() >= 0.0 && d.meatChance() <= 0.8, d.id() + " meatChance " + d.meatChance());
        }
        assertEquals(0.0, defs().get("elemental").meatChance(), "spirits have no flesh");
        assertEquals("raw_direwolf_haunch", defs().get("beast").meat());
        assertEquals("raw_slime_jelly", defs().get("slime").meat());
        assertEquals("raw_armor_flank", defs().get("construct").meat());
        assertEquals("raw_basilisk_fillet", defs().get("aquatic").meat());
        assertEquals("raw_basilisk_fillet", defs().get("insect").meat());
    }

    // ---------------------------------------------------------------- the entity-type tags
    @Test void tagFilesAreWellFormedAndBigEnough() throws IOException {
        Map<String, List<String>> m = members();
        int total = 0;
        for (Map.Entry<String, List<String>> e : m.entrySet()) {
            assertTrue(e.getValue().size() >= 3, e.getKey() + " lists only " + e.getValue().size() + " creatures");
            assertEquals(e.getValue().size(), new HashSet<>(e.getValue()).size(), e.getKey() + " lists a creature twice");
            for (String id : e.getValue()) assertTrue(LOCATION.matcher(id).matches(), e.getKey() + ": '" + id + "' is not a resource location");
            total += e.getValue().size();
        }
        assertTrue(total >= 100, "generous membership expected, found " + total);
        try (Stream<Path> files = Files.list(TAGS)) {
            assertEquals(11L, files.count(), "exactly the eleven tag files");
        }
    }

    @Test void noCreatureIsInTwoFactions() throws IOException {
        Map<String, String> owner = new HashMap<>();
        for (Map.Entry<String, List<String>> e : members().entrySet())
            for (String id : e.getValue()) {
                String previous = owner.put(id, e.getKey());
                assertNull(previous, id + " is in both " + previous + " and " + e.getKey());
            }
    }

    @Test void everyCreatureIdExists() throws IOException {
        Set<String> vanilla = new HashSet<>(lines("/mega/eco/vanilla_entities.txt")), tensura = new HashSet<>(lines("/mega/eco/tensura_entities.txt"));
        Set<String> ours = registeredEntities();
        Set<String> iceAndFire = new TreeSet<>();
        try (Stream<Path> files = Files.list(DUNGEON)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator)
                for (String id : dungeonTag(f.getFileName().toString().replace(".json", ""))) if (id.startsWith("iceandfire:")) iceAndFire.add(id);
        }
        assertTrue(iceAndFire.size() >= 20, "the Living Dungeon tags should hold the trusted Ice and Fire ids, found " + iceAndFire.size());
        List<String> bad = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : members().entrySet()) {
            for (String id : e.getValue()) {
                boolean ok = switch (id.substring(0, id.indexOf(':'))) {
                    case "minecraft" -> vanilla.contains(id);
                    case "tensura" -> tensura.contains(id);
                    case "nusmp" -> ours.contains(id.substring(6));
                    case "iceandfire" -> iceAndFire.contains(id);
                    default -> false;
                };
                if (!ok) bad.add(e.getKey() + " -> " + id);
            }
        }
        assertTrue(bad.isEmpty(), "ids that no loaded mod is known to register: " + bad);
    }

    /** The paths of this mod's entity types: every {@code ENTITIES.register("path"} in the Java sources. */
    private static Set<String> registeredEntities() throws IOException {
        Pattern p = Pattern.compile("ENTITIES\\.register\\(\\s*\"([a-z0-9_]+)\"");
        Set<String> out = new TreeSet<>();
        try (Stream<Path> files = Files.walk(JAVA)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java"))::iterator) {
                Matcher m = p.matcher(read(f));
                while (m.find()) out.add(m.group(1));
            }
        }
        assertTrue(out.contains("living_armor") && out.contains("dark_disciple"), "the registration scan found " + out);
        return out;
    }

    @Test void noBossNoExcludedAndNoLegendaryCreatureIsAMember() throws IOException {
        Set<String> banned = new TreeSet<>(dungeonTag("excluded"));
        banned.addAll(dungeonTag("legendary"));
        for (String boss : new String[] {"floor_warden", "goliath", "charybdis_spawn", "devil_avatar", "dante_zogratis", "vanica_zogratis", "zenon_zogratis", "lucius_zogratis",
                "zagred", "grimoire_daemon", "spirit_lord", "elemental_spirit_lord", "wind_spirit_lord", "god_of_order", "mirror_double", "zogratis_clone", "blood_beast",
                "rouge", "painted_construct", "tower_wizard", "player_avatar", "magicule_scale", "cotton_sheep", "cotton_cloud"})
            banned.add("nusmp:" + boss);
        banned.addAll(List.of("minecraft:ender_dragon", "minecraft:wither", "minecraft:warden", "minecraft:player", "tensura:orc_lord", "tensura:orc_disaster", "tensura:ifrit"));
        for (Map.Entry<String, List<String>> e : members().entrySet())
            for (String id : e.getValue()) assertFalse(banned.contains(id), id + " in " + e.getKey() + " is a boss, a servant type, an NPC or excluded from the roster");
    }

    @Test void everyCreatureOfTheToughnessTiersHasAFactionUnlessItIsAGrazer() throws IOException {
        Set<String> all = new TreeSet<>();
        for (List<String> l : members().values()) all.addAll(l);
        Set<String> grazers = dungeonTag("grazers");
        List<String> missing = new ArrayList<>();
        for (String tier : new String[] {"tier1", "tier2", "tier3"})
            for (String id : dungeonTag(tier)) if (!all.contains(id) && !grazers.contains(id)) missing.add(tier + " " + id);
        assertTrue(missing.isEmpty(), "creatures of the roster without a faction (add them to a mega tag or to the grazers): " + missing);
    }

    @Test void theFactionsHaveTheCreaturesTheDesignNamed() throws IOException {
        Map<String, List<String>> m = members();
        assertTrue(m.get("undead").containsAll(List.of("minecraft:zombie", "minecraft:skeleton", "minecraft:husk", "minecraft:drowned")));
        assertTrue(m.get("insect").containsAll(List.of("minecraft:spider", "minecraft:cave_spider", "minecraft:silverfish", "minecraft:endermite")));
        assertTrue(m.get("slime").containsAll(List.of("minecraft:slime", "minecraft:magma_cube", "tensura:slime")));
        assertTrue(m.get("cultist").containsAll(List.of("minecraft:witch", "minecraft:evoker")));
        assertTrue(m.get("elemental").contains("minecraft:blaze"));
        assertTrue(m.get("aquatic").contains("minecraft:guardian"));
        assertTrue(m.get("goblin").contains("tensura:goblin"));
        assertTrue(m.get("construct").contains("nusmp:living_armor"));
        assertTrue(m.get("beast").contains("tensura:direwolf"));
        assertTrue(m.get("ogre").contains("tensura:orc"));
        assertTrue(m.get("demon").contains("tensura:lesser_daemon"));
    }

    // ---------------------------------------------------------------- the economy and the cooking
    @Test void everyRawCutCooksOnACampfireAndInASmoker() throws IOException {
        Map<String, Set<String>> typesByCut = new HashMap<>();
        try (Stream<Path> files = Files.list(RECIPES)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                Map<String, Object> r = json(f);
                if (!(r.get("ingredient") instanceof Map<?, ?> ing) || !(ing.get("item") instanceof String item)) continue;
                typesByCut.computeIfAbsent(item, k -> new TreeSet<>()).add(String.valueOf(r.get("type")));
            }
        }
        for (String cut : FactionDef.CUTS) {
            Set<String> types = typesByCut.getOrDefault("nusmp:" + cut, Set.of());
            assertTrue(types.contains("minecraft:campfire_cooking"), cut + " has no campfire_cooking recipe (Farmer's Delight's stove and skillet use the same recipe type): " + types);
            assertTrue(types.contains("minecraft:smoking"), cut + " has no smoking recipe: " + types);
        }
    }

    @Test void theRawCutsAreTheFiveItemsOfDungeonFood() throws IOException {
        String food = read(JAVA.resolve("com/newuniverse/nusmp/dungeon/DungeonFood.java"));
        for (String cut : FactionDef.CUTS) assertTrue(food.contains("raw(\"" + cut + "\""), cut + " is not registered by DungeonFood");
        Matcher m = Pattern.compile("raw\\(\"(raw_[a-z_]+)\"").matcher(food);
        Set<String> registered = new TreeSet<>();
        while (m.find()) registered.add(m.group(1));
        assertEquals(new TreeSet<>(FactionDef.CUTS), registered, "DungeonFood gained or lost a raw cut: update FactionDef.CUTS and the faction files");
    }

    @Test void theCommonItemTagsListTheFiveCutsAndReplaceNothing() throws IOException {
        for (String file : new String[] {"c/tags/item/foods/raw_meat.json", "c/tags/item/raw_meats.json"}) {
            Map<String, Object> tag = json(DATA.resolve(file));
            assertEquals(Boolean.FALSE, tag.get("replace"), file);
            Set<String> values = toStrings((List<?>) tag.get("values"));
            Set<String> expected = new TreeSet<>();
            for (String cut : FactionDef.CUTS) expected.add("nusmp:" + cut);
            assertEquals(expected, values, file);
        }
    }

    private static Set<String> toStrings(List<?> l) {
        Set<String> out = new TreeSet<>();
        for (Object o : l) out.add((String) o);
        return out;
    }

    @Test void theStoneIdsAreTheBaseEconomysOwn() throws IOException {
        String drops = read(JAVA.resolve("com/newuniverse/nusmp/labyrinth/dnd/economy/EconomyDrops.java"));
        Matcher m = Pattern.compile("tensura\\(\"([a-z_]+)\"\\)").matcher(drops);
        Set<String> base = new TreeSet<>();
        while (m.find()) base.add(m.group(1));
        assertEquals(new TreeSet<>(List.of(FactionLoot.STONE_PATH, FactionLoot.LOW_PATH, FactionLoot.MEDIUM_PATH, FactionLoot.HIGH_PATH)), base,
                "EconomyDrops changed its item ids: update FactionLoot and FactionDrops to match");
    }

    @Test void theMarkerStringsAreStillSetWhereTheyWereCopiedFrom() throws IOException {
        StringBuilder all = new StringBuilder();
        try (Stream<Path> files = Files.walk(JAVA)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java") && !x.toString().replace('\\', '/').contains("/mega/eco/"))::iterator) all.append(read(f)).append('\n');
        }
        String code = all.toString();
        // nusmp_wanderer is set by the encounter director (another builder of this feature) and nusmp_servant is a convention of this package: neither is checked here
        for (String mark : List.of("nusmp_peak_boss", "nusmp_floor_boss", "nusmp_juggernaut", "nusmp_charybdis", "nusmp_underworld", "nusmp_paladin", "nusmp_paladin_of",
                "nusmp_paladin_turn", "nusmp_shadow", "nusmp_relic_owner", "nusmp_bull_owner"))
            assertTrue(code.contains("\"" + mark + "\""), "no code sets or reads \"" + mark + "\" any more: update TurfMarks");
        assertTrue(code.contains("\"floor_warden\""), "the Floor Warden registration moved: update TurfMarks.WARDEN_ID");
    }
}
