package com.newuniverse.nusmp.elite.nations.world;

import com.newuniverse.nusmp.elite.nations.core.Nation;
import com.newuniverse.nusmp.elite.nations.core.Res;
import com.newuniverse.nusmp.elite.nations.net.NationNet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Why: the capital of a nation. Placed by the LEADER (the {@code BlockEvent.EntityPlaceEvent} handler in {@link NationEvents} refuses anyone else, a second capital, a dungeon dimension and a spot too
 * close to another capital), it registers its position as the nation's capital; breaking it (leader only) removes the capital. Right click opens the nations screen. If a placement slips past the event
 * check, {@code setPlacedBy} re-validates and removes the block again, so a banner that is not a capital cannot exist.
 */
public class NationBannerBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(2, 0, 6, 14, 16, 10);

    public NationBannerBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(level instanceof ServerLevel sl)) return;
        if (!(placer instanceof ServerPlayer sp)) { level.removeBlock(pos, false); return; }
        Res r = NationService.setCapital(sp, sl, pos);
        if (!r.ok()) {
            level.removeBlock(pos, false);
            NationService.giveBannerItem(sp);
            Msg.tell(sp, r);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
            Nation n = NationService.data(sl.getServer()).capitalAt(sl.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ());
            if (n == null) { Msg.tell(sp, "nusmp.nation.m.orphan_banner"); return InteractionResult.CONSUME; }
            NationNet.open(sp, 0);
        }
        return InteractionResult.CONSUME;
    }
}
