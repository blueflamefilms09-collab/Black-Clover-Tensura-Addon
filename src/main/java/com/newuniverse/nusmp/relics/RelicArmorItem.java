package com.newuniverse.nusmp.relics;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;

/**
 * A HEAD or CHEST relic: real armour (vanilla armour points from the def, never breaks). Right-click puts it on, except that an unattuned one
 * that needs attunement starts the ritual while you sneak. Its look on the body is painted at run time on the client from the def's palette and look.
 */
public class RelicArmorItem extends ArmorItem implements RelicBearer {
    private final RelicDef def;

    public RelicArmorItem(RelicDef def, Holder<ArmorMaterial> material) {
        super(material, def.slot() == RelicSlot.HEAD ? ArmorItem.Type.HELMET : ArmorItem.Type.CHESTPLATE,
                new Item.Properties().stacksTo(1).rarity(RelicItemSupport.rarity(def.grade())).fireResistant()
                        .durability((def.slot() == RelicSlot.HEAD ? ArmorItem.Type.HELMET : ArmorItem.Type.CHESTPLATE).getDurability(37))
                        .component(DataComponents.UNBREAKABLE, new Unbreakable(false)));
        this.def = def;
    }

    @Override public RelicDef relic() { return def; }

    @Override public Component getName(ItemStack stack) { return RelicItemSupport.name(super.getName(stack), def); }

    @Override public boolean isFoil(ItemStack stack) { return def.grade().atLeast(Grade.LEGEND) || super.isFoil(stack); }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return RelicItemSupport.USE_TICKS; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (def.needsAttunement() && !Attune.isMine(stack, player) && player.isShiftKeyDown()) {
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer p) RelicEngine.useHeld(p, stack, def, hand);
            return InteractionResultHolder.consume(stack);
        }
        return super.use(level, player, hand);                                          // put it on
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) { RelicItemSupport.onUseTick(level, user, stack, remaining, def); }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) { RelicItemSupport.releaseUsing(stack, level, user, remaining, def); }
}
