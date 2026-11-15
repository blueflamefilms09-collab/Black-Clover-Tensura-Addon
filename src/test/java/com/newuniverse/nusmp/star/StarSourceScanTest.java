package com.newuniverse.nusmp.star;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: Star nodes run on a public server (rules 9 and 11) and nobody can play them here, so the rules are pinned by reading the real source. (1) Server safety: no class of the {@code star} package names a client
 * class, a packet class or a "current player" static; state is keyed by UUID; every effect goes through {@code StarFx}; no new packet is registered. (2) Bounds: placement goes through the two caps, a pane tick
 * asks ONE entity query and only while its pane lives, the bolt hook costs nothing without a pane, delayed parts are 100 ticks or less, damage only through {@code Ctx.hit}, no chunk is loaded, only
 * {@code MagicProps.spawn} makes entities. (3) Wiring: every hook is a small append (the page wrapper is outermost, the old casts are the fallbacks, enum entries only at the end, one registration each),
 * cleanup runs on logout, death, respawn, dimension change and server stop. (4) Halation is not built and the Time Magic VFX is not touched.
 */
class StarSourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) {
        try { return Files.readString(JAVA.resolve(rel), StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + rel, e); }
    }

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", ""); }

    private static List<Path> starFiles() throws IOException {
        List<Path> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(JAVA.resolve("star"), "*.java")) { for (Path p : ds) out.add(p); }
        return out;
    }

    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)");

    @Test
    void theStarPackageIsServerSafeKeyedByUuidAndSendsEffectsOnlyThroughStarFx() throws IOException {
        List<Path> files = starFiles();
        assertTrue(files.size() >= 10, "the package is there: " + files.size());
        for (Path f : files) {
            String c = code(Files.readString(f, StandardCharsets.UTF_8));
            String name = f.getFileName().toString();
            assertFalse(CLIENT.matcher(c).find(), name + " names a client class");
            assertFalse(c.contains("CustomPacketPayload") || c.contains("registrar(") || c.contains("PacketDistributor"), name + " defines or sends a packet of its own");
            assertFalse(Pattern.compile("static\\s+(?:volatile\\s+)?(?:ServerPlayer|Player|LivingEntity)\\s+\\w+\\s*[;=]").matcher(c).find(), name + " keeps a static current player");
            assertFalse(c.contains("TimeMagic") || c.contains("TimeBook"), name + " touches the Time Magic");
            assertFalse(c.contains("halation") || c.contains("Halation") || c.contains("HALATION"), name + ": Halation is not canon and is not built");
            if (!name.equals("StarFx.java")) assertFalse(c.contains("VfxSpawn") || c.contains("VfxPayload"), name + " sends an effect outside StarFx");
            assertFalse(c.contains("loadChunk") || c.contains("getChunk(") || c.contains("forceChunk") || c.contains("new Thread") || c.contains("Thread.sleep"), name + " loads chunks or makes threads");
        }
        for (String f : new String[]{"StarGame.java", "StarNodes.java", "StarPaneBook.java"}) {
            String c = code(read("star/" + f));
            for (Matcher m = Pattern.compile("static\\s+(?:final\\s+)?(?:Map|HashMap|Set|List)<([^>]+)>\\s+\\w+\\s*=").matcher(c); m.find(); )
                assertTrue(m.group(1).startsWith("UUID"), f + ": a static collection is keyed by UUID, not by " + m.group(1));
        }
        for (String pure : new String[]{"StarRules.java", "StarNodes.java", "StarMath.java", "StarPaneBook.java"})
            assertFalse(code(read("star/" + pure)).contains("net.minecraft") || code(read("star/" + pure)).contains("net.neoforged"), pure + " stays pure");
        String fx = code(read("star/StarFx.java"));
        assertTrue(fx.contains("VfxSpawn.send(level, new VfxPayload("), "the one send goes through the per-tick effect budget of VfxSpawn");
        assertTrue(fx.contains("CAST_PACKETS = 10") && fx.contains("left <= 0"), "a cast has ten packets at most");
    }

    @Test
    void theServerConfigIsALiteralFileWithEverySettingClampedWhenRead() {
        String boot = code(read("star/StarBoot.java"));
        assertTrue(boot.contains("ModConfig.Type.SERVER, StarConfig.SPEC, \"nusmp-star-server.toml\")"), "a SERVER config with a literal file name");
        assertTrue(read("star/StarRules.java").contains("\"nusmp-star-server.toml\""));
        String cfg = code(read("star/StarConfig.java"));
        for (String key : new String[]{"\"nodes\"", "\"maxPerPlayer\"", "\"maxPerHeir\"", "\"maxOnServer\"", "\"lifeTicks\"", "\"placeRange\"", "\"seedCooldown\"", "\"autoQuartet\"", "\"conjunctionFallback\"",
                "\"conjunctionMaxAllies\"", "\"heirMastery\"", "\"dualMode\""})
            assertTrue(cfg.contains(key), "setting " + key);
        assertTrue(cfg.contains("b.push(\"star\")") && cfg.contains("StarRules.DEFAULT_NODES") && cfg.contains("StarRules.DualMode.FUSION"), "star.nodes is on and star.dualMode is FUSION by default");
        for (String getter : new String[]{"StarRules.perPlayerCap(", "StarRules.serverCap(", "StarRules.lifeTicks(", "StarRules.placeRange(", "StarRules.seedCooldown(", "StarRules.maxAllies(", "StarRules.heirMastery("})
            assertTrue(cfg.contains(getter), "a getter clamps through " + getter);
        String main = code(read("NUSMP.java"));
        assertEquals(1, main.split("StarBoot\\.init\\(modContainer\\)", -1).length - 1, "one registration of the boot");
        assertEquals(1, main.split("StarV2SelfTests\\.register\\(\\)", -1).length - 1);
        assertTrue(boot.contains("StarSelfTests.register()") && read("star/StarSelfTests.java").contains("\"magic.star_nodes\"") && read("vfx/StarV2SelfTests.java").contains("\"magic.star_nodes_look\""), "the smoke keys start with magic.star_nodes");
    }

    @Test
    void placementGoesThroughBothCapsAndEveryCleanupEventIsListened() {
        String game = code(read("star/StarGame.java"));
        int place = game.indexOf("public static Placed placeAt(");
        assertTrue(place > 0);
        String placeBody = game.substring(place, game.indexOf("public static Placed seed(", place));
        for (String must : new String[]{"StarConfig.nodes()", "p.isAlive()", "StarConfig.placeRange()", "free(level, at)", "StarConfig.perPlayer(heir)", "StarConfig.serverCap()", "discardAll(", "MagicProps.spawn("})
            assertTrue(placeBody.contains(must), "placement validates / applies " + must);
        assertEquals(1, placeBody.split("MagicProps\\.spawn\\(", -1).length - 1, "one entity per placement");
        String boot = code(read("star/StarBoot.java"));
        for (String ev : new String[]{"PlayerLoggedOutEvent", "LivingDeathEvent", "PlayerRespawnEvent", "PlayerChangedDimensionEvent", "ServerStoppingEvent"})
            assertTrue(boot.contains(ev), "cleanup on " + ev);
        assertTrue(game.contains("public static void forget(ServerPlayer p)") && game.contains("NODES.clear(id)") && game.contains("PANES.clearOwner(id") && game.contains("LAST_SEED.remove(id)"), "forget() drops the stars, the pane and the cooldown");
        assertTrue(game.contains("public static void clearAll(MinecraftServer server)"));
        String props = code(read("star/StarProps.java"));
        assertTrue(props.contains("e.life() % 20 != 0") && props.contains("getPlayer(e.ownerId())"), "a star whose owner is gone ends on its own within a second");
        assertTrue(props.contains("StarGame.bodyGone(e)"), "an ended star leaves the registry");
    }

    @Test
    void thePaneCostsOneQueryPerTickWhileItLivesAndNothingWithoutAPane() {
        String props = code(read("star/StarProps.java"));
        assertEquals(1, props.split("getEntitiesOfClass\\(", -1).length - 1, "one entity query in the whole pane code");
        int tick = props.indexOf("private static void paneTick(");
        String body = props.substring(tick, props.indexOf("private static boolean hit(", tick));
        assertTrue(body.indexOf("if (pane == null || sl.getGameTime() >= pane.expiry) { e.expire(); return; }") < body.indexOf("getEntitiesOfClass("), "a dead pane returns before the query");
        assertTrue(body.contains("Projectile.class"), "only projectiles are asked for");
        String absorb = props.substring(props.indexOf("public static boolean absorbBolt("));
        assertTrue(absorb.indexOf("StarGame.PANES.size() == 0") > 0 && absorb.indexOf("StarGame.PANES.size() == 0") < absorb.indexOf("firstCrossed("), "no pane: no work");
        String rt = code(read("book/SpellRuntime.java"));
        assertTrue(rt.contains("StarProps.absorbBolt(b.level, b.pos, next, b.owner)"), "the mod's own bolts are absorbed by a pane");
        assertTrue(rt.indexOf("StarProps.absorbBolt") > rt.indexOf("wall.getType() != HitResult.Type.MISS) next = wall.getLocation();") && rt.indexOf("StarProps.absorbBolt") < rt.indexOf("for (LivingEntity e : b.level.getEntitiesOfClass(LivingEntity.class, new AABB(b.pos, next)"));
    }

    @Test
    void castsOnlyUseTheBoundedKitAndEveryDelayIsAtMostAHundredTicks() {
        String c = code(read("star/StarCasts.java"));
        assertFalse(c.contains(".hurt(") && !c.contains("c.hit("), "damage only through Ctx.hit");
        assertFalse(Pattern.compile("[^.]\\bhurt\\(").matcher(c).find(), "no direct hurt call");
        assertTrue(c.split("Pages15Kit\\.cast\\(").length - 1 >= 7, "every cast is a Pages15Kit.cast");
        assertTrue(c.contains("c.later(StarRules.HASTA_CHARGE_TICKS") && c.contains("c.later(4 * step"), "delays are the rule constants");
        assertTrue(StarRules.HASTA_CHARGE_TICKS <= 100 && 4 * StarRules.WHIP_STEPS <= 100);
        assertTrue(c.contains("c.zone(100, 20,"), "Black Neverland is one zone of 100 ticks in steps of 20");
        assertTrue(c.contains("StarRules.firstN(") && c.contains("StarRules.allyLimit(StarConfig.conjunctionAllies(), c.tier.maxTargets)"), "bounded ally list");
        assertTrue(c.contains("KitAllies.around(") && !c.contains("getEntitiesOfClass("), "the mod's own ally rule; no entity scan of its own");
        assertTrue(c.contains("StarGame.consume(p, List.of(star))") && c.contains("StarGame.consume(p, q)"), "Conjunction and Hasta consume their stars");
        int scutum = c.indexOf("public static BookPage.Cast scutum(");
        assertFalse(c.substring(scutum, c.indexOf("public static BookPage.Cast hasta(")).contains("consume("), "Scutum keeps the stars");
        int flag = c.indexOf("private static boolean lash(");
        assertFalse(c.substring(flag, c.indexOf("public static BookPage.Cast blackNeverland()")).contains("consume("), "the whip keeps the stars");
        assertTrue(c.contains("StarAccess.needsHeir(id, StarConfig.vfxForAll()) && !StarGame.isHeir(c.caster)"), "the heir gate (Black Neverland; every shared page too while star.vfxForAll is false) is checked on the server in every gated cast");
        assertTrue(c.contains("StarConfig.nodes() && (StarConfig.vfxForAll() || StarGame.isHeir(p)) ? whip.cast(b, i, p, mode) : cone.cast(b, i, p, mode)") && c.contains("StarConfig.nodes() ? nodes.cast(b, i, p, mode) : dash.cast(b, i, p, mode)"),
                "star.nodes = false restores the dash and the cone");
        assertTrue(c.contains("DAMAGE_RESISTANCE, StarRules.EVADE_TICKS"), "the evade is a 4-tick resistance effect that expires by itself (never an invulnerable flag that could be saved)");
        assertFalse(c.contains("setInvulnerable"), "no invulnerable flag");
    }

    @Test
    void everyHookIsASmallAppendAndTheOldCastsAreKept() {
        String gb = code(read("book/GrimoireBook.java"));
        assertTrue(code(read("book/pages15/Pages15.java")).contains("com.newuniverse.nusmp.star.StarPages.extend(bookPath, l == null ? List.of() : l)"), "the seven Star pages follow the nine of Batch1 inside Pages15, so the magic.pages15 self-test checks them (appended last, lang, tier, handler)");
        assertFalse(gb.contains("StarPages.append("), "GrimoireBook.familyAll is not touched");
        assertTrue(code(read("book/pages15/Pages15Rules.java")).contains("COOLDOWN_EXEMPT = java.util.Set.of(\"star_seed\")"), "Star Seed is the one page with a cooldown under its tier's minimum");
        assertTrue(read("../../../../../../tools/lang/merge_pages15.py").contains("star_magic.json"), "the lang merge includes the Star fragment");
        assertTrue(gb.contains("com.newuniverse.nusmp.star.StarPages.block(this, page(mode), p)") && gb.contains("CharacterSpells.block(this, page(mode), p)"), "the heir gate sits next to the old page block");
        String b1 = code(read("book/pages15/Batch1.java"));
        assertTrue(b1.contains("cone(Tier.STARTER, 8, 0.5, VfxShape.STAR_MAGIC, glow(60))") && b1.contains("dash(Tier.MID, false, 12, false, VfxShape.STAR_MAGIC, NONE)"), "the old cone and the old dash are still the casts they wrap");
        assertTrue(b1.contains("StarCasts.flagellum(cone(") && b1.contains("StarCasts.conjunction(dash("));
        for (String id : new String[]{"\"quartile_flagellum\"", "\"star_conjunction\"", "\"never_never_land\"", "\"meteor_swarm\""}) assertTrue(b1.contains(id), id + " is still a page of the book");
        String kind = code(read("prop/PropKind.java"));
        assertTrue(kind.contains("GNOME_1,\n    STAR_NODE,\n    STAR_PANE\n}") || kind.replaceAll("\\s+", " ").contains("GNOME_1, STAR_NODE, STAR_PANE }"), "the two prop kinds are appended at the END (ordinals never move)");
        String shape = code(read("vfx/VfxShape.java")).replaceAll("\\s+", " ");
        assertTrue(shape.contains("STORM_CANCEL, RUNE_WRITING, STAR_NODE, STAR_LINK, STAR_CONJ, STAR_PANE, STAR_LANCE, STAR_WHIP, STAR_DOME"), "the seven shapes are appended at the END, STAR_MAGIC stays where it was");
        String reg = code(read("prop/PropRegistry.java"));
        assertTrue(reg.contains("com.newuniverse.nusmp.star.StarProps.init();"));
        String pp = code(read("client/prop/PropPainterRegistry.java"));
        assertTrue(pp.contains("StarPropPainter.register();"));
        String mgr = code(read("vfx/client/VfxManager.java"));
        assertEquals(1, mgr.split("StarMagicLayer", -1).length - 1, "registered once");
        assertFalse(read("vfx/client/layer/ElementIdentityLayer.java").contains("StarV2"), "STAR_MAGIC and its layer are untouched");
        String wiz = code(read("wizard/WizardSystem.java"));
        assertTrue(wiz.contains("StarConfig.dualMode()") && wiz.contains("GrimoireAcceptance.grantExtra(p, cover, com.newuniverse.nusmp.blackclover.MagicType.STAR)") && wiz.contains("cover.isForbidden() ? new com.newuniverse.nusmp.star.StarRules.DualPlan"),
                "the tower asks the dual plan; a black cover never splits");
    }

    @Test
    void theToolsTheLangFragmentAndTheDocAreThere() {
        for (String f : new String[]{"tools/gen_star_v2_textures.py", "tools/lang/star_magic.json", "docs/vfx_remake/star_ingame.md", "docs/previews/vfx_remake/star/star_ingame.png", "tools/vfx_preview/star_ingame.py",
                "tools/vfx_preview/scenes.d/star_v2.json"})
            assertTrue(Files.exists(ROOT.resolve(f)), f);
    }
}
