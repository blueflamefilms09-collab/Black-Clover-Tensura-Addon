package com.newuniverse.nusmp.relics.djinn.weapon;

import com.newuniverse.nusmp.relics.djinn.Djinn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The twelve Djinn weapons (and their twelve dark twins): the temporary item a Half or Full Djinn Equip puts in the bearer's hand. One class; the
 * Djinn and the look (light or dark) decide the numbers ({@link DjinnWeaponRules}), the mesh, the skin and the engravings. A right-click is the first
 * act, a sneak + right-click the second ({@link DjinnWeaponActs}); the blow is a normal melee blow, and the Djinn's Half Equip special still rides on
 * it ({@code DjinnMagic.onOutgoing}). The item never breaks. Where it lives, when it goes and what is done with the item it displaced is
 * {@link DjinnWeaponService}'s; this class only polices itself: a weapon that is not its bearer's, or whose Equip is over, dissolves on the next tick.
 */
public class DjinnWeaponItem extends Item {
    private final Djinn djinn;
    private final boolean dark;

    public DjinnWeaponItem(Djinn djinn, boolean dark, Properties props) {
        super(props);
        this.djinn = djinn;
        this.dark = dark;
    }

    /** The item properties of a Djinn weapon: never breaks, melee numbers from the table. */
    public static Properties props(Djinn d, boolean dark) {
        DjinnWeaponRules.Spec s = DjinnWeaponRules.spec(d);
        return new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant().component(DataComponents.UNBREAKABLE, new Unbreakable(false))
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_damage"),
                                s.damage() * (dark ? 1.1 : 1.0) - 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED, new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_speed"),
                                s.speed() - 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(ResourceLocation.fromNamespaceAndPath("nusmp", "djinn_weapon_reach"),
                                s.reach(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .build());
    }

    public Djinn djinn() { return djinn; }

    public boolean dark() { return dark; }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    /** The stack's own state (a cooldown shown nowhere, a mark) must not replay the draw animation. */
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p) || hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        return DjinnWeaponActs.use(p, stack, this) ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    /** Once per tick in the owner's inventory: a bare weapon is adopted as an admin one, a temporary one that has lost its Equip dissolves. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof ServerPlayer p)) return;
        DjinnWeaponService.vet(p, stack, this);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        DjinnWeaponRules.Spec s = DjinnWeaponRules.spec(djinn);
        int color = dark ? 0xC08CFF : djinn.glow();
        DjinnTemp.Data t = DjinnTemp.get(stack);
        boolean admin = t == null || t.admin();
        tip.add(Component.translatable(admin ? "tooltip.nusmp.djinn_weapon.admin" : "tooltip.nusmp.djinn_weapon.temp", djinn.displayName()).withStyle(ChatFormatting.GRAY));
        if (dark) tip.add(Component.translatable("tooltip.nusmp.djinn_weapon.dark").withStyle(ChatFormatting.DARK_PURPLE));
        tip.add(actLine(s, DjinnWeaponRules.ACT_ONE, "tooltip.nusmp.djinn_weapon.act1", color));
        tip.add(Component.translatable("djinn_weapon.act." + s.one().key() + ".desc").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(actLine(s, DjinnWeaponRules.ACT_TWO, "tooltip.nusmp.djinn_weapon.act2", color));
        tip.add(Component.translatable("djinn_weapon.act." + s.two().key() + ".desc").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.translatable("tooltip.nusmp.djinn_weapon.onhit", djinn.trait().blurb()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.nusmp.djinn_weapon.rules").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    private Component actLine(DjinnWeaponRules.Spec s, int act, String key, int color) {
        DjinnWeaponRules.Act a = s.act(act);
        String cost = String.format(java.util.Locale.ROOT, "%.1f", DjinnWeaponRules.cost(s, act, dark) * 100.0);
        String secs = String.format(java.util.Locale.ROOT, "%.0f", a.cooldown() / 20.0);
        return Component.translatable(key, Component.translatable("djinn_weapon.act." + a.key()).withColor(color), cost, secs);
    }
}
