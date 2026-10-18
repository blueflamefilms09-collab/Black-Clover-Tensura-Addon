package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Why: the depth rule cannot be played by its author, so the CI server smoke test drives the same method the join event calls on REAL mobs of the live registries (made but never added to the
 * world, so nothing ticks and nothing is left behind): a zombie of floor +300 and one of floor -300 get the same, clearly higher health, armour and a mind with its goal installed; applying
 * twice changes nothing; floor +1 changes nothing; a pet and a non-hostile animal are left alone. Registered with {@link SelfTests} as {@code enc.depth}.
 */
final class DepthSelfTests {
    private DepthSelfTests() {}

    static void register() {
        SelfTests.register("enc.depth", DepthSelfTests::depth);
        SelfTests.register("enc.depth_drops", DepthSelfTests::drops);
    }

    private static SelfTests.Result depth(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Zombie plain = EntityType.ZOMBIE.create(level), up = EntityType.ZOMBIE.create(level), down = EntityType.ZOMBIE.create(level), shallow = EntityType.ZOMBIE.create(level);
        Wolf wolf = EntityType.WOLF.create(level);
        if (plain == null || up == null || down == null || shallow == null || wolf == null) return SelfTests.Result.fail("could not make the test mobs");
        double base = plain.getMaxHealth(), armor0 = plain.getAttributeValue(Attributes.ARMOR);
        DepthMobs.apply(up, 300);
        DepthMobs.apply(down, -300);
        DepthMobs.apply(shallow, 1);
        if (up.getMaxHealth() < base * 2.0f) return SelfTests.Result.fail("floor +300 health " + up.getMaxHealth() + " is not at least double the base " + base);
        if (Math.abs(up.getMaxHealth() - down.getMaxHealth()) > 0.01f) return SelfTests.Result.fail("+300 and -300 differ: " + up.getMaxHealth() + " against " + down.getMaxHealth());
        if (up.getAttributeValue(Attributes.ARMOR) <= armor0) return SelfTests.Result.fail("no armour bonus at +300");
        if (up.getHealth() < up.getMaxHealth() - 0.01f) return SelfTests.Result.fail("the health was not refilled to the new maximum");
        if (Math.abs(shallow.getMaxHealth() - base) > 0.01f) return SelfTests.Result.fail("floor +1 changed the health: " + shallow.getMaxHealth());
        float once = up.getMaxHealth();
        DepthMobs.apply(up, 300);
        if (Math.abs(up.getMaxHealth() - once) > 0.01f) return SelfTests.Result.fail("applying twice stacked: " + once + " then " + up.getMaxHealth());
        if (up.getPersistentData().getInt(DepthMobs.FLOOR_KEY) != 300 || down.getPersistentData().getInt(DepthMobs.FLOOR_KEY) != -300) return SelfTests.Result.fail("the born floor was not saved on the mob");
        int mind = up.getPersistentData().getInt(DepthMobs.MIND_KEY);
        if (mind != DepthRules.mind(300)) return SelfTests.Result.fail("mind " + mind + " is not the rule's " + DepthRules.mind(300));
        if (mind > 0 && !GoalAccess.has(up, DepthMind.class)) return SelfTests.Result.fail("a mob of mind " + mind + " has no mind goal (the goal list could not be read?)");
        if (shallow.getPersistentData().getInt(DepthMobs.MIND_KEY) != 0 && !GoalAccess.has(shallow, DepthMind.class)) return SelfTests.Result.fail("mind without goal at +1");
        if (!DepthMobs.eligible(plain)) return SelfTests.Result.fail("a zombie is not eligible");
        if (DepthMobs.eligible((Mob) wolf)) return SelfTests.Result.fail("a plain wolf is eligible");
        wolf.addTag(com.newuniverse.nusmp.labyrinth.mega.MegaDungeon.DM_TAG);                       // Tensura's monsters are not Enemy: the Dungeon Master's stamp is what makes them the dungeon's
        if (!DepthMobs.eligible((Mob) wolf)) return SelfTests.Result.fail("a Dungeon Master wolf is not eligible");
        return SelfTests.Result.ok("+300 and -300: " + String.format("%.0f", once) + " health from " + String.format("%.0f", base) + ", mind " + mind + "; +1 unchanged; no stacking");
    }

    /** The loot-table half of the depth drops, against the live registries: a zombie rolled many times leaves stacks, never more than the cap, and zero rolls leave nothing. */
    private static SelfTests.Result drops(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Zombie z = EntityType.ZOMBIE.create(level);
        if (z == null) return SelfTests.Result.fail("could not make a zombie");
        net.minecraft.server.level.ServerPlayer killer = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        net.minecraft.world.damagesource.DamageSource src = level.damageSources().playerAttack(killer);
        if (!DepthDrops.rollExtra(level, z, src, killer, 0, 0f).isEmpty()) return SelfTests.Result.fail("zero rolls left something");
        java.util.List<net.minecraft.world.item.ItemStack> many = DepthDrops.rollExtra(level, z, src, killer, 200, 3f);
        if (many.isEmpty()) return SelfTests.Result.fail("200 rolls of the zombie's loot table left nothing (does the loot table resolve?)");
        if (many.size() > DepthDropRules.MAX_EXTRA_STACKS) return SelfTests.Result.fail("the stack cap was passed: " + many.size());
        DepthDropRules.Params p = DepthDropRules.DEFAULTS;
        if (DepthDropRules.extraRolls(1, p, 0.0) != 0 || DepthDropRules.bonusRolls(1000, p) <= DepthDropRules.bonusRolls(10, p)) return SelfTests.Result.fail("the rolls curve is wrong");
        return SelfTests.Result.ok(many.size() + " extra stacks from 200 rolls (cap " + DepthDropRules.MAX_EXTRA_STACKS + "); floor 1000 gives " + String.format("%.2f", DepthDropRules.bonusRolls(1000, p)) + " expected extra rolls");
    }
}
