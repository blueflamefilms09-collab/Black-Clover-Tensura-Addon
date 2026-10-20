package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.newuniverse.nusmp.dungeon.DungeonEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Holder;

import java.util.List;
import java.util.UUID;

/**
 * Why: the boss's reinforcements (the adds of a phase, the waves, what bursts out of a broken tombstone or root node) must be a closed, budgeted, harmless-to-the-world set. This class is the whole
 * of it: the closed list of kinds ({@code DecennialVocab.ADD_KINDS}) mapped to entity types, the make-and-tag of one add (a servant of the boss, no drops, no experience, a limited life for the
 * ones that can fly through walls, the strength of the level it stands on), and the count of the ones that live. An add carries the boss's id, so a party that wipes (or a boss that falls) takes
 * every one of them away with it, and one that leaves its hall is put back or discarded. None of the kinds applies a food-bar effect.
 */
final class AddKinds {
    private AddKinds() {}

    /** The tag every add carries (the events and the counts look for it), the servant tag the turf wars respect, and the persistent key that holds the boss's id. */
    static final String TAG = "nusmp_dec_add", SERVANT = "nusmp_servant", BOSS_KEY = "nusmp_dec_boss";
    private static final ResourceLocation HP = ResourceLocation.fromNamespaceAndPath("nusmp", "decennial_add_hp"), DMG = ResourceLocation.fromNamespaceAndPath("nusmp", "decennial_add_damage");

    static EntityType<? extends Mob> typeOf(String kind) {
        return switch (kind) {
            case "skeleton" -> EntityType.SKELETON;
            case "zombie" -> EntityType.ZOMBIE;
            case "stray" -> EntityType.STRAY;
            case "wither_skeleton" -> EntityType.WITHER_SKELETON;
            case "vex" -> EntityType.VEX;
            case "spider" -> EntityType.SPIDER;
            case "cave_spider" -> EntityType.CAVE_SPIDER;
            case "blaze" -> EntityType.BLAZE;
            case "witch" -> EntityType.WITCH;
            case "silverfish" -> EntityType.SILVERFISH;
            case "drowned" -> EntityType.DROWNED;
            case "bogged" -> EntityType.BOGGED;
            case "endermite" -> EntityType.ENDERMITE;
            default -> DungeonEntities.LIVING_ARMOR.get();
        };
    }

    /** The strength multipliers of the level an add stands on: health and damage grow with the depth of the boss, with a ceiling. */
    static double healthMult(int depth) { return 1.0 + Math.min(5.0, depth * 0.05); }

    static double damageMult(int depth) { return 1.0 + Math.min(2.5, depth * 0.025); }

    /** Makes one add at {@code (x, y, z)}, adds it to the level and returns it, or null when the game refused. */
    static Mob spawn(ServerLevel sl, String kind, double x, double y, double z, int depth, UUID boss, Player target) {
        EntityType<? extends Mob> type = typeOf(kind);
        Mob m = type.create(sl);
        if (m == null) return null;
        m.moveTo(x, y, z, sl.getRandom().nextFloat() * 360f, 0f);
        m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(BlockPos.containing(x, y, z)), MobSpawnType.MOB_SUMMONED, null);
        m.setPersistenceRequired();
        m.addTag(TAG);
        m.addTag(SERVANT);
        m.getPersistentData().putUUID(BOSS_KEY, boss);
        scale(m, Attributes.MAX_HEALTH, HP, healthMult(depth) - 1.0);
        scale(m, Attributes.ATTACK_DAMAGE, DMG, damageMult(depth) - 1.0);
        m.setHealth(m.getMaxHealth());
        for (EquipmentSlot slot : EquipmentSlot.values()) m.setDropChance(slot, 0f);
        if (m instanceof Vex v) v.setLimitedLife(20 * 45);                          // a vex flies through walls: it does not stay
        if (target != null) m.setTarget(target);
        if (!sl.addFreshEntity(m)) return null;
        return m;
    }

    private static void scale(Mob m, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance a = m.getAttribute(attribute);
        if (a == null || amount <= 0.0) return;
        a.removeModifier(id);
        a.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** The adds of one boss that live inside (or just round) a box. */
    static List<Mob> of(ServerLevel sl, AABB box, UUID boss) {
        return sl.getEntitiesOfClass(Mob.class, box, e -> e.isAlive() && e.getTags().contains(TAG) && boss.equals(owner(e)));
    }

    /** The boss an add belongs to, or null. */
    static UUID owner(Mob m) {
        return m.getPersistentData().hasUUID(BOSS_KEY) ? m.getPersistentData().getUUID(BOSS_KEY) : null;
    }
}
