package com.newuniverse.nusmp.companion;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: nothing about a multiplayer server can be profiled here, so the rules of CLAUDE.md 9 and 11 are pinned the only way they can be: by reading the source of the companion system. A pattern that must never come back
 * fails the test: a client class named from common code, a static "current player", a keybind, a packet read without a bound, a talk pick that skips one of its checks, a bubble without its throttle or budget, a
 * crossing without its cap, a per-tick world scan without a gate. Comments are skipped, so a word in a comment never counts. If a rule is replaced by a better one on purpose, change the pin in the same commit and say why.
 */
class CompanionSourceScanTest {
    private static final Path MAIN = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)|Dist\\.CLIENT");

    /** The source of a file with its comments and string contents removed (so a word in a comment or a message never counts). */
    private static String code(String rel) throws IOException {
        String s = Files.readString(MAIN.resolve(rel), StandardCharsets.UTF_8);
        s = s.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\n]*", " ");
        return s;
    }

    private static List<String> files(String dir) throws IOException {
        List<String> out = new ArrayList<>();
        try (Stream<Path> w = Files.walk(MAIN.resolve(dir))) {
            w.filter(p -> p.toString().endsWith(".java")).forEach(p -> out.add(MAIN.relativize(p).toString().replace('\\', '/')));
        }
        return out;
    }

    /** The body of the first method called {@code name} (a small brace matcher). */
    private static String method(String src, String name) {
        Matcher m = Pattern.compile("\\b" + name + "\\s*\\([^;{}]*\\)\\s*(?:throws [\\w., ]+)?\\s*\\{").matcher(src);
        assertTrue(m.find(), "no method " + name);
        int open = m.end() - 1, depth = 0, i = open;
        for (; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) break;
        }
        return src.substring(open, Math.min(i + 1, src.length()));
    }

    // ---------------------------------------------------------------- rule 9: server friendly
    @Test
    void commonCompanionCodeNamesNoClientClass() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String f : files("companion")) if (CLIENT.matcher(code(f)).find()) bad.add(f);
        assertTrue(bad.isEmpty(), "common companion code that names a client class: " + bad);
    }

    @Test
    void thereIsNoStaticCurrentPlayerAndNoKeybind() throws IOException {
        Pattern staticPlayer = Pattern.compile("static\\s+(?:volatile\\s+)?(?:final\\s+)?(?:Server)?(?:Player|LocalPlayer)\\s+\\w+\\s*[;=]");
        Pattern key = Pattern.compile("KeyMapping|RegisterKeyMappingsEvent|InputConstants|GLFW_KEY");
        List<String> bad = new ArrayList<>();
        List<String> all = new ArrayList<>(files("companion"));
        all.addAll(files("client/companion"));
        for (String f : all) {
            String c = code(f);
            if (staticPlayer.matcher(c).find()) bad.add(f + " (static player)");
            if (key.matcher(c).find()) bad.add(f + " (keybind)");
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void everyPacketReadIsBoundedAndEveryPlayToServerHandlerGoesThroughTheTalkChecks() throws IOException {
        String net = code("companion/net/CompanionNet.java");
        assertFalse(Pattern.compile("readUtf\\(\\s*\\)").matcher(net).find(), "readUtf() without a bound");
        assertTrue(net.contains("readUtf(TalkRules.WIRE_ID)"), "the pick's id is read with the wire bound");
        assertTrue(net.contains(".optional()"), "the channel is optional");
        Matcher m = Pattern.compile("playToServer\\(([A-Za-z]+)\\.TYPE").matcher(net);
        int n = 0;
        while (m.find()) n++;
        assertEquals(2, n, "exactly two asks go up: pick and close");
        assertTrue(net.contains("Talk.onPick(sp,") && net.contains("Talk.onClose(sp,"), "both asks go to Talk");
        assertTrue(net.contains("ctx.player() instanceof ServerPlayer sp"), "the sender is checked to be a server player");
        String view = code("companion/talk/TalkView.java");
        assertTrue(view.contains("n > TalkRules.MAX_OPTIONS") && view.contains("slots > CompanionRules.POUCH_SLOTS"), "a view read off the wire is bounded");
    }

    @Test
    void aPickIsCheckedBeforeTheEntityHearsOfIt() throws IOException {
        String body = method(code("companion/talk/Talk.java"), "onPick");
        for (String must : new String[]{"SESSIONS.get(player.getUUID())", "s.entityId() != entityId", "isAlive()", "TalkRules.inReach", "PICKS.allow", "TalkRules.acceptPick(id, s.offered())", "getUUID().equals(s.entity())"})
            assertTrue(body.contains(must), "onPick lost the check: " + must);
        int accept = body.indexOf("TalkRules.acceptPick"), call = body.indexOf("safeChoose");
        assertTrue(accept > 0 && call > accept, "the id is accepted before talkChoose is called");
        assertTrue(body.indexOf("TalkRules.inReach") < call && body.indexOf("PICKS.allow") < call, "reach and rate limit come before the call");
        String open = method(code("companion/talk/Talk.java"), "open");
        for (String must : new String[]{"CompanionConfig.enabled()", "TalkRules.inReach", "OPENS.allow", "isAlive()"}) assertTrue(open.contains(must), "open lost the check: " + must);
    }

    @Test
    void theTalkStateIsPerPlayerAndIsCleanedUp() throws IOException {
        String talk = code("companion/talk/Talk.java");
        assertTrue(talk.contains("Map<UUID, Session> SESSIONS"), "one record per player UUID");
        String ev = code("companion/CompanionEvents.java");
        for (String must : new String[]{"PlayerLoggedOutEvent", "Talk.forget", "PlayerChangedDimensionEvent", "LivingDeathEvent", "ServerStoppedEvent", "Talk.clear()", "Talk.sweep", "CompanionTravel.forget", "EntityLeaveLevelEvent"})
            assertTrue(ev.contains(must), "CompanionEvents lost " + must);
    }

    @Test
    void settingsAreInTheRightConfigFiles() throws IOException {
        assertTrue(code("companion/CompanionBoot.java").contains("ModConfig.Type.SERVER, CompanionConfig.SPEC, \"nusmp-companions-server.toml\""), "gameplay settings are a SERVER config");
        assertTrue(code("client/companion/CompanionClient.java").contains("ModConfig.Type.CLIENT, CompanionClientConfig.SPEC, \"nusmp-companions-client.toml\""), "looks settings are a CLIENT config");
        String cfg = code("companion/CompanionConfig.java");
        assertTrue(cfg.contains("\"enabled\"") && cfg.contains("define(\"enabled\", true)"), "companions.enabled defaults to true");
        assertFalse(cfg.contains("defineInList"), "no list-validated value (it throws while the server loads)");
    }

    @Test
    void theSelfTestsUseTheSmokePrefix() throws IOException {
        String t = Files.readString(MAIN.resolve("companion/CompanionSelfTests.java"), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("SelfTests\\.register\\(\"([^\"]+)\"").matcher(t);
        int n = 0;
        while (m.find()) {
            assertTrue(m.group(1).startsWith("magic.companions."), m.group(1));
            n++;
        }
        assertTrue(n >= 5, "the five self-tests are registered: " + n);
        assertTrue(code("companion/CompanionBoot.java").contains("CompanionSelfTests.register()"), "the boot registers the self-tests");
    }

    // ---------------------------------------------------------------- rule 11: no lag
    @Test
    void bubblesAreThrottledBudgetedAndBounded() throws IOException {
        String b = method(code("companion/talk/Bubble.java"), "say");
        for (String must : new String[]{"CompanionConfig.bubbles()", "BubbleRules.mayFor", "BUDGET.take", "MAX_RECORDS", "sendToPlayersTrackingEntity", "chars > BubbleRules.MAX_CHARS * 4"})
            assertTrue(b.contains(must), "Bubble.say lost: " + must);
        assertFalse(b.contains("sendToAllPlayers") || b.contains("sendToPlayersInDimension"), "a bubble goes to the trackers only");
        String client = code("client/companion/BubbleClient.java");
        for (String must : new String[]{"BubbleRules.MAX_SHOWN", "BubbleRules.inDrawRange", "MAX_LINES", "b.wrapped == null"}) assertTrue(client.contains(must), "BubbleClient lost: " + must);
        assertEquals(1, Pattern.compile("\\.split\\(").matcher(client).results().count(), "text is split in one place, once per bubble, never per frame");
    }

    @Test
    void crossingsAreCappedGatedAndNeverForceAChunk() throws IOException {
        String t = code("companion/follow/CompanionTravel.java");
        for (String must : new String[]{"TravelRules.carried(", "CompanionConfig.travel()", "hasChunkAt", "TravelRules.stale", "addRegionTicket(TicketType.PORTAL", "getEntitiesOfClass(Mob.class"})
            assertTrue(t.contains(must), "CompanionTravel lost: " + must);
        assertEquals(1, Pattern.compile("getEntitiesOfClass\\(").matcher(t).results().count(), "one box query per crossing");
        assertFalse(t.contains("forceChunk") || t.contains("setChunkForced") || t.contains("getChunk(") , "no chunk is forced to load");
        String tick = method(t, "tick");
        assertTrue(tick.contains("ARRIVED.isEmpty()"), "the per-tick work returns at once when nothing is waiting");
        assertTrue(code("companion/CompanionEvents.java").contains("if (CompanionTravel.busy())"), "the tick hook is gated");
    }

    @Test
    void thePerTickWorkIsGated() throws IOException {
        String bond = code("companion/bond/Bond.java");
        assertTrue(method(bond, "tick").contains("tickCount % 20 != 7"), "the pouch looks at the player once a second");
        String ev = method(code("companion/CompanionEvents.java"), "onServerTick");
        assertTrue(ev.contains("% 20 != 3"), "the sweeps run once a second");
        String ent = code("companion/sample/ArcCompanionEntity.java");
        assertTrue(ent.contains("--cooldown") && ent.contains("me.owner != null ? 10 : 40"), "the target scan is rate limited");
        assertTrue(ent.contains("tickCount - followedAt >= 10"), "the owner lookup is cached");
        assertEquals(1, Pattern.compile("getEntitiesOfClass\\(").matcher(ent).results().count(), "the sample companion has one box query, the gated target scan");
        String voice = code("companion/voice/VoiceLines.java");
        assertTrue(voice.contains("BUDGET.take"), "voice lines have a per-tick budget");
        assertTrue(code("companion/CompanionBoot.java").contains("CompanionNet::register"), "the payloads are registered");
    }

    @Test
    void theDamageGuardIsCheap() throws IOException {
        String g = method(code("companion/CompanionEvents.java"), "onDamage");
        assertFalse(g.contains("getEntities") || g.contains("level()") , "no world access in the damage guard");
        assertTrue(g.indexOf("isCompanion(victim)") < g.indexOf("getSource"), "the first test is the cheap instanceof");
    }
}
