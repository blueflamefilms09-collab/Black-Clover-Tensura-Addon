package com.newuniverse.nusmp.celestial;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: a Celestial Key is a physical item (a Gold Zodiac key or a silver key), a collectible of the Celestial Gate (the Celestial Spirit Magic skill that once called the spirits was removed). Holding the key and
 * keeping the use button down performs the contract ritual (three seconds, a ring of stars, a share of the magicules): the key then carries the player's name and the
 * spirit answers only that player. Using the key while sneaking breaks the contract so the key can be given away. This item does not summon.
 */
public final class CelestialKeyItem extends Item {
    private final CelestialKey key;

    public CelestialKeyItem(CelestialKey key) {
        super(new Item.Properties().stacksTo(1).rarity(key.isGold() ? Rarity.EPIC : Rarity.RARE).fireResistant());
        this.key = key;
    }

    public CelestialKey key() { return key; }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return CelestialRules.BIND_TICKS; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.pass(stack);
        if (p.isShiftKeyDown()) {                                                                  // sneak: break the contract
            if (CelestialLedger.ownerOf(stack).equals(p.getUUID().toString())) {
                CelestialLedger.release(p, stack);
                p.displayClientMessage(Component.translatableWithFallback("message.nusmp.celestial.released", "The contract with %s is broken. The key is free.", key.spirit).withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.pass(stack);
        }
        CelestialRules.Bind why = CelestialLedger.bindCheck(p, stack);
        boolean rebind = why == CelestialRules.Bind.ALREADY_BOUND && CelestialLedger.ownerOf(stack).isEmpty();
        if (why != CelestialRules.Bind.OK && !rebind) {
            p.displayClientMessage(refusal(why), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel sl) || !(entity instanceof ServerPlayer p)) return;
        int done = getUseDuration(stack, entity) - remaining;
        double a = done * 0.45, r = 1.6 - 1.2 * done / (double) CelestialRules.BIND_TICKS;
        for (int i = 0; i < 3; i++) {
            double ang = a + i * Math.PI * 2 / 3;
            Fx.spray(sl, FxThemes.SKILL_CELESTIAL, FxOld.END_ROD, p.getX() + Math.cos(ang) * r, p.getY() + 0.2 + done * 0.03, p.getZ() + Math.sin(ang) * r, 1, 0, 0, 0, 0);
        }
        if (done % 20 == 0) sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 0.8f + done / 60f);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer p)) return stack;
        CelestialRules.Bind r = CelestialLedger.seal(p, stack);
        if (r == CelestialRules.Bind.OK) {
            p.displayClientMessage(Component.translatableWithFallback("message.nusmp.celestial.bound", "A contract is sealed with %s.", key.spirit).withStyle(ChatFormatting.GOLD), true);
            ServerLevel sl = (ServerLevel) level;
            sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9f, 1.6f);
            Fx.spray(sl, FxThemes.SKILL_CELESTIAL, FxOld.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 0.8, 0.6, 0.05);
        } else {
            p.displayClientMessage(refusal(r), true);
        }
        return stack;
    }

    private Component refusal(CelestialRules.Bind r) {
        return switch (r) {
            case SOMEONE_ELSES -> Component.translatableWithFallback("message.nusmp.celestial.not_yours", "This key keeps a contract with someone else.").withStyle(ChatFormatting.RED);
            case ALREADY_BOUND -> Component.translatableWithFallback("message.nusmp.celestial.already_bound", "You already hold a contract with %s.", key.spirit).withStyle(ChatFormatting.YELLOW);
            case NOT_ENOUGH_MAGICULES -> Component.translatableWithFallback("message.nusmp.celestial.no_magicules", "Not enough magicules for the contract.").withStyle(ChatFormatting.RED);
            default -> Component.translatableWithFallback("message.nusmp.celestial.no_spirit", "This key calls no spirit.").withStyle(ChatFormatting.RED);
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(key.spirit).withStyle(ChatFormatting.AQUA));
        tip.add(Component.literal(key.ability + ": " + key.abilityText).withStyle(ChatFormatting.GRAY));
        String owner = CelestialLedger.ownerNameOf(stack);
        tip.add(owner.isEmpty() ? Component.translatableWithFallback("tooltip.nusmp.celestial.unbound", "No contract. Hold use to seal one (sneak + use breaks it).").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatableWithFallback("tooltip.nusmp.celestial.contract", "Contract: %s", owner).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatableWithFallback("tooltip.nusmp.celestial.slot", "Carry it, or wear it in the Celestial Key slot, to call the spirit with Celestial Spirit Magic.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
