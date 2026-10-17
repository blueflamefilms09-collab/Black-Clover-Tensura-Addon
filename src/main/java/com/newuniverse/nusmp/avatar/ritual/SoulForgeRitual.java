package com.newuniverse.nusmp.avatar.ritual;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.avatar.AvatarConfig;
import com.newuniverse.nusmp.avatar.AvatarCreation;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarManager;
import com.newuniverse.nusmp.avatar.AvatarSoulForgeConfig;
import com.newuniverse.nusmp.avatar.AvatarSystem;
import com.newuniverse.nusmp.avatar.aptitude.Aptitude;
import com.newuniverse.nusmp.avatar.aptitude.AptitudeCatalog;
import com.newuniverse.nusmp.avatar.aptitude.AptitudeRoll;
import com.newuniverse.nusmp.avatar.aptitude.ForgeChoices;
import com.newuniverse.nusmp.avatar.aptitude.ForgePlan;
import com.newuniverse.nusmp.avatar.aptitude.ForgeRules;
import com.newuniverse.nusmp.avatar.item.GenesisCoreItem;
import com.newuniverse.nusmp.avatar.item.SoulForgeBlocks;
import com.newuniverse.nusmp.avatar.luck.LuckPoller;
import com.newuniverse.nusmp.avatar.menu.AvatarBuilderMenu;
import com.newuniverse.nusmp.avatar.menu.SoulForgeMenu;
import com.newuniverse.nusmp.avatar.net.RaceEntryFactory;
import com.newuniverse.nusmp.avatar.net.RollLimiter;
import com.newuniverse.nusmp.avatar.net.S2C_RacesPayload;
import com.newuniverse.nusmp.avatar.net.S2C_SoulForgeBirthPayload;
import com.newuniverse.nusmp.avatar.net.SoulForgeNetServer;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Random;
import java.util.UUID;

/**
 * Why: the Soul Forge ritual is the owner's "cinematic" replacement for the item-click creation screen, and this is its server-side director. One
 * {@link Ritual} per player who used a Soul Forge (or, with {@code coreUse = "item"}, a Genesis Core): the player is LOCKED in place ({@link RitualLock},
 * crash-safe: transient modifiers only), the great circle is drawn ({@link RitualFx}), after {@code ritualSeconds} the Soul Forge screen opens
 * ({@link SoulForgeMenu}), and the ritual ends the moment anything goes wrong: the screen is closed (the CANCEL PATH: nothing was taken, so nothing needs
 * to be returned, the lock is simply lifted), the player is hurt, dies, strays beyond the reach, changes dimension, logs out, breaks the forge, or keeps
 * the screen open longer than {@code maxOpenMinutes}. When the creator presses Finalize the server checks everything first ({@link AvatarCreation#precheckForge}
 * with FRESH reserves, so a bad request is turned away before any show), closes the screen and plays the birth spectacle on the pure {@link RitualClock}'s
 * beats: the circle charges and a thin beam warns, the beam strikes (and the camera shakes), the Avatar is created at the moment of the strike's aftermath
 * ({@link AvatarCreation#finalizeForge} checks it all AGAIN and only then takes the magicule, the Genesis Core(s) and applies the tax), it speaks and casts
 * its first ability ({@link BirthSpectacle}), and the lock is lifted. Because the real commit happens at the spawn beat and not at the click, a creator who
 * dies, logs out or is knocked away during the first second of the spectacle loses nothing. All state lives in one map keyed by player, cleared when the
 * server stops; nothing is saved.
 */
public final class SoulForgeRitual {
    private SoulForgeRitual() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<UUID, Ritual> ACTIVE = new HashMap<>();
    private static final RollLimiter START_LIMIT = new RollLimiter();
    private static final Random TOKENS = new Random();
    /** How often (ticks) the circle is redrawn while the screen is open, so people standing nearby keep seeing it. */
    private static final int SIGIL_REFRESH = 80;

