package com.newuniverse.nusmp.shadow;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: the three quick Unite pages (Canis, Gallus, Canis x Felis) only hand out potion effects, so nothing tells the armour overlay or the
 * abilities that the player has just united. The owner wants those spells left alone, so this server tick helper watches players for the exact
 * effect set one of those pages applies the moment it applies it ({@link UniteSignature}), and starts the matching mode (stats of the armour,
 * the overlay, Canis's pack). It also runs the periodic work of every running mode, and cleans up when a mode runs out.
 *
 * <p>Cost: one effect lookup per player per tick unless a fresh Speed effect is present.
 */
public final class UniteWatcher {
    private UniteWatcher() {}

    public static void onServerTick(ServerTickEvent.Post e) {
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            if (!p.isAlive()) continue;
            long now = p.level().getGameTime();
            detectOldPage(p, now);
            UniteSlot slot = UniteState.slot(p);
            if (slot.kind() < 0) continue;
            if (slot.expired(now)) { UniteState.stop(p); continue; }
            UniteKind kind = slot.running(now);
            if (kind == null) { UniteState.stop(p); continue; }
            if (now % 20 == 0) UniteState.refresh(p);
            boolean old = UniteState.fromOldPage(p);
            UniteAbilities.tick(p, kind, now, old);
            if (kind == UniteKind.FELIS && now % 10 == 0) {
                UniteAbilities.nightEyes(p);
                UniteAbilities.loner(p, kind);
                if (ShadowConfig.felisSight()) ShadowSight.update(p, kind.sightRange);
            }
        }
    }

    private static void detectOldPage(ServerPlayer p, long now) {
        MobEffectInstance speed = p.getEffect(MobEffects.MOVEMENT_SPEED);
        if (speed == null || speed.getDuration() < UniteSignature.FRESH_TICKS) return;
        Map<String, int[]> fx = new HashMap<>(8);
        put(fx, "speed", speed);
        put(fx, "strength", p.getEffect(MobEffects.DAMAGE_BOOST));
        put(fx, "jump", p.getEffect(MobEffects.JUMP));
        put(fx, "slow_falling", p.getEffect(MobEffects.SLOW_FALLING));
        put(fx, "resistance", p.getEffect(MobEffects.DAMAGE_RESISTANCE));
        UniteKind kind = UniteSignature.kind(UniteSignature.detect(fx));
        if (kind == null) return;
        UniteSlot slot = UniteState.slot(p);
        if (slot.running(now) == kind && slot.ticksLeft(now) > UniteKind.TICKS - 40) return;     // this very cast was already seen
        UniteState.start(p, kind, UniteKind.TICKS, true);
        UniteAbilities.onStart(p, kind, true);
    }

    private static void put(Map<String, int[]> fx, String id, MobEffectInstance inst) {
        if (inst != null) fx.put(id, new int[]{inst.getAmplifier(), inst.getDuration()});
    }
}
