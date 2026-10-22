package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.client.relics.worn.WornVisibility;
import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The self-test {@code relic.wear} (run by the CI server smoke test, see {@link SelfTests}): every relic can be put on, and is drawn unless the player says otherwise.
 * <ul>
 *   <li><b>Data</b>: the slot files, entity assignments and item tags of the jar are complete ({@link RelicWear#checkSlots}), and every registered relic that sits in a Curios slot maps to a
 *       slot the mod defines, gives that slot to the player and is in its tag, both in the files and as the game's own tag (the tags are bound, the item is a member of
 *       {@code curios:<slot>}). This covers every wave, the pairs, the Djinn and the forge items, because it walks {@link Relics#all()}.</li>
 *   <li><b>Armour</b>: every head, chest and feet relic is an armour item of the right type, the game puts it in the matching equipment slot, a slot click accepts it, and a right-click
 *       with it in the hand puts it on a (fake) player.</li>
 *   <li><b>Curios, when Curios is installed</b>: the player's Curios inventory has every slot type the relics use (the runtime fact behind the data), and right-clicking a relic of each slot
 *       type puts it in that slot ({@link RelicCurioEquip}). Without Curios this part is skipped and the result says so.</li>
 *   <li><b>Visibility</b>: the contract table of {@link WornVisibility} holds (pure).</li>
 * </ul>
 */
public final class RelicWearSelfTests {
    private RelicWearSelfTests() {}

    public static void register() { SelfTests.register("relic.wear", RelicWearSelfTests::run); }

    static SelfTests.Result run(MinecraftServer server) throws IOException {
        String contract = WornVisibility.checkTable();
        if (contract != null) return SelfTests.Result.fail("visibility contract: " + contract);
        RelicWear.Data data = RelicWear.read(RelicWear.classPath(RelicWearSelfTests.class.getClassLoader()));
        List<String> slotProblems = RelicWear.checkSlots(data);
        if (!slotProblems.isEmpty()) return SelfTests.Result.fail(slotProblems.size() + " slot problems, first: " + slotProblems.get(0));
        List<RelicDef> all = Relics.all();
        if (all.isEmpty()) return SelfTests.Result.fail("no relic is registered");
        Map<String, String> slots = new LinkedHashMap<>();
        int curios = 0, armour = 0;
        for (RelicDef d : all) {
            slots.put(Relics.itemId(d), d.slot().name());
            if (d.slot().isCurio()) curios++;
            if (d.slot().isArmor()) armour++;
        }
        List<String> problems = RelicWear.checkRelics(slots, data);
        if (!problems.isEmpty()) return SelfTests.Result.fail(problems.size() + " relics cannot be put on, first: " + problems.get(0));
        if (curios < 300 || armour < 40) return SelfTests.Result.fail("too few relics found: " + curios + " Curios, " + armour + " armour");

        for (RelicDef d : all) {                                                      // the game's own tags, bound at this point
            if (!d.slot().isCurio()) continue;
            Item item = item(d);
            if (item == null) return SelfTests.Result.fail(d.id() + ": its item is not registered");
            TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", d.slot().curiosSlot()));
            if (!item.builtInRegistryHolder().is(tag)) return SelfTests.Result.fail(d.id() + " is not in the game's tag curios:" + d.slot().curiosSlot());
        }

        ServerLevel level = server.overworld();
        FakePlayer p = SelfTests.player(level, "relic_wear", 0, 200, 0);
        String armourDetail = armour(all, level, p);
        if (armourDetail.startsWith("FAIL ")) return SelfTests.Result.fail(armourDetail.substring(5));
        String curiosDetail = curios(all, p);
        if (curiosDetail.startsWith("FAIL ")) return SelfTests.Result.fail(curiosDetail.substring(5));
        return SelfTests.Result.ok(curios + " Curios relics and " + armour + " armour relics map to defined, assigned, tagged slots; " + armourDetail + "; " + curiosDetail + "; visibility contract holds");
    }

    private static Item item(RelicDef d) {
        ResourceLocation id = ResourceLocation.parse(Relics.itemId(d));
        return BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : null;
    }

    /** Every armour relic is the right armour type and reaches the matching equipment slot; one of each slot is put on by a right-click. Returns a detail line, or "FAIL reason". */
    private static String armour(List<RelicDef> all, ServerLevel level, FakePlayer p) {
        RelicDef[] firstOf = new RelicDef[3];
        int checked = 0;
        for (RelicDef d : all) {
            if (!d.slot().isArmor() || d.isAdopted()) continue;                      // an adopted item is an older item of the mod, not a RelicArmorItem
            Item item = item(d);
            if (!(item instanceof ArmorItem a)) return "FAIL " + d.id() + " is an armour relic but its item is not an armour item";
            EquipmentSlot want = EquipmentSlot.valueOf(RelicWear.armourEquipSlot(d.slot().name()));
            if (a.getEquipmentSlot() != want) return "FAIL " + d.id() + ": its armour type is for " + a.getEquipmentSlot() + ", the relic is a " + want + " relic";
            ItemStack stack = new ItemStack(item);
            if (p.getEquipmentSlotForItem(stack) != want) return "FAIL " + d.id() + ": the game would put it in " + p.getEquipmentSlotForItem(stack);
            if (!stack.canEquip(want, p)) return "FAIL " + d.id() + ": a click on the " + want + " armour slot would refuse it";
            checked++;
            int i = want == EquipmentSlot.HEAD ? 0 : want == EquipmentSlot.CHEST ? 1 : 2;
            if (firstOf[i] == null) firstOf[i] = d;
        }
        for (RelicDef d : firstOf) {
            if (d == null) return "FAIL there is no relic for one of head, chest and feet";
            EquipmentSlot want = EquipmentSlot.valueOf(RelicWear.armourEquipSlot(d.slot().name()));
            p.setShiftKeyDown(false);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(d)));
            p.getItemInHand(InteractionHand.MAIN_HAND).use(level, p, InteractionHand.MAIN_HAND);
            boolean worn = p.getItemBySlot(want).getItem() == item(d);
            p.setItemSlot(want, ItemStack.EMPTY);
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (!worn) return "FAIL a right-click with " + d.id() + " did not put it on the " + want + " slot";
        }
        return checked + " armour relics fit their equipment slot and right-click puts one of each on";
    }

    /** With Curios: the slot types are there and a right-click puts a relic of each type in its slot. Returns a detail line, or "FAIL reason". */
    private static String curios(List<RelicDef> all, FakePlayer p) {
        if (!CuriosLink.available()) return "Curios is not installed here: the live slot and right-click part was skipped";
        Map<String, IItemHandler> handlers = CuriosLink.slotHandlers(p);
        if (handlers.isEmpty()) return "FAIL Curios is installed but the player has no Curios inventory (no slot type is assigned to the player)";
        for (String slot : RelicWear.RELIC_SLOTS) if (!handlers.containsKey(slot)) return "FAIL the player's Curios inventory has no \"" + slot + "\" slot (have " + handlers.keySet() + ")";
        if (!handlers.containsKey("dungeon_relic")) return "FAIL the player's Curios inventory has no \"dungeon_relic\" slot";
        int done = 0;
        for (String slot : RelicWear.RELIC_SLOTS) {
            RelicDef pick = null;
            for (RelicDef d : all) if (slot.equals(d.slot().curiosSlot()) && !d.isAdopted()) { pick = d; break; }
            if (pick == null) return "FAIL no relic for the \"" + slot + "\" slot";
            p.setShiftKeyDown(false);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(pick)));
            CurioEquipRules.Result r = RelicCurioEquip.tryEquip(p, InteractionHand.MAIN_HAND);
            boolean worn = false;
            for (ItemStack s : CuriosLink.equipped(p)) if (s.getItem() == item(pick)) worn = true;
            if (handlers.get(slot) instanceof IItemHandlerModifiable m) for (int i = 0; i < m.getSlots(); i++) m.setStackInSlot(i, ItemStack.EMPTY);
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            Relics.invalidate(p);
            if (r != CurioEquipRules.Result.EQUIPPED || !worn) return "FAIL right-click equip of " + pick.id() + " in \"" + slot + "\": " + r.text + (worn ? "" : " (not found in the Curios inventory)");
            done++;
        }
        return "Curios has every slot type; right-click equip works in " + done + " slot types";
    }
}