    /** One player's ritual. Public fields are read-only facts for the net handlers; the mutable ones are touched on the server thread only. */
    public static final class Ritual {
        public final UUID player;
        public final long token;
        public final BlockPos forge;
        public final ResourceKey<Level> dimension;
        public final Vec3 center, anchor;
        final RitualClock clock;
        String rolledAptitude = "";
        long rolledToken;
        int rolls;
        AvatarCreation.CreationRequest request;
        long statToken;
        AvatarCreation.ForgeInput input;
        AvatarEntity avatar;
        ForgePlan plan;

        Ritual(ServerPlayer p, BlockPos forge, Vec3 center) {
            this.player = p.getUUID();
            this.token = TOKENS.nextLong() | 1L;
            this.forge = forge;
            this.dimension = p.level().dimension();
            this.center = center;
            this.anchor = p.position();
            this.clock = new RitualClock(AvatarSoulForgeConfig.chargeTicks(), AvatarSoulForgeConfig.maxOpenTicks(), AvatarSoulForgeConfig.birth());
        }

        public String rolledAptitude() { return rolledAptitude; }

        public long rolledToken() { return rolledToken; }

        public int rollsLeft() { return Math.max(0, AvatarSoulForgeConfig.rules().rollsMax() - rolls); }

        public boolean open() { return clock.phase() == RitualClock.Phase.OPEN; }
    }

