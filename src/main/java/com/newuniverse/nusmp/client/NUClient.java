package com.newuniverse.nusmp.client;

import com.newuniverse.nusmp.blackclover.BlackCloverRegistry;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.MagicType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only setup. The grimoire's look depends on its magic type and leaf count. */
public final class NUClient {
    private NUClient() {}

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(BlackCloverRegistry.GRIMOIRE.get(),
                ResourceLocation.fromNamespaceAndPath("nusmp", "variant"),
                (stack, level, entity, seed) -> {
                    CompoundTag tag = GrimoireItem.data(stack);
                    if (!tag.contains("Magic")) return 0.0F;
                    int code = MagicType.byName(tag.getString("Magic")).ordinal() * 100 + GrimoireItem.cover(stack).ordinal();
                    return (code + 0.5F) / 10000.0F;
                }));
    }
}
