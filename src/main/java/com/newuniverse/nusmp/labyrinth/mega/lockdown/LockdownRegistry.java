package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the one registration entry of the anti-speedrun layer (1.107.0), in the shape every builder of the Mega-Dungeon uses: {@code MegaSystem} calls {@link #init}
 * (the block, its block entity and block item, the config file, the self-test) and {@link #register} (every game-bus handler, added with {@code addListener}: no
 * annotation scanning). It follows the trial chambers' registry ({@code TrialRegistry}): its own {@code DeferredRegister} for the block and the block entity type, the block
 * item on {@code NUItems.ITEMS} (so {@code MegaSystem.init} must run before that list is handed to the bus, which it does). The Lockdown Barrier is bedrock-hard like the
 * trial's Magic Barrier, with a different colour (amber, not cyan), so a player can tell at a glance which kind of seal it is: the cyan one belongs to a trial hall, the
 * amber one to a room that locked behind them. The two scoreboard tags mark what the layer made: {@link #MOB_TAG} on a locked room's wave, {@link #AMBUSH_TAG} on an ambush
 * squad, so the sweep, the loot and the commands can tell them apart.
 */
public final class LockdownRegistry {
    private LockdownRegistry() {}

    /** The tag of every monster of a locked room's wave. */
    public static final String MOB_TAG = "nusmp_lockdown";
    /** The tag of every monster of an ambush squad. */
    public static final String AMBUSH_TAG = "nusmp_ambush";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NUSMP.MODID);

    /** The wall of amber light in a doorway of a locked room: unbreakable, piston-proof, loot-less (see {@link LockdownBarrierBlock}). */
    public static final DeferredBlock<LockdownBarrierBlock> LOCKDOWN_BARRIER = BLOCKS.register("lockdown_barrier", () -> new LockdownBarrierBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).noOcclusion().strength(-1.0f, 3600000.0f).lightLevel(s -> 6).noLootTable()
                    .pushReaction(PushReaction.BLOCK).sound(SoundType.AMETHYST)
                    .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false).isRedstoneConductor((s, l, p) -> false).isValidSpawn((s, l, p, t) -> false)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LockdownBarrierBlockEntity>> LOCKDOWN_BARRIER_BE = BLOCK_ENTITIES.register("lockdown_barrier",
            () -> BlockEntityType.Builder.of(LockdownBarrierBlockEntity::new, LOCKDOWN_BARRIER.get()).build(null));

    public static final DeferredItem<BlockItem> LOCKDOWN_BARRIER_ITEM = NUItems.ITEMS.register("lockdown_barrier", () -> new BlockItem(LOCKDOWN_BARRIER.get(), new Item.Properties()));

    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        LockdownConfig.register();
        LockdownSelfTests.register();
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener((ServerTickEvent.Post e) -> {
            LockdownDirector.onServerTick(e.getServer());
            DynamicAmbushDirector.onServerTick(e.getServer());
        });
        gameBus.addListener((AddReloadListenerEvent e) -> e.addListener(new LockdownRooms.Reload()));
        gameBus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> DynamicAmbushDirector.forget(e.getEntity().getUUID()));
        gameBus.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> DynamicAmbushDirector.forget(e.getEntity().getUUID()));
        gameBus.addListener((ServerStoppedEvent e) -> {
            LockdownDirector.reset();
            DynamicAmbushDirector.reset();
        });
    }

    /** The operator's branch for {@code /multiverse mega}: {@code lockdown status | rules | release | forget | ambush}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() { return LockdownCommand.command(); }
}
