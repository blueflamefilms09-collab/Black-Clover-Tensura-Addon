package com.newuniverse.nusmp.blackclover;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the Grimoire Slot rework (docs/grimoire_slot_rework.md) moves the slot into the inventory screen and changes how the hip book is synced; nobody can click it here, so the CI server smoke test checks the
 * parts that need no client: key {@code slot.rules} (the click rules, the scan stagger, the change signal and the rate limit) and {@code slot.slot} (a stand-in player: set / get / replace round trip, the scan
 * moves an owned book from the inventory into the empty slot, a foreign book is refused, a stack of two places one, take / swap move the book through the cursor). Server side only; the player's old slot is
 * put back afterwards.
 */
public final class GrimoireSlotSelfTests {
    private GrimoireSlotSelfTests() {}

    public static void register() {
        SelfTests.register("slot.rules", s -> rules());
        SelfTests.register("slot.slot", GrimoireSlotSelfTests::slot);
    }

    private static Result rules() {
        SlotClickRules.Action[] a = SlotClickRules.Action.values();
        if (SlotClickRules.decide(true, true, true, false, 0) != SlotClickRules.Action.NONE) return Result.fail("empty cursor on an empty slot did something");
        if (SlotClickRules.decide(true, false, true, false, 0) != SlotClickRules.Action.PLACE) return Result.fail("an owned book on an empty slot is not placed");
        if (SlotClickRules.decide(false, false, true, false, 0) != SlotClickRules.Action.NONE) return Result.fail("a foreign item was placed");
        if (SlotClickRules.decide(true, false, false, false, 1) != SlotClickRules.Action.SWAP) return Result.fail("an owned book on a full slot is not swapped");
        if (SlotClickRules.decide(false, true, false, false, 0) != SlotClickRules.Action.TAKE) return Result.fail("an empty cursor does not take the book");
        if (SlotClickRules.decide(false, true, false, true, 0) != SlotClickRules.Action.TO_INVENTORY) return Result.fail("shift does not send the book to the inventory");
        if (SlotClickRules.decide(false, true, true, true, 0) != SlotClickRules.Action.FROM_INVENTORY) return Result.fail("shift on an empty slot does not pull a book");
        if (SlotClickRules.decide(true, false, false, false, 2) != SlotClickRules.Action.NONE) return Result.fail("a middle click did something");
        Set<Integer> phases = new HashSet<>();
        for (int i = 0; i < 200; i++) phases.add(SlotClickRules.phase(UUID.randomUUID()));
        if (phases.size() < 10) return Result.fail("the scan phases are not spread: " + phases.size());
        int due = 0;
        for (long t = 0; t < 200; t++) if (SlotClickRules.scanDue(t, 7)) due++;
        if (due != 10) return Result.fail("a player is scanned " + due + " times in 200 ticks, not 10");
        SlotClickRules.Signal sig = new SlotClickRules.Signal();
        if (!sig.dirty()) return Result.fail("a fresh signal does not send");
        sig.markSent();
        if (sig.dirty()) return Result.fail("a sent signal is dirty");
        sig.bump();
        if (!sig.dirty()) return Result.fail("a bump is not seen");
        SlotClickRules.Limiter lim = new SlotClickRules.Limiter();
        int ok = 0;
        for (int i = 0; i < 50; i++) if (lim.allow(100)) ok++;
        if (ok != SlotClickRules.CLICKS_PER_WINDOW || !lim.allow(100 + SlotClickRules.WINDOW_TICKS)) return Result.fail("the click limit let " + ok + " through in one tick");
        return Result.ok(a.length + " actions, scan every " + SlotClickRules.SCAN_EVERY + " ticks spread by UUID (" + phases.size() + " phases in 200 ids), " + SlotClickRules.CLICKS_PER_WINDOW + " clicks per " + SlotClickRules.WINDOW_TICKS + " ticks");
    }

    private static Result slot(MinecraftServer server) {
        ServerPlayer fp = SelfTests.player(server.overworld(), "slot_selftest", 0.5, 100, 0.5);
        ItemStack kept = GrimoireSlot.get(fp), keptInv = fp.getInventory().getItem(7), keptCursor = fp.containerMenu.getCarried();
        GrimoireSlot.State st = fp.getData(GrimoireSlot.STATE);
        try {
            GrimoireSlot.set(fp, ItemStack.EMPTY);
            fp.getInventory().setItem(7, ItemStack.EMPTY);
            fp.containerMenu.setCarried(ItemStack.EMPTY);
            ItemStack book = GrimoireItem.create(fp, GrimoireCover.THREE_LEAF, MagicType.FLAME, null);
            ItemStack other = GrimoireItem.create(fp, GrimoireCover.THREE_LEAF, MagicType.WIND, null);

            GrimoireSlot.set(fp, book);
            if (!GrimoireSlot.holdsOwn(fp, MagicType.FLAME)) return Result.fail("set / get lost the book");
            if (!st.signal.dirty()) return Result.fail("set did not mark the slot changed");
            if (!GrimoireSlot.replaceOwned(fp, old -> other) || !GrimoireSlot.holdsOwn(fp, MagicType.WIND)) return Result.fail("replaceOwned did not swap the book");
            GrimoireSlot.set(fp, ItemStack.EMPTY);
            if (!GrimoireSlot.get(fp).isEmpty()) return Result.fail("clearing the slot failed");

            fp.getInventory().setItem(7, book);
            if (!GrimoireSlot.scanInventory(fp) || !GrimoireSlot.holdsOwn(fp, MagicType.FLAME) || !fp.getInventory().getItem(7).isEmpty()) return Result.fail("the scan did not move the owned book into the empty slot");
            if (GrimoireSlot.scanInventory(fp)) return Result.fail("the scan moved a book into a full slot");

            GrimoireSlot.set(fp, ItemStack.EMPTY);
            fp.containerMenu.setCarried(GrimoireItem.createUnbound(GrimoireCover.THREE_LEAF, MagicType.FLAME, null));
            if (GrimoireSlot.apply(fp, st, 0, false) || !GrimoireSlot.get(fp).isEmpty()) return Result.fail("a book owned by nobody was placed");

            fp.containerMenu.setCarried(book.copyWithCount(2));
            if (!GrimoireSlot.apply(fp, st, 0, false) || GrimoireSlot.get(fp).getCount() != 1 || fp.containerMenu.getCarried().getCount() != 1) return Result.fail("a stack of two did not place exactly one");
            fp.containerMenu.setCarried(ItemStack.EMPTY);
            if (!GrimoireSlot.apply(fp, st, 0, false) || !GrimoireSlot.get(fp).isEmpty() || !GrimoireItem.isOwnedBy(fp.containerMenu.getCarried(), fp.getUUID())) return Result.fail("taking the book did not give it to the cursor");
            if (st.scanPausedUntil <= fp.tickCount) return Result.fail("taking the book did not pause the scan");
            fp.containerMenu.setCarried(other.copy());
            GrimoireSlot.set(fp, book.copy());
            if (!GrimoireSlot.apply(fp, st, 1, false) || !GrimoireSlot.holdsOwn(fp, MagicType.WIND) || !GrimoireItem.isOwnedBy(fp.containerMenu.getCarried(), fp.getUUID())) return Result.fail("swapping through the cursor failed");
            return Result.ok("round trip, scan, foreign refusal, stack of two, take and swap all behave");
        } finally {
            GrimoireSlot.set(fp, kept);
            fp.getInventory().setItem(7, keptInv);
            fp.containerMenu.setCarried(keptCursor);
            st.scanPausedUntil = 0;
        }
    }
}
