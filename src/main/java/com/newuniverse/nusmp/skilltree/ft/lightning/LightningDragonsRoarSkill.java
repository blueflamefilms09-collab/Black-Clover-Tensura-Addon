package com.newuniverse.nusmp.skilltree.ft.lightning;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonBudget;
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
 * Why: Lightning Dragon's Roar is the breath of the Lightning route and the second step of its main branch, and canon gives it a trait the Fire roar does not have: the head can
 * be moved while it is breathed, so the bolt covers an arc and not a line. That is a FAN here: three bolts drawn across a cone, and the damage is the cone itself (everything
 * inside it that the caster can see, nearest first, capped by a table; a wall stops it), which is forgiving to aim and different to use from the Fire lane. Like the Fire roar it is an inhale and then a breath, two server
 * events joined by {@link DragonTimers}, steered by where the caster looks when the breath leaves; the victims are numbed, which is a slowdown and not hard control, so no control
 * gate is involved. If the caster is gone when the breath is due, nothing happens.
 */
public final class LightningDragonsRoarSkill extends LightningDragonSkill {
    public LightningDragonsRoarSkill() { super(LightningDragonNumbers.ROAR, "ft_lightning_roar"); }

    @Override
    protected boolean perform(ServerPlayer p, ManasSkillInstance inst, int lv) {
        if (!pay(p, lv)) return false;
        ServerLevel level = p.serverLevel();
        float raw = (float) num("damage", lv);
        Vec3 mouth = DragonKit.mouth(p);
        Vec3 lane = DragonKit.wallEnd(p, mouth, mouth.add(p.getViewVector(1f).scale(num("range", lv))));

        DragonKit.tellGather(level, mouth, 0.5, TELL_RGB, 14);
        if (DragonBudget.needsTell(raw)) DragonKit.tellCone(level, mouth, p.getViewVector(1f), num("range", lv), num("half_angle", lv), TELL_RGB);
        AbilityKit.sound(p, SoundEvents.WARDEN_SONIC_CHARGE, 0.6f, 2.0f);
        DragonKit.tellLine(level, mouth, lane, TELL_RGB);

        double mastery = DragonKit.mastery(inst);
        if (!DragonTimers.later(level, Math.max(1, inum("windup", lv)), () -> breathe(p, level, lv, mastery))) breathe(p, level, lv, mastery);
        return true;
    }

    /** The breath itself, a few ticks after the inhale: a fan of three bolts and a cone of damage. */
    private void breathe(ServerPlayer p, ServerLevel level, int lv, double mastery) {
        if (!DragonKit.ready(p, level)) return;
        double range = num("range", lv), half = num("half_angle", lv), numb = num("numb_seconds", lv);
        Vec3 mouth = DragonKit.mouth(p), look = p.getViewVector(1f);

        List<LivingEntity> foes = DragonKit.cone(p, range, half);
        List<LivingEntity> hit = DragonKit.strike(p, mouth, foes, inum("targets", lv), (float) num("damage", lv), mastery, t -> DragonKit.numb(t, numb, 1));

        for (int side = -1; side <= 1; side++) {
            Vec3 dir = DragonKit.yawed(look, side * half * 0.7);
            Vec3 end = DragonKit.wallEnd(p, mouth, mouth.add(dir.scale(range)));
            AbilityKit.fx(level, VfxShape.LIGHTNING_SPEAR, mouth, end, 0, 12, 1.0f + 0.15f * lv);
        }
        DragonKit.arcsTo(level, mouth, hit, 10, 0.9f);
        AbilityKit.sound(p, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.7f, 1.6f);
        AbilityKit.sound(p, SoundEvents.ENDER_DRAGON_GROWL, 0.6f, 1.8f);
    }
}
