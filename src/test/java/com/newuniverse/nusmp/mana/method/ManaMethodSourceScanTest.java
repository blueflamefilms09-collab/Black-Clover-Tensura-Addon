package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.avatar.cast.AvatarSkillList;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Nothing can be played here, so the rules of the Mana Method that cost the server something are pinned by reading the source (CLAUDE.md rules 9, 11, 12): server-friendly (no
 * client class in common code but the two guarded files, no keybinds, a clientbound packet only), lag-free (the idle path, the caps, the budgets, no chunk loads, no allocation per
 * point), wired the way the audit says (pages last, the gate before the instant-page rule, one config file, one boot line), and documented.
 */
class ManaMethodSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path METHOD = SRC.resolve("mana/method"), CLIENT = SRC.resolve("client/manamethod");
    private static final Pattern CLIENT_CLASS = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.");

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    /** The code of a source without its comments (the pins look at code, not at prose). */
    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", ""); }

    private static List<Path> files(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) { return s.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
    }

    /** The body of the first method with this name (a brace matcher that skips strings, characters and comments). */
    private static String body(String src, String name) {
        Matcher m = Pattern.compile("(?m)^[ \\t]{0,8}(?:(?:public|private|protected|static|final|synchronized)\\s+)*[\\w<>\\[\\],.? ]+?\\s+" + Pattern.quote(name) + "\\s*\\([^;{}]*\\)\\s*(?:throws [\\w., ]+)?\\s*\\{").matcher(src);
        assertTrue(m.find(), "no method " + name);
        int open = m.end() - 1, depth = 0;
        for (int i = open; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '"') { for (i++; i < src.length() && src.charAt(i) != '"'; i++) if (src.charAt(i) == '\\') i++; }
            else if (c == '\'') { for (i++; i < src.length() && src.charAt(i) != '\''; i++) if (src.charAt(i) == '\\') i++; }
            else if (c == '/' && src.startsWith("//", i)) { int e = src.indexOf('\n', i); i = e < 0 ? src.length() : e; }
            else if (c == '/' && src.startsWith("/*", i)) { int e = src.indexOf("*/", i + 2); i = e < 0 ? src.length() : e + 1; }
            else if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return src.substring(open, i + 1);
        }
        return src.substring(open);
    }

    private static int count(String s, String needle) {
        int n = 0, at = 0;
        while ((at = s.indexOf(needle, at)) >= 0) { n++; at += needle.length(); }
        return n;
    }

    // ---------------------------------------------------------------- server friendly (rule 9)
    @Test
    void onlyTheTwoGuardedFilesNameAClientClassAndBothAreOnTheAllowList() throws IOException {
        List<String> named = new ArrayList<>();
        for (Path f : files(METHOD)) if (CLIENT_CLASS.matcher(code(read(f))).find()) named.add(f.getFileName().toString());
        assertEquals(List.of("ManaMethodBoot.java", "RuneNet.java"), named, "the boot (behind Dist.CLIENT) and the packet handler (a lambda that runs on the client) are the only ones");
        String allow = Files.readString(Path.of("docs/server_safe_allow.txt"), StandardCharsets.UTF_8);
        assertTrue(allow.contains("mana/method/ManaMethodBoot.java") && allow.contains("mana/method/RuneNet.java"));
        assertTrue(code(read(METHOD.resolve("ManaMethodBoot.java"))).contains("FMLEnvironment.dist == Dist.CLIENT) com.newuniverse.nusmp.client.manamethod.RuneClient.init()"), "the client holder is only touched on a client");
        for (Path f : files(METHOD)) assertFalse(code(read(f)).contains("Minecraft.getInstance"), f.getFileName() + " asks the client for the Minecraft instance");
    }

    @Test
    void thereIsNoKeybindAndTheOnlyPacketIsClientbound() throws IOException {
        for (Path dir : List.of(METHOD, CLIENT))
            for (Path f : files(dir)) {
                String c = code(read(f));
                assertFalse(c.contains("KeyMapping") || c.contains("RegisterKeyMappingsEvent") || c.contains("InputEvent"), f.getFileName() + " binds a key");
                assertFalse(c.contains("playToServer") || c.contains("sendToServer") || c.contains("playBidirectional"), f.getFileName() + " sends something to the server");
            }
        String net = code(read(METHOD.resolve("RuneNet.java")));
        assertEquals(1, count(net, "playToClient("));
        assertTrue(net.contains(".optional()"), "optional: a client without the mod still joins");
        assertTrue(net.contains("static boolean valid(") && read(CLIENT.resolve("RuneClient.java")).contains("RuneNet.valid(p)"), "the client validates what it receives");
        assertTrue(net.contains("PacketDistributor.sendToPlayersNear(") && !net.contains("sendToAllPlayers") && !net.contains("sendToAll("), "sent to nearby players only");
        assertTrue(net.contains("PerfBudgets.effectPacket("), "through the effect packet budget");
    }

    @Test
    void theStateIsKeyedByPlayerAndCleanedUpOnEveryExit() throws IOException {
        String m = code(read(METHOD.resolve("RuneManager.java")));
        assertTrue(m.contains("UUID ownerId") && m.contains("cancelOwner(UUID owner)"), "keyed by the player's UUID");
        for (String ev : new String[]{"onDeath(LivingDeathEvent", "onDimension(PlayerEvent.PlayerChangedDimensionEvent", "onLogout(PlayerEvent.PlayerLoggedOutEvent", "onServerStopping(MinecraftServer"})
            assertTrue(m.contains(ev), "clean-up on " + ev);
        String boot = code(read(METHOD.resolve("ManaMethodBoot.java")));
        for (String h : new String[]{"RuneManager::onServerTick", "RuneManager::onDeath", "RuneManager::onDimension", "RuneManager::onLogout", "RuneManager.onServerStopping(e.getServer())", "ManaMethodSelfTests.register()", "RuneNet::registerPayloads"})
            assertTrue(boot.contains(h), "the boot wires " + h);
        assertTrue(m.contains("p.isRemoved() || !p.isAlive() || p.level() != c.level"), "a cast of a dead, removed or moved caster is dropped by the tick itself");
        assertFalse(m.contains("static ServerPlayer") && m.contains("currentPlayer"), "no static current player");
        String tb = code(read(METHOD.resolve("TrueBlocks.java")));
        assertTrue(tb.contains("onServerStopping") || m.contains("TrueBlocks.clearAll(server)"), "the placed blocks are taken back when the server stops");
    }

    @Test
    void gameplaySettingsAreServerConfigsAndTheFileNameIsUnique() throws IOException {
        String boot = code(read(METHOD.resolve("ManaMethodBoot.java")));
        assertTrue(boot.contains("registerConfig(ModConfig.Type.SERVER, ManaMethodConfig.SPEC, \"nusmp-mana-method-server.toml\")"));
        assertFalse(boot.contains("ModConfig.Type.CLIENT"));
        int uses = 0;
        try (Stream<Path> w = Files.walk(SRC)) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".java"))::iterator) if (code(read(p)).contains("\"nusmp-mana-method-server.toml\"")) uses++;
        }
        assertEquals(1, uses, "the file name is registered once");
        String cfg = code(read(METHOD.resolve("ManaMethodConfig.java")));
        assertTrue(cfg.contains("define(\"enabled\", true)"), "the whole system has an off switch (the old behaviour: the pages are not usable)");
        assertTrue(cfg.contains("RuneRules.MAX_PER_CASTER") && cfg.contains("RuneRules.MAX_GLOBAL") && cfg.contains("TrueBlockRules.MAX_SECONDS"), "the caps can only be moved inside hard ranges");
    }

    // ---------------------------------------------------------------- no lag (rule 11)
    @Test
    void theIdlePathIsOneCheckAndEveryCostIsBounded() throws IOException {
        String m = code(read(METHOD.resolve("RuneManager.java")));
        String tick = body(m, "onServerTick");
        assertTrue(tick.contains("if (!ACTIVE.isEmpty())"), "no cast, no work");
        assertTrue(tick.contains("TrueBlocks.tick(server, now)"), "the expiry pass is the only other thing, and it returns at once on an empty ledger");
        assertTrue(code(read(METHOD.resolve("TrueBlocks.java"))).contains("if (LEDGER.isEmpty() || now % CHECK_EVERY != 0) return;"));
        assertTrue(m.contains("PerfProbe.section(\"s.mana.method\")") && m.contains("PerfProbe.begin()") && m.contains("PerfProbe.end(PROBE, t0)"), "the section is probed");
        // the caps are asked before anything is made
        String begin = body(m, "begin");
        assertTrue(begin.indexOf("ManaMethodConfig.perCaster()") > 0 && begin.indexOf("ManaMethodConfig.global()") > 0, "both caps");
        assertTrue(begin.indexOf("ManaMethodConfig.global()") < begin.indexOf("ACTIVE.add(c)"), "the caps come before the cast is added");
        assertTrue(begin.indexOf("ManaMethodConfig.enabled()") < begin.indexOf("ACTIVE.add(c)"), "the switch comes first");
        // one entity query when the bolt leaves the rune (Homing), one per move inside the shared budget; none elsewhere
        assertEquals(2, count(m, "getEntitiesOfClass("), "acquire and the sweep, nothing else");
        assertEquals(1, count(m, "acquire(c, look)"), "Homing looks once, at the launch");
        String fly = body(m, "fly");
        int budget = fly.indexOf("QUERIES.tryTake(now, 1)"), sweep = fly.indexOf("getEntitiesOfClass(");
        assertTrue(budget > 0 && sweep > budget, "the sweep sits behind the per-tick query budget");
        assertTrue(fly.contains("c.denied >= 2"), "a denied bolt is never blind for more than a few ticks");
        assertTrue(fly.contains("c.level.getEntity(c.target)"), "Homing finds its target by id, no scan");
        assertTrue(fly.contains("c.level.isLoaded(") && fly.contains("isWithinBounds("), "no chunk is loaded and the world border stops the bolt");
        assertTrue(fly.contains("RuneRules.MAX_FLIGHT_TICKS") && fly.contains("RuneRules.MAX_RANGE"), "a flight has a time and a range limit");
        assertTrue(fly.contains("c.age % RuneRules.TRAIL_EVERY == 0"), "a trail packet every few ticks, not every tick");
        assertTrue(count(m, "PerfParticles.send(") >= 3 && !m.contains("sendParticles("), "the particles go through the budget");
        assertTrue(m.contains("RuneRules.MAX_TARGETS"), "the splash is capped");
        assertTrue(m.contains("SafeCast.tick(\"mana-method\""), "a faulty cast cannot crash the tick");
    }

    @Test
    void placingBlocksNeverLoadsAChunkAndAsksPermissionForEveryCell() throws IOException {
        String t = code(read(METHOD.resolve("TrueBlocks.java")));
        for (String banned : new String[]{"getChunk(", "forceLoad", "setChunkForced", "addRegionTicket", "loadChunk"}) assertFalse(t.contains(banned), "a chunk must not be loaded: " + banned);
        String place = body(t, "place");
        assertTrue(place.contains("levelAllows(level)") && place.contains("ManaMethodConfig.trueBlocks()") && place.contains("TrueBlockRules.allowed("), "the setting, the guarded worlds and the caps");
        assertEquals(1, count(place, "setBlock("), "one write");
        assertTrue(place.indexOf("find(level") < place.indexOf("setBlock("), "only into a cell that was found free");
        assertTrue(t.contains("level.isLoaded(pos)") && t.contains("l.mayInteract(p, pos)"), "loaded chunks and the caster's building permission (spawn protection, border)");
        assertTrue(t.contains("Labyrinth.in(level)") && t.contains("DungeonGuard.on(level)"), "never in the Labyrinth or a world the Dungeon Guard holds");
        assertTrue(t.contains("GameRules.RULE_DOFIRETICK") && t.contains("ManaMethodConfig.trueFire()"), "fire needs the game rule and the setting");
        assertTrue(t.contains("state.getBlock()") && t.contains("getBlockState(e.pos).is(e.block)"), "a block is only taken back if it is still the one that was placed");
        assertTrue(t.contains("DungeonGuard.internal("), "taking a block back goes through the mod's own writer");
    }

    @Test
    void theClientDrawsBoundedAndAllocationFree() throws IOException {
        String c = code(read(CLIENT.resolve("RuneClient.java")));
        assertTrue(body(c, "onClientTick").contains("if (RUNES.isEmpty()) return;"), "no rune, no work");
        assertTrue(body(c, "onRender").contains("RUNES.isEmpty()"), "no rune, no book");
        assertTrue(c.contains("PerfBudgets.clientParticles(") && c.contains("RuneGeneratorUtil.distanceDetailSq(") && c.contains("PerfSettings.vfxQuality()"), "the sparks ask the budget, the distance and the quality");
        assertTrue(c.contains("RuneGeneratorUtil.MAX_RUNES_DRAWN") && c.contains("RUNES.size() >= RuneGeneratorUtil.MAX_RUNES_DRAWN"), "at most six runes at once");
        assertTrue(c.contains("RuneGlyphLayout.MAX_SPARKS_PER_TICK"), "the sparks per tick are capped");
        assertTrue(c.contains("SafeCast.client("), "a drawing fault never reaches the game loop");
        assertTrue(c.contains("ClientPlayerNetworkEvent.LoggingOut") && c.contains("RUNES.clear()"), "forgets everything when the world is left");
        assertTrue(c.contains("RuneGeneratorUtil.BOOK_DISTANCE") && c.contains("renderStatic(r.book, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND"), "the REAL grimoire model through the item renderer, within 32 blocks only");
        assertTrue(c.contains("GrimoireItem.withOpenView(") && c.contains("GrimoireCarry.OPEN_STEPS") && c.contains("BOOKS[e.ordinal()]"), "the open V of the mod's own grimoire, built once per element");
        assertTrue(c.contains("VfxManager.get().spawn(new VfxPayload(VfxShape.RUNE_WRITING.ordinal()"), "the picture is the vfx layer, so the effect quality and the frame vertex cap apply");
        assertFalse(body(c, "tick").contains("new "), "nothing is allocated per tick but what the particle engine takes");
        assertFalse(c.contains("Class.forName") || c.contains(".getMethod("), "no reflection");
        assertFalse(c.contains("RuneGeneratorUtil.geometry") || c.contains("toWorld(") || c.contains("strideFor(") || c.contains("newPointIndex("), "the old dot-ring drawing is gone");
        String u = code(read(METHOD.resolve("RuneGeneratorUtil.java")));
        assertFalse(u.contains("Geometry") || u.contains("BoundedCache") || u.contains("MAX_PER_TICK") || u.contains("RING_DENSITY"), "the dot-ring geometry and its particle budget are gone from the generator");
        for (Path f : files(METHOD)) assertFalse(code(read(f)).contains("Math.random()"), "no unseeded randomness in " + f.getFileName());
    }

    @Test
    void theWritingLayerIsRegisteredBudgetedAndAllocationFree() throws IOException {
        String l = code(read(SRC.resolve("vfx/client/layer/RuneWritingLayer.java")));
        assertTrue(l.contains("extends AbstractVfxLayer") && l.contains("EnumSet.of(VfxShape.RUNE_WRITING)"));
        assertTrue(l.contains("hardVertexCap(VfxShape shape) { return RuneGlyphLayout.MAX_QUADS * 4; }") && l.contains("vertexBudget(VfxShape shape) { return RuneGlyphLayout.MAX_QUADS * 4; }"), "ONE instance per rune with the vertex budget of the layout, nothing more");
        String render = body(l, "render");
        assertFalse(render.contains("new "), "nothing is allocated while a rune is drawn (the frame, the spec and the parameters are kept per instance)");
        assertTrue(render.contains("RuneGlyphLayout.levelFor(ctx.detail)"), "the manager's level of detail (quality x distance) chooses the level");
        assertTrue(l.contains("SLOTS = 8") && l.contains("s.inst == inst"), "eight slots for the frames of the runes alive");
        assertTrue(l.contains("VfxBlend.ADD"), "the glow blend");
        String vm = code(read(SRC.resolve("vfx/client/VfxManager.java")));
        assertTrue(vm.contains("register(new RuneWritingLayer())"), "registered with the manager");
        assertTrue(vm.contains("buffer.beginActivation(inst.layer.vertexBudget(inst.shape), inst.layer.hardVertexCap(inst.shape))"), "the buffer is told the layer's hard cap");
        assertEquals(3, count(vm, "inst.layer.hardVertexCap(inst.shape)"), "the frame plan counts the same cap (estimate and cost)");
        String ab = code(read(SRC.resolve("vfx/client/AbstractVfxLayer.java")));
        assertTrue(ab.contains("hardVertexCap(VfxShape shape) { return VfxVertexBuffer.MAX_VERTICES_PER_ACTIVATION; }"), "every other layer stays at 400 vertices");
        String vb = code(read(SRC.resolve("vfx/client/VfxVertexBuffer.java")));
        assertTrue(vb.contains("public static final int MAX_VERTICES_PER_ACTIVATION = 400;") && vb.contains("Math.min(limit, Math.max(MAX_VERTICES_PER_ACTIVATION, hardCap))"), "the shared cap is unchanged, a layer may ask for more only through its own hard cap");
        String shapes = read(SRC.resolve("vfx/VfxShape.java"));
        int storm = shapes.indexOf("STORM_CANCEL,"), rune = shapes.indexOf("RUNE_WRITING;");
        assertTrue(storm > 0 && rune > storm, "the shape is appended after the older entries (ordinals never move)");
        for (String tex : new String[]{"rune_stroke_gold", "rune_stroke_flame", "rune_stroke_water", "rune_stroke_wind", "rune_stroke_earth", "rune_stroke_lightning", "rune_stroke_plant", "rune_line_ring", "rune_arc_strip"}) {
            assertTrue(l.contains("\"" + tex + "\""), "the layer uses " + tex);
            assertTrue(Files.exists(Path.of("src/main/resources/assets/nusmp/textures/particle/" + tex + ".png")), tex + " ships");
        }
    }

    // ---------------------------------------------------------------- wiring
    @Test
    void theGrimoireAppendsTheFivePagesLastAndAsksTheGateBeforeTheInstantRule() throws IOException {
        String book = read(SRC.resolve("book/GrimoireBook.java"));
        int sig = book.indexOf("SignatureSpell.pages()"), truth = book.indexOf("TrueMagic.page()"), spirit = book.indexOf("SpiritControlPages.pages()"), mine = book.indexOf("ManaMethodPages.pages()");
        assertTrue(sig > 0 && truth > sig && spirit > truth && mine > spirit, "the Mana Method pages come after the spirit pages");
        assertEquals(1, count(book, "ManaMethodPages.pages()"));
        int usable = book.indexOf("public boolean usable("), method = book.indexOf("isMethodPage(mode)) return", usable), zone = book.indexOf("isZoneGated(mode)) return", usable), instant = book.indexOf("if (instant(mode)) return true;", usable);
        assertTrue(usable > 0 && method > usable && method < zone && zone < instant, "the Mana Method gate, then the Mana Zone gate, then the instant-page rule");
        assertTrue(book.contains("ManaMethodPages.isPage(page(mode).id())"));
        assertTrue(read(SRC.resolve("NUSMP.java")).contains("com.newuniverse.nusmp.mana.method.ManaMethodBoot.init(modEventBus, modContainer)"));
        String pages = code(read(METHOD.resolve("ManaMethodPages.java")));
        assertTrue(pages.contains("ManaZoneManager.gateOpenFor(e)") && pages.contains("ManaMethodConfig.enabled()"), "late-game gate and the switch");
    }

    @Test
    void theFivePagesHaveUniqueIdsNoKeyAndTheRecipeCosts() throws IOException {
        String pages = read(METHOD.resolve("ManaMethodPages.java"));
        Matcher m = Pattern.compile("(\\w+_ID) = \"([a-z_]+)\"").matcher(pages);
        List<String> ids = new ArrayList<>();
        while (m.find()) ids.add(m.group(2));
        assertEquals(List.of("mana_method_fire", "mana_method_water", "mana_method_wind", "mana_method_earth", "mana_method_true"), ids);
        for (String id : ids) {
            assertFalse(AvatarSkillList.allowedPage(id, 10.0), "an Avatar never casts " + id);
            assertTrue(id.length() >= 3 && id.length() <= 48, "id length");
        }
        String c = code(pages);
        assertTrue(c.contains("RuneRules.COST_PERCENT, RuneRules.COST_FLOOR, RuneRules.COOLDOWN") && c.contains("RuneRules.COST_PERCENT_TRUE, RuneRules.COST_FLOOR_TRUE, RuneRules.COOLDOWN_TRUE"), "cost, floor and cooldown come from the rules");
        assertTrue(RuneRules.COST_PERCENT == 12.0 && RuneRules.COST_FLOOR == 300.0 && RuneRules.COOLDOWN == 160, "the mid tier of the 15-spells recipe (12 %, floor 300, 160 ticks)");
        assertTrue(RuneRules.COST_PERCENT_TRUE == 20.0 && RuneRules.COST_FLOOR_TRUE == 600.0 && RuneRules.COOLDOWN_TRUE >= 300 && RuneRules.COOLDOWN_TRUE <= 1400, "True Magic is a zone-tier page (20 %, floor 600, cooldown inside 300..1400), below the signature tier");
        assertTrue(c.contains("TrueMagicRules.isNatural(") && c.contains("TrueMagicRules.controlEnough("), "True Magic reuses the rules of the older page: natural attribute, Stage 3 control");
        assertEquals(2, count(c, "new BookPage("), "the True Magic page and the one factory of the four element pages");
        assertFalse(c.contains("Pages15"), "the pages15 machinery is per family and mastery gated, so it is not used");
    }

    @Test
    void theOlderTrueMagicPageAndTheRuneStoneAreUntouched() throws IOException {
        String old = read(SRC.resolve("mana/zone/TrueMagic.java"));
        assertTrue(old.contains("public static final String ID = \"true_magic_rune_array\";") && old.contains("No world edits"), "the older page is as it was");
        assertTrue(read(SRC.resolve("item/MagicGear.java")).contains("public static boolean placeRune("), "the Rune Stone is kept");
        assertTrue(read(SRC.resolve("book/GrimoireBook.java")).contains("TrueMagic.page()"), "and still appended");
    }

    @Test
    void theWritingRunesAreTheOnlyLookAndAreWiredIntoTheGame() throws IOException {
        String net = code(read(METHOD.resolve("RuneNet.java")));
        assertTrue(net.contains("com.newuniverse.nusmp.client.manamethod.RuneClient.onRune(p)"), "the one packet reaches the client holder, which starts the writing");
        int users = 0;
        try (Stream<Path> w = Files.walk(SRC)) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String c = code(read(p));
                if (c.contains("RuneGlyphLayout") && !p.getFileName().toString().equals("RuneGlyphLayout.java")) users++;
            }
        }
        assertTrue(users >= 3, "the layout is used by the layer, the client holder and the self-test (" + users + ")");
        for (Path f : files(CLIENT)) assertFalse(code(read(f)).contains("sendParticles"), "no server particles from the client");
        String doc = read(Path.of("docs/magic/mana_method.md"));
        assertTrue(doc.contains("removed") && doc.contains("dot rings"), "the doc says exactly what was removed");
    }

    @Test
    void theTimeMagicVfxIsNotReferenced() throws IOException {
        for (Path dir : List.of(METHOD, CLIENT))
            for (Path f : files(dir)) {
                String c = read(f);
                assertFalse(c.contains("TimeMagicLayer") || c.contains("particle/time_") || c.contains("TimeBook"), f.getFileName() + " touches the Time Magic VFX");
            }
    }

    // ---------------------------------------------------------------- docs
    @Test
    void theDocsAndTheLangFragmentExist() throws IOException {
        String doc = read(Path.of("docs/magic/mana_method.md"));
        for (String must : new String[]{"NEW", "REPLACED", "KEPT", "worst case", "magic.mana_method", "not seen in a game", "RuneGeneratorUtil", "Homing", "Growth", "True Magic", "docs/previews/mana_method/"})
            assertTrue(doc.toLowerCase().contains(must.toLowerCase()), "docs/magic/mana_method.md lacks: " + must);
        String audit = read(Path.of("docs/magic/manamethod_audit.md"));
        for (String must : new String[]{"exists", "partly", "missing", "NEW", "REPLACED", "KEPT"}) assertTrue(audit.contains(must), "the audit lacks: " + must);
        String lang = read(Path.of("tools/lang/mana_method.json"));
        for (String id : new String[]{"mana_method_fire", "mana_method_water", "mana_method_wind", "mana_method_earth", "mana_method_true"})
            assertTrue(lang.contains("\"nusmp.mana_method.page." + id + ".desc\""), "a description for " + id);
        assertTrue(lang.contains("\"nusmp.mana_method.cast\""));
        assertTrue(read(METHOD.resolve("ManaMethodPages.java")).contains("\"nusmp.mana_method.cast\""));
        for (String png : new String[]{"rune_ingame.png", "rune_glyph_rings.png", "rune_glyph_writing.png", "rune_glyph_alphabet.png", "rune_glyph_charge.png", "rune_glyph_lod.png"})
            assertTrue(Files.exists(Path.of("docs/previews/mana_method/" + png)), "preview " + png);
    }
}
