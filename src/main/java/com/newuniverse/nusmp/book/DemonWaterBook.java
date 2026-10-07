package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/** 0.53 Demon Water Magic: STUB (replaced by the attribute's own implementation). */
public class DemonWaterBook extends GrimoireBook {
    static final int COLOR = 0xFF20D0A0;

    private static final VfxShape FX1 = VfxShape.DEMON_WATER_FX1, FX3 = VfxShape.DEMON_WATER_FX3;

    private final List<BookPage> pages = List.of(
            BookPage.starter("demon_water_strike", "Demon Tide Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, FX1, null, ElementBook.NONE)),
            // 0.54: the real book (wiki spells and the water-themed pages), appended after the first page
            BookPage.starter("tidal_push", "Demon Water Magic: Tidal Push", ElementBook.cone(6, 9, 0.5, FX3,
                    ElementBook.all(ElementBook.knock(2.0), DemonWaterArts.slow(60, 2), ElementBook.lift(0.3)))).withAnim("sweep"),
            BookPage.starter("dark_tide_wave", "Demon Water Magic: Dark Tide Wave", ElementBook.line(9, 16, 2.2, FX1,
                    ElementBook.all(ElementBook.knock(1.2), DemonWaterArts.slow(50, 1), DemonWaterArts.spirit(3.0)))),
            BookPage.mid("drowning_grasp", "Demon Water Magic: Drowning Grasp", DemonWaterArts::drowningGrasp),
            BookPage.mid("healing_mist", "Demon Water Magic: Healing Mist", DemonWaterArts::healingMist),
            BookPage.mid("serpent_surge", "Demon Water Magic: Serpent Surge", DemonWaterArts.surge()).withAnim("out"),
            BookPage.mid("abyssal_ward", "Demon Water Magic: Abyssal Ward", ElementBook.empower(240, 4f, true, FX3,
                    () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 240, 1), () -> new MobEffectInstance(MobEffects.WATER_BREATHING, 240, 0))),
            BookPage.zone("abyssal_whirlpool", "Demon Water Magic: Abyssal Whirlpool", DemonWaterArts::whirlpool),
            BookPage.signature("hydra_of_darkneros", "Demon Water Magic: Hydra of Darkneros", DemonWaterArts::hydra).withAnim("signature"),
            BookPage.daily("tide_rebirth", "Demon Water Magic: Tide Rebirth", DemonWaterArts::rebirth).withCooldown(12000));

    public DemonWaterBook() { super(MagicType.DEMON_WATER, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }
}
