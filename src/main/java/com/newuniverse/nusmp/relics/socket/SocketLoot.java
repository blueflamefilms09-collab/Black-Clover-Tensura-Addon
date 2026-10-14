package com.newuniverse.nusmp.relics.socket;

import com.newuniverse.nusmp.boss.BossRegistry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Where Cores come from: structure chests (called by the relic loot modifier with the table's path) and the mod's registered bosses (a drop listener).
 * Both at the low weights of {@link SocketLootRules}. Never throws: loot must not fail because of a Core.
 */
public final class SocketLoot {
    private SocketLoot() {}

    public static void register(IEventBus gameBus) { gameBus.addListener(SocketLoot::onDrops); }

    /** Maybe adds one Core to the loot of a chest table (called from RelicLootModifier). */
    public static void addToChest(ObjectArrayList<ItemStack> loot, String path, RandomSource rng) {
        try {
            if (!SocketLootRules.chest(path)) return;
            boolean deep = SocketLootRules.deep(path);
            ItemStack core = ItemStack.EMPTY;
            if (deep && rng.nextFloat() < SocketLootRules.CHEST_DEEP_DEVIL) core = SocketItems.stack(SocketLootRules.pickDevil(rng.nextDouble()));
            else if (rng.nextFloat() < (deep ? SocketLootRules.CHEST_DEEP_SPIRIT : SocketLootRules.CHEST_SPIRIT)) core = SocketItems.stack(SocketLootRules.pickSpirit(rng.nextDouble()));
            if (!core.isEmpty()) loot.add(core);
        } catch (RuntimeException | LinkageError ignored) {
            // loot must never fail
        }
    }

    private static void onDrops(LivingDropsEvent e) {
        try {
            if (!(e.getEntity().level() instanceof ServerLevel level)) return;
            EntityType<?> type = e.getEntity().getType();
            var key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (!BossRegistry.isBoss(key.getNamespace(), key.getPath())) return;
            RandomSource rng = level.getRandom();
            ItemStack drop = ItemStack.EMPTY;
            if (rng.nextFloat() < SocketLootRules.BOSS_DEVIL) drop = SocketItems.stack(SocketLootRules.pickDevil(rng.nextDouble()));
            else if (rng.nextFloat() < SocketLootRules.BOSS_SPIRIT) drop = SocketItems.stack(SocketLootRules.pickSpirit(rng.nextDouble()));
            if (drop.isEmpty()) return;
            Vec3 at = e.getEntity().position();
            e.getDrops().add(new ItemEntity(level, at.x, at.y + 0.3, at.z, drop));
        } catch (RuntimeException | LinkageError ignored) {
            // loot must never fail
        }
    }
}
