package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.RelicDef;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Wave 17: the pocket dimensions behind the pouches (Bag of Realistic Capacity, Shared Pouches of Transfer, Bags of Transferrence). A vault is a few
 * chest-sized boxes saved with the world under {@code nusmp_w17_vaults}; the relic stack carries only the vault's id ({@code w17_vault} in its data), so
 * the contents travel with the item, stay in the world if the item is destroyed, and can be opened by a linked companion who does not hold the item.
 * <p>
 * Each box is one live container shared by everyone who has it open, so two people looking into the same pouch see each other's changes at once.
 * A box refuses the storage relics themselves (no pouch inside a pouch), so nothing can ever nest.
 */
public final class Wave17Vault extends SavedData {
    public static final String NAME = "nusmp_w17_vaults", KEY = "w17_vault";
    public static final String KIND_POUCH = "pouch", KIND_SHARED = "shared", KIND_TRANSFER = "transfer";

    /** The relic ids that hold storage (a vault never takes one of these, nor the Wave 6 garments with pockets). */
    private static final Set<String> STORAGE = Set.of("bag_of_realistic_capacity", "shared_pouches_of_transfer", "bags_of_transferrence",
            Wave6Tune.ID_NEKET, Wave6Tune.ID_COAT);

    private final Map<UUID, Vault> vaults = new HashMap<>();

    // ================================================================ one vault
    public final class Vault {
        public final UUID id;
        public final String kind;
        public final Box[] boxes;
        public UUID owner, partner;
        public String ownerName = "", partnerName = "";

        Vault(UUID id, String kind, int sides, int rows) {
            this.id = id;
            this.kind = kind;
            this.boxes = new Box[sides];
            for (int i = 0; i < sides; i++) boxes[i] = new Box(this, rows);
        }

        /** May this player look into side {@code side}? Side 0 of a pair of bags is the owner's near bag; the rest are open to the owner and the companion. */
        public boolean mayOpen(Player p, int side) {
            if (p == null || !p.isAlive()) return false;
            boolean isOwner = p.getUUID().equals(owner), isPartner = p.getUUID().equals(partner);
            if (KIND_TRANSFER.equals(kind) && side == 0) return isOwner && carries(p, id);
            return (isOwner && carries(p, id)) || isPartner;
        }

        public void setOwner(Player p) {
            if (p.getUUID().equals(owner)) return;
            owner = p.getUUID();
            ownerName = p.getGameProfile().getName();
            if (p.getUUID().equals(partner)) { partner = null; partnerName = ""; }
            setDirty();
        }

        public void setPartner(Player p) {
            partner = p == null ? null : p.getUUID();
            partnerName = p == null ? "" : p.getGameProfile().getName();
            setDirty();
        }

        /** Opens side {@code side} for the player (a chest menu over the live box). */
        public void open(ServerPlayer p, int side, String title) {
            if (side < 0 || side >= boxes.length || !mayOpen(p, side)) return;
            if (p.containerMenu != p.inventoryMenu) p.closeContainer();
            Box box = boxes[side];
            int rows = box.getContainerSize() / 9;
            p.openMenu(new SimpleMenuProvider((cid, inv, pl) -> new ChestMenu(menuType(rows), cid, inv, box, rows), Component.literal(title)));
        }

        /** Trades the contents of side 0 and side 1 (the Bags of Transferrence). */
        public void swapSides() {
            if (boxes.length < 2) return;
            int n = Math.min(boxes[0].getContainerSize(), boxes[1].getContainerSize());
            for (int i = 0; i < n; i++) {
                ItemStack a = boxes[0].getItem(i), b = boxes[1].getItem(i);
                boxes[0].setItem(i, b);
                boxes[1].setItem(i, a);
            }
        }
    }

    // ================================================================ one box
    public final class Box extends SimpleContainer {
        private final Vault vault;

        Box(Vault vault, int rows) {
            super(Wave17Rules.slots(rows));
            this.vault = vault;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack s) { return !storage(s) && !uniqueRelic(s); }

        @Override
        public boolean stillValid(Player p) {
            for (int i = 0; i < vault.boxes.length; i++) if (vault.boxes[i] == this) return vault.mayOpen(p, i);
            return false;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            Wave17Vault.this.setDirty();
        }
    }

    // ================================================================ rules
    /** A unique relic never goes into a pouch: the Order's census cannot see inside one (the God of Order counts what players carry and wear). */
    public static boolean uniqueRelic(ItemStack s) {
        RelicDef d = s == null || s.isEmpty() ? null : Relics.defOf(s);
        return d != null && d.isUnique();
    }

    /** A stack that holds storage of its own: refused by every box, so pockets cannot nest. */
    public static boolean storage(ItemStack s) { return storage(s, 0); }

