package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Set;

/**
 * The bond with the spirit that chose you: a personality trait (50 of them), Trust 0-100 driven by
 * what you do, daily Over-Pull strain on heavy spirit magic, and the Incarnation that gives the
 * spirit a body (the Spirit Lord companion) once Trust reaches 100.
 */
public final class SpiritBond {
    private SpiritBond() {}

    public record Trait(String id, String name, String category, Set<String> likes, Set<String> dislikes) {}

    public static final List<Trait> TRAITS = List.of(
            new Trait("evil", "Evil / Bloodthirsty", "Aggressive", Set.of("kill_mob", "kill_player", "pvp"), Set.of("peaceful_5min", "heal_ally")),
            new Trait("bloodthirsty", "Bloodthirsty", "Aggressive", Set.of("kill_mob", "kill_player", "execute"), Set.of("flee_combat", "peaceful_5min")),
            new Trait("berserker", "Berserker", "Aggressive", Set.of("low_hp_kill", "rage_mode"), Set.of("heal_self", "retreat")),
            new Trait("warlord", "Warlord", "Aggressive", Set.of("kill_boss", "raid_victory"), Set.of("lose_raid", "flee_boss")),
            new Trait("predator", "Predator", "Aggressive", Set.of("ambush", "kill_from_stealth"), Set.of("detected", "open_combat")),
            new Trait("traveler", "Traveler / Nomad", "Exploration", Set.of("discover_biome", "discover_structure", "new_chunk"), Set.of("afk", "stand_still_5min")),
            new Trait("nomad", "Nomad", "Exploration", Set.of("travel_1000b", "cross_dimension"), Set.of("stay_base_1day")),
            new Trait("cartographer", "Cartographer", "Exploration", Set.of("map_area", "discover_structure"), Set.of("destroy_map")),
            new Trait("wanderer", "Wanderer", "Exploration", Set.of("new_chunk", "random_teleport"), Set.of("afk", "home_bound")),
            new Trait("pioneer", "Pioneer", "Exploration", Set.of("first_visit_biome", "build_outpost"), Set.of("abandon_outpost")),
            new Trait("glory_seeker", "Glory-Seeker", "Combat Style", Set.of("kill_boss", "kill_high_ep", "defeat_raid"), Set.of("retreat_under_20hp", "flee_boss")),
            new Trait("duelist", "Duelist", "Combat Style", Set.of("1v1_win", "fair_fight"), Set.of("gank", "use_potion_in_duel")),
            new Trait("tactician", "Tactician", "Combat Style", Set.of("use_environment", "trap_kill"), Set.of("brute_force_fail")),
            new Trait("guardian", "Guardian", "Combat Style", Set.of("protect_ally", "tank_damage"), Set.of("let_ally_die")),
            new Trait("assassin", "Assassin", "Combat Style", Set.of("backstab", "stealth_kill"), Set.of("detected", "open_combat")),
            new Trait("wildwood_keeper", "Wildwood Keeper", "Nature", Set.of("plant_tree", "breed_animal", "grow_crop"), Set.of("deforest", "kill_animal", "break_natural")),
            new Trait("beastmaster", "Beastmaster", "Nature", Set.of("tame_animal", "breed_animal"), Set.of("kill_tamed", "hurt_animal")),
            new Trait("druid", "Druid", "Nature", Set.of("plant_tree", "bonemeal", "heal_nature"), Set.of("deforest", "lava_place")),
            new Trait("elementalist", "Elementalist", "Nature", Set.of("channel_element", "balance_elements"), Set.of("over_pull")),
            new Trait("seasonal", "Seasonal Spirit", "Nature", Set.of("season_change", "weather_match"), Set.of("force_weather")),
            new Trait("treasure_hound", "Treasure Hound", "Greed", Set.of("open_chest", "loot_dungeon", "find_ore"), Set.of("throw_valuable", "destroy_loot")),
            new Trait("hoarder", "Hoarder", "Greed", Set.of("store_item", "fill_chest"), Set.of("drop_item", "give_away")),
            new Trait("merchant", "Merchant", "Greed", Set.of("trade", "sell_item"), Set.of("steal", "scam")),
            new Trait("collector", "Collector", "Greed", Set.of("unique_item", "complete_set"), Set.of("discard_unique")),
            new Trait("prospector", "Prospector", "Greed", Set.of("mine_ore", "find_diamond"), Set.of("waste_pickaxe")),
            new Trait("pure_conduit", "Pure Conduit", "Ascetic", Set.of("meditate", "rest_0_overpull", "clean_channel"), Set.of("over_pull", "kill_innocent")),
            new Trait("pacifist", "Pacifist", "Ascetic", Set.of("no_kill_1day", "heal_mob"), Set.of("kill_mob", "pvp")),
            new Trait("monk", "Monk", "Ascetic", Set.of("meditate", "fast", "no_armor"), Set.of("eat_meat", "use_weapon")),
            new Trait("hermit", "Hermit", "Ascetic", Set.of("alone_1day", "no_chat"), Set.of("party_up", "village_visit")),
            new Trait("sage", "Sage", "Ascetic", Set.of("learn_spell", "read_book", "teach"), Set.of("forget_spell", "destroy_book")),
            new Trait("loyalist", "Loyalist", "Social", Set.of("protect_ally", "share_loot", "follow_order"), Set.of("betray", "abandon_ally")),
            new Trait("trickster", "Trickster", "Social", Set.of("prank", "illusion", "confuse"), Set.of("honest_deal", "straight_fight")),
            new Trait("diplomat", "Diplomat", "Social", Set.of("trade", "peace_treaty", "negotiate"), Set.of("declare_war", "raid_village")),
            new Trait("mentor", "Mentor", "Social", Set.of("teach", "help_newbie", "gift_gear"), Set.of("mock_weak", "steal_from_weak")),
            new Trait("rival", "Rival", "Social", Set.of("challenge", "win_spar"), Set.of("refuse_challenge", "cheat")),
            new Trait("flameheart", "Flameheart", "Elemental", Set.of("fire_kill", "lava_swim", "burn_forest"), Set.of("water_damage", "extinguish")),
            new Trait("tidal", "Tidal Spirit", "Elemental", Set.of("water_kill", "rain", "ocean_explore"), Set.of("fire_damage", "dry_out")),
            new Trait("zephyr", "Zephyr", "Elemental", Set.of("fly", "wind_kill", "high_altitude"), Set.of("grounded", "cave_stuck")),
            new Trait("stonebound", "Stonebound", "Elemental", Set.of("mine", "build_stone", "tank"), Set.of("fall_damage", "fly")),
            new Trait("stormcaller", "Stormcaller", "Elemental", Set.of("lightning_kill", "thunder", "storm_weather"), Set.of("clear_weather", "underground")),
            new Trait("voidtouched", "Void-Touched", "Dark", Set.of("void_kill", "dimension_hop", "shadow_kill"), Set.of("holy_damage", "sunlight")),
            new Trait("cursed", "Cursed", "Dark", Set.of("curse_apply", "wither_kill"), Set.of("cleanse", "holy_item")),
            new Trait("reaper", "Reaper", "Dark", Set.of("execute", "soul_harvest", "kill_player"), Set.of("revive", "totem_use")),
            new Trait("nightmare", "Nightmare", "Dark", Set.of("sleep_interrupt", "fear_apply"), Set.of("sleep", "bed_use")),
            new Trait("chaos", "Chaos", "Dark", Set.of("random_action", "break_rule"), Set.of("order", "plan_follow")),
            new Trait("builder", "Builder", "Utility", Set.of("place_block", "complete_build"), Set.of("grief", "destroy_build")),
            new Trait("farmer", "Farmer", "Utility", Set.of("harvest", "breed", "plant"), Set.of("trample", "kill_crop")),
            new Trait("fisher", "Fisher", "Utility", Set.of("fish", "ocean_loot"), Set.of("waste_fish")),
            new Trait("alchemist", "Alchemist", "Utility", Set.of("brew", "potion_use", "craft_potion"), Set.of("waste_ingredient")),
            new Trait("scholar", "Scholar", "Utility", Set.of("read", "enchant", "research"), Set.of("burn_book", "disenchant"))
    );

