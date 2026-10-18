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
 * Why: Sky Dragon's Breath is the healing of the route (in the story, gusts with healing in them blown at someone badly hurt), and a healer is judged by two things: whom the heal
 * reaches and how safe it is. WHOM: allies in a circle round the caster ({@link SkyAllies}: the caster, the party, the team and their pets), and when more are wounded than one cast may
 * mend, the MOST wounded come first ({@link SkyNumbers#mostWounded}), so a dying friend is never passed over for a scratched one; nobody wounded means the cast is refused before it
 * costs anything. HOW SAFE: every point goes through {@code AbilityKit.heal} (the balance law: repeated heals within 20 seconds are weaker) and the amount is a share of the target's maximum
 * health capped at the law's 25 percent ({@link SkyNumbers#healAmount}), so no data file can make the breath a full heal. The caster receives only {@code self_share} of it: in the story the
 * healing magic of the sky dragons cannot be turned on the one who casts it, and this keeps that as a cost without making the route useless alone (the Roar and the Wing Attack are
 * there for that). Each healed friend gets the gentle blue ribbon the mod already draws for wound-closing, so the player can see who was reached.
 */
public final class SkyBreathSkill extends FtTreeSkill {
    public SkyBreathSkill() { super(SkyRoute.ID, SkyRoute.BREATH, "item/glistering_melon_slice", SkyNumbers::fallback); }

    @Override
    protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
        double share = num("heal", lv), radius = num("radius", lv), self = num("self_share", lv);
        int cap = whole("targets", lv);

        List<LivingEntity> pool = SkyAllies.around(p, p.position(), radius);
        double[] fraction = new double[pool.size()], distance = new double[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            LivingEntity e = pool.get(i);
            fraction[i] = e.getMaxHealth() <= 0f ? 1.0 : e.getHealth() / (double) e.getMaxHealth();
            distance[i] = e.distanceTo(p);
        }
        int[] picked = SkyNumbers.mostWounded(fraction, distance, cap);
        if (picked.length == 0) {
            AbilityKit.say(p, "No one nearby is wounded.");
            return false;
        }
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        Vec3 chest = p.position().add(0, 1.1, 0);
        for (int i : picked) {
            LivingEntity e = pool.get(i);
            boolean me = e == p;
            AbilityKit.heal(e, SkyNumbers.healAmount(e.getMaxHealth(), me ? share * self : share));
            if (me) AbilityKit.fx(level, VfxShape.SEAL_ORBIT, FtWorld.center(e), FtWorld.center(e), SkyRoute.MINT, 40, 0.9f);
            else AbilityKit.fx(level, VfxShape.SEAL_WOUND, chest, FtWorld.center(e), SkyRoute.MINT, 40, 1f);
        }
        AbilityKit.fx(level, VfxShape.WIND_RING, p.position().add(0, 0.6, 0), p.position().add(0, 0.6, 0), SkyRoute.MINT, 20, (float) radius / 6f);
        AbilityKit.sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 1.3f);
        AbilityKit.sound(p, SoundEvents.BREEZE_IDLE_AIR, 0.8f, 1.1f);
        return true;
    }
}
