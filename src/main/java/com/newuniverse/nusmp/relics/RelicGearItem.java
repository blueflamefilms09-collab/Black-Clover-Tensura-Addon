package com.newuniverse.nusmp.relics;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;

/**
 * A relic worn in a Curios slot (neck, back, belt, hands, charm). It works while worn (and attuned, if it needs that); its acts are fired with the
 * Use Worn Relic key. Right-click in the hand fires the first act too, and starts the attunement ritual if the relic needs it and is not yours yet.
 * Which slot it goes in is data: data/curios/tags/item/&lt;slot&gt;.json, written for every def by tools/relics/gen_curios_tags.py.
 */
public class RelicGearItem extends Item implements RelicBearer {
    private final RelicDef def;

    public RelicGearItem(RelicDef def) {
        super(new Item.Properties().stacksTo(1).rarity(RelicItemSupport.rarity(def.grade())).fireResistant()
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false)));
        this.def = def;
    }

    @Override public RelicDef relic() { return def; }

    @Override public Component getName(ItemStack stack) { return RelicItemSupport.name(super.getName(stack), def, stack); }

    @Override public boolean isFoil(ItemStack stack) { return RelicItemSupport.foil(stack, def) || super.isFoil(stack); }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return RelicItemSupport.USE_TICKS; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
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
