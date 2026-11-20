package com.newuniverse.nusmp.arc.lines;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The second voice pass of the Goku and Sinbad bosses (docs/boss_redesign_goku_sinbad.md, "Kokoro voice pass"), checked with no game and no Kokoro: the 117 files are the polished ones (mono Ogg Vorbis, 32 kHz,
 * 0.5 to 12 s), the first-pass files are all still on disk under {@code sounds/voice/<boss>/classic/} (nothing deleted, rule 2) and differ from the new ones, the new profiles are in the profile file next to
 * the old ones, and the polish tool and the measuring tool are in place. {@link BossLinesTest} keeps the registered list, the sound events and the subtitles honest.
 */
class BossVoicePolishTest {
    private static final Path VOICE = Path.of("src/main/resources/assets/anime_multiverse/sounds/voice");

    /** {sampleRate, channels, seconds*1000} from an Ogg Vorbis file: the identification header and the granule position of the last page. */
    static long[] info(byte[] b) {
        int id = -1;
        for (int i = 0; i + 30 < b.length; i++) if (b[i] == 1 && b[i + 1] == 'v' && b[i + 2] == 'o' && b[i + 3] == 'r' && b[i + 4] == 'b' && b[i + 5] == 'i' && b[i + 6] == 's') { id = i; break; }
        assertTrue(id > 0, "a Vorbis identification header");
        int channels = b[id + 11] & 0xFF;
        long rate = (b[id + 12] & 0xFFL) | (b[id + 13] & 0xFFL) << 8 | (b[id + 14] & 0xFFL) << 16 | (b[id + 15] & 0xFFL) << 24;
        int last = -1;
        for (int i = b.length - 27; i >= 0; i--) if (b[i] == 'O' && b[i + 1] == 'g' && b[i + 2] == 'g' && b[i + 3] == 'S') { last = i; break; }
        assertTrue(last >= 0, "an Ogg page");
        long granule = 0;
        for (int k = 7; k >= 0; k--) granule = granule << 8 | (b[last + 6 + k] & 0xFFL);
        return new long[]{rate, channels, granule * 1000 / rate};
    }

    @Test
    void everyLineIsAPolishedMonoOggAndItsFirstPassTakeIsKept() throws Exception {
        List<String> bad = new ArrayList<>();
        int checked = 0, regenerated = 0;
        for (String boss : new String[]{BossLines.GOKU, BossLines.SINBAD}) {
            for (String id : BossLines.ids(boss)) {
                Path now = VOICE.resolve(boss + "/" + id + ".ogg"), old = VOICE.resolve(boss + "/classic/" + id + ".ogg");
                if (!Files.exists(old)) bad.add("the first-pass file is gone: " + old);
                if (!Files.exists(now)) { bad.add("no file " + now); continue; }
                long[] i = info(Files.readAllBytes(now));
                checked++;
                if (i[0] != 32000 || i[1] != 1) bad.add(id + " is " + i[0] + " Hz, " + i[1] + " channel(s)");
                if (i[2] < 500 || i[2] > 12000) bad.add(id + " lasts " + i[2] + " ms");
                if (Files.exists(old) && !java.util.Arrays.equals(Files.readAllBytes(now), Files.readAllBytes(old))) regenerated++;
            }
        }
        assertEquals(117, checked, "52 Goku and 65 Sinbad lines");
        assertTrue(bad.isEmpty(), bad.toString());
        assertEquals(117, regenerated, "every file differs from its first-pass take (the pass was run on all of them)");
    }

    @Test
    void theNewProfilesAreNextToTheOldOnesAndTheToolsAreThere() throws Exception {
        JsonObject p = JsonParser.parseString(Files.readString(Path.of("tools/audio/offline_voice_profiles.json"))).getAsJsonObject();
        for (String k : new String[]{"goku", "sinbad", "goku2", "sinbad2"}) assertTrue(p.has(k), "profile " + k);
        assertTrue(p.getAsJsonObject("goku2").getAsJsonObject("voice").has("am_eric") && p.getAsJsonObject("sinbad2").get("lang").getAsString().equals("en-gb"));
        String tool = Files.readString(Path.of("tools/audio/boss_voice_polish.py"));
        for (String must : new String[]{"PRON = {", "def phonemes(", "def loudnorm_two_pass(", "def room(", "def selftest(", "http"})
            if (must.equals("http")) assertFalse(tool.contains("http"), "the polish tool calls no web service");
            else assertTrue(tool.contains(must), must);
        assertFalse(tool.contains("elevenlabs") || tool.contains("ElevenLabs"), "offline only");
        assertTrue(Files.exists(Path.of("tools/audio/voice_measure.py")));
        assertTrue(Files.exists(Path.of("tools/audio/boss_voice_polish_report.json")), "the before / after numbers are kept");
    }
}
