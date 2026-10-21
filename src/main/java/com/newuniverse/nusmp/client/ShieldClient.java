package com.newuniverse.nusmp.client;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.visual.shields.ShieldSet;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * 1.110.0: the client end of the shield fix (docs/visual_upgrade/shield_fix.md). Vanilla's {@code blocking} item property is registered per item (for
 * {@code minecraft:shield} only), and the model json's {@code blocking} override reads it: without it a modded shield never swaps to its raised-shield model.
 * This registers vanilla's own function for every shield-class item ({@link ShieldSet}), so each of them swaps to its {@code <id>_blocking} model while the
 * holder is using it, in either hand, the way the vanilla shield does. Draws and asks only; the blocking itself is decided by the server (the item classes).
 */
final class ShieldClient {
    private ShieldClient() {}

    static void registerBlocking() {
        ResourceLocation blocking = ResourceLocation.withDefaultNamespace("blocking");
        for (String id : ShieldSet.IDS) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, id));
            if (item == null || item == net.minecraft.world.item.Items.AIR) continue;                       // an id of the list that this build does not register
            ItemProperties.register(item, blocking, (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        }
    }
}
