package com.newuniverse.nusmp.elite.nations.world;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.elite.nations.core.GuardRules;
import com.newuniverse.nusmp.elite.nations.core.Nation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the NATION SPAWNER TRACKER ("nation spawner trackers, entity tags, tier scaling"). A nation with a capital keeps a bounded number of guards patrolling it. Which mobs come from the entity-type tags
 * {@code nusmp:nation_guard_tier_1..5} (and the fallback tag {@code nusmp:nation_guard}), so a datapack decides the guard roster using any existing mob; how many and how tough comes from the nation level
 * ({@link GuardRules}). Every guard carries a marker in its persistent data (nation id, tier), so after a restart the tracker re-adopts the survivors and discards the orphans of a disbanded nation. Guards
 * are re-targeted: they attack hostile monsters and the people their nation treats as enemies (at war, or hated), never their own members or allies. Spawns only happen while a player is near (no chunk
 * loading), at most a few per round, and never past the per-nation and global caps. Server thread only.
 */
public final class GuardService {
    private GuardService() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final Random RND = new Random();
    public static final String KEY_NATION = "nusmp_guard_nation", KEY_TIER = "nusmp_guard_tier";
    public static final TagKey<EntityType<?>> TAG_ALL = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", "nation_guard"));

    /** nation id to the entities currently loaded and alive (runtime only). */
    private static final Map<String, Set<UUID>> ALIVE = new HashMap<>();

