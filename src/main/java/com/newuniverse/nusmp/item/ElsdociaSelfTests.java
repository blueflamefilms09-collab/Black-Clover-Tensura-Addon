package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.multiverse.KnightRank;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/** 1.97.0: the in-game self-test {@code relic.elsdocia}: lineage, damage through the attribute path, no way back, learned magics, save and copy. */
public final class ElsdociaSelfTests {
    private ElsdociaSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("relic.elsdocia", ElsdociaSelfTests::lineage);
    }

    private static double attackDamage(ItemStack s) {
        double sum = 1.0;
        for (var e : s.getAttributeModifiers().modifiers())
            if (e.attribute().value() == Attributes.ATTACK_DAMAGE.value() && e.modifier().operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) sum += e.modifier().amount();
        return sum;
    }

    private static Result lineage(MinecraftServer server) throws Exception {
        if (com.newuniverse.nusmp.blackclover.MagicType.values().length != ElsdociaRules.TOTAL_MAGICS) return Result.fail("the magic count is " + com.newuniverse.nusmp.blackclover.MagicType.values().length);
        ServerLevel level = server.overworld();
        ServerPlayer a = SelfTests.player(level, "elsd_a", 0, 100, 0), b = SelfTests.player(level, "elsd_b", 2, 100, 0), c = SelfTests.player(level, "elsd_c", 4, 100, 0);
        ItemStack s = new ItemStack(NUItems.ELSDOCIA.get());
        if (!ElsdociaRules.isLegacyException(s)) return Result.fail("isLegacyException false for Elsdocia");
        if (Math.abs(attackDamage(s) - 400) > 1e-6) return Result.fail("fresh damage " + attackDamage(s));
        int junior = KnightRank.step(KnightRank.JUNIOR, 5), grand = KnightRank.step(KnightRank.GRAND, 0);

        if (ElsdociaEvents.settle(a, s, "FLAME", junior) != ElsdociaEvents.Result.CLAIMED) return Result.fail("first hold did not claim");
        if (ElsdociaComponents.get(s).damage() != 400) return Result.fail("claim changed damage");
        if (ElsdociaEvents.settle(b, s, "ICE", grand) != ElsdociaEvents.Result.PASSED) return Result.fail("pass to B failed");
        if (Math.abs(attackDamage(s) - 425) > 1e-6) return Result.fail("after Grand B damage " + attackDamage(s));
        ItemStack before = s.copy();
        if (ElsdociaEvents.settle(a, s, "FLAME", junior) != ElsdociaEvents.Result.BLOCKED) return Result.fail("A could take it back");
        if (!s.isEmpty()) return Result.fail("blocked stack stayed in A's hand");
        s = before;
        if (ElsdociaEvents.settle(c, s, "WIND", junior) != ElsdociaEvents.Result.PASSED) return Result.fail("pass to C failed");
        if (ElsdociaComponents.get(s).damage() != 425) return Result.fail("junior C changed damage to " + ElsdociaComponents.get(s).damage());
        if (ElsdociaComponents.get(s).verdict(a.getUUID()) != ElsdociaLineage.Verdict.PASS) return Result.fail("A (further back) may not take it again");
        if (ElsdociaComponents.get(s).verdict(b.getUUID()) != ElsdociaLineage.Verdict.BLOCKED) return Result.fail("B (last owner) not blocked");
        if (ElsdociaEvents.settle(a, s, "FLAME", junior) != ElsdociaEvents.Result.PASSED) return Result.fail("A could not retake it");
        if (ElsdociaComponents.get(s).damage() != 425) return Result.fail("A retaking gave a second bonus");
        var l = ElsdociaComponents.get(s);
        if (!l.learned().equals(java.util.List.of("FLAME", "ICE", "WIND"))) return Result.fail("learned " + l.learned());

        // saved (NBT) and copied
        var ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var tag = ItemStack.CODEC.encodeStart(ops, s).getOrThrow();
        ItemStack back = ItemStack.CODEC.parse(ops, tag).getOrThrow();
        if (!ElsdociaComponents.get(back).equals(l)) return Result.fail("lineage lost in the save");
        if (!ElsdociaComponents.get(s.copy()).equals(l)) return Result.fail("lineage lost in the copy");

        // an ability cast does not throw and starts its cooldown
        ElsdociaArts.Signature sig = ElsdociaArts.signature(com.newuniverse.nusmp.blackclover.MagicType.FLAME);
        if (sig.book() == null || sig.page() == null) return Result.fail("Flame has no signature page");
        a.getPersistentData().putLong("nusmp_elsdocia_cd_FLAME", 0);
        boolean cast = ElsdociaArts.cast(a, "FLAME");
        if (cast && a.getPersistentData().getLong("nusmp_elsdocia_cd_FLAME") <= level.getGameTime()) return Result.fail("no cooldown after the cast");
        return Result.ok("damage " + l.damage() + ", owners " + l.owners().size() + ", learned " + l.learned().size() + ", Flame signature " + sig.page().name() + (cast ? " cast" : " not cast"));
    }
}
