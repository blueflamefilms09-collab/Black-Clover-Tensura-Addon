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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: Ice-Make: Floor is the first technique of the STANDING branch and the cleanest example of what "static" Ice-Make is in the story: nothing is thrown, the ground itself is changed so that
 * the opponent loses their footing, and the mage can slide across it. A Minecraft server may not change blocks for this route (the owner's rule), so the floor is a circle on the clock
 * ({@link FtClock#zone}) that lasts the node's {@code life} (never over 10 seconds, so it can never be kept up for ever: the budget check also forces the cooldown to be longer than the life):
 * every half second it slows every fair foe standing in it and adds a few frozen ticks (the body's own cold), and gives Speed to the caster and the party inside, which is the slide.
 * It is placed where the caster looks (up to {@code reach} blocks, then down onto the ground), so it is both a trap and a lane. It does no damage by itself; its power is control, soft
 * control only (Slowness at most III, never a root), which is why it needs no hard-control gate. Each floor takes one of the caster's four construct slots ({@link IceConstructs}), is
 * refused before it costs anything when the slots are full, and the drawing is the mod's own Winter Field effect for exactly the floor's life.
 */
public final class IceFloorSkill extends FtTreeSkill {
    /** Ticks between two pulses of the floor, and how long its effects last (a little more than a pulse, so they never flicker). */
    public static final int PULSE = 10, EFFECT_TICKS = 24;

    public IceFloorSkill() { super(IceRoute.ID, IceRoute.FLOOR, "block/packed_ice", IceNumbers::fallback); }

    @Override
    protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
        double radius = num("radius", lv), reach = num("reach", lv);
        int life = IceNumbers.clampLife(whole("life", lv)), slowAmp = whole("slow_amp", lv), chill = whole("chill", lv), cap = whole("targets", lv);
        if (!IceConstructs.hasRoom(p)) {
            AbilityKit.say(p, "Four ice constructs already stand.");
            return false;
        }
        if (!pay(p, lv)) return false;
        IceConstructs.record(p, life);

        ServerLevel level = p.serverLevel();
        Vec3 c = FtWorld.groundAt(p, FtWorld.rayEnd(p, reach));
        AbilityKit.fx(level, VfxShape.ICE_FX2, c, c, IceRoute.ICE, life, (float) radius);
        FtWorld.soundAt(level, c, SoundEvents.POWDER_SNOW_PLACE, 1.0f, 0.8f);
        FtWorld.soundAt(level, c, SoundEvents.GLASS_PLACE, 0.8f, 1.4f);

        FtClock.zone(level, life, PULSE, age -> {
            if (!FtClock.present(p, level)) return;
            for (LivingEntity foe : FtWorld.nearest(FtWorld.hostile(p, AbilityKit.around(p, c, radius)), c, cap)) IceConstructs.chill(foe, EFFECT_TICKS, slowAmp, chill);
            for (ServerPlayer friend : IceConstructs.friendsAround(p, c, radius)) friend.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_TICKS, 0, true, false, true));
        });
        return true;
    }
}
