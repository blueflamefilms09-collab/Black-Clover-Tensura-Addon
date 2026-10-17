package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.item.DemonSlayerSwordItem;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Why: three of the monsters answer a blow by what KIND of blow it was (the Black Goliath's hide shrugs off a plain swing, the Amphisbaena's red head wears
 * down under physical and anti-magic blows but not under spells, Udaeus's ribs block by the side the blow came from). The rules themselves are pure
 * ({@link HideState}, {@link HeadCut}, {@link RibGuard}); this class is the one place that looks at a game {@link DamageSource} and says which kind it is,
 * so the three monsters cannot disagree about what a "plain blow" or a "spell" is.
 */
final class DmBlows {
    private DmBlows() {}

    /** The living entity behind a blow (a player, a summon's master is NOT followed), or null for fire, poison, the void and the like. */
    static LivingEntity attacker(DamageSource s) { return s.getEntity() instanceof LivingEntity le ? le : null; }

    /** A plain blow: a swing of a hand or a weapon, or an arrow or other thrown missile, that is not magic. Everything else (a spell, a skill, a technique) is not plain. */
    static boolean plain(DamageSource s) {
        if (AntiMagic.isMagic(s)) return false;
        Entity direct = s.getDirectEntity(), source = s.getEntity();
        return source != null && direct == source || s.is(DamageTypeTags.IS_PROJECTILE);
    }

    /** How a blow counts against the red head: a spell counts for nothing, an anti-magic blow double, any other blow once. */
    static HeadCut.Kind headKind(DamageSource s) {
        if (AntiMagic.isMagic(s)) return HeadCut.Kind.MAGIC;
        if (s.getEntity() instanceof Player p && (p.getMainHandItem().getItem() instanceof DemonSlayerSwordItem || AntiMagic.lord(p).isPresent())) return HeadCut.Kind.ANTI;
        return HeadCut.Kind.PHYSICAL;
    }
}
