package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: Hestia, Freya, Hephaestus and Loki are single-press divine Unique skills that all follow one script: refuse if on cooldown, pay
 * the magicules, do the power, start the cooldown, show the cast circle, earn mastery. The script lives here once (with the pack-wide
 * cost and cooldown multipliers); a power that cannot be used (nothing to repair, no room to blink) returns false and costs nothing.
 */
public abstract class GodSkill extends NUSkill {
    protected GodSkill(SkillText text) { super(SkillType.UNIQUE, text, false); }

    protected abstract double cost();
    protected abstract int cooldownTicks();
    /** The divine power. Return false if it could not be used (the cost is refunded and no cooldown starts). */
    protected abstract boolean use(ManasSkillInstance instance, ServerPlayer player, Tier tier);
    /** ARGB colour of the cast circle. */
    protected int color() { return 0xFFFFD86B; }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("god.cooldown", "This divine power is on cooldown.")); return; }
        double price = DMUtil.cost(cost());
        if (!SkillUtil.spendMagicules(player, price)) return;
        if (!use(instance, player, tier(instance, player))) {
            SkillUtil.refund(player, price);
            return;
        }
        cooldown(instance, mode, DMUtil.cooldown(cooldownTicks()));
        SkillUtil.castVfx(player, color());
        grow(instance, player, 1);
    }
}
