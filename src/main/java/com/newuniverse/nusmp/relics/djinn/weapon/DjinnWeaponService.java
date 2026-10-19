package com.newuniverse.nusmp.relics.djinn.weapon;

import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.DjinnPhaseRules;
import com.newuniverse.nusmp.relics.djinn.DjinnState;
import com.newuniverse.nusmp.relics.djinn.dark.DarkDjinn;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The lifecycle of the Djinn weapon on the server: {@link #apply} puts it in the bearer's hand for a Half or Full Equip (the item it displaces is
 * kept safe by {@link WeaponLoan}, whose state is saved in the bearer's Djinn record), {@link #release} takes it away and gives everything back. The
 * engine calls them on the phase changes; {@link DjinnWeaponEvents} calls {@link #apply} again every few ticks, which is the whole enforcement: a
 * weapon moved out of its slot, dropped, put in a container or the cursor, swapped to the offhand or lost is made whole again at once, and a
 * stale one (the other Djinn, the other look) is replaced in place.
 *
 * <p>A creative or spectator player gets no item (the old render-only look stays, and nothing is swapped). Admin weapons (command, creative tab)
 * are outside the loan: they are never wiped, never touch the stash and can be dropped.
 */
public final class DjinnWeaponService {
    private DjinnWeaponService() {}

    /** The 36 main slots, the offhand, four armour pieces and the cursor: the places the loan reads. */
    static final int SLOTS = 42;

    /** The loan's view of an item. {@code d} and {@code serial} are the owner's Djinn and vessel serial (null for a household member, who has no vessel): their vessel is overlaid, never displaced. */
    private static WeaponLoan.Ops<ItemStack> ops(Djinn d, java.util.UUID serial) {
        return new WeaponLoan.Ops<>() {
            @Override public ItemStack empty() { return ItemStack.EMPTY; }
            @Override public boolean isEmpty(ItemStack t) { return t.isEmpty(); }
            @Override public boolean isWeapon(ItemStack t) { return DjinnTemp.isLoanWeapon(t); }
            @Override public boolean same(ItemStack a, ItemStack b) { return ItemStack.matches(a, b); }
            @Override public boolean overlays(ItemStack t) { return VesselOverlay.isVesselOf(t, d, serial); }
        };
    }

    /** The player's inventory as the loan's flat slots. */
    private static final class InvSlots implements WeaponLoan.Slots<ItemStack> {
        private final ServerPlayer p;
        private final Inventory inv;

        InvSlots(ServerPlayer p) {
            this.p = p;
            this.inv = p.getInventory();
        }

        @Override public int size() { return SLOTS; }

        @Override
        public ItemStack get(int i) {
            if (i < 36) return inv.items.get(i);
            if (i == 36) return inv.offhand.get(0);
            if (i < 41) return inv.armor.get(i - 37);
            return p.containerMenu.getCarried();
        }

        @Override
        public void set(int i, ItemStack t) {
            if (i < 36) inv.items.set(i, t);
            else if (i == 36) inv.offhand.set(0, t);
            else if (i < 41) inv.armor.set(i - 37, t);
            else p.containerMenu.setCarried(t);
        }
    }

    // ================================================================ the loan record
    private static WeaponLoan<ItemStack> load(ServerPlayer p, DjinnState s) {
        WeaponLoan<ItemStack> loan = new WeaponLoan<>(ops(s.djinn(), s.serial()));
        if (!s.hasLoan()) return loan;
        CompoundTag c = s.loan();
        List<ItemStack> stash = new ArrayList<>();
        ListTag l = c.getList("stash", Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size(); i++) {
            ItemStack st = ItemStack.parseOptional(p.registryAccess(), l.getCompound(i));
            if (!st.isEmpty()) stash.add(st);
        }
        int moved = c.contains("moved") ? c.getInt("moved") : -1;
        ItemStack movedItem = moved >= 0 ? ItemStack.parseOptional(p.registryAccess(), c.getCompound("movedItem")) : ItemStack.EMPTY;
        ItemStack overlaid = VesselOverlay.readFrom(c, p.registryAccess());                           // the vessel the weapon sits over (it is in no slot)
        loan.load(c.contains("home") ? c.getInt("home") : -1, moved, movedItem.isEmpty() ? null : movedItem, stash, overlaid.isEmpty() ? null : overlaid);
        return loan;
    }

    private static void save(ServerPlayer p, DjinnState s, WeaponLoan<ItemStack> loan) {
        if (!loan.active() && loan.stash().isEmpty() && loan.overlaid() == null) {
            s.clearLoan();
            return;
        }
        CompoundTag c = new CompoundTag();
        c.putInt("home", loan.home());
        if (loan.moved() >= 0 && loan.movedItem() != null) {
            c.putInt("moved", loan.moved());
            c.put("movedItem", loan.movedItem().saveOptional(p.registryAccess()));
        }
        VesselOverlay.write(c, loan.overlaid(), p.registryAccess());
        ListTag l = new ListTag();
        for (ItemStack st : loan.stash()) l.add(st.saveOptional(p.registryAccess()));
        c.put("stash", l);
        s.clearLoan();
        s.loan().merge(c);
    }

    // ================================================================ summoning
    /** Is the weapon on loan to this player now? */
    public static boolean onLoan(ServerPlayer p) { return DjinnState.of(p).hasLoan(); }

    /**
     * Makes the weapon whole for a Half or Full Equip (summons it the first time), or, when the player is not in one (or cannot carry it), releases
     * it. Cheap enough to run every few ticks: 42 slot checks.
     */
    public static void apply(ServerPlayer p) {
        DjinnState s = DjinnState.of(p);
        Djinn d = s.active();
        if (d == null || s.phase() < DjinnPhaseRules.HALF || !p.isAlive() || p.isCreative() || p.isSpectator()) {
            if (s.hasLoan() || strayWeapon(p)) release(p);
            return;
        }
        boolean dark = DarkDjinn.active(s);
        Item want = DjinnWeaponItems.item(d, dark);
        WeaponLoan<ItemStack> loan = load(p, s);
        boolean was = loan.active();
        Supplier<ItemStack> factory = () -> make(p, d, dark, false);
        Predicate<ItemStack> current = st -> st.getItem() == want && DjinnTemp.isFor(st, p.getUUID(), d);
        InvSlots slots = new InvSlots(p);
        if (was) loan.ensure(slots, factory, current);
        else loan.summon(slots, p.getInventory().selected, factory, current);
        save(p, s, loan);
        if (!was) flourish(p, d, dark);
    }

    private static boolean strayWeapon(ServerPlayer p) {
        InvSlots slots = new InvSlots(p);
        for (int i = 0; i < SLOTS; i++) if (DjinnTemp.isLoanWeapon(slots.get(i))) return true;
        return false;
    }

    /** Takes the weapon away and gives the displaced item back (what finds no room is dropped at the feet, protected from fire and despawn). */
    public static void release(ServerPlayer p) {
        DjinnState s = DjinnState.of(p);
        WeaponLoan<ItemStack> loan = load(p, s);
        boolean had = loan.active() || !loan.stash().isEmpty() || loan.overlaid() != null;
        List<ItemStack> over = loan.release(new InvSlots(p));
        s.clearLoan();
        DjinnWeaponActs.endEquip(p.getUUID());
        for (ItemStack o : over) dropProtected(p, o);
        if (had) {
            p.getInventory().setChanged();
            p.displayClientMessage(Component.translatable("djinn_weapon.msg.returned"), true);
        }
    }

    /** Removes every Djinn weapon from the player, admin ones too (the command's clear), after ending the loan. */
    public static int clear(ServerPlayer p) {
        release(p);
        int n = 0;
        InvSlots slots = new InvSlots(p);
        for (int i = 0; i < SLOTS; i++) {
            ItemStack st = slots.get(i);
            if (DjinnWeaponItems.isDjinnWeapon(st)) {
                slots.set(i, ItemStack.EMPTY);
                n++;
            }
        }
        return n;
    }

    /** A fresh weapon for the player, engraved. {@code admin}: outside the loan. */
    public static ItemStack make(ServerPlayer p, Djinn d, boolean dark, boolean admin) {
        ItemStack st = new ItemStack(DjinnWeaponItems.item(d, dark));
        DjinnTemp.set(st, p.getUUID(), d, 0L, admin);
        DjinnWeaponEngrave.engrave(st, d, dark, p.registryAccess());
        return st;
    }

    /** The command's give: an admin weapon of this Djinn into the inventory (or at the feet if full). */
    public static void giveAdmin(ServerPlayer p, Djinn d, boolean dark) { p.getInventory().placeItemBackInInventory(make(p, d, dark, true)); }

    // ================================================================ the item's own checks
    /**
     * Called by the item once a tick while it sits in a player's inventory: a bare weapon is adopted as an admin one (engraved, and its owner the
     * holder); a temporary one dissolves unless its owner holds it, in the Equip of the Djinn it was made for, with the loan running.
     */
    static void vet(ServerPlayer p, ItemStack stack, DjinnWeaponItem item) {
        DjinnTemp.Data t = DjinnTemp.get(stack);
        if (t == null) {
            DjinnTemp.set(stack, p.getUUID(), item.djinn(), 0L, true);
            DjinnWeaponEngrave.engrave(stack, item.djinn(), item.dark(), p.registryAccess());
            return;
        }
        if (t.admin()) return;
        DjinnState s = DjinnState.of(p);
        boolean ok = p.getUUID().equals(t.owner()) && s.active() == item.djinn() && s.phase() >= DjinnPhaseRules.HALF && s.hasLoan() && !p.isCreative() && !p.isSpectator();
        if (ok && t.expires() > 0 && p.level().getGameTime() > t.expires()) ok = false;
        if (!ok) stack.setCount(0);
    }

    // ================================================================ the effects
    private static void flourish(ServerPlayer p, Djinn d, boolean dark) {
        ServerLevel l = p.serverLevel();
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 1.2, p.getZ(), 10, 0.4, 0.5, 0.4, 0.04);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.Fx.align(dark ? 0x7A2CC8 : d.glow()), p.getX(), p.getY() + 1.2, p.getZ(), 14, 0.4, 0.5, 0.4, 0.02);
        l.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 1.6f);
        p.displayClientMessage(Component.translatable("djinn_weapon.msg.summoned", Component.translatable("item.nusmp." + DjinnWeaponRules.itemPath(d, dark))), true);
    }

    static void dropProtected(ServerPlayer p, ItemStack st) {
        if (st.isEmpty()) return;
        ItemEntity e = new ItemEntity(p.level(), p.getX(), p.getY() + 0.3, p.getZ(), st);
        e.setPickUpDelay(20);
        e.setInvulnerable(true);
        e.setUnlimitedLifetime();
        p.level().addFreshEntity(e);
    }
}
