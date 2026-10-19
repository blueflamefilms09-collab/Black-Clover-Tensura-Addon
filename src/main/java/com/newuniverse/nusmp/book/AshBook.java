package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.newuniverse.nusmp.book.BookPage.*;

/**
 * Ash Magic (Zora Ideale, 0.34; rebuilt 1.93.0, see {@link AshSpells} and docs/attributes/ash_rebuild.md). Derived from Fire. The four original pages keep their ids and
 * order, three are appended (ash_shroud, ash_soldiers, cursed_ash). Revelation of the Cowardly arms ash traps that go off on whoever strikes you.
 */
public class AshBook extends ElementBook {
    public AshBook() {
        super(MagicType.ASH, 0xFFB0AAA2, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                starter("ash_bullets", "Ash Bullets", AshSpells::bullets),
                mid("ash_cloud", "Ash Cloud", AshSpells::cloud),
                zone("ash_absorbing_formation", "Ash Absorbing Formation", AshSpells::formation),
                signature("revelation_of_the_cowardly", "Revelation of the Cowardly", AshSpells::revelation).withCooldown(900),
                mid("ash_shroud", "Ash Shroud", AshSpells::shroud),
                zone("ash_soldiers", "Ash Soldiers", AshSpells::soldiers),
                mid("cursed_ash", "Cursed Ash", AshSpells::cursed)));
    }

    @Override
    public void castCircle(ServerPlayer p, float power) {
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.ASH_FX2, p, p.position().add(0, 0.05, 0), color, 22, Math.max(0.6f, power) * 0.8f);
    }

    @Override
    public void impact(ServerPlayer p, Vec3 at, float power) { vfx(p, VfxShape.ASH_FX3, at, at.add(0, 1, 0), 20, power * 0.4f); }

    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);
        if (owner instanceof ServerPlayer p && source.getEntity() instanceof LivingEntity attacker) AshSpells.trap(this, i, p, attacker);
        return true;
    }
}
