package com.newuniverse.nusmp.skilltree.ft.sky;

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

import java.util.List;

/**
 * Why: Sky Dragon's Roar is the root of the route because it is the technique every Sky Dragon Slayer has (the wiki calls it a tornado-like blast breathed from the mouth) and
 * because it is the one place the two halves of the route meet: it is an attack, and in the story its variant "Roar + Cure" carries Wendy's healing in the same wind. So the node
 * is a cone (the shape of a breath, found with the shared {@code AbilityKit.cone}) that damages and shoves back every fair target in line of sight, and from level 4 the same wind
 * mends the allies that stand in the cone, through {@code AbilityKit.heal} and the 25 percent cap of the balance law. The numbers (damage, range, angle, push, cure, cost, cooldown) come
 * from the route file at the player's node level, so the SP they spend is what makes the roar longer, wider and kinder; the damage stays at or below 8 raw, so it needs no tell and
 * can be used as the opener it is meant to be. The push uses the creature's own knockback rule (so heavy bosses with knockback resistance barely move) and is soft control: nothing here
 * stuns, roots or pulls, so it never needs the hard-control gate.
 */
public final class SkyRoarSkill extends FtTreeSkill {
    public SkyRoarSkill() { super(SkyRoute.ID, SkyRoute.ROAR, "item/goat_horn", SkyNumbers::fallback); }

    @Override
    protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
        double range = num("range", lv), half = num("half_angle", lv), push = num("knockback", lv), cure = num("heal", lv);
        float raw = (float) num("damage", lv);
        int cap = whole("targets", lv);
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), mouth = eye.add(look.scale(0.6)).add(0, -0.25, 0), end = eye.add(look.scale(range));
        double mastery = FtWorld.masteryFrac(instance);

        List<LivingEntity> foes = FtWorld.nearest(FtWorld.hostile(p, AbilityKit.cone(p, range, half)), eye, cap);
        for (LivingEntity foe : foes) {
            if (!p.hasLineOfSight(foe)) continue;
            AbilityKit.hit(p, foe, raw, mastery);
            FtWorld.shove(foe, p, push);
        }
        if (cure > 0) {
            for (LivingEntity ally : SkyAllies.around(p, p.position(), range)) {
                if (ally == p || ally.getHealth() >= ally.getMaxHealth() || !FtWorld.inCone(eye, look, FtWorld.center(ally), half)) continue;
                AbilityKit.heal(ally, SkyNumbers.healAmount(ally.getMaxHealth(), cure));
                AbilityKit.fx(level, VfxShape.SEAL_WOUND, mouth, FtWorld.center(ally), SkyRoute.MINT, 30, 1f);
            }
        }

        AbilityKit.fx(level, VfxShape.WIND_GALE, mouth, end, SkyRoute.WIND, 18, 1.0f + (float) range / 12f);
        AbilityKit.fx(level, VfxShape.WIND_RING, p.position().add(0, 1.0, 0), p.position().add(0, 1.0, 0), SkyRoute.SKY, 14, 0.7f);
        AbilityKit.sound(p, SoundEvents.ENDER_DRAGON_GROWL, 0.6f, 1.7f);
        AbilityKit.sound(p, SoundEvents.BREEZE_SHOOT, 0.8f, 1.2f);
        return true;
    }
}
