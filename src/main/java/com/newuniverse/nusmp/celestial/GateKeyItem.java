package com.newuniverse.nusmp.celestial;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Why: the key of the Celestial Gate. It is given automatically when the twelfth Gold Zodiac key is found ({@link CelestialLedger#scan}); using it while all twelve gold keys
 * are carried and bound opens the Gate ({@link GateRitual}) and offers the choice between challenging the Celestial Spirit King and asking for his recognition. It is not
 * consumed: it is the proof that the collection was complete.
 */
public final class GateKeyItem extends Item {
    private final CelestialKey key;

    public GateKeyItem(CelestialKey key) {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
        this.key = key;
    }

    public CelestialKey key() { return key; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.pass(stack);
        return GateRitual.open(p) ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("tooltip.nusmp.celestial.gate_key", "The key of the Celestial Gate.").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatableWithFallback("tooltip.nusmp.celestial.gate_key2", "Carry all twelve Gold Zodiac keys, each with its contract, and use it to open the Gate.").withStyle(ChatFormatting.GRAY));
    }
}
