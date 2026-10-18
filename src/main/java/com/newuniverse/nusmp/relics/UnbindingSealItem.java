package com.newuniverse.nusmp.relics;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
 * The Unbinding Seal (1.84.0, owner: "add an in-game item to craft that can remove attunements"). Hold it in one hand and an attuned relic in the other and use it: the relic lets go of whoever it
 * was attuned to (the same {@link Attune#release} the engine uses, so a pair loses both halves' owner together), and the seal is used up. Why a held item and not a crafting recipe: the relic keeps
 * all its other data (tier, sockets, engravings, charges), which a grid recipe would have to copy component by component; this way nothing but the attunement changes. Server-side only; the
 * decision is a small pure function ({@link AttuneRules#unbind}) so the rules are tested without a game.
 */
public class UnbindingSealItem extends Item {
    public UnbindingSealItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack seal = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(seal);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        RelicDef def = other.isEmpty() ? null : Relics.defOf(other);
        boolean attuned = def != null && Attune.owner(other) != null;
        switch (AttuneRules.unbind(def != null, attuned)) {
            case NO_RELIC -> sp.displayClientMessage(Component.translatableWithFallback("message.nusmp.unbinding_seal.no_relic", "Hold a relic in your other hand."), true);
            case NOT_ATTUNED -> sp.displayClientMessage(Component.translatableWithFallback("message.nusmp.unbinding_seal.not_attuned", "That relic is not attuned to anyone."), true);
            case RELEASED -> {
                Attune.release(sp, other);
                if (!sp.getAbilities().instabuild) seal.shrink(1);
                level.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.7f);
                sp.displayClientMessage(Component.translatableWithFallback("message.nusmp.unbinding_seal.released", "The seal dissolves. %s is no longer attuned to anyone.", other.getHoverName()), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(seal, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("item.nusmp.unbinding_seal.tip", "Hold it in one hand and an attuned relic in the other, then use it: the relic is freed from its attunement.").withStyle(ChatFormatting.LIGHT_PURPLE));
        tip.add(Component.translatableWithFallback("item.nusmp.unbinding_seal.tip2", "Used up. Only Legend grade and above relics need attunement.").withStyle(ChatFormatting.GRAY));
    }
}
