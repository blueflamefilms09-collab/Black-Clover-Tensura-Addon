package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.GodTierRules;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

/**
 * Instant Death (Yogiri Takatou): a standalone god-tier Ultimate. Die: whatever you look at dies. Mass Death: every hostile creature
 * around you dies. Bosses (and players, unless the [god_tier] config allows it) instead lose a large share of their max health
 * (instantDeathBossShare). Only things you may hurt are affected: never allies, pets, summons, or creative and spectating players.
 * Numbers: {@link GodTierRules} and the [god_tier] config.
 */
public class InstantDeathSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("instant_death", "Instant Death",
            "Ultimate. Whatever you will to die, dies. Bosses and protected players lose a share of their health instead.");

    public InstantDeathSkill() { super(SkillType.ULTIMATE, TEXT, true); }

    @Override public int getModes(ManasSkillInstance i) { return 2; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean r) { return mode == GodTierRules.DIE ? GodTierRules.MASS : GodTierRules.DIE; }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return mode == GodTierRules.DIE ? "death.die" : "death.mass"; }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        return mode == GodTierRules.DIE ? SkillMsg.mode("death.die", "Die") : SkillMsg.mode("death.mass", "Mass Death");
    }

    /** One creature dies, or (boss, protected player) loses a share of its health. */
    private static void strike(ServerPlayer player, LivingEntity t) {
        if (GodTierRules.kills(GodTier.fullyAffected(t))) GodTier.kill(player, t);
        else GodTier.percentDamage(player, t, NUConfig.INSTANT_DEATH_BOSS_SHARE.get());
        SkillFx.spray(player.serverLevel(), true, t.position().add(0, t.getBbHeight() * 0.6, 0), 10, 0.3, 0.5, 0.3, 0.02);
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("death.cooldown", "Not yet.")); return; }
        ServerLevel level = player.serverLevel();
        boolean mastered = instance.isMastered(player);

        if (mode == GodTierRules.DIE) {
            LivingEntity t = SkillTargets.lookFoe(player, GodTierRules.dieRange());
            if (t == null) { SkillUtil.fail(player, SkillMsg.t("death.no_target", "Nothing there to die.")); return; }
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(GodTierRules.dieCost()))) return;
            strike(player, t);
            SkillFx.beam(level, player.getEyePosition().add(0, -0.3, 0), t.position().add(0, t.getBbHeight() * 0.5, 0), true);
            cooldown(instance, mode, DMUtil.cooldown(GodTierRules.dieCooldown(mastered)));
        } else if (mode == GodTierRules.MASS) {
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(GodTierRules.massCost()))) return;
            double radius = GodTierRules.massRadius(mastered);
            for (LivingEntity t : SkillTargets.foes(player, player.position(), radius)) {
                if (t instanceof Enemy) strike(player, t);
            }
            Vec3 at = player.position();
            SkillFx.shape(level, VfxShape.NIHILITY_ZONE, at, at.add(0, 1, 0), 0xFF6A0010, 40, (float) Math.min(radius, 24.0));
            cooldown(instance, mode, DMUtil.cooldown(GodTierRules.massCooldown()));
        } else {
            return;
        }
        SkillFx.sound(level, player.position(), SoundEvents.WARDEN_HEARTBEAT, 2.0F, 0.5F);
        SkillUtil.castVfx(player, 0xFF6A0010);
        grow(instance, player, 1);
    }
}
