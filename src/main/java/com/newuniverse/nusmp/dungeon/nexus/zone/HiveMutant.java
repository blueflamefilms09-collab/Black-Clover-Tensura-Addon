package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Kind;
import com.newuniverse.nusmp.dungeon.wild.Dweller;
import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * The members of the swarm of the Tempest Mirage, made out of the real mobs of Tensura (docs/living_dungeon.md section 5, zone 3): orcs,
 * lizardmen, slimes, goblins and hound dogs, looked up by id at run time, grown by attribute modifiers into "mutated" versions (named,
 * outlined, tougher by a finite amount, see {@link HiveRules}), and marked in their persistent data with the Nexus they belong to, so a
 * restart finds them again. This class makes and marks them; {@link HiveDriver} runs them.
 */
public final class HiveMutant {
    private HiveMutant() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** Persistent-data keys: the key of the Nexus it belongs to, its kind (the ordinal), and the block it was born on. */
    static final String KEY = "nusmp_hive", KIND = "nusmp_hive_kind", HOME = "nusmp_hive_home";
    /** What its hunger is held at, so the Dungeon's food web neither starves it nor sends it off to graze or to hunt its own. */
    private static final int HUNGER = 85;
    private static final Set<String> WARNED = new HashSet<>();

    // ---------------------------------------------------------------- finding out
    public static boolean is(Entity e) { return e instanceof Mob && e.getPersistentData().contains(KEY); }

    /** The key of the Nexus (NexusCells.key of its core cell) this member belongs to. */
    public static long colonyOf(Entity e) { return e.getPersistentData().getLong(KEY); }

    public static Kind kindOf(Entity e) {
        Kind[] all = Kind.values();
        return all[Math.max(0, Math.min(e.getPersistentData().getInt(KIND), all.length - 1))];
    }

    /** The block it was born on: where it wanders back to. */
    public static BlockPos homeOf(Entity e) { return e.getPersistentData().contains(HOME) ? BlockPos.of(e.getPersistentData().getLong(HOME)) : e.blockPosition(); }

    // ---------------------------------------------------------------- making one
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path); }

    /**
     * A mutant of this kind standing at `at` (feet), made and marked but not yet added to the world: finalized as an event spawn so the
     * Dungeon's spawn hook leaves it alone. Null if the type is not registered (Tensura missing) or it could not be made. {@code roll} (0 to 1)
     * varies the individual a little.
     */
    public static Mob create(ServerLevel level, Kind kind, Vec3 at, long colony, BlockPos home, double roll) {
        EntityType<?> type = Wildlife.typeOf(HiveRules.typeId(kind));
        if (type == null) {
            if (WARNED.add(kind.name())) LOG.warn("[nusmp] The Tempest Mirage has no {}: {} is not registered", HiveRules.name(kind), HiveRules.typeId(kind));
            return null;
        }
        try {
            Entity made = type.create(level);
            if (!(made instanceof Mob m)) {
                if (made != null) made.discard();
                return null;
            }
            m.moveTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360f, 0f);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.EVENT, null);
            m.setPersistenceRequired();
            mutate(m, kind, roll);
            m.getPersistentData().putLong(KEY, colony);
            m.getPersistentData().putInt(KIND, kind.ordinal());
            m.getPersistentData().putLong(HOME, home.asLong());
            pinHunger(m);
            return m;
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.add(kind.name() + ex.getClass().getName())) LOG.warn("[nusmp] Could not make a {} for the Tempest Mirage", HiveRules.name(kind), ex);
            return null;
        }
    }

    /** Adds a made mutant to the world; false if the level would not take it. */
    public static boolean add(ServerLevel level, Mob m) { return level.addFreshEntity(m); }

    /** Grows the mob into its mutation: more health, a little more bite, its pace, its size, its name, an outline. */
    static void mutate(Mob m, Kind kind, double roll) {
        HiveRules.Stats s = HiveRules.mutate(kind, roll);
        grow(m, Attributes.MAX_HEALTH, "hive_health", s.health(), HiveRules.MAX_HEALTH);
        grow(m, Attributes.ATTACK_DAMAGE, "hive_attack", s.attack(), HiveRules.MAX_ATTACK);
        scale(m, Attributes.MOVEMENT_SPEED, "hive_pace", s.speed());
        scale(m, Attributes.SCALE, "hive_size", s.scale());
        m.setHealth(m.getMaxHealth());
        m.setCustomName(Component.literal(HiveRules.name(kind)));
        m.setCustomNameVisible(false);
        m.setGlowingTag(true);
    }

    /** Raises an attribute by the factor, but never past the cap and never below what it was (a replaced modifier of an earlier call does not stack). */
    private static void grow(Mob m, Holder<Attribute> attribute, String name, double factor, double cap) {
        AttributeInstance a = m.getAttribute(attribute);
        if (a == null) return;
        a.removeModifier(id(name));
        double base = a.getValue(), target = HiveRules.total(base, factor, cap);
        if (target > base) a.addPermanentModifier(new AttributeModifier(id(name), target - base, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void scale(Mob m, Holder<Attribute> attribute, String name, double factor) {
        AttributeInstance a = m.getAttribute(attribute);
        if (a == null) return;
        a.removeModifier(id(name));
        a.addPermanentModifier(new AttributeModifier(id(name), factor - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    /** The Dungeon's food web must not take a mutant for one of its own: its hunger is held where it neither starves, forages, hunts nor falls asleep. */
    static void pinHunger(Mob m) {
        try {
            if (m.hasData(Dweller.DWELLER.get())) m.getData(Dweller.DWELLER).appetite.set(HUNGER);
        } catch (RuntimeException | LinkageError ignored) {
            // a mob the food web does not know has nothing to pin
        }
    }
}
