package com.newuniverse.nusmp.compat;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforgespi.language.IModFileInfo;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Ships the data pack {@code nusmp_tni_compat} (resources/resourcepacks/nusmp_tni_compat, built by tools/compat/build_compat_pack.py) inside the
 * jar and offers it to the server only when Tensura No Index is loaded. Mod bus: {@code modEventBus.addListener(CompatPacks::onPacks)}.
 * <p>
 * The pack is offered as {@code required}, which is what makes a built-in data pack switch on by itself (the form Tensura No Index uses for
 * its own bridge pack). It has a distinct id ({@code mod/nusmp_tni_compat}) from that pack ({@code mod/tensuranoindex_multiverse_bridge}),
 * and every id inside lives in the namespace {@code nusmp_tni_compat}, so the two can run together. Any failure only logs; the game starts
 * without the pack.
 */
public final class CompatPacks {
    private CompatPacks() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final String DIR = "nusmp_tni_compat";

    public static void onPacks(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        try {
            ModList mods = ModList.get();
            if (mods == null || !mods.isLoaded(CompatCatalog.TNI_MODID)) return;
            IModFileInfo self = mods.getModFileById(NUSMP.MODID);
            if (self == null) return;
            Path root = self.getFile().findResource("resourcepacks", DIR);
            PackLocationInfo where = new PackLocationInfo("mod/" + CompatCatalog.PACK_ID, Component.literal("Multiverse of Anime x Tensura No Index"),
                    PackSource.BUILT_IN, Optional.empty());
            Pack pack = Pack.readMetaAndCreate(where, new PathPackResources.PathResourcesSupplier(root), PackType.SERVER_DATA,
                    new PackSelectionConfig(true, Pack.Position.TOP, false));
            if (pack == null) {
                LOG.warn("[nusmp] The Tensura No Index compatibility pack could not be read; it is not loaded");
                return;
            }
            event.addRepositorySource(consumer -> consumer.accept(pack));
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The Tensura No Index compatibility pack could not be added", failure);
        }
    }
}
