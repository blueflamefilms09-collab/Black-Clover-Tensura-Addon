package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.GradeDamage;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: wave 102 (1.111.0, key god_special_new) ADDS 9 God and 20 Special grade relics to balance the grade pyramid, written by a generator from a table, and everything the owner asked of it
 * is checkable without the game: exactly those grade counts, ids no other wave has (nothing existing was changed or replaced), the base damage of each weapon inside its grade's range of the
 * owner's table, Tensura engravings that exist, attunement only from God up, acts and passives, a lang fragment, and every file each relic needs (mesh and skins of a weapon, sprite and item
 * models, the worn catalog line of the gear, the effect tables of the God gear, the class rows, the Better Combat files of the melee weapons, the shield set, the dungeon roads, the Curios tags).
 * It reads the generated source and the asset files (no game needed).
 */
class Wave102DefsTest {
    private static final Path WAVES = Path.of("src/main/java/com/newuniverse/nusmp/relics/wave");
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path DATA = Path.of("src/main/resources/data");
    private static final Path LANG = Path.of("tools/lang/god_special_new.json");
    private static final Set<String> TENSURA_ENGRAVINGS = Set.of("barrier_piercing", "breathing_support", "crushing", "dead_end_rainbow", "elemental_boost", "elemental_resistance",
            "energy_protection", "energy_steal", "enervation", "growth", "holy_coat", "holy_weapon", "intangibility", "lethargy", "magic_capacity", "magic_interference",
            "magic_protection", "magic_weapon", "magicule_absorption", "restoration", "ruination", "sealing", "severance", "severance_protection", "slotting", "soul_eater",
            "spiritual_protection", "stagnation", "sturdy", "swift", "transcendence", "tsukumogami", "vigor", "vitality");

    private record Def(String id, String name, String grade, String slot, String block) {}

    private static String wave() throws IOException { return Files.readString(WAVES.resolve("Wave102.java")); }

    static List<Def> defs() throws IOException {
        String t = wave();
        Pattern p = Pattern.compile("RelicDef\\.of\\(\"([a-z0-9_]+)\",\\s*\"((?:[^\"\\\\]|\\\\.)*)\",\\s*Grade\\.(GOD|SPECIAL),\\s*RelicSlot\\.(\\w+)\\)");
        Matcher m = p.matcher(t);
        List<int[]> at = new ArrayList<>();
        List<String[]> head = new ArrayList<>();
        while (m.find()) {
            at.add(new int[]{m.start()});
            head.add(new String[]{m.group(1), m.group(2), m.group(3), m.group(4)});
        }
        List<Def> out = new ArrayList<>();
        for (int i = 0; i < at.size(); i++) {
            int end = i + 1 < at.size() ? at.get(i + 1)[0] : t.indexOf("    }\n\n    /** Registers", at.get(i)[0]);
            out.add(new Def(head.get(i)[0], head.get(i)[1], head.get(i)[2], head.get(i)[3], t.substring(at.get(i)[0], end)));
        }
        return out;
    }

    private static boolean weapon(Def d) { return d.slot().equals("MAIN_HAND"); }

    @Test
    void thereAreExactlyNineGodAndTwentySpecialRelics() throws IOException {
        Map<String, Integer> n = new HashMap<>();
        for (Def d : defs()) n.merge(d.grade(), 1, Integer::sum);
        int gods = n.getOrDefault("GOD", 0), specials = n.getOrDefault("SPECIAL", 0);
        assertEquals(9, gods, "God " + n);
        assertEquals(20, specials, "Special " + n);
        assertEquals(29, defs().size());
        long godWeapons = defs().stream().filter(d -> d.grade().equals("GOD") && weapon(d)).count();
        assertEquals(5, godWeapons, "five God weapons (a greatsword, a staff, blades, a bow, a shield)");
        assertEquals(4, defs().stream().filter(d -> d.grade().equals("SPECIAL") && weapon(d)).count());
    }

