package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.prop.EarthConstruct;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.world.level.block.Blocks;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.newuniverse.nusmp.book.BookPage.*;
import static com.newuniverse.nusmp.book.EarthKit.*;

/**
 * 1.93.0 The grimoires of Sandstone, Rock, Red Ochre, Stone and Mineral Magic, rebuilt (Earth Magic keeps its own book, {@link EarthBook}, and gets its new pages there).
 * Every book keeps the five page ids the generic wiki book had (attribute_bolt, attribute_cut, attribute_field, attribute_domain, attribute_guard: save keys, order and
 * mastery bits do not move) and re-implements them in the magic's own language; new pages are APPENDED after them.
 *
 * <pre>
 *            bolt (starter)      cut (mid)            field (zone)          domain (signature)       guard (mid)         appended pages
 *  Sandstone sand bolt, blind    slab eruption, slow  quicksand field       sandstorm, blind         Dune Plate armour   Variable Hardness Prison (canon), Sandstone Slabs wall, Sandstone Sphinx
 *  Rock      thrown boulder      crag eruption, lift  rockfall field        quake, weakness          Boulder armour      Boulder Titan, Boulder Barrage
 *  Red Ochre pigment bolt, weak  brush stroke, mark   pigment pool          war-paint burst, mark    Clay armour         Ochre Totem, War Paint
 *  Stone     stone bolt, slow    slab eruption        rising slabs field    shockwave                Masonry armour      Stone Sentinel, Stone Wall
 *  Mineral   crystal lance       crystal row, pierce  crystal garden        resonance burst          Crystal armour      Crystal Spire, Lance Volley
 * </pre>
 * Canon: only the Variable Hardness Prison of Sandstone Magic is a named spell the anime / wiki gives; the rest are the nearest honest versions (see docs/attributes/*_rebuild.md).
 */
public final class EarthFamilyBooks {
    private EarthFamilyBooks() {}

    private static final Set<MagicType> OWN = EnumSet.of(MagicType.SANDSTONE, MagicType.ROCK, MagicType.RED_OCHRE, MagicType.STONE, MagicType.MINERAL);

    public static boolean owns(MagicType magic) { return OWN.contains(magic); }

