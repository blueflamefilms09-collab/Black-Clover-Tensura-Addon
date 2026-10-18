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
 * Why: in the story Sky Dragon's Wing Attack is a whirlwind from each arm that strikes everything around the user, and it is the answer to being surrounded, which a cone like the
 * Roar cannot give. So this is a circle round the caster (the shared {@code AbilityKit.around}, then the fair-target filter) that damages and throws every target outward by its own
 * knockback rule: the push is what makes the technique work as an escape and not only as damage, and it is soft, so it needs no hard-control gate. It is the first node under the
 * root in the OFFENCE branch because a Sky Dragon Slayer who wants to fight needs a close-range tool first; the Updraft passive and the Wave Wind hang from it. Damage stays at or
 * below 8 raw (no tell), the circle grows with the node level, and the sound and the two crescents drawn to the left and right are the "wing" of the name. The caster is never
 * a target, nor is anyone of the caster's party, their pets or a villager.
 */
public final class SkyWingAttackSkill extends FtTreeSkill {
    public SkyWingAttackSkill() { super(SkyRoute.ID, SkyRoute.WING_ATTACK, "item/elytra", SkyNumbers::fallback); }

    @Override
    protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
        double radius = num("radius", lv), push = num("knockback", lv);
        float raw = (float) num("damage", lv);
        int cap = whole("targets", lv);
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        double mastery = FtWorld.masteryFrac(instance);
        List<LivingEntity> foes = FtWorld.nearest(FtWorld.hostile(p, AbilityKit.around(p, c, radius)), c, cap);
        for (LivingEntity foe : foes) {
            AbilityKit.hit(p, foe, raw, mastery);
            FtWorld.shove(foe, p, push);
        }

        Vec3 look = FtWorld.flatLook(p), right = new Vec3(-look.z, 0, look.x), chest = c.add(0, 1.0, 0);
        AbilityKit.fx(level, VfxShape.WIND_RING, chest, chest, SkyRoute.WIND, 16, (float) radius / 3.5f);
        AbilityKit.fx(level, VfxShape.WIND_SLASH, chest, chest.add(right.scale(radius)), SkyRoute.SKY, 10, 0.9f);
        AbilityKit.fx(level, VfxShape.WIND_SLASH, chest, chest.add(right.scale(-radius)), SkyRoute.SKY, 10, 0.9f);
        AbilityKit.sound(p, SoundEvents.ENDER_DRAGON_FLAP, 0.8f, 1.4f);
        AbilityKit.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f, 1.3f);
        return true;
    }
}
