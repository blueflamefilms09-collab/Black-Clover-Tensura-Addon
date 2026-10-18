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
 * Why: Fire Dragon's Claw is the first step of the second branch of the Fire route (kicks and rushes: Claw, then Wing Attack and Sword Horn), the technique that moves the
 * caster as well as the foe: a flaming kick in a wide arc that launches what it hits, with a small jet-propelled hop of the caster (canon: the flames on the feet also work as
 * a jet). The hop is a velocity push marked for sync (the server tells the player's client about it), not a teleport, so it feels like movement; the damage is instant, to the foes
 * in a short cone in front, nearest first, through the balance law; the launch is a short upward throw that is plain knock-back, not hard control, so it needs no control gate.
 * The client sees only the crescent and burst effects.
 */
public final class FireDragonsClawSkill extends FireDragonSkill {
    public FireDragonsClawSkill() { super(FireDragonNumbers.CLAW, "ft_fire_claw"); }

    @Override
    protected boolean perform(ServerPlayer p, ManasSkillInstance inst, int lv) {
        double range = num("range", lv), half = num("half_angle", lv);
        List<LivingEntity> foes = DragonKit.cone(p, range, half);
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        float raw = (float) num("damage", lv);
        float burn = (float) num("burn_seconds", lv);
        double launch = num("launch", lv), hop = num("hop", lv);
        Vec3 dir = DragonKit.flatLook(p);
        Vec3 foot = p.position().add(0.0, 0.35, 0.0);

        if (DragonBudget.needsTell(raw)) DragonKit.tellCone(level, p.position().add(0.0, 0.2, 0.0), dir, range, half, TELL_RGB);
        List<LivingEntity> hit = DragonKit.strike(p, p.position(), foes, inum("targets", lv), raw, DragonKit.mastery(inst), t -> {
            t.igniteForSeconds(burn);
            DragonKit.launch(t, launch);
        });

        p.setDeltaMovement(p.getDeltaMovement().add(dir.x * hop * 1.4, hop, dir.z * hop * 1.4));
        p.hurtMarked = true;

        AbilityKit.fx(level, VfxShape.WIND_SLASH, foot, foot.add(dir.scale(range)).add(0.0, 0.9, 0.0), FLAME_ARGB, 12, 1.0f + 0.1f * lv);
        DragonKit.burstOn(level, VfxShape.FIRE_BURST, hit, 14, 0.45f);
        AbilityKit.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.9f);
        AbilityKit.sound(p, SoundEvents.FIRECHARGE_USE, 0.8f, 1.0f);
        return true;
    }
}
