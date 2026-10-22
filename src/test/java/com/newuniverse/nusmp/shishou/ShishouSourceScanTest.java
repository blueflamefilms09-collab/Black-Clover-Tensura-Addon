package com.newuniverse.nusmp.shishou;

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

/**
 * The owner's rules 9 (server friendly) and 11 (no lag), pinned by reading the source of Shishou: the server decides, no client class in common code, per-player state keyed by UUID and dropped at every end,
 * packets validated, one SERVER config with a unique file name and a switch per skill, effects through the perf budget, bounded scans, and no hunger/exhaustion anywhere.
 */
class ShishouSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");

    private static String read(String rel) throws IOException { return Files.readString(SRC.resolve(rel)); }

    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", ""); }

    private static List<Path> common() throws IOException {
        try (Stream<Path> s = Files.list(SRC.resolve("shishou"))) { return s.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
    }

    private static List<Path> client() throws IOException {
        try (Stream<Path> s = Files.list(SRC.resolve("client/shishou"))) { return s.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
    }

    private static String method(String src, String name) {
        int at = src.indexOf(" " + name + "(");
        assertTrue(at > 0, "no method " + name);
        int open = src.indexOf('{', at), depth = 0;
        for (int i = open; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '{') depth++;
            if (c == '}' && --depth == 0) return src.substring(open, i + 1);
        }
        return src.substring(open);
    }

    @Test
    void noCommonShishouFileNamesAClientClass() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : common()) if (CLIENT.matcher(code(Files.readString(p))).find()) bad.add(p.getFileName().toString());
        assertTrue(bad.isEmpty(), "common code that names a client-only class: " + bad);
        assertFalse(Files.readString(Path.of("docs/server_safe_allow.txt")).contains("shishou/"), "Shishou needs no allow-list entry: its common side is clean");
        assertTrue(client().size() >= 4, "the client half lives in client/shishou");
    }

    @Test
    void theClientHooksOfThePacketsAreSetByTheClientOnly() throws IOException {
        String net = code(read("shishou/ShishouNet.java"));
        assertTrue(net.contains("clientSync") && net.contains("clientAppraisal"), "the client half is reached through hooks, no client class name in common code");
        String client = code(read("client/shishou/ShishouClient.java"));
        assertTrue(client.contains("ShishouNet.clientSync") && client.contains("ShishouNet.clientAppraisal"), "the client sets the hooks");
    }

    @Test
    void thePerPlayerStateIsKeyedByUuidAndDroppedAtEveryEnd() throws IOException {
        String live = code(read("shishou/ShishouLive.java"));
        assertTrue(live.contains("Map<UUID, Long> LINKED") && live.contains("Map<UUID, Long> KEY_GAP"), "per-player state is keyed by the UUID");
        assertFalse(Pattern.compile("static\\s+(ServerPlayer|Player)\\s+\\w+\\s*[;=]").matcher(live).find(), "no static 'current player'");
        String forget = method(live, "void forget"), clear = method(live, "void clearAll");
        for (String s : new String[]{"THEFT.forget", "COOLDOWNS.forget", "WINDOWS.forget", "CLONES.forget", "GATE.forget", "LINKED.remove", "KEY_GAP.remove", "APPRAISE_GAP.remove", "CHARGE.remove", "SPEECH_COUNTER.remove", "BLADE_READY.remove", "LOW_MANA_TOLD.remove"})
            assertTrue(forget.contains(s), "forget: " + s);
        for (String s : new String[]{"THEFT.clear", "COOLDOWNS.clear", "WINDOWS.clear", "CLONES.clear", "GATE.clear", "LINKED.clear", "KEY_GAP.clear"}) assertTrue(clear.contains(s), "clearAll: " + s);
        String ev = code(read("shishou/ShishouEvents.java"));
        for (String h : new String[]{"PlayerLoggedOutEvent", "PlayerRespawnEvent", "PlayerChangedDimensionEvent", "ServerStoppingEvent", "ServerStartedEvent"}) assertTrue(ev.contains(h), "cleanup / bind on " + h);
    }

    @Test
    void everyPacketIsValidatedOnTheServer() throws IOException {
        String net = code(read("shishou/ShishouNet.java"));
        assertTrue(net.contains("instanceof ServerPlayer") && net.contains("enqueueWork"), "the sender is a real player, the work runs on the server thread");
        assertTrue(net.contains("action < 0 || action >= ACTIONS"), "an unknown action is ignored");
        assertTrue(net.contains("entityId < -1 || entityId > 100_000_000"), "an entity id the game could not have handed out is ignored");
        assertTrue(net.contains("NAME_MAX"), "strings are length-bounded");
        String svc = code(read("shishou/ShishouService.java"));
        String key = method(svc, "onKey"), app = method(svc, "onAppraise");
        assertTrue(key.contains("KEY_GAP"), "key presses are rate limited per player");
        assertTrue(app.contains("APPRAISE_GAP"), "appraisal requests are rate limited per player");
        assertTrue(app.contains("distanceToSqr") || app.contains("closerThan") || app.contains("distance"), "an appraisal target must be near the asker");
    }

    @Test
    void theConfigIsOneServerConfigWithAUniqueFileNameAndASwitchPerSkill() throws IOException {
        String boot = code(read("shishou/ShishouBoot.java"));
        assertTrue(boot.contains("ModConfig.Type.SERVER") && boot.contains("\"nusmp-shishou-server.toml\""));
        assertFalse(boot.contains("ModConfig.Type.CLIENT"), "nothing of Shishou changes only looks");
        try (Stream<Path> w = Files.walk(Path.of("src/main/java"))) {
            long same = w.filter(p -> p.toString().endsWith(".java")).filter(p -> { try { return Files.readString(p).contains("\"nusmp-shishou-server.toml\""); } catch (IOException e) { return false; } }).count();
            assertEquals(1, same, "the file name is used by exactly one registration");
        }
        String cfg = code(read("shishou/ShishouConfig.java"));
        assertTrue(cfg.contains("shishouEnabled") && cfg.contains("shishouVoice"), "relic.shishouEnabled and relic.shishouVoice");
        assertTrue(cfg.contains("ShishouSkill.values()") && cfg.contains("configKey"), "one switch per skill, from the enum");
        assertTrue(cfg.contains("skillOn"), "skillOn(skill)");
        String skills = code(read("shishou/ShishouSkill.java"));
        Matcher m = Pattern.compile("configKey\\(\\)").matcher(skills);
        assertTrue(m.find(), "every skill has a config key");
    }

    @Test
    void everyEffectGoesThroughThePerfBudgetAndTheWorkIsBounded() throws IOException {
        StringBuilder all = new StringBuilder();
        for (Path p : common()) all.append(code(Files.readString(p))).append('\n');
        String s = all.toString();
        assertFalse(s.contains("sendParticles"), "particles go through PerfParticles, never raw");
        assertTrue(code(read("shishou/ShishouSpells.java")).contains("PerfParticles.send"), "spell effects use the perf budget");
        assertFalse(s.contains("PacketDistributor.sendToAllPlayers"), "no broadcast to everyone");
        assertFalse(s.contains("setBlock(") || s.contains("destroyBlock("), "no skill changes blocks: hit-scan and area only");
        // every entity scan is the one bounded helper (box query, capped) plus the one box query of the clone cleanup
        int scans = 0;
        Matcher m = Pattern.compile("getEntitiesOfClass\\(").matcher(s);
        while (m.find()) scans++;
        assertTrue(scans <= 2, "entity box queries stay in ShishouTargets.near and the one skill scan: " + scans);
        assertTrue(code(read("shishou/ShishouTargets.java")).contains("cap"), "the scan helper is capped");
        // the only chunk load is the rare recovery
        Matcher c = Pattern.compile("getChunk\\(").matcher(s);
        int chunks = 0;
        while (c.find()) chunks++;
        assertTrue(chunks <= 1, "one chunk load, in the recovery (a rare event): " + chunks);
        assertFalse(Pattern.compile("while\\s*\\(true\\)").matcher(s).find(), "no unbounded loops");
        ShishouClonesCaps();
    }

    private static void ShishouClonesCaps() {
        assertTrue(ShishouRules.CLONES_PER_CAST <= 6 && ShishouRules.CLONES_PER_CAST >= 1);
    }

    @Test
    void theHeavyPerTickWorkIsGatedByTheItemHooks() throws IOException {
        String ev = code(read("shishou/ShishouEvents.java"));
        assertTrue(ev.contains("SafeCast.tick"), "every listener runs inside the mod's guarded wrapper");
        String svc = code(read("shishou/ShishouService.java"));
        String hold = method(svc, "void holderTickBody");
        assertTrue(hold.contains("% ShishouRules.THEFT_CHECK != 0) return"), "the upkeep runs once a second, not every tick");
        assertEquals(20, ShishouRules.THEFT_CHECK, "that gate is one second");
        String live = code(read("shishou/ShishouEvents.java"));
        assertTrue(live.contains("anyLinked"), "kill / drop listeners are O(1) when nobody holds the sword");
        String tick = method(svc, "void tickServerBody");
        assertFalse(tick.contains("getAllEntities") && tick.contains("for (Entity"), "the server tick never walks all entities");
    }

    @Test
    void theFourHotPathsAreMeasurable() throws IOException {
        String svc = code(read("shishou/ShishouService.java"));
        for (String s : new String[]{"s.shishou.hold", "s.shishou.cast", "s.shishou.hit", "s.shishou.tick"}) assertTrue(svc.contains("PerfProbe.section(\"" + s + "\")"), "probe section " + s);
        for (String s : new String[]{"PROBE_HOLD", "PROBE_CAST", "PROBE_HIT", "PROBE_TICK"}) assertTrue(svc.contains("PerfProbe.end(" + s + ", t0)"), "closed: " + s);
    }

    @Test
    void thereIsNoHungerOrExhaustionAnywhere() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : common()) {
            String c = code(Files.readString(p));
            if (c.contains("getFoodData") || c.contains("causeFoodExhaustion") || c.contains("FoodData") || c.contains("exhaust(")) bad.add(p.getFileName().toString());
        }
        for (Path p : client()) {
            String c = code(Files.readString(p));
            if (c.contains("getFoodData") || c.contains("causeFoodExhaustion")) bad.add(p.getFileName().toString());
        }
        assertTrue(bad.isEmpty(), "hunger or exhaustion: " + bad);
    }

    @Test
    void theClientDrawsAndAsksOnly() throws IOException {
        String r = code(read("client/shishou/ShishouRenderer.java"));
        assertFalse(Pattern.compile("new (float|double|int)\\[").matcher(method(r, "render")).find(), "no array allocated per frame");
        assertTrue(r.contains("CULL_RANGE"), "a far sword is culled");
        String c = code(read("client/shishou/ShishouClient.java"));
        assertTrue(c.contains("PacketDistributor.sendToServer"), "the client only asks");
        assertFalse(c.contains("ShishouService.") || c.contains("ShishouData."), "the client never calls server logic");
    }

    @Test
    void thePureRulesNameNoMinecraftClass() throws IOException {
        for (String f : new String[]{"ShishouRules", "ShishouLevels", "ShishouSkill", "ShishouRecord", "ShishouCap", "ShishouStealTable", "ShishouPredation", "ShishouTheft", "ShishouRecovery", "ShishouCooldowns", "ShishouWindows",
                "ShishouClones", "ShishouStorageRules", "ShishouConcealment", "ShishouAppraisal", "ShishouNatural", "ShishouSpeechGate", "ShishouVoiceLines"}) {
            String c = code(read("shishou/" + f + ".java"));
            assertFalse(c.contains("import net.minecraft") || c.contains("import net.neoforged"), f + " is pure: it is unit tested without the game");
        }
    }
}
