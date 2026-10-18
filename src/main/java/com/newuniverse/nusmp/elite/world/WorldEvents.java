package com.newuniverse.nusmp.elite.world;

import com.newuniverse.nusmp.NUCreativeTab;
import com.newuniverse.nusmp.elite.world.block.MotherRockBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Why: the small game-bus and mod-bus handlers that do not belong to one subsystem: the commands, the creative tab entries (added to the mod's MAIN tab by an event, so {@code NUCreativeTab}
 * is not edited), and the extra drop of Tensura's own magic crystals from a cluster and from a Mother Rock (resolved by id at the moment of the drop, so the world engine works without Tensura:
 * the crystal is simply not added).
 */
public final class WorldEvents {
    private WorldEvents() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener((RegisterCommandsEvent e) -> WorldCommands.register(e));
        gameBus.addListener(WorldEvents::onBlockDrops);
    }

    public static void onCreativeTab(BuildCreativeModeTabContentsEvent e) {
        if (!e.getTabKey().equals(NUCreativeTab.MAIN.getKey())) return;
        for (Item it : WorldItems.creative()) e.accept(it);
    }

    public static void onBlockDrops(BlockDropsEvent e) {
        if (!WorldConfig.enabled()) return;
        var block = e.getState().getBlock();
        String crystal;
        double chance;
        if (block == WorldBlocks.CLUSTER.get()) {
            crystal = "tensura:low_quality_magic_crystal";
            chance = 0.25;
        } else if (block instanceof MotherRockBlock && e.getTool().getEnchantmentLevel(e.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH)) <= 0) {
            crystal = "tensura:medium_quality_magic_crystal";
            chance = 0.6;
        } else {
            return;
        }
        if (e.getLevel().getRandom().nextDouble() >= chance) return;
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(crystal)).orElse(null);
        if (item == null || item == Items.AIR) return;
        e.getDrops().add(new ItemEntity(e.getLevel(), e.getPos().getX() + 0.5, e.getPos().getY() + 0.5, e.getPos().getZ() + 0.5, new ItemStack(item)));
    }
}
