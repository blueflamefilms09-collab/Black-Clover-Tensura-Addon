package com.newuniverse.nusmp.client.relics.item;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the 1.62.0 Legendary Bow shipped with a hand-solved third-person display ({@code rotation [75, 90, 0]}) and stood up in front of the player like a vertical
 * plank with the arm posed wrongly (the owner's screenshot 81). Every bow and crossbow of the mod must be held the way the proven relic bows are, so this guard reads each
 * bow-class model json and its meshes and fails on a display that tips the bow: every weapon of class bow carries vanilla's Bow display on meshes placed on the sprite diagonal (bow_vanilla.py), no crossbow
 * is tilted about x by more than {@link #MAX_TILT} degrees in either person, a mesh of the draw stages keeps the place line of its rest mesh (it must not jump
 * when the string is drawn), and every bow mesh has its grip at or below the middle.
 */
class BowDisplayGuardTest {
    static final Path RES = Path.of("src", "main", "resources", "assets", "nusmp");
    static final double MAX_TILT = 30.0;
    static final Pattern ROT = Pattern.compile("\"%s\"\\s*:\\s*\\{[^}]*?\"rotation\"\\s*:\\s*\\[\\s*(-?[\\d.]+)\\s*,\\s*(-?[\\d.]+)\\s*,\\s*(-?[\\d.]+)");
    static final Pattern PLACE = Pattern.compile("(?m)^place\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)");
    static final Pattern GRIP = Pattern.compile("(?m)^grip\\s+(\\S+)");

    /** The ids of every bow-type weapon: its class is bow, or its id says bow. */
    static List<String> bowIds() throws IOException {
        WeaponClasses classes = WeaponMeshesTest.loadClasses();
        List<String> ids = new ArrayList<>();
        try (Stream<Path> s = Files.list(RES.resolve("models/item"))) {
            for (Path p : (Iterable<Path>) s::iterator) {
                String name = p.getFileName().toString();
                if (!name.endsWith(".json") || name.endsWith("_icon.json")) continue;
                String id = name.substring(0, name.length() - 5);
                if (!Files.exists(RES.resolve("weapon_meshes/" + id + ".txt"))) continue;
                if ("bow".equals(classes.className(id)) || id.contains("bow")) ids.add(id);
            }
        }
        return ids;
    }

    static double[] rotation(String json, String key) {
        Matcher m = Pattern.compile(String.format(ROT.pattern(), key)).matcher(json);
        return m.find() ? new double[]{Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3))} : null;
    }

    @Test
    void thereAreBowsToCheck() throws IOException {
        List<String> ids = bowIds();
        assertTrue(ids.contains("legendary_bow"), "the Legendary Bow is a bow-class item: " + ids);
        assertTrue(ids.size() >= 5, "the relic bows and crossbows are found: " + ids);
    }

    static final String[] VANILLA_KEYS = {"thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "firstperson_lefthand"};
    static final double[][] VANILLA_ROT = {{-80, 260, -40}, {-80, -280, 40}, {0, -90, 25}, {0, 90, -25}};

    /** Every bow of class "bow" is held with vanilla's Bow display and its meshes lie on the sprite diagonal (tools/relics/bow_vanilla.py writes both). */
    @Test
    void everyBowIsHeldLikeTheVanillaBow() throws IOException {
        WeaponClasses classes = WeaponMeshesTest.loadClasses();
        List<String> bad = new ArrayList<>();
        for (String id : bowIds()) {
            if (!"bow".equals(classes.className(id))) continue;
            String json = Files.readString(RES.resolve("models/item/" + id + ".json"));
            for (int i = 0; i < VANILLA_KEYS.length; i++) {
                double[] r = rotation(json, VANILLA_KEYS[i]);
                if (r == null || r[0] != VANILLA_ROT[i][0] || r[1] != VANILLA_ROT[i][1] || r[2] != VANILLA_ROT[i][2])
                    bad.add(id + " " + VANILLA_KEYS[i] + " is not vanilla's " + java.util.Arrays.toString(VANILLA_ROT[i]));
            }
            try (Stream<Path> s = Files.list(RES.resolve("weapon_meshes"))) {
                for (Path p : (Iterable<Path>) s::iterator) {
                    String n = p.getFileName().toString();
                    if (!n.equals(id + ".txt") && !(n.startsWith(id + "_") && n.endsWith(".txt"))) continue;
                    Matcher pl = PLACE.matcher(Files.readString(p));
                    if (!pl.find() || Math.abs(Double.parseDouble(pl.group(3)) - 45.0) > 1.0) bad.add(n + " is not placed on the sprite diagonal (place angle 45)");
                }
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    /** No other ranged weapon (the crossbows) is tipped about x by more than MAX_TILT in either person. */
    @Test
    void noCrossbowIsTippedOverInHand() throws IOException {
        WeaponClasses classes = WeaponMeshesTest.loadClasses();
        List<String> bad = new ArrayList<>();
        for (String id : bowIds()) {
            if ("bow".equals(classes.className(id))) continue;
            String json = Files.readString(RES.resolve("models/item/" + id + ".json"));
            for (String key : VANILLA_KEYS) {
                double[] r = rotation(json, key);
                if (r != null && Math.abs(r[0]) > MAX_TILT) bad.add(id + " " + key + " is tipped about x by " + r[0]);
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    @Test
    void theDrawStagesKeepThePlaceOfTheRestMesh() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : bowIds()) {
            Matcher rest = PLACE.matcher(Files.readString(RES.resolve("weapon_meshes/" + id + ".txt")));
            if (!rest.find()) continue;
            for (String stage : new String[]{"_pull1", "_pull2", "_pull3", "_charged", "_spent"}) {
                Path p = RES.resolve("weapon_meshes/" + id + stage + ".txt");
                if (!Files.exists(p)) continue;
                Matcher m = PLACE.matcher(Files.readString(p));
                if (!m.find()) { bad.add(id + stage + " has no place line"); continue; }
                for (int g = 1; g <= 5; g++) {
                    double a = Double.parseDouble(rest.group(g)), b = Double.parseDouble(m.group(g));
                    if (Math.abs(a - b) > 0.02 * Math.max(1.0, Math.abs(a))) bad.add(id + stage + " places differently from its rest mesh (field " + g + ": " + a + " vs " + b + ")");
                }
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    @Test
    void everyBowMeshIsHeldAtOrBelowTheMiddle() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : bowIds()) {
            Matcher g = GRIP.matcher(Files.readString(RES.resolve("weapon_meshes/" + id + ".txt")));
            if (g.find() && Double.parseDouble(g.group(1)) > 0.5) bad.add(id + " has the grip " + g.group(1) + " (above 0.5 flips the mesh)");
        }
        assertEquals(List.of(), bad);
    }

    /**
     * 1.72.0 (owner screenshots): a bow is SIZED like vanilla's too. The mesh bows were 1.76 to 2.58 blocks tip to tip (vanilla's is about 1.15), so the golden Legendary Bow filled the
     * screen in first person and stood taller than the player in third. Every mesh of a bow-class weapon, rest and draw stages, is between 0.9 and 1.4 blocks long (tools/relics/bow_vanilla.py).
     */
    @Test
    void everyBowIsTheSizeOfTheVanillaBow() throws IOException {
        WeaponClasses classes = WeaponMeshesTest.loadClasses();
        List<String> bad = new ArrayList<>();
        for (String id : bowIds()) {
            if (!"bow".equals(classes.className(id))) continue;
            try (Stream<Path> s = Files.list(RES.resolve("weapon_meshes"))) {
                for (Path p : (Iterable<Path>) s::iterator) {
                    String n = p.getFileName().toString();
                    if (!n.equals(id + ".txt") && !(n.startsWith(id + "_") && n.endsWith(".txt"))) continue;
                    String text = Files.readString(p);
                    Matcher pl = PLACE.matcher(text);
                    if (!pl.find()) continue;
                    double scale = Double.parseDouble(pl.group(4)), ymin = Double.parseDouble(pl.group(5)), ymax = -1e9;
                    for (String line : text.split("\n")) {
                        String[] v = line.trim().split("\\s+");
                        if (v.length < 22 || !(v[0].equals("q") || v[0].equals("qn"))) continue;
                        int stride = v[0].equals("q") ? 5 : 8;
                        for (int k = 0; k < 4; k++) ymax = Math.max(ymax, Double.parseDouble(v[2 + k * stride + 1]));
                    }
                    double length = (ymax - ymin) * scale;
                    if (length < 0.9 || length > 1.4) bad.add(n + " is " + String.format(java.util.Locale.ROOT, "%.2f", length) + " blocks long (a bow is 0.9 to 1.4, vanilla's about 1.15)");
                }
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }
}
