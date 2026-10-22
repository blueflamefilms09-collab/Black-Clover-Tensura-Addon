package com.newuniverse.nusmp.undine;

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
 * Nothing of the Undine can be run here, so the rules the owner set (server friendly, no lag, nothing removed) are pinned from the sources: what is pure stays pure, the common package names no client
 * class, per player state is keyed by UUID, nothing scans the world, nothing is allocated while drawing, the packet is validated on the server, the hooks in the shared files are the append-only
 * one-liners, the old look stays behind the switch and the Water grimoire's pages are untouched.
 */
class UndineSourceScanTest {
    private static final Path MAIN = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path COMMON = MAIN.resolve("undine");

    private static String read(Path p) throws IOException { return Files.readString(p); }

    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    /** The body of the first method called {@code name} (brace matched; strings and comments are not in the way of the code this test reads). */
    private static String body(String src, String name) {
        Matcher m = Pattern.compile("(?m)^[ \\t]*(?:(?:public|private|protected|static|final)\\s+)*[\\w<>\\[\\],.? ]+?\\s+" + name + "\\s*\\([^;{}]*\\)\\s*\\{").matcher(code(src));
        assertTrue(m.find(), "no method " + name);
        String c = code(src);
        int open = m.end() - 1, depth = 0;
        for (int i = open; i < c.length(); i++) {
            if (c.charAt(i) == '{') depth++;
            else if (c.charAt(i) == '}' && --depth == 0) return c.substring(open, i + 1);
        }
        fail("unbalanced " + name);
        return "";
    }

    @Test
    void thePureClassesNameNoMinecraftTypeSoJUnitCanRunThem() throws IOException {
        for (String f : new String[]{"UndineRules", "UndineLod", "UndineLedger", "UndineSoundSet", "UndineVoiceBudget", "UndinePose", "UndineFace", "UndineLines", "UndineMoodRules", "UndineSpeechBudget"}) {
            String c = code(read(COMMON.resolve(f + ".java")));
            assertFalse(c.contains("net.minecraft") || c.contains("net.neoforged") || c.contains("com.mojang"), f + " must stay pure");
        }
    }

