package com.newuniverse.nusmp.skilltree.ft;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.skilltree.ft.kit.FtClock;
import com.newuniverse.nusmp.skilltree.ft.sky.SkyBreathSkill;
import com.newuniverse.nusmp.skilltree.ft.sky.SkyDrillSkill;
import com.newuniverse.nusmp.skilltree.ft.sky.SkyEnchantSkill;
import com.newuniverse.nusmp.skilltree.ft.sky.SkyPassives;
import com.newuniverse.nusmp.skilltree.ft.sky.SkyRoarSkill;
import com.newuniverse.nusmp.skilltree.ft.sky.SkyRoute;
import com.newuniverse.nusmp.skilltree.ft.sky.SkySelfTests;
import com.newuniverse.nusmp.skilltree.ft.sky.SkyTroiaSkill;
import com.newuniverse.nusmp.skilltree.ft.sky.SkyWaveWindSkill;
import com.newuniverse.nusmp.skilltree.ft.sky.SkyWingAttackSkill;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the ManasCore skills of the route Sky Dragon Slayer (nusmp:sky_dragon_slayer). Each Fairy Tail route has its OWN register, owned by one content builder (stfb), so that two builders never edit the
 * same file when they add their abilities; {@code SkillTreeSystem.init} attaches every register to the mod bus once, before the ManasCore skill registry freezes, which is the only moment a skill can be
 * added. A skill is registered under the id the route's JSON names in the node's {@code skill} field ({@code ft_sky_dragons_roar} for {@code nusmp:ft_sky_dragons_roar}); the ids are spelled once in
 * {@link SkyRoute} and the self-test checks that every id of the file is registered here. The register also wires the route's runtime in its static initializer: the first time this class is used
 * (which {@code SkillTreeSystem.init} does when it attaches the register to the bus, on the mod's construction, on both sides) it hooks the tree's clock ({@link FtClock}), the two passives
 * ({@link SkyPassives}) and the self-test ({@link SkySelfTests}). Doing it here and not in the shared startup class means the route needs no integration line from the lead and is one removable
 * slice: delete the route's JSON, this register and the {@code sky} package and nothing else changes. {@link #init} is idempotent for the same reason.
 */
public final class SkyDragonSkills {
    private SkyDragonSkills() {}

    public static final DeferredRegister<ManasSkill> SKILLS = DeferredRegister.create(SkillAPI.getSkillRegistryKey(), NUSMP.MODID);

    public static final DeferredHolder<ManasSkill, SkyRoarSkill> ROAR = SKILLS.register(SkyRoute.SKILL_ROAR, SkyRoarSkill::new);
    public static final DeferredHolder<ManasSkill, SkyWingAttackSkill> WING_ATTACK = SKILLS.register(SkyRoute.SKILL_WING_ATTACK, SkyWingAttackSkill::new);
    public static final DeferredHolder<ManasSkill, SkyWaveWindSkill> WAVE_WIND = SKILLS.register(SkyRoute.SKILL_WAVE_WIND, SkyWaveWindSkill::new);
    public static final DeferredHolder<ManasSkill, SkyBreathSkill> BREATH = SKILLS.register(SkyRoute.SKILL_BREATH, SkyBreathSkill::new);
    public static final DeferredHolder<ManasSkill, SkyTroiaSkill> TROIA = SKILLS.register(SkyRoute.SKILL_TROIA, SkyTroiaSkill::new);
    public static final DeferredHolder<ManasSkill, SkyEnchantSkill> ENCHANT = SKILLS.register(SkyRoute.SKILL_ENCHANT, SkyEnchantSkill::new);
    public static final DeferredHolder<ManasSkill, SkyDrillSkill> SKY_DRILL = SKILLS.register(SkyRoute.SKILL_DRILL, SkyDrillSkill::new);

    private static boolean wired;

    /** Hooks the route's clock, passives and self-test (once). Called by this class's static initializer; calling it again is harmless. */
    public static synchronized void init() {
        if (wired) return;
        wired = true;
        FtClock.init();
        SkyPassives.init();
        SkySelfTests.register();
    }

    static {
        init();
    }
}
