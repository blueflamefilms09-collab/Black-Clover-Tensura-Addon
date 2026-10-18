package com.newuniverse.nusmp.skilltree.ft.fire;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonSelfTests;
import com.newuniverse.nusmp.skilltree.state.IPlayerSkillTree;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;

/**
 * Why: the Fire Dragon Slayer route has to prove itself in the real game on the CI server, because its authors cannot play it: this registers the smoke-test key
 * {@code tree.ft.fire_dragon_slayer} (the prefix {@code tree} is the one the smoke script expects of the skill-tree system) with the shared harness of
 * {@link DragonSelfTests} and adds the one question only this route can ask: do its two passives answer? It drives {@link FireDragonPassives#eat} and
 * {@link FireDragonPassives#scorch} directly with the stand-in player, because the game makes a fake player invulnerable, so the real damage event can never reach it in a test.
 * Registered from {@code FireDragonSkills} at mod construction, like every other system's self-tests.
 */
public final class FireDragonSelfTests {
    private FireDragonSelfTests() {}

    private static final DragonSelfTests.Spec SPEC = new DragonSelfTests.Spec(FireDragonNumbers.ROUTE, "st_ft_fire", FireDragonNumbers.TABLES, FireDragonSelfTests::probe);

    /** Registers {@code tree.ft.fire_dragon_slayer}. */
    public static void register() { SelfTests.register("tree.ft.fire_dragon_slayer", server -> DragonSelfTests.run(server, SPEC)); }

    private static void probe(FakePlayer fp, ServerLevel level, Zombie dummy, SkillRoute route, IPlayerSkillTree tree, List<String> problems) {
        String r = route.id();
        DamageSource burn = fp.damageSources().onFire();

        tree.setNodeLevel(r, FireDragonNumbers.FLAME_EATER, 1);
        if (!FireDragonPassives.eat(fp, burn, 4.0f)) problems.add("Eating Flame: an enemy's fire was not eaten with the node bought");
        if (FireDragonPassives.eat(fp, new DamageSource(fp.damageSources().inFire().typeHolder(), fp), 4.0f)) problems.add("Eating Flame: the player's own flames were eaten");
        if (FireDragonPassives.eat(fp, fp.damageSources().generic(), 4.0f)) problems.add("Eating Flame: plain damage was eaten");
        if (FireDragonPassives.eat(fp, fp.damageSources().lava(), 4.0f)) problems.add("Eating Flame: lava was eaten at level 1");
        tree.setNodeLevel(r, FireDragonNumbers.FLAME_EATER, route.node(FireDragonNumbers.FLAME_EATER).maxLevel());
        if (!FireDragonPassives.eat(fp, fp.damageSources().lava(), 4.0f)) problems.add("Eating Flame: lava was not eaten at the top level");
        tree.setNodeLevel(r, FireDragonNumbers.FLAME_EATER, 0);
        if (FireDragonPassives.eat(fp, burn, 4.0f)) problems.add("Eating Flame: works without the node");

        tree.setNodeLevel(r, FireDragonNumbers.BLAZING_SCALES, 1);
        dummy.clearFire();
        FireDragonPassives.scorch(fp, fp.damageSources().mobAttack(dummy));
        if (!dummy.isOnFire()) problems.add("Blazing Scales: the attacker was not set alight");
        tree.setNodeLevel(r, FireDragonNumbers.BLAZING_SCALES, 0);
        dummy.clearFire();
        dummy.getPersistentData().remove("nusmp_ft_scorch_until");
        FireDragonPassives.scorch(fp, fp.damageSources().mobAttack(dummy));
        if (dummy.isOnFire()) problems.add("Blazing Scales: works without the node");
    }
}
