package com.newuniverse.nusmp.unique;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.item.CuriosLink;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the server half of the Unique Guardian. Every 1-of-1 of the mod keeps its own rules; this class only watches that its one valid copy does not vanish for good, and when it does it puts the unique back into its
 * normal sources (the loot pool) through the kind's own door and logs it. Event driven and bounded: a holder's belongings are looked at once a second per online player (inventory, ender chest, Curios, the cursor, the
 * open menu, one level into shulker boxes and bundles); a dropped unique is made indestructible and lifted out of the void; a destroyed item entity starts a short confirm clock; a copy that is simply not accounted for
 * starts the full grace clock; a sighting anywhere cancels either. Theft (another player now holds it) is a sighting, not a loss. It never scans the world, never loads a chunk and never creates an item.
 * 4.103.2: with {@code floorOnly} (the default) only a dropped copy that is destroyed on the floor goes back; nothing else (a backpack, a storage mod, an absent holder, a removed container) counts as lost. Cost: it removes work.
 * Worst case: per player-second one pass over at most about 200 stacks (a few instanceof checks each) and over the tracked records (a few hundred at most); per tick at most 16 entity look-ups (64 every fourth tick), one
 * pass over the records a second, one verification pass a minute; nothing is sent over the network. Server thread only.
 */
public final class UniqueGuardian {
    private UniqueGuardian() {}

    private static final Logger LOG = LogUtils.getLogger();
    static final String STAMP = "ug_epoch";
    private static final int RESCUE_BUDGET = 64;

    private static volatile MinecraftServer server;
    private static volatile UniqueSaved saved;
    private static final Set<String> FOUND = new HashSet<>();
    private static int rescueCursor;

    // ---------------------------------------------------------------- binding
    static void bind(MinecraftServer s) {
        server = s;
        saved = UniqueSaved.get(s);
    }

    static void unbind() {
        server = null;
        saved = null;
        FOUND.clear();
        rescueCursor = 0;
    }

    static boolean bound() { return saved != null; }

    static boolean active() { return saved != null && UniqueConfig.enabled(); }

    public static UniqueSaved saved() { return saved; }

    private static long now() { return System.currentTimeMillis(); }

    private static String dim(net.minecraft.world.level.Level l) { return l.dimension().location().toString(); }

    private static ServerLevel level(MinecraftServer sv, String dim) {
        ResourceLocation rl = ResourceLocation.tryParse(dim);
        return rl == null ? null : sv.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
    }

    // ---------------------------------------------------------------- is this stack the valid copy
    enum Judge { VALID, IGNORE, DISSOLVE }

    static int stampOf(ItemStack s) {
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        return cd != null && cd.contains(STAMP) ? cd.copyTag().getInt(STAMP) : -1;
    }

