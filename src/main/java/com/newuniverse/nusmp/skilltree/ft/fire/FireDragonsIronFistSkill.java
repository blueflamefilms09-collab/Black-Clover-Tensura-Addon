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
 * Why: Fire Dragon's Iron Fist is the ROOT of the Fire route (the owner's example: Iron Fist, then Roar, then a Secret Art), so it must be the cheapest, quickest and most
 * forgiving technique: one punch, an instant effect, a short cooldown, five levels that visibly widen it (more reach, a wider arc, more foes, a longer burn). It is a real
 * ManasCore skill so it lives in Tensura's skill menu on Tensura's keys; the engine's cast gate and cooldown are in the base class, so this class is only the punch. Everything
 * happens on the server in the same tick (a melee blow needs no delay), the client sees only the effect packets, damage goes through the balance law, and a punch that finds
 * nobody still shows its flames and spends the (small) cost, so the key always answers.
 */
public final class FireDragonsIronFistSkill extends FireDragonSkill {
    public FireDragonsIronFistSkill() { super(FireDragonNumbers.IRON_FIST, "ft_fire_iron_fist"); }

    @Override
    protected boolean perform(ServerPlayer p, ManasSkillInstance inst, int lv) {
        List<LivingEntity> foes = DragonKit.cone(p, num("range", lv), num("half_angle", lv));
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        float raw = (float) num("damage", lv);
        float burn = (float) num("burn_seconds", lv);
        double knock = num("knockback", lv);
        Vec3 dir = DragonKit.flatLook(p), fist = DragonKit.fist(p);

        List<LivingEntity> hit = DragonKit.strike(p, p.position(), foes, inum("targets", lv), raw, DragonKit.mastery(inst), t -> {
            t.igniteForSeconds(burn);
            DragonKit.knock(t, dir, knock);
        });

        Vec3 impact = hit.isEmpty() ? fist.add(dir.scale(num("range", lv) * 0.6)) : DragonKit.center(hit.get(0));
        if (DragonBudget.needsTell(raw)) DragonKit.tellLine(level, fist, impact, TELL_RGB);
        AbilityKit.fx(level, VfxShape.FLAME_TRAIL, fist, impact, 0, 10, 0.55f + 0.05f * lv);
        AbilityKit.fx(level, VfxShape.FIRE_BURST, impact, impact, 0, 14, 0.5f + 0.05f * lv);
        AbilityKit.sound(p, SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.8f);
        AbilityKit.sound(p, SoundEvents.FIRECHARGE_USE, 0.8f, 1.2f);
        return true;
    }
}
