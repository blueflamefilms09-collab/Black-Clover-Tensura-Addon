package com.newuniverse.nusmp.skill.rules;

import static org.junit.jupiter.api.Assertions.*;

import com.newuniverse.nusmp.skill.rules.BeastSkillRules.Passive;
import com.newuniverse.nusmp.skill.rules.BeastSkillRules.Spec;
import com.newuniverse.nusmp.skill.rules.DbRules.Mode;
import com.newuniverse.nusmp.skill.rules.DbRules.Prim;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** 3.58.0: the Beast Will skill table. Pure data checks plus the files that must exist for every row (icons, lang, finisher shapes). */
class BeastSkillRulesTest {
    private static final List<String> OLD = List.of("dragon", "shadow_panther", "elderwood_guardian", "lion", "ridge_wolf", "crested_boar", "wind_skimmer", "flame_serpent", "thunder_fang_chimera");
    private static final List<String> NEW = List.of("fenrir", "jormungandr", "cerberus", "minotaur", "hydra", "kraken", "hafgufa", "typhon", "scylla_charybdis", "sphinx", "pegasus", "cu_sith", "nidhoggr",
            "yamata_no_orochi", "kitsune", "nue", "ammit", "apophis");
    private static final List<String> LEGENDARY = List.of("dragon", "typhon", "yamata_no_orochi", "apophis", "jormungandr", "fenrir", "nidhoggr");
    private static final String ICONS = "src/main/resources/assets/nusmp/textures/skill/";

    private static String read(String path) throws Exception { return Files.readString(Path.of(path), StandardCharsets.UTF_8); }

    @Test void twentySevenBeastsWithTheFixedKeysAndUniqueIds() {
        assertEquals(27, BeastSkillRules.SPECS.size());
        List<String> want = new ArrayList<>(OLD);
        want.addAll(NEW);
        assertEquals(want, BeastSkillRules.SPECS.stream().map(Spec::key).toList(), "the order is the registration order: only ever append");
        Set<String> ids = new HashSet<>();
        for (Spec s : BeastSkillRules.SPECS) {
            assertTrue(s.id().matches("bw_[a-z0-9_]+"), s.id());
            assertTrue(ids.add(s.id()) && ids.add(s.uniqueId()), "duplicate id " + s.id());
            assertEquals(s.key() + "_will", s.willId());
            assertSame(s, BeastSkillRules.spec(s.id()));
            assertSame(s, BeastSkillRules.spec(s.uniqueId()));
            assertSame(s, BeastSkillRules.spec(s.willId()));
        }
        assertEquals(54, ids.size());
    }

    @Test void noIdClashesWithAnExistingSkill() throws Exception {
        Set<String> taken = new HashSet<>();
        Matcher m = Pattern.compile("register\\(\"([a-z0-9_]+)\"").matcher(read("src/main/java/com/newuniverse/nusmp/skill/NUSkills.java"));
        while (m.find()) taken.add(m.group(1));
        for (DbRules.Spec d : DbRules.SPECS) { taken.add(d.id()); taken.add(d.uniqueId()); }
        assertTrue(taken.size() > 30, "the registry scan found the ids");
        for (Spec s : BeastSkillRules.SPECS) { assertFalse(taken.contains(s.id()), s.id()); assertFalse(taken.contains(s.uniqueId()), s.uniqueId()); }
    }

