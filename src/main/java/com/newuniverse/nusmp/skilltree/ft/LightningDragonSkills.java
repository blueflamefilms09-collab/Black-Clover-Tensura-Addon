package com.newuniverse.nusmp.skilltree.ft;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonTimers;
import com.newuniverse.nusmp.skilltree.ft.lightning.LightningDragonPassives;
import com.newuniverse.nusmp.skilltree.ft.lightning.LightningDragonSelfTests;
import com.newuniverse.nusmp.skilltree.ft.lightning.LightningDragonsBreakdownFistSkill;
import com.newuniverse.nusmp.skilltree.ft.lightning.LightningDragonsFlashSkill;
import com.newuniverse.nusmp.skilltree.ft.lightning.LightningDragonsHalberdSkill;
import com.newuniverse.nusmp.skilltree.ft.lightning.LightningDragonsIronFistSkill;
import com.newuniverse.nusmp.skilltree.ft.lightning.LightningDragonsJawSkill;
import com.newuniverse.nusmp.skilltree.ft.lightning.LightningDragonsRoarSkill;
import com.newuniverse.nusmp.skilltree.ft.lightning.RoaringThunderSkill;
import com.newuniverse.nusmp.skilltree.ft.lightning.ThunderPalaceSkill;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the ManasCore skills of the route Lightning Dragon Slayer (nusmp:lightning_dragon_slayer). Each Fairy Tail route has its OWN register, owned by one content builder (stfa), so
 * that two builders never edit the same file when they add their abilities; {@code SkillTreeSystem.init} attaches every register to the mod bus once, before the ManasCore skill
 * registry freezes. A skill is added as {@code SKILLS.register("ft_lightning_dragons_roar", LightningDragonsRoarSkill::new)}: the id starts with {@code ft_} and is the id the route's
 * JSON names in the node's {@code skill} field (a unit test and the in-game self-test check that). The register is also the one place that exists at mod construction for this
 * route, so the route's non-skill parts are started from its static initializer, which runs exactly once, on the first touch of {@code SKILLS} by {@code SkillTreeSystem.init}: the
 * server-tick timer the delayed techniques use, the game-bus listeners of the two passives (Lightning Body, Eating Lightning) and the self-test key
 * {@code tree.ft.lightning_dragon_slayer}. That keeps the lead's {@code SkillTreeSystem} unchanged (no extra line to forget); {@link #init} is public and idempotent in case the
 * lead prefers to call it by hand.
 */
public final class LightningDragonSkills {
    private LightningDragonSkills() {}

    public static final DeferredRegister<ManasSkill> SKILLS = DeferredRegister.create(SkillAPI.getSkillRegistryKey(), NUSMP.MODID);

    public static final DeferredHolder<ManasSkill, LightningDragonsIronFistSkill> IRON_FIST = SKILLS.register("ft_lightning_dragons_iron_fist", LightningDragonsIronFistSkill::new);
    public static final DeferredHolder<ManasSkill, LightningDragonsRoarSkill> ROAR = SKILLS.register("ft_lightning_dragons_roar", LightningDragonsRoarSkill::new);
    public static final DeferredHolder<ManasSkill, LightningDragonsBreakdownFistSkill> BREAKDOWN_FIST = SKILLS.register("ft_lightning_dragons_breakdown_fist", LightningDragonsBreakdownFistSkill::new);
    public static final DeferredHolder<ManasSkill, LightningDragonsHalberdSkill> HEAVENWARD_HALBERD = SKILLS.register("ft_lightning_dragons_heavenward_halberd", LightningDragonsHalberdSkill::new);
    public static final DeferredHolder<ManasSkill, ThunderPalaceSkill> THUNDER_PALACE = SKILLS.register("ft_thunder_palace", ThunderPalaceSkill::new);
    public static final DeferredHolder<ManasSkill, LightningDragonsJawSkill> JAW = SKILLS.register("ft_lightning_dragons_jaw", LightningDragonsJawSkill::new);
    public static final DeferredHolder<ManasSkill, LightningDragonsFlashSkill> FLASH = SKILLS.register("ft_lightning_dragons_flash", LightningDragonsFlashSkill::new);
    public static final DeferredHolder<ManasSkill, RoaringThunderSkill> ROARING_THUNDER = SKILLS.register("ft_roaring_thunder", RoaringThunderSkill::new);

    static { init(); }

    /** Starts the timer, the passives and the self-test of the Lightning route. Safe to call any number of times. */
    public static void init() {
        DragonTimers.init();
        LightningDragonPassives.init();
        LightningDragonSelfTests.register();
    }
}
