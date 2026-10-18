package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.AuraRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Spirit Card (Allen): TOGGLE healing aura. Every two seconds you and every player within 8 blocks (12 when mastered), plus your own
 * pets and summons, who is hurt is healed a little (through the balance law's healing cap); a mastered card also washes harmful effects
 * away. Costs 80 magicules per pulse. Numbers: {@link AuraRules}.
 */
public class SpiritCardSkill extends ToggleAuraSkill {
    static final SkillText TEXT = SkillText.of("spirit_card", "Spirit Card",
            "Toggle. A healing aura for you and everyone near you; once mastered it also cleanses harmful effects.");

    public SpiritCardSkill() { super(SkillType.EXTRA, TEXT, false); }

    @Override protected int interval() { return AuraRules.spiritInterval(); }
    @Override protected double drainPerPulse(Tier tier) { return AuraRules.SPIRIT_DRAIN; }

    @Override
    protected void pulse(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        for (LivingEntity ally : SkillTargets.alliesAround(player, player.position(), AuraRules.spiritRadius(tier))) {
            if (AuraRules.spiritCleanses(tier)) SkillUtil.cleanse(ally);
            if (ally.getHealth() >= ally.getMaxHealth()) continue;
            BalanceLaw.heal(ally, (float) AuraRules.spiritHeal(tier));
            SkillFx.spray(player.serverLevel(), false, ally.position().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.0);
        }
    }
}
