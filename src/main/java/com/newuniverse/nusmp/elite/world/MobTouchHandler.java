package com.newuniverse.nusmp.elite.world;

import com.newuniverse.nusmp.elite.world.rules.MobTouch;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Why: the MOB REPLACEMENT handler. A share of the natural spawns of ordinary monsters in magicule-dense chunks is replaced by a stronger "magicule-touched" variant: the same kind of mob
 * (so every other mod's rules about it still hold) with a tier from the density ({@link MobTouch#tier}): more health, damage, speed and armor, an outline that glows (config) and a name,
 * a faint Dark Rukh aura, and better drops (Magicule Shards, more experience). The chance and the tier come from the density of the spawn position, the table of who may be touched is
 * data ({@code mob_touch.json}). It never acts in a protected level (the Dungeon dimensions, the Hub) and never on a mob an operator spawned (command, egg) or a spawner made.
 * The calamity helpers carry a different tag and drop nothing.
 */
public final class MobTouchHandler {
    private MobTouchHandler() {}

    /** The persistent-data key of a touched mob (its tier, 1..3) and of a calamity helper (a flag). */
    public static final String TOUCHED = "nusmp_touched", ADD = "nusmp_calamity_add";

    private static final String MOD = "nusmp";
    private static final List<WeakReference<Mob>> LIVE = new ArrayList<>();

    public static void register(IEventBus gameBus) {
        gameBus.addListener(EventPriority.LOW, MobTouchHandler::onFinalizeSpawn);
        gameBus.addListener(MobTouchHandler::onJoin);
        gameBus.addListener(MobTouchHandler::onDrops);
        gameBus.addListener(MobTouchHandler::onXp);
        gameBus.addListener(MobTouchHandler::onServerTick);
    }

    public static int tierOf(net.minecraft.world.entity.Entity e) {
        CompoundTag t = e.getPersistentData();
        return t.contains(TOUCHED) ? t.getInt(TOUCHED) : 0;
    }

    public static boolean isHelper(net.minecraft.world.entity.Entity e) { return e.getPersistentData().getBoolean(ADD); }

    // ---------------------------------------------------------------- the spawn

    private static boolean natural(MobSpawnType t) { return t == MobSpawnType.NATURAL; }

    public static void onFinalizeSpawn(FinalizeSpawnEvent e) {
        try {
            if (e.isCanceled() || e.isSpawnCancelled() || !natural(e.getSpawnType()) || !WorldConfig.mobs()) return;
            ServerLevel level = e.getLevel().getLevel();
            if (!WorldGuard.allowed(level)) return;
            Mob mob = e.getEntity();
            if (!WorldData.mobs().allows(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString())) return;
            if (tierOf(mob) > 0) return;
            net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(e.getX(), e.getY(), e.getZ());
            double density = MagiculeApi.effective(level, pos);
            double chance = MobTouch.chance(density, WorldConfig.mobThreshold(), WorldConfig.mobChance(), WorldConfig.mobMaxChance());
            if (chance <= 0.0 || level.getRandom().nextDouble() >= chance) return;
            apply(mob, MobTouch.tier(density, WorldConfig.mobThreshold()));
        } catch (RuntimeException ex) {
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] elite world: the touched-mob handler failed: {}", ex.toString());
        }
    }

    /** Makes a mob magicule-touched at a tier (1..3). Public for the self-test and the commands. */
    public static void apply(Mob mob, int tier) {
        int t = Math.max(1, Math.min(3, tier));
        MobTouch.Stats st = WorldData.mobs().stats(t);
        mult(mob, Attributes.MAX_HEALTH, st.health() - 1.0, "touched_health");
        mult(mob, Attributes.ATTACK_DAMAGE, st.damage() - 1.0, "touched_damage");
        mult(mob, Attributes.MOVEMENT_SPEED, st.speed() - 1.0, "touched_speed");
        AttributeInstance armor = mob.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.addOrReplacePermanentModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath(MOD, "touched_armor"), st.armorAdd(), AttributeModifier.Operation.ADD_VALUE));
        mob.setHealth(mob.getMaxHealth());
        mob.getPersistentData().putInt(TOUCHED, t);
        mob.setCustomName(Component.translatable("nusmp.elite.touched", mob.getType().getDescription(), Component.translatable("nusmp.elite.tier." + t)));
        mob.setCustomNameVisible(false);
        if (WorldConfig.mobMarker()) mob.setGlowingTag(true);
        track(mob);
    }

    private static void mult(Mob mob, Holder<Attribute> attr, double amount, String id) {
        AttributeInstance a = mob.getAttribute(attr);
        if (a == null || amount == 0.0) return;
        a.addOrReplacePermanentModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath(MOD, id), amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static void track(Mob mob) {
        synchronized (LIVE) { LIVE.add(new WeakReference<>(mob)); }
    }

    /** A touched mob that loads from disk joins the aura list again. */
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof Mob m) || tierOf(m) <= 0) return;
        track(m);
    }

    // ---------------------------------------------------------------- the aura

    public static void onServerTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 40 != 0) return;
        synchronized (LIVE) {
            Iterator<WeakReference<Mob>> it = LIVE.iterator();
            while (it.hasNext()) {
                Mob m = it.next().get();
                if (m == null || m.isRemoved() || !m.isAlive()) {
                    it.remove();
                    continue;
                }
                if (m.level() instanceof ServerLevel sl && !sl.players().isEmpty() && sl.getNearestPlayer(m, 48.0) != null) RukhFx.aura(sl, m, RukhFx.Alignment.DARK);
            }
        }
    }

    // ---------------------------------------------------------------- the drops

    public static void onDrops(LivingDropsEvent e) {
        if (e.getEntity().level().isClientSide()) return;
        if (isHelper(e.getEntity())) {
            e.setCanceled(true);                                  // the Colossus's and the Lich's helpers drop nothing
            return;
        }
        int tier = tierOf(e.getEntity());
        if (tier <= 0) return;
        MobTouch.Stats st = WorldData.mobs().stats(tier);
        ServerLevel level = (ServerLevel) e.getEntity().level();
        int shards = 0;
        for (int i = 0; i < st.bonusDrops(); i++) if (level.getRandom().nextDouble() < 0.6) shards++;
        if (shards > 0) e.getDrops().add(new ItemEntity(level, e.getEntity().getX(), e.getEntity().getY(), e.getEntity().getZ(), new ItemStack(WorldItems.MAGICULE_SHARD.get(), shards)));
        if (tier >= 2 && level.getRandom().nextDouble() < 0.10 * tier) {
            Item crystal = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(tier >= 3 ? "tensura:medium_quality_magic_crystal" : "tensura:low_quality_magic_crystal")).orElse(null);
            if (crystal != null && crystal != net.minecraft.world.item.Items.AIR) e.getDrops().add(new ItemEntity(level, e.getEntity().getX(), e.getEntity().getY(), e.getEntity().getZ(), new ItemStack(crystal)));
        }
    }

    public static void onXp(LivingExperienceDropEvent e) {
        if (e.getEntity().level().isClientSide()) return;
        if (isHelper(e.getEntity())) {
            e.setDroppedExperience(0);
            return;
        }
        int tier = tierOf(e.getEntity());
        if (tier > 0) e.setDroppedExperience((int) Math.round(e.getDroppedExperience() * WorldData.mobs().stats(tier).xpMult()));
    }

    /** The registry id of an entity type (for logs). */
    public static String idOf(EntityType<?> t) { return BuiltInRegistries.ENTITY_TYPE.getKey(t).toString(); }
}
