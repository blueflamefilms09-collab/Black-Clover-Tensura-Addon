package com.newuniverse.nusmp.skilltree.hk;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.skilltree.kit.KitClock;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the technique classes of the four Heavy Knight books, registered as ManasCore skills (Oath of the Heavy Armor, Basic Swordsmanship, Lady Luck, Smoldering Fang of Insanity). A skill can only be added before the ManasCore skill registry freezes, so this
 * register is attached to the mod bus by {@code SkillBookSystem.init}; each skill is registered under the id its route file names in the node's {@code skill} field
 * ({@code hk_provoke} for {@code nusmp:hk_provoke}), spelled once in {@link HkRoute}. The static initializer wires the route family's runtime (the shared effect clock and the passives) the first time
 * the class is used, so the family needs no integration line beyond the register. Since 1.96.0 these skills are never learned: the four books' unique skills run them as modes.
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

    public static final DeferredHolder<ManasSkill, HkTechniques.EmberBite> EMBER_BITE = SKILLS.register(HkRoute.SKILL_EMBER_BITE, HkTechniques.EmberBite::new);
    public static final DeferredHolder<ManasSkill, HkTechniques.Insanity> INSANITY = SKILLS.register(HkRoute.SKILL_INSANITY, HkTechniques.Insanity::new);
    public static final DeferredHolder<ManasSkill, HkTechniques.HowlOfRuin> HOWL_OF_RUIN = SKILLS.register(HkRoute.SKILL_HOWL_OF_RUIN, HkTechniques.HowlOfRuin::new);
    public static final DeferredHolder<ManasSkill, HkTechniques.FangOfInsanity> FANG_OF_INSANITY = SKILLS.register(HkRoute.SKILL_FANG_OF_INSANITY, HkTechniques.FangOfInsanity::new);

    private static boolean wired;

    /** Hooks the shared clock, the passives and the self-test (once). */
    public static synchronized void init() {
        if (wired) return;
        wired = true;
        KitClock.init();
        HkPassives.init();
    }

    static {
        init();
    }
}
