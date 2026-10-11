package com.newuniverse.nusmp.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Unbreakable;

/**
 * 0.99: Elsdocia, the first Wizard King's legacy sword (Lemiel Silvamillion Clover's creation), raised to the
 * same God-class standard as the Genesis Demon-Slayer. It replaces nothing (new id "elsdocia") and its
 * mechanics live in {@link MagicWeaponItem} (Kind.ELSDOCIA):
 * <ul>
 *   <li><b>Absorbs, stores and releases magic</b> — hitting a foe drains 2% of their magicule pool into the
 *       stack's "ElsdociaCharge" (0-1000); Legacy Release (right-click) spends the whole reserve as a broad
 *       spatial rift that heals the wielder proportionally.</li>
 *   <li><b>Key Magic synergy</b> — with a floating Key Magic grimoire, Legacy Release reaches 28 blocks, hits
 *       for x1.35 damage, and Legacy Gate (sneak+right-click) opens a 32-block rift that drains 4% of each
 *       hit foe's magicules.</li>
 *   <li><b>Infinite durability</b> (unbreakable, per the Genesis convention).</li>
 * </ul>
 * The custom engraving is "Legacy of the Wizard Kings" (a nusmp: enchantment applied via
 * {@link WeaponEngravings}); the lore text (crystal blade/clover emblem, Lemiel's history, Asta's use against
 * Conrad, country-scale potential) lives in the lang keys shown in the tooltip.
 */
public class ElsdociaSwordItem extends MagicWeaponItem {
    public ElsdociaSwordItem() {
        super(Kind.ELSDOCIA, new Item.Properties().rarity(Rarity.EPIC).fireResistant()
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true)));
    }
}
