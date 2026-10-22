package com.newuniverse.nusmp.fairytail.storm;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Why: Storm Magic is a Fairy Tail magic and is tied to a staff, not to a grimoire. The Stormcaller Staff is that staff: hold it and use it to cast the chosen storm spell, sneak and
 * use to turn to the next one. The spells come out of the staff (the clients draw the cloud gathering over its head-stone and the bolts leaving the rod), cost Aura and magicules by the
 * mod's mana law, and have their own cooldowns per player. The choice is stored in the stack (custom data {@code StormSel}); everything else (cooldowns, the live storms, the caps) is
 * decided on the server and keyed by the player's UUID. The staff itself is an ordinary weapon in melee (a light blow, a Better Combat staff profile) and carries Tensura engravings
 * ({@code item/WeaponEngravings}). The client only asks (use) and draws: {@code use} does nothing on the client but the swing.
 */
public class StormStaffItem extends Item {
    public static final String SELECTED = "StormSel";

    public StormStaffItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant().attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.6, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build()));
    }

    // ------------------------------------------------------------------------------------------------------------------ the chosen spell
    /** The spell chosen on a stack (the first one for a stack that has none or a damaged value). */
    public static StormSpell selected(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? StormSpell.at(0) : StormSpell.at(d.copyTag().getInt(SELECTED));
    }

    public static void select(ItemStack stack, StormSpell spell) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(SELECTED, spell.ordinal()));
    }

    // ------------------------------------------------------------------------------------------------------------------ use
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.success(stack);       // the client only swings; the server decides
        if (p.isShiftKeyDown()) {
            StormSpell next = selected(stack).next();
            select(stack, next);
            p.displayClientMessage(Component.translatableWithFallback("nusmp.storm.selected", "Storm spell: %s",
                    Component.translatableWithFallback("nusmp.storm.spell." + next.id, next.displayName).withStyle(ChatFormatting.AQUA)), true);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }
        StormField.Cast r = StormField.cast(p, selected(stack));
        if (!r.done()) {
            StormField.tell(p, r);
            return InteractionResultHolder.fail(stack);
        }
        p.getCooldowns().addCooldown(this, 10);                         // a short lock so a double-sent use cannot cast twice
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    // ------------------------------------------------------------------------------------------------------------------ tooltip
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("nusmp.storm.tooltip.magic", "Fairy Tail: Storm Magic").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatableWithFallback("item.nusmp.stormcaller_staff.lore", "A staff of storm cloud and rod. The storm does not come from a book: it comes from the staff.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        StormSpell sel = selected(stack);
        tip.add(Component.translatableWithFallback("nusmp.storm.tooltip.selected", "Selected: %s", Component.translatableWithFallback("nusmp.storm.spell." + sel.id, sel.displayName).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatableWithFallback("nusmp.storm.spell." + sel.id + ".desc", sel.description).withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.translatableWithFallback("nusmp.storm.tooltip.use", "Use: cast. Sneak + use: next spell.").withStyle(ChatFormatting.BLUE));
        if (flag.isAdvanced()) {
            for (StormSpell s : StormSpell.values()) {
                tip.add(Component.literal((s == sel ? "> " : "  ") + s.displayName + "  " + s.costPercent + "% Aura, " + s.cooldownTicks / 20 + " s").withStyle(s == sel ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));
            }
        }
    }
}
