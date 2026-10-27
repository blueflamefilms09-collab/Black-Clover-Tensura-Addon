package com.newuniverse.nusmp.entity.veldanava;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (CLAUDE.md rules 9 and 11, docs/veldanava.md): nothing of the fight can be profiled or played here, so its cost bounds and its server-safety are pinned by reading the real files. The server
 * code names no client class, every effect goes through the boss's per-tick budget, no move scans entities or chunks or changes blocks on its own, the one block change is guarded by the griefBlocks
 * rule and recorded in the ledger, the packet that picks a boon is validated, the renderers build no location per frame and cull by distance, and every asset, lang key and smoke prefix of the boss exists.
 */
class VeldanavaSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path RES = Path.of("src/main/resources");
    private static final String[] BODIES = {"humanoid", "dark_avatar", "true_dragon"};
    private static final String[] ITEMS = {"star_king_dragon_core", "genesis_fragment", "creators_essence", "justice_king_michael_fragment", "covenant_king_uriel_fragment", "nihility_fragment", "veldanavas_divine_core"};

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    private static String server(String name) throws IOException { return Files.readString(SRC.resolve("entity/veldanava/" + name + ".java"), StandardCharsets.UTF_8); }

    private static String client(String name) throws IOException { return Files.readString(SRC.resolve("client/veldanava/" + name + ".java"), StandardCharsets.UTF_8); }

    private static List<Path> serverFiles() throws IOException {
        try (Stream<Path> s = Files.list(SRC.resolve("entity/veldanava"))) { return s.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
    }

    @Test
    void theServerCodeNamesNoClientClass() throws IOException {
        Pattern client = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");
        List<String> bad = new ArrayList<>();
        for (Path p : serverFiles()) if (client.matcher(code(Files.readString(p, StandardCharsets.UTF_8))).find()) bad.add(p.getFileName().toString());
        assertTrue(bad.isEmpty(), "Veldanava's server code names a client class: " + bad);
    }

    @Test
    void everyEffectOfTheMovesAndTheBossGoesThroughTheTickBudget() throws IOException {
        Pattern fx = Pattern.compile("VfxSpawn\\.send\\(|ZogratisVfx\\.\\w+\\(");
        List<String> bad = new ArrayList<>();
        for (String f : new String[]{"VeldanavaMoves", "VeldanavaBoss"}) {
            String[] lines = code(server(f)).split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (!fx.matcher(lines[i]).find()) continue;
                boolean guarded = false;                                          // the budget test is on the line itself or at the top of the helper (up to three lines above)
                for (int k = Math.max(0, i - 3); k <= i; k++) guarded |= lines[k].contains("fxBudget()");
                if (!guarded) bad.add(f + ":" + (i + 1) + " " + lines[i].strip());
            }
        }
        assertTrue(bad.isEmpty(), "an effect that skips the per-tick budget (b.fxBudget()): " + bad);
        String boss = code(server("VeldanavaBoss"));
        assertTrue(boss.contains("fxBudget = VeldanavaRules.FX_PER_TICK") && boss.contains("blockBudget = VeldanavaRules.BLOCKS_PER_TICK"), "the budgets are refilled once per tick from the rules");
    }

    @Test
    void theMovesScanNoEntitiesNoChunksAndChangeNoBlocksThemselves() throws IOException {
        String moves = code(server("VeldanavaMoves"));
        for (String banned : new String[]{"getEntitiesOfClass", "getEntities(", "getNearbyEntities", "getChunk", "setBlock(", "destroyBlock(", "addFreshEntity", "new ItemEntity", "Minecraft"})
            assertFalse(moves.contains(banned), "a move uses " + banned + " (go through the boss: playersNear, tryDestroy)");
        String boss = code(server("VeldanavaBoss"));
        assertFalse(boss.contains("getEntitiesOfClass") || boss.contains("getNearbyEntities") || boss.contains("getChunk("), "the boss scans entities or chunks");
        assertEquals(1, Pattern.compile("setBlock\\(").matcher(boss).results().count(), "one place changes a block: tryDestroy");
        String destroy = boss.substring(boss.indexOf("public boolean tryDestroy"));
        destroy = destroy.substring(0, destroy.indexOf("\n    }") + 1);
        assertTrue(destroy.contains("NUGameRules.griefBlocks(") && destroy.contains("ledger") && destroy.contains("blockBudget") && destroy.contains("MAX_DESTROY_SPEED"), "tryDestroy lost the griefBlocks rule, the ledger, the budget or the hardness limit");
    }

    @Test
    void theTargetsOfAMoveAreCappedAndAlwaysPlayers() throws IOException {
        String boss = code(server("VeldanavaBoss"));
        String near = boss.substring(boss.indexOf("public List<ServerPlayer> playersNear"));
        near = near.substring(0, near.indexOf("\n    }") + 1);
        assertTrue(near.contains("VeldanavaRules.MAX_TARGETS") && near.contains("sl().players()"), "playersNear is capped and walks the player list only");
        assertTrue(code(server("VeldanavaMoves")).contains("VeldanavaRules.marks(ps.size())"), "Starfall's marks come from the capped rule");
        assertTrue(boss.contains("lastHit.size() >= 128"), "the per-target hit table is bounded");
    }

    @Test
    void theBossIsBudgetedAndKeepsItsSafetyHooks() throws IOException {
        String boss = code(server("VeldanavaBoss"));
        for (String must : new String[]{"dpsBudget.hurt(", "dpsBudget.tick(", "ServerBossEvent", "VeldanavaConfig.damageScale", "removeAllPlayers", "ledger.restoreAll", "VeldanavaSoul."})
            assertTrue(boss.contains(must) || code(server("VeldanavaSoul")).contains(must), "the boss lost: " + must);
        assertTrue(boss.contains("transformLeft > 0") && boss.contains("guarding()") && boss.contains("return false;"), "the transformation and the guard make him unhurtable");
    }

    @Test
    void thePacketThatPicksABoonIsValidatedOnTheServer() throws IOException {
        String cmd = code(server("VeldanavaCommand"));
        assertTrue(cmd.contains("IntegerArgumentType.integer(0, VeldanavaBoons.COUNT)"), "the choice is bounded by the command");
        assertTrue(cmd.contains("getEntity() instanceof ServerPlayer"), "the sender must be a player");
        assertTrue(cmd.contains("hasPermission(2)"), "spawning the boss is an operator command");
        String orb = code(server("WillOfTheCreator"));
        assertTrue(orb.contains("VeldanavaBoons.valid(") && orb.contains("VeldanavaBoons.received(") && orb.contains("claimed.contains("), "the orb checks the choice, the one-time flag and the share");
        String boons = code(server("VeldanavaBoons"));
        assertTrue(boons.contains("if (!valid(choice) || received(p)) return false;"), "a boon is granted once");
    }

    @Test
    void theBossFollowsTheHungerBanAndTheParticleFacade() throws IOException {
        for (Path p : serverFiles()) {
            String c = code(Files.readString(p, StandardCharsets.UTF_8));
            String n = p.getFileName().toString();
            assertFalse(c.contains("MobEffects.HUNGER"), n + " applies the Hunger effect (the owner's hunger ban)");
            assertFalse(c.contains("setFoodLevel(") || c.contains("setSaturation(") || c.contains("setExhaustion(") || c.contains("addExhaustion("), n + " writes the food bar (use FoodData.eat to restore it)");
            assertFalse(c.contains("ParticleTypes."), n + " names a vanilla particle (go through particle.fx.Fx)");
        }
    }

    @Test
    void theConfigIsAServerConfigWithItsOwnFileName() throws IOException {
        String main = code(Files.readString(SRC.resolve("NUSMP.java"), StandardCharsets.UTF_8));
        assertTrue(main.contains("ModConfig.Type.SERVER, com.newuniverse.nusmp.entity.veldanava.VeldanavaConfig.SPEC, \"nusmp-veldanava-server.toml\""), "Veldanava's settings change gameplay, so they are a SERVER config");
        String cfg = code(server("VeldanavaConfig"));
        for (String key : new String[]{"enabled", "bossHealth", "damageScale", "soulSeizing", "willOfTheCreatorSeconds", "classicMode", "healthTotal", "regenPercentPerSecond", "tensuraMoves"}) assertTrue(cfg.contains("\"" + key + "\""), "config key missing: " + key);
    }

    @Test
    void theRenderersBuildNoLocationPerFrameAndCullByDistance() throws IOException {
        for (String f : new String[]{"VeldanavaRenderer", "VeldanavaOrbRenderer"}) {
            String src = code(client(f));
            long built = Pattern.compile("ResourceLocation\\.fromNamespaceAndPath\\(").matcher(src).results().count();
            long inStatics = src.lines().filter(l -> l.contains("ResourceLocation.fromNamespaceAndPath(") && l.contains("static final")).count();
            assertEquals(built, inStatics, f + " builds a ResourceLocation outside a static final");
            assertFalse(src.contains("new Vector3f(") || src.contains("new Quaternionf("), f + " allocates vectors per frame");
        }
        String r = code(client("VeldanavaRenderer"));
        assertTrue(r.contains("MAX_DISTANCE * MAX_DISTANCE") && r.contains("shouldRender"), "far dragons are skipped");
        assertFalse(r.contains("TimeMagicLayer"), "the Time Magic layer stays out of this");
    }

    @Test
    void everyAssetLangKeyAndSmokePrefixExists() throws IOException {
        for (String b : BODIES) {
            for (String f : new String[]{"geo/entity/veldanava_" + b + ".geo.json", "animations/entity/veldanava_" + b + ".animation.json", "textures/entity/veldanava_" + b + ".png", "textures/entity/veldanava_" + b + "_glow.png"})
                assertTrue(Files.exists(RES.resolve("assets/nusmp/" + f)), "missing " + f);
        }
        JsonObject lang = JsonParser.parseString(Files.readString(RES.resolve("assets/nusmp/lang/en_us.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        for (String k : new String[]{"entity.nusmp.veldanava", "entity.nusmp.will_of_the_creator", "item.nusmp.veldanava_spawn_egg", "nusmp.veldanava.name.dragon", "nusmp.veldanava.name.humanoid", "nusmp.veldanava.name.avatar"})
            assertTrue(lang.has(k), "missing lang key " + k);
        for (String id : ITEMS) {
            assertTrue(Files.exists(RES.resolve("assets/nusmp/textures/item/" + id + ".png")), "missing icon " + id);
            assertTrue(Files.exists(RES.resolve("assets/nusmp/models/item/" + id + ".json")), "missing item model " + id);
            for (String suffix : new String[]{"", ".desc", ".card"}) assertTrue(lang.has("item.nusmp." + id + suffix), "missing lang key item.nusmp." + id + suffix);
        }
        assertTrue(Files.exists(RES.resolve("assets/nusmp/models/item/veldanava_spawn_egg.json")));
        String smoke = Files.readString(Path.of(".github/workflows/smoke.yml"), StandardCharsets.UTF_8);
        assertTrue(Pattern.compile("SMOKE_EXPECT:[^\\n]*[\", ]veldanava[\",]").matcher(smoke).find(), "the smoke list must expect the veldanava self-tests");
        String tests = code(server("VeldanavaSelfTests"));
        for (String key : new String[]{"veldanava.rules", "veldanava.assets", "veldanava.boss", "veldanava.orb"}) assertTrue(tests.contains("\"" + key + "\""), "self-test missing: " + key);
    }
}
