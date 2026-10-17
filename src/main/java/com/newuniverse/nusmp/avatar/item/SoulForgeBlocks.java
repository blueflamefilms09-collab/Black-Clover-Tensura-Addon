package com.newuniverse.nusmp.avatar.item;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the Soul Forge needs a block and its item, and (like the Relic Forge and the Guardian Altar) they live in their own {@link DeferredRegister.Blocks}
 * so the feature adds nothing to a shared registry class and is wired by one call. The block item goes on {@link NUItems#ITEMS} with the mod's other
 * items, and that register is filled the moment this class loads, so {@code SoulForgeSystem.init} (called from {@code AvatarSystem.init}, before
 * {@code NUItems.ITEMS.register(modBus)}) is the one place that touches this class. Nothing outside the avatar package reads these holders except the
 * creative tab line the lead adds.
 *
 * <p>The block is "iron-hard but not unbreakable": it takes a pickaxe, and it lights its surroundings a little because its rune and gem glow. It has no
 * block entity: the ritual's state lives on the server in {@code ritual.SoulForgeRitual}, keyed by the player, so breaking the forge in the middle of a
 * ritual just ends it (the reach check) and there is nothing stored in the world to duplicate or lose.
 */
public final class SoulForgeBlocks {
    private SoulForgeBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);

    /** {@code nusmp:soul_forge}: the altar that starts the Avatar ritual. */
    public static final DeferredBlock<SoulForgeBlock> SOUL_FORGE = BLOCKS.register("soul_forge", () -> new SoulForgeBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.DEEPSLATE_BRICKS).strength(4.0f, 24.0f).noOcclusion().lightLevel(s -> 12)));

    public static final DeferredItem<SoulForgeItem> SOUL_FORGE_ITEM = NUItems.ITEMS.register("soul_forge",
            () -> new SoulForgeItem(SOUL_FORGE.get(), new Item.Properties().rarity(Rarity.EPIC)));

    /** Puts the block register on the mod bus (the item was registered when this class loaded). */
    public static void init(IEventBus modBus) { BLOCKS.register(modBus); }
}
