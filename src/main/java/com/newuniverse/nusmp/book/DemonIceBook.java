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

/** Demon Ice Magic: a sinister ice that slows, freezes and shatters (see DemonIceArts). */
public class DemonIceBook extends GrimoireBook {
    static final int COLOR = 0xFF3A6AFF;

    private final List<BookPage> pages = List.of(
            BookPage.starter("demon_ice_strike", "Demon Frost Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.DEMON_ICE_FX1, null, DemonIceArts.chilled(60, 1, 60))),
            // 0.54: the real book (appended)
            BookPage.starter("demon_ice_shards", "Demon Ice: Frost Shards", ElementBook.volley(7, 3.5f, 1.5, false, VfxShape.DEMON_ICE_FX1, DemonIceArts.chilled(40, 1, 30))),
            BookPage.starter("demon_ice_lance", "Demon Ice: Piercing Lance", DemonIceArts::lance),
            BookPage.mid("demon_ice_wave", "Demon Ice: Frostbite Wave", DemonIceArts::wave).withAnim("sweep"),
            BookPage.mid("demon_ice_soul", "Demon Ice: Frozen Soul", DemonIceArts::frozenSoul),
            BookPage.mid("demon_ice_shatter", "Demon Ice: Shatter", DemonIceArts::shatter).withAnim("slam"),
            BookPage.mid("demon_ice_wall", "Demon Ice: Glacier Wall", DemonIceArts::wall).withAnim("up"),
            BookPage.mid("demon_ice_slide", "Demon Ice: Glacier Slide", DemonIceArts::slide),
            BookPage.mid("demon_ice_body", "Demon Ice: Frost Body", DemonIceArts::body),
            BookPage.zone("demon_ice_blizzard", "Demon Ice: Blizzard", DemonIceArts::blizzard).withAnim("up"),
            BookPage.signature("demon_ice_zero", "Demon Ice: Absolute Zero", DemonIceArts::absoluteZero).withAnim("signature"),
            BookPage.daily("demon_ice_winter", "Demon Ice: Eternal Winter", DemonIceArts::winter).withCooldown(24000).withAnim("signature"));

    public DemonIceBook() { super(MagicType.DEMON_ICE, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.ICE_ELEMENTAL; }

    /** Frost Body: while it lasts your melee hits deal extra damage and chill the target. */
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        var tag = i.getOrCreateTag();
        if (source.getDirectEntity() == owner && owner.level().getGameTime() < tag.getLong("IceBodyUntil")) {
            amount.set(amount.get() + BalanceLaw.damage(target, tag.getFloat("IceBodyBonus"), masteryFrac(i)));
            DemonIceArts.chill(target, 60, 1, 60);
        }
        return true;
    }

    /** Frost Body: whoever strikes you is chilled. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);
        if (owner.level().getGameTime() < i.getOrCreateTag().getLong("IceBodyUntil") && source.getEntity() instanceof LivingEntity att
                && att != owner && DemonIceArts.valid(att)) DemonIceArts.chill(att, 60, 1, 40);
        return true;
    }
}
