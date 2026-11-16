package com.newuniverse.nusmp.saiyan.armor;

import com.mojang.authlib.GameProfile;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.UUID;

/**
 * Why: the CI server smoke test of the Saiyan alloy set (prefix {@code saiyan.armor} in SMOKE_EXPECT). On the dedicated server it checks what the server owns: the ingot and the four pieces are
 * registered under their ids, the armour material carries the numbers of {@link SaiyanArmorRules} for every piece, the pieces sit in the right slots, and a stand-in player wearing the pieces is counted
 * (four give the set bonus, three give none) without touching a client class.
 */
public final class SaiyanArmorSelfTests {
    private SaiyanArmorSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("saiyan.armor.items", SaiyanArmorSelfTests::items);
        SelfTests.register("saiyan.armor.set", SaiyanArmorSelfTests::set);
    }

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, id)); }

    private static Result items(MinecraftServer server) {
        if (item(SaiyanArmorRules.INGOT) != SaiyanArmorItems.INGOT.get()) return Result.fail("the alloy ingot is not registered under " + SaiyanArmorRules.INGOT);
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) {
            Item it = item(SaiyanArmorRules.PIECES.get(i));
            if (!(it instanceof SaiyanArmorItem a)) return Result.fail(SaiyanArmorRules.PIECES.get(i) + " is not a Saiyan armour item");
            if (a.getEquipmentSlot() != slots[i]) return Result.fail(SaiyanArmorRules.PIECES.get(i) + " is in the wrong slot");
            if (a.getDefense() != SaiyanArmorRules.DEFENSE[i]) return Result.fail(SaiyanArmorRules.PIECES.get(i) + " has " + a.getDefense() + " armour points, not " + SaiyanArmorRules.DEFENSE[i]);
            if (a.getToughness() != SaiyanArmorRules.TOUGHNESS) return Result.fail(SaiyanArmorRules.PIECES.get(i) + " has the wrong toughness");
            if (new ItemStack(a).getMaxDamage() != a.getType().getDurability(SaiyanArmorRules.DURABILITY_FACTOR)) return Result.fail(SaiyanArmorRules.PIECES.get(i) + " has the wrong durability");
        }
        if (!SaiyanArmorItems.ALLOY.get().repairIngredient().get().test(new ItemStack(SaiyanArmorItems.INGOT.get()))) return Result.fail("the alloy ingot does not repair the set");
        return Result.ok("the ingot and the four pieces are registered, in their slots, with " + SaiyanArmorRules.total(SaiyanArmorRules.DEFENSE) + " armour points and the alloy ingot as the repair");
    }

    private static Result set(MinecraftServer server) {
        FakePlayer fp = FakePlayerFactory.get(server.overworld(), new GameProfile(UUID.nameUUIDFromBytes("saiyan_armor_selftest".getBytes()), "[saiyan_armor_selftest]"));
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        try {
            for (int i = 0; i < 4; i++) fp.setItemSlot(slots[i], new ItemStack(item(SaiyanArmorRules.PIECES.get(i))));
            if (SaiyanArmorSet.worn(fp) != 4) return Result.fail("four worn pieces count as " + SaiyanArmorSet.worn(fp));
            double full = SaiyanArmorSet.kiRegenBonus(fp);
            if (SaiyanArmorConfig.setBonus() && !(full > 0.0 && full <= SaiyanArmorRules.MAX_KI_REGEN)) return Result.fail("the full set gives " + full);
            fp.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
            if (SaiyanArmorSet.worn(fp) != 3 || SaiyanArmorSet.kiRegenBonus(fp) != 0.0) return Result.fail("three pieces must give no bonus");
            return Result.ok("four pieces count as the set (+" + full + "% Ki a second), three give nothing");
        } finally {
            for (EquipmentSlot s : slots) fp.setItemSlot(s, ItemStack.EMPTY);
        }
    }
}
