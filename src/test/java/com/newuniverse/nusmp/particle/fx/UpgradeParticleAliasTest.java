package com.newuniverse.nusmp.particle.fx;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 1.110.0: every particle token of the effect tables of the upgraded items resolves to one of the mod's own custom particles, and none to a vanilla one. Three things are pinned: the resolver
 * ({@link UpgradeParticleAlias}: all vanilla names of 1.21.1 have an alias, every alias target is in the particle catalog, a token that is nothing known falls back to a custom mote), the
 * shipped tables (every {@code particle} string of {@code assets/nusmp/visual_upgrade/**} is {@code fx} or {@code nusmp:<catalog id>}, so no loader even needs the alias), and the emitters'
 * sources (no vanilla dust, no lookup in the vanilla particle registry, no raw {@code addParticle} that skips the gate).
 */
class UpgradeParticleAliasTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");
    private static final Path TABLES = ROOT.resolve("src/main/resources/assets/nusmp/visual_upgrade");

    /** The simple particle types and the data-carrying ones of vanilla 1.21.1 (paths). */
    private static final List<String> VANILLA = List.of("angry_villager", "block", "block_marker", "bubble", "cloud", "crit", "damage_indicator", "dragon_breath", "dripping_lava", "falling_lava",
            "landing_lava", "dripping_water", "falling_water", "dust", "dust_color_transition", "effect", "elder_guardian", "enchanted_hit", "enchant", "end_rod", "entity_effect", "explosion_emitter",
            "explosion", "gust", "small_gust", "gust_emitter_large", "gust_emitter_small", "sonic_boom", "falling_dust", "firework", "fishing", "flame", "infested", "cherry_leaves", "sculk_soul",
            "sculk_charge", "sculk_charge_pop", "soul_fire_flame", "soul", "flash", "happy_villager", "composter", "heart", "instant_effect", "item", "vibration", "item_slime", "item_cobweb",
            "item_snowball", "large_smoke", "lava", "mycelium", "note", "poof", "portal", "rain", "smoke", "white_smoke", "sneeze", "spit", "squid_ink", "sweep_attack", "totem_of_undying", "underwater",
            "splash", "witch", "bubble_pop", "current_down", "bubble_column_up", "nautilus", "dolphin", "campfire_cosy_smoke", "campfire_signal_smoke", "dripping_honey", "falling_honey", "landing_honey",
            "falling_nectar", "falling_spore_blossom", "ash", "crimson_spore", "warped_spore", "spore_blossom_air", "dripping_obsidian_tear", "falling_obsidian_tear", "landing_obsidian_tear",
            "reverse_portal", "white_ash", "small_flame", "snowflake", "dripping_dripstone_lava", "falling_dripstone_lava", "dripping_dripstone_water", "falling_dripstone_water", "glow_squid_ink", "glow",
            "wax_on", "wax_off", "electric_spark", "scrape", "shriek", "egg_crack", "dust_plume", "trial_spawner_detection", "trial_spawner_detection_ominous", "vault_connection", "dust_pillar",
            "ominous_spawning", "raid_omen", "trial_omen");

    @Test
    void theCatalogIsThere() {
        assertTrue(FxCatalog.all().size() >= 70, "the particle catalog could not be read");
    }

    @Test
    void everyVanillaParticleOfTheGameHasAnAliasThatIsACustomParticle() {
        List<String> problems = new ArrayList<>();
        for (String v : VANILLA) {
            if (v.equals("dust") || v.equals("dust_color_transition")) continue;             // the coloured dust is the hue-themed mote, not an alias
            if (!UpgradeParticleAlias.vanillaNames().contains(v)) problems.add(v + ": no alias");
            for (String token : new String[]{v, "minecraft:" + v}) {
                String id = UpgradeParticleAlias.resolve(token);
                if (id == null || !FxCatalog.has(id)) problems.add(token + " -> " + id + " is not a catalog particle");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everyAliasTargetIsInTheCatalogAndNoAliasNamesAVanillaOne() {
        for (String t : UpgradeParticleAlias.targets()) assertTrue(FxCatalog.has(t), "alias target " + t + " is not a catalog particle");
        assertTrue(FxCatalog.has(UpgradeParticleAlias.FALLBACK));
        for (String n : UpgradeParticleAlias.vanillaNames()) assertTrue(VANILLA.contains(n), "alias key " + n + " is not a vanilla particle of 1.21.1 (typo?)");
    }

    @Test
    void theThemedTokensAndTheCustomIdsAndTheUnknownNames() {
        for (String t : new String[]{null, "", "  ", "dust", "fx", "FX", "minecraft:dust"}) {
            assertTrue(UpgradeParticleAlias.isThemed(t), String.valueOf(t));
            assertNull(UpgradeParticleAlias.resolve(t), String.valueOf(t));
        }
        for (FxCatalog.Def d : FxCatalog.all()) {
            assertEquals(d.id(), UpgradeParticleAlias.resolve(d.id()), d.id());
            assertEquals(d.id(), UpgradeParticleAlias.resolve("nusmp:" + d.id()), d.id());
        }
        assertEquals("flame_tongue", UpgradeParticleAlias.resolve("minecraft:flame"));
        assertEquals("flame_tongue", UpgradeParticleAlias.resolve("flame"));
        assertEquals("mote", UpgradeParticleAlias.resolve("minecraft:end_rod"));
        assertEquals(UpgradeParticleAlias.FALLBACK, UpgradeParticleAlias.resolve("nobody:nothing"));
        assertEquals(UpgradeParticleAlias.FALLBACK, UpgradeParticleAlias.resolve("totally_unknown"));
    }

    @Test
    void theDyeKeepsTheLayersOwnColourAndFillsInTheVanillaOneOnlyForPlainWhite() {
        assertEquals(0x123456, UpgradeParticleAlias.dye("minecraft:witch", 0x123456));
        assertEquals(0xA040FF, UpgradeParticleAlias.dye("minecraft:witch", 0xFFFFFF));
        assertEquals(0xA040FF, UpgradeParticleAlias.dye("witch", -1));
        assertEquals(-1, UpgradeParticleAlias.dye("minecraft:end_rod", -1));
        assertEquals(0xFFFFFF, UpgradeParticleAlias.dye("minecraft:end_rod", 0xFFFFFF));
    }

    @Test
    void theJsonAliasTableOfTheGeneratorsIsTheJavaTable() throws IOException {
        JsonObject o = JsonParser.parseString(Files.readString(ROOT.resolve("tools/visual_upgrade/particle_alias.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject alias = o.getAsJsonObject("alias");
        assertEquals(UpgradeParticleAlias.vanillaNames().size(), alias.size(), "the python alias table (tools/visual_upgrade/particle_alias.json) and the Java one differ in size");
        for (Map.Entry<String, JsonElement> e : alias.entrySet()) {
            assertTrue(UpgradeParticleAlias.vanillaNames().contains(e.getKey()), e.getKey());
            assertEquals(UpgradeParticleAlias.resolve(e.getKey()), e.getValue().getAsString(), e.getKey());
        }
        assertEquals(13, o.getAsJsonObject("dye").size());
    }

    // ------------------------------------------------------------------------------------------------- the shipped tables
    private static void tokens(JsonElement e, String where, List<String> out) {
        if (e.isJsonObject()) {
            for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
                if (en.getKey().equals("particle") && en.getValue().isJsonPrimitive() && en.getValue().getAsJsonPrimitive().isString()) out.add(where + "|" + en.getValue().getAsString());
                else tokens(en.getValue(), where, out);
            }
        } else if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) tokens(x, where, out);
        }
    }

    @Test
    void everyParticleLayerOfEveryShippedTableIsACustomParticle() throws IOException {
        List<String> all = new ArrayList<>();
        int files = 0;
        try (Stream<Path> walk = Files.walk(TABLES)) {
            for (Path f : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".json"))::iterator) {
                files++;
                tokens(JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)), TABLES.relativize(f).toString().replace('\\', '/'), all);
            }
        }
        assertTrue(files >= 100, "only " + files + " tables found");
        assertTrue(all.size() >= 500, "only " + all.size() + " particle tokens found");
        List<String> bad = new ArrayList<>();
        Set<String> custom = Set.copyOf(FxCatalog.ids());
        for (String w : all) {
            String token = w.substring(w.indexOf('|') + 1);
            String id = UpgradeParticleAlias.resolve(token);
            boolean themed = token.equals("fx");
            boolean catalog = token.startsWith("nusmp:") && custom.contains(token.substring(6));
            if (!themed && !catalog) bad.add(w + " (a vanilla or unknown token: run tools/visual_upgrade/custom_particles.py)");
            if (!themed && (id == null || !custom.contains(id))) bad.add(w + " resolves to " + id);
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    // ------------------------------------------------------------------------------------------------- the emitters' sources
    private static final List<String> EMITTERS = List.of("client/upgrade/UpgradeFx.java", "client/vupgrade/godb/UpgradeFx.java", "client/vupgrade/godb/UpgradeSpecs.java",
            "client/relics/item/vu/VuFx.java", "client/relics/item/vu/DemonSlayerVu.java", "client/relics/vu/VuFxClient.java", "client/relics/fx/UpgradeFxClient.java",
            "relics/upgrade/UpgradeActFx.java", "client/relics/djinn/tiny/TinyDjinnFx.java", "client/ElsdociaGlitterClient.java");

    private static String code(String rel) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String line : Files.readAllLines(SRC.resolve(rel), StandardCharsets.UTF_8)) {
            String t = line.strip();
            if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) continue;
            int c = line.indexOf("//");
            sb.append(c >= 0 ? line.substring(0, c) : line).append('\n');
        }
        return sb.toString();
    }

    @Test
    void noEmitterOfTheUpgradedItemsSpawnsAVanillaParticleOrSkipsTheGate() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String rel : EMITTERS) {
            String c = code(rel);
            for (String forbidden : new String[]{"DustParticleOptions", "BuiltInRegistries.PARTICLE_TYPE", "ParticleTypes.", "level.addParticle(", "mc.level.addParticle(", "FxOld.vanilla"}) {
                if (c.contains(forbidden)) bad.add(rel + " uses " + forbidden);
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    @Test
    void theClientEmittersAllAskTheGate() throws IOException {
        for (String rel : EMITTERS) {
            if (rel.startsWith("relics/") || rel.endsWith("UpgradeSpecs.java")) continue;       // the act burst is the server's, the table loader spawns nothing
            String c = code(rel);
            assertTrue(c.contains("UpgradeGate.begin(") || c.contains("UpgradeGate.burst("), rel + " never opens the gate");
            assertTrue(c.contains("UpgradeGate.add("), rel + " never spawns through the gate");
        }
    }
}
