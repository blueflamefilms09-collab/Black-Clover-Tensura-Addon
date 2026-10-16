package com.newuniverse.nusmp.corpse;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.particle.rukh.RukhParticles;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Why: this is the one place that knows what a Soul Corpse IS in the game. A body is any creature that carries the persistent-data
 * compound {@value #TAG} (owner, birth tick, life, kind) and is listed in a {@link CorpseRoster}; the rest of the package only asks
 * questions of it. It owns the ledger of the dead ({@link CorpseLedger}, one per Soul Corpse mage, kept in the player's persistent
 * data so a relog does not lose the last minute), the raising of a body from a ledger entry (a fresh body of the same type, never a
 * boss, player, NPC or anything in {@code nusmp:corpse_immune}), the half-second sweep that drives every body (target, path, a blow
 * in reach, the slow bleed of health, the decay stage, the wisps, the end of its life) and the rules about who counts as a foe.
 * <p>
 * Tensura's mobs are brain-driven, so a body is pointed at a foe from outside and strikes when in reach, the same way
 * {@code relics.RelicSummons} drives its summons (that class is package-private, so the idea is reused, not the code). Nothing here
 * changes a block, so the Dungeon guard has nothing to veto. Bodies are never saved on purpose: the server discards them when it
 * stops, and any that were saved by a crash are discarded when they next join a level ({@link CorpseHooks#onJoin}).
 */
public final class Corpses {
    private Corpses() {}

    /** The persistent-data compound of a body: Owner (uuid), Born (game time), Life (ticks), Kind ("raised" or a {@link NamedCorpses} id). */
    public static final String TAG = "nusmp_corpse";
    static final String LEDGER_KEY = "nusmp_corpse_ledger";
    private static final int SWEEP = 10, WISP_EVERY = 20;

    static final CorpseRoster<UUID, UUID> RAISED = new CorpseRoster<>();
    static final CorpseRoster<UUID, UUID> NAMED = new CorpseRoster<>();
    private static final Map<UUID, CorpseLedger> LEDGERS = new HashMap<>();

    // ================================================================ identity
    public static boolean isCorpse(Entity e) { return e != null && e.getPersistentData().contains(TAG, Tag.TAG_COMPOUND); }

    public static boolean isNamed(Entity e) { return e instanceof NamedCorpseEntity; }

    /** The owner of a body (a reanimated one by its tag, a named one by its field), or null. */
    public static UUID ownerOf(Entity e) {
        if (e instanceof NamedCorpseEntity n) return n.getOwnerUUID();
        if (e == null || !isCorpse(e)) return null;
        CompoundTag t = e.getPersistentData().getCompound(TAG);
        return t.hasUUID("Owner") ? t.getUUID("Owner") : null;
    }

    /** True for a reanimated (not named) body, whose health bleeds and whose damage is raised. */
    public static boolean isRaised(Entity e) { return isCorpse(e) && !isNamed(e); }

    /** May this creature be raised as a Soul Corpse? Not a boss, player, NPC, ender dragon, anything immune, a body already, or something that cannot fight. */
    public static boolean raisable(LivingEntity e) {
        if (!(e instanceof Mob m) || e instanceof Player || e instanceof ArmorStand || e instanceof Npc) return false;
        if (isCorpse(e) || e.getType().is(CorpseTags.IMMUNE) || e.getType() == EntityType.ENDER_DRAGON) return false;
        if (BossIdentity.isBoss(e) || BalanceLaw.isBoss(e) || e.isSpectator()) return false;
        return m.getAttribute(Attributes.ATTACK_DAMAGE) != null || m instanceof RangedAttackMob;
    }

    /** Does the player have the Soul Corpse grimoire skill at all? (a death near someone without it is not worth remembering) */
    static boolean hasBook(ServerPlayer p) {
        var holder = NUSkills.WIKI_ATTRIBUTE_BOOKS.get(MagicType.SOUL_CORPSE);
        return holder != null && SkillAPI.getSkillsFrom(p).getSkill(holder.getId()).isPresent();
    }

    // ================================================================ the ledger
    /** The caster's ledger: loaded once from their persistent data, kept in memory, written back when it changes. */
    static CorpseLedger ledger(ServerPlayer p) {
        CorpseLedger l = LEDGERS.get(p.getUUID());
        if (l == null) {
            l = new CorpseLedger(CorpseConfig.ledgerMax());
            l.restore(read(p.getPersistentData().getList(LEDGER_KEY, Tag.TAG_COMPOUND)));
            LEDGERS.put(p.getUUID(), l);
        }
        l.setCap(CorpseConfig.ledgerMax());
        return l;
    }

    static void save(ServerPlayer p, CorpseLedger l) {
        ListTag list = new ListTag();
        for (CorpseLedger.Fallen f : l.entries()) {
            CompoundTag c = new CompoundTag();
            c.putString("Type", f.type());
            c.putString("Name", f.name() == null ? "" : f.name());
            c.putDouble("Hp", f.maxHealth());
            c.putDouble("Atk", f.attack());
            c.putDouble("Spd", f.speed());
            c.putString("Dim", f.dim());
            c.putDouble("X", f.x());
            c.putDouble("Y", f.y());
            c.putDouble("Z", f.z());
            c.putLong("T", f.tick());
            c.putBoolean("Pet", f.pet());
            list.add(c);
        }
        p.getPersistentData().put(LEDGER_KEY, list);
    }

    private static List<CorpseLedger.Fallen> read(ListTag list) {
        List<CorpseLedger.Fallen> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            if (!c.contains("Type")) continue;
            out.add(new CorpseLedger.Fallen(c.getString("Type"), c.getString("Name"), c.getDouble("Hp"), c.getDouble("Atk"), c.getDouble("Spd"),
                    c.getString("Dim"), c.getDouble("X"), c.getDouble("Y"), c.getDouble("Z"), c.getLong("T"), c.getBoolean("Pet")));
        }
        return out;
    }

    static void forget(UUID player) { LEDGERS.remove(player); }

    /** A creature died: write it into the ledger of every Soul Corpse mage within the ledger radius. */
    static void recordDeath(LivingEntity dead) {
        if (!CorpseConfig.enabled() || !(dead.level() instanceof ServerLevel sl) || !raisable(dead)) return;
        double r = CorpseConfig.ledgerRadius();
        long now = sl.getGameTime();
        String dim = sl.dimension().location().toString();
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType());
        double atk = dead instanceof Mob m && m.getAttribute(Attributes.ATTACK_DAMAGE) != null ? m.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0;
        double spd = dead.getAttribute(Attributes.MOVEMENT_SPEED) != null ? dead.getAttributeValue(Attributes.MOVEMENT_SPEED) : 0;
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(dead) > r * r || !hasBook(p)) continue;
            boolean pet = dead instanceof OwnableEntity o && p.getUUID().equals(o.getOwnerUUID());
            CorpseLedger l = ledger(p);
            CorpseLedger.Fallen f = new CorpseLedger.Fallen(id.toString(), dead.getName().getString(), dead.getMaxHealth(), atk, spd, dim,
                    dead.getX(), dead.getY(), dead.getZ(), now, pet);
            l.record(f);
            if (pet && Pacts.petWindowOpen(p.getUUID(), now)) {
                List<CorpseLedger.Fallen> again = l.takePets(now, 1);
                for (CorpseLedger.Fallen a : again) raise(p, a);
            }
            save(p, l);
        }
    }

    // ================================================================ raising
    /**
     * Raises one ledger entry as a Soul Corpse of 'owner': a fresh creature of the same type, scaled to the recorded health, owner-bound,
     * with its decay clock. At the spot where it fell when that is near and in the same dimension, else beside the owner. Null if the type
     * is gone, immune, or the world refused it. Older bodies of the owner crumble to keep within {@code corpse.maxActive}.
     */
    static LivingEntity raise(ServerPlayer owner, CorpseLedger.Fallen f) {
        if (owner == null || !owner.isAlive()) return null;
        ServerLevel level = owner.serverLevel();
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(f.type()));
        if (type.isEmpty() || type.get().is(CorpseTags.IMMUNE)) return null;
        try {
            Entity made = type.get().create(level);
            if (!(made instanceof LivingEntity living) || !raisable(living)) return null;
            boolean there = level.dimension().location().toString().equals(f.dim()) && owner.distanceToSqr(f.x(), f.y(), f.z()) < 40 * 40;
            Vec3 at = there ? new Vec3(f.x(), f.y(), f.z()) : owner.position().add(owner.getLookAngle().multiply(2.5, 0, 2.5));
            living.moveTo(at.x, at.y, at.z, owner.getYRot() + 180f, 0f);
            if (living instanceof Mob m) {
                m.finalizeSpawn(level, level.getCurrentDifficultyAt(owner.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                m.setPersistenceRequired();
            }
            AttributeInstance max = living.getAttribute(Attributes.MAX_HEALTH);
            if (max != null && f.maxHealth() > 0) max.setBaseValue(Math.min(f.maxHealth(), 280));
            living.setHealth(living.getMaxHealth());
            long now = level.getGameTime();
            int life = CorpseConfig.lifeTicks();
            CompoundTag t = new CompoundTag();
            t.putUUID("Owner", owner.getUUID());
            t.putLong("Born", now);
            t.putInt("Life", life);
            t.putString("Kind", "raised");
            living.getPersistentData().put(TAG, t);
            living.setCustomName(Component.literal("Soul Corpse of " + f.name()));
            living.setCustomNameVisible(false);
            if (!level.addFreshEntity(living)) return null;
            for (UUID old : RAISED.add(owner.getUUID(), living.getUUID(), CorpseConfig.maxActive())) crumbleById(owner.getServer(), old);
            mark(living, 0);
            CorpseVfx.raise(level, living.position(), Math.max(0.8f, Math.min(2.2f, living.getBbWidth() * 1.4f)), 34);
            level.playSound(null, living.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0f, 0.7f);
            return living;
        } catch (RuntimeException | LinkageError failed) {
            return null;
        }
    }

    /** Raises up to 'n' of the newest entries of the caster's ledger; returns how many stood up. */
    static int reanimate(ServerPlayer caster, int n) {
        CorpseLedger l = ledger(caster);
        List<CorpseLedger.Fallen> picked = l.take(caster.level().getGameTime(), n);
        save(caster, l);
        int made = 0;
        for (CorpseLedger.Fallen f : picked) if (raise(caster, f) != null) made++;
        return made;
    }

    /** True Soul Corpse's second window: re-raises the caster's pets and summons that fell within the pet window. */
    static int reraisePets(ServerPlayer caster) {
        CorpseLedger l = ledger(caster);
        List<CorpseLedger.Fallen> pets = l.takePets(caster.level().getGameTime(), CorpseConfig.petWindowTicks());
        save(caster, l);
        int made = 0;
        for (CorpseLedger.Fallen f : pets) if (raise(caster, f) != null) made++;
        return made;
    }

    /** Summons one of the named puppets beside the caster; an earlier one of the same name crumbles first. Null if the world refused. */
    static NamedCorpseEntity summonNamed(ServerPlayer owner, NamedCorpses kind) {
        ServerLevel level = owner.serverLevel();
        for (UUID id : NAMED.bodies(owner.getUUID())) {
            Entity e = find(owner.getServer(), id);
            if (e instanceof NamedCorpseEntity n && n.kind() == kind) { crumble(n); NAMED.remove(owner.getUUID(), id); }
        }
        NamedCorpseEntity n = CorpseEntities.type(kind).create(level);
        if (n == null) return null;
        Vec3 look = owner.getLookAngle();
        double dist = 2.2 + kind.width;
        n.moveTo(owner.getX() + look.x * dist, owner.getY(), owner.getZ() + look.z * dist, owner.getYRot() + 180f, 0f);
        n.setOwner(owner.getUUID());
        CompoundTag t = new CompoundTag();
        t.putUUID("Owner", owner.getUUID());
        t.putLong("Born", level.getGameTime());
        t.putInt("Life", CorpseConfig.namedLifeTicks());
        t.putString("Kind", kind.id);
        n.getPersistentData().put(TAG, t);
        n.setCustomName(Component.literal(kind.display));
        n.setCustomNameVisible(true);
        if (!level.addFreshEntity(n)) return null;
        for (UUID old : NAMED.add(owner.getUUID(), n.getUUID(), NamedCorpses.values().length)) crumbleById(owner.getServer(), old);
        mark(n, 0);
        CorpseVfx.raise(level, n.position(), Math.max(1.0f, kind.width * 2.0f), 40);
        level.playSound(null, n.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.2f, 0.55f);
        return n;
    }

    // ================================================================ the decay mark
    /** Puts (or updates) the {@code nusmp:soul_corpse} marker on a creature and tells everyone who sees it. The amplifier is the decay stage. */
    public static void mark(LivingEntity e, int stage) { mark(e, stage, false); }

    /** The same; 'resend' also repeats the packet when the stage did not change (every five seconds, so a client that missed one catches up). */
    static void mark(LivingEntity e, int stage, boolean resend) {
        if (e == null || e.level().isClientSide) return;
        int amp = CorpseDecay.amplifier(stage);
        MobEffectInstance cur = e.getEffect(CorpseEffects.holder());
        if (cur != null && cur.getAmplifier() == amp) {
            if (resend) broadcast(e, cur);
            return;
        }
        if (cur != null) e.removeEffect(CorpseEffects.holder());                  // an effect update never lowers the amplifier, so replace it
        MobEffectInstance inst = new MobEffectInstance(CorpseEffects.holder(), MobEffectInstance.INFINITE_DURATION, amp, true, false, false);
        e.addEffect(inst);
        broadcast(e, inst);
    }

    /** Takes the marker off (the decay is over, or the person is themselves again). */
    public static void unmark(LivingEntity e) {
        if (e == null || e.level().isClientSide) return;
        if (e.getEffect(CorpseEffects.holder()) != null) e.removeEffect(CorpseEffects.holder());
    }

    /** Vanilla only sends an effect to a tracker when the entity is first seen; an update is sent here so the look changes at once. */
    private static void broadcast(LivingEntity e, MobEffectInstance inst) {
        if (e.level() instanceof ServerLevel sl) sl.getChunkSource().broadcastAndSend(e, new ClientboundUpdateMobEffectPacket(e.getId(), inst, false));
    }

    // ================================================================ foes
    /** Is 'e' someone the owner's bodies should fight? Hostile monsters, anything hunting the owner or their friends, and a player only if the owner is already in a fight with them. */
    static boolean isFoe(ServerPlayer owner, LivingEntity e) {
        if (e == owner || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.isAlliedTo(owner)) return false;
        if (isCorpse(e) || e instanceof NamedCorpseEntity || e.getPersistentData().hasUUID("nusmp_relic_owner")) return false;
        if (e instanceof OwnableEntity o && owner.getUUID().equals(o.getOwnerUUID())) return false;
        if (Pacts.covers(owner.getUUID(), e)) return false;
        if (e instanceof Player p) {
            if (p.isCreative() || !owner.canHarmPlayer(p)) return false;
            return fighting(owner, p);
        }
        if (e instanceof Npc) return false;
        if (e instanceof Enemy || e.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) return true;
        if (fighting(owner, e)) return true;
        if (e instanceof Mob m) {
            LivingEntity t = m.getTarget();
            return t != null && (t == owner || Pacts.covers(owner.getUUID(), t) || owner.getUUID().equals(ownerOf(t)));
        }
        return false;
    }

    private static boolean fighting(ServerPlayer owner, LivingEntity e) {
        return (owner.getLastHurtByMob() == e && owner.tickCount - owner.getLastHurtByMobTimestamp() < 200)
                || (owner.getLastHurtMob() == e && owner.tickCount - owner.getLastHurtMobTimestamp() < 200);
    }

    /** The nearest foe of the owner within 16 blocks of the body, or null. */
    static LivingEntity pickFoe(ServerPlayer owner, LivingEntity body) {
        if (owner == null || !owner.isAlive() || !(body.level() instanceof ServerLevel sl) || owner.level() != sl) return null;
        List<LivingEntity> found = sl.getEntitiesOfClass(LivingEntity.class, body.getBoundingBox().inflate(16, 6, 16), e -> e != body && isFoe(owner, e));
        return found.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(body))).orElse(null);
    }

    // ================================================================ the sweep
    static void tick(MinecraftServer server) {
        if ((RAISED.isEmpty() && NAMED.isEmpty()) || server.getTickCount() % SWEEP != 7) return;
        sweep(server, RAISED, false);
        sweep(server, NAMED, true);
    }

    private static void sweep(MinecraftServer server, CorpseRoster<UUID, UUID> roster, boolean named) {
        for (UUID ownerId : roster.owners()) {
            ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
            LivingEntity pick = null;
            for (UUID id : roster.bodies(ownerId)) {
                Entity e = find(server, id);
                if (!(e instanceof LivingEntity body) || !body.isAlive() || !isCorpse(body)) { roster.remove(ownerId, id); continue; }
                ServerLevel level = (ServerLevel) body.level();
                long now = level.getGameTime();
                CompoundTag t = body.getPersistentData().getCompound(TAG);
                long born = t.getLong("Born");
                int life = t.getInt("Life");
                if (owner == null || !owner.isAlive() || owner.level() != level || now - born >= life) { crumble(body); roster.remove(ownerId, id); continue; }
                int stage = CorpseDecay.stage(born, now, life);
                mark(body, stage, body.tickCount % CorpseDecay.STAGE_REFRESH_TICKS < SWEEP);
                if (!named && body.tickCount % 20 < SWEEP) bleed(body, life);
                if (body.tickCount % WISP_EVERY < SWEEP) wisp(level, body, stage);
                if (!named && body instanceof Mob m) drive(m, owner, now);
            }
        }
    }

    /** Health only goes down: one second of the slow bleed, never to death (the clock ends the body). */
    private static void bleed(LivingEntity body, int life) {
        float next = (float) CorpseDecay.afterBleed(body.getHealth(), body.getMaxHealth(), life);
        if (next < body.getHealth()) body.setHealth(next);
    }

    private static void wisp(ServerLevel level, LivingEntity body, int stage) {
        level.sendParticles(RukhParticles.dark(), body.getX(), body.getY() + body.getBbHeight() * 0.6, body.getZ(), 1 + stage / 2,
                body.getBbWidth() * 0.4, body.getBbHeight() * 0.35, body.getBbWidth() * 0.4, 0.01);
    }

    /** Points a reanimated body at a foe, walks it there and lands a blow when in reach (only if its own AI has not landed one lately). */
    private static void drive(Mob m, ServerPlayer owner, long now) {
        if (m.getPersistentData().contains("nusmp_paladin_turn")) return;
        try {
            LivingEntity target = m.getTarget();
            if (target == null || !target.isAlive() || !isFoe(owner, target)) {
                target = pickFoe(owner, m);
                m.setTarget(target);
                if (target != null) m.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
            }
            if (target == null) {
                if (m.distanceToSqr(owner) > 64) m.getNavigation().moveTo(owner, 1.2);
                return;
            }
            if (!(m instanceof RangedAttackMob)) m.getNavigation().moveTo(target, 1.2);
            CompoundTag d = m.getPersistentData();
            if (now - d.getLong("nusmp_corpse_strike") >= 20 && m.tickCount - m.getLastHurtMobTimestamp() > 30
                    && m.getBoundingBox().inflate(1.1, 0.7, 1.1).intersects(target.getBoundingBox()) && m.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                d.putLong("nusmp_corpse_strike", now);
                m.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                m.doHurtTarget(target);
            }
        } catch (RuntimeException | LinkageError ignored) {
            // a creature of an unexpected shape keeps its own mind
        }
    }

    // ================================================================ the end
    /** A body comes apart: a puff of pale soul, no drops, no experience. */
    static void crumble(Entity e) {
        if (e == null || e.isRemoved()) return;
        if (e.level() instanceof ServerLevel sl) {
            sl.sendParticles(RukhParticles.dark(), e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 14, e.getBbWidth() * 0.5, e.getBbHeight() * 0.4, e.getBbWidth() * 0.5, 0.04);
            sl.playSound(null, e.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.NEUTRAL, 0.8f, 1.2f);
        }
        e.discard();
    }

    private static void crumbleById(MinecraftServer server, UUID id) {
        if (server != null) crumble(find(server, id));
    }

    static Entity find(MinecraftServer server, UUID id) {
        if (server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }

    /** Crumbles every body of one owner (they logged out). */
    static void crumbleAllOf(MinecraftServer server, UUID owner) {
        for (UUID id : RAISED.bodies(owner)) crumbleById(server, id);
        for (UUID id : NAMED.bodies(owner)) crumbleById(server, id);
        for (UUID id : RAISED.bodies(owner)) RAISED.remove(owner, id);
        for (UUID id : NAMED.bodies(owner)) NAMED.remove(owner, id);
    }

    /** The server is stopping: every body goes now, so none is saved with the world. */
    static void discardAll(MinecraftServer server) {
        for (UUID o : RAISED.owners()) crumbleAllOf(server, o);
        for (UUID o : NAMED.owners()) crumbleAllOf(server, o);
        RAISED.clear();
        NAMED.clear();
        LEDGERS.clear();
        Pacts.clear();
    }

    /** True if the roster knows this body (a tagged body that joins a level without being listed is an orphan from a crash). */
    static boolean listed(Entity e) {
        UUID owner = ownerOf(e);
        return owner != null && (RAISED.bodies(owner).contains(e.getUUID()) || NAMED.bodies(owner).contains(e.getUUID()));
    }

    /** Damage source helper for the hooks: the body behind a blow (a direct hit or its projectile), or null. */
    static LivingEntity bodyBehind(DamageSource source) {
        Entity by = source.getEntity();
        return by instanceof LivingEntity le && isCorpse(le) ? le : null;
    }
}
