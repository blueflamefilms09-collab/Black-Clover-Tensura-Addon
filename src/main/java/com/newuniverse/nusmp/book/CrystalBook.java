package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

import java.util.List;

/** 0.53 Crystal Magic: STUB (replaced by the attribute's own implementation). */
public class CrystalBook extends GrimoireBook {
    static final int COLOR = 0xFFA0E0FF;

    private final List<BookPage> pages = List.of(
            BookPage.starter("crystal_strike", "Crystal Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.CRYSTAL_FX1, null, ElementBook.NONE)));

    public CrystalBook() { super(MagicType.CRYSTAL, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }
}
