package com.newuniverse.nusmp.avatar.item;

import com.newuniverse.nusmp.avatar.AvatarConfig;
import com.newuniverse.nusmp.avatar.AvatarSoulForgeConfig;
import com.newuniverse.nusmp.avatar.menu.AvatarBuilderMenu;
import com.newuniverse.nusmp.avatar.ritual.SoulForgeRitual;
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
 * Why: the Genesis Core is the key to creating an Avatar (owner section 1). It is NOT used up by opening a screen, only when an Avatar is actually created
 * ({@link com.newuniverse.nusmp.avatar.AvatarCreation#finalizeAvatar} consumes one), so a player can look, roll and walk away. It drops from the dungeon
 * bosses ({@link GenesisCoreDrops}) and, since 1.58.0, it can be crafted from rare items (data/nusmp/recipe/genesis_core.json); the Soul Forge block is built
 * around one. 1.58.0: the item no longer starts a creation on its own: with {@code coreUse = "forge"} (the default of {@link AvatarSoulForgeConfig}) it only
 * works at a Soul Forge and shows a hint anywhere else; {@code "item"} keeps the old direct use (which opens the Soul Forge screen at the player's feet, or
 * the classic screen when {@code creationUi = "classic"}). In the hand it is a small glowing 3D core (model built by tools/gen_avatar_assets.py), with
 * its 16x16 sprite kept as the inventory icon.
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
        if (!AvatarSoulForgeConfig.coreWorksAlone()) {
            sp.displayClientMessage(Component.translatableWithFallback("message.nusmp.avatar.core_needs_forge", "The Genesis Core only wakes at a Soul Forge.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return InteractionResultHolder.fail(stack);
        }
        if (AvatarSoulForgeConfig.soulForgeUi()) SoulForgeRitual.interact(sp, null);
        else AvatarBuilderMenu.open(sp);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("item.nusmp.genesis_core.desc1", "Take it to a Soul Forge to design an Avatar. It is spent only when the Avatar is born.").withStyle(ChatFormatting.GRAY));
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

    /** 1.58.0: how many cores the player carries (main inventory and off hand; a creative player counts as having plenty, see {@link #hasCore}). */
    public static int countCores(ServerPlayer p) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (is(s)) n += s.getCount();
        if (is(p.getOffhandItem())) n += p.getOffhandItem().getCount();
        return n;
    }

    /**
     * 1.58.0: removes {@code n} cores (the main inventory first, then the off hand) and returns whether all of them were removed; nothing is removed when
     * the player does not have that many (the check comes first, so a half payment never happens). Creative players pay nothing.
     */
    public static boolean consume(ServerPlayer p, int n) {
        if (n <= 0 || p.isCreative()) return true;
        if (countCores(p) < n) return false;
        int left = n;
        for (ItemStack s : p.getInventory().items) {
            if (left <= 0) break;
            if (is(s)) { int take = Math.min(left, s.getCount()); s.shrink(take); left -= take; }
        }
        ItemStack off = p.getOffhandItem();
        if (left > 0 && is(off)) { int take = Math.min(left, off.getCount()); off.shrink(take); left -= take; }
        return left <= 0;
    }
}
