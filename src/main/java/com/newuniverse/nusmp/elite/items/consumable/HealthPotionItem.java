package com.newuniverse.nusmp.elite.items.consumable;

import com.newuniverse.nusmp.elite.items.EliteItems;
import com.newuniverse.nusmp.elite.items.EliteItemsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;

/**
 * Why: the three Health Potions. A drink with a short animation (the size sets how long), a heal at once and a regeneration tail ({@link HealthPotionTier} holds the numbers), then a cooldown shared by
 * all three sizes (config {@code healthPotions.cooldownTicks}, {@code sharedCooldown}) so a stack cannot be chain-drunk into an instant full heal. A creative player keeps the potion; everyone else gets an
 * empty glass bottle back. With the subsystem off the item is left alone (it can be held but not drunk).
 */
public class HealthPotionItem extends Item {
    private final HealthPotionTier tier;

    public HealthPotionItem(HealthPotionTier tier) {
        super(new Item.Properties().stacksTo(tier.stack()).rarity(switch (tier) {
            case SMALL -> Rarity.COMMON;
            case MEDIUM -> Rarity.UNCOMMON;
            case LARGE -> Rarity.RARE;
        }));
        this.tier = tier;
    }

    public HealthPotionTier tier() { return tier; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) { return tier.useTicks(); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!EliteItemsConfig.enabled()) return InteractionResultHolder.pass(stack);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (level.isClientSide || !(user instanceof Player player) || !EliteItemsConfig.enabled()) return stack;
        player.heal(tier.heal());
        if (tier.regenTicks() > 0) player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, tier.regenTicks(), tier.regenAmp(), false, true, true));
        if (tier.absorb() > 0) player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), tier.absorb()));
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.9f, 1.15f - 0.1f * tier.ordinal());
        int cd = EliteItemsConfig.get(EliteItemsConfig.HEALTH_POTION_COOLDOWN);
        if (cd > 0) {
            if (EliteItemsConfig.get(EliteItemsConfig.HEALTH_POTION_SHARED_COOLDOWN)) {
                for (HealthPotionTier t : HealthPotionTier.values()) player.getCooldowns().addCooldown(EliteItems.healthPotion(t).get(), cd);
            } else {
                player.getCooldowns().addCooldown(this, cd);
            }
        }
        if (player.getAbilities().instabuild) return stack;
        stack.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (stack.isEmpty()) return bottle;
        ItemHandlerHelper.giveItemToPlayer(player, bottle);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("tooltip.nusmp.health_potion.heal", "Restores %s health at once.", fmt(tier.heal())).withStyle(ChatFormatting.RED));
        if (tier.regenTicks() > 0)
            tip.add(Component.translatableWithFallback("tooltip.nusmp.health_potion.regen", "Then regenerates %s more over %s s.", fmt(tier.regenHeal()), tier.regenTicks() / 20).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (tier.absorb() > 0)
            tip.add(Component.translatableWithFallback("tooltip.nusmp.health_potion.absorb", "%s Absorption for %s s.", fmt(tier.absorb()), tier.absorbTicks() / 20).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatableWithFallback("tooltip.nusmp.health_potion.cooldown", "Shared cooldown: %s s.", EliteItemsConfig.get(EliteItemsConfig.HEALTH_POTION_COOLDOWN) / 20.0).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String fmt(float v) { return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v); }
}