    // ================================================================ entry points
    /**
     * The player used a Soul Forge ({@code forge} is its position) or a Genesis Core on its own ({@code forge} null, only when {@code coreUse = "item"}).
     * Opens the classic screen when the config says so, otherwise starts the ritual if the player may.
     */
    public static void interact(ServerPlayer p, BlockPos forge) {
        if (!AvatarConfig.enabled()) { p.displayClientMessage(note("message.nusmp.avatar.disabled", "Avatars are disabled on this server.", ChatFormatting.RED), true); return; }
        if (!AvatarSoulForgeConfig.soulForgeUi()) { AvatarBuilderMenu.open(p); return; }
        if (ACTIVE.containsKey(p.getUUID()) || !p.isAlive() || p.isSpectator() || p.isPassenger() || p.isSleeping()) return;
        if (!START_LIMIT.allow(p.getUUID(), System.currentTimeMillis(), AvatarSoulForgeConfig.startGapMillis())) return;
        int have = AvatarManager.get(p.getServer()).count(p.getUUID());
        if (have >= AvatarConfig.maxAvatarsPerPlayer()) {
            p.displayClientMessage(Component.translatableWithFallback("message.nusmp.avatar.forge.limit", "You already have %s Avatar(s): the Soul Forge has nothing more for you.", have)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!GenesisCoreItem.hasCore(p)) {
            p.displayClientMessage(note("message.nusmp.avatar.forge.hungry", "The Soul Forge hungers for a Genesis Core.", ChatFormatting.LIGHT_PURPLE), true);
            return;
        }
        start(p, forge);
    }

    private static void start(ServerPlayer p, BlockPos forge) {
        Vec3 center = forge == null ? p.position() : new Vec3(forge.getX() + 0.5, forge.getY() + 0.02, forge.getZ() + 0.5);
        Ritual r = new Ritual(p, forge, center);
        ACTIVE.put(p.getUUID(), r);
        p.getData(AvatarSystem.SESSION).clear();
        RitualLock.apply(p);
        if (p.level() instanceof ServerLevel sl) RitualFx.begin(sl, center, AvatarSoulForgeConfig.chargeTicks());
        p.displayClientMessage(note("message.nusmp.avatar.forge.begin", "The Soul Forge awakens. Stand still.", ChatFormatting.LIGHT_PURPLE), true);
        NusmpEvents.fire(p, "avatar.ritual_started");
    }

    /** The player's ritual, or null. */
    public static Ritual of(ServerPlayer p) { return ACTIVE.get(p.getUUID()); }

    /** The ritual whose screen this player has open right now (their open menu is its menu and it is in the OPEN phase), or null. */
    public static Ritual withScreenOpen(ServerPlayer p) {
        if (!(p.containerMenu instanceof SoulForgeMenu m)) return null;
        Ritual r = ACTIVE.get(p.getUUID());
        return r != null && r.token == m.ritualToken() && r.open() ? r : null;
    }

    /** True while the ritual that opened this menu still runs (the menu asks every tick; false closes the screen). */
    public static boolean menuStillValid(ServerPlayer p, SoulForgeMenu menu) {
        Ritual r = ACTIVE.get(p.getUUID());
        return r != null && r.token == menu.ritualToken() && r.clock.phase() != RitualClock.Phase.CANCELLED;
    }

    /** The screen was closed. While it is open that is the cancel path; when the server closed it itself to start the birth it is nothing. */
    public static void onMenuRemoved(ServerPlayer p, SoulForgeMenu menu) {
        Ritual r = ACTIVE.get(p.getUUID());
        if (r == null || r.token != menu.ritualToken()) return;
        if (r.open()) end(p, r, true, note("message.nusmp.avatar.forge.abandoned", "You leave the ritual. Nothing was taken.", ChatFormatting.GRAY), false);
    }

    // ================================================================ the free aptitude roll
    /** The result of a free roll: the aptitude, its token, the rolls left and a seed for the reel; {@code refusal} is set instead when nothing was rolled. */
    public record AptitudeRolled(String aptitude, long token, int rollsLeft, int reelSeed, String refusal) {
        public boolean ok() { return refusal == null; }
    }

    /**
     * Rolls the free aptitude, decided here on the server: the weights are the rarity classes' ({@link AptitudeRoll}), shifted towards the rare ones by the
     * creator's luck. The aptitudes the creator already picked by hand are left out of the pool. A new roll replaces the previous one (up to {@code aptitudeRolls}
     * rolls per ritual), so rerolling is free but limited. The id never leaves this class except in the answer; Finalize refers to it by token only.
     */
    public static AptitudeRolled rollAptitude(ServerPlayer p, List<String> picked) {
        Ritual r = withScreenOpen(p);
        if (r == null) return refused("The Soul Forge is not open.");
        ForgeRules rules = AvatarSoulForgeConfig.rules();
        if (rules.maxAptitudes() <= 0) return refused("This world gives Avatars no aptitudes.");
        if (picked.size() >= rules.maxAptitudes()) return refused("Your hand picks already fill every aptitude slot.");
        if (r.rolls >= rules.rollsMax()) return refused("You have no free rolls left.");
        List<Aptitude> pool = new ArrayList<>(AptitudeCatalog.all());
        pool.removeIf(a -> picked.contains(a.id()));
        Random rng = new Random(p.getRandom().nextLong() ^ System.nanoTime());
        Aptitude got = AptitudeRoll.pick(pool, LuckPoller.total(p), rules.luckStrength(), rng);
        if (got == null) return refused("There is no aptitude left to roll.");
        r.rolls++;
        r.rolledAptitude = got.id();
        r.rolledToken = rng.nextLong() | 1L;
        return new AptitudeRolled(got.id(), r.rolledToken, Math.max(0, rules.rollsMax() - r.rolls), rng.nextInt(), null);
    }

    private static AptitudeRolled refused(String why) { return new AptitudeRolled("", 0L, 0, 0, why); }

    private static Component note(String key, String fallback, ChatFormatting colour) { return Component.translatableWithFallback(key, fallback).withStyle(colour); }

    // ================================================================ Finalize
    /**
     * The creator pressed Finalize with the screen open. Everything is checked now against fresh reserves; a refusal is returned (and nothing changes) so the
     * screen can show it. On success the screen is closed and the birth spectacle begins; the commit itself happens at the spectacle's spawn beat.
     */
    public static AvatarCreation.Result finalizeRequest(ServerPlayer p, AvatarCreation.CreationRequest req, long statToken, ForgeChoices choices) {
        Ritual r = withScreenOpen(p);
        if (r == null) return new AvatarCreation.Result(false, "The Soul Forge is not open.");
        AvatarCreation.ForgeInput in = new AvatarCreation.ForgeInput(choices, r.rolledAptitude, r.rolledToken, spotFor(p, r));
        AvatarCreation.Result pre = AvatarCreation.precheckForge(p, req, statToken, in);
        if (!pre.ok()) return pre;
        if (!r.clock.accept()) return new AvatarCreation.Result(false, "The ritual is not ready.");
        r.request = req;
        r.statToken = statToken;
        r.input = in;
        p.closeContainer();
        return new AvatarCreation.Result(true, "");
    }

    /** Where the Avatar steps out: on top of the forge when there is room, else beside the creator (null). Faces the creator. */
    private static AvatarCreation.Spot spotFor(ServerPlayer p, Ritual r) {
        if (r.forge == null || !(p.level() instanceof ServerLevel sl)) return null;
        BlockPos a = r.forge.above(), b = a.above();
        if (!sl.getBlockState(a).getCollisionShape(sl, a).isEmpty() || !sl.getBlockState(b).getCollisionShape(sl, b).isEmpty()) return null;
        double x = r.forge.getX() + 0.5, y = r.forge.getY() + 0.75, z = r.forge.getZ() + 0.5;
        float yaw = (float) (Mth.atan2(-(p.getX() - x), p.getZ() - z) * (180.0 / Math.PI));
        return new AvatarCreation.Spot(x, y, z, yaw);
    }

    // ================================================================ the clock
    /** Every server tick: each running ritual keeps its guard conditions and advances its clock. */
    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        for (Ritual r : new ArrayList<>(ACTIVE.values())) {
            ServerPlayer p = server.getPlayerList().getPlayer(r.player);
            if (p == null) { ACTIVE.remove(r.player); continue; }
            if (!p.isAlive() || p.level().dimension() != r.dimension) { end(p, r, true, null); continue; }
            if (p.position().distanceToSqr(r.anchor) > AvatarSoulForgeConfig.reach() * AvatarSoulForgeConfig.reach()) {
                end(p, r, true, note("message.nusmp.avatar.forge.strayed", "You moved away from the circle: the ritual breaks. Nothing was taken.", ChatFormatting.RED));
                continue;
            }
            if (r.forge != null && !p.level().getBlockState(r.forge).is(SoulForgeBlocks.SOUL_FORGE.get())) {
                end(p, r, true, note("message.nusmp.avatar.forge.gone", "The Soul Forge is gone: the ritual breaks.", ChatFormatting.RED));
                continue;
            }
            RitualClock.Event e = r.clock.tick();
            switch (e) {
                case OPEN_MENU -> openScreen(p, r);
                case TIMEOUT -> end(p, r, true, note("message.nusmp.avatar.forge.timeout", "The ritual has waited too long and fades. Nothing was taken.", ChatFormatting.GRAY));
                case TELL -> { if (p.level() instanceof ServerLevel sl) RitualFx.tell(sl, r.center, r.clock.birth().strike(), plansAntiMagic(r)); }
                case STRIKE -> strike(p, r);
                case SPAWN -> commit(p, r);
                case NOVA -> { if (r.avatar != null && r.plan != null) BirthSpectacle.nova(r.avatar, r.plan); }
                case FINISH -> end(p, r, false, null);
                default -> {
                    if (r.open() && r.clock.age() % SIGIL_REFRESH == 0 && p.level() instanceof ServerLevel sl) RitualFx.circle(sl, r.center, SIGIL_REFRESH + 20);
                }
            }
        }
    }

