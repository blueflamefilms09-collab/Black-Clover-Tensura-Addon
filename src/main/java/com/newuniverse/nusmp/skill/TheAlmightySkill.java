package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.GodTierRules;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Almighty (Yhwach): a standalone god-tier Ultimate. You see every future. Passive: a share of attacks (almightyDodgeChance, one and
 * a half times as much when mastered, never above 95%) simply never happen. Press: Rewrite the Future: for a few seconds (5, or 8 when
 * mastered) no harm at all can reach you, and your harmful effects vanish. The void and /kill still work. Numbers: {@link GodTierRules}.
 */
public class TheAlmightySkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("the_almighty", "The Almighty",
            "Ultimate. See every future and choose the one where you are never harmed.");

    public TheAlmightySkill() { super(SkillType.ULTIMATE, TEXT, true); }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("almighty.cooldown", "That future is not yet visible.")); return; }
        if (!SkillUtil.spendMagicules(player, DMUtil.cost(GodTierRules.almightyCost()))) return;
        ServerLevel level = player.serverLevel();
        int ticks = GodTierRules.rewriteTicks(instance.isMastered(player));
        instance.getOrCreateTag().putLong("FutureUntil", level.getGameTime() + ticks);
        SkillUtil.cleanse(player);
        SkillUtil.actionbar(player, SkillMsg.t("almighty.rewritten", "The future has been rewritten.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        SkillFx.follow(level, com.newuniverse.nusmp.vfx.VfxShape.NIHILITY_ZONE, player, player.position(), 0xFF8A0A0A, ticks, 1.5f);
        SkillFx.sound(level, player.position(), SoundEvents.WITHER_SPAWN, 0.6F, 1.6F);
        cooldown(instance, mode, DMUtil.cooldown(GodTierRules.almightyCooldown()));
        SkillUtil.castVfx(player, 0xFF8A0A0A);
        grow(instance, player, 1);
    }

    /** That future never happens. */
    @Override
    public boolean onBeingDamaged(ManasSkillInstance instance, LivingEntity owner, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        boolean rewriting = owner.level().getGameTime() < instance.getOrCreateTag().getLong("FutureUntil");
        double chance = GodTierRules.dodgeChance(NUConfig.ALMIGHTY_DODGE_CHANCE.get(), instance.isMastered(owner));
        if (!rewriting && owner.getRandom().nextDouble() >= chance) return true;
        if (owner.level() instanceof ServerLevel level) {
            SkillFx.spray(level, true, owner.position().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.01);
        }
        return false;
    }
}
