package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

/**
 * Why: the seven Millennium Items are one design (the Puzzle's spirit, the Eye, the Ring, the Rod, the Key, the Scale and the Necklace), and wearing several of them together is the point.
 * The set bonus is read off what the player wears and works for them ({@link Relics#loadout}: the main hand, the armour slots and the Curios slots, attuned where a relic needs it): the
 * Millennium Items need no attunement (so all seven can be worn at once, which the cap of three attuned relics would forbid), and each is one of a kind in the world through the Order's ledger.
 * Nothing is stored: the count is asked when it is needed (once a second for the ambient gifts, once per act for the power factor).
 */
final class Wave101Set {
    private Wave101Set() {}

    /** How many different Millennium Items are worn and working for this player right now (0 to 7). */
    static int count(ServerPlayer p) {
        if (p == null) return 0;
        java.util.List<Relics.Worn> active = Relics.loadout(p).active();
        if (active.isEmpty()) return 0;                                              // 1.112.0 (lag audit): most players wear none of the seven: no set, no allocation
        int n = 0;
        String first = null, second = null, third = null;                           // at most seven different ids: a handful of comparisons instead of a HashSet per question
        java.util.Set<String> more = null;
        for (Relics.Worn w : active) {
            String id = w.def().id();
            if (!Wave101Rules.inSet(id)) continue;
            if (id.equals(first) || id.equals(second) || id.equals(third) || (more != null && more.contains(id))) continue;
            if (first == null) first = id;
            else if (second == null) second = id;
            else if (third == null) third = id;
            else (more == null ? more = new HashSet<>() : more).add(id);
            n++;
        }
        return n;
    }

    /** The factor of the damage and healing of a Millennium act for this player: 1.0 alone, up to 1.3 with the whole set. */
    static double power(ServerPlayer p) { return Wave101Rules.power(count(p)); }

    /** Once a second: the ambient gifts of the set (resistance from three pieces, regeneration from four, strength from five, speed from six, all stronger with seven). */
    static void tick(ServerPlayer p) {
        int n = count(p);
        if (n < Wave101Rules.RESIST_AT) return;
        give(p, "minecraft:resistance", Wave101Rules.resistAmp(n));
        give(p, "minecraft:regeneration", Wave101Rules.regenAmp(n));
        give(p, "minecraft:strength", Wave101Rules.strengthAmp(n));
        give(p, "minecraft:speed", Wave101Rules.speedAmp(n));
    }

    private static void give(ServerPlayer p, String effect, int amp) {
        if (amp >= 0) Fx.addEffect(p, effect, Wave101Rules.EFFECT_TICKS, amp);
    }
}
