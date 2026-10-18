package com.newuniverse.nusmp.heroweapon.gem;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Why: in the series a hero is not given a weapon, the weapon CHOOSES the hero, so the way in is a held ritual and not a click: hold right click, stand still
 * in the open, and either one of the four Legendary Weapons answers (the Holy Calling Gem) or the named Seven Star or Vassal Weapon does (a Vassal Seal). One
 * class serves all eighteen gems: {@link #kind()} is null for the Holy Calling Gem and the weapon for a Seal. The item only owns the hold (it uses the same
 * vanilla item-use hooks as the Hourglass of the Final Hour and the Chronicle Sigil: {@code use} starts it, {@code onUseTick} drives it, {@code finishUsingItem}
 * answers, {@code releaseUsing} ends it early); what is checked, shown and decided lives in {@link GemChoosing} and the pure {@link GemRules} and
 * {@link GemRitual}. The client cannot know the server's verdict, so it always shows the drawn-bow pose; the server decides what really happens.
 */
public class CallingGemItem extends Item {
    private final HeroKind kind;

    /** @param kind the weapon this Seal calls, or null for the Holy Calling Gem */
    public CallingGemItem(HeroKind kind, Properties props) {
        super(props);
        this.kind = kind;
    }

    /** The weapon this Seal calls, or null for the Holy Calling Gem. */
    public HeroKind kind() { return kind; }

    public boolean isHolyGem() { return kind == null; }

    @Override
    public Component getName(ItemStack stack) {
        return GemText.msg("item.nusmp." + (kind == null ? GemCatalog.HOLY_ID : GemCatalog.sealId(kind)));
    }

    // ---------------------------------------------------------------- the hold
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.fail(stack);
        if (sp.isUsingItem()) return InteractionResultHolder.consume(stack);                    // a repeated click packet while the ritual runs
        if (GemChoosing.begin(sp, this) != GemRules.Denial.NONE) return InteractionResultHolder.fail(stack);
        sp.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return GemConfig.ritualTicks(); }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide || !(entity instanceof ServerPlayer p)) return;
        GemChoosing.tick(p);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer p)) return stack;
        return GemChoosing.finish(p, this, stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide && entity instanceof ServerPlayer p) GemChoosing.released(p);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    // ---------------------------------------------------------------- the tooltip
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tip, TooltipFlag flag) {
        if (kind == null) {
            tip.add(GemText.msg(GemMessages.TIP_HOLY_KIND).withStyle(ChatFormatting.GOLD));
            tip.add(GemText.msg(GemMessages.TIP_HOLY_LORE).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        } else {
            tip.add(GemText.msg(GemMessages.TIP_SEAL_KIND, kind.heroClass().label()).withStyle(ChatFormatting.GOLD));
            tip.add(GemText.msg(GemMessages.TIP_SEAL_LORE, kind.display()).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            if (!kind.holder().isEmpty()) tip.add(GemText.msg(GemMessages.TIP_SEAL_HOLDER, kind.holder()).withStyle(ChatFormatting.GRAY));
        }
        tip.add(GemText.msg(GemMessages.TIP_USE, String.valueOf(GemConfig.get(GemConfig.RITUAL_SECONDS))).withStyle(ChatFormatting.GRAY));
        tip.add(GemText.msg(kind == null ? GemMessages.TIP_HOLY_RULE : GemMessages.TIP_SEAL_RULE).withStyle(ChatFormatting.DARK_GRAY));
    }
}
