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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the two books and their three data components are registered here, in registers of this system's own (not on the mod's shared item register), for two reasons.
 * First, ordering: the shared {@code NUItems.ITEMS} must be filled before it is attached to the mod bus, which makes every system that adds to it depend on the order
 * of lines in the mod constructor; the skill tree starts from one line ({@code SkillTreeSystem.init}) and attaches its own registers itself, so no ordering rule
 * exists to forget. Second, the data-component design: there is ONE Skill Book item and ONE SP Book item, and what a stack IS lives in components ({@code nusmp:skill_book_id}
 * says which book, {@code nusmp:sp_kind} and {@code nusmp:sp_amount} say which primer and how much it gives), exactly as the arc spawn egg carries its character, because
 * items cannot be added by a datapack but routes can, so a new magic must never need a new item. The components are persistent (they survive a save) and
 * network-synchronised (the client needs them to colour the cover, pick the sprite and write the tooltip). The amount uses the non-negative integer codec, so a
 * hand-typed negative amount is rejected when the stack is parsed instead of ever reaching the use logic (which clamps as well). The readers at the bottom are the one
 * way the rest of the mod reads these components, so a missing or wrong-typed one always answers with a harmless default.
 */
public final class SkillItems {
    private SkillItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    /** {@code nusmp:skill_book_id}: the id of the book ({@code nusmp:fire_dragon_slayer_book}), the route whose {@code book} field is this id is the one the stack opens. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BOOK_ID =
            COMPONENTS.registerComponentType("skill_book_id", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** {@code nusmp:sp_amount}: the Skill Points the stack gives; for the gamble kind the floor of the roll. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SP_AMOUNT =
            COMPONENTS.registerComponentType("sp_amount", b -> b.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** {@code nusmp:sp_kind}: which primer it is ({@link SpKind#id()}); optional. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SP_KIND =
            COMPONENTS.registerComponentType("sp_kind", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** {@code nusmp:skill_book}: the Skill Book. Sixteen to a stack at most, rare, enchanted-looking (see {@link SkillBookItem#isFoil}). */
    public static final DeferredItem<SkillBookItem> SKILL_BOOK = ITEMS.register("skill_book", () -> new SkillBookItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    /** {@code nusmp:sp_book}: the SP Book. */
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

    /** The SP kind id on a stack, or an empty string. */
    public static String spKindOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        String id = stack.get(SP_KIND.get());
        return id == null ? "" : id;
    }

    /** The {@code sp_amount} of a stack, or {@link BookRules#NO_AMOUNT} when it has none. */
    public static int spAmountOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return BookRules.NO_AMOUNT;
        Integer n = stack.get(SP_AMOUNT.get());
        return n == null ? BookRules.NO_AMOUNT : n;
    }
}
