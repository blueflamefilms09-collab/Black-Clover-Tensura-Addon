package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.rules.FictionRules;
import com.newuniverse.nusmp.skill.rules.NenRules;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.server.MinecraftServer;

import java.util.HashSet;
import java.util.Set;

/**
 * The CI self-tests of 3.46.0 (ids {@code skill.nen.registry} and {@code skill.fiction.rebuild}): on the booted server Nen is in the skill registry with its ten modes (ids unique, names not empty,
 * every cost and cooldown in bounds), and All Fiction is the same registered skill as before with the first two modes where they were, the other eight added after them, and the wheel the size the
 * server setting says (ten, or the old two). Registry and rule reads only: no player, no world, no tick work (the stand-in cannot hold a skill of this mod here, see {@link ResetGuardSelfTests}).
 */
public final class UltimatePairSelfTests {
    private UltimatePairSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("skill.nen.registry", UltimatePairSelfTests::nen);
        SelfTests.register("skill.fiction.rebuild", UltimatePairSelfTests::fiction);
    }

    private static Result nen(MinecraftServer server) {
        ManasSkill skill = SkillAPI.getSkillRegistry().get(NUSkills.NEN.getId());
        if (!(skill instanceof NenSkill nen)) return Result.fail("Nen is not registered as a NenSkill: " + skill);
        ManasSkillInstance inst = nen.createDefaultInstance();
        if (nen.getModes(inst) != NenRules.MODES) return Result.fail("Nen should have " + NenRules.MODES + " modes, has " + nen.getModes(inst));
        Set<String> ids = new HashSet<>();
        for (int m = 0; m < NenRules.MODES; m++) {
            String id = nen.getModeId(inst, m);
            if (!ids.add(id)) return Result.fail("two Nen modes share the id " + id);
            if (nen.getModeName(inst, m).getString().isBlank()) return Result.fail("Nen mode " + id + " has no name");
            if (NenRules.cost(m) < 0 || NenRules.cost(m) > 5000) return Result.fail("Nen mode " + id + " cost out of bounds: " + NenRules.cost(m));
            if (NenRules.cooldown(m) <= 0 || NenRules.cooldown(m) > 6000) return Result.fail("Nen mode " + id + " cooldown out of bounds: " + NenRules.cooldown(m));
            if (NenRules.timed(m) && NenRules.duration(m) > 1200) return Result.fail("Nen mode " + id + " window too long: " + NenRules.duration(m));
        }
        if (NenRules.takenMultiplier(true, true, true, true) < 0.25 || NenRules.takenMultiplier(false, false, true, true) > 1.5) return Result.fail("Nen defence multiplier left its [0.25, 1.5] band");
        return Result.ok(NenRules.MODES + " modes, ids unique, costs and cooldowns in bounds; server setting nenEnabled=" + NUConfig.NEN_ENABLED.get());
    }

    private static Result fiction(MinecraftServer server) {
        ManasSkill skill = SkillAPI.getSkillRegistry().get(NUSkills.ALL_FICTION.getId());
        if (!(skill instanceof AllFictionSkill fic)) return Result.fail("All Fiction is not registered as an AllFictionSkill: " + skill);
        ManasSkillInstance inst = fic.createDefaultInstance();
        int want = FictionRules.wheel(NUConfig.ALL_FICTION_REBUILT.get());
        if (fic.getModes(inst) != want) return Result.fail("All Fiction should have " + want + " modes with allFictionRebuilt=" + NUConfig.ALL_FICTION_REBUILT.get() + ", has " + fic.getModes(inst));
        if (!"fiction.undo".equals(fic.getModeId(inst, FictionRules.UNDO)) || !"fiction.erase".equals(fic.getModeId(inst, FictionRules.ERASE))) return Result.fail("the first two modes moved or were renamed");
        Set<String> ids = new HashSet<>();
        for (int m = 0; m < FictionRules.ALL_MODES; m++) {
            if (!ids.add(fic.getModeId(inst, m))) return Result.fail("two All Fiction modes share the id " + fic.getModeId(inst, m));
            if (fic.getModeName(inst, m).getString().isBlank()) return Result.fail("All Fiction mode " + m + " has no name");
            if (m != FictionRules.STAND_UP && (FictionRules.cost(m) <= 0 || FictionRules.cooldown(m) <= 0)) return Result.fail("All Fiction mode " + m + " has no cost or cooldown");
        }
        if (FictionRules.modes() != 2) return Result.fail("Book Maker's two modes changed (FictionRules.modes() = " + FictionRules.modes() + ")");
        if (FictionRules.worldPulseMaxProjectiles() > 32 || FictionRules.unmakeMax() > 32 || FictionRules.shareMax() > 8) return Result.fail("a per-press cap grew past its bound");
        return Result.ok(want + " modes on the wheel (" + FictionRules.ALL_MODES + " built), the first two unchanged, caps bounded; allFictionRebuilt=" + NUConfig.ALL_FICTION_REBUILT.get());
    }
}
