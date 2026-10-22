package com.newuniverse.nusmp.shishou;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Why: the SOULBOUND item of Shishou: the talking sword in the wielder's hand. It is unbreakable and fire-proof (Ultra-Speed Regeneration: wear is paid in sword mana, never durability), one in a stack, and its
 * truth is the world's record ({@link ShishouData}), not the stack: the stack carries only the wielder, the token that makes it THE stack, and a mirror of the level ({@link ShishouComponents}). Each tick in an
 * inventory it asks the service (cheap: a token compare, and once a second the heavy part); a non-wielder who holds it is punished and, after ten seconds, it tears itself away and goes the recovery way;
 * a dropped stack is rescued from the void and never despawns ({@link #onEntityItemUpdate}). Shift + right click opens Stomach. The tooltip shows "Akashic Relic - Grade: EX-Singularity", a separate display
 * tag: the relic {@code Grade} enum (and its saved ordinals) is not touched.
 */
public class ShishouItem extends Item {
    public static final int NAME_RGB = 0x7FEBFF, GRADE_RGB = 0xFFD76A;

    public ShishouItem(Properties properties) { super(properties); }

    /** One in a stack, epic, fire-proof, unbreakable; the melee numbers are those of a long sword (the level adds to them through the attribute event). */
    public static Properties props() {
        ItemAttributeModifiers.Builder mods = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_damage"), ShishouRules.BASE_ATTACK - 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_speed"), -2.4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(ResourceLocation.fromNamespaceAndPath("nusmp", "shishou_reach"), 0.5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        return new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant().component(DataComponents.UNBREAKABLE, new Unbreakable(false)).attributes(mods.build());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatableWithFallback("item.nusmp.shishou", "Shishou").withStyle(Style.EMPTY.withColor(NAME_RGB));
    }

    // ---------------------------------------------------------------- the tick in an inventory
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof ServerPlayer p)) return;
        boolean held = selected || p.getOffhandItem() == stack;
        SafeCast.tick("shishou.hold", () -> ShishouService.holderTick(p, stack, held));
    }

    /** Every tick of the dropped item: never despawns, never destroyed by fire or explosions, rescued from the void, a copy dissolves. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide) SafeCast.tick("shishou.drop", () -> ShishouService.itemEntityTick(entity, stack));
        return false;
    }

    // ---------------------------------------------------------------- using it
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) SafeCast.tick("shishou.stomach", () -> ShishouService.openStorage(sp));
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player p = ctx.getPlayer();
        if (p != null && p.isShiftKeyDown()) {
            if (!ctx.getLevel().isClientSide && p instanceof ServerPlayer sp) SafeCast.tick("shishou.stomach", () -> ShishouService.openStorage(sp));
            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
        }
        return InteractionResult.PASS;
    }

    // ---------------------------------------------------------------- feeding from the inventory (2.1.0)
    /** The sword on the cursor, clicked on a stack in your inventory: left click eats the stack (up to the per-click cap), right click one item. */
    @Override
    public boolean overrideStackedOnOther(ItemStack sword, net.minecraft.world.inventory.Slot slot, net.minecraft.world.inventory.ClickAction action, Player player) {
        return ShishouFeedService.swordOnStack(sword, slot, action, player);
    }

    /** A stack on the cursor, clicked on the sword in your inventory (the pattern of the Djinn Seal). */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack sword, ItemStack cursor, net.minecraft.world.inventory.Slot slot, net.minecraft.world.inventory.ClickAction action, Player player, net.minecraft.world.entity.SlotAccess access) {
        return ShishouFeedService.stackOnSword(sword, cursor, slot, action, player);
    }

    // ---------------------------------------------------------------- blows
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (!player.level().isClientSide) ShishouService.noteSwing(player);
        return false;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof ServerPlayer sp) SafeCast.tick("shishou.hit", () -> ShishouService.onHit(sp, stack, target));
        return true;                                                         // no wear: Ultra-Speed Regeneration pays in mana
    }

    @Override public boolean canFitInsideContainerItems() { return false; }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) { return slotChanged || oldStack.getItem() != newStack.getItem(); }

    // ---------------------------------------------------------------- text
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatableWithFallback("shishou.grade", "Akashic Relic - Grade: EX-Singularity").withStyle(Style.EMPTY.withColor(GRADE_RGB)));
        lines.add(Component.translatableWithFallback("shishou.tooltip.level", "Level %s", ShishouComponents.level(stack)).withStyle(net.minecraft.ChatFormatting.AQUA));
        lines.add(Component.translatableWithFallback("shishou.tooltip.soulbound", "Soulbound. Only its wielder may hold it.").withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.translatableWithFallback("shishou.tooltip.stomach", "Shift + right click: Stomach (internal storage).").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        lines.add(Component.translatableWithFallback("shishou.tooltip.skill_route", "Skills: the Ultimate skill Shishou (Tensura skill screen: mode key, skill key).").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        lines.add(Component.translatableWithFallback("shishou.tooltip.feed", "Click a stack of monster drops or magic stones on me in your inventory to feed me.").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        int shown = 0;
        int level = ShishouComponents.level(stack);
        List<ShishouSkill> have = ShishouSkill.availableList(level, 0L);
        for (int i = have.size() - 1; i >= 0 && shown < 4; i--) {
            ShishouSkill s = have.get(i);
            lines.add(Component.translatableWithFallback("shishou.tooltip.skill", "%s (%s)", Component.translatableWithFallback(s.langKey(), s.english()),
                    Component.translatableWithFallback(s.langKey() + ".jp", s.romaji())).withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
            shown++;
        }
    }
}
