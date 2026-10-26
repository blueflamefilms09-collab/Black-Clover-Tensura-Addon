package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the small pieces of server state Hercules' skill and events share: the leap waiting for its landing (Earth-Shattering Tremor), the seconds of movement and the stage of Unstoppable Momentum with its
 * two attribute modifiers, the end of Invictus (on the player's own data, as a game time), and the gaps that keep the pictures of the passives few. All per-player state is keyed by the player's UUID or kept on
 * the player's own data; every map holds only players who are using the skill right now (one small entry per owner) and every entry is dropped on death, logout, forgetting the skill and server stop. Server thread only.
 */
public final class HerculesState {
    private HerculesState() {}

    public static final String INVICTUS_UNTIL = "nusmp_hercules_invictus";
    private static final ResourceLocation ATTACK_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "hercules_momentum_attack");
    private static final ResourceLocation MOVE_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "hercules_momentum_speed");

    // ---------------------------------------------------------------- the leap that waits for the ground
    private record Leap(long until, ResourceKey<Level> dimension) {}

    private static final Map<UUID, Leap> LEAPS = new HashMap<>();

    /** The owner has leapt: the next landing within {@link HerculesRules#LEAP_WINDOW_TICKS} ticks is a tremor. */
    public static void leap(ServerPlayer p) {
        LEAPS.put(p.getUUID(), new Leap(p.level().getGameTime() + HerculesRules.LEAP_WINDOW_TICKS, p.level().dimension()));
    }

    /** True (and the leap is used up) when the owner leapt in this dimension and the time has not run out. */
    public static boolean takeLeap(ServerPlayer p) {
        Leap l = LEAPS.remove(p.getUUID());
        return l != null && l.until() >= p.level().getGameTime() && l.dimension().equals(p.level().dimension());
    }

    public static boolean leaping(UUID id) { return LEAPS.containsKey(id); }

    public static int leapCount() { return LEAPS.size(); }

    /** Drops a leap that ran out of time without a landing (once a second from the skill's own tick). */
    static void expireLeap(ServerPlayer p) {
        Leap l = LEAPS.get(p.getUUID());
        if (l != null && l.until() < p.level().getGameTime()) LEAPS.remove(p.getUUID());
    }

    // ---------------------------------------------------------------- the aftershock that waits its turn
    private record After(long at, ResourceKey<Level> dimension, double x, double y, double z) {}

    private static final Map<UUID, After> AFTER = new HashMap<>();

    /** The tremor landed at {@code at}: the ground heaves again {@link HerculesRules#AFTERSHOCK_DELAY_TICKS} ticks later (one pending aftershock per owner; a new tremor replaces it). */
    public static void aftershock(ServerPlayer p, net.minecraft.world.phys.Vec3 at) {
        AFTER.put(p.getUUID(), new After(p.level().getGameTime() + HerculesRules.AFTERSHOCK_DELAY_TICKS, p.level().dimension(), at.x, at.y, at.z));
    }

    public static int aftershockCount() { return AFTER.size(); }

    /** Every server tick: nothing when no aftershock is waiting (one emptiness test); otherwise the ones that are due are resolved (at most one per owner). */
    static void tickAftershocks(MinecraftServer server) {
        if (AFTER.isEmpty()) return;
        long now = server.overworld().getGameTime();
        List<Map.Entry<UUID, After>> due = null;
        for (Iterator<Map.Entry<UUID, After>> it = AFTER.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, After> e = it.next();
            if (e.getValue().at() > now) continue;
            if (due == null) due = new ArrayList<>(4);
            due.add(e);
            it.remove();
        }
        if (due == null) return;
        for (Map.Entry<UUID, After> e : due) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            ServerLevel l = server.getLevel(e.getValue().dimension());
            if (p == null || l == null || !p.isAlive() || p.level() != l) continue;
            HerculesSkill.aftershock(p, l, new net.minecraft.world.phys.Vec3(e.getValue().x(), e.getValue().y(), e.getValue().z()));
        }
    }

    // ---------------------------------------------------------------- Unstoppable Momentum
    /** One owner's run: where they stood a second ago, how many seconds they have been moving, and the stage that gives. */
    private static final class Run {
        double x, z;
        int seconds, stage;
        boolean placed;
    }

    private static final Map<UUID, Run> RUNS = new HashMap<>();

    /**
     * Once a second for an owner with Unstoppable Momentum on: counts the seconds of continuous movement from the distance covered since the last look ({@link HerculesRules#momentumSeconds}), and moves the two
     * attribute modifiers to the stage. A steady second changes nothing. Returns the stage.
     */
    public static int momentum(ServerPlayer p) {
        Run r = RUNS.computeIfAbsent(p.getUUID(), k -> new Run());
        double moved = r.placed ? Math.hypot(p.getX() - r.x, p.getZ() - r.z) : 0.0;
        r.x = p.getX();
        r.z = p.getZ();
        r.placed = true;
        r.seconds = HerculesRules.momentumSeconds(r.seconds, moved);
        int stage = HerculesRules.momentumStage(r.seconds);
        if (stage != r.stage) {
            r.stage = stage;
            modifiers(p, stage);
        }
        return stage;
    }

    /** The stage Unstoppable Momentum stands at for this owner (0 when it is off or the owner stands). */
    public static int stage(UUID id) {
        Run r = RUNS.get(id);
        return r == null ? 0 : r.stage;
    }

    /** Unstoppable Momentum was switched off: the run is forgotten and the modifiers go. */
    public static void endMomentum(ServerPlayer p) {
        RUNS.remove(p.getUUID());
        modifiers(p, 0);
    }

    private static void modifiers(ServerPlayer p, int stage) {
        set(p, Attributes.ATTACK_SPEED, ATTACK_ID, stage * HerculesRules.ATTACK_SPEED_PER_STAGE);
        set(p, Attributes.MOVEMENT_SPEED, MOVE_ID, stage * HerculesRules.MOVE_SPEED_PER_STAGE);
    }

    private static void set(ServerPlayer p, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance inst = p.getAttribute(attribute);
        if (inst == null) return;
        AttributeModifier now = inst.getModifier(id);
        if (amount <= 0.0) {
            if (now != null) inst.removeModifier(id);
            return;
        }
        if (now != null && now.amount() == amount) return;
        if (now != null) inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** True when the owner carries the speed modifier of Unstoppable Momentum (the self-test reads it). */
    public static boolean hasSpeedModifier(ServerPlayer p) {
        AttributeInstance inst = p.getAttribute(Attributes.MOVEMENT_SPEED);
        return inst != null && inst.getModifier(MOVE_ID) != null;
    }

    // ---------------------------------------------------------------- Invictus
    /** Starts Invictus: the Hit Points of the owner cannot fall below one until {@link HerculesRules#INVICTUS_TICKS} ticks have passed. */
    public static void startInvictus(ServerPlayer p) { p.getPersistentData().putLong(INVICTUS_UNTIL, p.level().getGameTime() + HerculesRules.INVICTUS_TICKS); }

    public static void endInvictus(LivingEntity e) { e.getPersistentData().remove(INVICTUS_UNTIL); }

    public static boolean invictus(LivingEntity e) {
        long until = e.getPersistentData().getLong(INVICTUS_UNTIL);
        return until > 0L && until > e.level().getGameTime();
    }

    // ---------------------------------------------------------------- the gaps of the pictures
    public static final int GAP_SHOCK = 0, GAP_BODY = 1;
    private static final Map<UUID, long[]> GAPS = new HashMap<>();

    /** True when {@code gap} ticks have passed since this kind of picture was last taken for the owner (and the time is taken). */
    public static boolean take(UUID id, int kind, long now, int gap) {
        long[] g = GAPS.computeIfAbsent(id, k -> new long[]{Long.MIN_VALUE / 2, Long.MIN_VALUE / 2});
        if (now - g[kind] < gap) return false;
        g[kind] = now;
        return true;
    }

    // ---------------------------------------------------------------- clearing
    /** Everything this skill kept for the player when the skill is forgotten: the modifiers, the run, the leap, Invictus and the gaps. */
    public static void clear(ServerPlayer p) {
        modifiers(p, 0);
        endInvictus(p);
        leave(p);
    }

    /** Death and logout: the temporary things go (the toggles stay on the skill, the modifiers are transient). */
    public static void leave(ServerPlayer p) {
        LEAPS.remove(p.getUUID());
        RUNS.remove(p.getUUID());
        GAPS.remove(p.getUUID());
        AFTER.remove(p.getUUID());
        endInvictus(p);
    }

    /** Tests and the server stop: forgets every entry without touching a player. */
    static void clearAll() {
        LEAPS.clear();
        RUNS.clear();
        GAPS.clear();
        AFTER.clear();
    }

    /** The number of entries kept (the self-test checks it empties). */
    public static int kept() { return LEAPS.size() + RUNS.size() + GAPS.size() + AFTER.size(); }
}
