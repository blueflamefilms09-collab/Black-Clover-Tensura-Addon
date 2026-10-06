package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Base for single-mode divine Unique skills: cost, cooldown, mastery handled here. */
public abstract class GodSkill extends Skill {
    protected GodSkill() { super(SkillType.UNIQUE); }

    protected abstract double cost();
    protected abstract int cooldownTicks();
    protected abstract boolean use(ManasSkillInstance instance, ServerPlayer player, boolean mastered);

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, "This divine power is on cooldown."); return; }
        if (!SkillUtil.spendMagicules(player, DMUtil.cost(cost()))) return;
        if (!use(instance, player, instance.isMastered(player))) return;
        instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(cooldownTicks())), mode);
        SkillUtil.castVfx(player, 0xFFFFD86B);
        instance.addMasteryPoint(player, 1);
        instance.markDirty();
    }
}
