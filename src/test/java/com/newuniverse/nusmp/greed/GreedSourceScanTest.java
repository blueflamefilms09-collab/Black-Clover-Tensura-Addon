package com.newuniverse.nusmp.greed;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Rules 9 and 11 for the Black Sword Greed, pinned by reading the source: no client class in common code, no static player, bounded scans, one server config, every form has its asset files. */
class GreedSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path RES = Path.of("src/main/resources/assets/anime_multiverse");
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");

    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", ""); }

    private static List<Path> common() throws IOException {
        try (Stream<Path> s = Files.list(SRC.resolve("greed"))) { return s.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
    }

    @Test
    void commonCodeTouchesNoClientClass() throws IOException {
        List<Path> files = common();
        assertFalse(files.isEmpty());
        for (Path p : files) assertFalse(CLIENT.matcher(code(Files.readString(p))).find(), p + " uses a client class");
    }

    @Test
    void noStaticCurrentPlayerAndNoWorldScansInTicks() throws IOException {
        for (Path p : common()) {
            String c = code(Files.readString(p));
            assertFalse(Pattern.compile("static\\s+(?:volatile\\s+)?(?:Server)?Player\\s+\\w+\\s*[;=]").matcher(c).find(), p + " keeps a static player");
            assertFalse(c.contains("getAllEntities") || c.contains("getEntities().getAll"), p + " scans every entity");
            assertFalse(c.contains("getChunk(") || c.contains("loadChunk"), p + " touches chunks");
        }
    }

    @Test
    void oneServerConfigWithItsOwnFileName() throws IOException {
        String c = Files.readString(SRC.resolve("greed/GreedBoot.java"));
        assertTrue(c.contains("nusmp-greed-server.toml"));
        assertTrue(c.contains("ModConfig.Type.SERVER"));
    }

    @Test
    void everyFormHasItsTextureMeshAndName() throws IOException {
        String lang = Files.readString(RES.resolve("lang/en_us.json"));
        for (GreedRules.Form f : GreedRules.Form.values()) {
            assertTrue(lang.contains("\"item.anime_multiverse." + f.itemId() + "\""), "no name for " + f.itemId());
            assertTrue(lang.contains("\"" + f.langKey() + "\""), "no form name " + f.langKey());
            assertTrue(lang.contains("\"" + f.techniqueKey() + "\""), "no technique name " + f.techniqueKey());
        }
        assertTrue(Files.exists(RES.resolve("textures/skill/lord_of_gluttonous_greed.png")));
    }
}
