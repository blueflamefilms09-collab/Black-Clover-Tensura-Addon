package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * DanMachi-style magic: you must CHANT (press several times) before you can CAST.
 * Mastering the magic shortens the chant by one line (Short Chant).
 * The chant breaks if you wait more than 10 seconds between lines.
 */
public abstract class ChantedMagicSkill extends Skill {
    protected static final int CHANT = 0, CAST = 1;
    private static final long CHANT_TIMEOUT = 200;

    protected ChantedMagicSkill() { super(SkillType.EXTRA); }

    /** Chant lines needed before casting. */
    protected abstract int chantLines();
    /** Total magicule cost, split across the chant lines. */
    protected abstract double totalCost();
    /** Cooldown after casting, in ticks. */
    protected abstract int castCooldown();
    /** Does the actual spell. Return true if it was cast. */
    protected abstract boolean cast(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, boolean mastered);

    @Override public int getModes(ManasSkillInstance instance) { return 2; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) { return mode == CHANT ? CAST : CHANT; }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return mode == CHANT ? "magic.chant" : "magic.cast"; }
    @Override public Component getModeName(ManasSkillInstance i, int mode) {
        return Component.translatable(mode == CHANT ? "nusmp.skill.mode.magic.chant" : "nusmp.skill.mode.magic.cast");
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        if (instance.onCoolDown(CAST)) { SkillUtil.fail(player, "This magic is on cooldown."); return; }

        boolean mastered = instance.isMastered(player);
        int needed = Math.max(1, chantLines() - (mastered ? 1 : 0));
        CompoundTag tag = instance.getOrCreateTag();
        long now = level.getGameTime();
        int done = now - tag.getLong("ChantAt") > CHANT_TIMEOUT ? 0 : tag.getInt("Chant");

        if (mode == CHANT) {
            if (done >= needed) { SkillUtil.fail(player, "Chant complete. Switch to Cast!"); return; }
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(totalCost()) / needed)) return;
            done++;
            tag.putInt("Chant", done);
            tag.putLong("ChantAt", now);
            instance.markDirty();
            level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.7F + done * 0.15F);
            level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.2, player.getZ(), 30, 0.6, 0.8, 0.6, 0.5);
            SkillUtil.actionbar(player, Component.literal("Chanting... (" + done + "/" + needed + ")").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }

        if (done < needed) { SkillUtil.fail(player, "Finish the chant first (" + done + "/" + needed + ")."); return; }
        if (!cast(instance, player, level, mastered)) return;
        tag.putInt("Chant", 0);
        instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(castCooldown())), CAST);
        SkillUtil.castVfx(player, 0xFFB070FF);
        instance.addMasteryPoint(player, 1);
        instance.markDirty();
    }
}
