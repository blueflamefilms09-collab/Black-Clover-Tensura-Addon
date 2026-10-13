package com.newuniverse.nusmp.relics;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** A relic that is used up: drunk or spent over a second and a half, running its first act, and gone only if the act ran. */
public class RelicConsumableItem extends Item implements RelicBearer {
    private final RelicDef def;

    public RelicConsumableItem(RelicDef def) {
        super(new Item.Properties().stacksTo(16).rarity(RelicItemSupport.rarity(def.grade())));
        this.def = def;
    }

    @Override public RelicDef relic() { return def; }

    @Override public Component getName(ItemStack stack) { return RelicItemSupport.name(super.getName(stack), def); }

    @Override public boolean isFoil(ItemStack stack) { return def.grade().atLeast(Grade.LEGEND) || super.isFoil(stack); }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }

    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return 32; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (def.acts().isEmpty()) return InteractionResultHolder.pass(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (level.isClientSide || !(user instanceof ServerPlayer p) || def.acts().isEmpty()) return stack;
        boolean ran = RelicEngine.activate(p, stack, def, def.firstAct(), p.isShiftKeyDown(), 0);
        if (ran && !p.isCreative()) stack.shrink(1);
        return stack;
    }
}
