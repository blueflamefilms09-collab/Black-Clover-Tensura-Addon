package com.newuniverse.nusmp.skilltree.item;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import com.newuniverse.nusmp.skilltree.rarity.BookRarity;
import com.newuniverse.nusmp.skilltree.unique.UniqueSkills;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the two books and their three data components are registered here, in registers of this system's own (not on the mod's shared item register), for two reasons.
 * First, ordering: the shared {@code NUItems.ITEMS} must be filled before it is attached to the mod bus, which makes every system that adds to it depend on the order
 * of lines in the mod constructor; the skill books start from one line ({@code SkillBookSystem.init}) and attach their own registers themselves, so no ordering rule
 * exists to forget. Second, the data-component design: there is ONE Skill Book item, and what a stack IS lives in a component ({@code nusmp:skill_book_id} says which book), exactly as the arc spawn egg carries
 * its character. The two other components ({@code nusmp:sp_kind}, {@code nusmp:sp_amount}) and the item {@code nusmp:sp_book} belonged to the Skill Points of the removed Skill Tree; they stay registered ONLY so an
 * old stack in a chest still loads (it is turned into a Skill Book when read). The components are persistent (they survive a save) and network-synchronised. The amount uses the non-negative integer codec, so a
 * hand-typed negative amount is rejected when the stack is parsed. The reader at the bottom is the one way the rest of the mod reads the book id, so a missing component always answers with a harmless default.
 */
public final class SkillItems {
    private SkillItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    /** {@code nusmp:skill_book_id}: the id of the book ({@code nusmp:lady_luck_book}); the unique skill of that book is the one the stack teaches. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BOOK_ID =
            COMPONENTS.registerComponentType("skill_book_id", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** {@code nusmp:sp_amount}: legacy, the Skill Points an old SP Book gave (kept so old stacks load). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SP_AMOUNT =
            COMPONENTS.registerComponentType("sp_amount", b -> b.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** {@code nusmp:sp_kind}: legacy, which primer an old SP Book was (kept so old stacks load). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SP_KIND =
            COMPONENTS.registerComponentType("sp_kind", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /**
     * {@code nusmp:skill_book_rarity} (3.26.0, APPENDED so no older registration moves): the optional rarity of ONE stack, the saved id of a {@link BookRarity} ({@code epic}). When a stack carries it, it wins over
     * the rarity of the book's line in the shipped table ({@link #rarityOf}); a stack without it (every older book) simply reads the table. Persistent and network-synchronised, so the tooltip and the name
     * colour are right on a client that has no table entry.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BOOK_RARITY =
            COMPONENTS.registerComponentType("skill_book_rarity", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** {@code nusmp:skill_book}: the Skill Book. Sixteen to a stack at most, rare, enchanted-looking (see {@link SkillBookItem#isFoil}). */
    public static final DeferredItem<SkillBookItem> SKILL_BOOK = ITEMS.register("skill_book", () -> new SkillBookItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    /** {@code nusmp:sp_book}: the legacy SP Book (a voucher for a Skill Book now). */
    public static final DeferredItem<SPBookItem> SP_BOOK = ITEMS.register("sp_book", () -> new SPBookItem(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));

    /** Attaches both registers to the mod bus; called once from {@code ItemsSystem.init}. */
    public static void init(IEventBus modBus) {
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
    }

    // ------------------------------------------------------------------ readers

    /** The book id on a stack, or an empty string when the stack is empty, is not a Skill Book, or has no (or a blank) id. */
    public static String bookIdOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        String id = stack.get(BOOK_ID.get());
        return id == null ? "" : id;
    }

    /**
     * The rarity of a Skill Book stack, the one reader: the stack's own component {@code nusmp:skill_book_rarity} when it has a known id, else the rarity of the book's line in the shipped table, else common.
     * An empty stack, a stack of another item and an unknown book are common. Never null, never throws; two map lookups.
     */
    public static BookRarity rarityOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return BookRarity.COMMON;
        String own = stack.get(BOOK_RARITY.get());
        BookRarity r = BookRarity.byId(own);
        if (r != null) return r;
        var line = UniqueSkills.lineOfBook(bookIdOf(stack));
        return line == null ? BookRarity.COMMON : line.rarity();
    }
}
