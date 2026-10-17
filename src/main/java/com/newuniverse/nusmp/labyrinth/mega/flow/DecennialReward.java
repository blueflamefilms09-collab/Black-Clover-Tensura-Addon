package com.newuniverse.nusmp.labyrinth.mega.flow;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.quests.NusmpEvents;
import com.newuniverse.nusmp.relics.RelicCatalog;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.order.UniqueLedger;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Why: the owner's brief promises that the Guardian of every tenth floor of the Holy road drops a guaranteed "Metal Vessel", a unique artifact that carries a
 * Djinn and, with it, elemental flight and great magic buffs. The mod already has exactly that item class: a relic of Legend grade or above that the catalog
 * flagged as a vessel ({@code RelicDef.isVessel}, {@code Grade.djinnCapable}), which a Djinn Seal can be bound into. So the reward is not a new item but
 * the hand-over of one of those: chosen among the vessels nobody owns yet (the Order's ledger says which are alive), stamped through the ledger like any
 * other issue ({@code RelicCatalog.newStack} then {@code UniqueLedger.issue}), so that it stays one of a kind in the world and cannot be duplicated. THIS IS THE
 * ONE OWNER-REQUESTED EXCEPTION to "Unique-or-higher relics come only from Crucibles". It drops once per sector (remembered in {@link FlowData}), as an item
 * at the Guardian's corpse that cannot burn or despawn and glows, to the killer's credit; when no vessel is left the Guardian drops nothing and the killer is
 * told, never an exception. The Guardian is recognised by the contract with the Guard builder: the scoreboard tag {@code nusmp_sector_guardian} on the entity and
 * the persistent int {@code nusmp_sector} holding the sector id (the sector at the corpse is the fallback). The Holy-only rule is the sector's polarity and the
 * decennial rule is {@code FloorMap.isDecennial(sector.lastFloor())}. An admin can run the same logic by hand ({@code /multiverse mega flow vessel give}), which
 * ignores the once-per-sector memory and the config switch and never writes to the memory.
 */
public final class DecennialReward {
    private DecennialReward() {}

    /** The tag and the persistent key the Guard builder puts on a sector Guardian. */
    public static final String GUARDIAN_TAG = "nusmp_sector_guardian", SECTOR_KEY = "nusmp_sector";
    /** The quest event key fired for the player who receives the vessel (arg = the relic id). */
    public static final String QUEST_KEY = "mega.vessel";

    private static final double FALLBACK_RADIUS = 64.0;
    private static final int TRIES = 8;

    public enum Status { GIVEN, DISABLED, NO_SECTOR, NOT_HOLY, NOT_DECENNIAL, ALREADY_PAID, NONE_LEFT }

    /** Why: the outcome of one run of the reward logic: what happened, and, when something was issued, which relic and the stamped stack to hand over. */
    public record Grant(Status status, String relicId, ItemStack stack) {}

    // ---------------------------------------------------------------- the candidates
    /** Every relic that may be handed out as a Metal Vessel, whether or not it is already in the world: unique, flagged vessel, Djinn-capable grade, not a pair half. */
    public static List<RelicDef> candidates() {
        List<RelicDef> out = new ArrayList<>();
        for (RelicDef d : Relics.all()) {
            if (VesselRules.eligible(d.isUnique(), d.isVessel(), d.grade().djinnCapable(), d.isPairHalf())) out.add(d);
        }
        return out;
    }

    /** The candidates that no one has: the Order's ledger holds no live copy of them. */
    public static List<RelicDef> free(MinecraftServer server) {
        Set<String> alive = UniqueLedger.liveIds(server);
        List<RelicDef> out = new ArrayList<>();
        for (RelicDef d : candidates()) if (!alive.contains(d.id())) out.add(d);
        return out;
    }

    /** The draw weights of a pool: the mod's loot weight of each relic's grade. */
    static List<VesselRules.Candidate> weights(List<RelicDef> pool) {
        List<VesselRules.Candidate> out = new ArrayList<>(pool.size());
        for (RelicDef d : pool) out.add(new VesselRules.Candidate(d.id(), d.grade().weight()));
        return out;
    }

    // ---------------------------------------------------------------- the event
    static void onDrops(LivingDropsEvent e) {
        try {
            LivingEntity dead = e.getEntity();
            if (!(dead.level() instanceof ServerLevel level) || !MegaDungeon.in(level)) return;
            if (!dead.getTags().contains(GUARDIAN_TAG)) return;
            if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;          // a Guardian's drops are loot like any other: the rule that switches loot off switches this off
            MinecraftServer server = level.getServer();
            Sector sector = sectorOf(dead, MegaData.get(server));
            if (gate(sector, server) != null) return;
            ServerPlayer killer = killerOf(dead, e.getSource(), level);
            if (killer == null) return;                                                       // nobody to credit: the sector keeps its reward for a death that has a killer
            Grant g = grant(level, sector, false);
            if (g.status() == Status.NONE_LEFT) {
                killer.sendSystemMessage(Component.translatableWithFallback("message.nusmp.mega.mfw.vessel.none",
                        "The Guardian has no Metal Vessel left to give: every relic that can carry a Djinn is already in the world.").withStyle(ChatFormatting.GRAY));
            } else if (g.status() == Status.GIVEN) {
                ItemEntity drop = new ItemEntity(level, dead.getX(), dead.getY() + 0.3, dead.getZ(), g.stack());
                drop.setUnlimitedLifetime();
                drop.setInvulnerable(true);                                                    // it cannot burn, drown in lava or be blown up
                drop.setGlowingTag(true);                                                      // and it can be found in the dark of the arena
                e.getDrops().add(drop);
                announce(level, killer, sector, g, dead, true);
            }
        } catch (RuntimeException ex) {
            FlowEvents.warn(ex);
        }
    }

    /** The sector of a Guardian: the persistent id the Guard builder stored, else the sector whose region holds the corpse; null when neither is known. */
    static Sector sectorOf(LivingEntity guardian, MegaData data) {
        CompoundTag tag = guardian.getPersistentData();
        if (tag.contains(SECTOR_KEY, Tag.TAG_ANY_NUMERIC)) {
            Sector s = data.sector(tag.getInt(SECTOR_KEY));
            if (s != null) return s;
        }
        return data.sectorAt(FlowEvents.posOf(guardian));
    }

    /** The player a kill is credited to: the attacker, the owner of the summon that struck, the player the game credits, else the nearest living player within 64 blocks. */
    static ServerPlayer killerOf(LivingEntity dead, DamageSource source, ServerLevel level) {
        Entity cause = source == null ? null : source.getEntity();
        ServerPlayer p = cause == null ? null : FlowEvents.playerOf(cause, level);
        if (p != null) return p;
        if (dead.getKillCredit() instanceof ServerPlayer credited) return credited;
        ServerPlayer best = null;
        double bestD = FALLBACK_RADIUS * FALLBACK_RADIUS;
        for (ServerPlayer q : level.players()) {
            if (q.isSpectator() || !q.isAlive()) continue;
            double d = q.distanceToSqr(dead);
            if (d <= bestD) {
                best = q;
                bestD = d;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- the reward
    /** Why a sector would not pay now, or null if it may: the switch, the sector, the road, the floor and the memory. */
    static Status gate(Sector sector, MinecraftServer server) {
        if (!FlowConfig.get(FlowConfig.ENABLED) || !FlowConfig.get(FlowConfig.VESSEL_ENABLED)) return Status.DISABLED;
        if (sector == null) return Status.NO_SECTOR;
        if (sector.polarity != Polarity.HOLY) return Status.NOT_HOLY;
        if (!FloorMap.isDecennial(sector.lastFloor(), sector.perRegion)) return Status.NOT_DECENNIAL;
        if (FlowData.get(server).rewarded(sector.id)) return Status.ALREADY_PAID;
        return null;
    }

    /**
     * Runs the reward logic for a sector: checks that it may pay (unless {@code admin}), picks one of the free vessels by the grade weights, issues it through
     * the Order's ledger and, for a real reward, records that the sector has paid. Returns the stamped stack for the caller to hand over (as a drop or into an
     * inventory); never throws for a missing sector or an empty pool. An admin run skips every check and the memory, because it is a hand-over, not the
     * sector's reward.
     */
    public static Grant grant(ServerLevel level, Sector sector, boolean admin) {
        MinecraftServer server = level.getServer();
        if (server == null) return new Grant(Status.NONE_LEFT, "", ItemStack.EMPTY);
        if (!admin) {
            Status why = gate(sector, server);
            if (why != null) return new Grant(why, "", ItemStack.EMPTY);
        }
        List<RelicDef> pool = free(server);
        for (int tries = 0; tries < TRIES && !pool.isEmpty(); tries++) {
            int i = VesselRules.pick(weights(pool), level.getRandom().nextDouble());
            if (i < 0) break;
            RelicDef d = pool.get(i);
            ItemStack stack = RelicCatalog.newStack(d);
            if (!stack.isEmpty() && UniqueLedger.issue(level, d.id(), stack)) {
                if (!admin && sector != null) FlowData.get(server).claim(sector.id, d.id());
                return new Grant(Status.GIVEN, d.id(), stack);
            }
            pool.remove(i);                                                                   // it was taken between the look and the issue: try another
        }
        return new Grant(Status.NONE_LEFT, "", ItemStack.EMPTY);
    }

    /** Tells the receiver and the players around {@code where} (null = nobody else) what was handed over, and, when {@code quest}, fires the quest event for the receiver. */
    static void announce(ServerLevel level, ServerPlayer receiver, Sector sector, Grant g, Entity where, boolean quest) {
        String floor = sector == null ? "?" : FloorMap.label(sector.lastFloor());
        Component line = Component.translatableWithFallback("message.nusmp.mega.mfw.vessel.given", "The Guardian of floor %s lets go of a Metal Vessel: %s.",
                floor, g.stack().getHoverName()).withStyle(ChatFormatting.GOLD);
        receiver.sendSystemMessage(line);
        if (where != null) {
            for (ServerPlayer q : level.players()) if (q != receiver && q.distanceToSqr(where) <= FALLBACK_RADIUS * FALLBACK_RADIUS) q.sendSystemMessage(line);
        }
        if (quest) NusmpEvents.fire(receiver, QUEST_KEY, g.relicId());
    }
}
