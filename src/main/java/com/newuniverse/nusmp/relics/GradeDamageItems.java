package com.newuniverse.nusmp.relics;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * The Minecraft side of {@link GradeDamage}: the weapon type of a stack or a relic def. The table lives in assets/nusmp/weapon_classes.json
 * (the same classes the weapon renderer sizes meshes by), with a keyword guess for ids nobody classified.
 */
public final class GradeDamageItems {
    private GradeDamageItems() {}

    /** The weapon type of a stack ("longsword", "dagger", "greathammer", "bow" ...); "item" for an empty stack. */
    public static String weaponTypeOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "item";
        RelicDef d = Relics.defOf(stack);
        if (d != null) return weaponTypeOf(d);
        return GradeDamage.typeOfId(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
    }

    /** The weapon type of a relic def: a bow or crossbow by its ranged kind, else by id, else by its look. */
    public static String weaponTypeOf(RelicDef d) {
        if (d == null) return "item";
        if (d.rangedKind() == RelicDef.RangedKind.BOW) return "bow";
        if (d.rangedKind() == RelicDef.RangedKind.CROSSBOW) return "crossbow";
        String t = GradeDamage.typeOfId(d.id());
        if (!t.equals("item")) return t;
        return switch (d.look()) {
            case RAPIER -> "rapier";
            case STAFF -> "staff";
            case WAND -> "wand";
            case HAMMER -> "hammer";
            case AXE -> "battleaxe";
            case BOW -> "bow";
            case SHIELD -> "shield";
            default -> "longsword";
        };
    }
}
