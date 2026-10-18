package com.newuniverse.nusmp.skilltree.ft.fire;

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
 * Why: Fire Dragon's Roar is the second step of the owner's required shape (Iron Fist, then Roar, then a Secret Art) and the signature of the route, so it is built like the
 * anime move: an inhale you can see (gathering motes, a charging sound, and, for the strong levels, a line of dust along the lane), then the breath. The two halves are two
 * server events joined by {@link DragonTimers}, the one tool the server has for "in a few ticks"; the lane is read again when the breath leaves, so the caster steers it
 * during the inhale, and if the caster died or left in between nothing happens. The damage is one hit per foe in a capsule along the lane, nearest first, capped by a table,
 * through the balance law; the client only ever receives the flame effect and the dust. The cost is paid at the start so a roar that is interrupted by death is not free.
 */
public final class FireDragonsRoarSkill extends FireDragonSkill {
    public FireDragonsRoarSkill() { super(FireDragonNumbers.ROAR, "ft_fire_roar"); }

    @Override
    protected boolean perform(ServerPlayer p, ManasSkillInstance inst, int lv) {
        if (!pay(p, lv)) return false;
        ServerLevel level = p.serverLevel();
        float raw = (float) num("damage", lv);
        Vec3 mouth = DragonKit.mouth(p);
        Vec3 lane = DragonKit.wallEnd(p, mouth, mouth.add(p.getViewVector(1f).scale(num("range", lv))));

        DragonKit.tellGather(level, mouth, 0.5, TELL_RGB, 14);
        if (DragonBudget.needsTell(raw)) DragonKit.tellLine(level, mouth, lane, TELL_RGB);
        AbilityKit.sound(p, SoundEvents.WARDEN_SONIC_CHARGE, 0.6f, 1.6f);

        double mastery = DragonKit.mastery(inst);
        if (!DragonTimers.later(level, Math.max(1, inum("windup", lv)), () -> breathe(p, level, lv, mastery))) breathe(p, level, lv, mastery);
        return true;
    }

    /** The breath itself, a few ticks after the inhale. */
    private void breathe(ServerPlayer p, ServerLevel level, int lv, double mastery) {
        if (!DragonKit.ready(p, level)) return;
        Vec3 mouth = DragonKit.mouth(p);
        Vec3 end = DragonKit.wallEnd(p, mouth, mouth.add(p.getViewVector(1f).scale(num("range", lv))));
        float burn = (float) num("burn_seconds", lv);

        List<LivingEntity> foes = AbilityKit.along(p, mouth, end, num("width", lv));
        List<LivingEntity> hit = DragonKit.strike(p, mouth, foes, inum("targets", lv), (float) num("damage", lv), mastery, t -> t.igniteForSeconds(burn));

        AbilityKit.fx(level, VfxShape.FLAME_TRAIL, mouth, end, 0, 22, 0.9f + 0.2f * lv);
        AbilityKit.fx(level, VfxShape.FLAME_EXPLOSION, end, end, 0, 20, 0.45f + 0.1f * lv);
        DragonKit.burstOn(level, VfxShape.FIRE_BURST, hit, 14, 0.4f);
        AbilityKit.sound(p, SoundEvents.ENDER_DRAGON_GROWL, 0.8f, 1.4f);
        AbilityKit.sound(p, SoundEvents.BLAZE_SHOOT, 1.0f, 0.7f);
    }
}
