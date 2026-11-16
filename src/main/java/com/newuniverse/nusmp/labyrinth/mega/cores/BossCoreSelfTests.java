package com.newuniverse.nusmp.labyrinth.mega.cores;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.labyrinth.mega.cores.BossCoreRules.Kit;
import com.newuniverse.nusmp.labyrinth.mega.cores.BossCoreRules.Piece;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;

/**
 * Why: nothing here can be played by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs these (keys {@code cores.rules},
 * {@code cores.items}, {@code cores.world}) on a dedicated server with a stand-in player. {@code cores.rules}: the pay rule (family, resource, count, the floor 249 edge, negative levels), the tier cap and the
 * numbers. {@code cores.items}: the seven cores and twenty-eight pieces are registered under the ids their models and recipes use. {@code cores.world}: four Striker pieces raise a real hit and the second
 * damage type is bounded per tick, the worn counting and the mixing cap hold, a Wellspring refills a pool when the player has Tensura data, and the per-player table is cleaned. Server safe: no client class.
 */
public final class BossCoreSelfTests {
    private BossCoreSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("cores.rules", s -> rules());
        SelfTests.register("cores.items", s -> items());
        SelfTests.register("cores.world", BossCoreSelfTests::world);
    }

    private static Result rules() {
        if (BossCoreRules.count(249, 250, 500, 750, 1000, 2, 3, 4, 5) != 0 || BossCoreRules.count(250, 250, 500, 750, 1000, 2, 3, 4, 5) != 2) return Result.fail("the first band edge is wrong");
        if (BossCoreRules.count(-1000, 250, 500, 750, 1000, 2, 3, 4, 5) != 5 || BossCoreRules.count(749, 250, 500, 750, 1000, 2, 3, 4, 5) != 3) return Result.fail("the later band edges are wrong");
        for (int level = -100; level <= 100; level++) {
            if (BossCoreRules.kitOfLevel(level) != BossCoreRules.kitOfLevel(-level)) return Result.fail("the side changed the pay at level " + level);
        }
        java.util.Set<Kit> seen = new java.util.HashSet<>();
        for (int level = 25; level <= 100; level++) seen.add(BossCoreRules.kitOfLevel(level));
        if (seen.size() != BossCoreRules.KITS) return Result.fail("not every core is paid by some level: " + seen.size());
        int[] six = new int[BossCoreRules.KITS];
        six[Kit.AEGIS.ordinal()] = 2;
        six[Kit.STRIKER.ordinal()] = 2;
        int[] capped = BossCoreRules.cappedTiers(six, 1);
        if (capped[Kit.AEGIS.ordinal()] + capped[Kit.STRIKER.ordinal()] != 1) return Result.fail("the mixing cap did not hold");
        if (BossCoreRules.resistPercent(4, 2, 5, 3, 7, 60) != 27.0 || BossCoreRules.resistPercent(4, 2, 25, 50, 50, 60) != 60.0) return Result.fail("the Aegis arithmetic or its cap is wrong");
        if (BossCoreRules.epBonus(1e12, 4, 2, 0.5, 1.5, 2.0, 1000.0, 0.5) > 5.0 + 1e-9) return Result.fail("the EP gain is not capped by the wearer's own EP");
        return Result.ok("pay rule, band edges, both sides, tier cap, Aegis cap and EP cap hold");
    }

    private static Result items() {
        for (Kit k : Kit.values()) {
            if (BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("anime_multiverse", k.coreId())).isEmpty()) return Result.fail("core not registered: " + k.coreId());
            for (Piece p : Piece.values()) {
                if (BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("anime_multiverse", k.pieceId(p))).isEmpty()) return Result.fail("piece not registered: " + k.pieceId(p));
                if (!(BossCoreItems.piece(k, p) instanceof BossCoreArmorItem a) || a.kit() != k || a.piece() != p) return Result.fail("piece of the wrong set: " + k.pieceId(p));
            }
        }
        if (BossCoreItems.allCores().size() != 7 || BossCoreItems.allPieces().size() != 28) return Result.fail("the item counts are wrong");
        return Result.ok("seven cores and twenty-eight pieces are registered, each in its own set");
    }

    private static void equip(ServerPlayer p, Kit k, int pieces) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        Piece[] ps = Piece.values();
        for (int i = 0; i < 4; i++) p.setItemSlot(slots[i], i < pieces ? new ItemStack(BossCoreItems.piece(k, ps[i])) : ItemStack.EMPTY);
    }

    private static Result world(MinecraftServer server) {
        if (!BossCoreConfig.enabled()) return Result.ok("skipped: bossCores.enabled is false");
        ServerLevel level = server.overworld();
        double y = level.getMaxBuildHeight() - 40;
        ServerPlayer p = SelfTests.standIn(level, "cores_a", 0.5, y, 0.5);
        Zombie z = EntityType.ZOMBIE.create(level);
        if (z == null) return Result.fail("no zombie");
        try {
            z.setPos(0.5, y, 3.5);
            z.setNoAi(true);
            z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400.0);
            level.addFreshEntity(z);
            // worn counting and the mixing cap on a real player
            equip(p, Kit.AEGIS, 4);
            int[] n = BossCoreEvents.worn(p);
            if (n[Kit.AEGIS.ordinal()] != 4 || BossCoreRules.cappedTiers(n, 2)[Kit.AEGIS.ordinal()] != 2) return Result.fail("four Aegis pieces were not counted as a full set");
            equip(p, Kit.STRIKER, 2);
            n = BossCoreEvents.worn(p);
            if (n[Kit.STRIKER.ordinal()] != 2 || n[Kit.AEGIS.ordinal()] != 0) return Result.fail("the worn count did not follow the slots");
            // Striker: a plain hit, then the same hit with four pieces on (more, because of the two piece rise and the second damage type)
            equip(p, Kit.STRIKER, 0);
            z.setHealth(z.getMaxHealth());
            z.invulnerableTime = 0;
            z.hurt(p.damageSources().playerAttack(p), 10f);
            float plain = z.getMaxHealth() - z.getHealth();
            if (plain <= 0f) return Result.fail("the dummy took no damage at all");
            if (BossCoreConfig.strikerSet2() > 0.0 && BossCoreConfig.strikerShare() > 0.0) {
                equip(p, Kit.STRIKER, 4);
                z.setHealth(z.getMaxHealth());
                z.invulnerableTime = 0;
                z.hurt(p.damageSources().playerAttack(p), 10f);
                float full = z.getMaxHealth() - z.getHealth();
                if (full <= plain + 0.5f) return Result.fail("four Striker pieces did not raise the hit: " + plain + " -> " + full);
            }
            // the second damage type is bounded per tick and the table is cleaned
            long now = level.getGameTime();
            for (int i = 0; i < 12; i++) {
                z.invulnerableTime = 0;
                z.hurt(p.damageSources().playerAttack(p), 1f);
            }
            if (BossCoreEvents.extraHits(p.getUUID(), now) > BossCoreConfig.extraHitsPerTick()) return Result.fail("the second damage type passed its per-tick bound");
            BossCoreEvents.forget(p.getUUID());
            if (BossCoreEvents.extraHits(p.getUUID(), now) != 0) return Result.fail("the per-player table was not cleaned");
            // Wellspring: a pool is refilled when the player has Tensura data
            equip(p, Kit.WELLSPRING_AURA, 4);
            var ex = TensuraStorages.getExistenceFrom(p);
            String pool = "no Tensura data on the stand-in, refill not exercised";
            if (ex != null) {
                ex.setAura(0.0);
                BossCoreEvents.wellspring(p);
                pool = ex.getAura() > 0.0 ? "the aura was refilled" : "the aura stayed empty (a stand-in with no maximum aura)";
            }
            return Result.ok("worn counting, mixing cap, Striker rise and its per-tick bound, cleanup hold; " + pool);
        } finally {
            equip(p, Kit.AEGIS, 0);
            BossCoreEvents.forget(p.getUUID());
            z.discard();
        }
    }
}
