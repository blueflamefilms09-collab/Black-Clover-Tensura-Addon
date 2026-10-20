package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: the events that make a pocket realm play: the second-by-second clock (warnings and the collapse), the press of a puzzle plate (a right click on its button, handled here and cancelled so
 * the button's own redstone does nothing), the death of the secret boss (which opens the Vault), and the rescue of a player who logs in inside a pocket that no longer exists (a server that
 * restarted mid-pocket, or a pocket that collapsed while they were offline): they are sent to their waypoint or Floor 0 instead of standing in a void. Server thread only.
 */
public final class PocketEvents {
    private PocketEvents() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(PocketEvents::onTick);
        gameBus.addListener(PocketEvents::onUse);
        gameBus.addListener(net.neoforged.bus.api.EventPriority.LOW, PocketEvents::onDeath);        // 1.94.0: after the Mythic Trait and every other listener that may cancel the death: only a real death opens the vault
        gameBus.addListener(PocketEvents::onLogin);
        PocketSwitch.register(gameBus);                                                              // 1.101.0: the release lever of the realm and of the tombs
    }

    private static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 20 != 0) return;
        ServerLevel mega = MegaDungeon.level(e.getServer());
        if (mega == null) return;
        PocketRealm realm = PocketRealm.get(e.getServer());
        if (!realm.pockets().isEmpty()) realm.tick(mega);
    }

    private static void onUse(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer p)) return;
        int slot = RiftRules.slotAt(e.getPos().getX(), e.getPos().getZ());
        if (slot < 0) return;
        ServerLevel level = (ServerLevel) e.getLevel();
        PocketRealm realm = PocketRealm.get(level.getServer());
        PocketRealm.Pocket pocket = realm.at(slot);
        if (pocket == null || pocket.variantKind() != RiftRules.Variant.PUZZLE || pocket.solved) return;
        int[][] buttons = new int[pocket.plates.length][];
        for (int i = 0; i < buttons.length; i++) buttons[i] = new int[]{pocket.plates[i].getX(), pocket.plates[i].getY(), pocket.plates[i].getZ()};
        int plate = RiftRules.plateHit(buttons, e.getPos().getX(), e.getPos().getY(), e.getPos().getZ());      // 1.94.0: the button or the coloured block it stands on (the button is two pixels tall)
        if (plate < 0) return;
        e.setCanceled(true);
        e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        press(level, realm, pocket, p, plate, e.getPos());
    }

    /** One press of plate {@code plate} of a puzzle pocket: advances, solves (the bars lift) or resets and wakes the guards. Public so the self-test drives the same code the click does. */
    static RiftRules.Press press(ServerLevel level, PocketRealm realm, PocketRealm.Pocket pocket, ServerPlayer p, int plate, BlockPos at) {
        RiftRules.Puzzle puzzle = new RiftRules.Puzzle(RiftRules.puzzleOrder(pocket.seed), pocket.pressed);
        RiftRules.Press r = puzzle.press(plate);
        pocket.pressed = puzzle.done();
        realm.setDirty();
        switch (r) {
            case PROGRESS -> {
                level.playSound(null, at, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value(), net.minecraft.sounds.SoundSource.BLOCKS, 1.2f, 0.7f + 0.25f * puzzle.done());
                p.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.rift.puzzle.progress", "The plate hums (%s of 4).", puzzle.done()).withStyle(ChatFormatting.AQUA), true);
            }
            case SOLVED -> realm.clear(level, pocket, "puzzle");
            case WRONG -> {
                level.playSound(null, at, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value(), net.minecraft.sounds.SoundSource.BLOCKS, 1.4f, 0.5f);
                p.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.rift.puzzle.wrong", "Wrong. The plates go dark; something stirs.").withStyle(ChatFormatting.RED), true);
                p.hurt(level.damageSources().magic(), 3f);
                PocketBuilder.wake(level, at);
            }
        }
        return r;
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof LabyrinthGuardianEntity g) || !g.getTags().contains(PocketBuilder.BOSS_TAG) || !(g.level() instanceof ServerLevel level)) return;
        int slot = RiftRules.slotAt(g.getX(), g.getZ());
        if (slot < 0) return;
        PocketRealm realm = PocketRealm.get(level.getServer());
        PocketRealm.Pocket pocket = realm.at(slot);
        if (pocket == null || pocket.bossDead) return;
        realm.clear(level, pocket, "boss");
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.getServer() == null) return;
        if (RiftRules.slotAt(p.getX(), p.getZ()) < 0 || !(p.level() instanceof ServerLevel level) || !com.newuniverse.nusmp.labyrinth.Labyrinth.inMega(level)) return;
        if (PocketRealm.get(p.getServer()).at(RiftRules.slotAt(p.getX(), p.getZ())) != null) return;            // the pocket still stands: they may stay
        PocketRealm.leave(level, p);
    }
}
