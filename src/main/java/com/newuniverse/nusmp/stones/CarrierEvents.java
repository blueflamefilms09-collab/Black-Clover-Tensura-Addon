package com.newuniverse.nusmp.stones;

import com.newuniverse.nusmp.relics.Relics;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: everything Phase B does on its own (not on a key press), bounded. Once a second per player (staggered by entity id): read what the player carries (the cached relic list, one pass), keep the
 * socket mask, sample for the Time Stone's rewind when one is socketed, and give the Mind Stone's mastery bonus (one point on one skill every 80 seconds). Once a tick: only the open lifts and links
 * (at most the server caps, 24 each by default). On death, dimension change, logout and server stop: the player's lift, link and tables end. A player with no carrier costs one cached list read a second.
 * Server only.
 */
public final class CarrierEvents {
    private CarrierEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(CarrierEvents::onPlayerTick);
        bus.addListener(CarrierEvents::onServerTick);
        bus.addListener(CarrierEvents::onDeath);
        bus.addListener(CarrierEvents::onLogout);
        bus.addListener(CarrierEvents::onDimension);
        bus.addListener(CarrierEvents::onStopping);
    }

    // ================================================================ once a second per player
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive() || p.isSpectator()) return;
        if ((p.tickCount + p.getId()) % 20 != 0) return;
        if (!StoneConfig.enabled()) {
            CarrierState.setMask(p.getUUID(), 0);
            return;
        }
        int mask = 0;
        for (Relics.Worn w : Relics.loadout(p).active()) {
            if (CarrierRules.allowedMask(w.def().id()) != 0) mask |= CarrierData.mask(w.stack());
        }
        CarrierState.setMask(p.getUUID(), mask);
        boolean slotMind = StoneSlot.active(p) == InfinityStone.MIND;
        boolean mind = slotMind || CarrierRules.has(mask, InfinityStone.MIND);
        if (mask != 0 && CarrierRules.has(mask, InfinityStone.TIME) && StoneConfig.carriers() && StoneSlot.gateOk(p)) StoneState.sample(p);
        if (mind && StoneConfig.masteryBonus()) masteryBonus(p, slotMind || StoneSlot.gateOk(p));
    }

    /** One extra mastery point on one learned, unfinished skill every 80 seconds while a Mind Stone works for the player (the stand-in for +25% mastery gain). */
    private static void masteryBonus(ServerPlayer p, boolean gateOk) {
        long now = p.level().getGameTime();
        if (!gateOk || !CarrierRules.masteryDue(now, CarrierState.lastMastery(p.getUUID()))) return;
        CarrierState.masteryAwarded(p.getUUID(), now);
        Skills skills = SkillAPI.getSkillsFrom(p);
        if (skills == null) return;
        List<ManasSkillInstance> open = new ArrayList<>();
        for (ManasSkillInstance i : skills.getLearnedSkills()) {
            if (open.size() >= 64) break;
            if (i.getSkill() instanceof Skill && i.getMastery() < i.getMaxMastery()) open.add(i);
        }
        if (open.isEmpty()) return;
        ManasSkillInstance pick = open.get(p.getRandom().nextInt(open.size()));
        pick.addMasteryPoint(p, 1.0);
        pick.markDirty();
    }

    // ================================================================ once a tick: the open lifts and links only
    static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        for (UUID id : CarrierState.liftOwners()) MindActs.tickLift(server, id);
        for (UUID id : CarrierState.linkOwners()) MindActs.tickLink(server, id);
    }

    // ================================================================ death, logout, dimension, stop
    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && !p.level().isClientSide()) CarrierState.forgetMotion(p.getUUID());
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        CarrierState.forgetAll(e.getEntity().getUUID());
    }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        CarrierState.forgetMotion(e.getEntity().getUUID());
    }

    static void onStopping(ServerStoppingEvent e) {
        CarrierState.clear();
    }
}
