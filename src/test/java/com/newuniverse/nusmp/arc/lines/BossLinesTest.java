package com.newuniverse.nusmp.arc.lines;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the own voice lines of the Goku and Sinbad bosses (docs/boss_redesign_goku_sinbad.md): the manifests {@code tools/audio/<boss>_lines.json} and the generated Java data say the same, every moment the
 * boss needs has lines, every line has its subtitle, the registered list is exactly the lines that have a file (and a sound event), and the two bosses really call {@link BossSpeech}.
 */
class BossLinesTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/anime_multiverse");
    private static final Pattern ENTRY = Pattern.compile("\"id\":\\s*\"([^\"]+)\",\\s*\"group\":\\s*\"([^\"]+)\",\\s*\"text\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\",\\s*\"delivery\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\",\\s*\"source\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private static String read(String p) throws IOException { return Files.readString(Path.of(p), StandardCharsets.UTF_8); }

    private static List<String[]> manifest(String boss) throws IOException {
        Matcher m = ENTRY.matcher(read("tools/audio/" + boss + "_lines.json"));
        List<String[]> out = new ArrayList<>();
        while (m.find()) out.add(new String[]{m.group(1), m.group(2), m.group(3), m.group(4), m.group(5)});
        return out;
    }

    @Test
    void manifestsMatchTheJavaData() throws IOException {
        for (String boss : new String[]{BossLines.GOKU, BossLines.SINBAD}) {
            List<String[]> rows = manifest(boss);
            assertFalse(rows.isEmpty(), boss + " manifest is empty");
            assertEquals(BossLines.ids(boss).size(), rows.size(), boss + ": line count differs, run tools/audio/boss_lines_gen.py");
            for (int i = 0; i < rows.size(); i++) {
                String[] r = rows.get(i);
                assertEquals(BossLines.ids(boss).get(i), r[0]);
                assertEquals(BossLines.groupOf(r[0]), r[1]);
                assertEquals(BossLines.text(r[0]), r[2].replace("\\\"", "\""));
                assertFalse(r[3].isBlank(), r[0] + " has no delivery note");
                assertFalse(r[4].isBlank(), r[0] + " has no source note");
            }
        }
    }

    @Test
    void idsAreUniqueOwnAndNotTheSharedBossLines() {
        Set<String> seen = new HashSet<>();
        for (String boss : new String[]{BossLines.GOKU, BossLines.SINBAD}) {
            for (String id : BossLines.ids(boss)) {
                assertTrue(seen.add(id), "duplicate id " + id);
                assertTrue(id.startsWith(boss + "_"), id);
                assertEquals(boss, BossLines.bossOf(id));
                assertFalse(BossLines.text(id).isBlank());
                assertTrue(BossLines.text(id).length() <= 80, "a line should be short: " + id);
            }
        }
        assertTrue(seen.stream().noneMatch(i -> i.startsWith("boss_")), "own lines, not the shared boss_<family>_<event> ones");
    }

    @Test
    void everyMomentHasLines() {
        List<String> common = List.of("intro", "hurt_big", "low_hp", "defeat", "victory", "player_death", "taunt");
        for (String boss : new String[]{BossLines.GOKU, BossLines.SINBAD}) for (String g : common) assertTrue(BossLines.hasGroup(boss, g), boss + " has no " + g);
        for (String g : List.of("form_ssj", "form_ssj2", "form_ssj3", "form_god", "form_blue", "form_kaioken", "form_ui_sign", "form_mui", "form_tui", "dodge", "spirit_bomb_broken"))
            assertTrue(BossLines.hasGroup(BossLines.GOKU, g), "goku has no " + g);
        for (String d : List.of("baal", "valefor", "zepar", "focalor", "vepar", "crocell", "furfur")) assertTrue(BossLines.hasGroup(BossLines.SINBAD, "equip_" + d), "sinbad has no equip_" + d);
        for (String g : List.of("unequip", "gates", "foresight")) assertTrue(BossLines.hasGroup(BossLines.SINBAD, g), "sinbad has no " + g);
    }

    @Test
    void everyCalloutNamesARealGroupAndARealMove() throws IOException {
        String gokuSrc = read("src/main/java/com/newuniverse/nusmp/arc/goku/GokuMoves.java") + read("src/main/java/com/newuniverse/nusmp/arc/goku/GokuArts.java");
        String sinbadSrc = read("src/main/java/com/newuniverse/nusmp/arc/magi/SinbadTable.java");
        for (String id : BossLines.calloutMoves(BossLines.GOKU)) {
            assertTrue(BossLines.hasGroup(BossLines.GOKU, BossLines.calloutGroup(BossLines.GOKU, id)), id);
            assertTrue(gokuSrc.contains("\"" + id + "\""), "goku has no move " + id);
        }
        for (String id : BossLines.calloutMoves(BossLines.SINBAD)) {
            assertTrue(BossLines.hasGroup(BossLines.SINBAD, BossLines.calloutGroup(BossLines.SINBAD, id)), id);
            assertTrue(sinbadSrc.contains("\"" + id + "\""), "sinbad has no move " + id);
        }
        assertNull(BossLines.calloutGroup(BossLines.GOKU, "no_such_move"));
    }

    @Test
    void everyLineHasASubtitle() throws IOException {
        String lang = read("src/main/resources/assets/anime_multiverse/lang/en_us.json");
        for (String boss : new String[]{BossLines.GOKU, BossLines.SINBAD})
            for (String id : BossLines.ids(boss)) assertTrue(lang.contains("\"" + BossLines.subtitleKey(id) + "\""), "no subtitle for " + id);
    }

    @Test
    void registeredIsExactlyTheLinesWithAFile() throws IOException {
        String sounds = read("src/main/resources/assets/anime_multiverse/sounds.json");
        Set<String> have = new HashSet<>();
        for (String boss : new String[]{BossLines.GOKU, BossLines.SINBAD}) {
            for (String id : BossLines.ids(boss)) {
                boolean file = Files.exists(ASSETS.resolve("sounds/voice/" + boss + "/" + id + ".ogg"));
                assertEquals(file, BossLines.isRegistered(id), id + ": registered must equal 'has a file'");
                if (file) {
                    have.add(id);
                    assertTrue(sounds.contains("\"voice." + id + "\""), id + " has a file but no sound event");
                }
            }
        }
        assertEquals(have.size(), BossLines.registeredIds().size());
    }

    @Test
    void theBossesSpeakThroughBossSpeechAndKeepCommonCodeClean() throws IOException {
        for (String f : new String[]{"src/main/java/com/newuniverse/nusmp/arc/goku/GokuBoss.java", "src/main/java/com/newuniverse/nusmp/arc/magi/SinbadBoss.java"}) {
            String s = read(f);
            assertTrue(s.contains("BossSpeech.callout("), f + " does not call out its moves");
            assertTrue(s.contains("BossSpeech.text("), f + " does not use its own opening and death lines");
            assertTrue(s.contains("BossSpeech.watchDeaths("), f + " does not speak when a player falls");
            assertTrue(s.contains("BossSpeech.forget("), f + " does not drop its speech state");
        }
        for (String f : new String[]{"BossSpeech", "BossLines", "BossLineGate", "BossLinesData"}) {
            String s = read("src/main/java/com/newuniverse/nusmp/arc/lines/" + f + ".java");
            assertFalse(s.contains("net.minecraft.client") || s.contains("Minecraft.getInstance") || s.contains("com.mojang.blaze3d") || s.contains("nusmp.client"), f + " touches client code");
        }
        for (String f : new String[]{"BossLines", "BossLineGate", "BossLinesData"}) assertFalse(read("src/main/java/com/newuniverse/nusmp/arc/lines/" + f + ".java").contains("import net.minecraft"), f + " must stay pure");
    }

    @Test
    void pickRotatesAndCooldownsAreSane() {
        String a = BossLines.pick(BossLines.GOKU, "intro", 0), b = BossLines.pick(BossLines.GOKU, "intro", 1);
        assertNotEquals(a, b);
        assertEquals(a, BossLines.pick(BossLines.GOKU, "intro", BossLines.idsOf(BossLines.GOKU, "intro").size()));
        assertEquals(a, BossLines.pick(BossLines.GOKU, "intro", -BossLines.idsOf(BossLines.GOKU, "intro").size()));
        assertNull(BossLines.pick(BossLines.GOKU, "nope", 0));
        assertEquals(0, BossLines.cooldown("intro"));
        assertTrue(BossLines.cooldown("taunt") >= 600);
        assertTrue(BossLines.priority("form_blue") && BossLines.priority("equip_baal") && !BossLines.priority("taunt"));
    }
}
