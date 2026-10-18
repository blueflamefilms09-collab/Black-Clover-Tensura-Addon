package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.FireForceRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Adolla Burst: the Ultimate evolution of {@link DevilsFootprintsSkill} (unlocked automatically by mastering it). Not an alias. The
 * Ignition Dash becomes Light Speed (a blink of up to 32 blocks that strikes and ignites every foe on the way), the Devil's Kick reaches
 * eight blocks and hits more than twice as hard, and a third mode, Adolla Burst, is appended: an explosion of flame ten blocks wide
 * that hurts, ignites and hurls back every foe around you. You are still immune to fire. Numbers: {@link FireForceRules}.
 */
public class AdollaBurstSkill extends DevilsFootprintsSkill {
    static final SkillText ADOLLA_TEXT = SkillText.of("adolla_burst", "Adolla Burst",
            "Ultimate. Evolved from Devil's Footprints. Move at the speed of light striking everything on the way, kick with far more force, and unleash the Adolla Burst around you.");

    public AdollaBurstSkill() { super(SkillType.ULTIMATE, ADOLLA_TEXT, true); }

    @Override public int getModes(ManasSkillInstance i) { return FireForceRules.modes(Tier.ULTIMATE); }

    @Override
    public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean r) {
        int count = getModes(i);
        return r ? (mode + count - 1) % count : (mode + 1) % count;
    }

    @Override
    public String getModeId(ManasSkillInstance i, int mode) {
        return switch (mode) {
            case FireForceRules.DASH -> "fireforce.light_speed";
            case FireForceRules.BURST -> "fireforce.burst";
            default -> super.getModeId(i, mode);
        };
    }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        return switch (mode) {
            case FireForceRules.DASH -> SkillMsg.mode("fireforce.light_speed", "Light Speed");
            case FireForceRules.BURST -> SkillMsg.mode("fireforce.burst", "Adolla Burst");
            default -> super.getModeName(i, mode);
        };
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        if (mode == FireForceRules.KICK) { super.onPressed(instance, entity, key, mode); return; }
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("fireforce.cooldown", "Your flames need a moment.")); return; }
        Tier tier = tier(instance, player);

        if (mode == FireForceRules.DASH) {
            double price = DMUtil.cost(FireForceRules.dashCost(tier));
            if (!SkillUtil.spendMagicules(player, price)) return;
            if (!lightSpeed(instance, player, tier)) { SkillUtil.refund(player, price); return; }
            cooldown(instance, mode, DMUtil.cooldown(FireForceRules.dashCooldown(tier)));
        } else if (mode == FireForceRules.BURST) {
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(FireForceRules.burstCost()))) return;
            burst(instance, player);
            cooldown(instance, mode, DMUtil.cooldown(FireForceRules.burstCooldown()));
        } else {
            return;
        }
        SkillUtil.castVfx(player, 0xFF6AD8FF);
        grow(instance, player, 1);
    }

    /** Light Speed: blink to the far end of your sight line, burning everything you pass. Returns false if there was no room. */
    private boolean lightSpeed(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        ServerLevel level = player.serverLevel();
        Vec3 from = player.position();
        if (!com.newuniverse.nusmp.relics.Fx.teleportLook(player, FireForceRules.lightSpeedRange(tier))) {
            SkillUtil.fail(player, SkillMsg.t("fireforce.no_room", "No room to move."));
            return false;
        }
        Vec3 to = player.position();
        Vec3 a = from.add(0, 1, 0), b = to.add(0, 1, 0);
        for (LivingEntity t : SkillTargets.foesAlong(player, a, b, FireForceRules.lightSpeedWidth())) {
            if (SkillDamage.hit(player, t, FireForceRules.lightSpeedDamage(tier), masteryFrac(instance)) > 0f) t.igniteForSeconds(5);
        }
        SkillFx.beam(level, a, b, false);
        SkillFx.shape(level, VfxShape.LIGHT_BLADE, a, b, 0xFF6AD8FF, 12, 1.2f);
        SkillFx.sound(level, to, SoundEvents.FIREWORK_ROCKET_BLAST, 1.5F, 0.6F);
        return true;
    }

    /** The Adolla Burst: flame in every direction. */
    private void burst(ManasSkillInstance instance, ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position();
        for (LivingEntity t : SkillTargets.foes(player, at, FireForceRules.burstRadius())) {
            if (SkillDamage.hit(player, t, FireForceRules.burstDamage(), masteryFrac(instance), SkillDamage.Kind.MAGIC) > 0f) {
                t.igniteForSeconds(FireForceRules.burstBurnSeconds());
                Vec3 away = t.position().subtract(at);
                if (away.lengthSqr() > 1.0e-4) {
                    Vec3 d = away.normalize();
                    t.setDeltaMovement(d.x * 1.6, 0.6, d.z * 1.6);
                    t.hurtMarked = true;
                }
            }
        }
        SkillFx.shape(level, VfxShape.FIRE_WILD, at, at.add(0, 1, 0), 0xFF6AD8FF, 30, (float) FireForceRules.burstRadius());
        SkillFx.ring(level, at, 8.0, 0x6AD8FF);
        SkillFx.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 1.2F);
    }
}
