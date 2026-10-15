package com.newuniverse.nusmp.labyrinth.trap;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.dnd.DndConfig;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the dungeon's light is a resource (rations, torches, the lantern cores): the D&amp;D layer wants a room that takes it away, and the
 * safest way to do that without a block entity, a per-player tick or a new effect is the vanilla Darkness effect plus a refusal. A
 * floor glyph schedules its own tick once a second ({@link TrapMath#GLYPH_PERIOD}); the tick looks for survival and adventure players
 * inside six blocks and (a) tops their Darkness up to ten seconds only when it is about to fade ({@link TrapMath#needsRefresh}, so the
 * screen does not pulse), and (b) the first time a player comes near ({@code active} false to true, reset when the room is empty again)
 * snuffs the torches, lanterns, campfires and candles already standing within four blocks, inside {@link DungeonGuard#internal}. The
 * refusal to PLACE a light within eight blocks is not here but in {@link TrapEvents} (an {@code EntityPlaceEvent}, Labyrinth only), because
 * the block cannot see a placement it is not part of. A scheduled tick is saved with its chunk, and the block is randomly ticking as a
 * safety net that re-schedules a tick lost to a world edit, so a glyph placed by a template, by hand or by a structure block always
 * starts and never stops. Inert when {@code dnd.traps} is off.
 */
public final class SnufferGlyphBlock extends Block {
    /** A player is near now: the once-per-approach snuff has been done. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public SnufferGlyphBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(ACTIVE); }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide && !state.is(old.getBlock())) level.scheduleTick(pos, this, TrapMath.GLYPH_PERIOD);
    }

    /** The net under the scheduled tick: a randomly ticking glyph that lost its schedule gets one (scheduling twice is a no-op). */
    @Override protected boolean isRandomlyTicking(BlockState state) { return true; }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rnd) { level.scheduleTick(pos, this, TrapMath.GLYPH_PERIOD); }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rnd) {
        level.scheduleTick(pos, this, TrapMath.GLYPH_PERIOD);
        if (!DndConfig.get(DndConfig.TRAPS)) return;
        List<ServerPlayer> near = new ArrayList<>();
        double r = TrapMath.DARKNESS_RADIUS;
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(r))) {
            if (p.isCreative() || p.isSpectator()) continue;
            if (TrapMath.within(p.getX() - (pos.getX() + 0.5), p.getY() - (pos.getY() + 0.5), p.getZ() - (pos.getZ() + 0.5), r)) near.add(p);
        }
        boolean active = state.getValue(ACTIVE);
        if (near.isEmpty()) {
            if (active) DungeonGuard.internal(() -> level.setBlock(pos, state.setValue(ACTIVE, false), 3));   // the room is empty: the next approach snuffs again
            return;
        }
        RukhFx.burst(level, new Vec3(pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5), RukhFx.Alignment.DARK, 2);        // dark Rukh drifting off the glyph while someone is near
        for (ServerPlayer p : near) {
            MobEffectInstance cur = p.getEffect(MobEffects.DARKNESS);
            if (TrapMath.needsRefresh(cur == null ? 0 : cur.getDuration()))
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, TrapMath.DARKNESS_TICKS, 0, false, false, true));
        }
        if (!active) {
            DungeonGuard.internal(() -> level.setBlock(pos, state.setValue(ACTIVE, true), 3));
            int out = snuffAround(level, pos);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 0.5f);
            RukhFx.burst(level, new Vec3(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5), RukhFx.Alignment.DARK, 10);
            for (ServerPlayer p : near) p.displayClientMessage(Component.translatable("message.nusmp.trap.snuffer", out).withStyle(ChatFormatting.DARK_GRAY), true);
        }
    }

    /** Is this a flame the glyph puts out by removing it (a torch or a lantern)? */
    static boolean isRemovable(BlockState s) {
        return s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH) || s.is(Blocks.SOUL_TORCH) || s.is(Blocks.SOUL_WALL_TORCH)
                || s.is(Blocks.LANTERN) || s.is(Blocks.SOUL_LANTERN);
    }

    /** Is this a flame the glyph puts out by clearing its {@code lit} property (a campfire or a candle)? */
    static boolean isLit(BlockState s) {
        return (s.is(BlockTags.CAMPFIRES) || s.is(BlockTags.CANDLES)) && s.hasProperty(BlockStateProperties.LIT) && s.getValue(BlockStateProperties.LIT);
    }

    /** Puts out every flame within {@link TrapMath#SNUFF_RADIUS} blocks once; returns how many. Only the mod's own span may change blocks here. */
    private static int snuffAround(ServerLevel level, BlockPos centre) {
        int r = TrapMath.SNUFF_RADIUS;
        int[] count = {0};
        DungeonGuard.internal(() -> {
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
                if (!TrapMath.within(p.getX() - centre.getX(), p.getY() - centre.getY(), p.getZ() - centre.getZ(), r)) continue;
                BlockState s = level.getBlockState(p);
                if (isRemovable(s)) {
                    level.destroyBlock(p.immutable(), true);
                    count[0]++;
                } else if (isLit(s)) {
                    level.setBlock(p.immutable(), s.setValue(BlockStateProperties.LIT, false), 3);
                    count[0]++;
                }
            }
        });
        return count[0];
    }
}
