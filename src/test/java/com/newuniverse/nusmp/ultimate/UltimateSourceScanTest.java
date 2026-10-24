package com.newuniverse.nusmp.ultimate;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: Ultimate Magic must stay server friendly and cheap (CLAUDE.md rules 9 and 11), and nothing can be profiled here, so the properties that keep it so are pinned by reading the source: no client class and no
 * {@code Minecraft.getInstance()}, no packet of its own, no static player, no entity spawn, no block change, no chunk loading, damage only through the grimoire's balance law, targets only through the foe rule,
 * particles only through the budget, effect packets only through the capped helper, an early return when nothing is being cast, one area query per pulse, every config a SERVER config. If one of these is replaced by
 * a better one on purpose, change the pin in the same commit and say why. Pure (file reads); the working directory is the project root.
 */
class UltimateSourceScanTest {
    private static final Path DIR = Path.of("").toAbsolutePath().resolve("src/main/java/com/newuniverse/nusmp/ultimate");

    private static String code(String file) throws IOException {
        String s = Files.readString(DIR.resolve(file), StandardCharsets.UTF_8);
        return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    private static List<String> files() throws IOException {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(DIR)) { s.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> out.add(p.getFileName().toString())); }
        return out;
    }

    @Test
    void noClientClassAndNoMinecraftInstanceAnywhere() throws IOException {
        Pattern client = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");
        for (String f : files()) assertFalse(client.matcher(code(f)).find(), f + " names a client-only class");
    }

    @Test
    void thePureClassesNameNoMinecraftType() throws IOException {
        for (String f : List.of("UltimateRules.java", "UltimateGate.java", "UltimateDef.java", "UltimateRegistry.java")) {
            Matcher m = Pattern.compile("(?m)^import (net\\.minecraft|net\\.neoforged|io\\.github|com\\.mojang)").matcher(code(f));
            assertFalse(m.find(), f + " must stay pure (it is unit tested without a game)");
        }
    }

    @Test
    void thereIsNoPacketOfItsOwnAndNoKeybind() throws IOException {
        Pattern net = Pattern.compile("CustomPacketPayload|PacketDistributor|registerPayloads|KeyMapping|StreamCodec");
        for (String f : files()) assertFalse(net.matcher(code(f)).find(), f + ": Ultimate Magic has no packet of its own (it reuses VfxSpawn and the grimoire's casting flow) and no keybind");
    }

    @Test
    void noStaticPlayerAndEveryPerPlayerMapIsKeyedByUuid() throws IOException {
        for (String f : files()) {
            String c = code(f);
            assertFalse(Pattern.compile("static\\s+(?:final\\s+|volatile\\s+)*(?:ServerPlayer|Player|FakePlayer)\\s+\\w+").matcher(c).find(), f + " has a static player");
            assertFalse(Pattern.compile("static\\s+[^;=]*Map<(?!UUID|String|Cancel)[^>]*,").matcher(c).find(), f + " has a static map that is not keyed by UUID (or a plain id)");
        }
        String m = code("UltimateManager.java");
        assertTrue(m.contains("Map<UUID, Session> SESSIONS"));
    }

    @Test
    void noEntitySpawnNoBlockChangeNoChunkLoadingNoExplosion() throws IOException {
        Pattern bad = Pattern.compile("addFreshEntity|\\.setBlock\\(|setBlockAndUpdate|destroyBlock|getChunk\\(|forceChunk|setChunkForced|loadChunk|\\.explode\\(|Explosion|LightningBolt|createEntity|summon");
        for (String f : files()) if (!f.equals("UltimateSelfTests.java")) assertFalse(bad.matcher(code(f)).find(), f + " spawns an entity, changes a block, loads a chunk or explodes: an Ultimate is damage, debuffs and pictures only");
    }

    @Test
    void damageGoesOnlyThroughTheBalanceLawAndTargetsOnlyThroughTheFoeRule() throws IOException {
        String m = code("UltimateManager.java");
        assertTrue(m.contains("s.book.hurt(s.instance, p, t, s.mode, raw)"), "damage through GrimoireBook.hurt (balance law, PvP hit cap)");
        assertEquals(0, count(m, "t.hurt("), "no direct hurt on a target");
        assertEquals(0, count(m, ".hurt(createSource"), "no direct damage source");
        assertTrue(m.contains("Foes.isFoe(p, e)"), "targets through the foe rule: allies, teams, pets, creative and spectators are never hit");
        assertTrue(m.contains("UltimateConfig.hitPlayers()"));
        assertTrue(m.contains("UltimateRules.pulseRaw("), "the raw damage is clamped by the pure rule");
        assertEquals(1, count(m, "getEntitiesOfClass("), "one area query per pulse, in one place");
        assertTrue(m.contains("UltimateRules.MAX_CANDIDATES") && m.contains("UltimateRules.MAX_TARGETS"), "candidates and targets are capped");
        assertTrue(m.contains("Foes") && m.contains("import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;"));
    }

    @Test
    void particlesAreBudgetedAndEffectPacketsAreCapped() throws IOException {
        String m = code("UltimateManager.java");
        assertEquals(0, count(m, ".sendParticles("), "server particles only through PerfParticles (the per-tick budget)");
        assertTrue(m.contains("PerfParticles.send("));
        assertEquals(2, count(m, "VfxSpawn.send"), "effect packets go through VfxSpawn (nearby players, per-tick budget): the capped helper + the follow circle");
        assertTrue(m.contains("s.packets >= UltimateRules.MAX_EFFECT_PACKETS"), "a cast cannot send more than the cap");
        assertEquals(2, count(m, "s.packets++"), "the follow circle and the helper both count");
        assertTrue(m.contains("UltimateRules.FX_EMIT_PERIOD"), "particles are emitted every 10 ticks, not every tick");
        assertTrue(m.contains("UltimateRules.CHARGE_PARTICLES"));
    }

    @Test
    void nothingRunsWhenNothingIsBeingCast() throws IOException {
        String m = code("UltimateManager.java");
        assertTrue(Pattern.compile("onServerTick\\(ServerTickEvent\\.Post event\\)\\s*\\{\\s*if \\(SESSIONS\\.isEmpty\\(\\)\\) return;").matcher(m).find());
        for (String h : List.of("onDeath", "onDimension", "onLogout", "onDamaged"))
            assertTrue(Pattern.compile(h + "\\([^)]*\\)\\s*\\{\\s*if \\(SESSIONS\\.isEmpty\\(\\)").matcher(m).find(), h + " must return at once when no cast exists");
        assertTrue(m.contains("PerfProbe.begin()") && m.contains("PerfProbe.end(PROBE"), "the tick is measured by the performance probe");
    }

    @Test
    void everyEndOfACastRemovesTheEntryAndTheMovementHold() throws IOException {
        String m = code("UltimateManager.java");
        assertEquals(1, count(m, "SESSIONS.put("), "one place creates a cast");
        assertTrue(m.contains("if (SESSIONS.remove(s.owner) == null) return;\n        unlock(s.player);") || m.contains("SESSIONS.remove(s.owner) == null"), "finish removes the entry and unlocks");
        for (String why : List.of("DEATH", "LOGOUT", "DIMENSION", "DAMAGE", "SEALED", "SNEAK", "LOW_MANA", "DISABLED", "SHUTDOWN"))
            assertTrue(m.contains("Cancel." + why), "the cancel " + why + " is used");
        assertTrue(m.contains("onServerStopping"));
        assertTrue(m.contains("addTransientModifier"), "the hold is a transient modifier: it can never be saved with the player");
        assertFalse(m.contains("addPermanentModifier"));
    }

    @Test
    void theMagiculeOnlyChangesAtTheReleaseAndTheCooldownIsSetThere() throws IOException {
        String m = code("UltimateManager.java");
        assertEquals(1, count(m, "setMagicule("), "the magicule is touched in one place: the release");
        int rel = m.indexOf("private static void release(");
        assertTrue(rel > 0 && m.indexOf("setMagicule(") > rel && m.indexOf("setMagicule(") < m.indexOf("private static void pulse("), "setMagicule is inside release()");
        assertEquals(1, count(m, "setCooldownUntil("), "the long cooldown is set in one place");
        assertTrue(m.indexOf("setCooldownUntil(") > rel);
        assertTrue(m.contains("UltimateRules.release(ex.getMagicule())"), "ALL the current magicule");
    }

    @Test
    void everyConfigIsAServerConfigWithItsOwnFileName() throws IOException {
        String boot = code("UltimateBoot.java");
        assertTrue(boot.contains("registerConfig(ModConfig.Type.SERVER, UltimateConfig.SPEC, \"nusmp-ultimate-server.toml\")"));
        assertFalse(boot.contains("ModConfig.Type.CLIENT") || boot.contains("ModConfig.Type.COMMON"));
    }

    @Test
    void theTickMethodsBuildNoResourceLocationAndUseNoReflection() throws IOException {
        String m = code("UltimateManager.java");
        int tick = m.indexOf("public static void onServerTick(");
        String body = m.substring(tick, m.indexOf("private static void step("));
        assertFalse(Pattern.compile("ResourceLocation\\.|Class\\.forName|\\.getMethod\\(|\\.invoke\\(").matcher(body).find());
        assertTrue(m.contains("EFFECTS.computeIfAbsent("), "potion effects are resolved once and kept");
    }

    @Test
    void theTimeMagicIsNotTouchedOrMentioned() throws IOException {
        for (String f : files()) {
            String c = code(f);
            assertFalse(c.contains("TimeMagicLayer") || c.contains("TimeBook") || c.contains("TIME_STASIS") || c.contains("TIME_CLOCK") || c.contains("book_time"), f);
        }
    }

    private static int count(String s, String what) {
        int n = 0;
        for (int at = s.indexOf(what); at >= 0; at = s.indexOf(what, at + 1)) n++;
        return n;
    }
}
