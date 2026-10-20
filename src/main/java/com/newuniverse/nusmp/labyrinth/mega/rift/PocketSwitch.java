package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.ExclusivePockets;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

/**
 * Why: the switch of the hidden realms and the tombs (1.101.0, docs/labyrinth/pocket_fix.md). A barred vault or a sealed treasury had no way open that a delver could see: the owner's report was "there is
 * no switch". Now each has a RELEASE LEVER in a chamber that is not behind the door it opens (the realm's lever chamber north of the Hall; the tomb's key room), flanked by lit lanterns, under a sign, and this class
 * is its server-side handler: a right click on the lever (any player, any party member, multiplayer safe: the work is done on the server and the vanilla toggle is cancelled) asks
 * {@link PocketSwitchRules}: when the door's own challenge is done, or the delay has run out, the door opens (the bars lift / the seal falls) with a click, a sound and a line to everybody inside ("A lever clicks
 * somewhere in the realm..."); before that the lever is stiff and says for how long. The realm's door also opens by itself after {@code switchAutoSeconds} so nobody is ever stuck, and the cue of the lever
 * (a wisp of light when it works, smoke while it is stiff) is sent every second to delvers near it. The blocks are held by the Dungeon Guard like all the dungeon, so the lever cannot be broken. The admin's
 * {@code rift switch} commands drive the same calls ({@link #pull}, {@link #leverAt}).
 */