    public static TagKey<EntityType<?>> tierTag(int tier) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", "nation_guard_tier_" + Math.max(1, Math.min(GuardRules.MAX_TIER, tier))));
    }

    // ---------------------------------------------------------------- markers and the registry of loaded guards
    public static String nationOf(Entity e) {
        CompoundTag t = e.getPersistentData();
        return t.contains(KEY_NATION) ? t.getString(KEY_NATION) : "";
    }

    public static boolean isGuard(Entity e) { return !nationOf(e).isEmpty(); }

    public static int aliveCount(String nationId) {
        Set<UUID> s = ALIVE.get(nationId);
        return s == null ? 0 : s.size();
    }

    public static int totalAlive() {
        int n = 0;
        for (Set<UUID> s : ALIVE.values()) n += s.size();
        return n;
    }

    public static void forgetAll() { ALIVE.clear(); }

    /** A marked mob entered a level (spawned or loaded): adopt it, or discard it when its nation is gone. Returns false when it must be discarded. */
    public static boolean onJoin(MinecraftServer s, Entity e) {
        String nid = nationOf(e);
        if (nid.isEmpty()) return true;
        NationsData d = NationService.data(s);
        Nation n = d.nations.get(nid);
        if (n == null || !n.hasCapital || !NationsConfig.enabled() || !NationsConfig.get(NationsConfig.GUARDS_ENABLED)) return false;
        Set<UUID> set = ALIVE.computeIfAbsent(nid, k -> new HashSet<>());
        if (set.size() >= GuardRules.desired(n.level, NationsConfig.get(NationsConfig.GUARD_BASE), NationsConfig.get(NationsConfig.GUARD_PER_LEVEL), NationPerks.guardExtra(n), NationsConfig.get(NationsConfig.GUARD_HARD_CAP)) + 2 && !set.contains(e.getUUID())) return false;
        set.add(e.getUUID());
        if (e instanceof Mob mob) {
            retarget(s, mob, nid);
            if (mob instanceof PathfinderMob pm) pm.restrictTo(new BlockPos(n.cx, n.cy, n.cz), Math.max(8, NationsConfig.get(NationsConfig.CAPITAL_RADIUS)));   // the leash is not saved with the mob
        }
        return true;
    }

    public static void onLeave(Entity e) {
        String nid = nationOf(e);
        if (nid.isEmpty()) return;
        Set<UUID> set = ALIVE.get(nid);
        if (set != null) { set.remove(e.getUUID()); if (set.isEmpty()) ALIVE.remove(nid); }
    }

    /** Removes every guard of a nation that is loaded right now. Unloaded ones are discarded when their chunk loads (see onJoin). */
    public static void discardAll(MinecraftServer s, String nationId) {
        Set<UUID> set = ALIVE.remove(nationId);
        if (set == null) return;
        for (UUID id : set) {
            for (ServerLevel lv : s.getAllLevels()) {
                Entity e = lv.getEntity(id);
                if (e != null) { e.discard(); break; }
            }
        }
    }

    // ---------------------------------------------------------------- targeting
    /** Replaces the target goals of a guard: strike back, hit hostile monsters, and hit whoever the nation is hostile to. */
    private static void retarget(MinecraftServer s, Mob mob, String nationId) {
        mob.targetSelector.removeAllGoals(g -> true);
        if (mob instanceof PathfinderMob pm) mob.targetSelector.addGoal(1, new HurtByTargetGoal(pm).setAlertOthers());
        mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, ServerPlayer.class, 10, true, false, t -> t instanceof ServerPlayer sp && WarService.guardsHostileTo(s, nationId, sp)));
        mob.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(mob, LivingEntity.class, 10, true, false, t -> t instanceof Enemy && !isGuard(t) && !(t instanceof ServerPlayer)));
    }

    // ---------------------------------------------------------------- the spawn round
    /** One pass over all nations: tops each capital's guards up to the wanted number. */
    public static void tick(MinecraftServer s, long now) {
        if (!NationsConfig.get(NationsConfig.GUARDS_ENABLED)) return;
        NationsData d = NationService.data(s);
        long interval = NationsConfig.seconds(NationsConfig.GUARD_INTERVAL_SECONDS);
        for (Nation n : d.nations.values()) {
            if (!n.hasCapital || now - n.lastGuardTick < interval) continue;
            n.lastGuardTick = now;
            try { spawnRound(s, n); } catch (RuntimeException e) { LOG.warn("[nusmp] guard round for {} failed: {}", n.id, e.toString()); }
        }
    }

    private static ServerLevel levelOf(MinecraftServer s, String dim) {
        for (ServerLevel lv : s.getAllLevels()) if (lv.dimension().location().toString().equals(dim)) return lv;
        return null;
    }

    private static void spawnRound(MinecraftServer s, Nation n) {
        ServerLevel level = levelOf(s, n.capitalDim);
        BlockPos cap = new BlockPos(n.cx, n.cy, n.cz);
        if (level == null || !level.isLoaded(cap)) return;
        double range = NationsConfig.get(NationsConfig.GUARD_ACTIVE_RANGE);
        boolean near = false;
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(n.cx + 0.5, n.cy, n.cz + 0.5) <= range * range) { near = true; break; }
        if (!near) return;
        pruneDead(s, n.id);
        int want = GuardRules.desired(n.level, NationsConfig.get(NationsConfig.GUARD_BASE), NationsConfig.get(NationsConfig.GUARD_PER_LEVEL), NationPerks.guardExtra(n), NationsConfig.get(NationsConfig.GUARD_HARD_CAP));
        int count = GuardRules.toSpawn(want, aliveCount(n.id), NationsConfig.get(NationsConfig.GUARD_PER_ROUND), totalAlive(), NationsConfig.get(NationsConfig.GUARD_GLOBAL_CAP));
        int tier = GuardRules.tierOf(n.level);
        for (int i = 0; i < count; i++) spawnOne(s, level, n, cap, tier);
    }

    /** Spawns up to {@code count} guards for the nation right now (admin / self-test), ignoring the timer and the nearby-player rule but not the caps. Returns how many were added. */
    public static int spawnNow(MinecraftServer s, Nation n, int count) {
        if (!n.hasCapital || !NationsConfig.get(NationsConfig.GUARDS_ENABLED)) return 0;
        ServerLevel level = levelOf(s, n.capitalDim);
        BlockPos cap = new BlockPos(n.cx, n.cy, n.cz);
        if (level == null || !level.isLoaded(cap)) return 0;
        int before = aliveCount(n.id);
        int want = GuardRules.desired(n.level, NationsConfig.get(NationsConfig.GUARD_BASE), NationsConfig.get(NationsConfig.GUARD_PER_LEVEL), NationPerks.guardExtra(n), NationsConfig.get(NationsConfig.GUARD_HARD_CAP));
        int todo = Math.min(count, GuardRules.toSpawn(want, before, count, totalAlive(), NationsConfig.get(NationsConfig.GUARD_GLOBAL_CAP)));
        for (int i = 0; i < todo; i++) spawnOne(s, level, n, cap, GuardRules.tierOf(n.level));
        return aliveCount(n.id) - before;
    }

    public static boolean tagHasMembers(TagKey<EntityType<?>> tag) {
        Optional<HolderSet.Named<EntityType<?>>> set = BuiltInRegistries.ENTITY_TYPE.getTag(tag);
        return set.isPresent() && set.get().size() > 0;
    }

    private static void pruneDead(MinecraftServer s, String nationId) {
        Set<UUID> set = ALIVE.get(nationId);
        if (set == null) return;
        set.removeIf(id -> {
            for (ServerLevel lv : s.getAllLevels()) {
                Entity e = lv.getEntity(id);
                if (e != null) return !e.isAlive();
            }
            return true;
        });
    }

    private static EntityType<?> pick(int tier) {
        EntityType<?> t = pickFrom(tierTag(tier));
        return t != null ? t : pickFrom(TAG_ALL);
    }

    private static EntityType<?> pickFrom(TagKey<EntityType<?>> tag) {
        Optional<HolderSet.Named<EntityType<?>>> set = BuiltInRegistries.ENTITY_TYPE.getTag(tag);
        if (set.isEmpty() || set.get().size() == 0) return null;
        List<Holder<EntityType<?>>> all = new ArrayList<>();
        set.get().forEach(all::add);
        return all.get(RND.nextInt(all.size())).value();
    }

    private static void spawnOne(MinecraftServer s, ServerLevel level, Nation n, BlockPos cap, int tier) {
        EntityType<?> type = pick(tier);
        if (type == null) return;
        int radius = Math.max(8, NationsConfig.get(NationsConfig.CAPITAL_RADIUS));
        for (int attempt = 0; attempt < 6; attempt++) {
            double ang = RND.nextDouble() * Math.PI * 2, r = 4 + RND.nextDouble() * (radius - 6);
            int x = cap.getX() + (int) Math.round(Math.cos(ang) * r), z = cap.getZ() + (int) Math.round(Math.sin(ang) * r);
            if (!level.isLoaded(new BlockPos(x, cap.getY(), z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - cap.getY()) > 12) continue;
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() || !level.getFluidState(pos).isEmpty()) continue;
            Entity e = type.create(level);
            if (!(e instanceof Mob mob)) { if (e != null) e.discard(); return; }
            CompoundTag data = mob.getPersistentData();
            data.putString(KEY_NATION, n.id);
            data.putInt(KEY_TIER, tier);
            mob.moveTo(x + 0.5, y, z + 0.5, RND.nextFloat() * 360f, 0f);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null);
            mob.setPersistenceRequired();
            mob.setCustomName(Component.literal(n.name + " Guard"));
            mob.setCustomNameVisible(false);
            if (NationsConfig.get(NationsConfig.GUARD_SCALE_STATS)) scale(mob, tier);
            if (mob instanceof PathfinderMob pm) pm.restrictTo(cap, radius);
            level.addFreshEntity(mob);
            return;
        }
    }

    private static void scale(Mob mob, int tier) {
        modify(mob, Attributes.MAX_HEALTH, "guard_health", GuardRules.healthFactor(tier) - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modify(mob, Attributes.ATTACK_DAMAGE, "guard_damage", GuardRules.damageBonus(tier), AttributeModifier.Operation.ADD_VALUE);
        modify(mob, Attributes.ARMOR, "guard_armor", GuardRules.armorBonus(tier), AttributeModifier.Operation.ADD_VALUE);
        mob.setHealth(mob.getMaxHealth());
    }

    private static void modify(Mob mob, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, String id, double amount, AttributeModifier.Operation op) {
        if (amount == 0) return;
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst == null) return;
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("nusmp", id);
        inst.removeModifier(rl);
        inst.addPermanentModifier(new AttributeModifier(rl, amount, op));
    }
}