    private static boolean plansAntiMagic(Ritual r) {
        return r.input != null && (r.input.choices().aptitudePicks().contains("anti_magic") || "anti_magic".equals(r.rolledAptitude) && r.input.choices().rollToken() != 0L);
    }

    private static void openScreen(ServerPlayer p, Ritual r) {
        OptionalInt id = p.openMenu(new SimpleMenuProvider((cid, inv, pl) -> new SoulForgeMenu(cid, r.token),
                Component.translatableWithFallback("nusmp.avatar.forge.menu_title", "The Soul Forge")));
        if (id.isEmpty()) {
            end(p, r, true, note("message.nusmp.avatar.forge.noscreen", "The Soul Forge could not open its screen.", ChatFormatting.RED));
            return;
        }
        PacketDistributor.sendToPlayer(p, new S2C_RacesPayload(RaceEntryFactory.build(p)));
        SoulForgeNetServer.sendState(p, r);
    }

    private static void strike(ServerPlayer p, Ritual r) {
        if (!(p.level() instanceof ServerLevel sl)) return;
        RitualFx.strike(sl, r.center, plansAntiMagic(r));
        if (AvatarSoulForgeConfig.effects() && AvatarSoulForgeConfig.birthShake() > 0f) {
            PacketDistributor.sendToPlayersNear(sl, null, r.center.x, r.center.y, r.center.z, 48.0,
                    new S2C_SoulForgeBirthPayload(r.center.x, r.center.y, r.center.z, 2.0f * AvatarSoulForgeConfig.birthShake(), 16));
        }
    }

