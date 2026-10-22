package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.firecast.FireCasting;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.multiverse.SpiritLordSkill;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Spirit Dive remake: the server half of Fuegoleon's flaming right arm (rules: {@link FuegoleonArmRules}). Once every {@link FuegoleonArmRules#RENEW_TICKS} ticks per player (staggered by entity id)
 * it asks whether the arm is on and, if so, renews the {@code Aura.FUEGOLEON_ARM} lease ({@link FuegoleonArmRules#LEASE_TICKS} ticks; one small packet to the players that track the wearer);
 * when the arm is no longer his it clears the lease at once. The per-player state lives in {@link PlayerAuras} (keyed by player UUID, dropped at logout); nothing is kept here. Every other player pays
 * one modulo and one flag read per check. The client only draws (client.aura.FuegoleonArmAura).
 */
public final class FuegoleonArm {
    private FuegoleonArm() {}

    private static final PerfProbe.Section SECTION = PerfProbe.section("s.spiritdive.arm");        // timed (/multiverse perf): only the wearer's own turn, once every 40 ticks

    /** The wearer's current reason for the arm (NONE when it is off). Reads the bound grimoire first, so every other player exits after one NBT read. */
    public static FuegoleonArmRules.Reason reasonFor(ServerPlayer p) {
        if (!SpiritDiveConfig.fuegoleonArm() || !p.isAlive() || p.isSpectator()) return FuegoleonArmRules.Reason.NONE;
        boolean fuegoleon = CharacterSpells.characterOf(p) == CanonBook.FUEGOLEON;
        if (!fuegoleon) return FuegoleonArmRules.Reason.NONE;
        boolean contract = FuegoleonArmRules.salamander(SpiritLordSkill.bondedType(p));
        boolean linked = FireCasting.salamanderOut(p);
        boolean dive = PlayerAuras.active(p, Aura.SPIRIT_DIVE) && SpiritDivePieces.element(PlayerAuras.style(p, Aura.SPIRIT_DIVE)) == SpiritDivePieces.Element.FIRE;
        return FuegoleonArmRules.reason(true, true, contract, linked, dive, true);
    }

    /** One check: renews or clears the lease. Returns whether the arm is on. */
    public static boolean update(ServerPlayer p) {
        FuegoleonArmRules.Reason r = reasonFor(p);
        try {
            if (r == FuegoleonArmRules.Reason.NONE) {
                if (PlayerAuras.active(p, Aura.FUEGOLEON_ARM)) PlayerAuras.clear(p, Aura.FUEGOLEON_ARM);
                return false;
            }
            boolean wasOn = PlayerAuras.active(p, Aura.FUEGOLEON_ARM);
            PlayerAuras.set(p, Aura.FUEGOLEON_ARM, FuegoleonArmRules.LEASE_TICKS, FuegoleonArmRules.styleOf(r));
            if (!wasOn) p.displayClientMessage(net.minecraft.network.chat.Component.translatableWithFallback("nusmp.spiritdive.arm.on", "The Salamander's flame runs down your right arm.")
                    .withStyle(net.minecraft.ChatFormatting.GOLD), true);
        } catch (NullPointerException noConnection) { /* a stand-in player without a connection: the server table is already written */ }
        return r != FuegoleonArmRules.Reason.NONE;
    }

    /** Game bus: every player tick; only the wearer's own turn does any work. */
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !FuegoleonArmRules.due(p.tickCount, p.getId())) return;
        long t0 = PerfProbe.begin();
        try {
            update(p);
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    /** Game bus: the arm goes with its wearer. */
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !PlayerAuras.active(p, Aura.FUEGOLEON_ARM)) return;
        try { PlayerAuras.clear(p, Aura.FUEGOLEON_ARM); } catch (NullPointerException noConnection) { /* nothing to send */ }
    }
}
