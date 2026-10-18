package com.newuniverse.nusmp.skilltree.ft.lightning;

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

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Lightning Dragon's Iron Fist is the ROOT of the Lightning route, the counterpart of the Fire route's fist, and it must teach the route's different idea from the first
 * key press: lightning does not burn, it numbs and it jumps. So the punch hits the foes in front, numbs them (a short slowdown, which is not hard control), and its higher levels
 * arc to further foes standing near the first one, each arc a smaller hit through the balance law, drawn with the mod's own lightning-arc effect. Everything is one instant
 * server-side effect; the client only sees the bolts. It is cheap and quick because a root technique is the one players will press most.
 */
public final class LightningDragonsIronFistSkill extends LightningDragonSkill {
    public LightningDragonsIronFistSkill() { super(LightningDragonNumbers.IRON_FIST, "ft_lightning_iron_fist"); }

    @Override
    protected boolean perform(ServerPlayer p, ManasSkillInstance inst, int lv) {
        List<LivingEntity> foes = DragonKit.cone(p, num("range", lv), num("half_angle", lv));
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        float raw = (float) num("damage", lv);
        double numb = num("numb_seconds", lv), knock = num("knockback", lv), mastery = DragonKit.mastery(inst);
        Vec3 dir = DragonKit.flatLook(p), fist = DragonKit.fist(p);

        List<LivingEntity> hit = DragonKit.strike(p, p.position(), foes, inum("targets", lv), raw, mastery, t -> {
            DragonKit.numb(t, numb, 1);
            DragonKit.knock(t, dir, knock);
        });

        Vec3 impact = hit.isEmpty() ? fist.add(dir.scale(num("range", lv) * 0.6)) : DragonKit.center(hit.get(0));
        if (DragonBudget.needsTell(raw)) DragonKit.tellLine(level, fist, impact, TELL_RGB);
        AbilityKit.fx(level, VfxShape.LIGHTNING_SPEAR, fist, impact, 0, 8, 0.7f + 0.05f * lv);

        int chains = inum("chain_targets", lv);
        if (chains > 0 && !hit.isEmpty()) {
            Vec3 from = DragonKit.center(hit.get(0));
            List<LivingEntity> near = new ArrayList<>();
            LivingEntity first = hit.get(0);
            for (LivingEntity e : AbilityKit.around(p, from, num("chain_range", lv))) if (!hit.contains(e) && first.hasLineOfSight(e)) near.add(e);
            DragonKit.arcsTo(level, from, DragonKit.strike(p, from, near, chains, (float) num("chain_damage", lv), mastery, e -> DragonKit.numb(e, numb, 1)), 10, 0.8f);
        }
        AbilityKit.sound(p, SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 1.1f);
        AbilityKit.sound(p, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5f, 1.8f);
        return true;
    }
}
