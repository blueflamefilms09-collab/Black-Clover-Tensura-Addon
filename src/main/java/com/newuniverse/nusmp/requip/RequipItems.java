package com.newuniverse.nusmp.requip;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Why: the items of Requip: The Knight in one register: the skill book "Titania's Requip" (using it teaches the skill) and the six weapons the armours put in the hand. The register is the mod's
 * own and is attached to the mod bus by {@link RequipSystem#init}.
 */
public final class RequipItems {
    private RequipItems() {}

    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(NUSMP.MODID);

    public static final DeferredItem<TitaniasRequipItem> TITANIAS_REQUIP = REGISTER.register("titanias_requip", TitaniasRequipItem::new);

    /** weapon id -> {damage bonus, attack speed modifier, lore line}. The ids are the {@link Armour#weapon()} values. */
    private static final Map<String, Object[]> WEAPON_STATS = new LinkedHashMap<>();
    private static final Map<String, DeferredItem<RequipWeaponItem>> WEAPONS = new LinkedHashMap<>();

    static {
        WEAPON_STATS.put("flame_empress_blade", new Object[]{7.0, -2.4f, "Flame Empress Armor: a broad blade of living flame."});
        WEAPON_STATS.put("lightning_empress_halberd", new Object[]{6.0, -2.6f, "Lightning Empress Armor: a halberd that crackles with thunder."});
        WEAPON_STATS.put("sea_empress_trident", new Object[]{6.0, -2.5f, "Sea Empress Armor: a trident of the deep."});
        WEAPON_STATS.put("black_wing_blade", new Object[]{6.0, -2.0f, "Black Wing Armor: a swift blade edged in shadow."});
        WEAPON_STATS.put("nakagami_edge", new Object[]{8.0, -2.4f, "Nakagami Armor: an edge that parts magic itself."});
        WEAPON_STATS.put("purgatory_mace", new Object[]{12.0, -3.2f, "Purgatory Armor: a mace that hurts the one who swings it."});
        for (Map.Entry<String, Object[]> e : WEAPON_STATS.entrySet()) {
            Object[] v = e.getValue();
            WEAPONS.put(e.getKey(), REGISTER.register(e.getKey(), () -> new RequipWeaponItem(com.newuniverse.nusmp.relics.WeaponGrades.damageOr(e.getKey(), (Double) v[0] + 5.0) - 5.0, (Float) v[1], Rarity.EPIC, (String) v[2])));
        }
    }

    /** The weapon item with this id (an {@link Armour#weapon()} value), or null. */
    public static Item weapon(String id) {
        DeferredItem<RequipWeaponItem> d = id == null ? null : WEAPONS.get(id);
        return d == null ? null : d.get();
    }

    /** All weapon ids, in registration order. */
    public static List<String> weaponIds() { return new ArrayList<>(WEAPONS.keySet()); }

    /** The weapons as suppliers, for {@code client.WeaponRenderer}'s list of 3D weapons. */
    public static List<Supplier<? extends Item>> weapons() { return new ArrayList<>(WEAPONS.values()); }

    /** Everything that belongs in the creative tab: the book first, then the weapons. */
    public static List<Item> creative() {
        List<Item> l = new ArrayList<>();
        l.add(TITANIAS_REQUIP.get());
        for (DeferredItem<RequipWeaponItem> d : WEAPONS.values()) l.add(d.get());
        return l;
    }
}
