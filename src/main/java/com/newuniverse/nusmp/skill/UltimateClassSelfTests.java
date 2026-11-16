package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.skill.rules.UltimateClass;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: the Ultimate classes (docs/ultimate_classes.md) change the price, mastery and tier of 17 skills and nobody can watch that in a game here, so the CI server smoke test checks them: key
 * {@code ultclass.rules} (the live SERVER numbers are inside their clamps, a cheap pool pays the flat price, the combined damage cap holds) and {@code ultclass.skills} (each classified skill reports
 * its class and the class's mastery, an unclassified one keeps Tensura's, a mastered Ultimate reads ULTIMATE_MASTERED, an old mastered instance stays mastered after the login rescale). Registry
 * reads and a stand-in instance that never belongs to a player: no world, no tick work.
 */
public final class UltimateClassSelfTests {
    private UltimateClassSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("ultclass.rules", s -> rules());
        SelfTests.register("ultclass.skills", UltimateClassSelfTests::skills);
    }

    private static Result rules() {
        for (UltimateClass c : UltimateClass.values()) {
            double cap = NUConfig.ucEpCap(c), pct = NUConfig.ucCostPct(c), ref = NUConfig.ucCostRef(c);
            if (cap < 1 || cap > UltimateClass.EP_CAP_MAX) return Result.fail(c + " epCap out of its clamp: " + cap);
            if (NUConfig.ucMaxMastery(c) < 100) return Result.fail(c + " maxMastery below 100");
            if (UltimateClass.price(1000, 0, pct, ref) != 1000) return Result.fail(c + ": an empty pool does not pay the flat price");
            if (UltimateClass.price(1000, 1.0E9, pct, ref) < 1000) return Result.fail(c + ": a big pool pays less than the flat price");
            double top = UltimateClass.totalScale(1.0E12, cap, 6.0, NUConfig.ucLateShare());
            if (top > cap * NUConfig.ucLateShare() + 1e-9 || top > UltimateClass.EP_CAP_MAX * 6.0) return Result.fail(c + ": the combined damage cap does not hold: x" + top);
        }
        return Result.ok("flatCosts=" + NUConfig.ucFlatCosts() + ", classMastery=" + NUConfig.ucClassMastery() + ", lateGameShare=" + NUConfig.ucLateShare());
    }

    private static Result skills(MinecraftServer server) {
        Map<DeferredHolder<ManasSkill, ? extends ManasSkill>, UltimateClass> want = new LinkedHashMap<>();
        want.put(NUSkills.THE_ALMIGHTY, UltimateClass.GOD);
        want.put(NUSkills.THE_VISIONARY, UltimateClass.GOD);
        want.put(NUSkills.ALL_FICTION, UltimateClass.GOD);
        want.put(NUSkills.GATE_OF_BABYLON, UltimateClass.KING);
        want.put(NUSkills.HELL_CONQUEROR, UltimateClass.KING);
        want.put(NUSkills.LORD_OF_SUMMONS, UltimateClass.KING);
        want.put(NUSkills.ADOLLA_BURST, UltimateClass.LORD);
        want.put(NUSkills.HEROS_ARGONAUT, UltimateClass.LORD);
        want.put(NUSkills.IMMORTAL_WILL, UltimateClass.LORD);
        want.put(NUSkills.NEN, UltimateClass.LORD);
        want.put(NUSkills.KI, UltimateClass.LORD);
        want.put(NUSkills.SHISHOU, UltimateClass.LORD);
        want.put(NUSkills.LORD_OF_GLUTTONOUS_GREED, UltimateClass.LORD);
        for (var e : want.entrySet()) {
            ManasSkill skill = SkillAPI.getSkillRegistry().get(e.getKey().getId());
            if (!(skill instanceof NUSkill s)) return Result.fail(e.getKey().getId() + " is not an NUSkill");
            if (s.ultimateClass() != e.getValue()) return Result.fail(e.getKey().getId() + " is " + s.ultimateClass() + ", not " + e.getValue());
            int expected = NUConfig.ucClassMastery() ? NUConfig.ucMaxMastery(e.getValue()) : s.tensuraMaxMastery();
            if (s.getMaxMastery() != expected) return Result.fail(e.getKey().getId() + " max mastery " + s.getMaxMastery() + ", expected " + expected);
        }
        ManasSkill plain = SkillAPI.getSkillRegistry().get(NUSkills.FIREBOLT.getId());
        if (plain instanceof NUSkill p && (p.ultimateClass() != null || p.getMaxMastery() != p.tensuraMaxMastery())) return Result.fail("Firebolt (a Unique) took a class");
        // an old save: mastered against Tensura's max, no stamp; the login rescale keeps it mastered
        NUSkill god = (NUSkill) SkillAPI.getSkillRegistry().get(NUSkills.THE_VISIONARY.getId());
        ManasSkillInstance inst = god.createDefaultInstance();
        inst.setMastery(god.tensuraMaxMastery());
        NUSkill.restamp(inst);
        if (inst.getMastery() < inst.getMaxMastery()) return Result.fail("an old mastered Visionary lost its mastery: " + inst.getMastery() + " / " + inst.getMaxMastery());
        if (NUSkill.restamp(inst)) return Result.fail("the rescale ran twice");
        if (Tier.of(true, true, true) != Tier.ULTIMATE_MASTERED || !Tier.ULTIMATE_MASTERED.ultimate()) return Result.fail("the mastered Ultimate tier is not live");
        return Result.ok(want.size() + " Ultimates classified, mastery " + NUConfig.ucMaxMastery(UltimateClass.LORD) + " / " + NUConfig.ucMaxMastery(UltimateClass.KING) + " / "
                + NUConfig.ucMaxMastery(UltimateClass.GOD) + ", an old mastered instance stays mastered");
    }
}
