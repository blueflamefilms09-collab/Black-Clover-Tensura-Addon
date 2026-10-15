package com.newuniverse.nusmp.bettercombat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: Better Combat finds a weapon by its registry id in data/nusmp/weapon_attributes/ and a missing file fails SILENTLY (the weapon just keeps
 * the vanilla swing), a bad animation reference is only logged by Better Combat at resource load, and none of that is a compile error. This test
 * is the compile step for the data, so adding a melee weapon without giving it a file fails the build instead of shipping a weapon that ignores
 * the combat mod. It is pure: java.nio and a tiny JSON reader (no JSON library is on the test class path), CWD = repo root.
 *
 * <p>Fix for a new weapon: run {@code python tools/bettercombat/gen_attributes.py} (it refreshes the inventory and writes the file), then commit it.
 */
class WeaponAttributesTest {
    private static final Path ROOT = Path.of(".");
    private static final Path ATTRS = ROOT.resolve("src/main/resources/data/nusmp/weapon_attributes");
    private static final Path ANIMS = ROOT.resolve("src/main/resources/assets/nusmp/attack_animations");
    private static final Path CLASSES = ROOT.resolve("src/main/resources/assets/nusmp/weapon_classes.json");
    private static final Path MANIFEST = ROOT.resolve("tools/bettercombat/manifest.json");
    private static final Path JAVA = ROOT.resolve("src/main/java");
    private static final Set<String> NON_MELEE = Set.of("handitem", "crossbow", "bow", "shield", "item");
    private static final Set<String> HITBOXES = Set.of("HORIZONTAL_PLANE", "VERTICAL_PLANE", "FORWARD_BOX");
    private static final String FIX = " -> run `python tools/bettercombat/gen_attributes.py` (and `python tools/bettercombat/inventory.py` for a new weapon) and commit the files";

