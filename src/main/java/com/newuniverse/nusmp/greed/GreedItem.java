package com.newuniverse.nusmp.greed;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.List;

/**
 * Why: the Black Sword Greed in one of its eight forms (one item id each, {@link GreedRules.Form}). It is soul-bound, unbreakable, fire-proof and one in a stack; its truth is the world's record ({@link GreedData}) and
 * the wielder's stats ({@link GreedState}), not the stack. The stack's right click does what its form does ({@link GreedForms}): the bow and the shield are drawn / held, the staff, gauntlets, spear and dual blades cast on
 * click. Only the wielder can use it. Ownership, the form switch and the ghost rules are {@link GreedService}'s; this class is the item's face.
 */
public class GreedItem extends Item {
    public static final int NAME_RGB = 0xB3122B;
    private final GreedRules.Form form;

    public GreedItem(GreedRules.Form form) {
        super(props(form));
        this.form = form;
    }

    public GreedRules.Form form() { return form; }

    private static Properties props(GreedRules.Form f) {
        double dmg = switch (f) {
            case SWORD -> 8.0;
            case BOW -> 3.0;
            case SCYTHE -> 10.0;
            case SHIELD -> 4.0;
            case STAFF -> 4.0;
            case GAUNTLETS -> 7.0;
            case SPEAR -> 9.0;
            case DUAL_BLADES -> 12.0;
        };
        double speed = switch (f) {
            case DUAL_BLADES -> -1.4;
            case SCYTHE -> -2.8;
            case GAUNTLETS -> -1.8;
            default -> -2.4;
        };
        ItemAttributeModifiers.Builder mods = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_damage"), dmg - 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_speed"), speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        return new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant().component(DataComponents.UNBREAKABLE, new Unbreakable(false)).attributes(mods.build());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatableWithFallback("item.anime_multiverse." + form.itemId(), "Black Sword Greed: " + form.english()).withStyle(Style.EMPTY.withColor(NAME_RGB));
    }

    // ---------------------------------------------------------------- using it
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return switch (form) {
            case BOW -> UseAnim.BOW;
            case SHIELD -> UseAnim.BLOCK;
            default -> UseAnim.NONE;
        };
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return form == GreedRules.Form.BOW || form == GreedRules.Form.SHIELD ? 72000 : 0; }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        if (form == GreedRules.Form.SHIELD && ItemAbilities.SHIELD_BLOCK.equals(ability)) return true;
        return super.canPerformAction(stack, ability);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (form == GreedRules.Form.BOW || form == GreedRules.Form.SHIELD) {
            if (!level.isClientSide && player instanceof ServerPlayer sp && !GreedService.mayUse(sp, stack)) return InteractionResultHolder.fail(stack);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp) SafeCast.tick("greed.use", () -> GreedService.use(sp, stack, this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide || form != GreedRules.Form.BOW || !(entity instanceof ServerPlayer sp)) return;
        int charged = getUseDuration(stack, entity) - timeLeft;
        SafeCast.tick("greed.bow", () -> GreedService.release(sp, stack, charged));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof ServerPlayer sp) SafeCast.tick("greed.hit", () -> GreedService.onHit(sp, stack, target, form));
        return true;                                                         // no wear: the sword never breaks
    }

    /** Every tick of the dropped item: it never despawns, is never destroyed, is rescued from the void, and a ghost dissolves. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide) SafeCast.tick("greed.drop", () -> GreedService.itemEntityTick(entity, stack));
        return false;
    }

    @Override public boolean canFitInsideContainerItems() { return false; }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) { return slotChanged || oldStack.getItem() != newStack.getItem(); }

    // ---------------------------------------------------------------- text
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatableWithFallback("greed.tooltip.title", "Mortal Sin: Greed. One in the world, soul-bound.").withStyle(ChatFormatting.DARK_RED));
        lines.add(Component.translatableWithFallback("greed.tooltip.form", "Form %s: %s", form.id(), Component.translatableWithFallback(form.langKey(), form.english())).withStyle(ChatFormatting.RED));
        lines.add(Component.translatableWithFallback("greed.tooltip.technique", "Secret Technique: %s (costs %s%% of the devoured stats)", Component.translatableWithFallback(form.techniqueKey(), form.techniqueEnglish()), (int) Math.round(form.share() * 100)).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatableWithFallback("greed.tooltip.skill", "Its wielder holds the Ultimate skill Lord of Gluttonous Greed: the skill key casts the Secret Technique, devours and offers.").withStyle(ChatFormatting.DARK_GRAY));
        if (GreedHands.isTwin(form.itemId())) lines.add(Component.translatableWithFallback("greed.tooltip.twin", "Twin form: one in each hand. With the off hand empty every blow is followed by a second one from it (60%).").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatableWithFallback("greed.tooltip.switch", "Form key (default R): next open form; sneak: previous. Forms open with tiers.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
