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
 * Why: Ice-Make: Saucer is the story's spinning disc of ice with the cutting power of a buzz saw, thrown at the target, and it is the first technique of the MOVING branch: something made of
 * ice that travels under the mage's command. The disc is not an entity (few spawned things are allowed, none that outlive 10 seconds) but a line and a timetable: at the moment of the
 * cast the server finds the foes along the disc's straight path (stopped by the first solid block, so it never cuts through a wall) and, for each, schedules the cut at exactly the tick the
 * disc, flying at {@code speed} blocks a tick, would reach it ({@link IceNumbers#saucerDelay}). So a near foe is cut at once and a far one a moment later, which is what makes the picture (the disc
 * visibly flying) and the damage agree, and no foe is cut before the disc arrives. From level 3 the disc turns at the end of its path and comes back through the same line, so a
 * foe is cut a second time on the return, after the creature's own protection ({@code returns} in the data). Every cut is one hit of at most 8 raw, at most {@code targets} foes are ever
 * cut, and a cut chills (soft control). If the caster dies or leaves before the disc arrives, the later cuts do not happen.
 */
public final class IceSaucerSkill extends FtTreeSkill {
    public IceSaucerSkill() { super(IceRoute.ID, IceRoute.SAUCER, "item/prismarine_shard", IceNumbers::fallback); }

    @Override
    protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
        double range = num("range", lv), width = num("width", lv), speed = num("speed", lv);
        float raw = (float) num("damage", lv);
        int cap = whole("targets", lv);
        boolean back = num("returns", lv) > 0;
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        double mastery = FtWorld.masteryFrac(instance);
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), start = eye.add(look.scale(0.5)).add(0, -0.3, 0);
        Vec3 end = FtWorld.clip(p, start, start.add(look.scale(range)));
        double reach = start.distanceTo(end);
        int flight = IceNumbers.saucerDelay(reach, reach, speed, false);

        AbilityKit.fx(level, VfxShape.ICE_WEDGE_FX1, start, end, IceRoute.ICE, Math.max(6, flight), 1.3f);
        if (back) FtClock.later(level, flight, () -> {
            if (FtClock.present(p, level)) AbilityKit.fx(level, VfxShape.ICE_WEDGE_FX1, end, start, IceRoute.ICE, Math.max(6, flight), 1.3f);
        });
        AbilityKit.sound(p, SoundEvents.GRINDSTONE_USE, 0.7f, 1.5f);
        AbilityKit.sound(p, SoundEvents.GLASS_BREAK, 0.4f, 1.8f);

        List<LivingEntity> foes = FtWorld.nearest(FtWorld.hostile(p, AbilityKit.along(p, start, end, width)), start, cap);
        for (LivingEntity foe : foes) {
            Vec3 c = FtWorld.center(foe);
            double along = reach <= 0 ? 0.0 : IceNumbers.along(start.x, start.y, start.z, (end.x - start.x) / reach, (end.y - start.y) / reach, (end.z - start.z) / reach, c.x, c.y, c.z, reach);
            cut(p, level, foe, raw, mastery, IceNumbers.saucerDelay(along, reach, speed, false));
            if (back) cut(p, level, foe, raw, mastery, IceNumbers.saucerDelay(along, reach, speed, true));
        }
        return true;
    }

    /** Schedules one cut of the disc on {@code foe} for {@code delay} ticks from now. */
    private static void cut(ServerPlayer p, ServerLevel level, LivingEntity foe, float raw, double mastery, int delay) {
        FtClock.later(level, delay, () -> {
            if (!FtClock.present(p, level) || !foe.isAlive()) return;
            AbilityKit.hit(p, foe, raw, mastery);
            IceConstructs.chill(foe, 30, 0, 20);
            AbilityKit.fx(level, VfxShape.ICE_FX3, FtWorld.center(foe), FtWorld.center(foe), IceRoute.ICE, 12, 0.5f);
        });
    }
}
