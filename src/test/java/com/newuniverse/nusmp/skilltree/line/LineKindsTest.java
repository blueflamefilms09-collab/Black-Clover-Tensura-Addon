package com.newuniverse.nusmp.skilltree.line;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the contract between the generator and the engine is the effect catalogue, and the owner's rule is that a kind without an executor, or an executor without a kind, FAILS THE BUILD: a kind that nobody
 * runs would be a purchasable node that does nothing, and an executor nobody can reach is dead code that still looks like a feature. The catalogue is {@code tools/lines/catalog.json}; the executors are
 * registered by {@code reg("kind", ...)} lines in {@code ActiveExecutors} and {@code PassiveExecutors} and by {@code LineAttrs.KIND}. This test reads the catalogue and those sources (it needs no Minecraft, so
 * it runs in every environment) and compares them in both directions, then goes further: it checks that every number and enum an executor reads ({@code c.num("range")}) is a parameter of THAT kind in the
 * catalogue, so a misspelt name (which would silently read the catalogue's base value forever) is caught, and that the words the engine turns into game objects (effects, attributes, conditions, categories,
 * damage kinds, effect shapes) are exactly the words of the catalogue. A second test, when Minecraft is on the class path (the real build), also compares the live registries.
 */
class LineKindsTest {
    private static final Path JAVA = Path.of("src/main/java/com/newuniverse/nusmp/skilltree/line");

    private static String source(String name) throws IOException { return Files.readString(JAVA.resolve(name), StandardCharsets.UTF_8); }

