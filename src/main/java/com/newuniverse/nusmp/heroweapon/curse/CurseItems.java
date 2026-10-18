package com.newuniverse.nusmp.heroweapon.curse;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Why: the Curse Series adds exactly one item, the Purification Charm ({@code nusmp:purification_charm}), the stand-in for the Bless Series that the owner's
 * description names but that is out of scope as forms. It is registered on the curse's own register (attached to the mod bus by {@link CurseSystem#init}) so the
 * other hero systems do not have to know it exists, and {@link #creativeItems} gives the creative tab its one list.
 */
public final class CurseItems {
    private CurseItems() {}

    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(NUSMP.MODID);

    /** {@code nusmp:purification_charm}: use it to clear a hero weapon's corruption; the first one also gives back hearts of the curse's tax. */
    public static final DeferredItem<PurificationCharmItem> PURIFICATION_CHARM = REGISTER.register("purification_charm",
            () -> new PurificationCharmItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    /** The items for the creative tab. */
    public static List<Item> creativeItems() { return List.of(PURIFICATION_CHARM.get()); }
}
