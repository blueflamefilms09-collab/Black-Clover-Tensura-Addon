package com.newuniverse.nusmp.devil.mastery;

import com.newuniverse.nusmp.devil.dual.DualRules;
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
 * Why: Devil Awakening (docs/devil_mastery.md) must stay server friendly, cheap and switchable (CLAUDE.md rules 9, 11, 12), and nothing can be profiled here, so the properties that keep it so are pinned by reading
 * the source: no client class, no static player or map, no world scan or spawn, no tick event of its own, the old assimilation behind devil.legacyAssimilation (default false), the skill in the extra slot and given and
 * taken with the contract, every art bounded, the packet fields appended. Pure (file reads); the working directory is the project root.
 */
class MasterySourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().resolve("src/main/java/com/newuniverse/nusmp");

    private static String code(String rel) throws IOException {
        String s = Files.readString(ROOT.resolve(rel), StandardCharsets.UTF_8);
        return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    private static final String[] FILES = {"devil/mastery/Mastery.java", "devil/mastery/MasteryRules.java", "devil/mastery/DevilKit.java", "skill/DevilAwakeningSkill.java", "skill/DevilKitArts.java"};

    @Test
    void noClientClassNoStaticPlayerNoStaticMapNoWorldScanNoSpawnNoTickEvent() throws IOException {
        Pattern client = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");
        for (String f : FILES) {
            String c = code(f);
            assertFalse(client.matcher(c).find(), f + " names a client-only class");
            assertFalse(Pattern.compile("static\\s+(?:final\\s+|volatile\\s+)*(?:ServerPlayer|Player|FakePlayer)\\s+\\w+").matcher(c).find(), f + " has a static player");
            assertFalse(Pattern.compile("static\\s+[^;=(]*(?:HashMap|ArrayList|HashSet|ConcurrentHashMap|LinkedHashMap)<").matcher(c).find(), f + " keeps a mutable static collection");
            assertFalse(Pattern.compile("getEntitiesOfClass|getAllEntities|getChunk\\(|forceChunk|setChunkForced|addFreshEntity|\\.explode\\(|Math\\.random").matcher(c).find(), f + " scans the world, spawns or uses unseeded randomness");
            assertFalse(c.contains("TickEvent") || c.contains("PlayerTickEvent") || c.contains("LevelTickEvent") || c.contains("ServerTickEvent"), f + " has tick work of its own: the once-a-second step of DevilContracts calls it");
            assertFalse(c.contains("ParticleTypes.") || c.contains("sendParticles("), f + " sends a vanilla particle: effects go through SkillFx / Fx (the Rukh particles and the budgets)");
        }
    }

    @Test
    void thePureClassesNameNoMinecraftType() throws IOException {
        for (String f : new String[] {"devil/mastery/MasteryRules.java", "devil/mastery/DevilKit.java", "devil/mastery/DevilScrollRules.java"})
            assertFalse(Pattern.compile("(?m)^import (net\\.minecraft|net\\.neoforged|com\\.mojang)").matcher(code(f)).find(), f + " must stay pure (it is unit tested without a game)");
    }

    @Test
    void theOldAssimilationStaysBehindALegacySettingThatDefaultsToOff() throws IOException {
        String cfg = code("devil/DevilConfig.java");
        assertTrue(Pattern.compile("LEGACY\\s*=[^;]*legacyAssimilation[^;]*false").matcher(cfg).find(), "devil.legacyAssimilation must default to false");
        assertTrue(cfg.contains("boolean legacyAssimilation()"), "the accessor");
        String m = code("devil/mastery/Mastery.java");
        assertTrue(m.contains("DevilConfig.legacyAssimilation()"), "the mastery rules read the legacy setting");
        assertTrue(code("devil/mastery/MasteryRules.java").contains("legacy ? 100f"), "the legacy ceiling is the old full reach");
        String c = code("devil/DevilContracts.java");
        assertTrue(c.contains("Mastery.ceiling(c)") && c.contains("Mastery.refuseBegin") && c.contains("Mastery.expired"), "the contract step uses the ceiling, the rest and the timer");
    }

    @Test
    void theSkillIsAnExtraSkillGivenWithTheContractAndTakenWithIt() throws IOException {
        assertTrue(code("skill/DevilAwakeningSkill.java").contains("super(SkillType.EXTRA, TEXT, false)"), "an Extra skill");
        assertTrue(code("skill/NUSkills.java").contains("register(\"devil_awakening\", DevilAwakeningSkill::new)"), "registered");
        String g = code("devil/dual/DevilSkillGrant.java");
        assertTrue(g.contains("NUSkills.DEVIL_AWAKENING") && g.contains("contract && DevilConfig.masteryOn()"), "granted while a contract is held and the system is on");
        assertTrue(g.contains("awaken_revoked"), "taken when it ends");
        assertTrue(g.contains("public static void mirror("), "the skill's mastery bar follows the contract's");
        // the verdict the grant uses: grant when wanted and missing, revoke when unwanted and present
        assertEquals(DualRules.Verdict.GRANT, DualRules.verdict(true, false));
        assertEquals(DualRules.Verdict.REVOKE, DualRules.verdict(false, true));
        assertEquals(DualRules.Verdict.KEEP, DualRules.verdict(true, true));
        assertEquals(DualRules.Verdict.KEEP, DualRules.verdict(false, false));
    }

    @Test
    void everyArtIsBoundedAndTheSkillChecksEverythingOnTheServer() throws IOException {
        String a = code("skill/DevilKitArts.java");
        assertTrue(a.contains("MAX_STRIKE = 6") && a.contains("MAX_BURST = 8") && a.contains("MAX_RUSH = 4"), "the target caps");
        assertTrue(a.contains("DualRules.MAX_RAW") && a.contains("BalanceLaw.damage("), "the hit is capped and goes through the balance law");
        assertTrue(a.contains("DevilMagic.source("), "devil magic damage type");
        String s = code("skill/DevilAwakeningSkill.java");
        assertTrue(s.contains("Mastery.awake(c)") && s.contains("onCoolDown") && s.contains("pay(") && s.contains("Mastery.tier(c) < art.tier()"), "awake, cooldown, price and tier are checked before an art");
        assertTrue(s.contains("DevilConfig.kitArts()"), "the arts have a server switch");
        assertTrue(s.indexOf("pay(") < s.indexOf("DevilKitArts.cast"), "the price is paid before the art fires");
    }

    @Test
    void thePacketFieldsAreAppendedAndReadOnlyWhenThere() throws IOException {
        String n = code("devil/DevilNet.java");
        assertTrue(n.contains("int mastery, int awakeSec, int restSec"), "the three fields");
        assertTrue(n.contains("boolean more = buf.isReadable()"), "a packet of the older shape still reads");
        assertTrue(n.contains("clampInt(buf.readVarInt(), 0, 1000)"), "the mastery is clamped on read");
    }

    @Test
    void gainsAreCountedOncePerSecondPerKindAndTheAfkFactorIsUsed() throws IOException {
        String m = code("devil/mastery/Mastery.java");
        assertTrue(m.contains("MasteryRules.afkFactor("), "the anti-AFK factor");
        assertTrue(m.contains("MasteryRules.capLeft("), "the daily cap");
        assertTrue(m.contains("lastPassiveHit < 20"), "one counted blow a second");
        assertTrue(m.contains("KILL_GAP") && m.contains("KILL_MIN_HEALTH"), "a counted kill needs a gap and a body");
    }
}
