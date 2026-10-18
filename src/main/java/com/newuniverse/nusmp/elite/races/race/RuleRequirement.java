package com.newuniverse.nusmp.elite.races.race;

import com.newuniverse.nusmp.elite.races.rule.RaceCtx;
import com.newuniverse.nusmp.elite.races.rule.RaceEvolutionRule;
import com.newuniverse.nusmp.elite.races.runtime.PlayerRaceCtx;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.tensura.race.template.EvolutionRequirement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: Tensura's evolution screen shows each requirement of a race with its own progress, and evolves the player when the whole bar is full. A {@link RaceEvolutionRule} is
 * wrapped as a Tensura {@code EvolutionRequirement} so the screen lists the elite races' rules exactly as it lists Tensura's own (EP, items, bosses), in green once met. The
 * progress is 0..1 (the reading the other add-ons' single-requirement races use: weight 100 times progress 1).
 */
final class RuleRequirement extends EvolutionRequirement {
    private final RaceEvolutionRule rule;

    RuleRequirement(RaceEvolutionRule rule) { this.rule = rule; }

    @Override
    public float getProgress(ManasRaceInstance instance, LivingEntity entity) {
        return (float) Math.max(0.0, Math.min(1.0, rule.progress(PlayerRaceCtx.of(entity))));
    }

    @Override
    public Component getRequirementComponent(ManasRaceInstance instance, LivingEntity entity) {
        RaceCtx ctx = PlayerRaceCtx.of(entity);
        int pct = (int) Math.round(Math.max(0.0, Math.min(1.0, rule.progress(ctx))) * 100);
        return Component.literal(rule.describe() + " (" + pct + "%)").withStyle(rule.met(ctx) ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }
}