    public static Trait trait(String id) { for (Trait t : TRAITS) if (t.id().equals(id)) return t; return null; }

    private static CompoundTag data(Player p) { return ForbiddenMagic.data(p); }
    private static void save(Player p, CompoundTag t) { p.getPersistentData().put(Player.PERSISTED_NBT_TAG, t); }

    public static boolean bonded(Player p) { return data(p).contains("nusmp_spirit_trait"); }
    public static int trust(Player p) { return data(p).getInt("nusmp_spirit_trust"); }
    public static boolean incarnate(Player p) { return data(p).getBoolean("nusmp_spirit_incarnate"); }
    public static String trueName(Player p) { return data(p).getString("nusmp_spirit_name"); }
    public static int fatigue(Player p) { return data(p).getInt("nusmp_spirit_fatigue"); }

    /** First time a spirit chooses you: roll its personality. */
    public static void bond(ServerPlayer p, String spirit) {
        CompoundTag d = data(p);
        if (d.contains("nusmp_spirit_trait")) return;
        Trait t = TRAITS.get(p.getRandom().nextInt(TRAITS.size()));
        d.putString("nusmp_spirit_trait", t.id());
        d.putString("nusmp_spirit_kind", spirit);
        d.putInt("nusmp_spirit_trust", 30);
        save(p, d);
        p.sendSystemMessage(Component.literal(spirit + " has chosen you. Its nature: " + t.name() + " (" + t.category() + ").").withStyle(ChatFormatting.AQUA));
        p.sendSystemMessage(Component.literal("It warms to: " + String.join(", ", t.likes()).replace('_', ' ')).withStyle(ChatFormatting.GREEN));
        p.sendSystemMessage(Component.literal("It resents: " + String.join(", ", t.dislikes()).replace('_', ' ')).withStyle(ChatFormatting.RED));
    }

