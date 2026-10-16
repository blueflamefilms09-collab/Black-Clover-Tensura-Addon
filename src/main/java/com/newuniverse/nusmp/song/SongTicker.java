package com.newuniverse.nusmp.song;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Why: this is the passive heart of Song Magic. Every server tick, for each singer who has the Song skill with a mode chosen, it
 * reads the singing level the voice bridge published (a plain number, never blocking), and every {@code pulseInterval} ticks it
 * runs one pulse of the mode with a strength from the loudness; it also runs the Crescendo charge-and-release and keeps the HUD
 * meter in sync. Rate limits: one pulse per interval per singer, a per-dimension cap of pulses per tick, and nothing at all for
 * players without the skill or without a mode. When the mana is gone the singer is told once and the song stops working.
 * Register {@link #onPlayerTick}, {@link #onLogout}, {@link #onChangeTarget} and {@link #onDamaged} on the game bus.
 */
public final class SongTicker {
    private static final String LAST_PULSE = "nusmp_song_last", SPENT_UNTIL = "nusmp_song_spent", LAST_SYNC = "nusmp_song_sync";
    private static final Map<ServerLevel, PulseScheduler> BUDGETS = new WeakHashMap<>();
    private static final Map<UUID, CrescendoCharge> CHARGES = new java.util.HashMap<>();

    private SongTicker() {}

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        var data = p.getPersistentData();
        if (data.getInt(SongSkill.CACHE_MODE) == 0) return;                      // no mode: costs one int read per tick
        long now = p.level().getGameTime();
        if (p.tickCount % 20 == 7 && SongSkill.of(p).isEmpty()) {                // the skill was lost: forget the mode
            data.remove(SongSkill.CACHE_MODE);
            sync(p, 0, 0, 255);
            return;
        }
        SongMode mode = SongSkill.cachedMode(p);
        float level = SingingState.level(p.getUUID(), System.currentTimeMillis());
        int charge = 255;

        long cres = data.getLong(SongPages.CRESCENDO_UNTIL);
        CrescendoCharge cc = CHARGES.get(p.getUUID());
        if (now < cres) {
            if (cc == null) CHARGES.put(p.getUUID(), cc = new CrescendoCharge());
            int needed = SongConfig.crescendoTicks();
            var res = cc.tick(level, SongConfig.crescendoLoud(), needed);
            charge = Math.min(100, cc.charge() * 100 / Math.max(1, needed));
            if (res == CrescendoCharge.Result.RELEASE) {
                float peak = cc.peak();
                cc.reset();
                data.putLong(SongPages.CRESCENDO_UNTIL, 0L);
                SongSkill.of(p).ifPresent(i -> {
                    if (!SongEffects.pulse(p, i, mode, SongMath.strength(peak), true)) spent(p, now);
                });
                charge = 255;
            }
        } else if (cc != null) {
            CHARGES.remove(p.getUUID());
            if (cres != 0L) data.putLong(SongPages.CRESCENDO_UNTIL, 0L);
        }

        if (level > 0f && now >= data.getLong(SPENT_UNTIL)
                && PulseScheduler.due(now, data.contains(LAST_PULSE) ? data.getLong(LAST_PULSE) : -1L, SongConfig.pulseInterval())) {
            PulseScheduler budget = BUDGETS.computeIfAbsent(p.serverLevel(), k -> new PulseScheduler());
            if (budget.tryTake(now, SongConfig.pulseCap())) {
                data.putLong(LAST_PULSE, now);
                com.newuniverse.nusmp.quests.NusmpEvents.fireOnce(p, "song.sung");
                ManasSkillInstance inst = SongSkill.of(p).orElse(null);
                if (inst != null && !SongEffects.pulse(p, inst, mode, SongMath.strength(level), false)) spent(p, now);
            }
        }

        if (p.tickCount % 4 == 0) {
            int q = Math.round(level * 100f);
            long packed = ((long) q << 16) | ((long) mode.id << 8) | charge;
            if (packed != data.getLong(LAST_SYNC) || p.tickCount % 40 == 0) {
                data.putLong(LAST_SYNC, packed);
                sync(p, q, mode.id, charge);
            }
        }
    }

    /** Out of mana: say so once, then stay quiet for 5 seconds. */
    private static void spent(ServerPlayer p, long now) {
        p.getPersistentData().putLong(SPENT_UNTIL, now + 100);
        p.displayClientMessage(Component.literal("Your voice gives out: no mana left to sing with.").withStyle(ChatFormatting.GRAY), true);
    }

    private static void sync(ServerPlayer p, int level, int mode, int charge) {
        PacketDistributor.sendToPlayer(p, new SongNet.Meter(level, mode, charge));
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        SingingState.clear(id);
        CHARGES.remove(id);
    }

    /** A creature under a lullaby does not pick a target. */
    public static void onChangeTarget(LivingChangeTargetEvent e) {
        LivingEntity who = e.getEntity();
        if (who instanceof Mob && who.level().getGameTime() < who.getPersistentData().getLong(SongEffects.SLEEP_UNTIL)) e.setCanceled(true);
    }

    /** Any damage wakes a sleeper (the slowness that came with the lullaby goes with it). */
    public static void onDamaged(LivingDamageEvent.Post e) {
        LivingEntity who = e.getEntity();
        if (who.level().isClientSide || e.getNewDamage() <= 0f) return;
        var data = who.getPersistentData();
        if (!data.contains(SongEffects.SLEEP_UNTIL)) return;
        boolean asleep = who.level().getGameTime() < data.getLong(SongEffects.SLEEP_UNTIL);
        data.remove(SongEffects.SLEEP_UNTIL);
        if (asleep) {
            var slow = who.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (slow != null && slow.getAmplifier() == 6) who.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
    }
}
