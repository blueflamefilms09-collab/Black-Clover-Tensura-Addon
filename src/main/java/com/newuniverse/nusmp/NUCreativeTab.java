package com.newuniverse.nusmp;

import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.MagicType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The "Multiverse of Anime in Tensura" creative tab. */
public final class NUCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NUSMP.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.nusmp.main"))
            .icon(() -> GrimoireItem.createUnbound(5, MagicType.ANTI_MAGIC, Devil.LIEBE))
            .displayItems((params, output) -> {
                // One of every cover state (Flame shown), then every magic as a three-leaf.
                for (GrimoireCover c : GrimoireCover.values()) {
                    MagicType m = c.isForbidden() ? MagicType.DARK : MagicType.FLAME;
                    output.accept(GrimoireItem.createUnbound(c, m, c.isForbidden() ? Devil.MEGICULA : null));
                }
                output.accept(GrimoireItem.createUnbound(GrimoireCover.FIVE_LEAF, MagicType.ANTI_MAGIC, Devil.LIEBE));
                for (var it : com.newuniverse.nusmp.item.NUItems.all()) output.accept(it.get());
                output.accept(com.newuniverse.nusmp.block.NUBlocks.GRIMOIRE_ALTAR_ITEM.get());
                for (MagicType m : MagicType.values()) {
                    if (m == MagicType.ANTI_MAGIC) continue;
                    output.accept(GrimoireItem.createUnbound(GrimoireCover.THREE_LEAF, m, null));
                }
                // the named canon grimoires (Fuegoleon, Yuno, Asta, Noelle, ...)
                for (var b : com.newuniverse.nusmp.grimoire.CanonBook.values()) output.accept(GrimoireItem.createCanon(b));
            })
            .build());

    private NUCreativeTab() {}
}
