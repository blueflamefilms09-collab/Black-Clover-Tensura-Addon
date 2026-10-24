package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Valkyrie armour v2 and the Valkyrie Drill Lance cannot be profiled or seen here, so their rules are pinned by reading the real source. (1) Server safety (rule 9): the common classes (rules, meshes,
 * budget, spin, self-test, the item, its boot) name no client class; the meshes, rules, budget and spin stay pure; no packet, no per-player static state on the server; the options are CLIENT config values in the
 * file the Wind overlay already uses, never a server config. (2) Frame cost (rule 11): the painter allocates nothing of its own per frame (every {@code new} is a static initialiser), builds no string or
 * location, plans once a frame under the armour's own pool, takes its render types from SafeAdditive and the vanilla entity types only, and the weapon renderer's drill feature allocates no vector per vertex.
 * (3) Wiring: the layer dispatches to it for the mode VALKYRIE and keeps the old overlay for CLASSIC, the water aura skips its doubled pieces, the first person and leg-hiding events are registered, the lance
 * is registered and in the creative tab only, the self-test is registered, the tools and the doc are there, and the Time Magic VFX is not touched.
 */
class ModeArmorValkyrieScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) {
        try { return Files.readString(JAVA.resolve(rel), StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + rel, e); }
    }

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", ""); }

    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)");
    private static final String[] PURE = {"book/ModeArmorValkyrieRules.java", "book/ModeArmorValkyrieMesh.java", "book/ModeArmorValkyrieBudget.java", "book/ModeArmorValkyrieSpin.java", "valkyrielance/ValkyrieLanceRules.java"};
    private static final String[] COMMON = {"book/ModeArmorValkyrieSelfTests.java", "valkyrielance/ValkyrieDrillLanceItem.java", "valkyrielance/ValkyrieLanceBoot.java"};

    /** The body of a method: from its signature to the matching closing brace. */
    private static String method(String src, String name) {
        java.util.regex.Matcher decl = Pattern.compile("\\b(?:void|int|float|boolean|ResourceLocation|VertexConsumer|Quaternionf|Result)\\s+" + name + "\\(").matcher(src);
        assertTrue(decl.find(), "method " + name + " not found");
        int open = src.indexOf('{', decl.start()), depth = 0, j = open;
        for (; j < src.length(); j++) {
            char c = src.charAt(j);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) break;
        }
        return src.substring(open, j + 1);
    }

    @Test
    void theCommonClassesAreServerSafePacketFreeAndTheMeshesRulesBudgetAndSpinStayPure() {
        for (String f : PURE) {
            String c = code(read(f));
            assertFalse(CLIENT.matcher(c).find(), f + " names a client class");
            assertFalse(c.contains("net.minecraft") || c.contains("net.neoforged"), f + " must stay pure");
            assertFalse(c.contains("PacketDistributor") || c.contains("CustomPacketPayload") || c.contains("ServerPlayer"), f + " sends something or keeps per-player state");
        }
        for (String f : COMMON) {
            String c = code(read(f));
            assertFalse(CLIENT.matcher(c).find(), f + " names a client class");
            assertFalse(c.contains("PacketDistributor") || c.contains("CustomPacketPayload"), f + " sends a packet");
        }
        String cfg = code(read("client/mode/ModeArmorValkyrieConfig.java"));
        assertFalse(cfg.contains("Type.SERVER") || cfg.contains("Type.COMMON") || cfg.contains("registerConfig"), "the options are client values of the Wind overlay's own client file");
        assertTrue(cfg.contains(".defineEnum(\"valkyrieLook\", Look.V2)") && cfg.contains(".defineEnum(\"valkyrieForm\", Form.VALKYRIE)") && cfg.contains(".defineEnum(\"valkyrieLance\", LanceMode.ATTACHED)"), "the three options and their defaults");
        String wind = code(read("client/mode/ModeArmorWindV2Config.java"));
        assertTrue(wind.contains("ModConfig.Type.CLIENT, SPEC, \"nusmp-modearmor-client.toml\"") && wind.contains("ModeArmorValkyrieConfig.define(b)") && wind.contains("ModeArmorValkyrieConfig.apply()"), "in the file of the Wind overlay, inside its modeArmor group");
        String item = code(read("valkyrielance/ValkyrieDrillLanceItem.java"));
        assertTrue(item.contains("hitBurstAllowed") && item.contains("HIT_BURSTS_PER_TICK") && item.contains("Fx.spray(level"), "the hit burst is capped and sent as ordinary particles to the players near");
        assertFalse(item.contains("onUseTick") || item.contains("ThrownTrident") || item.contains("shoot"), "an honest spear: no throw");
    }

    @Test
    void thePainterAllocatesNothingPerFramePlansOnceAFrameAndTakesItsRenderTypesFromSafeAdditive() {
        String c = code(read("client/mode/ModeArmorValkyrieV2.java"));
        for (String line : c.split("\n")) {
            if (!line.contains("new ")) continue;
            assertTrue(line.contains("static final") || line.contains("return new Quaternionf().rotationTo"), "an allocation outside a static initialiser: " + line.trim());
        }
        for (String m : new String[]{"draw", "capture", "lancePose", "emit", "emitWing", "spin", "ensurePlan", "drawArm", "lift", "slotOf", "poseOf", "buffer"}) {
            String b = method(c, m);
            assertFalse(b.contains("new ") || b.contains("String.format") || b.contains("\" +") || b.contains("ResourceLocation.") || b.contains("toLowerCase"), m + " allocates or builds something per frame");
        }
        assertTrue(countOf(c, "Minecraft.getInstance()") <= 3, "the game is asked only by the aura hook, the leg query and the draw (one static getter each)");
        assertFalse(method(c, "emit").contains("Minecraft") || method(c, "emitWing").contains("Minecraft"), "the vertex loops read nothing of the game");
        assertTrue(c.contains("ModeArmorValkyrieBudget.admit(") && c.contains("ModeArmorValkyrieBudget.place("), "admitted nearest first under the armour's own pool");
        assertTrue(method(c, "ensurePlan").contains("if (s == stamp) return;"), "the plan is made once a frame");
        assertTrue(c.contains("SafeAdditive.additive(ATLAS)") && c.contains("RenderType.entityTranslucent(ATLAS)") && c.contains("RenderType.entityCutoutNoCull(ATLAS)"), "render types through SafeAdditive and the vanilla entity types");
        assertFalse(c.contains("RenderType.eyes") || c.contains("entityTranslucentEmissive") || c.contains("RenderType.lightning"), "additive and emissive types come from SafeAdditive only");
        assertFalse(c.contains("TimeMagic"), "the Time Magic VFX is not touched");
        assertTrue(c.contains("ModeArmorValkyrieRules.lanceAt") || code(read("book/ModeArmorValkyrieBudget.java")).contains("lanceAt(level)"), "the lance is drawn at near and mid only");
    }

    private static int countOf(String s, String what) {
        int n = 0;
        for (int i = s.indexOf(what); i >= 0; i = s.indexOf(what, i + 1)) n++;
        return n;
    }

    @Test
    void theWorldHelperIsTheOnlyPlaceThatAsksTheLevelAndItsScanAllocatesNothing() {
        String w = code(read("client/mode/ModeArmorValkyrieWorld.java"));
        String scan = method(w, "scan");
        assertFalse(scan.contains("new ") || scan.contains("for (AbstractClientPlayer"), "an iterator or an allocation in the scan (indexed loop over the level's players)");
        assertTrue(scan.contains("players.size()"), "an indexed loop");
        assertTrue(scan.contains("p == mc.player && firstPerson"), "the local player in first person is not drawn by the layer and takes no room in the pool");
        assertTrue(w.contains("ValkyrieLanceBoot.LANCE.get()"), "holding the lance item turns the armour lance off");
        String e = code(read("client/mode/ModeArmorValkyrieEvents.java"));
        for (String line : e.split("\n")) if (line.contains("new ")) assertTrue(line.contains("static") && line.contains("new "), "an allocation in the events: " + line.trim());
        assertFalse(e.contains("PacketDistributor") || e.contains("CustomPacketPayload"), "no packet");
    }

    @Test
    void theWeaponRendererDrillFeatureIsSmallAndAllocatesNoVectorPerVertex() {
        String r = code(read("client/WeaponRenderer.java"));
        assertTrue(r.contains("case \"part\" ->") && r.contains("startsWith(\"drill\")") && r.contains("drillFrom"), "the `part drill*` directive");
        assertTrue(r.contains("private static void emitLayer(") && r.contains("DRILL_Q.rotationY("), "the drill is drawn after a turn about +y");
        assertFalse(method(r, "emitLayer").contains("new "), "emitLayer allocates");
        assertFalse(method(r, "drillAngle").contains("new "), "drillAngle allocates");
        assertTrue(method(r, "drillAngle").contains("ModeArmorValkyrieSpin.itemAngle("), "a pure function of the game clock and the holder's charge: no state");
        assertTrue(r.contains("mesh.drillFrom == null ? 0f : drillAngle(ctx, time)"), "a mesh without a drill part costs nothing");
        for (String f : new String[]{"emit"}) assertFalse(method(r, f).contains("new Vector3f"), "a vector per weapon vertex is back");
    }

    @Test
    void theLayerTheAuraTheEventsTheBootTheCreativeTabAndTheSelfTestAreWired() {
        String layer = code(read("client/mode/ModeArmorLayer.java"));
        assertTrue(layer.contains("ModeArmorValkyrieV2.active(mode)") && layer.contains("ModeArmorValkyrieV2.draw("), "the layer draws the v2 overlay");
        assertTrue(layer.contains("case VALKYRIE -> valkyrie(c, m);") && layer.contains("private static void valkyrie("), "the old overlay is kept for CLASSIC");
        assertTrue(layer.indexOf("ModeArmorWindV2.active(mode)") < layer.indexOf("ModeArmorValkyrieV2.active(mode)") && layer.indexOf("ModeArmorValkyrieV2.active(mode)") < layer.indexOf("case VALKYRIE -> valkyrie(c, m);"), "the dispatch line sits after the Wind one and before the switch");
        String aura = code(read("client/aura/SpiritDiveAura.java"));
        assertTrue(aura.contains("el == Element.WATER && com.newuniverse.nusmp.client.mode.ModeArmorValkyrieV2.replacesAuraPieces(c.player().getId())") && aura.contains("ModeArmorValkyrieV2.replacesAuraPiece(id)) continue;"), "the water dive skips its doubled wings, helm and halo");
        String client = code(read("client/mode/ModeArmorClient.java"));
        assertTrue(client.contains("ModeArmorValkyrieEvents.init()"));
        String ev = code(read("client/mode/ModeArmorValkyrieEvents.java"));
        for (String h : new String[]{"onRenderArm", "onRenderHand", "onLivingPre", "onLivingPost"}) assertTrue(ev.contains("addListener(ModeArmorValkyrieEvents::" + h + ")"), h + " is registered");
        assertTrue(ev.contains("RenderArmEvent") && ev.contains("RenderHandEvent") && ev.contains("RenderLivingEvent"), "the first person and leg-hiding hooks");
        assertTrue(method(ev, "onLivingPre").indexOf("ModeArmorValkyrieV2.look() != Look.V2") < method(ev, "onLivingPre").indexOf("hidesLegs"), "the cheap checks first");
        String main = code(read("NUSMP.java"));
        assertEquals(1, countOf(main, "ValkyrieLanceBoot.init(modEventBus)"), "one boot line");
        assertTrue(code(read("valkyrielance/ValkyrieLanceBoot.java")).contains("ModeArmorValkyrieSelfTests.register()"));
        assertTrue(read("book/ModeArmorValkyrieSelfTests.java").contains("\"magic.valkyrie_armor\""));
        assertTrue(code(read("client/WeaponRenderer.java")).contains("ValkyrieLanceBoot.renderables().stream()"), "the lance is a weapon the renderer draws");
        for (String f : new String[]{"tools/gen_valkyrie_ingame_assets.py", "tools/gen_valkyrie_lance_model.py", "tools/vfx_preview/valkyrie_ingame_weapon.py", "docs/vfx_remake/valkyrie_ingame.md"})
            assertTrue(Files.exists(ROOT.resolve(f)), f + " is missing");
        for (String f : new String[]{"client/mode/ModeArmorValkyrieV2.java", "client/mode/ModeArmorValkyrieEvents.java", "client/mode/ModeArmorValkyrieWorld.java", "book/ModeArmorValkyrieMesh.java", "valkyrielance/ValkyrieDrillLanceItem.java"})
            assertFalse(code(read(f)).contains("TimeMagic"), f + " touches the Time Magic VFX");
    }
}
