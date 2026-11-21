package com.newuniverse.nusmp.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: {@code catch (Throwable ...)} also swallows OutOfMemoryError and StackOverflowError, which hides a dying server instead of letting it fail loudly. The 4.80.1 audit and the catch audit
 * (docs/audit_catches.md) narrowed the silent ones to {@code RuntimeException | LinkageError}; this test keeps it that way. A file may hold only as many {@code catch (Throwable} as
 * {@code docs/broad_catch_allow.txt} lists for it (a ceiling, so the count can only go down), and no catch of an Error subclass such as OutOfMemoryError is allowed anywhere.
 * New code catches the narrow types (or the specific checked exception); if a new broad catch really is needed, list the file with a reason.
 */
class BroadCatchScanTest {
    private static final Path SRC = Paths.get("src/main/java/com/newuniverse/nusmp");
    private static final Pattern BROAD = Pattern.compile("catch\\s*\\(\\s*(?:final\\s+)?(?:[\\w.]+\\s*\\|\\s*)*Throwable\\b");
    private static final Pattern ERRORS = Pattern.compile("catch\\s*\\([^)]*\\b(?:OutOfMemoryError|StackOverflowError|VirtualMachineError)\\b");

    /** The source with comment lines dropped (a word in a comment never counts). */
    private static String code(Path p) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            String t = line.strip();
            if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) continue;
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static int count(Pattern pat, String s) {
        Matcher m = pat.matcher(s);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    private static Map<String, Integer> allowed() throws IOException {
        Map<String, Integer> out = new TreeMap<>();
        for (String l : Files.readAllLines(Paths.get("docs/broad_catch_allow.txt"), StandardCharsets.UTF_8)) {
            String s = l.strip();
            if (s.isEmpty() || s.startsWith("#")) continue;
            int hash = s.indexOf('#');
            String entry = (hash < 0 ? s : s.substring(0, hash)).strip();
            assertTrue(hash > 0, "every line of docs/broad_catch_allow.txt needs a reason after '#': " + s);
            int colon = entry.lastIndexOf(':');
            assertTrue(colon > 0, "a line needs <path>:<count>: " + s);
            out.put(entry.substring(0, colon).strip(), Integer.parseInt(entry.substring(colon + 1).strip()));
        }
        return out;
    }

    @Test
    void noNewCatchThrowableOutsideTheAllowList() throws IOException {
        Map<String, Integer> allow = allowed();
        List<String> bad = new ArrayList<>();
        try (Stream<Path> w = Files.walk(SRC)) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String rel = SRC.relativize(p).toString().replace('\\', '/');
                int n = count(BROAD, code(p));
                int max = allow.getOrDefault(rel, 0);
                if (n > max) bad.add(rel + " has " + n + " catch (Throwable, allowed " + max);
            }
        }
        assertTrue(bad.isEmpty(), "catch (Throwable also swallows OutOfMemoryError / StackOverflowError: catch RuntimeException | LinkageError (or the checked exception) instead, or list the file in docs/broad_catch_allow.txt with a reason: " + bad);
    }

    @Test
    void theAllowListHasNoStaleLines() throws IOException {
        List<String> stale = new ArrayList<>();
        for (Map.Entry<String, Integer> e : allowed().entrySet()) {
            Path p = SRC.resolve(e.getKey());
            if (!Files.exists(p)) { stale.add(e.getKey() + " (file is gone)"); continue; }
            if (count(BROAD, code(p)) == 0) stale.add(e.getKey() + " (no catch (Throwable left: remove the line)");
        }
        assertTrue(stale.isEmpty(), "stale lines in docs/broad_catch_allow.txt: " + stale);
    }

    @Test
    void noCatchOfAnErrorSubclassAnywhere() throws IOException {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> w = Files.walk(SRC)) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".java"))::iterator) {
                if (count(ERRORS, code(p)) > 0) bad.add(SRC.relativize(p).toString().replace('\\', '/'));
            }
        }
        assertTrue(bad.isEmpty(), "never catch OutOfMemoryError / StackOverflowError / VirtualMachineError: " + bad);
    }
}