    public static void addTrust(ServerPlayer p, int amount, String why) {
        CompoundTag d = data(p);
        int before = d.getInt("nusmp_spirit_trust"), after = Math.max(0, Math.min(100, before + amount));
        if (after == before) return;
        d.putInt("nusmp_spirit_trust", after);
        save(p, d);
        String spirit = d.getString("nusmp_spirit_kind");
        if (after == 100 && before < 100)
            p.sendSystemMessage(Component.literal(spirit + " trusts you completely. Name a Spirit Charm at an anvil and sneak-use it to give it a body.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        else if (after < 10 && before >= 10)
            p.sendSystemMessage(Component.literal(spirit + " turns away from you. It will not answer until you earn its trust back.").withStyle(ChatFormatting.RED));
        else if (amount != 0 && p.getRandom().nextInt(3) == 0)
            p.displayClientMessage(Component.literal(spirit + (amount > 0 ? " approves" : " disapproves") + " (" + why.replace('_', ' ') + "). Trust " + after + "/100")
                    .withStyle(amount > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY), true);
    }

    /** Report something the player did; the spirit's trait decides how it feels about it (each action counts once a minute). */
    public static void notify(ServerPlayer p, String action) {
        CompoundTag d = data(p);
        Trait t = trait(d.getString("nusmp_spirit_trait"));
        if (t == null) return;
        boolean like = t.likes().contains(action), dislike = t.dislikes().contains(action);
        if (!like && !dislike) return;
        long now = p.level().getGameTime(), last = p.getPersistentData().getLong("nusmp_trust_" + action);
        if (now - last < 1200) return;
        p.getPersistentData().putLong("nusmp_trust_" + action, now);
        addTrust(p, like ? 2 : -3, action);
    }

    /** Can the spirit be called? (bonded spirits below 10 Trust refuse) */
    public static boolean willAnswer(Player p) { return !bonded(p) || trust(p) >= 10; }

    // ---------------------------------------------------------------- Over-Pull strain
    /** A heavy spirit use (Spirit Dive / Spirit Channeling). Returns false if the spirit has dissolved. */
    public static void heavyUse(ServerPlayer p) {
        notify(p, "channel_element");
        if (incarnate(p)) return;                    // an incarnated spirit costs you nothing
        CompoundTag d = data(p);
        long day = p.level().getDayTime() / 24000L;
        if (d.getLong("nusmp_pull_day") != day) {
            if (d.getInt("nusmp_pull_uses") == 0) { d.putInt("nusmp_spirit_fatigue", Math.max(0, d.getInt("nusmp_spirit_fatigue") - 1)); notify(p, "rest_0_overpull"); }
            d.putLong("nusmp_pull_day", day);
            d.putInt("nusmp_pull_uses", 0);
        }
        int uses = d.getInt("nusmp_pull_uses") + 1;
        d.putInt("nusmp_pull_uses", uses);
        int stage = d.getInt("nusmp_spirit_fatigue");
        if (stage >= 2) p.hurt(p.damageSources().magic(), 4f);        // Fractured: every heavy pull recoils
        if (uses > 10 && p.getRandom().nextDouble() < (uses - 10) * 0.08) {
            stage++;
            d.putInt("nusmp_spirit_fatigue", stage);
            notify(p, "over_pull");
            switch (stage) {
                case 1 -> { p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2400, 1));
                    p.sendSystemMessage(Component.literal("Over-pull strain: Weakened. Your spirit needs rest.").withStyle(ChatFormatting.YELLOW)); }
                case 2 -> p.sendSystemMessage(Component.literal("Over-pull strain: Fractured. Every heavy pull now tears at you.").withStyle(ChatFormatting.RED));
                default -> dissolve(p, d);
            }
        }
        save(p, d);
    }

