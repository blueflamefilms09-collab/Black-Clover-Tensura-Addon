package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.djinn.BindingRules.Refusal;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * The binding ritual and its undoing. See {@link BindingRules} for why the price is what it is. The ritual's order: look (nothing is paid until
 * every condition holds), commit to the ledger (so two players racing for one Djinn cannot both win), pay, stamp the vessel, set the bearer's
 * record. A failed trial is paid at half and never touches the Seal or the vessel.
 */
public final class DjinnBinding {
    private DjinnBinding() {}

    private static final ResourceLocation CRYSTAL = ResourceLocation.fromNamespaceAndPath("tensura", "high_quality_magic_crystal");

    /** The Seal in one hand, a vessel in the other (the old entry point; the Seal's right-click). */
    public static void attempt(ServerPlayer p, ItemStack seal, ItemStack vessel) {
        attempt(p, seal, vessel, BindGesture.Gesture.USE_SEAL_MAIN);
    }

    /**
     * 1.101.0: the ritual from any gesture. Returns true when the Djinn was bound. A refusal is told to the player twice (action bar and chat) with the
     * exact reason ({@link BindGesture.Why}); the vessel is judged by its item and the ritual's own two components only, so extra components on the stack
     * (Better Combat's, other mods') change nothing.
     */
    public static boolean attempt(ServerPlayer p, ItemStack seal, ItemStack vessel, BindGesture.Gesture gesture) {
        Djinn d = DjinnItems.djinnOf(seal);
        MinecraftServer server = p.getServer();
        if (d == null || server == null) return false;
        BindGesture.Why gate = BindGesture.decide(gesture, true, VesselAccess.look(vessel, p), DjinnBindConfig.command(), DjinnBindConfig.gui());
        if (gate == BindGesture.Why.GESTURE_DISABLED) { tell(p, gate, vessel); return false; }
        DjinnLedger ledger = DjinnLedger.get(server);
        DjinnState st = DjinnState.of(p);
        int crystals = countCrystals(p);
        BindingRules.Inputs in = new BindingRules.Inputs(ledger.owner(d) == null, st.owns(), Bonds.canBind(p, Bonds.Kind.DJINN), st.lent(),
                VesselAccess.isVessel(vessel), VesselAccess.boundDjinn(vessel) == null, VesselAccess.attunedTo(vessel, p), Magoi.fraction(p), crystals, p.isCreative());
        Refusal why = BindingRules.check(in);
        if (why != Refusal.NONE) {
            if (why == Refusal.NO_VESSEL || why == Refusal.VESSEL_TAKEN || why == Refusal.NOT_ATTUNED) tell(p, gate == BindGesture.Why.OK ? BindGesture.Why.EMPTY : gate, vessel);
            else say(p, Component.literal(text(why, d)).withStyle(ChatFormatting.RED));
            return false;
        }
        ManaLaw.Stage stage = ManaLaw.stage(p);
        double chance = BindingRules.failChance(stage.number(), stage.arcane() || stage.saint());
        boolean fail = !p.isCreative() && BindingRules.fails(chance, p.getRandom().nextDouble());
        ServerLevel level = p.serverLevel();
        if (fail) {
            payEp(p, BindingRules.EP_FRACTION / 2);
            takeCrystals(p, BindingRules.failedCrystals());
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BindingRules.FAIL_WEAKNESS_TICKS, 1));
            p.getCooldowns().addCooldown(seal.getItem(), BindingRules.RETRY_COOLDOWN);
            level.playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2f, 0.6f);
            p.sendSystemMessage(Component.literal(d.displayName() + " tests you and turns away. Half the price is gone; the Seal and the vessel are whole. Try again in a minute.")
                    .withStyle(ChatFormatting.DARK_RED));
            return false;
        }
        UUID serial = UUID.randomUUID();
        if (!ledger.bind(d, p.getUUID(), p.getName().getString(), serial)) {
            say(p, Component.literal(text(Refusal.ALREADY_OWNED, d)).withStyle(ChatFormatting.RED));
            return false;
        }
        payEp(p, BindingRules.EP_FRACTION);
        takeCrystals(p, BindingRules.CRYSTALS);
        seal.shrink(1);
        VesselAccess.stamp(vessel, d, serial);
        st.bind(d, serial, DjinnPhaseRules.START_TRUST);
        com.newuniverse.nusmp.quests.NusmpEvents.fire(p, "relic.djinn_bound", d.name().toLowerCase(java.util.Locale.ROOT));
        DjinnEngine.restore(p);
        DjinnMagic.flare(p, d, DjinnFx.particle(d.affinity()));
        DjinnFx.ring(level, p.position(), 4.0, d.glow(), 64, 0.2);
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 0.8f);
        level.playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.2f, 0.8f);
        server.getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString() + " has conquered " + d.fullName() + ". The "
                + d.affinity().label + " Djinn sleeps in " + vessel.getHoverName().getString() + ".").withColor(d.color()), false);
        p.sendSystemMessage(Component.literal("Wear or hold the vessel and press the Djinn key (default J) to call " + d.displayName() + ". Its trust starts at "
                + DjinnPhaseRules.START_TRUST + "; the Full Equip needs " + DjinnPhaseRules.FULL_TRUST + ".").withStyle(ChatFormatting.GRAY));
        return true;
    }

    // ---------------------------------------------------------------- 1.101.0: the gestures that find their own Seal and vessel
    /** Where {@link #fromInventory} looks for the vessel. */
    public enum Pick { AUTO, MAIN, OFF, SLOT }

    /**
     * The command, the Djinn key and the inventory screen: no right-click on an item is needed, so nothing that changes an item's use hook (Better Combat's
     * two-handed off-hand rule, a click that is eaten by the other hand's item) can stop it. The Seal is {@code djinnHint}'s if given, else a Seal in a hand,
     * else the first Seal carried; the vessel is the hand or slot asked for, or (AUTO) the first legal one among the hands and the inventory.
     */
    public static boolean fromInventory(ServerPlayer p, BindGesture.Gesture gesture, Pick pick, int slot, Djinn djinnHint) {
        if (!BindGesture.enabled(gesture, DjinnBindConfig.command(), DjinnBindConfig.gui())) { tell(p, BindGesture.Why.GESTURE_DISABLED, ItemStack.EMPTY); return false; }
        ItemStack seal = findSeal(p, djinnHint);
        if (seal.isEmpty()) { tell(p, BindGesture.Why.NO_SEAL, ItemStack.EMPTY); return false; }
        ItemStack vessel = switch (pick) {
            case MAIN -> VesselAccess.rawMain(p);
            case OFF -> VesselAccess.rawOff(p);
            case SLOT -> slotStack(p, slot);
            default -> autoVessel(p, seal);
        };
        if (vessel == seal) vessel = ItemStack.EMPTY;                                   // a Seal is never its own vessel
        return attempt(p, seal, vessel, gesture);
    }

    /** The stack of inventory index 0-35 (main), 36-39 (armour feet..head), 40 (off hand); EMPTY outside. */
    public static ItemStack slotStack(Player p, int index) {
        Inventory inv = p.getInventory();
        if (index >= 0 && index < 36) return inv.items.get(index);
        if (index >= 36 && index < 40) return inv.armor.get(index - 36);
        if (index == 40) return inv.offhand.get(0);
        return ItemStack.EMPTY;
    }

    /** A Seal the player carries: the one of {@code hint} if given; else in the main hand, the off hand, then the first in the inventory. */
    public static ItemStack findSeal(Player p, Djinn hint) {
        ItemStack[] hands = {VesselAccess.rawMain(p), VesselAccess.rawOff(p)};
        for (ItemStack h : hands) if (isSeal(h, hint)) return h;
        Inventory inv = p.getInventory();
        for (int i = 0; i < 41; i++) {
            ItemStack s = slotStack(p, i);
            if (isSeal(s, hint)) return s;
        }
        return ItemStack.EMPTY;
    }

    private static boolean isSeal(ItemStack s, Djinn hint) {
        Djinn d = DjinnItems.djinnOf(s);
        return d != null && (hint == null || d == hint);
    }

    /** AUTO: the first stack that is a free, attuned, legal vessel (hands first, then the hotbar and the rest of the inventory); else the first relic in a hand, for the reason. */
    private static ItemStack autoVessel(Player p, ItemStack seal) {
        ItemStack firstRelic = ItemStack.EMPTY;
        for (int i = -2; i < 41; i++) {
            ItemStack s = i == -2 ? VesselAccess.rawMain(p) : i == -1 ? VesselAccess.rawOff(p) : slotStack(p, i);
            if (s.isEmpty() || s == seal || Relics.defOf(s) == null) continue;
            if (VesselAccess.verdict(s, p) == BindGesture.Why.OK) return s;
            if (firstRelic.isEmpty() && i < 0) firstRelic = s;
        }
        return firstRelic;
    }

    /** Tells the player why (action bar and chat). {@code vessel} names the stack in the text where the reason has a name. */
    static void tell(ServerPlayer p, BindGesture.Why why, ItemStack vessel) {
        String name = vessel == null || vessel.isEmpty() ? "That item" : vessel.getHoverName().getString();
        Component msg = Component.translatableWithFallback(why.key(), why.fallback().replace("%s", name), name).withStyle(ChatFormatting.RED);
        say(p, msg);
    }

    private static void say(ServerPlayer p, Component msg) {
        p.displayClientMessage(msg, true);
        p.displayClientMessage(msg, false);
    }

    private static String text(Refusal r, Djinn d) {
        return switch (r) {
            case ALREADY_OWNED -> d.displayName() + " already has a master.";
            case ALREADY_HOLDS_DJINN -> "You already carry a Djinn. Release it first (/multiverse djinn release).";
            case HOUSEHOLD -> "You are borrowing someone's Djinn. Leave their household first (/multiverse djinn leave).";
            case EXCLUSIVE_BOND -> "You hold a Spirit or a Devil. A mage holds one bond: a Spirit, a Devil or a Djinn.";
            case NO_VESSEL -> "Hold a Metal Vessel in your other hand: a relic of Legend grade or above (or use /multiverse djinn bind).";
            case VESSEL_TAKEN -> "That vessel already holds a Djinn.";
            case NOT_ATTUNED -> "The vessel must be attuned to you first.";
            case BAR_NOT_FULL -> d.displayName() + " answers only a full Magoi bar. Rest until it is full.";
            case NO_CRYSTALS -> "The ritual needs " + BindingRules.CRYSTALS + " high-quality magic crystals.";
            default -> "The ritual cannot begin.";
        };
    }

    // ---------------------------------------------------------------- the price
    private static void payEp(ServerPlayer p, double fraction) {
        if (p.isCreative()) return;
        double[] m = Magoi.pool(p), a = Magoi.aura(p);
        Magoi.take(p, BindingRules.epCost(m[0], m[1], fraction), BindingRules.epCost(a[0], a[1], fraction));
    }

    private static Item crystal() { return BuiltInRegistries.ITEM.getOptional(CRYSTAL).orElse(null); }

    static int countCrystals(ServerPlayer p) {
        Item c = crystal();
        if (c == null) return 0;
        int n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(c)) n += s.getCount();
        }
        return n;
    }

    private static void takeCrystals(ServerPlayer p, int count) {
        Item c = crystal();
        if (c == null || p.isCreative()) return;
        int left = count;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(c)) continue;
            int take = Math.min(left, s.getCount());
            s.shrink(take);
            left -= take;
        }
    }

    // ---------------------------------------------------------------- letting go
    /**
     * The owner lets the Djinn go: everything ends, the vessel forgets it, the household is called back, the ledger holds a Seal. Returns the Seal
     * (the caller puts it in the inventory, or drops it where the owner died); EMPTY if the player owns no Djinn.
     */
    public static ItemStack releaseOwned(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        DjinnState st = DjinnState.of(p);
        Djinn d = st.djinn();
        if (d == null || server == null) return ItemStack.EMPTY;
        DjinnLedger ledger = DjinnLedger.get(server);
        DjinnEngine.shutdown(p, DjinnPhaseRules.EndReason.ADMIN);
        UUID serial = st.serial();
        if (serial != null) VesselAccess.stripEverywhere(p, serial);
        for (UUID m : ledger.household(d)) {
            ServerPlayer mp = server.getPlayerList().getPlayer(m);
            if (mp != null) DjinnEngine.dropLent(mp);
        }
        ledger.release(d, p.level().dimension().location().toString(), p.blockPosition(), server.overworld().getGameTime());
        st.clearBond();
        DjinnEngine.restore(p);
        return DjinnItems.sealStack(d);
    }
}
