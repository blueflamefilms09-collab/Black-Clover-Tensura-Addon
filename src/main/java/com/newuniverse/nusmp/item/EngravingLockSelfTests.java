package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Why: the engraving lock (docs/engraving_lock.md) cannot be watched at a real anvil in CI, so the server smoke test checks its core: key {@code engraving.lock}
 * engraves a stand-in stack of one of the mod's weapons, strips every enchantment from it, runs the once-a-second pass again and expects the engravings back;
 * an unengraved stack and a foreign item are not "locked". A stack that never belongs to a player: no world, no tick work.
 */
public final class EngravingLockSelfTests {
    private EngravingLockSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("engraving.lock", EngravingLockSelfTests::lock);
    }

    private static Result lock(MinecraftServer server) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "last_word"));
        ItemStack s = new ItemStack(item);
        if (s.isEmpty()) return Result.ok("last_word is not registered here, skipped");
        if (EngravingLock.locked(s)) return Result.fail("an unengraved stack counts as locked");
        if (EngravingLock.locked(new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD))) return Result.fail("a foreign item counts as locked");
        WeaponEngravings.engrave(s, server.registryAccess());
        if (!EngravingLock.locked(s)) return Result.fail("an engraved stack is not locked");
        int before = s.getEnchantments().size();
        if (before == 0) return Result.ok("no engraving of last_word exists in this registry, skipped");
        s.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        boolean back = WeaponEngravings.engrave(s, server.registryAccess());
        int after = s.getEnchantments().size();
        if (EngravingLock.on() && (!back || after != before)) return Result.fail("stripped engravings did not come back: " + before + " -> " + after);
        return Result.ok("locked=" + EngravingLock.on() + ", engravings " + before + " -> stripped -> " + after);
    }
}
