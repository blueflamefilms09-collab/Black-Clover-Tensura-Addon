package com.newuniverse.nusmp.book.ext;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 0.53: one place where the 37 attributes of the Black Clover expansion hook the game's events (passive defences, buffs that last, damage
 * rewrites), so no attribute has to touch NUSMP. An attribute registers its hooks once, from its own {@code prop.<Name>Props.init()}; the
 * dispatchers below are registered on the game bus by NUSMP. Every hook runs inside its own try/catch: a bug in one attribute is logged
 * and cannot break another one or the server tick.
 *
 * <p>Hooks run for EVERY player / entity, every time: return at once when the caster's persistent-data flag (end game time) is not set.</p>
 */
public final class AttributeEvents {
    private AttributeEvents() {}

    private static final Logger LOG = LogUtils.getLogger();

    private static final List<Consumer<LivingIncomingDamageEvent>> INCOMING = new ArrayList<>();
    private static final List<Consumer<LivingDamageEvent.Post>> DAMAGED = new ArrayList<>();
    private static final List<Consumer<LivingDeathEvent>> DEATH = new ArrayList<>();
    private static final List<Consumer<ServerPlayer>> PLAYER_TICK = new ArrayList<>();
    private static final List<Consumer<MinecraftServer>> SERVER_TICK = new ArrayList<>();
    private static final List<Consumer<ServerPlayer>> LOGOUT = new ArrayList<>();

    /** Before damage is applied: read or change it (e.setAmount, e.setCanceled), e.getSource(), e.getEntity() is the victim. */
    public static void incoming(Consumer<LivingIncomingDamageEvent> h) { INCOMING.add(h); }

    /** After damage was applied (e.getNewDamage()). */
    public static void damaged(Consumer<LivingDamageEvent.Post> h) { DAMAGED.add(h); }

    /** An entity died (e.getEntity(), e.getSource()). */
    public static void death(Consumer<LivingDeathEvent> h) { DEATH.add(h); }

    /** Every tick for every server player, after the player's own tick. */
    public static void playerTick(Consumer<ServerPlayer> h) { PLAYER_TICK.add(h); }

    /** Every server tick, once. */
    public static void serverTick(Consumer<MinecraftServer> h) { SERVER_TICK.add(h); }

    /** A player logged out (clean up per-player state held in static maps). */
    public static void logout(Consumer<ServerPlayer> h) { LOGOUT.add(h); }

    // ------------------------------------------------------------------ dispatchers (registered by NUSMP on the game bus)
    public static void onIncomingDamage(LivingIncomingDamageEvent e) { run(INCOMING, e, "incoming"); }

    public static void onDamaged(LivingDamageEvent.Post e) { run(DAMAGED, e, "damaged"); }

    public static void onDeath(LivingDeathEvent e) { run(DEATH, e, "death"); }

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (PLAYER_TICK.isEmpty() || !(e.getEntity() instanceof ServerPlayer p)) return;
        run(PLAYER_TICK, p, "playerTick");
    }

    public static void onServerTick(ServerTickEvent.Post e) { if (!SERVER_TICK.isEmpty()) run(SERVER_TICK, e.getServer(), "serverTick"); }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (LOGOUT.isEmpty() || !(e.getEntity() instanceof ServerPlayer p)) return;
        run(LOGOUT, p, "logout");
    }

    private static <T> void run(List<Consumer<T>> hooks, T event, String what) {
        for (int i = 0; i < hooks.size(); i++) {
            try {
                hooks.get(i).accept(event);
            } catch (Throwable t) {
                LOG.error("[nusmp] attribute hook '{}' #{} failed", what, i, t);
            }
        }
    }
}
