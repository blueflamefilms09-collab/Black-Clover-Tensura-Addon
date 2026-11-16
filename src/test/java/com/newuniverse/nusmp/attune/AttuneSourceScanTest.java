package com.newuniverse.nusmp.attune;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Rules 9 and 11 for meditation attunement, pinned by reading the source: no client class in common code, the server checks every confirm, no static player, no scans, one server config, the texts exist. */
class AttuneSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");

    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", ""); }

    private static List<Path> common() throws IOException {
        try (Stream<Path> s = Files.list(SRC.resolve("attune"))) { return s.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
    }

    @Test
    void commonCodeTouchesNoClientClass() throws IOException {
        List<Path> files = common();
        assertTrue(files.size() >= 8);
        for (Path p : files) assertFalse(CLIENT.matcher(Files.readString(p)).find(), p + " names a client class");
    }

    @Test
    void theRulesArePure() throws IOException {
        String rules = Files.readString(SRC.resolve("attune/AttuneRules.java"));
        assertFalse(rules.contains("import net.") || rules.contains("import com.mojang"), "AttuneRules must stay free of Minecraft types");
    }

    @Test
    void everyConfirmIsCheckedOnTheServer() throws IOException {
        String net = code(Files.readString(SRC.resolve("attune/AttuneNet.java")));
        assertTrue(net.contains("ctx.player() instanceof ServerPlayer sp"), "the confirm must come from a real server player");
        assertTrue(net.contains("n > max"), "the id count is bounded when the packet is read");
        String svc = code(Files.readString(SRC.resolve("attune/AttuneService.java")));
        for (String must : new String[]{"AttuneConfig.enabled()", "asked(p, now)", "SESSIONS.get(p.getUUID())", "AttuneRules.stillMeditating(", "AttuneRules.validId(", "AttunePowers.owned(p)",
                "AttuneRules.check(", "AttuneRules.cooldownLeft(", "backlash(p)"})
            assertTrue(svc.contains(must), "the confirm lost its check " + must);
        assertTrue(code(Files.readString(SRC.resolve("attune/AttuneRules.java"))).contains("Math.min(BACKLASH_MAX, health - 1.0f)"), "the backlash must leave the player alive");
    }

    @Test
    void noStaticPlayerNoScansAndSessionsAreDropped() throws IOException {
        for (Path p : common()) {
            String c = code(Files.readString(p));
            assertFalse(Pattern.compile("static\\s+(?:volatile\\s+)?(?:Server)?Player\\s+\\w+\\s*[;=]").matcher(c).find(), p + " keeps a static player");
            assertFalse(c.contains("getEntitiesOfClass") || c.contains("getAllEntities") || c.contains("getEntities("), p + " scans entities");
            assertFalse(c.contains("getChunk(") || c.contains("setBlock"), p + " touches the world");
            assertFalse(c.contains("TickEvent"), p + " runs per tick");
        }
        String ev = code(Files.readString(SRC.resolve("attune/AttuneEvents.java")));
        for (String e : new String[]{"PlayerLoggedOutEvent", "PlayerChangedDimensionEvent", "LivingDeathEvent", "ServerStoppingEvent"}) assertTrue(ev.contains(e), "sessions are not dropped on " + e);
    }

    @Test
    void oneServerConfigWithItsOwnFileNameAndTheWiring() throws IOException {
        String boot = Files.readString(SRC.resolve("attune/AttuneBoot.java"));
        assertTrue(boot.contains("ModConfig.Type.SERVER, AttuneConfig.SPEC, \"nusmp-attune-server.toml\""));
        assertTrue(boot.contains("AttuneSelfTests.register()"));
        String mod = Files.readString(SRC.resolve("NUSMP.java"));
        assertTrue(mod.contains("attune.AttuneBoot.init(") && mod.contains("client.attune.AttuneClient.init("));
        assertTrue(Files.readString(Path.of(".github/workflows/smoke.yml")).contains(",attune,"), "the attune. self-tests are not on the smoke list");
        assertTrue(Files.readString(SRC.resolve("client/ui/move/GuiMoveRegistry.java")).contains("\"client/attune/MeditationScreen.java\""));
    }

    @Test
    void everyTextHasItsLangLine() throws IOException {
        String lang = Files.readString(Path.of("src/main/resources/assets/anime_multiverse/lang/en_us.json"));
        String frag = Files.readString(Path.of("tools/lang/attune.json"));
        java.util.regex.Matcher m = Pattern.compile("\"(attune\\.(?:msg|screen)\\.[a-z_]+)\"").matcher(
                Files.readString(SRC.resolve("attune/AttuneService.java")) + Files.readString(SRC.resolve("attune/AttuneEvents.java")) + Files.readString(SRC.resolve("client/attune/MeditationScreen.java")));
        int n = 0;
        while (m.find()) {
            n++;
            assertTrue(lang.contains("\"" + m.group(1) + "\""), "en_us.json lacks " + m.group(1));
            assertTrue(frag.contains("\"" + m.group(1) + "\""), "tools/lang/attune.json lacks " + m.group(1));
        }
        assertTrue(n >= 10);
        for (String r : new String[]{"too_many", "not_owned", "duplicate", "same_system"}) assertTrue(lang.contains("\"attune.msg." + r + "\""), "no text for the refusal " + r);
    }
}
