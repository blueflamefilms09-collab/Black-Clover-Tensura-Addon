package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.skill.rules.WillRules;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Immortal Will: the Ultimate evolution of {@link UnbreakableWillSkill} (unlocked automatically by mastering it). Not an alias. It
 * refuses death far more often (the will recovers in three minutes instead of ten), rises at FULL health instead of 30%, stays protected
 * for ten seconds, and answers a refused death with a shockwave that hurts and throws back every foe within eight blocks. It adds a second
 * mode, Rally: spend magicules to heal 40% of your health and cleanse every harmful effect at any time (a 90 second cooldown).
 * Numbers: {@link WillRules}.
 */
public class ImmortalWillSkill extends UnbreakableWillSkill {
    static final SkillText IMMORTAL_TEXT = SkillText.of("immortal_will", "Immortal Will",
            "Ultimate. Evolved from Unbreakable Will. Rise from a killing blow at full health, far more often, with a shockwave; Rally heals and cleanses you on demand.");

    public ImmortalWillSkill() { super(SkillType.ULTIMATE, IMMORTAL_TEXT, true); }

    @Override public int getModes(ManasSkillInstance instance) { return 2; }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        return mode == WillRules.READY ? WillRules.RALLY : WillRules.READY;
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) { return mode == WillRules.RALLY ? "will.rally" : "will.ready"; }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        return mode == WillRules.RALLY ? SkillMsg.mode("will.rally", "Rally") : SkillMsg.mode("will.ready", "Resolve");
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        if (mode != WillRules.RALLY) { status(instance, player); return; }
        if (instance.onCoolDown(WillRules.RALLY)) {
            SkillUtil.fail(player, SkillMsg.t("will.rally_cooldown", "You cannot rally again yet."));
            return;
        }
        if (!SkillUtil.spendMagicules(player, DMUtil.cost(WillRules.rallyCost()))) return;
        SkillUtil.cleanse(player);
        BalanceLaw.heal(player, (float) (player.getMaxHealth() * WillRules.rallyShare()));
        SkillFx.spray(player.serverLevel(), false, player.position().add(0, 1, 0), 12, 0.5, 0.9, 0.5, 0.15);
        SkillFx.sound(player.serverLevel(), player.position(), SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.2F);
        SkillUtil.actionbar(player, SkillMsg.t("will.rally", "You rally.").withStyle(ChatFormatting.GOLD));
        cooldown(instance, WillRules.RALLY, DMUtil.cooldown(WillRules.rallyCooldown()));
        SkillUtil.castVfx(player, 0xFFFFD060);
        grow(instance, player, 1);
    }

    /** The shockwave that answers a refused death. */
    @Override
    protected void refused(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        double radius = WillRules.shockRadius(tier);
        Vec3 at = player.position();
        for (LivingEntity foe : SkillTargets.foes(player, at, radius)) {
            if (SkillDamage.hit(player, foe, WillRules.shockDamage(), masteryFrac(instance), SkillDamage.Kind.MAGIC) > 0f) {
                Vec3 away = foe.position().subtract(at);
                if (away.lengthSqr() > 1.0e-4) {
                    Vec3 dir = away.normalize();
                    foe.setDeltaMovement(dir.x * WillRules.shockKnockback(), 0.5, dir.z * WillRules.shockKnockback());
                    foe.hurtMarked = true;
                }
            }
        }
        SkillFx.shape(player.serverLevel(), VfxShape.FIRE_BURST, at, at.add(0, 1, 0), 0xFFFFD060, 24, (float) (radius * 0.4));
        SkillFx.ring(player.serverLevel(), at, Math.min(radius, 8.0), 0xFFD060);
    }
}
