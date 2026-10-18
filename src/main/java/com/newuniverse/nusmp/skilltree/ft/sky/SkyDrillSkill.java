package com.newuniverse.nusmp.skilltree.ft.sky;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ft.kit.FtBudget;
import com.newuniverse.nusmp.skilltree.ft.kit.FtClock;
import com.newuniverse.nusmp.skilltree.ft.kit.FtTreeSkill;
import com.newuniverse.nusmp.skilltree.ft.kit.FtWorld;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: Shattering Light: Sky Drill is the Secret Art the story gives Wendy: she spreads her arms, a wall of wind closes round her and her opponent, she turns her arms and the wall
 * squeezes in and drills, and the opponent is thrown into the air and falls. The owner asked for the Secret Art of this route to be "a big heal-and-burst" that needs BOTH branches, so
 * the art is built from both: from the healing side the turning wind mends every friend inside the circle in one big heal at the first pulse (one call, so the balance law's
 * 20-second decay does not eat it), from the offence side the wall pulses on the foes inside and then bursts and throws them up. Because a Secret Art may hit hard, it is announced: a
 * ring of telegraph dust on the ground at the edge of the circle, the wind mantle on the caster and a gathering of motes appear at once, and nothing lands until the wind-up from the route
 * file is over (at least 8 ticks, the tell rule), so a player or a boss can walk out of the circle, which is the counter. The circle is fixed where the art was cast: foes beyond its
 * edge are never hit, and the wall visibly shrinks toward 60 percent of its radius to show the squeeze (it does not move the foes, only the picture). The whole sequence (pulses of
 * {@code pulse_damage} then one {@code burst_damage}) is held to the 30 raw per target of the Secret Art cap by {@code FtBudget}. The throw into the air is hard control, so it happens
 * only after {@code AbilityKit.control} grants the shared one-per-12-seconds lock, and never to a boss; when the lock is closed the burst only shoves the foes back. The timeline is
 * run by the tree's clock, one zone with a step every ten ticks, so nothing is spawned and nothing outlives half a second beyond the burst.
 */
public final class SkyDrillSkill extends FtTreeSkill {
    /** How hard the burst throws (blocks per tick of upward speed, about 3.5 blocks of height). */
    public static final double LAUNCH = 0.75;

    public SkyDrillSkill() { super(SkyRoute.ID, SkyRoute.SKY_DRILL, "item/breeze_rod", SkyNumbers::fallback); }

    @Override
    protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
        double radius = num("radius", lv), heal = num("heal", lv);
        int windup = Math.max(FtBudget.TELL_MIN_TICKS, whole("windup", lv)), pulses = Math.max(1, whole("pulses", lv)), cap = whole("targets", lv);
        float pulseDamage = (float) num("pulse_damage", lv), burstDamage = (float) num("burst_damage", lv);
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        double mastery = FtWorld.masteryFrac(instance);

        // the tell: the circle, the mantle and the gathering wind appear now; nothing lands for `windup` ticks
        Tells.ring(level, c, radius, SkyRoute.TELL);
        Tells.gather(level, c.add(0, 1.0, 0), radius * 0.5, SkyRoute.TELL, 24);
        VfxSpawn.sendFollowing(level, VfxShape.WIND_ZEPHYR, p, c.add(0, 1.0, 0), SkyRoute.WHITE, windup + 30, 1.3f);
        AbilityKit.fx(level, VfxShape.WIND_TORNADO, c, c.add(0, 1.0, 0), SkyRoute.WIND, windup + 30, (float) radius * 0.35f);
        AbilityKit.sound(p, SoundEvents.BREEZE_CHARGE, 1.0f, 1.0f);

        FtClock.later(level, windup, () -> FtClock.zone(level, SkyNumbers.drillLife(pulses), SkyNumbers.DRILL_INTERVAL, age -> {
            int step = SkyNumbers.drillStep(age, pulses);
            if (step == SkyNumbers.DRILL_NONE || !FtClock.present(p, level)) return;
            boolean burst = step == pulses + 1;
            List<LivingEntity> foes = FtWorld.nearest(FtWorld.hostile(p, AbilityKit.around(p, c, radius)), c, cap);
            double drawn = SkyNumbers.squeezeRadius(radius, step, pulses);

            if (!burst) {
                for (LivingEntity foe : foes) {
                    AbilityKit.hit(p, foe, pulseDamage, mastery);
                    foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1));
                }
                if (step == 1) mend(p, level, c, radius, heal);
                Tells.ring(level, c, drawn, SkyRoute.TELL);
                AbilityKit.fx(level, VfxShape.WIND_RING, c.add(0, 0.4, 0), c.add(0, 0.4, 0), SkyRoute.SKY, 12, (float) drawn / 3f);
                FtWorld.soundAt(level, c, SoundEvents.BREEZE_WHIRL, 0.8f, 1.1f + 0.1f * step);
                return;
            }

            boolean lift = !foes.isEmpty() && AbilityKit.control(p);
            for (LivingEntity foe : foes) {
                AbilityKit.hit(p, foe, burstDamage, mastery);
                if (lift && !BalanceLaw.isBoss(foe)) FtWorld.launch(foe, LAUNCH);
                else FtWorld.shove(foe, p, 1.0);
            }
            AbilityKit.fx(level, VfxShape.WIND_TORNADO, c, c.add(0, 1.0, 0), SkyRoute.WHITE, 24, (float) radius * 0.5f);
            AbilityKit.fx(level, VfxShape.WIND_RING, c.add(0, 0.4, 0), c.add(0, 0.4, 0), SkyRoute.WHITE, 16, (float) radius / 2.5f);
            FtWorld.soundAt(level, c, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f);
            FtWorld.soundAt(level, c, SoundEvents.BREEZE_SHOOT, 1.0f, 0.7f);
        }));
        return true;
    }

    /** The one big heal of the art: every wounded friend inside the circle gets {@code share} of their maximum health (the caster only half of it). */
    private static void mend(ServerPlayer p, ServerLevel level, Vec3 c, double radius, double share) {
        for (LivingEntity ally : SkyAllies.around(p, c, radius)) {
            if (ally.getHealth() >= ally.getMaxHealth()) continue;
            AbilityKit.heal(ally, SkyNumbers.healAmount(ally.getMaxHealth(), ally == p ? share * SkyNumbers.DRILL_SELF : share));
            AbilityKit.fx(level, VfxShape.SEAL_ORBIT, FtWorld.center(ally), FtWorld.center(ally), SkyRoute.MINT, 40, 0.9f);
        }
    }
}
