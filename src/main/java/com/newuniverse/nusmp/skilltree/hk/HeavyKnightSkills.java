package com.newuniverse.nusmp.skilltree.hk;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.skilltree.ft.kit.FtClock;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the ManasCore skills of the three Heavy Knight routes (Oath of the Heavy Armor, Basic Swordsmanship, Lady Luck). A skill can only be added before the ManasCore skill registry freezes, so this
 * register is attached to the mod bus by {@code SkillTreeSystem.init} like the Fairy Tail registers; each skill is registered under the id its route file names in the node's {@code skill} field
 * ({@code hk_provoke} for {@code nusmp:hk_provoke}), spelled once in {@link HkRoute}. The static initializer wires the route family's runtime (the shared tree clock, the passives and the self-test) the first time
 * the class is used, so the family needs no integration line beyond the register: delete the three JSON files, this register and the {@code hk} package and nothing else changes.
 */
public final class HeavyKnightSkills {
    private HeavyKnightSkills() {}

    public static final DeferredRegister<ManasSkill> SKILLS = DeferredRegister.create(SkillAPI.getSkillRegistryKey(), NUSMP.MODID);

    public static final DeferredHolder<ManasSkill, HkTechniques.Provoke> PROVOKE = SKILLS.register(HkRoute.SKILL_PROVOKE, HkTechniques.Provoke::new);
    public static final DeferredHolder<ManasSkill, HkTechniques.ShieldWall> SHIELD_WALL = SKILLS.register(HkRoute.SKILL_SHIELD_WALL, HkTechniques.ShieldWall::new);
    public static final DeferredHolder<ManasSkill, HkTechniques.ShieldBash> SHIELD_BASH = SKILLS.register(HkRoute.SKILL_SHIELD_BASH, HkTechniques.ShieldBash::new);
    public static final DeferredHolder<ManasSkill, HkTechniques.PowerStrike> POWER_STRIKE = SKILLS.register(HkRoute.SKILL_POWER_STRIKE, HkTechniques.PowerStrike::new);
    public static final DeferredHolder<ManasSkill, HkTechniques.SwiftStep> SWIFT_STEP = SKILLS.register(HkRoute.SKILL_SWIFT_STEP, HkTechniques.SwiftStep::new);
    public static final DeferredHolder<ManasSkill, HkTechniques.CrescentCut> CRESCENT_CUT = SKILLS.register(HkRoute.SKILL_CRESCENT_CUT, HkTechniques.CrescentCut::new);
    public static final DeferredHolder<ManasSkill, HkTechniques.GamblersCoin> GAMBLERS_COIN = SKILLS.register(HkRoute.SKILL_GAMBLERS_COIN, HkTechniques.GamblersCoin::new);
    public static final DeferredHolder<ManasSkill, HkTechniques.Jackpot> JACKPOT = SKILLS.register(HkRoute.SKILL_JACKPOT, HkTechniques.Jackpot::new);

    private static boolean wired;

    /** Hooks the shared clock, the passives and the self-test (once). */
    public static synchronized void init() {
        if (wired) return;
        wired = true;
        FtClock.init();
        HkPassives.init();
        HkSelfTests.register();
    }

    static {
        init();
    }
}
