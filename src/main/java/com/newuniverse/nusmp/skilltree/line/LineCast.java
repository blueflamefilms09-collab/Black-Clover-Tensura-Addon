package com.newuniverse.nusmp.skilltree.line;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.PlayerTreeState;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.TreeConfig;
import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

/**
 * Why: an art of a generated line is not a ManasCore skill (the skill registry is frozen at startup and 5,000 nodes must not become 5,000 registered skills), so there is no Tensura key to cast
 * it with. The Arts bar sends "use this art" as a packet, and THIS is what the server does with it, in the order that makes it impossible to cheat and cheap to refuse: the engine must be on; the
 * route and node must exist in the server's registry; the gate ({@link LineGate#activeLevel}) must say the line is the equipped one and the node is bought and an art (a modified client cannot
 * cast an art it does not own, or of a line it has not equipped); the art must be off its cooldown, which is a game time in the player's own data; the executor's own check must find what the art needs
 * (a target) BEFORE anything is spent; then the magicule price is paid through {@code AbilityKit.pay} (a share of the maximum pool, the same law every tree technique pays), the executor runs inside a
 * catch, the cooldown is set from the node's {@code cooldown} table (never below 3 seconds for an art, 1 second for a stance: the owner's floor, whatever a data file says) and the client is told
 * so its bar can show the sweep. The damage, heals and control inside the executors go through the balance law. Practice (how many times the art was used) feeds the law's mastery term, which is the line
 * engine's equivalent of a skill's mastery points. Returns why a cast did not happen, for the tests.
 */
public final class LineCast {
    private LineCast() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** The floor of an art's cooldown in ticks (3 seconds) and of a stance's (1 second). A finisher's 45 seconds is in the data and is held by the generator and the data-file test. */
    public static final int ART_MIN_COOLDOWN = 60, STANCE_MIN_COOLDOWN = 20;

    public enum Result { OK, DISABLED, UNKNOWN, LOCKED, COOLDOWN, NO_TARGET, NO_MAGICULES, FAILED }

    /** The cooldown in ticks an art has at a level: the node's table, held to the floor. */
    public static int cooldownTicks(SkillNode node, int level) {
        boolean stance = "stance".equals(node.kind());
        double base = LineCatalog.get().base(node.kind(), "cooldown", 100.0);
        return (int) Math.max(stance ? STANCE_MIN_COOLDOWN : ART_MIN_COOLDOWN, Math.round(node.value("cooldown", level, base)));
    }

    public static Result cast(ServerPlayer p, String routeId, String nodeId) {
        try {
            if (p == null || !p.isAlive() || p.isSpectator() || !TreeConfig.enabled()) return Result.DISABLED;
            SkillRoute route = RouteRegistry.byId(routeId);
            SkillNode node = route == null ? null : route.node(nodeId);
            ActiveExecutors.Art art = node == null ? null : ActiveExecutors.get(node.kind());
            if (art == null) return Result.UNKNOWN;
            int level = LineGate.activeLevel(new PlayerTreeState(SkillTreeApi.of(p)), route, node);
            if (level < 1) {
                refuse(p, LineText.CAST_LOCKED);
                return Result.LOCKED;
            }
            String key = SkillRoute.key(routeId, nodeId);
            long now = p.serverLevel().getGameTime();
            long ready = LineState.cooldownEnd(p, key);
            if (now < ready) {
                refuse(p, LineText.CAST_COOLDOWN, node.name(), (ready - now + 19) / 20);
                return Result.COOLDOWN;
            }
            ActiveCtx ctx = new ActiveCtx(p, route, node, level, LineState.mastery(p, key));
            if (!art.canUse().ok(ctx)) {
                refuse(p, LineText.CAST_NO_TARGET, node.name());
                return Result.NO_TARGET;
            }
            boolean dropsStance = "stance".equals(node.kind()) && key.equals(LineState.stance(p));           // putting a stance down is free
            if (!dropsStance && !AbilityKit.pay(p, ctx.num("cost"))) return Result.NO_MAGICULES;
            try {
                art.run().run(ctx);
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("[nusmp] line art {} ({}) failed: {}", key, node.kind(), e.toString());
                return Result.FAILED;
            }
            int cd = cooldownTicks(node, level);
            LineState.setCooldownEnd(p, key, now + cd);
            LineState.addCast(p, key);
            tell(p, key, cd);
            return Result.OK;
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] line cast failed for {} {}", routeId, nodeId, e);
            return Result.FAILED;
        }
    }

    private static void refuse(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(LineText.tr(key, args).withStyle(ChatFormatting.RED), true);
    }

    private static void tell(ServerPlayer p, String key, int cooldownTicks) {
        try {
            PacketDistributor.sendToPlayer(p, new LinePayloads.ArtState(key, cooldownTicks, LineState.stance(p)));
        } catch (RuntimeException | LinkageError e) {
            LOG.debug("[nusmp] could not tell {} about the art state", p.getGameProfile().getName(), e);              // no connection (a test world): nothing to show
        }
    }

    /** Tells a player which stance is on without starting a cooldown (a login, a class change). */
    public static void sendStance(ServerPlayer p) { tell(p, "", 0); }
}
