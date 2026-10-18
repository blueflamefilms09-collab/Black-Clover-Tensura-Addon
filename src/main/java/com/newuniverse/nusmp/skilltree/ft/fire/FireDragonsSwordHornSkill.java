package com.newuniverse.nusmp.skilltree.ft.fire;

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
 * Why: Fire Dragon's Sword Horn is the charge of the Fire route (canon: the whole body ablaze and a headbutt at high speed, a pillar of fire where it lands), the technique that
 * closes distance. A charge is a rush that reaches past melee range, so it is the one fist-and-feet technique with a real wind-up: the flames gather round the caster and the line
 * of the charge is drawn at once, and the charge itself leaves a few ticks later along that same line, so a foe can step off it (the owner's rule that a technique above eight raw
 * damage shows what it is about to do). A charge is a move, so it is computed first and refused cleanly when there is no room (the dash stops at the first wall, so it can never put
 * the caster inside a block or through one), and only then paid for; when the wind-up ends the path is worked out again from where the caster stands, and a caster who was walled in
 * meanwhile simply does not charge. The foes along the path are hit, nearest first, through the balance law, and thrown ahead; the caster is then placed at the end of the path
 * with a server-side teleport, which is exact and cannot desynchronise, instead of a velocity that a lag spike could turn into a different distance. A rider cannot charge (the
 * mount would be left behind).
 */
public final class FireDragonsSwordHornSkill extends FireDragonSkill {
    public FireDragonsSwordHornSkill() { super(FireDragonNumbers.SWORD_HORN, "ft_fire_sword_horn"); }

    @Override
    protected boolean perform(ServerPlayer p, ManasSkillInstance inst, int lv) {
        if (p.isPassenger()) { AbilityKit.say(p, "You cannot charge from a mount."); return false; }
        Vec3 dir = DragonKit.flatLook(p);
        Vec3 start = p.position();
        Vec3 dest = DragonKit.clearDash(p, dir, num("dash", lv));
        if (dest.distanceToSqr(start) < 1.0) { AbilityKit.say(p, "No room to charge."); return false; }
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        // the wind-up: the flames gather on the caster and the line of the charge is shown (the lane does not change afterwards, which is what makes it a fair tell)
        DragonKit.tellLine(level, start.add(0.0, 1.0, 0.0), dest.add(0.0, 1.0, 0.0), TELL_RGB);
        DragonKit.tellGather(level, start.add(0.0, 1.0, 0.0), 0.6, TELL_RGB, 12);
        AbilityKit.sound(p, SoundEvents.FIRECHARGE_USE, 0.8f, 0.5f);

        double mastery = DragonKit.mastery(inst);
        if (!DragonTimers.later(level, Math.max(1, inum("windup", lv)), () -> charge(p, level, lv, dir, mastery))) charge(p, level, lv, dir, mastery);
        return true;
    }

    /** The charge itself, a few ticks after the wind-up, along the line that was shown. */
    private void charge(ServerPlayer p, ServerLevel level, int lv, Vec3 dir, double mastery) {
        if (!DragonKit.ready(p, level) || p.isPassenger()) return;
        Vec3 start = p.position();
        Vec3 dest = DragonKit.clearDash(p, dir, num("dash", lv));
        if (dest.distanceToSqr(start) < 1.0) return;

        float burn = (float) num("burn_seconds", lv);
        double knock = num("knockback", lv);
        Vec3 from = start.add(0.0, 1.0, 0.0), to = dest.add(0.0, 1.0, 0.0);

        List<LivingEntity> foes = AbilityKit.along(p, from, to, num("width", lv));
        List<LivingEntity> hit = DragonKit.strike(p, start, foes, inum("targets", lv), (float) num("damage", lv), mastery, t -> {
            t.igniteForSeconds(burn);
            DragonKit.knock(t, dir, knock);
        });

        p.teleportTo(dest.x, dest.y, dest.z);
        AbilityKit.fx(level, VfxShape.FLAME_TRAIL, from, to, 0, 16, 1.2f + 0.1f * lv);
        AbilityKit.fx(level, VfxShape.FIRE_PILLAR, dest, dest.add(0.0, 1.0, 0.0), 0, 24, 0.9f + 0.1f * lv);
        DragonKit.burstOn(level, VfxShape.FIRE_BURST, hit, 14, 0.5f);
        AbilityKit.sound(p, SoundEvents.FIRECHARGE_USE, 1.0f, 0.7f);
        AbilityKit.sound(p, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f);
    }
}