    @Test void everyBeastHasThreePassivesTwoActivesAndOneFinisher() {
        Set<String> finisherNames = new HashSet<>();
        for (Spec s : BeastSkillRules.SPECS) {
            assertEquals(3, s.passives().size(), s.id() + " passives (the remake gives every beast three)");
            assertEquals(3, s.actives().size(), s.id() + " wheel size (3.60.0 remake: 2 actives + the finisher)");
            assertEquals(2, s.uniqueActives().size(), s.id() + " unique wheel size");
            assertNotNull(s.finisher(), s.id() + " finisher");
            assertEquals(1, s.actives().stream().filter(Mode::mastery).count(), s.id() + " finisher count");
            assertTrue(s.actives().get(s.actives().size() - 1).mastery(), s.id() + ": the finisher is last");
            assertTrue(finisherNames.add(s.finisher().name()) && finisherNames.add(s.finisher().id()), s.id() + " finisher is not unique to the beast");
            assertFalse(s.uniqueActives().contains(s.finisher()));
            Set<String> modeIds = new HashSet<>();
            for (Passive p : s.passives()) assertTrue(modeIds.add(p.id()) && p.id().matches("[a-z0-9_]+") && !p.name().isBlank(), s.id() + " passive " + p.id());
            for (Mode m : s.actives()) {
                assertTrue(modeIds.add(m.id()) && m.id().matches("[a-z0-9_]+"), s.id() + " duplicate mode " + m.id());
                assertFalse(m.passive(), m.id() + " must be an active primitive");
                assertTrue(m.cost() >= 0 && m.masteredCost() <= m.cost() && m.cooldown() > 0 && m.masteredCooldown() <= m.cooldown(), s.id() + "." + m.id());
                assertTrue(m.move().range() >= 0 && m.move().radius() >= 0 && m.move().n() >= 0 && m.move().rgb() == (s.rgb()), m.id() + " numbers and colour");
                assertTrue(m.move().n() <= BeastSkillRules.MAX_TARGETS, m.id() + " n is capped at 24 (a barrage's n is hits on one target, also capped)");
                assertTrue(m.move().range() <= 72 && m.move().radius() <= 24, m.id() + " stays inside the dispatcher's reach");
                assertEquals(0, m.move().debt(), m.id() + " uses no Temporal Debt");
                assertNotEquals(Prim.DETONATE, m.move().prim(), m.id());
            }
        }
        assertEquals(27, finisherNames.size() / 2);
    }

    @Test void legendaryBeastsAreTheStrongestAndUniqueIsHalf() {
        List<Spec> byScale = new ArrayList<>(BeastSkillRules.SPECS);
        byScale.sort((a, b) -> Double.compare(b.scale(), a.scale()));
        Set<String> top = new HashSet<>();
        for (int i = 0; i < LEGENDARY.size(); i++) top.add(byScale.get(i).key());
        assertEquals(new HashSet<>(LEGENDARY), top, "the seven legendary beasts hold the seven highest scales");
        for (Spec s : BeastSkillRules.SPECS) {
            assertTrue(s.scale() > 0.2 && s.scale() <= 1.0, s.id());
            assertEquals(BeastSkillRules.power(s, true) * BeastSkillRules.UNIQUE_FACTOR, BeastSkillRules.power(s, false), 1e-9);
        }
        assertEquals(DbRules.UNIQUE_FACTOR, BeastSkillRules.UNIQUE_FACTOR, 1e-9);
        assertTrue(BeastSkillRules.dodge(new Passive("x", "x", BeastSkillRules.Pas.ILLUSION, 0.9, 0), true) <= 0.6, "a dodge never passes 60 %");
        assertTrue(BeastSkillRules.lowHealth(6, 20) && !BeastSkillRules.lowHealth(8, 20));
    }

    @Test void uniqueFormHasTheFirstPassiveOnlyAndTheDbAdapterKeepsTheWheel() {
        for (Spec s : BeastSkillRules.SPECS) {
            assertEquals(1, s.passivesOf(false).size());
            assertEquals(s.passives().size(), s.passivesOf(true).size());
            DbRules.Spec d = s.asDb();
            assertEquals(s.actives().size(), d.actives().size(), s.id());
            assertEquals(s.scale(), d.scale(), 1e-9);
        }
    }

    @Test void theFenrirIsTheGodOfWarWolf() {
        Spec f = BeastSkillRules.FENRIR;
        assertEquals("Gleipnir Breaks", f.finisher().name());
        assertTrue(f.passives().stream().anyMatch(p -> p.name().contains("Soul")), "soul-light passive");
        assertTrue(f.actives().stream().anyMatch(m -> m.name().contains("Soul-Frost")), "soul-frost active");
    }

