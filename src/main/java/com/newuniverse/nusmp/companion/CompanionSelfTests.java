package com.newuniverse.nusmp.companion;

import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.companion.bond.Bond;
import com.newuniverse.nusmp.companion.bond.Companion;
import com.newuniverse.nusmp.companion.bond.CompanionData;
import com.newuniverse.nusmp.companion.bond.Favourites;
import com.newuniverse.nusmp.companion.bond.PersonState;
import com.newuniverse.nusmp.companion.bond.Stance;
import com.newuniverse.nusmp.companion.follow.CompanionTravel;
import com.newuniverse.nusmp.companion.net.CompanionNet;
import com.newuniverse.nusmp.companion.sample.ArcCompanionEntity;
import com.newuniverse.nusmp.companion.sample.CompanionEntities;
import com.newuniverse.nusmp.companion.sample.CompanionProfiles;
import com.newuniverse.nusmp.companion.talk.Bubble;
import com.newuniverse.nusmp.companion.talk.BubbleRules;
import com.newuniverse.nusmp.companion.talk.Talk;
import com.newuniverse.nusmp.companion.talk.TalkRules;
import com.newuniverse.nusmp.companion.talk.TalkView;
import com.newuniverse.nusmp.companion.voice.VoiceLines;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Why: nothing of the companion system can be played by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs these (keys
 * {@code magic.companions.*}) on a dedicated server with stand-in players. {@code wiring}: the entity type, attributes, egg, roster record and tastes exist, the screen's wire form round-trips and refuses an
 * oversized view or id. {@code talk}: a conversation opens, a forged id, a too-fast pick, a pick from too far and a pick to a dead entity never reach the entity, and a session of a player who left is swept.
 * {@code bond}: the high five (once a day), loved / plain / hated / refused gifts with their rapport, a stance, the pouch in and out, the pouch healing a low player, and the save round trip. {@code follow}: asking
 * to walk with the player and to wait, the foe rule (a zombie yes; a villager, a player, a companion, a pet and a cow no; a struck cow yes), the stances' targeting, the damage guard, and followers carried to the
 * player in the same level and through a REAL dimension change to the Nether. {@code bubble}: the speech bubble throttles and its per-tick budget, and the voice hook. Everything is cleaned up at the end.
 */
