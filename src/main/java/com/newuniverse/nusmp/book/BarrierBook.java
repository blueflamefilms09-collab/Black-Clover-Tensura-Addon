package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

import java.util.List;

/** 0.53 Barrier Magic: STUB (replaced by the attribute's own implementation). */
public class BarrierBook extends GrimoireBook {
    static final int COLOR = 0xFF6AD0FF;

    private final List<BookPage> pages = List.of(
            BookPage.starter("barrier_strike", "Barrier Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.BARRIER_FX1, null, ElementBook.NONE)));

    public BarrierBook() { super(MagicType.BARRIER, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }
}
