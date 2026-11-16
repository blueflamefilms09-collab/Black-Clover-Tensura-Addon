package com.newuniverse.nusmp.beastwill;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;

import java.util.List;

/**
 * Why: the Beast Will Core (3.45.0). Using it does NOT absorb it: it asks the server to check the player and show the preview ({@link BeastWillAcquire#open}); the core is only taken when the player
 * confirms and every check still holds. A core whose will no longer exists (a removed data file) just says so and stays in the inventory.
 */
public final class BeastWillCoreItem extends Item {
    public BeastWillCoreItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            BeastWillComponents.Core c = BeastWillComponents.of(stack);
            if (c == null) c = stampBare(sp, stack);   // a bare core (a plain /give) becomes a core of the first will the player does not hold, so the ask always opens
            if (!BeastWillAcquire.open(sp, c.will(), c.beast(), c.rankValue(), BeastWillAcquire.CORE, c.serial())) BeastWillFeedback.say(sp, BeastWillConfig.enabled() ? "no.unknown_will" : "no.disabled", true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static BeastWillComponents.Core stampBare(ServerPlayer p, ItemStack stack) {
        BeastWillState held = BeastWillStore.peek(p);
        BeastWillCores.Entry e = BeastWillCores.firstFree(id -> held != null && held.has(id));
        BeastWillComponents.Core c = new BeastWillComponents.Core(e.will(), e.beast(), e.rank().name(), java.util.UUID.randomUUID().toString());
        stack.set(BeastWillComponents.CORE.get(), c);
        return c;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tip, TooltipFlag flag) {
        BeastWillComponents.Core c = BeastWillComponents.of(stack);
        if (c == null) {
            tip.add(Component.translatable("item.anime_multiverse.beast_will_core.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        tip.add(Component.translatable("item.anime_multiverse.beast_will_core.of", BeastWillText.name(c.will())).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("item.anime_multiverse.beast_will_core.beast", c.beast().isBlank() ? "?" : c.beast(), c.rankValue().name()).withStyle(ChatFormatting.GRAY));
        String lore = "anime_multiverse.beastwill." + c.will() + ".lore";
        String canon = "anime_multiverse.beastwill." + c.will() + ".canon_lore", source = "anime_multiverse.beastwill." + c.will() + ".canon_source";
        net.minecraft.locale.Language lang = net.minecraft.locale.Language.getInstance();
        if (BeastWillConfig.canonRework() && lang.has(canon)) {                                                          // the canon rework: what the source says, then where it comes from
            tip.add(Component.translatable(canon).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
            if (lang.has(source)) tip.add(Component.translatable(source).withStyle(ChatFormatting.DARK_GRAY));
        } else if (lang.has(lore)) tip.add(Component.translatable(lore).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));   // 3.60.0: the remade will's lore line
        tip.add(Component.translatable("item.anime_multiverse.beast_will_core.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
