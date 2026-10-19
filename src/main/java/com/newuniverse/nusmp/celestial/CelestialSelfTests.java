package com.newuniverse.nusmp.celestial;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: nothing here can be played by the author, so the CI server smoke test runs this in the real game, with a fake player: every key is a registered, distinct item; carrying
 * the twelve gold keys is noticed and hands over the gate key exactly once; a contract can be sealed and marks the key; the Gate rules agree with the ledger; a spirit prop
 * can be made and carries the key and its power; the Spirit King can be made, scaled to the party, and has his health from the rules. Key {@code magic.celestial_keys}.
 */
public final class CelestialSelfTests {
    private CelestialSelfTests() {}

    public static void register() { SelfTests.register("magic.celestial_keys", CelestialSelfTests::keys); }

    private static SelfTests.Result keys(MinecraftServer server) {
        ServerLevel level = server.overworld();
        // 1: every key is a distinct, registered item
        Set<Item> items = new HashSet<>();
        for (CelestialKey k : CelestialKey.values()) {
            Item it = CelestialItems.item(k);
            if (it == null || !items.add(it)) return SelfTests.Result.fail("key " + k.id + " has no distinct item");
            if (CelestialItems.keyOf(new ItemStack(it)) != k) return SelfTests.Result.fail("keyOf(" + k.id + ") is wrong");
        }
        if (CelestialKey.gold().size() != 12) return SelfTests.Result.fail("there are not twelve gold keys");
        ServerPlayer p = SelfTests.player(level, "celestial_tester", 0.5, level.getMaxBuildHeight() - 10, 0.5);
        CelestialData d = CelestialAttachments.of(p);
        d.reset();
        for (ItemStack s : List.copyOf(p.getInventory().items)) if (CelestialItems.keyOf(s) != null) s.setCount(0);
        try {
            // 2: the twelve gold keys are noticed, the gate key comes once
            for (CelestialKey k : CelestialKey.gold()) p.getInventory().add(CelestialItems.stack(k));
            List<CelestialKey> fresh = CelestialLedger.scan(p);
            if (fresh.size() != 12) return SelfTests.Result.fail("scan noticed " + fresh.size() + " keys, not 12");
            if (!d.complete() || !d.gateKeyGiven()) return SelfTests.Result.fail("twelve gold keys were not seen as complete");
            int gates = count(p, CelestialKey.SPIRIT_KING_GATE);
            CelestialLedger.scan(p);
            if (gates != 1 || count(p, CelestialKey.SPIRIT_KING_GATE) != 1) return SelfTests.Result.fail("the gate key was given " + count(p, CelestialKey.SPIRIT_KING_GATE) + " times");
            // 3: the gate refuses while the contracts are missing and opens when they are sealed
            long now = level.getGameTime();
            if (KingRules.canOpen(CelestialLedger.heldMask(p), d.bound(), true, now, 0, false) != KingRules.Gate.NOT_BOUND) return SelfTests.Result.fail("the gate did not ask for the contracts");
            for (CelestialKey k : CelestialKey.gold()) {
                ItemStack s = CelestialLedger.find(p, k);
                CelestialRules.Bind r = CelestialLedger.seal(p, s);
                if (r != CelestialRules.Bind.OK) return SelfTests.Result.fail("contract with " + k.id + " refused: " + r);
                if (!CelestialLedger.ownerOf(s).equals(p.getUUID().toString())) return SelfTests.Result.fail("key " + k.id + " does not carry its owner");
            }
            if (KingRules.canOpen(CelestialLedger.heldMask(p), d.bound(), true, now, 0, false) != KingRules.Gate.OK) return SelfTests.Result.fail("the gate stayed shut with twelve bound keys");
            // 4: a spirit prop can be made and carries the key
            MagicPropEntity spirit = MagicProps.spawn(level, PropKind.CELESTIAL_1, new Vec3(0.5, level.getMaxBuildHeight() - 20, 0.5), 0f, 1f, 80, CelestialKey.LEO.ordinal(), p);
            if (spirit == null || (spirit.param() & 0xFF) != CelestialKey.LEO.ordinal()) return SelfTests.Result.fail("the Leo spirit could not be made");
            spirit.discard();
            // 5: the Spirit King is made and scaled
            SpiritKingEntity king = CelestialEntities.SPIRIT_KING.get().create(level);
            if (king == null) return SelfTests.Result.fail("the Spirit King could not be made");
            king.configure(3.0, 3, new Vec3(0.5, level.getMaxBuildHeight() - 20, 0.5), -1);
            double want = KingRules.health(3.0, 3);
            if (Math.abs(king.getMaxHealth() - want) > 1.0) return SelfTests.Result.fail("king health " + king.getMaxHealth() + " is not " + want);
            king.discard();
            return SelfTests.Result.ok("17 keys, collection, gate key once, 12 contracts, spirit prop, king " + (int) want + " hp");
        } finally {
            d.reset();
            for (ItemStack s : p.getInventory().items) if (CelestialItems.keyOf(s) != null) s.setCount(0);
        }
    }

    private static int count(ServerPlayer p, CelestialKey k) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (CelestialItems.keyOf(s) == k) n++;
        return n;
    }
}