    public static GrimoireBook book(MagicType magic) {
        String n = magic.displayName.replace(" Magic", "");
        int color = WikiBooks.color(magic);
        List<BookPage> pages = switch (magic) {
            case SANDSTONE -> List.of(
                    starter("attribute_bolt", n + " Bolt", ElementBook.bolt(9, 1.8, 0.55, 24, false, 1.4, VfxShape.WIKI_MAGIC_CAST, VfxShape.WIKI_MAGIC_BURST, all(knock(0.4), blind(40)))),
                    mid("attribute_cut", n + " Manifestation", eruption(8, 9f, 0.35, 1.3, slow(50, 1))),
                    zone("attribute_field", n + " Field", field(5, 4.5, 80, 16, true, all(slow(40, 2), blind(25)))),
                    signature("attribute_domain", n + " Domain", nova(15, 7, false, 12, all(knock(0.8), blind(70)), true)).withCooldown(900),
                    mid("attribute_guard", n + " Guard", guard(180, 1, 3f, 0.06)),
                    zone("sandstone_prison", "Variable Hardness Prison", EarthFamilyArts::prison).withCooldown(400),
                    mid("sandstone_slabs", "Sandstone Slabs", wall(Blocks.SANDSTONE.defaultBlockState(), 5, 3, 160)),
                    mid("sandstone_sphinx", "Sandstone Sphinx", guardian(EarthConstruct.SPHINX, 500, 1)));
            case ROCK -> List.of(
                    starter("attribute_bolt", n + " Bolt", thrown(EarthConstruct.BOULDER, 1, 0, 1.45, 0.9f, 50)),
                    mid("attribute_cut", n + " Manifestation", eruption(8, 11f, 0.6, 1.3, knock(0.3))),
                    zone("attribute_field", n + " Field", field(6, 5, 80, 14, true, all(slow(40, 1), knock(0.25)))),
                    signature("attribute_domain", n + " Domain", nova(17, 7, false, 12, all(knock(1.0), weaken(80, 1)), true)).withCooldown(900),
                    mid("attribute_guard", n + " Guard", guard(180, 2, 4f, 0.06)),
                    signature("boulder_titan", "Boulder Titan", EarthFamilyArts::titan).withCooldown(900),
                    mid("boulder_barrage", "Boulder Barrage", thrown(EarthConstruct.BOULDER, 3, 34, 1.35, 0.75f, 50)));
            case RED_OCHRE -> List.of(
                    starter("attribute_bolt", n + " Bolt", ElementBook.bolt(9, 1.8, 0.55, 24, false, 1.2, VfxShape.WIKI_MAGIC_CAST, VfxShape.WIKI_MAGIC_BURST, weaken(70, 0))),
                    mid("attribute_cut", n + " Manifestation", stroke(10f, 9, 0.9, all(mark(100), weaken(60, 0)))),
                    zone("attribute_field", n + " Field", field(4, 4.5, 80, 16, true, all(weaken(50, 0), slow(30, 1)))),
                    signature("attribute_domain", n + " Domain", nova(15, 7, false, 12, all(mark(160), weaken(100, 1)), true)).withCooldown(900),
                    mid("attribute_guard", n + " Guard", guard(180, 1, 3f, 0.06)),
                    mid("ochre_totem", "Ochre Totem", planted(EarthConstruct.TOTEM, 500, 1.0f, 2)),
                    mid("war_paint", "War Paint", EarthFamilyArts::warPaint));
            case STONE -> List.of(
                    starter("attribute_bolt", n + " Bolt", ElementBook.bolt(9, 1.8, 0.55, 24, false, 1.2, VfxShape.WIKI_MAGIC_CAST, VfxShape.WIKI_MAGIC_BURST, slow(60, 1))),
                    mid("attribute_cut", n + " Manifestation", eruption(8, 10f, 0.4, 1.3, slow(40, 1))),
                    zone("attribute_field", n + " Field", field(5, 4.5, 80, 16, true, slow(40, 2))),
                    signature("attribute_domain", n + " Domain", (b, i, p, m) -> nova(16, 6.5, false, 12, BalanceLaw.beginControl(p) ? all(knock(1.0), jam(60)) : knock(1.0), true).cast(b, i, p, m)).withCooldown(900),   // the dressed stone seals casting (Magic Jamming) when the control lock is free
                    mid("attribute_guard", n + " Guard", guard(180, 2, 3f, 0.06)),
                    mid("stone_sentinel", "Stone Sentinel", guardian(EarthConstruct.SENTINEL, 500, 1)),
                    mid("stone_wall", "Stone Wall", wall(Blocks.STONE_BRICKS.defaultBlockState(), 6, 4, 200)));
            default -> List.of(                                                                           // MINERAL
                    starter("attribute_bolt", n + " Bolt", thrown(EarthConstruct.LANCE, 1, 0, 1.9, 0.9f, 30)),
                    mid("attribute_cut", n + " Manifestation", eruption(8, 10f, 0.3, 1.2, ElementBook.NONE)),
                    zone("attribute_field", n + " Field", EarthFamilyArts::garden),
                    signature("attribute_domain", n + " Domain", (b, i, p, m) -> nova(16, 7, false, 12, BalanceLaw.beginControl(p) ? siphon(0.02) : ElementBook.NONE, true).cast(b, i, p, m)).withCooldown(900),   // the crystals resonate and drain magicules (Energy Drain) when the control lock is free
                    mid("attribute_guard", n + " Guard", guard(180, 1, 3f, 0.06)),
                    mid("mineral_spire", "Crystal Spire", planted(EarthConstruct.SPIRE, 300, 1.2f, 2)),
                    mid("lance_volley", "Lance Volley", thrown(EarthConstruct.LANCE, 5, 40, 1.8, 0.75f, 30)));
        };
        return new EarthFamilyBook(magic, color, pages);
    }
}
