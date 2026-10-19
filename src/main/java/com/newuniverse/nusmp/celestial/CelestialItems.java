package com.newuniverse.nusmp.celestial;

import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the items of Celestial Spirit Magic in one registry: the twelve Gold Zodiac keys, the four silver keys, the Spirit King's gate key (all from {@link CelestialKey},
 * so a key added to the table is registered without another line), the skill book that teaches the skill, and the sceptre the king leaves to the one who beats him.
 * They are registered through {@link NUItems#ITEMS}; call {@link #init()} from the mod constructor before that register is added to the bus.
 */
public final class CelestialItems {
    private CelestialItems() {}

    private static final Map<CelestialKey, DeferredItem<? extends Item>> KEYS = new EnumMap<>(CelestialKey.class);

    public static final DeferredItem<CelestialSkillBookItem> SKILL_BOOK;
    public static final DeferredItem<SpiritKingSceptreItem> SCEPTRE;

    static {
        for (CelestialKey k : CelestialKey.values()) {
            if (k.summons()) KEYS.put(k, NUItems.ITEMS.register(k.itemId(), () -> new CelestialKeyItem(k)));
            else KEYS.put(k, NUItems.ITEMS.register(k.itemId(), () -> new GateKeyItem(k)));
        }
        SKILL_BOOK = NUItems.ITEMS.register("celestial_spirit_magic_book", CelestialSkillBookItem::new);
        SCEPTRE = NUItems.ITEMS.register("spirit_king_sceptre", SpiritKingSceptreItem::new);
    }

    /** Loads this class (and so registers the items) before the item register's event. */
    public static void init() {}

    public static Item item(CelestialKey key) { return KEYS.get(key).get(); }

    public static ItemStack stack(CelestialKey key) { return new ItemStack(item(key)); }

    /** The key a stack is, or null. */
    public static CelestialKey keyOf(ItemStack s) {
        if (s.isEmpty()) return null;
        if (s.getItem() instanceof CelestialKeyItem k) return k.key();
        if (s.getItem() instanceof GateKeyItem g) return g.key();
        return null;
    }

    /** The items the model and the tab need: every key, the book and the sceptre. */
    public static List<DeferredItem<? extends Item>> all() {
        List<DeferredItem<? extends Item>> out = new ArrayList<>();
        for (CelestialKey k : CelestialKey.values()) out.add(KEYS.get(k));
        out.add(SKILL_BOOK);
        out.add(SCEPTRE);
        return List.copyOf(out);
    }

    public static List<DeferredItem<? extends Item>> creativeItems() { return all(); }

    /** The items drawn as 3D models in hand (client.WeaponRenderer): every key, the book and the sceptre. */
    public static List<java.util.function.Supplier<? extends Item>> renderables() {
        List<java.util.function.Supplier<? extends Item>> out = new ArrayList<>();
        out.addAll(all());
        return out;
    }
}
