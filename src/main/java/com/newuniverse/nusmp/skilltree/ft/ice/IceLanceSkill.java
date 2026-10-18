package com.newuniverse.nusmp.skilltree.ft.ice;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ft.kit.FtTreeSkill;
import com.newuniverse.nusmp.skilltree.ft.kit.FtWorld;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Why: Ice-Make: Lance is the root because it is the first thing every Ice-Make mage of the story makes (long, curved ice spears thrown from the extended arms, a ranged attack) and because it
 * is the one technique that needs no preparation, so a player who has just opened the book can fight at once and then decide whether to grow into the standing branch (floors, walls, cannons) or
 * the moving one (discs, birds, hammers). Each spear is a line (the shared {@code AbilityKit.along}) stopped by the first solid block, so a spear never pierces a wall; the lines are the
 * "length" of the spear and the node level makes it longer, wider and more numerous. Several spears are FANNED round the gaze ({@link IceNumbers#lanceAngles}) and a creature is hit by only
 * the first spear that reaches it: the fan widens what the technique covers and does not multiply its damage on one foe, which keeps the damage inside the balance law (at most 8 raw, no
 * tell, one hit per target). A struck foe is chilled for a moment (soft control). The drawing is the mod's own Heavenly Ice Fang along each line and a small shatter on each foe.
 */
public final class IceLanceSkill extends FtTreeSkill {
    public IceLanceSkill() { super(IceRoute.ID, IceRoute.LANCE, "item/trident", IceNumbers::fallback); }

    @Override
    protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
        double range = num("range", lv), width = num("width", lv), spread = num("spread", lv);
        float raw = (float) num("damage", lv);
        int count = Math.max(1, whole("count", lv)), cap = whole("targets", lv), chill = (int) Math.round(num("chill_seconds", lv) * 20);
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), hand = eye.add(look.scale(0.5)).add(0, -0.3, 0);
        double mastery = FtWorld.masteryFrac(instance);
        Set<LivingEntity> struck = new HashSet<>();
        for (double angle : IceNumbers.lanceAngles(count, spread)) {
            Vec3 dir = look.yRot((float) Math.toRadians(angle)).normalize();
            Vec3 end = FtWorld.clip(p, eye, eye.add(dir.scale(range)));
            for (LivingEntity foe : FtWorld.nearest(FtWorld.hostile(p, AbilityKit.along(p, eye, end, width)), eye, cap)) {
                if (struck.size() >= cap || !struck.add(foe)) continue;
                AbilityKit.hit(p, foe, raw, mastery);
                IceConstructs.chill(foe, chill, 0, 30);
                AbilityKit.fx(level, VfxShape.ICE_FX3, FtWorld.center(foe), FtWorld.center(foe), IceRoute.ICE, 14, 0.5f);
            }
            AbilityKit.fx(level, VfxShape.ICE_FX1, hand, end, IceRoute.ICE, 16, 1.0f);
        }
        AbilityKit.sound(p, SoundEvents.SNOW_GOLEM_SHOOT, 0.8f, 0.7f);
        AbilityKit.sound(p, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8f, 1.4f);
        return true;
    }
}
