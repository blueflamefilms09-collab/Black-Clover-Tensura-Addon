package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.List;
import java.util.Map;

/**
 * The self-test {@code relic.headfeet} (CI server smoke test): head and feet relics are worn in the Curios slots {@code relic_head} / {@code relic_feet}.
 * <ul>
 *   <li><b>Data</b>: every head and feet relic (registered, not adopted) is a member of the game's tag {@code curios:relic_head} / {@code curios:relic_feet}.</li>
 *   <li><b>Rules</b>: {@link HeadFeetMoveRules} (pure) holds.</li>
 *   <li><b>With Curios</b>: for a head and a feet relic, a right-click equip lands in the Curios place and leaves the armour slot empty, a second one meets a full place and changes nothing, a relic
 *       found in the armour slot is moved to the Curios place, a relic whose place is taken stays where it is, and a Curios-worn relic keeps its armour points ({@link HeadFeetCurios#shares}).
 *       Without Curios, or with the setting off, this part is skipped and the result says so.</li>
 * </ul>
 */
public final class HeadFeetSelfTests {
    private HeadFeetSelfTests() {}

    public static void register() { SelfTests.register("relic.headfeet", HeadFeetSelfTests::run); }

    static SelfTests.Result run(MinecraftServer server) {
        String rules = rules();
        if (rules != null) return SelfTests.Result.fail("rules: " + rules);
        RelicDef head = null, feet = null;
        int heads = 0, feets = 0;
        for (RelicDef d : Relics.all()) {
            if (d.isAdopted() || !(d.slot() == RelicSlot.HEAD || d.slot() == RelicSlot.FEET)) continue;
            Item item = item(d);
            if (item == null) return SelfTests.Result.fail(d.id() + ": its item is not registered");
            String slot = HeadFeetMoveRules.curiosSlot(d.slot().name());
            if (!item.builtInRegistryHolder().is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", slot))))
                return SelfTests.Result.fail(d.id() + " is not in the game's tag curios:" + slot);
            if (d.slot() == RelicSlot.HEAD) { heads++; if (head == null) head = d; }
            else { feets++; if (feet == null) feet = d; }
        }
        if (heads < 100 || feets < 40 || head == null || feet == null) return SelfTests.Result.fail("too few head / feet relics found: " + heads + " head, " + feets + " feet");
        String data = heads + " head and " + feets + " feet relics are in their Curios tags";
        if (!HeadFeetCurios.enabled()) return SelfTests.Result.ok(data + "; the setting headFeetInCurios is off here: the live part was skipped");
        if (!CuriosLink.available()) return SelfTests.Result.ok(data + "; Curios is not installed here: the live part was skipped");
        FakePlayer p = SelfTests.player(server.overworld(), "relic_headfeet", 0, 200, 0);
        Map<String, IItemHandler> handlers = CuriosLink.slotHandlers(p);
        for (String slot : List.of("relic_head", "relic_feet")) if (!handlers.containsKey(slot)) return SelfTests.Result.fail("the player's Curios inventory has no \"" + slot + "\" slot (have " + handlers.keySet() + ")");
        try {
            String a = live(p, head, EquipmentSlot.HEAD);
            if (a != null) return SelfTests.Result.fail(a);
            String b = live(p, feet, EquipmentSlot.FEET);
            if (b != null) return SelfTests.Result.fail(b);
        } finally {
            reset(p);
        }
        return SelfTests.Result.ok(data + "; right-click equip, the move out of the armour slot, the full place and the kept armour points work for " + head.id() + " and " + feet.id());
    }

    /** The pure part. Returns a problem or null. */
    static String rules() {
        if (!"relic_head".equals(HeadFeetMoveRules.curiosSlot("HEAD")) || !"relic_feet".equals(HeadFeetMoveRules.curiosSlot("feet"))) return "slot names";
        for (String other : new String[]{"CHEST", "NECK", "BACK", "BELT", "HANDS", "CHARM", "MAIN_HAND", "CONSUMABLE", null}) if (HeadFeetMoveRules.curiosSlot(other) != null) return "a " + other + " relic got a head / feet slot";
        if (!HeadFeetMoveRules.redirectOnUse(true, "HEAD", false, false)) return "a right-click is not redirected";
        if (HeadFeetMoveRules.redirectOnUse(false, "HEAD", false, false)) return "the setting off still redirects";
        if (HeadFeetMoveRules.redirectOnUse(true, "FEET", true, false)) return "a sneaking right-click is redirected (it is the ritual)";
        if (HeadFeetMoveRules.redirectOnUse(true, "FEET", false, true)) return "an empty hand is redirected";
        if (!HeadFeetMoveRules.belongsTo("HEAD", "HEAD") || !HeadFeetMoveRules.belongsTo("FEET", "FEET") || HeadFeetMoveRules.belongsTo("HEAD", "FEET") || HeadFeetMoveRules.belongsTo("CHEST", "CHEST")) return "belongsTo";
        if (HeadFeetMoveRules.due(false, false, 100, 0) || HeadFeetMoveRules.due(true, true, 100, 0) || HeadFeetMoveRules.due(true, false, 100, 101) || !HeadFeetMoveRules.due(true, false, 100, 100)) return "due";
        if (HeadFeetMoveRules.retryAt(1000) != 1000 + HeadFeetMoveRules.RETRY_TICKS) return "retryAt";
        List<HeadFeetMoveRules.Share> s = HeadFeetMoveRules.shares(List.of("a", "b", "c"), List.of("HEAD", "CHEST", "FEET"), List.of(2, 5, 1), List.of(0.0, 3.0, 0.75));
        if (s.size() != 3 || !s.get(0).equals(new HeadFeetMoveRules.Share("a/armor", false, 2)) || !s.get(2).toughness()) return "shares: " + s;
        return null;
    }

    private static String live(FakePlayer p, RelicDef d, EquipmentSlot armour) {
        Item item = item(d);
        String slot = HeadFeetMoveRules.curiosSlot(d.slot().name());
        reset(p);
        // 1. right-click equip lands in the Curios place, the armour slot stays empty
        p.setShiftKeyDown(false);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 2));
        CurioEquipRules.Result r = RelicCurioEquip.tryEquip(p, InteractionHand.MAIN_HAND);
        if (r != CurioEquipRules.Result.EQUIPPED) return d.id() + ": the right-click equip answered \"" + r.text + "\"";
        if (!p.getItemBySlot(armour).isEmpty()) return d.id() + ": the right-click also filled the armour slot";
        if (!worn(p, item)) return d.id() + ": it is not in the Curios inventory after the right-click";
        if (p.getItemInHand(InteractionHand.MAIN_HAND).getCount() != 1) return d.id() + ": the stack in the hand did not shrink by one";
        Relics.invalidate(p);
        boolean located = false;
        for (Relics.Located l : Relics.locatedList(p)) if (l.def() == d && l.where() == Relics.Where.CURIOS) located = true;
        if (!located) return d.id() + ": the relic scan does not find it in the Curios slot";
        if (d.armorDefence() > 0 && HeadFeetCurios.shares(Relics.locatedList(p)).stream().noneMatch(s -> s.key().equals(d.id() + "/armor") && s.amount() == d.armorDefence()))
            return d.id() + ": the armour points of its armour form are not kept";
        // 2. the place is taken: nothing changes, and the second copy is not worn as armour
        CurioEquipRules.Result full = RelicCurioEquip.tryEquip(p, InteractionHand.MAIN_HAND);
        if (full != CurioEquipRules.Result.SLOT_FULL) return d.id() + ": a taken " + slot + " place answered \"" + full.text + "\"";
        if (p.getItemInHand(InteractionHand.MAIN_HAND).getCount() != 1 || !p.getItemBySlot(armour).isEmpty()) return d.id() + ": a refused equip changed the hand or the armour slot";
        // 3. with the place taken, a relic in the armour slot stays there
        p.setItemSlot(armour, new ItemStack(item));
        if (HeadFeetCurios.moveWorn(p) != 0 || p.getItemBySlot(armour).isEmpty()) return d.id() + ": a relic whose Curios place is taken was moved or lost";
        reset(p);
        // 4. a relic in the armour slot is moved to the free Curios place
        p.setItemSlot(armour, new ItemStack(item));
        int moved = HeadFeetCurios.moveWorn(p);
        if (moved != 1 || !p.getItemBySlot(armour).isEmpty() || !worn(p, item)) return d.id() + ": the move out of the armour slot did not happen (moved " + moved + ")";
        // 5. with nothing left in the armour slots there is nothing to move
        if (HeadFeetCurios.moveWorn(p) != 0) return d.id() + ": a second move found something to move";
        reset(p);
        return null;
    }

    private static boolean worn(FakePlayer p, Item item) {
        CuriosLink.forget(p.getUUID());
        for (ItemStack s : CuriosLink.equipped(p)) if (s.getItem() == item) return true;
        return false;
    }

    /** Empties the hand, the head / feet armour slots and the two Curios slots of the test player. */
    private static void reset(FakePlayer p) {
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        p.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        Map<String, IItemHandler> handlers = CuriosLink.slotHandlers(p);
        for (String slot : List.of("relic_head", "relic_feet")) {
            if (handlers.get(slot) instanceof IItemHandlerModifiable m) for (int i = 0; i < m.getSlots(); i++) m.setStackInSlot(i, ItemStack.EMPTY);
        }
        Relics.invalidate(p);
        CuriosLink.forget(p.getUUID());
    }

    private static Item item(RelicDef d) {
        ResourceLocation id = ResourceLocation.parse(Relics.itemId(d));
        return BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : null;
    }
}
