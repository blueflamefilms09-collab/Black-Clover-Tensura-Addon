package com.newuniverse.nusmp.firecast;

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
 * Reads the real sources of the fire casting styles and fails when a rule of the doc (docs/magic/fire_casting.md) stops being true: the server decides and the client only draws, nothing scans the
 * world on a tick, every packet validates its sender and its data, the cast path of the grimoire still calls the hooks, the old pages are untouched, nothing is allocated per frame in the draw code.
 */
class FireCastSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String code(Path p) throws IOException { return Files.readString(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    private static List<Path> common() throws IOException {
        try (Stream<Path> s = Files.list(SRC.resolve("firecast"))) { return s.filter(f -> f.toString().endsWith(".java")).sorted().toList(); }
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
        for (String f : new String[]{"FireCasting.java", "FireCastEvents.java", "FireCastProps.java", "Salamander.java", "FireCastNet.java"}) {
            String c = code(SRC.resolve("firecast/" + f));
            assertFalse(c.contains("getEntitiesOfClass") || c.contains("getEntities(") || c.contains("GrimoireBook.around") || c.contains("GrimoireBook.along") || c.contains("getBlockState")
                    || c.contains("getChunk"), f + " must not search the world");
        }
        // the only entity searches of Magna Swing are on the contact tick of a swing (once per cast, capped): the homing target and the weak burst
        String magna = code(SRC.resolve("firecast/MagnaSwing.java"));
        Matcher m = Pattern.compile("GrimoireBook\\.around").matcher(magna);
        int n = 0;
        while (m.find()) n++;
        assertEquals(2, n, "exactly two searches, both inside impact()");
        assertTrue(magna.contains("seen++ >= 32") && magna.contains("WEAK_BURST_TARGETS"), "and both are capped");
    }

    @Test
    void thePlayerTickOnlyVisitsPlayersWithSomethingLive() throws IOException {
        String ev = code(SRC.resolve("firecast/FireCastEvents.java"));
        assertTrue(ev.contains("FireCasting.LIVE.isEmpty()") && ev.contains("(p.tickCount & 3) != 0") && ev.contains("FireCasting.LIVE.contains(p.getUUID())"));
        for (String cleanup : new String[]{"PlayerLoggedOutEvent", "LivingDeathEvent", "PlayerRespawnEvent", "PlayerChangedDimensionEvent", "ServerStoppingEvent"}) assertTrue(ev.contains(cleanup), cleanup);
    }

    @Test
    void everyPacketIsValidatedOnTheServer() throws IOException {
        String net = code(SRC.resolve("firecast/FireCastNet.java"));
        assertTrue(net.contains("ctx.player() instanceof ServerPlayer"), "the sender must be a real player");
        assertTrue(net.contains("action != 0"), "an unknown action is ignored");
        assertTrue(net.contains("playToServer") && !net.contains("playToClient"), "the one packet goes client to server and nothing is sent back");
        String fc = code(SRC.resolve("firecast/FireCasting.java"));
        assertTrue(fc.contains("PACKET_GAP") && fc.contains("CharacterSpells.characterOf(p) != CanonBook.FUEGOLEON") && fc.contains("FireCastConfig.enabled()"), "rate limit, character and switch are checked");
    }

    @Test
    void theConfigIsAServerConfigWithItsOwnFileName() throws IOException {
        String boot = code(SRC.resolve("firecast/FireCastBoot.java"));
        assertTrue(boot.contains("ModConfig.Type.SERVER, FireCastConfig.SPEC, \"nusmp-firecast-server.toml\""));
        String cfg = code(SRC.resolve("firecast/FireCastConfig.java"));
        assertTrue(cfg.contains("b.push(\"magic\")") && cfg.contains(".define(\"fireCasting\", true)"), "magic.fireCasting, default true");
    }

    @Test
    void theGrimoireCastPathStillCallsTheHooksAndTheOldPagesAreUntouched() throws IOException {
        String gb = code(SRC.resolve("book/GrimoireBook.java"));
        for (String hook : new String[]{"FireCasting.held(", "FireCasting.release(", "FireCasting.refuse(", "FireCasting.run(", "FireCasting.releaseClip(", "FireCasting.ready(", "FireCasting.chants(", "FireCasting.origin(",
                "FireCasting.damageMult(", "FireCasting.sizeMult("})
            assertTrue(gb.contains(hook), "GrimoireBook calls " + hook);
        assertTrue(code(SRC.resolve("book/SpellRuntime.java")).contains("FireCasting.origin(owner, start)") && code(SRC.resolve("book/SpellRuntime.java")).contains("FireCasting.homing(owner, homing)"));
        assertTrue(code(SRC.resolve("book/TensuraShots.java")).contains("FireCasting.origin(p,"));
        // the Fire book's pages, in order, as they were
        String fire = code(SRC.resolve("book/FireBook.java"));
        int last = -1;
        for (String id : new String[]{"exploding_fireball", "sol_linea", "calderos", "leo_rugiens", "spiral_flame", "wild_bursting_flame", "ignis_columna", "calidos_brachium", "crimson_lion_king", "flame_armour", "phoenix_flare"}) {
            int at = fire.indexOf("\"" + id + "\"");
            assertTrue(at > last, "page " + id + " is still there, in its place");
            last = at;
        }
        assertTrue(code(SRC.resolve("book/CharacterSpells.java")).contains("lock(\"book_fire\", \"leo_rugiens\", CanonBook.FUEGOLEON, CanonBook.LEOPOLD)"), "the character locks are untouched");
    }

    @Test
    void theStylesApplyOnlyToTheFireBookAndItsFamilyPages() throws IOException {
        String fc = code(SRC.resolve("firecast/FireCasting.java"));
        assertTrue(fc.contains("!(book instanceof FireBook) || !FireCastConfig.enabled()"), "off or another book: the generic casting");
        assertTrue(fc.contains("mode < book.familyCount()"), "only the family pages, never Summon / Store / Mana Strike");
    }

    @Test
    void theDrawCodeAllocatesNothingPerFrameAndBuildsNoRenderTypeOfItsOwn() throws IOException {
        for (String f : new String[]{"client/prop/FireCastDraw.java", "client/prop/FireCastPropPainter.java"}) {
            String c = code(SRC.resolve(f)).replaceAll("(?m)^.*static final.*$", "");      // static tables are built once
            assertFalse(c.contains("new float[") || c.contains("new double[") || c.contains("new Vec3") || c.contains("new ArrayList") || c.contains("new HashMap"), f + " allocates per frame");
            for (String b : new String[]{"RenderType.eyes(", "RenderType.lightning(", "ADDITIVE_TRANSPARENCY", "ResourceLocation.fromNamespaceAndPath(\"nusmp\", \"textures"})
                if (!f.endsWith("FireCastDraw.java") || !b.startsWith("Resource")) assertFalse(c.contains(b), f + " uses " + b);
        }
        String painter = code(SRC.resolve("client/prop/FireCastPropPainter.java"));
        assertTrue(painter.contains("FireCastLod.fit(") && painter.contains("FireCastLod.level("), "the level of detail and the frame budget");
        assertTrue(painter.contains("LAST_EMBER[slot] == e.tickCount"), "embers once per game tick, not per frame");
        assertTrue(painter.contains("o == mc.player"), "the caster's own form is told to the budget (client side only), so it is never dropped");
        assertTrue(painter.contains("FireCastLod.frameKey(e.level().getGameTime(), partial), o != null && o == mc.player)"), "the own flag reaches fit()");
        // the raised limits stay bounded: the budget's cap is the one constant, the cull distances come from the table
        String lod = code(SRC.resolve("firecast/FireCastLod.java"));
        assertTrue(lod.contains("FRAME_QUADS = 3200") && lod.contains("budget.force(frameKey, cost(dragon, LOW))"), "one frame cap, and the forced own form is the low level only");
    }

    @Test
    void theSpellsDecidedOnTheServerAreNeverDrawnByTheClient() throws IOException {
        String client = code(SRC.resolve("client/firecast/FireCastClient.java"));
        assertTrue(client.contains("PacketDistributor.sendToServer(new FireCastNet.Key(0))"));
        assertFalse(client.contains("FireCasting."), "the client never calls the server's rules");
    }
}
