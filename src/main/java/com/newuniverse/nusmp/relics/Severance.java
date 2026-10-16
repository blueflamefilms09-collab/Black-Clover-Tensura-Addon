package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.entity.zogratis.LuciusGodEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the god-tier Lucius can sever a player from their Djinn and their Black Clover spirits for twelve seconds. The Djinn engine, the
 * Spirit Lord skill, the spirit bond and the spirit companion are in different packages, so each asks this one function. Nothing is
 * destroyed or lost: a severed player keeps the bond, the trust, the spirit and the skill; they simply do not answer until the mob
 * effect {@code nusmp:severed} ends (by itself, on death, with milk, or when the boss clears it).
 */
public final class Severance {
    private Severance() {}

    /** Is this entity cut off from its Djinn and spirits right now? Cheap, safe on both sides. */
    public static boolean isSevered(LivingEntity e) {
        return e != null && e.hasEffect(LuciusGodEffects.SEVERED);
    }
}
