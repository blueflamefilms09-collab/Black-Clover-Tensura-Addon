package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.EntityEvents;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Vector Manipulation (Accelerator): TOGGLE reflection.
 * Projectiles are sent straight back, and part of every melee hit is reflected onto the attacker.
 */
public class VectorManipulationSkill extends ToggleAuraSkill {
    protected final boolean ultimate;
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/prismarine_shard.png");

    public VectorManipulationSkill() { this(SkillType.UNIQUE, false); }
    protected VectorManipulationSkill(SkillType type, boolean ultimate) { super(type); this.ultimate = ultimate; }

    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override protected int interval() { return 20; }
    @Override protected double drainPerPulse() { return ultimate ? 80 : 50; }

    @Override
    protected void pulse(ManasSkillInstance instance, ServerPlayer player, boolean mastered) {
        if (ultimate) player.fallDistance = 0; // vector control over your own fall
    }

    @Override
    public void onProjectileHit(ManasSkillInstance instance, LivingEntity living, EntityHitResult hitResult, Projectile projectile,
                                Changeable<ProjectileDeflection> deflection, Changeable<EntityEvents.ProjectileHitResult> result) {
        if (!instance.isToggled()) return;
        deflection.set(ProjectileDeflection.REVERSE);
        result.set(EntityEvents.ProjectileHitResult.PASS);
    }

    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (!instance.isToggled() || source.is(DamageTypes.THORNS)) return true;
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == owner || source.getDirectEntity() != attacker) return true;
        float share = ultimate ? 1.0F : (instance.isMastered(owner) ? 0.75F : 0.5F);
        attacker.hurt(owner.damageSources().thorns(owner), amount.get() * share);
        amount.set(amount.get() * (1.0F - share));
        if (owner.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, attacker.getX(), attacker.getY() + 1, attacker.getZ(), 15, 0.3, 0.5, 0.3, 0.1);
        }
        return true;
    }
}
