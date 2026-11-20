package com.newuniverse.nusmp.heroweapon.tracker;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.bond.HeroRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the server half of the Weapon Tracker. It owns no state of its own except the rate-limit table ({@link TrackerLimiter}); the truth is the hero register ({@code HeroRegistry}, who holds which weapon,
 * kept in the world file) and the settings ({@code holyUnique}, {@code vassalUnique}, {@code trackerShowNames}, {@code maxBonds}). A request or the command {@code /multiverse weapons} reads the register once
 * (21 weapons, one map lookup each, one name lookup per weapon that has a holder), runs the pure {@link TrackerRules} and sends one reply to that one player. Nothing runs per tick, nothing is broadcast, nothing
 * loads a chunk. The limiter entry of a player is dropped at logout and the table at server stop. Server thread only.
 */
public final class WeaponTrackerService {
    private WeaponTrackerService() {}

    private static final TrackerLimiter LIMITER = new TrackerLimiter();

    // ================================================================ the snapshot
    /** The table as {@code viewer} sees it now, from the real register and settings. */
    public static TrackerRules.Table snapshot(ServerPlayer viewer) {
        MinecraftServer server = viewer.getServer();
        Map<String, List<String>> holders = new HashMap<>();
        if (server != null) {
            for (HeroKind k : HeroKind.values()) {
                List<UUID> list = HeroRegistry.holdersOf(server, k);
                if (list.isEmpty()) continue;
                List<String> texts = new java.util.ArrayList<>(list.size());
                for (UUID u : list) texts.add(u.toString());
                holders.put(k.itemId(), texts);
            }
        }
        TrackerRules.Settings s = settings();
        TrackerRules.Viewer v = new TrackerRules.Viewer(viewer.getUUID().toString(), HeroBonds.kindOf(viewer));
        return TrackerRules.build(holders, s, v, text -> nameOf(server, text), text -> isOnline(server, text));
    }

    /** The settings as the config holds them now. */
    public static TrackerRules.Settings settings() {
        return new TrackerRules.Settings(HeroConfig.enabled(), HeroConfig.holyUnique(), HeroConfig.vassalUnique(), HeroConfig.trackerShowNames(), HeroConfig.maxBonds());
    }

    private static UUID parse(String text) {
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException notAUuid) {
            return null;
        }
    }

    private static boolean isOnline(MinecraftServer server, String uuidText) {
        UUID id = parse(uuidText);
        return server != null && id != null && server.getPlayerList().getPlayer(id) != null;
    }

    /** The holder's name, or "" when the register only knows the uuid (the name is then shown as "another player"). */
    private static String nameOf(MinecraftServer server, String uuidText) {
        UUID id = parse(uuidText);
        if (server == null || id == null) return "";
        String name = HeroRegistry.nameOf(server, id);
        return name == null || name.equals(uuidText) ? "" : name;
    }

    // ================================================================ requests
    /** A client asked for the table: answered at most once per two seconds, the rest is dropped without a reply. */
    public static void onRequest(ServerPlayer sp) {
        if (sp.connection == null) return;                                            // only a real, connected player is answered
        MinecraftServer server = sp.getServer();
        if (server == null || !LIMITER.allow(sp.getUUID(), server.getTickCount())) return;
        WeaponTrackerNet.send(sp, new WeaponTrackerNet.Reply(false, snapshot(sp)));
    }

    /** {@code /multiverse weapons}: sends the table with the order to open the screen. Returns 1 when sent, 0 when the player asked again within two seconds. */
    public static int open(ServerPlayer sp) {
        MinecraftServer server = sp.getServer();
        if (server == null || !LIMITER.allow(sp.getUUID(), server.getTickCount())) return 0;
        WeaponTrackerNet.send(sp, new WeaponTrackerNet.Reply(true, snapshot(sp)));
        return 1;
    }

    /** The command node, attached to {@code /multiverse} by one line in {@code MultiverseCommands}. Everyone may use it (it only reads the register). */
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("weapons").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (open(p) == 0) {
                ctx.getSource().sendFailure(Component.translatableWithFallback("anime_multiverse.weapon_tracker.too_soon", "Wait a moment before asking again."));
                return 0;
            }
            return 1;
        });
    }

    // ================================================================ cleanup
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { LIMITER.forget(e.getEntity().getUUID()); }

    static void onStopping(ServerStoppingEvent e) { LIMITER.clear(); }

    /** For the self-test: the limiter's size. */
    static int tracked() { return LIMITER.size(); }

    /** For the self-test: forgets a player. */
    static void forget(UUID id) { LIMITER.forget(id); }
}
