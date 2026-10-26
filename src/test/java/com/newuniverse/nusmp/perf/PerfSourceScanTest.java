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
            "client/remodel/AbyssalRenderer.java", "client/thread/ThreadTrapRenderer.java", "client/grimoire/GrimoireFloatClient.java", "client/relics/djinn/DjinnArmorLayer.java",
            "vfx/client/layer/WindMcLayer.java",        // the Wind pixel look: its textures are static finals, its frame methods build no location
            "vfx/client/layer/WaterBcLayer.java", "vfx/client/WaterBcDraw.java", "vfx/client/WaterBcFrame.java",        // the Water hybrid: static textures, a fixed plan, a mesh draw of three passes
            "vfx/client/layer/FireV2Layer.java",       // the Fire V2 look: the same, its textures are static finals resolved once
            "client/mode/ModeArmorValkyrieV2.java", "client/mode/ModeArmorValkyrieWorld.java", "client/mode/ModeArmorValkyrieEvents.java",       // the Valkyrie armour v2: static textures, a plan once a frame, no location built per frame
            "vfx/client/layer/StarMagicLayer.java", "client/prop/StarPropPainter.java",
            "vfx/client/layer/JudgmentLayer.java",       // the Judgment effects: static final textures, no location built per frame
            "vfx/client/layer/TrialLayer.java",          // the Trial effects (Israfil): static final textures, no location built per frame
            "vfx/client/layer/JibrilLayer.java",         // the Jibril effects (Lord of Rigor): static final textures, no location built per frame
            "vfx/client/layer/RuneWritingLayer.java", "client/manamethod/RuneClient.java");       // the Mana Method rune (3.5.0, the held rune is drawn from a view every frame): static textures, no location built per frame or per tick       // the Star V2 look: the layer's textures are static finals, the painter resolves its textures once in register()

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
    void theManaMethodRuneStateTickIsIdleWithNobodyOnAndRunsOncePerSecond() throws IOException {
        String tick = code(method("mana/method/RuneState.java", "onServerTick").body());
        int empty = tick.indexOf("ACTIVE.isEmpty()"), modulo = tick.indexOf("% RuneAccess.SWEEP_TICKS"), sweep = tick.indexOf("sweep(");
        assertTrue(empty >= 0 && empty < modulo && modulo < sweep, "RuneState.onServerTick must look at the empty map, then the once-a-second modulo, before it sweeps");
        String sw = code(method("mana/method/RuneState.java", "sweep").body());
        assertFalse(ENTITY_SCAN_ANY.matcher(sw).find() || sw.contains("getChunk") || sw.contains("setBlock"), "the rune state sweep reads only its own map and the players' stamps");
        String net = code(read("mana/method/RuneState.java"));
        assertFalse(net.contains("sendToAllPlayers") || net.contains("sendToPlayersNear"), "the rune state packet goes to the player themselves only");
    }

    private static final Pattern ENTITY_SCAN_ANY = Pattern.compile("getEntitiesOfClass|getEntities\\(|getNearbyEntities");

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
        PROBES.put("franchise/FranchiseEvents.java", "s.franchise.hit");                    // 1.112.0 (lag audit): the new handlers are measurable
        PROBES.put("relics/wave/Wave101Events.java", "s.relic.w101");
        PROBES.put("relics/wave/Wave102Events.java", "s.relic.w102");
        PROBES.put("client/prop/CompassPropPainter.java", "c.compass.paint");
        PROBES.put("mana/method/RuneManager.java", "s.mana.method");                                    // 3.6.0: the Mana Method's casts, the body runes and the painted runes are measurable
        PROBES.put("mana/method/AirStep.java", "s.mana.airstep");
        PROBES.put("mana/method/BodyRunes.java", "s.mana.body");
        PROBES.put("mana/method/BrushRunes.java", "s.mana.brush");
        PROBES.put("mana/method/RuneState.java", "s.mana.state");                                       // 3.8.0: the TIMED rune state sweep is measurable
        PROBES.put("shishou/ShishouService.java", "s.shishou.hold");                                    // Shishou: the holder tick, the casts, the blows and the server tick are measurable
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

    // ================================================================ lag audit 1 (docs/perf/lag_audit_1.md): the patterns it fixed
    private static List<Method> methodsNamed(String rel, String name) throws IOException {
        List<Method> out = new ArrayList<>();
        for (Method m : methods(read(rel), rel)) if (m.name().equals(name)) out.add(m);
        assertFalse(out.isEmpty(), rel + " has no method " + name);
        return out;
    }

    private static final Pattern NEW_OBJECT = Pattern.compile("\\bnew\\s+[A-Z\\w.<>]+\\s*[(\\[]");
    private static final Pattern ENTITY_SCAN = Pattern.compile("getEntitiesOfClass\\(|getEntities\\(|getNearbyEntities\\(|getEntitiesOfClass");
    private static final Pattern THROTTLE = Pattern.compile("% 20|% 10|% 200|% 5\\b|tickCount %|PerfMath\\.due|dueFor\\(|entityScanInterval");

    @Test
    void noUnthrottledEntityScanInATickMethodOfTheNewPackages() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path f : javaFiles()) {
            String rel = SRC.relativize(f).toString().replace('\\', '/');
            boolean mine = rel.startsWith("franchise/") || rel.startsWith("compass3d/") || rel.startsWith("client/compass/") || rel.startsWith("scalecast/") || rel.startsWith("client/scalecast/") || rel.matches("relics/wave/Wave10[1-5].*\\.java");
            if (!mine) continue;
            String src = Files.readString(f);
            if (!ENTITY_SCAN.matcher(src).find()) continue;
            for (Method m : methods(src, rel)) {
                String body = code(m.body());
                if (PER_FRAME_OR_TICK.matcher(m.name()).matches() && ENTITY_SCAN.matcher(body).find() && !THROTTLE.matcher(body).find()) bad.add(rel + " " + m.name());
            }
        }
        assertTrue(bad.isEmpty(), "an entity scan in a per-tick or per-frame method without a throttle (a modulo of the tick count or PerfMath.due): " + bad);
    }

    @Test
    void theFranchiseHotPathsDoNotRebuildCopyOrConcatenate() throws IOException {
        String ev = code(read("franchise/FranchiseEvents.java"));
        String actives = code(method("franchise/FranchiseEvents.java", "actives").body());
        assertTrue(actives.contains("st.cache != null") && actives.contains("st.cacheMain == main") && actives.contains("st.cacheWorn == worn"), "the active list is no longer kept per player");
        assertTrue(actives.indexOf("main.isEmpty()") >= 0 && actives.indexOf("main.isEmpty()") < actives.indexOf("new LinkedHashMap"), "a player with no engraving must cost no allocation");
        assertTrue(ev.contains("record Active(Engraving eng, Effect fx, int level, String key)"), "the cooldown key is built per call again");
        assertFalse(ev.contains("String key() {"), "Active#key builds a string per call again");
        String tick = code(method("franchise/FranchiseEvents.java", "tick").body());
        assertEquals(1, Pattern.compile("regenAround\\(").matcher(tick).results().count(), "the ally pulse must scan once per second per player, however many auras are worn");
        String regen = code(method("franchise/FranchiseEvents.java", "regenAround").body());
        assertTrue(regen.contains("MAX_AURA_TARGETS") && regen.contains("players().size()"), "the ally pulse lost its target cap or its no-one-else shortcut");
        String worn = code(method("franchise/FranchiseEvents.java", "readWorn").body());
        assertTrue(worn.contains("equippedCached(") && !worn.contains("CuriosLink.equipped("), "the franchise pass must share the Curios read");
        assertFalse(worn.contains("new LinkedHashMap") || worn.contains("new HashMap"), "readWorn allocates for a player with nothing");
        String hit = code(method("franchise/FranchiseEvents.java", "onIncoming0").body());
        assertTrue(hit.indexOf("instanceof ServerPlayer") >= 0 && hit.indexOf("instanceof ServerPlayer") < hit.indexOf("BYPASSES_INVULNERABILITY"), "onIncoming must leave the hits no player is part of before it looks at tags");
        String eng = code(read("franchise/FranchiseEngravings.java"));
        assertFalse(eng.contains("copyTag()"), "the franchise pass copies the custom data tag again");
        assertTrue(eng.contains("BY_ITEM.get(") && eng.contains("LINES.get("), "the item and tooltip caches are gone");
        assertFalse(code(method("franchise/FranchiseEngravings.java", "tooltipLine").body()).contains("FranchiseData.table().franchise(fid);\n        if (f == null) return null;\n        Component name"), "the tooltip line is built per call again");
        for (String rel : new String[]{"item/WeaponEngravings.java", "item/ArmorEngravings.java"})
            assertFalse(code(method(rel, "engrave").body()).contains("copyTag()"), rel + " copies the tag of every stack every second again");
        assertTrue(code(method("item/ArmorEngravings.java", "forItem").body()).contains("FOR_ITEM"), "the armour engraving lookup lost its cache");
        String style = code(method("tooltip/UnifiedTooltipItems.java", "styleOf").body());
        assertTrue(style.contains("kept(") && !style.contains("hintsOf("), "the tooltip style is worked out from the item's class chain on every frame again");
        assertTrue(code(method("tooltip/UnifiedTooltipItems.java", "categoryOf").body()).contains("kept("));
    }

    @Test
    void theCuriosReadIsSharedByTheOncePerSecondPasses() throws IOException {
        String passives = code(method("item/CuriosLink.java", "onPlayerTick0").body());
        assertTrue(passives.contains("equippedCached(") && !passives.contains(" equipped(p)"), "the sheathed-weapon pass reads Curios on its own again");
        String cached = code(method("item/CuriosLink.java", "equippedCached").body());
        assertTrue(cached.contains("tickCount") && cached.contains("SEEN.size() >"), "the shared Curios read lost its age test or its size bound");
    }

    @Test
    void theNewWaveListenersLeaveTheHitsOfMobsBeforeAnyClosure() throws IOException {
        String w101 = code(method("relics/wave/Wave101Events.java", "onIncoming0").body());
        assertTrue(w101.indexOf("instanceof ServerPlayer") >= 0 && w101.indexOf("instanceof ServerPlayer") < w101.indexOf("SafeCast.tick"), "Wave101Events.onIncoming builds a guard for hits no player is part of");
        String w102 = code(method("relics/wave/Wave102Events.java", "onIncoming0").body());
        assertTrue(w102.indexOf("containsKey(") >= 0 && w102.indexOf("containsKey(") < w102.indexOf("SafeCast.tick"), "Wave102Events.onIncoming builds a guard for players who hold none of its weapons");
        String tick = code(method("relics/wave/Wave101Events.java", "onPlayerTick0").body());
        assertTrue(tick.indexOf("active().isEmpty()") >= 0 && tick.indexOf("active().isEmpty()") < tick.indexOf("SafeCast.tick"), "Wave101Events.onPlayerTick works for players without a working relic");
        assertTrue(tick.indexOf("% 20") >= 0 && tick.indexOf("% 20") < tick.indexOf("Relics.loadout"), "Wave101Events.onPlayerTick lost its once-a-second stagger");
        String wearer = code(method("relics/wave/Wave101Events.java", "wearer").body());
        assertTrue(wearer.indexOf("flags().isEmpty()") >= 0 && wearer.indexOf("flags().isEmpty()") < wearer.indexOf("RelicFlags.has"), "the wearer's side asks flags one by one for players with none");
        for (String rel : new String[]{"relics/wave/Wave101Events.java", "relics/wave/Wave102Events.java"})
            assertEquals(1, Pattern.compile("\"w10[12]_\"\\s*\\+").matcher(code(read(rel))).results().count(), rel + " builds a flag key by concatenation outside its FLAGS table");
        String set = code(method("relics/wave/Wave101Set.java", "count").body());
        assertFalse(set.contains("new HashSet<>();\n        for") && set.indexOf("isEmpty()") < 0, "the Millennium set count allocates for everyone");
        assertTrue(set.contains("active.isEmpty()"));
        assertFalse(code(method("relics/wave/Wave20Util.java", "heldId").body()).contains("Optional") || code(method("relics/wave/Wave20Util.java", "heldId").body()).contains(".map("), "heldId makes an Optional per swing again");
    }

    @Test
    void theRelicLookEvictsOnlyWhatIsStaleAndAllocatesNoArrays() throws IOException {
        String cached = code(method("relics/Relics.java", "cached").body());
        assertTrue(cached.contains("evict(cache, now)") && !cached.contains("cache.clear()"), "a full relic table is cleared whole again (every player rescans on the same tick)");
        String scan = code(method("relics/Relics.java", "scan").body());
        assertFalse(scan.contains("new ItemStack[]") || scan.contains("new EquipmentSlot[]"), "the relic scan allocates arrays per look again");
    }

    @Test
    void theArmourFailsafeAndTheLayerCheckAllocateAndBuildNothingPerFrame() throws IOException {
        String fs = code(method("client/relics/RelicWornLayer.java", "armourFailsafe").body());
        assertFalse(fs.contains("new ArmorVisible.Facts"), "the failsafe makes a Facts record per armour relic per wearer per frame again");
        assertFalse(fs.contains("ArmorSlotPlan.decide(") || fs.contains("toLowerCase") || fs.contains("+ \"_\""), "the failsafe builds plans or strings per frame");
        String verify = code(method("client/relics/RelicTextures.java", "verify").body());
        assertFalse(verify.contains("armorLayer("), "verify builds a location from strings for every armour relic every ten seconds again");
        assertTrue(verify.contains("OWN_LAYERS.entrySet()"));
        assertTrue(code(method("client/relics/RelicTextures.java", "onClientTick").body()).contains("nextCheck"), "the layer check lost its interval");
        String resolve = code(read("client/relics/worn/ArmorVisible.java"));
        assertTrue(resolve.contains("public static Outcome resolve(ArmorSlotPlan.Verdict plan, int modelPieces"), "the allocation-free resolve is gone");
    }

    @Test
    void theWeaponGripCostsOneArrayReadPerFrame() throws IOException {
        String shift = code(method("visual/grip/GripState.java", "shift").body());
        assertTrue(shift.indexOf("cache[ctx]") >= 0 && shift.indexOf("cache[ctx]") < shift.indexOf("new float"), "GripState.shift must answer from its cache before it works anything out");
        assertFalse(code(method("client/WeaponGrip.java", "index").body()).contains("new "), "WeaponGrip.index allocates");
        String render = code(read("client/WeaponRenderer.java"));
        assertTrue(render.contains("mesh.grip.shift(gripCtx"), "the renderer no longer asks the cached shift");
        assertTrue(code(method("client/WeaponGrip.java", "displays").body()).contains("DISPLAYS.get(id)"), "the hand display entries are read from the resource manager per call again");
    }

    @Test
    void theCompassHasLevelsOfDetailABudgetAndNoPerFrameAllocation() throws IOException {
        // the painter asks for a level before it draws anything
        for (String name : new String[]{"main0", "arrow", "seal", "turn", "route"}) {
            String body = code(methodsNamed("client/prop/CompassPropPainter.java", name).get(0).body());
            assertTrue(body.contains("CompassFrame.") && body.contains("CompassLod.CULLED"), "CompassPropPainter." + name + " draws without a level of detail");
            assertFalse(body.contains("new Vec3") || body.contains(".getPosition(") || body.contains(".add(") || body.contains(".subtract("), "CompassPropPainter." + name + " allocates vectors per frame");
        }
        String painter = code(read("client/prop/CompassPropPainter.java"));
        assertFalse(painter.contains("CompassRules.dash("), "the painter allocates a float[2] per dash per frame again");
        // the renderer: no allocation, scratch sinks
        for (String name : new String[]{"compass", "arrow", "routePiece", "aura", "glint", "ring", "softGlow", "trail"})
            for (Method m : methodsNamed("client/compass/CompassRender.java", name)) assertFalse(NEW_OBJECT.matcher(code(m.body())).find(), "CompassRender." + name + " allocates per frame");
        // the frame helper: one answer per prop per frame, no allocation, the budget is the shared one
        for (String name : new String[]{"main", "small", "arrow", "route", "level"})
            for (Method m : methodsNamed("client/compass/CompassFrame.java", name)) assertFalse(NEW_OBJECT.matcher(code(m.body())).find(), "CompassFrame." + name + " allocates per frame");
        String frame = code(read("client/compass/CompassFrame.java"));
        assertTrue(frame.contains("PerfBudgets.COMPASS_QUADS") && frame.contains("CHOSEN.getOrDefault") && frame.contains("RenderLevelStageEvent.Stage.AFTER_SKY"), "the compass frame budget or its once-per-frame roll is gone");
        // the mesh keeps its lower levels and the render path asks them
        String render = code(read("client/compass/CompassRender.java"));
        assertTrue(render.contains("bodyLayer(") && render.contains("CompassLod.LOW") && render.contains("static final PoseSink SOLID"), "CompassRender lost its levels of detail or its scratch sinks");
        assertTrue(code(read("compass3d/CompassMesh.java")).contains("RIM_1") && code(read("compass3d/CompassMesh.java")).contains("FACE_2"), "the compass mesh lost its lower levels");
    }

    @Test
    void theCompassServerSideIsCappedThrottledAndCleanedUp() throws IOException {
        String kit = code(read("compass3d/CompassKit.java"));
        assertTrue(code(method("compass3d/CompassKit.java", "spawn").body()).contains("globalLive("), "the server-wide cap of the compass props is gone");
        assertTrue(kit.contains("CompassRules.shouldSetWord("), "the compass word is written on every aim change again");
        assertTrue(kit.contains("PlayerLoggedOutEvent") && kit.contains("ServerStoppingEvent"), "the per-caster tables are not dropped on log out and server stop");
        assertTrue(code(read("prop/CompassProps.java")).contains("CompassKit.register()"));
        String rules = code(read("compass3d/CompassRules.java"));
        assertTrue(rules.contains("GLOBAL_NEEDLES") && rules.contains("AIM_RESEND_TICKS"));
    }

    // ================================================================ GUI Move (docs/ui/gui_move.md): the HUD of every movable GUI costs one lookup and some field writes per frame
    @Test
    void theGuiMoveFrameCostIsOneLookupPerElementAndNoAllocation() throws IOException {
        // every method a HUD element or a layer hook calls on every frame: no allocation, no string building, no config read, no reflection
        String[] frame = {"begin", "end", "hidden", "moved", "report", "reportDrawn", "note", "guiScale", "layerPre", "toLocalX", "toLocalY", "lastScale", "screenXf", "panelOf", "editing"};
        Pattern strings = Pattern.compile("\\+\\s*\"|String\\.format|\\.getString\\(|Component\\.|translatable|StringBuilder|\\.stream\\(|GuiMoveOptions\\.|\\.get\\(\\)");
        for (String name : frame) {
            for (Method m : methodsNamed("client/ui/move/GuiMove.java", name)) {
                String body = code(m.body());
                assertFalse(NEW_OBJECT.matcher(body).find(), "GuiMove." + name + " allocates per frame");
                assertFalse(strings.matcher(body).find(), "GuiMove." + name + " builds a string or reads the config per frame");
            }
        }
        // the pure math the frame calls
        for (String name : new String[]{"total", "own", "clampVisible", "axisGui", "leftGui", "topGui", "toLocal", "toScreen", "sane"}) {
            for (Method m : methodsNamed("client/ui/move/GuiMoveMath.java", name)) assertFalse(NEW_OBJECT.matcher(code(m.body())).find(), "GuiMoveMath." + name + " allocates per frame");
        }
        // an element is found by one map lookup (the id is a constant), and the layout is read from the config only when the file loads or changes
        String move = code(read("client/ui/move/GuiMove.java"));
        assertTrue(move.contains("GuiMoveRegistry.byId(id)"), "GuiMove no longer finds an element with one lookup");
        assertTrue(code(read("client/ui/move/GuiMoveRegistry.java")).contains("BY_ID.get(id)"), "the registry lookup is not a map lookup any more");
        assertTrue(move.contains("ModConfigEvent.Loading") && move.contains("ModConfigEvent.Reloading") && move.contains("GuiMoveOptions.loadAll()"), "the layout is not loaded on the config events");
        // the edit overlay is drawn by its own screen only: no HUD layer, no per-frame hook of edit mode in the always-on path
        String screen = code(read("client/ui/move/GuiMoveScreen.java"));
        assertFalse(screen.contains("RenderGuiLayerEvent") || screen.contains("registerAboveAll") || screen.contains("ClientTickEvent"), "edit mode must draw only while its screen is open");
        assertTrue(move.contains("new GuiMoveScreen(") && !move.contains("RenderGuiEvent"), "edit mode is opened by the key or the command, never drawn by a hook");
        // the always-on tick is a counter, a key poll and a flag
        assertFalse(NEW_OBJECT.matcher(code(method("client/ui/move/GuiMove.java", "onTick").body()).replace("new GuiMoveScreen(null)", "")).find(), "GuiMove's tick allocates");
    }

    /** Rooms rebuilt: the armour stand gate and the variant choice are generation-time work only: nothing per tick, no world scan, one plan per floor. */
    @Test
    void theRareStandAndTheRoomVariantsCostNothingPerTick() throws IOException {
        String gate = code(read("labyrinth/props/ArmourStandGate.java"));
        for (Method m : methods(read("labyrinth/props/ArmourStandGate.java"), "labyrinth/props/ArmourStandGate.java")) {
            assertFalse(PER_FRAME_OR_TICK.matcher(m.name()).matches(), "ArmourStandGate has a per-tick or per-frame method: " + m.name());
        }
        assertFalse(gate.contains("Tick") || gate.contains("getEntities") || gate.contains("getEntitiesOfClass") || gate.contains("level.getChunk"), "the stand gate scans the world or ticks");
        assertTrue(gate.contains("e.loadedFromDisk()") && gate.contains("templateSignature(stand)"), "the stand gate touches stands it must leave alone");
        assertFalse(code(read("labyrinth/props/PropsRegistry.java")).contains("Tick"), "the props registry listens to a tick");
        String variants = code(read("labyrinth/mega/gen/RoomVariants.java"));
        assertFalse(variants.contains("net.minecraft") || variants.contains("Minecraft") || variants.contains("ServerLevel"), "RoomVariants must stay pure");
        String placer = code(read("labyrinth/mega/gen/SectorPlacer.java"));
        assertEquals(1, placer.split("RareStands\\.plan\\(", -1).length - 1, "the stand plan is made once per floor, in the constructor of the placer");
        assertFalse(code(method("labyrinth/mega/gen/SectorPlacer.java", "placeOne").body()).contains("RareStands.plan("), "the stand plan is recomputed for every piece");
        String stands = code(read("labyrinth/props/ArmourStandRarity.java"));
        assertFalse(stands.contains("net.minecraft"), "ArmourStandRarity must stay pure");
    }
}
