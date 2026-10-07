package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

import java.util.List;

/** 0.53 Iron Magic: STUB (replaced by the attribute's own implementation). */
public class IronBook extends GrimoireBook {
    static final int COLOR = 0xFFB0B8C8;

    private final List<BookPage> pages = List.of(
            BookPage.starter("iron_strike", "Iron Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.IRON_FX1, null, ElementBook.NONE)));

    public IronBook() { super(MagicType.IRON, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }
}
