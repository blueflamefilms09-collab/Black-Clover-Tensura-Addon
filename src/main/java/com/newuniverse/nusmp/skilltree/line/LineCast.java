package com.newuniverse.nusmp.skilltree.line;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Why: an art of a generated line is not a ManasCore skill of its own (the skill registry is frozen at startup and some two thousand arts must not become two thousand registered skills): it is run by the book's
 * unique skill, whose modes are the line's arts. A press of the skill key hands the art of the current mode to THIS class, which does what the server must do in the order that makes it cheap to refuse and impossible
 * to cheat: the art must be off its cooldown, which is a game time in the player's own data; the executor's own check must find what the art needs (a target) BEFORE anything is spent; then the magicule price is paid
 * through {@code AbilityKit.pay} (a share of the maximum pool, the same law every technique pays), the executor runs inside a catch, and the cooldown is set from the node's {@code cooldown} table (never below
 * 3 seconds for an art, 1 second for a stance: the owner's floor, whatever a data file says). Whether the player owns the skill, whether the art has awakened and at which level it counts are decided by the caller
 * ({@code UniqueBooks.useArt}, from the skill's mastery); this class trusts the level it is given. The damage, heals and control inside the executors go through the balance law. Practice (how many times the art
 * was used) feeds the law's mastery term. Returns why a cast did not happen, for the tests.
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

    /** Casts the art {@code node} of {@code route} for the player at {@code level} (at least 1). */
    public static Result cast(ServerPlayer p, SkillRoute route, SkillNode node, int level) {
        String routeId = route == null ? "" : route.id();
        try {
            if (p == null || !p.isAlive() || p.isSpectator()) return Result.DISABLED;
            ActiveExecutors.Art art = node == null ? null : ActiveExecutors.get(node.kind());
            if (art == null) return Result.UNKNOWN;
            if (level < 1) {
                refuse(p, LineText.CAST_LOCKED);
                return Result.LOCKED;
            }
            if (node.ultimate()) {                                                  // 3.26.0 (Evolution skills): the Ultimate art needs the evolution AND full mastery
                EvolutionRules.Gate gate = Evolution.gate(p, route, node);
                if (gate != EvolutionRules.Gate.OPEN) {
                    Evolution.refuse(p, route, node, gate);
                    return Result.LOCKED;
                }
            }
            String key = SkillRoute.key(routeId, node.id());
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
            LineState.setCooldownEnd(p, key, now + cooldownTicks(node, level));
            LineState.addCast(p, key);
            Evolution.onArtCast(p, route, node);                                    // 3.26.0: the "arts_cast" evolution counter
            return Result.OK;
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] line cast failed for {} {}", routeId, node == null ? "?" : node.id(), e);
            return Result.FAILED;
        }
    }

    private static void refuse(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(LineText.tr(key, args).withStyle(ChatFormatting.RED), true);
    }
}
