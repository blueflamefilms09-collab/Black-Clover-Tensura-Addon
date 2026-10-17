package com.newuniverse.nusmp.quests;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the "Multiverse of Anime" advancement tree is generated (tools/quests/gen_advancements.py), and a broken advancement is silent in the
 * game: it simply does not load and the quest that points at it never completes. This test reads the generated files (no game needed) and
 * fails when one does not parse, has a missing parent or a parent cycle, has no title or description translation, uses a trigger nobody
 * implements, names an item, entity, effect or dimension the mod does not have, or lists a requirement that names no criterion.
 */
class AdvancementDataTest {
    private static final Path ADV = Path.of("src/main/resources/data/nusmp/advancement");
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path JAVA = Path.of("src/main/java");

    /** The folders the generator owns; convergence/ holds the older hidden advancements and is only checked for parsing. */
    private static final Set<String> OWNED = Set.of("start", "magic", "spells", "arcs", "bosses", "relics", "weapons", "dungeon",
            "spirits", "squads", "world", "challenges", "future", "arc");

    private static final Set<String> VANILLA_TRIGGERS = Set.of("minecraft:tick", "minecraft:impossible", "minecraft:inventory_changed",
            "minecraft:player_killed_entity", "minecraft:changed_dimension", "minecraft:location", "minecraft:consume_item",
            "minecraft:effects_changed", "minecraft:tame_animal", "minecraft:summoned_entity", "minecraft:player_interacted_with_entity",
            "minecraft:placed_block", "minecraft:item_used_on_block", "minecraft:using_item", "minecraft:recipe_crafted");

    /** The event keys of the contract (docs/quests.md, the "event key" table). A key used in an advancement must be in this set. */
    private static final Set<String> EVENT_KEYS = Set.of(
            "grimoire.received", "grimoire.kingdom", "grimoire.opened", "ceremony.completed", "spell.cast", "spell.cast.signature", "spell.distinct",
            "page.unlocked", "mastery.max", "wizard.questionnaire", "relic.obtained", "relic.equipped", "relic.grade",
            "relic.forged", "relic.socketed", "relic.tier", "relic.djinn_bound", "relic.unique_ledger",
            "labyrinth.entered", "labyrinth.warden_defeated", "labyrinth.vault_opened", "labyrinth.seal_opened", "dungeon.entered",
            "dungeon.floor_boss", "arena.entered", "arena.hourglass_used", "arena.victory", "boss.defeated", "boss.phase",
            "spirit.contract", "devil.union", "squad.created", "squad.joined", "squad.captain", "squad.vice", "convergence.stage",
            "convergence.anomaly", "magic.merged", "weapon.engraved", "weapon.genesis", "butoh.dance", "song.sung", "corpse.raised",
            "paladin.converted", "rouge.saved", "thread.trap", "shadow.travel", "unite.mode", "time.stop",
            "mega.entered", "mega.sector_cleared", "mega.rested", "mega.crucible_choice", "mega.wanderer_survived",
            "mega.hub", "mega.aligned", "mega.floor", "mega.gear", "mega.system_quest", "mega.shadow", "mega.vessel",
            "arc.defeated", "arc.met", "arc.saga_clear", "arc.arena");

    private static final Set<String> DIMENSIONS = Set.of("nusmp:labyrinth", "nusmp:living_dungeon", "nusmp:zogratis_arena", "nusmp:dream",
            "nusmp:grimoire_library", "nusmp:mega_dungeon", "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");

    private static List<Path> files() throws IOException {
        try (Stream<Path> s = Files.walk(ADV)) {
            return s.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
    }

    private static String id(Path p) {
        String rel = ADV.relativize(p).toString().replace('\\', '/');
        return "nusmp:" + rel.substring(0, rel.length() - ".json".length());
    }

    private static boolean owned(Path p) {
        String rel = ADV.relativize(p).toString().replace('\\', '/');
        return rel.equals("root.json") || OWNED.contains(rel.split("/")[0]);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> read(Path p) throws IOException {
        return (Map<String, Object>) QstMiniJson.parse(Files.readString(p));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) { return (Map<String, Object>) o; }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object o) { return (List<Object>) o; }

    private static Set<String> modItems() throws IOException {
        Set<String> out = new HashSet<>();
        try (Stream<Path> s = Files.list(ASSETS.resolve("models/item"))) {
            s.forEach(p -> {
                String n = p.getFileName().toString();
                if (n.endsWith(".json")) out.add(n.substring(0, n.length() - 5));
            });
        }
        return out;
    }

    private static Set<String> modEntities() throws IOException {
        Set<String> out = new HashSet<>();
        Pattern pat = Pattern.compile("\\.build\\(\"([a-z0-9_]+)\"\\)");
        try (Stream<Path> s = Files.walk(JAVA)) {
            for (Path p : s.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher m = pat.matcher(Files.readString(p));
                while (m.find()) out.add(m.group(1));
            }
        }
        return out;
    }

    private static Set<String> lang() throws IOException {
        Map<String, Object> m = map(QstMiniJson.parse(Files.readString(ASSETS.resolve("lang/en_us.json"))));
        return m.keySet();
    }

    private static void collect(Object o, String key, List<String> out) {
        if (o instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (key.equals(e.getKey()) && e.getValue() instanceof String s) out.add(s);
                collect(e.getValue(), key, out);
            }
        } else if (o instanceof List<?> l) {
            for (Object x : l) collect(x, key, out);
        }
    }

