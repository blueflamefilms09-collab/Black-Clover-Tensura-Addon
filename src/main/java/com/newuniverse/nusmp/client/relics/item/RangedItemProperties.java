package com.newuniverse.nusmp.client.relics.item;

import com.newuniverse.nusmp.relics.RelicCatalog;
import com.newuniverse.nusmp.relics.RelicDef;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.function.Supplier;

/**
 * The vanilla item properties of every relic bow and crossbow, with vanilla's own formulas: {@code pull} (the draw, 0 to 1), {@code pulling} (is the
 * holder drawing this stack), and for crossbows {@code charged} and {@code firework}. They drive any {@code overrides} an item model json has, which
 * is how the inventory icon models can swap sprites (WeaponRenderer resolves the icon model's overrides). The 3D hand mesh does not use them: it is a
 * builtin/entity model, so {@link ItemStateVariants} computes the same stage from the same numbers ({@link RangedStage}) and picks the mesh itself.
 * Registered from {@link ItemFxClient#init}.
 */
public final class RangedItemProperties {
    private RangedItemProperties() {}

    public static void register(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            for (RelicDef d : RelicCatalog.defs()) {
                if (!d.isRanged() || d.isAdopted()) continue;
                Supplier<? extends Item> sup = RelicCatalog.itemOf(d.id());
                if (sup == null) continue;
                Item item = sup.get();
                boolean crossbow = d.rangedKind() == RelicDef.RangedKind.CROSSBOW;
                ItemProperties.register(item, ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) ->
                        crossbow ? crossbowPull(stack, entity) : bowPull(stack, entity));
                ItemProperties.register(item, ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) ->
                        using(stack, entity) && !(crossbow && CrossbowItem.isCharged(stack)) ? 1.0F : 0.0F);
                if (crossbow) {
                    ItemProperties.register(item, ResourceLocation.withDefaultNamespace("charged"), (stack, level, entity, seed) ->
                            CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
                    ItemProperties.register(item, ResourceLocation.withDefaultNamespace("firework"), (stack, level, entity, seed) ->
                            hasFirework(stack) ? 1.0F : 0.0F);
                }
            }
        });
    }

    private static boolean using(ItemStack stack, LivingEntity entity) {
        return entity != null && entity.isUsingItem() && entity.getUseItem() == stack;
    }

    static float bowPull(ItemStack stack, LivingEntity entity) {
        return using(stack, entity) ? RangedStage.bowPull(stack.getUseDuration(entity), entity.getUseItemRemainingTicks()) : 0.0F;
    }

    static float crossbowPull(ItemStack stack, LivingEntity entity) {
        if (!using(stack, entity)) return 0.0F;
        return RangedStage.crossbowPull(CrossbowItem.isCharged(stack), stack.getUseDuration(entity), entity.getUseItemRemainingTicks(),
                CrossbowItem.getChargeDuration(stack, entity));
    }

    /** A loaded crossbow whose charged projectile is a firework rocket (vanilla's {@code firework} property). */
    static boolean hasFirework(ItemStack stack) {
        var charged = stack.get(DataComponents.CHARGED_PROJECTILES);
        if (charged == null) return false;
        for (ItemStack projectile : charged.getItems()) if (projectile.is(Items.FIREWORK_ROCKET)) return true;
        return false;
    }
}
