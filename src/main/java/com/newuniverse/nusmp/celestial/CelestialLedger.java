package com.newuniverse.nusmp.celestial;

import com.newuniverse.nusmp.item.CuriosLink;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the server-side half of the key collection. It finds the keys a player carries (the inventory, the off hand, and the Celestial Key slot of Curios when Curios is
 * installed), notices a new key once, hands over the gate key when the twelfth gold key is found, seals and releases the contract of a key, and charges the magicules for
 * those. The rules it applies are in {@link CelestialRules}; the record it keeps is {@link CelestialData}. Server thread only.
 */
public final class CelestialLedger {
    private CelestialLedger() {}

    public static final String OWNER = "CelestialOwner", OWNER_NAME = "CelestialOwnerName";

    // ================================================================ what the player carries
    /** Every key stack the player carries: main inventory, off hand and the Celestial Key slot of Curios. */
    public static List<ItemStack> keyStacks(Player p) {
        List<ItemStack> out = new ArrayList<>();
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (CelestialItems.keyOf(s) != null) out.add(s);
        }
        for (ItemStack s : CuriosLink.equipped(p)) if (CelestialItems.keyOf(s) != null) out.add(s);
        return out;
    }

    /** The mask of the keys the player carries that they may use (not another player's contracted key). */
    public static int heldMask(Player p) {
        int mask = 0;
        String me = p.getUUID().toString();
        for (ItemStack s : keyStacks(p)) {
            CelestialKey k = CelestialItems.keyOf(s);
            String owner = ownerOf(s);
            if (k != null && (owner.isEmpty() || owner.equals(me))) mask |= k.bit();
        }
        return mask;
    }

    /** The stack of this key the player carries (a usable one), or empty. */
    public static ItemStack find(Player p, CelestialKey key) {
        String me = p.getUUID().toString();
        for (ItemStack s : keyStacks(p)) {
            if (CelestialItems.keyOf(s) != key) continue;
            String owner = ownerOf(s);
            if (owner.isEmpty() || owner.equals(me)) return s;
        }
        return ItemStack.EMPTY;
    }

    public static String ownerOf(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? "" : d.copyTag().getString(OWNER);
    }

    public static String ownerNameOf(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? "" : d.copyTag().getString(OWNER_NAME);
    }

    // ================================================================ noticing keys
    /**
     * Once a second: records every usable key the player carries in the collection, says so the first time, and when the collection reaches twelve gold keys gives the gate
     * key (once) with the words that say what it is for. Returns the keys found for the first time.
     */
    public static List<CelestialKey> scan(ServerPlayer p) {
        CelestialData data = CelestialAttachments.of(p);
        List<CelestialKey> fresh = new ArrayList<>();
        int held = heldMask(p);
        for (CelestialKey k : CelestialKey.values()) {
            if (!CelestialRules.has(held, k) || !k.summons()) continue;
            if (data.collect(k)) fresh.add(k);
        }
        for (CelestialKey k : fresh) {
            p.displayClientMessage(Component.translatableWithFallback("message.nusmp.celestial.found",
                    "A key answers your hand: %s. (%s of 12 gold keys)", k.display + " (" + k.spirit + ")", CelestialRules.goldCount(data.owned())).withStyle(ChatFormatting.GOLD), false);
            ((ServerLevel) p.level()).playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.4f);
        }
        if (data.gateKeyDue()) giveGateKey(p, data);
        return fresh;
    }

    /** Hands over the gate key (when the collection is complete and it was not given yet). */
    public static void giveGateKey(ServerPlayer p, CelestialData data) {
        ItemStack key = CelestialItems.stack(CelestialKey.SPIRIT_KING_GATE);
        if (!p.getInventory().add(key)) p.drop(key, false);
        data.gateKeyHandedOver();
        p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.celestial.complete",
                "All twelve Gold Zodiac keys are yours. A thirteenth key, pale as starlight, has appeared: the key of the Celestial Gate.").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        ((ServerLevel) p.level()).playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    // ================================================================ the contract
    /** The magicules of a player and their maximum (creative and no-Tensura players are treated as rich). */
    public static double magicules(Player p) {
        var ex = TensuraStorages.getExistenceFrom(p);
        return ex == null ? 1.0e12 : ex.getMagicule();
    }

    public static double maxMagicules(Player p) {
        try { return EnergyHelper.getMaxMagicule(p); } catch (Throwable t) { return 1000.0; }
    }

    /** Takes magicules; false (nothing taken) when the player cannot pay. Creative pays nothing. */
    public static boolean spend(ServerPlayer p, double cost) {
        if (p.isCreative() || cost <= 0) return true;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return true;
        if (ex.getMagicule() < cost) return false;
        ex.setMagicule(ex.getMagicule() - cost);
        ex.markDirty();
        return true;
    }

    public static void refund(ServerPlayer p, double amount) {
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null || amount <= 0) return;
        ex.setMagicule(Math.min(maxMagicules(p), ex.getMagicule() + amount));
        ex.markDirty();
    }

    /** The first reason the contract of the key in this stack cannot be sealed, or null when it can. */
    public static CelestialRules.Bind bindCheck(ServerPlayer p, ItemStack stack) {
        CelestialKey key = CelestialItems.keyOf(stack);
        CelestialData data = CelestialAttachments.of(p);
        return CelestialRules.canBind(key, key != null && data.isBound(key), ownerOf(stack), p.getUUID().toString(), magicules(p), maxMagicules(p));
    }

    /** Seals the contract: spends the ritual's magicules, marks the key as the player's and the spirit as bound. Returns the result (OK on success). */
    public static CelestialRules.Bind seal(ServerPlayer p, ItemStack stack) {
        CelestialRules.Bind r = bindCheck(p, stack);
        CelestialKey key = CelestialItems.keyOf(stack);
        if (r == CelestialRules.Bind.ALREADY_BOUND && key != null && ownerOf(stack).isEmpty()) r = CelestialRules.Bind.OK;   // the ledger knows the spirit, this key is a new copy: re-mark it without a second charge
        else if (r == CelestialRules.Bind.OK && !spend(p, CelestialRules.bindCost(maxMagicules(p)))) r = CelestialRules.Bind.NOT_ENOUGH_MAGICULES;
        if (r != CelestialRules.Bind.OK) return r;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag t) -> {
            t.putString(OWNER, p.getUUID().toString());
            t.putString(OWNER_NAME, p.getGameProfile().getName());
        });
        CelestialData data = CelestialAttachments.of(p);
        boolean first = !data.isBound(key);
        data.bind(key);
        if (first) data.addBond(key, 1);
        return r;
    }

    /** Breaks the contract of this key (the stack becomes free to give away, the spirit unbound). */
    public static void release(ServerPlayer p, ItemStack stack) {
        CelestialKey key = CelestialItems.keyOf(stack);
        if (key == null) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag t) -> { t.remove(OWNER); t.remove(OWNER_NAME); });
        CelestialAttachments.of(p).unbind(key);
    }
}
