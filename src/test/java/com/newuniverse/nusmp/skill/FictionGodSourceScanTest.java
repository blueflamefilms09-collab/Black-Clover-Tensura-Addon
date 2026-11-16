package com.newuniverse.nusmp.skill;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the god-class All Fiction runs on a public server: no client classes, no static state, every new hit through the balance law, every new feature behind its setting, bounded queries, the ids and evolution kept. */
class FictionGodSourceScanTest {
    private static String read(String file) throws Exception {
        return Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/" + file), StandardCharsets.UTF_8);
    }

    @Test
    void bothSkillsStayServerSafeAndBounded() throws Exception {
        for (String f : new String[]{"skill/AllFictionSkill.java", "skill/BookMakerSkill.java", "skill/FictionGodSelfTests.java"}) {
            String c = read(f);
            assertFalse(c.contains("net.minecraft.client") || c.contains("com.mojang.blaze3d") || c.contains("Minecraft.getInstance"), f + " reaches client code");
            assertFalse(c.contains("Map<UUID") || c.contains("static List<ServerPlayer"), f + " holds players in a static collection");
        }
        String a = read("skill/AllFictionSkill.java");
        assertTrue(a.split("getEntitiesOfClass\\(", -1).length - 1 <= 1, "All Fiction keeps its single projectile box query; creatures go through the capped SkillTargets helpers");
        assertTrue(a.contains("SkillTargets.foes(") && a.contains("FictionRules.rulePulseMaxProjectiles()"), "the rule pulse is bounded");
        assertTrue(a.contains("living.tickCount % 20 != 0"), "the tick stays gated to once a second");
        assertTrue(a.contains("FictionRules.worldPulseDue(tag.getLong(RULE_UNTIL)"), "the rule zone pulses through the pure rule");
    }

    @Test
    void newHitsGoThroughTheBalanceLawAndControlThroughItsCaps() throws Exception {
        String a = read("skill/AllFictionSkill.java");
        assertTrue(a.contains("SkillDamage.hit(player, target, FictionRules.denyDamage()") && a.contains("FictionRules.massDamage()") && a.contains("FictionRules.reversalRaw("), "damage goes through SkillDamage");
        assertTrue(a.contains("BalanceLaw.controlTicks(target, FictionRules.denySilenceTicks())") && a.contains("BalanceLaw.controlTicks(f, FictionRules.massSilenceTicks())"), "silence goes through the control caps");
    }

    @Test
    void everythingNewHonoursTheSettingAndTheOldIsKept() throws Exception {
        String a = read("skill/AllFictionSkill.java");
        assertTrue(a.contains("static boolean god()") && a.contains("NUConfig.allFictionGod()"), "All Fiction reads the god-class setting");
        assertTrue(a.contains("god() && owner instanceof ServerPlayer rp"), "the reversal hook honours the setting");
        for (String key : new String[]{"\"fiction.undo\"", "\"fiction.erase\"", "DeathReadyAt", "public boolean onDeath(", "FictionRules.wheel(rebuilt(), god())"}) assertTrue(a.contains(key), "old part dropped: " + key);
        String b = read("skill/BookMakerSkill.java");
        assertTrue(b.contains("NUConfig.allFictionGod()") && b.contains("SkillAnimeFx2.screw("), "Book Maker honours the setting and keeps its screw effect");
        String reg = read("skill/NUSkills.java");
        assertTrue(reg.contains("SKILLS.register(\"book_maker\", BookMakerSkill::new)") && reg.contains("SKILLS.register(\"all_fiction\", AllFictionSkill::new)"), "ids kept");
        assertTrue(read("skill/SkillEvolution.java").contains("m.put(NUSkills.BOOK_MAKER, NUSkills.ALL_FICTION)"), "the evolution is kept");
        assertTrue(read("NUConfig.java").contains("ALL_FICTION_GOD") && read("NUConfig.java").contains("ultimates_fiction_god"), "the setting exists in its own section");
        assertTrue(read("NUSMP.java").contains("FictionGodSelfTests.register()"), "the self-test is registered");
    }
}
