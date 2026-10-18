package com.newuniverse.nusmp.heroweapon.weapon.pierce;

import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: a technique that strikes one foe several times in the same instant (five lances, seven arrows, a lane and its burst) cannot simply call the hit five
 * times: the game gives a struck creature a short immunity, so a second blow in the same tick is cut down to the difference with the first, and the foe takes
 * the strongest blow instead of the sum. The balance budget counts the SUM per target, so the pieces are collected here and each foe is struck once with
 * the total, through the balance law and only if it is a foe the wielder may strike. Insertion order is kept so the nearest foes are struck first when the
 * cap on targets applies.
 */
public final class Hits {
    private final Map<LivingEntity, Float> raw = new LinkedHashMap<>();

    /** Adds {@code r} raw damage for {@code target}; several pieces for one target add up. Returns this for chaining. */
    public Hits add(LivingEntity target, float r) {
        if (target != null && r > 0f) raw.merge(target, r, Float::sum);
        return this;
    }

    /** Strikes each collected foe once with its total, at most {@code max} foes; returns the foes the law let through, in order. */
    public List<LivingEntity> apply(HeroCtx c, int max) {
        List<LivingEntity> struck = new ArrayList<>();
        int cap = Math.max(0, Math.min(max, Foes.MAX));
        for (Map.Entry<LivingEntity, Float> e : raw.entrySet()) {
            if (struck.size() >= cap) break;
            if (Foes.hit(c, e.getKey(), e.getValue())) struck.add(e.getKey());
        }
        return struck;
    }
}
