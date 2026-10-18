package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.elite.api.EliteHooks;
import com.newuniverse.nusmp.elite.skills.evolution.EvolutionEngine;
import com.newuniverse.nusmp.elite.skills.evolution.EvolutionRuntime;
import com.newuniverse.nusmp.elite.skills.evolution.EvolutionTable;
import com.newuniverse.nusmp.elite.skills.evolution.PlayerEvolutionContext;
import com.newuniverse.nusmp.elite.skills.evolution.Triggers;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing about the 21 elite skills has been played by the author, so each system registers a SELF-TEST (key prefix {@code elite.skills.}) that the CI server smoke test runs in the real
 * game with a {@link FakePlayer} standing in for a person: every skill registered under its id, the config readable, the shipped evolution data loaded and consistent with the registry, the twelve
 * triggers present, the reset ledger counting and feeding {@code EliteHooks.resetCount}, a whole evolution (requirements unmet, then met, an offering paid, the new skill learned), Gaia's
 * domain raising and its block guard refusing a stranger, Ouroboros' memory and rewind, Iron Resolve's stacking, and two real powers (Mechanical Hands' Mend and Telekinesis) run through the skill's own
 * code. Same format as the other self-tests of the mod: {@code /multiverse mega selftest elite.skills.<name>}. Tests change a throw-away world only and clean up what they learn.
 */
public final class EliteSkillsSelfTests {
    private EliteSkillsSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("elite.skills.registry", EliteSkillsSelfTests::registry);
        SelfTests.register("elite.skills.config", EliteSkillsSelfTests::config);
        SelfTests.register("elite.skills.evolution_data", EliteSkillsSelfTests::evolutionData);
        SelfTests.register("elite.skills.triggers", EliteSkillsSelfTests::triggers);
        SelfTests.register("elite.skills.reset_ledger", EliteSkillsSelfTests::resetLedger);
        SelfTests.register("elite.skills.evolution_flow", EliteSkillsSelfTests::evolutionFlow);
        SelfTests.register("elite.skills.gaia_guard", EliteSkillsSelfTests::gaiaGuard);
        SelfTests.register("elite.skills.ouroboros", EliteSkillsSelfTests::ouroboros);
        SelfTests.register("elite.skills.iron_resolve", EliteSkillsSelfTests::ironResolve);
        SelfTests.register("elite.skills.mechanical_hands", EliteSkillsSelfTests::mechanicalHands);
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    private static ServerLevel overworld(MinecraftServer server) { return server.overworld(); }

    private static FakePlayer fake(MinecraftServer server, String name) {
        ServerLevel level = overworld(server);
        return SelfTests.player(level, "elite_skills_" + name, level.getSharedSpawnPos().getX() + 0.5, level.getSharedSpawnPos().getY() + 1.0, level.getSharedSpawnPos().getZ() + 0.5);
    }

    private static ManasSkillInstance learn(FakePlayer p, ManasSkill skill) {
        var skills = SkillAPI.getSkillsFrom(p);
        skills.forgetSkill(skill);
        skills.learnSkill(skill);
        return skills.getSkill(skill).orElse(null);
    }

    // ---------------------------------------------------------------- registry
    private static Result registry(MinecraftServer server) {
        if (EliteSkills.ALL.size() != 21) return Result.fail("expected 21 elite skills, found " + EliteSkills.ALL.size());
        List<String> missing = new ArrayList<>();
        for (var holder : EliteSkills.ALL) {
            ResourceLocation rl = holder.getId();
            ManasSkill s = SkillAPI.getSkillRegistry().get(rl);
            if (s == null || s != holder.get()) missing.add(rl.toString());
        }
        if (!missing.isEmpty()) return Result.fail("not in the skill registry: " + missing);
        for (String old : List.of("hephaestus", "summoner", "hawk_eye", "limitless")) {
            if (SkillAPI.getSkillRegistry().get(id(old)) == null) return Result.fail("an older skill was lost: nusmp:" + old);
        }
        if (EliteSkills.HEPHAESTUS_ULTIMATE.get() == SkillAPI.getSkillRegistry().get(id("hephaestus"))) return Result.fail("the Ultimate Hephaestus replaced the DanMachi Unique");
        return Result.ok("21 elite skills registered next to the older ones (Hephaestus Unique kept)");
    }

    // ---------------------------------------------------------------- config
    private static Result config(MinecraftServer server) {
        double d = EliteSkillsConfig.damageScale(), c = EliteSkillsConfig.costScale(), cd = EliteSkillsConfig.cooldownScale();
        if (d < 0 || c < 0 || cd < 0) return Result.fail("a negative scale");
        return Result.ok("enabled=" + EliteSkillsConfig.enabled() + " damage x" + d + " cost x" + c + " cooldown x" + cd + " gaia veins/day " + EliteSkillsConfig.get(EliteSkillsConfig.GAIA_DAILY_VEINS));
    }

    // ---------------------------------------------------------------- evolution data
    private static Result evolutionData(MinecraftServer server) {
        EvolutionTable t = EvolutionRuntime.table();
        if (t.size() < 7) return Result.fail("only " + t.size() + " evolution paths loaded (expected the 7 shipped ones): did the reload listener run? errors " + t.errors());
        if (!t.errors().isEmpty()) return Result.fail("the data has errors: " + t.errors());
        for (EvolutionTable.Path p : t.paths()) {
            ResourceLocation from = ResourceLocation.tryParse(p.from()), to = ResourceLocation.tryParse(p.to());
            if (from == null || to == null || SkillAPI.getSkillRegistry().get(from) == null) return Result.fail("path starts at an unknown skill: " + p.from());
            if (SkillAPI.getSkillRegistry().get(to) == null) return Result.fail("path ends at an unknown skill: " + p.to());
        }
        return Result.ok(t.size() + " paths loaded, every skill id exists");
    }

    private static Result triggers(MinecraftServer server) {
        if (Triggers.count() != 12) return Result.fail("expected 12 triggers, found " + Triggers.count());
        return Result.ok("12 triggers: " + Triggers.ids());
    }

    // ---------------------------------------------------------------- reset ledger
    private static Result resetLedger(MinecraftServer server) {
        FakePlayer p = fake(server, "reset");
        ResetLedger.set(p, 0);
        if (EliteHooks.resetCount.applyAsInt(p.getUUID()) != 0) return Result.fail("EliteHooks.resetCount is not the ledger (or the count did not clear)");
        ResetLedger.record(p, "selftest");
        int two = ResetLedger.record(p, "selftest");
        int viaHook = EliteHooks.resetCount.applyAsInt(p.getUUID());
        ResetLedger.set(p, 0);
        if (two != 2 || viaHook != 2) return Result.fail("two resets counted as " + two + ", the hook says " + viaHook);
        return Result.ok("two resets counted and the hook agrees");
    }

    // ---------------------------------------------------------------- a whole evolution
    private static Result evolutionFlow(MinecraftServer server) {
        FakePlayer p = fake(server, "evolve");
        ManasSkill from = EliteSkills.CROCODILE_SKIN.get(), to = EliteSkills.BASILISK_SKIN.get();
        var skills = SkillAPI.getSkillsFrom(p);
        skills.forgetSkill(to);
        ManasSkillInstance inst = learn(p, from);
        if (inst == null) return Result.fail("the fake player could not learn nusmp:crocodile_skin");
        try {
            String json = "{\"nusmp:crocodile_skin\":{\"nusmp:basilisk_skin\":[{\"trigger\":\"ability_mastered\"},{\"trigger\":\"reset_count\",\"params\":{\"count\":1}},"
                    + "{\"trigger\":\"item_consume\",\"params\":{\"item\":\"minecraft:stick\",\"count\":2}}]}}";
            EvolutionTable t = new EvolutionTable().add(json, "selftest");
            EvolutionTable.Path path = t.get("nusmp:crocodile_skin", "nusmp:basilisk_skin");
            if (path == null) return Result.fail("the test path did not parse: " + t.errors());
            PlayerEvolutionContext ctx = new PlayerEvolutionContext(p);
            ResetLedger.set(p, 0);
            p.getInventory().clearContent();
            if (EvolutionEngine.check(path, ctx).ok()) return Result.fail("the path passed with nothing met");
            inst.setMastery(inst.getMaxMastery());
            ResetLedger.set(p, 1);
            p.getInventory().add(new ItemStack(Items.STICK, 5));
            EvolutionEngine.Outcome o = EvolutionEngine.check(path, ctx);
            if (!o.ok()) return Result.fail("the path did not pass with everything met: " + o.unmet());
            if (!EvolutionRuntime.evolve(p, skills, inst, to, path, ctx)) return Result.fail("evolve() refused although everything was met");
            if (skills.getSkill(to).isEmpty()) return Result.fail("the player did not learn nusmp:basilisk_skin");
            int sticks = ctx.itemCount("minecraft:stick");
            if (sticks != 3) return Result.fail("the offering was not paid exactly: " + sticks + " sticks left of 5 (expected 3)");
            return Result.ok("unmet -> met -> offering of 2 paid -> Basilisk Skin learned");
        } finally {
            skills.forgetSkill(from);
            skills.forgetSkill(to);
            ResetLedger.set(p, 0);
            p.getInventory().clearContent();
        }
    }

    // ---------------------------------------------------------------- Gaia
    private static Result gaiaGuard(MinecraftServer server) {
        FakePlayer owner = fake(server, "gaia_owner"), stranger = fake(server, "gaia_stranger");
        ServerLevel level = overworld(server);
        GaiaZone.clear();
        try {
            GaiaZone z = GaiaWorld.raise(owner, false);
            if (GaiaZone.of(owner.getUUID()) != z) return Result.fail("the zone was not registered");
            BlockPos pos = owner.blockPosition().below();
            var state = level.getBlockState(pos);
            BlockEvent.BreakEvent theirs = new BlockEvent.BreakEvent(level, pos, state, stranger);
            GaiaBlockGuard.onBreak(theirs);
            boolean refusedStranger = theirs.isCanceled();
            BlockEvent.BreakEvent mine = new BlockEvent.BreakEvent(level, pos, state, owner);
            GaiaBlockGuard.onBreak(mine);
            boolean refusedOwner = mine.isCanceled();
            GaiaWorld.dismiss(owner, true);
            BlockEvent.BreakEvent after = new BlockEvent.BreakEvent(level, pos, state, stranger);
            GaiaBlockGuard.onBreak(after);
            if (!EliteSkillsConfig.enabled() || !EliteSkillsConfig.get(EliteSkillsConfig.GAIA_BLOCK_GUARD)) return Result.ok("block guard is switched off by the config: nothing to check");
            if (!refusedStranger) return Result.fail("a stranger could break a block inside the domain");
            if (refusedOwner) return Result.fail("the owner was refused inside their own domain");
            if (after.isCanceled()) return Result.fail("the guard still holds after the domain was dismissed");
            return Result.ok("domain raised; stranger refused, owner allowed, guard gone after dismissal");
        } finally {
            GaiaZone.clear();
        }
    }

    // ---------------------------------------------------------------- Ouroboros
    private static Result ouroboros(MinecraftServer server) {
        FakePlayer p = fake(server, "ouro");
        OuroborosSkill.forget(p.getUUID());
        OuroborosSkill.sample(p);
        if (OuroborosSkill.remembered(p.getUUID()) != 1) return Result.fail("the first moment was not remembered");
        p.setHealth(Math.max(1.0f, p.getMaxHealth() * 0.1f));
        boolean back = OuroborosSkill.rewind(p, false, false);
        if (!back) return Result.fail("rewind refused although a moment was remembered");
        if (OuroborosSkill.remembered(p.getUUID()) != 0) return Result.fail("the memory was not cleared after the rewind");
        OuroborosSkill.forget(p.getUUID());
        return Result.ok("one moment remembered, rewind done, memory cleared");
    }

    // ---------------------------------------------------------------- Iron Resolve
    private static Result ironResolve(MinecraftServer server) {
        FakePlayer p = fake(server, "iron");
        ManasSkillInstance inst = learn(p, EliteSkills.IRON_RESOLVE.get());
        if (inst == null) return Result.fail("the fake player could not learn nusmp:iron_resolve");
        try {
            var skill = EliteSkills.IRON_RESOLVE.get();
            var src = p.damageSources().generic();
            p.setHealth(p.getMaxHealth());
            Changeable<Float> healthy = Changeable.of(10f);
            skill.onTakenDamage(inst, p, src, healthy);
            if (healthy.get() != 10f) return Result.fail("a healthy wearer was already protected (" + healthy.get() + ")");
            p.setHealth(Math.max(1.0f, p.getMaxHealth() * 0.3f));
            float last = 10f;
            for (int i = 0; i < 4; i++) {
                Changeable<Float> hit = Changeable.of(10f);
                skill.onTakenDamage(inst, p, src, hit);
                if (hit.get() > last) return Result.fail("the protection got weaker at hit " + (i + 1));
                last = hit.get();
            }
            if (!(last < 10f)) return Result.fail("wounded hits stacked no protection");
            if (!IronResolveSkill.knockbackImmune(inst, p)) return Result.fail("no knock-back immunity at four stacks");
            return Result.ok("wounded hits stack: the 5th blow of 10 lands for " + last + ", knock-back immune");
        } finally {
            SkillAPI.getSkillsFrom(p).forgetSkill(EliteSkills.IRON_RESOLVE.get());
        }
    }

    // ---------------------------------------------------------------- Mechanical Hands
    private static Result mechanicalHands(MinecraftServer server) {
        FakePlayer p = fake(server, "hands");
        ManasSkillInstance inst = learn(p, EliteSkills.MECHANICAL_HANDS.get());
        if (inst == null) return Result.fail("the fake player could not learn nusmp:mechanical_hands");
        ServerLevel level = overworld(server);
        ItemEntity loot = null;
        try {
            MechanicalHandsSkill skill = EliteSkills.MECHANICAL_HANDS.get();
            ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
            pick.setDamageValue(100);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pick);
            if (!skill.use(inst, p, 2)) return Result.fail("Mend refused a damaged pickaxe");
            if (pick.getDamageValue() >= 100) return Result.fail("Mend repaired nothing");
            int mended = 100 - pick.getDamageValue();
            loot = new ItemEntity(level, p.getX() + 6, p.getY(), p.getZ(), new ItemStack(Items.STICK));
            level.addFreshEntity(loot);
            if (!skill.use(inst, p, 1)) return Result.fail("Telekinesis found no item 6 blocks away");
            if (loot.getDeltaMovement().lengthSqr() < 1.0e-4) return Result.fail("the item was not pulled");
            return Result.ok("Mend restored " + mended + " durability; Telekinesis pulled an item 6 blocks away");
        } finally {
            if (loot != null) loot.discard();
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            SkillAPI.getSkillsFrom(p).forgetSkill(EliteSkills.MECHANICAL_HANDS.get());
        }
    }
}
