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
 * Why: Crimson Lotus: Exploding Flame Blade is the Secret Art of the Fire route and the end of the owner's required shape (Iron Fist, Roar, then a Secret Art), so it is the one
 * technique allowed the Secret Art numbers of the balance budget (up to 30 raw on the last blast, 8 percent of the magicules, 45 seconds and more of cooldown) and the one that
 * has to look like an ending. In canon both arms swirl and a torrent of flame barrages the target with a burst at every connecting strike and a larger final explosion; here that
 * is a lane fixed at the cast (the caster's facing), a spiral of flame drawn along it, a few pulses of damage spaced by the vanilla hurt-frame margin (so every pulse really
 * lands and the sum stays under the balance law), and a final blast at the lane's end, all on the server through {@link DragonTimers}, with the lane and the blast ring drawn as
 * warning dust at the very start. It needs two parents (Roar and Wing Attack), which is the "two branches" of the owner's tree. If the caster dies mid-barrage the rest is dropped.
 */
public final class CrimsonLotusSkill extends FireDragonSkill {
    /** Ticks from the cast to the first pulse: the arms swirl. */
    private static final int SWIRL_TICKS = 8;
    /** Ticks from the last pulse to the final blast. */
    private static final int BEFORE_BLAST_TICKS = 6;

    public CrimsonLotusSkill() { super(FireDragonNumbers.CRIMSON_LOTUS, "ft_fire_crimson_lotus"); }

    @Override
    protected boolean perform(ServerPlayer p, ManasSkillInstance inst, int lv) {
        if (!pay(p, lv)) return false;
        ServerLevel level = p.serverLevel();
        Vec3 dir = DragonKit.flatLook(p);
        Vec3 origin = DragonKit.fist(p);
        Vec3 end = DragonKit.wallEnd(p, origin, origin.add(dir.scale(num("range", lv))));
        Vec3 blast = DragonKit.groundBelow(p, end);
        int hits = Math.max(1, Math.min(DragonBudget.MAX_FOLLOW_UPS, inum("hits", lv)));
        int gap = Math.max(DragonBudget.MIN_PULSE_GAP_TICKS, inum("gap", lv));
        double mastery = DragonKit.mastery(inst);

        DragonKit.tellLine(level, origin, end, TELL_RGB);
        DragonKit.tellRing(level, blast, num("final_radius", lv), TELL_RGB);
        AbilityKit.fx(level, VfxShape.MAGIC_CIRCLE, p.position().add(0.0, 0.05, 0.0), p.position().add(0.0, 1.0, 0.0), FLAME_ARGB, SWIRL_TICKS + hits * gap, 1.3f);
        AbilityKit.fx(level, VfxShape.FIRE_SPIRAL, origin, end, 0, SWIRL_TICKS + hits * gap, 1.3f + 0.1f * lv);
        AbilityKit.sound(p, SoundEvents.ENDER_DRAGON_GROWL, 1.0f, 1.0f);

        int last = SWIRL_TICKS;
        for (int i = 0; i < hits; i++) {
            last = SWIRL_TICKS + i * gap;
            if (!DragonTimers.later(level, last, () -> pulse(p, level, lv, origin, end, mastery))) break;
        }
        DragonTimers.later(level, last + BEFORE_BLAST_TICKS, () -> explode(p, level, lv, blast, dir, mastery));
        return true;
    }

    /** One strike of the barrage: everyone in the lane. */
    private void pulse(ServerPlayer p, ServerLevel level, int lv, Vec3 origin, Vec3 end, double mastery) {
        if (!DragonKit.ready(p, level)) return;
        float burn = (float) num("burn_seconds", lv);
        List<LivingEntity> foes = AbilityKit.along(p, origin, end, num("width", lv));
        List<LivingEntity> hit = DragonKit.strike(p, origin, foes, inum("targets", lv), (float) num("hit_damage", lv), mastery, t -> t.igniteForSeconds(burn));
        DragonKit.burstOn(level, VfxShape.FIRE_BURST, hit, 14, 0.6f);
        AbilityKit.fx(level, VfxShape.FIRE_BURST, end, end, 0, 14, 0.5f);
        AbilityKit.sound(p, SoundEvents.FIRECHARGE_USE, 1.0f, 0.8f);
    }

    /** The final explosion at the end of the lane. */
    private void explode(ServerPlayer p, ServerLevel level, int lv, Vec3 blast, Vec3 dir, double mastery) {
        if (!DragonKit.ready(p, level)) return;
        float burn = (float) num("burn_seconds", lv);
        double knock = num("knockback", lv), radius = num("final_radius", lv);
        List<LivingEntity> foes = AbilityKit.around(p, blast, radius);
        List<LivingEntity> hit = DragonKit.strike(p, blast, foes, inum("targets", lv), (float) num("final_damage", lv), mastery, t -> {
            t.igniteForSeconds(burn);
            DragonKit.knock(t, DragonKit.awayFrom(blast, t, dir), knock);
        });
        AbilityKit.fx(level, VfxShape.FIRE_PILLAR, blast, blast.add(0.0, 1.0, 0.0), 0, 40, (float) (radius * 0.6));
        AbilityKit.fx(level, VfxShape.FLAME_EXPLOSION, blast, blast, 0, 28, (float) (radius / 2.2));
        AbilityKit.fx(level, VfxShape.FIRE_WILD, blast, blast.add(0.0, 1.0, 0.0), 0, 30, (float) radius);
        DragonKit.burstOn(level, VfxShape.FIRE_BURST, hit, 16, 0.7f);
        DragonKit.soundAt(level, blast, SoundEvents.DRAGON_FIREBALL_EXPLODE, 1.6f, 0.8f);
        DragonKit.soundAt(level, blast, SoundEvents.ENDER_DRAGON_SHOOT, 1.2f, 0.7f);
    }
}
