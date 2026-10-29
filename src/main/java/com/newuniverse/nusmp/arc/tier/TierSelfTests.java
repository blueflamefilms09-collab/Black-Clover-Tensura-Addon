package com.newuniverse.nusmp.arc.tier;

import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcConfig;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.ArcSpawner;
import com.newuniverse.nusmp.boss.BossRegistry;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Tier List bosses cannot be played by their author, so the CI server smoke test checks them in the real game ({@code /multiverse mega selftest}, keys
 * {@code tier.*}): every row is in the roster and the boss registry, has an entity type with attributes, obeys the house rules, builds its moves; and one of them is
 * really summoned, sized, named, dressed in its moves and removed again. Cost: one entity, made and discarded in the same tick.
 */
public final class TierSelfTests {
    private TierSelfTests() {}

    public static void register() {
        SelfTests.register("tier.roster", TierSelfTests::roster);
        SelfTests.register("tier.moves", TierSelfTests::moves);
        SelfTests.register("tier.spawn", TierSelfTests::spawn);
        SelfTests.register("tier.light.data", TierSelfTests::lightData);          // 3.36.0: Light Yagami (gag boss): row, data pack, sounds, setting
        SelfTests.register("tier.light.scene", TierSelfTests::lightScene);        // the scene with the setting off: no kill, cleanup, caps
        SelfTests.register("tier.light.cleanup", TierSelfTests::lightCleanup);    // logout / death / dimension change / the boss leaving the world
    }

    private static SelfTests.Result roster(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        for (TierRow row : TierBosses.all()) {
            ArcCharacter c = ArcRoster.byId(row.id());
            if (c == null) { bad.add(row.id() + " is not in the roster"); continue; }
            if (c.saga() != ArcSaga.TIER_LIST) bad.add(row.id() + " is not of the Tier List saga");
            if (!BossRegistry.known(row.id())) bad.add(row.id() + " is not in the boss registry");
            var type = ArcSpawner.typeOf(row.id());
            if (type == null) bad.add(row.id() + " has no entity type");
            else if (DefaultAttributes.getSupplier((net.minecraft.world.entity.EntityType) type) == null) bad.add(row.id() + " has no attributes");
            bad.addAll(TierRules.problems(row));
        }
        return bad.isEmpty() ? SelfTests.Result.ok(TierBosses.all().size() + " light bosses are consistent")
                : SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
    }

    private static SelfTests.Result moves(MinecraftServer server) {
        int total = 0;
        for (TierRow row : TierBosses.all()) {
            List<Move> built = TierMoves.build(row);
            if (built.size() != row.moves().size()) return SelfTests.Result.fail(row.id() + " builds " + built.size() + " of " + row.moves().size() + " moves");
            total += built.size();
        }
        return SelfTests.Result.ok(total + " moves build");
    }

    private static SelfTests.Result spawn(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return SelfTests.Result.ok("skipped: the spawn chunk is not loaded");
        TierRow row = TierBosses.all().get(0);
        ArcBoss boss = null;
        try {
            ArcSpawner.Result r = ArcSpawner.summon(level, row.character(), Vec3.atBottomCenterOf(at.above(2)).add(6, 0, 0), 0f, List.of(), true);
            if (r.outcome() != ArcSpawner.Outcome.SPAWNED) return SelfTests.Result.fail("summoning " + row.id() + " gave " + r.outcome());
            boss = r.boss();
            if (!(boss instanceof TierBoss tb)) return SelfTests.Result.fail(row.id() + " is not a TierBoss");
            int built = tb.moves().size();
            if (built != row.moves().size()) return SelfTests.Result.fail(row.id() + " has " + built + " moves");
            if (!row.character().name().equals(boss.bossName())) return SelfTests.Result.fail("the name is " + boss.bossName());
            if (boss.getMaxHealth() < row.letter().health() * 0.5) return SelfTests.Result.fail("max health " + boss.getMaxHealth());
            return SelfTests.Result.ok(row.id() + " summoned with " + built + " moves and " + (int) boss.getMaxHealth() + " health");
        } finally {
            if (boss != null) boss.discard();
        }
    }

    // ================================================================ 3.36.0: Light Yagami, the gag boss

