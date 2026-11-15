package com.newuniverse.nusmp.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the mod must always run on a dedicated server and with many players (CLAUDE.md rule 9). The commonest way to break a server is a common-code file that names a client-only class
 * (net.minecraft.client, blaze3d, the mod's own client package): the dedicated server does not have those classes and fails when the file is loaded. So no file outside {@code client/} may mention one unless
 * it is on {@code docs/server_safe_allow.txt} (the files that are known to be guarded), and {@code Minecraft.getInstance()} is banned outside {@code client/} altogether. A new file has to be guarded and
 * then listed with a reason in the allow-list.
 */
class ServerSafetyTest {
    private static final Path SRC = Paths.get("src/main/java/com/newuniverse/nusmp");
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.");
    private static final Pattern GET_INSTANCE = Pattern.compile("\\bMinecraft\\.getInstance\\(\\)");

    private static Set<String> allowed() throws IOException {
        Set<String> out = new TreeSet<>();
        for (String l : Files.readAllLines(Paths.get("docs/server_safe_allow.txt"), StandardCharsets.UTF_8)) {
            String s = l.strip();
            if (!s.isEmpty() && !s.startsWith("#")) out.add(s);
        }
        return out;
    }

    @Test
    void noNewCommonFileNamesAClientClass() throws IOException {
        Set<String> allow = allowed();
        List<String> bad = new ArrayList<>();
        try (Stream<Path> w = Files.walk(SRC)) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String rel = SRC.relativize(p).toString().replace('\\', '/');
                if (rel.startsWith("client/") || rel.contains("/client/")) continue;
                if (allow.contains(rel)) continue;
                if (CLIENT.matcher(Files.readString(p, StandardCharsets.UTF_8)).find()) bad.add(rel);
            }
        }
        assertTrue(bad.isEmpty(), "common code that names a client-only class (guard it, then list it in docs/server_safe_allow.txt with the reason): " + bad);
    }

    @Test
    void minecraftGetInstanceStaysInTheClientPackage() throws IOException {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> w = Files.walk(SRC)) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String rel = SRC.relativize(p).toString().replace('\\', '/');
                if (rel.startsWith("client/") || rel.contains("/client/")) continue;
                for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                    String t = line.strip();
                    if (t.startsWith("//") || t.startsWith("*")) continue;
                    if (GET_INSTANCE.matcher(line).find()) { bad.add(rel); break; }
                }
            }
        }
        assertTrue(bad.isEmpty(), "Minecraft.getInstance() outside client/ (it does not exist on a server): " + bad);
    }

    @Test
    void guiMoveIsClientOnlyAndCommonCodeNeverNamesIt() throws IOException {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> w = Files.walk(SRC)) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String rel = SRC.relativize(p).toString().replace('\\', '/');
                if (rel.startsWith("client/") || rel.contains("/client/")) continue;
                if (Files.readString(p, StandardCharsets.UTF_8).contains("client.ui.move")) bad.add(rel);
            }
        }
        assertTrue(bad.isEmpty(), "common code that names the client-only GUI Move package: " + bad);
        assertTrue(Files.exists(SRC.resolve("client/ui/move/GuiMove.java")), "GUI Move must live under client/");
        for (String line : Files.readAllLines(SRC.resolve("client/ui/move/GuiMove.java"), StandardCharsets.UTF_8)) {
            String t = line.strip();
            if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) continue;
            assertTrue(!t.contains("PacketDistributor") && !t.contains("sendToServer"), "GUI Move sends nothing to the server: " + t);
        }
    }
}
