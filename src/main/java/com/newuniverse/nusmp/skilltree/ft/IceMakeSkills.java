package com.newuniverse.nusmp.skilltree.ft;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.skilltree.ft.ice.IceCannonSkill;
import com.newuniverse.nusmp.skilltree.ft.ice.IceEagleSkill;
import com.newuniverse.nusmp.skilltree.ft.ice.IceExcaliburSkill;
import com.newuniverse.nusmp.skilltree.ft.ice.IceFloorSkill;
import com.newuniverse.nusmp.skilltree.ft.ice.IceHammerSkill;
import com.newuniverse.nusmp.skilltree.ft.ice.IceLanceSkill;
import com.newuniverse.nusmp.skilltree.ft.ice.IcePassives;
import com.newuniverse.nusmp.skilltree.ft.ice.IceRoute;
import com.newuniverse.nusmp.skilltree.ft.ice.IceSaucerSkill;
import com.newuniverse.nusmp.skilltree.ft.ice.IceSelfTests;
import com.newuniverse.nusmp.skilltree.ft.ice.IceShieldSkill;
import com.newuniverse.nusmp.skilltree.ft.kit.FtClock;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the ManasCore skills of the route Ice-Make, Maker Magic (nusmp:ice_make). Each Fairy Tail route has its OWN register, owned by one content builder (stfb), so that two builders never edit the
 * same file when they add their abilities; {@code SkillTreeSystem.init} attaches every register to the mod bus once, before the ManasCore skill registry freezes, which is the only moment a skill can be
 * added. A skill is registered under the id the route's JSON names in the node's {@code skill} field ({@code ft_ice_make_lance} for {@code nusmp:ft_ice_make_lance}); the ids are spelled once in
 * {@link IceRoute} and the self-test checks that every id of the file is registered here. The register also wires the route's runtime in its static initializer: the first time this class is used
 * (which {@code SkillTreeSystem.init} does when it attaches the register to the bus, on the mod's construction, on both sides) it hooks the tree's clock ({@link FtClock}), the passive
 * ({@link IcePassives}) and the self-test ({@link IceSelfTests}). Doing it here and not in the shared startup class means the route needs no integration line from the lead and is one removable
 * slice: delete the route's JSON, this register and the {@code ice} package and nothing else changes. {@link #init} is idempotent for the same reason.
 */
public final class IceMakeSkills {
    private IceMakeSkills() {}

    public static final DeferredRegister<ManasSkill> SKILLS = DeferredRegister.create(SkillAPI.getSkillRegistryKey(), NUSMP.MODID);

    public static final DeferredHolder<ManasSkill, IceLanceSkill> LANCE = SKILLS.register(IceRoute.SKILL_LANCE, IceLanceSkill::new);
    public static final DeferredHolder<ManasSkill, IceFloorSkill> FLOOR = SKILLS.register(IceRoute.SKILL_FLOOR, IceFloorSkill::new);
    public static final DeferredHolder<ManasSkill, IceShieldSkill> SHIELD = SKILLS.register(IceRoute.SKILL_SHIELD, IceShieldSkill::new);
    public static final DeferredHolder<ManasSkill, IceCannonSkill> CANNON = SKILLS.register(IceRoute.SKILL_CANNON, IceCannonSkill::new);
    public static final DeferredHolder<ManasSkill, IceSaucerSkill> SAUCER = SKILLS.register(IceRoute.SKILL_SAUCER, IceSaucerSkill::new);
    public static final DeferredHolder<ManasSkill, IceEagleSkill> EAGLE = SKILLS.register(IceRoute.SKILL_EAGLE, IceEagleSkill::new);
    public static final DeferredHolder<ManasSkill, IceHammerSkill> HAMMER = SKILLS.register(IceRoute.SKILL_HAMMER, IceHammerSkill::new);
    public static final DeferredHolder<ManasSkill, IceExcaliburSkill> EXCALIBUR = SKILLS.register(IceRoute.SKILL_EXCALIBUR, IceExcaliburSkill::new);

    private static boolean wired;

    /** Hooks the route's clock, passive and self-test (once). Called by this class's static initializer; calling it again is harmless. */
    public static synchronized void init() {
        if (wired) return;
        wired = true;
        FtClock.init();
        IcePassives.init();
        IceSelfTests.register();
    }

    static {
        init();
    }
}