    @Test
    void everyFileParsesAndTheTreeIsBigEnough() throws IOException {
        int owned = 0;
        for (Path p : files()) {
            Map<String, Object> adv = read(p);
            assertTrue(adv.containsKey("criteria"), id(p) + " has no criteria");
            if (owned(p)) owned++;
        }
        assertTrue(owned >= 150, "the owner asked for at least 150 advancements, found " + owned);
    }

    @Test
    void theOlderConvergenceFilesAreStillThere() throws IOException {
        long n = files().stream().filter(p -> id(p).startsWith("nusmp:convergence/")).count();
        assertTrue(n >= 25, "the 25 hidden convergence advancements must stay untouched, found " + n);
    }

    @Test
    void parentsExistAndThereIsExactlyOneRootAndNoCycle() throws IOException {
        Map<String, String> parent = new HashMap<>();
        Set<String> all = new HashSet<>();
        for (Path p : files()) all.add(id(p));
        int roots = 0;
        for (Path p : files()) {
            if (!owned(p)) continue;
            Map<String, Object> adv = read(p);
            String par = (String) adv.get("parent");
            if (par == null) {
                roots++;
                assertEquals("nusmp:root", id(p), "only nusmp:root may have no parent");
                assertTrue(map(adv.get("display")).containsKey("background"), "the root needs a background texture");
            } else {
                assertTrue(all.contains(par), id(p) + " has the missing parent " + par);
                parent.put(id(p), par);
            }
        }
        assertEquals(1, roots, "exactly one root");
        for (String start : parent.keySet()) {
            String cur = start;
            int guard = 0;
            while (parent.containsKey(cur)) {
                cur = parent.get(cur);
                assertTrue(++guard < 10_000, "parent cycle at " + start);
            }
            assertEquals("nusmp:root", cur, start + " does not lead back to nusmp:root");
        }
    }

    @Test
    void titlesAndDescriptionsAreTranslatable() throws IOException {
        Set<String> keys = lang();
        List<String> missing = new ArrayList<>();
        for (Path p : files()) {
            if (!owned(p)) continue;
            Map<String, Object> adv = read(p);
            Map<String, Object> display = map(adv.get("display"));
            assertTrue(display != null, id(p) + " has no display");
            for (String field : new String[] {"title", "description"}) {
                Object o = display.get(field);
                assertTrue(o instanceof Map, id(p) + " " + field + " must be a translate component");
                String tr = (String) map(o).get("translate");
                assertTrue(tr != null && tr.startsWith("advancement.nusmp."), id(p) + " " + field + " must use advancement.nusmp.*");
                if (!keys.contains(tr)) missing.add(tr);
            }
            assertTrue(Set.of("task", "goal", "challenge").contains(display.get("frame")), id(p) + " has a bad frame");
            assertTrue(display.get("icon") instanceof Map, id(p) + " needs an icon");
        }
        assertTrue(missing.isEmpty(), "missing lang keys (merge tools/quests/lang/qst.lang.json into en_us.json): "
                + missing.subList(0, Math.min(10, missing.size())) + " ... " + missing.size() + " in all");
    }

