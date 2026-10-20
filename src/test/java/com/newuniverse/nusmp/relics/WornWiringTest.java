package com.newuniverse.nusmp.relics;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The game cannot be run here, so the wiring that makes the worn relics reach the screen is pinned by reading the sources: each hook that a missing line would silently switch off
 * (the layer on the renderers, the first-frame guard, the packets, the server events, the self-test, the command, the lang keys of the report) is looked for where it must be.
 */
class WornWiringTest {
    static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    static String read(String rel) throws IOException { return Files.readString(SRC.resolve(rel)); }

    @Test
    void theLayerIsAddedToEveryPlayerSkinAndGuardedAtTheFirstFrame() throws IOException {
        String c = read("client/relics/RelicClient.java");
        assertTrue(c.contains("event.getSkins()"), "both skin types (default and slim) are visited");
        assertTrue(c.contains("r.addLayer(new RelicWornLayer(r))"));
        assertTrue(c.contains("RelicLayerGuard::onPre"), "a renderer the layer event never reached gets the layer at its first frame");
        assertTrue(c.contains("RelicLayerGuard.known(r)"), "the guard must know the renderers the event did reach, or it would add the layer twice");
        assertTrue(c.contains("modBus.addListener(RelicClient::addLayers)"));
        assertTrue(c.contains("new HumanoidRelicLayer(renderer)") && c.contains("r.getModel() instanceof HumanoidModel<?>"), "armour stands and humanoid mobs get the layer too");
        String k = read("client/playermodel/PlayerModelRenderer.java");
        assertTrue(k.contains("new KitRelicLayer<>(this)"), "the player-model characters wear their relics");
        String n = read("NUSMP.java");
        assertTrue(n.contains("client.relics.RelicClient.init(modEventBus)"));
    }

    @Test
    void thePacketsAreRegisteredAndTheServerEventsAreWired() throws IOException {
        String p = read("relics/RelicPayloads.java");
        assertTrue(p.contains("WornSync.TYPE") && p.contains("WornSync.STREAM_CODEC") && p.contains("WornSync.put("));
        assertTrue(p.contains("WornDebugPayload.TYPE") && p.contains("WornDebugClient.report("));
        assertTrue(p.contains("playToClient"));
        String s = read("relics/RelicSystem.java");
        assertTrue(s.contains("WornSync.register(gameBus)"), "without it the server never tells a client what a wearer carries");
        assertTrue(s.contains("WornRender2SelfTests.register()"));
        String w = read("relics/WornSync.java");
        for (String hook : new String[]{"onPlayerTick", "onEntityTick", "onStartTracking", "onLogout", "onLeave"}) assertTrue(w.contains("gameBus.addListener(WornSync::" + hook + ")"), hook);
        assertTrue(w.contains("sendToPlayersTrackingEntityAndSelf"), "the wearer itself is told too");
        assertTrue(w.contains("Relics.scanExtra"), "what is sent is the part vanilla does not sync: Curios and the pack");
    }

    @Test
    void theClientReadsTheServersListAndTheCommandExists() throws IOException {
        String r = read("relics/Relics.java");
        assertTrue(r.contains("if (e.level().isClientSide()) addSynced(e, out)"), "the client merges the server's list into its own look");
        assertTrue(r.contains("WornMerge.missing("));
        String cmd = read("relics/RelicCommands.java");
        assertTrue(cmd.contains("Commands.literal(\"debug\").requires(s -> s.hasPermission(2))"), "operators only");
        assertTrue(cmd.contains("new WornDebugPayload("), "the operator's own client adds its half");
        String rc = read("client/relics/RelicClient.java");
        assertTrue(rc.contains("WornSync.clearClient()"), "leaving a world forgets the server's lists");
    }

    @Test
    void theSelfTestHasItsKey() throws IOException {
        assertTrue(read("relics/WornRender2SelfTests.java").contains("SelfTests.register(\"relic.worn_render2\""));
    }

    @Test
    void everyTextOfTheReportIsInTheLangFragmentWithTheSameEnglish() throws IOException {
        Pattern t = Pattern.compile("\\bt\\(\"([a-z_]+)\",\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Map<String, String> code = new HashMap<>();
        for (String f : new String[]{"relics/WornDebug.java", "client/relics/worn/WornDebugClient.java"}) {
            Matcher m = t.matcher(read(f));
            while (m.find()) {
                String key = "message.nusmp.relic_debug." + m.group(1), text = m.group(2).replace("\\\"", "\"").replace("\\\\", "\\");
                String before = code.put(key, text);
                assertTrue(before == null || before.equals(text), "two different texts under " + key);
            }
        }
        assertTrue(code.size() >= 25, "keys in code: " + code.size());
        String json = Files.readString(Path.of("tools/lang/relic_render2.json"));
        for (Map.Entry<String, String> e : code.entrySet()) {
            String line = "\"" + e.getKey() + "\": \"" + e.getValue().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
            assertTrue(json.contains(line), "the fragment lacks: " + line);
        }
    }
}
