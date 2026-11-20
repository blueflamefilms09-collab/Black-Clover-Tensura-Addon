package com.newuniverse.nusmp.lords;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Why: form 1 or form 2 of one of the four Mortal Sin weapons (form 0 is the old {@code sinweapons.SinWeaponItem}, kept). One item id per form; the form switch swaps the id in the slot and keeps the components. It is
 * soul-bound, unbreakable, fire-proof and one in a stack; its truth is the world's record ({@link LordData}) and the wielder's mastery ({@link LordState}), not the stack. The right click is the form's art
 * ({@link LordArts}); everything is {@link LordService}'s and runs on the server. A melee weapon first (Better Combat file per item).
 */
public class LordItem extends SwordItem {
    private final LordRules.Sin sin;
    private final int form;

    public LordItem(LordRules.Sin sin, int form, float base, float speed) {
        super(Tiers.NETHERITE, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant().component(DataComponents.UNBREAKABLE, new Unbreakable(false))
                .attributes(SwordItem.createAttributes(Tiers.NETHERITE, base, speed)));
        this.sin = sin;
        this.form = form;
    }

    public LordRules.Sin sin() { return sin; }

    public int form() { return form; }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatableWithFallback("item.anime_multiverse." + sin.items[form], sin.formNames[form]).withStyle(Style.EMPTY.withColor(sin.rgb));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) SafeCast.tick("lords.use", () -> LordService.useArt(sp, stack, sin, form, this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** The skill's Technique mode: the same art, gates and cooldown as the right click. */
    public void fire(ServerPlayer sp) {
        ItemStack stack = sp.getMainHandItem();
        if (stack.getItem() == this) SafeCast.tick("lords.use", () -> LordService.useArt(sp, stack, sin, form, this));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        if (attacker instanceof ServerPlayer sp) SafeCast.tick("lords.hit", () -> LordService.onHit(sp, stack, target, sin));
        return r;
    }

    /** Every tick of the dropped item: it never despawns, is never destroyed, is rescued from the void, and a ghost dissolves. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide) SafeCast.tick("lords.drop", () -> LordService.itemEntityTick(entity, stack, sin));
        return false;
    }

    @Override public boolean canFitInsideContainerItems() { return false; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) { LordText.tooltip(lines, sin, form); }
}
