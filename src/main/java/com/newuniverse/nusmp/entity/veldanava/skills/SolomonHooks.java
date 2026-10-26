package com.newuniverse.nusmp.entity.veldanava.skills;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the few places of the older code that Solomon changes (the grimoire's chant time and price, the anti-magic zone, the Magicule drains) each call one of these one-liners, so the older files only
 * gain a line and every rule lives here. Every method returns its argument unchanged for anyone who is not a Solomon owner with the toggle on (a read of the player's mask: one int) and not inside a standing Realm
 * (an empty registry returns on its first line), and when the Veldanava skills are switched off in the server config. Only a {@link ServerPlayer} is ever changed, so the client's own
 * reading (a price in a skill list) is the unchanged one and the server charges the real one.
 */
public final class SolomonHooks {
    private SolomonHooks() {}

    private static boolean owns(LivingEntity e, int mode) {
        return e instanceof ServerPlayer p && JudgmentState.onFor(p, SolomonRules.CODE, mode) && JudgmentConfig.enabled();
    }

    /** True when the entity is an owner of Infinite Grimoire: anti-magic zones and mana drains do nothing to it. */
    public static boolean antiMagicImmune(LivingEntity e) { return owns(e, 0); }

    /** The chant of a grimoire cast in ticks: one tick inside an allied Realm of Magic (any caster), shorter with Infinite Grimoire, else {@code base}. */
    public static int chantTicks(LivingEntity e, int base) {
        if (!(e instanceof ServerPlayer p) || !JudgmentConfig.enabled()) return base;
        boolean realm = SolomonState.realmCovers(p), infinite = JudgmentState.onFor(p, SolomonRules.CODE, 0);
        return realm || infinite ? SolomonRules.chantTicks(base, infinite, infinite && JudgmentState.mastered(p), realm) : base;
    }

    /** The Magicule a grimoire spell costs: nothing inside an allied Realm (any caster), half with a mastered Infinite Grimoire, else {@code base}. */
    public static double spellCost(LivingEntity e, double base) {
        if (!(e instanceof ServerPlayer p) || !JudgmentConfig.enabled()) return base;
        boolean realm = SolomonState.realmCovers(p), infinite = JudgmentState.onFor(p, SolomonRules.CODE, 0);
        return realm || infinite ? SolomonRules.spellCost(base, infinite && JudgmentState.mastered(p), realm) : base;
    }
}
