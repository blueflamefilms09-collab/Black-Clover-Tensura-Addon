package com.newuniverse.nusmp.shishou;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing of Shishou can be played by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs these (keys {@code relic.shishou},
 * {@code relic.shishou_claim}, {@code relic.shishou_skills}) with stand-in players on a dedicated server: the registrations and files exist; one Shishou only (a second spawn is refused), the dormant sword is claimed by
 * a stand-in wielder and nobody else, a thief is punished and torn from, the sword is thrown and recalled, a copy of the item dissolves; the Skill Eater table, Stomach's refusals, the clone cap and Magic Sense's
 * bounds hold; and the Black Flame effect lights. Server safe: no client class. Everything is cleaned up (the world is left with no Shishou).
 */
public final class ShishouSelfTests {
    private ShishouSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("relic.shishou", ShishouSelfTests::basics);
        SelfTests.register("relic.shishou_claim", ShishouSelfTests::claim);
        SelfTests.register("relic.shishou_skills", ShishouSelfTests::skills);
        SelfTests.register("relic.shishou_skill_grant", ShishouSelfTests::skillGrant);
        SelfTests.register("relic.shishou_feed", ShishouSelfTests::feed);
        SelfTests.register("relic.shishou_hunt", ShishouSelfTests::hunt);
    }

    private static boolean exists(String path) {
        try (InputStream in = ShishouSelfTests.class.getResourceAsStream(path)) {
            return in != null;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    // ================================================================ relic.shishou: registrations, files, rules
    private static Result basics(MinecraftServer server) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("nusmp", "shishou");
        if (!BuiltInRegistries.ITEM.containsKey(id)) return Result.fail("the item nusmp:shishou is not registered");
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return Result.fail("the entity nusmp:shishou is not registered");
        if (!BuiltInRegistries.MOB_EFFECT.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", "black_flame"))) return Result.fail("the Black Flame effect is not registered");
        for (String c : new String[]{"shishou_owner", "shishou_token", "shishou_level"})
            if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", c))) return Result.fail("component " + c + " is not registered");
        for (String f : new String[]{"/assets/nusmp/weapon_meshes/shishou.txt", "/assets/nusmp/textures/entity/weapon/shishou.png", "/assets/nusmp/textures/item/shishou.png",
                "/assets/nusmp/animations/entity/shishou_sword.animation.json", "/assets/nusmp/models/item/shishou.json", "/assets/nusmp/models/item/shishou_icon.json"})
            if (!exists(f)) return Result.fail("missing asset " + f);
        if (ShishouSkill.values().length != 31) return Result.fail("the skill table changed size");
        if (ShishouRules.THEFT_SLOW_AMP != 9 || ShishouRules.THEFT_TICKS != 200) return Result.fail("the anti-theft numbers moved");
        if (ShishouLevels.levelFor(ShishouLevels.totalFor(50)) != 50) return Result.fail("the XP curve does not invert");
        if (ShishouStealTable.size() > 64) return Result.fail("the steal table is unbounded");
        ShishouCap c = new ShishouCap();
        if (!c.spawnWild(java.util.UUID.randomUUID(), "x", 0, 0, 0) || c.spawnWild(java.util.UUID.randomUUID(), "x", 0, 0, 0)) return Result.fail("the cap rule is broken");
        for (ShishouVoiceLines.Group g : ShishouVoiceLines.Group.values()) if (g.count() < 1) return Result.fail("empty voice group " + g);
        return Result.ok("registrations, assets, tables and the cap rule hold; the shipped enabled flag is " + ShishouConfig.enabled());
    }

    // ================================================================ relic.shishou_claim: one sword, claim, theft, throw, recall, copies
    private static Result claim(MinecraftServer server) {
        if (!ShishouData.bound()) return Result.fail("the data is not bound to the server");
        ShishouData d = ShishouData.get();
        ShishouCap cap = d.cap();
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 3, 0);
        ServerPlayer a = SelfTests.standIn(level, "shishou_a", at.x, at.y, at.z), b = SelfTests.standIn(level, "shishou_b", at.x + 1, at.y, at.z);
        List<net.minecraft.world.entity.Entity> made = new ArrayList<>();
        try {
            if (!ShishouConfig.enabled()) return Result.ok("skipped: relic.shishouEnabled is false (nothing may spawn)");
            cap.reset();
            ShishouLive.clearAll();
            ShishouEntity wild = ShishouService.spawnWild(level, at);
            if (wild == null) return Result.fail("the wild sword did not spawn on an empty world");
            made.add(wild);
            if (ShishouService.spawnWild(level, at.add(2, 0, 0)) != null) return Result.fail("a second Shishou was allowed");
            if (!cap.validEntity(wild.getUUID())) return Result.fail("the record does not know the wild body");
            ShishouService.claim(a, wild);
            if (cap.form() != ShishouCap.Form.WILD) return Result.fail("a flying sword was claimed");
            ShishouService.embed(wild, level.getSharedSpawnPos().below());
            if (!wild.isDormant() || cap.form() != ShishouCap.Form.DORMANT) return Result.fail("the sword did not settle into its pedestal");
            ShishouService.claim(a, wild);
            if (cap.form() != ShishouCap.Form.HELD || !cap.isOwner(a.getUUID())) return Result.fail("the claim did not give the sword to the player");
            if (!wild.isRemoved()) return Result.fail("the dormant body stayed after the claim");
            ItemStack sword = ShishouService.heldStack(a);
            if (sword.isEmpty() || ShishouComponents.owner(sword) == null || !a.getUUID().equals(ShishouComponents.owner(sword))) return Result.fail("the item does not carry its wielder");
            // a copy of the item in the same tick dissolves
            ItemStack copy = sword.copy();
            long now = level.getGameTime();
            cap.beat(ShishouComponents.token(sword), System.identityHashCode(sword), now);
            if (cap.beat(ShishouComponents.token(copy), System.identityHashCode(copy), now) != ShishouCap.Beat.DUPLICATE) return Result.fail("a copy was not seen");
            // a thief
            ItemStack stolen = copy;
            ShishouService.thief(b, stolen, ShishouTheft.Verdict.PUNISH_FIRST, level);
            if (!b.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) || b.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() != 9 || !b.hasEffect(MobEffects.WITHER)) return Result.fail("the thief did not get Slowness X and Wither");
            ShishouService.thief(b, stolen, ShishouTheft.Verdict.CONFISCATE, level);
            if (!stolen.isEmpty()) return Result.fail("the sword was not torn out of the thief's hands");
            // the fake wielder is not in the player list, so the recovery put a dormant sword at the wielder's home or the spawn
            if (cap.form() != ShishouCap.Form.DORMANT || cap.entity() == null) return Result.fail("the recovery did not leave a dormant sword (" + cap.form() + ")");
            net.minecraft.world.entity.Entity body = level.getEntity(cap.entity());
            if (!(body instanceof ShishouEntity dormant) || !dormant.isDormant()) return Result.fail("the recovered body is not in the world");
            made.add(body);
            if (ShishouService.spawnWild(level, at) != null) return Result.fail("a second sword was allowed after a recovery");
            ShishouService.claim(b, dormant);
            if (cap.form() != ShishouCap.Form.DORMANT) return Result.fail("another player took the sword that is kept for its wielder");
            ShishouService.claim(a, dormant);
            if (cap.form() != ShishouCap.Form.HELD) return Result.fail("the wielder could not take the sword back");
            // throw and recall
            d.record().refill(level.getGameTime());
            if (!ShishouService.throwOut(a)) return Result.fail("the throw was refused");
            if (cap.form() != ShishouCap.Form.MINION || !(level.getEntity(cap.entity()) instanceof ShishouEntity minion) || !minion.isMinion()) return Result.fail("the thrown sword is not a minion");
            made.add(minion);
            if (ShishouService.throwOut(a)) return Result.fail("a second throw was allowed");
            if (!ShishouService.recall(a) || cap.form() != ShishouCap.Form.HELD) return Result.fail("the recall did not give the sword back");
            if (!minion.isRemoved()) return Result.fail("the minion stayed after the recall");
            // the reset leaves the world with none, and a ghost body dissolves on its own check
            cap.reset();
            if (cap.validEntity(minion.getUUID())) return Result.fail("a reset did not turn the bodies into ghosts");
            return Result.ok("one sword only, claim, copy, theft, tear-away, recovery, reserve, throw and recall hold");
        } finally {
            for (var e : made) if (!e.isRemoved()) {
                if (e instanceof ShishouEntity se) se.expectRemoval();
                e.discard();
            }
            for (ServerLevel l : server.getAllLevels()) if (cap.entity() != null && l.getEntity(cap.entity()) instanceof ShishouEntity se) {
                se.expectRemoval();
                se.discard();
            }
            cap.reset();
            d.touch();
            ShishouLive.clearAll();
        }
    }

    // ================================================================ relic.shishou_skill_grant: the Tensura Ultimate skill follows the sword
    private static Result skillGrant(MinecraftServer server) {
        if (!ShishouData.bound()) return Result.fail("the data is not bound to the server");
        ShishouData d = ShishouData.get();
        ShishouCap cap = d.cap();
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 3, 0);
        ServerPlayer a = SelfTests.standIn(level, "shishou_g1", at.x, at.y, at.z), b = SelfTests.standIn(level, "shishou_g2", at.x + 1, at.y, at.z);
        try {
            // the skill itself: registered, an Ultimate, seven modes with unique ids and names
            com.newuniverse.nusmp.skill.ShishouUltimateSkill s = com.newuniverse.nusmp.skill.NUSkills.SHISHOU.get();
            if (s.getType() != io.github.manasmods.tensura.ability.skill.Skill.SkillType.ULTIMATE) return Result.fail("the skill is not an Ultimate");
            var inst = s.createDefaultInstance();
            if (s.getModes(inst) != ShishouModes.COUNT) return Result.fail("mode count " + s.getModes(inst));
            java.util.Set<String> ids = new java.util.HashSet<>();
            for (int m = 0; m < s.getModes(inst); m++) {
                if (!ids.add(s.getModeId(inst, m))) return Result.fail("duplicate mode id " + s.getModeId(inst, m));
                if (s.getModeName(inst, m).getString().isEmpty()) return Result.fail("mode " + m + " has no name");
            }
            if (s.nextMode(a, inst, ShishouModes.COUNT - 1, false) != 0 || s.nextMode(a, inst, 0, true) != ShishouModes.COUNT - 1) return Result.fail("the mode key does not wrap");
            if (!ShishouConfig.enabled() || !ShishouConfig.grantSkill()) return Result.ok("skipped the grant: relic.shishouEnabled or skill.grantUltimateSkill is off (the skill itself is fine)");
            cap.reset();
            ShishouLive.clearAll();
            ShishouGrantService.sync(a);
            ShishouGrantService.sync(b);
            if (ShishouGrantService.has(a) || ShishouGrantService.has(b)) return Result.fail("the skill exists with no wielder");
            cap.adopt(a.getUUID());
            if (ShishouGrantService.sync(a) != ShishouGrant.Verdict.GRANT || !ShishouGrantService.has(a)) return Result.fail("the wielder was not given the skill");
            if (ShishouGrantService.sync(b) != ShishouGrant.Verdict.KEEP || ShishouGrantService.has(b)) return Result.fail("a stranger got the skill");
            if (ShishouGrantService.sync(a) != ShishouGrant.Verdict.KEEP) return Result.fail("a second check changed something");
            ShishouGrantService.syncAll(server);
            cap.reset();                                                                         // the sword is nobody's now
            if (ShishouGrantService.sync(a) != ShishouGrant.Verdict.REVOKE || ShishouGrantService.has(a)) return Result.fail("the skill stayed after the sword left the wielder");
            return Result.ok("the skill (" + ShishouModes.COUNT + " modes) is granted to the wielder only, kept on a second check and revoked when the sword is no longer theirs");
        } finally {
            cap.reset();
            d.touch();
            ShishouLive.clearAll();
            ShishouGrantService.sync(a);
            ShishouGrantService.sync(b);
        }
    }

    // ================================================================ relic.shishou_feed: feeding from the inventory
    private static Result feed(MinecraftServer server) {
        if (!ShishouData.bound()) return Result.fail("the data is not bound to the server");
        ShishouData d = ShishouData.get();
        ShishouCap cap = d.cap();
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 3, 0);
        ServerPlayer a = SelfTests.standIn(level, "shishou_f1", at.x, at.y, at.z), b = SelfTests.standIn(level, "shishou_f2", at.x + 1, at.y, at.z);
        ShishouRecord rec = d.record();
        int levelBefore = rec.level();
        try {
            if (!ShishouConfig.enabled()) return Result.ok("skipped: relic.shishouEnabled is false");
            cap.reset();
            ShishouLive.clearAll();
            cap.adopt(a.getUUID());
            ItemStack sword = ShishouItems.creativeStack();
            sword.set(ShishouComponents.TOKEN.get(), cap.token());
            sword.set(ShishouComponents.OWNER.get(), a.getUUID().toString());
            rec.setLevel(Math.max(ShishouConfig.startLevel(), 12));
            // the pure rules agree with the real item classes
            if (ShishouFeed.classify(ShishouFeedService.describe(new ItemStack(Items.DIAMOND_SWORD))) != ShishouFeed.Kind.NONE) return Result.fail("a sword counted as food");
            if (ShishouFeed.classify(ShishouFeedService.describe(new ItemStack(Items.STONE))) != ShishouFeed.Kind.NONE) return Result.fail("a block counted as food");
            if (ShishouFeed.classify(ShishouFeedService.describe(new ItemStack(Items.ROTTEN_FLESH, 3))) != ShishouFeed.Kind.DROP) return Result.fail("rotten flesh is not a drop");
            // a stranger's click is not taken over, and nothing is eaten
            ItemStack food = new ItemStack(Items.ROTTEN_FLESH, 8);
            if (ShishouFeedService.onStacked(sword, food, b, true, true) || food.getCount() != 8) return Result.fail("a non-wielder fed him");
            // the wielder: the whole stack on a left click, valued by the table, experience paid once
            long before = rec.totalExp();
            if (!ShishouFeedService.onStacked(sword, food, a, true, true)) return Result.fail("the wielder's click was not taken over");
            long want = ShishouService.scaleExp(ShishouFeed.value(ShishouFeedService.describe(new ItemStack(Items.ROTTEN_FLESH, 8)), ShishouFeed.take(8, true, ShishouConfig.feedPerClick())));
            if (!ShishouConfig.feed()) return Result.ok("the click is taken over and refused while relic.shishouFeed is false");
            if (food.getCount() != 8 - ShishouFeed.take(8, true, ShishouConfig.feedPerClick())) return Result.fail("the stack was not eaten by count: " + food.getCount());
            if (rec.totalExp() - before != want) return Result.fail("experience paid " + (rec.totalExp() - before) + ", the table says " + want);
            // a second click in the same tick is refused (rate limit) and eats nothing
            ItemStack more = new ItemStack(Items.BONE, 4);
            ShishouFeedService.onStacked(sword, more, a, true, true);
            if (more.getCount() != 4) return Result.fail("a second feeding in the same tick was not refused");
            // a rare or enchanted drop is taken over but never eaten
            ShishouLive.FEED_GAP.clear();
            ItemStack fancy = new ItemStack(Items.BONE, 2);
            fancy.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING), 1);
            ShishouFeedService.onStacked(sword, fancy, a, true, true);
            if (fancy.getCount() != 2) return Result.fail("an enchanted bone was eaten");
            // a sword at the cap keeps the stack
            ShishouLive.FEED_GAP.clear();
            rec.setLevel(ShishouLevels.MAX_LEVEL);
            ItemStack last = new ItemStack(Items.ROTTEN_FLESH, 5);
            ShishouFeedService.onStacked(sword, last, a, true, true);
            if (last.getCount() != 5) return Result.fail("a sword at the level cap ate");
            return Result.ok("feeding: the wielder only, by count, paid by the table, rate limited, valuables and a full level keep the stack");
        } finally {
            rec.setLevel(levelBefore);
            cap.reset();
            d.touch();
            ShishouLive.clearAll();
        }
    }

    // ================================================================ relic.shishou_hunt: the bounded scan and the charge
    private static Result hunt(MinecraftServer server) {
        if (!ShishouData.bound()) return Result.fail("the data is not bound to the server");
        ShishouData d = ShishouData.get();
        ShishouCap cap = d.cap();
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 8, 0);
        List<net.minecraft.world.entity.Entity> made = new ArrayList<>();
        try {
            if (!ShishouConfig.enabled()) return Result.ok("skipped: relic.shishouEnabled is false");
            cap.reset();
            ShishouLive.clearAll();
            ShishouEntity sword = ShishouService.spawnWild(level, at);
            if (sword == null) return Result.fail("the wild sword did not spawn");
            made.add(sword);
            int r = ShishouConfig.huntRadius();
            Zombie near = EntityType.ZOMBIE.create(level), far = EntityType.ZOMBIE.create(level);
            net.minecraft.world.entity.npc.Villager villager = EntityType.VILLAGER.create(level);
            if (near == null || far == null || villager == null) return Result.fail("could not make the stand-in creatures");
            near.moveTo(at.x + r * 0.6, at.y + 10, at.z, 0, 0);                                  // up in the air within the cube: the 2.0.0 scan (4 blocks up and down) missed this
            far.moveTo(at.x + r * 1.5, at.y, at.z, 0, 0);
            villager.moveTo(at.x + 2, at.y, at.z, 0, 0);
            for (var e : new net.minecraft.world.entity.Mob[]{near, far, villager}) {
                e.setPersistenceRequired();
                e.setNoAi(true);
                if (level.addFreshEntity(e)) made.add(e);
            }
            net.minecraft.world.entity.LivingEntity found = ShishouGoals.scan(sword, level, level.getGameTime());
            if (found != near) return Result.fail("the scan picked " + found + " instead of the monster within the radius (never the villager, never one beyond " + r + " blocks)");
            // the charge accelerates and never exceeds the top speed
            sword.beginCharge();
            double top = ShishouHunt.topSpeed(sword.swordLevel(), ShishouConfig.chargePercent());
            Vec3 aim = near.position().add(0, 1, 0);
            double v0 = -1.0, v = 0.0;
            for (int i = 0; i < 12; i++) {
                sword.charge(aim);
                v = sword.getDeltaMovement().length();
                sword.setPos(sword.position().add(sword.getDeltaMovement()));                      // it flies, so it is never 'blocked'
                if (i == 0) v0 = v;
            }
            if (!(v > v0) || v > top + 1e-6) return Result.fail("the charge did not accelerate within the cap: " + v0 + " -> " + v + " (top " + top + ")");
            return Result.ok("the scan finds the monster in the cube, skips the villager and the far one; the charge accelerates to " + String.format("%.2f", v) + " blocks a tick (top " + String.format("%.2f", top) + ")");
        } finally {
            for (var e : made) if (!e.isRemoved()) {
                if (e instanceof ShishouEntity se) se.expectRemoval();
                e.discard();
            }
            cap.reset();
            d.touch();
            ShishouLive.clearAll();
        }
    }

    // ================================================================ relic.shishou_skills: Skill Eater, Stomach, clones, Magic Sense, Black Flame
    private static Result skills(MinecraftServer server) {
        if (!ShishouData.bound()) return Result.fail("the data is not bound to the server");
        ShishouData d = ShishouData.get();
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 3, 0);
        ServerPlayer p = SelfTests.standIn(level, "shishou_skills", at.x, at.y, at.z);
        List<net.minecraft.world.entity.Entity> made = new ArrayList<>();
        try {
            // Skill Eater's table
            if (ShishouStealTable.lookup("minecraft:blaze", false).skill() != ShishouSkill.FIRE_MAGIC || ShishouStealTable.lookup("minecraft:stray", false).skill() != ShishouSkill.WATER_ICE_MAGIC)
                return Result.fail("the steal table lost a row");
            // Stomach: refuses living things and containers, accepts loot, never above the usable rows
            if (ShishouStorage.accepts(new ItemStack(Items.SHULKER_BOX)) || ShishouStorage.accepts(new ItemStack(Items.PIG_SPAWN_EGG)) || ShishouStorage.accepts(new ItemStack(Items.AXOLOTL_BUCKET))
                    || ShishouStorage.accepts(ShishouItems.creativeStack())) return Result.fail("Stomach accepted something it must refuse");
            if (!ShishouStorage.accepts(new ItemStack(Items.DIAMOND, 3))) return Result.fail("Stomach refused plain loot");
            ShishouStorage st = new ShishouStorage();
            ItemStack rest = st.insert(new ItemStack(Items.DIAMOND, 64));
            if (!rest.isEmpty() || st.getItem(0).getCount() != 64) return Result.fail("Stomach did not take a full stack");
            ItemStack overflow = new ItemStack(Items.IRON_INGOT, 64);
            for (int i = 0; i < ShishouStorage.usable() + 2; i++) overflow = st.insert(new ItemStack(Items.IRON_INGOT, 64));
            if (overflow.isEmpty()) return Result.fail("Stomach took more than its usable slots");
            if (st.usedSlots() > ShishouStorage.usable()) return Result.fail("Stomach used slots beyond its rows");
            // Doppelganger: the clone caps
            java.util.UUID w = java.util.UUID.randomUUID(), t = java.util.UUID.randomUUID();
            ShishouLive.CLONES.clear();
            int n1 = ShishouLive.CLONES.spawn(w, t, 99, 0L, 100, ShishouRules.CLONES_PER_CAST, ShishouRules.CLONES_GLOBAL), n2 = ShishouLive.CLONES.spawn(w, t, 99, 0L, 100, ShishouRules.CLONES_PER_CAST, ShishouRules.CLONES_GLOBAL),
                    n3 = ShishouLive.CLONES.spawn(w, t, 99, 0L, 100, ShishouRules.CLONES_PER_CAST, ShishouRules.CLONES_GLOBAL);
            if (n1 != 4 || n2 != 2 || n3 != 0 || ShishouLive.CLONES.count() > ShishouRules.CLONES_GLOBAL) return Result.fail("the clone caps do not hold: " + n1 + "," + n2 + "," + n3);
            ShishouLive.CLONES.forget(w);
            if (ShishouLive.CLONES.count() != 0) return Result.fail("the clones were not cleaned up");
            // Magic Sense: one pulse marks at most 64 creatures, never a player
            for (int i = 0; i < ShishouRules.SENSE_TARGETS + 8; i++) {
                Zombie z = EntityType.ZOMBIE.create(level);
                if (z == null) break;
                z.moveTo(at.x + (i % 8) * 0.8 - 3, at.y, at.z + (i / 8) * 0.8 - 3, 0, 0);
                z.setPersistenceRequired();
                if (level.addFreshEntity(z)) made.add(z);
            }
            int marked = ShishouSpells.senseScan(level, p);
            if (marked < 1 || marked > ShishouRules.SENSE_TARGETS) return Result.fail("Magic Sense marked " + marked + " of " + made.size() + " creatures");
            if (p.hasEffect(MobEffects.GLOWING)) return Result.fail("Magic Sense marked a player");
            // Black Flame
            Zombie z = (Zombie) made.get(0);
            if (!ShishouEffects.ignite(z, p, 80) || !ShishouEffects.burning(z)) return Result.fail("Black Flame did not light");
            // Skill Eater's reward flow through the real service
            ShishouCap cap = d.cap();
            cap.reset();
            cap.adopt(p.getUUID());
            ShishouLive.LINKED.put(p.getUUID(), level.getGameTime());
            ItemStack sword = ShishouItems.creativeStack();
            sword.set(ShishouComponents.TOKEN.get(), cap.token());
            sword.set(ShishouComponents.OWNER.get(), p.getUUID().toString());
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
            long before = d.record().totalExp();
            long killsBefore = d.record().kills();
            Zombie victim = EntityType.ZOMBIE.create(level);
            victim.moveTo(at.x, at.y, at.z + 4, 0, 0);
            ShishouService.onKill(victim, level.damageSources().playerAttack(p));
            if (d.record().totalExp() <= before || d.record().kills() != killsBefore + 1) return Result.fail("a kill by the wielder paid nothing");
            ShishouService.onKill(victim, level.damageSources().generic());
            if (d.record().kills() != killsBefore + 1) return Result.fail("a kill by nobody was counted");
            return Result.ok("steal table, Stomach, clone caps, Magic Sense (" + marked + " marked), Black Flame and the kill reward hold");
        } finally {
            for (var e : made) e.discard();
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            d.cap().reset();
            d.touch();
            ShishouLive.clearAll();
        }
    }
}
