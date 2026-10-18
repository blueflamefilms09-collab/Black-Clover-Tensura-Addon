package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.skill.rules.VectorRules;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.EntityEvents;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Vector Manipulation (Accelerator): TOGGLE reflection. While on, projectiles that would hit you are sent back, and half of every melee
 * hit (three quarters when mastered) is turned back on the attacker (through the balance law's PvP cap, never as a chain: a reflected hit
 * cannot be reflected again). Costs 50 magicules a second. Black Wings (the Ultimate) extends this class. Numbers: {@link VectorRules}.
 */
public class VectorManipulationSkill extends ToggleAuraSkill {
    static final SkillText TEXT = SkillText.of("vector_manipulation", "Vector Manipulation",
            "Unique. Toggle. Reflect projectiles and turn part of every melee hit back on the attacker.");

    public VectorManipulationSkill() { this(SkillType.UNIQUE, TEXT, false); }

    protected VectorManipulationSkill(SkillType type, SkillText text, boolean ultimate) { super(type, text, ultimate); }

    @Override protected int interval() { return VectorRules.interval(); }
    @Override protected double drainPerPulse(Tier tier) { return VectorRules.drain(tier); }

    @Override
    protected void pulse(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        if (VectorRules.fallImmune(tier)) player.fallDistance = 0; // vector control over your own fall
    }

    /** How a projectile that reaches the wearer is turned back; the Ultimate sends it back faster, and as the wearer's own. */
    protected ProjectileDeflection deflection(ManasSkillInstance instance, LivingEntity living, Tier tier) {
        return ProjectileDeflection.REVERSE;
    }

    @Override
    public void onProjectileHit(ManasSkillInstance instance, LivingEntity living, EntityHitResult hitResult, Projectile projectile,
                                Changeable<ProjectileDeflection> deflection, Changeable<EntityEvents.ProjectileHitResult> result) {
        if (!instance.isToggled()) return;
        deflection.set(deflection(instance, living, tier(instance, living)));
        result.set(EntityEvents.ProjectileHitResult.PASS);
    }

    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (!instance.isToggled() || amount.get() <= 0) return true;
        Tier tier = tier(instance, owner);
        if (source.is(DamageTypes.FALL) && VectorRules.fallImmune(tier)) { amount.set(0.0F); return true; }   // wings: no fall damage
        if (source.is(DamageTypes.THORNS)) return true;                                                      // a reflected hit is not reflected again
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == owner || source.getDirectEntity() != attacker) return true;

        double hit = amount.get();
        float back = BalanceLaw.cap(attacker, (float) VectorRules.reflected(hit, tier));
        attacker.hurt(owner.damageSources().thorns(owner), back);
        amount.set((float) VectorRules.taken(hit, tier));
        if (owner.level() instanceof ServerLevel level) {
            SkillFx.spray(level, false, attacker.position().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.1);
        }
        return true;
    }
}
