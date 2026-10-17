package com.newuniverse.nusmp.labyrinth.mega.system;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.corpse.Corpses;
import com.newuniverse.nusmp.dungeon.nexus.zone.MonarchBoss;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.dnd.vital.HpBuffer;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.soul.Paladins;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Why: this is the whole life of a shadow in one place, so the four things that must never disagree stay together: what is STORED on the creature (the entity
 * tag {@value #TAG}, which the faction code already treats as "leave this servant alone", and a persistent-data compound {@value #DATA} with the owner, the
 * birth time and the moment the owner went away), what the ROSTER remembers (the cap of {@code maxMinions}, oldest released first), what the creature DOES
 * (follow, defend, assist: {@link MinionGoals} for goal-driven mobs and the ten-tick {@link #sweep} drive for every mob, the way the Soul Corpse and Paladin
 * systems do it, because Tensura's mobs are brain-driven), and how it ENDS (killed, released, or {@code minionMinutes} after the owner left, measured in game
 * time so it is still right after a server restart). A shadow is a fresh creature of the fallen one's type with health and attack at {@code minionStrength}
 * of what the fallen one had; it never harms its owner or a fellow shadow (the damage and target guards in {@link SystemEvents}), drops nothing and gives no
 * experience, never leaves the Mega-Dungeon on its own, and follows its owner through it, teleporting when left far behind. Server thread only.
 */
public final class ShadowMinions {
    private ShadowMinions() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** The entity (scoreboard) tag every shadow carries; the turf-war code reads it to leave servants alone. */
    public static final String TAG = "nusmp_servant";
    /** The persistent-data compound of a shadow: Owner (uuid), Born (game time), Away (game time the owner left, 0 = here). */
    public static final String DATA = "nusmp_minion";

    private static final double FOLLOW_TELEPORT_SQ = 40.0 * 40.0;
    private static final int STRIKE_COOLDOWN = 20;

    static final MinionRoster ROSTER = new MinionRoster();

    // ================================================================ identity
    public static boolean isMinion(Entity e) { return e != null && e.getPersistentData().contains(DATA, Tag.TAG_COMPOUND); }

    /** The owner of a shadow, or null (not a shadow). */
    public static UUID ownerOf(Entity e) {
        if (!isMinion(e)) return null;
        CompoundTag t = e.getPersistentData().getCompound(DATA);
        return t.hasUUID("Owner") ? t.getUUID("Owner") : null;
    }

    /** The online owner of a shadow, or null (offline, or not a shadow). */
    public static ServerPlayer ownerPlayer(Entity e) {
        UUID o = ownerOf(e);
        if (o == null) return null;
        MinecraftServer server = e.level().getServer();
        return server == null ? null : server.getPlayerList().getPlayer(o);
    }

    /** The player a creature acts FOR: the player themselves, or the owner of a shadow; null for anything else. Used to credit kills and to pair friends in the damage guard. */
    public static UUID principal(Entity e) {
        if (e instanceof Player p) return p.getUUID();
        return ownerOf(e);
    }

    /** Is this creature somebody's servant already (a shadow, a Paladin, a Soul Corpse, a shadow soldier, a relic summon, a tamed pet ...)? */
    public static boolean isServant(Entity e) {
        if (e == null) return false;
        try {
            if (isMinion(e) || e.getTags().contains(TAG) || e.getTags().contains("nusmp_underworld")) return true;
            if (Paladins.isPaladin(e) || Corpses.isCorpse(e) || MonarchBoss.isSoldier(e)) return true;
            CompoundTag d = e.getPersistentData();
            if (d.contains("nusmp_relic_owner") || d.contains("nusmp_bull_owner") || d.contains("nusmp_paladin_turn")) return true;
            return e instanceof OwnableEntity o && o.getOwnerUUID() != null;
        } catch (RuntimeException | LinkageError unreadable) {
            return false;
        }
    }

    /**
     * Is this a hostile creature of the dungeon? A vanilla {@link Enemy}, or one of the Dungeon Master's monsters: Tensura's monsters are tamable animals and not
     * {@code Enemy}, so the economy, the health buffer and the faction drops ask {@link MegaDungeon#isDungeonMonster} as well, and so does the System (a kill quest, a
     * cleared room and an extraction must count the Director's direwolves and goblins like zombies).
     */
    public static boolean isHostile(Entity e) { return e instanceof Enemy || MegaDungeon.isDungeonMonster(e); }

    /** Is this a boss by any of the mod's definitions (or a wandering boss of the dungeon)? */
    public static boolean isBoss(LivingEntity e) {
        try {
            return BossIdentity.isBoss(e) || BalanceLaw.isBoss(e) || e.getTags().contains("nusmp_wanderer") || e.getTags().contains("nusmp_peak_boss");
        } catch (RuntimeException | LinkageError unreadable) {
            return false;
        }
    }

    // ================================================================ raising
    /**
     * Raises the fallen creature as a shadow of {@code owner}: a fresh creature of the same type at the spot where it fell (or beside the owner when that spot
     * is blocked), scaled to {@code minionStrength}, marked, bound, given its goals, and listed (the oldest shadow is released when the owner is over the cap).
     * Null when the type is gone or cannot be a mob, the cap is 0, or the world refused it.
     */
    public static Mob raise(ServerPlayer owner, ExtractLedger.Fallen f) {
        int cap = SystemConfig.get(SystemConfig.MAX_MINIONS);
        if (cap <= 0 || owner == null || !owner.isAlive()) return null;
        ResourceLocation rl = ResourceLocation.tryParse(f.type());
        EntityType<?> type = rl == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
        if (type == null) return null;
        ServerLevel level = owner.serverLevel();
        Mob m = null;
        try {
            Entity made = type.create(level);
            if (!(made instanceof Mob mob)) {
                if (made != null) made.discard();
                return null;
            }
            m = mob;
            Vec3 spot = spotFor(level, mob, new Vec3(f.x(), f.y(), f.z()), owner);
            mob.moveTo(spot.x, spot.y, spot.z, owner.getYRot() + 180f, 0f);
            long now = level.getGameTime();
            mob.addTag(TAG);                                                   // before it joins, so every join listener sees a servant
            CompoundTag t = new CompoundTag();
            t.putUUID("Owner", owner.getUUID());
            t.putLong("Born", now);
            t.putLong("Away", 0L);
            mob.getPersistentData().put(DATA, t);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            scale(mob, f);
            keepBufferOff(mob);
            mob.setPersistenceRequired();
            mob.setCanPickUpLoot(false);
            mob.setTarget(null);
            mob.setCustomName(SystemMsg.text(SystemText.MSG_MINION_NAME, type.getDescription()));
            mob.setCustomNameVisible(false);
            MinionGoals.install(mob);
            if (!level.addFreshEntity(mob)) return null;
            for (UUID gone : ROSTER.add(owner.getUUID(), mob.getUUID(), cap)) {
                Entity old = find(owner.getServer(), gone);
                if (old instanceof Mob om) {
                    SystemSync.toast(owner, ToastKind.SHADOW, SystemText.TOAST_MINION_RELEASED, "@" + om.getType().getDescriptionId());
                    dismiss(om, true);
                }
            }
            RukhFx.burst(level, mob, RukhFx.Alignment.DARK, 12);
            level.playSound(null, mob.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0f, 0.6f);
            return mob;
        } catch (RuntimeException | LinkageError failed) {
            LOG.warn("[nusmp] A shadow of {} could not be raised: {}", f.type(), failed.toString());
            if (m != null && !m.isRemoved()) {
                ROSTER.remove(m.getUUID());
                m.discard();
            }
            return null;
        }
    }

    /** Health and attack at {@code minionStrength} of what the fallen creature had; random spawn bonuses are stripped so the numbers are exact. */
    private static void scale(Mob m, ExtractLedger.Fallen f) {
        double k = SystemConfig.get(SystemConfig.MINION_STRENGTH);
        AttributeInstance hp = m.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && f.maxHealth() > 0) {
            hp.removeModifiers();
            hp.setBaseValue(Math.max(1.0, f.maxHealth() * k));
        }
        m.setHealth(m.getMaxHealth());
        AttributeInstance atk = m.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null && f.attack() > 0) {
            atk.removeModifiers();
            atk.setBaseValue(Math.max(0.5, f.attack() * k));
        }
    }

    /**
     * The Labyrinth's health buffer multiplies the health of EVERY hostile creature that joins a Labyrinth dimension, and a shadow is hostile by type. Its health is
     * already a share of the fallen creature's (which carried the buffer), so it must not be buffered again. The buffer skips a creature that already has its
     * modifier, so the modifier goes on first with an amount of 0, which changes nothing. Saved with the creature, so a shadow that comes back from disk stays out
     * of it too.
     */
    private static void keepBufferOff(Mob m) {
        try {
            AttributeInstance hp = m.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) hp.addOrReplacePermanentModifier(new AttributeModifier(HpBuffer.MODIFIER_ID, 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } catch (RuntimeException | LinkageError buffer) {
            // the buffer class is part of this mod; if it cannot be read there is nothing to keep off
        }
    }

    private static Vec3 spotFor(ServerLevel level, Mob m, Vec3 preferred, ServerPlayer owner) {
        if (fits(level, m, preferred)) return preferred;
        for (int i = 0; i < 12; i++) {
            double a = i * 2.399, r = 1.8 + 0.5 * (i / 4);
            Vec3 c = owner.position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
            if (fits(level, m, c)) return c;
        }
        return owner.position();
    }

    private static boolean fits(ServerLevel level, Mob m, Vec3 at) {
        return level.noCollision(m.getType().getDimensions().makeBoundingBox(at.x, at.y, at.z));
    }

    // ================================================================ ending
    /** Removes a shadow (released, expired, or cleaned up): off the roster, a puff of dark Rukh, no drops. */
    public static void dismiss(Entity m, boolean puff) {
        if (m == null || m.isRemoved()) return;
        ROSTER.remove(m.getUUID());
        if (puff && m.level() instanceof ServerLevel sl) {
            RukhFx.burst(sl, m, RukhFx.Alignment.DARK, 8);
            sl.playSound(null, m.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.NEUTRAL, 0.8f, 1.2f);
        }
        m.discard();
    }

    /** Releases every shadow of one owner that is loaded. Returns how many there were. */
    public static int dismissAll(MinecraftServer server, UUID owner) {
        int n = 0;
        for (UUID id : ROSTER.clearOwner(owner)) {
            Entity e = find(server, id);
            if (e != null) {
                dismiss(e, true);
                n++;
            }
        }
        return n;
    }

    /** A shadow died: its place is free and the owner is told. */
    public static void onDeath(Mob m) {
        UUID owner = ownerOf(m);
        ROSTER.remove(m.getUUID());
        if (owner == null) return;
        MinecraftServer server = m.level().getServer();
        ServerPlayer p = server == null ? null : server.getPlayerList().getPlayer(owner);
        if (p != null) SystemSync.toast(p, ToastKind.SHADOW, SystemText.TOAST_MINION_LOST, "@" + m.getType().getDescriptionId());
    }

    /** The owner logged out or left: the clock of every loaded shadow of theirs starts (the sweep would find out within half a second; this makes it exact). */
    public static void ownerLeft(MinecraftServer server, UUID owner) {
        long now = server.overworld().getGameTime();
        for (UUID id : ROSTER.of(owner)) {
            Entity e = find(server, id);
            if (e != null && isMinion(e)) {
                CompoundTag d = e.getPersistentData().getCompound(DATA);
                if (d.getLong("Away") == 0L) d.putLong("Away", MinionLife.awayStamp(now));
            }
        }
    }

    // ================================================================ queries
    /** The loaded shadows of one owner, oldest first. */
    public static List<Mob> of(MinecraftServer server, UUID owner) {
        List<Mob> out = new ArrayList<>();
        for (UUID id : ROSTER.of(owner)) if (find(server, id) instanceof Mob m && m.isAlive()) out.add(m);
        return out;
    }

    public static int countOf(UUID owner) { return ROSTER.count(owner); }

    static Entity find(MinecraftServer server, UUID id) {
        if (server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }

    public static void clear() { ROSTER.clear(); }

    // ================================================================ loading
    /**
     * A saved shadow joins a level (the world loaded, its chunk loaded): if its owner has been away longer than the lifetime it is cancelled (it never comes
     * back), else it is listed again (the cap holds across restarts) and gets its goals back (they are not saved). Returns false when the join must be cancelled.
     */
    public static boolean adopt(Mob m) {
        UUID owner = ownerOf(m);
        MinecraftServer server = m.level().getServer();
        if (owner == null || server == null) return false;
        ServerPlayer p = server.getPlayerList().getPlayer(owner);
        CompoundTag d = m.getPersistentData().getCompound(DATA);
        long now = server.overworld().getGameTime();
        if (p == null) {
            long since = d.getLong("Away");
            if (since == 0L) d.putLong("Away", MinionLife.awayStamp(now));
            else if (MinionLife.expired(since, now, MinionLife.lifetimeTicks(SystemConfig.get(SystemConfig.MINION_MINUTES)))) return false;
        }
        for (UUID gone : ROSTER.add(owner, m.getUUID(), Math.max(1, SystemConfig.get(SystemConfig.MAX_MINIONS)))) {
            if (gone.equals(m.getUUID())) return false;
            Entity old = find(server, gone);
            if (old != null) dismiss(old, true);
        }
        MinionGoals.install(m);
        return true;
    }

    // ================================================================ the sweep
    /** Every ten ticks: ends the shadows whose owner has been away too long, and drives the others (target, path, a blow in reach) for their owner. */
    public static void sweep(MinecraftServer server) {
        if (ROSTER.isEmpty()) return;
        long now = server.overworld().getGameTime();
        long life = MinionLife.lifetimeTicks(SystemConfig.get(SystemConfig.MINION_MINUTES));
        for (UUID ownerId : ROSTER.owners()) {
            ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
            for (UUID id : ROSTER.of(ownerId)) {
                Entity e = find(server, id);
                if (!(e instanceof Mob m) || !m.isAlive() || !isMinion(m)) {
                    ROSTER.remove(id);
                    continue;
                }
                boolean away = owner == null || !owner.isAlive() || owner.level() != m.level();
                CompoundTag d = m.getPersistentData().getCompound(DATA);
                long since = d.getLong("Away");
                if (away) {
                    if (since == 0L) d.putLong("Away", MinionLife.awayStamp(now));
                    else if (MinionLife.expired(since, now, life)) dismiss(m, true);
                    continue;
                }
                if (since != 0L) d.putLong("Away", 0L);
                drive(m, owner, now);
            }
        }
    }

    // ================================================================ who to fight, and the drive
    /** Is {@code t} a fair target for this shadow of this owner? Hostile monsters, and whatever the owner is fighting; never a player, a shadow, a servant, a pet or a bystander. */
    static boolean fair(Mob shadow, ServerPlayer owner, LivingEntity t) {
        if (t == owner || t == shadow || !t.isAlive() || t.isSpectator() || t instanceof ArmorStand || t instanceof Player || t instanceof Npc) return false;
        if (t.level() != owner.level() || t.distanceToSqr(owner) > 40.0 * 40.0) return false;
        if (t.isAlliedTo(owner) || t.isAlliedTo(shadow) || isServant(t)) return false;
        if (isHostile(t) || t.getType().getCategory() == MobCategory.MONSTER) return true;
        return fighting(owner, t);
    }

    private static boolean fighting(ServerPlayer owner, LivingEntity e) {
        return (owner.getLastHurtByMob() == e && owner.tickCount - owner.getLastHurtByMobTimestamp() < 200)
                || (owner.getLastHurtMob() == e && owner.tickCount - owner.getLastHurtMobTimestamp() < 200);
    }

    /** The foe the owner was hurt by, else the one they hit, lately; else the nearest hostile near the shadow while the owner is close. Null if none is fair. */
    private static LivingEntity wanted(Mob shadow, ServerPlayer owner) {
        LivingEntity by = owner.getLastHurtByMob(), hit = owner.getLastHurtMob();
        if (by != null && owner.tickCount - owner.getLastHurtByMobTimestamp() < 200 && fair(shadow, owner, by)) return by;
        if (hit != null && owner.tickCount - owner.getLastHurtMobTimestamp() < 200 && fair(shadow, owner, hit)) return hit;
        if (!(shadow.level() instanceof ServerLevel sl) || shadow.distanceToSqr(owner) > 24.0 * 24.0) return null;
        return sl.getEntitiesOfClass(LivingEntity.class, shadow.getBoundingBox().inflate(12, 5, 12), x -> x != shadow && fair(shadow, owner, x)).stream()
                .min(Comparator.comparingDouble(x -> x.distanceToSqr(shadow))).orElse(null);
    }

    private static void drive(Mob m, ServerPlayer owner, long now) {
        try {
            if (m.distanceToSqr(owner) > FOLLOW_TELEPORT_SQ) {
                teleportNear(m, owner);
                return;
            }
            LivingEntity target = m.getTarget();
            if (target != null && (!target.isAlive() || !fair(m, owner, target))) {
                clearTarget(m);
                target = null;
            }
            if (target == null) {
                LivingEntity want = wanted(m, owner);
                if (want != null) {
                    m.setTarget(want);
                    try {
                        m.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, want);
                    } catch (RuntimeException goalDriven) {
                        // a goal-driven mob has no such memory
                    }
                    target = want;
                }
            }
            if (target == null) {
                if (m.distanceToSqr(owner) > 64.0) m.getNavigation().moveTo(owner, 1.2);
                return;
            }
            if (!MinionGoals.hasAttackGoal(m)) strike(m, target, now);
        } catch (RuntimeException | LinkageError odd) {
            // a creature of an unexpected shape keeps its own mind
        }
    }

    private static void strike(Mob m, LivingEntity prey, long now) {
        m.getNavigation().moveTo(prey, 1.2);
        CompoundTag d = m.getPersistentData();
        if (now - d.getLong("nusmp_minion_strike") < STRIKE_COOLDOWN || !m.getBoundingBox().inflate(1.1, 0.7, 1.1).intersects(prey.getBoundingBox())) return;
        d.putLong("nusmp_minion_strike", now);
        m.swing(InteractionHand.MAIN_HAND);
        if (m.getAttribute(Attributes.ATTACK_DAMAGE) != null) m.doHurtTarget(prey);
    }

    private static void clearTarget(Mob m) {
        m.setTarget(null);
        try {
            m.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        } catch (RuntimeException goalDriven) {
            // no brain memory to clear
        }
        m.getNavigation().stop();
    }

    /** Puts the shadow on a free spot beside its owner (the same dimension only: shadows never leave the Mega-Dungeon on their own). */
    private static boolean teleportNear(Mob m, ServerPlayer owner) {
        if (m.level() != owner.level() || !(owner.level() instanceof ServerLevel level)) return false;
        for (int i = 0; i < 12; i++) {
            double a = (i * 2.399 + m.getId()) % (Math.PI * 2), r = 1.8 + 0.5 * (i / 4);
            Vec3 c = owner.position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
            if (fits(level, m, c)) {
                m.teleportTo(c.x, c.y, c.z);
                clearTarget(m);
                return true;
            }
        }
        return false;
    }

    /** True when the entity is in the Mega-Dungeon: the only dimension a shadow may be in. */
    public static boolean inMega(Entity e) { return Labyrinth.inMega(e.level()); }
}