    private static Set<String> regs(String src) {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = Pattern.compile("reg\\(\"([a-z_]+)\"").matcher(src);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    private static void sameSet(String what, Set<String> catalogue, Set<String> engine) {
        Set<String> missing = new TreeSet<>(catalogue);
        missing.removeAll(engine);
        Set<String> extra = new TreeSet<>(engine);
        extra.removeAll(catalogue);
        assertTrue(missing.isEmpty(), what + ": kinds of the catalogue with NO executor: " + missing);
        assertTrue(extra.isEmpty(), what + ": executors with NO kind in the catalogue: " + extra);
    }

    @Test
    void everyCatalogueKindHasAnExecutorAndEveryExecutorAKind() throws IOException {
        LineCatalog cat = LineCatalog.get();
        assertTrue(cat.loaded());
        Set<String> active = new LinkedHashSet<>(cat.activeKinds());
        active.addAll(cat.stanceKinds());
        sameSet("active (arts, finishers and the stance)", active, regs(source("ActiveExecutors.java")));
        sameSet("passive", new LinkedHashSet<>(cat.passiveKinds()), regs(source("PassiveExecutors.java")));
        Matcher m = Pattern.compile("static final String KIND = \"([a-z_]+)\"").matcher(source("LineAttrs.java"));
        Set<String> attr = new LinkedHashSet<>();
        while (m.find()) attr.add(m.group(1));
        sameSet("attr", new LinkedHashSet<>(cat.attrKinds()), attr);
        assertEquals(26 + 1, active.size(), "26 arts and finishers plus the stance");
        assertEquals(25, cat.passiveKinds().size());
    }

    /** The text of the method (or lambda argument) body that starts at {@code from}: up to the matching closing brace. */
    private static String body(String src, int from) {
        int open = src.indexOf('{', from);
        int depth = 0;
        for (int i = open; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return src.substring(open, i + 1);
        }
        return src.substring(open);
    }

    private static Set<String> literals(String text) {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = Pattern.compile("\\b(?:num|whole|str)\\(\"([a-z_0-9]+)\"").matcher(text);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    private static Set<String> names(LineCatalog.Kind k) {
        Set<String> s = new LinkedHashSet<>(k.nums().keySet());
        s.addAll(k.enums().keySet());
        return s;
    }

    @Test
    void everyNumberAndEnumAnArtReadsIsAParameterOfItsOwnKind() throws IOException {
        LineCatalog cat = LineCatalog.get();
        String src = source("ActiveExecutors.java");
        Matcher m = Pattern.compile("reg\\(\"([a-z_]+)\"(?:, c -> ([^\\n]*?))?, ActiveExecutors::(\\w+)\\);").matcher(src);
        int checked = 0;
        List<String> bad = new ArrayList<>();
        while (m.find()) {
            String kind = m.group(1);
            LineCatalog.Kind k = cat.anyKind(kind);
            assertTrue(k != null, kind);
            int at = src.indexOf("private static void " + m.group(3) + "(");
            assertTrue(at > 0, "method " + m.group(3) + " of " + kind);
            Set<String> used = literals(body(src, at));
            if (m.group(2) != null) used.addAll(literals(m.group(2)));
            for (String u : used) if (!names(k).contains(u)) bad.add(kind + " reads '" + u + "', which is not a parameter of the kind (it has " + names(k) + ")");
            checked++;
        }
        assertEquals(27, checked, "all 26 arts and the stance were found and checked");
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    @Test
    void everyNumberAndEnumAPassiveReadsIsAParameterOfItsOwnKind() throws IOException {
        LineCatalog cat = LineCatalog.get();
        String src = source("PassiveExecutors.java");
        int start = src.indexOf("static {");
        List<int[]> spans = new ArrayList<>();
        Matcher m = Pattern.compile("reg\\(\"([a-z_]+)\"").matcher(src);
        List<String> kinds = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        while (m.find()) if (m.start() > start) {
            kinds.add(m.group(1));
            starts.add(m.start());
        }
        assertEquals(25, kinds.size());
        List<String> bad = new ArrayList<>();
        for (int i = 0; i < kinds.size(); i++) {
            int end = i + 1 < starts.size() ? starts.get(i + 1) : src.indexOf("    /** The hooks of a kind", starts.get(i));
            String text = src.substring(starts.get(i), end);
            // a long kind (doubled, aura) is a one-line reference to a method further down: add that method's body
            Matcher ref = Pattern.compile("(doubled|aura)\\(c").matcher(text);
            if (ref.find()) {
                int at = src.indexOf("private static void " + ref.group(1) + "(");
                text += body(src, at);
            }
            LineCatalog.Kind k = cat.kind(LineCatalog.Group.PASSIVE, kinds.get(i));
            for (String u : literals(text)) if (!names(k).contains(u)) bad.add(kinds.get(i) + " reads '" + u + "', which is not a parameter of the kind (it has " + names(k) + ")");
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    private static Set<String> mapKeys(String src, String startMarker, String endMarker, String regex) {
        int a = src.indexOf(startMarker), b = src.indexOf(endMarker, a);
        assertTrue(a >= 0 && b > a, startMarker);
        Set<String> out = new TreeSet<>();
        Matcher m = Pattern.compile(regex).matcher(src.substring(a, b));
        while (m.find()) out.add(m.group(1));
        return out;
    }

    private static Set<String> enumValues(LineCatalog cat, String kind, String param) {
        return new TreeSet<>(cat.anyKind(kind).enums().get(param));
    }

    @Test
    void theWordsTheEngineTurnsIntoGameObjectsAreExactlyTheCatalogues() throws IOException {
        LineCatalog cat = LineCatalog.get();
        String src = source("LineLookups.java");
        Set<String> effects = enumValues(cat, "buff_self", "effect");
        assertEquals(effects, mapKeys(src, "EFFECTS = Map.ofEntries(", ");", "Map\\.entry\\(\"([a-z_]+)\""), "effect words");
        assertEquals(enumValues(cat, "attr", "attribute"), mapKeys(src, "ATTRIBUTES = Map.ofEntries(", ");", "Map\\.entry\\(\"([a-z_]+)\""), "attribute words");
        assertEquals(enumValues(cat, "self_effect_when", "condition"), mapKeys(src, "public static boolean condition(", "private static boolean isNight", "case \"([a-z_]+)\""), "condition words");
        assertEquals(enumValues(cat, "dmg_vs", "category"), mapKeys(src, "public static boolean category(", "/** Is {@code src} damage", "case \"([a-z_]+)\""), "category words");
        assertEquals(enumValues(cat, "resist", "damage_kind"), mapKeys(src, "public static boolean damageKind(", "// ----", "case \"([a-z_]+)\""), "damage kind words");
        String shapes = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/vfx/VfxShape.java"), StandardCharsets.UTF_8);
        for (String v : cat.vfx()) assertTrue(Pattern.compile("\\b" + v + "\\b").matcher(shapes).find(), "the effect shape " + v + " of the catalogue does not exist in VfxShape");
        assertEquals(new TreeSet<>(cat.vfx()), enumValues(cat, "cone_strike", "vfx"), "the vfx list of the catalogue and of the arts agree");
        assertEquals(effects.size() + 1, enumValues(cat, "buff_self", "effect2").size(), "effect2 is the effect words plus none");
    }

    @SuppressWarnings("unchecked")
    private static Set<String> live(String className, String method) throws Exception {
        return new TreeSet<>((Set<String>) Class.forName(className).getMethod(method).invoke(null));
    }

    @Test
    void theLiveRegistriesAgreeWhenMinecraftIsOnTheClassPath() throws Exception {
        try {
            Class.forName("net.minecraft.world.entity.LivingEntity", false, LineKindsTest.class.getClassLoader());     // do not initialise it: that needs a bootstrapped game
        } catch (ClassNotFoundException notThere) {
            System.out.println("[kinds] live registry check skipped: Minecraft is not on the class path; the source check above covers it");
            return;
        }
        try {
            liveCheck();
        } catch (java.lang.reflect.InvocationTargetException wrapped) {
            if (!(wrapped.getCause() instanceof ExceptionInInitializerError || wrapped.getCause() instanceof NoClassDefFoundError)) throw wrapped;
            System.out.println("[kinds] live registry check skipped: Minecraft is not bootstrapped in a unit test; the in-game self-test tree.lines.kinds covers it");
        } catch (ExceptionInInitializerError | NoClassDefFoundError notBootstrapped) {
            // 1.82.0: Gradle's plain unit-test JVM has Minecraft on the class path but no bootstrapped registries (MobEffects, Attributes...); the same agreement is checked in game by the self-test tree.lines.kinds
            System.out.println("[kinds] live registry check skipped: Minecraft is not bootstrapped in a unit test (" + notBootstrapped.getClass().getSimpleName() + "); the in-game self-test tree.lines.kinds covers it");
        }
    }

    private void liveCheck() throws Exception {
        String p = "com.newuniverse.nusmp.skilltree.line.";
        LineCatalog cat = LineCatalog.get();
        Set<String> active = new TreeSet<>(cat.activeKinds());
        active.addAll(cat.stanceKinds());
        assertEquals(active, live(p + "ActiveExecutors", "kinds"), "active kinds with an executor");
        assertEquals(new TreeSet<>(cat.passiveKinds()), live(p + "PassiveExecutors", "kinds"), "passive kinds with an executor");
        assertEquals(new TreeSet<>(cat.attrKinds()), live(p + "LineAttrs", "kinds"), "attr kinds with an executor");
        assertEquals(new TreeSet<>(enumValues(cat, "buff_self", "effect")), live(p + "LineLookups", "effectWords"), "effect words");
        assertEquals(new TreeSet<>(enumValues(cat, "attr", "attribute")), live(p + "LineLookups", "attributeWords"), "attribute words");
    }
}
