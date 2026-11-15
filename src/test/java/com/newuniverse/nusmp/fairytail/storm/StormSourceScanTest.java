package com.newuniverse.nusmp.fairytail.storm;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Nothing about Storm Magic can be played here, so the rules it was built to are pinned by reading the source: the server decides and the client draws (no client class in common code,
 * no static player, no new packet), custom particles only, no hunger, no vanilla lightning, no changed block, the Time Magic effects untouched, and NO LAG (CLAUDE.md rule 11): every
 * per-tick piece of work goes through a budget, the scan interval of the performance pass and a probe, and the render methods of the layer allocate nothing.
 */
class StormSourceScanTest {
    private static final Path SRC = Path.of("src", "main", "java", "com", "newuniverse", "nusmp");
    private static final Path STORM = SRC.resolve("fairytail/storm");

    private static String read(Path p) throws IOException { return Files.readString(p); }

    /** The code of a file without comments (a word in a comment never counts). */
    private static String code(String src) {
        return src.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?m)//.*$", " ");
    }

    private static List<Path> stormFiles() throws IOException {
        try (Stream<Path> s = Files.list(STORM)) {
            List<Path> l = new ArrayList<>();
            s.filter(p -> p.toString().endsWith(".java")).forEach(l::add);
            return l;
        }
    }

    private static List<Path> allStormCode() throws IOException {
        List<Path> l = stormFiles();
        l.add(SRC.resolve("vfx/client/layer/StormLayer.java"));
        l.add(SRC.resolve("client/storm/StormClientFx.java"));
        return l;
    }

    @Test
    void theCommonCodeTouchesNoClientClass() throws IOException {
        for (Path p : stormFiles()) {
            String c = code(read(p));
            for (String bad : new String[]{"net.minecraft.client", "com.mojang.blaze3d", "com.newuniverse.nusmp.client", "vfx.client", "Minecraft.getInstance"})
                assertFalse(c.contains(bad), p.getFileName() + " reaches " + bad + " from common code");
        }
    }

    @Test
    void thereIsNoStaticCurrentPlayerAndNoNewPacket() throws IOException {
        Pattern staticPlayer = Pattern.compile("static\\s+(?:final\\s+)?(?:volatile\\s+)?(?:Server)?(?:Player|LivingEntity)\\s+\\w+\\s*[;=]");
        for (Path p : allStormCode()) {
            String c = code(read(p));
            assertFalse(staticPlayer.matcher(c).find(), p.getFileName() + " keeps a static player");
            assertFalse(c.contains("CustomPacketPayload"), p.getFileName() + " defines a packet: storms travel as VfxPayload only (server to client, nearby players, perf budgeted)");
        }
    }

    @Test
    void particlesAreTheModsOwnAndNothingTouchesHungerBlocksOrLightningEntities() throws IOException {
        Pattern hunger = Pattern.compile("(?i)foodData|foodLevel|saturation|causeFoodExhaustion|addExhaustion");
        Pattern blocks = Pattern.compile("setBlock|destroyBlock|removeBlock|setBlockAndUpdate|explode\\(");
        for (Path p : allStormCode()) {
            String c = code(read(p));
            String n = p.getFileName().toString();
            assertFalse(hunger.matcher(c).find(), n + " touches the food bar");
            assertFalse(blocks.matcher(c).find(), n + " changes a block");
            assertFalse(c.contains("ParticleTypes.") || c.contains("DustParticleOptions"), n + " uses a vanilla particle");
            assertFalse(c.contains("TimeMagicLayer") || c.contains("time_") || c.contains("TimeBook"), n + " touches Time Magic");
            if (!n.equals("StormSelfTests.java")) assertFalse(c.contains("EntityType.LIGHTNING_BOLT") || c.contains("new LightningBolt"), n + " spawns a vanilla lightning bolt (fire, charged creepers)");
            if (!n.equals("StormClientFx.java")) assertFalse(c.contains("sendParticles"), n + " sends server particles");
        }
        String fx = code(read(SRC.resolve("client/storm/StormClientFx.java")));
        for (String id : new String[]{"droplet", "shard_ice", "water_splash", "leaf_green"}) assertTrue(fx.contains("\"" + id + "\""), id);
        assertTrue(fx.contains("FxConfig.clientCustom()") && fx.contains("PerfBudgets.clientParticles"), "custom particles only, through the client budget");
    }

    // ------------------------------------------------------------------------------------------------------------------ no lag
    @Test
    void everyPerTickPieceOfWorkIsBudgetedAndProbed() throws IOException {
        String f = read(STORM.resolve("StormField.java"));
        String c = code(f);
        assertTrue(c.contains("StormBudget.scan(") && c.contains("StormBudget.hail(") && c.contains("StormBudget.bolt("), "scans, hail volleys and bolts go through StormBudget");
        assertTrue(c.contains("PerfSettings.entityScanInterval()"), "the scan interval of the performance pass");
        assertTrue(c.contains("PerfProbe.section(\"s.storm.tick\")") && c.contains("PerfProbe.begin()") && c.contains("PerfProbe.end("), "the tick is probed");
        int at = c.indexOf("private static void onServerTick");
        int end = c.indexOf("private static boolean tick(", at);
        String body = c.substring(at, end);
        assertTrue(body.indexOf("LIVE.isEmpty()") >= 0 && body.indexOf("LIVE.isEmpty()") < body.indexOf("PerfProbe.begin()"), "no storm, no work: the empty check comes first");
        assertFalse(body.contains("new ArrayList"), "the tick copies no list");
        // hail: ONE entity query per volley, not one per stone, and no block search per stone
        int h = c.indexOf("private static void hail(");
        String hail = c.substring(h, c.indexOf("private static void bolts(", h));
        assertEquals(1, count(hail, "StormWorld.in("), "one scan serves a hail volley");
        assertFalse(hail.contains("groundY") || hail.contains("StormWorld.near("), "no query or block search per stone");
        // every area query is cut at MAX_FOES
        String w = code(read(STORM.resolve("StormWorld.java")));
        assertTrue(w.contains("StormMath.MAX_FOES"), "area queries are capped");
        // the entity queries of the whole system live in StormWorld only
        for (Path p : stormFiles()) if (!p.getFileName().toString().equals("StormWorld.java")) assertFalse(code(read(p)).contains("getEntitiesOfClass"), p.getFileName() + " queries entities itself");
    }

    @Test
    void theLayerDrawsWithoutAllocatingAndWithinTheVertexBudget() throws IOException {
        String l = code(read(SRC.resolve("vfx/client/layer/StormLayer.java")));
        String marker = "helpers (no allocation)";
        String src = read(SRC.resolve("vfx/client/layer/StormLayer.java"));
        int at = src.indexOf(marker);
        assertTrue(at > 0, "the marker of the render half");
        String render = code(src.substring(at));
        assertFalse(Pattern.compile("\\bnew\\s").matcher(render).find(), "StormLayer allocates in a render method");
        assertFalse(render.contains("Vec3.") && render.contains("rel("), "no camera-relative vector per point");
        assertTrue(l.contains("ctx.seg("), "every segment count is thinned by quality and distance");
        assertFalse(l.contains("ParticleTypes") || l.contains("RenderType"), "the layer uses the vertex buffer only");
        assertTrue(l.contains("StormMath.boltPath(inst.seed, frame, a.x, a.y, a.z"), "bolt paths are written into kept buffers");
    }

    @Test
    void theClientParticlesAreBudgetedDistanceCulledAndCached() throws IOException {
        String fx = code(read(SRC.resolve("client/storm/StormClientFx.java")));
        assertTrue(fx.contains("EMITTERS_PER_TICK") && fx.contains("PER_STORM"), "a cap on storms and spawns per tick");
        assertTrue(fx.contains("NEAR * NEAR"), "no spawn from a far storm");
        assertTrue(fx.contains("PerfSettings.vfxQuality().detail"), "thinned by the quality level");
        assertFalse(Pattern.compile("emit\\(level, opts\\(").matcher(fx).find(), "particle options are cached, not built per spawn");
    }

    @Test
    void thePacketsAreFewAndGoThroughTheEffectBudget() throws IOException {
        String fx = code(read(STORM.resolve("StormFx.java")));
        assertTrue(fx.contains("VfxSpawn.send("), "nearby players only, counted by the per-tick effect-packet budget");
        String field = code(read(STORM.resolve("StormField.java")));
        assertEquals(1, count(field, "StormFx.cell("), "a cast sends the cloud and what falls once");
        assertEquals(1, count(field, "StormFx.front("));
        assertEquals(1, count(field, "StormFx.bolt("), "a bolt is one packet, and bolts are budgeted");
    }

    // ------------------------------------------------------------------------------------------------------------------ the rest of the owner's rules
    @Test
    void theConfigIsAServerConfigWithStaffOnlyOn() throws IOException {
        String c = code(read(STORM.resolve("StormConfig.java")));
        assertTrue(c.contains("DEFAULT_STAFF_ONLY = true") && c.contains("define(\"staffOnly\""), "staffOnly defaults to true");
        assertTrue(code(read(STORM.resolve("StormBoot.java"))).contains("ModConfig.Type.SERVER, StormConfig.SPEC, \"nusmp-storm-server.toml\""), "a SERVER config: it changes the game");
        assertFalse(c.contains("Type.CLIENT"), "nothing about looks is configured here");
    }

    @Test
    void theOldGrimoireStaysRegisteredAndOnlyTheRollsAreGated() throws IOException {
        for (String f : new String[]{"blackclover/MagicType.java", "wizard/MagicFlavors.java", "blackclover/GrimoireAcceptance.java"})
            assertTrue(read(SRC.resolve(f)).contains("StormGate."), f + " asks the gate");
        assertTrue(read(SRC.resolve("blackclover/MagicType.java")).contains("STORM(\"Storm Magic\""), "the enum constant stays");
        assertTrue(read(SRC.resolve("skill/NUSkills.java")).contains("book(\"book_storm\""), "the book stays registered");
        assertTrue(read(SRC.resolve("grimoire/CanonBook.java")).contains("KAISER(\"Kaiser Granvorka\", \"THREE_LEAF\", \"STORM\""), "the Kaiser captain book stays");
        assertFalse(read(SRC.resolve("core/magic/profile/MagicKeys.java")).contains("\"STORM\""), "the static restricted list is untouched (the gate is a server config)");
        assertTrue(read(SRC.resolve("NUCreativeTab.java")).contains("for (MagicType m : MagicType.values())"), "the creative tab still lists every grimoire");
    }

    @Test
    void theWiringIsOneLineEach() throws IOException {
        assertTrue(read(SRC.resolve("NUSMP.java")).contains("StormBoot.init(modEventBus, modContainer)"));
        assertTrue(read(SRC.resolve("NUCreativeTab.java")).contains("StormBoot.STAFF.get()"));
        assertTrue(read(SRC.resolve("client/WeaponRenderer.java")).contains("StormBoot.renderables()"));
        assertTrue(read(SRC.resolve("vfx/client/VfxManager.java")).contains("new com.newuniverse.nusmp.vfx.client.layer.StormLayer()"));
        assertTrue(read(SRC.resolve("item/WeaponEngravings.java")).contains("Map.entry(\"stormcaller_staff\""), "Tensura engravings (rule 6)");
        assertTrue(code(read(STORM.resolve("StormSelfTests.java"))).contains("SelfTests.register(\"magic.storm_staff\""));
    }

    @Test
    void theNewShapesAreAppendedAfterTheOldOnes() throws IOException {
        String s = code(read(SRC.resolve("vfx/VfxShape.java")));
        int deck = s.indexOf("DECK_SUMMON");
        assertTrue(deck > 0);
        for (String n : new String[]{"STORM_CLOUD", "STORM_FALL", "STORM_BOLT", "STORM_FRONT", "STORM_CANCEL"}) assertTrue(s.indexOf(n) > deck, n + " must come after DECK_SUMMON (ordinals are sent over the network)");
    }

    private static int count(String s, String what) {
        int n = 0, i = 0;
        while ((i = s.indexOf(what, i)) >= 0) {
            n++;
            i += what.length();
        }
        return n;
    }
}