    @Test
    void theCommonPackageNamesNoClientClassAndNoStaticCurrentPlayer() throws IOException {
        try (Stream<Path> w = Files.walk(COMMON)) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String c = code(read(p));
                assertFalse(Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.").matcher(c).find(), p + " names a client class");
                assertFalse(c.contains("Minecraft.getInstance()"), p + " asks for the client");
                assertFalse(Pattern.compile("static\\s+(?:volatile\\s+)?(?:ServerPlayer|Player|LivingEntity)\\s+\\w+").matcher(c).find(), p + " keeps a static current player");
            }
        }
        String casting = read(COMMON.resolve("UndineCasting.java"));
        assertTrue(casting.contains("Map<UUID, State> STATES") && casting.contains("Set<UUID> LIVE"), "state is per player UUID");
    }

    @Test
    void nothingScansTheWorldAndTheUpkeepIsCheap() throws IOException {
        for (String f : new String[]{"UndineCasting", "UndineProps", "UndineEvents", "UndineAudio", "UndineSpeech"}) {
            String c = code(read(COMMON.resolve(f + ".java")));
            for (String bad : new String[]{"getEntitiesOfClass", "getEntities(", "getBlockState", "getChunk(", "forceLoad", "loadChunk", "getAllEntities"}) assertFalse(c.contains(bad), f + " scans or loads the world: " + bad);
        }
        String ev = code(read(COMMON.resolve("UndineEvents.java")));
        assertTrue(ev.contains("UndineCasting.LIVE.isEmpty()") && ev.contains("(p.tickCount & 3) != 0"), "the tick handler returns at once for a player with nothing live, and works every fourth tick");
        String props = code(read(COMMON.resolve("UndineProps.java")));
        assertTrue(props.contains("e.life() % UndineRules.NUDGE != 0"), "a prop is only looked after every few ticks");
        String casting = code(read(COMMON.resolve("UndineCasting.java")));
        for (String cleanup : new String[]{"forget(ServerPlayer", "clearAll()", "LIVE.remove"}) assertTrue(casting.contains(cleanup), cleanup);
        String events = code(ev);
        for (String hook : new String[]{"PlayerLoggedOutEvent", "LivingDeathEvent", "PlayerRespawnEvent", "PlayerChangedDimensionEvent", "ServerStoppingEvent"}) assertTrue(events.contains(hook), "cleanup on " + hook);
    }

    @Test
    void theKeyPacketIsValidatedOnTheServer() throws IOException {
        String net = code(read(COMMON.resolve("UndineNet.java")));
        assertTrue(net.contains("if (action != 0) return;") && net.contains("ctx.player() instanceof ServerPlayer"), "an unknown action and a sender that is not a player are ignored");
        assertTrue(net.contains("playToServer"), "client to server only");
        String toggle = body(read(COMMON.resolve("UndineCasting.java")), "toggle");
        assertTrue(toggle.contains("PACKET_GAP") && toggle.contains("UndineConfig.enabled()") && toggle.contains("CanonBook.NOELLE"), "rate, switch and canon book are checked");
    }

    @Test
    void theConfigIsAServerConfigWithItsOwnFileAndTheSwitchDefaultsOn() throws IOException {
        String boot = code(read(COMMON.resolve("UndineBoot.java")));
        assertTrue(boot.contains("ModConfig.Type.SERVER, UndineConfig.SPEC, \"nusmp-undine-server.toml\""));
        String cfg = read(COMMON.resolve("UndineConfig.java"));
        assertTrue(cfg.contains(".define(\"undine\", true)") && cfg.contains("b.push(\"magic\")"), "magic.undine defaults to true");
        assertTrue(boot.contains("UndineSelfTests.register()") && boot.contains("UndineSounds.SOUNDS.register(modBus)"));
    }

    @Test
    void nothingIsAllocatedWhileDrawingAndNoFlatCutOutOrRawEyesTypeIsUsed() throws IOException {
        String draw = read(MAIN.resolve("client/prop/UndineDraw.java"));
        for (String m : new String[]{"undine", "pass", "tint", "ripples"}) assertFalse(Pattern.compile("\\bnew\\s+[A-Z]").matcher(body(draw, m)).find(), "UndineDraw." + m + " allocates");
        String painter = read(MAIN.resolve("client/prop/UndinePropPainter.java"));
        for (String m : new String[]{"paint", "spray"}) assertFalse(Pattern.compile("\\bnew\\s+[A-Z]").matcher(body(painter, m)).find(), "UndinePropPainter." + m + " allocates");
        String entity = read(MAIN.resolve("client/undine/UndineEntityDraw.java"));
        assertFalse(Pattern.compile("\\bnew\\s+[A-Z]").matcher(body(entity, "draw")).find(), "UndineEntityDraw.draw allocates");
        for (String s : new String[]{draw, painter, entity}) {
            String c = code(s);
            assertFalse(c.contains("RenderType.eyes(") || c.contains("entityTranslucentEmissive(") || c.contains("RenderType.energySwirl"), "a raw additive / emissive type: go through AuraRender (SafeAdditive)");
            assertFalse(c.contains("PropDraw.quad("), "no flat single quad of its own: the only flat quads are the soft ripple rings of SealDraw.flat");
        }
        assertTrue(code(draw).contains("AuraRender.additive(RING)") && code(draw).contains("AuraRender.additive(FX.texture())"), "the soft passes are additive through the SafeAdditive types");
        assertTrue(code(painter).contains("UndineLod.level(") && code(painter).contains("UndineLod.fit(") && code(painter).contains("UndineLod.ownerPriority("), "distance cull, level of detail and the shared budget");
        assertTrue(code(entity).contains("UndineLod.fit(") && code(entity).contains("UndineLod.level("));
    }

    @Test
    void theHooksInTheSharedFilesAreTheAppendOnlyOneLinersAndTheOldLookStaysBehindTheSwitch() throws IOException {
        String gb = read(MAIN.resolve("book/GrimoireBook.java"));
        assertTrue(gb.contains("com.newuniverse.nusmp.undine.UndineCasting.run(this, p, instance, player, mode)"), "the cast goes through UndineCasting.run, which falls through to FireCasting.run");
        assertTrue(gb.contains("UndineCasting.origin(p, com.newuniverse.nusmp.firecast.FireCasting.origin(p, from))"));
        assertTrue(read(MAIN.resolve("book/SpellRuntime.java")).contains("UndineCasting.origin(owner, com.newuniverse.nusmp.firecast.FireCasting.origin(owner, start))"));
        assertTrue(read(MAIN.resolve("book/TensuraShots.java")).contains("UndineCasting.origin(p, com.newuniverse.nusmp.firecast.FireCasting.origin("));
        String cast = code(read(COMMON.resolve("UndineCasting.java")));
        assertTrue(cast.contains("return FireCasting.run(book, page, inst, p, mode)"), "every other caster takes the generic path untouched");
        String pk = read(MAIN.resolve("prop/PropKind.java"));
        assertTrue(pk.indexOf("FIRECAST_2") < pk.indexOf("UNDINE_1") && pk.indexOf("SYLPH_1") < pk.indexOf("UNDINE_1"), "the new kind is appended after the older ones (later spirits follow it)");
        String r = read(MAIN.resolve("client/ElementalSpiritRenderer.java"));
        assertTrue(r.contains("GreatSpiritModels.UNDINE_MERMAID") && r.contains("UndineEntityDraw.draw("), "the old mermaid stays, drawn when the switch is off");
        assertTrue(read(MAIN.resolve("client/undine/UndineEntityDraw.java")).contains("if (!UndineConfig.enabled()) return false;"));
        String ent = read(MAIN.resolve("entity/ElementalSpiritLordEntity.java"));
        assertTrue(ent.contains("UndineAudio.play(this,"), "the Spirit Lord of Water uses the Undine sound set (through the budget, which is silent when the switch is off)");
        assertTrue(code(read(COMMON.resolve("UndineAudio.java"))).contains("if (!UndineConfig.enabled() || owner == null) return false;"));
    }

    @Test
    void theWaterGrimoiresPagesAreUntouched() throws IOException {
        String water = read(MAIN.resolve("book/WaterBook.java"));
        List<String> ids = new ArrayList<>();
        Matcher m = Pattern.compile("BookPage\\.\\w+\\(\"(\\w+)\"").matcher(water.substring(water.indexOf("private final List<BookPage> pages"), water.indexOf("public WaterBook()")));
        while (m.find()) ids.add(m.group(1));
        assertEquals(List.of("waterball", "roar", "cradle", "aqua_javelin", "sea_dragons_nest", "valkyrie_dress", "sea_dragons_wrath", "tide_lance", "scale_mail", "torrent", "mermaid_form", "sea_dragon_god_form", "black_dragon_form"), ids, "no page renamed, removed or reordered");
    }

    @Test
    void theSpeechGoesThroughTheVoiceSystemWithItsOwnLimitsAndOnlyToNearbyPlayers() throws IOException {
        String c = code(read(COMMON.resolve("UndineSpeech.java")));
        assertTrue(c.contains("Voice.say("), "the lines are said through the project's voice system");
        assertTrue(c.contains("BUDGET.ownerMay(") && c.contains("BUDGET.listenerMay("), "an owner cooldown per group and a gap per listener");
        assertTrue(c.contains("UndineSpeechBudget.RADIUS"), "only players within 32 blocks are sent a line");
        assertTrue(c.contains("UndineConfig.speech()"), "a server switch turns the lines off");
        assertTrue(c.contains("Cue.HUM"), "a group without a recorded line hums instead");
        assertFalse(c.contains("sendToAllPlayers") && !c.contains("sl.players()"), "never the whole server");
        String casting = code(read(COMMON.resolve("UndineCasting.java")));
        assertTrue(casting.contains("UndineMoodRules.decide("), "the mood comes from the pure rules");
        String events = code(read(COMMON.resolve("UndineEvents.java")));
        assertTrue(events.contains("LivingDamageEvent.Post") && events.contains("OWNER_KILL") && events.contains("ownerDied"), "hurt, kill and death are heard");
    }
}
