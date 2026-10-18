package com.newuniverse.nusmp.skilltree.ft.lightning;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonKit;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonTimers;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Roaring Thunder is the Secret Art of the Lightning route and the end of its tree (canon: the user rushes the target and releases a large arching burst of lightning from the
 * fist, heavily damaging and electrocuting it and throwing it back with great force; first seen against Jura in the Grand Magic Games). It is the one technique allowed the Secret
 * Art numbers of the balance budget, and it earns them by needing BOTH branches of the tree (the fist branch through the Breakdown Fist and the speed branch through the Flash): it
 * is a rush and then a burst, two server events joined by {@link DragonTimers}. The rush is a teleport to a point in front of the chosen foe, computed first and stopping at the
 * first wall; the target is the nearest foe in a narrow cone, and with none the technique refuses BEFORE paying. The burst hits the target for the big number, the foes around it
 * for the splash number, pushes them all away, and stuns the target only when the caster's shared hard-control turn allows (otherwise a heavy numbing), through the balance law.
 * A ring of warning dust marks the burst while the caster rushes in, and the burst really is where the ring is (it does not follow the target), so a foe quick enough to leave the
 * ring is missed; that is the technique's counter and the reason the ring is drawn. If the caster dies in between, the burst does not happen.
 */
public final class RoaringThunderSkill extends LightningDragonSkill {
    /** How far in front of the foe's feet the rush stops. */
    private static final double STAND_OFF = 1.6;

    public RoaringThunderSkill() { super(LightningDragonNumbers.ROARING_THUNDER, "ft_lightning_roaring_thunder"); }

    @Override
    protected boolean perform(ServerPlayer p, ManasSkillInstance inst, int lv) {
        if (p.isPassenger()) { AbilityKit.say(p, "You cannot rush from a mount."); return false; }
        double rush = num("rush_range", lv);
        LivingEntity target = null;
        for (LivingEntity e : DragonKit.cone(p, rush, 30.0)) if (target == null || e.distanceToSqr(p) < target.distanceToSqr(p)) target = e;
        if (target == null) { AbilityKit.say(p, "Roaring Thunder needs a foe in sight."); return false; }

        Vec3 start = p.position();
        Vec3 dir = DragonKit.awayFrom(start, target, DragonKit.flatLook(p));
        double flat = Math.sqrt(Math.pow(target.getX() - start.x, 2) + Math.pow(target.getZ() - start.z, 2));
        Vec3 dest = DragonKit.clearDash(p, dir, Math.max(0.0, Math.min(rush, flat - STAND_OFF)));
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        Vec3 foe = target.position();
        double radius = num("burst_radius", lv);
        int windup = Math.max(1, inum("windup", lv));

        DragonKit.tellLine(level, start.add(0.0, 1.0, 0.0), dest.add(0.0, 1.0, 0.0), TELL_RGB);
        DragonKit.tellRing(level, foe, radius, TELL_RGB);
        p.teleportTo(dest.x, dest.y, dest.z);
        AbilityKit.fx(level, VfxShape.LIGHTNING_SPEAR, start.add(0.0, 1.0, 0.0), dest.add(0.0, 1.0, 0.0), 0, 12, 2.0f);
        VfxSpawn.sendFollowing(level, VfxShape.LIGHTNING_FIEND, p, p.position(), 0, windup + 14, 1.4f);
        AbilityKit.sound(p, SoundEvents.FIREWORK_ROCKET_BLAST, 1.0f, 1.2f);

        LivingEntity chosen = target;
        double mastery = DragonKit.mastery(inst);
        if (!DragonTimers.later(level, windup, () -> burst(p, level, lv, chosen, foe, mastery))) burst(p, level, lv, chosen, foe, mastery);
        return true;
    }

    /**
     * The burst, a few ticks after the rush, FIXED on the spot where the ring of dust was drawn (the foe's place at the rush): whoever stands in the ring now is hit, the chosen
     * target for the big number and the others for the splash, shoved away from the caster; the target is then stunned (or heavily numbed). A target that has left the ring in
     * the meantime is missed, which is the counter the ring offers, and a target that died is simply not there.
     */
    private void burst(ServerPlayer p, ServerLevel level, int lv, LivingEntity target, Vec3 spot, double mastery) {
        if (!DragonKit.ready(p, level)) return;
        double radius = num("burst_radius", lv), knock = num("knockback", lv);
        Vec3 fallback = DragonKit.flatLook(p);

        boolean inside = false;
        List<LivingEntity> others = new ArrayList<>();
        for (LivingEntity e : AbilityKit.around(p, spot, radius)) {
            if (e == target) inside = true;
            else others.add(e);
        }
        List<LivingEntity> victims = new ArrayList<>();
        if (inside && AbilityKit.hit(p, target, (float) num("damage", lv), mastery)) victims.add(target);
        victims.addAll(DragonKit.strike(p, spot, others, Math.max(1, inum("targets", lv) - 1), (float) num("splash_damage", lv), mastery, null));

        for (LivingEntity t : victims) DragonKit.knock(t, DragonKit.awayFrom(p.position(), t, fallback), knock);
        Vec3 mid = spot.add(0.0, 1.0, 0.0);
        DragonKit.arcsTo(level, mid, victims, 10, 1.0f);
        if (victims.contains(target) && target.isAlive()) {
            if (AbilityKit.control(p)) DragonKit.stun(target, inum("stun_ticks", lv));
            else DragonKit.numb(target, inum("stun_ticks", lv) / 20.0, 3);
        }
        AbilityKit.fx(level, VfxShape.LIGHTNING_GOD, spot, mid, 0, 30, (float) radius);
        DragonKit.soundAt(level, spot, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.6f, 0.8f);
        DragonKit.soundAt(level, spot, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2f, 0.9f);
    }
}
