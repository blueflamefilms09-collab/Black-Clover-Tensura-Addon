package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * Wave 2's small pieces of per-item state (the deck of cards, the lives of the cape, the style of the mimic) live in the stack's own custom
 * data under one compound, so they travel with the item and need no component of their own.
 */
final class Wave2Data {
    private Wave2Data() {}

    static final String ROOT = "nusmp_w2";

    /** A copy of the wave's compound on the stack (empty if there is none). */
    static CompoundTag read(ItemStack s) {
        if (s == null || s.isEmpty()) return new CompoundTag();
        CompoundTag all = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return all.getCompound(ROOT);
    }

    /** Edits the wave's compound on the stack in place. */
    static void write(ItemStack s, Consumer<CompoundTag> edit) {
        if (s == null || s.isEmpty()) return;
        CustomData.update(DataComponents.CUSTOM_DATA, s, all -> {
            CompoundTag t = all.getCompound(ROOT);
            edit.accept(t);
            all.put(ROOT, t);
        });
    }

    /** The stack of a relic of this id that works for the player right now (worn, and attuned if it needs that), or an empty stack. */
    static ItemStack worn(ServerPlayer p, String relicId) {
        for (Relics.Worn w : Relics.loadout(p).active()) if (w.def().id().equals(relicId)) return w.stack();
        return ItemStack.EMPTY;
    }

    // ---------------------------------------------------------------- the deck of Pact of the Deck
    private static final String DECK = "deck", DAY = "day";

    /** The cards still in the deck today; a deck from an earlier day, or one never drawn from, is whole. */
    static long deck(ItemStack s, long today) {
        CompoundTag t = read(s);
        return Wave2Deck.forDay(t.contains(DECK), t.getLong(DECK), t.getLong(DAY), today);
    }

    static void setDeck(ItemStack s, long mask, long today) {
        write(s, t -> {
            t.putLong(DECK, mask);
            t.putLong(DAY, today);
        });
    }

    // ---------------------------------------------------------------- the lives of the Cape of Nine Lives
    private static final String LIVES = "lives", CAT = "cat";

    static Wave2Lives lives(ItemStack s) {
        CompoundTag t = read(s);
        return t.contains(LIVES) ? new Wave2Lives(t.getInt(LIVES), t.getInt(CAT)) : Wave2Lives.fresh();
    }

    static void setLives(ItemStack s, Wave2Lives l) {
        write(s, t -> {
            t.putInt(LIVES, l.left());
            t.putInt(CAT, l.cat());
        });
    }

    // ---------------------------------------------------------------- the style of the Cloak Mimic
    private static final String STYLE = "style";

    static int style(ItemStack s) { return read(s).getInt(STYLE); }

    static void setStyle(ItemStack s, int style) { write(s, t -> t.putInt(STYLE, style)); }
}