    @Test
    void theIdsAreListedUniqueAndNeverAnotherWavesId() throws IOException {
        Set<String> seen = new HashSet<>();
        for (Def d : defs()) assertTrue(seen.add(d.id()), "twice in the wave: " + d.id());
        String w = wave();
        int from = w.indexOf("IDS = {");
        Matcher ids = Pattern.compile("\"([a-z0-9_]+)\"").matcher(w.substring(from, w.indexOf("};", from)));
        Set<String> listed = new HashSet<>();
        while (ids.find()) listed.add(ids.group(1));
        assertEquals(seen, listed, "Wave102.IDS is the list of the defs");
        Pattern any = Pattern.compile("RelicDef\\.of\\(\"([a-z0-9_]+)\"|\"([a-z0-9_]+)\"");
        List<String> clash = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java") && !x.getFileName().toString().startsWith("Wave102"))::iterator) {
                String text = Files.readString(f);
                for (String id : seen) if (text.contains("RelicDef.of(\"" + id + "\"")) clash.add(id + " in " + f.getFileName());
            }
        }
        assertEquals(List.of(), clash, "an id that another wave already has: nothing existing may be replaced");
    }

    @Test
    void everyWeaponIsA3DMainHandWeaponWithEngravingsActsAndAPassiveRule() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Def d : defs()) {
            if (!weapon(d)) continue;
            String b = d.block();
            boolean god = d.grade().equals("GOD");
            if (!b.contains(".model3d()")) bad.add(d.id() + " is not 3D");
            if (god != b.contains(".attune()")) bad.add(d.id() + " attunement is wrong for its grade");
            if (god != b.contains(".vessel()")) bad.add(d.id() + " vessel flag is wrong for its grade");
            Matcher e = Pattern.compile("\\.engrave\\(([^)]*)\\)").matcher(b);
            if (!e.find()) {
                bad.add(d.id() + " has no engravings");
                continue;
            }
            Matcher one = Pattern.compile("\"([a-z_]+)(?:@(\\d))?\"").matcher(e.group(1));
            int count = 0;
            while (one.find()) {
                count++;
                if (!TENSURA_ENGRAVINGS.contains(one.group(1))) bad.add(d.id() + ": " + one.group(1) + " is not a Tensura engraving");
            }
            if (count < (god ? 3 : 2)) bad.add(d.id() + " has only " + count + " engravings");
            int acts = b.split("\\.act\\(Act\\.of\\(", -1).length - 1;
            if (god ? (acts != 3) : (acts != 2)) bad.add(d.id() + " has " + acts + " acts");
            if (!b.contains("Passive.flag(\"w102_" + d.id() + "\")")) bad.add(d.id() + " has no passive flag");
            if (!Wave102Passives.RULES.containsKey(d.id())) bad.add(d.id() + " has no passive rule");
            if (!b.contains(".melee(DMG_" + d.id().toUpperCase() + ",")) bad.add(d.id() + " does not use its own damage constant");
        }
        assertEquals(List.of(), bad);
        assertEquals(9, Wave102Passives.RULES.size(), "one rule for each of the nine weapons");
    }

    @Test
    void everyBaseDamageIsInsideTheRangeOfItsGrade() throws IOException {
        Matcher m = Pattern.compile("DMG_([A-Z0-9_]+) = GradeDamage\\.damage\\(\"(special|god)\", \"([a-z]+)\"\\)").matcher(wave());
        int n = 0;
        while (m.find()) {
            n++;
            double d = GradeDamage.damage(m.group(2), m.group(3));
            int[] r = GradeDamage.range(m.group(2));
            assertTrue(d >= r[0] && d <= r[1], m.group(1) + " " + d + " outside " + r[0] + "-" + r[1]);
        }
        assertEquals(9, n);
        assertEquals(35, GradeDamage.range("special")[0]);
        assertEquals(55, GradeDamage.range("special")[1]);
        assertEquals(190, GradeDamage.range("god")[0]);
        assertEquals(240, GradeDamage.range("god")[1]);
        for (Def d : defs()) {
            if (!weapon(d)) continue;
            Matcher g = Pattern.compile("DMG_" + d.id().toUpperCase() + " = GradeDamage\\.damage\\(\"(special|god)\"").matcher(wave());
            assertTrue(g.find(), d.id());
            assertEquals(d.grade().toLowerCase(), g.group(1), d.id() + " takes the damage of its own grade");
        }
    }

    @Test
    void rangedWeaponsAreMarkedAndTheRestAreNot() throws IOException {
        for (Def d : defs()) {
            if (!weapon(d)) continue;
            assertEquals(d.id().endsWith("_bow"), d.block().contains(".bow()"), d.id());
            assertFalse(d.block().contains(".crossbow()"), d.id());
        }
    }

    @Test
    void everyPieceOfGearIsAWornRelicOfItsGradeWithPassivesAndActs() throws IOException {
        int gear = 0;
        for (Def d : defs()) {
            if (weapon(d)) continue;
            gear++;
            String b = d.block();
            boolean god = d.grade().equals("GOD");
            assertEquals(god, b.contains(".attune()"), d.id() + " attunement follows the grade (Special never needs it)");
            assertTrue(b.contains(".passive("), d.id() + " has a passive");
            assertTrue(b.contains(".act(Act.of("), d.id() + " has an act");
            assertTrue(b.contains(".palette(") && b.contains(".look(Look."), d.id() + " has a palette and a look");
            assertFalse(b.contains(".melee(") || b.contains(".model3d()"), d.id() + " is not a weapon");
            Matcher cd = Pattern.compile("\\.cooldown\\((\\d+)\\)(?:\\.mana\\((\\d+)\\))?").matcher(b);
            while (cd.find()) {
                int ticks = Integer.parseInt(cd.group(1));
                int mana = cd.group(2) == null ? 0 : Integer.parseInt(cd.group(2));
                if (!god) {
                    assertTrue(ticks >= 90 * 20 && ticks <= 6000, d.id() + " a Special cooldown of " + ticks / 20 + " s");
                    assertTrue(mana <= 4, d.id() + " a Special cost of " + mana + "%");
                } else {
                    assertTrue(ticks >= 30 * 20 && mana >= 3, d.id() + " a God act is dearer: " + ticks / 20 + " s, " + mana + "%");
                }
            }
        }
        assertEquals(20, gear, "four God and sixteen Special pieces of gear");
        Set<String> slots = new HashSet<>();
        for (Def d : defs()) if (!weapon(d)) slots.add(d.slot());
        assertTrue(slots.containsAll(Set.of("NECK", "BACK", "BELT", "HANDS", "CHARM", "HEAD", "CHEST", "FEET")), "gear over every slot: " + slots);
    }

    @Test
    void noHungerOrFoodMechanicIsWritten() throws IOException {
        String t = wave().toLowerCase();
        for (String bad : new String[]{"hunger", "exhaustion", "setfoodlevel", "saturation", "starv"}) assertFalse(t.contains(bad), "the hunger ban: " + bad);
        String lang = Files.readString(LANG).toLowerCase();
        for (String bad : new String[]{"hunger", "exhaustion", "starv"}) assertFalse(lang.contains(bad), "the hunger ban in text: " + bad);
    }

    @Test
    void everyRelicHasItsLangEntries() throws IOException {
        String lang = Files.readString(LANG);
        assertTrue(lang.startsWith("{"));
        for (Def d : defs()) {
            for (String k : new String[]{"item.nusmp." + d.id(), "item.nusmp." + d.id() + ".desc", "item.nusmp." + d.id() + ".card"})
                assertTrue(lang.contains("\"" + k + "\": \""), "no lang key " + k);
            assertTrue(lang.contains("\"item.nusmp." + d.id() + "\": \"" + d.name().replace("\\\"", "\\\"") + "\""), "the lang name of " + d.id() + " is " + d.name());
        }
        for (int i = 0; i < lang.length(); i++) assertTrue(lang.charAt(i) < 128, "ASCII only: char " + (int) lang.charAt(i) + " at " + i);
        assertFalse(lang.toLowerCase().contains("claude") || lang.toLowerCase().contains("anthropic"), "no assistant names in text");
    }

    @Test
    void everyActNameIsInItsCardText() throws IOException {
        String lang = Files.readString(LANG);
        for (Def d : defs()) {
            Matcher m = Pattern.compile("Act\\.of\\(\"[a-z_]+\", \"((?:[^\"\\\\]|\\\\.)*)\"").matcher(d.block());
            int found = 0;
            while (m.find()) {
                found++;
                assertTrue(lang.contains(m.group(1) + " ("), d.id() + ": the card text does not describe " + m.group(1));
            }
            assertTrue(found >= 1, d.id());
        }
    }

    @Test
    void everyRelicHasItsSpriteItemModelsAndAWeaponItsMeshAndSkins() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Def d : defs()) {
            List<String> files = new ArrayList<>(List.of("textures/item/" + d.id() + ".png", "models/item/" + d.id() + ".json", "models/item/" + d.id() + "_icon.json"));
            if (weapon(d)) files.addAll(List.of("weapon_meshes/" + d.id() + ".txt", "textures/entity/weapon/" + d.id() + ".png", "textures/entity/weapon/" + d.id() + "_glow.png"));
            for (String f : files) if (!Files.exists(ASSETS.resolve(f))) bad.add(d.id() + ": " + f);
            if (!weapon(d)) {
                String model = Files.readString(ASSETS.resolve("models/item/" + d.id() + ".json"));
                if (!model.contains("minecraft:builtin/entity") || model.contains("\"display\"")) bad.add(d.id() + ": the item json is not the plain builtin/entity model");
            }
            if (!Files.exists(ASSETS.resolve("textures/item/" + d.id() + ".png"))) continue;
            byte[] png = Files.readAllBytes(ASSETS.resolve("textures/item/" + d.id() + ".png"));
            int w = ((png[16] & 255) << 24) | ((png[17] & 255) << 16) | ((png[18] & 255) << 8) | (png[19] & 255);
            int h = ((png[20] & 255) << 24) | ((png[21] & 255) << 16) | ((png[22] & 255) << 8) | (png[23] & 255);
            if (w != 16 || h != 16) bad.add(d.id() + ": the sprite is " + w + " x " + h);
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void theGearWearsAModelOfItsOwnInTheWornCatalog() throws IOException {
        List<String> catalog = Files.readAllLines(ASSETS.resolve("relic_worn/catalog.txt"));
        List<String> bad = new ArrayList<>();
        for (Def d : defs()) {
            if (weapon(d)) continue;
            String line = catalog.stream().filter(l -> l.startsWith("relic " + d.id() + " ")).findFirst().orElse(null);
            if (line == null) {
                bad.add(d.id() + " has no relic line");
                continue;
            }
            for (String piece : line.substring(("relic " + d.id() + " ").length()).trim().split("\\s+")) {
                String model = piece.split("@")[0].replace("~", "");
                if (!Files.exists(ASSETS.resolve("relic_worn/" + model + ".txt"))) bad.add(d.id() + ": the model " + model + " does not exist");
            }
            if (d.grade().equals("SPECIAL") && !line.contains("uq_" + d.id())) bad.add(d.id() + " does not wear its own ornament");
            if (d.grade().equals("GOD") && !line.contains("w102_")) bad.add(d.id() + " does not wear its signature model");
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void theGodGearHasEffectTablesWithCustomParticlesAndAnActEffectForEveryAct() throws IOException {
        Pattern tok = Pattern.compile("\"particle\":\\s*\"([^\"]+)\"");
        Set<String> catalogIds = new HashSet<>();
        Matcher ci = Pattern.compile("\"id\":\\s*\"([a-z0-9_]+)\"").matcher(Files.readString(DATA.resolve("nusmp/particle_catalog.json")));
        while (ci.find()) catalogIds.add(ci.group(1));
        int tables = 0;
        for (Def d : defs()) {
            if (weapon(d) || !d.grade().equals("GOD")) continue;
            tables++;
            String t = Files.readString(ASSETS.resolve("visual_upgrade/" + d.id() + ".json"));
            assertTrue(t.split("\"kind\":", -1).length - 1 >= 8, d.id() + " has at least eight ambient layers");
            Matcher m = tok.matcher(t);
            while (m.find()) assertTrue(m.group(1).equals("fx") || (m.group(1).startsWith("nusmp:") && catalogIds.contains(m.group(1).substring(6))), d.id() + " particle " + m.group(1) + " is not one of the mod's own");
            Matcher acts = Pattern.compile("Act\\.of\\(\"([a-z_]+)\"").matcher(d.block());
            while (acts.find()) assertTrue(t.contains("\"" + acts.group(1) + "\": ["), d.id() + " has no effect for the act " + acts.group(1));
        }
        assertEquals(4, tables);
    }

    @Test
    void theWeaponsHaveClassRowsBetterCombatFilesTheShieldSetAndTheGodRoads() throws IOException {
        String classesA = Files.readString(ASSETS.resolve("weapon_classes.json")), classesB = Files.readString(Path.of("tools/relics/weapon_classes.json"));
        String themes = Files.readString(DATA.resolve("nusmp/mega/relic_themes.json"));
        String shields = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/visual/shields/ShieldSet.java"));
        String shieldTool = Files.readString(Path.of("tools/gen_shield_models.py"));
        List<String> bad = new ArrayList<>();
        for (Def d : defs()) {
            if (weapon(d)) {
                if (!classesA.contains("\"" + d.id() + "\"")) bad.add(d.id() + " class row (assets)");
                if (!classesB.contains("\"" + d.id() + "\"")) bad.add(d.id() + " class row (tools)");
                boolean shield = d.block().contains("Look.SHIELD"), bow = d.block().contains(".bow()");
                boolean bc = Files.exists(DATA.resolve("nusmp/weapon_attributes/" + d.id() + ".json"));
                if (shield || bow) {
                    if (bc) bad.add(d.id() + " is a shield or a bow: no Better Combat file");
                } else if (!bc) bad.add(d.id() + " has no Better Combat file");
                if (shield) {
                    if (!shields.contains("\"" + d.id() + "\"") || !shieldTool.contains(d.id())) bad.add(d.id() + " is not in the shield set");
                    if (!Files.exists(ASSETS.resolve("models/item/" + d.id() + "_blocking.json"))) bad.add(d.id() + " has no blocking model");
                }
                if (bow) for (String s : new String[]{"_pull1", "_pull2", "_pull3"}) if (!Files.exists(ASSETS.resolve("weapon_meshes/" + d.id() + s + ".txt"))) bad.add(d.id() + " draw state " + s);
            }
            if (d.grade().equals("GOD") && !themes.contains("\"" + d.id() + "\"")) bad.add(d.id() + " has no dungeon road");
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void theCuriosRelicsAreInTheirSlotTags() throws IOException {
        Map<String, String> tag = Map.of("NECK", "necklace", "BACK", "back", "BELT", "belt", "HANDS", "hands", "CHARM", "charm");
        for (Def d : defs()) {
            String t = tag.get(d.slot());
            if (t == null) continue;
            assertTrue(Files.readString(DATA.resolve("curios/tags/item/" + t + ".json")).contains("\"nusmp:" + d.id() + "\""), d.id() + " is not wearable: not in " + t);
        }
    }

    /**
     * Nothing existing changed: tools/relics/new_wave102/existing_before.txt is the list of the 653 relics (id, grade) the mod had before this wave. Each id must still be in the sources, and
     * where the def is written with a literal id and grade (RelicDef.of("id", "name", Grade.X, ...)) the grade must be the one of the list: no relic was regraded, removed or replaced.
     */
    @Test
    void noExistingRelicWasRegradedRemovedOrReplaced() throws IOException {
        StringBuilder all = new StringBuilder();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/newuniverse/nusmp/relics"))) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java") && !x.getFileName().toString().startsWith("Wave102"))::iterator) all.append(Files.readString(f)).append('\n');
        }
        String src = all.toString();
        List<String> lines = Files.readAllLines(Path.of("tools/relics/new_wave102/existing_before.txt"));
        assertEquals(653, lines.size(), "the list of the relics before this wave");
        Map<String, Integer> perGrade = new HashMap<>();
        List<String> bad = new ArrayList<>();
        for (String line : lines) {
            String[] p = line.trim().split("\\s+");
            perGrade.merge(p[1], 1, Integer::sum);
            if (!src.contains("\"" + p[0] + "\"")) {
                bad.add(p[0] + " is no longer in the sources");
                continue;
            }
            Matcher m = Pattern.compile("RelicDef\\.of\\(\"" + p[0] + "\",\\s*\"(?:[^\"\\\\]|\\\\.)*\",\\s*Grade\\.(\\w+)").matcher(src);
            if (m.find() && !m.group(1).equals(p[1])) bad.add(p[0] + " is " + m.group(1) + " now, was " + p[1]);
        }
        assertEquals(List.of(), bad);
        int god = perGrade.get("GOD"), special = perGrade.get("SPECIAL");
        assertEquals(51, god, "God before");
        assertEquals(96, special, "Special before");
        assertEquals(20, (int) perGrade.get("GENESIS"), "Genesis before: this wave adds none");
    }

    @Test
    void theWaveIsRegisteredByOneLineEachInTheCatalog() throws IOException {
        String c = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/relics/RelicCatalog.java"));
        assertTrue(c.contains("Wave102.defs(out)"), "defs line");
        assertTrue(c.contains("Wave102::hooks"), "hooks line");
        String w = wave();
        assertTrue(w.contains("Wave102Events.register()") && w.contains("Wave102SelfTest.register()"));
        assertTrue(Files.readString(WAVES.resolve("Wave102SelfTest.java")).contains("\"relic.god_special_new\""));
    }

    @Test
    void theEventsKeepToTheServerAndNameNoClientClass() throws IOException {
        String t = Files.readString(WAVES.resolve("Wave102Events.java")) + Files.readString(WAVES.resolve("Wave102.java")) + Files.readString(WAVES.resolve("Wave102Passives.java"));
        assertFalse(t.contains("net.minecraft.client") || t.contains("com.mojang.blaze3d") || t.contains("com.newuniverse.nusmp.client") || t.contains("Minecraft.getInstance"), "server-safe (rule 9)");
        assertTrue(Files.readString(WAVES.resolve("Wave102Events.java")).contains("isClientSide"), "the handler leaves the client alone");
    }
}
