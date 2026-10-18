package com.newuniverse.nusmp.skilltree.ft.fire;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonBudget;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonKit;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: Fire Dragon's Wing Attack is the crowd answer of the Fire route and the second branch's first payoff (it is also one of the two parents of the Secret Art): both arms
 * sweep out in flame like the wings of a dragon and throw back everything in a wide fan in front of the caster. It is one instant server-side effect (a fan, so the foes of a
 * pack are all in it), nearest first and capped by a table, each hit through the balance law, each pushed away from the caster. The "wings" are two flame streams from the
 * shoulders and two crescent blades flying outward, drawn by the mod's own effect system, so the picture is the move and the client does no work; the fan edges are drawn as
 * warning dust for the levels above the owner's eight-raw-damage line. No hard control is applied (a shove is knock-back), so it needs no control gate.
 */
public final class FireDragonsWingAttackSkill extends FireDragonSkill {
    public FireDragonsWingAttackSkill() { super(FireDragonNumbers.WING_ATTACK, "ft_fire_wing_attack"); }

    @Override
    protected boolean perform(ServerPlayer p, ManasSkillInstance inst, int lv) {
        double range = num("range", lv), half = num("half_angle", lv);
        List<LivingEntity> foes = DragonKit.cone(p, range, half);
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        float raw = (float) num("damage", lv);
        float burn = (float) num("burn_seconds", lv);
        double knock = num("knockback", lv);
        Vec3 dir = DragonKit.flatLook(p), right = DragonKit.rightOf(dir);
        Vec3 chest = p.position().add(0.0, p.getBbHeight() * 0.7, 0.0);

        if (DragonBudget.needsTell(raw)) DragonKit.tellCone(level, p.position().add(0.0, 0.2, 0.0), dir, range, half, TELL_RGB);
        List<LivingEntity> hit = DragonKit.strike(p, p.position(), foes, inum("targets", lv), raw, DragonKit.mastery(inst), t -> {
            t.igniteForSeconds(burn);
            DragonKit.knock(t, DragonKit.awayFrom(p.position(), t, dir), knock);
        });

        for (int side = -1; side <= 1; side += 2) {
            Vec3 shoulder = chest.add(right.scale(0.45 * side));
            Vec3 tip = shoulder.add(DragonKit.yawed(dir, -side * half * 0.8).scale(range));
            AbilityKit.fx(level, VfxShape.FLAME_TRAIL, shoulder, tip, 0, 14, 0.7f + 0.1f * lv);
            AbilityKit.fx(level, VfxShape.WIND_SLASH, chest, chest.add(DragonKit.yawed(dir, -side * half * 0.6).scale(range)), FLAME_ARGB, 14, 1.1f + 0.1f * lv);
        }
        DragonKit.burstOn(level, VfxShape.FIRE_BURST, hit, 14, 0.45f);
        AbilityKit.sound(p, SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.2f);
        AbilityKit.sound(p, SoundEvents.BLAZE_SHOOT, 0.9f, 0.8f);
        return true;
    }
}
