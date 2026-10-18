package com.newuniverse.nusmp.skilltree.ft.sky;

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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: Sky Dragon's Wave Wind is, in the story, a great tornado made by one swing of the hand, and what makes it different from the Roar is that it TRAVELS: it is a place that moves, not a
 * cone that is over at once. The server has no tornado entity (the owner's rule allows few spawned entities and none that outlive 10 seconds, and a moving hit volume needs none), so the
 * tornado is a position on the clock ({@link FtClock#zone}): every two ticks it is moved a little further along the caster's flat gaze, as far as there is room (one look along the
 * path stops it at the first wall, so it never passes through blocks), and the foes inside its circle are hit. After it stops it lingers for a second, which is what lets a slow or
 * cornered foe be hit twice. The damage is split into small hits on purpose: a target is hit at most {@code hits} times, never twice within the length of the creature's own invulnerability
 * (10 ticks), so no hit is wasted on a protected target, and at most {@code targets} creatures are ever hit, so a crowd cannot multiply it. It slows what it touches (soft control, so
 * no hard-control gate). The effect is drawn as a lane of wind plus small tornadoes left along the path, and ends by itself: if the caster dies or leaves, the next step sees it and stops.
 */
public final class SkyWaveWindSkill extends FtTreeSkill {
    /** Blocks the tornado moves per tick, how long it lingers where it stops, and the ticks between two hits of one target. */
    public static final double SPEED = 0.45;
    public static final int LINGER = 20, HIT_GAP = 10, STEP = 2;

    public SkyWaveWindSkill() { super(SkyRoute.ID, SkyRoute.WAVE_WIND, "item/wind_charge", SkyNumbers::fallback); }

    @Override
    protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
        double radius = num("radius", lv), dist = num("distance", lv);
        float raw = (float) num("damage", lv);
        int maxHits = whole("hits", lv), cap = whole("targets", lv);
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        double mastery = FtWorld.masteryFrac(instance);
        Vec3 dir = FtWorld.flatLook(p);
        Vec3 origin = p.position().add(dir.scale(1.2)), eyeLine = origin.add(0, 1.0, 0);
        BlockHitResult wall = level.clip(new ClipContext(eyeLine, eyeLine.add(dir.scale(dist)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double reach = wall.getType() == HitResult.Type.MISS ? dist : Math.max(0.0, wall.getLocation().distanceTo(eyeLine) - 0.8);
        int travel = (int) Math.ceil(reach / SPEED);

        AbilityKit.fx(level, VfxShape.WIND_GALE, eyeLine, eyeLine.add(dir.scale(reach)), SkyRoute.WIND, travel + 6, 1.3f);
        AbilityKit.sound(p, SoundEvents.BREEZE_WHIRL, 0.9f, 1.0f);
        AbilityKit.sound(p, SoundEvents.ENDER_DRAGON_FLAP, 0.7f, 0.9f);

        Map<UUID, int[]> struck = new HashMap<>();                       // creature -> {hits so far, age of the last hit}
        FtClock.zone(level, travel + LINGER + 1, STEP, age -> {
            if (!FtClock.present(p, level)) return;
            Vec3 pos = origin.add(dir.scale(Math.min(reach, SPEED * age)));
            Vec3 mid = pos.add(0, 1.0, 0);
            if (age % 6 == 0 && age <= travel + LINGER - 6) AbilityKit.fx(level, VfxShape.WIND_TORNADO, pos, pos.add(0, 1.0, 0), SkyRoute.WIND, 12, (float) radius * 0.6f);
            for (LivingEntity foe : FtWorld.hostile(p, AbilityKit.around(p, mid, radius + 1.0))) {
                int[] s = struck.get(foe.getUUID());
                if (s == null) {
                    if (struck.size() >= cap) continue;
                    s = new int[] {0, -1000};
                    struck.put(foe.getUUID(), s);
                }
                if (s[0] >= maxHits || age - s[1] < HIT_GAP) continue;
                s[0]++;
                s[1] = age;
                AbilityKit.hit(p, foe, raw, mastery);
                foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
            }
        });
        return true;
    }
}
