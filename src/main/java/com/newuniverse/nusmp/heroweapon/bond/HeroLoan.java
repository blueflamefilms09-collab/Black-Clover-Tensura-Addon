package com.newuniverse.nusmp.heroweapon.bond;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.heroweapon.HeroColors;
import com.newuniverse.nusmp.heroweapon.HeroComponents;
import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.HeroWeaponItem;
import com.newuniverse.nusmp.heroweapon.bond.HeroLoanPolicy.Verdict;
import com.newuniverse.nusmp.heroweapon.curse.HeroCurse;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import com.newuniverse.nusmp.heroweapon.skill.HeroUse;
import com.newuniverse.nusmp.item.WeaponEngravings;
import com.newuniverse.nusmp.quests.NusmpEvents;
import com.newuniverse.nusmp.relics.djinn.weapon.WeaponLoan;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the owner wants a hero weapon "permanently bound to the user's body", but a Minecraft item can be dropped, put in a chest, handed over, burnt and lost.
 * The answer is the same as the Djinn weapons' (which already do this and are tested): the bond is the truth and the item in the hand is only a PROJECTION of
 * it, put in the selected hotbar slot on summon (the item that stood there is displaced, never deleted, and comes back on dismiss), made whole again every four
 * ticks, and unable to exist anywhere else: a projection in another inventory, in a container, on the cursor or after the bond ended dissolves; a bare stack
 * from the creative tab is adopted by its first holder or dissolves; nothing is ever dropped into the world. The state machine of slots and displaced items is
 * the Djinn weapons' own {@link WeaponLoan} (pure, already tested with random events), used here as {@code WeaponLoan<ItemStack>}; this class is the game
 * glue: the saved record in {@code bond.extra("loan")}, the checks, the effects and the hooks of the weapon's logic. Creative and spectator players are outside
 * the whole machine: the stack is a plain item for them. Server thread only.
 */
public final class HeroLoan {
    private HeroLoan() {}

    /** What a summon or dismiss came to. */
    public enum Outcome { OK, NO_BOND, ALREADY, NOT_OUT, EXEMPT, LOCKED, DISABLED, DEAD }

    /** The 36 main slots, the offhand, four armour pieces and the cursor: the places the loan reads. */
    static final int SLOTS = 42;

    private static final String KEY = "loan";

    private static final WeaponLoan.Ops<ItemStack> OPS = new WeaponLoan.Ops<>() {
        @Override public ItemStack empty() { return ItemStack.EMPTY; }

        @Override public boolean isEmpty(ItemStack t) { return t.isEmpty(); }

        @Override public boolean isWeapon(ItemStack t) { return isProjection(t); }

        @Override public boolean same(ItemStack a, ItemStack b) { return ItemStack.matches(a, b); }
    };

    /** Players whose weapon must be made whole at the next tick instead of waiting for the four-tick beat (a toss was cancelled, a slot was emptied). */
    private static final Set<UUID> DIRTY = new HashSet<>();
    /** Bare stacks found in a survival inventory that are to be adopted at the next tick (inventoryTick must not rebuild the inventory it is iterating). */
    private static final Map<UUID, HeroKind> ADOPT = new HashMap<>();

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

    // ================================================================ what is a projection
    /** Is this stack a projection: a hero weapon that carries an owner? (A bare stack from the creative tab or a command carries none.) */
    public static boolean isProjection(ItemStack s) {
        return s != null && !s.isEmpty() && s.getItem() instanceof HeroWeaponItem && !HeroComponents.owner(s).isEmpty();
    }

    /** Is this stack the projection the loan wants for this player and weapon? */
    static boolean current(ServerPlayer p, HeroKind kind, ItemStack st) {
        return HeroItems.kindOf(st) == kind && p.getUUID().toString().equals(HeroComponents.owner(st));
    }

    /** Creative and spectator players are outside the machine. */
    static boolean exempt(ServerPlayer p) { return p.isCreative() || p.isSpectator(); }

