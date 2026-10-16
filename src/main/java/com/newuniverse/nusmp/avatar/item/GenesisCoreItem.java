package com.newuniverse.nusmp.avatar.item;

import com.newuniverse.nusmp.avatar.AvatarConfig;
import com.newuniverse.nusmp.avatar.menu.AvatarBuilderMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Why: the Genesis Core is the key to creating an Avatar (owner section 1). Using it opens the creation GUI; it is NOT used up by opening it, only when an
 * Avatar is actually created ({@link com.newuniverse.nusmp.avatar.AvatarCreation#finalizeAvatar} consumes one), so a player can look, roll and walk away.
 * It drops only from the dungeon bosses ({@link GenesisCoreDrops}). In the hand it is a small glowing 3D core (model built by tools/gen_avatar_assets.py),
 * with its 16x16 sprite kept as the inventory icon.
 */
public class GenesisCoreItem extends Item {
    public GenesisCoreItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (!AvatarConfig.enabled()) {
            sp.displayClientMessage(Component.literal("Avatars are disabled on this server.").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        AvatarBuilderMenu.open(sp);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Use to design an Avatar. It is spent only when the Avatar is created.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Found in the hearts of the dungeon's great beasts.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    // ---------------------------------------------------------------- helpers shared with the creation service
    public static boolean is(ItemStack s) { return s.getItem() instanceof GenesisCoreItem; }

    /** True when the player holds a Genesis Core anywhere in their inventory, or is in creative mode (creative needs no core). */
    public static boolean hasCore(ServerPlayer p) {
        if (p.isCreative()) return true;
        for (ItemStack s : p.getInventory().items) if (is(s)) return true;
        return is(p.getOffhandItem());
    }

    /** Removes one core (the main inventory first, then the off hand). Returns whether one was removed. */
    public static boolean consume(ServerPlayer p) {
        for (ItemStack s : p.getInventory().items) if (is(s)) { s.shrink(1); return true; }
        ItemStack off = p.getOffhandItem();
        if (is(off)) { off.shrink(1); return true; }
        return false;
    }
}
