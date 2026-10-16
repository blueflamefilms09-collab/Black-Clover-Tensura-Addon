package com.newuniverse.nusmp.relics.forge;

import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.forge.ForgeRules.Button;
import com.newuniverse.nusmp.relics.forge.ForgeRules.CoreKind;
import com.newuniverse.nusmp.relics.forge.ForgeRules.Msg;
import com.newuniverse.nusmp.relics.forge.ForgeRules.Subject;
import com.newuniverse.nusmp.relics.socket.SocketComponents;
import com.newuniverse.nusmp.relics.socket.SocketData;
import com.newuniverse.nusmp.relics.socket.SocketEngine;
import com.newuniverse.nusmp.relics.socket.SocketItems;
import com.newuniverse.nusmp.relics.socket.SocketRules;
import com.newuniverse.nusmp.relics.socket.SocketService;
import com.newuniverse.nusmp.relics.tier.TierRitual;
import com.newuniverse.nusmp.relics.tier.TierRules;
import com.newuniverse.nusmp.relics.tier.Tiers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Why: Better Combat owns the off hand and the use input, so the sneak-use flows of the tier ritual and the sockets (a second item in the off hand)
 * cannot be triggered any more. This menu is a second doorway to the SAME services: UPGRADE calls {@link TierRitual#attemptFromForge}, SOCKET and DRAW OUT
 * call {@link SocketService#socket} / {@link SocketService#unsocket}, so attunement, the top grade, cooldowns, the magicule/aura price
 * ({@code CostMath.spendPooled}), the ledger serial and the damage caps are the existing code's and are not copied here. The relic sits in slot 0 as the
 * very ItemStack the services edit, and every item in the table goes back to the player when the window closes (as a crafting table does), so the
 * Order's ledger never sees a copy. Everything is decided on the server; the client menu only holds the synced {@link ForgeView} numbers.
 * <p>
 * Slot 4 is a read-only hint and NEVER holds a relic: the Order's scanner reads the stacks of every opened menu, so a preview copy of a serialised relic
 * would look like a duplicate and bring the God of Order. It shows what the next press needs or gives back (the catalyst, or the Core to be drawn out).
 */
public class RelicForgeMenu extends AbstractContainerMenu {
    /** Slot positions inside the window (the screen draws the labels and wells at the same places). */
    public static final int RELIC_X = 26, RELIC_Y = 22, CAT_A_X = 62, CAT_A_Y = 14, CAT_B_X = 62, CAT_B_Y = 32, CORE_X = 98, CORE_Y = 22, PREVIEW_X = 134, PREVIEW_Y = 22;
    public static final int INV_Y = 160, HOTBAR_Y = 218, WIDTH = 176, HEIGHT = 242;

    private final ContainerLevelAccess access;
    private final Container table;
    private final Container hint = new SimpleContainer(1);
    private final ContainerData data = new SimpleContainerData(ForgeView.COUNT);
    private final ServerPlayer owner;
    private int cycle;
    private Msg last = Msg.OK;

    /** Client side (from the network) and the server's own constructor both come here. */
    public RelicForgeMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ForgeRegistry.MENU.get(), id);
        this.access = access;
        this.owner = inv.player instanceof ServerPlayer sp ? sp : null;
        this.table = new SimpleContainer(4) {
            @Override public void setChanged() {
                super.setChanged();
                RelicForgeMenu.this.slotsChanged(this);
            }
        };
        addSlot(new Slot(table, ForgeRules.SLOT_RELIC, RELIC_X, RELIC_Y) {
            @Override public boolean mayPlace(ItemStack s) { return ForgeRules.acceptsSubject(TierRitual.acceptsRelic(s), ForgeEngraving.isModWeapon(s)); }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(table, ForgeRules.SLOT_CATALYST_A, CAT_A_X, CAT_A_Y) {
            @Override public boolean mayPlace(ItemStack s) { return ForgeRules.acceptsCatalyst(TierRitual.acceptsCatalyst(s)); }
        });
        addSlot(new Slot(table, ForgeRules.SLOT_CATALYST_B, CAT_B_X, CAT_B_Y) {
            @Override public boolean mayPlace(ItemStack s) { return ForgeRules.acceptsCatalyst(TierRitual.acceptsCatalyst(s)); }
        });
        addSlot(new Slot(table, ForgeRules.SLOT_CORE, CORE_X, CORE_Y) {
            @Override public boolean mayPlace(ItemStack s) { return ForgeRules.acceptsCore(coreKind(s)); }
        });
        addSlot(new Slot(hint, 0, PREVIEW_X, PREVIEW_Y) {
            @Override public boolean mayPlace(ItemStack s) { return false; }
            @Override public boolean mayPickup(Player p) { return false; }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, HOTBAR_Y));
        addDataSlots(data);
        refresh();
    }

    public static RelicForgeMenu client(int id, Inventory inv) { return new RelicForgeMenu(id, inv, ContainerLevelAccess.NULL); }

    /** The synced numbers, decoded for the screen. */
    public ForgeView.View view() {
        int[] d = new int[ForgeView.COUNT];
        for (int i = 0; i < d.length; i++) d[i] = data.get(i);
        return ForgeView.decode(d);
    }

    // ---------------------------------------------------------------- classification of stacks
    static CoreKind coreKind(ItemStack s) {
        if (s == null || s.isEmpty()) return CoreKind.NONE;
        if (SocketItems.defOf(s) != null) return CoreKind.SOCKET_CORE;
        if (SocketEngine.isEmptyElementCore(s)) return CoreKind.EMPTY_CORE;
        if (!ForgeEngraving.bookEngravings(s).isEmpty()) return CoreKind.ENGRAVING_BOOK;
        return CoreKind.OTHER;
    }

    private ForgeRules.State state() {
        ItemStack relic = table.getItem(ForgeRules.SLOT_RELIC);
        Subject subject = relic.isEmpty() ? Subject.NONE : TierRitual.acceptsRelic(relic) ? Subject.RELIC : ForgeEngraving.isModWeapon(relic) ? Subject.WEAPON : Subject.OTHER;
        boolean atTop = false;
        int used = 0, total = 0;
        if (subject == Subject.RELIC) {
            atTop = TierRules.nextTarget(Tiers.effectiveGrade(relic).ordinal()) < 0;
            used = SocketComponents.get(relic).size();
            total = SocketService.slotsOf(relic);
        }
        int cats = 0;
        if (!table.getItem(ForgeRules.SLOT_CATALYST_A).isEmpty()) cats++;
        if (!table.getItem(ForgeRules.SLOT_CATALYST_B).isEmpty()) cats++;
        ItemStack core = table.getItem(ForgeRules.SLOT_CORE);
        CoreKind ck = coreKind(core);
        int bookN = ck == CoreKind.ENGRAVING_BOOK ? ForgeEngraving.bookEngravings(core).size() : 0;
        boolean engravable = subject != Subject.NONE && subject != Subject.OTHER && ForgeEngraving.isModWeapon(relic);
        int added = engravable ? ForgeEngraving.forgeCount(relic) : 0;
        return new ForgeRules.State(subject, atTop, cats, used, total, ck, cycle, added, bookN, engravable);
    }

    // ---------------------------------------------------------------- the synced picture
    private void refresh() {
        if (owner == null) return;
        ForgeRules.State s = state();
        cycle = ForgeRules.clampCycle(cycle, s.sockets());
        ItemStack relic = table.getItem(ForgeRules.SLOT_RELIC);
        boolean isRelic = s.subject() == Subject.RELIC;
        int tier = -1, target = -1;
        if (isRelic) {
            tier = Tiers.effectiveGrade(relic).ordinal();
            target = TierRules.nextTarget(tier);
        }
        data.set(ForgeView.D_TIER, tier);
        data.set(ForgeView.D_MAX, TierRules.MAX_TIER);
        data.set(ForgeView.D_USED, s.sockets());
        data.set(ForgeView.D_TOTAL, s.socketTotal());
        data.set(ForgeView.D_TARGET, target);
        data.set(ForgeView.D_COST, target < 0 ? 0 : (int) Math.round(TierRules.costPercent(target)));
        data.set(ForgeView.D_CHANCE, target < 0 ? 0 : (int) Math.round(TierRules.successChance(target) * 100.0));
        data.set(ForgeView.D_MASK, ForgeRules.enabledMask(new ForgeRules.State(s.subject(), s.atTop(), s.catalystStacks(), s.sockets(), s.socketTotal(), s.core(), cycle,
                s.forgeEngravings(), s.bookEngravings(), s.engravable())));
        data.set(ForgeView.D_MSG, last.ordinal());
        data.set(ForgeView.D_CYCLE, cycle);
        data.set(ForgeView.D_FORGE_ENGRAVINGS, s.forgeEngravings());
        data.set(ForgeView.D_COOLDOWN_SECONDS, isRelic ? (int) ((TierRitual.cooldownLeft(owner) + 19) / 20) : 0);
        data.set(ForgeView.D_SUBJECT, ForgeView.subjectCode(s.subject()));
        hint.setItem(0, hintStack(s, relic, target));
    }

    /** What the next press needs (the first accepted catalyst) or gives back (the Core at the chosen socket). Never a relic. */
    private ItemStack hintStack(ForgeRules.State s, ItemStack relic, int target) {
        if (s.subject() != Subject.RELIC) return ItemStack.EMPTY;
        if (s.core() == CoreKind.EMPTY_CORE && s.sockets() > 0) {
            SocketData sd = SocketComponents.get(relic);
            return SocketItems.stack(sd.entries().get(ForgeRules.clampCycle(cycle, sd.size())).id());
        }
        if (target >= 1) {
            TierRules.Catalyst[] alts = TierRules.catalystsTo(target);
            if (alts.length > 0) {
                Item it = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(alts[0].itemId())).orElse(null);
                if (it != null) return new ItemStack(it, alts[0].count());
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void slotsChanged(Container c) {
        if (c == table) {
            last = Msg.OK;
            refresh();
        }
        super.slotsChanged(c);
    }

    // ---------------------------------------------------------------- the buttons
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer sp) || sp != owner) return false;
        Button b = Button.of(id);
        ForgeRules.State s = state();
        Msg refusal = ForgeRules.check(b, s);
        if (refusal != Msg.OK) return finish(sp, refusal);
        ItemStack relic = table.getItem(ForgeRules.SLOT_RELIC), core = table.getItem(ForgeRules.SLOT_CORE);
        Msg result = Msg.OK;
        switch (b) {
            case UPGRADE -> {
                List<ItemStack> cats = List.of(table.getItem(ForgeRules.SLOT_CATALYST_A), table.getItem(ForgeRules.SLOT_CATALYST_B));
                result = fromRitual(TierRitual.attemptFromForge(sp, relic, cats).reason());
            }
            case SOCKET -> {
                var out = SocketService.socket(sp, relic, SocketItems.defOf(core), false);
                if (out == SocketRules.Outcome.OK) {
                    if (!sp.isCreative()) core.shrink(1);
                    result = Msg.SOCKET_OK;
                } else result = Msg.SOCKET_REFUSED;
            }
            case DRAW_OUT -> {
                ItemStack back = SocketService.unsocket(sp, relic, ForgeRules.clampCycle(cycle, s.sockets()));
                if (back.isEmpty()) result = Msg.NOTHING_BOUND;
                else {
                    SocketService.give(sp, back);
                    if (!sp.isCreative()) core.shrink(1);
                    result = Msg.DRAWN_OUT;
                }
            }
            case CYCLE -> cycle = ForgeRules.cycleNext(cycle, s.sockets());
            case ENGRAVE -> result = ForgeEngraving.apply(sp, relic, core);
        }
        if (result.good() && result != Msg.OK && result != Msg.DRAWN_OUT) com.newuniverse.nusmp.quests.NusmpEvents.fire(sp, "relic.forged");
        if (result == Msg.ENGRAVED) com.newuniverse.nusmp.quests.NusmpEvents.fire(sp, "weapon.engraved");
        table.setChanged();                                                           // the stacks were edited in place: re-read them
        return finish(sp, result);
    }

    private boolean finish(ServerPlayer sp, Msg m) {
        last = m;
        if (m != Msg.OK) sp.displayClientMessage(Component.translatable(m.langKey()).withStyle(m.good() ? ChatFormatting.AQUA : ChatFormatting.RED), true);
        refresh();
        broadcastChanges();
        return true;
    }

    static Msg fromRitual(TierRitual.Reason r) {
        return switch (r) {
            case SUCCESS -> Msg.UPGRADE_OK;
            case NOT_A_RELIC -> Msg.NOT_ALLOWED;
            case COOLDOWN -> Msg.UPGRADE_COOLDOWN;
            case UNATTUNED -> Msg.UPGRADE_UNATTUNED;
            case AT_TOP -> Msg.AT_TOP;
            case WRONG_CATALYST -> Msg.UPGRADE_WRONG_CATALYST;
            case NEED_COUNT -> Msg.UPGRADE_NEED_COUNT;
            case NO_ENERGY -> Msg.UPGRADE_NO_ENERGY;
            case FAILED_KEPT -> Msg.UPGRADE_FAILED_KEPT;
            case FAILED_LOST -> Msg.UPGRADE_FAILED_LOST;
        };
    }

    // ---------------------------------------------------------------- moving stacks, closing
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot from = slots.get(index);
        if (index == ForgeRules.SLOT_PREVIEW || !from.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = from.getItem(), copy = stack.copy();
        if (index < ForgeRules.SLOT_PREVIEW) {
            if (!moveItemStackTo(stack, ForgeRules.TABLE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int t = 0; t < ForgeRules.SLOT_PREVIEW && !moved; t++) {
                Slot to = slots.get(t);
                if (to.mayPlace(stack) && !(t == ForgeRules.SLOT_RELIC && to.hasItem())) moved = moveItemStackTo(stack, t, t + 1, false);
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) from.setByPlayer(ItemStack.EMPTY); else from.setChanged();
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) clearContainer(player, table);          // every item goes back to the player, the way a crafting table does
    }

    @Override
    public boolean stillValid(Player player) { return stillValid(access, player, ForgeRegistry.RELIC_FORGE.get()); }
}