    /** The live projection of this player's weapon, or an empty stack. */
    public static ItemStack stackOf(ServerPlayer p) {
        HeroKind kind = HeroBonds.kindOf(p);
        if (kind == null) return ItemStack.EMPTY;
        InvSlots s = new InvSlots(p);
        for (int i = 0; i < SLOTS; i++) {
            ItemStack st = s.get(i);
            if (current(p, kind, st)) return st;
        }
        return ItemStack.EMPTY;
    }

    /** A fresh projection of the bond's weapon for the player: owned, in the active form's look, engraved. */
    static ItemStack make(ServerPlayer p, HeroBond b) {
        ItemStack st = HeroItems.stack(b.kind());
        st.set(HeroComponents.OWNER.get(), p.getUUID().toString());
        try {
            HeroForm f = HeroForms.active(b);
            if (f != null && HeroForm.LOOK_CURSE.equals(f.look())) st.set(HeroComponents.LOOK.get(), HeroForm.LOOK_CURSE);
        } catch (RuntimeException brokenForm) {
            // a form that cannot be read leaves the base look; the forms system corrects it on its refresh
        }
        try {
            WeaponEngravings.engrave(st, p.registryAccess());
        } catch (RuntimeException noEngravings) {
            // the engraving table or Tensura's enchantments are not there: the weapon is still a weapon
        }
        return st;
    }

    // ================================================================ the saved record
    private static WeaponLoan<ItemStack> load(ServerPlayer p, HeroBond b) {
        WeaponLoan<ItemStack> loan = new WeaponLoan<>(OPS);
        if (!b.hasExtra(KEY)) return loan;
        CompoundTag c = b.extra(KEY);
        if (c.isEmpty()) return loan;
        List<ItemStack> stash = new ArrayList<>();
        ListTag l = c.getList("stash", Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size(); i++) {
            ItemStack st = ItemStack.parseOptional(p.registryAccess(), l.getCompound(i));
            if (!st.isEmpty()) stash.add(st);
        }
        int moved = c.contains("moved") ? c.getInt("moved") : -1;
        ItemStack movedItem = moved >= 0 ? ItemStack.parseOptional(p.registryAccess(), c.getCompound("movedItem")) : ItemStack.EMPTY;
        loan.load(c.contains("home") ? c.getInt("home") : -1, moved, movedItem.isEmpty() ? null : movedItem, stash);
        return loan;
    }

    private static void save(ServerPlayer p, HeroBond b, WeaponLoan<ItemStack> loan) {
        if (!loan.active() && loan.stash().isEmpty()) {
            clearRecord(b);
            return;
        }
        CompoundTag c = b.extra(KEY);
        for (String k : new ArrayList<>(c.getAllKeys())) c.remove(k);
        c.putInt("home", loan.home());
        if (loan.moved() >= 0 && loan.movedItem() != null) {
            c.putInt("moved", loan.moved());
            c.put("movedItem", loan.movedItem().saveOptional(p.registryAccess()));
        }
        ListTag l = new ListTag();
        for (ItemStack st : loan.stash()) l.add(st.saveOptional(p.registryAccess()));
        c.put("stash", l);
    }

    private static void clearRecord(HeroBond b) {
        if (!b.hasExtra(KEY)) return;
        CompoundTag c = b.extra(KEY);
        for (String k : new ArrayList<>(c.getAllKeys())) c.remove(k);
    }

    // ================================================================ summon
    /** Summons the bonded weapon into the selected hotbar slot. {@code explain}: say why on the action bar when it cannot be done. */
    public static Outcome summon(ServerPlayer p, boolean explain) {
        Outcome o = trySummon(p);
        if (explain) explain(p, o);
        return o;
    }

