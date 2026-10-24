package com.newuniverse.nusmp.ultimate;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the kingdom animations must stay server-chosen, cheap and packet-free of their own (rules 9 and 11). Reads the real sources: the choice is made on the server through the existing cast-animation mechanism
 * ({@code CastAnim.play}), no packet or client class was added, the number of plays per cast is bounded and none of them is in a per-tick loop, the setting is a SERVER config with the old behaviour behind it, the
 * grimoire hook is one line, and the old clips and the Time Magic are untouched. No game.
 */
class UltimateAnimSourceScanTest {
    private static final Path ROOT = Path.of(".");
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String read(String p) throws IOException { return Files.readString(ROOT.resolve(p)); }

    private static String code(String p) throws IOException { return read(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", ""); }

    private static int count(String s, String what) {
        int n = 0;
        for (int at = s.indexOf(what); at >= 0; at = s.indexOf(what, at + 1)) n++;
        return n;
    }

    @Test
    void theChoiceIsPureAndNamesNoMinecraftOrClientClass() throws IOException {
        String c = code(SRC + "/ultimate/UltimateAnim.java");
        assertFalse(Pattern.compile("(?m)^import (net\\.minecraft|net\\.neoforged|io\\.github|com\\.mojang)").matcher(c).find(), "UltimateAnim is unit tested without a game");
        assertFalse(Pattern.compile("com\\.newuniverse\\.nusmp\\.client\\.|Minecraft\\.getInstance\\(").matcher(c).find());
        assertFalse(c.contains("static ServerPlayer") || c.contains("static Player"), "no static player");
    }

    @Test
    void thereAreAtMostFourPlaysPerCastAndNoneInAPerTickLoop() throws IOException {
        String m = code(SRC + "/ultimate/UltimateManager.java");
        assertEquals(4, count(m, "CastAnim.play("), "start, the hold, the release and the fizzle: at most 4 payloads per cast, plus the stop");
        assertEquals(1, count(m, "CastAnim.stop("), "one stop, in finish()");
        assertEquals(1, count(m, "s.clips.loopDue(age)"), "the hold is sent from one place, once (age == START_TICKS)");
        assertEquals(1, count(m, "s.clips.release()"), "the release clip is sent from the release only");
        assertEquals(1, count(m, "s.clips.first()") - count(m, "s.clips.first() : clip"), "the first clip is sent from start() only (the page hook reads it)");
        String step = m.substring(m.indexOf("private static void step("), m.indexOf("private static boolean sealed("));
        assertEquals(1, count(step, "CastAnim.play("), "the per-tick step plays at most the one hold clip, guarded by loopDue");
        assertTrue(step.contains("if (s.clips.loopDue(age)) CastAnim.play(p, s.clips.loop())"));
    }

    @Test
    void noPacketOfItsOwnAndTheOldPayloadIsUnchanged() throws IOException {
        for (String f : new String[]{"UltimateAnim.java", "UltimateManager.java", "UltimateConfig.java"})
            assertFalse(Pattern.compile("CustomPacketPayload|StreamCodec|registerPayloads").matcher(code(SRC + "/ultimate/" + f)).find(), f);
        String payload = read(SRC + "/anim/CastAnimPayload.java");
        assertTrue(payload.contains("buf.writeVarInt(p.entity()); buf.writeUtf(p.clip(), 48);"), "the existing payload keeps its shape (an entity id and a clip name of at most 48 characters)");
        for (String id : UltimateAnim.ids()) assertTrue(id.length() <= 48, id + " does not fit the payload");
    }

    @Test
    void theKingdomIsLookedUpOnTheServerWithTheHeritageThenTheCover() throws IOException {
        String m = code(SRC + "/ultimate/UltimateManager.java");
        assertTrue(m.contains("Heritage.kingdom(p)") && m.contains("GrimoireBook.cover(inst).kingdom") && m.contains("UltimateAnim.resolveKingdom(heritage, cover)"));
        assertTrue(m.contains("UltimateAnim.choose(UltimateConfig.kingdomAnimations(), kingdomOf(p, inst))"), "the setting is read on the server, once per cast");
        assertEquals(1, count(m, "kingdomOf(p, inst)"), "once per cast, not per tick");
    }

    @Test
    void theSettingIsAServerConfigWithItsDefaultTrue() throws IOException {
        String c = read(SRC + "/ultimate/UltimateConfig.java");
        assertTrue(c.contains(".define(\"kingdomAnimations\", true)"));
        assertTrue(c.contains("define(\"showInCharacterGrimoires\", true)"));
        assertTrue(read(SRC + "/ultimate/UltimateBoot.java").contains("Type.SERVER"), "the ultimate settings are SERVER settings (the animation is drawn by the clients, the choice is the server's)");
        assertEquals(0, count(c, "Type.CLIENT"));
    }

    @Test
    void theGrimoireHookIsOneLineAndTheDefaultsAreTheOldIds() throws IOException {
        String g = read(SRC + "/book/GrimoireBook.java");
        assertEquals(1, count(g, "UltimateManager.pageClip("), "one hook in the grimoire's release flow");
        assertTrue(g.contains("UltimateManager.pageClip(player, p.id(), com.newuniverse.nusmp.anim.CastAnim.releaseFor(p))"));
        String a = read(SRC + "/ultimate/UltimateAnim.java"), anim = read(SRC + "/anim/CastAnim.java");
        assertTrue(a.contains("new Clips(null, \"cast_mana_zone\", \"cast_signature\")"));
        assertTrue(anim.contains("MANA_ZONE = \"cast_mana_zone\"") && anim.contains("SIGNATURE = \"cast_signature\""), "the default ids are still the CastAnim constants");
        assertTrue(read(SRC + "/ultimate/UltimatePages.java").contains("com.newuniverse.nusmp.anim.CastAnim.MANA_ZONE"), "the page's own clip is kept (setting off = exactly the old flow)");
    }

    @Test
    void theTimeMagicAndTheOldGeneratorAreNotTouchedAndTheNewOneOnlyAppendsItsOwnClips() throws IOException {
        String gen = read("tools/gen_ultimate_anims.py");
        assertTrue(gen.contains("data[\"animations\"].update(clips)"), "the generator adds or refreshes only its own clips");
        assertTrue(gen.contains("ult_%s_%s"));
        assertFalse(gen.contains("TimeMagic") || gen.contains("time_"), "no Time Magic");
        for (String f : new String[]{"UltimateAnim.java", "UltimateSelfTests.java"}) {
            String c = code(SRC + "/ultimate/" + f);
            assertFalse(c.contains("TimeMagicLayer") || c.contains("TimeBook") || c.contains("book_time"), f);
        }
        String client = read(SRC + "/client/CastAnimClient.java");
        assertFalse(client.contains("ult_"), "the client player is unchanged: it plays whatever clip name the server sends");
    }

    @Test
    void theSelfTestsAreRegisteredUnderTheUltimatePrefix() throws IOException {
        String t = read(SRC + "/ultimate/UltimateSelfTests.java");
        assertTrue(t.contains("\"magic.ultimate.anim\"") && t.contains("\"magic.ultimate.charbook\""));
    }
}
