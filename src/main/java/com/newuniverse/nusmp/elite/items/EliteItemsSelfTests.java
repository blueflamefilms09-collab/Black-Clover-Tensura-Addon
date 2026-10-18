package com.newuniverse.nusmp.elite.items;

import com.newuniverse.nusmp.elite.items.armor.NightingaleBonus;
import com.newuniverse.nusmp.elite.items.consumable.HealthPotionItem;
import com.newuniverse.nusmp.elite.items.consumable.HealthPotionTier;
import com.newuniverse.nusmp.elite.items.consumable.PotentialCatalystItem;
import com.newuniverse.nusmp.elite.items.crate.CrateRoller;
import com.newuniverse.nusmp.elite.items.crate.CrateTable;
import com.newuniverse.nusmp.elite.items.crate.CrateTables;
import com.newuniverse.nusmp.elite.items.crate.CrateTier;
import com.newuniverse.nusmp.elite.items.crate.RewardEntry;
import com.newuniverse.nusmp.elite.items.crate.RewardSerializers;
import com.newuniverse.nusmp.elite.items.plush.PlushCollection;
import com.newuniverse.nusmp.elite.items.plush.PlushKinds;
import com.newuniverse.nusmp.elite.items.weapon.AbilitySpec;
import com.newuniverse.nusmp.elite.items.weapon.AbilityTables;
import com.newuniverse.nusmp.elite.items.weapon.EliteWeaponItem;
import com.newuniverse.nusmp.elite.items.weapon.RunicSwordItem;
import com.newuniverse.nusmp.elite.items.weapon.WeaponAbilities;
import com.newuniverse.nusmp.item.WeaponEngravings;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Why: the owner and the CI server smoke test run these in the REAL game ({@code /multiverse mega selftest elite.items.*}) because the build author cannot play it. Each test drives the subsystem
 * through its own public entry points with a {@code FakePlayer}: that every id of {@link EliteIds} is a registered item (and every crate and plushie a registered block), that the three crate tables
 * loaded and a whole opening gives something, that the plushie counter counts a kind once, that all six weapons' technique files loaded with valid effects and VFX names, that each weapon takes its
 * engravings, that a rune can be slotted into a Runic Sword, that the catalyst's health bonus is rebuilt exactly, that a Health Potion heals, and that four Nightingale pieces make the set. A test
 * that changes the fake player puts it back.
 */
public final class EliteItemsSelfTests {
    private EliteItemsSelfTests() {}

    public static void register() {
        SelfTests.register("elite.items.registry", EliteItemsSelfTests::registry);
        SelfTests.register("elite.items.crates", EliteItemsSelfTests::crates);
        SelfTests.register("elite.items.plush", EliteItemsSelfTests::plush);
        SelfTests.register("elite.items.abilities", EliteItemsSelfTests::abilities);
        SelfTests.register("elite.items.engravings", EliteItemsSelfTests::engravings);
        SelfTests.register("elite.items.runic", EliteItemsSelfTests::runic);
        SelfTests.register("elite.items.catalyst", EliteItemsSelfTests::catalyst);
        SelfTests.register("elite.items.potion", EliteItemsSelfTests::potion);
        SelfTests.register("elite.items.nightingale", EliteItemsSelfTests::nightingale);
    }