    private static Outcome trySummon(ServerPlayer p) {
        if (!HeroConfig.enabled()) return Outcome.DISABLED;
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound()) return Outcome.NO_BOND;
        if (!p.isAlive()) return Outcome.DEAD;
        if (exempt(p)) return Outcome.EXEMPT;
        if (b.summoned()) return Outcome.ALREADY;
        b.setSummoned(true);
        try {
            WeaponLoan<ItemStack> loan = load(p, b);
            loan.summon(new InvSlots(p), p.getInventory().selected, () -> make(p, b), st -> current(p, b.kind(), st));
            save(p, b, loan);
        } catch (RuntimeException broken) {
            b.setSummoned(false);
            throw broken;
        }
        p.getInventory().setChanged();
        afterSummon(p, b);
        return Outcome.OK;
    }

    private static void afterSummon(ServerPlayer p, HeroBond b) {
        HeroKind kind = b.kind();
        SafeCast.tick("hero.summon.forms", () -> HeroForms.refresh(p));
        SafeCast.tick("hero.summon.logic", () -> {
            HeroCtx c = HeroCtx.of(p);
            if (c != null) HeroLogics.of(kind).onSummon(c);
        });
        ServerLevel level = p.serverLevel();
        SafeCast.tick("hero.summon.fx", () -> {
            VfxSpawn.sendFollowing(level, VfxShape.MANA_CHARGE, p, p.position().add(0, 1.0, 0), HeroColors.argb(kind), 14, 1.0f);
            level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 1.5f);
        });
        if (HeroConfig.showMessages())
            p.displayClientMessage(Component.translatable("hero.msg.summoned", Component.translatable("item.nusmp." + kind.itemId())).withColor(HeroColors.rgb(kind)), true);
        NusmpEvents.fire(p, "hero.summoned", kind.itemId());
        HeroSync.sendNow(p);
    }

    // ================================================================ dismiss
    /** Puts the weapon away and gives back what it displaced; refused while the curse holds the wielder. */
    public static Outcome dismiss(ServerPlayer p, boolean explain) {
        Outcome o = tryDismiss(p);
        if (explain) explain(p, o);
        return o;
    }

    private static Outcome tryDismiss(ServerPlayer p) {
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound()) return Outcome.NO_BOND;
        if (!b.summoned()) return Outcome.NOT_OUT;
        boolean locked = false;
        try {
            locked = HeroCurse.locked(p);
        } catch (RuntimeException brokenCurse) {
            // the curse system failing must not trap the wielder with the weapon in the hand
        }
        if (locked) return Outcome.LOCKED;
        HeroKind kind = b.kind();
        SafeCast.tick("hero.dismiss.logic", () -> {
            HeroCtx c = HeroCtx.of(p);
            if (c != null) HeroLogics.of(kind).onDismiss(c);
        });
        end(p, false);
        SafeCast.tick("hero.dismiss.forms", () -> HeroForms.refresh(p));
        ServerLevel level = p.serverLevel();
        SafeCast.tick("hero.dismiss.fx", () -> {
            VfxSpawn.send(level, VfxShape.WIND_RING, p.position().add(0, 0.2, 0), p.position().add(0, 1.0, 0), HeroColors.argb(kind), 10, 0.8f);
            level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.8f);
        });
        if (HeroConfig.showMessages())
            p.displayClientMessage(Component.translatable("hero.msg.dismissed", Component.translatable("item.nusmp." + kind.itemId())).withStyle(ChatFormatting.GRAY), true);
        HeroSync.sendNow(p);
        return Outcome.OK;
    }

    private static void explain(ServerPlayer p, Outcome o) {
        if (!HeroConfig.showMessages()) return;
        String key = switch (o) {
            case NO_BOND -> "hero.msg.unbound";
            case EXEMPT -> "hero.msg.creative";
            case LOCKED -> "hero.msg.locked";
            case DISABLED -> "hero.msg.disabled";
            default -> null;
        };
        if (key != null) p.displayClientMessage(Component.translatable(key), true);
    }

    // ================================================================ ending the loan without ceremony
    /**
     * Ends the loan: every projection is wiped from the player, the displaced items go back, the record is cleared. {@code keepFlag}: leave the bond saying the
     * weapon is out (a logout or a server stop: the next login puts it back); otherwise the weapon is stowed. What finds no room is dropped at the feet,
     * protected. No hooks, no effects: the callers that want them (a dismiss) add their own.
     */
    static void end(ServerPlayer p, boolean keepFlag) {
        HeroBond b = HeroBonds.of(p);
        HeroUse.cancelPending(p, p.isAlive());
        WeaponLoan<ItemStack> loan = load(p, b);
        List<ItemStack> over = loan.release(new InvSlots(p));
        clearRecord(b);
        b.setSummoned(keepFlag && b.isBound());
        for (ItemStack o : over) dropProtected(p, o);
        p.getInventory().setChanged();
    }

    /** The bond is being released: the weapon goes away, for good. */
    public static void onReleased(ServerPlayer p) {
        end(p, false);
        ADOPT.remove(p.getUUID());
    }

    /** The player is dying: the loan ends before the drops, so the displaced item is among them and the weapon is not. The bond stays. */
    public static void onDeath(ServerPlayer p) {
        if (!HeroBonds.has(p) && !hasStray(p)) return;
        end(p, false);
        SafeCast.tick("hero.death.forms", () -> HeroForms.refresh(p));
    }

    /** A logout or a server stop: the projection is taken out of the inventory that is about to be saved, and the bond keeps "out" so the next login restores it. */
    public static void shutdown(ServerPlayer p) {
        ADOPT.remove(p.getUUID());
        DIRTY.remove(p.getUUID());
        if (!HeroBonds.has(p) && !hasStray(p)) return;
        end(p, true);
    }

    // ================================================================ keeping it whole
    /** Marks a player whose weapon must be made whole at the next tick. */
    public static void markDirty(UUID player) { DIRTY.add(player); }

    /** Was this player marked (and now unmarked)? */
    static boolean takeDirty(UUID player) { return !DIRTY.isEmpty() && DIRTY.remove(player); }

    /**
     * Makes the loan whole: exactly one current projection in the home slot and none anywhere else when the bond says the weapon is out; none at all when it
     * says it is not. Cheap in the common case (42 slot checks, no NBT read). Called every four ticks, on login, respawn and dimension change.
     */
    public static void reconcile(ServerPlayer p) {
        HeroBond b = HeroBonds.of(p);
        boolean stray = hasStray(p);
        if (!b.isBound()) {
            if (stray) wipeStrays(p);
            return;
        }
        if (!p.isAlive()) return;
        if (!HeroConfig.enabled() || exempt(p)) {
            if (b.summoned() || stray) {
                end(p, false);
                SafeCast.tick("hero.reconcile.forms", () -> HeroForms.refresh(p));
            }
            return;
        }
        if (b.summoned()) {
            if (isWhole(p, b)) return;
            WeaponLoan<ItemStack> loan = load(p, b);
            loan.summon(new InvSlots(p), p.getInventory().selected, () -> make(p, b), st -> current(p, b.kind(), st));
            save(p, b, loan);
            p.getInventory().setChanged();
        } else if (stray || b.hasExtra(KEY) && !b.extra(KEY).isEmpty()) {
            end(p, false);
        }
    }

    private static boolean isWhole(ServerPlayer p, HeroBond b) {
        if (!b.hasExtra(KEY)) return false;
        CompoundTag rec = b.extra(KEY);
        if (!rec.contains("home")) return false;
        int home = rec.getInt("home");
        InvSlots s = new InvSlots(p);
        int found = 0;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack st = s.get(i);
            if (!isProjection(st)) continue;
            if (i != home || !current(p, b.kind(), st)) return false;
            found++;
        }
        return found == 1;
    }

    private static boolean hasStray(ServerPlayer p) {
        InvSlots s = new InvSlots(p);
        for (int i = 0; i < SLOTS; i++) if (isProjection(s.get(i))) return true;
        return false;
    }

    private static void wipeStrays(ServerPlayer p) {
        InvSlots s = new InvSlots(p);
        for (int i = 0; i < SLOTS; i++) if (isProjection(s.get(i))) s.set(i, ItemStack.EMPTY);
    }

    // ================================================================ the stack's own check (inventoryTick) and adoption
    /**
     * Called by the item once a tick for each hero weapon in a player's inventory: reads the facts, asks {@link HeroLoanPolicy}, and obeys. A stack that is not
     * the player's dissolves; a bare stack is queued for adoption. The projection that IS the loan's is left alone (the four-tick {@link #reconcile} decides
     * about duplicates and the home slot).
     */
    public static void vet(ServerPlayer p, ItemStack stack, HeroKind kind) {
        if (exempt(p)) return;
        if (!HeroConfig.enabled()) {
            stack.setCount(0);
            return;
        }
        HeroBond b = HeroBonds.of(p);
        String owner = HeroComponents.owner(stack);
        HeroLoanPolicy.Owner o = owner.isEmpty() ? HeroLoanPolicy.Owner.NONE : owner.equals(p.getUUID().toString()) ? HeroLoanPolicy.Owner.SELF : HeroLoanPolicy.Owner.OTHER;
        HeroLoanPolicy.Bond bd = !b.isBound() ? HeroLoanPolicy.Bond.NONE : b.kind() == kind ? HeroLoanPolicy.Bond.SAME : HeroLoanPolicy.Bond.OTHER;
        Verdict v = HeroLoanPolicy.verdict(false, o, bd, b.summoned());
        if (v == Verdict.KEEP) return;
        stack.setCount(0);
        if (v == Verdict.ADOPT && HeroConfig.adoptStacks()) ADOPT.put(p.getUUID(), kind);
        DIRTY.add(p.getUUID());
    }

    /** Runs the adoption the item queued: binds the weapon if the player is free, then summons it. Called from the player tick. */
    public static void processAdoption(ServerPlayer p) {
        if (ADOPT.isEmpty()) return;
        HeroKind kind = ADOPT.remove(p.getUUID());
        if (kind == null || exempt(p)) return;
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound()) {
            String why = HeroBonds.bind(p, kind);
            if (why != null) {
                if (HeroConfig.showMessages()) p.displayClientMessage(Component.translatable("hero.msg.adopt_refused", Component.translatable("item.nusmp." + kind.itemId()), why), true);
                return;
            }
        }
        if (HeroBonds.kindOf(p) == kind && !HeroBonds.of(p).summoned()) trySummon(p);
    }

    // ================================================================ containers and the cursor
    /**
     * Empties every hero weapon out of the slots of the menu the player has open that are not the player's own inventory (a chest, a crafting grid, an anvil),
     * so a projection put anywhere else vanishes within four ticks and is rebuilt in the hotbar. The cursor is the loan's own slot 41. Creative players are exempt.
     */
    public static void scanMenu(ServerPlayer p) {
        if (exempt(p)) return;
        Inventory inv = p.getInventory();
        boolean any = false;
        for (Slot s : p.containerMenu.slots) {
            if (s.container == inv) continue;
            ItemStack st = s.getItem();
            if (!st.isEmpty() && st.getItem() instanceof HeroWeaponItem) {
                s.set(ItemStack.EMPTY);
                any = true;
            }
        }
        if (any) DIRTY.add(p.getUUID());
    }

    // ================================================================ housekeeping
    static void dropProtected(ServerPlayer p, ItemStack st) {
        if (st.isEmpty()) return;
        ItemEntity e = new ItemEntity(p.level(), p.getX(), p.getY() + 0.3, p.getZ(), st);
        e.setPickUpDelay(20);
        e.setInvulnerable(true);
        e.setUnlimitedLifetime();
        p.level().addFreshEntity(e);
    }

    /** Forgets what is queued for one player (they left). */
    public static void forget(UUID player) {
        DIRTY.remove(player);
        ADOPT.remove(player);
    }

    /** Forgets everything (the server stopped). */
    public static void clear() {
        DIRTY.clear();
        ADOPT.clear();
    }
}
