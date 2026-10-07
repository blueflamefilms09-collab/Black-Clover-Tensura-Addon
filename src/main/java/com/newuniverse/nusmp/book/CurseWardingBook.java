package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

import java.util.List;

/** Curse-Warding Magic: cleanses and moves curses, wards that reflect them, the thrall curse, Exploding Life, Decaying World. */
public class CurseWardingBook extends GrimoireBook {
    static final int COLOR = 0xFFC08AFF;

    private final List<BookPage> pages = List.of(
            BookPage.starter("curse_warding_strike", "Ward Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.CURSE_WARDING_FX1, null, ElementBook.NONE)),
            // 0.55: the real book (appended)
            BookPage.starter("hex_eater", "Hex Eater", CurseWardingArts::hexEater),
            BookPage.mid("curse_cleanse", "Curse Cleanse", CurseWardingArts::cleanseAll),
            BookPage.mid("rune_step", "Rune Step", CurseWardingArts::runeStep).withCooldown(200),
            BookPage.mid("malevolent_femcantation", "Malevolent Femcantation", CurseWardingArts::malevolent),
            BookPage.mid("curse_transfer", "Curse Transfer", CurseWardingArts::transfer),
            BookPage.mid("mirror_ward", "Mirror Ward", CurseWardingArts::mirrorWard).withCooldown(400),
            BookPage.mid("unbound_slave", "Unbound Slave", CurseWardingArts::unboundSlave).withCooldown(300),
            BookPage.zone("ward_circle", "Ward Circle", CurseWardingArts::wardCircle),
            BookPage.zone("decaying_world", "Decaying World", CurseWardingArts::decayingWorld).withCooldown(500),
            BookPage.signature("exploding_life", "Exploding Life", CurseWardingArts::explodingLife).withAnim("signature"),
            BookPage.daily("undying_curse", "Undying Curse", CurseWardingArts::undying).withCooldown(24000));

    public CurseWardingBook() { super(MagicType.CURSE_WARDING, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }
}
