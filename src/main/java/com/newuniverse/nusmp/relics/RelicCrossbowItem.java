package com.newuniverse.nusmp.relics;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A relic crossbow. Loading (the charge duration of {@link CrossbowItem#getChargeDuration}, so Quick Charge counts), the bolt search and
 * consumption, the loaded state (the vanilla charged-projectiles component, so {@link CrossbowItem#isCharged} and fireworks work) and the shot are
 * vanilla's own; the crossbow never breaks. Sneak + use fires the relic's act instead of loading or shooting (see {@link RelicRangedSupport}).
 * While an act or the attunement ritual is held the use duration is the relic hold of an hour, as for every relic, not the short vanilla charge.
 * In hand it is a 3D mesh: &lt;id&gt;, &lt;id&gt;_pull1..3 while loading, &lt;id&gt;_charged once loaded (client/relics/item).
 */
public class RelicCrossbowItem extends CrossbowItem implements RelicBearer {
    private final RelicDef def;

    public RelicCrossbowItem(RelicDef def) {
        super(new Item.Properties().stacksTo(1).rarity(RelicItemSupport.rarity(def.grade())).fireResistant()
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false)).attributes(RelicRangedSupport.attributes(def)));
        this.def = def;
    }

    @Override public RelicDef relic() { return def; }

    /** Stack-borne state (a counter, a charge, a drum) changes the stack every so often: that must not replay the draw animation. */
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) { return slotChanged || oldStack.getItem() != newStack.getItem(); }

    @Override public Component getName(ItemStack stack) { return RelicItemSupport.name(super.getName(stack), def); }

    @Override public boolean isFoil(ItemStack stack) { return def.grade().atLeast(Grade.LEGEND) || super.isFoil(stack); }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return RelicRangedSupport.actMode(user) ? RelicItemSupport.USE_TICKS : super.getUseDuration(stack, user);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        InteractionResultHolder<ItemStack> act = RelicRangedSupport.intercept(level, player, hand, def);
        return act != null ? act : super.use(level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!RelicRangedSupport.handleTick(level, user, stack, remaining, def)) super.onUseTick(level, user, stack, remaining);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (!RelicRangedSupport.handleRelease(stack, level, user, remaining, def)) super.releaseUsing(stack, level, user, remaining);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tip, flag);
        RelicRangedSupport.hint(tip, def, "tooltip.nusmp.relic.crossbow_acts");
    }
}
