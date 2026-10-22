package com.newuniverse.nusmp.shishou;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/** Why: the one item of Shishou ({@code nusmp:shishou}) in its own register (attached to the mod bus by {@link ShishouBoot}). */
public final class ShishouItems {
    private ShishouItems() {}

    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(NUSMP.MODID);

    public static final DeferredItem<ShishouItem> SHISHOU = REGISTER.register("shishou", () -> new ShishouItem(ShishouItem.props()));

    /** For the weapon renderer's list of 3D weapons. */
    public static List<Supplier<? extends Item>> renderables() { return List.of(SHISHOU); }

    /**
     * The creative tab's entry: a bare stack with no token. It becomes the sword only if no Shishou exists (the first tick in an inventory adopts it for that player); otherwise it dissolves with a message,
     * so the entry can never break the one-per-world cap.
     */
    public static ItemStack creativeStack() { return new ItemStack(SHISHOU.get()); }
}
