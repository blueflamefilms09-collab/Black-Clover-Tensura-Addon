package com.newuniverse.nusmp.skilltree.ft.ice;

import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ft.kit.FtBudget;
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
 * Why: Ice-Make: Hammer is, in the story, a huge ice hammer that forms above the opponent, hangs there, and drops with great force, and the whole point of it is the hang: the opponent can see
 * it coming. That is exactly the owner's "tell" rule for anything that hits harder than 8 raw (this technique hits for 8 to 14), so the tell is the technique's own shape and not an add-on:
 * the moment it is cast, a ring of telegraph dust is drawn on the ground at the size of the blow, a column of dust rises where the hammer will fall and a frost patch spreads under it, and
 * nothing lands until the wind-up from the route file is over (never under 8 ticks), when a giant ice fang drops the last few blocks and the blow lands on whatever still stands in the ring. The
 * place is fixed when the hammer is cast (on the foe the caster is aiming at: the one nearest the crosshair that the caster can see), so moving out of the ring is the counter, for a player or a
 * boss. It is one heavy hit per foe inside the ring (at most {@code targets}), with a slowing chill (soft control, so no hard-control gate), and it is cast only when there is a foe to aim at, so
 * it is never spent on empty air. If the caster dies or leaves during the wind-up, the hammer does not fall.
 */
public final class IceHammerSkill extends FtTreeSkill {
    /** How high above the ground the hammer hangs, and how long the final drop is drawn. */
    public static final double HANG = 7.0;
    public static final int DROP = 8;

    public IceHammerSkill() { super(IceRoute.ID, IceRoute.HAMMER, "item/mace", IceNumbers::fallback); }

    @Override
    protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
        double range = num("range", lv), radius = num("radius", lv);
        float raw = (float) num("damage", lv);
        int windup = Math.max(FtBudget.TELL_MIN_TICKS, whole("windup", lv)), cap = whole("targets", lv), slow = (int) Math.round(num("slow_seconds", lv) * 20);

        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        LivingEntity target = null;
        double bestDot = -2.0;
        for (LivingEntity e : FtWorld.hostile(p, AbilityKit.cone(p, range, 18))) {
            if (!p.hasLineOfSight(e)) continue;
            double dot = FtWorld.center(e).subtract(eye).normalize().dot(look);
            if (dot > bestDot) {
                bestDot = dot;
                target = e;
            }
        }
        if (target == null) {
            AbilityKit.say(p, "No foe in sight to bring the hammer down on.");
            return false;
        }
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        double mastery = FtWorld.masteryFrac(instance);
        Vec3 spot = target.position();

        // the tell: nothing lands until the wind-up is over
        Tells.ring(level, spot, radius, IceRoute.TELL);
        Tells.column(level, spot, HANG, IceRoute.TELL);
        AbilityKit.fx(level, VfxShape.ICE_FX2, spot, spot, IceRoute.ICE, windup + 6, (float) radius);
        FtWorld.soundAt(level, spot, SoundEvents.GLASS_PLACE, 1.0f, 0.5f);

        FtClock.later(level, windup - DROP, () -> {
            if (FtClock.present(p, level)) AbilityKit.fx(level, VfxShape.ICE_FX1, spot.add(0, HANG, 0), spot, IceRoute.WHITE, DROP, 2.6f);
        });
        FtClock.later(level, windup, () -> {
            if (!FtClock.present(p, level)) return;
            List<LivingEntity> hit = FtWorld.nearest(FtWorld.hostile(p, AbilityKit.around(p, spot, radius)), spot, cap);
            for (LivingEntity foe : hit) {
                AbilityKit.hit(p, foe, raw, mastery);
                IceConstructs.chill(foe, slow, 1, 60);
            }
            AbilityKit.fx(level, VfxShape.ICE_FX3, spot, spot, IceRoute.ICE, 28, (float) (radius / 2.0));
            FtWorld.soundAt(level, spot, SoundEvents.ANVIL_LAND, 1.0f, 0.6f);
            FtWorld.soundAt(level, spot, SoundEvents.GLASS_BREAK, 1.0f, 0.7f);
        });
        return true;
    }
}
