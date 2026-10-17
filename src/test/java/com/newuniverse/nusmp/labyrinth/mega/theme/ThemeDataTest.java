package com.newuniverse.nusmp.labyrinth.mega.theme;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeSwap.How;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeSwap.Resolved;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the eleven theme files are the part of the Region Theme that cannot be type-checked and cannot be seen without a game, and a typo in one is a
 * sector that silently keeps its deepslate stairs or, worse, a block id that does not exist. This test is the compile step for them: it parses the
 * very bytes that ship with the very parser that runs in game ({@code ThemeDef.parse}), checks every block id against the list of vanilla 1.21.1
 * block ids ({@code tools/labyrinth/vanilla_blocks_1_21_1.txt}), derives the full 31-block swap table of each theme with {@code ThemeSwap}, and fails
 * if any palette block has no target, if a theme is not clearly different from the baseline and from every other theme, if a catastrophe table is
 * empty or an ambient particle is not one a plain {@code sendParticles} can draw. Pure: reads files from the project root (the working directory of
 * Gradle's test task), no Minecraft, no JSON library.
 */
class ThemeDataTest {
    private static final Path THEMES = Path.of("src/main/resources/data/nusmp/mega/themes");
    private static final Path VANILLA = Path.of("tools/labyrinth/vanilla_blocks_1_21_1.txt");
    /** Particles that are a SimpleParticleType in 1.21.1 (checked against ParticleTypes in the API dump): the only kind a plain sendParticles can draw. */
    private static final Set<String> SIMPLE_PARTICLES = Set.of("minecraft:ash", "minecraft:crimson_spore", "minecraft:warped_spore", "minecraft:portal", "minecraft:reverse_portal",
            "minecraft:flame", "minecraft:small_flame", "minecraft:snowflake", "minecraft:spore_blossom_air", "minecraft:electric_spark", "minecraft:sculk_soul", "minecraft:soul",
            "minecraft:end_rod", "minecraft:glow", "minecraft:enchant", "minecraft:white_ash", "minecraft:witch", "minecraft:cloud", "minecraft:smoke", "minecraft:soul_fire_flame",
            "nusmp:light_rukh");
    /** The themes the mod ships, in the order of the {@code RegionTheme} enum (append a new one here AND in the enum; a file nobody lists is a stray). */
    private static final List<String> SHIPPED = List.of("gravity", "blood", "spatial", "flame", "frost", "verdant", "storm", "shadow", "astral", "radiant", "abyss");
    /** The one mod particle a theme may name: a SimpleParticleType registered by {@code particle/rukh/RukhParticles}. */
    private static final Path RUKH = Path.of("src/main/java/com/newuniverse/nusmp/particle/rukh/RukhParticles.java");

    private static Set<String> vanilla() throws IOException {
        Set<String> ids = new TreeSet<>();
        for (String line : Files.readAllLines(VANILLA, StandardCharsets.UTF_8)) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("#")) ids.add("minecraft:" + t);
        }
        return ids;
    }

    private static Map<String, ThemeDef> themes() throws IOException {
        Map<String, ThemeDef> out = new TreeMap<>();
        try (var files = Files.list(THEMES)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                String file = p.getFileName().toString();
                String id = file.substring(0, file.length() - ".json".length());
                ThemeDef.Parsed parsed = ThemeDef.parse(id, MiniJson.parse(Files.readString(p, StandardCharsets.UTF_8)));
                assertNotNull(parsed.def(), file + " did not parse");
                assertEquals(List.of(), parsed.warnings(), file + " has warnings");
                assertEquals(id, parsed.def().id(), file + ": the id inside must be the file name");
                out.put(id, parsed.def());
            }
        }
        return out;
    }

    @Test
    void everyRegionThemeHasAFileAndEveryFileIsAShippedTheme() throws IOException {
        Set<String> files = themes().keySet();
        for (RegionTheme t : RegionTheme.values()) assertTrue(files.contains(t.id()), "RegionTheme " + t + " has no data/nusmp/mega/themes/" + t.id() + ".json");
        for (String f : files) assertTrue(SHIPPED.contains(f), "themes/" + f + ".json is not a shipped theme: add it to RegionTheme (appended last) and to SHIPPED here");
        assertEquals(new TreeSet<>(SHIPPED), new TreeSet<>(files), "one file per shipped theme");
        assertEquals(11, files.size());
    }

    @Test
    void theEnumListsTheShippedThemesInTheShippedOrder() {
        // RegionTheme is append-only; until the two newest entries (radiant, abyss) are merged the enum is a prefix of the shipped list, afterwards it is the whole list
        RegionTheme[] all = RegionTheme.values();
        assertTrue(all.length <= SHIPPED.size() && all.length >= 9, "the enum has " + all.length + " themes");
        for (int i = 0; i < all.length; i++) assertEquals(SHIPPED.get(i), all[i].id(), "RegionTheme entry " + i);
    }

    @Test
    void theLightRukhAmbientNamesAParticleTheModRegisters() throws IOException {
        String src = Files.readString(RUKH, StandardCharsets.UTF_8);
        assertTrue(src.contains("PARTICLES.register(\"light_rukh\", () -> new SimpleParticleType("), "nusmp:light_rukh must be a SimpleParticleType registered by RukhParticles");
        assertEquals("nusmp:light_rukh", themes().get("radiant").ambient(), "the Holy road is thick with Light Rukh");
    }

    @Test
    void everyBlockIdIsAVanillaBlock() throws IOException {
        Set<String> vanilla = vanilla();
        assertTrue(vanilla.size() > 900, "the vanilla block list looks truncated: " + vanilla.size());
        for (ThemeDef d : themes().values()) {
            for (Map.Entry<String, String> f : d.families().entrySet()) assertTrue(vanilla.contains(f.getValue()), d.id() + " families." + f.getKey() + " = " + f.getValue() + " is not a vanilla block");
            for (Map.Entry<String, String> x : d.explicit().entrySet()) {
                assertTrue(vanilla.contains(x.getKey()), d.id() + " explicit source " + x.getKey() + " is not a vanilla block");
                assertTrue(vanilla.contains(x.getValue()), d.id() + " explicit target " + x.getValue() + " is not a vanilla block");
            }
        }
    }

    @Test
    void everyPaletteBlockHasATargetInEveryTheme() throws IOException {
        Predicate<String> vanilla = vanilla()::contains;
        for (ThemeDef d : themes().values()) {
            assertEquals(List.of(), ThemeSwap.problems(d.families(), d.explicit(), vanilla), d.id());
            Map<String, Resolved> table = ThemeSwap.table(d.families(), d.explicit(), vanilla);
            for (ThemeSwap.Entry e : ThemeSwap.PALETTE) assertTrue(table.get(e.id()).how() != How.NONE, d.id() + " leaves " + e.id() + " alone");
            for (String slot : List.of("brick", "tile", "polished", "chiseled", "cobbled", "cracked_brick", "cracked_tile", "rock", "wood"))
                assertTrue(d.families().containsKey(slot), d.id() + " has no " + slot + " family");
        }
    }

    @Test
    void theBaselineSwapsNothingAndEveryOtherThemeSwapsPlenty() throws IOException {
        Predicate<String> vanilla = vanilla()::contains;
        for (ThemeDef d : themes().values()) {
            long swaps = ThemeSwap.table(d.families(), d.explicit(), vanilla).values().stream().filter(Resolved::swaps).count();
            if (d.id().equals("gravity")) assertEquals(0L, swaps, "gravity is the identity");
            else assertTrue(swaps >= 10, d.id() + " swaps only " + swaps + " blocks");
        }
    }

    @Test
    void fallbacksAreRareAndNeverWood() throws IOException {
        Predicate<String> vanilla = vanilla()::contains;
        for (ThemeDef d : themes().values()) {
            int fallbacks = 0;
            for (ThemeSwap.Entry e : ThemeSwap.PALETTE) {
                Resolved r = ThemeSwap.resolve(e, d.families(), d.explicit(), vanilla);
                if (r.how() == How.FALLBACK) {
                    fallbacks++;
                    assertTrue(e.slot() != ThemeSwap.Slot.WOOD, d.id() + ": wood fell back to stone for " + e.id());
                    assertTrue(e.shape() != ThemeSwap.Shape.PLAIN, d.id() + ": a plain block fell back (" + e.id() + ")");
                }
            }
            assertTrue(fallbacks <= 8, d.id() + " relies on " + fallbacks + " fallback shapes: give the missing shapes in the explicit map");
        }
    }

    @Test
    void everyWoodBlockStaysInTheThemesWoodFamily() throws IOException {
        Predicate<String> vanilla = vanilla()::contains;
        for (ThemeDef d : themes().values()) {
            String stem = d.families().get("wood").replace("_planks", "");
            for (ThemeSwap.Entry e : ThemeSwap.PALETTE) {
                if (e.slot() != ThemeSwap.Slot.WOOD) continue;
                String t = ThemeSwap.resolve(e, d.families(), d.explicit(), vanilla).target();
                String path = t.substring(t.indexOf(':') + 1);
                String stemPath = stem.substring(stem.indexOf(':') + 1);
                assertTrue(path.contains(stemPath), d.id() + ": " + e.id() + " became " + t + ", not a " + stemPath + " block");
            }
        }
    }

    @Test
    void everyThemeIsClearlyDifferentFromEveryOther() throws IOException {
        List<ThemeDef> all = new ArrayList<>(themes().values());
        List<String> slots = List.of("brick", "tile", "polished", "chiseled", "cobbled", "cracked_brick", "cracked_tile", "rock", "wood");
        for (int a = 0; a < all.size(); a++) {
            for (int b = a + 1; b < all.size(); b++) {
                int differ = 0;
                for (String s : slots) if (!all.get(a).families().get(s).equals(all.get(b).families().get(s))) differ++;
                assertTrue(differ >= 4, all.get(a).id() + " and " + all.get(b).id() + " differ in only " + differ + " of 9 slots");
                assertTrue(!all.get(a).families().get("brick").equals(all.get(b).families().get("brick")), all.get(a).id() + " and " + all.get(b).id() + " have the same walls");
            }
        }
    }

    @Test
    void everyThemeCarriesItsWhyKeywordsCatastrophesAndAmbient() throws IOException {
        for (Map.Entry<String, ThemeDef> x : themes().entrySet()) {
            ThemeDef d = x.getValue();
            String raw = Files.readString(THEMES.resolve(x.getKey() + ".json"), StandardCharsets.UTF_8);
            assertTrue(raw.contains("\"_why\": \""), d.id() + ": every JSON file says why it is built that way");
            assertTrue(d.keywords().size() >= 8, d.id() + " has too few relic keywords");
            for (String k : d.keywords()) assertTrue(k.matches("[a-z0-9]{3,}"), d.id() + ": keyword '" + k + "' must be one lower case word of 3+ letters");
            assertTrue(d.weight("collapse") + d.weight("flood") + d.weight("spirit") > 0, d.id() + " has no catastrophe");
            assertTrue(SIMPLE_PARTICLES.contains(d.ambient()), d.id() + ": ambient '" + d.ambient() + "' is not a simple particle");
            assertTrue(d.name().length() > 2 && d.color() != 0);
        }
    }

    @Test
    void theLightSwapsOfTheSoulAndFlameThemesMirrorEachOther() throws IOException {
        Map<String, ThemeDef> t = themes();
        assertEquals("minecraft:soul_lantern", t.get("shadow").explicit().get("minecraft:lantern"));
        assertEquals("minecraft:soul_campfire", t.get("shadow").explicit().get("minecraft:campfire"));
        assertEquals("minecraft:lantern", t.get("flame").explicit().get("minecraft:soul_lantern"));
        assertEquals("minecraft:campfire", t.get("flame").explicit().get("minecraft:soul_campfire"));
        assertEquals("minecraft:quartz_stairs", t.get("astral").explicit().get("minecraft:deepslate_brick_stairs"), "quartz bricks have no stairs of their own");
    }
}
