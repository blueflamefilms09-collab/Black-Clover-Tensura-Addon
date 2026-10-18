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

/**
 * Why: Ice-Make: Cold Excalibur is the Secret Art of the route: a gigantic ice longsword, sharp and durable as metal, swung in one huge arc. The owner asked for the Secret Art to need two
 * branches, and the sword does by its nature (a weapon that STANDS, from the Shield's branch, swung by a hand that MOVES, from the Hammer's), so it hangs from both. It is the one technique of the
 * route that is announced for a long moment: three lines of telegraph dust show the two edges and the middle of the arc and motes gather in the caster's hand, and nothing lands until the
 * wind-up from the route file is over (at least 8 ticks, here 16 to 18), so a player or a boss can leave the arc, which is the counter. The arc is FIXED when the sword is raised (the eye and
 * the gaze at that moment), not where the caster turns during the wind-up, so what the dust showed is what is cut; a foe is hit if it is inside the cone and in the clear from the sword's
 * origin (a wall stops the arc). One hit per foe, at most 28 raw (the Secret Art cap is 30), at most {@code targets} foes, then a heavy chill and a shove back, both soft control, so no
 * hard-control gate. Its cost is the highest of the route (at most 8 percent of the magicules) and its cooldown at least 45 seconds, the Secret Art floor of the balance law. The drawing is the wide
 * crescent of the mod's wind sweep in ice colours for the arc, the Heavenly Ice Fang along its middle and the Ice Wedge eruption where it ends.
 */
public final class IceExcaliburSkill extends FtTreeSkill {
    public IceExcaliburSkill() { super(IceRoute.ID, IceRoute.EXCALIBUR, "item/diamond_sword", IceNumbers::fallback); }

    @Override
    protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
        double range = num("range", lv), half = num("half_angle", lv);
        float raw = (float) num("damage", lv);
        int windup = Math.max(FtBudget.TELL_MIN_TICKS, whole("windup", lv)), cap = whole("targets", lv), slow = (int) Math.round(num("slow_seconds", lv) * 20);
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        double mastery = FtWorld.masteryFrac(instance);
        Vec3 origin = p.position(), eye = p.getEyePosition(), look = p.getViewVector(1f);
        Vec3 left = look.yRot((float) Math.toRadians(half)).normalize(), right = look.yRot((float) Math.toRadians(-half)).normalize();
        Vec3 end = eye.add(look.scale(range));

        // the tell: the two edges and the middle of the arc, and the motes gathering in the hand; nothing lands until the wind-up is over
        Tells.line(level, eye, eye.add(left.scale(range)), IceRoute.TELL);
        Tells.line(level, eye, end, IceRoute.TELL);
        Tells.line(level, eye, eye.add(right.scale(range)), IceRoute.TELL);
        Tells.gather(level, eye.add(look.scale(0.8)), 1.2, IceRoute.TELL, 28);
        AbilityKit.fx(level, VfxShape.ICE_FX3, eye.add(look.scale(0.8)), eye.add(look.scale(2.5)), IceRoute.WHITE, windup + 8, 0.9f);
        AbilityKit.sound(p, SoundEvents.BEACON_ACTIVATE, 0.9f, 0.6f);
        AbilityKit.sound(p, SoundEvents.GLASS_PLACE, 1.0f, 0.5f);

        FtClock.later(level, windup, () -> {
            if (!FtClock.present(p, level)) return;
            int struck = 0;
            for (LivingEntity foe : FtWorld.nearest(FtWorld.hostile(p, AbilityKit.around(p, origin, range)), origin, 40)) {
                Vec3 c = FtWorld.center(foe);
                if (struck >= cap || !FtWorld.inCone(eye, look, c, half) || !FtWorld.clear(p, eye, c)) continue;
                struck++;
                AbilityKit.hit(p, foe, raw, mastery);
                IceConstructs.chill(foe, slow, 1, 100);
                FtWorld.shove(foe, p, 1.0);
                AbilityKit.fx(level, VfxShape.ICE_FX3, c, c, IceRoute.ICE, 20, 0.7f);
            }
            AbilityKit.fx(level, VfxShape.WIND_EMPEROR, eye, end, IceRoute.WHITE, 20, 2.4f);
            AbilityKit.fx(level, VfxShape.ICE_FX1, eye.add(look.scale(0.8)), end, IceRoute.ICE, 14, 3.0f);
            AbilityKit.fx(level, VfxShape.ICE_WEDGE_FX3, end, end.add(look.scale(2.0)), IceRoute.ICE, 28, 1.6f);
            FtWorld.soundAt(level, eye.add(look.scale(2.0)), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);
            FtWorld.soundAt(level, end, SoundEvents.ANVIL_LAND, 1.0f, 0.5f);
            FtWorld.soundAt(level, end, SoundEvents.GLASS_BREAK, 1.0f, 0.6f);
        });
        return true;
    }
}
