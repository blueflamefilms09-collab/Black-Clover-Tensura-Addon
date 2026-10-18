package com.newuniverse.nusmp.skilltree.ft.lightning;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonKit;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonTimers;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: Lightning Dragon's Breakdown Fist is the one technique of the Lightning route that STUNS, and the stun is the route's identity (canon: the electricity stuns the target
 * and leaves it open to the next attack), so it is also the one that touches the balance law's hard-control rule: a stun is only applied after {@code AbilityKit.control}
 * granted the caster's shared one-per-twelve-seconds turn, for the time {@code BalanceLaw.controlTicks} allows that target (shorter on a boss, halved when repeated), and when the
 * turn is not available the foe is only numbed. It needs a target (the first foe in the line, found on the server), so it refuses before paying when there is none. The "magic
 * seal" of canon is the mod's magic-circle effect opened in front of the caster, and it is the technique's wind-up and its tell (the owner's rule that a technique above eight raw
 * damage shows what it is about to do, which for a hit that reaches ten to fourteen blocks must come BEFORE the hit): the seal opens and the line is drawn at once, the fist flies a
 * few ticks later along that same line, and whoever is in the line then takes it, so a foe that stepped out of the line is missed. The fist is the lightning bolt flying through the seal.
 */
public final class LightningDragonsBreakdownFistSkill extends LightningDragonSkill {
    public LightningDragonsBreakdownFistSkill() { super(LightningDragonNumbers.BREAKDOWN_FIST, "ft_lightning_breakdown_fist"); }

    @Override
    protected boolean perform(ServerPlayer p, ManasSkillInstance inst, int lv) {
        Vec3 from = DragonKit.mouth(p), look = p.getViewVector(1f);
        Vec3 end = DragonKit.wallEnd(p, from, from.add(look.scale(num("range", lv))));
        if (DragonKit.nearestAlong(p, from, end, num("width", lv)) == null) { AbilityKit.say(p, "No foe in the line of your fist."); return false; }
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        int windup = Math.max(1, inum("windup", lv));
        Vec3 seal = from.add(look.scale(1.2));

        DragonKit.tellLine(level, from, end, TELL_RGB);
        DragonKit.tellGather(level, seal, 0.5, TELL_RGB, 10);
        AbilityKit.fx(level, VfxShape.MAGIC_CIRCLE, seal, seal.add(look), BOLT_ARGB, windup + 14, 0.9f);
        AbilityKit.sound(p, SoundEvents.WARDEN_SONIC_CHARGE, 0.5f, 2.0f);

        double mastery = DragonKit.mastery(inst);
        if (!DragonTimers.later(level, windup, () -> fire(p, level, lv, from, end, mastery))) fire(p, level, lv, from, end, mastery);
        return true;
    }

    /** The fist leaves the seal, a few ticks after it opened, along the line that was shown: the first foe in it is struck, stunned if the control turn is free. */
    private void fire(ServerPlayer p, ServerLevel level, int lv, Vec3 from, Vec3 end, double mastery) {
        if (!DragonKit.ready(p, level)) return;
        LivingEntity target = DragonKit.nearestAlong(p, from, end, num("width", lv));
        Vec3 at = target != null ? DragonKit.center(target) : end;
        AbilityKit.fx(level, VfxShape.LIGHTNING_SPEAR, from, at, 0, 10, 1.5f + 0.2f * lv);
        AbilityKit.sound(p, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.9f, 1.4f);
        if (target == null) return;

        List<LivingEntity> hit = DragonKit.strike(p, from, List.of(target), 1, (float) num("damage", lv), mastery, null);
        if (!hit.isEmpty() && target.isAlive()) {
            if (AbilityKit.control(p)) DragonKit.stun(target, inum("stun_ticks", lv));
            else DragonKit.numb(target, num("numb_seconds", lv), 2);
            AbilityKit.fx(level, VfxShape.FX_LIGHTNING_ARC, at.add(0.0, 1.5, 0.0), at, 0, 10, 0.9f);
        }
        AbilityKit.sound(p, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.2f);
    }
}
