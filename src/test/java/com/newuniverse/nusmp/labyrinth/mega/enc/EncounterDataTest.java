package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the encounter tables and the roster of wandering bosses are DATA that nobody can play-test here, and a data mistake is silent in game (an entry
 * with a typo in its entity id is skipped, a cue with a misspelled sound plays nothing, a table that lacks a band quietly rolls "no usable entry"). So
 * the files that SHIP are read here, from the source tree, exactly as the loader reads them: every file must parse with zero problems, every theme must
 * have its table, every table must have 8 to 14 entries and every band, every id must be one that exists (vanilla entities, simple particles and the
 * cue sounds from the API dump, Tensura entities from its id list, nusmp entities from the registrations in the source tree), every message must be a
 * key of the Dungeon Master's namespace, and the dice must be able to answer: for every threat, party size and state of the party, every band finds an
 * entry. It also checks the two promises the tables make to a new player: at threat one nothing harder than a standard fight is on offer, and no entry
 * asks for a first monster that costs more than the entry's own band can pay at the threat where it first appears.
 */
class EncounterDataTest {
    private static final Path ENC = Path.of("src/main/resources/data/nusmp/mega/encounters");
    private static final Path WANDERERS = Path.of("src/main/resources/data/nusmp/mega/wanderers.json");
    private static final Path KNOWN = Path.of("src/test/resources/mega/enc/known_ids.txt");
    private static final Path LANG = Path.of("src/main/resources/assets/nusmp/lang/en_us.json");
    private static final Path SOURCE = Path.of("src/main/java");
    /** 1.59.0: every theme of the enum has a table, so a theme appended to {@link RegionTheme} without one fails here (the list used to be written out and had to be edited by every builder of a theme). */
    private static final List<String> THEMES = themeIds();
    private static final Set<String> NAMESPACES = Set.of("minecraft", "tensura", "nusmp");
    private static final String MESSAGE_PREFIX = "message.nusmp.mega.enc.";

    /** Default maximum health of the vanilla and nusmp bodies the tables name explicitly, to check that an entry's first monster fits its band's budget. */
    private static final Map<String, Double> HEALTH = new HashMap<>();

    static {
        String[][] h = {{"zombie", "20"}, {"husk", "20"}, {"skeleton", "20"}, {"stray", "20"}, {"wither_skeleton", "20"}, {"spider", "16"}, {"cave_spider", "12"},
                {"silverfish", "8"}, {"endermite", "8"}, {"creeper", "20"}, {"blaze", "20"}, {"breeze", "30"}, {"witch", "26"}, {"evoker", "24"}, {"vindicator", "24"},
                {"illusioner", "32"}, {"piglin_brute", "50"}, {"hoglin", "40"}, {"zoglin", "40"}, {"ravager", "100"}, {"magma_cube", "20"}, {"phantom", "20"},
                {"enderman", "40"}, {"vex", "14"}, {"shulker", "30"}, {"iron_golem", "100"}, {"polar_bear", "30"}, {"bat", "6"}, {"drowned", "20"}};
        for (String[] e : h) HEALTH.put("minecraft:" + e[0], Double.parseDouble(e[1]));
        HEALTH.put("nusmp:living_armor", 22.0);
        HEALTH.put("nusmp:floor_warden", 200.0);
    }

