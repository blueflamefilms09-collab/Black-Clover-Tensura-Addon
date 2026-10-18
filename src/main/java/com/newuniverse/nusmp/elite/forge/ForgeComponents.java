package com.newuniverse.nusmp.elite.forge;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.elite.forge.rules.Quality;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the forged item itself must carry what the forge did to it, as data components (so it survives a chest, a trade and a death, and reaches the client for
 * the tooltip): {@code nusmp:forge_quality} (1..6, Common to Mythical), {@code nusmp:forge_smith} (the name of the player who forged it) and
 * {@code nusmp:forge_engraved} (how many engravings the forge has put on it, which uses up the quality's engraving slots) and {@code nusmp:forge_kind} (the category
 * of the recipe). All four are persistent and network synchronised. An item without {@code forge_quality} was not forged; nothing else reads these but the tooltip, the station's Engrave button and the self-tests.
 */
public final class ForgeComponents {
    private ForgeComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> QUALITY =
            COMPONENTS.registerComponentType("forge_quality", b -> b.persistent(Codec.intRange(1, 6)).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SMITH =
            COMPONENTS.registerComponentType("forge_smith", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENGRAVED =
            COMPONENTS.registerComponentType("forge_engraved", b -> b.persistent(Codec.intRange(0, 64)).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** The category id of the recipe it was forged from ({@code weapon}, {@code armor}, {@code tool}, {@code rune}); decides the specialization's engraving slots. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> KIND =
            COMPONENTS.registerComponentType("forge_kind", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** The quality a stack was forged at, or null when it was not forged. */
    public static Quality qualityOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Integer lv = stack.get(QUALITY.get());
        return lv == null ? null : Quality.ofLevel(lv);
    }

    /** Engravings the forge has put on the stack. */
    public static int engravedCount(ItemStack stack) {
        Integer n = stack == null || stack.isEmpty() ? null : stack.get(ENGRAVED.get());
        return n == null ? 0 : n;
    }
}
