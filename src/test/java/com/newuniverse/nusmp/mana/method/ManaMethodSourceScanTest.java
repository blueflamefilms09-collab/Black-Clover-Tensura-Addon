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
        assertEquals(5, count(net, "playToClient("), "the rune packet, the small hold packet (3.5.0), the body packet and the brush packet (3.6.0), the rune-state packet (3.8.0): all clientbound");
        assertTrue(net.contains(".optional()"), "optional: a client without the mod still joins");
        assertTrue(net.contains("static boolean valid(") && read(CLIENT.resolve("RuneClient.java")).contains("RuneNet.valid(p)"), "the client validates what it receives");
        assertTrue(net.contains("static boolean validHold(") && read(CLIENT.resolve("RuneClient.java")).contains("RuneNet.validHold(p)"), "and so it does for the hold packet");
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
        String begin = body(m.substring(m.indexOf("public static Begin begin(GrimoireBook book, ManasSkillInstance inst, ServerPlayer p, int mode, RuneSpec spec, long now, Charge rc)")), "begin");
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
        assertTrue(l.contains("SLOTS = 16") && l.contains("s.inst == inst"), "sixteen slots for the frames of the runes alive (six cast runes and eight painted ones, 3.6.0)");
        assertTrue(render.contains("v.paintTicks > 0") && render.contains("layout.bookLight = 0f"), "a painted rune (brush, body) has no grimoire, so no gold glow where the book would be (found by the preview)");
        String glyphs = code(read(SRC.resolve("mana/method/RuneGlyphLayout.java")));
        assertTrue(glyphs.contains("LightningCharge.arcsReached(writtenOf(0, prog[0]), writtenOf(1, prog[1]), writtenOf(2, prog[2]))"), "the arcs are asked with the written share of each ring, not with the timeline progress (the preview drew no arc)");
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
        int storm = shapes.indexOf("STORM_CANCEL,"), rune = shapes.indexOf("RUNE_WRITING,");
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
        assertEquals(List.of("mana_method_fire", "mana_method_water", "mana_method_wind", "mana_method_earth", "mana_method_true", "mana_method_air_step", "mana_method_dash", "mana_method_body", "mana_method_brush", "mana_method_state"), ids, "the five rune pages of 3.4.0, the two mobility pages, the Body and Brush pages (3.6.0), then the Rune State page (3.8.0) appended last");
        for (String id : ids) {
            assertFalse(AvatarSkillList.allowedPage(id, 10.0), "an Avatar never casts " + id);
            assertTrue(id.length() >= 3 && id.length() <= 48, "id length");
        }
        String c = code(pages);
        assertTrue(c.contains("RuneRules.COST_PERCENT, RuneRules.COST_FLOOR, RuneRules.COOLDOWN") && c.contains("RuneRules.COST_PERCENT_TRUE, RuneRules.COST_FLOOR_TRUE, RuneRules.COOLDOWN_TRUE"), "cost, floor and cooldown come from the rules");
        assertTrue(RuneRules.COST_PERCENT == 12.0 && RuneRules.COST_FLOOR == 300.0 && RuneRules.COOLDOWN == 160, "the mid tier of the 15-spells recipe (12 %, floor 300, 160 ticks)");
        assertTrue(RuneRules.COST_PERCENT_TRUE == 20.0 && RuneRules.COST_FLOOR_TRUE == 600.0 && RuneRules.COOLDOWN_TRUE >= 300 && RuneRules.COOLDOWN_TRUE <= 1400, "True Magic is a zone-tier page (20 %, floor 600, cooldown inside 300..1400), below the signature tier");
        assertTrue(c.contains("TrueMagicRules.isNatural(") && c.contains("TrueMagicRules.controlEnough("), "True Magic reuses the rules of the older page: natural attribute, Stage 3 control");
        assertEquals(7, count(c, "new BookPage("), "the True Magic page, the two mobility pages, the Body and Brush pages (3.6.0), the one factory of the four element pages and the Rune State page (3.8.0)");
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

    // ---------------------------------------------------------------- 3.5.0: hold-to-charge, the filter, True Magic's world and body, the mobility runes
    @Test
    void theHoldIsMeasuredByTheServerFromTheGrimoiresOwnHoldAndTheGrimoireHooksAreAppendOnly() throws IOException {
        String h = code(read(METHOD.resolve("RuneHold.java")));
        assertTrue(h.contains("Map<UUID, Holding> HOLDING") && h.contains("Map<UUID, Released> RELEASED"), "keyed by the player's UUID");
        assertTrue(h.contains("heldTicks") && h.contains("RuneCharge.of(held, min, max)"), "the server turns the ticks held into the charge");
        assertTrue(body(h, "tick").contains("HOLDING.isEmpty() && RELEASED.isEmpty()") && body(h, "tick").contains("now % SWEEP_EVERY != 0"), "idle: two isEmpty checks; busy: one sweep every few ticks");
        assertTrue(body(h, "onHeld").contains("if (!active()) return;") && body(h, "onRelease").contains("if (!active()) return;"), "the setting off is the 3.4.0 cast");
        assertTrue(h.contains("RuneNet.sendHold(p, RuneNet.PHASE_HOLD") && h.contains("RuneNet.PHASE_RELEASE") && h.contains("RuneNet.PHASE_CANCEL"), "three small packets at most");
        assertTrue(h.contains("RELEASED_TTL") && h.contains("STALE"), "an abandoned hold and an untaken release go by themselves");
        String book = code(read(SRC.resolve("book/GrimoireBook.java")));
        assertTrue(body(book, "onHeld").contains("if (isMethodPage(mode)) com.newuniverse.nusmp.mana.method.RuneHold.onHeld(this, instance, p, heldTicks, mode)"), "the hold hook, for Mana Method pages only");
        String rel = body(book, "onRelease");
        assertTrue(rel.indexOf("if (pendingUntil > now) return;") > 0 && rel.indexOf("RuneHold.onRelease(") > rel.indexOf("if (pendingUntil > now) return;"), "a repeat tap during the wind-up never overrides the pending charge");
        assertTrue(book.contains("ManaMethodPages.usable(this, i, e, page(mode).id())"), "the access mode, the element rule and the pages' own settings are asked in one place");
        String pages = code(read(METHOD.resolve("ManaMethodPages.java")));
        assertTrue(pages.contains("RuneHold.take(p.getUUID(), mode, p.level().getGameTime())"), "the cast takes the release's charge");
        assertFalse(pages.contains("KeyMapping"), "no key");
    }

    @Test
    void theRunePacketIsUnchangedAndTheHoldPacketIsSmallValidatedAndNearby() throws IOException {
        String net = code(read(METHOD.resolve("RuneNet.java")));
        String runePayload = net.substring(net.indexOf("record RunePayload"), net.indexOf("PHASE_HOLD ="));
        assertEquals(6, count(runePayload, "b.writeFloat("), "the 28 byte rune packet: six floats ...");
        assertTrue(runePayload.contains("b.writeByte(spec)") && runePayload.contains("b.writeByte(charge)") && runePayload.contains("b.writeVarInt(ownerId)"), "... and the three small fields, as in 3.4.0");
        String hold = net.substring(net.indexOf("record HoldPayload"), net.indexOf("static boolean validHold("));
        assertEquals(1, count(hold, "b.writeVarInt("), "the hold packet: the owner id ...");
        assertEquals(5, count(hold, "b.writeByte("), "... and five single bytes (about 7 bytes in all)");
        String send = body(net, "sendHold");
        assertTrue(send.contains("PerfBudgets.effectPacket(") && send.contains("sendToPlayersNear(") && send.contains("RANGE"), "nearby players only, through the effect packet budget");
        assertTrue(body(net, "validHold").contains("HOLD_CAP") && body(net, "validHold").contains("MIN_SPAN"), "the client checks the numbers it is sent");
    }

    @Test
    void theFilterWritesThePursueAndGrowCommandsIntoTheBoltOnceAtTheLaunch() throws IOException {
        String m = code(read(METHOD.resolve("RuneManager.java")));
        String launch = body(m, "launch");
        assertTrue(launch.contains("ManaMethodConfig.commandFilter() ? RuneCommands.inject(c.spec, c.rc, ManaMethodConfig.lightningChargeScale()) : RuneCommands.legacy(c.spec)"), "the setting off is the 3.4.0 bolt, value by value");
        assertTrue(launch.indexOf("c.bolt =") < launch.indexOf("acquire(c, look)"), "the commands are in the bolt before Homing looks for its target");
        String fly = body(m, "fly");
        for (String old : new String[]{"RuneRules.homingActive(", "RuneRules.HOMING_TURN", "RuneRules.hitRadius(", "RuneRules.growthScale("}) assertFalse(fly.contains(old), "the flight reads the bolt, not " + old);
        assertTrue(fly.contains("c.bolt.homingActive(c.age)") && fly.contains("c.bolt.hitRadius("), "the flight reads the injected state");
        String ras = code(read(METHOD.resolve("RuneCommands.java")));
        assertFalse(ras.contains("net.minecraft"), "the filter is pure");
        assertFalse(m.contains("extends Entity") || m.contains("EntityType."), "the rune is a small server record, not a vanilla entity (docs: why the rune is not an entity)");
    }

    @Test
    void trueMagicHurtsPhysicallyThroughTheBalanceLawAndAntiMagicStillCancelsByDefault() throws IOException {
        String m = code(read(METHOD.resolve("RuneManager.java")));
        String strike = body(m, "strike");
        assertTrue(strike.contains("TrueDamageRules.route(c.spec.trueMagic(), ManaMethodConfig.physicalDamage(), ManaMethodConfig.antiMagicStillCancels()"), "both settings");
        assertTrue(strike.contains("c.book.hurtDirect(") && strike.contains("c.book.hurt("), "the physical route and the old route");
        assertTrue(m.contains("new DamageSource(holder, caster, caster)"), "attributed to the caster, so a kill is theirs");
        assertTrue(m.contains("AntiMagic.isUser(pl)") && m.contains("return true;"), "a failed Anti-Magic lookup takes the old, safe route");
        String book = code(read(SRC.resolve("book/GrimoireBook.java")));
        String direct = body(book, "hurtDirect");
        assertTrue(direct.contains("BalanceLaw.damage(caster, target, r, masteryFrac(i))"), "the balance law's caps stay");
        assertTrue(direct.contains("target.isAlliedTo(caster)"), "never an ally");
        assertTrue(m.contains("TrueDamageRules.resistanceFactor("), "the resistance is only undone on the old route");
    }

    @Test
    void theCageAndTheLightningAreCappedGuardedAndTakenBack() throws IOException {
        String t = code(read(METHOD.resolve("TrueBlocks.java")));
        String cage = body(t, "placeCage");
        assertTrue(cage.contains("ManaMethodConfig.plantCage()") && cage.contains("levelAllows(level)") && cage.contains("TrueWorldRules.allowedCage(liveOf(owner), LEDGER.size())"), "the setting, the guarded worlds, the caps");
        assertTrue(cage.contains("free(level, cell, may)") && cage.contains("isUnobstructed("), "free cells only, never a cell a creature stands in");
        assertEquals(1, count(cage, "setBlock("), "one write per cell");
        assertTrue(cage.contains("LEDGER.add(") && cage.contains("TrueWorldRules.cageSeconds("), "on the ledger, taken back after the cage's time");
        assertTrue(body(t, "cancelOwner").contains("if (LEDGER.isEmpty()) return;") && body(t, "cancelOwner").contains("takeBack(l, e)"), "a player's blocks are taken back at once, nothing to do while the ledger is empty");
        String m = code(read(METHOD.resolve("RuneManager.java")));
        for (String ev : new String[]{"onDeath", "onDimension", "onLogout"}) assertTrue(body(m, ev).contains("dropOwner(p)"), ev + " drops the player's casts, hold and blocks");
        assertTrue(body(m, "dropOwner").contains("TrueBlocks.cancelOwner(p.getServer(), p.getUUID())") && body(m, "dropOwner").contains("RuneHold.cancelOwner(p.getUUID())"));
        assertTrue(body(m, "onServerStopping").contains("RuneHold.clearAll()") && body(m, "onServerStopping").contains("TrueBlocks.clearAll(server)"));
        assertTrue(body(m, "trueWorld").contains("TrueBlocks.place(") && body(m, "trueWorld").contains("caged == 0"), "the moss carpet is the fallback");
        String l = code(read(METHOD.resolve("TrueLightning.java")));
        String strike = body(l, "strike");
        assertTrue(strike.contains("ManaMethodConfig.lightningBolt()") && strike.contains("TrueBlocks.levelAllows(level)") && strike.contains("level.isLoaded(pos)") && strike.contains("may.test(level, pos)") && strike.contains("TrueWorldRules.takeBolt("), "setting, guarded worlds, loaded chunk, building permission, server-wide rate");
        assertEquals(1, count(strike, "addFreshEntity("), "at most one bolt per strike");
        assertTrue(strike.contains("setVisualOnly(!real)"), "visual only unless the full bolt is switched on and safe");
        for (String banned : new String[]{"getChunk(", "forceLoad", "setChunkForced", "addRegionTicket"}) assertFalse(l.contains(banned) || t.contains(banned), "no chunk is loaded: " + banned);
    }

    @Test
    void theAirStepIsABoundedLedgerThatIsTakenBackOnEveryExit() throws IOException {
        String a = code(read(METHOD.resolve("AirStep.java")));
        assertTrue(a.contains("Map<UUID, Session> SESSIONS") && a.contains("MobilityRules.MAX_SESSIONS"), "keyed by the player, at most sixteen at once");
        assertTrue(body(a, "onServerTick").contains("if (SESSIONS.isEmpty()) return;"), "idle: one isEmpty check");
        assertTrue(a.contains("PerfProbe.section(\"s.mana.airstep\")"), "probed");
        String place = body(a, "place");
        assertTrue(place.contains("MobilityRules.cellAllowed(") && place.contains("level.isLoaded(pos)") && place.contains("level.mayInteract(p, pos)") && place.contains("isAir()"), "caps, loaded chunks, building permission, air cells only");
        assertEquals(1, count(place, "setBlock("), "one write");
        assertTrue(a.contains("Blocks.BARRIER") && body(a, "remove").contains("is(Blocks.BARRIER)") && body(a, "remove").contains("DungeonGuard.internal("), "only a barrier that is still ours is taken back");
        assertTrue(body(a, "start").contains("TrueBlocks.levelAllows("), "never in the Labyrinth or a guarded world");
        assertTrue(body(a, "tickAll").contains("!p.isAlive()") && body(a, "tickAll").contains("p.level() != s.level") && body(a, "tickAll").contains("!ManaMethodConfig.mobility()"), "a dead, moved or switched-off session ends");
        assertTrue(body(a, "step").contains("MobilityRules.decide(p.isShiftKeyDown()"), "sneak drops");
        String boot = code(read(METHOD.resolve("ManaMethodBoot.java")));
        for (String h : new String[]{"AirStep::onServerTick", "AirStep::onDeath", "AirStep::onDimension", "AirStep::onLogout", "AirStep.onServerStopping(e.getServer())", "Dash::onLogout"}) assertTrue(boot.contains(h), "the boot wires " + h);
        assertFalse(a.contains("getChunk(") || a.contains("forceLoad"), "no chunk is loaded");
    }

    @Test
    void theDashSetsTheVelocityOnTheServerAndTellsTheClientAndKeepsACooldownPerPlayer() throws IOException {
        String d = code(read(METHOD.resolve("Dash.java")));
        assertTrue(d.contains("p.hurtMarked = true") && d.contains("p.setDeltaMovement("), "the new motion is sent to the player's own client");
        assertTrue(d.contains("Map<UUID, Long> LAST") && d.contains("MobilityRules.dashReady("), "a cooldown per player");
        assertTrue(d.contains("MobilityRules.fallAfterDash(p.fallDistance)") && !d.contains("fallDistance = 0"), "the fall is only forgiven a little, never reset");
        assertTrue(d.contains("PerfParticles.send("), "the puff goes through the particle budget");
        assertTrue(d.contains("MobilityRules.dashSpeed(") && d.contains("MobilityRules.dashVelocity("), "the maths is the pure, tested one");
    }

    @Test
    void theNewSettingsAreServerSettingsThatDefaultToTheNewBehaviourAndHaveHardRanges() throws IOException {
        String cfg = code(read(METHOD.resolve("ManaMethodConfig.java")));
        for (String key : new String[]{"holdToCharge", "minHoldTicks", "maxHoldTicks", "commandFilter", "mobility", "airStepSeconds", "dashStrength", "physicalDamage", "antiMagicStillCancels", "plantCage", "lightningBolt", "lightningRealBolt"})
            assertTrue(cfg.contains("\"" + key + "\""), "setting " + key);
        assertTrue(cfg.contains("define(\"holdToCharge\", true)") && cfg.contains("define(\"commandFilter\", true)") && cfg.contains("define(\"mobility\", true)") && cfg.contains("define(\"physicalDamage\", true)") && cfg.contains("define(\"antiMagicStillCancels\", true)") && cfg.contains("define(\"plantCage\", true)") && cfg.contains("define(\"lightningBolt\", true)"), "the new behaviour is on by default ...");
        assertTrue(cfg.contains("define(\"lightningRealBolt\", false)"), "... except the full vanilla bolt, which is opt-in");
        assertTrue(cfg.contains("RuneCharge.minHold(") && cfg.contains("RuneCharge.maxHold(") && cfg.contains("MobilityRules.seconds(") && cfg.contains("MobilityRules.strength("), "every number is clamped by the pure rules");
    }

    @Test
    void theHoldViewAndTheDrawingStayAllocationFreeAndBounded() throws IOException {
        String l = code(read(SRC.resolve("vfx/client/layer/RuneWritingLayer.java")));
        assertTrue(l.contains("RuneHoldView.lookup(RuneHoldView.keyOf(inst.seed))") && l.contains("RuneGlyphLayout.fillStaged("), "the held rune is drawn from its view");
        String c = code(read(CLIENT.resolve("RuneClient.java")));
        assertTrue(c.contains("RuneHoldView.forgetAll()"), "the views are forgotten when the world is left");
        assertTrue(body(c, "start").contains("RUNES.size() >= RuneGeneratorUtil.MAX_RUNES_DRAWN"), "a held rune counts against the six");
        assertTrue(c.contains("ORPHAN_TICKS"), "a release whose cast never comes ends by itself");
        String v = code(read(METHOD.resolve("RuneHoldView.java")));
        assertFalse(v.contains("net.minecraft") || v.contains("Minecraft"), "the timeline is pure");
        assertTrue(code(read(METHOD.resolve("RuneGlyphLayout.java"))).contains("public static void fillStaged("), "the staged drawing");
    }

    // ---------------------------------------------------------------- 3.6.0: the price, the lightning, the Body Rune and the Brush Rune
    @Test
    void thePriceFollowsTheChargeThroughOneLineInTheGrimoireAndRefundsARefusedCast() throws IOException {
        String g = code(read(SRC.resolve("book/GrimoireBook.java")));
        String cost = body(g, "getMagiculeCost");
        assertTrue(cost.contains("ManaMethodPages.isPage(p.id())") && cost.contains("ManaMethodPages.costFactor(mp, p.id(), mode)"), "the grimoire asks the Mana Method for the share, for its own pages only");
        assertTrue(cost.contains("entity instanceof ServerPlayer mp"), "only a server player has a charge waiting");
        String pages = code(read(METHOD.resolve("ManaMethodPages.java")));
        String factor = body(pages, "costFactor");
        assertTrue(factor.contains("RuneHold.peek(") && !factor.contains("RuneHold.take("), "the price peeks at the release, it never uses it up (the cast does)");
        assertTrue(factor.contains("ManaMethodConfig.costScalesWithCharge()") && factor.contains("AIR_STEP_ID.equals(pageId)"), "the setting turns it off and the Air Step Rune is not scaled");
        assertTrue(count(pages, "refund(b, i, p, mode, charge)") + count(pages, "paid(b, i, p, mode,") >= 8, "every refusal of a cast gives the price back");
        assertTrue(body(pages, "refund").contains("p.isCreative()"), "a creative player paid nothing and gets nothing");
        String rm = code(read(METHOD.resolve("RuneManager.java")));
        assertTrue(rm.contains("double costFraction") && rm.contains("Math.max(0.0, Math.min(1.0, costFraction))"), "the True Magic fuel is paid back on what was really paid, never more");
        String cost2 = code(read(METHOD.resolve("RuneCost.java")));
        assertFalse(cost2.contains("net.minecraft") || cost2.contains("Minecraft"), "the curve is pure");
        assertFalse(code(read(METHOD.resolve("RuneHold.java"))).contains("setCoolDown"), "the cooldown is not scaled by the charge");
    }

    @Test
    void theLightningScalesInsideItsCapsBehindASettingAndThroughTheExistingEffect() throws IOException {
        String rm = code(read(METHOD.resolve("RuneManager.java")));
        assertTrue(rm.contains("TrueLightning.strike(c.level, p, at, may, now) && ManaMethodConfig.lightningChargeScale() && !c.rc.legacy()) lightningFx(c, at)"), "the extra look comes only after a strike the existing allowance let through, with the setting on");
        String fx = body(rm, "lightningFx");
        assertTrue(fx.contains("VfxSpawn.send(") && fx.contains("VfxShape.LIGHTNING_SPEAR") && fx.contains("st.bolts()"), "the existing lightning effect, once per bolt of the plan");
        assertFalse(fx.contains("addFreshEntity") && fx.contains("LightningBolt"), "the real bolt stays off: no entity here");
        String lc = code(read(METHOD.resolve("LightningCharge.java")));
        assertFalse(lc.contains("net.minecraft") || lc.contains("Minecraft"), "the lightning plan is pure");
        assertTrue(lc.contains("MAX_BOLTS = 3") && lc.contains("MAX_RADIUS_MULT = 1.4"), "hard caps");
        String cmd = code(read(METHOD.resolve("RuneCommands.java")));
        assertTrue(cmd.contains("LightningCharge.radiusMult(") && cmd.contains("MAX_HIT_RADIUS") && cmd.contains("MAX_SPLASH"), "the radius grows inside the bolt state's own caps");
        String cfg = code(read(METHOD.resolve("ManaMethodConfig.java")));
        assertTrue(cfg.contains("\"lightningChargeScale\"") && cfg.contains("\"costScalesWithCharge\"") && cfg.contains("\"minCostFraction\""), "the settings that restore the old behaviour");
    }

    @Test
    void theBodyRuneIsTransientBoundedAndCleanedUpOnEveryExit() throws IOException {
        String b = code(read(METHOD.resolve("BodyRunes.java")));
        assertTrue(b.contains("addTransientModifier(") && !b.contains("addPermanentModifier("), "transient: a crash or a restart cannot leave a modifier behind");
        assertTrue(b.contains("a.removeModifier(id(i));") && b.indexOf("a.removeModifier(id(i));") < b.indexOf("addTransientModifier("), "removed before it is added: a second cast refreshes, it never stacks");
        assertTrue(b.contains("SESSIONS.size() >= BodyRuneRules.MAX_SESSIONS") && b.contains("Map<UUID, Session>"), "one session per player UUID, capped");
        assertTrue(b.contains("PerfProbe.section(\"s.mana.body\")") && b.contains("now % SWEEP_EVERY != 0") && b.contains("SESSIONS.isEmpty()"), "an idle server pays one check; the sweep runs every five ticks");
        assertTrue(b.contains("p.isRemoved() || !p.isAlive() || p.level() != s.level") && b.contains("ManaMethodConfig.bodyRunes()"), "a dead, removed or moved player, or the setting off, ends the runes");
        assertFalse(b.contains("getTickCount"), "time is the world clock");
        assertFalse(b.contains("MobEffects.LEVITATION") || b.contains("mayfly") || b.contains("setNoGravity") || b.contains("getAbilities") || b.contains("onUpdateAbilities"), "a body rune never grants flight or creative abilities (the invulnerability damage tag is not an ability)");
        String rm = code(read(METHOD.resolve("RuneManager.java")));
        assertTrue(body(rm, "dropOwner").contains("BodyRunes.cancelOwner(") && body(rm, "onServerStopping").contains("BodyRunes.clearAll()"), "gone on death, dimension change, logout and server stop");
        assertTrue(body(rm, "onServerTick").contains("BodyRunes.tick(server, world)"), "swept from the one server tick handler");
        String r = code(read(METHOD.resolve("BodyRuneRules.java")));
        assertFalse(r.contains("net.minecraft") || r.contains("Minecraft"), "the rules are pure");
    }

    @Test
    void theBrushRuneIsCappedBudgetedPermissionCheckedAndNeverLoadsAChunk() throws IOException {
        String b = code(read(METHOD.resolve("BrushRunes.java")));
        assertTrue(b.contains("BrushRules.allowed(own, PAINTS.size(), ManaMethodConfig.brushPerCaster(), ManaMethodConfig.brushGlobal())"), "both caps, asked before anything is made");
        assertTrue(b.contains("level.mayInteract(p, pos)") && b.contains("TrueBlocks.levelAllows(level)") && b.contains("getWorldBorder().isWithinBounds("), "spawn protection, the world border, the Labyrinth and the Dungeon Guard");
        assertTrue(b.contains("level.isLoaded(pos)") && b.contains("level.isLoaded(front)"), "only loaded chunks");
        assertFalse(b.contains("getChunk(") || b.contains("setBlock(") || b.contains("removeBlock(") || b.contains("addFreshEntity("), "no chunk load, no block, no entity");
        assertTrue(b.contains("new PerfBudget(\"mana-method-brush\", BrushRules.QUERIES_PER_TICK)") && b.contains("QUERIES.tryTake(now, 1)") && b.contains("BrushRules.due(now, a.id)"), "one staggered query per array, all together under a budget");
        assertEquals(1, count(b, "getEntitiesOfClass("), "exactly one entity query, of a small box");
        assertTrue(b.contains("Foes.isFoe(p, e)"), "never the caster or an ally");
        assertTrue(b.contains("RuneManager.beginAt(") && b.contains("!= RuneManager.Begin.OK) return false"), "the bolt goes through the same caps as every rune's; if they are full the array waits");
        assertTrue(b.contains("PerfProbe.section(\"s.mana.brush\")") && b.contains("if (PAINTS.isEmpty()) return;"), "probed; idle is one check");
        assertFalse(b.contains("getTickCount"), "time is the world clock");
        assertTrue(b.contains("List<Paint> PAINTS") && b.contains("cancelOwner(UUID owner)") && b.contains("clearAll()"), "a ledger keyed by owner, with clean-up");
        String rm = code(read(METHOD.resolve("RuneManager.java")));
        assertTrue(body(rm, "dropOwner").contains("BrushRunes.cancelOwner(") && body(rm, "onServerStopping").contains("BrushRunes.clearAll()") && body(rm, "onServerTick").contains("BrushRunes.tick(server, world)"), "cleaned on every exit, swept once a tick");
        assertTrue(body(rm, "beginAt").contains("activeCount(p.getUUID()) >= ManaMethodConfig.perCaster()") && body(rm, "beginAt").contains("ACTIVE.size() >= ManaMethodConfig.global()"), "a painted rune's bolt counts against the rune caps");
        String r = code(read(METHOD.resolve("BrushRules.java")));
        assertFalse(r.contains("net.minecraft") || r.contains("Minecraft"), "the rules are pure");
    }

    @Test
    void theNewPacketsAreSmallValidatedAndClientboundAndThePagesAreAppendedLastAndGated() throws IOException {
        String net = code(read(METHOD.resolve("RuneNet.java")));
        assertTrue(net.contains("static boolean validBody(") && net.contains("static boolean validBrush("));
        String c = code(read(CLIENT.resolve("RuneClient.java")));
        assertTrue(c.contains("RuneNet.validBody(p)") && c.contains("RuneNet.validBrush(p)"), "the client validates both");
        assertTrue(c.contains("BrushRules.MAX_PAINTED_DRAWN") && c.contains("BrushRules.PAINT_DISTANCE"), "at most eight painted runes drawn, none farther than 56 blocks");
        assertTrue(net.contains("sendToPlayersNear(") && count(net, "PerfBudgets.effectPacket(") >= 4, "the new packets go to nearby players through the effect packet budget");
        String pages = code(read(METHOD.resolve("ManaMethodPages.java")));
        assertTrue(pages.contains("BODY_ID, BRUSH_ID);") && pages.contains("ManaMethodConfig.bodyRunes()") && pages.contains("ManaMethodConfig.brushRunes()"), "appended last and behind their own settings");
        assertTrue(pages.contains("List.of(IDS, IDS_36, IDS_35, RUNE_IDS)"), "the older lists are still recognised at the tail");
        assertFalse(AvatarSkillList.allowedPage("mana_method_body", BodyRuneRules.COST_PERCENT) || AvatarSkillList.allowedPage("mana_method_brush", BrushRules.COST_PERCENT), "an Avatar never casts the new pages");
        String cfg = code(read(METHOD.resolve("ManaMethodConfig.java")));
        for (String k : new String[]{"bodyRunes", "brushRunes", "bodySeconds", "brushSeconds", "brushPerCaster", "brushGlobal"})
            assertTrue(cfg.contains("\"" + k + "\""), "setting " + k);
    }

    @Test
    void theTimeBasesOfTheLedgersAreOneClock() throws IOException {
        String rm = code(read(METHOD.resolve("RuneManager.java")));
        assertTrue(body(rm, "onServerTick").contains("server.overworld().getGameTime()"), "the hold sweep and the new ledgers run on the world clock, which is what they stamp");
        assertTrue(code(read(METHOD.resolve("AirStep.java"))).contains("tickAll(event.getServer().overworld().getGameTime())"), "the Air Step platform ends by the clock it was started with");
    }

    // ---------------------------------------------------------------- docs
    @Test
    void theDocsAndTheLangFragmentExist() throws IOException {
        String doc = read(Path.of("docs/magic/mana_method.md"));
        for (String must : new String[]{"NEW", "REPLACED", "KEPT", "worst case", "magic.mana_method", "not seen in a game", "RuneGeneratorUtil", "Homing", "Growth", "True Magic", "docs/previews/mana_method/", "Canon check", "Anti-Magic", "why the rune is not an entity", "Owner decisions"})
            assertTrue(doc.toLowerCase().contains(must.toLowerCase()), "docs/magic/mana_method.md lacks: " + must);
        String audit = read(Path.of("docs/magic/manamethod_audit.md"));
        for (String must : new String[]{"exists", "partly", "missing", "NEW", "REPLACED", "KEPT"}) assertTrue(audit.contains(must), "the audit lacks: " + must);
        String lang = read(Path.of("tools/lang/mana_method.json"));
        for (String id : new String[]{"mana_method_fire", "mana_method_water", "mana_method_wind", "mana_method_earth", "mana_method_true"})
            assertTrue(lang.contains("\"nusmp.mana_method.page." + id + ".desc\""), "a description for " + id);
        assertTrue(lang.contains("\"nusmp.mana_method.cast\""));
        for (String k : new String[]{"nusmp.mana_method.page.mana_method_air_step.desc", "nusmp.mana_method.page.mana_method_dash.desc", "nusmp.mana_method.air_step", "nusmp.mana_method.air_step_end"})
            assertTrue(lang.contains("\"" + k + "\""), "lang key " + k);
        for (String k : new String[]{"nusmp.mana_method.page.mana_method_body.desc", "nusmp.mana_method.page.mana_method_brush.desc", "nusmp.mana_method.body", "nusmp.mana_method.body_end", "nusmp.mana_method.brush"})
            assertTrue(lang.contains("\"" + k + "\""), "3.6.0 lang key " + k);
        for (String must : new String[]{"3.6.0", "Body Rune", "Brush Rune", "Gadjah", "Rill", "costScalesWithCharge", "lightningChargeScale", "bodyRunes", "brushRunes", "magic.mana_method.body", "magic.mana_method.brush", "ENOTFOUND", "Owner decisions (3.6.0)"})
            assertTrue(doc.contains(must), "the doc lacks " + must + " (3.6.0)");
        assertTrue(read(METHOD.resolve("ManaMethodPages.java")).contains("\"nusmp.mana_method.cast\""));
        for (String png : new String[]{"rune_ingame.png", "rune_glyph_rings.png", "rune_glyph_writing.png", "rune_glyph_alphabet.png", "rune_glyph_charge.png", "rune_glyph_lod.png", "rune_hold_tap.png", "rune_hold_half.png", "rune_hold_full.png", "rune_hold_compare.png",
                "rune_lightning_rune.png", "rune_lightning_strike.png", "rune_body.png", "rune_brush_floor.png", "rune_brush_surfaces.png"})
            assertTrue(Files.exists(Path.of("docs/previews/mana_method/" + png)), "preview " + png);
    }

    // ---------------------------------------------------------------- 3.8.0: access modes, the Rune State page, the user's own element
    @Test
    void theAccessDecisionIsAskedInOnePlaceAndTheOldGateIsKept() throws IOException {
        String book = code(read(SRC.resolve("book/GrimoireBook.java")));
        assertTrue(book.contains("if (isMethodPage(mode)) return com.newuniverse.nusmp.mana.method.ManaMethodPages.usable(this, i, e, page(mode).id());"), "the grimoire asks the Mana Method once, with its own instance");
        assertTrue(book.contains("if (isZoneGated(mode)) return com.newuniverse.nusmp.mana.zone.ManaZoneManager.gateOpenFor(e);"), "the Mana Zone, Omni Volley and Pressure Space keep the Mana Zone gate");
        assertTrue(book.indexOf("if (isMethodPage(mode)) return") < book.indexOf("if (isZoneGated(mode)) return"), "the Mana Method pages are decided before the zone-gated ones, as in 3.7.0");
        String pages = code(read(METHOD.resolve("ManaMethodPages.java")));
        String u = pages.substring(pages.indexOf("public static boolean usable(GrimoireBook b"));
        u = u.substring(0, u.indexOf("public static boolean elementOk"));
        assertTrue(u.contains("ManaMethodConfig.enabled()") && u.contains("RuneAccess.allowed(") && u.contains("elementOk(i, pageId)"), "enabled, then the access mode, then the element");
        assertTrue(u.contains("RuneAccess.needsGate(mode) && !statePage && ManaZoneManager.gateOpenFor(e)"), "the late-game gate is only evaluated in its own mode");
        assertTrue(u.contains("b.masteryFrac(i)") && u.contains("RuneState.on(e)"), "mastery of the page's own grimoire; the caster's own state");
        assertTrue(pages.contains("public static boolean usable(LivingEntity e) { return ManaMethodConfig.enabled() && ManaZoneManager.gateOpenFor(e); }"), "the 3.7.0 test is still there, unchanged");
        assertTrue(code(read(SRC.resolve("mana/zone/ManaZoneManager.java"))).contains("public static boolean gateOpenFor(LivingEntity e)"), "the gate itself was not touched");
        assertFalse(u.contains("getUUID") || u.contains("static Player") || u.contains("Minecraft"), "no static current player: the decision reads the entity and the instance it is given");
    }

    @Test
    void theElementRuleReadsOnlyWhatTheClientAlreadyHasAndTheCastChecksItAgain() throws IOException {
        String pages = code(read(METHOD.resolve("ManaMethodPages.java")));
        String el = pages.substring(pages.indexOf("public static boolean elementOk"));
        el = el.substring(0, el.indexOf("public static double costFactor"));
        assertTrue(el.contains("GrimoirePages.magicOf(i)") && el.contains("getString(\"Fusion\")") && el.contains("RuneOwnElement.allowed(true, pageId"), "the grimoire's magic and its fusion tag, both in the instance");
        assertTrue(body(pages, "castElement").contains("elementOk(i,"), "the cast looks again (a second, server-side check)");
        assertTrue(body(pages, "castElement").contains("paid(b, i, p, mode, ch, false)"), "a refused rune gives its price back");
        assertFalse(code(read(METHOD.resolve("RuneOwnElement.java"))).contains("net.minecraft") || code(read(METHOD.resolve("RuneOwnElement.java"))).contains("neoforged"), "the element rule is pure");
        assertFalse(code(read(METHOD.resolve("RuneAccess.java"))).contains("net.minecraft") || code(read(METHOD.resolve("RuneAccess.java"))).contains("neoforged"), "the access rule is pure");
        assertFalse(code(read(METHOD.resolve("RuneStateGate.java"))).contains("net.minecraft"), "the client flag holds no Minecraft type");
    }

    @Test
    void theRuneStateIsPerPlayerBoundedAndCleanedUpOnEveryExit() throws IOException {
        String st = code(read(METHOD.resolve("RuneState.java")));
        assertTrue(st.contains("Map<UUID, Long> ACTIVE") && !st.contains("static ServerPlayer") && !st.contains("static Player "), "keyed by the player's UUID, no static player");
        assertTrue(body(st, "onServerTick").contains("if (ACTIVE.isEmpty()) return;") && body(st, "onServerTick").contains("% RuneAccess.SWEEP_TICKS"), "idle: one isEmpty check; busy: one pass a second");
        assertTrue(body(st, "onServerTick").contains("server.overworld().getGameTime()") && st.contains("getServer().overworld().getGameTime()"), "the world clock, the one the stamps use");
        assertTrue(st.contains("Player.PERSISTED_NBT_TAG") && st.contains("\"OnUntil\"") && st.contains("\"CooldownUntil\""), "the stamps live in the player's persisted data, so a relog cannot reset the cooldown");
        for (String m : new String[]{"onDeath", "onDimension", "onLogout", "onLogin", "onServerStopping"})
            assertTrue(st.contains("void " + m + "("), "RuneState." + m);
        assertTrue(body(st, "onServerStopping").contains("endEarly(p, false)") && body(st, "onServerStopping").contains("clearAll()"), "the server stopping ends every state and forgets them");
        String boot = code(read(METHOD.resolve("ManaMethodBoot.java")));
        for (String m : new String[]{"RuneState::onServerTick", "RuneState::onDeath", "RuneState::onDimension", "RuneState::onLogout", "RuneState::onLogin", "RuneState.onServerStopping("})
            assertTrue(boot.contains(m), "wired: " + m);
        assertFalse(st.contains("getEntitiesOfClass") || st.contains("getChunk") || st.contains("setBlock") || st.contains("getBlockState"), "the state touches no world");
        assertTrue(st.contains("PerfProbe.section(\"s.mana.state\")"), "measurable");
    }

    @Test
    void theStatePacketIsTinyValidatedClientboundAndToThePlayerOnly() throws IOException {
        String net = code(read(METHOD.resolve("RuneNet.java")));
        assertTrue(net.contains("record StatePayload(boolean on, int secondsLeft)") && net.contains("static boolean validState("), "a boolean and a number of seconds, validated");
        assertTrue(net.contains("if (validState(p)) RuneStateGate.set(p.on(), p.secondsLeft())"), "the client validates what it receives and touches no client class");
        String st = code(read(METHOD.resolve("RuneState.java")));
        assertTrue(st.contains("PacketDistributor.sendToPlayer(p, new RuneNet.StatePayload(") && !st.contains("sendToAllPlayers") && !st.contains("sendToPlayersNear"), "to the player themselves only");
        assertTrue(count(st, "sendState(p, ") <= 8, "start, natural end, cut short, death: a handful of call sites, none per tick");
        assertTrue(code(read(CLIENT.resolve("RuneClient.java"))).contains("RuneStateGate.clear()"), "the client forgets the state on logout");
    }

    @Test
    void theRuneStatePageIsAppendedLastCostsNothingAndTheNewSettingsAreServerSettings() throws IOException {
        String pages = code(read(METHOD.resolve("ManaMethodPages.java")));
        assertTrue(pages.contains("new BookPage(STATE_ID, \"Rune State\", \"Mana Method: Rune State\", 0, 0, 0, ManaMethodPages::castState)"), "a free page");
        assertTrue(pages.indexOf("new BookPage(STATE_ID") > pages.indexOf("new BookPage(BRUSH_ID"), "appended after the Brush Rune, so no mode number moves");
        assertTrue(pages.contains("IDS = List.of(FIRE_ID, WATER_ID, WIND_ID, EARTH_ID, TRUE_ID, AIR_STEP_ID, DASH_ID, BODY_ID, BRUSH_ID, STATE_ID)") && pages.contains("IDS_36 = List.of(FIRE_ID, WATER_ID, WIND_ID, EARTH_ID, TRUE_ID, AIR_STEP_ID, DASH_ID, BODY_ID, BRUSH_ID)"), "the 3.6.0 order is kept as it was");
        assertTrue(body(pages, "castState").contains("RuneState.cast(p)") && body(pages, "castState").contains("RuneHold.take("), "the cast uses up a waiting release and starts the state");
        String cfg = code(read(METHOD.resolve("ManaMethodConfig.java")));
        for (String k : new String[]{"\"access\"", "\"passiveMastery\"", "\"timedSeconds\"", "\"timedCooldownSeconds\"", "\"ownElementOnly\""}) assertTrue(cfg.contains(k), "setting " + k);
        assertTrue(cfg.contains("defineEnum(\"access\", RuneAccess.Mode.PASSIVE)") && cfg.contains(".define(\"ownElementOnly\", true)"), "the owner's defaults: PASSIVE and own element only");
        assertTrue(cfg.contains("ModConfigSpec.EnumValue<RuneAccess.Mode> ACCESS") && read(METHOD.resolve("ManaMethodBoot.java")).contains("ModConfig.Type.SERVER"), "gameplay settings are SERVER settings");
        assertTrue(read(METHOD.resolve("RuneAccess.java")).contains("enum Mode { PASSIVE, TIMED, MANA_ZONE_GATE }"), "entries only ever appended");
    }

    @Test
    void the380DocAndLangFragmentExist() throws IOException {
        String doc = read(Path.of("docs/magic/mana_method.md"));
        for (String must : new String[]{"## 3.8.0", "manaMethod.access", "PASSIVE", "TIMED", "MANA_ZONE_GATE", "passiveMastery", "timedSeconds", "timedCooldownSeconds", "ownElementOnly", "mana_method_state", "magic.mana_method.access", "magic.mana_method.element", "Owner decisions (3.8.0)"})
            assertTrue(doc.contains(must), "the doc lacks " + must + " (3.8.0)");
        String lang = read(Path.of("tools/lang/mana_method.json"));
        for (String k : new String[]{"nusmp.mana_method.page.mana_method_state.desc", "nusmp.mana_method.state.start", "nusmp.mana_method.state.warn", "nusmp.mana_method.state.end", "nusmp.mana_method.state.cooling", "nusmp.mana_method.state.already", "nusmp.mana_method.state.cut"})
            assertTrue(lang.contains("\"" + k + "\""), "lang key " + k);
    }
}
