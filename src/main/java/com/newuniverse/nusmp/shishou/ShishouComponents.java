package com.newuniverse.nusmp.shishou;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

/**
 * Why: in 1.21.1 an item carries DATA COMPONENTS, not NBT. The stack of Shishou carries three small, new components (the ids are new, nothing old is touched): the wielder ({@code nusmp:shishou_owner}, a UUID
 * as text), the token that makes this stack THE stack ({@code nusmp:shishou_token}, see {@link ShishouCap}: a copy or an old stack has a token that is not the current one and dissolves), and a mirror of
 * the sword's level for the tooltip and the attributes ({@code nusmp:shishou_level}). The truth (level, experience, mana, skills, the storage) is NOT here: it is the world's single record
 * ({@link ShishouData}), so a copied stack cannot carry a second copy of the sword's strength. All three are persistent and network synchronised like the hero weapons' components.
 */
public final class ShishouComponents {
    private ShishouComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> OWNER =
            COMPONENTS.registerComponentType("shishou_owner", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> TOKEN =
            COMPONENTS.registerComponentType("shishou_token", b -> b.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LEVEL =
            COMPONENTS.registerComponentType("shishou_level", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** The wielder written on a stack, or null (a bare stack, an unreadable value). */
    public static UUID owner(ItemStack stack) {
        String s = stack.isEmpty() ? null : stack.get(OWNER.get());
        if (s == null || s.isEmpty()) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException bad) {
            return null;
        }
    }

    /** The token of a stack, 0 when it has none. */
    public static long token(ItemStack stack) {
        Long t = stack.isEmpty() ? null : stack.get(TOKEN.get());
        return t == null ? 0L : t;
    }

    /** The mirrored level of a stack (1 when none). */
    public static int level(ItemStack stack) {
        Integer l = stack.isEmpty() ? null : stack.get(LEVEL.get());
        return l == null ? 1 : Math.max(1, Math.min(ShishouLevels.MAX_LEVEL, l));
    }
}
