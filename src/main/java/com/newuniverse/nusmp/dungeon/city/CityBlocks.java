package com.newuniverse.nusmp.dungeon.city;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The altar of the Golden City and the core it gives. */
public final class CityBlocks {
    private CityBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);

    /** The Aurelian Altar on the dais of a city's plaza. It cannot be broken: it gives each player one Aurelian Core. */
    public static final DeferredBlock<AltarBlock> AURELIAN_ALTAR = BLOCKS.register("aurelian_altar", () -> new AltarBlock(
            BlockBehaviour.Properties.of().strength(-1f, 3600000f).lightLevel(s -> 12).sound(SoundType.AMETHYST).noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)));

    public static final DeferredItem<BlockItem> AURELIAN_ALTAR_ITEM = NUItems.ITEMS.register("aurelian_altar",
            () -> new BlockItem(AURELIAN_ALTAR.get(), new Item.Properties().rarity(Rarity.EPIC)));

    /** The gift of the altar: spent, it raises a Golden Ward for twenty minutes. */
    public static final DeferredItem<AurelianCore> AURELIAN_CORE = NUItems.ITEMS.register("aurelian_core",
            () -> new AurelianCore(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static void init() {}

    /** The altar: right-click once, as each player, for a core. What it gives is not a chest's loot: it is a share of the city's own magicules. */
    public static final class AltarBlock extends Block {
        private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 3, 15), Block.box(4, 3, 4, 12, 9, 12), Block.box(1, 9, 1, 15, 12, 15));

        AltarBlock(BlockBehaviour.Properties props) { super(props); }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide || !(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
            long key = CitySave.key(Math.floorDiv(pos.getX(), CityPlan.CELL), Math.floorDiv(pos.getZ(), CityPlan.CELL));
            if (!CitySave.get(sl.getServer()).claim(key, player.getUUID())) {
                player.displayClientMessage(Component.literal("The altar is quiet. It has already given you all this city had to give.").withStyle(ChatFormatting.GRAY), true);
                sl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7f, 0.6f);
                return InteractionResult.CONSUME;
            }
            ItemStack core = new ItemStack(AURELIAN_CORE.get());
            if (!player.getInventory().add(core)) player.drop(core, false);
            sl.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.3f);
            sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 36, 0.35, 0.5, 0.35, 0.06);
            player.displayClientMessage(Component.literal("The altar warms, and something that was the city's heart settles in your hands.").withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.CONSUME;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
            if (rnd.nextInt(3) != 0) return;
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.2 + rnd.nextDouble() * 0.6, pos.getY() + 1.0 + rnd.nextDouble() * 0.4, pos.getZ() + 0.2 + rnd.nextDouble() * 0.6, 0.0, 0.02, 0.0);
        }
    }
}
