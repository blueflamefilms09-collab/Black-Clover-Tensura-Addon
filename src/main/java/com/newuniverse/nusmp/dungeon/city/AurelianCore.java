package com.newuniverse.nusmp.dungeon.city;

import com.newuniverse.nusmp.dungeon.RadiationShield;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Aurelian Core: the gift of a Golden City's altar, a heart of the old city's magicules. Used, it is spent and wraps the user in a Golden
 * Ward for twenty minutes, in which the magicule radiation of the deep cannot take hold (see {@link RadiationShield}).
 */
public final class AurelianCore extends Item {
    /** Twenty minutes of Golden Ward. */
    public static final int WARD_TICKS = 20 * 60 * 20;

    public AurelianCore(Item.Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(level instanceof ServerLevel sl)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        RadiationShield.ward(player, WARD_TICKS);
        if (!player.isCreative()) stack.shrink(1);
        sl.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.5f);
        sl.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.4f, 0.8f);
        sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.5, 0.9, 0.5, 0.08);
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.4, 0.8, 0.4, 0.3);
        player.displayClientMessage(Component.literal("Gold closes round you like a held breath: the Golden Ward is up for twenty minutes.").withStyle(ChatFormatting.GOLD), true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Use: spend the core to raise a Golden Ward for 20 minutes.").withStyle(ChatFormatting.GOLD));
        tip.add(Component.literal("  The magicule radiation of the Golden City cannot take hold while it holds.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("The still-warm heart of a city that was asleep, given once to each who comes to its altar.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
