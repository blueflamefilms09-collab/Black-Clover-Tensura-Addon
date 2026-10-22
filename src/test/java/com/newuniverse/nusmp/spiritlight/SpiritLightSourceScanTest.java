package com.newuniverse.nusmp.spiritlight;

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
 * Reads the real sources of the Spirit of Light and fails when a rule of the doc (docs/magic/spirit_light.md) stops being true: the server decides and the client only draws, nothing scans the world on a
 * tick, every packet validates its sender and its data, no hunger or exhaustion is ever touched, no damage is a share of a target's health, nothing kills outright, Swiftness is a short ambient effect,
 * the beam goes through the Light Magic hit, the config is a server config with its own file name, nothing is allocated per frame in the draw code, and the voice limits are in place.
 */
class SpiritLightSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String code(Path p) throws IOException { return Files.readString(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    private static List<Path> common() throws IOException {
        try (Stream<Path> s = Files.list(SRC.resolve("spiritlight"))) { return s.filter(f -> f.toString().endsWith(".java")).sorted().toList(); }
    }

    @Test
    void theCommonCodeNeverTouchesAClientClass() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : common()) {
            String c = code(p);
            for (String b : new String[]{"net.minecraft.client", "com.mojang.blaze3d", "com.newuniverse.nusmp.client", "Minecraft.getInstance"}) if (c.contains(b)) bad.add(p.getFileName() + " uses " + b);
        }
        assertTrue(bad.isEmpty(), bad.toString());
        assertFalse(code(SRC.resolve("book/LightSpiritBridge.java")).contains("net.minecraft.client"));
    }

    @Test
    void theRulesClassesArePureJava() throws IOException {
        for (String f : new String[]{"SpiritLightRules.java", "SpiritLightLod.java", "SpiritLightLedger.java", "LightMoodRules.java", "SpiritLightVoiceLines.java"}) {
            String c = code(SRC.resolve("spiritlight/" + f));
            for (Matcher m = Pattern.compile("^import ([\\w.]+);", Pattern.MULTILINE).matcher(c); m.find(); ) assertTrue(m.group(1).startsWith("java."), f + " imports " + m.group(1) + ": it must stay pure so JUnit runs it");
        }
    }

    @Test
    void nothingOnATickOrInAnEventScansTheWorldAndTheOnlySearchesAreBehindTheirCooldowns() throws IOException {
        for (String f : new String[]{"SpiritLightEvents.java", "SpiritLightProps.java", "SpiritLightNet.java", "SpiritLightBoot.java"}) {
            String c = code(SRC.resolve("spiritlight/" + f));
            assertFalse(c.contains("getEntitiesOfClass") || c.contains("getEntities(") || c.contains("getBlockState") || c.contains("getChunk") || c.contains("clip("), f + " must not search the world");
        }
        String sl = code(SRC.resolve("spiritlight/SpiritLight.java"));
        Matcher m = Pattern.compile("getEntitiesOfClass\\(").matcher(sl);
        int n = 0;
        while (m.find()) n++;
        assertEquals(2, n, "exactly two box searches in SpiritLight: one per beam, one per pulse");
        int beam = sl.indexOf("static void fireBeam("), pulse = sl.indexOf("static void pulse("), upkeep = sl.indexOf("public static void upkeep(");
        assertTrue(beam > 0 && pulse > beam && upkeep > pulse);
        assertTrue(sl.substring(beam, pulse).contains("if (now < s.nextBeam) return;"), "the beam search is behind its cooldown");
        assertTrue(sl.substring(pulse, upkeep).contains("if (now < s.nextPulse) return;"), "the pulse search is behind its cooldown");
        assertFalse(sl.substring(upkeep).contains("getEntitiesOfClass"), "the upkeep never scans");
        String voice = code(SRC.resolve("spiritlight/SpiritLightVoice.java"));
        assertEquals(1, Pattern.compile("getEntitiesOfClass\\(").matcher(voice).results().count(), "one box search of players per spoken line");
    }

    @Test
    void thePlayerTickOnlyVisitsPlayersWithASpiritAndEveryCleanupHookIsThere() throws IOException {
        String ev = code(SRC.resolve("spiritlight/SpiritLightEvents.java"));
        assertTrue(ev.contains("SpiritLight.LIVE.isEmpty()") && ev.contains("(p.tickCount & 3) != 0") && ev.contains("SpiritLight.LIVE.contains(p.getUUID())"));
        for (String cleanup : new String[]{"PlayerLoggedOutEvent", "LivingDeathEvent", "PlayerRespawnEvent", "PlayerChangedDimensionEvent", "ServerStoppingEvent"}) assertTrue(ev.contains(cleanup), cleanup);
        assertTrue(ev.contains("LivingDamageEvent.Post") && ev.indexOf("SpiritLight.LIVE.isEmpty()", ev.indexOf("onHurt")) > 0, "the damage event looks at the live set first");
        String sl = code(SRC.resolve("spiritlight/SpiritLight.java"));
        assertTrue(sl.contains("removeSwiftness(p)") && sl.contains("SPIRITS.forget(id)") && sl.contains("POWERED.forget(id)") && sl.contains("EFFECTS.forget(id)"), "forget() drops everything");
    }

    @Test
    void everyPacketIsValidatedOnTheServer() throws IOException {
        String net = code(SRC.resolve("spiritlight/SpiritLightNet.java"));
        assertTrue(net.contains("ctx.player() instanceof ServerPlayer"), "the sender must be a real player");
        assertFalse(net.contains("playToServer"), "3.0.0: the key packet is gone, only the voice line travels (server to client)");
        assertTrue(net.contains("playToClient"), "the voice packet goes to the client");
        assertTrue(net.contains("readUtf(MAX_ID)") && net.contains("SpiritLightVoiceLines.has("), "the line id is a bounded string checked against the catalogue");
        assertTrue(net.contains(".optional()"), "the channel is optional: a client without the mod is never sent anything");
        String sl = code(SRC.resolve("spiritlight/SpiritLight.java"));
        assertTrue(sl.contains("PACKET_GAP") && sl.contains("eligible(p)") && sl.contains("SpiritLightConfig.enabled()"), "rate limit, Light Magic and the switch are checked");
        String client = code(SRC.resolve("client/spiritlight/SpiritLightClient.java"));
        assertFalse(client.contains("PacketDistributor") || client.contains("KeyMapping"), "3.0.0: the client class sends nothing and registers no key");
        assertFalse(client.contains("SpiritLight.") && client.contains("SpiritLight.on"), "the client never calls the server's rules");
    }

    @Test
    void theConfigIsAServerConfigWithItsOwnFileNameAndOnlyTurnsThingsDown() throws IOException {
        String boot = code(SRC.resolve("spiritlight/SpiritLightBoot.java"));
        assertTrue(boot.contains("ModConfig.Type.SERVER, SpiritLightConfig.SPEC, \"nusmp-spiritlight-server.toml\""));
        String cfg = code(SRC.resolve("spiritlight/SpiritLightConfig.java"));
        assertTrue(cfg.contains("b.push(\"magic\")") && cfg.contains(".define(\"spiritLight\", true)"), "magic.spiritLight, default true");
        assertTrue(cfg.contains("\"beamDamageScale\", 1.0, 0.0, 1.0"), "the damage scale can only go down");
    }

    @Test
    void noHungerNoPercentOfHealthNoInstantKillAndSwiftnessIsAShortAmbientEffect() throws IOException {
        for (Path p : common()) {
            String c = code(p);
            for (String banned : new String[]{"FoodData", "getFoodData", "causeFoodExhaustion", "setFoodLevel", "addExhaustion", "MobEffects.HUNGER", "MobEffects.SATURATION", "getMaxHealth() *", "* getMaxHealth()", "setHealth(0",
                    "kill()", "Integer.MAX_VALUE, 0", "genericKill", "DamageTypes.FELL_OUT_OF_WORLD"})
                assertFalse(c.contains(banned), p.getFileName() + " uses " + banned);
        }
        String sl = code(SRC.resolve("spiritlight/SpiritLight.java"));
        assertTrue(sl.contains("MobEffects.MOVEMENT_SPEED, SpiritLightRules.SWIFT_TICKS, SpiritLightRules.SWIFT_AMPLIFIER, true, false, false"), "ambient, no particles, no icon, short");
        assertTrue(sl.contains("MobEffects.GLOWING, ticks, 0, true, false, false"), "the glow is a short vanilla effect");
        String bridge = code(SRC.resolve("book/LightSpiritBridge.java"));
        assertTrue(bridge.contains("LightBook.lightHit("), "the beam hits like Light Magic does (armour ignored, the balance law and its caps)");
        assertTrue(sl.contains("LightSpiritBridge.hit("), "and the spirit only ever hurts through it");
        assertFalse(sl.contains(".hurt("), "no direct hurt call: every hit goes through the Light Magic path");
    }

    @Test
    void thePerFrameDrawCodeAllocatesNothingAndBuildsNoRenderTypeOfItsOwn() throws IOException {
        for (String f : new String[]{"client/prop/SpiritLightDraw.java", "client/prop/SpiritLightPropPainter.java"}) {
            String c = code(SRC.resolve(f)).replaceAll("(?m)^.*static final.*$", "");      // static tables are built once
            assertFalse(c.contains("new float[") || c.contains("new double[") || c.contains("new Vec3") || c.contains("new ArrayList") || c.contains("new HashMap"), f + " allocates per frame");
            for (String b : new String[]{"RenderType.eyes(", "RenderType.lightning(", "ADDITIVE_TRANSPARENCY", "ResourceLocation.fromNamespaceAndPath(\"nusmp\", \"textures"})
                if (!f.endsWith("SpiritLightDraw.java") || !b.startsWith("Resource")) assertFalse(c.contains(b), f + " uses " + b);
        }
        String painter = code(SRC.resolve("client/prop/SpiritLightPropPainter.java"));
        assertTrue(painter.contains("SpiritLightLod.fit(") && painter.contains("SpiritLightLod.level(") && painter.contains("SpiritLightLod.ownClose("), "the level of detail, the frame budget and the owner's exemption");
        assertTrue(painter.contains("LAST_MOTE[slot] == e.tickCount"), "motes once per game tick, not per frame");
        assertTrue(painter.contains("fitSmall("), "a beam and a ring go through the frame budget too");
        String draw = code(SRC.resolve("client/prop/SpiritLightDraw.java"));
        assertFalse(draw.contains("PropDraw.quad("), "no flat object cut-out: soft glows and rings through SealDraw only");
    }

    @Test
    void theVoiceIsLimitedPerListenerAndOnlyReachesPlayersNearby() throws IOException {
        String v = code(SRC.resolve("spiritlight/SpiritLightVoice.java"));
        assertTrue(v.contains("RANGE = 32.0") && v.contains("LISTENER_GAP = 80"), "32 blocks, one line every four seconds per listener");
        assertTrue(v.contains("LIMITER.tryAcquire(") && v.contains("VoiceConfig.maxPerSecond()") && v.contains("FakePlayer"), "the project's limiter, the voice.maxPerSecond option, fake players skipped");
        assertTrue(v.contains("sendToPlayer"), "to each listener in range, never a broadcast");
        assertFalse(v.contains("sendToAllPlayers") || v.contains("sendToPlayersNear"));
        assertTrue(v.contains("LAST_HEARD.remove(player)"), "state is dropped on logout");
    }

    @Test
    void theSoundsAreBudgetedAndPlayedForNearbyPlayersOnly() throws IOException {
        String s = code(SRC.resolve("spiritlight/SpiritLightSounds.java"));
        assertTrue(s.contains("PerfBudgets.effectPacket(") && s.contains("SpiritLightRules.gapOk("), "the effect packet budget and the per player gap");
        assertTrue(s.contains("sl.playSound(null,"), "the ordinary level sound: a packet to the players within hearing range only");
        String sl = code(SRC.resolve("spiritlight/SpiritLight.java"));
        assertTrue(sl.contains("VfxSpawn.send(") && !sl.contains("sendToAllPlayers"), "effects go through the effect packet budget");
    }

    @Test
    void theHooksIntoSharedFilesAreAppendOnlyOneLiners() throws IOException {
        assertTrue(code(SRC.resolve("NUSMP.java")).contains("com.newuniverse.nusmp.spiritlight.SpiritLightBoot.init(modEventBus, modContainer);"));
        assertTrue(code(SRC.resolve("NUSMP.java")).contains("com.newuniverse.nusmp.client.spiritlight.SpiritLightClient.init(modEventBus);"));
        assertTrue(code(SRC.resolve("prop/PropRegistry.java")).contains("SpiritLightProps.init();"));
        assertTrue(code(SRC.resolve("client/prop/PropPainterRegistry.java")).contains("SpiritLightPropPainter.register();"));
        String kinds = code(SRC.resolve("prop/PropKind.java"));
        assertTrue(kinds.contains("SPIRIT_LIGHT_1") && kinds.contains("SPIRIT_LIGHT_2") && kinds.indexOf("FIRECAST_2") < kinds.indexOf("SPIRIT_LIGHT_1"), "appended at the end, nothing before it moved");
    }
}
