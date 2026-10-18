package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.GodRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Freya, Goddess of Beauty: Charm. Every monster around you drops its aggro on you and keeps forgetting you for a few seconds (the lock is
 * re-applied every half second by a zone, so a monster that re-targets you is turned away again), and every foe in range is dazed: slowed
 * and weakened (players also reel). The hard part of the daze goes through the balance law's control caps (short on players and bosses,
 * halved when repeated). Only foes are affected: allies, pets and summons are left alone. Numbers: {@link GodRules.Freya}.
 */
public class FreyaSkill extends GodSkill {
    static final SkillText TEXT = SkillText.of("freya", "Freya",
            "Unique. Goddess of Beauty. Your Charm makes nearby monsters forget you and leaves your enemies dazed.");

    public FreyaSkill() { super(TEXT); }

    @Override protected double cost() { return GodRules.Freya.COST; }
    @Override protected int cooldownTicks() { return GodRules.Freya.COOLDOWN; }
    @Override protected int color() { return 0xFFFFA6D2; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        ServerLevel level = player.serverLevel();
        List<LivingEntity> foes = SkillTargets.foes(player, player.position(), GodRules.Freya.radius(tier));
        List<UUID> charmed = new ArrayList<>();
        for (LivingEntity t : foes) {
            int daze = GodRules.Freya.dazeTicks(tier);
            if (t instanceof Mob mob) {
                forget(mob, player);
                charmed.add(mob.getUUID());
            }
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, daze), GodRules.Freya.slowAmp()));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, daze, GodRules.Freya.weakAmp(tier)));
            if (t instanceof ServerPlayer) t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, BalanceLaw.controlTicks(t, daze), 0));
            SkillFx.spray(level, false, t.position().add(0, t.getBbHeight() + 0.3, 0), 4, 0.3, 0.2, 0.3, 0.0);
        }
        if (!charmed.isEmpty()) {
            int pulses = GodRules.Freya.charmTicks(tier) / 10;
            SkillZones.start(player, 10, pulses, (owner, lvl, index) -> {
                for (UUID id : charmed) {
                    Entity e = lvl.getEntity(id);
                    if (e instanceof Mob mob && mob.isAlive() && mob.getTarget() == owner
                            && mob.distanceToSqr(owner) < GodRules.Freya.keepDistance() * GodRules.Freya.keepDistance()) forget(mob, owner);
                }
            });
        }
        Vec3 at = player.position();
        SkillFx.ring(level, at, Math.min(GodRules.Freya.radius(tier) * 0.6, 8.0), 0xFFA6D2);
        SkillFx.spray(level, false, at.add(0, 1, 0), 12, 1.2, 0.8, 1.2, 0.02);
        SkillFx.sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 2.0F, 1.2F);
        return true;
    }

    /** The mob forgets the caster: no target, no remembered attack target. */
    private static void forget(Mob mob, ServerPlayer caster) {
        if (mob.getTarget() == caster) mob.setTarget(null);
        try {
            var brain = mob.getBrain();
            if (brain.getMemory(MemoryModuleType.ATTACK_TARGET).filter(t -> t == caster).isPresent()) brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        } catch (RuntimeException ignored) {
            // a mob without a brain (most vanilla mobs) just keeps the cleared target
        }
        mob.getNavigation().stop();
    }
}
