package com.newuniverse.nusmp.dungeon.sentient;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonConfig;
import com.newuniverse.nusmp.dungeon.DungeonMaw;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Dungeon's immune system. Players who hurt the Dungeon too much are taken for a lethal threat, and the Dungeon answers by birthing a
 * Juggernaut from its walls near them. What counts as hurting it (all of it in {@link ThreatMeter}): blows at its unbreakable walls (the
 * guard refuses them and tells this class), explosions that were refused their blocks, and massive spells (a spell that spends a good share of
 * the caster's magicules, unless they wear the Amulet of the Juggernaut's Blindness). A Wild Magic Surge of the Gambler's Page and the admin
 * command call {@link #aggro} directly. The Dungeon warns first (the walls tremble at half the threshold), a Juggernaut is never sent twice
 * to one player within the cooldown or when three live already, and nothing happens inside a Nexus volume (the boss rooms are sealed from
 * it), for creative or spectating players, or while the Sentient Dungeon is off. The memory of threat is in the server's memory only: it
 * is let go when the player logs out and when the server stops.
 */
public final class ImmuneSystem {
    private ImmuneSystem() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final double NOTICE_RANGE = 48.0, BLAST_BLAME_RANGE = 12.0;
    private static final int TICKS_PER_DAY = 24000;

    private static final class Account {
        final ThreatMeter meter = new ThreatMeter();
        long cooldownUntil, retryAt;
        int warned;
    }

    private static final Map<UUID, Account> ACCOUNTS = new HashMap<>();
    private static final Set<String> WARNED = new HashSet<>();
    /** The day of the last server-wide message about a Juggernaut (the first of each day is announced; the rest are quiet). */
    private static long announcedDay = -1;

    private static Account account(ServerPlayer p) { return ACCOUNTS.computeIfAbsent(p.getUUID(), u -> new Account()); }

    private static void warnOnce(String what, Throwable failure) {
        if (WARNED.add(what + failure.getClass().getName())) LOG.warn("[nusmp] The immune system could not {} (logged once)", what, failure);
    }

    private static long now(ServerPlayer p) { return p.level().getGameTime(); }

    /** Does the immune system answer for this player right now? */
    private static boolean applies(ServerPlayer p) {
        if (!SentientMode.active() || !DungeonConfig.get(DungeonConfig.JUGGERNAUT) || !LivingDungeon.in(p) || !Targets.valid(p)) return false;
        return FloorLookup.nexusAt(p.serverLevel(), p.blockPosition()) == null;      // the boss rooms are sealed from it
    }

    /** Does Relics.masksMana say this player's mana is masked? A missing class (an older build of the relics) never breaks the Dungeon. */
    private static boolean masked(Player p) {
        try {
            return com.newuniverse.nusmp.dungeon.nexus.relic.Relics.masksMana(p);
        } catch (LinkageError missing) {
            return false;
        }
    }

    /** Why the immune system does not answer for this player right now, in words; null if it does ({@code force}: the admin command, which only needs the Dungeon and the switches). */
    static String whyNot(ServerPlayer p, boolean force) {
        if (!SentientMode.active()) return "the Sentient Dungeon is off (dungeon.sentient, dungeon.enabled)";
        if (!DungeonConfig.get(DungeonConfig.JUGGERNAUT)) return "the Juggernaut is switched off (dungeon.juggernaut)";
        if (!LivingDungeon.in(p)) return "you are not in the Dungeon";
        if (force) return null;
        if (!Targets.valid(p)) return "creative, spectating and dead players are ignored";
        if (FloorLookup.nexusAt(p.serverLevel(), p.blockPosition()) != null) return "you stand inside a Nexus: the boss rooms are sealed from the immune system";
        return null;
    }

    /** Is the player's cooldown over, and when does it end (game time)? */
    static long cooldownUntil(ServerPlayer p) {
        Account r = ACCOUNTS.get(p.getUUID());
        return r == null ? 0L : r.cooldownUntil;
    }

    // ---------------------------------------------------------------- what the players do
    /** How much threat the player carries now (0 if the Dungeon has not noticed them). */
    public static float threat(ServerPlayer p) {
        Account r = ACCOUNTS.get(p.getUUID());
        return r == null ? 0f : (float) r.meter.value(now(p));
    }

    /** A refused blow at a wall of the Dungeon (DungeonGuard calls it): a point of threat for every fourth tick of it at most. */
    public static void onWallHit(ServerPlayer p, BlockPos pos) {
        try {
            if (!SentientMode.active() || !LivingDungeon.in(p)) return;
            Account r = account(p);
            long now = now(p);
            if (!r.meter.wallHitDue(now) || !applies(p)) return;
            r.meter.wallHit(now);
            evaluate(p, r, "walls");
        } catch (RuntimeException | LinkageError failure) {
            warnOnce("count a blow at a wall", failure);
        }
    }

    /** An explosion that was refused {@code blocks} blocks of the Dungeon (DungeonGuard calls it): a point a block, one addition for the whole blast. */
    public static void onWallBlast(ServerLevel level, BlockPos at, int blocks, Entity source) {
        try {
            if (!SentientMode.active() || !LivingDungeon.in(level) || blocks <= 0) return;
            ServerPlayer culprit = null;
            if (source instanceof ServerPlayer sp) culprit = sp;
            else if (source == null) culprit = Targets.nearest(level, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, BLAST_BLAME_RANGE);        // a blast nobody is named for: whoever stands nearest it
            if (culprit == null || !applies(culprit)) return;
            Account r = account(culprit);
            r.meter.add(ThreatMeter.blastPoints(blocks), now(culprit));
            evaluate(culprit, r, "blast");
        } catch (RuntimeException | LinkageError failure) {
            warnOnce("count a blast", failure);
        }
    }

    /** A grimoire spell was cast in the Dungeon (DungeonHooks.onCast calls it): a massive one is a lethal threat, unless the caster's mana is masked. */
    public static void onSpell(ServerPlayer p, double costPercent) {
        try {
            if (!SentientMode.active() || !LivingDungeon.in(p)) return;
            double points = ThreatMeter.spellPoints(costPercent);
            if (points <= 0.0 || masked(p) || !applies(p)) return;
            Account r = account(p);
            r.meter.add(points, now(p));
            evaluate(p, r, "spell");
        } catch (RuntimeException | LinkageError failure) {
            warnOnce("count a spell", failure);
        }
    }

    /** Adds threat for a reason of the caller's own (the admin command, other systems of the mod). */
    public static void addThreat(ServerPlayer p, float points, String reason) {
        try {
            if (points <= 0f || !applies(p)) return;
            Account r = account(p);
            r.meter.add(points, now(p));
            evaluate(p, r, reason);
        } catch (RuntimeException | LinkageError failure) {
            warnOnce("add threat", failure);
        }
    }

    /**
     * The Dungeon takes this player for a lethal threat NOW: a Juggernaut is born from the walls near them, if the caps and the cooldown
     * allow and there is a wall to birth it from. True if one is coming. Never throws.
     */
    public static boolean aggro(ServerPlayer p, String reason) { return aggro(p, reason, false); }

    /** The same; {@code force} (the admin command) skips the player's cooldown and the check for a Nexus, but not the cap of three. */
    static boolean aggro(ServerPlayer p, String reason, boolean force) {
        try {
            if (p == null || !SentientMode.active() || !DungeonConfig.get(DungeonConfig.JUGGERNAUT) || !LivingDungeon.in(p) || !p.isAlive()) return false;
            if (!force && !Targets.valid(p)) return false;        // an explicit aggro (the Gambler's Wild Magic Surge) is not swallowed by the Nexus exclusion that guards the passive threat
            return send(p, account(p), reason, force);
        } catch (RuntimeException | LinkageError failure) {
            warnOnce("send a Juggernaut", failure);
            return false;
        }
    }

    // ---------------------------------------------------------------- the answer
    /** After threat was added: warn at half and four fifths of the threshold, answer at the threshold ({@link ImmunePolicy}). */
    private static void evaluate(ServerPlayer p, Account r, String reason) {
        long now = now(p);
        double threshold = DungeonConfig.get(DungeonConfig.JUGGERNAUT_THREAT);
        double value = r.meter.value(now);
        switch (ImmunePolicy.after(value, threshold, r.warned)) {
            case SEND -> {
                if (!send(p, r, reason, false)) r.meter.clampTo(ImmunePolicy.held(threshold), now);        // held back by the cooldown, the cap or the walls: it does not stay over the line for good
            }
            case WARN_FIRST -> warn(p, 1);
            case WARN_SECOND -> warn(p, 2);
            default -> { }
        }
        r.warned = ImmunePolicy.warned(r.meter.value(now), threshold);                                // after a Juggernaut was sent the meter is empty again, and so is the memory of the warnings
    }

    private static void warn(ServerPlayer p, int level) {
        p.displayClientMessage(Component.literal(level >= 2 ? SentientText.WARN_SECOND : SentientText.WARN_FIRST).withStyle(level >= 2 ? ChatFormatting.RED : ChatFormatting.GRAY), true);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 0.8f, level >= 2 ? 0.6f : 0.8f);
    }

    private static boolean send(ServerPlayer p, Account r, String reason, boolean force) {
        long now = now(p);
        if (ImmunePolicy.mayTry(force, now, r.retryAt, r.cooldownUntil, JuggernautSpawner.alive(), JuggernautSpawner.pending(now)) != ImmunePolicy.Verdict.GO) return false;
        if (!JuggernautSpawner.spawn(p, reason)) {
            r.retryAt = ImmunePolicy.retryAt(now);
            return false;
        }
        r.meter.reset(now);
        r.warned = 0;
        r.cooldownUntil = JuggernautPlan.cooldownUntil(now, DungeonConfig.get(DungeonConfig.JUGGERNAUT_COOLDOWN_MINUTES));
        notice(p, now);
        return true;
    }

    /** The players within 48 blocks of the one the Dungeon has noticed are told, on the action bar; the first Juggernaut of the day is also announced to the server. */
    private static void notice(ServerPlayer p, long now) {
        ServerLevel level = p.serverLevel();
        Component msg = Component.literal(String.format(SentientText.NOTICED, p.getName().getString())).withStyle(ChatFormatting.DARK_RED);
        double r2 = NOTICE_RANGE * NOTICE_RANGE;
        for (ServerPlayer q : level.players()) if (!q.isSpectator() && q.distanceToSqr(p) <= r2) q.displayClientMessage(msg, true);
        long day = now / TICKS_PER_DAY;
        MinecraftServer server = level.getServer();
        if (day != announcedDay && server != null) {
            announcedDay = day;
            DungeonMaw.broadcast(server, Component.literal(SentientText.BROADCAST).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
    }

    // ---------------------------------------------------------------- events
    /** Registered on the game bus: keeps the boss bars of the Juggernauts, every twenty ticks. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 == 5) JuggernautSpawner.upkeep();
    }

    /** When a Juggernaut falls the Dungeon's anger cools: the threat of every player within 64 blocks is gone. */
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity v = e.getEntity();
        if (!(v.level() instanceof ServerLevel level) || !v.getTags().contains(SentientTags.JUGGERNAUT)) return;
        JuggernautSpawner.forget(v);
        long now = level.getGameTime();
        double r2 = 64.0 * 64.0;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(v) > r2) continue;
            Account r = ACCOUNTS.get(p.getUUID());
            if (r != null) {
                r.meter.reset(now);
                r.warned = 0;
            }
            p.displayClientMessage(Component.literal(SentientText.FALLEN).withStyle(ChatFormatting.GRAY), true);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ACCOUNTS.remove(p.getUUID());
        JuggernautSpawner.dropPlayer(p);
    }

    public static void onServerStopping(ServerStoppingEvent e) {
        ACCOUNTS.clear();
        WARNED.clear();
        JuggernautSpawner.clear();
        announcedDay = -1;
    }
}
