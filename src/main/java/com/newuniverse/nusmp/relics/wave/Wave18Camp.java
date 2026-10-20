package com.newuniverse.nusmp.relics.wave;


import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.ArrayList;
import java.util.List;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Wave 18: The Pocket Campsite. Pointing the pouch pitches a large tent with a bed, two small tents, a campfire and seats of oak slabs in front of
 * the bearer (all wool, slabs, a campfire and a bed: nothing that drops anything), and raises the tiny hut over the fire for ten minutes: a ring where
 * those who rest mend and hostile things are turned away. The positions of what was placed are kept on the pouch, so pressing the rune again (sneak)
 * packs the camp up and the pouch holds it again. Never in the Dungeon, only in loaded chunks, only into air and replaceable plants, all in one go
 * (about a hundred blocks) and never more than {@link Wave18Tune#CAMP_MAX_BLOCKS}.
 */
final class Wave18Camp {
    private Wave18Camp() {}

    static final String KEY = "w18_camp", KEY_DIM = "w18_camp_dim";
    /** Cells that may be skipped (uneven ground, a tree in the way) before the camp is refused as not having room. */
    private static final int TOLERANCE = 6;
    /** How far from the camp the bearer must be to pack it. */
    private static final double PACK_REACH = 48.0;

    private static BlockState stateOf(Wave18CampPlan.Kind k, Direction facing) {
        return switch (k) {
            case FIRE -> Blocks.CAMPFIRE.defaultBlockState();
            case SEAT -> Blocks.OAK_SLAB.defaultBlockState();
            case BIG_WALL, BIG_ROOF -> Blocks.RED_WOOL.defaultBlockState();
            case LEFT_WALL, LEFT_ROOF -> Blocks.YELLOW_WOOL.defaultBlockState();
            case RIGHT_WALL, RIGHT_ROOF -> Blocks.BLUE_WOOL.defaultBlockState();
            case BED_FOOT -> Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.FOOT);
            case BED_HEAD -> Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.HEAD);
            default -> Blocks.RED_WOOL.defaultBlockState();
        };
    }

    /** Is this block one the camp could have placed (so packing never removes a block that is something else now)? */
    private static boolean ours(BlockState s) {
        return s.is(Blocks.RED_WOOL) || s.is(Blocks.YELLOW_WOOL) || s.is(Blocks.BLUE_WOOL) || s.is(Blocks.OAK_SLAB) || s.is(Blocks.CAMPFIRE) || s.is(Blocks.RED_BED);
    }

    private static boolean free(ServerLevel level, ServerPlayer p, BlockPos pos) {
        if (!level.isLoaded(pos) || !level.mayInteract(p, pos)) return false;           // spawn protection and the world border are respected
        BlockState old = level.getBlockState(pos);
        return old.isAir() || (old.canBeReplaced() && old.getFluidState().isEmpty());
    }

    static void pitch(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        ItemStack pouch = c.stack();
        if (Labyrinth.in(level)) {
            Fx.say(p, "The Dungeon will not take a camp.");
            return;
        }
        if (tagOf(pouch).getLongArray(KEY).length > 0) {
            Fx.say(p, "The camp is already pitched: sneak and press the rune to pack it up.");
            return;
        }
        Direction f = p.getDirection();
        int fx = f.getStepX(), fz = f.getStepZ();
        BlockPos fire = p.blockPosition().relative(f, 4);
        int y0 = p.blockPosition().getY();
        List<Wave18CampPlan.Cell> cells = Wave18CampPlan.plan();
        List<BlockPos> at = new ArrayList<>();
        int bad = 0;
        boolean bedOk = true;
        for (Wave18CampPlan.Cell cell : cells) {
            int[] o = Wave18CampPlan.world(cell.u(), cell.v(), fx, fz);
            BlockPos pos = new BlockPos(fire.getX() + o[0], y0 + cell.y(), fire.getZ() + o[1]);
            at.add(pos);
            boolean ok = free(level, p, pos);
            if (ok && cell.y() == 0) ok = level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
            if (!ok) {
                bad++;
                if (cell.kind() == Wave18CampPlan.Kind.BED_FOOT || cell.kind() == Wave18CampPlan.Kind.BED_HEAD) bedOk = false;
            }
        }
        if (bad > TOLERANCE) {
            Fx.say(p, "There is no room for a camp here: it needs flat, open ground about eleven blocks ahead.");
            return;
        }
        List<Long> placed = new ArrayList<>();
        for (int i = 0; i < cells.size() && placed.size() < Wave18Tune.CAMP_MAX_BLOCKS; i++) {
            Wave18CampPlan.Cell cell = cells.get(i);
            BlockPos pos = at.get(i);
            boolean bed = cell.kind() == Wave18CampPlan.Kind.BED_FOOT || cell.kind() == Wave18CampPlan.Kind.BED_HEAD;
            if (bed && !bedOk) continue;
            if (!free(level, p, pos)) continue;
            if (cell.y() == 0 && !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) continue;
            level.setBlock(pos, stateOf(cell.kind(), f), 3);
            placed.add(pos.asLong());
        }
        long[] arr = new long[placed.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = placed.get(i);
        String dim = level.dimension().location().toString();
        CustomData.update(DataComponents.CUSTOM_DATA, pouch, t -> {
            t.putLongArray(KEY, arr);
            t.putString(KEY_DIM, dim);
        });
        Wave18Events.pitch(p.getUUID(), new Wave18Events.Zone(level.dimension(), fire, level.getGameTime() + Wave18Tune.CAMP_TICKS));
        level.playSound(null, fire, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.4f, 0.8f);
        com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_18, FxTone.LIGHT, fire.getX() + 0.5, fire.getY() + 1.0, fire.getZ() + 0.5, 30, 2.5, 0.8, 2.5, 0.02);
        Fx.say(p, "The camp unfolds: a large tent with a bed, two small tents, a fire and seats. The tiny hut keeps the dark away for ten minutes.");
    }

    static void pack(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        ItemStack pouch = c.stack();
        long[] arr = tagOf(pouch).getLongArray(KEY);
        if (arr.length == 0) {
            Fx.say(p, "There is no camp to pack up.");
            return;
        }
        String dim = tagOf(pouch).getString(KEY_DIM);
        ResourceLocation here = level.dimension().location();
        if (!here.toString().equals(dim)) {
            Fx.say(p, "The camp is in another world.");
            return;
        }
        if (Labyrinth.in(level)) {
            Fx.say(p, "The Dungeon will not let a camp be folded away.");
            return;
        }
        BlockPos first = BlockPos.of(arr[0]);
        if (p.distanceToSqr(first.getX() + 0.5, first.getY(), first.getZ() + 0.5) > PACK_REACH * PACK_REACH) {
            Fx.say(p, "The camp is too far away to pack up.");
            return;
        }
        int removed = 0;
        for (long l : arr) {
            BlockPos pos = BlockPos.of(l);
            if (!level.isLoaded(pos) || !level.mayInteract(p, pos)) continue;
            BlockState s = level.getBlockState(pos);
            if (!ours(s)) continue;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            removed++;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, pouch, t -> {
            t.remove(KEY);
            t.remove(KEY_DIM);
        });
        Wave18Events.strike(p.getUUID());
        level.playSound(null, p.blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 1.2f, 1.1f);
        Fx.say(p, "The camp folds away into the pouch (" + removed + " blocks).");
    }

    private static net.minecraft.nbt.CompoundTag tagOf(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }

    /** Blocks the plan would place, for the tests of the glue (the plan itself is tested in pure Java). */
    static int plannedBlocks() { return Wave18CampPlan.plan().size(); }
}
