package com.newuniverse.nusmp.soul;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.entity.zogratis.PaladinTurns;
import com.newuniverse.nusmp.entity.zogratis.SoulPaladins;
import com.newuniverse.nusmp.entity.zogratis.ZogratisEntities;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Why: this is the whole server side of a converted creature in one place, so the three things that must never disagree stay together:
 * what is stored on the creature, what the roster ({@link PaladinRoster}) remembers, and what the creature does. A Paladin is an
 * ordinary mob with (1) a compound {@value #TAG} in its persistent data (owner id, conversion time, the follower cap at the time), which
 * is what saves it, (2) the {@code nusmp:paladin} marker effect, which is what the client sees, (3) six saved attribute modifiers, which
 * are the buffs, and (4) a roster entry, which is the limit. Releasing undoes all four. No entity class is added.
 * <ul>
 *   <li><b>One conversion heal.</b> At conversion the creature is set to its new full health once. That is the owner-requested
 *       conversion heal: the Paladin is not a boss and has no regeneration, nothing here heals it again.</li>
 *   <li><b>Control.</b> A goal-driven mob gets {@link PaladinGoals}; any mob, goal-driven or brain-driven (Tensura's), gets the same
 *       decisions from {@link #drive} every ten ticks (set the target, path to it, strike in reach when it has no attack goal of its
 *       own), the same way {@code relics/RelicSummons} and {@code PaladinTurns} drive Tensura's mobs.</li>
 *   <li><b>Safety.</b> A Paladin never harms its owner, their allies or its fellow Paladins (damage cancelled in {@link PaladinEvents},
 *       explosions and projectiles included); its blows on players and bosses are capped ({@link PaladinStats#capHit}); nothing here
 *       changes a block, so the Dungeon guard has nothing to veto.</li>
 * </ul>
 * This is a different feature from Lucius's Soul Paladins of the boss fight ({@code SoulPaladins}) and from the 20-second turn of
 * companions ({@code PaladinTurns}); both stay untouched, and a creature that is one of those is refused here.
 */
public final class Paladins {
    private Paladins() {}

    /** The compound in the creature's persistent data: Owner (uuid), Time (game time of the conversion), Cap (follower limit then). */
    public static final String TAG = "nusmp_paladin";
    private static final String RELIC_OWNER = "nusmp_relic_owner";
    /** Entity types that can never be purified (data: data/nusmp/tags/entity_type/paladin_immune.json). */
    public static final TagKey<EntityType<?>> IMMUNE = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "paladin_immune"));

    static final PaladinRoster<UUID> ROSTER = new PaladinRoster<>();

    public enum Reason { DISMISSED, OWNER_DIED, EVICTED, OUTLIVED }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path); }

    private static final ResourceLocation HP = id("paladin_health"), ATK = id("paladin_attack"), ARM = id("paladin_armor"),
            TOUGH = id("paladin_toughness"), SPD = id("paladin_speed"), KB = id("paladin_knockback");

    // ================================================================ questions
    public static boolean isPaladin(Entity e) { return e != null && e.getPersistentData().contains(TAG, 10); }

    /** The owner's id, or null if this is not a Paladin. */
    public static UUID ownerOf(Entity e) {
        if (!isPaladin(e)) return null;
        CompoundTag t = e.getPersistentData().getCompound(TAG);
        return t.hasUUID("Owner") ? t.getUUID("Owner") : null;
    }

    static long timeOf(Entity e) { return isPaladin(e) ? e.getPersistentData().getCompound(TAG).getLong("Time") : 0L; }

    static int capOf(Entity e) { return isPaladin(e) ? Math.max(1, e.getPersistentData().getCompound(TAG).getInt("Cap")) : PaladinRoster.BASE; }

    /** The online owner of a Paladin, or null (offline, or not a Paladin). */
    static ServerPlayer ownerPlayer(Mob m) {
        UUID o = ownerOf(m);
        MinecraftServer server = m.level().getServer();
        return o == null || server == null ? null : server.getPlayerList().getPlayer(o);
    }

    /** Looks an entity up in every level (a Paladin may have followed its owner through a portal). */
    static Entity find(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }

    /** The loaded Paladins of one owner, oldest first. */
    public static List<Mob> of(MinecraftServer server, UUID owner) {
        List<Mob> out = new ArrayList<>();
        for (UUID id : ROSTER.of(owner)) if (find(server, id) instanceof Mob m && m.isAlive()) out.add(m);
        return out;
    }

    // ================================================================ may this be purified?
    /** Reads every fact the rules need off the target. */
    public static PaladinRules.Subject subject(ServerPlayer caster, LivingEntity t) {
        UUID owner = ownerOf(t);
        boolean pet = t instanceof OwnableEntity o && o.getOwnerUUID() != null
                || t.getPersistentData().hasUUID(RELIC_OWNER) || PaladinTurns.isTurned(t);
        boolean named = t.hasCustomName() && "tensura".equals(BuiltInRegistries.ENTITY_TYPE.getKey(t.getType()).getNamespace()) && t.getMaxHealth() >= 100;
        double hpFrac = t.getMaxHealth() <= 0 ? 1.0 : t.getHealth() / t.getMaxHealth();
        return new PaladinRules.Subject(t instanceof Player, BossIdentity.isBoss(t) || BalanceLaw.isBoss(t), t.getType().is(IMMUNE), named,
                ZogratisEntities.isFamily(t), SoulPaladins.isPaladin(t), owner != null && owner.equals(caster.getUUID()),
                owner != null && !owner.equals(caster.getUUID()), pet, t.isAlive() && !t.isRemoved(), hpFrac, ep(t), ep(caster), resistance(t), t instanceof Enemy);
    }

    private static double ep(LivingEntity e) {
        try { return Math.max(0, EnergyHelper.getMaxEP(e)); } catch (Throwable unknown) { return 0; }
    }

    /** Tensura's magic resistance as 0..1 (the attribute may be a fraction or a percent: above 1 it is read as a percent). */
    private static double resistance(LivingEntity e) {
        try {
            AttributeInstance a = e.getAttribute(TensuraAttributes.MAGIC_RESISTANCE);
            if (a == null) return 0;
            double v = a.getValue();
            return Math.max(0, Math.min(1, v > 1 ? v / 100.0 : v));
        } catch (Throwable unknown) {
            return 0;
        }
    }

    // ================================================================ making a Paladin
    /**
     * Turns 'm' into a Paladin of 'owner'. 'cap' is the owner's follower limit now; if they are over it, their oldest Paladins are
     * released ({@link Reason#EVICTED}). Returns false if the mob is not a mob that can be turned any more (dead, already one).
     */
    public static boolean convert(ServerPlayer owner, Mob m, int cap) {
        if (!(m.level() instanceof ServerLevel sl) || !m.isAlive() || isPaladin(m)) return false;
        long now = sl.getGameTime();
        CompoundTag t = new CompoundTag();
        t.putUUID("Owner", owner.getUUID());
        t.putLong("Time", now);
        t.putInt("Cap", cap);
        m.getPersistentData().put(TAG, t);
        for (UUID gone : ROSTER.add(owner.getUUID(), m.getUUID(), now, cap)) {
            if (find(owner.server, gone) instanceof Mob old) release(old, Reason.EVICTED);
        }
        applyStats(m);
        m.setHealth(m.getMaxHealth());                                         // the one conversion heal (see the class note)
        m.addEffect(PaladinEffects.marker());
        m.setGlowingTag(true);
        m.setPersistenceRequired();
        m.setLastHurtByMob(null);
        clearTarget(m);
        PaladinGoals.install(m);
        PaladinNet.announce(m);
        owner.displayClientMessage(Component.literal(m.getDisplayName().getString() + " is purified and follows you.").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    private static void mod(Mob m, Holder<Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance a = m.getAttribute(attr);
        if (a == null) return;
        a.addOrReplacePermanentModifier(new AttributeModifier(id, amount, op));
    }

    /** The six saved modifiers (permanent, so they are written with the creature). A mob without one of the attributes simply skips it. */
    static void applyStats(Mob m) {
        PaladinStats.Mods s = PaladinStats.mods(SoulConfig.strength());
        mod(m, Attributes.MAX_HEALTH, HP, s.health(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        mod(m, Attributes.ATTACK_DAMAGE, ATK, s.attack(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        mod(m, Attributes.ARMOR, ARM, s.armor(), AttributeModifier.Operation.ADD_VALUE);
        mod(m, Attributes.ARMOR_TOUGHNESS, TOUGH, s.toughness(), AttributeModifier.Operation.ADD_VALUE);
        mod(m, Attributes.MOVEMENT_SPEED, SPD, s.speed(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        mod(m, Attributes.KNOCKBACK_RESISTANCE, KB, s.knockback(), AttributeModifier.Operation.ADD_VALUE);
    }

    private static void removeStats(Mob m) {
        for (Holder<Attribute> a : List.of(Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS,
                Attributes.MOVEMENT_SPEED, Attributes.KNOCKBACK_RESISTANCE)) {
            AttributeInstance i = m.getAttribute(a);
            if (i == null) continue;
            for (ResourceLocation id : List.of(HP, ATK, ARM, TOUGH, SPD, KB)) i.removeModifier(id);
        }
        if (m.getHealth() > m.getMaxHealth()) m.setHealth(m.getMaxHealth());
    }

    private static void clearTarget(Mob m) {
        m.setTarget(null);
        try { m.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET); } catch (RuntimeException goalDriven) { /* no brain memory to clear */ }
        m.getNavigation().stop();
    }

    // ================================================================ letting one go
    /** Undoes all four parts of a conversion. Safe to call twice (the second call finds nothing to undo). */
    public static void release(Mob m, Reason why) {
        if (!isPaladin(m)) return;
        UUID owner = ownerOf(m);
        ROSTER.remove(m.getUUID());
        m.getPersistentData().remove(TAG);
        removeStats(m);
        m.removeEffect(PaladinEffects.PALADIN);
        m.setGlowingTag(false);
        PaladinNet.withdraw(m);
        PaladinGoals.uninstall(m, SoulConfig.releasedHostile());
        clearTarget(m);
        if (m.level() instanceof ServerLevel sl) {
            PaladinFx.released(sl, m);
            if (owner != null && sl.getServer().getPlayerList().getPlayer(owner) instanceof ServerPlayer p && why != Reason.OWNER_DIED) {
                String what = why == Reason.EVICTED ? " lets go of the oldest follower: " : " is released: ";
                p.displayClientMessage(Component.literal("Your soul" + what + m.getDisplayName().getString()).withStyle(ChatFormatting.GRAY), true);
            }
        }
    }

    /** The owner has died: every Paladin of theirs that is loaded goes free, and the death is written down for the ones that are not. */
    static void onOwnerDeath(ServerPlayer owner) {
        PaladinMemory.get(owner.server).died(owner.getUUID(), owner.level().getGameTime());
        for (UUID id : ROSTER.releaseOwner(owner.getUUID())) if (find(owner.server, id) instanceof Mob m) release(m, Reason.OWNER_DIED);
    }

    /** A Paladin was killed or discarded: its place is free again. */
    static void onGone(Entity e) {
        if (ROSTER.remove(e.getUUID()) != null && e.level() instanceof ServerLevel sl && e instanceof LivingEntity le && !le.isAlive()) {
            UUID o = ownerOf(e);
            if (o != null && sl.getServer().getPlayerList().getPlayer(o) instanceof ServerPlayer p)
                p.displayClientMessage(Component.literal(e.getDisplayName().getString() + " has fallen.").withStyle(ChatFormatting.GRAY), true);
        }
    }

    // ================================================================ load (join a level)
    /** A tagged creature joined a level (world load, chunk load, a portal): put everything back in order. */
    static void onLoad(Mob m) {
        UUID owner = ownerOf(m);
        MinecraftServer server = m.level().getServer();
        if (owner == null || server == null) return;
        if (PaladinMemory.get(server).outlived(owner, timeOf(m))) { release(m, Reason.OUTLIVED); return; }
        for (UUID gone : ROSTER.add(owner, m.getUUID(), timeOf(m), Math.min(SoulConfig.max(), capOf(m)))) {
            if (gone.equals(m.getUUID())) { release(m, Reason.EVICTED); return; }
            if (find(server, gone) instanceof Mob old) release(old, Reason.EVICTED);
        }
        applyStats(m);
        if (m.getHealth() > m.getMaxHealth()) m.setHealth(m.getMaxHealth());
        if (!PaladinEffects.marked(m)) m.addEffect(PaladinEffects.marker());
        m.setGlowingTag(true);
        PaladinGoals.install(m);
    }

    // ================================================================ who to fight, and the drive
    /** Is 't' a fair target for this Paladin of this owner? */
    static boolean fair(Mob paladin, ServerPlayer owner, LivingEntity t) {
        UUID oid = owner.getUUID();
        boolean samePal = isPaladin(t) && oid.equals(ownerOf(t));
        boolean pet = t instanceof OwnableEntity o && oid.equals(o.getOwnerUUID())
                || t.getPersistentData().hasUUID(RELIC_OWNER) && oid.equals(t.getPersistentData().getUUID(RELIC_OWNER));
        boolean safe = t instanceof Player p && (p.isCreative() || p.isSpectator()) || t.isSpectator();
        return PaladinTargeting.fairTarget(new PaladinTargeting.Candidate(t.isAlive() && t != paladin, t == owner, t.isAlliedTo(owner) || t.isAlliedTo(paladin),
                samePal, pet, safe, false, t.level() == owner.level() ? t.distanceTo(owner) : Double.MAX_VALUE));
    }

    /** The foe the owner was struck by, else the creature they struck, lately (null if none is fair). */
    static LivingEntity desiredTarget(Mob paladin, ServerPlayer owner) {
        LivingEntity hurtBy = owner.getLastHurtByMob(), hurt = owner.getLastHurtMob();
        boolean byFair = hurtBy != null && fair(paladin, owner, hurtBy), hFair = hurt != null && fair(paladin, owner, hurt);
        int pick = PaladinTargeting.pick(owner.tickCount, owner.getLastHurtByMobTimestamp(), byFair, owner.getLastHurtMobTimestamp(), hFair);
        return pick == 1 ? hurtBy : pick == 2 ? hurt : null;
    }

    /** Every ten ticks per Paladin: marker, owner, leash, target, and the blow for a creature with no attack goal. */
    static void drive(MinecraftServer server, Mob m, long now) {
        try {
            if (!PaladinEffects.marked(m)) m.addEffect(PaladinEffects.marker());      // milk or a totem cannot undress a Paladin for long
            if (now % 20 == 0) PaladinNet.announce(m);
            ServerPlayer owner = ownerPlayer(m);
            if (owner == null || !owner.isAlive()) return;                            // offline: it stays where it is
            boolean same = owner.level() == m.level();
            LivingEntity target = m.getTarget();
            if (target != null && same && !fair(m, owner, target)) { clearTarget(m); target = null; }
            switch (PaladinTargeting.follow(same ? m.distanceTo(owner) : Double.MAX_VALUE, target != null, same)) {
                case TELEPORT -> { teleportNear(m, owner); return; }
                case RETURN -> { clearTarget(m); m.getNavigation().moveTo(owner, 1.3); return; }
                case WALK -> m.getNavigation().moveTo(owner, 1.15);
                default -> { }
            }
            LivingEntity want = desiredTarget(m, owner);
            if (want != null && want != target) {
                m.setTarget(want);
                try { m.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, want); } catch (RuntimeException goalDriven) { /* no brain */ }
                target = want;
            }
            if (target != null && !PaladinGoals.hasAttackGoal(m)) strike(m, target, now);
        } catch (RuntimeException | LinkageError odd) {
            // a creature of an unexpected shape keeps its own mind
        }
    }

    private static void strike(Mob m, LivingEntity prey, long now) {
        m.getNavigation().moveTo(prey, 1.2);
        CompoundTag d = m.getPersistentData();
        if (now - d.getLong("nusmp_paladin_strike") < 20 || !m.getBoundingBox().inflate(1.1, 0.7, 1.1).intersects(prey.getBoundingBox())) return;
        d.putLong("nusmp_paladin_strike", now);
        m.swing(InteractionHand.MAIN_HAND);
        if (m.getAttribute(Attributes.ATTACK_DAMAGE) != null) m.doHurtTarget(prey);
        else prey.hurt(m.damageSources().mobAttack(m), (float) PaladinStats.fallbackDamage(SoulConfig.strength()));   // a cow has no attack attribute
    }

    // ================================================================ recall
    /** Puts the Paladin on a free spot beside 'to' (the owner), through a portal if need be. Returns false if it could not be moved. */
    static boolean teleportNear(Mob m, Entity to) {
        if (!(to.level() instanceof ServerLevel dest)) return false;
        Vec3 spot = freeSpot(dest, m, to);
        PaladinFx.recall(dest, m.position(), m.getBbHeight());
        boolean ok;
        if (m.level() == dest) {
            m.teleportTo(spot.x, spot.y, spot.z);
            ok = true;
        } else {
            ok = m.teleportTo(dest, spot.x, spot.y, spot.z, EnumSet.noneOf(RelativeMovement.class), m.getYRot(), m.getXRot());
        }
        if (ok) { clearTarget(m); PaladinFx.recall(dest, spot, m.getBbHeight()); }
        return ok;
    }

    private static Vec3 freeSpot(ServerLevel level, Mob m, Entity to) {
        for (int i = 0; i < 12; i++) {
            double a = (i * 2.399 + m.getId()) % (Math.PI * 2);
            double r = 1.8 + 0.5 * (i / 4);
            Vec3 c = to.position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
            AABB box = m.getBoundingBox().move(c.x - m.getX(), to.getY() - m.getY(), c.z - m.getZ());
            if (level.noCollision(m, box)) return new Vec3(c.x, to.getY(), c.z);
        }
        return to.position();
    }
}