public final class PocketSwitch {
    private PocketSwitch() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(EventPriority.HIGH, PocketSwitch::onUse);
    }

    private static long unlockTicks() { return 20L * RiftConfig.get(RiftConfig.SWITCH_UNLOCK_SECONDS); }

    private static long autoTicks(PocketRealm.Pocket p) { return PocketSwitchRules.clampToLife(20L * RiftConfig.get(RiftConfig.SWITCH_AUTO_SECONDS), p.expiresAt - p.createdAt); }

    /** The position of the release lever of a pocket of either kind, or null when its plan has none (an old realm built before 1.101.0 has no lever block: the world is asked). */
    public static BlockPos leverAt(PocketRealm.Pocket p) {
        if (p == null) return null;
        if (p.exclusive()) {
            PocketRuntime rt = PocketRuntime.of(p);
            return rt == null ? null : rt.switchPos;
        }
        return PocketBuilder.leverPos(p.slot);
    }

    private static void onUse(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ServerPlayer p) || !(e.getLevel() instanceof ServerLevel level) || !Labyrinth.inMega(level)) return;
        int slot = RiftRules.slotAt(e.getPos().getX(), e.getPos().getZ());
        if (slot < 0) return;
        PocketRealm realm = PocketRealm.get(level.getServer());
        PocketRealm.Pocket pk = realm.at(slot);
        if (pk == null || pk.exclusive() && pk.phase >= 2) return;
        BlockPos lever = leverAt(pk);
        if (lever == null || !lever.equals(e.getPos()) || !level.getBlockState(lever).is(Blocks.LEVER)) return;
        e.setCanceled(true);                                                         // the lever is worked here, on the server; vanilla's own toggle would only flip the picture
        e.setCancellationResult(InteractionResult.SUCCESS);
        if (e.getHand() != InteractionHand.MAIN_HAND) return;
        pull(level, realm, pk, p);
    }

    /** One pull of a pocket's lever by {@code by} (may be null for the admin): "opened", "locked" or "spent". */
    public static String pull(ServerLevel level, PocketRealm realm, PocketRealm.Pocket pk, ServerPlayer by) {
        if (pk.exclusive()) {
            PocketRuntime rt = PocketRuntime.of(pk);
            return rt == null ? "locked" : ExclusivePockets.pullLever(level, realm, pk, rt, by);
        }
        boolean opened = pk.bossDead || pk.solved;
        long age = level.getGameTime() - pk.createdAt;
        PocketSwitchRules.State st = PocketSwitchRules.state(opened, opened, age, unlockTicks());
        BlockPos at = leverAt(pk);
        switch (st) {
            case SPENT -> {
                level.playSound(null, at, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6f, 0.6f);
                if (by != null) by.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.rift.lever.spent", "The lever is down. The vault is open already.").withStyle(ChatFormatting.GRAY), true);
                return "spent";
            }
            case LOCKED -> {
                level.playSound(null, at, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.7f, 0.4f);
                level.playSound(null, at, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.7f, 0.5f);
                if (by != null) by.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.rift.lever.locked", "The lever is stiff: the vault answers to the guardian's fall or the plates. It gives in %s s.",
                        PocketSwitchRules.secondsToUnlock(false, age, unlockTicks())).withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return "locked";
            }
            default -> {
                level.playSound(null, at, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0f, 0.8f);
                if (level.getBlockState(at).is(Blocks.LEVER)) level.setBlock(at, level.getBlockState(at).setValue(LeverBlock.POWERED, true), 3);
                level.sendParticles(ParticleTypes.END_ROD, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);
                if (realm.clear(level, pk, "lever")) {
                    for (ServerPlayer m : PocketRealm.playersIn(level, pk.slot))
                        m.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.rift.lever.pulled", "A lever clicks somewhere in the realm... iron grinds: the bars of the vault lift.").withStyle(ChatFormatting.GOLD), true);
                }
                return "opened";
            }
        }
    }

    /** The first line a delver reads on arriving in a realm: what the vault is, where the lever is, and when it works. */
    public static void welcome(ServerLevel level, ServerPlayer m, PocketRealm.Pocket pk) {
        long age = level.getGameTime() - pk.createdAt;
        boolean boss = pk.variantKind() == RiftRules.Variant.BOSS;
        m.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.rift.welcome", "[Realm] The vault behind the Hall is barred. %s Or pull the RELEASE LEVER in the small chamber north of the Hall: it gives in %s s, and the bars lift by themselves in %s s. The exit rift is behind you.",
                Component.translatableWithFallback(boss ? "message.nusmp.mega.rift.welcome.boss" : "message.nusmp.mega.rift.welcome.puzzle", boss ? "The guardian in the Hall holds it: beat it." : "Press the four plates in the order the east wall shows."),
                Math.max(0, PocketSwitchRules.secondsToUnlock(false, age, unlockTicks())), Math.max(0, PocketSwitchRules.secondsToAuto(age, unlockTicks(), autoTicks(pk)))).withStyle(ChatFormatting.AQUA), false);
    }

    /**
     * One second of a realm's lever with people inside (the realm's tick calls it): the cue over the lever, the word when it first gives, and the door that opens by itself. Returns true when it opened the
     * vault this call.
     */
    public static boolean tick(ServerLevel level, PocketRealm realm, PocketRealm.Pocket pk, List<ServerPlayer> in, long now) {
        if (pk.exclusive() || pk.bossDead || pk.solved || in.isEmpty()) return false;
        long age = now - pk.createdAt;
        BlockPos at = leverAt(pk);
        PocketSwitchRules.State st = PocketSwitchRules.state(false, false, age, unlockTicks());
        if (level.getBlockState(at).is(Blocks.LEVER)) {
            level.sendParticles(st == PocketSwitchRules.State.READY ? ParticleTypes.END_ROD : ParticleTypes.SMOKE, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 2, 0.15, 0.3, 0.15, 0.01);
            if (st == PocketSwitchRules.State.READY && !TOLD.contains(pk.slot * 31L + pk.seed)) {
                TOLD.add(pk.slot * 31L + pk.seed);
                for (ServerPlayer m : in) {
                    m.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.rift.lever.ready", "A lever clicks somewhere in the realm: the release lever north of the Hall gives way now.").withStyle(ChatFormatting.AQUA), true);
                    level.playSound(null, m.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5f, 1.2f);
                }
            }
        }
        if (PocketSwitchRules.autoOpens(false, age, unlockTicks(), autoTicks(pk), true)) {
            boolean did = realm.clear(level, pk, "auto");
            if (did) for (ServerPlayer m : in)
                m.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.rift.lever.auto", "The old mechanism gives way by itself: the bars of the vault lift.").withStyle(ChatFormatting.GOLD), true);
            return did;
        }
        return false;
    }

    /** Realms whose lever has been announced (runtime only: keyed by slot and seed). */
    private static final java.util.Set<Long> TOLD = new java.util.HashSet<>();

    /** Forgets the announcements (server stop). */
    public static void stopped() { TOLD.clear(); }

    /** The pockets of a server that have a lever, for the admin lists: slot, kind, position, state. */
    public static String describe(MinecraftServer server, PocketRealm.Pocket pk) {
        ServerLevel mega = com.newuniverse.nusmp.labyrinth.mega.MegaDungeon.level(server);
        BlockPos at = leverAt(pk);
        if (at == null) return "slot " + pk.slot + ": no lever";
        long age = mega == null ? 0 : mega.getGameTime() - pk.createdAt;
        boolean opened = pk.exclusive() ? pk.sealOpen : pk.bossDead || pk.solved;
        PocketSwitchRules.State st = PocketSwitchRules.state(opened, pk.exclusive() ? pk.bossDead : opened, age, unlockTicks());
        boolean built = mega != null && mega.isLoaded(at) && mega.getBlockState(at).is(Blocks.LEVER);
        return "slot " + pk.slot + " (" + (pk.exclusive() ? "tomb " + pk.pocketId : pk.variant) + "): lever at " + at.toShortString() + (built ? "" : " (NOT BUILT)") + ", " + st.name().toLowerCase(java.util.Locale.ROOT)
                + ", unlocks in " + PocketSwitchRules.secondsToUnlock(pk.exclusive() ? pk.bossDead : opened, age, unlockTicks()) + " s, opens by itself in " + PocketSwitchRules.secondsToAuto(age, unlockTicks(), autoTicks(pk)) + " s";
    }
}
