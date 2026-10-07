package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

import java.util.List;

/** Curse Magic: stacking Hex Marks, poison and wasting over time, a shrunken life pool, a cursed zone, and a curse that spreads on death (see CurseArts). */
public class CurseBook extends GrimoireBook {
    static final int COLOR = 0xFF9A2AFF;

    private final List<BookPage> pages = List.of(
            BookPage.starter("curse_strike", "Curse Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.CURSE_FX1, null, ElementBook.NONE)),
            // the real book (appended)
            BookPage.starter("aufwachen_dachs", "Aufwachen Dachs", CurseArts::dachs),
            BookPage.starter("withering_hex", "Withering Hex", CurseArts::witheringHex),
            BookPage.mid("sekke_poison_lizard", "Sekke Poison Lizard", CurseArts::lizard),
            BookPage.mid("ash_absorbing_formation", "Ash Absorbing Formation", CurseArts::ashFormation).withAnim("out"),
            BookPage.mid("joyful_destructive_ash", "Joyful Destructive Ash", CurseArts::joyfulAsh),
            BookPage.mid("curse_workers_neighbor", "Curse-Worker's Neighbor", CurseArts::neighbor),
            BookPage.mid("hex_aegis", "Hex Aegis", CurseArts::aegis).withAnim("up"),
            BookPage.mid("cursed_step", "Cursed Step", CurseArts::cursedStep),
            BookPage.zone("dwelling_poison_cloud", "Dwelling of the Poison Cloud", CurseArts::poisonCloud).withAnim("up"),
            BookPage.signature("ultra_giant_bull", "Ultra Giant Bull", CurseArts::bull).withAnim("signature"),
            BookPage.daily("curse_epidemic", "Curse Epidemic", CurseArts::epidemic).withCooldown(24000).withAnim("signature"));

    public CurseBook() { super(MagicType.CURSE, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.CURSE; }
}