    /**
     * The Wave 18 bags count as storage too (a pouch never rides in a bag and a bag never in a pouch), and so does a shulker box or bundle that
     * carries any of it (otherwise a pouch could be smuggled in inside a box). Depth-limited: a box nested deeper than four is refused outright.
     */
    private static boolean storage(ItemStack s, int depth) {
        if (s == null || s.isEmpty()) return false;
        RelicDef d = Relics.defOf(s);
        if (d != null && (STORAGE.contains(d.id()) || Wave18Pure.Bag.isBag(d.id()))) return true;
        if (!vaultId(s).isEmpty() && vaultIdOf(s) != null) return true;
        ItemContainerContents cc = s.get(DataComponents.CONTAINER);
        BundleContents bc = s.get(DataComponents.BUNDLE_CONTENTS);
        if (cc == null && bc == null) return false;
        if (depth >= 4) return true;
        if (cc != null) for (int i = 0; i < cc.getSlots(); i++) if (storage(cc.getStackInSlot(i), depth + 1)) return true;
        if (bc != null) for (ItemStack in : bc.items()) if (storage(in, depth + 1)) return true;
        return false;
    }

    private static MenuType<ChestMenu> menuType(int rows) {
        return switch (Math.max(1, Math.min(6, rows))) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    // ---------------------------------------------------------------- the id on a stack
    private static String vaultId(ItemStack s) {
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        return cd == null ? "" : cd.copyTag().getString(KEY);
    }

    /** The vault id written on this stack, or null. */
    public static UUID vaultIdOf(ItemStack s) {
        String id = vaultId(s);
        if (id.isEmpty()) return null;
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException bad) {
            return null;
        }
    }

    /** Does the player carry (inventory or a worn slot) a stack that points at this vault? */
    public static boolean carries(Player p, UUID vault) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && vault.equals(vaultIdOf(s))) return true;
        }
        for (Relics.Worn w : Relics.wornList(p)) if (vault.equals(vaultIdOf(w.stack()))) return true;   // the cached look: this runs every tick while a menu is open
        return false;
    }

    // ================================================================ the saved registry
    public static Wave17Vault get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Wave17Vault::new, Wave17Vault::load, null), NAME);
    }

    public Vault byId(UUID id) { return vaults.get(id); }

    /** The vault of this relic stack, made (and its id written onto the stack) the first time. {@code stack} must be the live stack. */
    public Vault of(ItemStack stack, ServerPlayer owner, String kind, int sides, int rows) {
        UUID id = vaultIdOf(stack);
        Vault v = id == null ? null : vaults.get(id);
        if (v == null) {
            UUID fresh = id != null ? id : UUID.randomUUID();
            v = new Vault(fresh, kind, sides, rows);
            vaults.put(fresh, v);
            if (id == null) CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putString(KEY, fresh.toString()));
            setDirty();
        }
        v.setOwner(owner);
        return v;
    }

    /** Every vault in which this player is the companion (for the /pouch command). */
    public java.util.List<Vault> companionOf(UUID player) {
        java.util.List<Vault> out = new java.util.ArrayList<>();
        for (Vault v : vaults.values()) if (player.equals(v.partner)) out.add(v);
        return out;
    }

    // ---------------------------------------------------------------- saving
    private static Wave17Vault load(CompoundTag tag, HolderLookup.Provider provider) {
        Wave17Vault root = new Wave17Vault();
        ListTag list = tag.getList("Vaults", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            if (!t.hasUUID("Id")) continue;
            ListTag sides = t.getList("Sides", 10);
            int rows = Math.max(1, Math.min(6, t.getInt("Rows")));
            Vault v = root.new Vault(t.getUUID("Id"), t.getString("Kind"), Math.max(1, Math.min(2, sides.size())), rows);
            if (t.hasUUID("Owner")) v.owner = t.getUUID("Owner");
            if (t.hasUUID("Partner")) v.partner = t.getUUID("Partner");
            v.ownerName = t.getString("OwnerName");
            v.partnerName = t.getString("PartnerName");
            for (int s = 0; s < v.boxes.length && s < sides.size(); s++) {
                ListTag items = sides.getCompound(s).getList("Items", 10);
                for (int k = 0; k < items.size(); k++) {
                    CompoundTag e = items.getCompound(k);
                    int slot = e.getInt("Slot");
                    if (slot < 0 || slot >= v.boxes[s].getContainerSize() || !e.contains("Item")) continue;
                    ItemStack st = ItemStack.parse(provider, e.get("Item")).orElse(ItemStack.EMPTY);
                    if (!st.isEmpty()) v.boxes[s].setItem(slot, st);
                }
            }
            root.vaults.put(v.id, v);
        }
        return root;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Vault v : vaults.values()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", v.id);
            t.putString("Kind", v.kind);
            t.putInt("Rows", v.boxes[0].getContainerSize() / 9);
            if (v.owner != null) t.putUUID("Owner", v.owner);
            if (v.partner != null) t.putUUID("Partner", v.partner);
            t.putString("OwnerName", v.ownerName);
            t.putString("PartnerName", v.partnerName);
            ListTag sides = new ListTag();
            for (Box b : v.boxes) {
                ListTag items = new ListTag();
                for (int i = 0; i < b.getContainerSize(); i++) {
                    ItemStack s = b.getItem(i);
                    if (s.isEmpty()) continue;
                    CompoundTag e = new CompoundTag();
                    e.putInt("Slot", i);
                    e.put("Item", s.save(provider));
                    items.add(e);
                }
                CompoundTag side = new CompoundTag();
                side.put("Items", items);
                sides.add(side);
            }
            t.put("Sides", sides);
            list.add(t);
        }
        tag.put("Vaults", list);
        return tag;
    }
}
