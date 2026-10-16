package com.newuniverse.nusmp.butoh;

import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Butoh dance is a state the dancer holds for a while (not a one-off cast), so something must remember who is dancing, which move
 * is on, pay the mana upkeep, shrink the hitbox and put everything back on every way out. This is that server-side state machine. It is kept in
 * memory only: the dance ends on logout, death, a dimension change, a stun, sleep, a time stop or empty mana, so nothing here is ever saved and
 * nothing can be left on a player after a reload. The moves come from the pure {@link DanceChoreographer}; the hitbox from {@link DanceScale}.
 */
public final class ButohDance {
    private ButohDance() {}

    static final ResourceLocation SCALE_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "butoh_dance_scale");
    /** How long a stuck hitbox may wait for room to grow back before it is forced back to normal (ticks). */
    private static final int GROW_BACK_LIMIT = 200;

    private static final class Session {
        final DanceChoreographer.Cursor cursor;
        final long startTick;
        final ResourceKey<Level> dimension;
        final DanceScale scale = new DanceScale();
        DanceMove applied;          // the move the hitbox currently matches
        DanceMove last;             // the move the ripples were last drawn for
        int side = 1;               // which side the next sidestep slips to (flips each time)

        Session(long seed, long startTick, ResourceKey<Level> dimension) {
            this.cursor = new DanceChoreographer.Cursor(seed);
            this.startTick = startTick;
            this.dimension = dimension;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    /** Players whose hitbox modifier is still on after the dance ended, waiting for room to grow back: uuid -> force-clear tick. */
    private static final Map<UUID, Long> PENDING_CLEAR = new HashMap<>();

    // ------------------------------------------------------------------ queries
    public static boolean dancing(LivingEntity e) { return e instanceof ServerPlayer p && SESSIONS.containsKey(p.getUUID()); }

    /** The state message for a player who starts watching this dancer (null when not dancing). */
    static DanceStatePayload stateOf(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        return s == null ? null : new DanceStatePayload(p.getId(), true, s.cursor.seed(), s.startTick);
    }

    /** The move on right now, or null when the player is not dancing. */
    public static DanceMove currentMove(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        return s == null ? null : s.cursor.at((int) Math.max(0, p.level().getGameTime() - s.startTick)).move();
    }

    // ------------------------------------------------------------------ start / stop
    /** The dance page: starts the dance, or ends it when it is already on. Returns true (the cast always counts). */
    public static boolean toggle(ServerPlayer p) {
        if (SESSIONS.containsKey(p.getUUID())) { stop(p, null); return true; }
        start(p);
        return true;
    }

    private static void start(ServerPlayer p) {
        long now = p.level().getGameTime();
        long seed = DanceChoreographer.seedFor(p.getUUID().getMostSignificantBits(), p.getUUID().getLeastSignificantBits(), now);
        Session s = new Session(seed, now, p.level().dimension());
        SESSIONS.put(p.getUUID(), s);
        PENDING_CLEAR.remove(p.getUUID());
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new DanceStatePayload(p.getId(), true, seed, now));
        p.displayClientMessage(Component.literal("The dance begins.").withStyle(net.minecraft.ChatFormatting.AQUA), true);
    }

    /** Ends the dance on every path (with a short reason for the player, or null for a quiet end). Safe to call when not dancing. */
    public static void stop(ServerPlayer p, String why) {
        Session s = SESSIONS.remove(p.getUUID());
        if (s == null) return;
        DanceScale.Op op = s.scale.update(false, null);
        if (op.kind() == DanceScale.Kind.CLEAR) clearScale(p);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new DanceStatePayload(p.getId(), false, s.cursor.seed(), s.startTick));
        if (why != null && !why.isEmpty()) p.displayClientMessage(Component.literal(why).withStyle(net.minecraft.ChatFormatting.GRAY), true);
    }

    /** Logout, server stop: no packets, no waiting for room; the modifier is removed outright (it is transient anyway). */
    public static void forget(ServerPlayer p) {
        SESSIONS.remove(p.getUUID());
        PENDING_CLEAR.remove(p.getUUID());
        AttributeInstance inst = p.getAttribute(Attributes.SCALE);
        if (inst != null) inst.removeModifier(SCALE_ID);
    }

    public static void clearAll() { SESSIONS.clear(); PENDING_CLEAR.clear(); }

    // ------------------------------------------------------------------ the tick
    /** Called every tick for every player by ButohEvents. Cheap when the player is not dancing. */
    public static void tick(ServerPlayer p) {
        UUID id = p.getUUID();
        if (!PENDING_CLEAR.isEmpty() && PENDING_CLEAR.containsKey(id)) growBack(p);
        Session s = SESSIONS.get(id);
        if (s == null) return;
        long now = p.level().getGameTime();
        if (!p.isAlive()) { forget(p); return; }
        if (p.level().dimension() != s.dimension) { stop(p, "The dance is lost between worlds."); return; }
        String block = interrupted(p);
        if (block != null) { stop(p, block); return; }

        long elapsed = now - s.startTick;
        DanceChoreographer.Step step = s.cursor.at((int) elapsed);
        DanceMove move = step.move();
        if (move != s.last) {
            s.last = move;
            VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BUTOH_DANCE_RIPPLE, p, p.position(), 0xFFB8E6FF, step.length() + 6, 1.0f + 0.12f * move.ordinal());
        }
        if (move != s.applied) applyScale(p, s, move);

        if (elapsed > 0 && elapsed % 20 == 0 && !upkeep(p)) stop(p, "Your mana runs dry; the dance falters.");
    }

    /** Stun, sleep, time stop, spectating: anything that takes the body out of the dancer's hands ends the dance. */
    private static String interrupted(ServerPlayer p) {
        if (p.isSpectator()) return "";
        if (p.isSleeping()) return "The dance is broken by sleep.";
        if (TimeStop.isFrozen(p)) return "The dance is frozen out of you.";
        if (p.level().getGameTime() % 5 != 0) return null;                                  // the effect lookups are cheap but not needed every tick
        for (String id : new String[] {"paralysis", "sleep", "petrification"}) {
            Holder<MobEffect> h = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("tensura", id)).map(x -> (Holder<MobEffect>) x).orElse(null);
            if (h != null && p.hasEffect(h)) return "The dance is broken: you cannot move.";
        }
        return null;
    }

    /** One second of upkeep: a fraction of the maximum magicule (config butoh.danceDrain). False when there is not enough. */
    private static boolean upkeep(ServerPlayer p) {
        double want = EnergyHelper.getMaxMagicule(p) * ButohConfig.danceDrain() * ManaLaw.costMult(p);
        if (want <= 0) return true;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null || ex.getMagicule() < want) return false;
        ex.setMagicule(ex.getMagicule() - want);
        ex.markDirty();
        return true;
    }

    // ------------------------------------------------------------------ the hitbox
    private static void applyScale(ServerPlayer p, Session s, DanceMove move) {
        AttributeInstance inst = p.getAttribute(Attributes.SCALE);
        if (inst == null || !ButohConfig.danceShrink()) { s.applied = move; return; }
        double target = DanceScale.amountFor(move);
        double have = s.scale.applied() ? s.scale.amount() : 0;
        if (target > have && !fits(p, inst.getValue() - have + target)) return;      // growing back needs room; try again next tick
        DanceScale.Op op = s.scale.update(true, move);
        if (op.kind() == DanceScale.Kind.SET) inst.addOrUpdateTransientModifier(new AttributeModifier(SCALE_ID, op.amount(), AttributeModifier.Operation.ADD_VALUE));
        s.applied = move;
    }

    /** Takes the modifier off: at once when the bigger body fits where the dancer stands, otherwise as soon as it does (or after a limit). */
    private static void clearScale(ServerPlayer p) {
        AttributeInstance inst = p.getAttribute(Attributes.SCALE);
        if (inst == null) return;
        AttributeModifier m = inst.getModifier(SCALE_ID);
        if (m == null) return;
        double after = inst.getValue() - m.amount();
        if (!p.isAlive() || fits(p, after)) inst.removeModifier(SCALE_ID);
        else PENDING_CLEAR.put(p.getUUID(), p.level().getGameTime() + GROW_BACK_LIMIT);
    }

    private static void growBack(ServerPlayer p) {
        AttributeInstance inst = p.getAttribute(Attributes.SCALE);
        AttributeModifier m = inst == null ? null : inst.getModifier(SCALE_ID);
        if (m == null || SESSIONS.containsKey(p.getUUID())) { PENDING_CLEAR.remove(p.getUUID()); return; }
        long deadline = PENDING_CLEAR.get(p.getUUID());
        if (p.level().getGameTime() >= deadline || fits(p, inst.getValue() - m.amount())) {
            inst.removeModifier(SCALE_ID);
            PENDING_CLEAR.remove(p.getUUID());
        }
    }

    /** Whether the player's body at scale {@code newScale} would stand free where it is now (no block inside it). */
    private static boolean fits(ServerPlayer p, double newScale) {
        AttributeInstance inst = p.getAttribute(Attributes.SCALE);
        double cur = inst == null ? 1.0 : inst.getValue();
        if (newScale <= cur + 1e-6 || cur <= 1e-6) return true;
        double r = newScale / cur;
        AABB bb = p.getBoundingBox();
        double cx = (bb.minX + bb.maxX) / 2, cz = (bb.minZ + bb.maxZ) / 2;
        double hw = bb.getXsize() * r / 2, hd = bb.getZsize() * r / 2;
        AABB grown = new AABB(cx - hw, bb.minY, cz - hd, cx + hw, bb.minY + bb.getYsize() * r, cz + hd);
        return p.level().noCollision(p, grown);
    }

    // ------------------------------------------------------------------ evasion
    /**
     * A blow was rolled to miss: the dancer slips one step sideways (alternating sides, only where there is room and ground) and an ink
     * after-image stays where the blow would have landed. Returns without moving when the sidestep is off or no safe place exists.
     */
    static void slip(ServerPlayer p, DamageSource source) {
        Session s = SESSIONS.get(p.getUUID());
        if (s == null) return;
        net.minecraft.server.level.ServerLevel level = p.serverLevel();
        Vec3 from = p.position();
        Vec3 side = sideAxis(p, source);
        Vec3 dest = null;
        if (ButohConfig.danceSidestep()) {
            for (int attempt = 0; attempt < 2 && dest == null; attempt++) {
                double sign = attempt == 0 ? s.side : -s.side;
                for (double d = 1.0; d >= 0.4; d -= 0.3) {
                    Vec3 c = from.add(side.scale(sign * d));
                    if (safeSpot(p, c)) { dest = c; s.side = (int) -sign; break; }
                }
            }
        }
        Vec3 to = dest == null ? from.add(side.scale(0.4 * s.side)) : dest;
        VfxSpawn.send(level, VfxShape.BUTOH_AFTERIMAGE, from, to, 0xFFB8E6FF, 16, 1.0f);
        if (dest != null) {
            p.teleportTo(dest.x, dest.y, dest.z);
            p.fallDistance = 0;
        }
        com.newuniverse.nusmp.boss.god.DodgeEvents.record(p);                           // 1.45.0: a dodge, for the god-tier Lucius's Time Stasis Traps
    }

    /** The horizontal direction at right angles to the blow (from the attacker if there is one, otherwise to the way the dancer faces). */
    private static Vec3 sideAxis(ServerPlayer p, DamageSource source) {
        Vec3 away;
        if (source.getEntity() != null) away = p.position().subtract(source.getEntity().position());
        else away = p.getViewVector(1f);
        away = new Vec3(away.x, 0, away.z);
        if (away.lengthSqr() < 1e-4) away = new Vec3(0, 0, 1);
        away = away.normalize();
        return new Vec3(-away.z, 0, away.x);
    }

    /** A place the dancer could stand: nothing solid in the body there and ground within a step and a half below it. */
    static boolean safeSpot(ServerPlayer p, Vec3 c) {
        Vec3 delta = c.subtract(p.position());
        AABB moved = p.getBoundingBox().move(delta);
        if (!p.level().noCollision(p, moved) || p.level().containsAnyLiquid(moved)) return false;
        return !p.level().noCollision(p, moved.move(0, -1.5, 0));
    }

    // ------------------------------------------------------------------ +15% damage while dancing
    /** The damage bonus factor (1.0 when the player is not dancing) for Butoh pages cast while the dance is on. */
    public static float castBonus(LivingEntity owner) {
        return dancing(owner) ? 1f + (float) ButohConfig.danceCastBonus() : 1f;
    }
}
