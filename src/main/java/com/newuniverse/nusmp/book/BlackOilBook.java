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

/** Black Oil Magic: sticky oil that slows and soaks, curse-filled nails, tar pits, and fire that makes the oil explode. */
public class BlackOilBook extends GrimoireBook {
    static final int COLOR = 0xFF40E0A0;

    private final List<BookPage> pages = List.of(
            BookPage.starter("black_oil_strike", "Black Oil Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.BLACK_OIL_FX1, VfxShape.BLACK_OIL_FX3,
                    BlackOilArts.slick(60, 1))),
            // 0.55: the real book (appended)
            BookPage.starter("sticky_tar", "Sticky Tar", BlackOilArts::stickyTar),
            BookPage.mid("curse_filled_nails", "Black Oil Creation Magic: Curse-Filled Nails", BlackOilArts::cursedNails).withAnim("out"),
            BookPage.mid("oil_slick", "Oil Slick", BlackOilArts::oilSlick).withAnim("sweep"),
            BookPage.mid("ignite_oil", "Ignite Oil", BlackOilArts::igniteOil).withCooldown(200),
            BookPage.mid("oil_slide", "Oil Slide", BlackOilArts::oilSlide).withCooldown(200),
            BookPage.mid("oil_soaked_arms", "Oil-Soaked Arms", BlackOilArts::soakedArms).withCooldown(400),
            BookPage.mid("viscous_coat", "Viscous Coat", BlackOilArts::viscousCoat).withCooldown(260),
            BookPage.zone("tar_pit", "Tar Pit", BlackOilArts::tarPit),
            BookPage.zone("curse_candle_ritual_disk", "Black Oil Magic: Curse Candle Ritual Disk", BlackOilArts::candleDisk),
            BookPage.signature("black_oil_eruption", "Black Oil Eruption", BlackOilArts::eruption).withAnim("signature"),
            BookPage.daily("black_oil_sea", "Black Oil Sea", BlackOilArts::oilSea).withCooldown(24000));

    public BlackOilBook() { super(MagicType.BLACK_OIL, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** Oil-Soaked Arms: while it lasts, your melee hits hit harder and leave the target gummed and soaked. */
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (source.getDirectEntity() == owner && owner.level().getGameTime() < i.getOrCreateTag().getLong("OilArmsUntil")) {
            amount.set(amount.get() + BalanceLaw.damage(target, 3f, masteryFrac(i)));
            BlackOilArts.gum(target, 50, 1);
            BlackOilArts.oil(target, 160);
        }
        return true;
    }
}
