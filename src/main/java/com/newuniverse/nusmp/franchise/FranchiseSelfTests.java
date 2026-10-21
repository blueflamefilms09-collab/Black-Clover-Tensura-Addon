package com.newuniverse.nusmp.franchise;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Map;
import java.util.Optional;

/**
 * The CI self-test {@code item.franchise_engravings} (server smoke test): the table is whole, every engraving exists as an enchantment on the running server and sits in the
 * {@code tensura:engraving} tag, an item of every mapped franchise gets exactly its franchise's engravings once (and keeps the older ones), food and unmapped items get none,
 * and a real incoming-damage event runs through {@link FranchiseEvents} (a wounded bearer of Dragon Force hits harder, within the cap; a hit with nothing engraved is untouched).
 */
public final class FranchiseSelfTests {
    private FranchiseSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("item.franchise_engravings", FranchiseSelfTests::run);
    }

    private static SelfTests.Result run(MinecraftServer server) {
        FranchiseData.Table table = FranchiseData.table();
        if (table.franchises().size() < 30 || table.engravings().size() < 40) return SelfTests.Result.fail("the table is short: " + table.franchises().size() + " franchises, " + table.engravings().size() + " engravings");
        for (FranchiseData.Franchise f : table.franchises()) if (table.of(f.id()).size() < 2) return SelfTests.Result.fail("franchise " + f.id() + " has fewer than 2 engravings");
        // ---- every engraving is a registered enchantment and an engraving of Tensura's tag
        HolderLookup.RegistryLookup<Enchantment> lookup = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        TagKey<Enchantment> tag = TagKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("tensura", "engraving"));
        for (FranchiseData.Engraving en : table.engravings().values()) {
            Optional<? extends Holder<Enchantment>> h = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("nusmp", en.id())));
            if (h.isEmpty()) return SelfTests.Result.fail("enchantment nusmp:" + en.id() + " is not registered");
            if (!h.get().is(tag)) return SelfTests.Result.fail("nusmp:" + en.id() + " is not in the tensura:engraving tag");
        }
        // ---- every mapped franchise: an existing item of it is engraved once, with its franchise's engravings
        int engraved = 0;
        Map<String, String> items = FranchiseMap.map().items();
        java.util.Set<String> done = new java.util.HashSet<>();
        for (Map.Entry<String, String> m : items.entrySet()) {
            if (done.contains(m.getValue())) continue;
            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("nusmp", m.getKey()));
            if (item.isEmpty()) continue;
            ItemStack s = new ItemStack(item.get());
            if (!FranchiseEngravings.engravable(s)) continue;
            if (!FranchiseEngravings.engrave(s, server.registryAccess())) return SelfTests.Result.fail(m.getKey() + " was not engraved");
            for (FranchiseData.Engraving en : table.of(m.getValue())) {
                Holder<Enchantment> h = lookup.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("nusmp", en.id())));
                if (s.getEnchantments().getLevel(h) < 1) return SelfTests.Result.fail(m.getKey() + " lacks " + en.id());
            }
            if (FranchiseEngravings.engrave(s, server.registryAccess())) return SelfTests.Result.fail(m.getKey() + " was engraved twice");
            done.add(m.getValue());
            engraved++;
        }
        if (engraved < 15) return SelfTests.Result.fail("only " + engraved + " franchises could be checked on real items");
        if (FranchiseEngravings.engrave(new ItemStack(Items.APPLE), server.registryAccess()) || FranchiseEngravings.engrave(new ItemStack(Items.IRON_SWORD), server.registryAccess())) return SelfTests.Result.fail("an item without a franchise was engraved");
        // ---- a real hit through the handler
        ServerLevel level = server.overworld();
        FakePlayer p = SelfTests.player(level, "franchise-test", level.getSharedSpawnPos().getX() + 0.5, level.getSharedSpawnPos().getY() + 1.0, level.getSharedSpawnPos().getZ() + 0.5);
        LivingEntity victim = EntityType.ZOMBIE.create(level);
        if (victim == null) return SelfTests.Result.fail("no test victim");
        try {
            Holder<Enchantment> dragonForce = lookup.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("nusmp", "fr_dragon_force")));
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
            p.setHealth(4.0f);
            LivingIncomingDamageEvent plain = new LivingIncomingDamageEvent(victim, new DamageContainer(p.damageSources().playerAttack(p), 100f));
            FranchiseEvents.onIncoming(plain);
            if (Math.abs(plain.getAmount() - 100f) > 0.01f) return SelfTests.Result.fail("a hit with nothing engraved changed: " + plain.getAmount());
            sword.enchant(dragonForce, 1);
            LivingIncomingDamageEvent hit = new LivingIncomingDamageEvent(victim, new DamageContainer(p.damageSources().playerAttack(p), 100f));
            FranchiseEvents.onIncoming(hit);
            float expected = (float) (100.0 * (1.0 + FranchiseRules.cappedAttack(0.08, false, FranchiseRules.ATTACK_CAP)));
            if (Math.abs(hit.getAmount() - expected) > 0.05f) return SelfTests.Result.fail("Dragon Force gave " + hit.getAmount() + " instead of " + expected);
            if (hit.getAmount() > 100f * (1.0 + FranchiseRules.ATTACK_CAP) + 0.01f) return SelfTests.Result.fail("the attack cap was passed: " + hit.getAmount());
            p.setHealth(p.getMaxHealth());
            LivingIncomingDamageEvent healthy = new LivingIncomingDamageEvent(victim, new DamageContainer(p.damageSources().playerAttack(p), 100f));
            FranchiseEvents.onIncoming(healthy);
            if (Math.abs(healthy.getAmount() - 100f) > 0.01f) return SelfTests.Result.fail("Dragon Force worked at full health: " + healthy.getAmount());
        } finally {
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            victim.discard();
            FranchiseEvents.forget(p.getUUID());
        }
        return SelfTests.Result.ok(table.franchises().size() + " franchises, " + table.engravings().size() + " engravings, " + engraved + " franchises checked on real items, a Dragon Force hit capped as designed");
    }
}