    /** Stage 3: the spirit dissolves. The bond, trust and slot are lost. */
    private static void dissolve(ServerPlayer p, CompoundTag d) {
        String spirit = d.getString("nusmp_spirit_kind");
        for (String k : new String[]{"nusmp_spirit_trait", "nusmp_spirit_trust", "nusmp_spirit_incarnate", "nusmp_spirit_name", "nusmp_spirit_fatigue", "nusmp_spirit_kind"}) d.remove(k);
        SpiritSlots slots = SpiritSlots.get(p.getServer());
        if (p.getUUID().equals(slots.owner(spirit))) slots.release(spirit);
        p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(spirit + " has dissolved from over-pulling. It is free to choose another mage.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
    }

    /** Incarnation: needs Trust 100 and a Spirit Charm renamed (its name becomes the spirit's True Name). */
    public static boolean incarnate(ServerPlayer p, net.minecraft.world.item.ItemStack charm) {
        if (incarnate(p)) return true;
        if (trust(p) < 100) {
            p.displayClientMessage(Component.literal(data(p).getString("nusmp_spirit_kind") + " does not trust you enough to take a body (" + trust(p) + "/100).").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (!charm.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            p.displayClientMessage(Component.literal("Give it a True Name first: rename a Spirit Charm at an anvil.").withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        CompoundTag d = data(p);
        d.putBoolean("nusmp_spirit_incarnate", true);
        d.putString("nusmp_spirit_name", charm.getHoverName().getString());
        save(p, d);
        charm.shrink(1);
        p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString() + "'s spirit takes a body and the name " + trueName(p) + ".").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        return true;
    }

    /** Admin incarnation (/multiverse spirit incarnate): gives the bonded spirit a body and True Name without trust or a charm. */
    public static boolean forceIncarnate(ServerPlayer p, String name) {
        CompoundTag d = data(p);
        if (!d.contains("nusmp_spirit_kind") && !com.newuniverse.nusmp.multiverse.SpiritLordSkill.instance(p).isPresent()) return false;
        d.putBoolean("nusmp_spirit_incarnate", true);
        d.putString("nusmp_spirit_name", name);
        d.putInt("nusmp_spirit_trust", Math.max(d.getInt("nusmp_spirit_trust"), 100));
        save(p, d);
        p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString() + "'s spirit takes a body and the name " + name + ".").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        return true;
    }

    // ---------------------------------------------------------------- what the spirit notices
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || !bonded(p)) return;
        LivingEntity dead = e.getEntity();
        p.getPersistentData().putLong("nusmp_last_kill", p.level().getGameTime());
        if (dead instanceof Player) { notify(p, "kill_player"); notify(p, "pvp"); }
        else if (dead instanceof AbstractVillager) notify(p, "kill_innocent");
        else if (dead instanceof TamableAnimal ta && ta.isTame()) notify(p, "kill_tamed");
        else if (dead instanceof Animal) notify(p, "kill_animal");
        else if (dead instanceof Enemy) notify(p, "kill_mob");
        if (com.newuniverse.nusmp.balance.BalanceLaw.isBoss(dead)) notify(p, "kill_boss");
        if (p.getHealth() < p.getMaxHealth() * 0.3f) notify(p, "low_hp_kill");
        if (dead.isOnFire()) notify(p, "fire_kill");
        if (dead.isInWater()) notify(p, "water_kill");
        if (dead.hasEffect(MobEffects.WITHER)) notify(p, "wither_kill");
        if (dead.hasEffect(MobEffects.INVISIBILITY) || p.hasEffect(MobEffects.INVISIBILITY)) notify(p, "stealth_kill");
    }

    public static void onHurt(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !bonded(p)) return;
        var s = e.getSource();
        if (s.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) notify(p, "fall_damage");
        else if (s.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) notify(p, "fire_damage");
        else if (s.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)) notify(p, "water_damage");
    }

    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p) || !bonded(p)) return;
        var st = e.getState();
        notify(p, "mine");
        if (st.is(Tags.Blocks.ORES)) { notify(p, "mine_ore"); notify(p, "find_ore"); }
        if (st.is(Tags.Blocks.ORES_DIAMOND)) notify(p, "find_diamond");
        if (st.is(BlockTags.LOGS)) { notify(p, "deforest"); notify(p, "break_natural"); }
        if (st.is(BlockTags.CROPS)) notify(p, "harvest");
        if (st.is(Blocks_BOOKSHELF())) notify(p, "destroy_book");
    }
    private static net.minecraft.world.level.block.Block Blocks_BOOKSHELF() { return net.minecraft.world.level.block.Blocks.BOOKSHELF; }

    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !bonded(p)) return;
        var st = e.getPlacedBlock();
        notify(p, "place_block");
        if (st.is(BlockTags.SAPLINGS)) { notify(p, "plant_tree"); notify(p, "plant"); }
        if (st.is(BlockTags.CROPS)) { notify(p, "grow_crop"); notify(p, "plant"); }
        if (st.is(Tags.Blocks.STONES) || st.is(BlockTags.STONE_BRICKS)) notify(p, "build_stone");
        if (st.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) notify(p, "lava_place");
    }

    public static void onUseFinish(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !bonded(p)) return;
        var item = e.getItem();
        if (item.is(ItemTags.MEAT)) notify(p, "eat_meat");
        if (item.getItem() instanceof net.minecraft.world.item.PotionItem) notify(p, "potion_use");
    }

    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && bonded(p)) { notify(p, "cross_dimension"); notify(p, "dimension_hop"); }
    }
    public static void onWake(PlayerWakeUpEvent e) { if (e.getEntity() instanceof ServerPlayer p && bonded(p)) { notify(p, "sleep"); notify(p, "bed_use"); } }
    public static void onFish(ItemFishedEvent e) { if (e.getEntity() instanceof ServerPlayer p && bonded(p)) notify(p, "fish"); }
    public static void onTrade(TradeWithVillagerEvent e) { if (e.getEntity() instanceof ServerPlayer p && bonded(p)) notify(p, "trade"); }
    public static void onTame(AnimalTameEvent e) { if (e.getTamer() instanceof ServerPlayer p && bonded(p)) notify(p, "tame_animal"); }
    public static void onBreed(BabyEntitySpawnEvent e) { if (e.getCausedByPlayer() instanceof ServerPlayer p && bonded(p)) { notify(p, "breed"); notify(p, "breed_animal"); } }

    /** Every 5 minutes: surroundings and habits. */
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 6000 != 0 || !bonded(p)) return;
        var lvl = p.serverLevel();
        if (p.getY() > 200) notify(p, "high_altitude");
        if (p.getY() < 0) notify(p, "underground");
        if (lvl.isDay() && lvl.canSeeSky(p.blockPosition())) notify(p, "sunlight");
        if (lvl.isThundering()) { notify(p, "thunder"); notify(p, "storm_weather"); }
        else if (lvl.isRaining()) notify(p, "rain");
        if (lvl.getGameTime() - p.getPersistentData().getLong("nusmp_last_kill") > 6000) notify(p, "peaceful_5min");
        var last = p.getPersistentData();
        double dx = p.getX() - last.getDouble("nusmp_bond_x"), dz = p.getZ() - last.getDouble("nusmp_bond_z");
        double moved = Math.sqrt(dx * dx + dz * dz);
        if (moved < 2) { notify(p, "stand_still_5min"); notify(p, "afk"); }
        if (moved > 1000) notify(p, "travel_1000b");
        if (moved > 64) notify(p, "new_chunk");
        last.putDouble("nusmp_bond_x", p.getX()); last.putDouble("nusmp_bond_z", p.getZ());
    }
}
