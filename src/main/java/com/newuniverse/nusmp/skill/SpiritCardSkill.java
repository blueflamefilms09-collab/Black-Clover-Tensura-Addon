package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Spirit Card (Allen): TOGGLE healing aura for you and every player near you. */
public class SpiritCardSkill extends ToggleAuraSkill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/ghast_tear.png");
    public SpiritCardSkill() { super(SkillType.EXTRA); }

    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override protected int interval() { return 40; }
    @Override protected double drainPerPulse() { return 80; }

    @Override
    protected void pulse(ManasSkillInstance instance, ServerPlayer player, boolean mastered) {
        double range = mastered ? 12 : 8;
        for (ServerPlayer ally : player.serverLevel().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(range))) {
            if (ally.getHealth() >= ally.getMaxHealth()) continue;
            ally.heal(mastered ? 4.0F : 2.0F);
            player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, ally.getX(), ally.getY() + 1, ally.getZ(), 6, 0.3, 0.5, 0.3, 0.0);
        }
    }
}