    @Test void everyFinisherShapeExistsInTheEffectEnum() throws Exception {
        String shapes = read("src/main/java/com/newuniverse/nusmp/vfx/VfxShape.java");
        for (Spec s : BeastSkillRules.SPECS) {
            assertTrue(s.fx().matches("[LGFS]:[A-Z0-9_]+"), s.id() + " fx " + s.fx());
            assertTrue(Pattern.compile("(?m)^\\s*(?:[A-Z0-9_]+,\\s*)*" + s.fx().substring(2) + "\\b").matcher(shapes).find(), s.id() + ": no VfxShape " + s.fx());
        }
    }

    @Test void everySkillHasAnOpaqueDistinct32pxIcon() throws Exception {
        Map<String, String> seen = new HashMap<>();
        for (Spec s : BeastSkillRules.SPECS) for (String id : new String[] {s.id(), s.uniqueId()}) {
            File f = new File(ICONS + id + ".png");
            assertTrue(f.isFile(), "icon " + id);
            BufferedImage img = ImageIO.read(f);
            assertNotNull(img, id);
            assertEquals(32, img.getWidth(), id); assertEquals(32, img.getHeight(), id);
            int[] px = img.getRGB(0, 0, 32, 32, null, 0, 32);
            assertNull(seen.put(Arrays.toString(px), id), id + " has the same icon as another skill");
            assertTrue(Arrays.stream(px).filter(p -> (p >>> 24) > 200).count() > 32 * 32 * 0.8, id + " icon should fill its frame");
        }
    }

    @Test void theLangFileHasEveryNameDescriptionAndMode() throws Exception {
        String lang = read("tools/lang/beastskills.json");
        for (Spec s : BeastSkillRules.SPECS) {
            for (String k : new String[] {"nusmp.skill." + s.id(), "nusmp.skill." + s.id() + ".description", "nusmp.skill." + s.uniqueId(), "nusmp.skill." + s.uniqueId() + ".description"})
                assertTrue(lang.contains("\"" + k + "\""), "lang " + k);
            for (Passive p : s.passives()) assertTrue(lang.contains("\"nusmp.skill.mode." + s.id() + "." + p.id() + "\""), "lang passive " + p.id());
            for (Mode m : s.actives()) assertTrue(lang.contains("\"nusmp.skill.mode." + s.id() + "." + m.id() + "\""), "lang mode " + m.id());
        }
    }

    @Test void theSkillsAreWiredInAndTheSmokeListExpectsThem() throws Exception {
        assertTrue(read("src/main/java/com/newuniverse/nusmp/skill/NUSkills.java").contains("BeastSkills.registerAll(SKILLS)"));
        assertTrue(read("src/main/java/com/newuniverse/nusmp/skill/SkillEvolution.java").contains("BeastSkills.evolve(player, skills)"));
        assertTrue(read("src/main/java/com/newuniverse/nusmp/NUSMP.java").contains("BeastSkillSelfTests.register()"));
        assertTrue(read("src/main/java/com/newuniverse/nusmp/NUSMP.java").contains("BeastPassives.register("));
        assertTrue(read("src/main/java/com/newuniverse/nusmp/multiverse/MultiverseCommands.java").contains("BeastSkills.node()"));
        assertTrue(read("src/main/java/com/newuniverse/nusmp/skill/BeastSkillSelfTests.java").contains("\"skill.beast\""));
        assertTrue(read(".github/workflows/smoke.yml").contains("skill.db,skill.beast,"), "the smoke list expects skill.beast");
        String cfg = read("src/main/java/com/newuniverse/nusmp/NUConfig.java");
        assertTrue(cfg.contains("\"beastSkillsEnabled\"") && cfg.contains("\"beastSkillGrantOnMastery\""));
    }
}
