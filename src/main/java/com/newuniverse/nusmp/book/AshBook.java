package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

import static com.newuniverse.nusmp.book.BookPage.*;

/** Ash Magic (Zora Ideale, 0.34). Derived from Fire. Revelation of the Cowardly arms ash traps that go off on whoever strikes you. */
public class AshBook extends ElementBook {
    public AshBook() {
        super(MagicType.ASH, 0xFFB0AAA2, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                starter("ash_bullets", "Ash Bullets", WikiSpells::ashBullets),
                mid("ash_cloud", "Ash Cloud", WikiSpells::ashCloud),
                zone("ash_absorbing_formation", "Ash Absorbing Formation", WikiSpells::ashFormation),
                signature("revelation_of_the_cowardly", "Revelation of the Cowardly", WikiSpells::revelationOfTheCowardly).withCooldown(900)));
    }

    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);
        if (owner instanceof ServerPlayer p && source.getEntity() instanceof LivingEntity attacker) WikiSpells.ashTrap(this, i, p, attacker);
        return true;
    }
}
