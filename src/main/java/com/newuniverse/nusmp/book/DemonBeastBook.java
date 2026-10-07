package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** 0.53 Demon Beast Magic: chimera and hydra beasts (claws and quills, roars and terror, bleeding bites, regrowth). */
public class DemonBeastBook extends GrimoireBook {
    static final int COLOR = 0xFFB02A6A;

    private final List<BookPage> pages = List.of(
            BookPage.starter("demon_beast_strike", "Underworld Beast Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.DEMON_BEAST_FX1, null, ElementBook.NONE)),
            BookPage.starter("chimera_claw_rend", "Chimera Claw Rend", DemonBeastArts::clawRend),
            BookPage.starter("demon_porcupine_stingers", "Demon Porcupine's Stingers", DemonBeastArts::stingers),
            BookPage.mid("chimeras_roar", "Chimera's Roar", DemonBeastArts::roar).withAnim("out"),
            BookPage.mid("chimera_fang_lunge", "Chimera Fang Lunge", DemonBeastArts::fangLunge),
            BookPage.mid("terror_howl", "Terror Howl", DemonBeastArts::terrorHowl),
            BookPage.mid("horned_spirit_rush", "Horned Spirit Beast Rush", DemonBeastArts.rush()),
            BookPage.zone("chimera_hunting_ground", "Chimera Hunting Ground", DemonBeastArts::huntingGround),
            BookPage.mid("demon_beast_mantle", "Demon Beast Mantle", DemonBeastArts::mantle),
            BookPage.mid("underworld_hide", "Underworld Hide", DemonBeastArts::hide),
            BookPage.signature("hydras_many_heads", "Hydra's Many Heads", DemonBeastArts::manyHeads).withAnim("signature"),
            BookPage.daily("hydras_regrowth", "Hydra's Regrowth", DemonBeastArts::regrowth).withCooldown(3600));

    public DemonBeastBook() { super(MagicType.DEMON_BEAST, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    // Mantle: your melee hits carry the bonus; Underworld Hide: thorns on whoever strikes you (same tags as the shared empower shape).
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        var tag = i.getOrCreateTag();
        if (source.getDirectEntity() == owner && owner.level().getGameTime() < tag.getLong("EmpowerUntil")) {
            amount.set(amount.get() + BalanceLaw.damage(target, tag.getFloat("EmpowerBonus"), masteryFrac(i)));
        }
        return true;
    }

    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);
        if (owner.level().getGameTime() < i.getOrCreateTag().getLong("ThornsUntil") && source.getEntity() instanceof LivingEntity att
                && att != owner && !source.is(net.minecraft.world.damagesource.DamageTypes.THORNS)) {
            att.hurt(owner.damageSources().thorns(owner), 2f);
        }
        return true;
    }
}
