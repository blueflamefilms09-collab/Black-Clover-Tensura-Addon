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
 * Why: Fire Dragon's Brilliant Flame is the ranged area attack of the Fire route (canon in the anime: a great fireball thrown with both hands), the answer to foes out of reach
 * of the fist and the roar. A fireball takes time to fly, so it is two server events: the throw (the stream of flame to the target, a ring of warning dust on the ground where
 * it will land, the cost) and, after a flight time worked out from the distance, the burst (everyone near the point takes one hit and is thrown outward). The target point is
 * fixed at the throw, which is what the ring shows, so a foe can step out of it: the technique is fair to the one it is cast at. No entity is spawned and no block is touched,
 * the fireball is only an effect, and if the caster died meanwhile the burst does not happen.
 */
public final class FireDragonsBrilliantFlameSkill extends FireDragonSkill {
    public FireDragonsBrilliantFlameSkill() { super(FireDragonNumbers.BRILLIANT_FLAME, "ft_fire_brilliant_flame"); }

    @Override
    protected boolean perform(ServerPlayer p, ManasSkillInstance inst, int lv) {
        if (!pay(p, lv)) return false;
        ServerLevel level = p.serverLevel();
        Vec3 hand = DragonKit.fist(p).add(0.0, 0.3, 0.0);
        Vec3 impact = DragonKit.aimPoint(p, num("range", lv));
        double radius = num("radius", lv);
        int flight = Math.max(6, Math.min(16, (int) (hand.distanceTo(impact) / 2.0)));

        DragonKit.tellRing(level, DragonKit.groundBelow(p, impact), radius, TELL_RGB);
        AbilityKit.fx(level, VfxShape.FLAME_TRAIL, hand, impact, 0, flight + 4, 1.1f + 0.15f * lv);
        AbilityKit.sound(p, SoundEvents.BLAZE_SHOOT, 1.0f, 0.6f);

        double mastery = DragonKit.mastery(inst);
        if (!DragonTimers.later(level, flight, () -> burst(p, level, lv, impact, mastery))) burst(p, level, lv, impact, mastery);
        return true;
    }

    /** The burst where the fireball lands. */
    private void burst(ServerPlayer p, ServerLevel level, int lv, Vec3 impact, double mastery) {
        if (!DragonKit.ready(p, level)) return;
        float burn = (float) num("burn_seconds", lv);
        double knock = num("knockback", lv);
        Vec3 fallback = DragonKit.flatLook(p);
        List<LivingEntity> foes = AbilityKit.around(p, impact, num("radius", lv));
        List<LivingEntity> hit = DragonKit.strike(p, impact, foes, inum("targets", lv), (float) num("damage", lv), mastery, t -> {
            t.igniteForSeconds(burn);
            DragonKit.knock(t, DragonKit.awayFrom(impact, t, fallback), knock);
        });
        AbilityKit.fx(level, VfxShape.FLAME_EXPLOSION, impact, impact, 0, 24, (float) (num("radius", lv) / 2.5));
        DragonKit.burstOn(level, VfxShape.FIRE_BURST, hit, 14, 0.45f);
        DragonKit.soundAt(level, impact, SoundEvents.DRAGON_FIREBALL_EXPLODE, 1.2f, 1.0f);
    }
}
