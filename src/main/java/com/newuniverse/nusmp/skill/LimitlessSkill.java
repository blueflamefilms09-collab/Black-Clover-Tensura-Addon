package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
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
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

/**
 * Limitless (Satoru Gojo): Ultimate.
 * Infinity: toggle a barrier that stops every attack from reaching you (drains magicules).
 * Blue: pull everything toward a point. Red: blast everything away from you.
 * Hollow Purple: an erasing beam.
 */
public class LimitlessSkill extends Skill {
    private static final int INFINITY = 0, BLUE = 1, RED = 2, PURPLE = 3, MODES = 4;
    private static final String[] KEYS = {"infinity", "blue", "red", "purple"};
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/amethyst_shard.png");

    public LimitlessSkill() { super(SkillType.ULTIMATE); }

    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override public int getModes(ManasSkillInstance i) { return MODES; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean r) { return r ? (mode + MODES - 1) % MODES : (mode + 1) % MODES; }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return "limitless." + KEYS[mode]; }
    @Override public Component getModeName(ManasSkillInstance i, int mode) { return Component.translatable("nusmp.skill.mode.limitless." + KEYS[mode]); }
    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return true; }

    private static boolean infinityOn(ManasSkillInstance i) { return i.getOrCreateTag().getBoolean("Infinity"); }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity living) {
        if (!(living instanceof ServerPlayer player) || !infinityOn(instance) || player.tickCount % 20 != 0) return;
        if (!SkillUtil.spendMagicules(player, DMUtil.cost(instance.isMastered(player) ? 60 : 120))) {
            instance.getOrCreateTag().putBoolean("Infinity", false);
            player.displayClientMessage(Component.literal("Infinity collapsed.").withStyle(ChatFormatting.GRAY), true);
        }
        instance.markDirty();
    }

    @Override
    public boolean onBeingDamaged(ManasSkillInstance instance, LivingEntity entity, DamageSource source, float amount) {
        if (!infinityOn(instance) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        if (source.getEntity() == null && source.getDirectEntity() == null) return true; // only attacks from something
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(new DustParticleOptions(new Vector3f(0.6F, 0.8F, 1.0F), 1.0F),
                    entity.getX(), entity.getY() + 1, entity.getZ(), 12, 0.5, 0.7, 0.5, 0.0);
        }
        return false; // the attack never reaches you
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        boolean mastered = instance.isMastered(player);

        if (mode == INFINITY) {
            boolean on = !infinityOn(instance);
            instance.getOrCreateTag().putBoolean("Infinity", on);
            instance.markDirty();
            player.displayClientMessage(Component.literal(on ? "Infinity: ON" : "Infinity: OFF")
                    .withStyle(on ? ChatFormatting.AQUA : ChatFormatting.GRAY), true);
            return;
        }
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, "Cursed technique on cooldown."); return; }

        switch (mode) {
            case BLUE -> {
                if (!SkillUtil.spendMagicules(player, DMUtil.cost(1500))) return;
                Vec3 c = DMUtil.lookPoint(player, 32);
                for (LivingEntity t : DMUtil.around(player, c, mastered ? 12 : 9)) {
                    Vec3 pull = c.subtract(t.position()).normalize().scale(1.5);
                    t.setDeltaMovement(pull.x, pull.y + 0.2, pull.z);
                    t.hurtMarked = true;
                    t.hurt(player.damageSources().playerAttack(player), DMUtil.dmg(mastered ? 16 : 10));
                }
                level.sendParticles(new DustParticleOptions(new Vector3f(0.1F, 0.3F, 1.0F), 2.0F), c.x, c.y + 1, c.z, 200, 1.5, 1.5, 1.5, 0.0);
                instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(200)), BLUE);
            }
            case RED -> {
                if (!SkillUtil.spendMagicules(player, DMUtil.cost(1800))) return;
                for (LivingEntity t : DMUtil.around(player, player.position(), mastered ? 10 : 7)) {
                    Vec3 push = t.position().subtract(player.position()).normalize().scale(3.0);
                    t.setDeltaMovement(push.x, 0.8, push.z);
                    t.hurtMarked = true;
                    t.hurt(player.damageSources().playerAttack(player), DMUtil.dmg(mastered ? 24 : 16));
                }
                level.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 2.0F), player.getX(), player.getY() + 1, player.getZ(), 200, 2.5, 1.0, 2.5, 0.0);
                level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 1.4F);
                instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(240)), RED);
            }
            default -> {
                if (!SkillUtil.spendMagicules(player, DMUtil.cost(6000))) return;
                Vec3 start = player.getEyePosition();
                Vec3 dir = player.getViewVector(1.0F);
                Set<LivingEntity> hit = new HashSet<>();
                for (double d = 0; d < 64; d += 1.0) {
                    Vec3 p = start.add(dir.scale(d));
                    level.sendParticles(new DustParticleOptions(new Vector3f(0.6F, 0.1F, 0.9F), 3.0F), p.x, p.y, p.z, 4, 0.6, 0.6, 0.6, 0.0);
                    hit.addAll(DMUtil.around(player, p, 2.5));
                }
                for (LivingEntity t : hit) {
                    if (GodTier.fullyAffected(t)) GodTier.kill(player, t);
                    else t.hurt(player.damageSources().playerAttack(player), DMUtil.dmg(mastered ? 80 : 50));
                }
                level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0F, 0.6F);
                instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(1800)), PURPLE);
            }
        }
        SkillUtil.castVfx(player, 0xFF7FB2FF);
        instance.addMasteryPoint(player);
        instance.markDirty();
    }
}
