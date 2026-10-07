package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

import java.util.List;

/** 0.53 Curse-Warding Magic: STUB (replaced by the attribute's own implementation). */
public class CurseWardingBook extends GrimoireBook {
    static final int COLOR = 0xFFC08AFF;

    private final List<BookPage> pages = List.of(
            BookPage.starter("curse_warding_strike", "Ward Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.CURSE_WARDING_FX1, null, ElementBook.NONE)));

    public CurseWardingBook() { super(MagicType.CURSE_WARDING, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }
}
