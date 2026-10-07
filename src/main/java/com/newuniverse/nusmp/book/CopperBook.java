package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

import java.util.List;

/** 0.53 Copper Magic: STUB (replaced by the attribute's own implementation). */
public class CopperBook extends GrimoireBook {
    static final int COLOR = 0xFFFF9A4A;

    private final List<BookPage> pages = List.of(
            BookPage.starter("copper_strike", "Copper Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.COPPER_FX1, null, ElementBook.NONE)));

    public CopperBook() { super(MagicType.COPPER, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }
}
