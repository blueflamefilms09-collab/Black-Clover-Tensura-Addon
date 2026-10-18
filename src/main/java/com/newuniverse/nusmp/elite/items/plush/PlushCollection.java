package com.newuniverse.nusmp.elite.items.plush;

import com.newuniverse.nusmp.elite.items.EliteItemsConfig;
import com.newuniverse.nusmp.elite.items.EliteItemsData;
import com.newuniverse.nusmp.elite.items.api.PlushCollectedEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: the collection counter. A plushie counts for a player the first time they HOLD it (it is in the inventory), PLACE it or SQUEEZE one: all three go through {@link #collect}, and a scan of the
 * inventory every two seconds catches every other way an item arrives (a chest, a trade, a crate, creative mode, a command). Only a NEW kind raises the count; it then tells the player ("Plushie
 * collected: Warden (12/54)") and posts {@link PlushCollectedEvent} for the records builder. The set lives in the player's attachment ({@link EliteItemsData}), so it survives death. Off with the config
 * {@code plushies.collectionCounter} or the master switch.
 */
public final class PlushCollection {
    private PlushCollection() {}

    /** Game bus: scans the inventory of each player every 40 ticks. */
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 40 != 13) return;
        if (!enabled()) return;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            PlushKinds.Kind k = kindOf(inv.getItem(i));
            if (k != null) collect(p, k.id());
        }
    }

    /** The plushie kind of a stack, or null when it is not a plushie. */
    public static PlushKinds.Kind kindOf(ItemStack s) {
        if (s == null || s.isEmpty() || !(s.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof PlushBlock pb)) return null;
        return pb.kind();
    }

    static boolean enabled() { return EliteItemsConfig.enabled() && EliteItemsConfig.get(EliteItemsConfig.PLUSH_COUNTER); }

    /** Marks a kind as collected by the player; true when it was new. */
    public static boolean collect(ServerPlayer p, String kindId) {
        if (!enabled()) return false;
        PlushKinds.Kind kind = PlushKinds.byId(kindId);
        if (kind == null) return false;
        EliteItemsData d = EliteItemsData.of(p);
        if (!d.plushies().add(kindId)) return false;
        int have = d.plushies().size(), total = PlushKinds.ALL.size();
        p.displayClientMessage(Component.translatableWithFallback("message.nusmp.plush.collected", "Plushie collected: %s (%s/%s)", kind.name(), have, total).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        NeoForge.EVENT_BUS.post(new PlushCollectedEvent(p, kindId, have, total));
        return true;
    }

    /** How many different plushies the player has collected. */
    public static int count(ServerPlayer p) { return EliteItemsData.of(p).plushies().size(); }

    /** Has the player collected this kind? */
    public static boolean has(ServerPlayer p, String kindId) { return EliteItemsData.of(p).plushies().contains(kindId); }
}
