package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

import java.util.List;

/** 0.53 Glass Magic: STUB (replaced by the attribute's own implementation). */
public class GlassBook extends GrimoireBook {
    static final int COLOR = 0xFFE0FFFF;

    private final List<BookPage> pages = List.of(
            BookPage.starter("glass_strike", "Glass Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.GLASS_FX1, null, ElementBook.NONE)));

    public GlassBook() { super(MagicType.GLASS, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }
}
