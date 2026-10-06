package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;

/**
 * All Fiction (Kumagawa Misogi): make things never have happened.
 * Undo: your health returns to what it was 5 seconds ago and harmful effects vanish.
 * Fiction: erase a target's existence (bosses/players instead lose all effects and 25% max health).
 * Passive: once per cooldown, your death becomes fiction.
 */
public class AllFictionSkill extends Skill {
    private static final int UNDO = 0, FICTION = 1, DEATH_CD = 2;
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/writable_book.png");
    public AllFictionSkill() { super(SkillType.ULTIMATE); }

    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override public int getModes(ManasSkillInstance i) { return 2; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean r) { return mode == UNDO ? FICTION : UNDO; }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return mode == UNDO ? "fiction.undo" : "fiction.erase"; }
    @Override public Component getModeName(ManasSkillInstance i, int mode) {
        return Component.translatable(mode == UNDO ? "nusmp.skill.mode.fiction.undo" : "nusmp.skill.mode.fiction.erase");
    }
    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return true; }

    /** Remembers your health each second for the last 5 seconds. */
    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity living) {
        if (living.level().isClientSide || living.tickCount % 20 != 0) return;
        CompoundTag tag = instance.getOrCreateTag();
        float[] hist = new float[5];
        for (int i = 0; i < 5; i++) hist[i] = tag.getFloat("H" + i);
        for (int i = 4; i > 0; i--) tag.putFloat("H" + i, hist[i - 1]);
        tag.putFloat("H0", living.getHealth());
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, "Not yet."); return; }
        ServerLevel level = player.serverLevel();

        if (mode == UNDO) {
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(1500))) return;
            float past = instance.getOrCreateTag().getFloat("H4");
            if (past > player.getHealth()) player.setHealth(Math.min(past, player.getMaxHealth()));
            for (MobEffectInstance e : new ArrayList<>(player.getActiveEffects())) {
                if (!e.getEffect().value().isBeneficial()) player.removeEffect(e.getEffect());
            }
            player.clearFire();
            player.displayClientMessage(Component.literal("It's All Fiction.").withStyle(ChatFormatting.DARK_PURPLE), true);
            instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(400)), UNDO);
        } else {
            LivingEntity target = GodTier.lookLiving(player, 24);
            if (target == null) { SkillUtil.fail(player, "No target."); return; }
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(4000))) return;
            level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + 1, target.getZ(), 60, 0.4, 0.8, 0.4, 0.05);
            if (GodTier.fullyAffected(target)) {
                target.discard(); // erased: no body, no drops
            } else {
                target.removeAllEffects();
                GodTier.percentDamage(player, target, 0.25);
            }
            instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(1200)), FICTION);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 0.5F);
        SkillUtil.castVfx(player, 0xFF5A2A7A);
        instance.addMasteryPoint(player);
        instance.markDirty();
    }

    @Override
    public boolean onDeath(ManasSkillInstance instance, LivingEntity owner, DamageSource source) {
        if (!(owner instanceof ServerPlayer player) || owner.level().getGameTime() < instance.getOrCreateTag().getLong("DeathReadyAt")) return true;
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) return true;
        player.setHealth(player.getMaxHealth());
        player.removeAllEffects();
        player.clearFire();
        player.serverLevel().getServer().getPlayerList().broadcastSystemMessage(Component.literal(player.getName().getString()
                + "'s death has been made fiction.").withStyle(ChatFormatting.DARK_PURPLE), false);
        instance.getOrCreateTag().putLong("DeathReadyAt", player.level().getGameTime() + DMUtil.cooldown(18000));
        instance.markDirty();
        return false;
    }
}
