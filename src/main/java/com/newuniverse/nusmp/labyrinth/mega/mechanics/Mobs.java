package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Why: the primitives that spawn monsters (a band, an ambush, guards, a swarm, an elite, the wardens a counter needs, a mimic, a doppelganger) all need the same dozen careful
 * steps, so they live here once, after the pattern of the Dungeon Master's spawner: the type comes from the data-driven roster ({@link MobGroups}) and is checked for existence
 * (so a roster may name the foundry's mobs and a missing mod simply skips them), the position is a free, floored, fluid-free cell inside the mechanic's area and never in a safe
 * room, the monster is finalized as an EVENT spawn, scaled by depth with permanent attribute modifiers, stamped with the entity tags that make the rest of the mod treat it as a
 * dungeon monster (the Dungeon Master's tag, so the economy and the health buffer apply) and the instance's own tag (so the instance can find and remove it), and it is
 * remembered in the instance. Nothing here throws into the caller.
 */
final class Mobs {
    private Mobs() {}

    private static final Logger LOG = LogUtils.getLogger();
    static final String TAG = "nusmp_mech";
    static final ResourceLocation HP_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "mech_hp"), DMG_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "mech_dmg");
    private static final Set<String> WARNED = new HashSet<>();
    private static final int[][] RING = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}, {2, 0}, {-2, 0}, {0, 2}, {0, -2}};

    /** Is a cell fit for a monster (floored, free, dry, inside the area, not in a safe room)? */
    static boolean fit(Instance in, BlockPos p) {
        if (!in.level.hasChunksAt(p.offset(-1, -1, -1), p.offset(1, 3, 1))) return false;
        if (!Safety.allowedAt(in.level, p) || !in.area.contains(p)) return false;
        BlockState floor = in.level.getBlockState(p.below());
        if (floor.getCollisionShape(in.level, p.below()).isEmpty() || floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CACTUS)) return false;
        if (!in.level.getBlockState(p).getCollisionShape(in.level, p).isEmpty() || !in.level.getBlockState(p.above()).getCollisionShape(in.level, p.above()).isEmpty()) return false;
        return in.level.getFluidState(p).isEmpty();
    }

    /** Up to {@code n} free cells on a ring of about {@code ring} blocks round the anchor (shuffled with the instance's rng). */
    static List<BlockPos> cells(Instance in, int ring, int n) {
        List<BlockPos> all = new ArrayList<>();
        int r = Math.max(2, ring);
        for (int a = 0; a < 24; a++) {
            double ang = a * Math.PI * 2 / 24 + in.rng.nextDouble() * 0.2;
            for (int dr = 0; dr <= 2; dr++) {
                int x = (int) Math.round(Math.cos(ang) * (r - dr)), z = (int) Math.round(Math.sin(ang) * (r - dr));
                BlockPos p = in.anchor.offset(x, 0, z);
                if (fit(in, p)) { all.add(p); break; }
            }
        }
        for (int i = all.size() - 1; i > 0; i--) {
            int j = in.rng.nextInt(i + 1);
            BlockPos t = all.get(i);
            all.set(i, all.get(j));
            all.set(j, t);
        }
        if (all.isEmpty() && fit(in, in.anchor)) all.add(in.anchor);
        return all.size() > n ? new ArrayList<>(all.subList(0, n)) : all;
    }

    /** Spawns a band round the anchor; returns the monsters made (fewer than asked when the room is cramped). */
    static List<Mob> band(Instance in, String group, int count, int ring, double hp, boolean hunt, boolean still) {
        List<Mob> out = new ArrayList<>();
        List<BlockPos> cells = cells(in, ring, Math.max(count, 1));
        if (cells.isEmpty()) return out;
        for (int i = 0; i < count; i++) {
            Mob m = one(in, group, cells.get(i % cells.size()), hp, still, hunt);
            if (m != null) out.add(m);
        }
        return out;
    }

    /** One monster of the group at or beside the cell; null when nothing fits. */
    static Mob one(Instance in, String group, BlockPos cell, double hp, boolean still, boolean hunt) {
        int tier = Scaling.tierOf(in.floor);
        MobGroups.Entry e = MobGroups.pick(group, tier, in.rng, x -> BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(x.id())));
        return make(in, e, cell, hp, still, hunt);
    }

    /** One monster of this exact entity id (a swarm kind, a doppelganger's zombie); null when nothing fits. */
    static Mob oneId(Instance in, String entityId, BlockPos cell, double hp, boolean still, boolean hunt) {
        return make(in, new MobGroups.Entry(entityId, "", 1, 1, false), cell, hp, still, hunt);
    }

    private static Mob make(Instance in, MobGroups.Entry e, BlockPos cell, double hp, boolean still, boolean hunt) {
        if (e == null) return null;
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(e.id()));
        if (type.isEmpty()) return null;
        Mob mob = null;
        try {
            Entity made = type.get().create(in.level);
            if (!(made instanceof Mob m)) {
                if (made != null) made.discard();
                return null;
            }
            mob = m;
            if (!e.variant().isEmpty()) {
                try {
                    CompoundTag t = new CompoundTag();
                    mob.saveWithoutId(t);
                    t.putString("variant", e.variant());
                    mob.load(t);
                } catch (RuntimeException variantFailed) {
                    // the foundry's variant key is theirs to define; a mob that refuses it stays as it was made
                }
            }
            BlockPos p = cell;
            boolean placed = false;
            for (int[] o : RING) {
                BlockPos q = cell.offset(o[0], 0, o[1]);
                if (!fit(in, q)) continue;
                mob.moveTo(q.getX() + 0.5, q.getY(), q.getZ() + 0.5, in.rng.nextFloat() * 360f, 0f);
                if (in.level.noCollision(mob)) { placed = true; p = q; break; }
            }
            if (!placed) { mob.discard(); return null; }
            mob.finalizeSpawn(in.level, in.level.getCurrentDifficultyAt(p), MobSpawnType.EVENT, null);
            mob.addTag(MegaDungeon.DM_TAG);
            mob.addTag(TAG);
            mob.addTag(in.mobTag());
            double s = Scaling.s(in.floor);
            scale(mob, (hp - 1.0) + 0.45 * s * in.difficulty, 0.2 * s * in.difficulty);
            if (still && mob instanceof PathfinderMob pm) {
                pm.restrictTo(p, 4);
                pm.setPersistenceRequired();
            }
            if (!in.level.addFreshEntity(mob)) { mob.discard(); return null; }
            in.mobs.add(mob.getUUID());
            if (hunt) {
                ServerPlayer target = nearest(in, mob);
                if (target != null) mob.setTarget(target);
            }
            return mob;
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.add(e.id() + ex.getClass().getName())) LOG.warn("[nusmp] mechanics could not spawn {}: {}", e.id(), ex.toString());
            if (mob != null) mob.discard();
            return null;
        }
    }

    static ServerPlayer nearest(Instance in, Entity from) {
        ServerPlayer best = null;
        double bd = Double.MAX_VALUE;
        for (ServerPlayer p : in.inside) {
            if (!Safety.affects(p)) continue;
            double d = p.distanceToSqr(from);
            if (d < bd) { bd = d; best = p; }
        }
        return best;
    }

    static void scale(Mob mob, double health, double damage) {
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && health > 0.0) {
            hp.addOrReplacePermanentModifier(new AttributeModifier(HP_ID, health, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            mob.setHealth(mob.getMaxHealth());
        }
        AttributeInstance dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null && damage > 0.0) dmg.addOrReplacePermanentModifier(new AttributeModifier(DMG_ID, damage, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
