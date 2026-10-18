package com.newuniverse.nusmp.elite.races;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.races.data.RaceTemplates;
import com.newuniverse.nusmp.elite.races.race.RaceText;
import com.newuniverse.nusmp.elite.races.rule.KillKeys;
import com.newuniverse.nusmp.elite.races.state.EntryCondition.Reason;
import com.newuniverse.nusmp.elite.races.state.RaceEnglish;
import com.newuniverse.nusmp.elite.races.state.TransformMachine;
import com.newuniverse.nusmp.elite.races.state.TransformState;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the owner's rule is that the lang file is merged by the lead from a fragment ({@code tools/elite/races_lang.json}), while the code carries the same English as a fallback.
 * If the two drift, the merge would silently change the wording, and a key missing from the fragment would show raw in a game that has no fallback. This test keeps them equal:
 * every state, reason, end, race name and description, the hotkey, and every {@code RaceMessages.msg} key used in the source is in the fragment, and where the source gives a plain
 * English literal it is the fragment's text. It reads the real files (the test's working directory is the project root).
 */
class RaceLangFragmentTest {
    private static JsonObject lang() throws IOException {
        return JsonParser.parseString(Files.readString(Path.of("tools/elite/races_lang.json"), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static String get(JsonObject o, String key) {
        assertTrue(o.has(key), "tools/elite/races_lang.json lacks '" + key + "'");
        return o.get(key).getAsString();
    }

    @Test
    void statesReasonsEndsAndRacesAreAllThere() throws IOException {
        JsonObject l = lang();
        for (TransformState s : TransformState.values()) assertEquals(RaceEnglish.name(s), get(l, "nusmp.elite.race.state." + s.id));
        for (Reason r : Reason.values()) assertEquals(RaceEnglish.why(r), get(l, "nusmp.elite.race.why." + r.name().toLowerCase(Locale.ROOT)));
        for (TransformMachine.Event e : TransformMachine.Event.values()) {
            if (e == TransformMachine.Event.NONE) continue;
            assertEquals(RaceEnglish.end(e), get(l, "nusmp.elite.race.end." + e.name().toLowerCase(Locale.ROOT)));
        }
        List<String> problems = RaceTemplates.loadBundled();
        assertTrue(problems.isEmpty(), problems.toString());
        for (String id : RaceTemplates.all().keySet()) {
            assertEquals(RaceText.title(id), get(l, "nusmp.race." + id), "the name fallback of " + id + " must equal the fragment");
            assertTrue(get(l, "nusmp.race." + id + ".description").length() > 60, id + " needs a real description");
        }
        assertTrue(get(l, "key.nusmp.elite_race_transform").length() > 5);
    }

    @Test
    void everyMessageKeyUsedInTheSourceIsInTheFragmentAndPlainLiteralsAgree() throws IOException {
        JsonObject l = lang();
        Pattern key = Pattern.compile("(?:RaceMessages\\.msg|[^A-Za-z.]fail)\\(\"([a-z.]+)\"");
        Pattern literal = Pattern.compile("(?:RaceMessages\\.msg|[^A-Za-z.]fail)\\(\"([a-z.]+)\",\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        int seen = 0;
        try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/newuniverse/nusmp/elite/races"))) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                String src = Files.readString(f, StandardCharsets.UTF_8);
                Matcher km = key.matcher(src);
                while (km.find()) { get(l, "nusmp.elite.race.msg." + km.group(1)); seen++; }
                Matcher lm = literal.matcher(src);
                while (lm.find()) {
                    String english = lm.group(2).replace("\\\"", "\"").replace("\\\\", "\\");
                    assertEquals(english, get(l, "nusmp.elite.race.msg." + lm.group(1)), "the source's English for '" + lm.group(1) + "' (" + f.getFileName() + ") differs from the fragment");
                }
            }
        }
        assertTrue(seen >= 10, "the scan found only " + seen + " message keys; the pattern is out of date");
    }

    @Test
    void killKeysFeedTheRightCounters() {
        assertEquals(List.of("player", "any"), KillKeys.classify(true, false, false, 20, 10));
        assertEquals(List.of("boss", "any", "hostile"), KillKeys.classify(false, true, true, 300, 10));
        assertEquals(List.of("any", "hostile"), KillKeys.classify(false, true, false, 20, 10));
        assertEquals(List.of("any"), KillKeys.classify(false, false, false, 20, 10));
        assertEquals(List.of(), KillKeys.classify(false, true, false, 4, 10), "a chicken-sized creature counts for nothing");
        assertEquals(List.of("boss", "any", "hostile"), KillKeys.classify(false, false, true, 1, 10), "a boss always counts");
    }
}
