package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.AuraRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Hati (Bete Loga): PASSIVE. Fire, lightning and magic that hit you are partly absorbed (only 60% lands, 40% when mastered) and the
 * absorbed part is turned into Strength: every absorbed hit raises your Strength by one level (up to II, or IV when mastered) for ten
 * seconds (fifteen when mastered), refreshed by each hit. A mastered Hati also heals a quarter of what it absorbed. Numbers: {@link AuraRules}.
 */
public class HatiSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("hati", "Hati",
            "Unique. Passive. Fire, lightning and magic hitting you are partly absorbed and turned into stacking Strength.");

    public HatiSkill() { super(SkillType.UNIQUE, TEXT, false); }

    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        boolean magical = source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_LIGHTNING)
                || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC);
        if (!magical || amount.get() <= 0) return true;

        Tier tier = tier(instance, owner);
        double incoming = amount.get();
        double absorbed = AuraRules.hatiAbsorbed(incoming, tier);
        amount.set((float) (incoming - absorbed));

        MobEffectInstance current = owner.getEffect(MobEffects.DAMAGE_BOOST);
        int next = AuraRules.hatiNextAmp(current == null ? -1 : current.getAmplifier(), tier);
        owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, AuraRules.hatiTicks(tier), next));
        double heal = AuraRules.hatiHeal(absorbed, tier);
        if (heal > 0) BalanceLaw.heal(owner, (float) heal);
        if (owner.level() instanceof ServerLevel level) {
            SkillFx.spray(level, false, owner.position().add(0, 1, 0), 8, 0.4, 0.6, 0.4, 0.02);
        }
        if (owner.getRandom().nextInt(4) == 0) instance.addMasteryPoint(owner);
        instance.markDirty();
        return true;
    }
}
