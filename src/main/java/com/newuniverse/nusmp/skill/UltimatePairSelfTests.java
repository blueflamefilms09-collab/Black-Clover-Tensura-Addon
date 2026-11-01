package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.rules.ControlRules;
import com.newuniverse.nusmp.skill.rules.FictionRules;
import com.newuniverse.nusmp.skill.rules.GateRules;
import com.newuniverse.nusmp.skill.rules.KiRules;
import com.newuniverse.nusmp.skill.rules.NenRules;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.server.MinecraftServer;

import java.util.HashSet;
import java.util.Set;

/**
 * The CI self-tests of 3.46.0 and 3.47.0 (ids {@code skill.nen.registry}, {@code skill.fiction.rebuild}, {@code skill.gate.registry} and {@code skill.ki.registry}): on the booted server Nen,
 * Gate of Babylon and Ki are in the skill registry with their ten modes (ids unique, names not empty, every cost, cooldown, window and per-press cap in bounds), and All Fiction is the same
 * registered skill as before with the first two modes where they were, the other eight added after them, and the wheel the size the server setting says (ten, or the old two). Registry and rule
 * reads only: no player, no world, no tick work (the stand-in cannot hold a skill of this mod here, see {@link ResetGuardSelfTests}).
 */
public final class UltimatePairSelfTests {
    private UltimatePairSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("skill.nen.registry", UltimatePairSelfTests::nen);
        SelfTests.register("skill.fiction.rebuild", UltimatePairSelfTests::fiction);
        SelfTests.register("skill.gate.registry", UltimatePairSelfTests::gate);   // 3.47.0
        SelfTests.register("skill.ki.registry", UltimatePairSelfTests::ki);       // 3.47.0
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
    private static Result gate(MinecraftServer server) {
        ManasSkill skill = SkillAPI.getSkillRegistry().get(NUSkills.GATE_OF_BABYLON.getId());
        if (!(skill instanceof GateOfBabylonSkill gate)) return Result.fail("Gate of Babylon is not registered as a GateOfBabylonSkill: " + skill);
        ManasSkillInstance inst = gate.createDefaultInstance();
        if (gate.getModes(inst) != GateRules.MODES) return Result.fail("Gate of Babylon should have " + GateRules.MODES + " modes, has " + gate.getModes(inst));
        Set<String> ids = new HashSet<>();
        for (int m = 0; m < GateRules.MODES; m++) {
            String id = gate.getModeId(inst, m);
            if (!ids.add(id)) return Result.fail("two Gate of Babylon modes share the id " + id);
            if (gate.getModeName(inst, m).getString().isBlank()) return Result.fail("Gate of Babylon mode " + id + " has no name");
            if (GateRules.cost(m) <= 0 || GateRules.cost(m) > 10000) return Result.fail("Gate of Babylon mode " + id + " cost out of bounds: " + GateRules.cost(m));
            if (GateRules.cooldown(m) <= 0 || GateRules.cooldown(m) > 6000) return Result.fail("Gate of Babylon mode " + id + " cooldown out of bounds: " + GateRules.cooldown(m));
            if (GateRules.timed(m) && GateRules.duration(m) > 1200) return Result.fail("Gate of Babylon mode " + id + " window too long: " + GateRules.duration(m));
        }
        if (GateRules.volleyMax() > 12 || GateRules.rainMax() > 12 || GateRules.gaePierce() > 6) return Result.fail("a per-press cap of Gate of Babylon grew past its bound");
        if (ControlRules.controlTicks(GateRules.chainsTicks(), true, true) >= ControlRules.controlTicks(GateRules.chainsTicks(), false, false)) return Result.fail("a hold must be shorter on a boss or a player");
        return Result.ok(GateRules.MODES + " modes, ids unique, costs and cooldowns in bounds, caps bounded; server setting gateEnabled=" + NUConfig.GATE_ENABLED.get());
    }

    private static Result ki(MinecraftServer server) {
        ManasSkill skill = SkillAPI.getSkillRegistry().get(NUSkills.KI.getId());
        if (!(skill instanceof KiSkill ki)) return Result.fail("Ki is not registered as a KiSkill: " + skill);
        ManasSkillInstance inst = ki.createDefaultInstance();
        if (ki.getModes(inst) != KiRules.MODES) return Result.fail("Ki should have " + KiRules.MODES + " modes, has " + ki.getModes(inst));
        Set<String> ids = new HashSet<>();
        for (int m = 0; m < KiRules.MODES; m++) {
            String id = ki.getModeId(inst, m);
            if (!ids.add(id)) return Result.fail("two Ki modes share the id " + id);
            if (ki.getModeName(inst, m).getString().isBlank()) return Result.fail("Ki mode " + id + " has no name");
            if (KiRules.cost(m) <= 0 || KiRules.cost(m) > 10000) return Result.fail("Ki mode " + id + " cost out of bounds: " + KiRules.cost(m));
            if (KiRules.cooldown(m) <= 0 || KiRules.cooldown(m) > 6000) return Result.fail("Ki mode " + id + " cooldown out of bounds: " + KiRules.cooldown(m));
            if (KiRules.timed(m) && KiRules.duration(m) > 1200) return Result.fail("Ki mode " + id + " window too long: " + KiRules.duration(m));
        }
        if (KiRules.takenMultiplier(true, true, true) < 0.1 || KiRules.takenMultiplier(false, true, false) > 1.5) return Result.fail("Ki defence multiplier left its [0.1, 1.5] band");
        if (KiRules.spiritDamage(1000) != KiRules.spiritDamage(KiRules.spiritLendMax())) return Result.fail("Spirit Bomb lender cap is not applied");
        return Result.ok(KiRules.MODES + " modes, ids unique, costs and cooldowns in bounds, Spirit Bomb capped at " + KiRules.spiritLendMax() + " lenders; server setting kiEnabled=" + NUConfig.KI_ENABLED.get());
    }
}
