package com.newuniverse.nusmp.labyrinth.mega.poise;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.eco.EcoConfig;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryConfig;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryMobEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.slf4j.Logger;

/**
 * Why: the unified dungeon faction. The owner's design is that the dungeon's monsters exist to hunt the player; two of them fighting each other while the party stands in the next room, or a
 * skeleton's arrow killing the zombie in front of it, only makes the dungeon quieter and the loot cheaper. So while {@code foundry.poise.dungeonPassive} is on, a monster of the dungeon
 * cannot TARGET another monster of the dungeon ({@link LivingChangeTargetEvent} is cancelled) and cannot HURT one (friendly fire: arrows, blasts, abilities, a charge through the line are
 * cancelled at {@link LivingIncomingDamageEvent}). The old systems are kept behind their switches and run again only with {@code dungeonPassive=false}: {@code eco.turfWarsEnabled}
 * (the turf wars and the old target guard, ordinary monsters) and {@code foundry.factionWars} (the foundry's {@code RivalGoal}); both now default to false. A monster of the dungeon is one that is
 * an {@link Enemy}, a monster by category, a foundry mob, or tagged by the Dungeon Master, inside the Labyrinth's dimensions and NOT a servant of a player (a tamed or owned creature, a
 * shadow, a relic's summon): servants and pets are never protected from the dungeon and never blocked from fighting it. A player is never involved: the rule only ever sees mob against mob.
 */
public final class Passivity {
    private Passivity() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean warned;

    public static boolean enabled() { return FoundryConfig.get(FoundryConfig.DUNGEON_PASSIVE); }

    /** Do the eco turf wars run? Off while the dungeon is passive, whatever the old switch says; then the old switch decides. */
    public static boolean turfWarsRun() { return PassivityRule.quarrelsRun(enabled(), EcoConfig.get(EcoConfig.TURF_WARS)); }

    /** Does the foundry's rival goal run? Same rule. */
    public static boolean factionWarsRun() { return PassivityRule.quarrelsRun(enabled(), FoundryConfig.get(FoundryConfig.FACTION_WARS)); }

    /** Is this entity one of the dungeon's monsters (one side with every other)? */
    public static boolean dungeonMob(Entity e) {
        if (!(e instanceof Mob m) || !Labyrinth.in(m.level())) return false;
        if (PoiseSystem.isServant(m)) return false;
        return m instanceof FoundryMobEntity || m instanceof Enemy || MegaDungeon.isDungeonMonster(m) || m.getType().getCategory() == MobCategory.MONSTER;
    }

    /** Must {@code attacker} be stopped from going for / hurting {@code victim}? The pure rule plus the two entity questions and the live config. */
    public static boolean blocks(Entity attacker, Entity victim) {
        if (attacker == victim || !enabled()) return false;
        if (!(attacker instanceof Mob) || !(victim instanceof Mob)) return false;
        return PassivityRule.blocks(true, dungeonMob(attacker), dungeonMob(victim));
    }

    public static void onChangeTarget(LivingChangeTargetEvent e) {
        try {
            LivingEntity next = e.getNewAboutToBeSetTarget();
            if (next == null || next.level().isClientSide()) return;
            if (blocks(e.getEntity(), next)) e.setCanceled(true);
        } catch (RuntimeException | LinkageError ex) {
            warn("target", ex);
        }
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        try {
            LivingEntity victim = e.getEntity();
            if (victim.level().isClientSide) return;
            DamageSource src = e.getSource();
            Entity by = src.getEntity();
            if (by != null && blocks(by, victim)) e.setCanceled(true);
        } catch (RuntimeException | LinkageError ex) {
            warn("damage", ex);
        }
    }

    private static void warn(String what, Throwable t) {
        if (warned) return;
        warned = true;
        LOG.warn("[nusmp] The dungeon passivity failed once in {} and goes on: {}", what, t.toString(), t);
    }
}
