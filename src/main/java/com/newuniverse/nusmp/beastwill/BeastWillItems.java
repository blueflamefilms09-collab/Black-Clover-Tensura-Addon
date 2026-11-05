package com.newuniverse.nusmp.beastwill;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

/**
 * Why: the one item of the Beast Will system (3.45.0): {@code nusmp:beast_will_core}, the rare core that holds a beast's will. It has its own register (attached to the mod bus by {@link BeastWillBoot}), a flat
 * icon (and, since the solid cores change, a 3D orb in hand), and no recipe: it only comes from a beast's drop or a command. The look is a flat generated icon (see {@code tools/beastwill/gen_core_icon.py}); a 3D hand model is not part of 3.45.0.
 */
public final class BeastWillItems {
    private BeastWillItems() {}

    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(NUSMP.MODID);

    public static final DeferredItem<BeastWillCoreItem> CORE = REGISTER.register("beast_will_core",
            () -> new BeastWillCoreItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));

    /** The one list the client's 3D hand renderer reads ({@code client.WeaponRenderer}, like {@code manacore.CoreItems.renderables()}): the core is drawn as a solid orb (docs/solid_cores.md). */
    public static java.util.List<java.util.function.Supplier<? extends Item>> renderables() { return java.util.List.of(CORE); }

    /** The creative-tab cores: one per shipped will (display names, the will id decides what a core gives; the server reads the data file). */
    public static java.util.List<ItemStack> creativeStacks() {
        return java.util.List.of(core("dragon_will", "Aether Dragon", BeastRank.S), core("shadow_panther_will", "Shadow Panther", BeastRank.A),
                core("elderwood_guardian_will", "Elderwood Guardian", BeastRank.A), core("lion_will", "Mana Lion", BeastRank.A));
    }

    /** A fresh core of this will with a new serial (every core is a different stack, so cores never merge and the ledger can tell a copy from a real one). */
    public static ItemStack core(String willId, String beast, BeastRank rank) {
        ItemStack stack = new ItemStack(CORE.get());
        stack.set(BeastWillComponents.CORE.get(), new BeastWillComponents.Core(willId, beast == null ? "" : beast, (rank == null ? BeastRank.A : rank).name(), UUID.randomUUID().toString()));
        return stack;
    }
}
