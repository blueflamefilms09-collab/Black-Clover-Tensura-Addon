package com.newuniverse.nusmp.arena;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: one entry point for the whole arena (1.46.0) so the mod class gains a single line, and so the ORDER is fixed in one place: the
 * Hourglass of the Final Hour and the Return Sigil's block item go on {@code NUItems.ITEMS} (the pattern of the Labyrinth and the Relic Forge), which
 * means {@link #init} must run BEFORE {@code NUItems.ITEMS.register(modEventBus)}, otherwise they are added to a register that has already been handed
 * to the bus. The block has its own {@code DeferredRegister.Blocks}. The game-bus handlers (the director, the spawn gate, the guard of
 * the courtyard, the command) are added with {@code addListener}, like the rest of the mod.
 */
public final class ArenaSystem {
    private ArenaSystem() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);

    /** {@code nusmp:arena_return_sigil}: right click sends you home. Unbreakable, lit, no drops. */
    public static final DeferredBlock<ReturnSigilBlock> RETURN_SIGIL = BLOCKS.register("arena_return_sigil", () -> new ReturnSigilBlock(
            BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_LIGHT_BLUE).strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.AMETHYST).lightLevel(s -> 12)));

    public static final DeferredItem<BlockItem> RETURN_SIGIL_ITEM = NUItems.ITEMS.register("arena_return_sigil",
            () -> new BlockItem(RETURN_SIGIL.get(), new Item.Properties().rarity(Rarity.EPIC)));

    /** {@code nusmp:zogratis_hourglass}: the Hourglass of the Final Hour. Epic, one at a time. */
    public static final DeferredItem<ZogratisHourglassItem> HOURGLASS = NUItems.ITEMS.register("zogratis_hourglass",
            () -> new ZogratisHourglassItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    private static boolean wired;

    /** Call from the mod constructor BEFORE {@code NUItems.ITEMS.register(modBus)}. Idempotent for the game-bus part. */
    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        if (wired) return;
        wired = true;
        ArenaDirector.register(NeoForge.EVENT_BUS);
        ArenaSpawnEvents.register(NeoForge.EVENT_BUS);
        ArenaCommand.register(NeoForge.EVENT_BUS);
    }

    /** The two arena items for the creative tab, in order. */
    public static java.util.List<DeferredItem<? extends Item>> creativeItems() { return java.util.List.of(HOURGLASS, RETURN_SIGIL_ITEM); }
}
