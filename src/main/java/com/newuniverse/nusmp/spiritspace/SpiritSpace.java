package com.newuniverse.nusmp.spiritspace;

import com.newuniverse.nusmp.NUGameRules;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.spiritspace.SpaceMoodRules.Expression;
import com.newuniverse.nusmp.spiritspace.SpaceMoodRules.Mood;
import com.newuniverse.nusmp.spiritspace.SpaceMoodRules.Situation;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the Spirit of Space (docs/magic/spirit_space.md). A spatial mage can link a rare spirit that looks like a tear in reality: a floating starfield orb that circles them, and that swells into a
 * celestial dragon-like wisp for a few seconds whenever it works. It has three jobs, all decided here on the SERVER (the client only draws and asks), all capped:
 * <ul>
 *   <li>waypoints and portals: up to {@link SpiritSpaceConfig#waypoints()} named waypoints per player, and a portal to one of them inside the SAME dimension and the portal range, after a
 *       {@link SpiritSpaceRules#CHANNEL_TICKS}-tick channel, to a landing that {@link SpaceLanding} found safe in chunks that were already loaded;</li>
 *   <li>gravity: Levitation on a few nearby enemies, one scan per cast, through the balance law's control cap;</li>
 *   <li>the spatial cut (after Langris): deletes projectiles out of the air near the owner (a capped number, one scan per cast) and deals bounded damage to ONE target. It never removes a block.</li>
 * </ul>
 * All state is per player UUID in {@link #STATES} (only players who linked the spirit have an entry) and is dropped on logout, death, respawn, dimension change and server stop. Nothing here scans the
 * world on a tick: the upkeep (every {@link SpiritSpaceRules#UPKEEP_EVERY} ticks, only for the players in {@link #LIVE}) looks at the player's own entities by id; the only entity searches are the
 * gravity pulse and the cut, once per cast, capped; the only block reads are the landing search, once per portal, in loaded chunks.
 */
public final class SpiritSpace {
    private SpiritSpace() {}

    /** The state of one player's spirit. */
    static final class State {
        long lastPacket = Long.MIN_VALUE / 4, lastMessage = Long.MIN_VALUE / 4;
        long portalReadyAt, gravityReadyAt, cutReadyAt, wispReadyAt, ringsUntil, lastHum;
        boolean linked;
        UUID spirit, wisp, ringIn, ringOut;
        // the portal channel
        boolean channelling;
        int channelSlot;
        long channelBegan;
        double startX, startY, startZ;
        Waypoints.Waypoint[] book;
        // moods: when and which pose the spirit is in, the memory of when each situation last changed it, and how long the owner has stood still
        final SpaceMoodRules.Tracker mood = new SpaceMoodRules.Tracker();
        Mood currentMood = Mood.CALM;
        long moodAt;
        double lastX, lastY, lastZ;
        long stillSince;
    }

    static final Map<UUID, State> STATES = new HashMap<>();
    /** The players with anything live (a linked spirit, a channel, rings): only these are visited by the upkeep. */
    static final Set<UUID> LIVE = new HashSet<>();
    static final SpaceLedger SPIRITS = new SpaceLedger(), WISPS = new SpaceLedger(), PAIRS = new SpaceLedger();

    private static final ThemeKey SPACE = ThemeKey.of("magic.SPATIAL");
    private static final PerfProbe.Section PROBE_UPKEEP = PerfProbe.section("s.spirit_space.upkeep");
    /** Server thread scratch: the anchor being worked out, the distances of a scan, the picked indices. */
    /** The most entities one scan looks at, however many the box holds. */
    private static final int SCAN_CAP = 64;
    private static final double[] AT = new double[3];
    private static final double[] DIST = new double[SCAN_CAP];
    private static final int[] PICK = new int[SpiritSpaceRules.CUT_PROJECTILES_HARD];
    private static final Projectile[] SEEN = new Projectile[SCAN_CAP];
    private static final Mob[] MOBS = new Mob[SCAN_CAP];

    private static State state(UUID id) { return STATES.computeIfAbsent(id, k -> new State()); }

    // ================================================================================================ who may
    /** The switch is on, the player is not a spectator and (unless the config relaxes it) owns the Spatial Magic grimoire. */
    public static boolean allowed(ServerPlayer p) {
        if (!SpiritSpaceConfig.enabled() || p.isSpectator() || !p.isAlive()) return false;
        return !SpiritSpaceConfig.requireSpatialBook() || SkillAPI.getSkillsFrom(p).getSkill(NUSkills.BOOK_SPATIAL.getId()).isPresent();
    }

    private static void say(ServerPlayer p, State s, String key, String text, ChatFormatting color) {
        long now = p.level().getGameTime();
        if (!SpiritSpaceRules.gapOk(now, s.lastMessage, 12)) return;
        s.lastMessage = now;
        p.displayClientMessage(Component.translatableWithFallback("nusmp.spirit_space." + key, text).withStyle(color), true);
    }

    private static void sound(ServerLevel sl, ServerPlayer p, SoundGate.Kind k, Vec3 at, float volume, float pitch) {
        SpiritSpaceSounds.play(sl, p, k, at.x, at.y, at.z, volume, pitch);
    }

    // ================================================================================================ the key packet
    /** The packet arrived (already checked to come from a real player with a known action): rate limit, then the action. */
    public static void onKey(ServerPlayer p, int action, int ability, int slot) {
        State s = state(p.getUUID());
        long now = p.level().getGameTime();
        if (!SpiritSpaceRules.gapOk(now, s.lastPacket, SpiritSpaceRules.PACKET_GAP)) return;
        s.lastPacket = now;
        if (!SpiritSpaceRules.validAction(action)) return;
        if (!allowed(p)) {
            if (s.linked) unlink(p, s, false);
            if (action == SpiritSpaceRules.ACT_TOGGLE) say(p, s, "refused", "The Spirit of Space does not answer you.", ChatFormatting.GRAY);
            settle(p, s);
            if (!LIVE.contains(p.getUUID())) STATES.remove(p.getUUID(), s);
            return;
        }
        if (action == SpiritSpaceRules.ACT_TOGGLE) {
            if (s.linked) unlink(p, s, true); else link(p, s);
            return;
        }
        if (!SpiritSpaceRules.validAbility(ability)) return;
        if (!s.linked) { say(p, s, "notlinked", "Link the spirit first.", ChatFormatting.GRAY); return; }
        switch (ability) {
            case SpiritSpaceRules.A_SET -> setWaypoint(p, s, slot, null);
            case SpiritSpaceRules.A_PORTAL -> beginPortal(p, s, slot);
            case SpiritSpaceRules.A_GRAVITY -> gravity(p, s);
            default -> cut(p, s);
        }
    }

    // ================================================================================================ link and unlink
    static boolean link(ServerPlayer p, State s) {
        ServerLevel sl = p.serverLevel();
        if (!SPIRITS.acquire(p.getUUID(), SpiritSpaceRules.SPIRITS_PER_PLAYER, SpiritSpaceConfig.maxSpirits())) {
            say(p, s, "full", "The Spirit of Space cannot answer: too many spirits are out.", ChatFormatting.RED);
            return false;
        }
        MagicPropEntity e = spawnOrb(p, sl);
        if (e == null) { SPIRITS.release(p.getUUID()); return false; }
        s.spirit = e.getUUID();
        s.linked = true;
        LIVE.add(p.getUUID());
        Vec3 at = e.position();
        Fx.send(sl, SPACE, at.x, at.y, at.z, 10, 0.5);
        sound(sl, p, SoundGate.Kind.PING, at, 0.9f, 1.0f);
        sound(sl, p, SoundGate.Kind.HUM, at, 0.8f, 1.0f);
        say(p, s, "linked", "A tear in space opens beside you: the Spirit of Space answers.", ChatFormatting.LIGHT_PURPLE);
        s.lastX = p.getX(); s.lastY = p.getY(); s.lastZ = p.getZ(); s.stillSince = sl.getGameTime();
        mood(p, s, Situation.LINK);
        return true;
    }

    private static MagicPropEntity spawnOrb(ServerPlayer p, ServerLevel sl) {
        SpiritSpaceRules.orbitTo(AT, p.getX(), p.getY(), p.getZ(), p.yBodyRot, 0);
        MagicPropEntity e = MagicProps.spawn(sl, PropKind.SPIRITSPACE_1, new Vec3(AT[0], AT[1], AT[2]), p.yBodyRot, 1.0f, 1_000_000, SpiritSpaceRules.param(SpiritSpaceRules.V_ORB, 0), p);
        if (e != null) e.setTarget(p);
        return e;
    }

    static void unlink(ServerPlayer p, State s, boolean say) {
        ServerLevel sl = p.serverLevel();
        Vec3 at = p.position().add(0, 1.9, 0);
        if (say) { mood(p, s, Situation.UNLINK); farewell(sl, s); }                              // the orb winds into itself and goes out
        else { discard(sl, s.spirit); discard(sl, s.wisp); }
        if (s.spirit != null) SPIRITS.release(p.getUUID());
        if (s.wisp != null) WISPS.release(p.getUUID());
        s.spirit = null;
        s.wisp = null;
        s.linked = false;
        cancelChannel(p, s, false);
        if (say) {
            sound(sl, p, SoundGate.Kind.UNLINK, at, 0.8f, 1.0f);
            say(p, s, "unlinked", "The tear closes. The spirit returns to the space between.", ChatFormatting.GRAY);
        }
        settle(p, s);
    }

    /** Drops the player from the upkeep when nothing of theirs is live any more. */
    private static void settle(ServerPlayer p, State s) {
        if (!s.linked && s.spirit == null && s.wisp == null && !s.channelling && s.ringIn == null && s.ringOut == null) LIVE.remove(p.getUUID());
    }

    private static void discard(ServerLevel sl, UUID id) {
        MagicPropEntity e = prop(sl, id);
        if (e != null) e.discard();
    }

    // ================================================================================================ the powered form
    /** Calls the wisp for this work if it is allowed (cooldown, one per player, the server cap). True when it is out. */
    private static boolean summonWisp(ServerPlayer p, State s, long now) {
        if (s.wisp != null) return true;
        if (now < s.wispReadyAt) return false;
        if (!WISPS.acquire(p.getUUID(), SpiritSpaceRules.WISPS_PER_PLAYER, SpiritSpaceConfig.maxWisps())) return false;
        ServerLevel sl = p.serverLevel();
        SpiritSpaceRules.wispTo(AT, p.getX(), p.getY(), p.getZ(), p.yBodyRot);
        MagicPropEntity e = MagicProps.spawn(sl, PropKind.SPIRITSPACE_1, new Vec3(AT[0], AT[1], AT[2]), p.yBodyRot, 1.0f, SpiritSpaceRules.WISP_TICKS,
                SpiritSpaceRules.param(SpiritSpaceRules.V_WISP, SpiritSpaceRules.stampOf(now)), p);
        if (e == null) { WISPS.release(p.getUUID()); return false; }
        e.setTarget(p);
        s.wisp = e.getUUID();
        s.wispReadyAt = now + SpiritSpaceRules.WISP_TICKS + SpiritSpaceRules.WISP_COOLDOWN;
        MagicPropEntity orb = prop(sl, s.spirit);
        if (orb != null) orb.setParam(orb.param() | SpiritSpaceRules.HIDDEN);                 // one body per player: the orb hides while the wisp is out
        sound(sl, p, SoundGate.Kind.WISP, e.position(), 1.0f, 1.0f);
        return true;
    }

    // ================================================================================================ moods, poses and voice
    /**
     * A situation happened: the pose and the expression it gets (from {@link SpaceMoodRules}) go into the props' param, the clients play them; if the situation has a voice cue it is sent to the listeners
     * near the spirit. Cooldowns per situation and the rule that weak situations never interrupt a running pose live in the pure class; this is only the wiring.
     */
    static void mood(ServerPlayer p, State s, Situation sit) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        if (!s.mood.ready(sit, now) || !SpaceMoodRules.canReplace(sit, s.currentMood, (int) (now - s.moodAt))) return;
        s.mood.mark(sit, now);
        Expression e = SpaceMoodRules.expressionFor(sit);
        int stamp = SpiritSpaceRules.stampOf(now);
        boolean wisp = s.wisp != null;
        Mood orbMood = SpaceMoodRules.moodFor(sit, false, (int) (now / 400));
        Mood wispMood = SpaceMoodRules.moodFor(sit, true, (int) (now / 400));
        s.currentMood = wisp ? wispMood : orbMood;
        s.moodAt = now;
        MagicPropEntity o = prop(sl, s.spirit);
        if (o != null) o.setParam(SpaceMoodRules.withMood(o.param(), orbMood, e, stamp));
        MagicPropEntity w = prop(sl, s.wisp);
        if (w != null) w.setParam(SpaceMoodRules.withMood(w.param(), wispMood, e, stamp));
        Vec3 at = w != null ? w.position() : o != null ? o.position() : p.position().add(0, 1.9, 0);
        SpiritSpaceVoice.say(sl, p, s.mood, SpaceMoodRules.voiceFor(sit, (int) (now / 400)), at, 0.9f);
    }

    /** The orb sleeps when the owner has not moved for a minute (a quiet companion), and wakes with the next situation. Called from the upkeep. */
    private static void idle(ServerPlayer p, State s, long now) {
        double dx = p.getX() - s.lastX, dy = p.getY() - s.lastY, dz = p.getZ() - s.lastZ;
        if (dx * dx + dy * dy + dz * dz > 0.04) { s.lastX = p.getX(); s.lastY = p.getY(); s.lastZ = p.getZ(); s.stillSince = now; }
        if (s.wisp != null || s.channelling) return;
        if (SpaceMoodRules.sleepy(now - s.stillSince)) {
            if (s.currentMood != Mood.SLEEPY && SpaceMoodRules.canReplace(Situation.IDLE, s.currentMood, (int) (now - s.moodAt))) {
                s.currentMood = Mood.SLEEPY;
                s.moodAt = now;
                MagicPropEntity o = prop(p.serverLevel(), s.spirit);
                if (o != null) o.setParam(SpaceMoodRules.withMood(o.param(), Mood.SLEEPY, Expression.SLEEPY, SpiritSpaceRules.stampOf(now)));
                SpiritSpaceVoice.say(p.serverLevel(), p, s.mood, SpaceMoodRules.Voice.SIGH, p.position().add(0, 1.4, 0), 0.7f);
            }
        } else if (s.currentMood == Mood.SLEEPY) {
            s.currentMood = Mood.CALM;
            s.moodAt = now;
            MagicPropEntity o = prop(p.serverLevel(), s.spirit);
            if (o != null) o.setParam(SpaceMoodRules.withMood(o.param(), Mood.CURIOUS, Expression.EXCITED, SpiritSpaceRules.stampOf(now)));
        } else {
            if (SpaceMoodRules.ownerLow(p.getHealth(), p.getMaxHealth())) mood(p, s, Situation.OWNER_LOW);
            else mood(p, s, Situation.IDLE);
        }
    }

    /** The spirit winds into itself and goes out: the props show the farewell pose, then end 24 ticks later. */
    private static void farewell(ServerLevel sl, State s) {
        long now = sl.getGameTime();
        for (UUID id : new UUID[]{s.spirit, s.wisp}) {
            MagicPropEntity e = prop(sl, id);
            if (e == null) continue;
            e.setParam(SpaceMoodRules.withMood(e.param(), Mood.FAREWELL, Expression.SLEEPY, SpiritSpaceRules.stampOf(now)));
            SpellRuntime.later(sl, SpaceMoodRules.duration(Mood.FAREWELL) + 2, e::discard);
        }
    }

    // ================================================================================================ mana
    private static double cost(ServerPlayer p, double percent) {
        return BalanceLaw.cost(p, percent, SpiritSpaceRules.COST_FLOOR) * ManaLaw.costMult(p) * SpiritSpaceConfig.manaCostScale();
    }

    private static boolean canPay(ServerPlayer p, double amount) {
        if (p.isCreative() || amount <= 0) return true;
        var ex = TensuraStorages.getExistenceFrom(p);
        return ex != null && ex.getMagicule() >= amount;
    }

    private static void pay(ServerPlayer p, double amount) {
        if (p.isCreative() || amount <= 0) return;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return;
        ex.setMagicule(Math.max(0, ex.getMagicule() - amount));
        ex.markDirty();
    }

    private static boolean cooling(ServerPlayer p, State s, long readyAt, long now) {
        if (now >= readyAt) return false;
        say(p, s, "cooldown", "The spirit is still gathering itself.", ChatFormatting.GRAY);
        return true;
    }

    // ================================================================================================ waypoints
    private static Waypoints.Waypoint[] book(ServerPlayer p, State s) {
        if (s.book == null || s.book.length != SpiritSpaceConfig.waypoints()) s.book = WaypointStore.load(p);
        return s.book;
    }

    /** Sets the waypoint of a slot at the player's feet (the name keeps the slot's old name or becomes "Waypoint N"; a given name replaces it). */
    static boolean setWaypoint(ServerPlayer p, State s, int slot, String name) {
        Waypoints.Waypoint[] b = book(p, s);
        if (!SpiritSpaceRules.validSlot(slot, b.length)) { say(p, s, "slot", "There is no such waypoint slot.", ChatFormatting.RED); return false; }
        ServerLevel sl = p.serverLevel();
        if (!p.onGround() || p.isInWater() || p.isPassenger()) { say(p, s, "ground", "Stand on solid ground to mark a waypoint.", ChatFormatting.GRAY); return false; }
        if (Waypoints.checkSpot(p.getX(), p.getY(), p.getZ(), WaypointStore.minY(sl), WaypointStore.maxY(sl)) != Waypoints.OK) { say(p, s, "spot", "That place cannot be marked.", ChatFormatting.RED); return false; }
        String keep = b[slot] != null ? b[slot].name() : null;
        String n = Waypoints.cleanName(name != null ? name : keep, slot);
        b[slot] = new Waypoints.Waypoint(n, WaypointStore.dimensionId(sl), p.getX(), p.getY(), p.getZ(), p.getYRot());
        WaypointStore.save(p, b);
        Vec3 at = p.position();
        Fx.ring(sl, SPACE, at.x, at.y + 0.1, at.z, 0.9, 10);
        sound(sl, p, SoundGate.Kind.PING, at.add(0, 1, 0), 0.8f, 1.3f);
        p.displayClientMessage(Component.translatableWithFallback("nusmp.spirit_space.set", "Waypoint %s marked.", n).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    static boolean renameWaypoint(ServerPlayer p, State s, int slot, String name) {
        Waypoints.Waypoint[] b = book(p, s);
        if (!SpiritSpaceRules.validSlot(slot, b.length) || b[slot] == null) { say(p, s, "empty", "No waypoint is set in that slot.", ChatFormatting.GRAY); return false; }
        Waypoints.Waypoint w = b[slot];
        b[slot] = new Waypoints.Waypoint(Waypoints.cleanName(name, slot), w.dim(), w.x(), w.y(), w.z(), w.yaw());
        WaypointStore.save(p, b);
        return true;
    }

    static boolean clearWaypoint(ServerPlayer p, State s, int slot) {
        Waypoints.Waypoint[] b = book(p, s);
        if (!SpiritSpaceRules.validSlot(slot, b.length) || b[slot] == null) { say(p, s, "empty", "No waypoint is set in that slot.", ChatFormatting.GRAY); return false; }
        b[slot] = null;
        WaypointStore.save(p, b);
        return true;
    }

    /** The book as lines for the list command: slot, name, place. */
    static List<String> describe(ServerPlayer p, State s) {
        Waypoints.Waypoint[] b = book(p, s);
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (int i = 0; i < b.length; i++) {
            Waypoints.Waypoint w = b[i];
            out.add((i + 1) + ": " + (w == null ? "(empty)" : w.name() + "  " + w.dim() + "  " + Math.round(w.x()) + " " + Math.round(w.y()) + " " + Math.round(w.z())));
        }
        return out;
    }

    // ================================================================================================ the portal
    /** The slot's waypoint is checked, the channel begins. Nothing is spent yet: the mana is drawn when the portal opens. */
    static boolean beginPortal(ServerPlayer p, State s, int slot) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        if (s.channelling) { say(p, s, "channel", "The portal is already opening.", ChatFormatting.GRAY); return false; }
        if (cooling(p, s, s.portalReadyAt, now)) return false;
        Waypoints.Waypoint[] b = book(p, s);
        String why = Waypoints.checkTrip(b, slot, WaypointStore.dimensionId(sl), p.getX(), p.getY(), p.getZ(), SpiritSpaceConfig.portalRange());
        if (why != Waypoints.OK) { refuse(p, s, why); return false; }
        if (PAIRS.count(p.getUUID()) >= SpiritSpaceRules.PAIRS_PER_PLAYER || PAIRS.total() >= SpiritSpaceConfig.maxPairs()) { say(p, s, "pairs", "Too many portals are open.", ChatFormatting.RED); return false; }
        if (!canPay(p, cost(p, SpiritSpaceRules.PORTAL_COST_PERCENT))) { say(p, s, "mana", "Not enough mana to hold a portal open.", ChatFormatting.RED); return false; }
        s.channelling = true;
        s.channelSlot = slot;
        s.channelBegan = now;
        s.startX = p.getX();
        s.startY = p.getY();
        s.startZ = p.getZ();
        LIVE.add(p.getUUID());
        summonWisp(p, s, now);
        mood(p, s, Situation.PORTAL_CHANNEL);
        Vec3 at = p.position().add(0, 1.0, 0);
        sound(sl, p, SoundGate.Kind.SWELL, at, 1.0f, 1.0f);
        Fx.ring(sl, SPACE, p.getX(), p.getY() + 0.1, p.getZ(), 1.1, 12);
        say(p, s, "opening", "Space parts around " + b[slot].name() + "... hold still.", ChatFormatting.LIGHT_PURPLE);
        return true;
    }

    private static void refuse(ServerPlayer p, State s, String why) {
        switch (why) {
            case Waypoints.EMPTY -> say(p, s, "empty", "No waypoint is set in that slot.", ChatFormatting.GRAY);
            case Waypoints.OTHER_DIMENSION -> say(p, s, "dimension", "That waypoint lies in another dimension: the spirit cannot cross dimensions.", ChatFormatting.RED);
            case Waypoints.TOO_FAR -> say(p, s, "far", "That waypoint is beyond the reach of the portal.", ChatFormatting.RED);
            default -> say(p, s, "slot", "There is no such waypoint slot.", ChatFormatting.RED);
        }
    }

    private static void cancelChannel(ServerPlayer p, State s, boolean say) {
        if (!s.channelling) return;
        s.channelling = false;
        if (say) say(p, s, "broken", "The portal closes: you moved.", ChatFormatting.GRAY);
    }

    /** The channel ran its course: re-check everything, find the landing, open the two rings and step through. */
    private static void completePortal(ServerPlayer p, State s) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        s.channelling = false;
        Waypoints.Waypoint[] b = book(p, s);
        String why = Waypoints.checkTrip(b, s.channelSlot, WaypointStore.dimensionId(sl), p.getX(), p.getY(), p.getZ(), SpiritSpaceConfig.portalRange());
        if (why != Waypoints.OK) { refuse(p, s, why); return; }
        Waypoints.Waypoint w = b[s.channelSlot];
        if (!sl.isLoaded(BlockPos.containing(w.x(), w.y(), w.z()))) { say(p, s, "unloaded", "The far side is not there yet: nobody has been near it lately.", ChatFormatting.RED); return; }
        int[] land = SpaceLanding.find(new LoadedProbe(sl), (int) Math.floor(w.x()), (int) Math.floor(w.y()), (int) Math.floor(w.z()));
        if (land == null) { say(p, s, "nolanding", "There is no safe ground at that waypoint.", ChatFormatting.RED); return; }
        double cost = cost(p, SpiritSpaceRules.PORTAL_COST_PERCENT);
        if (!canPay(p, cost)) { say(p, s, "mana", "Not enough mana to hold a portal open.", ChatFormatting.RED); return; }
        if (!PAIRS.acquire(p.getUUID(), SpiritSpaceRules.PAIRS_PER_PLAYER, SpiritSpaceConfig.maxPairs())) { say(p, s, "pairs", "Too many portals are open.", ChatFormatting.RED); return; }
        pay(p, cost);
        double lx = land[0] + 0.5, ly = land[1], lz = land[2] + 0.5;
        Vec3 here = p.position();
        MagicPropEntity in = spawnRing(p, sl, SpiritSpaceRules.V_PORTAL_IN, here.x - Math.sin(Math.toRadians(p.getYRot())) * 0.9, here.y, here.z + Math.cos(Math.toRadians(p.getYRot())) * 0.9, p.getYRot());
        MagicPropEntity out = spawnRing(p, sl, SpiritSpaceRules.V_PORTAL_OUT, lx, ly, lz, w.yaw());
        if (in == null || out == null) {
            if (in != null) in.discard();
            if (out != null) out.discard();
            PAIRS.release(p.getUUID());
            return;
        }
        s.ringIn = in.getUUID();
        s.ringOut = out.getUUID();
        s.ringsUntil = now + SpiritSpaceRules.PORTAL_TICKS;
        s.portalReadyAt = now + SpiritSpaceConfig.portalCooldown();
        sound(sl, p, SoundGate.Kind.OPEN, here.add(0, 1, 0), 1.0f, 1.0f);
        Fx.burst(sl, SPACE, here.x, here.y + 1.0, here.z, 14, 0.6);
        if (p.isPassenger()) p.stopRiding();
        p.ejectPassengers();
        p.teleportTo(sl, lx, ly, lz, w.yaw(), p.getXRot());
        p.resetFallDistance();
        sound(sl, p, SoundGate.Kind.OPEN, new Vec3(lx, ly + 1.0, lz), 1.0f, 1.2f);
        Fx.burst(sl, SPACE, lx, ly + 1.0, lz, 14, 0.6);
        mood(p, s, Situation.PORTAL_OPEN);
        say(p, s, "arrived", "You step out at " + w.name() + ".", ChatFormatting.LIGHT_PURPLE);
    }

    private static MagicPropEntity spawnRing(ServerPlayer p, ServerLevel sl, int variant, double x, double y, double z, float yaw) {
        MagicPropEntity e = MagicProps.spawn(sl, PropKind.SPIRITSPACE_2, new Vec3(x, y, z), yaw, 1.0f, SpiritSpaceRules.PORTAL_TICKS, SpiritSpaceRules.param(variant, SpiritSpaceRules.stampOf(sl.getGameTime())), p);
        return e;
    }

    // ================================================================================================ gravity
    static boolean gravity(ServerPlayer p, State s) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        if (cooling(p, s, s.gravityReadyAt, now)) return false;
        double cost = cost(p, SpiritSpaceRules.GRAVITY_COST_PERCENT);
        if (!canPay(p, cost)) { say(p, s, "mana", "Not enough mana.", ChatFormatting.RED); return false; }
        if (!BalanceLaw.beginControl(p)) { say(p, s, "control", "Your control magic needs " + (BalanceLaw.controlCooldownLeft(p) + 19) / 20 + "s.", ChatFormatting.GRAY); return false; }
        pay(p, cost);
        s.gravityReadyAt = now + SpiritSpaceRules.GRAVITY_COOLDOWN;
        summonWisp(p, s, now);
        double r = SpiritSpaceConfig.gravityRadius();
        int lifted = lift(p, sl, r, SpiritSpaceConfig.gravityTargets());
        mood(p, s, Situation.GRAVITY);
        Vec3 c = p.position();
        sound(sl, p, SoundGate.Kind.GRAVITY, c.add(0, 1, 0), 1.0f, 1.0f);
        Fx.ring(sl, SPACE, c.x, c.y + 0.2, c.z, Math.min(r, 6.0), 18);
        say(p, s, "gravity", lifted + (lifted == 1 ? " enemy" : " enemies") + " float free of the ground.", ChatFormatting.LIGHT_PURPLE);
        return true;
    }

    /** One scan: the hostile mobs near the player (at most {@link #SCAN_CAP} looked at), the nearest {@code cap} of them get Levitation through the control cap. Returns how many. */
    private static int lift(ServerPlayer p, ServerLevel sl, double r, int cap) {
        AABB box = p.getBoundingBox().inflate(r, r * 0.6, r);
        List<Mob> found = sl.getEntitiesOfClass(Mob.class, box, e -> e instanceof Enemy && e.isAlive() && !e.isAlliedTo(p) && e.distanceToSqr(p) <= r * r);
        int n = Math.min(found.size(), SCAN_CAP);
        for (int i = 0; i < n; i++) {
            MOBS[i] = found.get(i);
            DIST[i] = MOBS[i].distanceToSqr(p);
        }
        int got = SpiritSpaceRules.nearest(DIST, n, cap, PICK);
        for (int k = 0; k < got; k++) {
            Mob t = MOBS[PICK[k]];
            t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, BalanceLaw.controlTicks(t, SpiritSpaceRules.LEVITATION_TICKS), 0, false, true));
            Fx.send(sl, SPACE, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 3, 0.3);
        }
        for (int i = 0; i < n; i++) MOBS[i] = null;                                          // the scratch keeps no references
        return got;
    }

    // ================================================================================================ the spatial cut
    static boolean cut(ServerPlayer p, State s) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        if (cooling(p, s, s.cutReadyAt, now)) return false;
        double cost = cost(p, SpiritSpaceRules.CUT_COST_PERCENT);
        if (!canPay(p, cost)) { say(p, s, "mana", "Not enough mana.", ChatFormatting.RED); return false; }
        pay(p, cost);
        s.cutReadyAt = now + SpiritSpaceRules.CUT_COOLDOWN;
        summonWisp(p, s, now);
        double r = SpiritSpaceConfig.cutRadius();
        int cap = SpiritSpaceConfig.cutProjectiles();
        int deleted = deleteProjectiles(p, sl, r, cap);
        mood(p, s, Situation.CUT);
        LivingEntity target = cutTarget(p);
        Vec3 from = p.getEyePosition();
        if (target != null) {
            double dmg = SpiritSpaceRules.cutDamage(SpiritSpaceConfig.cutDamage(), BalanceLaw.epScale(p), NUGameRules.spellDamage(sl), ManaLaw.powerMult(p));
            float f = BalanceLaw.cap(target, (float) dmg);
            if (f > 0f) target.hurt(sl.damageSources().indirectMagic(p, p), f);
            Vec3 tc = target.getBoundingBox().getCenter();
            VfxSpawn.send(sl, VfxShape.SPATIAL_RIFT, tc, tc, 0x8A5CFF, 14, 0.8f);
            Fx.burst(sl, SPACE, tc.x, tc.y, tc.z, 10, 0.5);
        }
        sound(sl, p, SoundGate.Kind.TEAR, from, 1.0f, 1.0f);
        say(p, s, "cut", deleted + (deleted == 1 ? " projectile" : " projectiles") + " cut out of the air" + (target != null ? ", and the space around your target is cut." : "."), ChatFormatting.LIGHT_PURPLE);
        return true;
    }

    /** One scan for projectiles, capped; the nearest {@code cap} of the foes' are removed (the vanilla ones), then the mod's own spell bolts for what is left of the cap. Never touches a block. */
    private static int deleteProjectiles(ServerPlayer p, ServerLevel sl, double r, int cap) {
        List<Projectile> found = sl.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(r), pr -> !(pr instanceof FishingHook) && pr.isAlive()
                && pr.getOwner() != p && !(pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p)) && pr.distanceToSqr(p) <= r * r);
        int n = Math.min(found.size(), SCAN_CAP);
        for (int i = 0; i < n; i++) {
            SEEN[i] = found.get(i);
            DIST[i] = SEEN[i].distanceToSqr(p);
        }
        int got = SpiritSpaceRules.nearest(DIST, n, Math.min(cap, PICK.length), PICK);
        for (int k = 0; k < got; k++) {
            Projectile pr = SEEN[PICK[k]];
            Fx.send(sl, SPACE, pr.getX(), pr.getY(), pr.getZ(), 2, 0.2);
            pr.discard();
        }
        for (int i = 0; i < n; i++) SEEN[i] = null;
        int total = got;
        int left = SpiritSpaceRules.deleteCount(cap - got, cap);
        if (left > 0) total += SpellRuntime.dissolveBolts(sl, p.position(), r, p, left);
        return total;
    }

    private static LivingEntity cutTarget(ServerPlayer p) {
        LivingEntity t = GrimoireBook.target(p, SpiritSpaceRules.CUT_REACH);
        if (t == null || t == p || t instanceof ArmorStand || t.isSpectator() || t.isAlliedTo(p) || !t.isAlive()) return null;
        if (t instanceof Player pl && !p.canHarmPlayer(pl)) return null;
        return t;
    }

    // ================================================================================================ the upkeep and the cleanup
    /** Every few ticks for the players in {@link #LIVE}: the channel, lost props, the hum, the validity of the link. */
    public static void upkeep(ServerPlayer p) {
        long t0 = PerfProbe.begin();
        try {
            upkeep0(p);
        } finally {
            PerfProbe.end(PROBE_UPKEEP, t0);
        }
    }

    private static void upkeep0(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null) { LIVE.remove(p.getUUID()); return; }
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        if (s.wisp != null && prop(sl, s.wisp) == null) {
            s.wisp = null;
            WISPS.release(p.getUUID());
            MagicPropEntity orb = prop(sl, s.spirit);
            if (orb != null) orb.setParam(orb.param() & ~SpiritSpaceRules.HIDDEN);
        }
        if ((s.ringIn != null || s.ringOut != null) && now >= s.ringsUntil + 2) {
            discard(sl, s.ringIn);
            discard(sl, s.ringOut);
            if (s.ringIn != null) PAIRS.release(p.getUUID());
            s.ringIn = null;
            s.ringOut = null;
        }
        if (s.linked) {
            if (p.tickCount % 20 == 0 && !allowed(p)) { unlink(p, s, true); return; }
            if (prop(sl, s.spirit) == null) {                                              // lost (a chunk unloaded, a cleanup): the orb comes back, or the link ends
                MagicPropEntity e = spawnOrb(p, sl);
                if (e == null) { unlink(p, s, false); return; }
                s.spirit = e.getUUID();
                if (s.wisp != null) e.setParam(e.param() | SpiritSpaceRules.HIDDEN);
            }
            if (SpiritSpaceRules.gapOk(now, s.lastHum, 160)) {
                s.lastHum = now;
                sound(sl, p, SoundGate.Kind.HUM, p.position().add(0, 1.9, 0), 0.5f, 1.0f);
            }
            idle(p, s, now);
        }
        if (s.channelling) {
            if (SpiritSpaceRules.channelBroken(s.startX, s.startY, s.startZ, p.getX(), p.getY(), p.getZ())) cancelChannel(p, s, true);
            else if (SpiritSpaceRules.channelDone(now, s.channelBegan)) completePortal(p, s);
        }
        settle(p, s);
    }

    /** The player was hurt while a channel runs: the portal closes. */
    public static void onHurt(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null) return;
        if (s.channelling) cancelChannel(p, s, true);
        if (s.linked) mood(p, s, Situation.DAMAGED);
    }

    /** The owner killed something hostile: the spirit flares with delight (its own cooldown keeps a farm from making it spam). */
    public static void onKill(ServerPlayer p, LivingEntity victim) {
        State s = STATES.get(p.getUUID());
        if (s != null && s.linked && victim != p && victim instanceof Enemy) mood(p, s, Situation.OWNER_KILL);
    }

    /** The owner died: the spirit mourns (the farewell pose and a sigh), then everything of theirs is dropped. */
    public static void died(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s != null && s.linked && p.level() instanceof ServerLevel sl) {
            mood(p, s, Situation.OWNER_DIED);
            farewell(sl, s);
            s.spirit = null;                                                                       // the props end themselves after the farewell; the ledgers are cleared below
            s.wisp = null;
        }
        forget(p);
    }

    /** Everything of this player goes: logout, death, respawn, dimension change. */
    public static void forget(ServerPlayer p) {
        UUID id = p.getUUID();
        State s = STATES.remove(id);
        LIVE.remove(id);
        SPIRITS.forget(id);
        WISPS.forget(id);
        PAIRS.forget(id);
        SpiritSpaceSounds.GATE.forget(id);
        SpiritSpaceVoice.forgetListener(id);
        if (s == null || !(p.level() instanceof ServerLevel sl)) return;
        for (UUID e : new UUID[]{s.spirit, s.wisp, s.ringIn, s.ringOut}) discard(sl, e);
    }

    /** The server stops: nothing may survive into the next world. */
    public static void clearAll() {
        STATES.clear();
        LIVE.clear();
        SPIRITS.clear();
        WISPS.clear();
        PAIRS.clear();
        SpiritSpaceSounds.GATE.clear();
        SpiritSpaceVoice.clearAll();
    }

    /** A prop ended on its own (its life ran out, or it was an orphan): if it is the one the player's state names, give its slot back. */
    public static void propEnded(UUID owner, int variant, UUID entity) {
        State s = owner == null ? null : STATES.get(owner);
        if (s == null || entity == null) return;
        if (variant == SpiritSpaceRules.V_WISP && entity.equals(s.wisp)) {
            s.wisp = null;
            WISPS.release(owner);
        } else if (variant == SpiritSpaceRules.V_ORB && entity.equals(s.spirit)) {
            s.spirit = null;                                                                   // the upkeep brings the orb back while the link stands; the slot stays held
        } else if (variant == SpiritSpaceRules.V_PORTAL_IN && entity.equals(s.ringIn)) {
            s.ringIn = null;
            PAIRS.release(owner);
        } else if (variant == SpiritSpaceRules.V_PORTAL_OUT && entity.equals(s.ringOut)) {
            s.ringOut = null;
        }
    }

    /** True when the entity is the spirit or wisp the owner's state names (props of players who unlinked are orphans and end themselves). */
    static boolean isCurrent(UUID owner, int variant, UUID entity) {
        State s = STATES.get(owner);
        if (s == null) return false;
        return switch (variant) {
            case SpiritSpaceRules.V_ORB -> entity.equals(s.spirit);
            case SpiritSpaceRules.V_WISP -> entity.equals(s.wisp);
            default -> true;
        };
    }

    static MagicPropEntity prop(ServerLevel sl, UUID id) { return id != null && sl.getEntity(id) instanceof MagicPropEntity e && !e.isRemoved() ? e : null; }

    /** For the self-test: how many live things the ledgers count. */
    public static int liveCount() { return SPIRITS.total() + WISPS.total() + PAIRS.total(); }

    static State stateOf(ServerPlayer p) { return state(p.getUUID()); }

    static void markLive(ServerPlayer p) { LIVE.add(p.getUUID()); }
}
