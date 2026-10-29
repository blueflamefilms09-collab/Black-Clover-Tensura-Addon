package com.newuniverse.nusmp.skilltree.item;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the Skill Book ({@code nusmp:skill_book}) and the SP Book ({@code nusmp:sp_book}) were removed at the owner's request, together with the unique skills they taught. What stays is the four
 * DATA COMPONENT types they carried, registered under their old ids, so that a world saved before the removal still loads: a chest or an inventory slot that still holds a stack with one of these
 * components (the stack's item is gone, so the game drops the stack, but the component ids in the saved data must still resolve, and nothing else in the file may fail with them). Nothing reads
 * or writes these components any more. They are persistent and network-synchronised exactly as before. Do not remove or rename them: see {@code docs/skill_books_removed.md}.
 */
public final class SkillItems {
    private SkillItems() {}

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    /** {@code nusmp:skill_book_id}: legacy, the id of the book of an old Skill Book stack. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BOOK_ID =
            COMPONENTS.registerComponentType("skill_book_id", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** {@code nusmp:sp_amount}: legacy, the Skill Points an old SP Book gave. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SP_AMOUNT =
            COMPONENTS.registerComponentType("sp_amount", b -> b.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** {@code nusmp:sp_kind}: legacy, which primer an old SP Book was. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SP_KIND =
            COMPONENTS.registerComponentType("sp_kind", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** {@code nusmp:skill_book_rarity}: legacy, the rarity id of one old Skill Book stack. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BOOK_RARITY =
            COMPONENTS.registerComponentType("skill_book_rarity", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** Attaches the register to the mod bus; called once from {@code SkillBookSystem.init}. */
    public static void init(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
