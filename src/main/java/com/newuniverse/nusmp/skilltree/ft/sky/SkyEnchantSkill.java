package com.newuniverse.nusmp.skilltree.ft.sky;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ft.kit.FtTreeSkill;
import com.newuniverse.nusmp.skilltree.ft.kit.FtWorld;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Why: the Sky Dragon Slayer of the story is also an Enchanter: Arms raises attack power, Vernier raises speed, and the "Ile" version chants the spell twice for twice the effect. A game
 * has ordinary status effects for exactly these, so this technique gives the allies round the caster Strength (Arms) and Speed (Vernier) for a number of seconds that grows with
 * the node level, and at the top level the speed is doubled by one step (the Ile chant) while the strength is left alone, because a second step of Strength would be a very large
 * damage bonus and the doubled chant is already a gift. The duration is always shorter than the cooldown (the budget check enforces it), so the enchant can be kept up almost, never
 * for ever, and a support who wants it on the whole party must keep casting it. The effects are ambient and without particles (shown only as an icon in the corner), because the mod purged
 * vanilla particle spam; the look of the cast is the wind mantle of the sky dragons on the caster and a spirit aura on each friend. The caster is one of the allies and always comes
 * first, so a small party size cap can never leave the caster out of their own spell.
 */
public final class SkyEnchantSkill extends FtTreeSkill {
    public SkyEnchantSkill() { super(SkyRoute.ID, SkyRoute.ENCHANT, "item/sugar", SkyNumbers::fallback); }

    @Override
    protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
        int seconds = whole("seconds", lv), speedAmp = whole("speed_amp", lv), cap = whole("targets", lv);
        double radius = num("radius", lv);
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        int ticks = Math.max(20, seconds * 20);
        List<LivingEntity> friends = FtWorld.nearest(SkyAllies.around(p, p.position(), radius), p.position(), cap);
        for (LivingEntity e : friends) {
            e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 0, true, false, true));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, speedAmp, true, false, true));
            VfxSpawn.sendFollowing(level, VfxShape.SPIRIT_AURA, e, FtWorld.center(e), SkyRoute.SKY, 30, 0.8f);
        }
        VfxSpawn.sendFollowing(level, VfxShape.WIND_ZEPHYR, p, p.position().add(0, 1.0, 0), SkyRoute.WIND, 50, 1.0f);
        AbilityKit.sound(p, SoundEvents.BEACON_ACTIVATE, 0.6f, 1.7f);
        AbilityKit.sound(p, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        return true;
    }
}
