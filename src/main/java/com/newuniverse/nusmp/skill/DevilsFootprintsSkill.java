package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Devil's Footprints (Shinra Kusakabe, Fire Force): flames from your feet.
 * Modes: Ignition Dash, Devil's Kick. Passive: your own flames can't burn you.
 */
public class DevilsFootprintsSkill extends Skill {
    protected static final int DASH = 0, KICK = 1;
    protected final boolean ultimate;
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/fire_charge.png");

    public DevilsFootprintsSkill() { this(SkillType.UNIQUE, false); }
    protected DevilsFootprintsSkill(SkillType type, boolean ultimate) { super(type); this.ultimate = ultimate; }

    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override public int getModes(ManasSkillInstance i) { return 2; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean r) { return mode == DASH ? KICK : DASH; }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return mode == DASH ? "fireforce.dash" : "fireforce.kick"; }
    @Override public Component getModeName(ManasSkillInstance i, int mode) {
        return Component.translatable(mode == DASH
                ? (ultimate ? "nusmp.skill.mode.fireforce.light_speed" : "nusmp.skill.mode.fireforce.dash")
                : "nusmp.skill.mode.fireforce.kick");
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, "Your flames need a moment."); return; }
        ServerLevel level = player.serverLevel();
        boolean mastered = instance.isMastered(player);

        if (mode == DASH) {
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(ultimate ? 600 : 150))) return;
            if (ultimate) lightSpeed(player, level, mastered); else dash(player, level, mastered);
            instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(ultimate ? 60 : 30)), DASH);
        } else {
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(ultimate ? 800 : 300))) return;
            kick(player, level, mastered);
            instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(ultimate ? 60 : 100)), KICK);
        }
        SkillUtil.castVfx(player, 0xFFFF5A1E);
        instance.addMasteryPoint(player);
        instance.markDirty();
    }

    private void dash(ServerPlayer player, ServerLevel level, boolean mastered) {
        Vec3 look = player.getViewVector(1.0F);
        double power = mastered ? 2.4 : 1.7;
        player.setDeltaMovement(look.x * power, Math.max(0.35, look.y * power), look.z * power);
        player.hurtMarked = true;
        player.fallDistance = 0;
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY(), player.getZ(), 40, 0.2, 0.1, 0.2, 0.08);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.4F);
    }

    /** Adolla Burst: move at the speed of light, striking everything you pass. */
    private void lightSpeed(ServerPlayer player, ServerLevel level, boolean mastered) {
        Vec3 from = player.position();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 dest = null;
        for (double d = mastered ? 40 : 28; d >= 2; d -= 0.5) {
            Vec3 p = from.add(look.scale(d));
            if (level.noCollision(player, player.getBoundingBox().move(p.subtract(from)))) { dest = p; break; }
        }
        if (dest == null) { SkillUtil.fail(player, "No room to move."); return; }
        for (double d = 0; d < from.distanceTo(dest); d += 1.0) {
            Vec3 p = from.add(look.scale(d));
            for (LivingEntity t : DMUtil.around(player, p.add(0, 1, 0), 1.8)) {
                t.hurt(player.damageSources().playerAttack(player), DMUtil.dmg(mastered ? 30 : 20));
                t.igniteForSeconds(5);
            }
        }
        GodTier.line(level, ParticleTypes.END_ROD, from.add(0, 1, 0), dest.add(0, 1, 0));
        player.teleportTo(dest.x, dest.y, dest.z);
        player.fallDistance = 0;
        level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.5F, 0.6F);
    }

    private void kick(ServerPlayer player, ServerLevel level, boolean mastered) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        double range = ultimate ? 8 : 5;
        for (LivingEntity t : DMUtil.around(player, player.position(), range)) {
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            if (to.normalize().dot(look) < 0.5) continue;
            t.hurt(player.damageSources().playerAttack(player), DMUtil.dmg((ultimate ? 30 : 14) * (mastered ? 1.5 : 1.0)));
            t.igniteForSeconds(6);
            t.knockback(1.5, -look.x, -look.z);
        }
        level.sendParticles(ParticleTypes.FLAME, eye.x + look.x * 2, eye.y - 0.5, eye.z + look.z * 2, 80, 1.2, 0.4, 1.2, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5F, 0.7F);
    }

    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) amount.set(0.0F); // Shinra's flames don't burn him
        return true;
    }
}
