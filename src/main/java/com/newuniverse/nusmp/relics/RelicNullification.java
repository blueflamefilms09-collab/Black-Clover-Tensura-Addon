package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.entity.zogratis.LuciusGodEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the god-tier Lucius can de-attune a player's relics for ten seconds, and the relic systems of this mod are many (the relic waves,
 * the sockets, the Treasury relics of the two Dungeon Relic Curios slots), so the switch must live in the few places that all of them
 * ask, not in each relic. The question is this one function. It is asked by {@link Relics#loadout} and {@link Relics#loadoutFresh}
 * (everything the waves, the sockets and the passive pass read), by {@code dungeon.nexus.relic.Worn.mask/stack} (the Treasury relics)
 * and by {@link RelicEngine#activate} (every act, held or socketed). Nothing is unequipped, dropped or damaged: the items stay where
 * they are and simply stop counting; the mob effect {@code nusmp:relic_nullified} ends by itself, on death, with milk, and when the boss
 * clears it. Not covered, honestly: the Tensura engraving enchantments on armour and weapons are Tensura's own and are read by Tensura.
 */
public final class RelicNullification {
    private RelicNullification() {}

    /** Is this entity's relic power silenced right now? Cheap (a map lookup), safe on both sides and before the registries are bound. */
    public static boolean isNullified(LivingEntity e) {
        return e != null && e.hasEffect(LuciusGodEffects.RELIC_NULLIFIED);
    }
}
