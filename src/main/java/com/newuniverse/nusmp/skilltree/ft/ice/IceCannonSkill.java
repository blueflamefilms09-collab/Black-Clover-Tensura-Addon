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

/**
 * Why: Ice-Make: Cannon is the story's example of a STATIC construct that still acts: a small ornate cannon of ice that stays where it was made and fires shells (rubble from the ground) at
 * the enemy, and several can be made at once. In a game that is a turret, and a turret is the one thing here that needs the clock to do work without the player: a point on the ground
 * ({@code reach} blocks in front of the caster) that, every {@code interval} ticks for {@code shots} shots, picks the nearest fair foe it can see (a clear line, no block between the muzzle and the
 * foe, so it never fires through a wall) and shoots it. It is a point and a drawing, not an entity (the owner limits spawned things and forbids block changes), it lives only as long as
 * its shots need (the budget check ties the life to {@code shots * interval} and caps it at 10 seconds), and it takes one of the caster's four construct slots. A shot is at most 6 raw,
 * so a turret is a steady helper and not a burst; the caster who wants more places the cannon where the foes will come. If nothing is in sight when a shot is due, that shot is simply
 * not fired. The turret stops at once if its caster dies or leaves. The drawing is the mod's Ice Wedge eruption (the cannon rising), a patch of frost under it, and a Heavenly Ice Fang as the shell.
 */
public final class IceCannonSkill extends FtTreeSkill {
    public IceCannonSkill() { super(IceRoute.ID, IceRoute.CANNON, "item/crossbow_standby", IceNumbers::fallback); }

    @Override
    protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
        double range = num("range", lv), reach = num("reach", lv);
        float raw = (float) num("damage", lv);
        int shots = whole("shots", lv), interval = Math.max(4, whole("interval", lv)), life = IceNumbers.clampLife(whole("life", lv));
        if (!IceConstructs.hasRoom(p)) {
            AbilityKit.say(p, "Four ice constructs already stand.");
            return false;
        }
        if (!pay(p, lv)) return false;
        IceConstructs.record(p, life);

        ServerLevel level = p.serverLevel();
        double mastery = FtWorld.masteryFrac(instance);
        Vec3 spot = FtWorld.groundAt(p, p.position().add(FtWorld.flatLook(p).scale(reach))), muzzle = spot.add(0, 1.1, 0);
        AbilityKit.fx(level, VfxShape.ICE_WEDGE_FX3, spot, spot.add(0, 1.0, 0), IceRoute.ICE, 28, 0.9f);
        AbilityKit.fx(level, VfxShape.ICE_FX2, spot, spot, IceRoute.ICE, life, 1.2f);
        FtWorld.soundAt(level, spot, SoundEvents.GLASS_PLACE, 1.0f, 0.9f);

        FtClock.zone(level, life, interval, age -> {
            if (!FtClock.present(p, level) || age / interval >= shots) return;
            LivingEntity target = null;
            for (LivingEntity e : FtWorld.nearest(FtWorld.hostile(p, AbilityKit.around(p, muzzle, range)), muzzle, 6)) {
                if (FtWorld.clear(p, muzzle, FtWorld.center(e))) {
                    target = e;
                    break;
                }
            }
            if (target == null) return;
            Vec3 at = FtWorld.center(target);
            AbilityKit.fx(level, VfxShape.ICE_FX1, muzzle, at, IceRoute.ICE, 6, 0.5f);
            AbilityKit.hit(p, target, raw, mastery);
            IceConstructs.chill(target, 20, 0, 15);
            AbilityKit.fx(level, VfxShape.ICE_FX3, at, at, IceRoute.ICE, 12, 0.4f);
            FtWorld.soundAt(level, muzzle, SoundEvents.SNOW_GOLEM_SHOOT, 0.9f, 0.9f);
        });
        return true;
    }
}
