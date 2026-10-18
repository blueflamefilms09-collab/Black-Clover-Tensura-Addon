package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.FireboltRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Firebolt (Bell Cranel): chantless, instant lightning-fire magic. Press and a bolt leaves your hand at once and strikes what you look at:
 * full damage to the target, half to everything near the impact, the target set alight and a visual-only lightning strike on it (no
 * block damage, no fire spread). Mastery doubles the damage and widens the splash. Only a shot that hit something teaches mastery.
 * Numbers: {@link FireboltRules} and the config (fireboltDamage, fireboltMagiculeCost, fireboltCooldown).
 */
public class FireboltSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("firebolt", "Firebolt",
            "Instant magic. A bolt of lightning-fire fired the moment you press, striking what you look at and scorching everything near it. Mastery doubles the damage.");

    public FireboltSkill() { super(SkillType.EXTRA, TEXT, false); }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("firebolt.cooldown", "Firebolt is on cooldown.")); return; }
        if (!SkillUtil.spendMagicules(player, NUConfig.FIREBOLT_MAGICULE_COST.get())) return;

        ServerLevel level = player.serverLevel();
        Tier tier = tier(instance, player);
        double range = FireboltRules.range(tier);
        Vec3 start = player.getEyePosition();
        Vec3 wall = DMUtil.lookPoint(player, range);                       // where the bolt would stop on a block
        AABB box = player.getBoundingBox().expandTowards(wall.subtract(start)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, start, wall, box,
                e -> e instanceof LivingEntity le && SkillTargets.isAimable(player, le));
        Vec3 impact = hit != null ? hit.getLocation() : wall;
        LivingEntity target = hit != null ? (LivingEntity) hit.getEntity() : null;

        Vec3 hand = start.add(player.getViewVector(1.0F).scale(0.8)).add(0, -0.25, 0);
        SkillFx.beam(level, hand, impact, false);
        SkillFx.shape(level, VfxShape.LIGHTNING_SPEAR, hand, impact, 0xFFFF8A2A, 8, 1.0f);
        SkillFx.sound(level, player.position(), SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8F, 1.6F);

        boolean hitSomething = false;
        double mastery = masteryFrac(instance);
        double dmg = FireboltRules.damage(NUConfig.FIREBOLT_DAMAGE.get(), tier);
        if (target != null) {
            hitSomething = SkillDamage.hit(player, target, dmg, mastery, SkillDamage.Kind.MAGIC) > 0f;
            target.igniteForSeconds(FireboltRules.igniteSeconds(tier));
            strikeVisual(level, target.position());
            for (LivingEntity other : SkillTargets.foes(player, target.position(), FireboltRules.splashRadius(tier))) {
                if (other == target) continue;
                if (SkillDamage.hit(player, other, dmg * FireboltRules.splashShare(), mastery, SkillDamage.Kind.MAGIC) > 0f) {
                    other.igniteForSeconds(FireboltRules.igniteSeconds(tier) / 2);
                    hitSomething = true;
                }
            }
        }
        SkillFx.burst(level, impact, false, 10, 0.4);

        cooldown(instance, mode, NUConfig.FIREBOLT_COOLDOWN.get());
        SkillUtil.castVfx(player, 0xFFFF7A2A);
        grow(instance, player, FireboltRules.masteryGain(hitSomething));
    }

    /** A lightning strike that is only to look at: no damage, no fire, no block change. */
    private static void strikeVisual(ServerLevel level, Vec3 at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.moveTo(at.x, at.y, at.z);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }
}
