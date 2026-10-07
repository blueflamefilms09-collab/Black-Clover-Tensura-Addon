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

/** Ice Wedge Magic: wedges that slow, pin and split, up to World End Martyr and World End Church (see IceWedgeArts). */
public class IceWedgeBook extends GrimoireBook {
    static final int COLOR = 0xFFC0F0FF;

    private final List<BookPage> pages = List.of(
            BookPage.starter("ice_wedge_strike", "Wedge Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.ICE_WEDGE_FX1, null, IceWedgeArts.chilled(60, 1, 40))),
            // 0.54: the real book (appended)
            BookPage.starter("wedge_volley", "Ice Wedge: Wedge Volley", ElementBook.volley(5, 4f, 1.5, false, VfxShape.ICE_WEDGE_FX1, IceWedgeArts.chilled(50, 1, 30))),
            BookPage.starter("wedge_driver", "Ice Wedge: Wedge Driver", IceWedgeArts::driver).withAnim("slam"),
            BookPage.mid("wedge_fan", "Ice Wedge: Wedge Fan", ElementBook.cone(9f, 9, 0.5, VfxShape.ICE_WEDGE_FX1, IceWedgeArts.chilled(90, 2, 70))).withAnim("sweep"),
            BookPage.mid("splitting_wedge", "Ice Wedge: Splitting Wedge", IceWedgeArts::split),
            BookPage.mid("frost_brand", "Ice Wedge: Frost Brand", IceWedgeArts::brand),
            BookPage.mid("wedge_aegis", "Ice Wedge: Wedge Aegis", IceWedgeArts::aegis).withAnim("up"),
            BookPage.mid("wedge_wall", "Ice Wedge: Wedge Wall", IceWedgeArts::wall).withAnim("up"),
            BookPage.mid("wedge_step", "Ice Wedge: Wedge Step", IceWedgeArts::step),
            BookPage.zone("wedge_field", "Ice Wedge: Wedge Field", IceWedgeArts::field).withAnim("up"),
            BookPage.signature("world_end_martyr", "Ice Wedge Magic: World End Martyr", IceWedgeArts::martyr).withAnim("signature"),
            BookPage.daily("world_end_church", "Ice Wedge Magic: World End Church", IceWedgeArts::church).withCooldown(24000).withAnim("signature"));

    public IceWedgeBook() { super(MagicType.ICE_WEDGE, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.ICE_ELEMENTAL; }

    /** Frost Brand: while it lasts your melee hits deal extra damage, chill the target and bleed it. */
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        var tag = i.getOrCreateTag();
        if (source.getDirectEntity() == owner && owner.level().getGameTime() < tag.getLong("WedgeBrandUntil") && IceWedgeArts.valid(target)) {
            amount.set(amount.get() + BalanceLaw.damage(target, tag.getFloat("WedgeBrandBonus"), masteryFrac(i)));
            IceWedgeArts.chill(target, 60, 1, 60);
            com.newuniverse.nusmp.antimagic.Nullification.bleed(target, 0.01);
        }
        return true;
    }

    /** Wedge Aegis: whoever strikes you is slowed and frosted. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);
        if (owner.level().getGameTime() < i.getOrCreateTag().getLong("WedgeAegisUntil") && source.getEntity() instanceof LivingEntity att
                && att != owner && IceWedgeArts.valid(att)) IceWedgeArts.chill(att, 60, 2, 60);
        return true;
    }
}
