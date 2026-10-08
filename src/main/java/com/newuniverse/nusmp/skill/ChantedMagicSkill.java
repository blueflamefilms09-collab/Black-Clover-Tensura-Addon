package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Release-to-cast spells from the DanMachi crossover. Mastery still affects each spell's own cast.
 */
public abstract class ChantedMagicSkill extends Skill {
    protected static final int CAST = 0;

    protected ChantedMagicSkill() { super(SkillType.EXTRA); }

    /** Legacy spell metadata retained for existing concrete magic skills. */
    protected abstract int chantLines();
    /** Total magicule cost for a single cast. */
    protected abstract double totalCost();
    /** Cooldown after casting, in ticks. */
    protected abstract int castCooldown();
    /** Does the actual spell. Return true if it was cast. */
    protected abstract boolean cast(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, boolean mastered);

    @Override public int getModes(ManasSkillInstance instance) { return 1; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) { return CAST; }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return "magic.cast"; }
    @Override public Component getModeName(ManasSkillInstance i, int mode) { return Component.translatable("nusmp.skill.mode.magic.cast"); }

    @Override
    public void onRelease(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        if (instance.onCoolDown(CAST)) { SkillUtil.fail(player, "This magic is on cooldown."); return; }

        boolean mastered = instance.isMastered(player);
        if (!SkillUtil.spendMagicules(player, DMUtil.cost(totalCost()))) return;
        if (!cast(instance, player, level, mastered)) return;
        instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(castCooldown())), CAST);
        SkillUtil.castVfx(player, 0xFFB070FF);
        instance.addMasteryPoint(player, 1);
        instance.markDirty();
    }
}
