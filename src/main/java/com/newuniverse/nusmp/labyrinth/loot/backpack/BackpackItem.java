package com.newuniverse.nusmp.labyrinth.loot.backpack;

import com.newuniverse.nusmp.labyrinth.loot.LootConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Why: the backpack item is the same object in three states: carried (a stack with a tier and, in the vanilla container component, its contents), opened in the hand (use it in the
 * air and its inventory opens as a window, nothing is placed), and set down (use it on a block and it becomes the block, taking its tier and its contents with it; take it back
 * and it returns to this item with both). The tier lives in {@code nusmp:backpack_tier} (a stack without it is leather), the contents in {@code minecraft:container}, so a pack that
 * is dropped, traded, stored in a chest or lost to a creeper and picked up again is exactly what it was. A backpack cannot hold a backpack (a container inside itself would be an
 * item-duplication and size trap), which is why it answers {@code canFitInsideContainerItems} with no and the slots refuse it. Stacks to one. The hover text says the tier, the slots
 * and how many are filled, never the items themselves (the window is for that).
 */
public class BackpackItem extends BlockItem {
    public BackpackItem(Block block, Properties props) { super(block, props); }

    // ---------------------------------------------------------------- the tier
    /** The tier a backpack stack is; a stack with no (or a stale) tier is leather. */
    public static BackpackTier tierOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return BackpackTier.LEATHER;
        Integer t = stack.get(BackpackRegistry.TIER.get());
        return BackpackTier.of(t == null ? 0 : t);
    }

    public static void setTier(ItemStack stack, BackpackTier tier) { stack.set(BackpackRegistry.TIER.get(), tier.ordinal()); }

    /** A new, empty backpack of this tier. */
    public static ItemStack of(BackpackTier tier) {
        ItemStack s = new ItemStack(BackpackRegistry.ITEM.get());
        setTier(s, tier);
        return s;
    }

    /** How many slots of this backpack stack hold something. */
    public static int filled(ItemStack stack) {
        ItemContainerContents c = stack.get(DataComponents.CONTAINER);
        int n = 0;
        if (c != null) for (ItemStack s : c.nonEmptyItems()) if (!s.isEmpty()) n++;
        return n;
    }

    // ---------------------------------------------------------------- behaviour
    @Override
    public boolean canFitInsideContainerItems() { return false; }

    @Override
    public ItemStack getDefaultInstance() { return of(BackpackTier.LEATHER); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!LootConfig.get(LootConfig.BACKPACK_HAND)) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide && player instanceof ServerPlayer sp) openHeld(sp, stack, hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Opens the window of the backpack in the hand. The slot is the player-inventory index of the stack (the selected hotbar slot, or 40 for the off hand). */
    static void openHeld(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        BackpackTier tier = tierOf(stack);
        int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
        HeldBackpack container = new HeldBackpack(stack, tier);
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new BackpackMenu(id, inv, container, tier, slot), stack.getHoverName()), buf -> {
            buf.writeVarInt(tier.ordinal());
            buf.writeVarInt(slot);
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, net.minecraft.world.item.TooltipFlag flag) {
        BackpackTier tier = tierOf(stack);
        tip.add(Component.translatableWithFallback("item.nusmp.backpack.tier." + tier.id(), "Tier: " + tier.id() + ", " + tier.slots() + " slots").withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatableWithFallback("item.nusmp.backpack.filled", "%s of %s slots used", filled(stack), tier.slots()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatableWithFallback("item.nusmp.backpack.lore", "Use it in the air to open it, on the ground to set it down. It keeps what you packed.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
