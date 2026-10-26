package com.newuniverse.nusmp.skilltree.item;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.rarity.BookRarity;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Why: the owner wants the unlock of a route to be an event ("an anime-style unlock"), and it must be the SAME event whichever way the route was unlocked: a book, a
 * command, the Black Market, a quest. That is why this class is not called by the book: it is registered as a listener of the one door ({@code SkillTreeApi}), which
 * announces every successful unlock with its {@code announce} flag, so the show cannot be forgotten by a new way of unlocking and is silent for a quiet one (a reload
 * that re-grants routes). The show is two stages, because an anime power-up has a build and a payoff. Stage one, at once: the pages turn, the runes whisper, a magic
 * circle in the route's colour opens on the ground and charges, motes of the same colour are drawn into the reader, and the packet is sent to the reader's client
 * (which waits for the payoff before it flashes the screen and slams the banner). Stage two, {@link UnlockTiming#BURST_TICK} ticks later, exactly when the circle bursts: a
 * pillar of light, a lens flare, the fanfare and a low chime, and an aura of spirit spirals round the reader for a few seconds. Everything visual goes through the mod's
 * own effect system ({@code VfxSpawn}) and nothing through vanilla particles (purged in 1.36.0); the sounds are vanilla sound events. Stage two is a small queue
 * of (due tick, player id, colour) drained from the server tick: the effect system has no delay of its own, a scheduled task must not keep a player object alive, and
 * the queue is cleared when the server stops, so no state outlives a world. A player who logged out or died in the meantime simply gets no second stage.
 * Server thread only; no method here throws into the caller (the API must never fail because of a show).
 */
public final class UnlockFx {
    private UnlockFx() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** The second stage of one show: when it is due (server tick), for whom, in which colour, whether the fanfare toast is part of it, and how strong the burst is (1.0 = the 3.25 show). */
    private record Pending(int due, UUID player, int argb, boolean toast, float power) {}

    private static final List<Pending> PENDING = new ArrayList<>();
    /** At most this many second stages wait at once (a reader waits one second and the read has a cooldown: this is only a safety net against a flood). */
    private static final int MAX_PENDING = 512;

    /** The listener the skill-tree API calls after every unlock: plays the show for {@code announce}, nothing otherwise. */
    public static void play(ServerPlayer player, String routeId, int rgb, boolean announce) {
        play(player, routeId, rgb, announce, null);
    }

    /**
     * The same show for a book of a rarity (3.26.0): the circle, the motes, the pillar, the flare and the aura are drawn in the RARITY's colour and a little stronger for each rank (the number of
     * effects is the same, so the cost is the same); the fanfare toast is left out of stage two because {@code BookUse} plays it at the moment of reading. A null rarity is the old show exactly.
     */
    public static void play(ServerPlayer player, String routeId, int rgb, boolean announce, BookRarity rarity) {
        if (!announce || player == null || routeId == null) return;
        try {
            if (rarity == null) stageOne(player, routeId, rgb, true, 1.0f);
            else stageOne(player, routeId, rarity.particleColor(), false, 1.0f + 0.12f * rarity.rank());
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[nusmp] the skill-learned show failed (the skill is learned all the same)", e);
        }
    }

    private static void stageOne(ServerPlayer player, String routeId, int rgb, boolean toast, float power) {
        ServerLevel level = player.serverLevel();
        int argb = 0xFF000000 | (rgb & 0xFFFFFF);
        Vec3 feet = player.position();
        MinecraftServer server = level.getServer();
        if (server != null && PENDING.size() < MAX_PENDING) PENDING.add(new Pending(server.getTickCount() + UnlockTiming.BURST_TICK, player.getUUID(), argb, toast, power));     // first: the payoff comes even if a build effect fails
        try {
            PacketDistributor.sendToPlayer(player, new RouteUnlockedPayload(routeId));                                             // second: the banner is the part the reader cannot miss
        } catch (RuntimeException | LinkageError e) {
            LOG.debug("[nusmp] could not send the skill-learned packet (a stand-in player has no connection)", e);
        }
        sound(level, feet, SoundEvents.BOOK_PAGE_TURN, 1.0f, 0.9f);
        sound(level, feet, SoundEvents.ENCHANTMENT_TABLE_USE, 1.2f, 0.8f);
        // the circle charges for 60% of its life and bursts: its length is chosen so the burst lands on the second stage
        fx(level, VfxShape.MAGIC_CIRCLE_EXPLOSION, feet.add(0, 0.05, 0), feet.add(0, 1.05, 0), argb, UnlockTiming.circleTicks(), 1.45f * power, -1);
        fx(level, VfxShape.MANA_CHARGE, feet.add(0, 0.95, 0), feet.add(0, 1.95, 0), argb, UnlockTiming.BURST_TICK, 1.2f * power, player.getId());
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        MinecraftServer server = event.getServer();
        int now = server.getTickCount();
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            if (p.due() > now) continue;
            it.remove();
            try {
                ServerPlayer player = server.getPlayerList().getPlayer(p.player());
                if (player != null && player.isAlive()) stageTwo(player, p.argb(), p.toast(), p.power());
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("[nusmp] the second stage of the route-unlocked show failed", e);
            }
        }
    }

    /** Forgets the queue when the server stops, so nothing outlives the world. */
    public static void onServerStopped(ServerStoppedEvent event) { PENDING.clear(); }

    private static void stageTwo(ServerPlayer player, int argb, boolean toast, float power) {
        ServerLevel level = player.serverLevel();
        Vec3 feet = player.position();
        // the strike form of the pillar (an even seed): the full column and flash, not the dim telegraph
        fx(level, VfxShape.DIVINE_PILLAR, feet, feet.add(0, 1, 0), argb, 26, 1.3f * power, -1);
        fx(level, VfxShape.LIGHT_FLARE, feet.add(0, 1.0, 0), feet.add(0, 1.0, 0), argb, 16, 2.0f * power, -1);
        fx(level, VfxShape.SPIRIT_AURA, feet.add(0, 1.0, 0), feet.add(0, 1.0, 0), argb, UnlockTiming.AURA_TICKS, 1.2f * power, player.getId());
        if (toast) sound(level, feet, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        sound(level, feet, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 0.5f);
        sound(level, feet, SoundEvents.BEACON_POWER_SELECT, 0.7f, 1.3f);
    }

    /** One effect with an even seed (the full form of shapes that also have a telegraph form), optionally following an entity. */
    private static void fx(ServerLevel level, VfxShape shape, Vec3 from, Vec3 to, int argb, int ticks, float power, int followEntity) {
        long seed = level.random.nextLong() & ~1L;
        VfxSpawn.send(level, new VfxPayload(shape.ordinal(), from, to, argb, ticks, power, followEntity, seed));
    }

    private static void sound(ServerLevel level, Vec3 at, SoundEvent event, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, event, SoundSource.PLAYERS, volume, pitch);
    }

    /** How many second stages are waiting (for the self-test). */
    public static int pendingCount() { return PENDING.size(); }
}
