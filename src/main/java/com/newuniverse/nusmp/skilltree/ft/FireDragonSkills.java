package com.newuniverse.nusmp.skilltree.ft;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonTimers;
import com.newuniverse.nusmp.skilltree.ft.fire.CrimsonLotusSkill;
import com.newuniverse.nusmp.skilltree.ft.fire.FireDragonPassives;
import com.newuniverse.nusmp.skilltree.ft.fire.FireDragonSelfTests;
import com.newuniverse.nusmp.skilltree.ft.fire.FireDragonsBrilliantFlameSkill;
import com.newuniverse.nusmp.skilltree.ft.fire.FireDragonsClawSkill;
import com.newuniverse.nusmp.skilltree.ft.fire.FireDragonsIronFistSkill;
import com.newuniverse.nusmp.skilltree.ft.fire.FireDragonsRoarSkill;
import com.newuniverse.nusmp.skilltree.ft.fire.FireDragonsSwordHornSkill;
import com.newuniverse.nusmp.skilltree.ft.fire.FireDragonsWingAttackSkill;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the ManasCore skills of the route Fire Dragon Slayer (nusmp:fire_dragon_slayer). Each Fairy Tail route has its OWN register, owned by one content builder (stfa), so that two
 * builders never edit the same file when they add their abilities; {@code SkillTreeSystem.init} attaches every register to the mod bus once, before the ManasCore skill registry
 * freezes. A skill is added as {@code SKILLS.register("ft_fire_dragons_roar", FireDragonsRoarSkill::new)}: the id starts with {@code ft_} and is the id the route's JSON names in the
 * node's {@code skill} field (a unit test and the in-game self-test check that). The register is also the one place that exists at mod construction for this route, so the
 * route's non-skill parts are started from its static initializer, which runs exactly once, on the first touch of {@code SKILLS} by {@code SkillTreeSystem.init}: the server-tick
 * timer the delayed techniques use, the game-bus listeners of the two passives (Eating Flame, Blazing Scales) and the self-test key {@code tree.ft.fire_dragon_slayer}. That keeps
 * the lead's {@code SkillTreeSystem} unchanged (no extra line to forget); {@link #init} is public and idempotent in case the lead prefers to call it by hand.
 */
public final class FireDragonSkills {
    private FireDragonSkills() {}

    public static final DeferredRegister<ManasSkill> SKILLS = DeferredRegister.create(SkillAPI.getSkillRegistryKey(), NUSMP.MODID);

    public static final DeferredHolder<ManasSkill, FireDragonsIronFistSkill> IRON_FIST = SKILLS.register("ft_fire_dragons_iron_fist", FireDragonsIronFistSkill::new);
    public static final DeferredHolder<ManasSkill, FireDragonsRoarSkill> ROAR = SKILLS.register("ft_fire_dragons_roar", FireDragonsRoarSkill::new);
    public static final DeferredHolder<ManasSkill, FireDragonsClawSkill> CLAW = SKILLS.register("ft_fire_dragons_claw", FireDragonsClawSkill::new);
    public static final DeferredHolder<ManasSkill, FireDragonsBrilliantFlameSkill> BRILLIANT_FLAME = SKILLS.register("ft_fire_dragons_brilliant_flame", FireDragonsBrilliantFlameSkill::new);
    public static final DeferredHolder<ManasSkill, FireDragonsWingAttackSkill> WING_ATTACK = SKILLS.register("ft_fire_dragons_wing_attack", FireDragonsWingAttackSkill::new);
    public static final DeferredHolder<ManasSkill, FireDragonsSwordHornSkill> SWORD_HORN = SKILLS.register("ft_fire_dragons_sword_horn", FireDragonsSwordHornSkill::new);
    public static final DeferredHolder<ManasSkill, CrimsonLotusSkill> CRIMSON_LOTUS = SKILLS.register("ft_crimson_lotus_exploding_flame_blade", CrimsonLotusSkill::new);

    static { init(); }

    /** Starts the timer, the passives and the self-test of the Fire route. Safe to call any number of times. */
    public static void init() {
        DragonTimers.init();
        FireDragonPassives.init();
        FireDragonSelfTests.register();
    }
}
