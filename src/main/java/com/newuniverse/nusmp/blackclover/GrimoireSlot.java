package com.newuniverse.nusmp.blackclover;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * The dedicated Grimoire Slot (0.22): a bound grimoire lives here instead of the hotbar or inventory, and works from here (casting,
 * Summon Grimoire). While the slot is filled the book hangs dormant at the owner's right hip; an empty slot means no grimoire is
 * carried. Kept through death. Open it from the Multiverse status screen, the "Grimoire Slot" key or /multiverse grimoire slot.
 *
 * <p>Every grimoire the mod hands out still lands in the inventory first; each tick an owned grimoire found anywhere in the
 * inventory (hotbar and offhand included) is moved into an empty slot, so all the grant paths fill the slot without changes.
 * Casting needs the book summoned from the slot (book.GrimoireSummon); the altar reads the slot too.
 *
 * <p>Sync: {@link HipPayload} (entity id + the slot's stack) to the owner and everyone tracking them, for the hip book. It is sent only when the
 * slot changed ({@link SlotClickRules.Signal}: a version bumped by {@link #set} and {@code setChanged}, plus one deep compare per second as the net for a stack
 * edited in place); the inventory scan runs once per second per player, spread by UUID. Never a stack copy per tick.
 *
 * <p>Setting {@code grimoireSlotInventory} (docs/grimoire_slot_rework.md, default on): the slot is drawn and used inside the player's own inventory screen
 * ({@link SlotClickPayload}); off = the separate {@link Menu} window of 0.22.
 */
public final class GrimoireSlot {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> SLOT = ATTACHMENTS.register("grimoire_slot",
            () -> AttachmentType.builder(() -> ItemStack.EMPTY).serialize(ItemStack.OPTIONAL_CODEC).copyOnDeath().build());

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, NUSMP.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<Menu>> MENU = MENUS.register("grimoire_slot",
            () -> IMenuTypeExtension.create((id, inv, buf) -> new Menu(id, inv, new SimpleContainer(1))));

    /** Per-player sync state, an attachment on the player (gone with the entity: a respawn starts fresh and sends again; never serialized). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<State>> STATE = ATTACHMENTS.register("grimoire_slot_state",
            () -> AttachmentType.builder(State::new).build());

    /** Last stack sent (one copy, made when it was sent), the change signal, the click and open rate limits, and the scan flags. */
    public static final class State {
        final SlotClickRules.Signal signal = new SlotClickRules.Signal();
        final SlotClickRules.Limiter clicks = new SlotClickRules.Limiter(), opens = new SlotClickRules.Limiter();
        ItemStack sent = ItemStack.EMPTY;
        boolean needScan = true;                // the first tick after login / respawn / dimension change
        long scanPausedUntil;                   // a book the player took out on purpose is not pulled back at once (inventory mode only)
    }

    /** True when the slot is used inside the inventory screen (server config {@code grimoireSlotInventory}). */
    public static boolean inventoryMode() { return com.newuniverse.nusmp.NUConfig.grimoireSlotInventory(); }

    private GrimoireSlot() {}

    // ---------------------------------------------------------------- access
    public static ItemStack get(Player p) { return p.getData(SLOT); }

    public static void set(Player p, ItemStack stack) {
        p.setData(SLOT, stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack);          // a count-0 leftover is stored as EMPTY
        touch(p);
    }

    /** The slot's stack changed (or was edited in place): the hip book is sent on the next tick. */
    public static void touch(Player p) { p.getData(STATE).signal.bump(); }

    /** Where a bound grimoire counts as carried (0.24: the Grimoire Slot only; the hotbar and hands never enable anything). */
    public static List<ItemStack> ready(Player p) { return List.of(get(p)); }

    /** True if the slot or a hand holds a stack matching {@code test}. */
    public static boolean anyReady(Player p, Predicate<ItemStack> test) {
        for (ItemStack s : ready(p)) if (!s.isEmpty() && test.test(s)) return true;
        return false;
    }

    /** True if the slot holds one of the player's own grimoires (optionally of this magic). */
    public static boolean holdsOwn(Player p, MagicType magic) {
        ItemStack s = get(p);
        return GrimoireItem.isOwnedBy(s, p.getUUID()) && (magic == null || magic.name().equals(GrimoireItem.data(s).getString("Magic")));
    }

    /** Applies {@code fn} to the slot's grimoire if it is the player's own (rebuilt covers, darkened books, canon looks). */
    public static boolean replaceOwned(Player p, UnaryOperator<ItemStack> fn) {
        ItemStack s = get(p);
        if (!GrimoireItem.isOwnedBy(s, p.getUUID())) return false;
        set(p, fn.apply(s));
        return true;
    }

    // ---------------------------------------------------------------- tick: fill the slot, sync the hip book
    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_S_BOOK_GRIMOIRE_SLOT = com.newuniverse.nusmp.perf.PerfProbe.section("s.book.grimoire_slot");   // 1.99.0: performance probe (off by default: one flag read)

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            onPlayerTick0(event);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_S_BOOK_GRIMOIRE_SLOT, t0);
        }
    }

    private static void onPlayerTick0(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        State st = p.getData(STATE);
        if (st.needScan || SlotClickRules.scanDue(p.tickCount, SlotClickRules.phase(p.getUUID()))) {
            st.needScan = false;
            if (p.tickCount >= st.scanPausedUntil && get(p).isEmpty()) scanInventory(p);
            // the net for a stack edited in place: one deep compare per second, never per tick
            if (!st.signal.dirty() && !ItemStack.matches(st.sent, get(p))) st.signal.bump();
        }
        if (st.signal.dirty()) sendHip(p, st);
    }

    /** Moves the first owned grimoire of the inventory (main slots, then the offhand) into the empty slot. True if one moved. */
    static boolean scanInventory(ServerPlayer p) {
        if (!get(p).isEmpty()) return false;
        Inventory inv = p.getInventory();
        UUID id = p.getUUID();
        for (int pass = 0; pass < 2; pass++) {
            var list = pass == 0 ? inv.items : inv.offhand;
            for (int i = 0; i < list.size(); i++) {
                ItemStack s = list.get(i);
                if (!GrimoireItem.isOwnedBy(s, id)) continue;
                list.set(i, ItemStack.EMPTY);
                set(p, s);                                  // the stack itself moves: no copy
                return true;
            }
        }
        return false;
    }

    /** One copy of the stack, made now, is both the snapshot for the next compare and the packet's content (never mutated afterwards). */
    private static void sendHip(ServerPlayer p, State st) {
        ItemStack snap = get(p).copy();
        st.sent = snap;
        st.signal.markSent();
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new HipPayload(p.getId(), snap));
    }

    /** Sends the hip book now if the slot changed (after a click or a menu change), instead of waiting for the next tick. */
    public static void syncNow(ServerPlayer p) {
        State st = p.getData(STATE);
        if (st.signal.dirty()) sendHip(p, st);
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) PacketDistributor.sendToPlayer(p, new ModePayload(inventoryMode(), false));
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { resend(e.getEntity()); }

    /** Respawn / dimension change make a new client entity: send again (and look at the inventory once). */
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { resend(e.getEntity()); }
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { resend(e.getEntity()); }

    private static void resend(Player p) {
        State st = p.getData(STATE);
        st.signal.reset();
        st.needScan = true;
        st.sent = ItemStack.EMPTY;
    }

    public static void onStartTracking(PlayerEvent.StartTracking e) {
        if (!(e.getEntity() instanceof ServerPlayer watcher) || !(e.getTarget() instanceof ServerPlayer target)) return;
        ItemStack s = get(target);
        if (!s.isEmpty()) PacketDistributor.sendToPlayer(watcher, new HipPayload(target.getId(), s.copy()));
    }

    // ---------------------------------------------------------------- open
    /** The key, the status-screen button and /multiverse slot land here: the inventory screen (inventory mode) or the old window. */
    public static void open(ServerPlayer p) {
        if (inventoryMode()) { PacketDistributor.sendToPlayer(p, new ModePayload(true, true)); return; }   // the client opens its own inventory screen
        openWindow(p);
    }

    /** The old separate window (kept, rule 2). */
    public static void openWindow(ServerPlayer p) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new Menu(id, inv, new SlotContainer(p)), Component.literal("Grimoire Slot")));
    }

    // ---------------------------------------------------------------- click in the inventory screen
    /**
     * A click on the slot drawn in the inventory screen. The server decides everything: a ServerPlayer whose open menu is its own inventory menu,
     * rate limited, button 0 or 1; only an owned grimoire is placed (one of the cursor stack), taking gives the book to the cursor.
     */
    public static void click(ServerPlayer p, int button, boolean shift) {
        if (!inventoryMode()) { PacketDistributor.sendToPlayer(p, new ModePayload(false, false)); return; }   // the client was told wrong: correct it
        if ((button != 0 && button != 1) || !p.isAlive() || p.isSpectator() || p.containerMenu != p.inventoryMenu) return;
        State st = p.getData(STATE);
        if (!st.clicks.allow(p.tickCount)) return;
        if (!apply(p, st, button, shift)) return;
        p.containerMenu.broadcastChanges();                                  // the cursor and the inventory slots reach the client now
        syncNow(p);                                                           // and so does the hip book
    }

    /** The state change of one validated click (no packets). True if something changed. */
    static boolean apply(ServerPlayer p, State st, int button, boolean shift) {
        AbstractContainerMenu menu = p.containerMenu;
        ItemStack cursor = menu.getCarried();
        ItemStack slot = get(p);
        switch (SlotClickRules.decide(GrimoireItem.isOwnedBy(cursor, p.getUUID()), cursor.isEmpty(), slot.isEmpty(), shift, button)) {
            case PLACE -> {
                set(p, cursor.split(1));                                    // one of the stack, never the whole stack
                if (cursor.isEmpty()) menu.setCarried(ItemStack.EMPTY);
            }
            case SWAP -> {
                if (cursor.getCount() != 1) return false;                    // the book could not go back into a stack
                set(p, cursor);
                menu.setCarried(slot);
            }
            case TAKE -> {
                set(p, ItemStack.EMPTY);
                menu.setCarried(slot);
                st.scanPausedUntil = p.tickCount + PAUSE_TICKS;
            }
            case TO_INVENTORY -> {
                ItemStack moving = slot.copy();
                if (!p.getInventory().add(moving)) return false;             // no room: nothing changes
                set(p, ItemStack.EMPTY);
                st.scanPausedUntil = p.tickCount + PAUSE_TICKS;
            }
            case FROM_INVENTORY -> { if (!scanInventory(p)) return false; st.scanPausedUntil = 0; }
            case NONE -> { return false; }
        }
        return true;
    }

    /** After the owner takes the book out on purpose, the scan leaves the inventory alone this long (10 s), so it can be put somewhere else. */
    static final int PAUSE_TICKS = 200;

    /** The slot as a one-item Container over the attachment (server side). */
    static final class SlotContainer implements Container {
        private final Player owner;
        SlotContainer(Player owner) { this.owner = owner; }
        @Override public int getContainerSize() { return 1; }
        @Override public boolean isEmpty() { return get(owner).isEmpty(); }
        @Override public ItemStack getItem(int slot) { return get(owner); }
        @Override public ItemStack removeItem(int slot, int amount) {
            ItemStack s = get(owner);
            if (s.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            if (amount >= s.getCount()) { set(owner, ItemStack.EMPTY); return s; }       // the stored stack itself leaves: no copy of a big stack
            ItemStack out = s.split(amount);
            touch(owner);
            return out;
        }
        @Override public ItemStack removeItemNoUpdate(int slot) { ItemStack s = get(owner); set(owner, ItemStack.EMPTY); return s; }
        @Override public void setItem(int slot, ItemStack stack) { set(owner, stack); }   // stored as given, like every vanilla container (the caller hands the stack over)
        @Override public int getMaxStackSize() { return 1; }
        @Override public void setChanged() { touch(owner); }
        @Override public boolean stillValid(Player player) { return player == owner && owner.isAlive(); }
        @Override public void clearContent() { set(owner, ItemStack.EMPTY); }
    }

    /** One grimoire slot above the player's inventory. Only the player's own bound grimoire fits. */
    public static final class Menu extends AbstractContainerMenu {
        public static final int SLOT_X = 80, SLOT_Y = 26, INV_Y = 64;

        public Menu(int id, Inventory inv, Container slot) {
            super(MENU.get(), id);
            Player player = inv.player;
            addSlot(new Slot(slot, 0, SLOT_X, SLOT_Y) {
                @Override public boolean mayPlace(ItemStack stack) { return GrimoireItem.isOwnedBy(stack, player.getUUID()); }
                @Override public int getMaxStackSize() { return 1; }
            });
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, INV_Y + 58));
        }

        /** Double-click "collect all" must never pull the book out of the slot. */
        @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.index != 0 && super.canTakeItemForPickAll(stack, slot); }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            Slot from = slots.get(index);
            if (!from.hasItem()) return ItemStack.EMPTY;
            ItemStack stack = from.getItem(), copy = stack.copy();
            if (index == 0) {
                if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!slots.get(0).mayPlace(stack) || slots.get(0).hasItem() || !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) from.setByPlayer(ItemStack.EMPTY); else from.setChanged();
            return copy;
        }

        @Override public boolean stillValid(Player player) { return player.isAlive(); }
    }

    // ---------------------------------------------------------------- network
    /** Player {@code entityId}'s Grimoire Slot content (empty = no grimoire at the hip). */
    public record HipPayload(int entityId, ItemStack stack) implements CustomPacketPayload {
        public static final Type<HipPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "grimoire_hip"));
        public static final StreamCodec<RegistryFriendlyByteBuf, HipPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, HipPayload::entityId,
                ItemStack.OPTIONAL_STREAM_CODEC, HipPayload::stack,
                HipPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client asks to open its Grimoire Slot. */
    public record OpenPayload() implements CustomPacketPayload {
        public static final OpenPayload INSTANCE = new OpenPayload();
        public static final Type<OpenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "grimoire_slot_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client to server: a click on the slot in the inventory screen (button 0 / 1, shift held). The server validates everything. */
    public record SlotClickPayload(int button, boolean shift) implements CustomPacketPayload {
        public static final Type<SlotClickPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "grimoire_slot_click"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SlotClickPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SlotClickPayload::button,
                ByteBufCodecs.BOOL, SlotClickPayload::shift,
                SlotClickPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server to client: which mode the server runs (sent at login), and whether to open the inventory screen now. */
    public record ModePayload(boolean inventory, boolean open) implements CustomPacketPayload {
        public static final Type<ModePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "grimoire_slot_mode"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ModePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, ModePayload::inventory,
                ByteBufCodecs.BOOL, ModePayload::open,
                ModePayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(HipPayload.TYPE, HipPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.grimoire.GrimoireFloatClient.receiveHip(payload.entityId(), payload.stack())));
        r.playToServer(OpenPayload.TYPE, OpenPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp && sp.getData(STATE).opens.allow(sp.tickCount)) open(sp); }));
        r.playToServer(SlotClickPayload.TYPE, SlotClickPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) click(sp, payload.button(), payload.shift()); }));
        r.playToClient(ModePayload.TYPE, ModePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.grimoire.GrimoireSlotInventoryClient.receiveMode(payload.inventory(), payload.open())));
    }
}
