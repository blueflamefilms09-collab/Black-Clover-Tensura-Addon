package com.newuniverse.nusmp.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: NeoForge keeps one config per file name. Two specs registered under the same name collide (the loader reports a "config file conflict" and can leave later configs unloaded, which
 * is how another mod's config can read as "not loaded"). 1.102.0 registered the visual-upgrade settings of the Genesis weapons and of the God weapons under one name and nothing
 * caught it, because the CI server smoke test never runs the client-only registrations. So every registerConfig call of the mod must use a file name no other call uses, and at most one call
 * per type may omit the name (that one gets the default name).
 */
class ConfigFileNamesTest {
    private static final Path SRC = Paths.get("src/main/java");
    private static final Pattern CALL = Pattern.compile("registerConfig\\(\\s*(?:net\\.neoforged\\.fml\\.config\\.)?ModConfig\\.Type\\.(\\w+)\\s*,\\s*([^;]*?)\\)\\s*;");
    private static final Pattern NAME = Pattern.compile("\"([^\"]+\\.toml)\"\\s*$");

    @Test
    void everyRegisteredConfigFileNameIsUnique() throws IOException {
        Map<String, List<String>> names = new HashMap<>();
        Map<String, Integer> unnamed = new HashMap<>();
        try (Stream<Path> w = Files.walk(SRC)) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".java"))::iterator) {
                StringBuilder code = new StringBuilder();
                for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                    String t = line.strip();
                    if (t.startsWith("*") || t.startsWith("//") || t.startsWith("/*")) continue;
                    int c = line.indexOf("//");
                    code.append(c >= 0 ? line.substring(0, c) : line).append('\n');
                }
                Matcher m = CALL.matcher(code);
                while (m.find()) {
                    String args = m.group(2).replaceAll("\\s+", " ").strip();
                    Matcher n = NAME.matcher(args);
                    if (n.find()) names.computeIfAbsent(n.group(1), k -> new ArrayList<>()).add(p.getFileName().toString());
                    else unnamed.merge(m.group(1), 1, Integer::sum);
                }
            }
        }
        List<String> bad = new ArrayList<>();
        names.forEach((k, v) -> { if (v.size() > 1) bad.add(k + " in " + v); });
        unnamed.forEach((k, v) -> { if (v > 1) bad.add(v + " " + k + " configs without a file name"); });
        assertTrue(bad.isEmpty(), "config file names registered more than once: " + bad);
    }

    @Test
    void theGuiLayoutFileIsRegisteredOnceAsAClientConfig() throws IOException {
        int count = 0;
        try (Stream<Path> w = Files.walk(SRC)) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String text = Files.readString(p, StandardCharsets.UTF_8);
                int at = 0;
                while ((at = text.indexOf("\"nusmp-gui-layout-client.toml\"", at)) >= 0) {
                    count++;
                    String line = text.substring(text.lastIndexOf('\n', at) + 1, text.indexOf('\n', at));
                    if (!line.strip().startsWith("*") && !line.strip().startsWith("//")) assertTrue(line.contains("ModConfig.Type.CLIENT"), "the GUI layout (where a GUI sits) is a looks setting: CLIENT only, in " + p.getFileName());
                    at++;
                }
            }
        }
        assertTrue(count >= 1, "the GUI Move layout file is not registered");
    }
}
