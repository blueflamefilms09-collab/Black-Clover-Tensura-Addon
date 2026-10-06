package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Instant Death (Yogiri Takatou): Ultimate.
 * Die: whatever you look at dies. Mass Death: every hostile creature around you dies.
 * Bosses (and players, unless allowed in config) instead lose a large share of their max health.
 */
public class InstantDeathSkill extends Skill {
    private static final int DIE = 0, MASS = 1;
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/wither_skeleton_skull.png");
    public InstantDeathSkill() { super(SkillType.ULTIMATE); }

    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override public int getModes(ManasSkillInstance i) { return 2; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean r) { return mode == DIE ? MASS : DIE; }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return mode == DIE ? "death.die" : "death.mass"; }
    @Override public Component getModeName(ManasSkillInstance i, int mode) {
        return Component.translatable(mode == DIE ? "nusmp.skill.mode.death.die" : "nusmp.skill.mode.death.mass");
    }

    private static void strike(ServerPlayer player, LivingEntity t) {
        if (GodTier.fullyAffected(t)) GodTier.kill(player, t);
        else GodTier.percentDamage(player, t, NUConfig.INSTANT_DEATH_BOSS_SHARE.get());
        player.serverLevel().sendParticles(ParticleTypes.SOUL, t.getX(), t.getY() + 1, t.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, "Not yet."); return; }
        ServerLevel level = player.serverLevel();
        boolean mastered = instance.isMastered(player);

        if (mode == DIE) {
            LivingEntity t = GodTier.lookLiving(player, 48);
            if (t == null) { SkillUtil.fail(player, "Nothing there to die."); return; }
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(3000))) return;
            strike(player, t);
            instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(mastered ? 200 : 400)), DIE);
        } else {
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(8000))) return;
            for (LivingEntity t : DMUtil.around(player, player.position(), mastered ? 24 : 16)) {
                if (t instanceof Enemy) strike(player, t);
            }
            instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(3600)), MASS);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.0F, 0.5F);
        SkillUtil.castVfx(player, 0xFF6A0010);
        instance.addMasteryPoint(player);
        instance.markDirty();
    }
}
