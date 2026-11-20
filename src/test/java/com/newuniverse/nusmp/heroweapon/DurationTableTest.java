package com.newuniverse.nusmp.heroweapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Why: docs/heroweapon/durations.md is the owner's list of every duration of the hero and vassal techniques, what was lengthened and what was left. A new
 * duration constant that is not in the table, a value that drifted from the table, or a value under 40 ticks without a status would put the doc out of step with the
 * code, so each of those fails the build.
 */
class DurationTableTest {
    private static final Path WEAPONS = Path.of("src/main/java/com/newuniverse/nusmp/heroweapon/weapon");
    private static final Path DOC = Path.of("docs/heroweapon/durations.md");
    private static final Set<String> STATUS = Set.of("RAISED", "LEFT", "BOW");
    private static final Pattern NAME = Pattern.compile("_(TICKS|SECONDS|LIFE|ROOT|SLOW|GLOW|STAGGER|SLOW_FALL|FADE|BASE)$");
    private static final Pattern DECL = Pattern.compile("static\\s+final\\s+int\\s+([^;]*);", Pattern.DOTALL);
    private static final Pattern ITEM = Pattern.compile("\\b([A-Z][A-Z0-9_]*)\\s*=\\s*(\\d+)\\s*(?:,|$)");
    private static final Pattern ROW = Pattern.compile("^\\|\\s*\\w+\\s*\\|\\s*`([A-Za-z]+\\.[A-Z0-9_]+)`\\s*\\|[^|]*\\|\\s*(\\d+)\\s*\\|\\s*(\\d+)\\s*\\|\\s*([A-Z]+)\\s*\\|");

    /** Class.NAME to the value of every duration constant of the weapon packages. */
    private static Map<String, Integer> constants() throws IOException {
        Map<String, Integer> out = new LinkedHashMap<>();
        try (Stream<Path> files = Files.walk(WEAPONS)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.getFileName().toString().matches(".*(Numbers|PoiseMath)\\.java")).sorted()::iterator) {
                String cls = f.getFileName().toString().replace(".java", "");
                Matcher d = DECL.matcher(Files.readString(f, StandardCharsets.UTF_8));
                while (d.find()) {
                    String body = d.group(1).replaceAll("//[^\n]*", "");
                    Matcher i = ITEM.matcher(body);
                    while (i.find()) if (NAME.matcher(i.group(1)).find()) out.put(cls + "." + i.group(1), Integer.parseInt(i.group(2)));
                }
            }
        }
        return out;
    }

    private static Map<String, int[]> table() throws IOException {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (String line : Files.readAllLines(DOC, StandardCharsets.UTF_8)) {
            Matcher m = ROW.matcher(line);
            if (!m.find()) continue;
            String status = m.group(4);
            assertTrue(STATUS.contains(status), "unknown status " + status + " in: " + line);
            out.put(m.group(1), new int[] {Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), status.hashCode()});
        }
        return out;
    }

    @Test
    void everyDurationConstantIsInTheTable() throws IOException {
        Map<String, Integer> consts = constants();
        Map<String, int[]> table = table();
        assertTrue(consts.size() > 100, "the scan found only " + consts.size() + " constants");
        List<String> missing = new ArrayList<>();
        for (String k : consts.keySet()) if (!table.containsKey(k)) missing.add(k);
        assertTrue(missing.isEmpty(), "not in docs/heroweapon/durations.md: " + missing);
    }

    @Test
    void theTableMatchesTheCode() throws IOException {
        Map<String, Integer> consts = constants();
        for (Map.Entry<String, int[]> e : table().entrySet()) {
            Integer v = consts.get(e.getKey());
            assertTrue(v != null, "the table names a constant that no longer exists: " + e.getKey());
            assertEquals(v.intValue(), e.getValue()[0], "old value of " + e.getKey() + " in the table");
        }
    }

    @Test
    void shortValuesAreListedWithAStatus() throws IOException {
        Map<String, int[]> table = table();
        for (Map.Entry<String, Integer> e : constants().entrySet()) {
            if (e.getValue() >= 40) continue;
            assertTrue(table.containsKey(e.getKey()), e.getKey() + " is under 40 ticks and must be listed as RAISED, LEFT or BOW");
        }
    }

    @Test
    void raisedValuesAreNeverShorterAndLeftValuesAreUnchanged() throws IOException {
        for (String line : Files.readAllLines(DOC, StandardCharsets.UTF_8)) {
            Matcher m = ROW.matcher(line);
            if (!m.find()) continue;
            int old = Integer.parseInt(m.group(2)), now = Integer.parseInt(m.group(3));
            if (m.group(4).equals("RAISED")) assertTrue(now >= old, line);
            else assertEquals(old, now, line);
        }
    }
}