    // ---------------------------------------------------------------- reading the shipped files
    private static String read(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + p.toAbsolutePath() + " (run the tests from the project directory): " + e.getMessage());
        }
    }

    private static List<String> themeIds() {
        List<String> out = new ArrayList<>();
        for (RegionTheme t : RegionTheme.values()) out.add(t.id());
        return out;
    }

    private static List<String> tableNames() {
        List<String> names = new ArrayList<>();
        names.add("default");
        names.addAll(THEMES);
        return names;
    }

    private static EncounterTableParser.Parsed parse(String name) {
        return EncounterTableParser.parse(name, read(ENC.resolve(name + ".json")));
    }

    private static Map<String, EncounterTable> tables() {
        Map<String, EncounterTable> out = new TreeMap<>();
        for (String n : tableNames()) out.put(n, parse(n).table());
        return out;
    }

    private static JsonObject rawTable(String name) {
        return JsonParser.parseString(read(ENC.resolve(name + ".json"))).getAsJsonObject();
    }

    // ---------------------------------------------------------------- the registry ids that exist
    private static Set<String> known(String registry) {
        Set<String> out = new HashSet<>();
        for (String line : read(KNOWN).split("\\R")) {
            if (line.isBlank() || line.startsWith("#")) continue;
            int sp = line.indexOf(' ');
            if (sp > 0 && line.substring(0, sp).equals(registry)) out.add(line.substring(sp + 1).trim());
        }
        return out;
    }

    /** The ids the mod registers as entity types, read from the registrations in the source tree (the registrars are all called ENTITIES). */
    private static Set<String> nusmpEntities() {
        Pattern p = Pattern.compile("(?<![A-Z_])ENTITIES\\.register\\(\"([a-z0-9_]+)\"");
        Set<String> out = new HashSet<>();
        try (Stream<Path> files = Files.walk(SOURCE)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java"))::iterator) {
                Matcher m = p.matcher(read(f));
                while (m.find()) out.add("nusmp:" + m.group(1));
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot walk the source tree: " + e.getMessage());
        }
        return out;
    }

    private static boolean entityExists(String id, Set<String> vanilla, Set<String> tensura, Set<String> nusmp) {
        if (id.startsWith("minecraft:")) return vanilla.contains(id);
        if (id.startsWith("tensura:")) return tensura.contains(id);
        if (id.startsWith("nusmp:")) return nusmp.contains(id);
        return false;
    }

    private static Set<String> knownEntities() {
        Set<String> all = new HashSet<>(known("entity"));
        all.addAll(nusmpEntities());
        return all;
    }

    // ---------------------------------------------------------------- the tables
    @Test
    void everyThemeHasItsTableAndAllParseWithoutProblems() {
        assertEquals(1 + RegionTheme.values().length, tableNames().size());
        for (String n : tableNames()) {
            assertTrue(Files.exists(ENC.resolve(n + ".json")), "missing encounters/" + n + ".json");
            EncounterTableParser.Parsed p = parse(n);
            assertNotNull(p.table(), n);
            assertTrue(p.problems().isEmpty(), n + ": " + p.problems());
        }
        // nothing else is in the folder: a stray file would be read as a table of a theme that does not exist
        try (Stream<Path> files = Files.list(ENC)) {
            Set<String> found = new TreeSet<>();
            files.forEach(f -> found.add(f.getFileName().toString()));
            Set<String> want = new TreeSet<>();
            for (String n : tableNames()) want.add(n + ".json");
            assertEquals(want, found, "the folder holds exactly the default table and one table per theme");
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    @Test
    void everyTableHasEightToFourteenEntriesAndEveryBand() {
        for (Map.Entry<String, EncounterTable> t : tables().entrySet()) {
            int n = t.getValue().entries().size();
            assertTrue(n >= 8 && n <= 14, t.getKey() + " has " + n + " entries");
            Set<Band> bands = new HashSet<>();
            for (EncounterTable.Entry e : t.getValue().entries()) bands.add(e.band());
            for (Band b : new Band[] {Band.MINOR, Band.STANDARD, Band.HARD, Band.AMBUSH}) assertTrue(bands.contains(b), t.getKey() + " has no " + b.label() + " entry");
        }
    }

    @Test
    void everyTableExplainsItselfAndNamesItsFaction() {
        for (String n : tableNames()) {
            JsonObject raw = rawTable(n);
            assertTrue(raw.has("comment") && raw.get("comment").getAsString().startsWith("Why:"), n + ": the table needs a comment that starts with Why:");
            assertTrue(raw.has("description") && !raw.get("description").getAsString().isEmpty(), n + ": the table needs a description");
            EncounterTable t = parse(n).table();
            assertNotNull(t.defaultFaction(), n + ": a default faction fills the faction monsters that name none");
            JsonArray entries = raw.getAsJsonArray("entries");
            for (JsonElement el : entries) {
                JsonObject o = el.getAsJsonObject();
                assertTrue(o.has("comment") && o.get("comment").getAsString().length() > 30, n + "/" + o.get("id").getAsString() + ": every entry says why it exists");
            }
        }
    }

    @Test
    void entryIdsAreUniqueAcrossAllFiles() {
        Map<String, String> seen = new HashMap<>();
        for (Map.Entry<String, EncounterTable> t : tables().entrySet()) {
            for (EncounterTable.Entry e : t.getValue().entries()) {
                String before = seen.put(e.id(), t.getKey());
                assertTrue(before == null, "entry id '" + e.id() + "' is in both " + before + " and " + t.getKey() + " (the admin command spawns by id)");
            }
        }
    }

    @Test
    void everyEntityIdExists() {
        Set<String> vanilla = known("entity"), tensura = new HashSet<>(), nusmp = nusmpEntities();
        for (String id : vanilla) if (id.startsWith("tensura:")) tensura.add(id);
        vanilla.removeIf(id -> id.startsWith("tensura:"));
        Set<String> used = new TreeSet<>();
        for (Map.Entry<String, EncounterTable> t : tables().entrySet()) {
            for (EncounterTable.Entry e : t.getValue().entries()) {
                for (EncounterTable.MobSpec m : e.mobs()) {
                    if (m.kind() != EncounterTable.Kind.ENTITY) continue;
                    String ns = m.entity().substring(0, m.entity().indexOf(':'));
                    assertTrue(NAMESPACES.contains(ns), t.getKey() + "/" + e.id() + ": '" + m.entity() + "' is not a vanilla, Tensura or nusmp entity");
                    assertTrue(entityExists(m.entity(), vanilla, tensura, nusmp), t.getKey() + "/" + e.id() + ": no such entity type as '" + m.entity() + "'");
                    used.add(m.entity());
                }
            }
        }
        assertTrue(used.size() >= 30, "the tables name only " + used.size() + " different bodies");
        System.out.println("entity ids used by the tables (" + used.size() + "): " + used);
    }

    @Test
    void everyCueSoundAndParticleIsOneThatExists() {
        Set<String> sounds = known("sound"), particles = known("particle");
        Set<String> usedSounds = new TreeSet<>(), usedParticles = new TreeSet<>();
        for (Map.Entry<String, EncounterTable> t : tables().entrySet()) {
            for (EncounterTable.Entry e : t.getValue().entries()) {
                EncounterTable.Cue c = e.cue();
                if (!c.sound().isEmpty()) {
                    assertTrue(sounds.contains(c.sound()), t.getKey() + "/" + e.id() + ": unverified sound '" + c.sound() + "' (check it, then add it to known_ids.txt)");
                    usedSounds.add(c.sound());
                }
                if (!c.particle().isEmpty()) {
                    assertTrue(particles.contains(c.particle()), t.getKey() + "/" + e.id() + ": '" + c.particle() + "' is not a simple particle type of 1.21.1");
                    usedParticles.add(c.particle());
                }
            }
            for (JsonElement el : rawTable(t.getKey()).getAsJsonArray("entries")) {
                JsonObject o = el.getAsJsonObject();
                if (!o.has("cue")) continue;
                JsonObject cue = o.getAsJsonObject("cue");
                assertFalse(cue.has("count") && !cue.has("particle"), t.getKey() + "/" + o.get("id").getAsString() + ": a particle count without a particle");
            }
        }
        System.out.println("cue sounds used (" + usedSounds.size() + "), particles used (" + usedParticles.size() + ")");
    }

    @Test
    void messagesUseTheNamespaceAndAreUnique() {
        Set<String> keys = new HashSet<>();
        int count = 0;
        for (Map.Entry<String, EncounterTable> t : tables().entrySet()) {
            for (EncounterTable.Entry e : t.getValue().entries()) {
                if (e.message().isEmpty()) continue;
                count++;
                assertTrue(e.message().equals(MESSAGE_PREFIX + t.getKey() + "." + e.id()), t.getKey() + "/" + e.id() + ": the message key must be " + MESSAGE_PREFIX + t.getKey() + "." + e.id());
                assertTrue(keys.add(e.message()), "message key used twice: " + e.message());
            }
        }
        assertTrue(count >= 20, "only " + count + " entries carry a message; the tables are meant to be mostly silent but not mute");
    }

    /**
     * The message keys are translated in the language file once the lead merges the fragment of this builder into it. The check works one table at a time: a
     * table none of whose keys is in the file has simply not been merged yet (the test says so and passes), but a table that is merged only in part, or any
     * merged key without {@code wanderer_spawn}, fails the build, because a half-merged fragment shows raw keys to players.
     */
    @Test
    void messagesAreTranslatedOnceTheFragmentIsMerged() {
        if (!Files.exists(LANG)) return;
        JsonObject lang = JsonParser.parseString(read(LANG)).getAsJsonObject();
        List<String> missing = new ArrayList<>(), unmerged = new ArrayList<>();
        boolean anyMerged = false;
        for (Map.Entry<String, EncounterTable> t : tables().entrySet()) {
            List<String> keys = new ArrayList<>();
            for (EncounterTable.Entry e : t.getValue().entries()) if (!e.message().isEmpty()) keys.add(e.message());
            boolean some = false;
            for (String k : keys) if (lang.has(k)) some = true;
            if (some) anyMerged = true;
            if (!some) {
                if (!keys.isEmpty()) unmerged.add(t.getKey());
                continue;
            }
            for (String k : keys) if (!lang.has(k)) missing.add(k);
        }
        for (String k : lang.keySet()) if (k.startsWith(MESSAGE_PREFIX)) anyMerged = true;
        if (anyMerged && !lang.has(MESSAGE_PREFIX + "wanderer_spawn")) missing.add(MESSAGE_PREFIX + "wanderer_spawn");
        assertTrue(missing.isEmpty(), "untranslated: " + missing);
        if (!unmerged.isEmpty()) System.out.println("note: the language fragment of the tables " + unmerged + " is not merged into en_us.json yet, translation check skipped for them");
    }

    @Test
    void theFirstSectorOffersNothingHarderThanAStandardFight() {
        for (Map.Entry<String, EncounterTable> t : tables().entrySet()) {
            int offered = 0;
            for (EncounterTable.Entry e : t.getValue().entries()) {
                if (!e.fitsThreat(1)) continue;
                offered++;
                assertTrue(e.band() == Band.MINOR || e.band() == Band.STANDARD, t.getKey() + "/" + e.id() + " is a " + e.band().label() + " fight at threat 1");
            }
            assertTrue(offered >= 4, t.getKey() + " offers only " + offered + " entries at threat 1");
        }
    }

    @Test
    void noEntryAsksForAFirstMonsterItsBandCannotPay() {
        for (Map.Entry<String, EncounterTable> t : tables().entrySet()) {
            for (EncounterTable.Entry e : t.getValue().entries()) {
                EncounterTable.MobSpec first = e.mobs().get(0);
                if (first.kind() != EncounterTable.Kind.ENTITY || !HEALTH.containsKey(first.entity())) continue;
                double cost = EncounterRoll.cost(HEALTH.get(first.entity()));
                double budget = EncounterRoll.budget(8.0, e.band(), e.minThreat(), 1.0, t.getValue().budgetScale());
                assertTrue(cost <= budget + 1e-9, t.getKey() + "/" + e.id() + ": its first monster costs " + cost + " of a budget of " + budget + " at threat " + e.minThreat() + " for one player");
            }
        }
    }

    @Test
    void everyBandAnswersAtEveryThreatPartySizeAndState() {
        boolean[][] states = {{false, false, false}, {true, false, false}, {false, true, false}, {false, false, true}, {true, false, true}, {false, true, true}};
        int[] threats = {1, 2, 3, 4, 5, 6, 8, 10, 13, 17, 25, 40, 100, 1000};
        Random rnd = new Random(2024);
        for (Map.Entry<String, EncounterTable> t : tables().entrySet()) {
            for (int threat : threats) {
                for (int party = 1; party <= 6; party++) {
                    for (boolean[] s : states) {
                        for (Band b : new Band[] {Band.MINOR, Band.STANDARD, Band.HARD, Band.AMBUSH}) {
                            EncounterRoll.Choice c = EncounterRoll.choose(t.getValue().entries(), b, threat, party, s[0], s[1], s[2], null, rnd);
                            assertNotNull(c, t.getKey() + ": nothing for a " + b.label() + " roll at threat " + threat + ", party " + party + ", dark/lit/noisy " + s[0] + "/" + s[1] + "/" + s[2]);
                        }
                    }
                }
            }
        }
    }

    @Test
    void theHolyRoadIsMadeForLightAndTheAbyssalRoadForTheDark() {
        EncounterTable radiant = parse("radiant").table(), abyss = parse("abyss").table();
        int radiantLit = 0, radiantDark = 0, abyssLit = 0, abyssDark = 0;
        for (EncounterTable.Entry e : radiant.entries()) {
            if (Boolean.TRUE.equals(e.needs().lit())) radiantLit++;
            if (Boolean.TRUE.equals(e.needs().dark())) radiantDark++;
        }
        for (EncounterTable.Entry e : abyss.entries()) {
            if (Boolean.TRUE.equals(e.needs().lit())) abyssLit++;
            if (Boolean.TRUE.equals(e.needs().dark())) abyssDark++;
        }
        assertTrue(radiantLit >= 4, "the Rukh-bright road has only " + radiantLit + " entries that need light");
        assertTrue(radiantDark >= 1 && radiantDark <= 1, "the dark entries of the Holy road are rare: " + radiantDark);
        assertTrue(abyssDark >= 4, "the dark road has only " + abyssDark + " entries that need the dark");
        assertTrue(abyssLit == 0, "nothing of the Abyssal road needs a lit party: " + abyssLit);
        // the roll bonus cancels what the light modifier would add (a bright road) or take away (a dark one)
        assertTrue(radiant.rollBonus() <= -1, "a road of light needs a negative roll bonus: " + radiant.rollBonus());
        assertTrue(abyss.rollBonus() >= 1, "a road of darkness needs a positive roll bonus: " + abyss.rollBonus());
        assertTrue(abyss.budgetScale() >= 1.0, "the ecosystems of the descent are not smaller than the baseline");
        assertEquals("construct", radiant.defaultFaction());
        assertEquals("beast", abyss.defaultFaction());
    }

    @Test
    void theHolyRoadLeansOnConstructsElementalsAndCultists() {
        Set<String> factions = new TreeSet<>();
        for (EncounterTable.Entry e : parse("radiant").table().entries()) {
            if (!e.faction().isEmpty()) factions.add(e.faction());
            for (EncounterTable.MobSpec m : e.mobs()) if (m.kind() == EncounterTable.Kind.FACTION && !m.faction().isEmpty()) factions.add(m.faction());
        }
        assertTrue(factions.containsAll(Set.of("construct", "elemental", "cultist")), "the factions of the Holy road: " + factions);
    }

    @Test
    void theAbyssalRoadHasItsEcosystemsItsWallAmbushItsCisternsAndItsGrowingPacks() {
        EncounterTable abyss = parse("abyss").table();
        Set<String> factions = new TreeSet<>();
        boolean wallAmbush = false, cistern = false, brute = false, clash = false;
        int biggestBeastPack = 0, firstPackAt = Integer.MAX_VALUE, bigPackAt = Integer.MAX_VALUE;
        for (EncounterTable.Entry e : abyss.entries()) {
            if (!e.faction().isEmpty()) factions.add(e.faction());
            Set<String> inEntry = new TreeSet<>();
            for (EncounterTable.MobSpec m : e.mobs()) {
                String f = m.kind() == EncounterTable.Kind.FACTION ? (!m.faction().isEmpty() ? m.faction() : e.faction()) : "";
                if (!f.isEmpty()) {
                    factions.add(f);
                    inEntry.add(f);
                }
                if (m.kind() == EncounterTable.Kind.FACTION && "beast".equals(f)) {
                    biggestBeastPack = Math.max(biggestBeastPack, m.count().max());
                    if (m.count().max() <= 5) firstPackAt = Math.min(firstPackAt, e.minThreat());
                    if (m.count().max() >= 8) bigPackAt = Math.min(bigPackAt, e.minThreat());
                }
            }
            if (e.maxRange() > 0.0 && e.maxRange() <= 24.0 && e.band() == Band.STANDARD && e.cells() >= 3) wallAmbush = true;
            if ("aquatic".equals(e.faction())) cistern = true;
            if (e.needs().noisy() != null && e.needs().noisy() && e.band() == Band.HARD) brute = true;
            if (inEntry.size() >= 2 && e.band() == Band.HARD) clash = true;
        }
        assertTrue(factions.containsAll(Set.of("beast", "insect", "undead", "demon", "slime", "aquatic")), "the ecosystems of the descent: " + factions);
        assertTrue(wallAmbush, "an ambush from the walls");
        assertTrue(cistern, "a cistern entry");
        assertTrue(brute, "a loud brute");
        assertTrue(clash, "an entry that sets two ecosystems on the party at once");
        assertTrue(biggestBeastPack >= 8 && bigPackAt > firstPackAt, "the beast packs grow with the floor: a first pack from threat " + firstPackAt + ", a big one (up to " + biggestBeastPack + ") from threat " + bigPackAt);
    }

    @Test
    void theTablesShareTheCountsTheyPromise() {
        // the summary the report quotes: entries per band over all twelve files, the number of needs-gated entries, and how many name a message
        Map<Band, Integer> perBand = new TreeMap<>();
        int total = 0, gated = 0, messages = 0, factionOnly = 0;
        for (EncounterTable t : tables().values()) {
            for (EncounterTable.Entry e : t.entries()) {
                total++;
                perBand.merge(e.band(), 1, Integer::sum);
                if (e.needs().any()) gated++;
                if (!e.message().isEmpty()) messages++;
                boolean onlyFaction = true;
                for (EncounterTable.MobSpec m : e.mobs()) if (m.kind() == EncounterTable.Kind.ENTITY) onlyFaction = false;
                if (onlyFaction) factionOnly++;
            }
        }
        assertTrue(total >= 100, "only " + total + " entries in all");
        assertTrue(gated >= 30, "only " + gated + " entries react to the dark, the light or the noise");
        assertTrue(factionOnly >= 20, "only " + factionOnly + " entries are made of faction members alone, so the ecology of the rooms barely shows");
        System.out.println("entries: " + total + " (per band " + perBand + "), gated by needs " + gated + ", with a message " + messages + ", faction-only " + factionOnly);
    }

    // ---------------------------------------------------------------- the roster of wandering bosses
    @Test
    void theRosterParsesAndNamesRealBodies() {
        WandererRoster.Parsed p = WandererRoster.parse(read(WANDERERS));
        assertNotNull(p.roster());
        assertTrue(p.problems().isEmpty(), p.problems().toString());
        List<WandererRoster.Entry> entries = p.roster().entries();
        assertTrue(entries.size() >= 10, "only " + entries.size() + " wandering bosses");
        Set<String> vanilla = known("entity"), tensura = new HashSet<>(), nusmp = nusmpEntities();
        for (String id : vanilla) if (id.startsWith("tensura:")) tensura.add(id);
        vanilla.removeIf(id -> id.startsWith("tensura:"));
        WandererRoster.Entry warden = null;
        Set<String> used = new TreeSet<>();
        for (WandererRoster.Entry e : entries) {
            String ns = e.entity().substring(0, e.entity().indexOf(':'));
            assertTrue(NAMESPACES.contains(ns), "'" + e.entity() + "' is not a vanilla, Tensura or nusmp entity");
            assertTrue(entityExists(e.entity(), vanilla, tensura, nusmp), "no such entity type as '" + e.entity() + "'");
            assertFalse(e.name().isEmpty());
            used.add(e.entity());
            if (e.entity().equals("nusmp:floor_warden")) warden = e;
        }
        assertNotNull(warden, "the Floor Warden is the signature wanderer");
        assertEquals(5, warden.weight());
        assertEquals(1, warden.minThreat());
        assertEquals("The Warden", warden.name());
        System.out.println("wandering boss ids (" + used.size() + "): " + used);
    }

    @Test
    void theRosterExplainsItselfAndAlwaysHasSomeoneToSend() {
        JsonObject raw = JsonParser.parseString(read(WANDERERS)).getAsJsonObject();
        assertTrue(raw.get("comment").getAsString().startsWith("Why:"));
        for (JsonElement el : raw.getAsJsonArray("entries")) {
            JsonObject o = el.getAsJsonObject();
            assertTrue(o.has("comment") && o.get("comment").getAsString().length() > 30, o.get("entity").getAsString() + ": every wanderer says why it is on the list");
        }
        WandererRoster roster = WandererRoster.parse(read(WANDERERS)).roster();
        Random rnd = new Random(5);
        for (int threat : new int[] {1, 2, 3, 5, 8, 13, 50, 1000}) {
            Set<String> drawn = new HashSet<>();
            for (int i = 0; i < 400; i++) {
                WandererRoster.Entry e = roster.pick(threat, null, rnd);
                assertNotNull(e, "nobody to send at threat " + threat);
                assertTrue(e.fitsThreat(threat));
                drawn.add(e.entity());
            }
            if (threat == 1) assertTrue(drawn.size() >= 3, "only " + drawn.size() + " kinds can appear at threat 1");
            if (threat >= 5) assertTrue(drawn.size() >= 10, "only " + drawn.size() + " kinds can appear at threat " + threat);
        }
    }

    @Test
    void shippedFilesAreAsciiOnly() throws IOException {
        List<Path> files = new ArrayList<>();
        for (String n : tableNames()) files.add(ENC.resolve(n + ".json"));
        files.add(WANDERERS);
        for (Path f : files) {
            byte[] bytes = Files.readAllBytes(f);
            for (byte b : bytes) assertTrue(b >= 0, f.getFileName() + " holds a non-ASCII byte; keep the shipped data plain");
        }
    }
}
