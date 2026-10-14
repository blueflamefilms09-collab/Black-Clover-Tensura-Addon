package com.newuniverse.nusmp.relics.pair;

import com.newuniverse.nusmp.relics.RelicDef;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a pair's off-hand half adds to its second blow (a flame or a chill, a hook). A wave registers it once, in its {@code hooks()}, by the pair's
 * base id; a pair without an entry simply strikes the second blow plain. The effect runs after the blow was dealt, with the off-hand half's def.
 */
public final class PairEffects {
    private PairEffects() {}

    /** The extra effect of one off-hand blow. */
    @FunctionalInterface
    public interface OffHandEffect {
        void strike(ServerPlayer wielder, LivingEntity victim, RelicDef offHalf);
    }

    private static final Map<String, OffHandEffect> EFFECTS = new ConcurrentHashMap<>();

    public static void onOffHand(String baseId, OffHandEffect effect) {
        if (baseId != null && effect != null) EFFECTS.put(baseId, effect);
    }

    static OffHandEffect of(String baseId) { return EFFECTS.get(baseId); }
}
