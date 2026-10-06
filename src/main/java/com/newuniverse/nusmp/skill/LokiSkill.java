package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/** Loki, the Trickster: vanish and blink forward. */
public class LokiSkill extends GodSkill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/ender_pearl.png");
    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override protected double cost() { return 800; }
    @Override protected int cooldownTicks() { return 400; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, boolean mastered) {
        ServerLevel level = player.serverLevel();
        Vec3 from = player.position();
        Vec3 look = player.getViewVector(1.0F);
        double max = mastered ? 16 : 10;
        Vec3 dest = null;
        for (double d = max; d >= 2; d -= 0.5) {
            Vec3 p = from.add(look.x * d, Math.max(0, look.y * d), look.z * d);
            if (level.noCollision(player, player.getBoundingBox().move(p.subtract(from)))) { dest = p; break; }
        }
        if (dest == null) { SkillUtil.fail(player, "No room to blink."); return false; }
        level.sendParticles(ParticleTypes.LARGE_SMOKE, from.x, from.y + 1, from.z, 30, 0.3, 0.6, 0.3, 0.02);
        player.teleportTo(dest.x, dest.y, dest.z);
        player.fallDistance = 0;
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, mastered ? 200 : 100, 0));
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.3F);
        return true;
    }
}
