package com.newuniverse.nusmp.relics;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The game cannot be run here, so what makes relics_wear work is pinned by reading the sources: the hooks that a missing line would silently switch off, the server-friendliness rules (no
 * client class in common code, state keyed by UUID and cleared at logout, a switch that changes only looks is a client option), and the per-frame paths that must not allocate.
 */
class RelicWearWiringTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) throws IOException { return Files.readString(SRC.resolve(rel)); }

    /** The body of the first method called {@code name} in the text (brace matched, strings and comments skipped). */
    static String body(String src, String name) {
        Matcher m = Pattern.compile("(?m)^[ \\t]{0,8}(?:(?:public|private|protected|static|final|synchronized)\\s+)*[\\w<>\\[\\],.? ]+?\\s+" + Pattern.quote(name) + "\\s*\\([^;{}]*\\)\\s*(?:throws [\\w., ]+)?\\s*\\{").matcher(src);
        assertTrue(m.find(), "no method " + name);
        int open = m.end() - 1, depth = 0, i = open;
        for (; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '"') { for (i++; i < src.length() && src.charAt(i) != '"'; i++) if (src.charAt(i) == '\\') i++; }
            else if (c == '/' && src.startsWith("//", i)) { int e = src.indexOf('\n', i); i = e < 0 ? src.length() : e; }
            else if (c == '{') depth++;
            else if (c == '}' && --depth == 0) break;
        }
        return src.substring(open, Math.min(i + 1, src.length()));
    }

    @Test
    void theHooksAreWired() throws IOException {
        String sys = read("relics/RelicSystem.java");
        assertTrue(sys.contains("RelicCurioEquip.register(gameBus)"), "right-click equip");
        assertTrue(sys.contains("RelicWearSelfTests.register()"), "the self-test relic.wear");
        assertTrue(sys.contains("WornOnce.setSink("), "the causes reach the log");
        assertTrue(read("relics/RelicWearSelfTests.java").contains("SelfTests.register(\"relic.wear\""));
        String eq = read("relics/RelicCurioEquip.java");
        assertTrue(eq.contains("EventPriority.LOW") && eq.contains("PlayerInteractEvent.RightClickItem"), "low priority: something that already took the item on this click wins");
        assertTrue(eq.contains("e.getLevel().isClientSide()"), "the server decides");
        assertTrue(eq.contains("CurioEquipRules.tryOnUse("), "sneak keeps the ritual");
        String w = read("relics/WornSync.java");
        for (String hook : new String[]{"onLogin", "onRespawn", "onDimension"}) assertTrue(w.contains("gameBus.addListener(WornSync::" + hook + ")"), hook);
        assertTrue(w.contains("WornSyncRules.looksNow(") && w.contains("WornSyncRules.shouldSend("));
        assertTrue(body(w, "onLogout").contains("FORCE.remove(") && body(w, "onLogout").contains("forget("), "state keyed by the player's UUID is dropped at logout");
        String rc = read("client/relics/RelicClient.java");
        assertTrue(rc.contains("RelicWearClient::onClientTick") && rc.contains("RelicWearClient::onLeave") && rc.contains("RelicWearClient::registerCommands") && rc.contains("RelicWearClient.onLogout()"));
        assertTrue(read("client/relics/RelicActKeys.java").contains("e.register(TOGGLE_SHOW_WORN)"));
        String pay = read("relics/RelicPayloads.java");
        assertTrue(pay.contains("RelicShowPayload.TYPE") && pay.contains("RelicWearClient.apply(") && pay.contains("playToClient(RelicShowPayload.TYPE"), "the server only tells the player's client; the preference is the client's own");
        String cmd = read("relics/RelicCommands.java");
        assertTrue(cmd.contains("Commands.literal(\"show\")") && cmd.contains("new RelicShowPayload(mode)") && cmd.contains("src.getEntity() instanceof ServerPlayer p"), "/multiverse relic show is a server command on the player who runs it");
        assertFalse(read("client/relics/RelicWearClient.java").contains("literal(\"multiverse\")"), "a client command with the root multiverse would swallow the server's subcommands");
        assertTrue(read("relics/RelicShowPayload.java").contains("relic show mode out of range"), "the packet validates its data");
        String guard = read("client/relics/RelicLayerGuard.java");
        assertTrue(guard.contains("new HumanoidRelicLayer(renderer)") && guard.contains("r.getModel() instanceof HumanoidModel<?>"), "a body form that is not a PlayerModel still gets a worn layer");
    }

    @Test
    void everyWornLayerPassesThePlayersAllRelicsSwitch() throws IOException {
        for (String f : new String[]{"client/relics/RelicWornLayer.java", "client/relics/HumanoidRelicLayer.java", "client/playermodel/KitRelicLayer.java"})
            assertTrue(read(f).contains("RelicFxOptions.showWorn()"), f + " must obey relicfx.showWorn");
        assertTrue(read("client/relics/worn/lib/ArmorSlotClient.java").contains("RelicFxOptions.showWorn()"), "the vanilla armour layer obeys it too");
        String o = read("client/relics/fx/RelicFxOptions.java");
        assertTrue(o.contains("define(\"showWorn\", true)"), "worn relics are shown by default");
        assertTrue(o.contains("ConfigValue") || o.contains("BooleanValue SHOW_WORN"));
    }

    @Test
    void theSwitchIsAClientOptionNotAGameplayRule() throws IOException {
        String o = read("client/relics/fx/RelicFxOptions.java");
        assertTrue(o.contains("config/nusmp-relicfx-client.toml"), "it lives in the existing client file (no new config file)");
        assertFalse(read("relics/WornSync.java").contains("showWorn"), "the server never learns the player's looks preference");
    }

    @Test
    void commonCodeNeverTouchesTheClient() throws IOException {
        for (String f : new String[]{"relics/RelicWear.java", "relics/RelicCurioEquip.java", "relics/CurioEquipRules.java", "relics/WornSyncRules.java", "relics/WornOnce.java", "relics/WornSync.java"}) {
            String s = read(f);
            assertFalse(s.contains("net.minecraft.client") || s.contains("com.mojang.blaze3d") || s.contains("nusmp.client") || s.contains("Minecraft.getInstance"), f);
        }
    }

    @Test
    void theHotPathsAllocateNothing() throws IOException {
        String vis = read("client/relics/worn/WornVisibility.java");
        assertFalse(body(vis, "decide").contains("new "), "WornVisibility.decide runs per worn relic per frame");
        String once = body(read("relics/WornOnce.java"), "first");
        assertFalse(once.substring(0, once.indexOf("synchronized")).contains("new "), "WornOnce.first: the seen-before path is a set lookup");
        String layer = read("client/relics/RelicWornLayer.java");
        for (String m : new String[]{"noteFallbacks", "noteBox"}) {
            String b = body(layer, m);
            assertFalse(b.contains("new ") || b.contains("String.format") || b.matches("(?s).*\"\\s*\\+.*") || b.matches("(?s).*\\+\\s*\".*"), m + " runs per frame: no allocation, no string building");
        }
        assertFalse(body(read("client/relics/RelicWearClient.java"), "onClientTick").contains("new "));
        assertFalse(body(read("relics/Relics.java"), "ttlFor").contains("new "));
        for (String m : new String[]{"looksNow", "shouldSend", "lookTtl", "nextFastHold"}) assertFalse(body(read("relics/WornSyncRules.java"), m).contains("new "), m);
    }

    @Test
    void theTableOfCausesIsBoundedAndTheGuardLogsThem() throws IOException {
        assertTrue(WornOnce.MAX <= 128, "a flood of different causes cannot grow the table");
        String g = read("client/relics/RelicLayerGuard.java");
        assertTrue(g.contains("WornOnce.log(WornOnce.Cause.LAYER_LATE") && g.contains("WornOnce.Cause.RENDERER_NOT_HUMANOID"));
        String l = read("client/relics/RelicWornLayer.java");
        assertTrue(l.contains("WornOnce.Cause.NO_MODEL") && l.contains("WornOnce.Cause.MODELS_OFF") && l.contains("WornOnce.Cause.SYNCED_ONLY"));
    }

    @Test
    void theDebugReportPrintsTheWearFactsOfEveryRelic() throws IOException {
        String s = read("relics/WornDebug.java"), c = read("client/relics/worn/WornDebugClient.java");
        for (String word : new String[]{"slot defined", "tag:", "equipped", "sent to clients", "visible by default"}) assertTrue(s.contains(word), "server: " + word);
        for (String word : new String[]{"synced from the server", "visible now", "equipped (found on this client)"}) assertTrue(c.contains(word), "client: " + word);
        assertTrue(s.contains("WornVisibility.decide(") && c.contains("WornVisibility.decide("), "both halves use the contract");
    }

    @Test
    void theLangFragmentHasTheNewTexts() throws IOException {
        String frag = Files.readString(Path.of("tools/lang/relics_wear.json"));
        for (String key : List.of("message.nusmp.relic_wear.equipped", "message.nusmp.relic_wear.show_on", "message.nusmp.relic_wear.show_off", "key.nusmp.toggle_show_worn_relics"))
            assertTrue(frag.contains("\"" + key + "\""), key);
    }
}