    static void stamp(ItemStack s, int epoch) { CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putInt(STAMP, epoch)); }

    /** The owning system's own verdict first; then, for a kind without a token, the epoch stamp: a copy made before the last return to the pool dissolves, an unstamped copy is adopted. */
    static Judge judge(MinecraftServer sv, UniqueData d, UniqueKinds.Ident i, ItemStack s, boolean mutable) {
        if (!i.kind().valid(sv, i.id(), s)) return Judge.IGNORE;
        if (!i.kind().stamped()) return Judge.VALID;
        int st = stampOf(s), ep = d.epoch(i.id());
        if (UniqueRules.oldCopy(st, ep)) return mutable ? Judge.DISSOLVE : Judge.IGNORE;
        if (st < 0 && mutable) stamp(s, ep);
        return Judge.VALID;
    }

    // ---------------------------------------------------------------- the once-a-second look at a player
    private static final class Ctx {
        final ServerPlayer p;
        final MinecraftServer sv;
        final UniqueSaved saved;
        final long tick, ms;
        String dim;

        Ctx(ServerPlayer p, MinecraftServer sv, UniqueSaved saved) {
            this.p = p;
            this.sv = sv;
            this.saved = saved;
            this.tick = p.serverLevel().getGameTime();
            this.ms = now();
        }

        String dim() { return dim == null ? (dim = UniqueGuardian.dim(p.level())) : dim; }
    }

    /** What a player carries (inventory, ender chest, Curios, the cursor, the open menu, and {@code extra} when a container was just closed): the valid copies are seen; a copy their record says they hold but is gone starts the grace. */
    static void scan(ServerPlayer p, AbstractContainerMenu extra) {
        UniqueSaved sd = saved;
        MinecraftServer sv = p.getServer();
        if (sd == null || sv == null || !UniqueConfig.enabled()) return;
        Ctx c = new Ctx(p, sv, sd);
        FOUND.clear();
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) look(c, inv.getItem(i), true, false, UniqueRecord.NO_BLOCK);
        Container ender = p.getEnderChestInventory();
        for (int i = 0; i < ender.getContainerSize(); i++) look(c, ender.getItem(i), true, false, UniqueRecord.NO_BLOCK);
        AbstractContainerMenu menu = p.containerMenu;
        look(c, menu.getCarried(), true, false, UniqueRecord.NO_BLOCK);
        try {
            for (ItemStack s : CuriosLink.equippedCached(p, 20)) look(c, s, false, false, UniqueRecord.NO_BLOCK);
        } catch (RuntimeException noCurios) {
            // a player without a Curios inventory (a stand-in): nothing worn
        }
        menuSlots(c, menu, inv, ender, menu != p.inventoryMenu);
        if (extra != null && extra != menu) menuSlots(c, extra, inv, ender, true);
        if (!p.isAlive()) return;
        UUID me = p.getUUID();
        UniqueData d = sd.data();
        if (UniqueConfig.floorOnly()) return;                                                      // 4.103.2: not seeing a copy (a backpack, a storage mod) is never a loss
        for (UniqueRecord r : d.all()) {
            if (r.state == UniqueState.HELD && me.equals(r.holder) && !FOUND.contains(r.id) && d.markMissing(r.id, c.tick, false)) {
                sd.setDirty();
                LOG.info("[anime_multiverse] unique guardian: {} is no longer with {} (grace of {} s starts)", r.id, p.getGameProfile().getName(), UniqueConfig.graceSeconds());
            }
        }
    }

    private static void menuSlots(Ctx c, AbstractContainerMenu menu, Inventory inv, Container ender, boolean contained) {
        for (Slot sl : menu.slots) {
            Container ct = sl.container;
            if (ct == inv || ct == ender) continue;                                                    // already looked at as the player's own
            long block = contained && ct instanceof BlockEntity be ? be.getBlockPos().asLong() : UniqueRecord.NO_BLOCK;
            look(c, sl.getItem(), true, contained, block);
        }
    }

    private static void look(Ctx c, ItemStack s, boolean mutable, boolean contained, long block) {
        if (s.isEmpty()) return;
        UniqueKinds.Ident i = UniqueKinds.identify(s);
        if (i != null) found(c, i, s, mutable, contained, block);
        if (s.has(DataComponents.CONTAINER)) {
            ItemContainerContents cc = s.get(DataComponents.CONTAINER);
            if (cc != null) for (int n = 0; n < cc.getSlots(); n++) inner(c, cc.getStackInSlot(n), contained, block);
        }
        if (s.has(DataComponents.BUNDLE_CONTENTS)) {
            BundleContents b = s.get(DataComponents.BUNDLE_CONTENTS);
            if (b != null) for (ItemStack in : b.items()) inner(c, in, contained, block);
        }
    }

    /** One level into a shulker box or a bundle: the copy is seen (it is with the carrier), but never changed. */
    private static void inner(Ctx c, ItemStack s, boolean contained, long block) {
        if (s.isEmpty()) return;
        UniqueKinds.Ident i = UniqueKinds.identify(s);
        if (i != null) found(c, i, s, false, contained, block);
    }

    private static void found(Ctx c, UniqueKinds.Ident i, ItemStack s, boolean mutable, boolean contained, long block) {
        UniqueData d = c.saved.data();
        Judge j = judge(c.sv, d, i, s, mutable);
        if (j == Judge.DISSOLVE) {
            s.setCount(0);
            c.p.displayClientMessage(Component.translatableWithFallback("unique.msg.dissolved", "An old copy of an item that went back to the world crumbles to dust."), true);
            return;
        }
        if (j == Judge.IGNORE) return;
        FOUND.add(i.id());
        UniqueSpot spot = contained ? UniqueSpot.contained(c.p.getUUID(), c.dim(), c.p.getX(), c.p.getY(), c.p.getZ(), block) : UniqueSpot.held(c.p.getUUID(), c.dim(), c.p.getX(), c.p.getY(), c.p.getZ());
        if (d.see(i.id(), spot, c.tick, c.ms)) c.saved.setDirty();
    }

    // ---------------------------------------------------------------- item entities
    /** A dropped item entity joins a level. True when it must not join (an old copy). A valid unique is made indestructible and its place is recorded. */
    static boolean joined(ItemEntity ie, ServerLevel sl) {
        UniqueSaved sd = saved;
        if (sd == null || !UniqueConfig.enabled()) return false;
        ItemStack s = ie.getItem();
        UniqueKinds.Ident i = UniqueKinds.identify(s);
        if (i == null) return false;
        MinecraftServer sv = sl.getServer();
        UniqueData d = sd.data();
        Judge j = judge(sv, d, i, s, true);
        if (j == Judge.DISSOLVE) return true;
        if (j == Judge.IGNORE) return false;
        ItemGuard.protect(ie);
        Entity owner = ie.getOwner();
        if (d.see(i.id(), UniqueSpot.dropped(owner == null ? null : owner.getUUID(), dim(sl), ie.getX(), ie.getY(), ie.getZ(), ie.getUUID()), sl.getGameTime(), now())) sd.setDirty();
        return false;
    }

    /** An item entity leaves a level. Only the tracked entity of a tracked copy counts, and only when it was destroyed (not picked up, unloaded or sent to another dimension). */
    static void left(ItemEntity ie, ServerLevel sl) {
        UniqueSaved sd = saved;
        if (sd == null || !UniqueConfig.enabled()) return;
        Entity.RemovalReason why = ie.getRemovalReason();
        if (why == null || !why.shouldDestroy()) return;
        ItemStack s = ie.getItem();
        if (s.isEmpty()) return;                                                                   // picked up: the stack was emptied
        UniqueKinds.Ident i = UniqueKinds.identify(s);
        if (i == null) return;
        UniqueData d = sd.data();
        UniqueRecord r = d.get(i.id());
        if (r == null || r.state != UniqueState.DROPPED || !ie.getUUID().equals(r.entity)) return;
        long tick = sl.getGameTime();
        BlockPos box = containerAt(sl, ie);
        if (box != null) {                                                                         // a hopper took it: it is in that container now
            d.see(i.id(), UniqueSpot.contained(null, dim(sl), box.getX(), box.getY(), box.getZ(), box.asLong()), tick, now());
            sd.setDirty();
            return;
        }
        boolean confirmed = why == Entity.RemovalReason.KILLED || ie.getY() < sl.getMinBuildHeight();      // a plain discard may be a mob or a pick-up mod taking it: only the full grace decides
        if (d.markMissing(i.id(), tick, confirmed)) {
            sd.setDirty();
            LOG.info("[anime_multiverse] unique guardian: the item entity of {} was removed ({}) at {} {}: {}", i.id(), why, dim(sl), ie.blockPosition().toShortString(), confirmed ? "confirmed after five seconds" : "grace starts");
        }
    }

    private static BlockPos containerAt(ServerLevel level, ItemEntity ie) {
        BlockPos at = ie.blockPosition();
        for (BlockPos p : new BlockPos[]{at, at.below()}) {
            if (!level.isLoaded(p)) continue;
            if (level.getBlockEntity(p) instanceof Container) return p;
        }
        return null;
    }

    // ---------------------------------------------------------------- the server tick
    static void onServerTick(MinecraftServer sv) {
        if (!active()) return;
        int t = sv.getTickCount();
        if (t % 4 == 0) rescuePass(sv);
        if (t % 20 == 7) confirmPass(sv, sv.overworld().getGameTime());
        if (t % 1200 == 601) minutePass(sv);
    }

    /** Every fourth tick: at most {@value #RESCUE_BUDGET} dropped uniques, looked up by uuid in their own level (never a world scan), lifted out of the void. */
    static void rescuePass(MinecraftServer sv) {
        UniqueSaved sd = saved;
        if (sd == null || sd.data().size() == 0) return;
        int start = rescueCursor, n = 0, budget = RESCUE_BUDGET;
        for (UniqueRecord r : sd.data().all()) {
            if (n++ < start) continue;
            if (r.state != UniqueState.DROPPED || r.entity == null) continue;
            if (budget-- <= 0) {
                rescueCursor = n - 1;
                return;
            }
            ServerLevel l = level(sv, r.dim);
            if (l != null && l.getEntity(r.entity) instanceof ItemEntity ie && !ie.isRemoved()) {
                r.x = ie.getX();
                r.y = ie.getY();
                r.z = ie.getZ();
                ItemGuard.rescue(ie, l);
            }
        }
        rescueCursor = 0;
    }

    /** Once a second: every copy whose clock has run out goes back into the pool. */
    static void confirmPass(MinecraftServer sv, long nowTick) { confirmPass(sv, nowTick, null); }

    /** {@code only}: a self-test looks at its own stand-in unique and leaves every real record alone. */
    static void confirmPass(MinecraftServer sv, long nowTick, String only) {
        UniqueSaved sd = saved;
        if (sd == null || sd.data().size() == 0) return;
        long grace = UniqueRules.graceTicks(UniqueConfig.graceSeconds());
        List<UniqueRecord> due = null;
        for (UniqueRecord r : sd.data().all()) {
            if (only != null && !only.equals(r.id)) continue;
            if (UniqueRules.verdict(r, nowTick, grace, UniqueConfig.floorOnly()) == UniqueRules.Verdict.RELEASE) {
                if (due == null) due = new ArrayList<>(2);
                due.add(r);
            }
        }
        if (due != null) for (UniqueRecord r : due) giveBack(sv, r, r.confirmed ? "destroyed" : "not accounted for anywhere", false);
    }

    /** Once a minute: a record whose own system already returned it is retired, a holder gone for good frees their unique, a copy in a container whose block is gone starts the clock, the Holy Weapons rule. */
    static void minutePass(MinecraftServer sv) {
        UniqueSaved sd = saved;
        if (sd == null) return;
        long ms = now();
        long absence = UniqueRules.absenceMs(UniqueConfig.absenceDays());
        long tick = sv.overworld().getGameTime();
        List<UniqueRecord> snapshot = new ArrayList<>(sd.data().all());
        for (UniqueRecord r : snapshot) {
            if (r.state == UniqueState.POOL) continue;
            UniqueKind k = UniqueKinds.kindOfId(r.id);
            if (k == null || !k.exists(sv, r.id)) {
                giveBack(sv, r, "", true);
            } else if (UniqueConfig.floorOnly()) {
                continue;                                                                          // 4.103.2: only a destroyed floor drop returns a unique
            } else if (UniqueRules.absent(r, r.holder != null && sv.getPlayerList().getPlayer(r.holder) != null, ms, absence)) {
                giveBack(sv, r, "its holder has been away for " + UniqueConfig.absenceDays() + " days", false);
            } else if (r.state == UniqueState.CONTAINED && r.block != UniqueRecord.NO_BLOCK) {
                verifyContainer(sv, sd.data(), r, tick);
            }
        }
        HeroUnique.minute(sv, sd, ms);
    }

    /** The block entity that held the copy is gone (a command removed the chest, no items dropped): only a loaded chunk is looked at, never loaded. */
    private static void verifyContainer(MinecraftServer sv, UniqueData d, UniqueRecord r, long tick) {
        ServerLevel l = level(sv, r.dim);
        if (l == null) return;
        BlockPos bp = BlockPos.of(r.block);
        if (l.getChunkSource().getChunkNow(bp.getX() >> 4, bp.getZ() >> 4) == null) return;
        if (!(l.getBlockEntity(bp) instanceof Container) && d.markMissing(r.id, tick, true)) {
            saved.setDirty();
            LOG.info("[anime_multiverse] unique guardian: the container of {} at {} is gone", r.id, bp.toShortString());
        }
    }

    // ---------------------------------------------------------------- back in the pool
    /**
     * The unique returns to its normal sources through its kind's own door: the owning system's flag, ledger entry or claim is reset and the old copy is invalid. If the owning system already put it back (or no longer
     * believes a copy exists) the record is only retired. A flapping unique (a few returns in an hour) waits. Nothing is ever put into an inventory.
     */
    static void giveBack(MinecraftServer sv, UniqueRecord r, String reason, boolean quietRetire) {
        UniqueSaved sd = saved;
        if (sd == null) return;
        UniqueData d = sd.data();
        UniqueKind k = UniqueKinds.kindOfId(r.id);
        long ms = now();
        if (k == null || !k.exists(sv, r.id)) {
            boolean wasMissing = r.missing();
            String label = k == null ? r.id : k.label(r.id);
            d.retire(r.id, k == null ? "no longer in the mod" : "its own system returned it");
            sd.setDirty();
            if (wasMissing && k != null && !quietRetire) announce(sv, "unique.msg.ops.own", "[Unique Guardian] %s was lost, and its own rules had already returned it to the loot pool.", label);
            return;
        }
        if (!UniqueRules.mayRelease(r, ms)) return;                                                // flapping: it stays unaccounted for and is asked again
        String label = k.label(r.id), where = r.dim + " " + (int) r.x + " " + (int) r.y + " " + (int) r.z, last = holderName(sv, r.holder);
        boolean did;
        try {
            did = k.release(sv, r.id);
        } catch (RuntimeException broken) {
            LOG.error("[anime_multiverse] unique guardian: putting {} back into the pool failed", r.id, broken);
            return;
        }
        d.release(r.id, ms, reason.isEmpty() ? "gone" : reason);
        sd.setDirty();
        if (did) announce(sv, "unique.msg.ops.lost", "[Unique Guardian] %s was lost (%s; last held by %s, last seen at %s). It is back in the loot pool. %s", label, reason, last, where, k.pool(r.id));
    }

    /** An operator's {@code release}: the same door, with no clocks and no flap guard. Returns null on success or the reason it was refused. */
    static String force(MinecraftServer sv, String id, String by) {
        UniqueSaved sd = saved;
        if (sd == null) return "The Unique Guardian is not running (the server has not started).";
        if (!UniqueRules.validId(id)) return "'" + id + "' is not a unique id.";
        if (HeroUnique.owns(id)) {
            int n = HeroUnique.force(sv, sd, HeroUnique.kind(id));
            if (n == 0) return "Nobody holds " + id + ": it is already back with its normal source.";
            announce(sv, "unique.msg.ops.forced", "[Unique Guardian] %s was returned to the loot pool by %s.", id, by);
            return null;
        }
        UniqueKind k = UniqueKinds.kindOfId(id);
        if (k == null) return "Unknown unique '" + id + "'. /multiverse unique list shows what is tracked.";
        if (!k.exists(sv, id)) return k.label(id) + " has no valid copy in the world: it is already in the loot pool.";
        boolean did;
        try {
            did = k.release(sv, id);
        } catch (RuntimeException broken) {
            LOG.error("[anime_multiverse] unique guardian: forced release of {} failed", id, broken);
            return "The release failed (see the server log).";
        }
        if (sd.data().retire(id, "forced by " + by)) sd.setDirty();
        if (!did) return k.label(id) + " had nothing to put back.";
        announce(sv, "unique.msg.ops.forced", "[Unique Guardian] %s was returned to the loot pool by %s.", k.label(id), by);
        return null;
    }

    // ---------------------------------------------------------------- the log and the operators
    static void announce(MinecraftServer sv, String key, String fallback, Object... args) {
        Component msg = Component.translatableWithFallback(key, fallback, args);
        LOG.warn("[anime_multiverse] {}", msg.getString());
        for (ServerPlayer p : sv.getPlayerList().getPlayers()) {
            if (sv.getPlayerList().isOp(p.getGameProfile())) p.sendSystemMessage(msg.copy().withStyle(ChatFormatting.GOLD));
        }
    }

    static String holderName(MinecraftServer sv, UUID id) {
        if (id == null) return "nobody";
        ServerPlayer on = sv.getPlayerList().getPlayer(id);
        if (on != null) return on.getGameProfile().getName();
        try {
            if (sv.getProfileCache() != null) return sv.getProfileCache().get(id).map(g -> g.getName()).orElse(id.toString());
        } catch (RuntimeException unknown) {
            // a profile cache that cannot answer: the uuid will do
        }
        return id.toString();
    }
}
