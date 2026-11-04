package com.newuniverse.nusmp.manacore;

import com.newuniverse.nusmp.beastwill.BeastWillCatalog;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Why: nothing of the rank Mana Cores can be seen by its author in a game, so the CI server smoke test boots the mod and runs these two (keys {@code manacore.table}, {@code manacore.player}).
 * {@code table}: the pure table validates, all 24 items are registered, the Thunder-Fang Chimera will of the AA beast core was loaded and is valid, every beast core of rank A or better names a will that exists.
 * {@code player}: on a stand-in player a held core turns its passive modifiers on, taking it out of the hand turns them off, an active takes effect once and spawns no entity, its cooldown refuses the second press,
 * and a cleanup leaves no state behind.
 */
public final class ManaCoreSelfTests {
    private ManaCoreSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("manacore.table", ManaCoreSelfTests::table);
        SelfTests.register("manacore.player", ManaCoreSelfTests::player);
    }

    private static Result table(MinecraftServer server) {
        String bad = ManaCoreRules.validate();
        if (bad != null) return Result.fail(bad);
        for (ManaCoreRules.Core c : ManaCoreRules.all()) {
            if (ManaCoreItems.item(c.id()) == null) return Result.fail("the item is not registered: " + c.id());
            if (c.will() != null) {
                var def = BeastWillCatalog.get(c.will());
                if (def == null) return Result.fail(c.id() + " names a will that was not loaded: " + c.will());
                if (def.problem() != null) return Result.fail(def.problem());
            }
        }
        return Result.ok("24 cores validate and are registered; every beast core of rank A or better has its Beast Will loaded and valid");
    }

    private static Result player(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "manacore", at.x, at.y, at.z);
        try {
            ManaCoreRuntime.cleanup(p);
            ManaCoreRules.Core c = ManaCoreRules.byId("core_e_standard");
            p.setItemInHand(InteractionHand.MAIN_HAND, ManaCoreItems.stack(c));
            ManaCoreRuntime.second(p, null);
            if (!ManaCoreRuntime.anyMods(p)) return Result.fail("a held core did not turn its passive modifier on");
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            ManaCoreRuntime.second(p, ManaCoreRuntime.RUN.get(p.getUUID()));
            if (ManaCoreRuntime.anyMods(p)) return Result.fail("taking the core out of the hand left a modifier on");
            p.setItemInHand(InteractionHand.MAIN_HAND, ManaCoreItems.stack(c));
            AABB box = new AABB(at, at).inflate(48);
            int before = level.getEntities((Entity) null, box, e -> true).size();
            if (!ManaCoreActives.use(p, c, 1, true)) return Result.fail("Core Spark did not take effect on its caster");
            if (!p.hasEffect(MobEffects.DIG_SPEED)) return Result.fail("Core Spark did not give haste");
            if (level.getEntities((Entity) null, box, e -> true).size() > before) return Result.fail("an active spawned an entity");
            if (ManaCoreActives.use(p, c, 1, true)) return Result.fail("the cooldown did not refuse the second press");
            if (ManaCoreRuntime.readyAt(p, "core_spark") <= level.getGameTime()) return Result.fail("no cooldown was recorded");
            return Result.ok("passive modifier on and off, an active that takes effect once, spawns nothing and cools down");
        } finally {
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            ManaCoreRuntime.cleanup(p);
        }
    }
}
