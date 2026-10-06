package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Argonaut (Bell Cranel): HOLD the skill key to charge — a bell chimes each second (max 5).
 * RELEASE to unleash a heroic strike in front of you. Damage scales with charge.
 */
public class ArgonautSkill extends Skill {
    private static final int TICKS_PER_CHARGE = 20;
    protected final boolean ultimate;
    private static final ResourceLocation ICON =
            ResourceLocation.withDefaultNamespace("textures/item/golden_sword.png");

    public ArgonautSkill() { this(SkillType.UNIQUE, false); }

    protected ArgonautSkill(SkillType type, boolean ultimate) {
        super(type);
        this.ultimate = ultimate;
    }

    protected int maxCharge() { return ultimate ? 10 : 5; }

    @Override public ResourceLocation getSkillIcon() { return ICON; }

    @Override
    public int getMaxHeldTime(ManasSkillInstance instance, LivingEntity entity) {
        return maxCharge() * TICKS_PER_CHARGE + 40;
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        instance.getOrCreateTag().putInt("Paid", 0);
        instance.markDirty();
    }

    @Override
    public boolean onHeld(ManasSkillInstance instance, LivingEntity living, int heldTicks, int mode) {
        if (!(living instanceof ServerPlayer player)) return true;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, "Argonaut is recovering."); return false; }
        if (heldTicks <= 0 || heldTicks % TICKS_PER_CHARGE != 0) return true;

        int paid = instance.getOrCreateTag().getInt("Paid");
        if (paid >= maxCharge()) {
            SkillUtil.actionbar(player, Component.literal("Argonaut fully charged - release!").withStyle(ChatFormatting.GOLD));
            return true;
        }
        if (!SkillUtil.spendMagicules(player, NUConfig.ARGONAUT_MAGICULE_PER_CHARGE.get())) return false;
        paid++;
        instance.getOrCreateTag().putInt("Paid", paid);
        instance.markDirty();

        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 0.8F + paid * 0.15F);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 10 * paid, 0.4, 0.6, 0.4, 0.02);
        SkillUtil.actionbar(player, Component.literal("Argonaut charge: " + paid + "/" + maxCharge()).withStyle(ChatFormatting.WHITE));
        return true;
    }

    @Override
    public void onRelease(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int keyNumber, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        int charge = instance.getOrCreateTag().getInt("Paid");
        instance.getOrCreateTag().putInt("Paid", 0);
        instance.markDirty();
        if (charge <= 0) return;

        ServerLevel level = player.serverLevel();
        double damage = NUConfig.ARGONAUT_DAMAGE_PER_CHARGE.get() * charge * (instance.isMastered(player) ? 1.5 : 1.0) * (ultimate ? 2.0 : 1.0);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        int hits = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0),
                e -> e != player && e.isAlive())) {
            Vec3 to = target.getBoundingBox().getCenter().subtract(eye);
            if (to.length() > 6.0 || to.normalize().dot(look) < 0.6) continue;
            target.hurt(player.damageSources().playerAttack(player), (float) damage);
            target.knockback(0.5 + charge * 0.3, -look.x, -look.z);
            hits++;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5F, 0.6F);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, eye.x + look.x * 2, eye.y - 0.3, eye.z + look.z * 2, 3 + charge, 1.0, 0.3, 1.0, 0.0);
        instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(NUConfig.ARGONAUT_COOLDOWN.get()), mode);
        SkillUtil.castVfx(player, 0xFFFFFFFF);
        if (hits > 0) instance.addMasteryPoint(player, charge);
        instance.markDirty();
    }
}
