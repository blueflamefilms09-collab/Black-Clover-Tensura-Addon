package com.newuniverse.nusmp.skilltree.ft.lightning;

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
 * Why: the Lightning Dragon Slayer route has to prove itself in the real game on the CI server, because its authors cannot play it: this registers the smoke-test key
 * {@code tree.ft.lightning_dragon_slayer} (the prefix {@code tree} is the one the smoke script expects of the skill-tree system) with the shared harness of
 * {@link DragonSelfTests} and adds the one question only this route can ask: do its two passives answer? It drives {@link LightningDragonPassives#eat} and
 * {@link LightningDragonPassives#evade} directly with the stand-in player, because the game makes a fake player invulnerable, so the real damage event can never reach it in a
 * test; the evade is driven with a forced dice roll so that both outcomes are proven. Registered from {@code LightningDragonSkills} at mod construction.
 */
public final class LightningDragonSelfTests {
    private LightningDragonSelfTests() {}

    private static final DragonSelfTests.Spec SPEC = new DragonSelfTests.Spec(LightningDragonNumbers.ROUTE, "st_ft_lightning", LightningDragonNumbers.TABLES, LightningDragonSelfTests::probe);

    /** Registers {@code tree.ft.lightning_dragon_slayer}. */
    public static void register() { SelfTests.register("tree.ft.lightning_dragon_slayer", server -> DragonSelfTests.run(server, SPEC)); }

    private static void probe(FakePlayer fp, ServerLevel level, Zombie dummy, SkillRoute route, IPlayerSkillTree tree, List<String> problems) {
        String r = route.id();
        DamageSource bolt = fp.damageSources().lightningBolt();

        tree.setNodeLevel(r, LightningDragonNumbers.THUNDER_EATER, 1);
        if (!LightningDragonPassives.eat(fp, bolt, 5.0f)) problems.add("Eating Lightning: lightning was not eaten with the node bought");
        if (LightningDragonPassives.eat(fp, new DamageSource(fp.damageSources().lightningBolt().typeHolder(), fp), 5.0f)) problems.add("Eating Lightning: the player's own lightning was eaten");
        if (LightningDragonPassives.eat(fp, fp.damageSources().generic(), 5.0f)) problems.add("Eating Lightning: plain damage was eaten");
        tree.setNodeLevel(r, LightningDragonNumbers.THUNDER_EATER, 0);
        if (LightningDragonPassives.eat(fp, bolt, 5.0f)) problems.add("Eating Lightning: works without the node");

        DamageSource blow = fp.damageSources().mobAttack(dummy);
        tree.setNodeLevel(r, LightningDragonNumbers.LIGHTNING_BODY, 1);
        if (LightningDragonPassives.evade(fp, blow, 99.9)) problems.add("Lightning Body: a roll of 99.9 avoided a blow");
        if (!LightningDragonPassives.evade(fp, blow, 0.0)) problems.add("Lightning Body: a roll of 0 did not avoid a blow");
        if (LightningDragonPassives.evade(fp, blow, 0.0)) problems.add("Lightning Body: avoided two blows within its interval");
        if (LightningDragonPassives.evade(fp, fp.damageSources().generic(), 0.0)) problems.add("Lightning Body: avoided damage that has no attacker");
        fp.getPersistentData().remove("nusmp_ft_evade_until");
        tree.setNodeLevel(r, LightningDragonNumbers.LIGHTNING_BODY, 0);
        if (LightningDragonPassives.evade(fp, blow, 0.0)) problems.add("Lightning Body: works without the node");
    }
}
