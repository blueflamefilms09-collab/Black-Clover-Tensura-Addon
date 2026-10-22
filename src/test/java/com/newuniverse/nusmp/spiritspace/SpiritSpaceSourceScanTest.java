package com.newuniverse.nusmp.spiritspace;

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
 * Reads the real sources of the Spirit of Space and fails when a rule of the doc (docs/magic/spirit_space.md) stops being true: the server decides and the client only draws, nothing scans the world on a
 * tick, every entity scan is capped and happens once per cast, blocks are read only from loaded chunks and never changed, no hunger, no percent-of-max-health damage, no instant kill, every packet is
 * validated, the config is a server config with its own file name and no long-range switch, and nothing is allocated per frame in the draw code.
 */
class SpiritSpaceSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String code(Path p) throws IOException { return Files.readString(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    private static String code(String rel) throws IOException { return code(SRC.resolve(rel)); }

    private static List<Path> common() throws IOException {
        try (Stream<Path> s = Files.list(SRC.resolve("spiritspace"))) { return s.filter(f -> f.toString().endsWith(".java")).sorted().toList(); }
    }

    @Test
    void theCommonCodeNeverTouchesAClientClass() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : common()) {
            String c = code(p);
            for (String b : new String[]{"net.minecraft.client", "com.mojang.blaze3d", "com.newuniverse.nusmp.client", "Minecraft.getInstance", "net.neoforged.neoforge.client"}) if (c.contains(b)) bad.add(p.getFileName() + " uses " + b);
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void thePureClassesImportNoGameClassAtAll() throws IOException {
        for (String f : new String[]{"SpiritSpaceRules", "SpaceMoodRules", "SpaceLanding", "Waypoints", "SpaceLedger", "SoundGate", "SpiritSpaceMesh", "SpiritSpaceLod"}) {
            String c = code("spiritspace/" + f + ".java");
            assertFalse(Pattern.compile("import (net\\.minecraft|net\\.neoforged|com\\.mojang)").matcher(c).find(), f + " must stay pure so a unit test can run it");
        }
    }

    @Test
    void entityScansHappenOnlyOnACastAreCappedAndNeverOnATick() throws IOException {
        for (Path p : common()) {
            String name = p.getFileName().toString(), c = code(p);
            boolean scans = c.contains("getEntitiesOfClass") || c.contains("getEntities(") || c.contains("getNearbyEntities") || c.contains("getAllEntities");
            if (name.equals("SpiritSpace.java")) continue;
            assertFalse(scans, name + " must not search the world");
        }
        String s = code("spiritspace/SpiritSpace.java");
        assertEquals(2, Pattern.compile("getEntitiesOfClass\\(").matcher(s).results().count(), "exactly two searches: the gravity pulse and the cut");
        assertEquals(2, Pattern.compile("Math\\.min\\(found\\.size\\(\\), SCAN_CAP\\)").matcher(s).results().count(), "both look at most SCAN_CAP entities");
        assertTrue(s.contains("private static int lift(") && s.contains("private static int deleteProjectiles("));
        int upkeep = s.indexOf("private static void upkeep0("), end = s.indexOf("public static void onHurt(");
        assertTrue(upkeep > 0 && end > upkeep);
        assertFalse(s.substring(upkeep, end).contains("getEntitiesOfClass"), "the upkeep never scans");
        assertTrue(s.contains("SpiritSpaceRules.nearest(") && s.contains("SpiritSpaceConfig.cutProjectiles()") && s.contains("SpiritSpaceConfig.gravityTargets()"), "the caps come from the config, clamped");
    }

    @Test
    void blocksAreOnlyReadFromLoadedChunksAndNeverChanged() throws IOException {
        for (Path p : common()) {
            String name = p.getFileName().toString(), c = code(p);
            assertFalse(Pattern.compile("setBlock|destroyBlock|removeBlock|setBlockAndUpdate|\\.explode\\(|Explosion|BlockEntity|tempBlock|fillBlocks").matcher(c).find(), name + " must not change blocks");
            if (!name.equals("LoadedProbe.java")) assertFalse(c.contains("getBlockState") || c.contains("getChunk("), name + " must not read blocks or load chunks");
        }
        String probe = code("spiritspace/LoadedProbe.java");
        assertTrue(probe.contains("getChunkNow(") && !probe.contains("getChunk("), "the probe never loads a chunk");
        String s = code("spiritspace/SpiritSpace.java");
        assertTrue(s.contains("sl.isLoaded(") && s.contains("new LoadedProbe(sl)"), "the portal checks that the far side is loaded and finds a landing in loaded chunks");
    }

    @Test
    void noHungerNoPercentOfMaxHealthDamageNoInstantKillNoCrossDimensionTravel() throws IOException {
        for (Path p : common()) {
            String name = p.getFileName().toString(), c = code(p);
            assertFalse(Pattern.compile("causeFoodExhaustion|FoodData|setFoodLevel|setSaturation|exhaustion|MobEffects\\.HUNGER").matcher(c).find(), name + " must not touch hunger");
            assertFalse(Pattern.compile("getMaxHealth\\(\\)\\s*\\*|\\*\\s*[a-zA-Z.()]*getMaxHealth|\\.kill\\(\\)|setHealth\\(0|\\.setHealth\\(|Float\\.MAX_VALUE|DamageTypes\\.GENERIC_KILL|genericKill").matcher(c).find(), name + " must not do percent or instant damage");
            assertFalse(c.contains("changeDimension") || c.contains("DimensionTransition") || c.contains("teleportTo(server") || (c.contains("getLevel(ResourceKey") && !name.equals("WaypointStore.java")), name + " must not cross dimensions (only the store looks a level up, to check that a waypoint's dimension still exists)");
        }
        String s = code("spiritspace/SpiritSpace.java");
        assertTrue(s.contains("BalanceLaw.cap(target, (float) dmg)") && s.contains("SpiritSpaceRules.cutDamage("), "the cut goes through the balance law's caps");
        assertTrue(s.contains("BalanceLaw.beginControl(p)") && s.contains("BalanceLaw.controlTicks("), "the gravity pulse goes through the hard-control cap");
        assertTrue(s.contains("Waypoints.checkTrip(") && s.contains("WaypointStore.dimensionId(sl)"), "a trip is checked against the dimension the player stands in");
    }

    @Test
    void thePlayerTickOnlyVisitsPlayersWithSomethingLiveAndEveryCleanupEventIsListened() throws IOException {
        String ev = code("spiritspace/SpiritSpaceEvents.java");
        assertTrue(ev.contains("SpiritSpace.LIVE.isEmpty()") && ev.contains("(p.tickCount & 3) != 0") && ev.contains("SpiritSpace.LIVE.contains(p.getUUID())"));
        for (String cleanup : new String[]{"PlayerLoggedOutEvent", "LivingDeathEvent", "PlayerRespawnEvent", "PlayerChangedDimensionEvent", "ServerStoppingEvent", "PlayerEvent.Clone"}) assertTrue(ev.contains(cleanup), cleanup);
        String s = code("spiritspace/SpiritSpace.java");
        assertTrue(s.contains("PerfProbe.section(\"s.spirit_space.upkeep\")"), "the upkeep is measured by the probe");
        assertTrue(s.contains("STATES.remove(id)") && s.contains("SPIRITS.forget(id)") && s.contains("WISPS.forget(id)") && s.contains("PAIRS.forget(id)") && s.contains("SpiritSpaceVoice.forgetListener(id)"), "forget drops the state, the ledgers and the listener memory");
    }

    @Test
    void everyPacketIsValidatedOnTheServerAndNothingIsSentBack() throws IOException {
        String net = code("spiritspace/SpiritSpaceNet.java");
        assertTrue(net.contains("ctx.player() instanceof ServerPlayer"), "the sender must be a real player");
        assertTrue(net.contains("validAction(action)") && net.contains("validAbility(ability)") && net.contains("validSlot(slot"), "action, ability and slot are range checked");
        assertTrue(net.contains("playToServer") && !net.contains("playToClient") && !net.contains("sendToPlayer"), "the one packet goes client to server");
        String s = code("spiritspace/SpiritSpace.java");
        assertTrue(s.contains("PACKET_GAP") && s.contains("allowed(p)"), "rate limit and permission are checked");
        String client = code("client/spiritspace/SpiritSpaceClient.java");
        assertFalse(client.contains("SpiritSpace."), "the client never calls the server's rules");
        assertTrue(client.contains("PacketDistributor.sendToServer(new SpiritSpaceNet.Key("));
    }

    @Test
    void theConfigIsAServerConfigWithItsOwnFileNameAndHasNoLongRangeSwitch() throws IOException {
        String boot = code("spiritspace/SpiritSpaceBoot.java");
        assertTrue(boot.contains("ModConfig.Type.SERVER, SpiritSpaceConfig.SPEC, \"nusmp-spiritspace-server.toml\""));
        assertFalse(boot.contains("ModConfig.Type.CLIENT"));
        String cfg = code("spiritspace/SpiritSpaceConfig.java");
        assertTrue(cfg.contains("b.push(\"magic\")") && cfg.contains(".define(\"spiritSpace\", true)"), "magic.spiritSpace, default true");
        assertFalse(cfg.contains("longRange") || cfg.contains("crossDimension") || cfg.contains("unbounded"), "server-wide and cross-dimension travel are not built");
        assertTrue(cfg.contains("RANGE_HARD") && cfg.contains("SpiritSpaceRules.clamp("), "every value is clamped to its hard cap when it is read");
    }

    @Test
    void theVoiceIsSentOnlyToListenersWithinRangeAndAtMostOnePerThreeSeconds() throws IOException {
        String v = code("spiritspace/SpiritSpaceVoice.java");
        assertTrue(v.contains("SpaceMoodRules.hears(") && v.contains("SpiritSpaceSounds.GATE.allow(") && v.contains("tracker.voiceReady("), "the listener rule, the shared gate and the owner's gap");
        assertTrue(v.contains("pl.connection == null") && v.contains("sl.players()"), "only the level's players, never a world scan");
        String sounds = code("spiritspace/SpiritSpaceSounds.java");
        assertTrue(sounds.contains("GATE.allow(owner.getUUID(), k, sl.getGameTime())"), "every ability sound passes the gate");
        String s = code("spiritspace/SpiritSpace.java");
        assertTrue(s.contains("SpiritSpaceVoice.say("));
    }

    @Test
    void theDrawCodeAllocatesNothingPerFrameAndBuildsNoRenderTypeOfItsOwn() throws IOException {
        for (String f : new String[]{"client/prop/SpiritSpaceDraw.java", "client/prop/SpiritSpacePropPainter.java"}) {
            String c = code(f).replaceAll("(?m)^.*static final.*$", "");           // static tables are built once
            assertFalse(Pattern.compile("new float\\[|new double\\[|new int\\[|new Vec3|new ArrayList|new HashMap|new Quaternionf|new Vector3f|Axis\\.[XYZ]P\\.rotation|WaterPropDraw\\.lazy|new LazyBuffers").matcher(c).find(), f + " allocates per frame");
            for (String b : new String[]{"RenderType.eyes(", "RenderType.lightning(", "ADDITIVE_TRANSPARENCY", "RenderType.energySwirl(", "ResourceLocation.fromNamespaceAndPath(\"nusmp\", \"textures"})
                assertFalse(c.contains(b) && !(f.endsWith("Draw.java") && b.startsWith("Resource")), f + " uses " + b);
            assertFalse(c.contains("PropDraw.quad("), f + " draws no single flat quad of its own");
        }
        String draw = code("client/prop/SpiritSpaceDraw.java");
        assertTrue(draw.contains("SafeAdditive.additive(") && draw.contains("SafeAdditive.emissive("), "every glow pass goes through the safe helper");
        String painter = code("client/prop/SpiritSpacePropPainter.java");
        assertTrue(painter.contains("SpiritSpaceLod.fit(") && painter.contains("SpiritSpaceLod.level(") && painter.contains("BUDGET.take("), "the level of detail and the frame budget");
        assertTrue(painter.contains("LAST_MOTE[slot] == e.tickCount"), "motes once per game tick, not per frame");
        assertTrue(painter.contains("PORTAL_RANGE"), "rings are culled by distance");
    }

    @Test
    void theSharedFilesWereOnlyAppendedToAndTheTimeMagicFilesAreUntouched() throws IOException {
        assertTrue(code("prop/PropKind.java").contains("FIRECAST_2,\n") || code("prop/PropKind.java").contains("FIRECAST_2"), "the older kinds are still there");
        String kinds = code("prop/PropKind.java");
        assertTrue(kinds.indexOf("FIRECAST_2") < kinds.indexOf("SPIRITSPACE_1") && kinds.indexOf("SPIRITSPACE_1") < kinds.indexOf("SPIRITSPACE_2"), "new kinds only at the end");
        assertTrue(code("prop/PropRegistry.java").contains("SpiritSpaceProps.init()"));
        assertTrue(code("client/prop/PropPainterRegistry.java").contains("SpiritSpacePropPainter.register()"));
        assertTrue(code("NUSMP.java").contains("SpiritSpaceBoot.init(modEventBus, modContainer)") && code("NUSMP.java").contains("SpiritSpaceClient.init(modEventBus)"));
        assertTrue(code("book/SpellRuntime.java").contains("public static int dissolveBolts(ServerLevel level, Vec3 at, double r, LivingEntity keeper) {"), "the old overload is kept");
        String time = Files.readString(SRC.resolve("vfx/client/layer/TimeMagicLayer.java"));
        assertFalse(time.contains("spiritspace") || time.contains("SpiritSpace"), "the Time Magic layer knows nothing of the spirit");
    }
}
