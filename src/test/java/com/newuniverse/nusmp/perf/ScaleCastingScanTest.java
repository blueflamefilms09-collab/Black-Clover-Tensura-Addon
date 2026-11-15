package com.newuniverse.nusmp.perf;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: Scale casting (docs/magic/scale_casting.md) promises rule 9 and rule 11 of CLAUDE.md: the server decides everything and only a bounded amount of it, and the client draws a bounded
 * amount without allocating per frame. Nothing can be profiled here, so the promises are pinned the way PerfSourceScanTest pins the compass: by reading the source. A test fails if a pinned
 * pattern comes back: an unthrottled scan, a lost cap, a lost clean-up, an allocation inside a per-frame or per-hit method, a client class in common code, a hook that was dropped from a shared
 * file, a Scale page whose id or number changed. If a pin is replaced by a better one on purpose, change it in the same commit and say why.
 */
class ScaleCastingScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private record Method(String name, String body) {}

    private static String read(String rel) throws IOException { return Files.readString(SRC.resolve(rel)); }

    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", ""); }

    private static int close(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                for (i++; i < s.length() && s.charAt(i) != '"'; i++) if (s.charAt(i) == '\\') i++;
            } else if (c == '\'') {
                for (i++; i < s.length() && s.charAt(i) != '\''; i++) if (s.charAt(i) == '\\') i++;
            } else if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '/') {
                int e = s.indexOf('\n', i);
                i = e < 0 ? s.length() : e;
            } else if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '*') {
                int e = s.indexOf("*/", i + 2);
                i = e < 0 ? s.length() : e + 1;
            } else if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return i;
            }
        }
        return s.length() - 1;
    }

    private static final Pattern HEADER = Pattern.compile("(?m)^[ \\t]{0,12}(?:(?:public|private|protected|static|final|synchronized|abstract|default)\\s+)*[\\w<>\\[\\],.? ]+?\\s+(\\w+)\\s*\\(([^;{}]*)\\)\\s*(?:throws [\\w., ]+)?\\s*\\{");
    private static final List<String> NOT_METHODS = List.of("if", "for", "while", "switch", "catch", "synchronized", "return", "new", "else", "try");

    private static List<Method> methods(String rel) throws IOException {
        String src = read(rel);
        List<Method> out = new ArrayList<>();
        Matcher m = HEADER.matcher(src);
        int from = 0;
        while (m.find(from)) {
            int open = m.end() - 1;
            if (!NOT_METHODS.contains(m.group(1))) out.add(new Method(m.group(1), code(src.substring(open, close(src, open) + 1))));
            from = m.end();
        }
        return out;
    }

    private static Method method(String rel, String name) throws IOException {
        for (Method m : methods(rel)) if (m.name().equals(name)) return m;
        assertNotNull(null, rel + " has no method " + name);
        return null;
    }

    private static final Pattern NEW_OBJECT = Pattern.compile("\\bnew\\s+[A-Z\\w.<>]+\\s*[(\\[]");
    private static final Pattern ENTITY_SCAN = Pattern.compile("getEntitiesOfClass\\(|getEntities\\(|getNearbyEntities\\(");

    // ================================================================ the client: levels of detail, a budget, no allocation per frame
    @Test
    void thePainterAsksForALevelOfDetailBeforeDrawingAndAllocatesNothingPerFrame() throws IOException {
        String painter = code(read("client/prop/ScaleBalancePainter.java"));
        Method paint = method("client/prop/ScaleBalancePainter.java", "paint0");
        assertTrue(paint.body().contains("ScaleFrame.level(") && paint.body().contains("ScaleLod.CULLED"), "the painter draws without a level of detail");
        assertTrue(paint.body().indexOf("ScaleFrame.level(") < paint.body().indexOf("ScaleRender.scale("), "the level is asked before anything is drawn");
        assertTrue(paint.body().contains("ScaleVisualConfig.scale3d()"), "the looks-only switch is gone");
        for (String name : new String[]{"paint0", "readout", "near"}) {
            String body = method("client/prop/ScaleBalancePainter.java", name).body();
            assertFalse(body.contains("new Vec3") || body.contains(".getPosition(") || body.contains(".add(") || body.contains(".subtract(") || body.contains("new float") || body.contains("String.format"),
                    "ScaleBalancePainter." + name + " allocates per frame");
        }
        assertTrue(paint.body().contains("ANIMS.computeIfAbsent(e, k -> new Anim())"), "the per-prop state is created once per prop, in the weak map");
        String paintBody = paint.body().replace("ANIMS.computeIfAbsent(e, k -> new Anim())", "ANIMS.get(e)");                 // once per prop, not per frame
        assertFalse(NEW_OBJECT.matcher(paintBody).find(), "paint0 allocates an object per frame (found " + NEW_OBJECT.matcher(paintBody).results().map(r -> r.group()).toList() + ")");
        assertTrue(painter.contains("WeakHashMap") && painter.contains("buildText(a, word, t)"), "the per-prop state or the text cache is gone");
        Method text = method("client/prop/ScaleBalancePainter.java", "buildText");
        assertTrue(text.body().contains("a.textWord == word") && text.body().indexOf("a.textWord == word") < text.body().indexOf("translatableWithFallback"), "the readout text is built every frame again");
    }

    @Test
    void theRendererAndTheFrameHelperAllocateNothingPerFrame() throws IOException {
        for (String name : new String[]{"scale", "orb", "halo", "groundRing", "line"})
            assertFalse(NEW_OBJECT.matcher(method("client/scalecast/ScaleRender.java", name).body()).find(), "ScaleRender." + name + " allocates per frame");
        for (String name : new String[]{"level", "distSq", "begin"})
            assertFalse(NEW_OBJECT.matcher(method("client/scalecast/ScaleFrame.java", name).body()).find(), "ScaleFrame." + name + " allocates per frame");
        String render = code(read("client/scalecast/ScaleRender.java"));
        assertTrue(render.contains("static final PoseSink SOLID") && render.contains("GLOW = new PoseSink()"), "the scratch sinks are gone");
        assertTrue(render.contains("SafeAdditive.additive(") && !render.contains("RenderType.eyes(") && !render.contains("RenderType.lightning("), "a glow pass skips SafeAdditive");
        String frame = code(read("client/scalecast/ScaleFrame.java"));
        assertTrue(frame.contains("QUADS.setCap(ScaleLod.frameQuads(") && frame.contains("CHOSEN.getOrDefault") && frame.contains("RenderLevelStageEvent.Stage.AFTER_SKY") && frame.contains("ScaleLod.admit("),
                "the Scale's frame budget or its once-per-frame roll is gone");
        String mesh = code(read("scalecast/ScaleMesh.java"));
        assertTrue(mesh.contains("BEAM_1") && mesh.contains("PAN_2") && mesh.contains("CHAIN_2"), "the mesh lost its lower levels");
        assertTrue(code(read("client/scalecast/ScaleHud.java")).contains("Long.MIN_VALUE") && code(read("client/scalecast/ScaleHud.java")).contains("seen > 6L"), "the plate no longer lapses by itself");
    }

    // ================================================================ the server: capped, throttled, cleaned up
    @Test
    void theServerScansAreThrottledCappedAndBoundedByAGlobalAllowance() throws IOException {
        String kit = code(read("scalecast/ScaleCastKit.java"));
        Method tick = method("scalecast/ScaleCastKit.java", "tick0");
        assertTrue(tick.body().contains("p.tickCount % 10 == 3") && tick.body().contains("if (st == null && !due) return;"), "a player without a Scale must cost one map lookup per tick");
        assertTrue(tick.body().contains("ScaleCastRules.SCAN_EVERY") && tick.body().contains("SCANS.tryTake(now, 1)"), "the aura scan lost its interval or its server-wide allowance");
        int scans = 0;
        Matcher m = ENTITY_SCAN.matcher(kit);
        while (m.find()) scans++;
        assertEquals(1, scans, "exactly one entity scan in the whole kit");
        assertTrue(method("scalecast/ScaleCastKit.java", "scan").body().contains("getEntitiesOfClass("), "the one scan is in scan()");
        assertTrue(kit.contains("new PerfBudget(\"scale-scans\", ScaleCastRules.SCANS_PER_TICK)"), "the scan allowance is gone");
        assertTrue(kit.contains("(now & 1L) == 0L") && kit.contains("ScaleCastRules.shouldSetWord("), "the Scale moves every tick or its word is written on every change again (position and data packets)");
        assertTrue(method("scalecast/ScaleCastKit.java", "state").body().contains("ScaleCastRules.GLOBAL_SCALES"), "the server-wide cap on Scales is gone");
        String scan = method("scalecast/ScaleCastKit.java", "scan").body();
        assertTrue(scan.contains("ScaleCastRules.MAX_FOES") && scan.contains("ScaleCastRules.ENTRY_LIFE"), "the per-caster foe cap or the entry life is gone");
        assertTrue(method("scalecast/ScaleCastKit.java", "insert").body().contains("ScaleCastRules.MAX_FOES"), "the nearest-foes pick lost its cap");
        assertTrue(scan.contains("ScaleCastConfig.damageCap()") && scan.contains("ScaleCastRules.reduction(") && scan.contains("ScaleCastRules.magicReduction("), "the reductions are not computed through the capped rules");
        assertTrue(scan.contains("red > en.red"), "a foe held by several casters must take the strongest reduction, not the sum");
        assertFalse(scan.contains("+= red") && !scan.contains("en.red = (float) red"), "reductions must never be added together");
        assertTrue(method("scalecast/ScaleCastKit.java", "scan").body().contains("Arrays.fill(PICK, null)"), "the pick scratch keeps entities alive after a scan");
    }

    @Test
    void theHitAndSpellHooksCostOneLookupAndAllocateNothing() throws IOException {
        for (String name : new String[]{"incoming", "protects", "magicMult", "reductionOf"}) {
            String body = method("scalecast/ScaleCastKit.java", name).body();
            assertFalse(NEW_OBJECT.matcher(body).find(), "ScaleCastKit." + name + " allocates");
            assertFalse(ENTITY_SCAN.matcher(body).find(), "ScaleCastKit." + name + " scans entities");
        }
        String incoming = method("scalecast/ScaleCastKit.java", "incoming").body();
        assertTrue(incoming.contains("ScaleCastRules.apply(e.getAmount(), en.red)"), "the damage goes through the floor and the cap");
        assertTrue(incoming.contains("BYPASSES_INVULNERABILITY") && incoming.contains("en.magicStamp == now"), "a kill command or a spell already reduced this tick must be left alone");
        assertFalse(incoming.contains("getMaxHealth"), "no percent of anyone's health");
        assertTrue(incoming.contains("protects(victim, en.by)"), "the aura must only protect its caster, their pets and their team");
        assertTrue(code(read("scalecast/ScaleCastKit.java")).contains("if (SUPP.isEmpty()) return;"), "an idle server must pay one isEmpty per hit");
    }

    @Test
    void everythingIsCleanedUpOnLogoutDeathRespawnDimensionAndServerStop() throws IOException {
        String reg = method("scalecast/ScaleCastKit.java", "register").body();
        assertTrue(reg.contains("AttributeEvents.logout(") && reg.contains("AttributeEvents.death(") && reg.contains("PlayerChangedDimensionEvent") && reg.contains("PlayerRespawnEvent")
                && reg.contains("ServerStoppingEvent") && reg.contains("AttributeEvents.serverTick("), "a clean-up of the per-player tables is gone");
        assertTrue(method("scalecast/ScaleCastKit.java", "tick0").body().contains("st.dim != p.level().dimension()") && method("scalecast/ScaleCastKit.java", "tick0").body().contains("!p.isAlive()"),
                "the tick no longer ends a Scale after a dimension change or a death");
        assertTrue(method("scalecast/ScaleCastKit.java", "forget").body().contains("release(") && method("scalecast/ScaleCastKit.java", "forget").body().contains("discard()"), "forget no longer frees the entries and the prop");
        assertTrue(method("scalecast/ScaleCastKit.java", "prune").body().contains("removeIf"), "expired entries are never pruned");
        assertTrue(code(read("prop/ScaleProps.java")).contains("ScaleCastKit.register()") && code(read("prop/ScaleProps.java")).contains("ScaleCastSelfTests.register()"), "ScaleProps no longer wires the kit and its self-test");
    }

    @Test
    void theStateIsKeyedByPlayerUuidAndNoCommonFileTouchesAClientClass() throws IOException {
        String kit = code(read("scalecast/ScaleCastKit.java"));
        assertTrue(kit.contains("Map<UUID, State> STATES") && kit.contains("Map<UUID, Entry> SUPP"), "the tables are not keyed by UUID");
        assertFalse(kit.contains("static ServerPlayer") && kit.contains("static LivingEntity current"), "a static current player");
        Pattern client = Pattern.compile("net\\.minecraft\\.client|com\\.mojang\\.blaze3d|com\\.newuniverse\\.nusmp\\.client|neoforge\\.client|Minecraft\\.getInstance");
        try (Stream<Path> s = Files.list(SRC.resolve("scalecast"))) {
            for (Path p : s.filter(f -> f.toString().endsWith(".java")).toList()) assertFalse(client.matcher(code(Files.readString(p))).find(), p + " names a client class");
        }
        assertTrue(read("scalecast/ScaleCastConfig.java").contains("ModConfigSpec") && read("NUSMP.java").contains("ModConfig.Type.SERVER, com.newuniverse.nusmp.scalecast.ScaleCastConfig.SPEC, \"nusmp-scale-casting-server.toml\""),
                "the gameplay switch must be a SERVER config");
        assertTrue(read("client/scalecast/ScaleHud.java").contains("ModConfig.Type.CLIENT, ScaleVisualConfig.SPEC, \"nusmp-scale-casting-client.toml\""), "the looks switches must be a CLIENT config");
        assertTrue(code(read("scalecast/ScaleCastConfig.java")).contains("\"scaleCasting\"") && code(read("scalecast/ScaleCastConfig.java")).contains(".define(\"scaleCasting\", true)"), "magic.scaleCasting must default to true");
    }

    // ================================================================ the hooks in the shared files and the pages that must not change
    @Test
    void theSharedHooksAreThere() throws IOException {
        String book = code(read("book/GrimoireBook.java"));
        assertTrue(book.contains("castMagic == MagicType.SCALE") && book.contains("ScaleCastKit.onCast(player)"), "the cast hook is gone");
        assertTrue(book.contains("magic == MagicType.SCALE") && book.contains("ScaleCastKit.onHeld(p)"), "the hold hook is gone");
        assertTrue(code(read("mana/ManaLaw.java")).contains("m *= com.newuniverse.nusmp.scalecast.ScaleCastKit.magicMult(p);"), "the spell power hook is gone");
        String kinds = code(read("prop/PropKind.java"));
        assertTrue(kinds.replaceAll("\\s+", "").contains("BONE_JA,SCALE_BALANCE,"), "SCALE_BALANCE must keep its place right after BONE_JA (ordinals are synced; later kinds are appended after it)");
        assertTrue(code(read("client/prop/PropPainterRegistry.java")).contains("ScaleBalancePainter.register();"));
        assertTrue(code(read("book/ScaleSpells.java")).contains("ScaleCastKit.origin(p, start)"), "Scale Volley no longer starts at the Scale");
    }

    @Test
    void theScalePagesKeepTheirIdsOrderAndNumbers() throws IOException {
        String s = code(read("book/ScaleSpells.java"));
        int a = s.indexOf("\"scale_volley\""), b = s.indexOf("\"scale_serpent\""), c = s.indexOf("\"scale_shield\""), d = s.indexOf("\"scale_mail\""), e = s.indexOf("\"scale_tide\"");
        assertTrue(a > 0 && a < b && b < c && c < d && d < e, "the five page ids or their order changed");
        assertTrue(s.contains("SERPENT_TICKS = 500, SHIELD_TICKS = 300, ARMOUR_TICKS = 400"), "a duration changed");
        assertTrue(s.contains("b.hurt(i, p, t, mode, 5f * k)") && s.contains("12f * k") && s.contains("ARMOUR_TICKS, 0.25f, 0f, 1)") && s.contains(".withCooldown(1800)"), "a damage, guard or cooldown number changed");
        assertTrue(s.contains("n * 0.14") && s.contains("SpellRuntime.bolt(p, start, dir.scale(1.6), 0.5, 14,"), "the volley's fan or bolt numbers changed");
        assertTrue(s.contains("GrimoireBook.around(p, c, 6.0)") && s.contains("n++ >= 16"), "the tide's reach or target cap changed");
    }
}
