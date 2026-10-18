package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.skill.rules.VectorRules;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.Vec3;

/**
 * Black Wings: the Ultimate evolution of {@link VectorManipulationSkill} (unlocked automatically by mastering it). Not an alias. It
 * reflects ALL of a melee hit (you take none of it) and sends projectiles back one and a half times as fast and as YOUR projectiles; you
 * take no fall damage at all; and it grows wings: while switched on, holding sneak in mid-air makes you glide (you sink only slowly and
 * drift forward) with dark wings of Rukh behind you. Costs 80 magicules a second. Numbers: {@link VectorRules}.
 */
public class BlackWingsSkill extends VectorManipulationSkill {
    static final SkillText WINGS_TEXT = SkillText.of("black_wings", "Black Wings",
            "Ultimate. Toggle. Total reflection of melee and projectiles, no fall damage, and wings that let you glide in the air while you sneak.");

    public BlackWingsSkill() { super(SkillType.ULTIMATE, WINGS_TEXT, true); }

    @Override
    protected ProjectileDeflection deflection(ManasSkillInstance instance, LivingEntity living, Tier tier) {
        double boost = VectorRules.projectileBoost(tier);
        return (projectile, hit, random) -> {
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-boost));
            projectile.setYRot(projectile.getYRot() + 180.0F);
            projectile.yRotO += 180.0F;
            projectile.setOwner(living);               // it comes back as the wearer's own
            projectile.hasImpulse = true;
        };
    }

    /** The glide: runs every tick while the wings are on. */
    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity living) {
        super.onTick(instance, living);                                       // the drain and pulses (once a second)
        if (!(living instanceof ServerPlayer player) || !instance.isToggled() || !player.isAlive()) return;
        if (!VectorRules.canGlide(Tier.ULTIMATE, player.isShiftKeyDown(), player.onGround(), player.isInWater())) return;
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x * VectorRules.glideForward(), Math.max(v.y, VectorRules.glideFall()), v.z * VectorRules.glideForward());
        player.hurtMarked = true;
        player.fallDistance = 0;
        if (player.tickCount % 6 == 0) {
            SkillFx.spray(player.serverLevel(), true, player.position().add(0, 1.2, 0), 4, 0.9, 0.4, 0.3, 0.01);
        }
    }
}
