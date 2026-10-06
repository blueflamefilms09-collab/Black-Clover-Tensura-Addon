package com.newuniverse.nusmp.block;

import com.newuniverse.nusmp.blackclover.GrimoireAcceptance;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.NightmareSouls;
import com.newuniverse.nusmp.item.MagicGear;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Grimoire Altar (found at the top of Grimoire Towers). Pray empty-handed to undergo Grimoire Acceptance
 * if you never have. Pray holding your own grimoire (once per in-game day) for a small chance that
 * your cover evolves: three-leaf -> four-leaf, spade -> double spade.
 */
public class GrimoireAltarBlock extends Block {
    public GrimoireAltarBlock(Properties props) {
        super(props.mapColor(MapColor.COLOR_GREEN).strength(2.5f, 6f).lightLevel(s -> 10).sound(SoundType.STONE).noOcclusion());
    }

    private static void glow(ServerPlayer p, BlockPos pos, int color, float power) {
        Vec3 c = Vec3.atCenterOf(pos).add(0, 0.6, 0);
        VfxSpawn.send(p.serverLevel(), VfxShape.MAGIC_CIRCLE, c, c.add(0, 1, 0), color, 40, power);
        p.serverLevel().playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1f, 0.8f);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
        if (GrimoireAcceptance.hasRolled(p)) {
            p.displayClientMessage(Component.literal("Your fate is already written. Hold your grimoire and pray to seek a higher cover.").withStyle(ChatFormatting.GRAY), true);
            glow(p, pos, 0xFF8CFFC2, 0.8f);
            return InteractionResult.CONSUME;
        }
        String soul = NightmareSouls.soulTypeOf(p);
        glow(p, pos, 0xFFFFD86B, 1.4f);
        GrimoireAcceptance.roll(p, soul == null ? "" : soul);
        return InteractionResult.CONSUME;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof GrimoireItem)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return ItemInteractionResult.SUCCESS;
        if (!GrimoireItem.isOwnedBy(stack, p.getUUID())) { p.displayClientMessage(Component.literal("The altar only hears a grimoire's owner.").withStyle(ChatFormatting.RED), true); return ItemInteractionResult.CONSUME; }
        var g = GrimoirePages.grimoireOf(p);
        if (g.isEmpty()) return ItemInteractionResult.CONSUME;
        long day = level.getDayTime() / 24000L;
        var data = p.getPersistentData();
        if (data.getLong("nusmp_altar_day") == day + 1) {
            p.displayClientMessage(Component.literal("The altar has already heard you today.").withStyle(ChatFormatting.GRAY), true);
            return ItemInteractionResult.CONSUME;
        }
        data.putLong("nusmp_altar_day", day + 1);
        GrimoireCover cover = GrimoirePages.coverOf(g.get());
        GrimoireCover next = cover == GrimoireCover.THREE_LEAF ? GrimoireCover.FOUR_LEAF : cover == GrimoireCover.SPADE ? GrimoireCover.DOUBLE_SPADE : null;
        if (next != null && p.getRandom().nextFloat() < 0.05f) {
            MagicGear.setCover(p, g.get(), next);
            glow(p, pos, 0xFFFFE070, 2.0f);
            p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString() + "'s grimoire grows a new leaf at the altar!").withStyle(ChatFormatting.GOLD), false);
        } else {
            glow(p, pos, 0xFF8CFFC2, 0.9f);
            p.displayClientMessage(Component.literal(next == null ? "The altar blesses your grimoire, but your cover cannot grow here." : "The altar is silent today. Return tomorrow.").withStyle(ChatFormatting.GRAY), true);
        }
        return ItemInteractionResult.CONSUME;
    }
}
