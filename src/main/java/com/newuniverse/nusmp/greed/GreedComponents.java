package com.newuniverse.nusmp.greed;

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
 * Why: a stack of the Black Sword carries two small components: the wielder ({@code anime_multiverse:greed_owner}, a UUID as text) and the token that makes it THE stack ({@code anime_multiverse:greed_token}, see
 * {@link GreedData}: a copy or an old stack has a token that is not the world's and dissolves). The truth (tier, stats, vault) is NOT on the stack: it is the world's record and the wielder's attachment, so a copied stack
 * carries nothing.
 */
public final class GreedComponents {
    private GreedComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> OWNER =
            COMPONENTS.registerComponentType("greed_owner", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> TOKEN =
            COMPONENTS.registerComponentType("greed_token", b -> b.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static UUID owner(ItemStack stack) {
        String s = stack.isEmpty() ? null : stack.get(OWNER.get());
        if (s == null || s.isEmpty()) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException bad) {
            return null;
        }
    }

    public static long token(ItemStack stack) {
        Long t = stack.isEmpty() ? null : stack.get(TOKEN.get());
        return t == null ? 0L : t;
    }

    public static void write(ItemStack stack, UUID owner, long token) {
        if (owner == null) stack.remove(OWNER.get()); else stack.set(OWNER.get(), owner.toString());
        stack.set(TOKEN.get(), token);
    }
}
