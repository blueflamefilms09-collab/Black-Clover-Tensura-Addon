package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.rules.FictionRules;
import com.newuniverse.nusmp.skill.rules.UltimateClass;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.server.MinecraftServer;

import java.util.HashSet;
import java.util.Set;

/**
 * CI self-tests of the All Fiction god-class rework (prefix {@code fictiongod}): on the booted server All Fiction is still the registered {@link AllFictionSkill} in the GOD class with the sixteen modes (ten
 * with the setting off) whose first ten are where they were, ids unique, names not empty, every cost, cooldown, window and per-press cap in bounds; Book Maker keeps its id and its first two modes and has
 * the lead-in modes after them; the evolution Book Maker to All Fiction is still registered. Registry and rule reads only: no player, no world, no tick work.
 */
public final class FictionGodSelfTests {
    private FictionGodSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("fictiongod.allfiction", FictionGodSelfTests::allFiction);
        SelfTests.register("fictiongod.bookmaker", FictionGodSelfTests::bookMaker);
    }

    private static Result allFiction(MinecraftServer server) {
        ManasSkill skill = SkillAPI.getSkillRegistry().get(NUSkills.ALL_FICTION.getId());
        if (!(skill instanceof AllFictionSkill fic)) return Result.fail("All Fiction is not registered as an AllFictionSkill: " + skill);
        if (fic.ultimateClass() != UltimateClass.GOD) return Result.fail("All Fiction left the GOD class: " + fic.ultimateClass());
        ManasSkillInstance inst = fic.createDefaultInstance();
        int want = FictionRules.wheel(NUConfig.ALL_FICTION_REBUILT.get(), NUConfig.allFictionGod());
        if (fic.getModes(inst) != want) return Result.fail("All Fiction should have " + want + " modes, has " + fic.getModes(inst));
        if (FictionRules.GOD_IDS.length != FictionRules.GOD_MODES) return Result.fail("GOD_IDS and GOD_MODES disagree");
        Set<String> ids = new HashSet<>();
        for (int m = 0; m < FictionRules.GOD_MODES; m++) {
            if (!FictionRules.GOD_IDS[m].equals(fic.getModeId(inst, m))) return Result.fail("mode " + m + " id moved: " + fic.getModeId(inst, m));
            if (m < FictionRules.ALL_MODES && !FictionRules.ALL_IDS[m].equals(FictionRules.GOD_IDS[m])) return Result.fail("one of the first ten modes was renamed: " + m);
            if (!ids.add(fic.getModeId(inst, m))) return Result.fail("two All Fiction modes share the id " + fic.getModeId(inst, m));
            if (fic.getModeName(inst, m).getString().isBlank()) return Result.fail("All Fiction mode " + m + " has no name");
            if (m != FictionRules.STAND_UP && (FictionRules.cost(m) <= 0 || FictionRules.cost(m) > 10000)) return Result.fail("All Fiction mode " + m + " cost out of bounds: " + FictionRules.cost(m));
            if (m != FictionRules.STAND_UP && (FictionRules.cooldown(m) <= 0 || FictionRules.cooldown(m) > 6000)) return Result.fail("All Fiction mode " + m + " cooldown out of bounds: " + FictionRules.cooldown(m));
        }
        if (FictionRules.ruleTicks() > 600 || FictionRules.skinTicks() > 1200 || FictionRules.reversalTicks() > 400 || FictionRules.reversalCharges() > 5) return Result.fail("a god-class window grew past its bound");
        if (FictionRules.ruleRadius() > 12 || FictionRules.massRadius() > 16 || FictionRules.rulePulseMaxProjectiles() > 32 || FictionRules.retconPerSecond() > 2) return Result.fail("a god-class area or cap grew past its bound");
        if (FictionRules.massUnlocked(false) || !FictionRules.massUnlocked(true)) return Result.fail("Mass Fiction must be mastered-only");
        return Result.ok(want + " modes (" + FictionRules.GOD_MODES + " built), GOD class, ids unique, costs / cooldowns / windows / caps in bounds; allFictionGod=" + NUConfig.allFictionGod());
    }

    private static Result bookMaker(MinecraftServer server) {
        ManasSkill skill = SkillAPI.getSkillRegistry().get(NUSkills.BOOK_MAKER.getId());
        if (!(skill instanceof BookMakerSkill bm)) return Result.fail("Book Maker is not registered as a BookMakerSkill: " + skill);
        ManasSkillInstance inst = bm.createDefaultInstance();
        int want = FictionRules.bookMakerModes(NUConfig.allFictionGod());
        if (bm.getModes(inst) != want) return Result.fail("Book Maker should have " + want + " modes, has " + bm.getModes(inst));
        if (!"bookmaker.pin".equals(bm.getModeId(inst, FictionRules.PIN)) || !"bookmaker.barrage".equals(bm.getModeId(inst, FictionRules.BARRAGE))) return Result.fail("Pin or Barrage moved");
        if (FictionRules.modes() != 2) return Result.fail("the old Book Maker count changed");
        return Result.ok(want + " Book Maker modes, Pin and Barrage first, evolves into All Fiction; allFictionGod=" + NUConfig.allFictionGod());
    }
}
