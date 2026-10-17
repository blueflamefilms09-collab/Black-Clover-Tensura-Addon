package com.newuniverse.nusmp.avatar.ai;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.inventory.AvatarGear;
import com.newuniverse.nusmp.mana.MagicStoneItem;
import com.newuniverse.nusmp.relics.RelicBearer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Why: {@link LootRules} judges numbers, and something has to read them off the world: how rare is this item, is it a relic or a magic stone of this mod, a stack
 * of gold, would it be an upgrade, did a player throw it a moment ago. This is that reader. It holds no state and changes nothing.
 */
public final class LootClassifier {
    private LootClassifier() {}

    /** The item's registry path ("diamond"), or "" when it has none. */
    static String pathOf(ItemStack s) {
        var key = BuiltInRegistries.ITEM.getKey(s.getItem());
        return key == null ? "" : key.getPath();
    }

    /** True for the relics and the magic stones of this mod (the drops the Merchant's greed is aimed at). */
    static boolean treasure(ItemStack s) { return s.getItem() instanceof RelicBearer || s.getItem() instanceof MagicStoneItem; }

    /** Is a stack in a pocket worth handing to the creator: valuable, a stack of valuables, a relic or stone, or rare? */
    public static boolean worthGiving(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        String path = pathOf(s);
        return LootRules.isValuable(path) || LootRules.isValuableStack(path, s.getCount()) || treasure(s) || s.getRarity().ordinal() >= LootRules.RARE;
    }

    /** The facts about one dropped item as the Avatar sees it. */
    public static LootRules.Drop drop(AvatarEntity a, ItemEntity e) {
        ItemStack s = e.getItem();
        String path = pathOf(s);
        return new LootRules.Drop(s.getRarity().ordinal(), AvatarGear.wouldUpgrade(a, s), LootRules.isValuable(path), LootRules.isValuableStack(path, s.getCount()), treasure(s),
                e.getAge(), e.getOwner() instanceof Player, a.distanceTo(e), Graves.near(e.level(), e.position()));
    }
}
