package com.newuniverse.nusmp.relics;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * A relic held in a hand: swords, spears, staffs, hammers, shields. Its attack damage and speed come from the def, it never breaks, and a
 * right-click uses the first act (the second while sneaking). A relic that needs attunement starts the five-second ritual instead until it is
 * yours. With the SHIELD look it blocks like a vanilla shield while the button is held.
 */
public class RelicWeaponItem extends SwordItem implements RelicBearer {
    private final RelicDef def;

    public RelicWeaponItem(RelicDef def) {
        super(Tiers.NETHERITE, new Item.Properties().stacksTo(1).rarity(RelicItemSupport.rarity(def.grade())).fireResistant()
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false)).attributes(attributes(def)));
        this.def = def;
    }

    /** Vanilla's own ids for the base stats, so the tooltip shows them the way it shows a sword's. */
    private static ItemAttributeModifiers attributes(RelicDef def) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_damage"),
                        def.baseDamage() - 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_speed"),
                        def.meleeSpeedModifier(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override public RelicDef relic() { return def; }

    /** Stack-borne state (a counter, a charge, a drum) changes the stack every so often: that must not replay the draw animation. */
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) { return slotChanged || oldStack.getItem() != newStack.getItem(); }

    @Override public Component getName(ItemStack stack) { return RelicItemSupport.name(super.getName(stack), def, stack); }

    @Override public boolean isFoil(ItemStack stack) { return RelicItemSupport.foil(stack, def) || super.isFoil(stack); }

    private boolean shield() { return def.look() == Look.SHIELD; }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return shield() ? UseAnim.BLOCK : UseAnim.NONE; }

    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return RelicItemSupport.USE_TICKS; }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        if (shield() && ability == ItemAbilities.SHIELD_BLOCK) return true;
        return super.canPerformAction(stack, ability);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (shield()) {                                                            // 1.110.0: a shield is a shield first: the plain click raises it (both sides, either hand), the relic's own click is the sneaking one
            ShieldUseRules.Use use = ShieldUseRules.decide(player.isShiftKeyDown(), !def.acts().isEmpty(), def.needsAttunement(), Attune.isMine(stack, player));
            if (use != ShieldUseRules.Use.RELIC_CLICK) {
                if (use == ShieldUseRules.Use.BLOCK_HINT && !level.isClientSide && player instanceof ServerPlayer p)
                    RelicEngine.tell(p, def.cardName() + " needs attunement: sneak and hold use for 5 seconds.", net.minecraft.ChatFormatting.LIGHT_PURPLE);
                player.startUsingItem(hand);
                return InteractionResultHolder.consume(stack);
            }
        }
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.pass(stack);
        return switch (RelicEngine.useHeld(p, stack, def, hand)) {
            case DONE, HOLD -> InteractionResultHolder.consume(stack);
            case REFUSED -> InteractionResultHolder.fail(stack);
            default -> InteractionResultHolder.pass(stack);
        };
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) { RelicItemSupport.onUseTick(level, user, stack, remaining, def); }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) { RelicItemSupport.releaseUsing(stack, level, user, remaining, def); }
}
