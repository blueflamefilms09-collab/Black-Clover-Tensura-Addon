package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.labyrinth.mega.rift.PocketRealm;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftConfig;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Why: the game-bus half of the exclusive tombs: the every-tick work ({@link ExclusivePockets#fast}), the keys of the treasury's seal, the protection of the tomb's blocks, and the three deaths that
 * matter (the master's, the key keeper's, a delver's). THE SEAL has three answers: a right click with the tomb's Sigil in the hand; a right click by a mage whose grimoire magic is on the seal's list
 * (it unravels); and FORCE: each fully charged swing at the seal adds its attack damage (at most a quarter of the seal's strength per blow, {@link PocketRules#sealForce}) and holding the attack button
 * adds a trickle ({@link PocketRules#sealHold}). THE BLOCKS of a tomb cannot be broken, placed or exploded by survival players (the walls are the dungeon; the seal is the only door that can be opened),
 * and buckets, fire and other block-placing items are refused on its blocks. THE DEATHS: the master's opens the treasury's loot stage and rolls its drops; the key keeper drops the Sigil; a delver
 * who dies inside keeps their items (config {@code keepItemsOnDeath}) and is sent home on respawn. Every handler first asks whether the position is inside a live exclusive tomb, which is a map lookup,
 * so the cost outside the pocket field is a handful of comparisons.
 */
public final class ExclusiveEvents {
    private ExclusiveEvents() {}

    private static final Logger LOG = LogUtils.getLogger();

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ExclusiveEvents::onTick);
        gameBus.addListener(ExclusiveEvents::onRightClick);
        gameBus.addListener(ExclusiveEvents::onLeftClick);
        gameBus.addListener(ExclusiveEvents::onBreakSpeed);
        gameBus.addListener(ExclusiveEvents::onBreak);
        gameBus.addListener(ExclusiveEvents::onPlace);
        gameBus.addListener(ExclusiveEvents::onExplosion);
        gameBus.addListener(EventPriority.LOW, ExclusiveEvents::onDeath);
        gameBus.addListener(ExclusiveEvents::onDrops);
        gameBus.addListener(ExclusiveEvents::onClone);
        gameBus.addListener(ExclusiveEvents::onRespawn);
        gameBus.addListener(ExclusiveEvents::onLogin);
        gameBus.addListener(ExclusiveEvents::onLogout);
        gameBus.addListener(ExclusiveEvents::onDimension);
        gameBus.addListener(ExclusiveEvents::onStopped);
        com.newuniverse.nusmp.dungeon.DungeonGuard.addBreakRule(ExclusiveEvents::sealBreakable);        // 1.101.0: the seal's blows reach the tomb's handlers (see sealBreakable)
    }

    private static void onTick(ServerTickEvent.Post e) {
        try {
            ExclusivePockets.fast(e.getServer());
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] The tomb tick failed (further detail at debug): {}", ex.toString());
            LOG.debug("tomb tick", ex);
        }
    }

    private static void onStopped(ServerStoppedEvent e) { ExclusivePockets.stopped(); }

    // ---------------------------------------------------------------- which tomb
    /** The live exclusive tomb whose cell holds a block of this level, or null. */
    private static PocketRealm.Pocket tombAt(ServerLevel level, BlockPos pos) {
        if (!Labyrinth.inMega(level)) return null;
        int slot = RiftRules.slotAt(pos.getX(), pos.getZ());
        if (slot < 0) return null;
        PocketRealm.Pocket p = PocketRealm.get(level.getServer()).at(slot);
        return p != null && p.exclusive() ? p : null;
    }

    private static boolean survival(net.minecraft.world.entity.player.Player p) { return !p.isCreative() && !p.isSpectator(); }

    // ---------------------------------------------------------------- the seal and the blocks
    private static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ServerPlayer p) || !(e.getLevel() instanceof ServerLevel level)) return;
        PocketRealm.Pocket pk = tombAt(level, e.getPos());
        if (pk == null) return;
        PocketRuntime rt = PocketRuntime.of(pk);
        if (rt == null) return;
        if (rt.isSeal(e.getPos())) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
            if (e.getHand() != InteractionHand.MAIN_HAND || pk.sealOpen) return;
            String how = ExclusivePockets.answer(p, pk, rt.def);
            PocketRealm realm = PocketRealm.get(level.getServer());
            if (how.equals("sigil")) {
                p.getMainHandItem().shrink(1);
                ExclusivePockets.openSeal(level, realm, pk, rt, p, "sigil");
            } else if (how.equals("magic")) {
                ExclusivePockets.openSeal(level, realm, pk, rt, p, "magic");
            } else {
                p.displayClientMessage(net.minecraft.network.chat.Component.translatableWithFallback("message.nusmp.mega.pocket.seal.deny", "The seal does not answer you. It wants %s magic, the %s, or force.",
                        net.minecraft.network.chat.Component.translatableWithFallback("pocket.nusmp." + rt.def.id() + ".seal", rt.def.seal().label()),
                        net.minecraft.network.chat.Component.translatableWithFallback("pocket.nusmp." + rt.def.id() + ".sigil", rt.def.seal().sigil())).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
            }
            return;
        }
        if (survival(p)) {                                                              // nothing is placed, poured or lit on the tomb's blocks; chests and pedestals still open
            ItemStack held = e.getItemStack();
            if (!held.isEmpty() && (held.getItem() instanceof BlockItem || held.getItem() instanceof BucketItem || held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE) || held.is(Items.BONE_MEAL)))
                e.setUseItem(TriState.FALSE);
        }
    }

    private static void onLeftClick(PlayerInteractEvent.LeftClickBlock e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ServerPlayer p) || !(e.getLevel() instanceof ServerLevel level)) return;
        PocketRealm.Pocket pk = tombAt(level, e.getPos());
        if (pk == null) return;
        PocketRuntime rt = PocketRuntime.of(pk);
        if (rt == null) return;
        boolean seal = rt.isSeal(e.getPos());
        if (!seal && !survival(p)) return;
        e.setCanceled(true);                                                            // a tomb's blocks are not mined; the seal is struck instead
        if (!seal || e.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START || pk.sealOpen) return;
        double charge = p.getAttackStrengthScale(0.5f);
        double force = PocketRules.sealForce(ExclusivePockets.attackOf(p), charge, rt.def.seal().forceHp());
        if (force <= 0.0) return;
        p.resetAttackStrengthTicker();
        ExclusivePockets.addForce(level, PocketRealm.get(level.getServer()), pk, rt, p, force);
    }

    private static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level) || e.getPosition().isEmpty()) return;
        BlockPos pos = e.getPosition().get();
        PocketRealm.Pocket pk = tombAt(level, pos);
        if (pk == null) return;
        PocketRuntime rt = PocketRuntime.of(pk);
        if (rt != null && rt.isSeal(pos) && !pk.sealOpen) {
            ExclusivePockets.addForce(level, PocketRealm.get(level.getServer()), pk, rt, null, PocketRules.sealHold(ExclusivePockets.attackOf(p)));
            e.setNewSpeed(0f);
        } else if (survival(p)) {
            e.setNewSpeed(0f);
        }
    }

    private static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        PocketRealm.Pocket pk = tombAt(level, e.getPos());
        if (pk == null) return;
        PocketRuntime rt = PocketRuntime.of(pk);
        if (survival(e.getPlayer()) || (rt != null && rt.isSeal(e.getPos()))) e.setCanceled(true);                // the seal is struck, never broken, not even in creative
    }

    /**
     * 1.101.0: the Dungeon Guard's allow-list rule for the blocks of a sealed treasury's door. The guard's own handlers (highest priority) cancel every left click and every blow at a held block BEFORE the
     * handlers above run, so the seal's "force" answer (a strike, a held blow) never reached them; this rule lets the event through for the seal's blocks only, and the handlers above then cancel the break
     * themselves (the seal is hit, never broken) and add the force. On the client it only has to let the crack start, so any crying obsidian in the pocket field passes.
     */
    static boolean sealBreakable(net.minecraft.world.level.Level level, BlockPos pos) {
        if (level == null || pos == null || RiftRules.slotAt(pos.getX(), pos.getZ()) < 0) return false;
        if (level instanceof ServerLevel sl) {
            PocketRealm.Pocket pk = tombAt(sl, pos);
            PocketRuntime rt = pk == null ? null : PocketRuntime.of(pk);
            return rt != null && !pk.sealOpen && rt.isSeal(pos);
        }
        return level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN);
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level) || tombAt(level, e.getPos()) == null) return;
        if (e.getEntity() instanceof net.minecraft.world.entity.player.Player p && !survival(p)) return;
        e.setCanceled(true);
    }

    private static void onExplosion(ExplosionEvent.Detonate e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !Labyrinth.inMega(level)) return;
        e.getAffectedBlocks().removeIf(pos -> tombAt(level, pos) != null);
    }

    // ---------------------------------------------------------------- the deaths
    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity().level() instanceof ServerLevel level) || !Labyrinth.inMega(level)) return;
        if (e.getEntity() instanceof LabyrinthGuardianEntity g && g.getTags().contains(PocketBoss.TAG)) {
            PocketRealm.Pocket pk = tombAt(level, g.blockPosition());
            PocketRuntime rt = pk == null ? null : PocketRuntime.of(pk);
            if (pk != null && rt != null) ExclusivePockets.bossDied(level, PocketRealm.get(level.getServer()), pk, rt, g);
        } else if (e.getEntity() instanceof ServerPlayer p) {
            PocketRealm.Pocket pk = tombAt(level, p.blockPosition());
            if (pk != null && pk.phase < 3) {
                p.getPersistentData().putBoolean(ExclusivePockets.DIED, true);
                PocketGravity.clear(p);
            }
        }
    }

    private static void onDrops(LivingDropsEvent e) {
        if (!(e.getEntity().level() instanceof ServerLevel level) || !Labyrinth.inMega(level)) return;
        if (e.getEntity() instanceof Mob mob && mob.getTags().contains(PocketMobs.TAG_KEEPER)) {
            PocketRealm.Pocket pk = tombAt(level, mob.blockPosition());
            PocketRuntime rt = pk == null ? null : PocketRuntime.of(pk);
            if (rt != null && !pk.sealOpen) {
                ItemEntity ie = new ItemEntity(level, mob.getX(), mob.getY() + 0.5, mob.getZ(), PocketSigil.make(rt.def, pk.seed));
                ie.setUnlimitedLifetime();
                e.getDrops().add(ie);
                for (ServerPlayer m : PocketRealm.playersIn(level, pk.slot))
                    m.displayClientMessage(net.minecraft.network.chat.Component.translatableWithFallback("message.nusmp.mega.pocket.keeper.dead", "The keeper falls and drops the Sigil of the seal.").withStyle(net.minecraft.ChatFormatting.GOLD), true);
            }
        } else if (e.getEntity() instanceof ServerPlayer p && RiftConfig.get(RiftConfig.KEEP_ITEMS)) {
            PocketRealm.Pocket pk = tombAt(level, p.blockPosition());
            if (pk == null || pk.phase >= 3) return;
            ListTag kept = new ListTag();
            for (ItemEntity ie : e.getDrops()) if (!ie.getItem().isEmpty()) kept.add(ie.getItem().save(p.registryAccess()));
            e.getDrops().clear();
            e.setCanceled(true);
            p.getPersistentData().put(ExclusivePockets.KEPT, kept);
        }
    }

    private static void onClone(PlayerEvent.Clone e) {
        CompoundTag from = e.getOriginal().getPersistentData(), to = e.getEntity().getPersistentData();
        if (from.contains(MegaRifts.PITY)) to.putInt(MegaRifts.PITY, from.getInt(MegaRifts.PITY));          // the pity of the rift table outlives a death
        if (!e.isWasDeath() || !from.getBoolean(ExclusivePockets.DIED)) return;
        to.putBoolean(ExclusivePockets.DIED, true);
        if (from.contains(ExclusivePockets.KEPT)) to.put(ExclusivePockets.KEPT, from.get(ExclusivePockets.KEPT).copy());
        if (from.contains(PocketRealm.RETURN)) to.put(PocketRealm.RETURN, from.getCompound(PocketRealm.RETURN).copy());
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        CompoundTag data = p.getPersistentData();
        if (!data.getBoolean(ExclusivePockets.DIED)) return;
        data.remove(ExclusivePockets.DIED);
        if (data.contains(ExclusivePockets.KEPT)) {
            ListTag kept = data.getList(ExclusivePockets.KEPT, Tag.TAG_COMPOUND);
            data.remove(ExclusivePockets.KEPT);
            for (int i = 0; i < kept.size(); i++) {
                ItemStack s = ItemStack.parseOptional(p.registryAccess(), kept.getCompound(i));
                if (s.isEmpty()) continue;
                if (!p.getInventory().add(s) && !s.isEmpty()) p.drop(s, false);
            }
        }
        PocketGravity.clear(p);
        if (data.contains(PocketRealm.RETURN) && p.level() instanceof ServerLevel level) PocketRealm.leave(level, p);           // home to the place they went in from
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.getServer() == null || !(p.level() instanceof ServerLevel level) || !Labyrinth.inMega(level)) return;
        PocketGravity.clear(p);
        int slot = RiftRules.slotAt(p.getX(), p.getZ());
        if (slot < 0) return;
        PocketRealm.Pocket pk = PocketRealm.get(p.getServer()).at(slot);
        if (pk != null && pk.exclusive() && pk.phase >= 3) PocketRealm.leave(level, p);                    // the tomb fell while they were away
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) PocketGravity.clear(p);
    }

    private static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) PocketGravity.clear(p);
    }

    /** Is the Mega-Dungeon's dimension loaded for this server (the self-test and the commands ask)? */
    public static boolean available(net.minecraft.server.MinecraftServer server) { return MegaDungeon.level(server) != null; }
}
