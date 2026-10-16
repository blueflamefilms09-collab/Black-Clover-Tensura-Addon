package com.newuniverse.nusmp.quests;

import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Why: a few event keys need a little logic before they can be reported (is this discovered item a relic, which players took part in a boss
 * fight, which page is the time stop). Putting that logic here keeps every fire point in the game code a single call; the game code never learns
 * the quest rules, and when a key's meaning changes only this file does.
 *
 * <p>Every method is safe to call at any time: they only read, then call {@link NusmpEvents}, which swallows its own failures.
 */
public final class QuestHooks {
    private QuestHooks() {}

    /** Blocks around a boss within which a player counts as having taken part when no better record exists. */
    public static final double PARTICIPANT_RADIUS = 64.0;

    /** Page ids of Time magic that stop time for a target or a zone (they all freeze with {@code TimeStop}). */
    private static final java.util.Set<String> TIME_STOP_PAGES = java.util.Set.of("chrono_stasis", "grigora");

    // ---------------------------------------------------------------- spells
    /**
     * A page was cast successfully. Reports {@code spell.cast} (arg = the magic, lower case), {@code spell.cast.signature} for a signature
     * page (35 percent cost tier, the mod's definition of a signature page), and {@code time.stop} for the Time pages that freeze a target.
     */
    public static void onCast(ServerPlayer p, String magic, String pageId, double costPercent) {
        if (magic == null) return;
        String m = magic.toLowerCase(Locale.ROOT);
        NusmpEvents.fire(p, "spell.cast", m);
        NusmpEvents.fireOnce(p, "spell.distinct", m);          // the plain counter of this key = how many different magics were cast
        if (costPercent >= 35.0) NusmpEvents.fire(p, "spell.cast.signature", m);
        if ("time".equals(m) && pageId != null && TIME_STOP_PAGES.contains(pageId)) NusmpEvents.fire(p, "time.stop", pageId);
    }

    // ---------------------------------------------------------------- relics
    /**
     * The discovery system just marked something as found for the player ({@code key} like {@code item:nusmp:thing}). If it is a relic, reports
     * {@code relic.obtained} (arg = relic id), {@code relic.grade} (arg = the grade, lower case) and, for a world-unique relic that the God of
     * Order's ledger tracks, {@code relic.unique_ledger}.
     */
    public static void onDiscovered(ServerPlayer p, String key) {
        if (key == null || !key.startsWith("item:nusmp:")) return;
        RelicDef def = Relics.byId(key.substring("item:nusmp:".length()));
        if (def == null) return;
        NusmpEvents.fire(p, "relic.obtained", def.id());
        NusmpEvents.fire(p, "relic.grade", def.grade().name().toLowerCase(Locale.ROOT));
        if (def.isUnique()) NusmpEvents.fire(p, "relic.unique_ledger", def.id());
    }

    /** A relic was attuned (bound) to the player: {@code relic.equipped} (arg = relic id). */
    public static void relicEquipped(ServerPlayer p, ItemStack stack) {
        RelicDef def = Relics.defOf(stack);
        NusmpEvents.fire(p, "relic.equipped", def == null ? null : def.id());
    }

    /** A tier ritual raised a relic to {@code to}: {@code relic.tier} and {@code relic.grade} (both with the new grade as arg). */
    public static void relicTier(ServerPlayer p, Grade to) {
        String g = to.name().toLowerCase(Locale.ROOT);
        NusmpEvents.fire(p, "relic.tier", g);
        NusmpEvents.fire(p, "relic.grade", g);
    }

    // ---------------------------------------------------------------- bosses
    /**
     * A boss entered a new phase or stage: {@code boss.phase} for everyone who is fighting it (arg = {@code <boss id>:<stage>}, for example
     * {@code lucius:2}; the stage that STARTS).
     */
    public static void bossPhase(LivingEntity boss, String bossId, int stage) {
        if (!(boss.level() instanceof ServerLevel)) return;
        NusmpEvents.fireAll(participants(boss, null), "boss.phase", bossId + ":" + stage);
    }

    /** The players who took part in a boss fight: the killer (or their owner), anyone the boss recorded as a fighter, and everyone close by. */
    public static List<ServerPlayer> participants(LivingEntity boss, net.minecraft.world.damagesource.DamageSource source) {
        List<ServerPlayer> out = new ArrayList<>();
        if (!(boss.level() instanceof ServerLevel sl)) return out;
        if (source != null) {
            net.minecraft.world.entity.Entity cause = source.getEntity();
            if (cause instanceof ServerPlayer sp) out.add(sp);
            else if (cause instanceof net.minecraft.world.entity.OwnableEntity o && o.getOwnerUUID() != null
                    && sl.getPlayerByUUID(o.getOwnerUUID()) instanceof ServerPlayer owner && !out.contains(owner)) out.add(owner);
        }
        if (boss instanceof ZogratisBoss zb) {
            for (UUID id : new ArrayList<>(zb.fighters())) {
                if (sl.getPlayerByUUID(id) instanceof ServerPlayer f && !out.contains(f)) out.add(f);
            }
        }
        double r2 = PARTICIPANT_RADIUS * PARTICIPANT_RADIUS;
        for (ServerPlayer p : sl.players()) {
            if (!out.contains(p) && !p.isSpectator() && p.distanceToSqr(boss) <= r2) out.add(p);
        }
        return out;
    }

    // ---------------------------------------------------------------- the no-relic arena clear
    /** Persistent-data flag: true once the player wore or held a relic during the current arena visit. */
    public static final String ARENA_RELIC_FLAG = "nusmp_arena_relic";

    /** True if the entity wears a relic or holds one in a hand. */
    public static boolean carriesRelic(LivingEntity e) {
        try {
            if (!Relics.equipped(e).isEmpty()) return true;
            return Relics.defOf(e.getMainHandItem()) != null || Relics.defOf(e.getOffhandItem()) != null;
        } catch (RuntimeException unreadable) {
            return true;                                        // cannot prove a clean run: do not award it
        }
    }

    /** A player came into the arena: the flag starts from what they carry now. */
    public static void arenaEntered(ServerPlayer p) {
        p.getPersistentData().putBoolean(ARENA_RELIC_FLAG, carriesRelic(p));
    }

    /** Called about once a second for players inside the arena: a relic picked up or put on during the fight sets the flag. */
    public static void arenaWatch(ServerPlayer p) {
        if (!p.getPersistentData().getBoolean(ARENA_RELIC_FLAG) && carriesRelic(p)) p.getPersistentData().putBoolean(ARENA_RELIC_FLAG, true);
    }

    /** Lucius fell: a player who carried no relic since entering also gets {@code arena.victory} with arg {@code norelic} (team-shared). */
    public static void arenaVictory(ServerPlayer p) {
        arenaWatch(p);
        if (!p.getPersistentData().getBoolean(ARENA_RELIC_FLAG)) NusmpEvents.fireTeam(p, "arena.victory", "norelic");
    }
}
