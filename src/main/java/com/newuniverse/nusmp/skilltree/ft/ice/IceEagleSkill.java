package com.newuniverse.nusmp.skilltree.ft.ice;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ft.kit.FtClock;
import com.newuniverse.nusmp.skilltree.ft.kit.FtTreeSkill;
import com.newuniverse.nusmp.skilltree.ft.kit.FtWorld;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: Ice-Make: Eagle is the story's own example of DYNAMIC Ice-Make: ice shaped into living creatures that move under the mage's control, here several eagles that fly through the air to
 * different opponents and, because their magic is dynamic, dodge whatever is in the way. That last point decides the design: the eagles do not need a clear line, so the technique asks
 * for none, and it is the right tool against foes behind cover, where the Lance and the Saucer fail. The eagles are not entities (few are allowed, none past 10 seconds): each is a flight drawn by
 * the mod's Heavenly Ice Fang and a strike scheduled for the tick it lands ({@link FtClock}), so the damage arrives when the picture does. They are shared out among the nearest foes in turn
 * ({@link IceNumbers#eagleAssignment}), never more than two on one foe and the second one a moment later (after the creature's own protection), so the technique spreads over a crowd
 * and cannot be stacked on a single target past two hits of at most 6 raw. With no foe in range it refuses before it costs anything. Each strike chills (soft control). If the caster dies
 * or leaves while the eagles fly, the strikes that have not landed do not happen.
 */
public final class IceEagleSkill extends FtTreeSkill {
    /** The most eagles that may strike one foe, and how many ticks the second one trails the first. */
    public static final int PER_FOE = 2, SECOND_TRAILS = 12;

    public IceEagleSkill() { super(IceRoute.ID, IceRoute.EAGLE, "item/feather", IceNumbers::fallback); }

    @Override
    protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
        double range = num("range", lv), speed = Math.max(0.3, num("speed", lv));
        float raw = (float) num("damage", lv);
        int count = whole("count", lv), chill = (int) Math.round(num("chill_seconds", lv) * 20);

        List<LivingEntity> foes = FtWorld.nearest(FtWorld.hostile(p, AbilityKit.around(p, p.position(), range)), p.position(), count);
        if (foes.isEmpty()) {
            AbilityKit.say(p, "No foe within reach of the eagles.");
            return false;
        }
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        double mastery = FtWorld.masteryFrac(instance);
        Vec3 look = FtWorld.flatLook(p), right = new Vec3(-look.z, 0, look.x), wing = p.getEyePosition().add(right.scale(0.5)).add(0, 0.2, 0);
        int[] plan = IceNumbers.eagleAssignment(count, foes.size(), PER_FOE);
        for (int k = 0; k < plan.length; k++) {
            LivingEntity foe = foes.get(plan[k]);
            int trail = k >= foes.size() ? SECOND_TRAILS : 0;
            int flight = (int) Math.ceil(wing.distanceTo(FtWorld.center(foe)) / speed) + 2;
            Vec3 from = wing.add(right.scale(k % 2 == 0 ? 0.0 : -1.0));
            FtClock.later(level, trail, () -> {
                if (FtClock.present(p, level)) AbilityKit.fx(level, VfxShape.ICE_FX1, from, FtWorld.center(foe), IceRoute.ICE, Math.max(6, flight), 0.7f);
            });
            FtClock.later(level, trail + flight, () -> {
                if (!FtClock.present(p, level) || !foe.isAlive()) return;
                AbilityKit.hit(p, foe, raw, mastery);
                IceConstructs.chill(foe, chill, 0, 25);
                AbilityKit.fx(level, VfxShape.ICE_FX3, FtWorld.center(foe), FtWorld.center(foe), IceRoute.ICE, 12, 0.5f);
            });
        }
        AbilityKit.sound(p, SoundEvents.ENDER_DRAGON_FLAP, 0.7f, 1.8f);
        AbilityKit.sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 0.7f);
        return true;
    }
}
