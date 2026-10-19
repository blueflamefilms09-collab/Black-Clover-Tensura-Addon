package com.newuniverse.nusmp.relics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Data-driven: every graded weapon of the sources and tables lies inside its grade's range (docs/grade_damage.md). */
class GradeDamageDataTest {
    private static final Path SRC = Paths.get("src/main/java/com/newuniverse/nusmp");
    private static final String[] GRADES = {"NORMAL", "SPECIAL", "RARE", "UNIQUE", "LEGEND", "GOD", "GENESIS"};

    private static String read(String rel) throws IOException { return Files.readString(SRC.resolve(rel)); }

    @Test void everyWeaponClassHasAFactor() throws IOException {
        JsonObject o = JsonParser.parseString(Files.readString(Paths.get("src/main/resources/assets/nusmp/weapon_classes.json"))).getAsJsonObject();
        for (String c : o.getAsJsonObject("classes").keySet()) {
            // an unknown name would fall back to the middle silently: every class must be listed on purpose
            assertTrue(knownType(c), "weapon class without a damage factor: " + c);
        }
        for (var e : o.getAsJsonObject("weapons").entrySet()) assertTrue(knownType(e.getValue().getAsString()), e.getKey());
    }

    private static boolean knownType(String t) {
        // factor() returns 0.5 for unknown; compare against a name that is certainly unknown to tell a listed 0.5 from the fallback
        return GradeDamage.factor(t) != 0.5 || t.equals("longsword") || t.equals("sword") || t.equals("item") || t.equals("handitem") || t.equals("bow");
    }

    @Test void fixedWeaponTableIsInRange() {
        assertFalse(WeaponGrades.all().isEmpty());
        WeaponGrades.all().forEach((k, en) -> {
            int[] r = GradeDamage.range(en.grade());
            assertTrue(knownType(en.type()), k + " type " + en.type());
            assertTrue(en.damage() >= r[0] && en.damage() <= r[1], k + " " + en.damage());
            assertTrue(GradeDamage.inRange(GRADES[en.grade()], en.damage()), k);
        });
        assertNull(WeaponGrades.of("elsdocia"), "Elsdocia keeps its own damage");
    }

    @Test void magicKindsAreAllGraded() throws IOException {
        Matcher m = Pattern.compile("^\\s+([A-Z_]+)\\(Tiers\\.", Pattern.MULTILINE).matcher(read("item/MagicWeaponItem.java"));
        int n = 0;
        while (m.find()) {
            n++;
            String k = m.group(1);
            if (k.equals("ELSDOCIA")) continue;
            String key = k.equals("RIMEHEART_RUNEBLADE") ? "rimeheart" : k.toLowerCase();
            assertNotNull(WeaponGrades.of(key), "ungraded magic weapon " + k);
        }
        assertTrue(n >= 28, "kinds found: " + n);
    }

    @Test void requipAndHeroWeaponsAreAllGraded() throws IOException {
        Matcher m = Pattern.compile("WEAPON_STATS.put\\(\"([a-z_]+)\"").matcher(read("requip/RequipItems.java"));
        int n = 0;
        while (m.find()) { n++; assertNotNull(WeaponGrades.of(m.group(1)), m.group(1)); }
        assertEquals(6, n);
        m = Pattern.compile("[A-Z_]+\\(\"([a-z_]+)\", \"[^\"]+\", HeroClass\\.").matcher(read("heroweapon/HeroKind.java"));
        n = 0;
        while (m.find()) { n++; assertNotNull(WeaponGrades.of(m.group(1)), m.group(1)); }
        assertEquals(21, n);
    }

    @Test void djinnWeaponsLandInTheirGrade() throws IOException {
        Matcher m = Pattern.compile("row\\(Djinn\\.[A-Z_]+, \"([a-z]+)\"").matcher(read("relics/djinn/weapon/DjinnWeaponRules.java"));
        int n = 0;
        while (m.find()) {
            n++;
            assertTrue(knownType(m.group(1)), m.group(1));
            for (boolean dark : new boolean[]{false, true}) {
                var en = WeaponGrades.djinn(m.group(1), dark);
                assertTrue(GradeDamage.inRange(GRADES[en.grade()], en.damage()), m.group(1));
            }
        }
        assertTrue(n >= 35, "djinn rows: " + n);
    }

    @Test void everyRelicWeaponCardResolvesInsideItsRange() throws IOException {
        Pattern p = Pattern.compile("RelicDef\\.of\\(\"([a-z0-9_]+)\",\\s*\"[^\"]*\",\\s*Grade\\.([A-Z]+),\\s*RelicSlot\\.MAIN_HAND");
        int n = 0;
        try (Stream<Path> s = Files.walk(SRC.resolve("relics"))) {
            for (Path f : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".java"))::iterator) {
                Matcher m = p.matcher(Files.readString(f));
                while (m.find()) {
                    n++;
                    String id = m.group(1);
                    int g = java.util.Arrays.asList(GRADES).indexOf(m.group(2));
                    assertTrue(g >= 0, id);
                    double d = GradeDamage.damage(g, GradeDamage.typeOfId(id));
                    int[] r = GradeDamage.range(g);
                    assertTrue(d >= r[0] && d <= r[1], id + " " + d);
                }
            }
        }
        assertTrue(n > 100, "main-hand cards found: " + n);
    }

    @Test void tierAndAuraStayInTheEffectiveRange() {
        for (int b = 0; b < 7; b++) for (int e = b; e < 7; e++) {
            for (String t : new String[]{"dagger", "greatsword", "bow", "scythe", "spear"}) {
                double v = GradeDamage.raised(b, e, GradeDamage.damage(b, t), t);
                assertTrue(v <= GradeDamage.range(e)[1] && v >= GradeDamage.range(e)[0] || e == b, b + ">" + e);
            }
        }
        float hit = com.newuniverse.nusmp.relics.scaling.AuraScaling.apply(100f, 1.6, 0f, 115.0);
        assertEquals(115f, hit, 1e-4);
        assertEquals(130f, com.newuniverse.nusmp.relics.scaling.AuraScaling.apply(130f, 1.6, 0f, 115.0), 1e-4, "an over-range hit is left alone");
    }
}
