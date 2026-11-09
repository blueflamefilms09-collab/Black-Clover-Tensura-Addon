package com.newuniverse.nusmp.saiyan;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Saiyan system must stay server friendly and cheap (CLAUDE.md rules 9 and 11), and nothing can be profiled here, so the properties that keep it so are pinned by reading the source: no client class and
 * no {@code Minecraft.getInstance()}, no static player, every per-player map keyed by UUID, no block placed and no entity spawned by an attack (so there is nothing to protect or clean up), targets only
 * through the foe rule, particles only through the budget, every packet validated and rate limited, the only config a SERVER config, and every text key the code uses exists in the language file. If one
 * of these is replaced by a better one on purpose, change the pin in the same commit and say why. Pure (file reads); the working directory is the project root.
 */
class SaiyanSourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path DIR = ROOT.resolve("src/main/java/com/newuniverse/nusmp/saiyan");

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
    void noClientClassAndNoMinecraftInstanceInTheCommonCode() throws IOException {
        Pattern client = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");
        for (String f : files()) assertFalse(client.matcher(code(f)).find(), f + " names a client-only class");
    }

    @Test
    void thePureClassesNameNoGameType() throws IOException {
        for (String f : List.of("SaiyanRules.java", "SaiyanState.java")) {
            assertFalse(Pattern.compile("(?m)^import (net\\.minecraft|net\\.neoforged|io\\.github|com\\.mojang)").matcher(code(f)).find(), f + " must stay pure (it is unit tested without a game)");
        }
        assertFalse(Pattern.compile("(?m)^import (net\\.neoforged|io\\.github|com\\.mojang)").matcher(code("SaiyanTag.java")).find(), "SaiyanTag may only use the NBT classes");
    }

    @Test
    void noStaticPlayerAndEveryPerPlayerMapIsKeyedByUuid() throws IOException {
        for (String f : files()) {
            String c = code(f);
            assertFalse(Pattern.compile("static\\s+(?:final\\s+|volatile\\s+)*(?:ServerPlayer|Player|FakePlayer)\\s+\\w+").matcher(c).find(), f + " has a static player");
            assertFalse(Pattern.compile("static\\s+[^;=(]*Map<(?!UUID)(?![^;=(]*ModConfigSpec)[^;=(]*>\\s*\\w+\\s*[=;]").matcher(c).find(), f + " has a static map that is not keyed by UUID (a table of config values is fine)");
        }
    }

    @Test
    void anAttackNeverPlacesABlockSpawnsAnEntityOrLoadsAChunk() throws IOException {
        Pattern bad = Pattern.compile("addFreshEntity|\\.setBlock\\(|setBlockAndUpdate|destroyBlock|getChunk\\(|forceChunk|setChunkForced|loadChunk|\\.explode\\(|Explosion|LightningBolt|createEntity|\\.spawn\\(|EntityType\\.[A-Z_]+\\.create");
        for (String f : files()) if (!f.equals("SaiyanSelfTests.java") && !f.equals("SaiyanTechSelfTests.java")) assertFalse(bad.matcher(code(f)).find(), f + " places a block, spawns an entity, loads a chunk or explodes: a Ki attack is a raycast and a few particles");
    }

    @Test
    void targetsGoThroughTheFoeRuleDamageThroughTheBalanceLawAndParticlesThroughTheBudget() throws IOException {
        String a = code("SaiyanAbilities.java");
        assertTrue(a.contains("SongTargets.isFoe") && a.contains("FtbTeamHelper.sameTeam") && a.contains("mayInteract"), "foes only, teams and protected places respected");
        assertTrue(a.contains("BalanceLaw.damage("), "damage goes through the balance law");
        assertTrue(a.contains("Fx.spray(") && !a.contains("sendParticles(") && !a.contains("ParticleTypes."), "particles go through the budget");
        assertTrue(a.contains("MAX_TRAIL") && a.contains("MAX_CANDIDATES"), "the trail and the candidate list are bounded");
        assertTrue(code("SaiyanConfig.java").contains("defineInRange(\"barrageRays\", 5, 1, 8)"), "a barrage is at most eight rays");
    }

    @Test
    void everyPacketIsValidatedAndRateLimitedAndTheSyncIsNotPerTick() throws IOException {
        String n = code("SaiyanNet.java");
        assertTrue(n.contains("static boolean valid(int op)") && n.contains("if (!valid(op)) return;"), "an ask is range-checked before the server looks at it");
        assertTrue(n.contains("ctx.player() instanceof ServerPlayer"), "the sender must be a real server player");
        String m = code("SaiyanManager.java");
        assertTrue(m.contains("SaiyanNet.MIN_GAP_TICKS"), "asks are rate limited");
        assertTrue(m.contains("now - r.lastSent >= 5"), "at most four sync packets a second");
        assertTrue(m.contains("(p.tickCount % 20) == 0"), "the upkeep runs once a second");
        assertTrue(m.contains("!s.saiyan") && m.contains("SaiyanConfig.enabled()"), "a player who is not a Saiyan, or a disabled system, is answered with nothing");
    }

    @Test
    void theOnlyConfigIsAServerConfigWithItsOwnFileAndTheTickReturnsAtOnce() throws IOException {
        String b = code("SaiyanBoot.java");
        assertTrue(b.contains("ModConfig.Type.SERVER, SaiyanConfig.SPEC, \"nusmp-saiyan-server.toml\""));
        assertFalse(b.contains("ModConfig.Type.CLIENT"), "gameplay settings are server settings");
        assertTrue(code("SaiyanStore.java").contains("p.hasData(STATE)"), "reading never creates the attachment");
        assertTrue(code("SaiyanManager.java").contains("SaiyanState s = SaiyanStore.peek(p);\n        if (s == null || !s.saiyan) return;"), "a player who is not a Saiyan costs one lookup per tick");
    }

    @Test
    void theModifiersAreTransientWithFixedIdsAndAreAllRemovable() throws IOException {
        String a = code("SaiyanAttributes.java");
        assertTrue(a.contains("addTransientModifier") && !a.contains("addPermanentModifier"), "a crash can not leave a permanent modifier");
        assertTrue(a.contains("public static void clear(") && a.contains("public static boolean any("), "one place removes them all");
    }

    @Test
    void everyTextKeyTheSaiyanCodeUsesIsInTheLanguageFile() throws IOException {
        String lang = Files.readString(ROOT.resolve("src/main/resources/assets/nusmp/lang/en_us.json"), StandardCharsets.UTF_8);
        List<String> keys = new ArrayList<>();
        for (SaiyanRules.Rank r : SaiyanRules.Rank.values()) keys.add(r.key());
        for (String m : List.of("not_awakened", "no_ki", "no_stamina", "awakened", "rank_up")) keys.add("nusmp.saiyan.msg." + m);
        Matcher h = Pattern.compile("\\bt\\(\"([a-z_]+)\"").matcher(Files.readString(ROOT.resolve("src/main/java/com/newuniverse/nusmp/client/saiyan/SaiyanClient.java")));
        while (h.find()) keys.add("nusmp.saiyan.hud." + h.group(1));
        keys.add("nusmp.saiyan.hud.blast");
        keys.add("nusmp.saiyan.hud.barrage");
        keys.add("key.nusmp.saiyan_blast");
        keys.add("key.nusmp.saiyan_barrage");
        keys.add("gui_move.element.saiyan_hud");
        keys.add("nusmp.gui_scale.gui.saiyan_hud");
        for (String k : keys) assertTrue(lang.contains("\"" + k + "\""), "the language file lacks " + k);
        Matcher say = Pattern.compile("say\\(p, \"([a-z_]+)\"").matcher(code("SaiyanAbilities.java"));
        while (say.find()) assertTrue(lang.contains("\"nusmp.saiyan.msg." + say.group(1) + "\""), "the language file lacks the message " + say.group(1));
    }

    // ------------------------------------------------------------------ 3.65.0: Stage 2, the Goku path and the forms
    @Test
    void theStage2PureClassesNameNoGameType() throws IOException {
        for (String f : List.of("SaiyanForms.java", "SaiyanUltimates.java", "GokuRules.java", "SaiyanFlightRules.java", "SaiyanSenseRules.java", "SaiyanCombatRules.java", "SaiyanTraining.java")) {
            assertFalse(Pattern.compile("(?m)^import (net\\.minecraft|net\\.neoforged|io\\.github|com\\.mojang)").matcher(code(f)).find(), f + " must stay pure (it is unit tested without a game)");
        }
    }

    @Test
    void particlesOnlyGoThroughTheFxFacade() throws IOException {
        for (String f : files()) {
            if (f.equals("SaiyanSelfTests.java") || f.equals("SaiyanStage2SelfTests.java")) continue;
            String c = code(f);
            assertFalse(c.contains("sendParticles(") || c.contains("ParticleTypes."), f + " draws particles outside the budgeted Fx facade");
        }
    }

    @Test
    void flightTakesBackOnlyWhatItGaveAndTheSpeedIsCheckedOnTheServer() throws IOException {
        String c = code("SaiyanFlight.java");
        assertTrue(c.contains("MARK = \"nusmp_saiyan_flight\"") && c.contains("d.getBoolean(MARK)") && c.contains("d.remove(MARK)"), "the flight we give is marked and only a marked flight is taken back");
        assertTrue(c.contains("p.isCreative() || p.isSpectator()"), "creative and spectator flight is never taken away");
        assertTrue(c.contains("SaiyanFlightRules.speedOk("), "the server checks the speed of the flight");
        assertTrue(c.contains("onUpdateAbilities"), "the client is told when the ability changes");
        assertTrue(code("SaiyanManager.java").contains("SaiyanFlight.release"), "a logout, a death and a removed race give the ability back");
    }

    @Test
    void theRadarHasAServerWideBudgetAndABoundedList() throws IOException {
        String c = code("SaiyanSenses.java");
        assertTrue(c.contains("SCANS_PER_TICK") && c.contains("MAX_CANDIDATES") && c.contains("getEntitiesOfClass("), "one bounded query, at most a few per tick on the whole server");
        assertTrue(c.contains("now - r.lastPing < 40"), "a radar acts once every two seconds");
        assertTrue(c.contains("SaiyanSenseRules.MAX_ENTRIES"), "the list sent to the client is bounded");
    }

    @Test
    void theDodgeCanNeverPassTheHardCapAndCostsAStaminaAndACooldown() throws IOException {
        String c = code("SaiyanCombat.java");
        assertTrue(c.contains("SaiyanCombatRules.capped(") && c.contains("SaiyanCombatRules.budgetOk(") && c.contains("reflexReadyAt"), "a dodge has a cap, a budget per window and a cool-down");
        String r = code("SaiyanCombatRules.java");
        assertTrue(r.contains("HARD_DODGE_CAP = 0.75"), "no source can dodge more than three blows in four");
    }

    @Test
    void theKamehamehaAndTheComboAreRaycastsAndOneQueryEach() throws IOException {
        String c = code("GokuAbilities.java");
        assertTrue(c.contains("level.clip(") && c.contains("getEntitiesOfClass("), "a beam is one raycast and one entity query");
        assertTrue(c.contains("MAX_CANDIDATES") && c.contains("MAX_BEAM_PARTICLES"), "the candidates and the beam particles are bounded");
        assertTrue(c.contains("SaiyanAbilities.allowed("), "the beam and the combo only hit foes the rules allow");
        assertTrue(c.contains("SaiyanAbilities.hurt(") && c.contains("SaiyanAbilities.hurtBlow("), "damage goes through the balance law");
    }

    @Test
    void theFormTableIsTheOnlyPlaceAFormIsDefinedAndTheEngineKnowsNoForm() throws IOException {
        String e = code("SaiyanFormEngine.java");
        for (SaiyanForms.Form f : SaiyanForms.ALL) assertFalse(e.contains("\"" + f.id() + "\""), "the engine must not name the form " + f.id() + ": a new form is a new row of the table");
        assertTrue(code("SaiyanConfig.java").contains("for (SaiyanForms.Form f : SaiyanForms.ALL)"), "every form gets its server settings from the table");
    }

    @Test
    void everyStage2TextKeyIsInTheLanguageFile() throws IOException {
        String lang = Files.readString(ROOT.resolve("src/main/resources/assets/nusmp/lang/en_us.json"), StandardCharsets.UTF_8);
        List<String> keys = new ArrayList<>();
        for (SaiyanForms.Form f : SaiyanForms.ALL) keys.add(f.key());
        for (SaiyanFlightRules.Tier t : SaiyanFlightRules.Tier.values()) keys.add(t.key());
        for (SaiyanSenseRules.Tier t : SaiyanSenseRules.Tier.values()) keys.add(t.key());
        for (SaiyanUltimates.Ultimate u : SaiyanUltimates.ALL) {
            keys.add(u.nameKey());
            keys.add(u.epithetKey());
        }
        Pattern lit = Pattern.compile("(?:say\\(p, |msg\\(p, |deactivate\\(p, s, r, now, )\"([a-z_0-9]+)\"");
        Pattern direct = Pattern.compile("translatable\\(\"(nusmp\\.[a-z_0-9.]*[a-z_0-9])\"");
        for (String f : files()) {
            if (f.equals("SaiyanSelfTests.java") || f.equals("SaiyanStage2SelfTests.java")) continue;
            String c = code(f);
            Matcher m = lit.matcher(c);
            while (m.find()) keys.add("nusmp.saiyan.msg." + m.group(1));
            Matcher d = direct.matcher(c);
            while (d.find()) keys.add(d.group(1));
        }
        Matcher h = Pattern.compile("\\bt\\(\"([a-z_0-9]+)\"").matcher(Files.readString(ROOT.resolve("src/main/java/com/newuniverse/nusmp/client/saiyan/SaiyanClient.java")));
        while (h.find()) keys.add("nusmp.saiyan.hud." + h.group(1));
        Matcher k = Pattern.compile("new KeyMapping\\(\"(key\\.nusmp\\.saiyan_[a-z_]+)\"").matcher(Files.readString(ROOT.resolve("src/main/java/com/newuniverse/nusmp/client/saiyan/SaiyanClient.java")));
        while (k.find()) keys.add(k.group(1));
        assertTrue(keys.size() > 40, "the scan found the keys it should");
        for (String key : keys) assertTrue(lang.contains("\"" + key + "\""), "the language file lacks " + key);
    }
}
