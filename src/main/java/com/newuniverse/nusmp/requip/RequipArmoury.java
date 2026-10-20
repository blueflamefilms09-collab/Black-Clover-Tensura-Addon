package com.newuniverse.nusmp.requip;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * Why: the pocket space of Fairy Tail's Requip (Equip) Magic. The user keeps weapons, armour and items in a dimension of their own and calls them back at will. Here that is a per-player DATA ATTACHMENT
 * ({@code nusmp:requip_armoury}): up to 54 cells that live on the server, are saved with the player, follow them through death, respawn and dimension changes and are not part of the inventory, so
 * an inventory wipe or another mod's inventory handling cannot touch them. The same attachment holds the six loadouts (templates of what to wear, see {@link LoadoutRules}), the weapon that Requip:
 * Blade has out, the weapon that Requip: Strike has thrown, and the Partial Armour setting.
 * <p>
 * The window is a container menu (server authoritative: every cell is read and written on the server, the client only mirrors it) with three tabs: Pocket (the cells), Loadouts (save, equip, clear,
 * reflex) and Partial Armour (the eleven families). Cells past the user's capacity ({@link ArmouryRules#capacity}) take nothing new but still give theirs back, so nothing is ever stranded or lost.
 * Nothing can be duplicated: a stack is moved, never copied, and the pure rules keep the totals.
 */
public final class RequipArmoury {
    private RequipArmoury() {}

    // ------------------------------------------------------------ the saved data
    /** One loadout: a template of what each of the {@link LoadoutRules#SLOTS} worn slots should hold, never the items themselves. */
    public static final class Loadout {
        public final List<ItemStack> template;
        public Loadout() { this.template = emptyList(LoadoutRules.SLOTS); }
        public Loadout(List<ItemStack> t) {
            this.template = emptyList(LoadoutRules.SLOTS);
            for (int i = 0; i < Math.min(t.size(), LoadoutRules.SLOTS); i++) this.template.set(i, t.get(i).copy());
        }
        public boolean saved() { for (ItemStack s : template) if (!s.isEmpty()) return true; return false; }
        public int pieces() { int n = 0; for (ItemStack s : template) if (!s.isEmpty()) n++; return n; }
        public void clear() { for (int i = 0; i < template.size(); i++) template.set(i, ItemStack.EMPTY); }
    }

    /** Everything one player keeps in the pocket space. Mutable; read and written on the server. */
    public static final class Data {
        public final List<ItemStack> cells = emptyList(ArmouryRules.MAX_SLOTS);
        public final List<Loadout> loadouts = new ArrayList<>();
        /** The weapon Requip: Strike has thrown (empty when none is out). It is saved, so a thrown weapon survives a relog, a crash and a death. */
        public ItemStack limbo = ItemStack.EMPTY;
        /** The hotbar slot the thrown weapon left, so it comes home to the same place. */
        public int limboSlot = -1;
        /** Requip: Blade: the cell the displaced item went to (-1 when no weapon is out), a copy of the weapon that was summoned and of what it displaced. */
        public int blade = -1;
        public ItemStack bladeSig = ItemStack.EMPTY, bladeBack = ItemStack.EMPTY;
        /** The armoury cell of the weapon Blade and Barrage use (-1 = the first weapon found). */
        public int selected = -1;
        /** The loadout in use and the loadout the Requip Reflex swaps to (-1 = none). */
        public int active = -1, reflex = -1;
        /** Partial Armour: the families worn and whether the partial setting is on. */
        public int mask = PartialArmourMask.FULL;
        public boolean partialOn = false;
        /** Game time of the last Requip Reflex. */
        public long reflexAt = Long.MIN_VALUE / 4;

        public Data() { fix(); }

        @SuppressWarnings("java:S107")
        Data(List<ItemStack> cells, List<Loadout> loadouts, ItemStack limbo, int limboSlot, int blade, ItemStack bladeSig, ItemStack bladeBack, int selected, int active, int reflex, int mask,
             boolean partialOn, long reflexAt) {
            for (int i = 0; i < Math.min(cells.size(), ArmouryRules.MAX_SLOTS); i++) this.cells.set(i, cells.get(i));
            this.loadouts.addAll(loadouts);
            this.limbo = limbo; this.limboSlot = limboSlot; this.blade = blade; this.bladeSig = bladeSig; this.bladeBack = bladeBack;
            this.selected = selected; this.active = active; this.reflex = reflex; this.mask = mask; this.partialOn = partialOn; this.reflexAt = reflexAt;
            fix();
        }