    /** The spawn beat: the real creation, checked and paid now. A failure is told plainly and ends the ritual with nothing taken. */
    private static void commit(ServerPlayer p, Ritual r) {
        if (r.request == null || r.input == null) { end(p, r, true, null); return; }
        AvatarCreation.ForgeResult res = AvatarCreation.finalizeForge(p, r.request, r.statToken, r.input);
        if (!res.ok()) {
            end(p, r, true, Component.literal("The ritual fails: " + res.message() + " Nothing was taken.").withStyle(ChatFormatting.RED));
            return;
        }
        r.avatar = res.avatar();
        r.plan = res.plan();
        if (p.level() instanceof ServerLevel sl) RitualFx.emerge(sl, r.avatar.position(), BirthSpectacle.colour(r.avatar, r.plan));
        LOG.debug("[nusmp] Soul Forge: {} created Avatar {}", p.getGameProfile().getName(), r.avatar.getUUID());
    }

    // ================================================================ ending
    /**
     * Ends a ritual: the lock is lifted, the state dropped, the open screen closed, the roll session cleared, and (for a cancel) the circle let go and the
     * reason told. {@code cancelled} false is the happy end after the birth. Safe to call twice.
     */
    private static void end(ServerPlayer p, Ritual r, boolean cancelled, Component reason) { end(p, r, cancelled, reason, true); }

    /** {@code closeScreen} is false when the screen is already closing (the menu's own {@code removed} called us). */
    private static void end(ServerPlayer p, Ritual r, boolean cancelled, Component reason, boolean closeScreen) {
        if (ACTIVE.remove(r.player) == null) return;
        r.clock.cancel();
        RitualLock.release(p);
        p.getData(AvatarSystem.SESSION).clear();
        if (closeScreen && !p.hasDisconnected() && p.containerMenu instanceof SoulForgeMenu) p.closeContainer();
        if (cancelled) {
            if (p.level() instanceof ServerLevel sl) RitualFx.cancelled(sl, r.center);
            if (reason != null) p.displayClientMessage(reason, false);
        }
    }

    /** Ends the player's ritual from outside the tick (a hit, a death, a logout, a dimension change). Nothing happens when they have none. */
    public static void cancel(ServerPlayer p, String why) {
        Ritual r = ACTIVE.get(p.getUUID());
        if (r == null) return;
        Component msg = why == null ? null : Component.literal(why).withStyle(ChatFormatting.RED);
        // a logout or death must not try to talk to a player who is going away
        end(p, r, true, p.isAlive() && !p.hasDisconnected() ? msg : null);
    }

    /** True while this player is in a ritual (for the damage hook, which must stay cheap). */
    public static boolean active(ServerPlayer p) { return !ACTIVE.isEmpty() && ACTIVE.containsKey(p.getUUID()); }

    /** The server stopped: nothing outlives it. */
    public static void clearAll() {
        ACTIVE.clear();
        START_LIMIT.clear();
    }
}
