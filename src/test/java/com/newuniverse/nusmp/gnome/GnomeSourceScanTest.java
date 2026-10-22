package com.newuniverse.nusmp.gnome;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** The rules of the owner (9: server friendly, 11: no lag), pinned by reading the source of the Gnome: the server decides, no client class in common code, nothing allocated per frame, a unique config file, bounded work. */
class GnomeSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");

    private static String read(String rel) throws IOException { return Files.readString(SRC.resolve(rel)); }

    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", ""); }

    private static List<Path> gnomeFiles() throws IOException {
        try (Stream<Path> s = Files.list(SRC.resolve("gnome"))) { return s.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
    }

    /** The source of the method with this name (a brace matcher that skips nothing clever: the files are small). */
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
    void noGnomeCommonFileNamesAClientClass() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : gnomeFiles()) if (CLIENT.matcher(code(Files.readString(p))).find()) bad.add(p.getFileName().toString());
        assertTrue(bad.isEmpty(), "common code that names a client-only class: " + bad);
        assertFalse(Files.readString(Path.of("docs/server_safe_allow.txt")).contains("gnome/"), "the Gnome needs no allow-list entry: its common side is clean");
    }

    @Test
    void theStateIsPerPlayerUuidAndDroppedAtEveryEnd() throws IOException {
        String g = code(read("gnome/Gnome.java"));
        assertTrue(g.contains("Map<UUID, State> STATES") && g.contains("Set<UUID> LIVE"), "per-player state is keyed by the UUID");
        assertFalse(Pattern.compile("static\\s+(ServerPlayer|Player)\\s+\\w+\\s*[;=]").matcher(g).find(), "no static 'current player'");
        String ev = code(read("gnome/GnomeEvents.java"));
        for (String h : new String[]{"PlayerLoggedOutEvent", "LivingDeathEvent", "PlayerRespawnEvent", "PlayerChangedDimensionEvent", "ServerStoppingEvent"}) assertTrue(ev.contains(h), "cleanup on " + h);
        String forget = method(g, "void forget");
        for (String s : new String[]{"STATES.remove", "LIVE.remove", "SPIRITS.forget", "FULLS.forget", "GATE.forget", "LINES.forget", "discard()"}) assertTrue(forget.contains(s), "forget: " + s);
        assertTrue(method(g, "clearAll").contains("STATES.clear()"), "a server stop clears everything");
    }

    @Test
    void theKeyPacketIsValidatedOnTheServer() throws IOException {
        String net = code(read("gnome/GnomeNet.java"));
        assertTrue(net.contains("action != 0") && net.contains("instanceof ServerPlayer") && net.contains("enqueueWork"), "the sender is a real player, an unknown action is ignored, the work runs on the server thread");
        assertTrue(net.contains("playToServer") && !net.contains("playToClient"), "client to server only: one byte per press");
        String g = code(read("gnome/Gnome.java"));
        String toggle = method(g, "toggle");
        assertTrue(toggle.contains("gapOk") && toggle.contains("eligible"), "rate limited and gated by the Earth grimoire and the switch");
    }

    @Test
    void theConfigIsAServerConfigWithAUniqueFileName() throws IOException {
        String boot = code(read("gnome/GnomeBoot.java"));
        assertTrue(boot.contains("ModConfig.Type.SERVER") && boot.contains("\"nusmp-gnome-server.toml\""));
        assertFalse(boot.contains("ModConfig.Type.CLIENT"), "nothing of the Gnome changes only looks");
        String cfg = code(read("gnome/GnomeConfig.java"));
        assertTrue(cfg.contains("\"gnome\"") && cfg.contains("push(\"magic\")"), "magic.gnome");
        assertTrue(cfg.contains(".define(\"gnome\", true)"), "default true");
        try (Stream<Path> w = Files.walk(Path.of("src/main/java"))) {
            long same = w.filter(p -> p.toString().endsWith(".java")).filter(p -> { try { return Files.readString(p).contains("\"nusmp-gnome-server.toml\""); } catch (IOException e) { return false; } }).count();
            assertEquals(1, same, "the file name is used once");
        }
    }

    @Test
    void theEarthBookIsWrappedAndTheShotsAskTheEmitter() throws IOException {
        String earth = read("book/EarthBook.java");
        assertTrue(earth.contains("gnome.Gnome.wrap(List.of("), "the nine pages go through Gnome.wrap");
        assertTrue(earth.contains("\"spike_line\"") && earth.contains("\"sinkhole\"") && earth.contains("\"rampaging_mother_earth\""), "the page ids are the old ones");
        String shots = read("book/TensuraShots.java");
        assertTrue(shots.contains("gnome.Gnome.origin("), "the Boulder Shot starts from the emitter");
        assertTrue(shots.contains("firecast.FireCasting.origin("), "the fire casting hook is still there");
        String g = code(read("gnome/Gnome.java"));
        String wrap = method(g, "wrap");
        assertTrue(wrap.contains("pg.cost") && wrap.contains("pg.cooldown()") && wrap.contains("pg.id()"), "the wrapped page keeps its id, cost and cooldown");
        assertTrue(method(g, "cast").contains("GnomeConfig.enabled()"), "with magic.gnome off every page is cast the old way");
    }

    @Test
    void theServerWorkIsBoundedAndNothingScansTheWorld() throws IOException {
        String all = "";
        for (Path p : gnomeFiles()) all += code(Files.readString(p));
        assertFalse(all.contains("getEntitiesOfClass") || all.contains("getEntities(") || all.contains("getNearbyEntities") || all.contains("getChunk") || all.contains("getBlockState"), "no world or entity scan, no chunk load");
        String ev = code(read("gnome/GnomeEvents.java"));
        assertTrue(method(ev, "onPlayerTick").contains("LIVE.isEmpty()") && method(ev, "onPlayerTick").contains("& 3"), "the upkeep: an empty set first, then every fourth tick, live players only");
        String props = code(read("gnome/GnomeProps.java"));
        assertTrue(props.contains("NUDGE"), "the companion is nudged every few ticks, not every tick");
        String g = code(read("gnome/Gnome.java"));
        assertTrue(g.contains("SpellRuntime.later(") && g.contains("GATE.allow("), "contact effects are scheduled, sounds go through the gate");
        assertFalse(all.toLowerCase().contains("exhaustion") || all.toLowerCase().contains("foodlevel") || all.toLowerCase().contains("addexhaustion"), "nothing touches hunger");
    }

    @Test
    void thePainterAllocatesNothingPerFrameAndCullsAndBudgets() throws IOException {
        String p = code(read("client/prop/GnomePropPainter.java"));
        assertTrue(p.contains("GnomeLod.level(") && p.contains("GnomeLod.fit(") && p.contains("PerfSettings.vfxQuality()"), "level of detail, the shared budget and the effect quality");
        for (String m : new String[]{"paint", "dust", "pebbles"}) {
            String body = method(p, m);
            assertFalse(Pattern.compile("\\bnew\\s+[A-Z]").matcher(body).find(), m + " allocates per frame");
            assertFalse(body.contains("ResourceLocation.") || body.contains("String.format") || body.contains("+ \"") , m + " builds locations or strings per frame");
        }
        assertTrue(p.contains("Fx.send(") && p.contains("Fx.ring("), "dust and pebbles are custom particles through the Fx door");
        assertFalse(p.contains("ParticleTypes") || p.contains("addParticle("), "no vanilla particle");
        String d = code(read("client/prop/GnomeDraw.java"));
        assertFalse(Pattern.compile("\\bnew\\s+[A-Z]").matcher(method(d, "gnome")).find(), "GnomeDraw.gnome allocates per frame");
        assertTrue(d.contains("Layer.ADDITIVE") && !d.contains("RenderType.eyes"), "the glow is the additive pass with a glow-only sheet, never RenderType.eyes");
        String lod = code(read("gnome/GnomeLod.java"));
        assertTrue(lod.contains("FRAME_QUADS = 3200") && lod.contains("112.0") && lod.contains("64.0"));
    }

    @Test
    void theMoodsAndTheVoiceAreDecidedByTheServerHeardNearbyAndLimited() throws IOException {
        String mood = code(read("gnome/GnomeMoodRules.java"));
        assertFalse(mood.contains("net.minecraft") || mood.contains("net.neoforged"), "the table of moods is pure: no game class");
        String g = code(read("gnome/Gnome.java"));
        assertTrue(g.contains("Voice.say(") && g.contains("LINES.allow(") && g.contains("LINE_RADIUS") && g.contains("isSpectator"), "a line goes through the voice system, to listeners within 32 blocks, through the per-listener gate");
        assertTrue(g.contains("GnomeMoodRules.pick(") && g.contains("mood.ready(") || g.contains("s.mood.ready("), "a situation has its own cooldown");
        assertFalse(g.contains("PacketDistributor"), "no packet of its own for the voice: the voice system sends its tiny Say packet");
        String ev = code(read("gnome/GnomeEvents.java"));
        assertTrue(ev.contains("LivingDamageEvent.Post") && ev.contains("onOwnerHurt") && ev.contains("onOwnerKill") && ev.contains("onOwnerDeath"), "hit, kill and death of the master are heard");
        assertTrue(method(ev, "onDamage").contains("LIVE.contains"), "only a master with a Gnome out costs anything on a hit");
        assertTrue(method(ev, "onDeath").contains("LIVE.contains") || method(ev, "onDeath").contains("onOwnerDeath"), "death handling is gated");
        String props = code(read("gnome/GnomeProps.java"));
        assertTrue(props.contains("CLIP_SINK") && props.contains("CLIP_DOZE"), "a farewell is not cut short by the orphan check");
        String sounds = code(read("gnome/GnomeSounds.java"));
        assertFalse(sounds.contains("Minecraft.getInstance"), "sounds are server driven");
    }

    @Test
    void theClientKeyOnlyAsksAndSendsOneByte() throws IOException {
        String c = code(read("client/gnome/GnomeClient.java"));
        assertTrue(c.contains("new GnomeNet.Key(0)") && c.contains("consumeClick"), "the key asks; the server decides");
        assertFalse(c.contains("Gnome.toggle"), "the client never runs the rules itself");
    }
}
