package com.newuniverse.nusmp.celestial;

import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * Why: what the Celestial Spirit King gives. A victory in the Spirit World, or the recognition of the twelve spirits, claims a reward ONCE per outcome (the ledger remembers it):
 * a permanent buff (maximum health, and a share of maximum magicules and aura: the large EP gain), a deeper bond with all twelve spirits, a full or half refill, the unique
 * sceptre (victory only) and the King's modes of the skill (the Blessing for either, the Summon for a victory). The numbers are {@link KingRules#reward}. The permanent buff is held
 * as transient attribute modifiers that are put back from the record every second ({@link #refresh}), so it survives death, a relog and a change of Tensura's own scaling.
 * Only a player who owns the whole collection claims the lasting part; a helper who came along without it is mended and thanked.
 */
public final class KingRewards {
    private KingRewards() {}

    private static final ResourceLocation HP = id("spirit_king_hp"), MAGICULE = id("spirit_king_magicule"), AURA = id("spirit_king_aura");

    private static ResourceLocation id(String p) { return ResourceLocation.fromNamespaceAndPath("nusmp", p); }

    /** The king fell: claims the victory reward (once) and sets the long wait before the next challenge. */
    public static void victory(ServerPlayer p) {
        CelestialData d = CelestialAttachments.of(p);
        long now = p.level().getGameTime();
        d.markWon();
        d.setKingCooldown(now + KingRules.refightCooldown(true));
        claim(p, d, KingRules.Outcome.VICTORY, d.claimWin());
    }

    /** The king recognised the player: claims the smaller reward (once). */
    public static void recognition(ServerPlayer p) {
        CelestialData d = CelestialAttachments.of(p);
        d.markRecognised();
        claim(p, d, KingRules.Outcome.RECOGNITION, d.claimRecognition());
    }

    private static void claim(ServerPlayer p, CelestialData d, KingRules.Outcome outcome, boolean first) {
        KingRules.Reward r = KingRules.reward(outcome);
        ServerLevel sl = p.serverLevel();
        if (!d.complete()) {                                                    // a helper without the collection: mended, thanked, nothing lasting
            refill(p, 1.0);
            p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.celestial.reward.helper", "The King thanks you and mends you. His gifts are for the keeper of all twelve keys.").withStyle(ChatFormatting.GOLD));
            return;
        }
        if (!first) {                                                           // faced again: a refill and a nod
            refill(p, 1.0);
            p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.celestial.reward.again", "The King nods. He has already given you all he has.").withStyle(ChatFormatting.GOLD));
            return;
        }
        for (CelestialKey k : CelestialKey.gold()) d.addBond(k, 4 * r.bond());
        if (r.sceptre() && !d.sceptreGiven()) {
            ItemStack s = new ItemStack(CelestialItems.SCEPTRE.get());
            if (!p.getInventory().add(s)) p.drop(s, false);
            d.sceptreHandedOver();
        }
        refill(p, r.refill());
        refresh(p);
        p.sendSystemMessage(Component.translatableWithFallback(outcome == KingRules.Outcome.VICTORY ? "message.nusmp.celestial.reward.win" : "message.nusmp.celestial.reward.recognised",
                outcome == KingRules.Outcome.VICTORY
                        ? "The Celestial Spirit King lowers his sceptre. Your spirits kneel. +%s maximum health, +%s%% magicules and aura, a deeper bond with all twelve, and the sceptre of the stars."
                        : "The Celestial Spirit King recognises you. +%s maximum health, +%s%% magicules and aura, a deeper bond with all twelve.",
                r.hp() / 2, (int) Math.round(r.magicule() * 100)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (r.summonMode()) p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.celestial.reward.mode", "New modes: the Spirit King's Blessing and the Summon of the Spirit King.").withStyle(ChatFormatting.AQUA));
        else if (r.blessingMode()) p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.celestial.reward.mode2", "New mode: the Spirit King's Blessing.").withStyle(ChatFormatting.AQUA));
        sl.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.2f, 1.0f);
        CelestialNet.sync(p);
    }

    /** Refills magicules and aura to this share of the maximum (never lowers them). */
    public static void refill(ServerPlayer p, double frac) {
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null || frac <= 0) return;
        try {
            ex.setMagicule(Math.max(ex.getMagicule(), EnergyHelper.getMaxMagicule(p) * frac));
            ex.setAura(Math.max(ex.getAura(), EnergyHelper.getMaxAura(p) * frac));
            ex.markDirty();
        } catch (Throwable ignored) { }
    }

    /** Puts the permanent buff of the claimed rewards on the player (idempotent; once a second from the player tick). */
    public static void refresh(ServerPlayer p) {
        CelestialData d = CelestialAttachments.of(p);
        KingRules.Reward t = d.complete() ? KingRules.total(d.winClaimed(), d.recognitionClaimed()) : KingRules.total(false, false);
        put(p, Attributes.MAX_HEALTH, HP, t.hp(), AttributeModifier.Operation.ADD_VALUE);
        put(p, TensuraAttributes.MAX_MAGICULE, MAGICULE, t.magicule(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        put(p, TensuraAttributes.MAX_AURA, AURA, t.aura(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void put(ServerPlayer p, Holder<Attribute> a, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        AttributeModifier old = inst.getModifier(id);
        if (amount <= 0) { if (old != null) inst.removeModifier(id); return; }
        if (old != null && Math.abs(old.amount() - amount) < 1e-9) return;
        inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, amount, op));
    }
}
