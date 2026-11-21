package com.newuniverse.nusmp.dragonballs.voice;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Reads the repo's own files and pins the rules of the voice feature that only the source can show: who names a voice-mod type, what the voice thread may touch, what is bounded, what is registered. */
class VoiceSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path DB = SRC.resolve("dragonballs");
    private static final Path VOICE = DB.resolve("voice");

    private static List<Path> sources(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) { return s.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
    }

    private static String code(Path f) throws IOException {
        return Files.readString(f).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    @Test
    void onlyTheVoicePluginNamesAVoiceModType() throws IOException {
        for (Path f : sources(DB)) {
            boolean names = Files.readString(f).contains("de.maxhenkel");
            assertEquals(f.getFileName().toString().equals("DbVoicePlugin.java"), names, f + (names ? " names a voice-mod type" : " must name them"));
        }
    }

    @Test
    void thePluginIsRegisteredNextToTheSongPluginAndNeverTouchesTheGame() throws IOException {
        List<String> lines = Files.readAllLines(Path.of("src/main/resources/META-INF/services/de.maxhenkel.voicechat.api.VoicechatPlugin")).stream().map(String::strip).filter(s -> !s.isEmpty()).toList();
        assertTrue(lines.contains("com.newuniverse.nusmp.song.voice.SongVoicePlugin"), "the Song plugin stays");
        assertTrue(lines.contains("com.newuniverse.nusmp.dragonballs.voice.DbVoicePlugin"));
        String plugin = Files.readString(VOICE.resolve("DbVoicePlugin.java"));
        assertFalse(plugin.contains("net.minecraft") || plugin.contains("net.neoforged"), "the voice thread never touches the game");
        String c = code(VOICE.resolve("DbVoicePlugin.java"));
        int listen = c.indexOf("DbVoice.listening"), decode = c.indexOf(".decode(");
        assertTrue(listen > 0 && decode > listen, "frames of players nobody listens to are dropped before they are decoded");
        assertTrue(c.contains("whisperOk"), "whispering follows the setting");
        assertTrue(c.contains("close()"), "the decoders are closed");
    }

    @Test
    void theVoiceThreadOnlyCallsTheFourSafeEntryPoints() throws IOException {
        String c = code(VOICE.resolve("DbVoicePlugin.java"));
        Matcher m = Pattern.compile("DbVoice\\.(\\w+)").matcher(c);
        while (m.find()) assertTrue(List.of("listening", "whisperOk", "feed", "endTransmission").contains(m.group(1)), "the plugin calls DbVoice." + m.group(1));
    }

    @Test
    void serverSafeAndBounded() throws IOException {
        for (Path f : sources(VOICE)) {
            String c = code(f);
            assertFalse(c.contains("Minecraft.getInstance") || c.contains("net.minecraft.client") || c.contains("com.newuniverse.nusmp.client"), f.toString());
            assertFalse(c.contains("catch (Throwable"), f + " catches too broadly");
            assertFalse(c.contains("Thread.sleep") || c.contains(".getChunk(") || c.contains("getAllEntities"), f.toString());
            if (!f.getFileName().toString().equals("BoundedEngine.java")) assertFalse(c.contains("new Thread(") || c.contains("Executors."), f + " starts a thread");
        }
        String probe = code(DB.resolve("DbVoiceProbe.java"));
        assertFalse(probe.contains("getEntitiesOfClass") || probe.contains("getChunk") || probe.contains("getEntities("), "the gate probe must not scan the world");
        String eng = code(VOICE.resolve("BoundedEngine.java"));
        assertTrue(eng.contains("ArrayBlockingQueue") && eng.contains("QUEUE_CAP") && eng.contains("Utterance.admit") && eng.contains("STALE_MS"), "the queue is bounded");
        assertEquals(1, eng.split("new Thread\\(", -1).length - 1, "exactly one worker thread");
    }

    @Test
    void theGateJobIsSpreadAndCapped() throws IOException {
        String v = code(VOICE.resolve("DbVoice.java"));
        assertTrue(v.contains("VoiceGate.bucket") && v.contains("maxListeners()"), "staggered once per second, capped");
        assertTrue(v.contains("ELIGIBLE.remove"), "per-player state is dropped on logout");
        assertTrue(v.contains("DbVoiceConfig.enabled()"), "everything sits behind voice.enabled");
        assertTrue(code(VOICE.resolve("SpeechBridge.java")).contains("DbVoiceConfig.enabled()"));
    }

    @Test
    void theChatPathStillOwnsTheRules() throws IOException {
        String chat = Files.readString(DB.resolve("DbChat.java"));
        assertTrue(chat.contains("p.getServer().execute(() -> handle(p, raw));"), "typed chat is unchanged");
        assertTrue(chat.contains("public static void acceptLine") && chat.contains("handle(p, raw);"), "voice and chat share handle()");
        assertTrue(code(VOICE.resolve("SpeechBridge.java")).contains("DbChat.acceptLine"));
    }

    @Test
    void theRelayCommandAndTheConfigAreRegistered() throws IOException {
        String cmd = code(DB.resolve("DbCommands.java"));
        assertTrue(cmd.contains("Commands.literal(\"say\")") && cmd.contains("SpeechBridge.heard") && cmd.contains("hasPermission(2)"), "the relay is an operator command");
        String boot = code(DB.resolve("DbBoot.java"));
        assertTrue(boot.contains("ModConfig.Type.SERVER, DbVoiceConfig.SPEC, \"nusmp-dbvoice-server.toml\""));
        assertTrue(boot.contains("DbVoiceSelfTests.register()"));
        String cfg = Files.readString(VOICE.resolve("DbVoiceConfig.java"));
        for (String k : List.of("\"enabled\"", "\"whisperCounts\"", "\"maxListeners\"", "\"modelPath\"", "\"matchTolerance\"")) assertTrue(cfg.contains(k), k);
        assertTrue(cfg.contains("\"config/nusmp/vosk-model\"") && cfg.contains("b.push(\"voice\")"));
    }

    @Test
    void noVoskClassIsNamedAtCompileTime() throws IOException {
        for (Path f : sources(DB)) assertFalse(code(f).matches("(?s).*\\bimport\\s+org\\.vosk.*"), f + " imports Vosk");
        assertFalse(Files.readString(Path.of("build.gradle")).toLowerCase().contains("vosk"), "no Vosk dependency, no jar of it in ours");
    }

    @Test
    void everyTranslationKeyTheVoiceCodeAsksForExists() throws IOException {
        JsonObject frag = JsonParser.parseString(Files.readString(Path.of("tools/lang/dbvoice.json"))).getAsJsonObject();
        JsonObject en = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/anime_multiverse/lang/en_us.json"))).getAsJsonObject();
        List<String> used = new ArrayList<>();
        List<Path> files = new ArrayList<>(sources(VOICE));
        files.add(DB.resolve("DbCommands.java"));
        Pattern key = Pattern.compile("\"(anime_multiverse\\.dbvoice\\.[a-z_.]+)\"");
        for (Path f : files) {
            Matcher m = key.matcher(code(f));
            while (m.find()) used.add(m.group(1));
        }
        assertFalse(used.isEmpty());
        for (String k : used) {
            assertTrue(frag.has(k), k + " is missing from tools/lang/dbvoice.json");
            assertTrue(en.has(k), k + " is missing from en_us.json (run tools/lang/merge_fragment.py)");
        }
        for (var e : frag.entrySet()) assertEquals(frag.get(e.getKey()), en.get(e.getKey()), "en_us.json differs from the fragment for " + e.getKey());
    }
}
