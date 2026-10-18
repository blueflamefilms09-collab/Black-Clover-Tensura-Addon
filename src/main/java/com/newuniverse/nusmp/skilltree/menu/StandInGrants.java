package com.newuniverse.nusmp.skilltree.menu;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.TreeConfig;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the server half of the fog of war. A player may SEE a Unique Skill Line in Tensura's menu only if they own it, and Tensura shows exactly what is in the player's learned-skill list, so the rule is
 * enforced where it cannot be bypassed: the stand-in skill of a line is TAUGHT when the player owns the line and FORGOTTEN when they do not. It is done as one reconciliation, {@link #reconcile}, which
 * compares the stand-ins the player holds with the lines the player owns ({@link StandInPlan}) and fixes the difference, and it is run on every event that could make them differ: login, respawn, the moment
 * a line is unlocked (the unlock listener, so the entry appears at once), and a light check on the player's own tick (every half second the owned set's signature is compared, every five seconds the lists
 * are compared in full), which also heals a stand-in that another system removed, copied or lost. It touches ONLY {@link LineStandInSkill}s: no tree skill, no Tensura skill and no grimoire page is ever
 * taught or forgotten here, and {@code SkillGrants.reconcile} (which forgets tree skills) never touches a stand-in because a stand-in is not a tree skill. Teaching is silent (no "skill acquired" banner:
 * the line already has its own unlock banner) and every call is guarded, because a storage that is not ready must be a logged refusal and never a crash of the login. The per-player memory is one
 * integer in a small map, emptied when the player leaves and when the server stops. With the master switch off ({@code tensuraMenu.enabled}) the wanted set is empty, so the stand-ins are cleaned away.
 */
final class StandInGrants {
    private StandInGrants() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<UUID, Integer> APPLIED = new HashMap<>();
    private static boolean wired;

    static synchronized void register() {
        if (wired) return;
        wired = true;
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(StandInGrants::onLogin);
        bus.addListener(StandInGrants::onRespawn);
        bus.addListener(StandInGrants::onLogout);
        bus.addListener(StandInGrants::onTick);
        bus.addListener((ServerStoppedEvent e) -> APPLIED.clear());
        SkillTreeApi.addUnlockListener((player, route, announce) -> reconcile(player));
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) reconcile(p);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) reconcile(p);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { APPLIED.remove(e.getEntity().getUUID()); }

    private static void onTick(PlayerTickEvent.Post e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer || (p.tickCount + p.getId()) % 10 != 0) return;
            Integer last = APPLIED.get(p.getUUID());
            boolean full = (p.tickCount + p.getId()) % 100 == 0;
            if (last == null || full || last != signatureOf(p)) reconcile(p);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] tensura menu: the stand-in check failed", ex);
        }
    }

    private static int signatureOf(ServerPlayer p) {
        return StandInPlan.signature(wanted(p)) * 31 + (MenuConfig.enabled() ? 1 : 0);
    }

    /** The lines whose stand-in the player should hold now: the owned lines that have one (none when the engine or the integration is off). */
    private static List<String> wanted(ServerPlayer p) {
        if (!MenuConfig.enabled() || !TreeConfig.enabled()) return List.of();
        List<String> owned = new ArrayList<>(SkillTreeApi.of(p).unlockedRoutes());
        owned.removeIf(id -> !LineStandIns.routeIds().contains(id));
        return owned;
    }

    /** Makes the stand-ins in the player's skill list match the lines the player owns. Never throws. */
    static void reconcile(ServerPlayer p) {
        try {
            if (p == null || p instanceof FakePlayer) return;
            Skills skills = SkillAPI.getSkillsFrom(p);
            if (skills == null) return;
            List<String> held = new ArrayList<>();
            for (ManasSkillInstance inst : new ArrayList<>(skills.getLearnedSkills())) if (inst.getSkill() instanceof LineStandInSkill s) held.add(s.routeId());
            List<String> want = wanted(p);
            StandInPlan.Plan plan = StandInPlan.diff(held, want, LineStandIns.routeIds());
            for (String route : plan.forget()) {
                ResourceLocation id = LineStandIns.skillId(route);
                skills.forgetSkill(id);
            }
            for (String route : plan.learn()) {
                var holder = LineStandIns.holder(route);
                if (holder != null) skills.learnSkill(holder.get().createDefaultInstance());
            }
            APPLIED.put(p.getUUID(), signatureOf(p));
        } catch (RuntimeException | LinkageError ex) {
            LOG.warn("[nusmp] tensura menu: could not update the stand-in skills of {}", p == null ? "?" : p.getGameProfile().getName(), ex);
        }
    }
}
