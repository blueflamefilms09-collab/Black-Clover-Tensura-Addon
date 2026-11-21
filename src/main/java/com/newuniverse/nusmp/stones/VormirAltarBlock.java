package com.newuniverse.nusmp.stones;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Why: the Soul Stone's trial had only a command; this is the physical altar of Vormir. Right-click it and the server tells the bearer what the altar asks (a clickable line for the highest-mastery
 * skill, a suggested command for a named pet); the offer itself is the same server-checked command as before ({@code /multiverse stones offer ...}: the EP gate, no keeper of the Soul Stone, nothing
 * consumed before the stone is certain to be issued, never a player, never EP). With the setting {@code altarRequired} on, the trial works only within six blocks of an altar; off (the default), the
 * command still works anywhere as in Phase A. The block does nothing else: no block entity, no ticking, no world generation (an operator or creative player places it; a Vormir structure is not built).
 */
public class VormirAltarBlock extends Block {
    public VormirAltarBlock(Properties props) {
        super(props.mapColor(MapColor.COLOR_ORANGE).strength(50.0f, 1200.0f).lightLevel(s -> 7).sound(SoundType.DEEPSLATE).noOcclusion());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
        long now = level.getGameTime();
        if (now < p.getPersistentData().getLong("nusmp_infb_altar")) return InteractionResult.CONSUME;          // one answer a second, so a held click is not a chat flood
        p.getPersistentData().putLong("nusmp_infb_altar", now + 20L);
        if (!StoneConfig.altar()) {
            p.displayClientMessage(Component.literal("The altar is silent on this server.").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        String refusal = StoneCommand.trialRefusal(p, true);
        if (refusal != null) {
            p.displayClientMessage(Component.literal(refusal).withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        level.playSound(null, pos, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 1.0f, 0.6f);
        MutableComponent skill = Component.literal("[Offer your best skill]").withStyle(s -> s.withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/multiverse stones offer skill"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Shows which skill the altar would take. Nothing is lost until you confirm."))));
        MutableComponent pet = Component.literal("[Offer a named pet]").withStyle(s -> s.withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/multiverse stones offer pet "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Type the name of your pet after the command. It must stand within sixteen blocks."))));
        p.sendSystemMessage(Component.literal("The altar of Vormir asks for what you love. What is given is gone for good and never returned. ").withStyle(ChatFormatting.GOLD)
                .append(skill).append(Component.literal(" ")).append(pet));
        return InteractionResult.CONSUME;
    }

    /** Is an altar within six blocks of the player? A box of 13 x 13 x 13 block reads, once per command (never per tick). */
    public static boolean near(ServerPlayer p) {
        BlockPos c = p.blockPosition();
        net.minecraft.world.level.block.Block altar = com.newuniverse.nusmp.block.NUBlocks.VORMIR_ALTAR.get();
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-6, -6, -6), c.offset(6, 6, 6))) {
            if (p.level().getBlockState(q).is(altar)) return true;
        }
        return false;
    }
}
