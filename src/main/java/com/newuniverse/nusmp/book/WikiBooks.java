package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;

import java.util.List;

import static com.newuniverse.nusmp.book.BookPage.*;

/** 0.34: grimoires for the attributes added after the wiki: Transmutation (Grey) and Recombination (Henry). Ash and Cotton / Food
 *  have their own classes (AshBook, CottonBook) for their passives. */
public final class WikiBooks {
    private WikiBooks() {}

    public static GrimoireBook transmutation() {
        return new ElementBook(MagicType.TRANSMUTATION, 0xFF7AF0D8, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                starter("iron_spikes", "Transmutation: Iron Spikes", WikiSpells::ironSpikes),
                mid("magic_convert", "Magic Convert", WikiSpells::magicConvert),
                zone("quagmire", "Transmutation: Quagmire", WikiSpells::quagmire),
                signature("grand_transmutation", "Grand Transmutation", WikiSpells::grandTransmutation)));
    }

    public static GrimoireBook recombination() {
        return new ElementBook(MagicType.RECOMBINATION, 0xFFFF9A3C, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                starter("mana_corkscrew", "Mana Corkscrew", WikiSpells::manaCorkscrew),
                mid("bulwark", "Recombination: Bulwark", WikiSpells::bulwark),
                zone("room_swap", "Recombination: Room Swap", WikiSpells::roomSwap),
                signature("raging_black_bull", "The Raging Black Bull", WikiSpells::ragingBlackBull).withCooldown(2400)));
    }
}