    private static SelfTests.Result lightData(MinecraftServer server) {
        TierRow row = TierBosses.all().stream().filter(r -> r.id().equals(TierRosterGag.LIGHT)).findFirst().orElse(null);
        if (row == null) return SelfTests.Result.fail("Light Yagami is not in the tier roster");
        List<String> bad = new ArrayList<>(TierRules.problems(row));
        if (ArcRoster.byId(row.id()) == null) bad.add("not in the arc roster");
        var type = ArcSpawner.typeOf(row.id());
        if (type == null) bad.add("no entity type");
        else if (DefaultAttributes.getSupplier((net.minecraft.world.entity.EntityType) type) == null) bad.add("no attributes");
        var damage = server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
        var holder = damage.getHolder(LightDeathNote.TYPE);
        if (holder.isEmpty()) bad.add("damage type nusmp:death_note is missing");
        else {
            var h = holder.get();
            if (!h.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR) || !h.is(net.minecraft.tags.DamageTypeTags.BYPASSES_EFFECTS)) bad.add("the death note does not bypass armour and effects");
            if (h.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) bad.add("the death note must not bypass invulnerability (the Totem of Undying has to work)");
            if (!"nusmp.death_note".equals(h.value().msgId())) bad.add("message id " + h.value().msgId());
        }
        for (String id : new String[]{LightSounds.CHIP_LINE, LightSounds.JUST_AS_PLANNED, LightSounds.I_AM_JUSTICE}) {
            if (!net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("nusmp", id))) bad.add("sound event " + id + " is not registered");
        }
        if (!ArcConfig.lightDeathNoteLethal()) bad.add("lightDeathNoteLethal is not on by default");
        return bad.isEmpty() ? SelfTests.Result.ok("Light Yagami: row, damage type, three sounds and the setting are in place")
                : SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(5, bad.size()))));
    }

    /** Spawns Light, runs a whole scene at a stand-in player with the lethal setting forced OFF: nobody is hurt, everything is cleaned up, a second scene is refused while the player cools down. */
    private static SelfTests.Result lightScene(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return SelfTests.Result.ok("skipped: the spawn chunk is not loaded");
        TierRow row = TierBosses.all().stream().filter(r -> r.id().equals(TierRosterGag.LIGHT)).findFirst().orElseThrow();
        LightScenes.reset();
        LightYagamiBoss light = null;
        LightYagamiBoss.lethalOverride = Boolean.FALSE;
        try {
            ArcSpawner.Result r = ArcSpawner.summon(level, row.character(), Vec3.atBottomCenterOf(at.above(2)).add(6, 0, 0), 0f, List.of(), true);
            if (r.outcome() != ArcSpawner.Outcome.SPAWNED || !(r.boss() instanceof LightYagamiBoss l)) return SelfTests.Result.fail("summoning Light gave " + r.outcome() + " / " + r.boss());
            light = l;
            if (l.getMaxHealth() > 100) return SelfTests.Result.fail("Light has " + l.getMaxHealth() + " health, a gag boss has little");
            if (l.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) > 0.0) return SelfTests.Result.fail("Light resists knockback (knocking him away is the counterplay)");
            var fake = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("light-test".getBytes()), "LightTest"));
            fake.moveTo(Vec3.atBottomCenterOf(at.above(2)));
            float before = fake.getHealth();
            l.testTarget = fake;
            if (!l.startSceneFor(level, fake)) return SelfTests.Result.fail("the scene did not start");
            if (LightScenes.active() != 1 || LightScenes.ofTarget(fake.getUUID()) == null) return SelfTests.Result.fail("the registry does not list the scene");
            LightScene s = l.scene();
            int ticks = 0;
            while (s.running() && ticks < LightScene.TOTAL_TICKS + 10) {
                l.stepScene(level);
                ticks++;
            }
            if (s.phase() != LightScene.Phase.DONE) return SelfTests.Result.fail("the scene ended as " + s.phase() + " (" + s.cancelReason() + ") after " + ticks + " ticks");
            if (ticks != LightScene.TOTAL_TICKS) return SelfTests.Result.fail("the scene took " + ticks + " ticks, not " + LightScene.TOTAL_TICKS);
            if (!fake.isAlive() || fake.getHealth() < before) return SelfTests.Result.fail("the setting is off but the player was hurt");
            if (LightScenes.active() != 0 || l.scene() != null) return SelfTests.Result.fail("the scene was not cleaned up");
            if (l.startSceneFor(level, fake)) return SelfTests.Result.fail("a second scene started on the same player at once (cooldown ignored)");
            fake.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            if (LightDeathNote.eligible(fake)) return SelfTests.Result.fail("a creative player is eligible");
            fake.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            l.testTarget = null;
            return SelfTests.Result.ok("a whole scene ran in " + ticks + " ticks with the setting off: nobody hurt, registry clean, cooldown holds, creative skipped");
        } finally {
            LightYagamiBoss.lethalOverride = null;
            if (light != null) light.discard();
            LightScenes.reset();
        }
    }

    /** The scene is cancelled and forgotten when the player logs out, dies or changes dimension, when the boss leaves the world, and the cap holds. */
    private static SelfTests.Result lightCleanup(MinecraftServer server) {
        LightScenes.reset();
        try {
            java.util.UUID p = java.util.UUID.randomUUID();
            LightScene[] out = new LightScene[1];
            if (LightScenes.start(java.util.UUID.randomUUID(), p, 100, out) != LightScenes.Start.STARTED) return SelfTests.Result.fail("start refused");
            LightScenes.playerGone(p);                                                                                                  // logout, death and dimension change all call this
            if (out[0].phase() != LightScene.Phase.CANCELLED || LightScenes.active() != 0 || LightScenes.ofTarget(p) != null) return SelfTests.Result.fail("playerGone left the scene behind");
            java.util.UUID light = java.util.UUID.randomUUID();
            LightScenes.start(light, java.util.UUID.randomUUID(), 100, out);
            LightScene gone = LightScenes.lightGone(light, LightScene.Cancel.LIGHT_DIED);
            if (gone == null || gone.cancelReason() != LightScene.Cancel.LIGHT_DIED || LightScenes.active() != 0) return SelfTests.Result.fail("lightGone left the scene behind");
            for (int i = 0; i < LightScenes.MAX_ACTIVE + 3; i++) LightScenes.start(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), 100, null);
            if (LightScenes.active() != LightScenes.MAX_ACTIVE) return SelfTests.Result.fail(LightScenes.active() + " scenes run, the cap is " + LightScenes.MAX_ACTIVE);
            LightScenes.reset();
            if (LightScenes.active() != 0 || LightScenes.cooldowns() != 0) return SelfTests.Result.fail("reset left state behind");
            return SelfTests.Result.ok("logout / death / dimension change / boss gone cancel the scene; the cap of " + LightScenes.MAX_ACTIVE + " holds; reset is clean");
        } finally {
            LightScenes.reset();
        }
    }
}
