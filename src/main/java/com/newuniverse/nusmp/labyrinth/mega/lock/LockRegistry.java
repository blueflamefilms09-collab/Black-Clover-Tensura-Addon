package com.newuniverse.nusmp.labyrinth.mega.lock;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: the one registration entry of the escape rules (1.94.0, the owner's "remove the random being locked in a room or add a way to get out"), in the shape every builder of
 * the Mega-Dungeon uses: {@code MegaSystem} calls {@link #init} (its config file and its self-test) and {@link #register} (every game-bus handler, added with
 * {@code addListener}: no annotation scanning, like the rest of the mod). The handlers are: the once-a-second clocks of the lock book and of the safe-spot memory, the click on a
 * release rune (an interact, an interact-at and a hit all count, once), the player's command {@code /dungeon}, and the clean-up on logout and when the server stops. The
 * operator's branch {@code lock} is returned by {@link #command()} for {@code MegaCommands}.
 */
public final class LockRegistry {
    private LockRegistry() {}

    public static void init(IEventBus modBus) {
        LockConfig.register();
        LockSelfTests.register();
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener((ServerTickEvent.Post e) -> {
            LockBook.onServerTick(e.getServer());
            UnstuckService.onServerTick(e.getServer());
        });
        gameBus.addListener((RegisterCommandsEvent e) -> e.getDispatcher().register(DungeonCommand.player()));
        gameBus.addListener((PlayerInteractEvent.EntityInteract e) -> {
            if (rune(e.getEntity(), e.getTarget())) {
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
            }
        });
        gameBus.addListener((PlayerInteractEvent.EntityInteractSpecific e) -> {
            if (rune(e.getEntity(), e.getTarget())) {
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
            }
        });
        gameBus.addListener((AttackEntityEvent e) -> {
            if (rune(e.getEntity(), e.getTarget())) e.setCanceled(true);
        });
        gameBus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            LockBook.forget(e.getEntity().getUUID());
            UnstuckService.forget(e.getEntity().getUUID());
        });
        gameBus.addListener((ServerStoppedEvent e) -> {
            LockBook.clearAll();
            UnstuckService.clear();
        });
    }

    /** The operator's branch for {@code /multiverse mega}: {@code lock list | release | unstuck | cases | reach}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() { return DungeonCommand.admin(); }

    /** Is the target one of the two parts of a rune? Then, on the server, the click is handled here. */
    private static boolean rune(Player player, Entity target) {
        if (!LockRunes.isRune(target)) return false;
        if (player instanceof ServerPlayer sp && !sp.level().isClientSide) {
            String key = LockRunes.keyOf(target);
            if (key == null) target.discard();
            else LockBook.useRune(sp, key);
        }
        return true;
    }
}
