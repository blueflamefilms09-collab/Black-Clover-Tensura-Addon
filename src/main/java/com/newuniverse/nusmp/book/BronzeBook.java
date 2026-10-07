package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/** Bronze Magic: poison lizards, cannonballs, the winged bronze bicycle, bronze statues and shields. */
public class BronzeBook extends GrimoireBook {
    static final int COLOR = 0xFFD09A4A;

    private final List<BookPage> pages = List.of(
            BookPage.starter("bronze_strike", "Bronze Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.BRONZE_FX1, null, ElementBook.NONE)),
            // 0.55: the real book (appended)
            BookPage.starter("verdigris_spit", "Verdigris Spit", ElementBook.cone(5f, 6, 0.6, VfxShape.BRONZE_FX1, BronzeArts.VERDIGRIS)),
            BookPage.starter("bronze_shrapnel", "Bronze Shrapnel", ElementBook.volley(4, 3.5f, 1.5, false, VfxShape.BRONZE_FX1, BronzeArts.SHRAPNEL)),
            BookPage.mid("sekke_poison_lizard", "Bronze Curse Magic: Sekke Poison Lizard", BronzeArts::lizard),
            BookPage.mid("sekke_magnum_cannonball", "Bronze Creation Magic: Sekke Magnum Cannonball", BronzeArts::cannonball).withCooldown(260),
            BookPage.mid("bronze_bulwark", "Bronze Bulwark", ElementBook.wall(Blocks.WAXED_WEATHERED_COPPER.defaultBlockState())).withAnim("up"),
            BookPage.mid("bronze_statue", "Bronze Statue", BronzeArts::statue).withCooldown(360),
            BookPage.mid("sekke_shooting_star", "Bronze Creation Magic: Sekke Shooting Star", BronzeArts::shootingStar).withCooldown(500).withAnim("up"),
            BookPage.zone("statue_garrison", "Statue Garrison", BronzeArts::sentinels),
            BookPage.zone("molten_bronze", "Molten Bronze", BronzeArts::molten).withAnim("slam"),
            BookPage.signature("super_sekke_magnum_cannonball", "Super Sekke Magnum Cannonball", BronzeArts::superCannonball).withAnim("signature"),
            BookPage.daily("bronze_colossus", "Bronze Colossus", BronzeArts::colossus).withCooldown(24000));

    public BronzeBook() { super(MagicType.BRONZE, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }
}