        /** Repairs the shape after a load: six loadouts, indices in range. */
        private void fix() {
            while (loadouts.size() < LoadoutRules.MAX_LOADOUTS) loadouts.add(new Loadout());
            while (loadouts.size() > LoadoutRules.MAX_LOADOUTS) loadouts.remove(loadouts.size() - 1);
            if (selected < -1 || selected >= ArmouryRules.MAX_SLOTS) selected = -1;
            if (blade < -1 || blade >= ArmouryRules.MAX_SLOTS) blade = -1;
            if (active < -1 || active >= LoadoutRules.MAX_LOADOUTS) active = -1;
            if (reflex < -1 || reflex >= LoadoutRules.MAX_LOADOUTS) reflex = -1;
            mask = PartialArmourMask.normalize(mask);
        }

        public boolean isEmpty() {
            if (!limbo.isEmpty() || blade >= 0 || partialOn || reflex >= 0 || mask != PartialArmourMask.FULL) return false;
            for (ItemStack s : cells) if (!s.isEmpty()) return false;
            for (Loadout l : loadouts) if (l.saved()) return false;
            return true;
        }

        public boolean[] savedFlags() {
            boolean[] f = new boolean[LoadoutRules.MAX_LOADOUTS];
            for (int i = 0; i < f.length; i++) f[i] = loadouts.get(i).saved();
            return f;
        }
    }

