package com.newuniverse.nusmp.heroweapon;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.heroweapon.bond.HeroLoan;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.heroweapon.skill.HeroCombat;
import com.newuniverse.nusmp.heroweapon.skill.HeroUse;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
 * Why: the item a hero weapon is while it is summoned. One class for all 21 weapons: what a weapon DOES is in its logic and its forms, what the item must do
 * is common. It hands the input to the dispatcher (the use key is the first technique, sneak and use the second; a sneak and use with something in the off
 * hand feeds the gem first), it is unbreakable and fire-proof, it carries the melee numbers of {@link HeroBaseStats}, and it reports the use animation of its
 * STYLE: a Shield is raised like a vanilla shield (the item answers {@code SHIELD_BLOCK}, so blocking, the arm pose and the 5-tick raise are vanilla's, and the
 * blocked damage goes through the weapon's {@code onBlocked}), a Bow is drawn like a vanilla bow and its release is handed to the weapon's logic with the charge.
 * The item also polices itself: a projection that is not its wielder's, or whose bond ended, dissolves on the next tick ({@code HeroLoan.vet}); it cannot be
 * put in a bundle; and it never causes the re-equip animation when only its look or modifiers change. Melee blows are reported to {@link HeroCombat}.
 */
public class HeroWeaponItem extends Item {
    private final HeroKind kind;

    public HeroWeaponItem(HeroKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public HeroKind kind() { return kind; }

    /** The item properties of a hero weapon: one in a stack, epic, fire-proof, unbreakable, with the melee numbers (damage, speed, reach) of its row in {@link HeroBaseStats}. */
    public static Properties props(HeroKind kind) {
        HeroBaseStats.Row row = HeroBaseStats.of(kind);
        ItemAttributeModifiers.Builder mods = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_damage"), row.damageModifier(),
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_speed"), row.speedModifier(),
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        if (row.reach() > 0.0)
            mods.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(ResourceLocation.fromNamespaceAndPath("nusmp", "hero_weapon_reach"), row.reach(),
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        return new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant().component(DataComponents.UNBREAKABLE, new Unbreakable(false)).attributes(mods.build());
    }

    private boolean raised() { return kind.style() == HeroStyle.BLOCK || kind.style() == HeroStyle.DRAW; }

    // ================================================================ the use animation of the style
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return switch (kind.style()) {
            case BLOCK -> UseAnim.BLOCK;
            case DRAW -> UseAnim.BOW;
            default -> UseAnim.NONE;
        };
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) { return raised() ? 72000 : 0; }

    /** A Shield is a real shield: {@code LivingEntity.isBlocking} reads this, the block animation and the 5-tick raise are vanilla's. */
    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        if (kind.style() == HeroStyle.BLOCK && ability == ItemAbilities.SHIELD_BLOCK) return true;
        return super.canPerformAction(stack, ability);
    }

    // ================================================================ using it
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        boolean sneak = player.isShiftKeyDown();
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (sneak && !sp.getOffhandItem().isEmpty()) {
                boolean[] absorbed = {false};
                SafeCast.tick("hero.absorb", () -> absorbed[0] = HeroForms.absorbOffhand(sp));       // a fault in the forms system must not kick the player
                if (absorbed[0]) return InteractionResultHolder.success(stack);
            }
            // a raised style has no plain-use technique: the use key raises the guard or draws the string
            HeroTrigger trigger = sneak ? HeroTrigger.SNEAK_USE : raised() ? null : HeroTrigger.USE;
            if (trigger != null && HeroUse.fire(sp, trigger)) return InteractionResultHolder.success(stack);
        }
        if (raised()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    /** The string was let go: a drawn style hands the release (and how long it was drawn) to the weapon's logic. */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (kind.style() != HeroStyle.DRAW || level.isClientSide || !(entity instanceof ServerPlayer sp)) return;
        HeroUse.release(sp, getUseDuration(stack, entity) - timeLeft);
    }

    // ================================================================ blows
    /** The swing began: remember how charged the attack was (the strength is reset before the hit is reported). */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        HeroCombat.onSwing(player);
        return false;
    }

    /** A melee blow landed. */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof ServerPlayer sp) HeroCombat.onMelee(sp, target);
        return super.hurtEnemy(stack, target, attacker);
    }

    // ================================================================ keeping it bound
    /** Once a tick in a player's inventory: a projection that is not this player's, or whose bond ended, dissolves; a bare stack is adopted or dissolves. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof ServerPlayer p)) return;
        SafeCast.tick("hero.vet", () -> HeroLoan.vet(p, stack, kind));
    }

    /** The stack's own state (its look, its modifiers) must not replay the draw animation. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    /** A hero weapon never goes into a bundle (the loan could not see it there). */
    @Override
    public boolean canFitInsideContainerItems() { return false; }

    /** The Drop key does nothing to a hero weapon (NeoForge's hook for "the player drops the item"); the toss event cancels any other route. */
    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) { return false; }

    // ================================================================ text
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        HeroTooltip.build(kind, stack, context, lines);
    }
}
