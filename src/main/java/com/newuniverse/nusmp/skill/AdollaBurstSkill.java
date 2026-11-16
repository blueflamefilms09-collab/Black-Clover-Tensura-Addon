package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.ShinraRules;
import com.newuniverse.nusmp.skill.rules.UltimateClass;
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
 * Adolla Burst: the Ultimate of the Shinra skill {@link HellWalkerSkill} (unlocked automatically by mastering it; before the Shinra rework: of Devil's Footprints, which is folded into Hell Walker). Not an alias. The
 * Ignition Dash becomes Light Speed (a blink of up to 32 blocks that strikes and ignites every foe on the way), the Devil's Kick reaches
 * eight blocks and hits more than twice as hard, and a third mode, Adolla Burst, is appended: an explosion of flame ten blocks wide
 * that hurts, ignites and hurls back every foe around you. You are still immune to fire. Numbers: {@link FireForceRules}.
 */
public class AdollaBurstSkill extends HellWalkerSkill {
    static final SkillText ADOLLA_TEXT = SkillText.of("adolla_burst", "Adolla Burst",
            "Ultimate. Shinra's Ultimate, evolved from Hell Walker. Move at the speed of light striking everything on the way, kick with far more force, unleash the Adolla Burst around you, and in Adolla Link near death a hellfire aura burns the foes around you.");

    public AdollaBurstSkill() { super(SkillType.ULTIMATE, ADOLLA_TEXT, true); }
    @Override public UltimateClass ultimateClass() { return UltimateClass.LORD; }      // docs/ultimate_classes.md

    @Override protected boolean fullWheel() { return true; }
    /** Adolla Burst always had its wheel: it stays on with the setting off (then without the Rapid-Man Kick, and without the Hell Mode toggle). */
    @Override protected boolean wheelOn() { return true; }
    @Override public int getModes(ManasSkillInstance i) { return NUConfig.shinraRework() ? super.getModes(i) : FireForceRules.modes(Tier.ULTIMATE); }
    @Override public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) { return NUConfig.shinraRework(); }
    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return NUConfig.shinraRework(); }

    /** Adolla Link at the Ultimate: the hellfire aura and the berserk of the old Hell Conqueror, on top of the grin and link of {@link HellWalkerSkill#pulse}. */
    @Override
    protected void pulse(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        super.pulse(instance, player, tier);
        hellfire(instance, player, tier);
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        int move = ShinraRules.move(true, mode);
        if (move == ShinraRules.KICK || move == ShinraRules.RAPID) { super.onPressed(instance, entity, key, mode); return; }
        int slot = Math.floorMod(mode, getModes(instance));
        if (instance.onCoolDown(slot)) { SkillUtil.fail(player, SkillMsg.t("fireforce.cooldown", "Your flames need a moment.")); return; }
        Tier tier = tier(instance, player);

        if (move == ShinraRules.DASH) {
            double price = price(instance, player, DMUtil.cost(FireForceRules.dashCost(tier)));
            if (!charge(instance, player, slot, price)) return;
            if (!lightSpeed(instance, player, tier)) { SkillUtil.refund(player, price); return; }
            moveCooldown(instance, player, slot, FireForceRules.dashCooldown(tier));
        } else if (move == ShinraRules.BURST) {
            if (!pay(instance, player, slot, DMUtil.cost(FireForceRules.burstCost()))) return;
            burst(instance, player);
            moveCooldown(instance, player, slot, FireForceRules.burstCooldown());
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
            if (SkillDamage.hit(player, t, FireForceRules.lightSpeedDamage(tier), masteryFrac(instance), instance) > 0f) t.igniteForSeconds(5);
        }
        SkillFx.beam(level, a, b, false);
        SkillFx.shape(level, VfxShape.LIGHT_BLADE, a, b, 0xFF6AD8FF, 12, 1.2f);
        SkillAnimeFx.firebolt(level, a, b, 0xFF6AD8FF, 1.2f);                            // VFX wave 3: the blue flash of Light Speed
        SkillFx.sound(level, to, SoundEvents.FIREWORK_ROCKET_BLAST, 1.5F, 0.6F);
        return true;
    }

    /** The Adolla Burst: flame in every direction. */
    private void burst(ManasSkillInstance instance, ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position();
        for (LivingEntity t : SkillTargets.foes(player, at, FireForceRules.burstRadius())) {
            if (SkillDamage.hit(player, t, FireForceRules.burstDamage(), masteryFrac(instance), SkillDamage.Kind.MAGIC, instance) > 0f) {
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
        SkillAnimeFx.sigil(level, SkillAnimeFx.FIRE_FORCE, at, FireForceRules.burstRadius(), 0xFF6AD8FF, 30);      // VFX wave 3: the Adolla flames burst out
        SkillFx.ring(level, at, 8.0, 0x6AD8FF);
        SkillFx.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 1.2F);
    }
}
