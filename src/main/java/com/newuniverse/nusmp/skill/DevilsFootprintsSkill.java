package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.FireForceRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Devil's Footprints (Shinra Kusakabe, Fire Force): flames from your feet. Modes: Ignition Dash (rocket through the air; no fall damage
 * from it) and Devil's Kick (a burning kick on everything in front of you, thrown back and set alight). Passive: your own flames cannot
 * burn you (all fire damage is cancelled). Adolla Burst (the Ultimate) extends this class. Numbers: {@link FireForceRules}.
 */
public class DevilsFootprintsSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("devils_footprints", "Devil's Footprints",
            "Unique. Flames burst from your feet: dash through the air or deliver a burning kick. Your own fire cannot hurt you.");

    public DevilsFootprintsSkill() { this(SkillType.UNIQUE, TEXT, false); }

    protected DevilsFootprintsSkill(SkillType type, SkillText text, boolean ultimate) { super(type, text, ultimate); }

    @Override public int getModes(ManasSkillInstance i) { return 2; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean r) { return mode == FireForceRules.DASH ? FireForceRules.KICK : FireForceRules.DASH; }

    @Override
    public String getModeId(ManasSkillInstance i, int mode) { return mode == FireForceRules.KICK ? "fireforce.kick" : "fireforce.dash"; }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        return mode == FireForceRules.KICK ? SkillMsg.mode("fireforce.kick", "Devil's Kick") : SkillMsg.mode("fireforce.dash", "Ignition Dash");
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("fireforce.cooldown", "Your flames need a moment.")); return; }
        Tier tier = tier(instance, player);

        if (mode == FireForceRules.DASH) {
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(FireForceRules.dashCost(tier)))) return;
            dash(player, tier);
            cooldown(instance, mode, DMUtil.cooldown(FireForceRules.dashCooldown(tier)));
        } else if (mode == FireForceRules.KICK) {
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(FireForceRules.kickCost(tier)))) return;
            kick(instance, player, tier);
            cooldown(instance, mode, DMUtil.cooldown(FireForceRules.kickCooldown(tier)));
        } else {
            return;
        }
        SkillUtil.castVfx(player, 0xFFFF5A1E);
        grow(instance, player, 1);
    }

    private void dash(ServerPlayer player, Tier tier) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getViewVector(1.0F);
        double power = FireForceRules.dashPower(tier);
        player.setDeltaMovement(look.x * power, Math.max(0.35, look.y * power), look.z * power);
        player.hurtMarked = true;
        player.fallDistance = 0;
        Vec3 from = player.position().add(0, 0.2, 0);
        SkillFx.shape(level, VfxShape.FLAME_TRAIL, from, from.add(look.scale(-6.0)), 0xFFFF5A1E, 16, 1.0f);
        SkillFx.spray(level, false, from, 10, 0.2, 0.1, 0.2, 0.08);
        SkillFx.sound(level, player.position(), SoundEvents.FIRECHARGE_USE, 1.0F, 1.4F);
    }

    /** Shared with Adolla Burst: the kick. */
    protected final void kick(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        double damage = FireForceRules.kickDamage(tier);
        for (LivingEntity t : SkillTargets.foesInCone(player, FireForceRules.kickRange(tier), 0.5)) {
            if (SkillDamage.hit(player, t, damage, masteryFrac(instance)) > 0f) {
                t.igniteForSeconds(FireForceRules.kickBurnSeconds(tier));
                t.knockback(1.5, -look.x, -look.z);
            }
        }
        Vec3 tip = eye.add(look.scale(3.0)).add(0, -0.6, 0);
        SkillFx.shape(level, VfxShape.FIRE_BURST, tip, tip.add(0, 1, 0), 0xFFFF5A1E, 18, 1.2f);
        SkillFx.spray(level, false, tip, 10, 1.0, 0.3, 1.0, 0.05);
        SkillFx.sound(level, player.position(), SoundEvents.BLAZE_SHOOT, 1.5F, 0.7F);
    }

    /** Shinra's own flames do not burn him. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (FireForceRules.fireImmune(true) && source.is(DamageTypeTags.IS_FIRE)) amount.set(0.0F);
        return true;
    }
}
