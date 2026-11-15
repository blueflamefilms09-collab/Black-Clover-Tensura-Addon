package com.newuniverse.nusmp.greed;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Why: the eight forms of the Black Sword Greed are eight item ids in their own register (attached to the mod bus by {@link GreedBoot}); the form switch swaps the item id in the slot and keeps the components. */
public final class GreedItems {
    private GreedItems() {}

    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(NUSMP.MODID);

    private static final List<DeferredItem<GreedItem>> FORMS = new ArrayList<>();

    public static final DeferredItem<GreedItem> SWORD = form(GreedRules.Form.SWORD);
    public static final DeferredItem<GreedItem> BOW = form(GreedRules.Form.BOW);
    public static final DeferredItem<GreedItem> SCYTHE = form(GreedRules.Form.SCYTHE);
    public static final DeferredItem<GreedItem> SHIELD = form(GreedRules.Form.SHIELD);
    public static final DeferredItem<GreedItem> STAFF = form(GreedRules.Form.STAFF);
    public static final DeferredItem<GreedItem> GAUNTLETS = form(GreedRules.Form.GAUNTLETS);
    public static final DeferredItem<GreedItem> SPEAR = form(GreedRules.Form.SPEAR);
    public static final DeferredItem<GreedItem> DUAL_BLADES = form(GreedRules.Form.DUAL_BLADES);

    private static DeferredItem<GreedItem> form(GreedRules.Form f) {
        DeferredItem<GreedItem> d = REGISTER.register(f.itemId(), () -> new GreedItem(f));
        FORMS.add(d);
        return d;
    }

    public static Item of(GreedRules.Form f) { return FORMS.get(f.id()).get(); }

    /** The form of a stack, or null when it is not one of the eight. */
    public static GreedRules.Form formOf(ItemStack s) { return s.getItem() instanceof GreedItem g ? g.form() : null; }

    /** For the weapon renderer's list of 3D weapons. */
    public static List<Supplier<? extends Item>> renderables() { return new ArrayList<>(FORMS); }

    /** The creative tab's entry: a bare stack with no token. It becomes THE sword only if no Black Sword Greed exists (the first scan in an inventory adopts it for that player); otherwise it dissolves. */
    public static ItemStack creativeStack() { return new ItemStack(SWORD.get()); }
}