public final class CompanionSelfTests {
    private CompanionSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.companions.wiring", CompanionSelfTests::wiring);
        SelfTests.register("magic.companions.talk", CompanionSelfTests::talk);
        SelfTests.register("magic.companions.bond", CompanionSelfTests::bond);
        SelfTests.register("magic.companions.follow", CompanionSelfTests::follow);
        SelfTests.register("magic.companions.bubble", CompanionSelfTests::bubble);
    }

    private static void ensure(boolean ok, String what) {
        if (!ok) throw new IllegalStateException(what);
    }

    private static ArcCompanionEntity spawn(ServerLevel level, Vec3 at) {
        ArcCompanionEntity e = CompanionEntities.CHARMY.get().create(level);
        ensure(e != null, "the companion could not be created");
        e.moveTo(at.x, at.y, at.z, 0f, 0f);
        ensure(level.addFreshEntity(e), "the companion could not be added to the level");
        return e;
    }

    private static void clean(ServerPlayer p, Entity... entities) {
        Talk.forget(p);
        CompanionTravel.forget(p.getUUID());
        p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).remove(CompanionData.ROOT);
        p.getInventory().clearContent();
        p.setHealth(p.getMaxHealth());
        for (Entity e : entities) if (e != null) e.discard();
    }

    private static void pick(ServerPlayer p, Entity e, String id) {
        Talk.clearLimits();
        Talk.onPick(p, e.getId(), id);
    }

    private static void hand(ServerPlayer p, ItemStack s) { p.setItemInHand(InteractionHand.MAIN_HAND, s); }

    // ================================================================ wiring
    private static Result wiring(MinecraftServer server) throws Exception {
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", "arc_companion"))) return Result.fail("the entity type nusmp:arc_companion is not registered");
        if (!DefaultAttributes.hasSupplier(CompanionEntities.CHARMY.get())) return Result.fail("the companion has no attributes");
        if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", "companion_charmy_spawn_egg"))) return Result.fail("the spawn egg is not registered");
        if (ArcRoster.byId(CompanionProfiles.CHARMY.characterId()) == null) return Result.fail("Charmy is not in the arcs' roster");
        for (CompanionProfiles.Profile pr : CompanionProfiles.all()) if (!Favourites.has(pr.bondKey())) return Result.fail("the tastes of " + pr.bondKey() + " are not registered");
        if (CompanionConfig.maxFollowers() < 1) return Result.fail("companions.maxFollowersPerPlayer is below 1");

        // the wire form of a screen
        ServerLevel level = server.overworld();
        TalkView.Builder b = TalkView.builder(Component.literal("N"), Component.translatable("x.sub"), 0xF4EAD0).text(Component.translatable("x.text", "a"));
        b.add("t:a", Component.translatable("x.a"), TalkView.TALK).addBlocked("t:b", Component.translatable("x.b"), TalkView.INTERACT, Component.translatable("x.why"));
        b.addSystem(TalkView.Option.of("k:five", Component.translatable("x.five"), TalkView.INTERACT));
        List<ItemStack> pouch = new ArrayList<>();
        pouch.add(new ItemStack(Items.BREAD, 3));
        for (int i = 1; i < CompanionRules.POUCH_SLOTS; i++) pouch.add(ItemStack.EMPTY);
        b.status(12, 1, 1, pouch);
        TalkView v = b.build();
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        CompanionNet.Show.STREAM_CODEC.encode(buf, new CompanionNet.Show(77, v));
        CompanionNet.Show back = CompanionNet.Show.STREAM_CODEC.decode(buf);
        if (back.entityId() != 77 || back.view().options.size() != 3 || !back.view().ids().equals(Set.of("t:a", "t:b", "k:five"))) return Result.fail("the screen did not survive the wire");
        if (!back.view().options.get(1).blocked() || back.view().options.get(0).blocked()) return Result.fail("the blocked flags did not survive the wire");
        if (back.view().rapport != 12 || back.view().stance != 1 || back.view().useIt != 1 || back.view().pouch.size() != 9 || back.view().pouch.get(0).getCount() != 3) return Result.fail("the status did not survive the wire");
        if (!back.view().name.getString().equals("N")) return Result.fail("the name did not survive the wire");

        // an oversized view and an oversized id are refused when read
        RegistryFriendlyByteBuf evil = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(evil, Component.empty());
        net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(evil, Component.empty());
        evil.writeInt(0);
        net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(evil, Component.empty());
        evil.writeVarInt(TalkRules.MAX_OPTIONS + 1);
        boolean refused = false;
        try {
            TalkView.STREAM_CODEC.decode(evil);
        } catch (RuntimeException expected) {
            refused = true;
        }
        if (!refused) return Result.fail("a view with 41 options was accepted off the wire");
        RegistryFriendlyByteBuf longId = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        longId.writeVarInt(5);
        longId.writeUtf("a".repeat(200), 400);
        refused = false;
        try {
            CompanionNet.Pick.STREAM_CODEC.decode(longId);
        } catch (RuntimeException expected) {
            refused = true;
        }
        if (!refused) return Result.fail("a 200 character id was accepted off the wire");
        refused = false;
        try {
            TalkView.builder(Component.empty(), Component.empty(), 0).add("k:mine", Component.empty(), 0);
        } catch (IllegalArgumentException expected) {
            refused = true;
        }
        if (!refused) return Result.fail("a companion's own option could take a k: id");
        TalkView.Builder many = TalkView.builder(Component.empty(), Component.empty(), 0);
        for (int i = 0; i < TalkRules.MAX_OPTIONS; i++) many.add("t:" + i, Component.empty(), 0);
        refused = false;
        try {
            many.add("t:over", Component.empty(), 0);
        } catch (IllegalArgumentException expected) {
            refused = true;
        }
        if (!refused) return Result.fail("a 41st option was accepted");
        return Result.ok("entity, attributes, egg, roster record, tastes; the screen round-trips; 41 options, a k: id and a 200 character id are refused");
    }

    // ================================================================ talk
    private static Result talk(MinecraftServer server) throws Exception {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "companion_talk", at.x, at.y, at.z);
        ArcCompanionEntity c = spawn(level, at.add(2, 0, 0));
        try {
            Talk.clearLimits();
            if (!Talk.open(p, c)) return Result.fail("the conversation did not open");
            if (!Talk.isOpen(p.getUUID())) return Result.fail("no session was recorded");
            if (Talk.open(p, c)) return Result.fail("a second open in the same instant was accepted (no rate limit)");
            Set<String> offered = Talk.offered(p.getUUID());
            for (String id : new String[]{"t:self", "t:food", "t:magic", "k:five", "k:gift", "k:likes", "k:stance:guard", "k:stance:aggressive", "k:stance:passive", "k:put"})
                if (!offered.contains(id)) return Result.fail("the first screen lacks " + id + ": " + offered);
            if (offered.size() > TalkRules.MAX_OPTIONS) return Result.fail("more than 40 options");

            // a forged id never reaches her
            int rapport = c.bondRapport(p);
            pick(p, c, "t:forged");
            pick(p, c, "k:forged");
            pick(p, c, "a".repeat(41));
            pick(p, c, "");
            if (!Talk.isOpen(p.getUUID()) || !Talk.offered(p.getUUID()).equals(offered)) return Result.fail("a forged id changed the conversation");
            if (c.bondRapport(p) != rapport) return Result.fail("a forged id changed the rapport");

            // a pick for another entity id is ignored
            Talk.clearLimits();
            Talk.onPick(p, c.getId() + 100000, "k:five");
            if (c.bondRapport(p) != rapport || c.socialKind() != 0) return Result.fail("a pick naming another entity was acted on");

            // too fast: the second pick in the same tick is dropped
            Talk.clearLimits();
            Talk.onPick(p, c.getId(), "t:self");
            Talk.onPick(p, c.getId(), "k:five");
            if (c.socialKind() != 0) return Result.fail("a second pick in the same tick was not rate limited");

            // a pick from more than 8 blocks away ends the conversation and does nothing
            c.teleportTo(at.x + 20, at.y, at.z);
            pick(p, c, "k:five");
            if (Talk.isOpen(p.getUUID())) return Result.fail("a pick from 20 blocks away did not end the conversation");
            if (c.socialKind() != 0 || c.bondRapport(p) != rapport) return Result.fail("a pick from 20 blocks away reached the companion");
            c.teleportTo(at.x + 2, at.y, at.z);

            // a dead entity
            Talk.clearLimits();
            if (!Talk.open(p, c)) return Result.fail("the conversation did not reopen");
            c.discard();
            pick(p, c, "k:five");
            if (Talk.isOpen(p.getUUID())) return Result.fail("a pick to a removed entity did not end the conversation");

            // a session of a player who is not in the player list (left) is swept
            ArcCompanionEntity d = spawn(level, at.add(2, 0, 0));
            Talk.clearLimits();
            if (!Talk.open(p, d)) return Result.fail("the conversation did not reopen with a new entity");
            Talk.sweep(server);
            if (Talk.isOpen(p.getUUID())) return Result.fail("the session of a player who is gone was not swept");
            d.discard();
            Talk.forget(p);
            if (Talk.openCount() != 0 && Talk.isOpen(p.getUUID())) return Result.fail("forget kept the session");
            return Result.ok("opens; a forged id, another entity id, a too-fast pick, 20 blocks away, a dead entity and a gone player all end or ignore");
        } finally {
            clean(p, c);
        }
    }

    // ================================================================ bond
    private static Result bond(MinecraftServer server) throws Exception {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "companion_bond", at.x, at.y, at.z);
        ArcCompanionEntity c = spawn(level, at.add(2, 0, 0));
        String key = c.bondKey();
        try {
            clean(p);
            Talk.clearLimits();
            if (!Talk.open(p, c)) return Result.fail("the conversation did not open");
            if (c.bondRapport(p) != 0) return Result.fail("rapport did not start at 0");

            // the high five: +1 once a day
            pick(p, c, "k:five");
            if (c.bondRapport(p) != 1 || c.socialKind() != Companion.FIVE) return Result.fail("the high five gave " + c.bondRapport(p) + " and kind " + c.socialKind());
            pick(p, c, "k:five");
            c.social(0, 0);
            pick(p, c, "k:five");
            if (c.bondRapport(p) != 1) return Result.fail("a second high five the same day earned rapport");
            CompanionData.setFiveDay(p, key, CompanionRules.day(level.getGameTime()) - 1);
            c.social(0, 0);
            pick(p, c, "k:five");
            if (c.bondRapport(p) != 2) return Result.fail("the high five on a new day did not earn 1: " + c.bondRapport(p));

            // gifts
            hand(p, ItemStack.EMPTY);
            pick(p, c, "k:gift");
            if (c.bondRapport(p) != 2) return Result.fail("a gift with an empty hand changed the rapport");
            hand(p, new ItemStack(Items.CAKE, 3));
            pick(p, c, "k:gift");
            if (c.bondRapport(p) != 7) return Result.fail("the first loved gift did not give 5: " + c.bondRapport(p));
            pick(p, c, "k:gift");
            if (c.bondRapport(p) != 12) return Result.fail("the second loved gift did not give 5: " + c.bondRapport(p));
            pick(p, c, "k:gift");
            if (c.bondRapport(p) != 13) return Result.fail("the third loved gift of the day did not give 1: " + c.bondRapport(p));
            if (!p.getMainHandItem().isEmpty()) return Result.fail("three gifts did not use up three cakes");
            hand(p, new ItemStack(Items.ROTTEN_FLESH, 2));
            pick(p, c, "k:gift");
            if (c.bondRapport(p) != 12 || p.getMainHandItem().getCount() != 1) return Result.fail("a hated gift did not cost 1 and take the item: " + c.bondRapport(p));
            hand(p, new ItemStack(Items.STICK, 2));
            pick(p, c, "k:gift");
            if (c.bondRapport(p) != 12 || p.getMainHandItem().getCount() != 2) return Result.fail("a refused gift changed the rapport or was taken");
            hand(p, new ItemStack(Items.APPLE, 1));
            pick(p, c, "k:gift");
            if (c.bondRapport(p) != 14 || !p.getMainHandItem().isEmpty()) return Result.fail("a plain food gift did not give 2: " + c.bondRapport(p));
            hand(p, new ItemStack(Items.SUGAR, 1));
            pick(p, c, "k:gift");
            if (c.bondRapport(p) != 16) return Result.fail("an accepted non-food gift did not give 2: " + c.bondRapport(p));
            if (CompanionRules.tier(c.bondRapport(p)) != CompanionRules.FRIEND) return Result.fail("16 is not a friend");

            // a stance
            pick(p, c, "k:stance:aggressive");
            if (c.bond().stance != Stance.AGGRESSIVE) return Result.fail("the stance did not change");
            pick(p, c, "k:stance:guard");
            if (c.bond().stance != Stance.GUARD) return Result.fail("the stance did not change back");

            // the pouch (she is nobody's and the player is a friend)
            hand(p, new ItemStack(Items.BREAD, 5));
            pick(p, c, "k:put");
            if (!p.getMainHandItem().isEmpty() || c.bond().pouch.get(0).getCount() != 5) return Result.fail("the pouch did not take the bread");
            pick(p, c, "k:take:0");
            if (!c.bond().isEmpty() || p.getInventory().countItem(Items.BREAD) != 5) return Result.fail("the pouch did not give the bread back");
            pick(p, c, "k:use:off");
            if (c.bond().useIt) return Result.fail("the use switch did not turn off");
            pick(p, c, "k:use:on");
            if (!c.bond().useIt) return Result.fail("the use switch did not turn on");

            // the pouch heals a player who is low (she walks with them now)
            PersonState st = c.bond();
            st.put(new ItemStack(Items.APPLE, 2));
            st.put(new ItemStack(Items.COOKED_BEEF, 2));
            st.put(PotionContents.createItemStack(Items.POTION, Potions.HEALING));
            if (st.pouch.get(2).isEmpty() || Bond.bestSlot(st) != 2) return Result.fail("a healing potion is not the first choice: slot " + Bond.bestSlot(st));
            st.take(2);
            if (Bond.bestSlot(st) != 1) return Result.fail("the most nourishing food is not the next choice: slot " + Bond.bestSlot(st));
            Talk.end(p, false);
            c.talkChoose(p, "t:join");
            if (!p.getUUID().equals(c.bondOwner())) return Result.fail("she did not agree to walk with a friend");
            p.setHealth(3f);
            c.tickCount = 7;
            Bond.tick(c);
            if (p.getHealth() <= 3f || c.bond().pouch.get(1).getCount() != 1) return Result.fail("she did not use the beef on a low player: health " + p.getHealth());
            float after = p.getHealth();
            p.setHealth(3f);
            c.tickCount = 27;
            Bond.tick(c);
            if (p.getHealth() != 3f) return Result.fail("she used the pouch twice within 30 seconds (health " + p.getHealth() + ", was " + after + ")");
            c.talkChoose(p, "t:wait");

            // the save round trip
            c.bond().stance = Stance.PASSIVE;
            c.bond().useIt = false;
            CompoundTag tag = new CompoundTag();
            c.addAdditionalSaveData(tag);
            ArcCompanionEntity again = spawn(level, at.add(-2, 0, 0));
            try {
                again.readAdditionalSaveData(tag);
                if (again.bond().stance != Stance.PASSIVE || again.bond().useIt || again.bond().pouch.get(0).getItem() != Items.APPLE || again.bond().pouch.get(0).getCount() != 2) return Result.fail("the bond did not survive saving");
            } finally {
                again.discard();
            }
            return Result.ok("high five once a day; loved 5/5/1, plain 2, hated -1, refused untouched; stance; pouch in, out, switch and healing every 30 s; save round trip");
        } finally {
            clean(p, c);
        }
    }

    // ================================================================ follow
    private static Result follow(MinecraftServer server) throws Exception {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "companion_follow", at.x, at.y, at.z);
        List<Entity> mess = new ArrayList<>();
        try {
            clean(p);
            ArcCompanionEntity a = spawn(level, at.add(2, 0, 0)), b = spawn(level, at.add(-2, 0, 0)), c = spawn(level, at.add(0, 0, 3));
            mess.add(a);
            mess.add(b);
            mess.add(c);
            boolean hasGoals = a.goalSelector.getAvailableGoals().stream().anyMatch(w -> w.getGoal().getClass().getSimpleName().equals("FollowPlayerGoal"))
                    && a.targetSelector.getAvailableGoals().stream().anyMatch(w -> w.getGoal().getClass().getSimpleName().equals("StanceTargets"));
            if (!hasGoals) return Result.fail("the follow or target goal is not registered");

            // asking to walk
            a.talkChoose(p, "t:join");
            if (a.bondOwner() != null) return Result.fail("a stranger agreed to walk with the player");
            CompanionData.addRapport(p, a.bondKey(), CompanionRules.ACQUAINTANCE_AT);
            a.talkChoose(p, "t:join");
            b.talkChoose(p, "t:join");
            c.talkChoose(p, "t:join");
            int walking = (p.getUUID().equals(a.bondOwner()) ? 1 : 0) + (p.getUUID().equals(b.bondOwner()) ? 1 : 0) + (p.getUUID().equals(c.bondOwner()) ? 1 : 0);
            if (walking != Math.min(3, CompanionConfig.maxFollowers())) return Result.fail(walking + " walk with the player, the setting allows " + CompanionConfig.maxFollowers());
            if (a.followedPlayer() == null) return Result.fail("the first one did not agree");

            // who may be attacked
            Zombie zombie = EntityType.ZOMBIE.create(level);
            Villager villager = EntityType.VILLAGER.create(level);
            Wolf wolf = EntityType.WOLF.create(level);
            net.minecraft.world.entity.animal.Cow cow = EntityType.COW.create(level);
            for (Entity e : new Entity[]{zombie, villager, wolf, cow}) {
                e.moveTo(at.x + 4, at.y, at.z, 0f, 0f);
                level.addFreshEntity(e);
                mess.add(e);
            }
            wolf.tame(p);
            if (!Bond.foe(a, p, zombie)) return Result.fail("a zombie is not a foe");
            if (Bond.foe(a, p, villager)) return Result.fail("a villager is a foe");
            if (Bond.foe(a, p, p)) return Result.fail("the player is a foe");
            if (Bond.foe(a, p, b)) return Result.fail("another companion is a foe");
            if (Bond.foe(a, p, wolf)) return Result.fail("the owner's wolf is a foe");
            if (Bond.foe(a, p, cow)) return Result.fail("a cow nobody touched is a foe");
            p.tickCount = 500;
            p.setLastHurtMob(cow);
            if (!Bond.foe(a, p, cow)) return Result.fail("a cow the player just struck is not a foe");
            p.tickCount = 900;
            if (Bond.foe(a, p, cow)) return Result.fail("a blow from 400 ticks ago still counts");
            if (Bond.foe(a, null, zombie) != true) return Result.fail("a hostile thing is not a foe to a companion with no owner");

            // stances
            a.bond().stance = Stance.PASSIVE;
            if (Bond.wants(a, a, p, zombie)) return Result.fail("a passive companion goes for a zombie");
            a.bond().stance = Stance.AGGRESSIVE;
            if (!Bond.wants(a, a, p, zombie)) return Result.fail("an aggressive companion ignores a zombie 4 blocks away");
            a.bond().stance = Stance.GUARD;
            if (!Bond.wants(a, a, p, zombie)) return Result.fail("a guard ignores a zombie 4 blocks from the player");
            if (Bond.wants(a, a, p, villager)) return Result.fail("a guard goes for a villager");

            // the damage guard
            LivingIncomingDamageEvent byPlayer = new LivingIncomingDamageEvent(a, new DamageContainer(level.damageSources().playerAttack(p), 4f));
            CompanionEvents.onDamage(byPlayer);
            if (!byPlayer.isCanceled()) return Result.fail("a player's blow landed on a companion");
            LivingIncomingDamageEvent byOther = new LivingIncomingDamageEvent(a, new DamageContainer(level.damageSources().mobAttack(b), 4f));
            CompanionEvents.onDamage(byOther);
            if (!byOther.isCanceled()) return Result.fail("a companion's blow landed on a companion");
            LivingIncomingDamageEvent byMagic = new LivingIncomingDamageEvent(a, new DamageContainer(level.damageSources().magic(), 4f));
            CompanionEvents.onDamage(byMagic);
            if (byMagic.isCanceled()) return Result.fail("the guard blocked damage that is nobody's blow");
            if (CompanionEvents.isCompanion(zombie)) return Result.fail("a zombie counts as a companion");

            // followers come with the player: the same level first
            for (Entity e : new Entity[]{a, b, c}) e.teleportTo(at.x + 25, at.y, at.z);
            int noted = CompanionTravel.collect(p);
            // the followers are 25 blocks away, inside the 32 block collect radius
            int cap = Math.min(walking, Math.min(CompanionConfig.maxFollowers(), 6));
            if (noted != cap || CompanionTravel.collectedFor(p.getUUID()) != cap) return Result.fail("collected " + noted + " followers, expected " + cap);
            if (!CompanionTravel.markArrived(p)) return Result.fail("the crossing was not moved to the arrivals");
            int moved = CompanionTravel.arriveNow(server, p, level);
            if (moved != cap) return Result.fail("put " + moved + " followers down, expected " + cap);
            int near = 0;
            for (Entity e : new Entity[]{a, b, c}) if (e.isAlive() && e.distanceToSqr(p) < 8.0 * 8.0) near++;
            if (near != cap) return Result.fail(near + " followers stand beside the player, expected " + cap);
            if (CompanionTravel.kept() != 0) return Result.fail("the travel record was kept after the crossing");

            // and through a real dimension change
            ServerLevel nether = server.getLevel(Level.NETHER);
            if (nether != null) {
                nether.getChunk(p.blockPosition().getX() >> 4, p.blockPosition().getZ() >> 4);              // the chunk the player would stand in, loaded by the test
                for (Entity e : new Entity[]{a, b, c}) if (e.isAlive() && !e.isRemoved()) e.teleportTo(at.x + 3, at.y, at.z);
                int n = CompanionTravel.collect(p);
                CompanionTravel.markArrived(p);
                int over = CompanionTravel.arriveNow(server, p, nether);
                if (over != n || n < 1) return Result.fail("carried " + over + " of " + n + " followers to the Nether");
                int there = 0;
                for (Entity e : new Entity[]{a, b, c}) {
                    Entity moved2 = nether.getEntity(e.getUUID());
                    if (moved2 instanceof ArcCompanionEntity ace && ace.isAlive()) {
                        there++;
                        if (!p.getUUID().equals(ace.bondOwner())) return Result.fail("a follower lost her owner in the crossing");
                        mess.add(ace);
                    }
                }
                if (there != over) return Result.fail(there + " followers stand in the Nether, expected " + over);
            }
            return Result.ok("walk / wait; foe rule; stances; damage guard; " + cap + " followers put down beside the player in the same level" + (nether != null ? " and through a real change to the Nether" : " (no Nether level to test the crossing)"));
        } finally {
            clean(p, mess.toArray(new Entity[0]));
        }
    }

    // ================================================================ bubble and voice
    private static Result bubble(MinecraftServer server) throws Exception {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        List<Entity> mess = new ArrayList<>();
        String key = "nusmp.companion.selftest.line";
        try {
            Bubble.clear();
            ArcCompanionEntity c = spawn(level, at);
            mess.add(c);
            Component line = Component.translatable("nusmp.companion.charmy.greet", "x");
            if (!Bubble.say(c, line)) return Result.fail("the first bubble was refused");
            if (Bubble.say(c, line)) return Result.fail("a second bubble in the same instant was sent (no per-speaker gap)");
            if (Bubble.recorded() != 1) return Result.fail("the record count is " + Bubble.recorded());
            if (Bubble.say(c, Component.literal("x".repeat(BubbleRules.MAX_CHARS * 5)))) return Result.fail("an absurdly long line was sent");
            Bubble.forget(c.getUUID());
            if (Bubble.recorded() != 0) return Result.fail("forget kept the record");

            // the global per-tick budget
            Bubble.clear();
            int sent = 0;
            for (int i = 0; i < 20; i++) {
                ArcCompanionEntity e = spawn(level, at.add(i * 0.01, 0, 0));
                mess.add(e);
                if (Bubble.say(e, line)) sent++;
            }
            if (sent > BubbleRules.GLOBAL_PER_TICK) return Result.fail(sent + " bubbles in one tick, the budget is " + BubbleRules.GLOBAL_PER_TICK);
            if (sent == 0) return Result.fail("no bubble was sent");
            Bubble.clear();

            // the voice hook
            Component spoken = Component.translatable(key);
            if (VoiceLines.play(level, at, spoken, SoundSource.NEUTRAL)) return Result.fail("a line with no registration played");
            VoiceLines.register(key, () -> SoundEvents.NOTE_BLOCK_BELL.value());
            if (!VoiceLines.has(key) || !VoiceLines.play(level, at, spoken, SoundSource.NEUTRAL)) return Result.fail("a registered line did not play");
            if (VoiceLines.play(level, at, Component.literal("plain"), SoundSource.NEUTRAL)) return Result.fail("a plain text played");
            int played = 1;
            for (int i = 0; i < 10; i++) if (VoiceLines.play(level, at, spoken, SoundSource.NEUTRAL)) played++;
            if (played > VoiceLines.PER_TICK) return Result.fail(played + " voice lines in one tick, the budget is " + VoiceLines.PER_TICK);
            VoiceLines.unregister(key);
            if (VoiceLines.has(key)) return Result.fail("unregister kept the line");
            return Result.ok("bubble: one per speaker per 2 s, " + sent + " of 20 in one tick (budget " + BubbleRules.GLOBAL_PER_TICK + "), long lines refused; voice: unregistered lines are silent, registered ones play within " + VoiceLines.PER_TICK + " a tick");
        } finally {
            VoiceLines.unregister(key);
            Bubble.clear();
            for (Entity e : mess) e.discard();
        }
    }
}
