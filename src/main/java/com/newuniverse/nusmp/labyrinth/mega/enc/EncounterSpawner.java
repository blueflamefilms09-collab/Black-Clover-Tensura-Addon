package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Why: putting one monster into the Mega-Dungeon safely is a dozen small decisions that every spawn must repeat identically, so they live in one
 * place. The monster is made with the type's own factory and finalized as an EVENT spawn (a direct finalize does not fire the spawn event, so the
 * dungeon's other hooks leave the monster alone, the pattern the Living Dungeon's wildlife uses), after the position was proven: the chunk and its
 * neighbours are loaded (a spawn never loads anything), a solid floor is under the feet, no water or lava and no magma or cactus there, the
 * monster's box collides with nothing, the place is still at least the minimum distance from every party member and still out of their sight. A
 * cell chosen by the picker may be crowded by the group already placed, so a few places beside it are tried too; each trial counts against a
 * shared attempt budget, which is what bounds the work of one roll. The monster then gets the Dungeon Master's stamp (an entity tag, the entry
 * id and the birth time in its persistent data, so a sweep and the admin commands can find it again after a restart) and its threat scaling,
 * two attribute modifiers on health and damage that are added before the monster joins the world, so the Labyrinth's health buffer, which runs
 * on join, multiplies the scaled value and fills the bar once. Persistence is left alone: a monster nobody is near despawns by itself, and the
 * sweep covers the cases the game's own rule misses. Nothing here throws into the caller; a failing type is logged once and skipped.
 */
final class EncounterSpawner {
    private EncounterSpawner() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** The entity tag of every monster the Dungeon Master made: the contract's own constant, which the economy, the health buffer and the faction drops ask for. */
    static final String TAG = MegaDungeon.DM_TAG;
    /** Persistent data: the id of the table entry that made it, and the game time it was made. */
    static final String ENTRY = "nusmp_dm_entry", BORN = "nusmp_dm_born";
    static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "mega_dm_health"), DAMAGE_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "mega_dm_damage");
    private static final Set<String> WARNED = new HashSet<>();
    /** The places tried round a cell, nearest first. */
    private static final int[][] OFFSETS = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {2, 0}, {-2, 0}, {0, 2}, {0, -2}};

    /** What a placement must keep true: the party's eyes, the distance band, how to tell if the party sees the place, and the shared attempt counter. */
    record Rules(List<SpawnPicker.Eye> eyes, double minDist, double maxDist, SpawnPicker.LineOfSight sight, int[] attempts) {}

    /** Makes one monster of {@code type} at or beside {@code cell}; null when nothing fits (the attempt budget is spent, the chunk is not loaded, the place is bad). */
    static Mob spawn(ServerLevel level, EntityType<?> type, BlockPos cell, Rules rules, int threat, String faction, String entryId, Random rnd) {
        Mob mob = null;
        try {
            Entity made = type.create(level);
            if (!(made instanceof Mob m)) {
                if (made != null) made.discard();
                return null;
            }
            mob = m;
            if (placeNear(level, mob, cell, rules, rnd)) {
                finish(level, mob, mob.blockPosition(), threat, faction, entryId);
                if (level.addFreshEntity(mob)) return mob;
            }
        } catch (RuntimeException | LinkageError e) {
            String key = Bestiary.id(type) + ":" + e.getClass().getName();
            if (WARNED.add(key)) LOG.warn("[nusmp] The Dungeon Master could not spawn {}: {}", Bestiary.id(type), e.toString());
        }
        if (mob != null) mob.discard();
        return null;
    }

    /** Tries the cell and the places beside it, nearest first, each counting against the shared attempt budget; leaves the monster standing on the first that fits. */
    static boolean placeNear(ServerLevel level, Mob mob, BlockPos cell, Rules rules, Random rnd) {
        for (int[] o : OFFSETS) {
            if (rules.attempts()[0] <= 0) return false;
            rules.attempts()[0]--;
            if (place(level, mob, cell.offset(o[0], 0, o[1]), rules, rnd)) return true;
        }
        return false;
    }

    /** Puts the monster at the block and says whether the place is acceptable. Leaves it there when it is. */
    private static boolean place(ServerLevel level, Mob mob, BlockPos p, Rules rules, Random rnd) {
        if (!level.hasChunksAt(p.offset(-2, -1, -2), p.offset(2, 3, 2))) return false;
        BlockPos below = p.below();
        BlockState floor = level.getBlockState(below);
        if (floor.getCollisionShape(level, below).isEmpty() || floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CACTUS) || floor.is(Blocks.CAMPFIRE) || floor.is(Blocks.SOUL_CAMPFIRE)) return false;
        if (!level.getFluidState(p).isEmpty() || !level.getFluidState(p.above()).isEmpty()) return false;
        BlockState here = level.getBlockState(p);
        if (here.is(Blocks.FIRE) || here.is(Blocks.SOUL_FIRE) || here.is(Blocks.COBWEB) || here.is(Blocks.POWDER_SNOW) || here.is(Blocks.SWEET_BERRY_BUSH)) return false;
        mob.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, rnd.nextFloat() * 360f, 0f);
        if (!level.noCollision(mob)) return false;
        double near = SpawnPicker.nearest(new SpawnPicker.Cell(p.getX(), p.getY(), p.getZ(), 1.0, 0, false), rules.eyes());
        if (near < rules.minDist() - 0.01 || near > rules.maxDist() + 3.0) return false;
        if (rules.sight() != null) {
            for (SpawnPicker.Eye e : rules.eyes()) if (rules.sight().sees(e.x(), e.y(), e.z(), p.getX(), p.getY(), p.getZ())) return false;
        }
        return true;
    }

    /** The Dungeon Master's stamp and the threat scaling; the monster is not yet in the world. */
    private static void finish(ServerLevel level, Mob mob, BlockPos p, int threat, String faction, String entryId) {
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.EVENT, null);
        mob.addTag(TAG);
        if (faction != null && !faction.isEmpty()) mob.addTag(TAG + "_" + faction);
        mob.getPersistentData().putString(ENTRY, entryId);
        mob.getPersistentData().putLong(BORN, level.getGameTime());
        scale(mob, ThreatScale.amount(threat, EncConfig.get(EncConfig.HEALTH_PER_LEVEL)), ThreatScale.amount(threat, EncConfig.get(EncConfig.DAMAGE_PER_LEVEL)));
    }

    /** Health and damage scaling as permanent multiplicative modifiers (they survive a chunk unload with the monster), health refilled to the new maximum. */
    static void scale(Mob mob, double health, double damage) {
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && health > 0.0) {
            hp.addOrReplacePermanentModifier(new AttributeModifier(HEALTH_ID, health, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            mob.setHealth(mob.getMaxHealth());
        }
        AttributeInstance dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null && damage > 0.0) dmg.addOrReplacePermanentModifier(new AttributeModifier(DAMAGE_ID, damage, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** Is this one of the Dungeon Master's monsters? */
    static boolean isMine(Entity e) { return e.getTags().contains(TAG); }

    static void forgetWarnings() { WARNED.clear(); }
}
