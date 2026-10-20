package com.newuniverse.nusmp.labyrinth.mega.admin;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Why: the one registration entry of the {@code /dungeon} admin tree, in the shape every builder of the Mega-Dungeon uses ({@code MegaSystem} calls it with the game bus, one
 * line; listeners are added with {@code addListener}, no annotation scanning). It adds: the command tree on the dispatcher ({@code RegisterCommandsEvent}; Brigadier merges it with
 * the player's {@code /dungeon}), the clock that advances the three kinds of slow work (teleport tickets, skill jobs, particle bursts) once per server tick, the clean-up of a
 * player's open work when they log out, the clean-up of everything when the server stops, and the CI self-test {@code dungeon.admin}. The clock never throws: a failure is logged
 * once and the next tick tries again, because an exception out of a tick listener would stop the server.
 *
 * <p>WIRING of the two systems written by other hands (add these two lines when they exist; until then the commands use the default mechanisms of the dungeon, see
 * {@link DungeonAdminHooks}):
 * <pre>
 *   DungeonAdminHooks.setBossHook(DungeonAdminHooks.BossHook.of(DecennialBosses::spawnBoss, DecennialBosses::killBosses));
 *   DungeonAdminHooks.setRiftHook(RiftAdmin::spawnRift);
 * </pre>
 * (the method references need {@code boolean spawnBoss(ServerLevel, int, BlockPos)}, {@code int killBosses(ServerLevel, ServerPlayer)} and {@code boolean spawnRift(ServerLevel,
 * BlockPos, boolean)}; for a facade method of another return type use a lambda that calls it and returns the matching value).
 */
public final class AdminRegistry {
    private AdminRegistry() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean warned;

    public static void register(IEventBus gameBus) {
        gameBus.addListener((RegisterCommandsEvent e) -> e.getDispatcher().register(AdminTree.node()));
        gameBus.addListener((ServerTickEvent.Post e) -> tick(e.getServer()));
        gameBus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> forget(e.getEntity().getUUID()));
        gameBus.addListener((ServerStoppedEvent e) -> {
            FloorTeleport.clear();
            SkillAdmin.clear();
            WfcDebugService.clear();
            warned = false;
        });
        SelfTests.register("dungeon.admin", AdminSelfTests::run);
        // 1.99.0: the Decennial bosses and their thematic halls answer /dungeon boss spawn and kill
        DungeonAdminHooks.setBossHook(DungeonAdminHooks.BossHook.of(
                (l, lv, p) -> com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialBosses.spawnBoss(l, lv, p) != null,
                com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialBosses::killBosses));
    }

    private static void tick(MinecraftServer server) {
        try {
            FloorTeleport.tick(server);
            SkillAdmin.tick(server);
            WfcDebugService.tick(server);
        } catch (RuntimeException e) {
            if (!warned) {
                warned = true;
                LOG.warn("[nusmp] The /dungeon admin clock failed (further failures are not logged): {}", e.toString());
            }
        }
    }

    private static void forget(UUID id) {
        FloorTeleport.forget(id);
        SkillAdmin.forget(id);
        WfcDebugService.forget(id);
    }
}
