package com.newuniverse.nusmp.relics.order;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.order.LedgerRules.Action;
import com.newuniverse.nusmp.relics.order.LedgerRules.Known;
import com.newuniverse.nusmp.relics.order.LedgerRules.Mode;
import com.newuniverse.nusmp.relics.order.LedgerRules.Verdict;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Looks at stacks and judges them. A {@link Job} is the stacks of one place (a player's belongings, an opened container, an item entity, a mob's
 * equipment); jobs are walked a few stacks per tick ({@link ScanCursor}: at most sixteen per player per tick, a shulker box or a bundle counting for
 * what is inside it). Each unique relic found goes through {@link LedgerRules}: the valid copy is noted, a first copy is adopted (and stamped with a
 * serial), a duplicate is reported to the Order (or, during the census, only listed). Three kinds of run: SCAN (the normal watch), COUNT (the
 * Order checking that a duplicate is still there; changes nothing) and STRIP (the Order taking the duplicates away).
 */
final class RelicScanner {
    private RelicScanner() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    enum Run { SCAN, COUNT, STRIP }

    // ================================================================ a job
    static final class Job {
        final Run run;
        final ServerPlayer actor;
        final List<StackRef> refs = new ArrayList<>();
        final List<Spot> spots = new ArrayList<>();
        final List<Integer> costs = new ArrayList<>();
        final Set<UUID> seen = new HashSet<>();
        WeakReference<Container> container;
        ScanCursor cursor;
        /** Duplicates met (COUNT, STRIP) and taken (STRIP), by relic id. */
        final List<String> duplicateIds = new ArrayList<>();
        final List<String> removedIds = new ArrayList<>();

        Job(Run run, ServerPlayer actor) { this.run = run; this.actor = actor; }

        /** Adds a place if what is there can matter: a relic, or something with stacks inside it. */
        void add(StackRef ref, Spot spot) {
            ItemStack s = ref.get();
            int c = costOf(s);
            if (c <= 0) return;
            refs.add(ref);
            spots.add(spot);
            costs.add(c);
        }

        Job seal() {
            int[] c = new int[costs.size()];
            for (int i = 0; i < c.length; i++) c[i] = costs.get(i);
            cursor = new ScanCursor(c);
            return this;
        }

        boolean empty() { return refs.isEmpty(); }
    }

    /** 0 if the stack cannot matter; else 1 plus the stacks inside it. */
    static int costOf(ItemStack s) {
        if (s == null || s.isEmpty()) return 0;
        int inside = OrderNest.count(s);
        return OrderRelics.defOf(s) != null || inside > 0 ? 1 + inside : 0;
    }

    // ================================================================ the running jobs
    private static final Map<UUID, Job> INVENTORY = new HashMap<>();
    private static final Map<UUID, Job> MENU = new HashMap<>();
    private static final Set<UUID> FORCE = new HashSet<>();

