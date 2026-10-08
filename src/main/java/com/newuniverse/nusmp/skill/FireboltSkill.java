package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Firebolt (Bell Cranel): chantless, instant lightning-fire magic. */
public class FireboltSkill extends Skill {
    private static final double RANGE = 32.0;
    private static final ResourceLocation ICON =
            ResourceLocation.withDefaultNamespace("textures/item/blaze_powder.png");

    public FireboltSkill() { super(SkillType.EXTRA); }

    @Override public ResourceLocation getSkillIcon() { return ICON; }

    @Override
    public void onRelease(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, "Firebolt is on cooldown."); return; }
        if (!SkillUtil.spendMagicules(player, NUConfig.FIREBOLT_MAGICULE_COST.get())) return;

        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(RANGE));

        HitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();

        AABB box = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, start, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != player);
        Vec3 impact = entityHit != null ? entityHit.getLocation() : end;

        // trail
        Vec3 step = impact.subtract(start).normalize().scale(0.5);
        Vec3 p = start;
        for (int i = 0; i < impact.distanceTo(start) * 2; i++) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
            level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
            p = p.add(step);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8F, 1.6F);

        if (entityHit != null) {
            Entity target = entityHit.getEntity();
            float dmg = (float) (NUConfig.FIREBOLT_DAMAGE.get() * (instance.isMastered(player) ? 2.0 : 1.0));
            target.hurt(player.damageSources().playerAttack(player), dmg);
            target.igniteForSeconds(4.0F);
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(target.getX(), target.getY(), target.getZ());
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
        }

        instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(NUConfig.FIREBOLT_COOLDOWN.get()), mode);
        SkillUtil.castVfx(player, 0xFFFF7A2A);
        instance.addMasteryPoint(player, 1);
        instance.markDirty();
    }
}
