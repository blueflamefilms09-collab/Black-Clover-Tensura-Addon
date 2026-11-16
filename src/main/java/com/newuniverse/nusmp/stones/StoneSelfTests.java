package com.newuniverse.nusmp.stones;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.order.UniqueLedger;

import java.util.List;
import java.util.UUID;

/**
 * Why: the CI server smoke test proves what a server with no player can: {@code stones.rules} (the gate, the scale, the factors, the never-lethal recoil, the healing and control caps), {@code stones.defs}
 * (the six Wave 30 stones are still there with their old acts and gained exactly two slot acts each; no act is called anything that controls a mind), {@code stones.state} (the tables are bounded and
 * emptied) and {@code stones.ledger} (the away rule runs through the Order's ledger and touches nothing when nobody is away). Server safe: no client class.
 */
public final class StoneSelfTests {
    private StoneSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("stones.rules", s -> rules());
        SelfTests.register("stones.defs", s -> defs());
        SelfTests.register("stones.state", s -> state());
        SelfTests.register("stones.ledger", s -> ledger());
    }

    private static Result rules() {
        double g = StoneRules.GATE_EP;
        if (StoneRules.gateOk(g - 1, g) || !StoneRules.gateOk(g, g)) return Result.fail("the gate is not exactly at the setting");
        if (StoneRules.stoneScale(g, g) != 1.0 || Math.abs(StoneRules.stoneScale(g * 10, g) - 2.0) > 1.0e-9 || StoneRules.stoneScale(g * 1.0e6, g) != 2.0) return Result.fail("the stone scale is not 1 at the gate and 2 at the cap");
        if (StoneRules.stoneScale(Double.NaN, g) != 1.0 || StoneRules.stoneScale(g * 3, 0) != 1.0) return Result.fail("the stone scale trusts a bad number");
        if (StoneRules.cooldownFactor(InfinityStone.POWER) < StoneRules.MIN_FACTOR || StoneRules.cooldownFactor(InfinityStone.TIME) < StoneRules.MIN_FACTOR) return Result.fail("a cooldown factor is under the floor");
        if (StoneRules.costFactor(InfinityStone.SPACE) != 1.0 || StoneRules.cooldownFactor(null) != 1.0) return Result.fail("a stone with no lend changes costs");
        if (StoneRules.recoil(100f, 3f, 0.5) > 2f || Math.abs(StoneRules.recoil(100f, 100f, 0.05) - 5f) > 1.0e-4f) return Result.fail("the recoil can kill or is wrong");
        if (StoneRules.soulworldPulse(2.0) * 15 > 0.5) return Result.fail("the Soulworld heals more than half a bar a cast");
        if (StoneRules.lockTicks(true, false) > 20 || StoneRules.lockTicks(false, true) > 60 || StoneRules.lockTicks(false, false) > 100) return Result.fail("Time Lock is longer than the balance law");
        if (StoneRules.offeringAllowed("ep") || StoneRules.offeringAllowed("player") || !StoneRules.offeringAllowed("skill")) return Result.fail("the altar accepts the wrong thing");
        if (StoneRules.awayExpired(false, Long.MAX_VALUE, 1) || StoneRules.awayExpired(true, 10, 0) || !StoneRules.awayExpired(true, 10, 5)) return Result.fail("the away rule is wrong");
        return Result.ok("gate, scale, factors, recoil, healing and control caps, offering and away rules");
    }

    private static Result defs() {
        for (InfinityStone s : InfinityStone.values()) {
            RelicDef d = Relics.byId(s.relicId());
            if (d == null) return Result.fail("the " + s.id() + " stone is not in the catalog");
            Act a1 = d.actByKey(s.actKey(1)), a2 = d.actByKey(s.actKey(2));
            if (a1 == null || a2 == null) return Result.fail(s.id() + " lacks a slot act");
            int slotActs = 0;
            for (Act a : d.acts()) {
                if (!InfinityStone.isSlotAct(a.key())) continue;
                slotActs++;
                String n = a.displayName().toLowerCase(java.util.Locale.ROOT);
                if (n.contains("dominion") || n.contains("dominate") || n.contains("charm") || n.contains("control")) return Result.fail(s.id() + " has a controlling act: " + a.displayName());
            }
            if (slotActs != 2) return Result.fail(s.id() + " has " + slotActs + " slot acts, not 2");
            if (d.acts().size() < 3) return Result.fail(s.id() + " lost its old acts");
        }
        if (StonePassives.count(InfinityStone.SPACE) < 1) return Result.fail("the Space Stone has no attribute passive");
        return Result.ok("six stones, two slot acts each, old acts kept, no controlling act");
    }

    private static Result state() {
        StoneState.clear();
        for (int i = 0; i < StoneState.MAX_ZONES + 8; i++) StoneState.openZone(UUID.randomUUID(), new StoneState.Zone("minecraft:overworld", 0, 0, 0, 100));
        if (StoneState.zones() != StoneState.MAX_ZONES) return Result.fail("the zone table is not capped at " + StoneState.MAX_ZONES);
        UUID id = UUID.randomUUID();
        StoneState.openZone(id, new StoneState.Zone("minecraft:overworld", 0, 0, 0, 100));
        StoneState.keep(id, new StoneState.Pending(net.minecraft.world.item.ItemStack.EMPTY, "minecraft:overworld", 0, 0, 0));
        StoneState.forgetAll(id);
        if (StoneState.zone(id) != null || StoneState.ringSize(id) != 0) return Result.fail("forgetting a player left their tables");
        StoneState.clear();
        if (StoneState.zones() != 0 || !StoneState.allPending().isEmpty()) return Result.fail("clear left something");
        return Result.ok("zones capped, a player's tables are emptied");
    }

    private static Result ledger() {
        List<String> ids = List.of(InfinityStone.SOUL.relicId(), InfinityStone.MIND.relicId());
        if (!UniqueLedger.loseAway(null, ids, 1).isEmpty()) return Result.fail("a null server lost a stone");
        return Result.ok("the away rule answers without a server");
    }
}
