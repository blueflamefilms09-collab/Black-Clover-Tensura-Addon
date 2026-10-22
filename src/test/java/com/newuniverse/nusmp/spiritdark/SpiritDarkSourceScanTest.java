package com.newuniverse.nusmp.spiritdark;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reads the real sources of the Spirit of Darkness and fails when a rule of the doc (docs/magic/spirit_dark.md) stops being true: the server decides and the client only draws, nothing scans the world
 * on a tick, every packet validates its sender and its data, no hunger code, nothing is allocated per frame in the draw code, the config has its own file name and the switch, the voice goes to nearby
 * players only through the listener gate, and the existing pages and the shared hooks are as they were.
 */
class SpiritDarkSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String code(Path p) throws IOException { return Files.readString(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    private static List<Path> common() throws IOException {
        try (Stream<Path> s = Files.list(SRC.resolve("spiritdark"))) { return s.filter(f -> f.toString().endsWith(".java")).sorted().toList(); }
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
    void nothingTouchesHungerOrTakesAPercentageOfMaxHealthOrKillsOutright() throws IOException {
        for (Path p : common()) {
            String c = code(p);
            assertFalse(c.contains("getFoodData") || c.contains("causeFoodExhaustion") || c.contains("setFoodLevel") || c.contains("FoodData") || c.contains("MobEffects.HUNGER") || c.contains("MobEffects.SATURATION"), p.getFileName() + " touches hunger");
            assertFalse(c.contains("getMaxHealth() *") || c.contains("* target.getMaxHealth()") || c.contains("setHealth(0") || c.contains(".kill()") || c.contains("genericKill"), p.getFileName() + " is a percentage or an instant kill");
        }
        assertFalse(code(SRC.resolve("spiritdark/SpiritDark.java")).contains("hurt("), "the spirit never deals damage of its own: it heals a capped amount and adds short status effects");
    }

    @Test
    void nothingOnATickOrInAnEventScansTheWorld() throws IOException {
        for (String f : new String[]{"SpiritDark.java", "SpiritDarkEvents.java", "SpiritDarkProps.java", "SpiritDarkNet.java"}) {
            String c = code(SRC.resolve("spiritdark/" + f));
            assertFalse(c.contains("getEntitiesOfClass") || c.contains("getEntities(") || c.contains("GrimoireBook.around") || c.contains("getBlockState") || c.contains("getChunk") || c.contains("getNearbyEntities"), f + " must not search the world");
        }
        String probe = code(SRC.resolve("spiritdark/ShadowProbe.java"));
        assertTrue(probe.contains("hasChunkAt") && !probe.contains("getChunk("), "the Shadow Step asks if a chunk is loaded, it never loads one");
    }

    @Test
    void thePlayerTickOnlyVisitsPlayersWithASpiritOutAndEveryCleanupEventIsListened() throws IOException {
        String ev = code(SRC.resolve("spiritdark/SpiritDarkEvents.java"));
        assertTrue(ev.contains("SpiritDark.LIVE.isEmpty()") && ev.contains("(p.tickCount & 3) != 0") && ev.contains("SpiritDark.LIVE.contains(p.getUUID())"));
        assertTrue(ev.contains("SpiritDark.STATES.isEmpty()"), "the damage and death handlers return at once when nobody has a spirit");
        for (String cleanup : new String[]{"PlayerLoggedOutEvent", "PlayerRespawnEvent", "PlayerChangedDimensionEvent", "ServerStoppingEvent", "LivingDeathEvent"}) assertTrue(ev.contains(cleanup), cleanup);
        String sd = code(SRC.resolve("spiritdark/SpiritDark.java"));
        assertTrue(sd.contains("static void forget(ServerPlayer p)") || sd.contains("public static void forget(ServerPlayer p)"));
        assertTrue(sd.contains("SPIRITS.forget(p.getUUID())") && sd.contains("POWERED.forget(p.getUUID())") && sd.contains("SpiritDarkSounds.forget(p.getUUID())"), "forget frees the ledgers and the sound gates of the player");
    }

    @Test
    void everyPacketIsValidatedOnTheServer() throws IOException {
        assertFalse(SRC.resolve("spiritdark/SpiritDarkNet.java").toFile().exists(), "3.0.0: the key packet is gone (the spirit control plane calls the spirit; spiritcontrol validates every ask)");
        String sd = code(SRC.resolve("spiritdark/SpiritDark.java"));
        assertTrue(sd.contains("PACKET_GAP") && sd.contains("eligible(p)") && sd.contains("SpiritDarkConfig.enabled()"), "rate limit, Dark Magic and the switch are checked");
        assertTrue(sd.contains("NUSkills.BOOK_DARK"), "only a Dark Magic caster may link");
    }

    @Test
    void theShadowStepIsValidatedOnTheServerBeforeAnyMove() throws IOException {
        String sd = code(SRC.resolve("spiritdark/SpiritDark.java"));
        int step = sd.indexOf("private static void step(");
        int tp = sd.indexOf("p.teleportTo(", step);
        assertTrue(step > 0 && tp > step);
        String before = sd.substring(step, tp);
        for (String need : new String[]{"stepReadyAt", "isAlive()", "isPassenger()", "ShadowStep.pick(", "lastLook(", "stepCooldown("}) assertTrue(before.contains(need), "before the move: " + need);
        String last = sd.substring(sd.indexOf("private static boolean lastLook"));
        assertTrue(last.contains("getWorldBorder().isWithinBounds") && last.contains("noCollision("), "world border and the real hit box are checked");
        assertFalse(sd.contains("changeDimension") || sd.contains("ServerLevel.dimension") || sd.contains("teleportTo(sl"), "same dimension only");
    }

    @Test
    void theConfigIsAServerConfigWithItsOwnFileNameAndTheSwitch() throws IOException {
        String boot = code(SRC.resolve("spiritdark/SpiritDarkBoot.java"));
        assertTrue(boot.contains("ModConfig.Type.SERVER, SpiritDarkConfig.SPEC, \"nusmp-spiritdark-server.toml\""));
        String cfg = code(SRC.resolve("spiritdark/SpiritDarkConfig.java"));
        assertTrue(cfg.contains("b.push(\"magic\")") && cfg.contains(".define(\"spiritDark\", true)"), "magic.spiritDark, default true");
        assertTrue(code(SRC.resolve("spiritdark/SpiritDark.java")).contains("if (!SpiritDarkConfig.enabled()) return false;"), "the switch off: nobody is eligible");
    }

    @Test
    void theVoiceGoesToNearbyPlayersOnlyAndThroughTheListenerGate() throws IOException {
        String s = code(SRC.resolve("spiritdark/SpiritDarkSounds.java"));
        assertTrue(s.contains("LISTENERS.tryHear(") && s.contains("distanceToSqr(at) > r2") && s.contains("RANGE"), "range and the per listener gate");
        assertTrue(s.contains("GATE.tryPlay("), "the cues go through the per source gap and the per tick allowance");
        assertFalse(s.contains("PacketDistributor.sendToAllPlayers") || s.contains("getPlayerList().broadcast"), "never to everyone");
        assertTrue(code(SRC.resolve("spiritdark/SpiritDark.java")).contains("SpiritDarkConfig.voice()"), "the voice has its own switch");
    }

    @Test
    void theSpiritsAreCappedPerPlayerAndServerWide() throws IOException {
        String sd = code(SRC.resolve("spiritdark/SpiritDark.java"));
        assertTrue(sd.contains("SPIRITS.acquire(p.getUUID(), SpiritDarkRules.SPIRITS_PER_PLAYER, SpiritDarkConfig.maxSpirits())"));
        assertTrue(sd.contains("POWERED.acquire(p.getUUID(), SpiritDarkRules.POWERED_PER_PLAYER, SpiritDarkConfig.maxPowered())"));
        assertTrue(sd.contains("SpiritDarkRules.POWER_COOLDOWN"), "the demon has a cooldown");
    }

    @Test
    void theDrawCodeAllocatesNothingPerFrameAndBuildsNoRenderTypeOfItsOwn() throws IOException {
        for (String f : new String[]{"client/prop/SpiritDarkDraw.java", "client/prop/SpiritDarkPropPainter.java"}) {
            String c = code(SRC.resolve(f)).replaceAll("(?m)^.*static final.*$", "").replaceAll("(?s)static \\{.*?\\n    \\}", "").replaceAll("(?s)private static GeoSpec (lod|face)\\(.*?\\n    \\}", "");      // static tables, the static initialiser and the two spec builders it calls are built once
            assertFalse(c.contains("new float[") || c.contains("new double[") || c.contains("new Vec3") || c.contains("new ArrayList") || c.contains("new HashMap") || c.contains("LazyBuffers") || c.contains("lazy("), f + " allocates per frame");
            for (String b : new String[]{"RenderType.eyes(", "RenderType.lightning(", "ADDITIVE_TRANSPARENCY", "ResourceLocation.fromNamespaceAndPath"}) assertFalse(c.contains(b), f + " uses " + b);
        }
        String painter = code(SRC.resolve("client/prop/SpiritDarkPropPainter.java"));
        assertTrue(painter.contains("SpiritDarkLod.fit(") && painter.contains("SpiritDarkLod.level(") && painter.contains("SpiritDarkLod.ownerClose("), "the level of detail, the frame budget and the owner's protection");
        assertTrue(painter.contains("LAST_SMOKE[slot] == e.tickCount"), "smoke once per game tick, not per frame");
    }

    @Test
    void theKeysAreGoneAndTheClientHasNothingToAsk() throws IOException {
        assertFalse(SRC.resolve("client/spiritdark/SpiritDarkClient.java").toFile().exists(), "3.0.0: the three Darkness keys are gone, the client has nothing to ask");
    }

    @Test
    void theSharedHooksAreOneLinersAndTheDarkBookIsUntouched() throws IOException {
        assertTrue(code(SRC.resolve("book/GrimoireBook.java")).contains("com.newuniverse.nusmp.spiritdark.SpiritDark.onCast(player, castMagic)"));
        assertTrue(code(SRC.resolve("prop/PropRegistry.java")).contains("SpiritDarkProps.init()"));
        assertTrue(code(SRC.resolve("client/prop/PropPainterRegistry.java")).contains("SpiritDarkPropPainter.register()"));
        assertTrue(code(SRC.resolve("NUSMP.java")).contains("SpiritDarkBoot.init(modEventBus, modContainer)") && !code(SRC.resolve("NUSMP.java")).contains("SpiritDarkClient.init(modEventBus)"), "3.0.0: no client key setup any more");
        assertTrue(code(SRC.resolve("prop/PropKind.java")).contains("SPIRITDARK_1"));
        String dark = code(SRC.resolve("book/DarkBook.java"));
        int last = -1;
        for (String id : new String[]{"avidya_slash", "black_hole", "dimension_slash", "death_thrust", "black_blade", "avidya_wild_slash", "black_moon", "iai_slash", "dimension_slash_equinox", "dark_cloak", "shadow_embrace", "black_hole_collapse"}) {
            int at = dark.indexOf("\"" + id + "\"");
            assertTrue(at > last, "Dark page " + id + " is still there, in its place");
            last = at;
        }
        assertTrue(Files.exists(SRC.resolve("entity/ElementalSpiritLordEntity.java")) && Files.exists(SRC.resolve("entity/SpiritLordEntity.java")), "the older spirit entities are kept");
    }
}
