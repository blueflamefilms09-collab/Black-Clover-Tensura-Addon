package com.newuniverse.nusmp.skill.rules;

import static org.junit.jupiter.api.Assertions.*;

import com.newuniverse.nusmp.heroweapon.HeroClass;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.skill.rules.WeaponSkillRules.Act;
import com.newuniverse.nusmp.skill.rules.WeaponSkillRules.Kind;
import com.newuniverse.nusmp.skill.rules.WeaponSkillRules.Mode;
import com.newuniverse.nusmp.skill.rules.WeaponSkillRules.Verdict;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Weapon skills (docs/weapon_skills.md): the pure table (every mode of every wheel is exactly one server action, no id twice, every action used), the wiring into the three systems and the files every row needs (icons, lang),
 * and a source scan that the new code names no client class (rule 9: ServerSafetyTest's allow-list must not need a new line).
 */
class WeaponSkillRulesTest {
    private static final String MAIN = "src/main/java/com/newuniverse/nusmp/";
    private static final String ICONS = "src/main/resources/assets/anime_multiverse/textures/skill/";
    private static final List<String> NEW_FILES = List.of("skill/WeaponSkill.java", "skill/WeaponSkills.java", "skill/WeaponSkillSelfTests.java", "skill/rules/WeaponSkillRules.java");

    private static String read(String path) throws Exception { return Files.readString(Path.of(path), StandardCharsets.UTF_8); }

    private static List<Mode> all() {
        List<Mode> out = new ArrayList<>();
        for (Kind k : Kind.values()) out.addAll(WeaponSkillRules.modes(k));
        return out;
    }

    @Test void everyModeIsExactlyOneActionAndNoIdOrActionRepeats() {
        assertEquals(5, WeaponSkillRules.HERO.size());
        assertEquals(7, WeaponSkillRules.DJINN.size());
        assertEquals(6, WeaponSkillRules.BEAST.size());
        Set<String> ids = new HashSet<>();
        Map<Act, String> used = new EnumMap<>(Act.class);
        for (Mode m : all()) {
            assertTrue(ids.add(m.id()), "mode id used twice: " + m.id());
            assertTrue(m.id().matches("(hero|djinn|beast)_[a-z0-9_]+"), m.id());
            assertNotNull(m.act(), m.id());
            assertNull(used.put(m.act(), m.id()), "action " + m.act() + " is stood for by two modes");
            assertFalse(m.name().isBlank());
            assertFalse(m.blurb().isBlank());
        }
        assertEquals(Act.values().length, used.size(), "every action must belong to a mode: " + used.keySet());
    }

    @Test void actionsBelongToTheirOwnFamily() {
        for (Mode m : WeaponSkillRules.HERO) assertTrue(m.act().name().startsWith("H_"), m.id());
        for (Mode m : WeaponSkillRules.DJINN) assertTrue(m.act().name().startsWith("D_"), m.id());
        for (Mode m : WeaponSkillRules.BEAST) assertTrue(m.act().name().startsWith("B_"), m.id());
    }

    @Test void theTwelveSkillsAreTheTenVassalWeaponsTheDjinnAndTheBeastWill() {
        List<HeroKind> kinds = WeaponSkillRules.heroKinds();
        assertEquals(10, kinds.size());
        for (HeroKind k : kinds) assertEquals(HeroClass.VASSAL, k.heroClass(), k.name());
        assertEquals(List.of(HeroKind.CARRIAGE, HeroKind.FAN, HeroKind.SCYTHE, HeroKind.KATANA, HeroKind.BOOK, HeroKind.MIRROR, HeroKind.INSTRUMENT, HeroKind.HARPOON, HeroKind.SEWING_SET, HeroKind.CHAIN), kinds);
        assertFalse(WeaponSkillRules.coversHero(HeroKind.SHIELD));
        assertFalse(WeaponSkillRules.coversHero(HeroKind.HAMMER), "the Seven Star weapons are not in this release");
        assertFalse(WeaponSkillRules.coversHero(null));
        List<String> ids = WeaponSkillRules.allIds();
        assertEquals(12, ids.size());
        assertEquals(12, new HashSet<>(ids).size());
        for (String id : ids) assertTrue(id.matches("ws_[a-z_]+"), id);
        assertEquals("ws_vassal_fan", WeaponSkillRules.heroId(HeroKind.FAN));
        assertEquals("Vassal Fan Arts", WeaponSkillRules.heroName(HeroKind.FAN));
        assertEquals(List.of("ws_djinn", "ws_beast_will"), ids.subList(10, 12), "the order is the registration order: only ever append");
    }

    @Test void theVerdictChecksTheSettingFirstThenTheOwner() {
        assertEquals(Verdict.SWITCHED_OFF, WeaponSkillRules.verdict(false, true));
        assertEquals(Verdict.SWITCHED_OFF, WeaponSkillRules.verdict(false, false));
        assertEquals(Verdict.NOT_OWNED, WeaponSkillRules.verdict(true, false));
        assertEquals(Verdict.OK, WeaponSkillRules.verdict(true, true));
    }

    @Test void atMostOneActionPerPlayerPerTick() {
        assertTrue(WeaponSkillRules.due(100, 50));
        assertFalse(WeaponSkillRules.due(100, 100), "a second press in the same tick is dropped");
        assertTrue(WeaponSkillRules.due(100, 101), "a clock that went back is due");
    }

    @Test void theSkillClassHandlesEveryActionAndEachCallsTheKeysOwnEntryPoint() throws Exception {
        String src = read(MAIN + "skill/WeaponSkill.java");
        String dispatch = src.substring(src.indexOf("private static boolean dispatch"), src.indexOf("private static boolean technique"));
        assertFalse(dispatch.contains("default ->"), "no default: a new action must fail to compile until wired");
        for (Act a : Act.values()) assertEquals(1, count(dispatch, "case " + a.name() + " ->"), a.name());
        // the packets of the old keys end in the same calls the skill makes
        assertTrue(read(MAIN + "heroweapon/bond/HeroNet.java").contains("HeroKeys.onKey(sp, payload.slot(), payload.phase())"));
        assertTrue(read(MAIN + "heroweapon/bond/HeroNet.java").contains("HeroKeys.onAction(sp, payload.action())"));
        assertTrue(read(MAIN + "relics/djinn/DjinnNet.java").contains("DjinnEngine.onKey(sp, payload.action())"));
        assertTrue(read(MAIN + "beastwill/BeastWillNet.java").contains("BeastWillManager.onAction(p, op, arg)"));
        assertTrue(dispatch.contains("HeroKeys.onKey(") || src.contains("HeroKeys.onKey(p, slot, HeroNet.PRESS)"));
        assertTrue(dispatch.contains("HeroKeys.onAction(p, HeroNet.ACT_TOGGLE)") && dispatch.contains("HeroKeys.onAction(p, HeroNet.ACT_CYCLE)"));
        assertTrue(dispatch.contains("DjinnEngine.onKey(p, DjinnNet.ACT_DOWN)") && dispatch.contains("DjinnEngine.onKey(p, DjinnNet.ACT_OFF)") && dispatch.contains("DjinnEngine.onKey(p, DjinnNet.ACT_RUKH)"));
        assertTrue(dispatch.contains("BeastWillManager.onAction(p, BeastWillNet.OP_FORM, BeastWillRules.ACQUIRE)") && dispatch.contains("BeastWillManager.onAction(p, BeastWillNet.OP_FORM, BeastWillRules.INTEGRATE)"));
        assertTrue(dispatch.contains("BeastWillNet.OP_ABILITY, 1)") && dispatch.contains("BeastWillNet.OP_ABILITY, 2)"));
        assertTrue(dispatch.contains("BeastWillNet.OP_SWAP, 1)") && dispatch.contains("BeastWillNet.OP_SWAP, -1)"));
        // the server entry the Djinn wheel adds ends in the same go() the keys end in, behind the same guards
        String engine = read(MAIN + "relics/djinn/DjinnEngine.java");
        String target = engine.substring(engine.indexOf("public static void onSkillTarget"), engine.indexOf("static boolean go(ServerPlayer p, int target, boolean admin)"));
        assertTrue(target.contains("go(p, target, false)") && target.contains("DjinnPhaseRules.KEY_SPACING") && target.contains("Severance.isSevered(p)"));
    }

    private static int count(String s, String part) {
        int n = 0;
        for (int i = s.indexOf(part); i >= 0; i = s.indexOf(part, i + 1)) n++;
        return n;
    }

    @Test void theOldKeysAndPacketsStayExactlyAsTheyWere() throws Exception {
        String hero = read(MAIN + "client/heroweapon/HeroKeysClient.java");
        assertTrue(hero.contains("new HeroNet.Key(i, HeroNet.PRESS)") && hero.contains("HeroNet.ACT_TOGGLE") && hero.contains("HeroNet.ACT_CYCLE"));
        String djinn = read(MAIN + "client/relics/djinn/DjinnKeys.java");
        assertTrue(djinn.contains("DjinnNet.ACT_UP") && djinn.contains("DjinnNet.ACT_DOWN") && djinn.contains("DjinnNet.ACT_OFF") && djinn.contains("DjinnNet.ACT_RUKH"));
        String beast = read(MAIN + "client/beastwill/BeastWillClient.java");
        assertTrue(beast.contains("BeastWillNet.OP_ABILITY, 1") && beast.contains("BeastWillNet.OP_SWAP") && beast.contains("BeastWillNet.OP_FORM, BeastWillRules.INTEGRATE"));
    }

    @Test void theNewCodeNamesNoClientClassAndNeedsNoAllowListLine() throws Exception {
        Pattern client = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)|\\bDist\\.CLIENT\\b");
        String allow = read("docs/server_safe_allow.txt");
        for (String f : NEW_FILES) {
            String src = read(MAIN + f);
            assertFalse(client.matcher(src).find(), f + " names a client class");
            assertFalse(allow.contains(f), f + " must not be on the allow-list");
        }
        // the three systems got only a call into the skills, and the Djinn hook is a no-op for anything but a server player
        assertTrue(read(MAIN + "relics/djinn/DjinnState.java").contains("player instanceof net.minecraft.server.level.ServerPlayer sp"));
    }

    @Test void thePerTickCostIsNil() throws Exception {
        for (String f : NEW_FILES) {
            String src = read(MAIN + f);
            assertFalse(src.contains("PlayerTickEvent") || src.contains("ServerTickEvent") || src.contains("LevelTickEvent"), f + " must not listen to a tick");
            assertFalse(src.contains("canTick") || src.contains("onTick("), f + " must not tick");
            assertFalse(src.contains("getEntitiesOfClass") || src.contains("getPlayers()"), f + " must not scan");
        }
        assertTrue(read(MAIN + "skill/WeaponSkills.java").contains("LAST.remove(e.getEntity().getUUID())"), "the per-player entry is dropped at logout");
    }

    @Test void theSkillsAreWiredIntoTheModAndTheSystems() throws Exception {
        String sk = read(MAIN + "skill/NUSkills.java");
        assertTrue(sk.contains("WeaponSkills.registerAll(SKILLS)"));
        assertTrue(sk.indexOf("WeaponSkills.registerAll(SKILLS)") > sk.indexOf("SEIFORT_HOLY_JUDGEMENT"), "registered last so no older order changes");
        assertTrue(read(MAIN + "NUSMP.java").contains("WeaponSkills.init()"));
        assertTrue(read(MAIN + "multiverse/MultiverseCommands.java").contains("WeaponSkills.node()"));
        String state = read(MAIN + "relics/djinn/DjinnState.java");
        assertEquals(3, count(state, "        weaponSkills();"), "bind, lend and clearBond tell the skills");
        assertTrue(state.contains("WeaponSkills.syncDjinn(sp)"));
        assertTrue(read(MAIN + "beastwill/BeastWillAcquire.java").contains("WeaponSkills.syncBeast(p)"));
        assertTrue(read(MAIN + "beastwill/BeastWillCommands.java").contains("WeaponSkills.syncBeast(p)"));
        String skills = read(MAIN + "skill/WeaponSkills.java");
        assertTrue(skills.contains("HeroBonds.addListener(") && skills.contains("onBound(") && skills.contains("onReleased("));
        assertTrue(skills.contains("PlayerLoggedInEvent") && skills.contains("PlayerRespawnEvent"));
        assertTrue(read(MAIN + "skill/WeaponSkillSelfTests.java").contains("\"weapon.skills\""));
        String cfg = read(MAIN + "NUConfig.java");
        assertTrue(cfg.contains("\"weaponSkills\"") && cfg.contains("WEAPON_SKILLS"));
        assertTrue(read(MAIN + "skill/WeaponSkill.java").contains("NUConfig.WEAPON_SKILLS.get()"), "the SERVER setting turns the skills off");
    }

    @Test void everySkillHasAnIconAndAllTheLangKeys() throws Exception {
        String fragment = read("tools/lang/weapon_skills.json");
        String lang = read("src/main/resources/assets/anime_multiverse/lang/en_us.json");
        for (String id : WeaponSkillRules.allIds()) {
            File f = new File(ICONS + id + ".png");
            assertTrue(f.isFile(), "icon " + id);
            BufferedImage img = ImageIO.read(f);
            assertNotNull(img, id);
            assertEquals(32, img.getWidth(), id);
            assertEquals(32, img.getHeight(), id);
            for (String k : new String[] {"anime_multiverse.skill." + id, "anime_multiverse.skill." + id + ".description"}) {
                assertTrue(fragment.contains("\"" + k + "\""), "fragment " + k);
                assertTrue(lang.contains("\"" + k + "\""), "en_us " + k);
            }
        }
        for (Mode m : all()) {
            String k = "anime_multiverse.skill.mode.weapon." + m.id();
            assertTrue(fragment.contains("\"" + k + "\"") && lang.contains("\"" + k + "\""), k);
        }
        // every SkillMsg key the skill code uses is in the lang
        Matcher msg = Pattern.compile("SkillMsg\\.t\\(\"(weapon\\.[a-z_]+)\"").matcher(read(MAIN + "skill/WeaponSkill.java"));
        int n = 0;
        while (msg.find()) {
            n++;
            assertTrue(lang.contains("\"anime_multiverse.skill.msg." + msg.group(1) + "\""), msg.group(1));
        }
        assertEquals(4, n);
        for (String k : new String[] {"granted", "revoked"}) assertTrue(lang.contains("\"anime_multiverse.skill.msg.weapon." + k + "\""), k);
    }
}
