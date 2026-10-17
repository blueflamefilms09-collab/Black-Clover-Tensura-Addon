package com.newuniverse.nusmp.labyrinth.mega.system;

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
 * Why: the Shadow Core is the System's second reward ("a 'Shadow Core' item, which can be used to extract the souls of defeated Holy enemies as summoned
 * minions"). It is an ordinary right-click item: using it calls {@link ShadowExtraction#use}, which finds the creature this player has just slain nearby and
 * raises it as a shadow, spending one of the core's charges; a spent core turns into nothing. All the rules live in the extraction class; this class is the
 * item (the charges on the stack, the tooltip, the click) and stays thin. In the hand it is a 3D model (a small hovering dark crystal with a violet heart, built
 * by {@code tools/gen_weapon_models.py}), with its 16x16 sprite kept as the inventory icon. A trinket, not a weapon, so it carries no engravings.
 */
public class ShadowCoreItem extends Item {
    public ShadowCoreItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        return ShadowExtraction.use(sp, stack) == ShadowExtraction.Result.RAISED ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(SystemMsg.text(SystemText.TIP_CHARGES, charges(stack), CoreCharges.MAX).withStyle(ChatFormatting.LIGHT_PURPLE));
        tip.add(SystemMsg.text(SystemText.TIP_USE, SystemConfig.get(SystemConfig.EXTRACT_WINDOW_TICKS) / 20).withStyle(ChatFormatting.GRAY));
        tip.add(SystemMsg.text(SystemText.TIP_LIMIT, SystemConfig.get(SystemConfig.MAX_MINIONS)).withStyle(ChatFormatting.GRAY));
        tip.add(SystemMsg.text(SystemText.TIP_LORE).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    // ---------------------------------------------------------------- the stack
    /** Is this stack a Shadow Core? */
    public static boolean is(ItemStack s) { return s != null && !s.isEmpty() && s.getItem() instanceof ShadowCoreItem; }

    /** The extractions this core has left; a stack with no component is a full core. */
    public static int charges(ItemStack s) { return CoreCharges.clamp(s.getOrDefault(ShadowItems.CHARGES.get(), CoreCharges.START)); }

    public static void setCharges(ItemStack s, int n) { s.set(ShadowItems.CHARGES.get(), CoreCharges.clamp(n)); }

    /** A new, full core. */
    public static ItemStack create() { return new ItemStack(ShadowItems.SHADOW_CORE.get()); }

    /** A core with this many charges. */
    public static ItemStack create(int charges) {
        ItemStack s = create();
        setCharges(s, charges);
        return s;
    }
}