    private static Item item(String path) { return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("nusmp", path)).orElse(null); }

    private static SelfTests.Result registry(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        for (String id : EliteIds.allItems()) if (item(id) == null) bad.add("item " + id);
        for (String id : EliteIds.crates()) if (!BuiltInRegistries.BLOCK.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", id))) bad.add("block " + id);
        for (String id : EliteIds.plushies()) if (!BuiltInRegistries.BLOCK.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", id))) bad.add("block " + id);
        if (!bad.isEmpty()) return SelfTests.Result.fail("not registered: " + String.join(", ", bad.subList(0, Math.min(8, bad.size()))));
        return SelfTests.Result.ok(EliteIds.allItems().size() + " items, " + (EliteIds.crates().size() + EliteIds.plushies().size()) + " blocks");
    }

    private static SelfTests.Result crates(MinecraftServer server) {
        int rolls = 0;
        for (CrateTier t : CrateTier.values()) {
            CrateTable table = CrateTables.of(t);
            if (table == null || table.entries().isEmpty()) return SelfTests.Result.fail("no table for " + t.id());
            for (RewardEntry e : table.entries()) if (!RewardSerializers.known(e.type())) return SelfTests.Result.fail(t.id() + " names the unknown reward type " + e.type());
            Random r = new Random(5);
            List<String> got = CrateRoller.open(table, r::nextDouble, roll -> roll.entry().type() + ":" + roll.amount());
            if (got.isEmpty()) return SelfTests.Result.fail("an opening of " + t.id() + " gave nothing");
            rolls += got.size();
        }
        return SelfTests.Result.ok("3 tables, " + rolls + " rewards in the sample openings, types " + RewardSerializers.ids());
    }

    private static SelfTests.Result plush(MinecraftServer server) {
        ServerLevel level = server.overworld();
        FakePlayer p = SelfTests.player(level, "elite-plush", 0.5, 100, 0.5);
        EliteItemsData d = EliteItemsData.of(p);
        java.util.Set<String> saved = new java.util.LinkedHashSet<>(d.plushies());
        try {
            d.plushies().clear();
            boolean first = PlushCollection.collect(p, "warden");
            boolean again = PlushCollection.collect(p, "warden");
            boolean unknown = PlushCollection.collect(p, "not_a_mob");
            if (!first || again || unknown) return SelfTests.Result.fail("first=" + first + " again=" + again + " unknown=" + unknown);
            if (PlushCollection.count(p) != 1 || !PlushCollection.has(p, "warden")) return SelfTests.Result.fail("the count is " + PlushCollection.count(p));
            PlushCollection.collect(p, "allay");
            if (PlushCollection.count(p) != 2) return SelfTests.Result.fail("the count after a second kind is " + PlushCollection.count(p));
            ItemStack stack = new ItemStack(item("plush_zombie"));
            if (PlushCollection.kindOf(stack) == null || !"zombie".equals(PlushCollection.kindOf(stack).id())) return SelfTests.Result.fail("kindOf(plush_zombie) failed");
            return SelfTests.Result.ok(PlushKinds.ALL.size() + " plushies, the counter counts each kind once");
        } finally {
            d.plushies().clear();
            d.plushies().addAll(saved);
        }
    }

    private static SelfTests.Result abilities(MinecraftServer server) {
        int n = 0;
        for (String id : EliteIds.WEAPONS) {
            WeaponAbilities w = AbilityTables.of(id);
            if (w == null) return SelfTests.Result.fail("no technique file for " + id);
            for (AbilitySpec s : List.of(w.primary(), w.alt())) {
                if (!s.vfx().isEmpty()) {
                    try {
                        VfxShape.valueOf(s.vfx());
                    } catch (IllegalArgumentException ex) {
                        return SelfTests.Result.fail(id + ": unknown VFX shape " + s.vfx());
                    }
                }
                List<AbilitySpec.EffectSpec> all = new ArrayList<>(s.foeEffects());
                all.addAll(s.selfEffects());
                for (AbilitySpec.EffectSpec e : all) {
                    ResourceLocation rl = ResourceLocation.tryParse(e.id());
                    if (rl == null || !BuiltInRegistries.MOB_EFFECT.containsKey(rl)) return SelfTests.Result.fail(id + ": unknown effect " + e.id());
                }
                n++;
            }
        }
        return SelfTests.Result.ok(AbilityTables.size() + " weapons, " + n + " techniques");
    }

    private static SelfTests.Result engravings(MinecraftServer server) {
        int total = 0;
        for (String id : EliteIds.WEAPONS) {
            ItemStack s = new ItemStack(item(id));
            WeaponEngravings.engrave(s, server.registryAccess());
            int n = s.getEnchantments().size();
            if (n == 0) return SelfTests.Result.fail(id + " has no engraving after the engraving pass (is Tensura's enchantment set present?)");
            total += n;
        }
        return SelfTests.Result.ok("6 weapons, " + total + " engravings");
    }

    private static SelfTests.Result runic(MinecraftServer server) {
        ServerLevel level = server.overworld();
        FakePlayer p = SelfTests.player(level, "elite-runic", 0.5, 100, 0.5);
        ItemStack sword = new ItemStack(item("runic_sword_renewed"));
        RunicSwordItem it = (RunicSwordItem) sword.getItem();
        ItemStack rune = new ItemStack(Items.ECHO_SHARD, 2);
        boolean handled = it.overrideOtherStackedOnMe(sword, rune, null, ClickAction.SECONDARY, p, null);
        if (!handled || EliteComponents.runes(sword).size() != 1 || rune.getCount() != 1) return SelfTests.Result.fail("slotting failed: handled=" + handled + " runes=" + EliteComponents.runes(sword) + " left=" + rune.getCount());
        ItemStack second = new ItemStack(Items.AMETHYST_SHARD, 1);
        it.overrideOtherStackedOnMe(sword, second, null, ClickAction.SECONDARY, p, null);
        if (EliteComponents.runes(sword).size() != 1 || second.getCount() != 1) return SelfTests.Result.fail("the one-slot sword took a second rune");
        var mods = sword.get(DataComponents.ATTRIBUTE_MODIFIERS);
        boolean bonus = mods != null && mods.modifiers().stream().anyMatch(m -> m.modifier().id().getPath().equals("rune_bonus"));
        if (!bonus) return SelfTests.Result.fail("the rune's damage bonus is not on the stack");
        return SelfTests.Result.ok("a rune slots, a full sword refuses another, the damage bonus is on the stack");
    }

    private static SelfTests.Result catalyst(MinecraftServer server) {
        ServerLevel level = server.overworld();
        FakePlayer p = SelfTests.player(level, "elite-catalyst", 0.5, 100, 0.5);
        EliteItemsData d = EliteItemsData.of(p);
        int saved = d.catalystUses();
        try {
            double base = p.getAttributeBaseValue(Attributes.MAX_HEALTH);
            d.setCatalystUses(3);
            PotentialCatalystItem.applyHealth(p);
            double a = p.getAttributeValue(Attributes.MAX_HEALTH);
            PotentialCatalystItem.applyHealth(p);                                  // twice must not stack
            double b = p.getAttributeValue(Attributes.MAX_HEALTH);
            int per = EliteItemsConfig.get(EliteItemsConfig.CATALYST_HEALTH_HALF_HEARTS);
            if (Math.abs(a - b) > 1e-9 || Math.abs(a - (base + 3 * per)) > 1e-9) return SelfTests.Result.fail("max health " + a + " / " + b + " for base " + base + " and 3 uses of " + per);
            d.setCatalystUses(0);
            PotentialCatalystItem.applyHealth(p);
            if (Math.abs(p.getAttributeValue(Attributes.MAX_HEALTH) - base) > 1e-9) return SelfTests.Result.fail("the bonus did not go away");
            return SelfTests.Result.ok("the health bonus is exact and does not stack");
        } finally {
            d.setCatalystUses(saved);
            PotentialCatalystItem.applyHealth(p);
        }
    }

    private static SelfTests.Result potion(MinecraftServer server) {
        ServerLevel level = server.overworld();
        FakePlayer p = SelfTests.player(level, "elite-potion", 0.5, 100, 0.5);
        p.setHealth(1.0f);
        ItemStack s = new ItemStack(EliteItems.healthPotion(HealthPotionTier.SMALL).get(), 2);
        ((HealthPotionItem) s.getItem()).finishUsingItem(s, level, p);
        float after = p.getHealth();
        p.removeAllEffects();
        p.setHealth(p.getMaxHealth());
        if (after < 1.0f + HealthPotionTier.SMALL.heal() - 0.01f) return SelfTests.Result.fail("health after a small potion: " + after);
        return SelfTests.Result.ok("a small potion healed to " + after);
    }

    private static SelfTests.Result nightingale(MinecraftServer server) {
        ServerLevel level = server.overworld();
        FakePlayer p = SelfTests.player(level, "elite-nightingale", 0.5, 100, 0.5);
        try {
            p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(EliteItems.NIGHTINGALE_HOOD.get()));
            p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(EliteItems.NIGHTINGALE_CLOAK.get()));
            p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(EliteItems.NIGHTINGALE_LEGGINGS.get()));
            if (NightingaleBonus.worn(p) != 3) return SelfTests.Result.fail("three pieces counted as " + NightingaleBonus.worn(p));
            p.setItemSlot(EquipmentSlot.FEET, new ItemStack(EliteItems.NIGHTINGALE_BOOTS.get()));
            if (NightingaleBonus.worn(p) != 4) return SelfTests.Result.fail("four pieces counted as " + NightingaleBonus.worn(p));
            return SelfTests.Result.ok("4 pieces make the set");
        } finally {
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) p.setItemSlot(s, ItemStack.EMPTY);
        }
    }
}