    // ---------------------------------------------------------------- the weapons that must have a file
    /** Every melee id: the manifest, the class table (minus non-melee classes) and every relic def that calls .melee(...), plus pair left halves. */
    private static Set<String> meleeIds() throws IOException {
        Set<String> ids = new TreeSet<>();
        Map<String, Object> classes = obj(parse(read(CLASSES)).get("weapons"));
        for (Map.Entry<String, Object> e : classes.entrySet()) if (!NON_MELEE.contains((String) e.getValue())) ids.add(e.getKey());
        Map<String, Object> manifest = obj(obj(parse(read(MANIFEST))).get("weapons"));
        ids.addAll(manifest.keySet());
        Pattern of = Pattern.compile("RelicDef\\.of\\(\"([a-z0-9_]+)\"");
        try (Stream<Path> files = Files.walk(JAVA)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                String t = read(f);
                if (!t.contains("RelicDef.of(")) continue;
                List<int[]> spans = new ArrayList<>();
                List<String> names = new ArrayList<>();
                Matcher m = of.matcher(t);
                while (m.find()) { spans.add(new int[]{m.end(), -1}); names.add(m.group(1)); }
                for (int i = 0; i < spans.size(); i++) {
                    int next = t.indexOf("RelicDef.of(", spans.get(i)[0]);
                    String blk = t.substring(spans.get(i)[0], next < 0 ? t.length() : next);
                    // a relic that is a crossbow / hand item also carries melee numbers (every hand relic does): the class table says it is no melee weapon
                    if (blk.contains(".melee(") && !NON_MELEE.contains(classes.get(names.get(i)))) ids.add(names.get(i));
                    if (blk.contains(".pair(")) ids.add(names.get(i) + "_left");
                }
            }
        }
        return ids;
    }

    @Test
    void everyMeleeWeaponHasAFile() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String id : meleeIds()) if (!Files.exists(ATTRS.resolve(id + ".json"))) missing.add(id);
        assertTrue(missing.isEmpty(), "melee weapons without a Better Combat file in data/nusmp/weapon_attributes: " + missing + FIX);
    }

    @Test
    void manifestIsInSyncWithTheClassTable() throws IOException {
        Map<String, Object> manifest = obj(obj(parse(read(MANIFEST))).get("weapons"));
        Map<String, Object> excluded = obj(obj(parse(read(MANIFEST))).get("excluded"));
        Map<String, Object> classes = obj(parse(read(CLASSES)).get("weapons"));
        List<String> unknown = new ArrayList<>();
        for (String id : classes.keySet()) if (!manifest.containsKey(id) && !excluded.containsKey(id)) unknown.add(id);
        assertTrue(unknown.isEmpty(), "weapon_classes.json entries the Better Combat inventory has never seen: " + unknown + FIX);
    }

    // ---------------------------------------------------------------- the files themselves
    @Test
    void everyFileParsesWithSaneNumbers() throws IOException {
        int n = 0;
        try (Stream<Path> files = Files.list(ATTRS)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json")).sorted()::iterator) {
                n++;
                String name = f.getFileName().toString();
                Map<String, Object> doc = parse(read(f));
                Object parent = doc.get("parent");
                assertTrue(parent instanceof String && ((String) parent).startsWith("bettercombat:"), name + ": parent must start with bettercombat:");
                Object attrs = doc.get("attributes");
                if (attrs == null) continue;
                Map<String, Object> a = obj(attrs);
                if (a.containsKey("range_bonus")) {
                    assertTrue(a.get("range_bonus") instanceof Double, name + ": range_bonus must be a number");
                    double r = (Double) a.get("range_bonus");
                    assertTrue(r >= -1.0 && r <= 3.0, name + ": range_bonus " + r + " outside -1..3");
                }
                if (a.containsKey("two_handed")) assertTrue(a.get("two_handed") instanceof Boolean, name + ": two_handed must be a boolean");
                if (a.containsKey("attacks")) {
                    assertTrue(a.get("attacks") instanceof List, name + ": attacks must be a list");
                    List<?> attacks = (List<?>) a.get("attacks");
                    assertFalse(attacks.isEmpty(), name + ": attacks must not be empty (leave it out to inherit the parent's combo)");
                    for (Object o : attacks) {
                        Map<String, Object> at = obj(o);
                        assertTrue(HITBOXES.contains(at.get("hitbox")), name + ": unknown hitbox " + at.get("hitbox"));
                        double mult = num(at, "damage_multiplier", name), angle = num(at, "angle", name), up = num(at, "upswing", name);
                        assertTrue(mult >= 0.1 && mult <= 3.0, name + ": damage_multiplier " + mult + " outside 0.1..3");
                        assertTrue(angle >= 0 && angle <= 360, name + ": angle " + angle + " outside 0..360");
                        assertTrue(up >= 0 && up <= 1, name + ": upswing " + up + " outside 0..1");
                    }
                }
            }
        }
        assertTrue(n >= 100, "expected a file for every melee weapon, found only " + n);
    }

    @Test
    void everyAnimationReferenceResolves() throws IOException {
        Pattern ref = Pattern.compile("\"animation\"\\s*:\\s*\"([a-z0-9_]+):([a-z0-9_/]+)\"");
        List<String> dangling = new ArrayList<>();
        try (Stream<Path> files = Files.list(ATTRS)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                Matcher m = ref.matcher(read(f));
                while (m.find())
                    if (m.group(1).equals("nusmp") && !Files.exists(ANIMS.resolve(m.group(2) + ".json")))
                        dangling.add(f.getFileName() + " -> nusmp:" + m.group(2));
            }
        }
        assertTrue(dangling.isEmpty(), "animation references without a file under assets/nusmp/attack_animations: " + dangling + " -> run `python tools/bettercombat/gen_animations.py`");
    }

    // ---------------------------------------------------------------- the animations
    @Test
    void everyAnimationHasAscendingTicksAndOrderedWindow() throws IOException {
        int n = 0;
        try (Stream<Path> files = Files.list(ANIMS)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json")).sorted()::iterator) {
                n++;
                String name = f.getFileName().toString();
                Map<String, Object> doc = parse(read(f));
                assertTrue(doc.get("name") != null, name + ": missing name");
                Map<String, Object> e = obj(doc.get("emote"));
                int begin = (int) num(e, "beginTick", name), end = (int) num(e, "endTick", name), stop = (int) num(e, "stopTick", name);
                assertTrue(begin <= end && end <= stop, name + ": need beginTick <= endTick <= stopTick, got " + begin + "," + end + "," + stop);
                List<?> moves = (List<?>) e.get("moves");
                assertTrue(moves != null && !moves.isEmpty(), name + ": no moves");
                double last = -1;
                for (Object o : moves) {
                    double tick = num(obj(o), "tick", name);
                    assertTrue(tick > last, name + ": ticks must strictly ascend (" + tick + " after " + last + ")");
                    assertTrue(tick <= end, name + ": a move at tick " + tick + " is after endTick " + end);
                    last = tick;
                }
            }
        }
        assertTrue(n >= 6, "expected the custom animations, found " + n);
    }

    // ---------------------------------------------------------------- helpers: file read and a tiny JSON reader
    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> obj(Object o) { assertTrue(o instanceof Map, "expected a JSON object but got " + o); return (Map<String, Object>) o; }

    private static double num(Map<String, Object> m, String key, String file) {
        assertTrue(m.get(key) instanceof Double, file + ": " + key + " must be a number");
        return (Double) m.get(key);
    }

    private static Map<String, Object> parse(String text) {
        Reader r = new Reader(text);
        Object v = r.value();
        r.ws();
        assertEquals(text.length(), r.i, "trailing characters after the JSON value at " + r.i);
        return obj(v);
    }

    /** Minimal JSON: objects (insertion ordered), arrays, strings with the usual escapes, numbers (as Double), true/false/null. */
    private static final class Reader {
        final String s; int i;
        Reader(String s) { this.s = s; }
        void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        char peek() { ws(); assertTrue(i < s.length(), "unexpected end of JSON"); return s.charAt(i); }
        void expect(char c) { assertEquals(c, peek(), "expected '" + c + "' at " + i); i++; }
        Object value() {
            char c = peek();
            if (c == '{') {
                i++;
                Map<String, Object> m = new LinkedHashMap<>();
                if (peek() == '}') { i++; return m; }
                while (true) {
                    String k = string();
                    expect(':');
                    m.put(k, value());
                    if (peek() == ',') { i++; continue; }
                    expect('}');
                    return m;
                }
            }
            if (c == '[') {
                i++;
                List<Object> l = new ArrayList<>();
                if (peek() == ']') { i++; return l; }
                while (true) {
                    l.add(value());
                    if (peek() == ',') { i++; continue; }
                    expect(']');
                    return l;
                }
            }
            if (c == '"') return string();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            int st = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            assertTrue(i > st, "bad JSON value at " + st);
            return Double.valueOf(s.substring(st, i));
        }
        String string() {
            expect('"');
            StringBuilder b = new StringBuilder();
            while (true) {
                assertTrue(i < s.length(), "unterminated string");
                char c = s.charAt(i++);
                if (c == '"') return b.toString();
                if (c != '\\') { b.append(c); continue; }
                char e = s.charAt(i++);
                switch (e) {
                    case 'n' -> b.append('\n');
                    case 't' -> b.append('\t');
                    case 'r' -> b.append('\r');
                    case 'b' -> b.append('\b');
                    case 'f' -> b.append('\f');
                    case 'u' -> { b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                    default -> b.append(e);
                }
            }
        }
    }
}