    /** The Order wants everybody looked at on the next tick. */
    static void requestAll(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) FORCE.add(p.getUUID());
    }

    static void forget(UUID player) {
        INVENTORY.remove(player);
        MENU.remove(player);
        FORCE.remove(player);
    }

    static void clear() {
        INVENTORY.clear();
        MENU.clear();
        FORCE.clear();
        CENSUS_DUPES.clear();
        WARNED.clear();
    }

    /** Every tick of a player (server side): starts their inventory scan on their turn and gives the running jobs their sixteen stacks. */
    static void onPlayerTick(MinecraftServer server, ServerPlayer p, long tick) {
        UUID id = p.getUUID();
        Job menu = MENU.get(id);
        Job inv = INVENTORY.get(id);
        if (inv == null && (FORCE.remove(id) || ScanCursor.shouldStart(tick, p.getId(), ScanCursor.PLAYER_INTERVAL))) {
            inv = forPlayer(Run.SCAN, p);
            INVENTORY.put(id, inv);
        }
        if (menu == null && inv == null) return;
        UniqueLedger ledger = UniqueLedger.get(server);
        int budget = ScanCursor.PER_TICK;
        if (menu != null) {
            budget -= step(server, ledger, menu, budget, tick);
            if (menu.cursor.done()) MENU.remove(id);
        }
        if (inv != null && budget > 0) {
            step(server, ledger, inv, budget, tick);
            if (inv.cursor.done()) INVENTORY.remove(id);
        }
    }

    /** A player opened or closed a container: its stacks are looked at over the next ticks. */
    static void onMenu(ServerPlayer p, AbstractContainerMenu menu) {
        Job j = forMenu(Run.SCAN, p, menu);
        if (j == null || j.empty()) MENU.remove(p.getUUID());
        else MENU.put(p.getUUID(), j);
    }

    /** An item entity joined a level, or a mob changed its equipment: looked at at once (a handful of stacks). */
    static void now(MinecraftServer server, Job j, long tick) {
        if (j == null || j.empty()) return;
        UniqueLedger ledger = UniqueLedger.get(server);
        while (!j.cursor.done()) step(server, ledger, j, Integer.MAX_VALUE, tick);
    }

    /** Runs a job to its end in one go (the Order's COUNT and STRIP runs: bounded by the size of one person's belongings). */
    static void runAll(MinecraftServer server, Job j, long tick) { now(server, j, tick); }

    // ================================================================ stepping
    /** Examines the next stacks of a job within 'budget'. Returns the cost spent. */
    private static int step(MinecraftServer server, UniqueLedger ledger, Job j, int budget, long tick) {
        int from = j.cursor.position();
        int n = j.cursor.take(budget);
        int spent = 0;
        for (int i = from; i < from + n; i++) {
            spent += Math.max(1, j.costs.get(i));
            StackRef ref = j.refs.get(i);
            Spot spot = j.spots.get(i);
            try {
                examine(server, ledger, j, ref, spot, tick);
                for (StackRef inside : StackRefs.insideOf(ref)) examine(server, ledger, j, inside, spot, tick);
            } catch (RuntimeException ex) {
                LOG.warn("[nusmp] The Order could not examine a stack: {}", ex.toString());
            }
        }
        return Math.max(spent, n > 0 ? 1 : 0);
    }

    private static void examine(MinecraftServer server, UniqueLedger L, Job j, StackRef ref, Spot spot, long tick) {
        ItemStack s = ref.get();
        RelicDef def = OrderRelics.defOf(s);
        if (!OrderRelics.unique(def)) return;
        String id = def.id();
        UUID serial = RelicSerial.get(s);
        Verdict v = LedgerRules.judge(true, L.known(id), serial, serial != null && j.seen.contains(serial));
        if (v == Verdict.CANONICAL && movedFromAnotherCopy(server, L.entry(id), spot, s)) v = Verdict.DUPLICATE;
        if (v == Verdict.DUPLICATE) {
            j.duplicateIds.add(id);
            if (j.run == Run.COUNT) return;
            if (j.run == Run.STRIP) {
                if (StackRefs.clear(ref)) j.removedIds.add(id);
                else LOG.warn("[nusmp] The Order could not take a duplicate of {} {}", id, spot.describe());
                return;
            }
        }
        if (j.run == Run.COUNT) {
            if (serial != null && v != Verdict.DUPLICATE) j.seen.add(serial);
            return;
        }
        Mode mode = j.run == Run.STRIP ? Mode.ENFORCE : L.mode(tick);
        Action a = LedgerRules.action(v, mode);
        switch (a) {
            case ADOPT, REVIVE -> {
                UUID use = serial != null ? serial : UUID.randomUUID();
                if (serial == null && !StackRefs.stamp(ref, use)) {
                    LOG.warn("[nusmp] The Order could not stamp {} {}; it is left unrecorded", id, spot.describe());
                    return;
                }
                L.adopt(id, use, spot, tick, true);
                j.seen.add(use);
            }
            case TOUCH -> {
                L.touch(id, spot, tick);
                if (serial != null) j.seen.add(serial);
            }
            case WARN -> warnCensus(server, L, id, spot, tick);
            case ORDER -> OrderDirector.report(server, new Sighting(id, spot, j.container, tick));
            default -> { }
        }
    }

    // ================================================================ is the recorded copy still where the ledger says?
    /**
     * The ledger remembers the valid copy somewhere else, and that place still demonstrably holds a stack with its serial: then this stack is a second
     * copy. Only places that can be checked count (an online player, an item entity or a mob that is loaded); a container cannot be checked, so a copy
     * that was moved into one and out of it again is simply taken to have moved.
     */
    private static boolean movedFromAnotherCopy(MinecraftServer server, UniqueLedger.Entry e, Spot here, ItemStack self) {
        if (e == null || !e.live || e.serial == null || e.holder == null || e.spot().sameAs(here)) return false;
        switch (e.kind) {
            case PLAYER: {
                ServerPlayer p = server.getPlayerList().getPlayer(e.holder);
                return p != null && holds(p, e.serial, self);
            }
            case GROUND:
            case MOB:
                for (ServerLevel lv : server.getAllLevels()) {
                    Entity en = lv.getEntity(e.holder);
                    if (en == null) continue;
                    if (en instanceof ItemEntity ie) return ie.getItem() != self && e.serial.equals(RelicSerial.get(ie.getItem()));
                    if (en instanceof LivingEntity le) {
                        for (EquipmentSlot es : EQUIPMENT) {
                            ItemStack worn = le.getItemBySlot(es);
                            if (worn != self && e.serial.equals(RelicSerial.get(worn))) return true;
                        }
                    }
                    return false;
                }
                return false;
            default:
                return false;
        }
    }

    private static final EquipmentSlot[] EQUIPMENT = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /**
     * Does this player carry a stack with this serial (inventory, ender chest, Curios, the cursor; one level into shulker boxes and bundles)? The stack
     * 'self' does not count: it is the one being judged, and the same object is still in the player's slot while it is being dropped.
     */
    static boolean holds(ServerPlayer p, UUID serial, ItemStack self) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (has(inv.getItem(i), serial, self)) return true;
        Container ender = p.getEnderChestInventory();
        for (int i = 0; i < ender.getContainerSize(); i++) if (has(ender.getItem(i), serial, self)) return true;
        for (ItemStack s : CuriosLink.equipped(p)) if (has(s, serial, self)) return true;
        return has(p.containerMenu.getCarried(), serial, self);
    }

    private static boolean has(ItemStack s, UUID serial, ItemStack self) {
        if (s.isEmpty() || s == self) return false;
        if (serial.equals(RelicSerial.get(s))) return true;
        for (ItemStack in : OrderNest.container(s)) if (in != self && serial.equals(RelicSerial.get(in))) return true;
        for (ItemStack in : OrderNest.bundle(s)) if (in != self && serial.equals(RelicSerial.get(in))) return true;
        return false;
    }

    // ================================================================ building jobs
    /** All of a player's belongings: inventory, armour, hands, the cursor, the ender chest, Curios. */
    static Job forPlayer(Run run, ServerPlayer p) {
        Job j = new Job(run, p);
        Spot spot = Spot.player(p);
        Set<ItemStack> have = Collections.newSetFromMap(new IdentityHashMap<>());
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (costOf(s) > 0) have.add(s);
            j.add(StackRefs.container(inv, i), spot);
        }
        Container ender = p.getEnderChestInventory();
        for (int i = 0; i < ender.getContainerSize(); i++) j.add(StackRefs.container(ender, i), spot);
        j.add(StackRefs.carried(p.containerMenu), spot);
        for (ItemStack s : CuriosLink.equipped(p)) if (have.add(s)) j.add(StackRefs.raw(s), spot);
        for (ItemStack s : Relics.equipped(p)) if (have.add(s)) j.add(StackRefs.raw(s), spot);
        return j.seal();
    }

    /** The slots of an opened menu that belong to the container (not the player's own inventory, not an output slot). Null for the plain inventory. */
    static Job forMenu(Run run, ServerPlayer p, AbstractContainerMenu menu) {
        if (menu == null || menu == p.inventoryMenu) return null;
        Job j = new Job(run, p);
        BlockPos at = null;
        Container first = null;
        List<Slot> picked = new ArrayList<>();
        for (Slot slot : menu.slots) {
            Container c = slot.container;
            if (c == null || c instanceof Inventory || slot instanceof ResultSlot) continue;
            if (first == null) first = c;
            if (at == null) at = placeOf(c);
            picked.add(slot);
        }
        if (first == null) return null;
        j.container = new WeakReference<>(first);
        Spot spot = Spot.container(p, at);
        for (Slot slot : picked) j.add(StackRefs.slot(slot), spot);
        return j.seal();
    }

    /** The stacks of a container object itself (the Order's visit, when the person who opened it has closed it). */
    static Job forContainer(Run run, ServerPlayer actor, Container c, Spot spot) {
        Job j = new Job(run, actor);
        j.container = new WeakReference<>(c);
        for (int i = 0; i < c.getContainerSize(); i++) j.add(StackRefs.container(c, i), spot);
        return j.seal();
    }

    static Job forItemEntity(Run run, ItemEntity e, ServerLevel level) {
        Job j = new Job(run, null);
        j.add(StackRefs.itemEntity(e), Spot.entity(Spot.Kind.GROUND, e, level));
        return j.seal();
    }

    static Job forMob(Run run, LivingEntity e, ServerLevel level) {
        Job j = new Job(run, null);
        Spot spot = Spot.entity(Spot.Kind.MOB, e, level);
        for (EquipmentSlot es : EQUIPMENT) j.add(StackRefs.equipment(e, es), spot);
        return j.seal();
    }

    private static BlockPos placeOf(Container c) {
        if (c instanceof BlockEntity be) return be.getBlockPos();
        if (c instanceof Entity e) return e.blockPosition();
        return null;
    }

    /** The jobs that cover the place a duplicate was seen at (for the Order's COUNT and STRIP runs). */
    static List<Job> locate(MinecraftServer server, Run run, Sighting s) {
        List<Job> out = new ArrayList<>();
        Spot sp = s.spot();
        ServerLevel level = levelOf(server, sp.dim());
        ServerPlayer who = sp.holder() == null ? null : server.getPlayerList().getPlayer(sp.holder());
        switch (sp.kind()) {
            case PLAYER:
                if (who != null) {
                    out.add(forPlayer(run, who));
                    Job m = forMenu(run, who, who.containerMenu);
                    if (m != null) out.add(m);
                }
                break;
            case CONTAINER: {
                Container c = s.container() == null ? null : s.container().get();
                if (who != null) {
                    Job m = forMenu(run, who, who.containerMenu);
                    if (m != null) out.add(m);
                }
                if (c == null && level != null && level.isLoaded(sp.blockPos()) && level.getBlockEntity(sp.blockPos()) instanceof Container bc) c = bc;
                if (c != null) out.add(forContainer(run, who, c, sp));
                break;
            }
            case GROUND:
                if (level != null && level.getEntity(sp.holder()) instanceof ItemEntity ie) out.add(forItemEntity(run, ie, level));
                break;
            case MOB:
                if (level != null && level.getEntity(sp.holder()) instanceof LivingEntity le) out.add(forMob(run, le, level));
                break;
            default:
                break;
        }
        return out;
    }

    static ServerLevel levelOf(MinecraftServer server, String dim) {
        if (dim == null || dim.isEmpty()) return null;
        ResourceLocation rl = ResourceLocation.tryParse(dim);
        if (rl == null) return null;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, rl);
        return server.getLevel(key);
    }

    // ================================================================ the census
    /** relic id -> the names of the people who hold an extra copy, for the summary at the end. */
    private static final Map<String, Set<String>> CENSUS_DUPES = new LinkedHashMap<>();
    private static final Set<String> WARNED = new HashSet<>();

    static int censusGroups() { return CENSUS_DUPES.size(); }

    static void resetCensus() {
        CENSUS_DUPES.clear();
        WARNED.clear();
    }

    private static void warnCensus(MinecraftServer server, UniqueLedger L, String id, Spot spot, long tick) {
        CENSUS_DUPES.computeIfAbsent(id, k -> new LinkedHashSet<>()).add(spot.holderName() == null ? "" : spot.holderName());
        int minutes = LedgerRules.censusMinutesLeft(L.censusEnd(), tick);
        if (spot.holder() != null && WARNED.add(id + "@" + spot.holder())) {
            ServerPlayer holder = server.getPlayerList().getPlayer(spot.holder());
            if (holder != null) holder.sendSystemMessage(OrderChat.censusWarning(id, minutes));
        }
        if (WARNED.add("ops@" + id)) {
            for (ServerPlayer op : OrderChat.ops(server)) op.sendSystemMessage(OrderChat.censusWarning(id, minutes).copy()
                    .append(net.minecraft.network.chat.Component.literal(" (" + spot.describe() + ")")));
        }
    }
}
