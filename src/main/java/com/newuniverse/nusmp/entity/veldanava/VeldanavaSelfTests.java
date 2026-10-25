package com.newuniverse.nusmp.entity.veldanava;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The CI server smoke test of Veldanava (prefix {@code veldanava} in SMOKE_EXPECT): {@code veldanava.rules} (the move table, the choice, the damage and loot numbers, the
 * caps), {@code veldanava.assets} (the three models, their animations, their textures and the lang keys are in the jar), {@code veldanava.boss} (the entity spawns on the
 * real server with the dragon's hitbox, the saved form brings back the right hitbox, the hard-control immunity, the arena rule) and {@code veldanava.orb} (the Will of the
 * Creator gives one boon once, drops the loot for the fighter, and goes away when everybody has taken their share). They run on the throw-away world and clean up after
 * themselves.
 */
public final class VeldanavaSelfTests {
    private VeldanavaSelfTests() {}

    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("veldanava.rules", VeldanavaSelfTests::rules);
        SelfTests.register("veldanava.assets", VeldanavaSelfTests::assets);
        SelfTests.register("veldanava.boss", VeldanavaSelfTests::boss);
        SelfTests.register("veldanava.orb", VeldanavaSelfTests::orb);
    }

    // ---------------------------------------------------------------- rules (pure)
    private static Result rules(MinecraftServer server) {
        if (VeldanavaRules.MOVES != 11) return Result.fail("expected 11 moves, found " + VeldanavaRules.MOVES);
        for (int f = 0; f < VeldanavaRules.FORMS; f++) {
            List<VeldanavaRules.Spec> list = VeldanavaRules.movesOf(f);
            if (list.size() < 5) return Result.fail("body " + f + " has only " + list.size() + " moves");
            for (VeldanavaRules.Spec s : list) {
                if (VeldanavaRules.cooldown(s, f) < 20) return Result.fail(s.name() + " cools down in under a second");
                if (s.length() > 400) return Result.fail(s.name() + " is longer than 20 seconds");
            }
        }
        long[] ready = new long[VeldanavaRules.MOVES];
        for (int f = 0; f < VeldanavaRules.FORMS; f++) {
            for (int roll = 0; roll < 200; roll++) {
                final int r = roll;
                int id = VeldanavaRules.choose(f, 5.0, ready, 1000L, total -> r % total);
                if (id >= 0 && !VeldanavaRules.spec(id).has(f)) return Result.fail("body " + f + " chose a move it does not have: " + VeldanavaRules.spec(id).name());
            }
        }
        if (VeldanavaRules.choose(VeldanavaRules.DRAGON, 5.0, new long[]{9999, 9999, 9999, 9999, 9999, 9999, 9999, 9999, 9999, 9999, 9999}, 1000L, t -> 0) != -1) return Result.fail("a move on cooldown was chosen");
        if (VeldanavaRules.marks(0) != 0 || VeldanavaRules.marks(100) > VeldanavaRules.MAX_MARKS) return Result.fail("the Starfall marks are not capped");
        if (VeldanavaRules.fighters(100) != VeldanavaRules.MAX_FIGHTERS) return Result.fail("the fighter cap is not applied");
        if (VeldanavaRules.damage(1.0E9, 0.6f, 5f, 100f, 1f) > 100.0001f) return Result.fail("the damage cap is not applied");
        if (VeldanavaRules.damage(1.0, 0.6f, 5f, 100f, 1f) < 5f) return Result.fail("the damage floor is not applied");
        if (!VeldanavaRules.isFatal(5.0, 0.0, 5.0) || VeldanavaRules.isFatal(5.0, 2.0, 5.0)) return Result.fail("the fatal-blow test is wrong");
        if (VeldanavaRules.pullStep(2.0, 38.0) != 0.0 || VeldanavaRules.pullStep(50.0, 38.0) != 0.0 || VeldanavaRules.pullStep(10.0, 38.0) <= 0.0) return Result.fail("the pull range is wrong");
        if (!VeldanavaRules.inBeam(0, 10, 0, 56, 3.5) || VeldanavaRules.inBeam(0, -10, 0, 56, 3.5) || VeldanavaRules.inBeam(8, 10, 0, 56, 3.5)) return Result.fail("the beam geometry is wrong");
        if (!VeldanavaRules.limiterDue(VeldanavaRules.HUMANOID, 300f, 4000f) || VeldanavaRules.limiterDue(VeldanavaRules.DRAGON, 300f, 4000f)) return Result.fail("the Limiter test is wrong");
        int[] g = VeldanavaRules.guaranteed();
        if (g[0] != 1 || g[1] != 3 || g[2] != 1) return Result.fail("the guaranteed loot is not 1 core, 3 fragments, 1 essence");
        if (VeldanavaRules.orbTicks(300) != 6000) return Result.fail("the orb does not wait 5 minutes by default");
        for (int c = 1; c <= VeldanavaBoons.COUNT; c++) if (VeldanavaBoons.boon(c).changes().isEmpty()) return Result.fail("boon " + c + " changes nothing");
        return Result.ok("11 moves, 3 bodies, caps and numbers hold");
    }

    // ---------------------------------------------------------------- assets
    private static Result assets(MinecraftServer server) throws Exception {
        for (int f = 0; f < VeldanavaRules.FORMS; f++) {
            String g = VeldanavaRules.geo(f);
            for (String path : new String[]{"geo/entity/veldanava_" + g + ".geo.json", "animations/entity/veldanava_" + g + ".animation.json",
                    "textures/entity/veldanava_" + g + ".png", "textures/entity/veldanava_" + g + "_glow.png"}) {
                if (VeldanavaSelfTests.class.getResource("/assets/nusmp/" + path) == null) return Result.fail("missing " + path);
            }
        }
        JsonObject lang;
        try (InputStream in = VeldanavaSelfTests.class.getResourceAsStream("/assets/nusmp/lang/en_us.json")) {
            if (in == null) return Result.fail("no en_us.json");
            lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        Set<String> need = new HashSet<>(List.of("entity.nusmp.veldanava", "entity.nusmp.will_of_the_creator", "item.nusmp.veldanava_spawn_egg", "item.nusmp.star_king_dragon_core",
                "item.nusmp.genesis_fragment", "item.nusmp.creators_essence", "item.nusmp.justice_king_michael_fragment", "item.nusmp.covenant_king_uriel_fragment",
                "item.nusmp.nihility_fragment", "item.nusmp.veldanavas_divine_core"));
        for (String k : need) if (!lang.has(k)) return Result.fail("missing lang key " + k);
        return Result.ok("3 models x (geo, animation, texture, glow) and " + need.size() + " lang keys are in the jar");
    }

    // ---------------------------------------------------------------- the entity on a real server
    private static Result boss(MinecraftServer server) {
        ServerLevel sl = server.overworld();
        for (VeldanavaBoss old : sl.getEntities(VeldanavaEntities.VELDANAVA.get(), e -> true)) old.discard();
        BlockPos at = sl.getSharedSpawnPos();
        VeldanavaBoss b = VeldanavaBoss.spawnAt(sl, Vec3.atBottomCenterOf(at.above(2)), 0f);
        if (b == null) return Result.fail("the boss could not be spawned (config off?)");
        try {
            if (b.form() != VeldanavaRules.DRAGON) return Result.fail("he does not start as the dragon");
            if (Math.abs(b.getBbWidth() - VeldanavaRules.width(VeldanavaRules.DRAGON)) > 0.01f || Math.abs(b.getBbHeight() - VeldanavaRules.height(VeldanavaRules.DRAGON)) > 0.01f)
                return Result.fail("the dragon's hitbox is " + b.getBbWidth() + "x" + b.getBbHeight());
            if (VeldanavaBoss.spawnAt(sl, Vec3.atBottomCenterOf(at.above(2)), 0f) != null) return Result.fail("a second Veldanava was allowed in the same world");
            double hp = b.getAttributeValue(Attributes.MAX_HEALTH);
            if (Math.abs(hp - VeldanavaConfig.bossHealth()) > 0.5) return Result.fail("max health " + hp + " is not the configured " + VeldanavaConfig.bossHealth());
            for (int form = VeldanavaRules.HUMANOID; form <= VeldanavaRules.AVATAR; form++) {                 // the saved body brings back its hitbox
                CompoundTag t = new CompoundTag();
                b.saveWithoutId(t);
                t.putInt("VForm", form);
                b.load(t);
                if (b.form() != form) return Result.fail("the saved body " + form + " did not load");
                if (Math.abs(b.getBbHeight() - VeldanavaRules.height(form)) > 0.01f) return Result.fail("body " + form + " has a hitbox of height " + b.getBbHeight());
            }
            var slow = new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 20, 1);
            if (!b.canBeAffected(slow)) return Result.fail("an ordinary slowness should still land on him");
            return Result.ok("spawned, one per world, hitbox and saved bodies hold, max health " + (int) hp);
        } finally {
            b.discard();
        }
    }

    // ---------------------------------------------------------------- the Will of the Creator
    private static Result orb(MinecraftServer server) {
        ServerLevel sl = server.overworld();
        BlockPos at = sl.getSharedSpawnPos().above(3);
        FakePlayer p = SelfTests.standIn(sl, "veldanava_orb", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        p.getPersistentData().remove(VeldanavaBoons.TAG_RECEIVED);
        p.getPersistentData().remove(VeldanavaBoons.TAG_CHOICE);
        WillOfTheCreator orb = WillOfTheCreator.spawn(sl, Vec3.atBottomCenterOf(at), Set.of(p.getUUID()));
        if (orb == null) return Result.fail("the orb could not be raised");
        AABB around = new AABB(at).inflate(8);
        try {
            if (!orb.isFighter(p.getUUID())) return Result.fail("the fighter is not on the orb's list");
            if (!orb.claim(p, 0).startsWith("Choose")) return Result.fail("a player without a boon was allowed the loot-only choice");
            if (!orb.claim(p, 9).startsWith("That is not")) return Result.fail("an invalid boon number was accepted");
            String ok = orb.claim(p, 2);
            if (!orb.hasClaimed(p.getUUID()) || !VeldanavaBoons.received(p)) return Result.fail("the boon was not granted: " + ok);
            if (p.getPersistentData().getInt(VeldanavaBoons.TAG_CHOICE) != 2) return Result.fail("the choice was not remembered");
            if (!orb.claim(p, 3).startsWith("You have already")) return Result.fail("a second claim was allowed");
            if (VeldanavaBoons.grant(p, 1)) return Result.fail("a second boon was granted to the same player");
            List<ItemEntity> drops = sl.getEntitiesOfClass(ItemEntity.class, around);
            int core = 0, frag = 0, essence = 0;
            for (ItemEntity e : drops) {
                ItemStack s = e.getItem();
                if (s.is(VeldanavaEntities.STAR_KING_DRAGON_CORE.get())) core += s.getCount();
                if (s.is(VeldanavaEntities.GENESIS_FRAGMENT.get())) frag += s.getCount();
                if (s.is(VeldanavaEntities.CREATORS_ESSENCE.get())) essence += s.getCount();
            }
            if (core < 1 || frag < 3 || essence < 1) return Result.fail("the guaranteed loot is incomplete: core " + core + ", fragments " + frag + ", essence " + essence);
            if (!orb.isRemoved()) return Result.fail("the orb stayed after everybody took their share");
            return Result.ok("one boon, once; loot " + core + "/" + frag + "/" + essence + "; the orb dissolved");
        } finally {
            for (ItemEntity e : sl.getEntitiesOfClass(ItemEntity.class, around)) e.discard();
            if (!orb.isRemoved()) orb.discard();
            VeldanavaBoons.revoke(p);
        }
    }
}
