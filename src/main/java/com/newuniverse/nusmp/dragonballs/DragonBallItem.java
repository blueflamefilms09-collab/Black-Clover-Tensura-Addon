package com.newuniverse.nusmp.dragonballs;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * One Dragon Ball: a set (Earth or Super) and a star count (1..8), one item per ball so every ball is its own recipe-free, unstackable thing. The stack carries two small marks in its custom data:
 * {@code db_gen} (the generation of the scatter that made it, so a ball of an old scatter crumbles when its set is scattered again) and {@code db_stone} (the ball is a grey stone while its set is
 * locked; the model switches with custom_model_data 1). Both are checked on the SERVER only; the client only draws the model and the tooltip. A ball in a hand or bag reports its place to the registry
 * once every five seconds ({@link #inventoryTick}), which is how the Dragon Radar finds a ball somebody carries.
 */
public class DragonBallItem extends Item {
    public static final String GEN = "db_gen", STONE = "db_stone";

    private final BallSet set;
    private final int star;

    public DragonBallItem(BallSet set, int star) {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
        this.set = set;
        this.star = star;
    }

    public BallSet set() { return set; }

    public int star() { return star; }

    // ---------------------------------------------------------------- the stack marks
    public static boolean isStone(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d != null && d.contains(STONE);
    }

    /** The generation of the scatter that made this ball, or -1 for a ball nobody scattered (a creative one: it is accepted as current). */
    public static int gen(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d != null && d.contains(GEN) ? d.copyTag().getInt(GEN) : -1;
    }

    /** A fresh ball of the set. {@code gen} < 0 leaves no generation (creative / test). */
    public static ItemStack make(BallSet set, int star, int gen, boolean stone) {
        ItemStack s = new ItemStack(DbItems.ball(set, star));
        if (gen >= 0) CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putInt(GEN, gen));
        if (stone) setStone(s, true);
        return s;
    }

    public static void setStone(ItemStack s, boolean stone) {
        if (stone) {
            CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putBoolean(STONE, true));
            s.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        } else {
            CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.remove(STONE));
            s.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) { return !isStone(stack); }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("anime_multiverse.dragonballs.tip.stars", star).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("anime_multiverse.dragonballs.tip.set." + set.id(), DbConfig.count()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable(isStone(stack) ? "anime_multiverse.dragonballs.tip.stone" : "anime_multiverse.dragonballs.tip.how", set.dragonName()).withStyle(isStone(stack) ? ChatFormatting.DARK_GRAY : ChatFormatting.YELLOW));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (level.isClientSide || !(level instanceof ServerLevel sl) || !(holder instanceof ServerPlayer p) || (p.tickCount + slot * 7) % 100 != 0) return;
        DbManager.onHeld(sl, p, stack, set, star);
    }
}
