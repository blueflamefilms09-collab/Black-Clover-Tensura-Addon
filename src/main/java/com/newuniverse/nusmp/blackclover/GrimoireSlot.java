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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * The dedicated Grimoire Slot (0.22): a bound grimoire lives here instead of the hotbar or inventory, and works from here (casting,
 * Summon Grimoire). While the slot is filled the book hangs dormant at the owner's right hip; an empty slot means no grimoire is
 * carried. Kept through death. Open it from the Multiverse status screen, the "Grimoire Slot" key or /multiverse grimoire slot.
 *
 * <p>Every grimoire the mod hands out still lands in the inventory first; each tick an owned grimoire found outside the hands is
 * moved into an empty slot, so all the old grant paths fill the slot without changes. A grimoire held in a hand is left alone
 * (the altar and old habits still work).
 *
 * <p>Sync: {@link HipPayload} (entity id + the slot's stack) to the owner and everyone tracking them, for the hip book.
 */
public final class GrimoireSlot {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> SLOT = ATTACHMENTS.register("grimoire_slot",
            () -> AttachmentType.builder(() -> ItemStack.EMPTY).serialize(ItemStack.OPTIONAL_CODEC).copyOnDeath().build());

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, NUSMP.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<Menu>> MENU = MENUS.register("grimoire_slot",
            () -> IMenuTypeExtension.create((id, inv, buf) -> new Menu(id, inv, new SimpleContainer(1))));

    /** Last stack sent per player, so the hip book is only re-sent when it changes. */
    private static final Map<UUID, ItemStack> SENT = new HashMap<>();

    private GrimoireSlot() {}

    // ---------------------------------------------------------------- access
    public static ItemStack get(Player p) { return p.getData(SLOT); }

    public static void set(Player p, ItemStack stack) { p.setData(SLOT, stack == null ? ItemStack.EMPTY : stack); }

    /** Where a grimoire counts as "with you, ready": the slot, then both hands. */
    public static List<ItemStack> ready(Player p) { return List.of(get(p), p.getMainHandItem(), p.getOffhandItem()); }

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
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (get(p).isEmpty()) {
            Inventory inv = p.getInventory();
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == inv.selected) continue;                                   // a book in the hand stays there
                ItemStack s = inv.items.get(i);
                if (!GrimoireItem.isOwnedBy(s, p.getUUID())) continue;
                set(p, s.copy());
                inv.items.set(i, ItemStack.EMPTY);
                break;
            }
        }
        ItemStack now = get(p), last = SENT.get(p.getUUID());
        if (last == null || !ItemStack.matches(last, now)) {
            SENT.put(p.getUUID(), now.copy());
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new HipPayload(p.getId(), now.copy()));
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { SENT.remove(e.getEntity().getUUID()); }

    /** Respawn / dimension change make a new client entity: send again. */
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { SENT.remove(e.getEntity().getUUID()); }
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { SENT.remove(e.getEntity().getUUID()); }

    public static void onStartTracking(PlayerEvent.StartTracking e) {
        if (!(e.getEntity() instanceof ServerPlayer watcher) || !(e.getTarget() instanceof ServerPlayer target)) return;
        ItemStack s = get(target);
        if (!s.isEmpty()) PacketDistributor.sendToPlayer(watcher, new HipPayload(target.getId(), s.copy()));
    }

    // ---------------------------------------------------------------- menu
    public static void open(ServerPlayer p) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new Menu(id, inv, new SlotContainer(p)), Component.literal("Grimoire Slot")));
    }

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
            ItemStack out = s.copyWithCount(Math.min(amount, s.getCount()));
            ItemStack rest = s.copyWithCount(s.getCount() - out.getCount());
            set(owner, rest);
            return out;
        }
        @Override public ItemStack removeItemNoUpdate(int slot) { ItemStack s = get(owner); set(owner, ItemStack.EMPTY); return s; }
        @Override public void setItem(int slot, ItemStack stack) { set(owner, stack.copy()); }
        @Override public int getMaxStackSize() { return 1; }
        @Override public void setChanged() {}
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
        public static final Type<HipPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "grimoire_hip"));
        public static final StreamCodec<RegistryFriendlyByteBuf, HipPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, HipPayload::entityId,
                ItemStack.OPTIONAL_STREAM_CODEC, HipPayload::stack,
                HipPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client asks to open its Grimoire Slot. */
    public record OpenPayload() implements CustomPacketPayload {
        public static final OpenPayload INSTANCE = new OpenPayload();
        public static final Type<OpenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "grimoire_slot_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(HipPayload.TYPE, HipPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.grimoire.GrimoireFloatClient.receiveHip(payload.entityId(), payload.stack())));
        r.playToServer(OpenPayload.TYPE, OpenPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) open(sp); }));
    }
}
