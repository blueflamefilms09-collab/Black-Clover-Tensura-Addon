package com.newuniverse.nusmp.sylph;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reads the real sources of the Sylph and fails when a rule of the doc (docs/magic/sylph.md) stops being true: the server decides and the client only draws, nothing scans the world on a tick, every
 * packet validates its sender and its data, sounds go to nearby players through a limiter, the cast path of the grimoire still calls the hook, the old pages are untouched, nothing is allocated
 * per frame in the draw code, and the drawn sizes match the numbers the asset test checks.
 */
class SylphSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String code(Path p) throws IOException { return Files.readString(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    private static List<Path> common() throws IOException {
        try (Stream<Path> s = Files.list(SRC.resolve("sylph"))) { return s.filter(f -> f.toString().endsWith(".java")).sorted().toList(); }
    }

    @Test
    void theCommonCodeNeverTouchesAClientClass() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : common()) {
            String c = code(p);
            for (String b : new String[]{"net.minecraft.client", "com.mojang.blaze3d", "com.newuniverse.nusmp.client", "Minecraft.getInstance"}) if (c.contains(b)) bad.add(p.getFileName() + " uses " + b);
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void nothingOnATickOrInAnEventScansTheWorld() throws IOException {
        for (Path p : common()) {
            String c = code(p);
            assertFalse(c.contains("getEntitiesOfClass") || c.contains("getEntities(") || c.contains("GrimoireBook.around") || c.contains("GrimoireBook.along") || c.contains("getBlockState")
                    || c.contains("getChunk"), p.getFileName() + " must not search the world");
        }
    }

    @Test
    void thePlayerTickOnlyVisitsPlayersWithSomethingLive() throws IOException {
        String ev = code(SRC.resolve("sylph/SylphEvents.java"));
        assertTrue(ev.contains("SylphCasting.LIVE.isEmpty()") && ev.contains("(p.tickCount & 3) != 0") && ev.contains("SylphCasting.LIVE.contains(p.getUUID())"));
        for (String cleanup : new String[]{"PlayerLoggedOutEvent", "LivingDeathEvent", "PlayerRespawnEvent", "PlayerChangedDimensionEvent", "ServerStoppingEvent"}) assertTrue(ev.contains(cleanup), cleanup);
    }

    @Test
    void everyPacketIsValidatedOnTheServer() throws IOException {
        assertFalse(SRC.resolve("sylph/SylphNet.java").toFile().exists(), "3.0.0: the key packet is gone (the spirit control plane calls her; spiritcontrol validates every ask)");
        assertTrue(code(SRC.resolve("sylph/SylphSpirit.java")).contains("SylphCasting.applies(p)"), "the control plane handle asks the same rule: the canon book and the switch");
        String sc = code(SRC.resolve("sylph/SylphCasting.java"));
        assertTrue(sc.contains("PACKET_GAP") && sc.contains("CharacterSpells.characterOf(p) == CanonBook.YUNO") && sc.contains("SylphConfig.enabled()"), "rate limit, character and switch are checked");
    }

    @Test
    void theConfigIsAServerConfigWithItsOwnFileName() throws IOException {
        String boot = code(SRC.resolve("sylph/SylphBoot.java"));
        assertTrue(boot.contains("ModConfig.Type.SERVER, SylphConfig.SPEC, \"nusmp-sylph-server.toml\""));
        String cfg = code(SRC.resolve("sylph/SylphConfig.java"));
        assertTrue(cfg.contains("b.push(\"magic\")") && cfg.contains(".define(\"sylph\", true)"), "magic.sylph, default true");
        assertTrue(boot.contains("SylphSounds.SOUNDS.register(modBus)"), "her sound events are registered");
    }

    @Test
    void theSoundsGoToNearbyPlayersThroughTheLimiterOnly() throws IOException {
        String s = code(SRC.resolve("sylph/SylphSounds.java"));
        assertTrue(s.contains("SOUND_RADIUS") && s.contains("LIMITER.tryAcquire(") && s.contains("p.connection.send("), "radius, limiter and one packet per allowed listener");
        assertFalse(s.contains("level.playSound(") && !s.contains("LIMITER"), "no broadcast that skips the limiter");
        assertTrue(s.contains("level.players()"), "only the players of this level are asked");
        String c = code(SRC.resolve("sylph/SylphCasting.java"));
        assertFalse(c.contains("playSound("), "the casting plays sounds through SylphSounds only");
    }

    @Test
    void theGrimoireCastPathStillCallsTheHookAndTheOldPagesAreUntouched() throws IOException {
        String gb = code(SRC.resolve("book/GrimoireBook.java"));
        assertTrue(gb.contains("SylphCasting.run(this, p, player, mode,"), "the cast path hands the cast to the Sylph hook");
        assertTrue(gb.contains("SylphCasting.origin(p, ") && gb.contains("FireCasting.origin(p, from)"), "the spell VFX start (the Sylph wraps the other casters' origins)");
        assertTrue(gb.contains("UndineCasting.run(this, p, instance, player, mode)") && code(SRC.resolve("undine/UndineCasting.java")).contains("FireCasting.run(book, page, inst, p, mode)"), "and the Undine hook (which falls through to the fire casting hook) is still the inner cast");
        assertTrue(code(SRC.resolve("book/SpellRuntime.java")).contains("SylphCasting.origin(owner,"));
        assertTrue(code(SRC.resolve("book/TensuraShots.java")).contains("SylphCasting.origin(p,"));
        String wind = code(SRC.resolve("book/WindBook.java"));
        int last = -1;
        for (String id : new String[]{"kamaitachi", "gust_lane", "spirit_storm", "towering_tornado", "slicing_wind_emperor", "spirit_of_zephyr"}) {
            int at = wind.indexOf("\"" + id + "\"");
            assertTrue(at > last, "page " + id + " is still there, in its place");
            last = at;
        }
        assertTrue(code(SRC.resolve("book/CharacterSpells.java")).contains("lock(\"book_wind\", \"spirit_of_zephyr\", CanonBook.YUNO)"), "the character lock is untouched");
        assertTrue(wind.contains("static boolean zephyr(") && wind.contains("WIND_SPIRIT_DIVE"), "the Zephyr page and its Spirit Dive armour are untouched");
    }

    @Test
    void theStyleAppliesOnlyToTheWindBookAndItsFamilyPagesForYuno() throws IOException {
        String sc = code(SRC.resolve("sylph/SylphCasting.java"));
        assertTrue(sc.contains("!(book instanceof WindBook)") && sc.contains("mode >= book.familyCount()"), "only the Wind family pages, never Summon / Store / Mana Strike");
        assertTrue(sc.contains("static boolean applies(ServerPlayer p) { return SylphConfig.enabled() && CharacterSpells.characterOf(p) == CanonBook.YUNO; }"));
    }

    @Test
    void theExistingEntityIdsAndTheOldLooksAreKept() throws IOException {
        String ent = code(SRC.resolve("entity/ElementalSpiritLordEntity.java"));
        assertTrue(ent.contains("SYLPH(\"Sylph\", \"Sylph\""), "the Sylph kind of the exclusive Spirit Lord stays");
        String r = code(SRC.resolve("client/ElementalSpiritRenderer.java"));
        assertTrue(r.contains("SylphConfig.enabled()") && r.contains("GreatSpiritModels.sylph("), "the switch falls back to the older fairy, which is still drawn");
        String kinds = code(SRC.resolve("prop/PropKind.java"));
        assertTrue(kinds.indexOf("FIRECAST_2") < kinds.indexOf("SYLPH_1"), "the new prop kind is appended at the end");
        assertTrue(Files.exists(Path.of("src/main/resources/assets/nusmp/geo/entity/spirit_sylph_fairy.geo.json")) && Files.exists(Path.of("src/main/resources/assets/nusmp/geo/entity/spirit_sylph.geo.json")), "the older models stay");
    }

    @Test
    void theDrawCodeAllocatesNothingPerFrameAndBuildsNoRenderTypeOfItsOwn() throws IOException {
        for (String f : new String[]{"client/prop/SylphDraw.java", "client/prop/SylphPropPainter.java", "client/prop/SylphView.java", "client/prop/FlatLit.java", "client/prop/SheenUv.java"}) {
            String c = code(SRC.resolve(f)).replaceAll("(?m)^.*static final.*$", "");      // static tables are built once
            assertFalse(c.contains("new float[") || c.contains("new double[") || c.contains("new Vec3") || c.contains("new ArrayList") || c.contains("new HashMap"), f + " allocates per frame");
            for (String b : new String[]{"RenderType.eyes(", "RenderType.lightning(", "ADDITIVE_TRANSPARENCY"}) assertFalse(c.contains(b), f + " uses " + b);
        }
        String painter = code(SRC.resolve("client/prop/SylphPropPainter.java"));
        assertTrue(painter.contains("SylphView.lod("), "the level of detail and the frame budget");
        assertTrue(code(SRC.resolve("client/prop/SylphView.java")).contains("SylphLod.fit(") && code(SRC.resolve("client/prop/SylphView.java")).contains("SylphLod.level("));
        assertTrue(code(SRC.resolve("client/ElementalSpiritRenderer.java")).contains("SylphView.lod("), "the Spirit Lord body goes through the same shared budget");
    }

    @Test
    void theWingSheenIsDrawnOnlyThroughSafeAdditiveAndShiftsVNotModels() throws IOException {
        String d = code(SRC.resolve("client/prop/SylphDraw.java"));
        assertTrue(d.contains("SafeAdditive.additive(wings.glow())"), "the sheen pass asks the safe additive type for the sheet");
        assertFalse(d.contains("AuraRender.additive(wings"), "no other additive route for the wings");
        assertTrue(d.contains("SheenUv.over(") && d.contains("SylphLod.SHEEN_PHASES") && d.contains("SylphLod.SHEEN_TICKS_PER_PHASE"), "the phase comes from the clock through the shared constants");
        assertTrue(d.contains("if (lod >= SylphLod.LOW) return;"), "the lowest level draws no sheen");
        String u = code(SRC.resolve("client/prop/SheenUv.java"));
        assertTrue(u.contains("(v + k) * inv"), "V moves into the phase");
        assertFalse(u.contains("RenderType") || u.contains("Minecraft.getInstance"), "the wrapper only moves V");
    }

    @Test
    void theDrawnSizesAreTheOnesTheAssetTestChecks() throws IOException {
        String d = code(SRC.resolve("client/prop/SylphDraw.java"));
        assertTrue(d.contains("SMALL_SCALE = 0.24f") && d.contains("POWERED_SCALE = 0.36f"), "SylphAssetsTest.theSmallFormIsSmallAndThePoweredFormBig uses these two numbers");
    }

    @Test
    void theKeyIsGoneAndTheClientHasNothingToAsk() throws IOException {
        assertFalse(SRC.resolve("client/sylph/SylphClient.java").toFile().exists(), "3.0.0: the Sylph key is gone, the client has nothing to ask");
    }
}
