package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.AuraRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Hell Walker (Hell Mode): TOGGLE. While on, the closer you are to death the harder you hit: your melee damage is multiplied by
 * 1 + (share of health you are missing) x 1.1, so at 10% health you deal about double (x1.6 steepness when mastered). Costs 60 magicules
 * a second and switches itself off when they run out. Hell Conqueror (the Ultimate) extends it with a much steeper curve, life steal and
 * a burning aura. Numbers: {@link AuraRules}.
 */
public class HellWalkerSkill extends ToggleAuraSkill {
    static final SkillText TEXT = SkillText.of("hell_walker", "Hell Walker",
            "Unique. Toggle. The closer you are to death, the harder you hit: at ten percent health your blows deal about double.");

    public HellWalkerSkill() { this(SkillType.UNIQUE, TEXT, false); }

    protected HellWalkerSkill(SkillType type, SkillText text, boolean ultimate) { super(type, text, ultimate); }

    @Override protected int interval() { return AuraRules.HELL_INTERVAL; }
    @Override protected double drainPerPulse(Tier tier) { return AuraRules.HELL_DRAIN; }

    @Override
    protected void pulse(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        if (player.getHealth() < player.getMaxHealth() * 0.5F) {
            SkillFx.spray(player.serverLevel(), true, player.position().add(0, 0.2, 0), 6, 0.3, 0.1, 0.3, 0.01);
        }
    }

    @Override
    protected void turnedOn(ManasSkillInstance instance, ServerPlayer player) {
        SkillFx.sound(player.serverLevel(), player.position(), SoundEvents.WITHER_AMBIENT, 0.6F, 0.6F);
        SkillFx.spray(player.serverLevel(), true, player.position().add(0, 1, 0), 12, 0.4, 0.6, 0.4, 0.02);
    }

    @Override
    public boolean onDamageEntity(ManasSkillInstance instance, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (!instance.isToggled() || owner.getMaxHealth() <= 0 || amount.get() <= 0) return true;
        Tier tier = tier(instance, owner);
        float dealt = (float) (amount.get() * AuraRules.hellMultiplier(owner.getHealth(), owner.getMaxHealth(), tier));
        amount.set(dealt);
        double steal = AuraRules.hellLifesteal(owner.getHealth(), owner.getMaxHealth(), tier);
        if (steal > 0) BalanceLaw.heal(owner, (float) (dealt * steal));
        return true;
    }
}
