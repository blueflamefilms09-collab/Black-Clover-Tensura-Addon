package com.newuniverse.nusmp.dungeon.nexus.relic;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.item.DemonSlayerSwordItem;
import io.github.manasmods.tensura.data.TensuraTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

/** What kind of damage a hit is, for the relics: magic (a spell, a skill), anti-magic, or physical (a blow, an arrow). */
final class RelicRules {
    private RelicRules() {}

    /** Spell damage: vanilla magic, anything the anti-magic code counts as magic, or any Tensura damage type that is not physical. */
    static boolean magic(DamageSource s) {
        if (s.is(DamageTypes.MAGIC) || s.is(DamageTypes.INDIRECT_MAGIC) || AntiMagic.isMagic(s)) return true;
        var key = s.typeHolder().unwrapKey();
        return key.isPresent() && "tensura".equals(key.get().location().getNamespace()) && !tensuraPhysical(s);
    }

    /**
     * A plain physical blow: a melee hit, a missile, or a Tensura physical damage type. A spell is not, and neither is the anti-magic of the
     * Demon-Slayer's Conceptual Severance, so the Mantle's curse never blunts those.
     */
    static boolean physical(DamageSource s) {
        if (s.is(DamageTypes.MAGIC) || s.is(DamageTypes.INDIRECT_MAGIC) || s.is(DemonSlayerSwordItem.SEVERANCE) || AntiMagic.isMagic(s)) return false;
        if (s.is(DamageTypes.PLAYER_ATTACK) || s.is(DamageTypes.MOB_ATTACK) || s.is(DamageTypes.MOB_ATTACK_NO_AGGRO) || s.is(DamageTypes.ARROW)
                || s.is(DamageTypes.TRIDENT) || s.is(DamageTypes.THROWN) || s.is(DamageTypes.MOB_PROJECTILE)) return true;
        return tensuraPhysical(s);
    }

    private static boolean tensuraPhysical(DamageSource s) {
        try {
            return s.is(TensuraTags.DamageTypes.IS_PHYSICAL);
        } catch (RuntimeException | LinkageError noTensuraTags) {
            return false;
        }
    }
}
