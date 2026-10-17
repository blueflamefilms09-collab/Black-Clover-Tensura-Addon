package com.newuniverse.nusmp.charter;

import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Charter Grimoire items {@code charter_grimoire_<bossId>} (the seven bosses, and from 1.60.0 each arc boss with a grimoire), registered with the mod's other items. {@link #init()} must be called
 * before {@code NUItems.ITEMS.register(modBus)}.
 */
public final class CharterItems {
    private CharterItems() {}

    private static final Map<String, DeferredItem<CharterGrimoireItem>> BY_BOSS = new LinkedHashMap<>();

    static {
        for (CharterTable.Boss b : CharterTable.all()) BY_BOSS.put(b.id(), make(b));
    }

    private static DeferredItem<CharterGrimoireItem> make(CharterTable.Boss b) {
        return NUItems.ITEMS.register(b.itemId(), () -> new CharterGrimoireItem(b, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    }

    public static void init() {}

    /**
     * 1.60.0: registers the grimoire item of an arc boss ({@link CharterTable#addArc} made its row). Like {@link #init} it must run before
     * {@code NUItems.ITEMS} sees the registry event (the roster exists once {@code ArcCharacters.bootstrap()} has run in the mod constructor). Harmless
     * to call twice for the same boss: an item is registered once.
     */
    public static void registerArc(CharterTable.Boss b) {
        if (b == null) return;
        synchronized (BY_BOSS) {
            if (!BY_BOSS.containsKey(b.id())) BY_BOSS.put(b.id(), make(b));
        }
    }

    /** The item of a boss id, or null. */
    public static Item item(String bossId) {
        DeferredItem<CharterGrimoireItem> d;
        synchronized (BY_BOSS) { d = BY_BOSS.get(bossId); }
        return d == null ? null : d.get();
    }

    /** Every grimoire item (the seven, then the arc bosses'), for the creative tab. */
    public static List<Item> all() {
        List<Item> out = new ArrayList<>();
        synchronized (BY_BOSS) { for (DeferredItem<CharterGrimoireItem> d : BY_BOSS.values()) out.add(d.get()); }
        return out;
    }
}
