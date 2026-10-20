package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.guard.DndProfile;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: thirteen tombs are thirteen data files, thirteen boss profiles, thirteen biomes and sixty-five loot tables, and nothing may be wrong in them, because a wrong biome file stops the
 * whole game from loading and a wrong loot table leaks a relic. This test reads the files from disk and checks: every pocket parses, has a biome (with its datapack file), three to five
 * exclusive mobs whose foundry bodies exist, a mega-boss profile that the Guardian's own reader accepts, a layout the generator can make for any seed, relics that are real relic definitions,
 * a seal whose magics are real Black Clover magics, a theme that is a real region theme; the ids are unique; every sound, particle, block and entity named is one of the vanilla ids this mod
 * has checked (or a namespaced string of the mod's own sounds, played by code with a fallback); no hunger anywhere; and the loot tables are gated: every pool carries the in-pocket
 * condition of its own pocket, a pocket's relics are in no loot table but its own, and the relic entry names only that pocket's relics.
 */
class PocketDataTest {
    static final Path DATA = Path.of("src/main/resources/data/nusmp");
    static final Path POCKETS = DATA.resolve("mega/pockets");

    /** The vanilla ids this mod's pocket data may use (checked against the game's registries by hand and by the in-game self-test rift.pockets). */
    static final Set<String> SOUNDS = Set.of("minecraft:ambient.basalt_deltas.loop", "minecraft:ambient.basalt_deltas.mood", "minecraft:ambient.basalt_deltas.additions", "minecraft:music.nether.basalt_deltas",
            "minecraft:ambient.crimson_forest.loop", "minecraft:ambient.crimson_forest.mood", "minecraft:ambient.crimson_forest.additions", "minecraft:music.nether.crimson_forest",
            "minecraft:ambient.warped_forest.loop", "minecraft:ambient.warped_forest.mood", "minecraft:ambient.warped_forest.additions",
            "minecraft:ambient.soul_sand_valley.loop", "minecraft:ambient.soul_sand_valley.mood", "minecraft:ambient.soul_sand_valley.additions", "minecraft:music.nether.soul_sand_valley",
            "minecraft:ambient.nether_wastes.loop", "minecraft:ambient.nether_wastes.mood", "minecraft:ambient.nether_wastes.additions",
            "minecraft:ambient.underwater.loop", "minecraft:ambient.underwater.loop.additions.rare", "minecraft:ambient.cave", "minecraft:music.under_water", "minecraft:music.end",
            "minecraft:music.overworld.lush_caves", "minecraft:music.overworld.frozen_peaks", "minecraft:music.overworld.jagged_peaks", "minecraft:music.overworld.stony_peaks",
            "minecraft:music.overworld.dripstone_caves", "minecraft:music.overworld.swamp", "minecraft:music.overworld.meadow", "minecraft:music.overworld.deep_dark");
    static final Set<String> PARTICLES = Set.of("minecraft:ash", "minecraft:white_ash", "minecraft:flame", "minecraft:small_flame", "minecraft:lava", "minecraft:dripping_lava", "minecraft:soul",
            "minecraft:soul_fire_flame", "minecraft:snowflake", "minecraft:cloud", "minecraft:underwater", "minecraft:bubble", "minecraft:bubble_pop", "minecraft:dolphin", "minecraft:glow",
            "minecraft:end_rod", "minecraft:spore_blossom_air", "minecraft:crimson_spore", "minecraft:warped_spore", "minecraft:sneeze", "minecraft:happy_villager", "minecraft:composter",
            "minecraft:electric_spark", "minecraft:squid_ink", "minecraft:witch", "minecraft:enchant", "minecraft:reverse_portal", "minecraft:falling_nectar", "minecraft:damage_indicator");
    static final Set<String> ENTITIES = Set.of("minecraft:zombie", "minecraft:husk", "minecraft:drowned", "minecraft:stray", "minecraft:wither_skeleton", "minecraft:skeleton", "minecraft:vindicator",
            "minecraft:pillager", "minecraft:witch", "minecraft:evoker", "minecraft:iron_golem", "minecraft:spider", "minecraft:cave_spider", "minecraft:silverfish", "minecraft:wolf", "minecraft:vex",
            "minecraft:blaze", "minecraft:phantom");
    static final Set<String> BLOCKS = Set.of("minecraft:polished_blackstone_bricks", "minecraft:blackstone", "minecraft:chiseled_polished_blackstone", "minecraft:gilded_blackstone",
            "minecraft:red_nether_bricks", "minecraft:shroomlight", "minecraft:red_stained_glass", "minecraft:lava", "minecraft:water", "minecraft:mossy_stone_bricks", "minecraft:mossy_cobblestone",
            "minecraft:stone_bricks", "minecraft:stripped_oak_log", "minecraft:moss_block", "minecraft:lime_stained_glass", "minecraft:calcite", "minecraft:smooth_quartz", "minecraft:packed_ice",
            "minecraft:chiseled_quartz_block", "minecraft:light_blue_concrete", "minecraft:sea_lantern", "minecraft:light_blue_stained_glass", "minecraft:dark_prismarine", "minecraft:prismarine_bricks",
            "minecraft:prismarine", "minecraft:cyan_terracotta", "minecraft:cyan_stained_glass", "minecraft:tuff_bricks", "minecraft:polished_tuff", "minecraft:chiseled_tuff",
            "minecraft:waxed_oxidized_copper", "minecraft:light_gray_concrete", "minecraft:white_stained_glass", "minecraft:cut_sandstone", "minecraft:smooth_sandstone", "minecraft:chiseled_sandstone",
            "minecraft:gold_block", "minecraft:glowstone", "minecraft:yellow_stained_glass", "minecraft:deepslate_bricks", "minecraft:polished_deepslate", "minecraft:chiseled_deepslate",
            "minecraft:cracked_deepslate_bricks", "minecraft:crying_obsidian", "minecraft:pearlescent_froglight", "minecraft:purple_stained_glass", "minecraft:deepslate_tiles",
            "minecraft:polished_blackstone", "minecraft:amethyst_block", "minecraft:purpur_pillar", "minecraft:blue_stained_glass", "minecraft:nether_bricks", "minecraft:stripped_crimson_stem",
            "minecraft:nether_wart_block", "minecraft:waxed_cut_copper", "minecraft:polished_andesite", "minecraft:waxed_chiseled_copper", "minecraft:raw_gold_block", "minecraft:ochre_froglight",
            "minecraft:orange_stained_glass", "minecraft:quartz_bricks", "minecraft:white_concrete", "minecraft:quartz_pillar", "minecraft:light_gray_stained_glass", "minecraft:polished_diorite",
            "minecraft:mud_bricks", "minecraft:packed_mud", "minecraft:stripped_mangrove_log", "minecraft:honeycomb_block");

    // ---------------------------------------------------------------- loading
    static JsonObject json(Path p) {
        try {
            return JsonParser.parseString(Files.readString(p)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException(p + ": " + e);
        }
    }

    static List<Path> pocketFiles() throws IOException {
        try (Stream<Path> s = Files.list(POCKETS)) {
            return s.filter(f -> f.toString().endsWith(".json")).sorted().toList();
        }
    }

    static Map<String, PocketDef> defs() throws IOException {
        Map<String, PocketDef> out = new java.util.LinkedHashMap<>();
        for (Path f : pocketFiles()) {
            String id = f.getFileName().toString().replace(".json", "");
            out.put(id, PocketDef.parse(id, json(f)));
        }
        return out;
    }

    /** A minimal valid definition for the tests of the rules: a copy of the first real pocket with another id, depth and weight. */
    static PocketDef stub(String id, int minDepth, int weight) {
        try {
            JsonObject o = json(POCKETS.resolve("ember_sanctum.json"));
            o.addProperty("id", id);
            o.addProperty("min_depth", minDepth);
            o.addProperty("weight", weight);
            return PocketDef.parse(id, o);
        } catch (RuntimeException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Every relic id that a wave source of the mod defines (RelicDef.of("id", ...)). */
    static Set<String> relicIds() throws IOException {
        Set<String> out = new HashSet<>();
        Pattern def = Pattern.compile("RelicDef\\s*\\.\\s*of\\s*\\(\\s*\"([a-z0-9_]+)\"");
        try (Stream<Path> s = Files.list(Path.of("src/main/java/com/newuniverse/nusmp/relics/wave"))) {
            for (Path p : s.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher m = def.matcher(Files.readString(p));
                while (m.find()) out.add(m.group(1));
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- the tests
    @Test
    void thereAreAtLeastTwelveExclusivePocketsAndTheirIdsAreUnique() throws IOException {
        Map<String, PocketDef> defs = defs();
        assertTrue(defs.size() >= 12, "only " + defs.size() + " pockets");
        Set<String> names = new HashSet<>(), accents = new HashSet<>(), bosses = new HashSet<>();
        for (PocketDef d : defs.values()) {
            assertTrue(names.add(d.name()), "two pockets are called " + d.name());
            assertTrue(accents.add(d.accent() + "/" + d.sky()), "two pockets share the colour " + d.id());
            assertTrue(bosses.add(d.boss()), "two pockets share the boss " + d.boss());
        }
    }

    @Test
    void everyPocketHasABiomeMobsABossALayoutRelicsAndASeal() throws IOException {
        for (PocketDef d : defs().values()) {
            assertNotNull(d.biome());
            assertTrue(d.mobs().size() >= 3 && d.mobs().size() <= 5, d.id() + " mobs");
            Set<String> ids = new HashSet<>();
            for (PocketDef.Mob m : d.mobs()) assertTrue(ids.add(m.id()), d.id() + ": duplicate mob " + m.id());
            assertTrue(Files.exists(POCKETS.resolve(d.bossFile() + ".json")), d.id() + ": no boss profile " + d.bossFile());
            assertTrue(d.layout().rooms() >= PocketDef.ROOMS_MIN);
            assertTrue(d.relics().all().size() >= 2 && d.relics().all().size() <= 3, d.id() + " relics");
            assertTrue(d.seal().forceHp() >= 400);
            assertFalse(d.seal().magic().isEmpty());
            assertFalse(d.resonance().isEmpty());
            assertTrue(d.floorMin() >= 1 && d.floorMin() <= 1000);
            assertTrue(d.collapseSeconds() >= PocketDef.COLLAPSE_MIN);
            assertFalse(d.lore().isBlank());
        }
    }

    @Test
    void nothingIsSilentlyClampedByTheReader() throws IOException {
        for (Path f : pocketFiles()) {
            JsonObject o = json(f);
            PocketDef d = PocketDef.parse(f.getFileName().toString().replace(".json", ""), o);
            assertEquals(o.getAsJsonObject("layout").get("rooms").getAsInt(), d.layout().rooms());
            assertEquals(o.get("collapse_seconds").getAsInt(), d.collapseSeconds(), d.id() + " collapse seconds were clamped");
            assertEquals(o.get("floor_min").getAsInt(), d.floorMin(), d.id() + " floor_min was clamped");
            assertEquals(o.get("weight").getAsInt(), d.weight(), d.id() + " weight was clamped");
            assertEquals(o.getAsJsonObject("seal").get("force").getAsInt(), d.seal().forceHp(), d.id() + " seal force was clamped");
            for (int i = 0; i < d.mobs().size(); i++) {
                JsonObject m = o.getAsJsonArray("mobs").get(i).getAsJsonObject();
                PocketDef.Mob mob = d.mobs().get(i);
                assertEquals(m.get("hp").getAsDouble(), mob.hp(), 0.0, d.id() + "/" + mob.id() + " hp was clamped");
                assertEquals(m.get("damage").getAsDouble(), mob.damage(), 0.0, d.id() + "/" + mob.id() + " damage was clamped");
                assertEquals(m.get("speed").getAsDouble(), mob.speed(), 0.0, d.id() + "/" + mob.id() + " speed was clamped");
                assertEquals(m.get("scale").getAsDouble(), mob.scale(), 0.0, d.id() + "/" + mob.id() + " scale was clamped");
            }
        }
    }

    @Test
    void theBodiesOfTheMobsExistAndTheFallbacksAreKnownTypes() throws IOException {
        for (PocketDef d : defs().values()) {
            boolean air = false;
            for (PocketDef.Mob m : d.mobs()) {
                if (m.foundryBase()) assertTrue(Files.exists(DATA.resolve("mega/foundry/" + m.foundryId() + ".json")), d.id() + "/" + m.id() + ": no foundry mob " + m.foundryId());
                else assertTrue(ENTITIES.contains(m.base()), d.id() + "/" + m.id() + ": unknown base " + m.base());
                assertTrue(ENTITIES.contains(m.fallback()), d.id() + "/" + m.id() + ": unknown fallback " + m.fallback());
                assertTrue(PARTICLES.contains(m.aura()), d.id() + "/" + m.id() + ": unknown aura particle " + m.aura());
                if (m.air()) air = true;
            }
            if (d.gravity().inverted() + d.gravity().negated() > 0) assertTrue(air, d.id() + " has gravity rooms but no flying mob to put in them");
        }
    }

    @Test
    void everySoundParticleBlockAndEntityIsOneThisModHasChecked() throws IOException {
        for (PocketDef d : defs().values()) {
            PocketDef.Biome b = d.biome();
            for (String s : new String[]{b.ambient(), b.mood(), b.additions(), b.music()}) assertTrue(SOUNDS.contains(s), d.id() + ": biome sound " + s + " is not in the checked list");
            for (String s : new String[]{b.softAmbient(), b.softCrackle()}) assertTrue(s.startsWith("nusmp:") && s.matches("[a-z0-9_.:]+"), d.id() + ": soft sound " + s);
            assertTrue(PARTICLES.contains(b.particle()), d.id() + ": biome particle " + b.particle());
            PocketDef.Palette p = d.palette();
            for (String s : new String[]{p.wall(), p.floor(), p.ceiling(), p.pillar(), p.accent(), p.light(), p.glass(), p.liquid()}) assertTrue(BLOCKS.contains(s), d.id() + ": block " + s + " is not in the checked list");
            assertTrue(p.liquid().equals("minecraft:water") || p.liquid().equals("minecraft:lava"), "the liquid of a river is water or lava");
            for (PocketElement e : d.traps()) assertNotNull(e);
        }
    }

    @Test
    void theBiomeFilesExistAndSayWhatThePocketSays() throws IOException {
        for (PocketDef d : defs().values()) {
            Path f = DATA.resolve("worldgen/biome/pocket_" + d.id() + ".json");
            assertTrue(Files.exists(f), "no biome file for " + d.id());
            JsonObject o = json(f), fx = o.getAsJsonObject("effects");
            assertEquals(d.biome().fog(), fx.get("fog_color").getAsInt(), d.id() + " fog");
            assertEquals(d.biome().sky(), fx.get("sky_color").getAsInt(), d.id() + " sky");
            assertEquals(d.biome().water(), fx.get("water_color").getAsInt());
            assertEquals(d.biome().ambient(), fx.get("ambient_sound").getAsString());
            assertEquals(d.biome().mood(), fx.getAsJsonObject("mood_sound").get("sound").getAsString());
            assertEquals(d.biome().additions(), fx.getAsJsonObject("additions_sound").get("sound").getAsString());
            assertEquals(d.biome().music(), fx.getAsJsonObject("music").get("sound").getAsString());
            assertEquals(d.biome().particle(), fx.getAsJsonObject("particle").getAsJsonObject("options").get("type").getAsString());
            for (String k : new String[]{"has_precipitation", "temperature", "downfall", "spawners", "spawn_costs", "carvers", "features"}) assertTrue(o.has(k), d.id() + " biome lacks " + k);
            for (String k : new String[]{"monster", "creature", "ambient", "axolotls", "underground_water_creature", "water_creature", "water_ambient", "misc"})
                assertTrue(o.getAsJsonObject("spawners").has(k) && o.getAsJsonObject("spawners").getAsJsonArray(k).isEmpty(), d.id() + " spawner " + k + ": nothing spawns by itself in a tomb");
            assertEquals(d.biomeId(), "nusmp:pocket_" + d.id());
        }
    }

    @Test
    void theMegaBossProfilesAreAcceptedByTheGuardiansOwnReader() throws IOException {
        Set<String> looks = new HashSet<>();
        JsonObject lookTable = json(Path.of("src/main/resources/assets/nusmp/abyssal/looks.json")).getAsJsonObject("looks");
        looks.addAll(lookTable.keySet());
        for (PocketDef d : defs().values()) {
            JsonObject o = json(POCKETS.resolve(d.bossFile() + ".json"));
            DndProfile p = DndProfile.parse(d.boss(), o);
            assertEquals(3, p.forms().size());
            assertFalse(p.legendary().isEmpty(), d.id() + " boss has no legendary action");
            assertFalse(p.lair().isEmpty(), d.id() + " boss has no lair action");
            assertTrue(p.mythic(), d.id() + " boss is a mega-boss: it must have a second life");
            assertTrue(p.tier() >= 3, d.id() + " boss tier");
            assertTrue(looks.contains(p.look()), d.id() + ": look " + p.look() + " is not an abyssal look");
            PocketBossExtra x = PocketBossExtra.parse(d.id(), o);
            assertTrue(x.damage() >= 1.0 && x.scale() >= 1.0);
            assertFalse(x.intro().isBlank());
            assertFalse(x.death().isBlank());
            if (!x.adds().isEmpty()) {
                boolean found = false;
                for (PocketDef.Mob m : d.mobs()) if (m.id().equals(x.adds())) found = true;
                assertTrue(found, d.id() + ": the court '" + x.adds() + "' is not one of its mobs");
                assertTrue(x.addCount() > 0);
            }
            assertEquals(o.getAsJsonObject("mega").get("damage").getAsDouble(), x.damage(), 0.0, d.id() + " boss damage was clamped");
        }
        Set<String> used = new HashSet<>();
        for (PocketDef d : defs().values()) assertTrue(used.add(DndProfile.parse(d.boss(), json(POCKETS.resolve(d.bossFile() + ".json"))).look()), "two mega-bosses wear the same look");
    }

    @Test
    void theThemesAndTheMagicsAreRealOnes() throws IOException {
        Set<String> themes = new HashSet<>();
        for (RegionTheme t : RegionTheme.values()) themes.add(t.id());
        String magicSource = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/blackclover/MagicType.java"));
        Set<String> magics = new HashSet<>();
        Matcher m = Pattern.compile("^    ([A-Z_]+)\\(\"", Pattern.MULTILINE).matcher(magicSource);
        while (m.find()) magics.add(m.group(1));
        assertTrue(magics.contains("FLAME") && magics.size() > 50, "the parser should see the Black Clover magics, saw " + magics.size());
        for (PocketDef d : defs().values()) {
            assertTrue(themes.contains(d.theme()), d.id() + ": unknown region theme " + d.theme());
            for (String s : d.seal().magic()) assertTrue(magics.contains(s), d.id() + ": unknown magic " + s);
            for (String s : d.resonance()) assertTrue(magics.contains(s), d.id() + ": unknown resonance magic " + s);
        }
    }

    @Test
    void theRelicsAreRealRelicDefinitionsAndEachBelongsToOnePocket() throws IOException {
        Set<String> real = relicIds();
        Map<String, String> owner = new HashMap<>();
        for (PocketDef d : defs().values()) {
            for (String r : d.relics().all()) {
                assertTrue(real.contains(r), d.id() + ": the relic '" + r + "' is not defined by any wave");
                assertEquals(null, owner.put(r, d.id()), "the relic " + r + " is in two pockets");
            }
        }
        assertTrue(owner.size() >= 24, "at least two exclusive relics a pocket, " + owner.size());
        assertEquals(PocketRelics.BUILT_IN, owner.keySet(), "the built-in set of exclusive relics (kept out of every random relic roll) is exactly the relics of the pocket files");
        for (String r : owner.keySet()) assertTrue(PocketRelics.exclusive(r), r);
        assertFalse(PocketRelics.exclusive("vorpal_sword"), "an ordinary relic stays in the pools");
        assertFalse(PocketRelics.exclusive(null));
    }

    @Test
    void theWeaponsAreWave78AndTheAccessoriesWave79AndEachTombHasOneOfEach() throws IOException {
        String w78 = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/relics/wave/Wave78.java")), w79 = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/relics/wave/Wave79.java"));
        for (PocketDef d : defs().values()) {
            assertEquals(1, d.relics().boss().size(), d.id() + ": one weapon from the boss");
            assertEquals(1, d.relics().treasury().size(), d.id() + ": one artifact from the treasury");
            assertTrue(w78.contains("RelicDef.of(\"" + d.relics().boss().get(0) + "\""), d.id() + ": the boss relic is a weapon of wave 78");
            assertTrue(w79.contains("RelicDef.of(\"" + d.relics().treasury().get(0) + "\""), d.id() + ": the treasury relic is an artifact of wave 79");
            assertEquals(d.relics().treasury().get(0), d.relics().primary(), d.id() + ": the artifact on the pedestal is the primary one, whose removal collapses the tomb");
        }
    }

    @Test
    void noHungerAnywhereInThePocketData() throws IOException {
        List<Path> all = new ArrayList<>(pocketFiles());
        try (Stream<Path> s = Files.walk(POCKETS.resolve("bosses"))) { s.filter(f -> f.toString().endsWith(".json")).forEach(all::add); }
        try (Stream<Path> s = Files.walk(DATA.resolve("loot_table/pocket"))) { s.filter(f -> f.toString().endsWith(".json")).forEach(all::add); }
        for (Path f : all) {
            String t = Files.readString(f).toLowerCase();
            assertFalse(t.contains("hunger") || t.contains("saturation") || t.contains("exhaustion") || t.contains("starv"), f + " mentions hunger");
        }
    }

    // ---------------------------------------------------------------- the loot is gated
    private static void collectConditions(JsonElement pool, List<JsonObject> out) {
        if (pool.getAsJsonObject().has("conditions")) for (JsonElement c : pool.getAsJsonObject().getAsJsonArray("conditions")) out.add(c.getAsJsonObject());
    }

    private static void collectRelicEntries(JsonElement e, List<JsonObject> out) {
        if (!e.isJsonObject()) return;
        JsonObject o = e.getAsJsonObject();
        if (o.has("type") && o.get("type").getAsString().equals("nusmp:pocket_relic")) out.add(o);
        if (o.has("children")) for (JsonElement c : o.getAsJsonArray("children")) collectRelicEntries(c, out);
        if (o.has("entries")) for (JsonElement c : o.getAsJsonArray("entries")) collectRelicEntries(c, out);
    }

    @Test
    void everyPoolOfAPocketsLootIsGatedByItsOwnPocketAndOnlyNamesItsOwnRelics() throws IOException {
        for (PocketDef d : defs().values()) {
            for (String table : new String[]{"boss", "treasury", "secondary", "chest", "minor"}) {
                Path f = DATA.resolve("loot_table/pocket/" + d.id() + "/" + table + ".json");
                assertTrue(Files.exists(f), "no loot table " + f);
                JsonObject o = json(f);
                JsonArray pools = o.getAsJsonArray("pools");
                assertTrue(pools.size() >= 1);
                for (JsonElement pool : pools) {
                    List<JsonObject> conds = new ArrayList<>();
                    collectConditions(pool, conds);
                    boolean gated = false;
                    for (JsonObject c : conds) {
                        if (c.get("condition").getAsString().equals("nusmp:in_pocket")) {
                            gated = true;
                            assertEquals(d.id(), c.get("pocket").getAsString(), "a pool of " + d.id() + " is gated by another pocket");
                            String stage = c.get("stage").getAsString();
                            if (table.equals("boss")) assertEquals("boss", stage);
                            if (table.equals("treasury") || table.equals("secondary") || table.equals("chest")) assertEquals("treasury", stage);
                        }
                    }
                    assertTrue(gated, f + " has a pool that is not gated by nusmp:in_pocket");
                    List<JsonObject> relics = new ArrayList<>();
                    collectRelicEntries(pool, relics);
                    for (JsonObject r : relics) {
                        assertEquals(d.id(), r.get("pocket").getAsString());
                        assertTrue(d.relics().all().contains(r.get("relic").getAsString()), f + " names a relic that " + d.id() + " does not own");
                        if (table.equals("boss")) assertTrue(d.relics().boss().contains(r.get("relic").getAsString()), "the boss table gives only the boss's relics");
                        if (table.equals("treasury")) assertEquals(d.relics().primary(), r.get("relic").getAsString());
                        assertFalse(table.equals("minor") || table.equals("chest") || table.equals("secondary"), "a relic in a table that is not the boss's or the primary pedestal's");
                    }
                }
            }
            // the boss table gives every boss relic, the treasury table the primary
            String boss = Files.readString(DATA.resolve("loot_table/pocket/" + d.id() + "/boss.json")), treasury = Files.readString(DATA.resolve("loot_table/pocket/" + d.id() + "/treasury.json"));
            for (String r : d.relics().boss()) assertTrue(boss.contains("\"" + r + "\""), d.id() + ": the boss table lacks " + r);
            assertTrue(treasury.contains("\"" + d.relics().primary() + "\""), d.id() + ": the treasury table lacks the primary relic");
        }
    }

    @Test
    void aPocketsRelicIsInNoLootTableButItsOwn() throws IOException {
        Map<String, String> owner = new HashMap<>();
        for (PocketDef d : defs().values()) for (String r : d.relics().all()) owner.put(r, d.id());
        try (Stream<Path> s = Files.walk(DATA.resolve("loot_table"))) {
            for (Path f : s.filter(x -> x.toString().endsWith(".json")).toList()) {
                String t = Files.readString(f);
                for (Map.Entry<String, String> e : owner.entrySet()) {
                    if (!t.contains("\"" + e.getKey() + "\"") && !t.contains(":" + e.getKey() + "\"")) continue;
                    String norm = f.toString().replace('\\', '/');
                    assertTrue(norm.contains("loot_table/pocket/" + e.getValue() + "/"), "the relic " + e.getKey() + " of " + e.getValue() + " is named in " + f);
                }
            }
        }
        // and no other data file (a loot modifier, a recipe, a tag) names them either, apart from the pockets' own files
        try (Stream<Path> s = Files.walk(DATA)) {
            for (Path f : s.filter(x -> x.toString().endsWith(".json")).toList()) {
                String norm = f.toString().replace('\\', '/');
                if (norm.contains("loot_table/pocket/") || norm.contains("mega/pockets/") || norm.endsWith("mega/relic_themes.json")) continue;     // the dungeon road of a weapon is not a way to get it
                String t = Files.readString(f);
                for (String r : owner.keySet()) assertFalse(t.contains("\"nusmp:" + r + "\"") || t.contains("\"" + r + "\""), r + " appears in " + f);
            }
        }
    }

    // ---------------------------------------------------------------- the generator can make every pocket's plan, for any seed
    @Test
    void everyPocketHasPlansForManySeedsThatAreSoundAndHaveItsZones() throws IOException {
        for (PocketDef d : defs().values()) {
            LayoutGen.Params p = LayoutGen.Params.of(d);
            for (long s = 0; s < 120; s++) {
                PocketLayout l = LayoutGen.generate(s * 6700417L + 11, p);
                assertTrue(LayoutCheck.validate(l).isEmpty(), d.id() + " seed " + s + ": " + LayoutCheck.validate(l));
                assertTrue(l.count(PocketLayout.Kind.INVERTED) >= d.gravity().inverted(), d.id() + " inverted rooms, seed " + s);
                assertTrue(l.count(PocketLayout.Kind.NEGATED) >= d.gravity().negated(), d.id() + " weightless rooms, seed " + s);
                assertTrue(l.count(PocketLayout.Kind.RIVER) >= d.gravity().rivers(), d.id() + " river rooms, seed " + s);
                assertEquals(l.rooms().size(), l.rooms().size());
                Set<String> elements = new HashSet<>();
                for (PocketLayout.Room r : l.rooms()) if (r.kind() == PocketLayout.Kind.TRAP) elements.add(r.element());
                for (String e : elements) assertTrue(d.traps().contains(PocketElement.byId(e)), d.id() + " trap element " + e);
            }
        }
    }
}
