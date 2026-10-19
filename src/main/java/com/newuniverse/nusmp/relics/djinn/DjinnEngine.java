package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.relics.djinn.DjinnPhaseRules.EndReason;
import com.newuniverse.nusmp.relics.djinn.DjinnPhaseRules.Verdict;
import com.newuniverse.nusmp.relics.djinn.dark.DarkDjinn;
import com.newuniverse.nusmp.relics.djinn.dark.DarkRules;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The phases of a Djinn, on the server (the numbers are {@link DjinnPhaseRules}'s). A key press arrives as {@link DjinnNet.Act}; the engine asks the
 * rules whether the move is allowed, pays the Magoi, and puts the bearer in the new phase. Once a second it feeds the phase (the upkeep), checks that
 * the vessel is still on the bearer and the Djinn still theirs, grows assimilation and trust, and ends what has run out. Every effect that can leak is
 * a transient attribute modifier or a flag that is undone at the end, on death, on logout and on the next login ({@link #restore}).
 *
 * <p>Full Djinn Equip (Maso): attribute boosts, an EP spike (Tensura's max magicule and aura +25%: its EP follows both), and a bounded, safe flight
 * (the ability to fly is given only if the player did not already have it and taken back at the end, with Slow Falling so the landing is never
 * the backlash). At most 90 s; it ends in a backlash (Slowness, Weakness, Nausea, Hunger) and a lockout.
 */
public final class DjinnEngine {
    private DjinnEngine() {}

    /** The things a bearer has going that are not saved: an Extreme Magic charging, the sync state, the key spacing. */
    static final class Run {
        boolean charging;
        long chargeStart;
        int step;
        Djinn djinn;
        Vec3 centre = Vec3.ZERO, dir = new Vec3(0, 0, 1);
        List<Vec3> points = List.of();
        final Map<UUID, Integer> hits = new HashMap<>();
        long lastKey = -100;
        int lastSig = Integer.MIN_VALUE;
        int pulse;
        boolean dark;                 // the Extreme Magic charging was begun by a dark Djinn (fixed at the start)
        double paid;                  // the Magoi share the Extreme Magic charging cost (what a cancelled one gives back)
        boolean rukh;
        long severedAt = -1;          // 1.45.0: the tick the god-tier Lucius severed this bearer, or -1
        long rukhStart;
        Vec3 rukhAt = Vec3.ZERO;
    }

    private static final Map<UUID, Run> RUNS = new HashMap<>();

    static Run run(ServerPlayer p) { return RUNS.computeIfAbsent(p.getUUID(), k -> new Run()); }

    static boolean charging(ServerPlayer p) {
        Run r = RUNS.get(p.getUUID());
        return r != null && r.charging;
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    private static final ResourceLocation STRAIN = id("djinn_strain"), VESSEL_REGEN = id("djinn_vessel_regen");
    private static final ResourceLocation M_SPEED = id("djinn_maso_speed"), M_ATTACK = id("djinn_maso_attack"), M_ARMOR = id("djinn_maso_armor"),
            M_TOUGH = id("djinn_maso_toughness"), M_KB = id("djinn_maso_knockback"), M_HEALTH = id("djinn_maso_health"), M_MAGICULE = id("djinn_maso_magicule"),
            M_AURA = id("djinn_maso_aura"), M_ELEMENT = id("djinn_maso_element"), M_REGEN = id("djinn_maso_noregen");

    // ================================================================ the tick
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        int t = p.tickCount;
        if ((t & 3) != 0) return;
        if (com.newuniverse.nusmp.relics.Severance.isSevered(p)) return;               // 1.45.0: severed by the god-tier Lucius: no upkeep, no skills lent, no sync; see severed(p, on)
        DjinnState s = DjinnState.of(p);
        Run r = RUNS.get(p.getUUID());
        boolean charging = r != null && r.charging, drawing = r != null && r.rukh;
        if (s.phase() == DjinnPhaseRules.OFF && !charging && !drawing) {
            if (t % 80 == 0) idle(p, s);
            return;
        }
        if (r == null) r = run(p);
        long now = p.level().getGameTime();
        if (!p.isAlive()) return;
        if (r.rukh) rukhStep(p, s, r, now);
        if (r.pulse > 0) r.pulse = Math.max(0, r.pulse - 4);
        if (r.charging) chargeStep(p, s, r, now);
        if (s.phase() >= DjinnPhaseRules.HALF && t % 8 == 0) trail(p, s);
        Djinn d = s.active();
        if (d != null && s.phase() == DjinnPhaseRules.LESSER && t % 40 == 0) DjinnMagic.lesserBolt(p, d);
        if (((t >> 2) + p.getId()) % 5 == 0) second(p, s, r, now);
    }

    /** Every four seconds while nothing is out: the bond is still real, assimilation recedes, and the owner's bar is refreshed. */
    private static void idle(ServerPlayer p, DjinnState s) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        syncStrain(p, s);
        Djinn d = s.djinn();
        syncVesselRegen(p, s);
        if (d != null) {
            if (!p.getUUID().equals(DjinnLedger.get(server).owner(d))) { lose(p, s, "Your Djinn is no longer bound to you."); return; }
        } else if (s.lent()) {
            Djinn l = s.lentDjinn();
            if (l == null || !DjinnLedger.get(server).isMember(l, p.getUUID())) { lose(p, s, "The Djinn you were lent has returned to its master."); return; }
        }
        if (s.owns() || s.lent()) {
            float a = s.assimilation();
            if (a > 0f) s.setAssimilation(a - DjinnPhaseRules.assimilationDecayPerSecond(s.lent()) * 4f);
            syncMark(p, s);
            cleanStale(p, s);
            sync(p, s, run(p), p.level().getGameTime(), false);
        }
    }

    private static void second(ServerPlayer p, DjinnState s, Run r, long now) {
        Djinn d = s.active();
        MinecraftServer server = p.getServer();
        if (d == null || server == null) { shutdown(p, EndReason.ADMIN); return; }
        boolean lent = s.lent();
        DjinnLedger ledger = DjinnLedger.get(server);
        if (lent) {
            if (!ledger.isMember(d, p.getUUID())) { lose(p, s, "The Djinn you were lent has returned to its master."); return; }
        } else {
            if (!p.getUUID().equals(ledger.owner(d))) { lose(p, s, "Your Djinn is no longer bound to you."); return; }
            if (VesselAccess.findBound(p, s.serial()).isEmpty()) {
                p.displayClientMessage(Component.literal("Your vessel left you: the Djinn's link breaks.").withStyle(ChatFormatting.RED), true);
                stepDown(p, s, DjinnPhaseRules.OFF, EndReason.VESSEL_LOST);
                return;
            }
        }
        int ph = s.phase();
        if (ph == DjinnPhaseRules.OFF) { sync(p, s, r, now, false); return; }
        int mask = Bonds.mask(p);
        boolean multi = Bonds.multiBond(p);
        boolean dark = DarkDjinn.active(s);
        double factor = DarkRules.upkeep(DjinnPhaseRules.drainFactor(mask, multi, s.assimilation()), dark);
        if (!Magoi.pay(p, DjinnPhaseRules.upkeepFraction(ph) * factor, DjinnPhaseRules.FLOOR)) {
            p.displayClientMessage(Component.literal("The Djinn starves: your Magoi is spent.").withStyle(ChatFormatting.RED), true);
            com.newuniverse.nusmp.mana.ManaLaw.exhaust(p);
            stepDown(p, s, DjinnPhaseRules.OFF, EndReason.DEPLETED);
            return;
        }
        float before = s.assimilation();
        s.setAssimilation(before + DarkRules.assimilationRate(DjinnPhaseRules.assimilationPerSecond(ph, lent), dark));
        assimilationNotes(p, s, before, lent);
        if (lent && s.assimilation() >= DjinnPhaseRules.HOUSEHOLD_CAP) {
            p.displayClientMessage(Component.literal("The Djinn's nature has taken half of you: it withdraws.").withStyle(ChatFormatting.DARK_RED), true);
            stepDown(p, s, DjinnPhaseRules.OFF, EndReason.VOLUNTARY);
            return;
        }
        if (!lent && s.assimilation() >= DjinnPhaseRules.OWNER_CAP && ph >= DjinnPhaseRules.HALF) {
            p.displayClientMessage(Component.literal("The Djinn has too much of you. It will not lend more than a whisper until it recedes.").withStyle(ChatFormatting.DARK_RED), true);
            stepDown(p, s, DjinnPhaseRules.LESSER, EndReason.VOLUNTARY);
            return;
        }
        if (ph >= DjinnPhaseRules.HALF) {
            int a = s.activeTicks() + 20;
            if (a >= DjinnPhaseRules.TRUST_EVERY_TICKS) {
                a = 0;
                s.setTrust(s.trust() + Bonds.scaleTrustGain(p, 1));
            }
            s.setActiveTicks(a);
        }
        if (ph == DjinnPhaseRules.LESSER && now >= s.end()) {
            stepDown(p, s, DjinnPhaseRules.OFF, EndReason.VOLUNTARY);
            return;
        }
        if (ph == DjinnPhaseRules.FULL) {
            s.setUsed(s.used() + 20);
            if (s.used() >= DjinnPhaseRules.FULL_MAX_TICKS) {
                p.displayClientMessage(Component.literal("Ninety seconds: the Maso breaks.").withStyle(ChatFormatting.RED), true);
                stepDown(p, s, DjinnPhaseRules.OFF, EndReason.TIMEOUT);
                return;
            }
            ensureFlight(p, s);
        }
        syncMark(p, s);
        syncStrain(p, s);
        syncVesselRegen(p, s);
        List<String> skills = new ArrayList<>(DjinnSkills.forPhase(d.affinity(), Math.min(ph, DjinnPhaseRules.FULL)));
        if (dark) skills.addAll(DarkRules.skills(Math.min(ph, DjinnPhaseRules.FULL)));
        if (!skills.isEmpty()) com.newuniverse.nusmp.relics.Fx.lendSkills(p, DjinnSkills.LEASE_TICKS, skills.toArray(new String[0]));
        DarkDjinn.second(p, s, ph);
        sync(p, s, r, now, true);
    }

    private static void assimilationNotes(ServerPlayer p, DjinnState s, float before, boolean lent) {
        float after = s.assimilation();
        if (!lent && before < DjinnPhaseRules.MARK_AT && after >= DjinnPhaseRules.MARK_AT)
            p.sendSystemMessage(Component.literal("The Djinn's mark has surfaced on your skin. It is half in you now.").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (!lent && before < DjinnPhaseRules.OWNER_STRAIN && after >= DjinnPhaseRules.OWNER_STRAIN)
            p.sendSystemMessage(Component.literal("The Djinn wants more of you: everything you feed it now costs a quarter more.").withStyle(ChatFormatting.DARK_RED));
        if (lent && before < 25f && after >= 25f)
            p.sendSystemMessage(Component.literal("Assimilation: a quarter. The borrowed Djinn leaves its mark on you.").withStyle(ChatFormatting.YELLOW));
    }

    // ================================================================ 1.45.0: severance
    /**
     * The god-tier Lucius's Spirit/Djinn Severance begins ({@code on}) or ends for this bearer. Nothing is destroyed: on the start a running
     * Rukh or Extreme Magic charge is let go (the Magoi is refunded), the Maso's attribute boosts and its flight are taken off while the
     * phase and the bond stay as they are, and the tick above does nothing; on the end the boosts and flight are put back, a Lesser Summon's
     * clock is moved on by the time lost, and the client is told. Called by the Lucius capabilities once per change; idempotent.
     */
    public static void severed(ServerPlayer p, boolean on) {
        DjinnState s = DjinnState.of(p);
        Run r = run(p);
        long now = p.level().getGameTime();
        if (on) {
            if (r.severedAt >= 0) return;
            r.severedAt = now;
            if (r.charging) cancelCharge(p, s, r, true);
            if (r.rukh) endRukh(p, r, "The Rukh slips away: you are severed.");
            if (s.phase() >= DjinnPhaseRules.FULL) {
                removeMaso(p);
                if (s.flightGranted()) revokeFlight(p, s, false);
            }
            removeMark(p);
            return;
        }
        if (r.severedAt < 0) return;
        long lost = Math.max(0, now - r.severedAt);
        r.severedAt = -1;
        Djinn d = s.active();
        if (d != null && s.phase() >= DjinnPhaseRules.FULL) {
            applyMaso(p, s, d);
            ensureFlight(p, s);
        }
        if (d != null && s.phase() == DjinnPhaseRules.LESSER && s.end() > 0) s.setSpan(s.start(), s.end() + lost);
        if (s.owns() || s.lent()) syncMark(p, s);
        r.lastSig = Integer.MIN_VALUE;
        sync(p, s, r, now, true);
    }

    // ================================================================ keys
    /** A key press from the client. */
    public static void onKey(ServerPlayer p, int action) {
        if (action < DjinnNet.ACT_UP || action > DjinnNet.ACT_RUKH) return;                          // never trust the client: only the four known keys
        if (!p.isAlive() || p.isSpectator()) return;
        if (com.newuniverse.nusmp.relics.Severance.isSevered(p)) {                      // 1.45.0
            p.displayClientMessage(Component.literal("Your Djinn is severed from you for a moment.").withStyle(ChatFormatting.DARK_PURPLE), true);
            return;
        }
        Run r = run(p);
        long now = p.level().getGameTime();
        if (now - r.lastKey < DjinnPhaseRules.KEY_SPACING) return;
        r.lastKey = now;
        DjinnState s = DjinnState.of(p);
        if (s.active() == null) {
            p.displayClientMessage(Component.literal("You hold no Djinn. Bind a Djinn Seal to a Legend-grade vessel.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        int ph = s.phase();
        if (action == DjinnNet.ACT_RUKH) { toggleRukh(p, s, r, now); return; }
        if (action == DjinnNet.ACT_OFF) { go(p, DjinnPhaseRules.OFF, false); return; }
        if (action == DjinnNet.ACT_DOWN) {
            if (r.charging) { go(p, DjinnPhaseRules.FULL, false); return; }
            go(p, Math.max(0, ph - 1), false);
            return;
        }
        if (r.charging) return;
        go(p, ph >= DjinnPhaseRules.FULL ? DjinnPhaseRules.EXTREME : ph + 1, false);
    }

    /** Moves the bearer to {@code target} (0 off, 1-3 phases, 4 the Extreme Magic) if the rules allow it. {@code admin} skips cost, trust and waits. */
    static boolean go(ServerPlayer p, int target, boolean admin) {
        DjinnState s = DjinnState.of(p);
        Run r = run(p);
        long now = p.level().getGameTime();
        if (target == DjinnPhaseRules.FULL && r.charging && s.phase() == DjinnPhaseRules.FULL) { cancelCharge(p, s, r, true); return true; }
        if (target <= DjinnPhaseRules.OFF) { stepDown(p, s, DjinnPhaseRules.OFF, EndReason.VOLUNTARY); return true; }
        if (target < s.phase()) { stepDown(p, s, target, EndReason.VOLUNTARY); return true; }
        if (target == s.phase()) return true;
        Verdict v = admin ? adminVerdict(s, target) : DjinnPhaseRules.request(snapshot(p, s, now), target);
        if (v != Verdict.OK) { refuse(p, s, v, target); return false; }
        enter(p, s, r, target, now, admin);
        return true;
    }

    private static Verdict adminVerdict(DjinnState s, int target) {
        if (s.active() == null) return Verdict.NOT_BOUND;
        if (s.lent() && target > DjinnPhaseRules.LESSER) return Verdict.HOUSEHOLD_LIMIT;
        if (target == DjinnPhaseRules.EXTREME && s.phase() != DjinnPhaseRules.FULL) return Verdict.NOT_IN_FULL;
        if (target > s.phase() + 1) return Verdict.ORDER;
        return Verdict.OK;
    }

    static DjinnPhaseRules.Snapshot snapshot(ServerPlayer p, DjinnState s, long now) {
        boolean bound = s.owns(), lent = s.lent();
        boolean vesselOn = bound && !VesselAccess.findBound(p, s.serial()).isEmpty();
        int trust = DarkRules.gateTrust(s.trust(), DjinnPhaseRules.EXTREME_TRUST, DarkDjinn.active(s));
        return new DjinnPhaseRules.Snapshot(s.phase(), bound, lent, vesselOn, trust, s.assimilation(), Magoi.fraction(p), now,
                s.lockoutUntil(), s.lesserReadyAt(), s.extremeReadyAt(), p.isCreative());
    }

    private static void refuse(ServerPlayer p, DjinnState s, Verdict v, int target) {
        long now = p.level().getGameTime();
        String m = switch (v) {
            case NOT_BOUND -> "You hold no Djinn.";
            case NO_VESSEL -> "Your vessel is not on you: wear or hold the relic the Djinn sleeps in.";
            case ORDER -> "Climb one step at a time: Lesser Summon, Half Equip, Full Equip.";
            case NOT_IN_FULL -> "An Extreme Magic can only be cast from a Full Djinn Equip.";
            case LOCKED_OUT -> "The Maso is still closed to you for " + secs(s.lockoutUntil() - now) + ".";
            case COOLDOWN -> target == DjinnPhaseRules.EXTREME ? "The Extreme Magic needs " + secs(s.extremeReadyAt() - now) + " more to gather."
                    : "The Djinn needs " + secs(s.lesserReadyAt() - now) + " more before it answers again.";
            case NEED_TRUST -> "The Djinn does not trust you enough yet: " + s.trust() + " of "
                    + (target == DjinnPhaseRules.EXTREME ? DarkRules.extremeTrustNeeded(DjinnPhaseRules.EXTREME_TRUST, DarkDjinn.active(s)) : DjinnPhaseRules.FULL_TRUST) + ". Use its power in Half Equip to earn it.";
            case NEED_MAGOI -> "Your Magoi is too low: " + (int) Math.ceil(DjinnPhaseRules.requiredFraction(target) * 100) + "% of the bar is needed.";
            case ASSIMILATED -> s.lent() ? "The Djinn has taken half of you. Rest until it recedes." : "The Djinn has too much of you. Rest until it recedes.";
            case HOUSEHOLD_LIMIT -> "A borrowed Djinn gives you only the Lesser Summon.";
            default -> "Not now.";
        };
        p.displayClientMessage(Component.literal(m).withStyle(ChatFormatting.GRAY), true);
    }

    private static String secs(long ticks) {
        long s = Math.max(1, (ticks + 19) / 20);
        return s >= 90 ? (s + 59) / 60 + " min" : s + " s";
    }

    // ================================================================ entering a phase
    private static void enter(ServerPlayer p, DjinnState s, Run r, int target, long now, boolean free) {
        Djinn d = s.active();
        double factor = DjinnPhaseRules.drainFactor(Bonds.mask(p), Bonds.multiBond(p), s.assimilation());
        double cost = target == DjinnPhaseRules.EXTREME ? DarkRules.extremePrice(DjinnPhaseRules.entryFraction(target), DarkDjinn.active(s)) : DjinnPhaseRules.entryFraction(target) * factor;
        if (!free && !Magoi.pay(p, cost, DjinnPhaseRules.FLOOR)) {
            p.displayClientMessage(Component.literal("Your Magoi cannot pay for it.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (target == DjinnPhaseRules.EXTREME) r.paid = free ? 0.0 : cost;
        ServerLevel l = p.serverLevel();
        switch (target) {
            case DjinnPhaseRules.LESSER -> {
                s.setPhase(DjinnPhaseRules.LESSER);
                s.setSpan(now, now + DjinnPhaseRules.LESSER_TICKS);
                s.setActiveTicks(0);
                DjinnMagic.flare(p, d, DjinnFx.particle(d.affinity()));
                l.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4f, 1.3f);
                p.displayClientMessage(Component.literal(d.displayName() + " answers: a small shape of " + d.affinity().label.toLowerCase() + " at your shoulder.").withColor(d.color()), true);
            }
            case DjinnPhaseRules.HALF -> {
                s.setPhase(DjinnPhaseRules.HALF);
                s.setSpan(now, 0);
                DjinnMagic.flare(p, d, DjinnFx.particle(d.affinity()));
                l.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2f, 1.4f);
                p.displayClientMessage(Component.literal("Djinn Weapon: " + d.displayName() + "'s " + d.affinity().label.toLowerCase() + " is in your blows. " + d.trait().blurb()).withColor(d.color()), true);
                com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponService.apply(p);                                                       // the real temporary weapon (relics.djinn.weapon): the displaced item is kept safe
            }
            case DjinnPhaseRules.FULL -> {
                s.setPhase(DjinnPhaseRules.FULL);
                s.setSpan(now, now + DjinnPhaseRules.FULL_MAX_TICKS);
                s.setUsed(0);
                applyMaso(p, s, d);
                ensureFlight(p, s);
                com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponService.apply(p);                                                       // the Half Equip's weapon stays: the Full Equip opens its second act
                DjinnMagic.flare(p, d, DjinnFx.particle(d.affinity()));
                DjinnFx.ring(l, p.position(), 3.5, d.glow(), 48, 0.2);
                l.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 0.7f);
                l.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.6f, 1.2f);
                DjinnFx.bar(p, 48, p.getName().getString() + " becomes " + d.formName() + ".", d.glow());
                p.sendSystemMessage(Component.literal("Full Djinn Equip: " + d.formName() + ". Ninety seconds. Fly with the jump key twice.").withColor(d.color()));
            }
            case DjinnPhaseRules.EXTREME -> beginExtreme(p, s, r, d, now);
            default -> { }
        }
        sync(p, s, r, now, true);
    }

    // ================================================================ leaving a phase
    /** Steps down to {@code target}, ending a Full Equip properly (backlash, lockout, trust) if that is what is left. */
    static void stepDown(ServerPlayer p, DjinnState s, int target, EndReason why) {
        Run r = run(p);
        long now = p.level().getGameTime();
        int from = s.phase();
        if (r.charging) cancelCharge(p, s, r, why == EndReason.VOLUNTARY);
        if (from >= DjinnPhaseRules.FULL && target < DjinnPhaseRules.FULL) endMaso(p, s, why);
        if (from > target) {
            s.setPhase(target);
            if (target == DjinnPhaseRules.OFF) {
                s.setSpan(0, 0);
                s.setActiveTicks(0);
                long wait = from == DjinnPhaseRules.LESSER ? DjinnPhaseRules.LESSER_COOLDOWN : 100;
                s.setLesserReadyAt(Math.max(s.lesserReadyAt(), now + wait));
            }
            Djinn d = s.active();
            if (d != null && target < from) p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
        if (from > target && target < DjinnPhaseRules.HALF) com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponService.release(p);   // below the Half Equip the weapon goes and the displaced item returns
        syncMarkSafe(p, s);
        sync(p, s, r, now, true);
    }

    private static void syncMarkSafe(ServerPlayer p, DjinnState s) {
        try { syncMark(p, s); } catch (RuntimeException ignored) { }
    }

    private static void endMaso(ServerPlayer p, DjinnState s, EndReason why) {
        long now = p.level().getGameTime();
        int used = s.used();
        removeMaso(p);
        revokeFlight(p, s, true);
        double sev = DarkRules.backlash(DjinnPhaseRules.severity(why, used), DarkDjinn.active(s), why == EndReason.VOLUNTARY || why == EndReason.TIMEOUT || why == EndReason.DEPLETED);
        int lock = DjinnPhaseRules.lockoutTicks(why, used);
        if (lock > 0) s.setLockoutUntil(Math.max(s.lockoutUntil(), now + lock));
        int delta = DjinnPhaseRules.trustDelta(why, used);
        if (delta != 0) s.setTrust(s.trust() + (delta > 0 ? Bonds.scaleTrustGain(p, delta) : delta));
        s.setUsed(0);
        s.setSpan(0, 0);
        DjinnPhaseRules.Backlash b = DjinnPhaseRules.backlash(sev);
        if (!b.none()) applyBacklash(p, b);
        if (why != EndReason.DEATH && why != EndReason.LOGOUT) {
            p.sendSystemMessage(Component.literal("The Maso ends." + (b.none() ? "" : " The backlash hits you.") + (lock > 0 ? " It stays closed for " + secs(lock) + "." : "")).withStyle(ChatFormatting.GRAY));
            Djinn d = s.active();
            if (d != null) DjinnMagic.flare(p, d, DjinnFx.particle(d.affinity()));
        }
    }

    private static void applyBacklash(ServerPlayer p, DjinnPhaseRules.Backlash b) {
        if (p.isCreative()) return;
        if (b.slowTicks() > 0) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, b.slowTicks(), b.slowAmp()));
        if (b.weakTicks() > 0) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, b.weakTicks(), b.weakAmp()));
        if (b.nauseaTicks() > 0) p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, b.nauseaTicks(), 0));
        if (b.hungerTicks() > 0) p.addEffect(new MobEffectInstance(MobEffects.HUNGER, b.hungerTicks(), 1));
        if (b.foodLoss() > 0) p.getFoodData().setFoodLevel(Math.max(1, p.getFoodData().getFoodLevel() - b.foodLoss()));
        if (b.blindTicks() > 0) p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, b.blindTicks(), 0));
        if (b.healthLoss() > 0) {
            DjinnState s = DjinnState.of(p);
            s.setStrain(p.level().getGameTime() + b.strainTicks(), b.healthLoss(), b.strainTicks());
            syncStrain(p, s);
        }
    }

    /**
     * The strain of a hard backlash: maximum health taken away by a transient attribute modifier whose size follows the time still to run, so it
     * heals back by itself. The end time is in the player's record, so a relog or a death does not wash it away (nor make it permanent).
     */
    static void syncStrain(ServerPlayer p, DjinnState s) {
        AttributeInstance inst = p.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) return;
        long left = s.strainUntil() - p.level().getGameTime();
        double amount = DjinnPhaseRules.strainAmount(s.strainLoss(), left, s.strainTotal());
        AttributeModifier old = inst.getModifier(STRAIN);
        if (amount == 0.0) {
            if (old != null) { inst.removeModifier(STRAIN); if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth()); }
            if (s.strainUntil() != 0L && left <= 0) s.clearStrain();
            return;
        }
        if (old != null && Math.abs(old.amount() - amount) < 0.2) return;
        if (old != null) inst.removeModifier(STRAIN);
        inst.addTransientModifier(new AttributeModifier(STRAIN, amount, Operation.ADD_VALUE));
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** The bonded vessel on the body feeds the magicule store: a Heritage-style +25% on Tensura's natural magicule regeneration while it is held or worn. */
    static void syncVesselRegen(ServerPlayer p, DjinnState s) {
        AttributeInstance inst = p.getAttribute(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER);
        if (inst == null) return;
        boolean on = s.owns() && !VesselAccess.findBound(p, s.serial()).isEmpty();
        boolean has = inst.hasModifier(VESSEL_REGEN);
        if (on && !has) inst.addTransientModifier(new AttributeModifier(VESSEL_REGEN, DjinnPhaseRules.VESSEL_REGEN, Operation.ADD_MULTIPLIED_BASE));
        else if (!on && has) inst.removeModifier(VESSEL_REGEN);
    }

    // ================================================================ Draw Rukh
    /** Starts or ends the channel: standing still with the bonded vessel on you and no Djinn out, you draw the Rukh of the place into your magicules. */
    private static void toggleRukh(ServerPlayer p, DjinnState s, Run r, long now) {
        if (r.rukh) { endRukh(p, r, "You let the Rukh go."); return; }
        if (!s.owns()) { p.displayClientMessage(Component.literal("Only the bearer of a Djinn can draw the Rukh.").withStyle(ChatFormatting.GRAY), true); return; }
        if (s.phase() != DjinnPhaseRules.OFF || r.charging) { p.displayClientMessage(Component.literal("Dismiss the Djinn first: drawing Rukh needs stillness.").withStyle(ChatFormatting.GRAY), true); return; }
        if (VesselAccess.findBound(p, s.serial()).isEmpty()) { p.displayClientMessage(Component.literal("Hold or wear your vessel to draw the Rukh.").withStyle(ChatFormatting.GRAY), true); return; }
        r.rukh = true;
        r.rukhStart = now;
        r.rukhAt = p.position();
        Djinn d = s.djinn();
        p.displayClientMessage(Component.literal("You draw the Rukh of this place. Stay still.").withColor(d.color()), true);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 0.7f);
    }

    private static void endRukh(ServerPlayer p, Run r, String why) {
        r.rukh = false;
        p.displayClientMessage(Component.literal(why).withStyle(ChatFormatting.GRAY), true);
    }

    /** Every processed tick while channelling: the checks, the pull of dust, and once a second the gain (Tensura's own gain path, scaled by ManaLaw's density). */
    private static void rukhStep(ServerPlayer p, DjinnState s, Run r, long now) {
        Djinn d = s.djinn();
        if (d == null || !p.isAlive()) { r.rukh = false; return; }
        if (s.phase() != DjinnPhaseRules.OFF) { endRukh(p, r, "The Djinn's call breaks the stillness."); return; }
        if (p.position().distanceToSqr(r.rukhAt) > 0.6 || p.hurtTime > 0 || p.isSprinting()) { endRukh(p, r, "The Rukh slips away: you moved or were struck."); return; }
        if (now - r.rukhStart > DjinnPhaseRules.RUKH_MAX_TICKS) { endRukh(p, r, "The Rukh is spent for now."); return; }
        ServerLevel l = p.serverLevel();
        double a = (now - r.rukhStart) * 0.35;
        for (int i = 0; i < 3; i++) {
            double ang = a + i * 2.094, rr = 3.5 - ((now - r.rukhStart + i * 6) % 24) / 24.0 * 3.0;
            com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.Fx.align(d.glow()), p.getX() + Math.cos(ang) * rr, p.getY() + 0.3 + rr * 0.4, p.getZ() + Math.sin(ang) * rr, 1, 0, 0, 0, 0);
        }
        if ((now - r.rukhStart) % 20 < 4) {
            double density = com.newuniverse.nusmp.mana.ManaLaw.density(l, p.blockPosition());
            double gain = io.github.manasmods.tensura.util.EnergyHelper.getMaxMagicule(p) * DjinnPhaseRules.rukhGain(density);
            io.github.manasmods.tensura.util.EnergyHelper.gainMagicule(p, gain, io.github.manasmods.tensura.util.EnergyHelper.GainType.NORMAL);
        }
    }

    /** Ends everything for a player at once (logout, death, an admin): no questions, no lockout beyond the reason's. */
    static void shutdown(ServerPlayer p, EndReason why) {
        DjinnState s = DjinnState.of(p);
        Run r = RUNS.get(p.getUUID());
        if (r != null && r.charging) { r.charging = false; r.hits.clear(); }
        if (r != null) r.rukh = false;
        if (s.phase() >= DjinnPhaseRules.FULL) endMaso(p, s, why);
        else { removeMaso(p); revokeFlight(p, s, false); }
        if (s.phase() != DjinnPhaseRules.OFF) {
            s.setPhase(DjinnPhaseRules.OFF);
            s.setSpan(0, 0);
            s.setActiveTicks(0);
        }
        com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponService.release(p);                                                             // death, logout, an admin, a lost Djinn: the weapon goes, the stash returns
        if (r != null) sync(p, s, r, p.level().getGameTime(), true);
    }

    /** The Djinn is gone from this player (taken back by the ledger): everything ends and the record is cleared. */
    private static void lose(ServerPlayer p, DjinnState s, String message) {
        shutdown(p, EndReason.ADMIN);
        UUID serial = s.serial();
        if (serial != null) VesselAccess.stripEverywhere(p, serial);
        s.clearBond();
        removeMark(p);
        syncVesselRegen(p, s);
        p.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
        sync(p, s, run(p), p.level().getGameTime(), true);
    }

    /** A household member loses the lent Djinn at once (the owner released it, or took it back). */
    static void dropLent(ServerPlayer member) {
        DjinnState s = DjinnState.of(member);
        if (s.lent()) lose(member, s, "The Djinn lent to you has been called back.");
    }

    // ================================================================ the Maso's body
    private static void put(ServerPlayer p, Holder<Attribute> a, ResourceLocation id, double amount, Operation op) {
        if (a == null) return;
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, amount, op));
    }

    private static void drop(ServerPlayer p, Holder<Attribute> a, ResourceLocation id) {
        if (a == null) return;
        AttributeInstance inst = p.getAttribute(a);
        if (inst != null && inst.hasModifier(id)) inst.removeModifier(id);
    }

    private static void applyMaso(ServerPlayer p, DjinnState s, Djinn d) {
        boolean dark = DarkDjinn.active(s);
        put(p, Attributes.MOVEMENT_SPEED, M_SPEED, DarkRules.masoSpeed(DjinnPhaseRules.MASO_SPEED, dark), Operation.ADD_MULTIPLIED_TOTAL);
        put(p, Attributes.ATTACK_DAMAGE, M_ATTACK, DarkRules.masoAttack(DjinnPhaseRules.MASO_ATTACK, dark), Operation.ADD_VALUE);
        put(p, Attributes.ARMOR, M_ARMOR, DjinnPhaseRules.MASO_ARMOR, Operation.ADD_VALUE);
        put(p, Attributes.ARMOR_TOUGHNESS, M_TOUGH, DjinnPhaseRules.MASO_TOUGHNESS, Operation.ADD_VALUE);
        put(p, Attributes.KNOCKBACK_RESISTANCE, M_KB, DjinnPhaseRules.MASO_KNOCKBACK, Operation.ADD_VALUE);
        put(p, Attributes.MAX_HEALTH, M_HEALTH, DjinnPhaseRules.MASO_HEALTH, Operation.ADD_VALUE);
        put(p, TensuraAttributes.MAX_MAGICULE, M_MAGICULE, DjinnPhaseRules.MASO_EP, Operation.ADD_MULTIPLIED_TOTAL);
        put(p, TensuraAttributes.MAX_AURA, M_AURA, DjinnPhaseRules.MASO_EP, Operation.ADD_MULTIPLIED_TOTAL);
        switch (d.affinity()) {
            case STRENGTH -> put(p, Attributes.ATTACK_DAMAGE, M_ELEMENT, DarkRules.masoElementFlat(3.0, DjinnPhaseRules.MASO_ELEMENT, dark), Operation.ADD_VALUE);
            case LIFE -> put(p, Attributes.MAX_HEALTH, M_ELEMENT, DarkRules.masoElementFlat(6.0, DjinnPhaseRules.MASO_ELEMENT, dark), Operation.ADD_VALUE);
            default -> put(p, DjinnFx.boost(d.affinity()), M_ELEMENT, DarkRules.masoElement(DjinnPhaseRules.MASO_ELEMENT, dark), Operation.ADD_MULTIPLIED_BASE);
        }
        if (BondRules.regenBlocked(Bonds.mask(p), Bonds.multiBond(p), DjinnPhaseRules.FULL))
            put(p, TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER, M_REGEN, -1.0, Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void removeMaso(ServerPlayer p) {
        boolean had = hasMaso(p);
        drop(p, Attributes.MOVEMENT_SPEED, M_SPEED);
        drop(p, Attributes.ATTACK_DAMAGE, M_ATTACK);
        drop(p, Attributes.ATTACK_DAMAGE, M_ELEMENT);
        drop(p, Attributes.ARMOR, M_ARMOR);
        drop(p, Attributes.ARMOR_TOUGHNESS, M_TOUGH);
        drop(p, Attributes.KNOCKBACK_RESISTANCE, M_KB);
        drop(p, Attributes.MAX_HEALTH, M_HEALTH);
        drop(p, Attributes.MAX_HEALTH, M_ELEMENT);
        drop(p, TensuraAttributes.MAX_MAGICULE, M_MAGICULE);
        drop(p, TensuraAttributes.MAX_AURA, M_AURA);
        drop(p, TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER, M_REGEN);
        for (Affinity a : Affinity.values()) drop(p, DjinnFx.boost(a), M_ELEMENT);
        if (had) {
            if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
            Magoi.clamp(p);
        }
    }

    private static boolean hasMaso(ServerPlayer p) {
        AttributeInstance a = p.getAttribute(Attributes.MOVEMENT_SPEED);
        return a != null && a.hasModifier(M_SPEED);
    }

    /** The body the Djinn has made: the resistance and the mark that assimilation brings, kept in line with it (Tensura attribute modifiers). */
    private static void syncMark(ServerPlayer p, DjinnState s) {
        DjinnBody.syncTransient(p, s.djinn(), s.assimilation());
    }

    private static void removeMark(ServerPlayer p) { DjinnBody.syncTransient(p, null, 0f); }

    // ================================================================ flight
    private static void ensureFlight(ServerPlayer p, DjinnState s) {
        if (p.isCreative() || p.isSpectator()) return;
        Abilities ab = p.getAbilities();
        boolean changed = false;
        if (!ab.mayfly) { ab.mayfly = true; s.setFlightGranted(true); changed = true; }
        if (s.flightGranted() && ab.getFlyingSpeed() != DjinnPhaseRules.MASO_FLY_SPEED) { ab.setFlyingSpeed(DjinnPhaseRules.MASO_FLY_SPEED); changed = true; }
        if (changed) p.onUpdateAbilities();
    }

    /** Takes the flight back (only if the Maso gave it), and gives Slow Falling for the landing. */
    private static void revokeFlight(ServerPlayer p, DjinnState s, boolean landing) {
        if (!s.flightGranted()) return;
        s.setFlightGranted(false);
        if (p.isCreative() || p.isSpectator()) return;
        Abilities ab = p.getAbilities();
        ab.mayfly = false;
        ab.flying = false;
        ab.setFlyingSpeed(0.05f);
        p.onUpdateAbilities();
        if (landing) p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, DjinnPhaseRules.MASO_LANDING_TICKS, 0, true, false));
    }

    /** Anything left over from a crash or a missed event: modifiers, flight. */
    private static void cleanStale(ServerPlayer p, DjinnState s) {
        if (s.phase() < DjinnPhaseRules.FULL) {
            if (hasMaso(p)) removeMaso(p);
            if (s.flightGranted()) revokeFlight(p, s, false);
        }
    }

    // ================================================================ trail
    private static void trail(ServerPlayer p, DjinnState s) {
        Djinn d = s.djinn();
        if (d == null) return;
        ServerLevel l = p.serverLevel();
        boolean full = s.phase() >= DjinnPhaseRules.FULL;
        Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(0.8)).add(0, -0.5, 0);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.Fx.align(d.glow()), hand.x, hand.y, hand.z, 2, 0.1, 0.1, 0.1, 0.0);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.djinn.DjinnFx.align(d.affinity()), hand.x, hand.y, hand.z, 1, 0.12, 0.12, 0.12, 0.01);
        if (full) {
            com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.djinn.DjinnFx.align(d.affinity()), p.getX(), p.getY() + 1.0, p.getZ(), 3, 0.4, 0.7, 0.4, 0.02);
            com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.Fx.align(d.color()), p.getX(), p.getY() + 0.2, p.getZ(), 2, 0.3, 0.1, 0.3, 0.0);
        }
    }

    // ================================================================ Extreme Magic
    private static void beginExtreme(ServerPlayer p, DjinnState s, Run r, Djinn d, long now) {
        r.charging = true;
        r.chargeStart = now;
        r.step = 0;
        r.djinn = d;
        r.dark = DarkDjinn.active(s);
        r.centre = p.position();
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        r.dir = look.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : look.normalize();
        r.points = DjinnMagic.points(d, r.centre, r.dir, p.getRandom());
        r.hits.clear();
        s.setExtremeReadyAt(now + DjinnPhaseRules.EXTREME_COOLDOWN);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DjinnPhaseRules.EXTREME_CHARGE + 4, 4, true, false));
        DjinnMagic.flare(p, d, DjinnFx.particle(d.affinity()));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 3.0f, 0.5f);
        DjinnFx.bar(p, 64, d.extreme().name(), d.glow());
        p.sendSystemMessage(Component.literal("Extreme Magic: " + d.extreme().name() + ". " + d.extreme().blurb() + " Three seconds.").withColor(d.color()));
        DjinnMagic.telegraph(p, r, 0);
    }

    private static void chargeStep(ServerPlayer p, DjinnState s, Run r, long now) {
        if (!p.isAlive() || s.phase() < DjinnPhaseRules.FULL || r.djinn == null) { r.charging = false; r.hits.clear(); return; }
        long age = now - r.chargeStart;
        if (age < DjinnPhaseRules.EXTREME_CHARGE) { DjinnMagic.telegraph(p, r, age); return; }
        int steps = DjinnMagic.steps(r.djinn.extreme().shape());
        if (age < DjinnPhaseRules.EXTREME_CHARGE + (long) r.step * 4) return;
        DjinnMagic.fire(p, r, r.step);
        if (r.step == 0) {
            r.pulse = 20;
            sync(p, s, r, now, true);
        }
        r.step++;
        if (r.step >= steps) {
            r.charging = false;
            r.hits.clear();
            s.setAssimilation(s.assimilation() + DarkRules.assimilationRate(DjinnPhaseRules.EXTREME_ASSIMILATION, r.dark));
            p.displayClientMessage(Component.literal(r.djinn.extreme().name() + "!").withColor(r.djinn.glow()), true);
            sync(p, s, r, now, true);
        }
    }

    /** Stops a charging Extreme Magic before it strikes; {@code refund} gives back the Magoi and the cooldown. */
    private static void cancelCharge(ServerPlayer p, DjinnState s, Run r, boolean refund) {
        if (!r.charging) return;
        boolean struck = p.level().getGameTime() - r.chargeStart >= DjinnPhaseRules.EXTREME_CHARGE;
        r.charging = false;
        r.hits.clear();
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (refund && !struck) {
            Magoi.refund(p, r.paid);
            s.setExtremeReadyAt(p.level().getGameTime());
            p.displayClientMessage(Component.literal("You let the Extreme Magic go. The Magoi returns.").withStyle(ChatFormatting.GRAY), true);
        }
    }

    // ================================================================ sync
    /** The flags of the sync packet: bit 0 (1) a lent Djinn, bit 1 (2) the Djinn's mark, bit 3 (8) the Djinn is dark. Bit 2 (4) is the client's own. */
    static int flags(DjinnState s) {
        return (s.lent() ? 1 : 0) | (s.assimilation() >= DjinnPhaseRules.MARK_AT ? 2 : 0) | (DarkDjinn.active(s) ? DjinnNet.FLAG_DARK : 0);
    }

    /** For the Dark Djinn: whatever depends on the dark flag is put right (the Maso numbers) and the clients are told. */
    public static void refresh(ServerPlayer p) {
        DjinnState s = DjinnState.of(p);
        Djinn d = s.active();
        if (d != null && s.phase() >= DjinnPhaseRules.FULL) applyMaso(p, s, d);
        com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponService.apply(p);                                                               // light or dark: the weapon takes the look of the Djinn now
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        sync(p, s, run(p), p.level().getGameTime(), true);
    }

    static void sync(ServerPlayer p, DjinnState s, Run r, long now, boolean force) {
        Djinn d = s.active();
        int phase = r.charging ? DjinnPhaseRules.EXTREME : s.phase();
        int left = 0;
        if (phase == DjinnPhaseRules.EXTREME) left = (int) Math.max(0, DjinnPhaseRules.EXTREME_CHARGE - (now - r.chargeStart));
        else if (phase == DjinnPhaseRules.LESSER) left = (int) Math.max(0, s.end() - now);
        else if (phase == DjinnPhaseRules.FULL) left = Math.max(0, DjinnPhaseRules.FULL_MAX_TICKS - s.used());
        int flags = flags(s);
        DjinnNet.Sync pkt = new DjinnNet.Sync(p.getId(), phase, d == null ? 0 : d.ordinal() + 1, left, Mth.clamp((int) (Magoi.fraction(p) * 100), 0, 100),
                Mth.clamp((int) (s.assimilation() * 10), 0, 1000), Mth.clamp(s.trust(), 0, 100), (int) Math.max(0, s.lockoutUntil() - now),
                (int) Math.max(0, s.extremeReadyAt() - now), r.pulse, flags);
        int sig = phase * 31 + (d == null ? 0 : d.ordinal() + 1) * 7 + flags * 3 + (int) (s.assimilation() / 5) * 11 + s.trust() * 13 + (r.pulse > 0 ? 17 : 0);
        if (!force && sig == r.lastSig) return;
        boolean changed = sig != r.lastSig;
        r.lastSig = sig;
        if (phase >= DjinnPhaseRules.LESSER || changed) PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, pkt);
        else PacketDistributor.sendToPlayer(p, pkt);
    }

    /** A new watcher sees the player: tell them what the player has out. */
    public static void onStartTracking(net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking e) {
        if (!(e.getEntity() instanceof ServerPlayer watcher) || !(e.getTarget() instanceof ServerPlayer target)) return;
        DjinnState s = DjinnState.of(target);
        if (s.phase() == DjinnPhaseRules.OFF && s.assimilation() < DjinnPhaseRules.MARK_AT && !DarkDjinn.active(s)) return;
        Run r = run(target);
        long now = target.level().getGameTime();
        Djinn d = s.active();
        int flags = flags(s);
        PacketDistributor.sendToPlayer(watcher, new DjinnNet.Sync(target.getId(), r.charging ? 4 : s.phase(), d == null ? 0 : d.ordinal() + 1, 0, 0,
                Mth.clamp((int) (s.assimilation() * 10), 0, 1000), 0, 0, 0, 0, flags));
    }

    // ================================================================ lifecycle
    /** Login, respawn: put right whatever the last session left, and tell the client. */
    public static void restore(ServerPlayer p) {
        DjinnState s = DjinnState.of(p);
        com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponService.release(p);                                                             // anything a crash left: a stray weapon, a stash
        if (s.phase() != DjinnPhaseRules.OFF) shutdown(p, EndReason.LOGOUT);
        cleanStale(p, s);
        if (s.owns() || s.lent()) {
            // the owner keeps the Djinn through death now: write trust and assimilation back onto the new body (the record's copy is the fallback, a no-op if the modifiers survived)
            if (s.owns()) s.setTrust(s.trust());
            s.setAssimilation(s.assimilation());
            syncMark(p, s);
        }
        syncStrain(p, s);
        syncVesselRegen(p, s);
        RUNS.remove(p.getUUID());
        sync(p, s, run(p), p.level().getGameTime(), true);
    }

    public static void onLogout(ServerPlayer p) {
        DjinnState s = DjinnState.of(p);
        shutdown(p, EndReason.LOGOUT);
        removeMark(p);
        AttributeInstance reg = p.getAttribute(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER);
        if (reg != null && reg.hasModifier(VESSEL_REGEN)) reg.removeModifier(VESSEL_REGEN);
        RUNS.remove(p.getUUID());
        PendingInvites.forget(p.getUUID());
        DjinnMagic.STREAKS.forget(p.getUUID());
    }

    /** Dimension change: the same player object lives on, but the client must be told again. */
    public static void onDimension(ServerPlayer p) {
        Run r = RUNS.get(p.getUUID());
        if (r != null) r.lastSig = Integer.MIN_VALUE;
        sync(p, DjinnState.of(p), run(p), p.level().getGameTime(), true);
    }

    /** The server is closing: nothing is kept. */
    public static void onServerStopping() {
        RUNS.clear();
        PendingInvites.clear();
        DjinnMagic.STREAKS.clear();
    }

    /** Every Djinn-state clock a status command may show: {@code [phase, trust, assimilationTenths, lockLeft, cooldownLeft]}. */
    static int[] summary(ServerPlayer p) {
        DjinnState s = DjinnState.of(p);
        long now = p.level().getGameTime();
        return new int[]{s.phase(), s.trust(), (int) (s.assimilation() * 10), (int) Math.max(0, s.lockoutUntil() - now), (int) Math.max(0, s.extremeReadyAt() - now)};
    }

    /** The vessel stack in use, for the status command. */
    static ItemStack vesselOf(ServerPlayer p) { return VesselAccess.findBound(p, DjinnState.of(p).serial()); }

    /** Players with a Djinn out right now, for the commands (a copy). */
    static List<UUID> active() { return new ArrayList<>(RUNS.keySet()); }
}
