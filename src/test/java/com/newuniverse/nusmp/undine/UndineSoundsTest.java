package com.newuniverse.nusmp.undine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Undine's sound set says the same everywhere: the catalogue ({@link UndineSoundSet}, from which {@link UndineSounds} registers one event per cue), the entries of {@code sounds.json}, the
 * {@code .ogg} files that {@code tools/gen_undine_sounds.py} synthesises, its own cue table and the subtitle text. A sound event without an entry plays nothing, an entry without a file logs an error in
 * the game, a file nothing names is dead weight, a subtitle without a line shows its key. The older entries must all still be there (nothing is removed), and the set is her own: no entry names a vanilla,
 * Tensura or mob sound.
 */
class UndineSoundsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp"), SOUNDS = ASSETS.resolve("sounds");

    private static JsonObject soundsJson() throws IOException { return JsonParser.parseString(Files.readString(ASSETS.resolve("sounds.json"))).getAsJsonObject(); }

    private static Path fileOf(String name) { return SOUNDS.resolve(name.substring(name.indexOf(':') + 1) + ".ogg"); }

    /** Channels, sample rate and seconds from the identification header and the last page of an Ogg Vorbis file. */
    private static double[] ogg(Path file) throws IOException {
        byte[] d = Files.readAllBytes(file);
        assertTrue(d.length > 64 && d[0] == 'O' && d[1] == 'g' && d[2] == 'g' && d[3] == 'S', file + " is not an Ogg file");
        int at = indexOf(d, new byte[]{1, 'v', 'o', 'r', 'b', 'i', 's'});
        assertTrue(at > 0, file + " has no Vorbis identification header");
        ByteBuffer b = ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN);
        int rate = b.getInt(at + 12), channels = d[at + 11] & 0xff;
        int last = lastIndexOf(d, new byte[]{'O', 'g', 'g', 'S'});
        return new double[]{channels, rate, b.getLong(last + 6) / (double) rate};
    }

    private static int indexOf(byte[] d, byte[] pat) {
        outer:
        for (int i = 0; i <= d.length - pat.length; i++) {
            for (int k = 0; k < pat.length; k++) if (d[i + k] != pat[k]) continue outer;
            return i;
        }
        return -1;
    }

    private static int lastIndexOf(byte[] d, byte[] pat) {
        outer:
        for (int i = d.length - pat.length; i >= 0; i--) {
            for (int k = 0; k < pat.length; k++) if (d[i + k] != pat[k]) continue outer;
            return i;
        }
        return -1;
    }

    @Test
    void everyCueHasItsEntryAndEveryFileExistsAndIsAShortMonoOgg() throws IOException {
        JsonObject doc = soundsJson();
        for (UndineSoundSet.Cue c : UndineSoundSet.Cue.values()) {
            JsonElement entry = doc.get(c.event());
            assertNotNull(entry, "no sounds.json entry " + c.event());
            JsonObject e = entry.getAsJsonObject();
            assertEquals(c.subtitleKey(), e.get("subtitle").getAsString(), c.event() + " subtitle");
            var sounds = e.getAsJsonArray("sounds");
            assertEquals(c.variants(), sounds.size(), c.event() + " variants");
            for (int i = 0; i < sounds.size(); i++) {
                JsonObject s = sounds.get(i).getAsJsonObject();
                assertEquals(c.file(i + 1), s.get("name").getAsString());
                assertEquals(c.range(), s.get("attenuation_distance").getAsInt(), c.event() + " range");
                double[] o = ogg(fileOf(s.get("name").getAsString()));
                assertEquals(1, (int) o[0], c.file(i + 1) + " must be mono");
                assertEquals(44100, (int) o[1], c.file(i + 1) + " sample rate");
                assertTrue(o[2] >= 0.3 && o[2] <= 3.05, c.file(i + 1) + " is " + o[2] + " s");
                boolean streamed = s.has("stream") && s.get("stream").getAsBoolean();
                assertEquals(o[2] >= 2.4, streamed, c.file(i + 1) + ": files of 2.4 s and more are streamed, shorter ones are not");
            }
        }
    }

    @Test
    void noUndineEntryAndNoUndineFileIsAnOrphanAndNothingBorrowsAnotherSet() throws IOException {
        JsonObject doc = soundsJson();
        Set<String> events = new HashSet<>(UndineSoundSet.allEventIds()), named = new HashSet<>();
        for (String key : doc.keySet()) {
            if (!key.startsWith("undine.")) continue;
            assertTrue(events.contains(key), "sounds.json entry " + key + " is not in the catalogue");
            for (JsonElement s : doc.getAsJsonObject(key).getAsJsonArray("sounds")) {
                String n = s.getAsJsonObject().get("name").getAsString();
                assertTrue(n.startsWith("nusmp:undine/"), key + " names a sound of another set: " + n);
                named.add(fileOf(n).toString().replace('\\', '/'));
            }
        }
        try (Stream<Path> files = Files.walk(SOUNDS.resolve("undine"))) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".ogg"))::iterator) assertTrue(named.contains(f.toString().replace('\\', '/')), "no entry names " + f);
        }
        assertEquals(UndineSoundSet.allFiles().size(), named.size());
    }

    @Test
    void everySubtitleHasItsLineAndTheOlderEntriesStayAndTheSetStaysModest() throws IOException {
        JsonObject lang = JsonParser.parseString(Files.readString(Path.of("tools/lang/undine.json"))).getAsJsonObject();
        for (UndineSoundSet.Cue c : UndineSoundSet.Cue.values()) {
            String line = lang.has(c.subtitleKey()) ? lang.get(c.subtitleKey()).getAsString() : "";
            assertFalse(line.isBlank(), "no lang line for " + c.subtitleKey());
            assertTrue(line.contains(c.verb()), "the subtitle says what the cue is");
        }
        for (String key : new String[]{"key.nusmp.undine_link", "nusmp.undine.spirit.full", "nusmp.undine.spirit.linked", "nusmp.undine.spirit.unlinked"}) assertTrue(lang.has(key), key);
        JsonObject doc = soundsJson();
        for (String old : new String[]{"zagred_word", "zagred_seal", "zagred_fall", "zagred_shatter", "zagred_heal", "zagred_overwrite", "zagred_phase", "mob.goblin.taunt", "mob.dragon.cast"})
            assertTrue(doc.has(old), "the older entry " + old + " must stay");
        long total = 0;
        try (Stream<Path> walk = Files.walk(SOUNDS.resolve("undine"))) {
            for (Path f : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".ogg"))::iterator) total += Files.size(f);
        }
        assertTrue(total < 2L * 1024 * 1024, "the Undine's sounds take " + total + " bytes, the budget is 2 MB");
    }

    @Test
    void theGeneratorsCueTableIsTheCataloguesOwn() throws IOException {
        String py = Files.readString(Path.of("tools/gen_undine_sounds.py"));
        Matcher m = Pattern.compile("'(\\w+)': \\((\\d+), (\\d+), '([^']+)', (-?[\\d.]+)\\),").matcher(py.substring(py.indexOf("CUES = {"), py.indexOf("STREAM_OVER")));
        int found = 0;
        while (m.find()) {
            UndineSoundSet.Cue c = UndineSoundSet.Cue.byId(m.group(1));
            assertNotNull(c, "the generator makes a cue the catalogue does not know: " + m.group(1));
            assertEquals(c.variants(), Integer.parseInt(m.group(2)), c.id() + " variants");
            assertEquals(c.range(), Integer.parseInt(m.group(3)), c.id() + " range");
            assertEquals(c.verb(), m.group(4), c.id() + " subtitle verb");
            found++;
        }
        assertEquals(UndineSoundSet.Cue.values().length, found, "every cue has a row in the generator");
    }

    @Test
    void theRegistryIsBuiltFromTheCatalogueAndTheSoundsAreServerDrivenAndBudgeted() throws IOException {
        String reg = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/undine/UndineSounds.java"));
        assertTrue(reg.contains("UndineSoundSet.Cue.values()") && reg.contains("createVariableRangeEvent"), "one variable-range event per cue, from the catalogue");
        String audio = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/undine/UndineAudio.java"));
        assertTrue(audio.contains("BUDGET.tryAcquire(") && audio.contains("getPlayerList().broadcast(null, x, y, z, radius(cue)"), "a sound goes through the budget and only to the players within its radius");
        assertTrue(audio.contains("Math.min(UndineRules.FX_RADIUS, cue.range())"), "the radius is never more than the fx radius");
        assertFalse(audio.contains("getPlayerList().getPlayers()"), "never to every player");
        for (UndineSoundSet.Cue c : UndineSoundSet.Cue.values()) assertTrue(c.range() <= 56 && c.volume() <= 1.2f, c.id() + " is not absurdly loud or far");
        assertEquals(1.4f, UndineSoundSet.volume(UndineSoundSet.Cue.CRASH, true), 1e-6, "the full form is louder, never above 1.4");
        assertTrue(UndineSoundSet.formPitch(true) < UndineSoundSet.formPitch(false), "the big form sounds lower");
        assertEquals(1.0f, UndineSoundSet.Cue.CAST.pitch(0.5), 0.05);
    }
}
