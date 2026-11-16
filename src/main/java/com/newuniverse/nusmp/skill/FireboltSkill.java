package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.ArgonautMergeRules;
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
 * Magic as the wiki has it: no chant, the name is enough, quick, strong at short and medium range (docs/argonaut_rework.md). Numbers: {@link FireboltRules} and the config (fireboltDamage, fireboltMagiculeCost, fireboltCooldown).
 */
public class FireboltSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("firebolt", "Firebolt",
            "Instant magic, no chant: say its name and it fires. A fast bolt of fire that strikes what you look at and scorches what stands near it; it is at its best at short and medium range and weakens at long range. Mastery doubles the damage. Argonaut now holds this as a mode; this skill stays for old saves.");

    public FireboltSkill() { super(SkillType.EXTRA, TEXT, false); }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        cast(this, instance, player, mode, 0.0);
    }

    /**
     * The bolt itself, shared with Argonaut's Firebolt mode (docs/argonaut_rework.md): {@code owner} is the skill that holds {@code instance}
     * (its tier, price, cooldown and mastery are used). {@code masteryPerHit} 0 = the old rule (FireboltRules.masteryGain), else Argonaut's own.
     */
    static void cast(NUSkill owner, ManasSkillInstance instance, ServerPlayer player, int mode, double masteryPerHit) {
        if (!player.isAlive()) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("firebolt.cooldown", "Firebolt is on cooldown.")); return; }
        if (!pay(instance, player, mode, NUConfig.FIREBOLT_MAGICULE_COST.get())) return;

        boolean hitSomething = fire(owner, instance, player, 0);
        cooldown(instance, mode, NUConfig.FIREBOLT_COOLDOWN.get());
        SkillUtil.castVfx(player, 0xFFFF7A2A);
        grow(instance, player, masteryPerHit > 0 ? ArgonautMergeRules.fireboltMastery(hitSomething) : FireboltRules.masteryGain(hitSomething));
    }

    /**
     * The shot only (aim, damage, splash, ignite, effects); the caller has checked the cooldown and paid. {@code charge} is the number of Argonaut bells riding on the bolt
     * (0 = the plain instant Firebolt; the wiki's "Argonaut charged Firebolt" grows its damage and blast, {@link FireboltRules#chargedFactor}). Returns whether it hit something.
     */
    static boolean fire(NUSkill owner, ManasSkillInstance instance, ServerPlayer player, int charge) {
        ServerLevel level = player.serverLevel();
        Tier tier = owner.tier(instance, player);
        double range = FireboltRules.range(tier);
        Vec3 start = player.getEyePosition();
        Vec3 wall = DMUtil.lookPoint(player, range);                       // where the bolt would stop on a block
        AABB box = player.getBoundingBox().expandTowards(wall.subtract(start)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, start, wall, box,
                e -> e instanceof LivingEntity le && SkillTargets.isAimable(player, le));
        Vec3 impact = hit != null ? hit.getLocation() : wall;
        LivingEntity target = hit != null ? (LivingEntity) hit.getEntity() : null;

        Vec3 hand = start.add(player.getViewVector(1.0F).scale(0.8)).add(0, -0.25, 0);
        float width = 1.0f + 0.12f * Math.max(0, charge);
        SkillFx.beam(level, hand, impact, false);
        SkillFx.shape(level, VfxShape.LIGHTNING_SPEAR, hand, impact, 0xFFFF8A2A, 8, width);
        SkillAnimeFx.firebolt(level, hand, impact, 0xFFFF8A2A, width);                    // VFX wave 3: the instant fire-lightning bolt
        SkillFx.sound(level, player.position(), SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8F, 1.6F);

        boolean hitSomething = false;
        double mastery = masteryFrac(instance);
        double dmg = FireboltRules.damage(NUConfig.FIREBOLT_DAMAGE.get(), tier) * FireboltRules.chargedFactor(charge);
        if (charge <= 0 && ArgonautMerge.canon()) dmg *= ArgonautMergeRules.rangeFalloff(start.distanceTo(impact), range);   // wiki: long range is Firebolt's weak point (a charged bolt flies on)
        if (target != null) {
            hitSomething = SkillDamage.hit(player, target, dmg, mastery, SkillDamage.Kind.MAGIC, instance) > 0f;
            target.igniteForSeconds(FireboltRules.igniteSeconds(tier));
            strikeVisual(level, target.position());
            for (LivingEntity other : SkillTargets.foes(player, target.position(), FireboltRules.splashRadius(tier) + FireboltRules.chargedSplashBonus(charge))) {
                if (other == target) continue;
                if (SkillDamage.hit(player, other, dmg * FireboltRules.splashShare(), mastery, SkillDamage.Kind.MAGIC, instance) > 0f) {
                    other.igniteForSeconds(FireboltRules.igniteSeconds(tier) / 2);
                    hitSomething = true;
                }
            }
        }
        SkillFx.burst(level, impact, false, 10, 0.4);
        return hitSomething;
    }

    /** A lightning strike that is only to look at: no damage, no fire, no block change. */
    static void strikeVisual(ServerLevel level, Vec3 at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.moveTo(at.x, at.y, at.z);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }
}