    private static List<ItemStack> emptyList(int n) {
        List<ItemStack> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++) l.add(ItemStack.EMPTY);
        return l;
    }

    // ------------------------------------------------------------ codecs
    /** A stack that never fails the whole list: a stack that cannot be read (an item of a mod that was removed) is dropped, as vanilla drops it from an inventory, instead of taking the armoury with it. */
    private static final Codec<ItemStack> STACK = new Codec<>() {
        @Override
        public <T> DataResult<Pair<ItemStack, T>> decode(DynamicOps<T> ops, T input) {
            DataResult<Pair<ItemStack, T>> r = ItemStack.OPTIONAL_CODEC.decode(ops, input);
            return r.result().isPresent() ? r : DataResult.success(Pair.of(ItemStack.EMPTY, input));
        }

        @Override
        public <T> DataResult<T> encode(ItemStack s, DynamicOps<T> ops, T prefix) { return ItemStack.OPTIONAL_CODEC.encode(s, ops, prefix); }
    };

    private static final Codec<Loadout> LOADOUT_CODEC = STACK.listOf().xmap(Loadout::new, l -> l.template);

    public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
            STACK.listOf().optionalFieldOf("cells", List.of()).forGetter(d -> d.cells),
            LOADOUT_CODEC.listOf().optionalFieldOf("loadouts", List.of()).forGetter(d -> d.loadouts),
            STACK.optionalFieldOf("limbo", ItemStack.EMPTY).forGetter(d -> d.limbo),
            Codec.INT.optionalFieldOf("limbo_slot", -1).forGetter(d -> d.limboSlot),
            Codec.INT.optionalFieldOf("blade", -1).forGetter(d -> d.blade),
            STACK.optionalFieldOf("blade_sig", ItemStack.EMPTY).forGetter(d -> d.bladeSig),
            STACK.optionalFieldOf("blade_back", ItemStack.EMPTY).forGetter(d -> d.bladeBack),
            Codec.INT.optionalFieldOf("selected", -1).forGetter(d -> d.selected),
            Codec.INT.optionalFieldOf("active", -1).forGetter(d -> d.active),
            Codec.INT.optionalFieldOf("reflex", -1).forGetter(d -> d.reflex),
            Codec.INT.optionalFieldOf("mask", PartialArmourMask.FULL).forGetter(d -> d.mask),
            Codec.BOOL.optionalFieldOf("partial_on", false).forGetter(d -> d.partialOn),
            Codec.LONG.optionalFieldOf("reflex_at", Long.MIN_VALUE / 4).forGetter(d -> d.reflexAt)
    ).apply(i, Data::new));

    // ------------------------------------------------------------ registration
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);
    /** {@code nusmp:requip_armoury}: saved with the player, copied on death (the pocket space is not lost by dying). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Data>> DATA = ATTACHMENTS.register("requip_armoury",
            () -> AttachmentType.builder(() -> new Data()).serialize(CODEC, d -> !d.isEmpty()).copyOnDeath().build());

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, NUSMP.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<Menu>> MENU = MENUS.register("requip_armoury",
            () -> IMenuTypeExtension.create((id, inv, buf) -> new Menu(id, inv, new SimpleContainer(ArmouryRules.MAX_SLOTS))));

    public static Data data(Player p) { return p.getData(DATA); }

    // ------------------------------------------------------------ the stack adaptor
    /** The rules' view of an {@link ItemStack}. */
    public static final ArmouryRules.Ops<ItemStack> OPS = new ArmouryRules.Ops<>() {
        @Override public ItemStack empty() { return ItemStack.EMPTY; }
        @Override public boolean isEmpty(ItemStack s) { return s == null || s.isEmpty(); }
        @Override public int count(ItemStack s) { return s.getCount(); }
        @Override public int max(ItemStack s) { return s.getMaxStackSize(); }
        @Override public boolean same(ItemStack a, ItemStack b) { return ItemStack.isSameItemSameComponents(a, b); }
        @Override public ItemStack withCount(ItemStack s, int count) { return count <= 0 ? ItemStack.EMPTY : s.copyWithCount(count); }
    };

    /** The same item with the same name, enchantments and custom data: the identity of a worn weapon, whatever its damage. */
    public static boolean sameIdentity(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty() || !ItemStack.isSameItem(a, b)) return false;
        return Objects.equals(a.get(DataComponents.CUSTOM_DATA), b.get(DataComponents.CUSTOM_DATA)) && Objects.equals(a.get(DataComponents.CUSTOM_NAME), b.get(DataComponents.CUSTOM_NAME))
                && Objects.equals(a.get(DataComponents.ENCHANTMENTS), b.get(DataComponents.ENCHANTMENTS));
    }

    /** What may go into the pocket: anything, except the weapon an armour requipped (it goes with the armour) . */
    public static boolean mayStore(ItemStack s) { return !RequipWeaponItem.flagged(s); }

    // ------------------------------------------------------------ the cells as a container (server)
    /** The cells of the attachment as a vanilla container, so the menu's slots read and write them directly. */
    static final class CellContainer implements Container {
        private final Player owner;
        CellContainer(Player owner) { this.owner = owner; }
        private List<ItemStack> cells() { return data(owner).cells; }
        @Override public int getContainerSize() { return ArmouryRules.MAX_SLOTS; }
        @Override public boolean isEmpty() { for (ItemStack s : cells()) if (!s.isEmpty()) return false; return true; }
        @Override public ItemStack getItem(int slot) { return slot >= 0 && slot < cells().size() ? cells().get(slot) : ItemStack.EMPTY; }
        @Override public ItemStack removeItem(int slot, int amount) { return ContainerHelper.removeItem(cells(), slot, amount); }
        @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(cells(), slot); }
        @Override public void setItem(int slot, ItemStack stack) { if (slot >= 0 && slot < cells().size()) cells().set(slot, stack); }
        @Override public void setChanged() { }
        @Override public boolean stillValid(Player player) { return player == owner && owner.isAlive(); }
        /** Never clears the pocket: a mod that clears "the container" must not be able to empty it. */
        @Override public void clearContent() { }
    }

    // ------------------------------------------------------------ opening
    /** Opens the armoury window. The caller checks that the player may. */
    public static void open(ServerPlayer p) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new Menu(id, inv, new CellContainer(p)), Component.translatableWithFallback("nusmp.requip.armoury.title", "Requip Armoury")));
    }

    /** The server menu of a player (for the in-game self-test and for code that opens the window itself). */
    public static Menu serverMenu(ServerPlayer p, int containerId) { return new Menu(containerId, p.getInventory(), new CellContainer(p)); }

    // ------------------------------------------------------------ the menu
    public static final class Menu extends AbstractContainerMenu {
        public static final int TAB_POCKET = 0, TAB_LOADOUTS = 1, TAB_PARTIAL = 2;
        /** Button ids of {@link #clickMenuButton}. */
        public static final int BTN_TAB = 0, BTN_SAVE = 10, BTN_EQUIP = 20, BTN_CLEAR = 30, BTN_REFLEX = 40, BTN_FAMILY = 50, BTN_PRESET = 70, BTN_PARTIAL_ON = 71, BTN_APPLY = 72;
        public static final int WIDTH = 176, HEIGHT = 240, X0 = 8, Y0 = 32, INV_Y = 160;
        public static final int CELLS = ArmouryRules.MAX_SLOTS, PLAYER_FIRST = CELLS, PLAYER_END = CELLS + 36;

        private final Player player;
        private final DataSlot tab = DataSlot.standalone(), capacity = DataSlot.standalone(), saved = DataSlot.standalone(), active = DataSlot.standalone(), reflex = DataSlot.standalone(),
                mask = DataSlot.standalone(), partialOn = DataSlot.standalone(), selected = DataSlot.standalone(), blade = DataSlot.standalone();
        private final DataSlot[] pieces = new DataSlot[LoadoutRules.MAX_LOADOUTS];
        private IntSupplier capacityFn;

        public Menu(int id, Inventory inv, Container pocket) {
            super(MENU.get(), id);
            this.player = inv.player;
            addDataSlot(tab); addDataSlot(capacity); addDataSlot(saved); addDataSlot(active); addDataSlot(reflex); addDataSlot(mask); addDataSlot(partialOn); addDataSlot(selected); addDataSlot(blade);
            for (int i = 0; i < pieces.length; i++) { pieces[i] = DataSlot.standalone(); addDataSlot(pieces[i]); }
            capacity.set(ArmouryRules.MIN_CAPACITY);
            mask.set(PartialArmourMask.FULL);
            active.set(-1); reflex.set(-1); selected.set(-1); blade.set(-1);
            for (int r = 0; r < ArmouryRules.ROWS; r++)
                for (int c = 0; c < ArmouryRules.COLUMNS; c++) addSlot(new CellSlot(pocket, c + r * ArmouryRules.COLUMNS, X0 + c * 18, Y0 + r * 18));
            for (int r = 0; r < 3; r++)
                for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, X0 + c * 18, INV_Y + r * 18));
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, X0 + c * 18, INV_Y + 58));
            if (player instanceof ServerPlayer sp) capacityFn = () -> SkillLink.of(sp);
        }

        public int tab() { return tab.get(); }
        public int capacity() { return capacity.get(); }
        public boolean loadoutSaved(int n) { return (saved.get() & (1 << n)) != 0; }
        public int loadoutPieces(int n) { return pieces[n].get(); }
        public int activeLoadout() { return active.get(); }
        public int reflexLoadout() { return reflex.get(); }
        public int mask() { return PartialArmourMask.normalize(mask.get()); }
        public boolean partialOn() { return partialOn.get() != 0; }
        public int selectedCell() { return selected.get(); }
        public int bladeCell() { return blade.get(); }

        /** A cell of the armoury: takes new stacks only below the capacity (and never an armour's requipped weapon), always gives back, and is hidden on the other tabs. */
        final class CellSlot extends Slot {
            CellSlot(Container c, int index, int x, int y) { super(c, index, x, y); }
            @Override public boolean mayPlace(ItemStack stack) { return tab.get() == TAB_POCKET && getContainerSlot() < capacity.get() && mayStore(stack); }
            @Override public boolean mayPickup(Player p) { return tab.get() == TAB_POCKET; }
            @Override public boolean isActive() { return tab.get() == TAB_POCKET; }
        }

        /** Server: refreshes the mirrored numbers, then sends what changed. */
        @Override
        public void broadcastChanges() {
            if (player instanceof ServerPlayer && capacityFn != null) {
                Data d = data(player);
                capacity.set(capacityFn.getAsInt());
                int bits = 0;
                for (int i = 0; i < LoadoutRules.MAX_LOADOUTS; i++) { if (d.loadouts.get(i).saved()) bits |= 1 << i; pieces[i].set(d.loadouts.get(i).pieces()); }
                saved.set(bits);
                active.set(d.active); reflex.set(d.reflex); mask.set(d.mask); partialOn.set(d.partialOn ? 1 : 0); selected.set(d.selected); blade.set(d.blade);
            }
            super.broadcastChanges();
        }

        @Override
        public boolean clickMenuButton(Player p, int id) {
            if (!(p instanceof ServerPlayer sp) || !sp.isAlive()) return false;
            Data d = data(sp);
            if (id >= BTN_TAB && id <= BTN_TAB + 2) { tab.set(id - BTN_TAB); return true; }
            if (id >= BTN_SAVE && id < BTN_SAVE + LoadoutRules.MAX_LOADOUTS) {
                if (!SkillLink.mayUse(sp, "loadouts")) return false;
                int n = RequipLoadouts.save(sp, id - BTN_SAVE);
                sp.displayClientMessage(Component.translatableWithFallback(n > 0 ? "nusmp.requip.loadout.saved" : "nusmp.requip.loadout.nothing", n > 0 ? "Loadout %s saved: %s pieces." : "Nothing is worn to save.", id - BTN_SAVE + 1, n), true);
                return true;
            }
            if (id >= BTN_EQUIP && id < BTN_EQUIP + LoadoutRules.MAX_LOADOUTS) { SkillLink.equipLoadout(sp, id - BTN_EQUIP); return true; }
            if (id >= BTN_CLEAR && id < BTN_CLEAR + LoadoutRules.MAX_LOADOUTS) { if (!SkillLink.mayUse(sp, "loadouts")) return false; RequipLoadouts.clear(sp, id - BTN_CLEAR); return true; }
            if (id >= BTN_REFLEX && id < BTN_REFLEX + LoadoutRules.MAX_LOADOUTS) {
                if (!SkillLink.mayUse(sp, "loadouts")) return false;
                int n = id - BTN_REFLEX;
                d.reflex = d.reflex == n ? -1 : n;
                return true;
            }
            if (id >= BTN_FAMILY && id <= BTN_APPLY && !SkillLink.mayUse(sp, "partial")) return false;
            if (id >= BTN_FAMILY && id < BTN_FAMILY + PartialArmourMask.FAMILIES) { d.mask = PartialArmourMask.toggle(d.mask, id - BTN_FAMILY); d.partialOn = true; return true; }
            if (id == BTN_PRESET) {
                int now = PartialArmourMask.presetIndexOf(d.mask);
                d.mask = PartialArmourMask.preset(now + 1).mask();
                d.partialOn = d.mask != PartialArmourMask.FULL;
                return true;
            }
            if (id == BTN_PARTIAL_ON) { d.partialOn = !d.partialOn; return true; }
            if (id == BTN_APPLY) { SkillLink.applyPartial(sp); return true; }
            return false;
        }

        /** Shift-click: a stack in the inventory goes into the armoury (below the capacity), a stack in the armoury goes to the inventory. Only on the Pocket tab. */
        @Override
        public ItemStack quickMoveStack(Player p, int index) {
            if (tab.get() != TAB_POCKET) return ItemStack.EMPTY;
            Slot from = slots.get(index);
            if (!from.hasItem()) return ItemStack.EMPTY;
            ItemStack stack = from.getItem(), copy = stack.copy();
            if (index < CELLS) {
                if (!moveItemStackTo(stack, PLAYER_FIRST, PLAYER_END, true)) return ItemStack.EMPTY;
            } else {
                if (!mayStore(stack) || !moveItemStackTo(stack, 0, Math.min(CELLS, capacity.get()), false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) from.setByPlayer(ItemStack.EMPTY); else from.setChanged();
            return copy;
        }

        @Override public boolean stillValid(Player p) { return p.isAlive(); }
    }

    // ------------------------------------------------------------ network: the key that opens the window
    /** Client asks to open its armoury (the key). */
    public record OpenPayload() implements CustomPacketPayload {
        public static final OpenPayload INSTANCE = new OpenPayload();
        public static final Type<OpenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "requip_armoury_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToServer(OpenPayload.TYPE, OpenPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) SkillLink.openFor(sp);
        }));
    }

    /**
     * The hooks into the skill (which lives in the skill package and is not visible from here by design): set once by {@code skill.ErzaRequipMagic}. Until it is set (the skill never loaded) the
     * armoury is closed.
     */
    public static final class SkillLink {
        private SkillLink() {}

        public interface Hooks {
            int capacity(ServerPlayer p);
            boolean mayOpen(ServerPlayer p);
            /** True when the player knows the skill and the mode ({@code loadouts}, {@code partial}, ...) is open to them. */
            boolean mayUse(ServerPlayer p, String modeId);
            void equipLoadout(ServerPlayer p, int n);
            void applyPartial(ServerPlayer p);
        }

        private static volatile Hooks hooks;

        public static void set(Hooks h) { hooks = h; }
        static int of(ServerPlayer p) { Hooks h = hooks; return h == null ? ArmouryRules.MIN_CAPACITY : h.capacity(p); }
        static boolean mayUse(ServerPlayer p, String mode) { Hooks h = hooks; return h != null && h.mayUse(p, mode); }
        static void equipLoadout(ServerPlayer p, int n) { Hooks h = hooks; if (h != null) h.equipLoadout(p, n); }
        static void applyPartial(ServerPlayer p) { Hooks h = hooks; if (h != null) h.applyPartial(p); }
        static void openFor(ServerPlayer p) { Hooks h = hooks; if (h != null && h.mayOpen(p)) open(p); }
    }
}