    @Test
    void triggersAreKnownAndEventKeysAreInTheContract() throws IOException {
        List<String> badKeys = new ArrayList<>();
        for (Path p : files()) {
            if (!owned(p)) continue;
            Map<String, Object> crits = map(read(p).get("criteria"));
            assertTrue(!crits.isEmpty(), id(p) + " has no criterion");
            for (Map.Entry<String, Object> e : crits.entrySet()) {
                Map<String, Object> c = map(e.getValue());
                String trigger = (String) c.get("trigger");
                if ("nusmp:event".equals(trigger)) {
                    Map<String, Object> cond = map(c.get("conditions"));
                    String key = (String) cond.get("key");
                    assertTrue(key != null && !key.isEmpty(), id(p) + "/" + e.getKey() + " nusmp:event without a key");
                    if (!EVENT_KEYS.contains(key)) badKeys.add(id(p) + " -> " + key);
                    for (String k : cond.keySet())
                        assertTrue(Set.of("key", "count", "arg").contains(k), id(p) + ": unknown event condition field " + k);
                    if (cond.containsKey("count")) assertTrue(((Number) cond.get("count")).intValue() >= 2, id(p) + ": count is only written when above 1");
                } else {
                    assertTrue(VANILLA_TRIGGERS.contains(trigger), id(p) + " uses the unknown trigger " + trigger);
                }
            }
        }
        assertTrue(badKeys.isEmpty(), "event keys outside the contract: " + badKeys);
    }

    @Test
    void requirementsNameRealCriteriaAndEveryCriterionIsUsed() throws IOException {
        for (Path p : files()) {
            if (!owned(p)) continue;
            Map<String, Object> adv = read(p);
            Set<String> names = map(adv.get("criteria")).keySet();
            Set<String> used = new HashSet<>();
            for (Object group : list(adv.get("requirements"))) {
                assertTrue(!list(group).isEmpty(), id(p) + " has an empty requirement group");
                for (Object n : list(group)) {
                    assertTrue(names.contains(n), id(p) + " requirement names the missing criterion " + n);
                    used.add((String) n);
                }
            }
            assertEquals(names, used, id(p) + " has a criterion that no requirement uses");
        }
    }

    @Test
    void modItemsEntitiesEffectsAndDimensionsExist() throws IOException {
        Set<String> items = modItems();
        Set<String> entities = modEntities();
        Set<String> lang = lang();
        List<String> problems = new ArrayList<>();
        for (Path p : files()) {
            if (!owned(p)) continue;
            Map<String, Object> adv = read(p);
            List<String> found = new ArrayList<>();
            collect(adv, "id", found);        // the icon
            collect(adv, "items", found);     // inventory_changed / consume_item predicates
            for (String it : found) {
                if (it.startsWith("nusmp:") && !items.contains(it.substring(6))) problems.add(id(p) + ": item " + it);
            }
            List<String> types = new ArrayList<>();
            collect(adv, "type", types);
            for (String t : types) if (t.startsWith("nusmp:") && !entities.contains(t.substring(6))) problems.add(id(p) + ": entity " + t);
            List<String> to = new ArrayList<>();
            collect(adv, "to", to);
            for (String d : to) if (!DIMENSIONS.contains(d)) problems.add(id(p) + ": dimension " + d);
            for (Object o : crit(adv, "minecraft:effects_changed")) {
                for (String eff : map(map(o).get("effects")).keySet())
                    if (eff.startsWith("nusmp:") && !lang.contains("effect.nusmp." + eff.substring(6))) problems.add(id(p) + ": effect " + eff);
            }
        }
        assertTrue(problems.isEmpty(), "unknown mod ids in advancements: " + problems);
    }

    private static List<Object> crit(Map<String, Object> adv, String trigger) {
        List<Object> out = new ArrayList<>();
        for (Object c : map(adv.get("criteria")).values()) {
            if (trigger.equals(map(c).get("trigger"))) out.add(map(c).get("conditions"));
        }
        return out;
    }

    @Test
    void futureBossPlaceholdersAreImpossibleHiddenAndNamedByArc() throws IOException {
        int n = 0;
        for (Path p : files()) {
            if (!id(p).startsWith("nusmp:future/")) continue;
            n++;
            Map<String, Object> adv = read(p);
            Map<String, Object> crits = map(adv.get("criteria"));
            assertEquals(1, crits.size(), id(p) + " must have one criterion");
            assertEquals("minecraft:impossible", map(crits.values().iterator().next()).get("trigger"), id(p));
            assertTrue(Boolean.TRUE.equals(map(adv.get("display")).get("hidden")), id(p) + " must be hidden until awarded");
            assertTrue(((String) adv.get("parent")).startsWith("nusmp:arcs/"), id(p) + " hangs off its arc");
            assertTrue(id(p).matches("nusmp:future/[a-z0-9_]+_[a-z0-9]+"), id(p) + " must be future/<arc>_<boss>");
        }
        assertTrue(n >= 8, "the arcs without a boss need their placeholders, found " + n);
    }

    @Test
    void theMagicBranchHasOneAdvancementPerFamily() throws IOException {
        long casts = files().stream().filter(p -> id(p).startsWith("nusmp:magic/cast_")).count();
        assertEquals(105, casts, "one cast advancement per magic family (105)");
    }
}
