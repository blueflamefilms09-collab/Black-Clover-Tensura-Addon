package com.newuniverse.nusmp.celestial;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Celestial Gate is the moment the collection pays off. Using the gate key with all twelve Gold Zodiac keys carried and bound opens the Gate (a pillar of light, the
 * door of the Spirit World rising from the ground, a circle of the twelve signs) and offers a choice for ninety seconds: challenge the Celestial Spirit King in the Spirit
 * World, or ask him for his recognition. The choice is made with the two clickable lines in the chat (they run {@code /multiverse celestial gate challenge|recognition}) so no GUI
 * is needed. The rules of who may do which are {@link KingRules}; the fight is {@link SpiritWorld}; what each gives is {@link KingRewards}. Server thread only.
 */
public final class GateRitual {
    private GateRitual() {}

    /** The skill mastery as 0..1. The Celestial Spirit Magic skill was removed (the book that taught it is gone), so nobody has any: always 0. */
    public static double mastery(ServerPlayer p) { return 0.0; }

    /** The gate key was used: opens the Gate if the rules allow, else says what is missing. */
    public static boolean open(ServerPlayer p) {
        CelestialData d = CelestialAttachments.of(p);
        long now = p.level().getGameTime();
        int held = CelestialLedger.heldMask(p);
        KingRules.Gate g = KingRules.canOpen(held, d.bound(), CelestialRules.has(held, CelestialKey.SPIRIT_KING_GATE), now, d.kingCooldownUntil(), now < d.gateOpenUntil());
        if (g != KingRules.Gate.OK) {
            p.displayClientMessage(refusal(g, d, now), true);
            return false;
        }
        d.setGateOpenUntil(now + KingRules.CHOICE_TICKS);
        ServerLevel sl = p.serverLevel();
        SpiritWorld.showGate(p);
        Vec3 c = p.position();
        com.newuniverse.nusmp.vfx.VfxSpawn.send(sl, com.newuniverse.nusmp.vfx.VfxShape.DIVINE_PILLAR, c, c.add(0, 3, 0), 0xFFFFF3B0, 60, 3.0f);
        com.newuniverse.nusmp.vfx.VfxSpawn.send(sl, com.newuniverse.nusmp.vfx.VfxShape.DIVINE_SIGIL, c, c.add(0, 1, 0), 0xFFFFF3B0, 80, 2.4f);
        com.newuniverse.nusmp.vfx.VfxSpawn.send(sl, com.newuniverse.nusmp.vfx.VfxShape.STAR_MAGIC, c.add(0, 6, 0), c, 0xFFFFE27A, 60, 2.0f);
        sl.playSound(null, p.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.6f, 0.9f);
        sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4f, 0.6f);
        p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.celestial.gate", "The Celestial Gate opens. The twelve keys sing. Choose:").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        p.sendSystemMessage(choice("[Challenge the Celestial Spirit King]", "/multiverse celestial gate challenge", ChatFormatting.RED));
        boolean earned = KingRules.recognitionEarned(d.bound(), d.bondCopy(), mastery(p), d.recognised());
        p.sendSystemMessage(choice(earned ? "[Ask for his recognition]" : "[Ask for his recognition] (not yet earned)", "/multiverse celestial gate recognition", earned ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        CelestialNet.sync(p);
        return true;
    }

    private static MutableComponent choice(String text, String command, ChatFormatting color) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(color).withBold(true).withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    private static Component refusal(KingRules.Gate g, CelestialData d, long now) {
        return switch (g) {
            case NOT_ALL_KEYS -> Component.translatableWithFallback("message.nusmp.celestial.gate.keys", "The Gate needs all twelve Gold Zodiac keys on you (%s of 12).", CelestialRules.goldCount(d.owned())).withStyle(ChatFormatting.RED);
            case NOT_BOUND -> Component.translatableWithFallback("message.nusmp.celestial.gate.bound", "Every one of the twelve keys needs a sealed contract.").withStyle(ChatFormatting.RED);
            case COOLDOWN -> Component.translatableWithFallback("message.nusmp.celestial.gate.cooldown", "The King will not see you again for %s minutes.", Math.max(1, (d.kingCooldownUntil() - now) / 1200)).withStyle(ChatFormatting.RED);
            case ALREADY_OPEN -> Component.translatableWithFallback("message.nusmp.celestial.gate.open", "The Gate is already open: choose.").withStyle(ChatFormatting.YELLOW);
            default -> Component.translatableWithFallback("message.nusmp.celestial.gate.nokey", "You need the key of the Gate.").withStyle(ChatFormatting.RED);
        };
    }

    /** The command behind the two lines: the choice made after the Gate opened. */
    public static void choose(ServerPlayer p, KingRules.Choice choice) {
        CelestialData d = CelestialAttachments.of(p);
        long now = p.level().getGameTime();
        if (now >= d.gateOpenUntil()) {
            p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.celestial.gate.closed", "The Gate is closed. Use the gate key to open it.").withStyle(ChatFormatting.RED));
            return;
        }
        if (choice == KingRules.Choice.RECOGNITION) {
            if (!KingRules.recognitionEarned(d.bound(), d.bondCopy(), mastery(p), d.recognised())) {
                p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.celestial.recognition.no",
                        "He does not know you well enough: all twelve spirits need a bond of at least 2 and the skill half mastered (or you were already recognised).").withStyle(ChatFormatting.RED));
                return;
            }
            d.setGateOpenUntil(0);
            KingRewards.recognition(p);
            return;
        }
        List<ServerPlayer> group = new ArrayList<>();
        group.add(p);
        for (ServerPlayer o : p.serverLevel().players()) {
            if (o == p || o.isSpectator() || o.distanceToSqr(p) > 12 * 12 || CelestialAttachments.of(o).kingCooldownUntil() > now) continue;
            boolean mate = o.isAlliedTo(p);
            try { mate |= com.newuniverse.nusmp.labyrinth.mega.party.PartyService.partyOf(p) > 0 && com.newuniverse.nusmp.labyrinth.mega.party.PartyService.partyOf(o) == com.newuniverse.nusmp.labyrinth.mega.party.PartyService.partyOf(p); }
            catch (RuntimeException | LinkageError ignored) { }
            if (mate && group.size() < KingRules.MAX_PARTY) group.add(o);
        }
        if (!SpiritWorld.enter(p, group)) return;
    }
}
