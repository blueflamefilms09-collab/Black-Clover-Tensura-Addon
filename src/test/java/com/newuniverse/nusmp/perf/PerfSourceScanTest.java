package com.newuniverse.nusmp.perf;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: nothing can be profiled here, so the fixes of the performance pass are pinned the only way they can be: by reading the source. A test reads the real files and fails if a fixed pattern
 * comes back: reflection or a location built from a string inside a per-frame or per-tick method, a per-quad key in the vertex buffer, an allocation in the geo draw, a lost cache, a lost
 * budget, a probe that was dropped from a handler. The method finder is a small brace matcher that skips strings and comments, so a word in a comment never counts. If a fix is replaced by a
 * better one on purpose, change the pin in the same commit and say why.
 */
class PerfSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    // ---------------------------------------------------------------- a tiny Java method finder
    private record Method(String name, String body, String file) {}

    private static final Pattern HEADER = Pattern.compile("(?m)^[ \\t]{0,8}(?:(?:public|private|protected|static|final|synchronized|abstract|default)\\s+)*[\\w<>\\[\\],.? ]+?\\s+(\\w+)\\s*\\(([^;{}]*)\\)\\s*(?:throws [\\w., ]+)?\\s*\\{");
    private static final List<String> NOT_METHODS = List.of("if", "for", "while", "switch", "catch", "synchronized", "return", "new", "else", "try");

    private static String read(String rel) throws IOException { return Files.readString(SRC.resolve(rel)); }

    /** The index of the brace that closes the one at {@code open}, skipping strings, characters and comments. */
    private static int close(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                if (s.startsWith("\"\"\"", i)) {
                    int e = s.indexOf("\"\"\"", i + 3);
                    i = e < 0 ? s.length() : e + 2;
                } else {
                    for (i++; i < s.length() && s.charAt(i) != '"'; i++) if (s.charAt(i) == '\\') i++;
                }
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

    /** Every method with a body in the source (top level or nested), in file order. */
    private static List<Method> methods(String src, String file) {
        List<Method> out = new ArrayList<>();
        Matcher m = HEADER.matcher(src);
        int from = 0;
        while (m.find(from)) {
            String name = m.group(1);
            int open = m.end() - 1;
            if (NOT_METHODS.contains(name)) {
                from = m.end();
                continue;
            }
            int end = close(src, open);
            out.add(new Method(name, src.substring(open, end + 1), file));
            from = m.end();                                              // nested methods (a lambda body, an inner class) are found too
        }
        return out;
    }

    private static Method method(String rel, String name) throws IOException {
        for (Method m : methods(read(rel), rel)) if (m.name().equals(name)) return m;
        assertNotNull(null, rel + " has no method " + name);
        return null;
    }

    private static List<Path> javaFiles() throws IOException {
        try (Stream<Path> s = Files.walk(SRC)) {
            return s.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
    }

    /** The code of a body without its comments (the pins look at code, not at prose). */
    private static String code(String body) {
        return body.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    // ---------------------------------------------------------------- the rules
    private static final Pattern PER_FRAME_OR_TICK = Pattern.compile("^(on\\w*Tick\\w*|tick\\w*|aiStep\\w*|customServerAiStep\\w*|render\\w*|paint\\w*|draw\\w*|onRender\\w*|setupAnim|getTextureLocation|shouldRender)$");
    private static final Pattern REFLECTION = Pattern.compile("Class\\.forName\\(|\\.getMethod\\(|\\.getDeclaredMethod\\(|\\.getDeclaredField\\(|\\.getField\\(|\\.getDeclaredConstructor\\(|\\.invoke\\(");
    private static final Pattern BUILDS_LOCATION = Pattern.compile("ResourceLocation\\.(fromNamespaceAndPath|parse|withDefaultNamespace|tryParse)\\(|new ResourceLocation\\(");

    @Test
    void noReflectionLookupInsideATickOrRenderMethod() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path f : javaFiles()) {
            String src = Files.readString(f);
            if (!REFLECTION.matcher(src).find()) continue;
            for (Method m : methods(src, SRC.relativize(f).toString())) {
                if (PER_FRAME_OR_TICK.matcher(m.name()).matches() && REFLECTION.matcher(code(m.body())).find()) bad.add(m.file() + " " + m.name());
            }
        }
        assertTrue(bad.isEmpty(), "reflection inside a method that runs every tick or every frame (look the Method/Field up once, keep it in a static): " + bad);
    }

    /** The files whose per-frame and per-tick methods must not build a ResourceLocation (the hot render files of the pass). */
    private static final List<String> HOT_RENDER_FILES = List.of(
            "vfx/client/VfxVertexBuffer.java", "vfx/client/VfxManager.java", "vfx/client/FxRenderer.java", "client/geo/GeoDraw.java", "client/aura/PlayerAuraLayer.java",
            "client/aura/AuraRender.java", "client/relics/RelicWornLayer.java", "client/relics/HumanoidRelicLayer.java", "client/labyrinth/foundry/FoundryRenderer.java",
            "client/remodel/AbyssalRenderer.java", "client/thread/ThreadTrapRenderer.java", "client/grimoire/GrimoireFloatClient.java", "client/relics/djinn/DjinnArmorLayer.java");

    @Test
    void theHotRenderFilesBuildNoLocationInTheirFrameMethods() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String rel : HOT_RENDER_FILES) {
            for (Method m : methods(read(rel), rel)) {
                if (PER_FRAME_OR_TICK.matcher(m.name()).matches() && BUILDS_LOCATION.matcher(code(m.body())).find()) bad.add(rel + " " + m.name());
            }
        }
        assertTrue(bad.isEmpty(), "a ResourceLocation is built from strings inside a per-frame method (keep it in a static final or in perf.RlCache): " + bad);
    }

    @Test
    void auraLayerCardAndRuneTexturesAreCached() throws IOException {
        String rune = code(method("client/grimoire/GrimoireFloatClient.java", "runeTexture").body());
        assertTrue(rune.contains("PerfEpoch.get()") && rune.contains("RUNE_TEXTURES["), "the rune texture lookup lost its cache");
        String tex = code(method("client/aura/AuraRender.java", "tex").body());
        assertTrue(tex.contains("RlCache.modTexture("), "AuraRender.tex must be answered from the cache (fifteen painters ask for sprites while they paint)");
        assertTrue(code(method("client/relics/RelicTextures.java", "armorGlow").body()).contains("GLOW_LOCATIONS.get("), "the armour glow location lost its cache");
        assertTrue(code(method("vfx/client/FxRenderer.java", "drawPiece").body()).contains("RlCache.fxTexture("), "the FX piece texture lost its cache");
    }

    // ---------------------------------------------------------------- the vertex buffer, the geo draw, the FX pieces
    @Test
    void theVertexBufferAllocatesNothingPerQuadAndKeepsItsBatches() throws IOException {
        String src = code(read("vfx/client/VfxVertexBuffer.java"));
        assertFalse(src.contains("new Key("), "a key object per quad is back");
        assertFalse(src.contains("record Key"), "the per-quad key type is back");
        String quad = code(method("vfx/client/VfxVertexBuffer.java", "quadRaw").body());
        assertFalse(quad.contains("new "), "quadRaw must not allocate");
        assertFalse(quad.contains("computeIfAbsent"), "quadRaw must find its batch through the last-batch shortcut and the texture table");
        for (String shape : new String[]{"billboard", "plane", "ring", "beam", "arc"}) {
            String body = code(method("vfx/client/VfxVertexBuffer.java", shape).body());
            long vectors = Pattern.compile("new Vector3f\\(").matcher(body).results().count();
            long allowed = shape.equals("beam") ? 4 : 0;                // beam builds its four frame vectors once per call, not per segment
            assertEquals(allowed, vectors, shape + " allocates vectors");
        }
    }

    @Test
    void theGeoDrawAllocatesNothingPerCubeOrBone() throws IOException {
        String src = code(read("client/geo/GeoDraw.java"));
        assertEquals(1, Pattern.compile("new Quaternionf\\(").matcher(src).results().count(), "a quaternion per turned bone is back (only the static TURN scratch may be built)");
        assertTrue(src.contains("Quaternionf TURN = new Quaternionf();"));
        assertFalse(src.replace("new float[3]", "").contains("new float["), "arrays per cube or per face are back (the cube's tables are worked out once: GeoModelData.Cube#prepareDraw)");
        assertEquals(3, Pattern.compile("new float\\[3\\]").matcher(src).results().count(), "only the three scratch arrays of the bone pass may be allocated, once, in static fields");
        assertFalse(src.contains(".sample("), "sampling must write into the scratch arrays (Track#sampleInto)");
        assertTrue(src.contains("sampleInto("));
        assertTrue(src.contains("prepareDraw("));
        assertTrue(code(read("client/geo/GeoAnim.java")).contains("public boolean sampleInto("));
    }

    @Test
    void theWeaponRendererAllocatesNoVectorPerVertex() throws IOException {
        String emit = code(method("client/WeaponRenderer.java", "emit").body());
        assertFalse(emit.contains("new Vector3f"), "a vector per weapon vertex is back (use the scratch vectors)");
        String render = code(method("client/WeaponRenderer.java", "renderByItem").body());
        int voidPass = render.indexOf("NURenderTypes.demonVoid()");
        assertTrue(voidPass > 0, "the void pass moved; update this pin");
        assertFalse(render.substring(voidPass, voidPass + 400).contains("new Vector3f"), "the void pass allocates a vector per vertex again");
    }

    @Test
    void theParticleEmittersOfTheHotStepsGoThroughTheBudget() throws IOException {
        assertTrue(code(read("perf/PerfParticles.java")).contains("PerfBudgets.particles("), "PerfParticles lost its budget");
        for (String rel : new String[]{"labyrinth/mega/theme/ThemeAmbient.java", "labyrinth/mega/safe/RestEvents.java", "entity/zogratis/LuciusFx.java", "entity/zogratis/VanicaMoves.java", "arc/spade/MorrisMoves.java"})
            assertTrue(read(rel).contains("PerfParticles.send("), rel + " sends its particles around the budget again");
    }

    @Test
    void theModeArmorLibraryKeyIsBuiltOncePerMode() throws IOException {
        assertTrue(code(method("client/mode/ModeArmorLayer.java", "libraryKey").body()).contains("LIBRARY_KEYS["));
        assertFalse(code(method("client/mode/ModeArmorLayer.java", "render0").body()).contains("toLowerCase"), "the library key is built per frame again");
    }

    @Test
    void theFxPiecesPlaceTheirVerticesWithScratchVectors() throws IOException {
        String body = code(method("vfx/client/FxRenderer.java", "drawPiece").body());
        int loop = body.indexOf("for (int k = 0; k < mesh.length");
        assertTrue(loop > 0, "the vertex loop moved; update this pin");
        String loopBody = body.substring(loop);
        assertFalse(loopBody.contains("new Vector3f(camUp)"), "a vector per sprite vertex is back");
        assertFalse(loopBody.contains("toWorld("), "toWorld allocates three vectors per ribbon vertex; use the scratch");
    }

    @Test
    void theVfxManagerThinsByDistanceAndNeverTouchesTheTimeLayer() throws IOException {
        String render = code(method("vfx/client/VfxManager.java", "render").body());
        assertTrue(render.contains("PerfSettings.vfxQuality()"), "the effect quality is no longer consulted");
        assertTrue(render.contains("ctx.detail ="), "the per-effect level of detail is no longer applied");
        String plan = code(method("vfx/client/VfxManager.java", "plan").body());
        assertTrue(plan.contains("instanceof TimeMagicLayer"), "the Time Magic layer must stay exempt from the level of detail and the culling");
        assertTrue(code(read("vfx/client/VfxManager.java")).contains("Math.min(MAX_ACTIVE, PerfSettings.vfxQuality().maxActive)"));
    }

    // ---------------------------------------------------------------- caches and budgets
    @Test
    void theRelicLookUsesTheConfiguredLifetimeAndTheCurioIdsAreCached() throws IOException {
        String cached = code(method("relics/Relics.java", "cached").body());
        assertTrue(cached.contains("ttl()") && !cached.contains("< TTL"), "the relic look must follow relicScanInterval");
        String id = code(method("item/CuriosLink.java", "id").body());
        assertTrue(id.contains("IDS["), "the curio modifier ids must be built once");
    }

    @Test
    void theEffectPacketsOfOneTickHaveABudget() throws IOException {
        assertTrue(code(read("vfx/VfxSpawn.java")).contains("PerfBudgets.effectPacket("), "VfxSpawn.send lost its per-tick budget");
    }

    @Test
    void theElsdociaLookFollowsTheConfiguredInterval() throws IOException {
        assertTrue(code(method("item/ElsdociaEvents.java", "onPlayerTick").body()).contains("PerfSettings.entityScanInterval()"));
    }

    @Test
    void thePerEntityHandlersExitBeforeTheirCostlyChecks() throws IOException {
        String freeze = code(method("blackclover/TimeStop.java", "onEntityTick").body());
        assertTrue(freeze.indexOf("FROZEN.isEmpty()") >= 0 && freeze.indexOf("FROZEN.isEmpty()") < freeze.indexOf("containsKey"), "TimeStop.onEntityTick must look at the empty map first");
        String mob = code(method("relics/MobRelics.java", "onTick0").body());
        assertTrue(mob.indexOf("% 20") >= 0 && mob.indexOf("% 20") < mob.indexOf("MobRelicsConfig.enabled()"), "MobRelics.onTick must test the modulo before it reads the config");
    }

    @Test
    void theFoundryAndAbyssalRenderersBuildNoStringsPerFrame() throws IOException {
        String foundry = code(method("client/labyrinth/foundry/FoundryRenderer.java", "render0").body());
        assertFalse(foundry.contains("+ \"|\" +") || foundry.contains(".toString()"), "FoundryRenderer.render builds strings per mob per frame again");
        String abyssal = code(method("client/remodel/AbyssalRenderer.java", "render").body());
        assertFalse(abyssal.contains("look.id() + \"/\""), "AbyssalRenderer.render builds a cache key string per frame again");
        assertTrue(code(read("client/labyrinth/foundry/FoundryRenderer.java")).contains("class Kit"));
    }

    // ---------------------------------------------------------------- the probe sections stay where they were put
    private static final Map<String, String> PROBES = new LinkedHashMap<>();
    static {
        PROBES.put("item/CuriosLink.java", "s.curios.passives");
        PROBES.put("item/WeaponEngravings.java", "s.engravings.weapon");
        PROBES.put("item/ArmorEngravings.java", "s.engravings.armor");
        PROBES.put("mana/Heritage.java", "s.mana.heritage");
        PROBES.put("mana/ManaLaw.java", "s.mana.law");
        PROBES.put("mana/zone/ManaZoneManager.java", "s.mana.zone");
        PROBES.put("relics/djinn/DjinnEngine.java", "s.djinn.engine");
        PROBES.put("relics/WornSync.java", "s.relic.worn_sync");
        PROBES.put("relics/RelicHolderSync.java", "s.relic.holder_sync");
        PROBES.put("relics/Relics.java", "s.relic.scan");
        PROBES.put("book/GrimoireBook.java", "s.book.grimoire");
        PROBES.put("book/SpellRuntime.java", "s.book.spell_runtime");
        PROBES.put("labyrinth/mega/puzzle/PuzzleDirector.java", "s.mega.puzzle");
        PROBES.put("labyrinth/mega/mechanics/MechanicsDirector.java", "s.mega.mechanics");
        PROBES.put("labyrinth/mega/floors/FloorTracker.java", "s.mega.floor_tracker");
        PROBES.put("labyrinth/mega/system/FloorInfo.java", "s.mega.floor_info");
        PROBES.put("labyrinth/mega/system/SystemSync.java", "s.mega.system_sync");
        PROBES.put("boss/bar/BossBarSync.java", "s.boss.bar_sync");
        PROBES.put("compat/create/CreateTrapDirector.java", "s.create.traps");
        PROBES.put("vfx/VfxSpawn.java", "s.vfx.send");
        PROBES.put("vfx/client/VfxManager.java", "c.vfx.render");
        PROBES.put("vfx/client/FxRenderer.java", "c.fx.render");
        PROBES.put("client/aura/PlayerAuraLayer.java", "c.aura.layer");
        PROBES.put("client/relics/RelicWornLayer.java", "c.relic.worn_layer");
        PROBES.put("client/relics/djinn/DjinnArmorLayer.java", "c.djinn.armor_layer");
        PROBES.put("client/labyrinth/foundry/FoundryRenderer.java", "c.foundry.render");
        PROBES.put("fairytail/storm/StormField.java", "s.storm.tick");                                   // Storm Magic: the whole storm tick
    }

    @Test
    void theBiggestHandlersAreWrappedInProbeSections() throws IOException {
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> e : PROBES.entrySet()) {
            String src = read(e.getKey());
            if (!src.contains("PerfProbe.section(\"" + e.getValue() + "\")")) missing.add(e.getKey() + " " + e.getValue());
        }
        assertTrue(missing.isEmpty(), "probe sections lost: " + missing);
    }

    @Test
    void theProbeCostsOneFlagReadWhenOff() throws IOException {
        String probe = code(read("perf/PerfProbe.java"));
        assertTrue(probe.contains("return enabled ? System.nanoTime() : 0L;"), "begin() must be a flag read when off");
        assertTrue(probe.contains("if (started != 0L)"), "end() must return at once on 0");
    }

    // ---------------------------------------------------------------- the config and the wiring
    @Test
    void theConfigFilesCarryTheKnobsTheOwnerAskedFor() throws IOException {
        String cfg = read("perf/PerfConfig.java");
        for (String key : new String[]{"vfxQuality", "maxParticlesPerTick", "auraLodNear", "auraLodFar", "entityScanInterval", "relicScanInterval", "packetRefreshTicks", "maxEffectPacketsPerTick", "enabled"})
            assertTrue(cfg.contains("\"" + key + "\""), "config key missing: " + key);
        String boot = read("perf/PerfBoot.java");
        assertTrue(boot.contains("nusmp-performance-common.toml"));
        assertTrue(read("client/perf/PerfClient.java").contains("nusmp-performance-client.toml"));
        String main = read("NUSMP.java");
        assertTrue(main.contains("perf.PerfBoot.init(") && main.contains("client.perf.PerfClient.init("), "NUSMP does not wire the performance pass");
        assertTrue(boot.contains("PerfSelfTests.register()") && read("perf/PerfSelfTests.java").contains("\"perf.core\""));
    }

    @Test
    void theCommandsExistOnBothSides() throws IOException {
        assertTrue(read("perf/PerfCommands.java").contains("Commands.literal(\"multiverse\")") && read("perf/PerfCommands.java").contains("Commands.literal(\"perf\")"));
        assertTrue(read("client/perf/PerfClient.java").contains("Commands.literal(\"nusmp\")") && read("client/perf/PerfClient.java").contains("Commands.literal(\"perf\")"));
    }

    @Test
    void theLangFragmentIsFlatJson() throws IOException {
        Path p = Path.of("tools/lang/performance.json");
        assertTrue(Files.exists(p), "tools/lang/performance.json is missing");
        String text = Files.readString(p).trim();
        assertTrue(text.startsWith("{") && text.endsWith("}"));
        assertEquals(-1, text.indexOf('{', 1), "the fragment must be flat (key -> text)");
    }
}
