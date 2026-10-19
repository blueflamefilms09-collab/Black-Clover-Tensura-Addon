package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.unique.UniqueBooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Why: this is the ONE place a passive or an attribute bonus is allowed to act. Every event that can matter (a blow dealt, a blow taken, a kill, experience, drops, the half-second beat) asks
 * {@link #active} for the nodes of the player's owned unique skills that count right now, and that function asks {@link LineGate#passiveLevel} for each: skill owned and switched on, level from its mastery. An
 * executor ({@link PassiveExecutors}, {@link LineAttrs}) is therefore only ever called for a node that is allowed to act, and nothing else in the engine reaches a passive: the owner's rule "a passive of a skill that is switched off does nothing" cannot be forgotten by one of the 51 kinds, because none of them checks it. A player with no book costs one lookup and
 * an empty list. Which nodes of a route are passives is the same for every player, so it is worked out once per route record (a weak cache, dropped with the route on a reload) and not per event. Every
 * entry point is guarded by the caller ({@code LineEvents}) so a fault in one executor is logged and never reaches the server loop.
 */
public final class LinePassives {
    private LinePassives() {}

    /** A passive or attribute node that counts right now, with its bought level. */
    public record ActiveNode(SkillRoute route, SkillNode node, int level) {}

    private static final Map<SkillRoute, List<SkillNode>> TOGGLE_NODES = Collections.synchronizedMap(new WeakHashMap<>());

    /** The nodes of a route that carry a switch (passives and attribute bonuses), computed once per route record. */
    static List<SkillNode> toggleNodes(SkillRoute route) {
        return TOGGLE_NODES.computeIfAbsent(route, r -> {
            List<SkillNode> out = new ArrayList<>();
            for (SkillNode n : r.nodes()) if (n.hasToggle()) out.add(n);
            return List.copyOf(out);
        });
    }

    /** The passive and stat nodes of a route (the self-test and the tooltips ask which lines have any). */
    public static List<SkillNode> toggleNodesOf(SkillRoute route) { return toggleNodes(route); }

    /** THE gate: the passive and attribute nodes of every owned unique skill that is switched on, with the level the skill's mastery gives them. Empty when the player owns none or all are off. */
    public static List<ActiveNode> active(ServerPlayer p) {
        List<UniqueBooks.Owned> owned = UniqueBooks.ownedLines(p);
        if (owned.isEmpty()) return List.of();
        List<ActiveNode> out = null;
        for (UniqueBooks.Owned o : owned) {
            if (!o.on()) continue;
            SkillRoute route = RouteRegistry.byId(o.skill().routeId());
            if (route == null || !route.isLine()) continue;
            double frac = o.fraction();
            for (SkillNode n : toggleNodes(route)) {
                int lv = LineGate.passiveLevel(true, n, frac);
                if (lv < 1) continue;
                if (out == null) out = new ArrayList<>(8);
                out.add(new ActiveNode(route, n, lv));
            }
        }
        return out == null ? List.of() : out;
    }

    // ================================================================== the events
    /** A blow the player takes: every active passive reshapes the amount in turn; a result of 0 or less means the blow is avoided. */
    public static float taken(ServerPlayer p, DamageSource src, float amount) {
        for (ActiveNode a : active(p)) {
            PassiveExecutors.Passive ex = PassiveExecutors.get(a.node().kind());
            if (ex == null || ex.taken() == null) continue;
            amount = ex.taken().apply(new PassiveCtx(p, a.route(), a.node(), a.level()), src, amount);
            if (amount <= 0f) return 0f;
        }
        return amount;
    }

    /** A blow the player deals: every active passive and the stance reshape the amount in turn. */
    public static float dealt(ServerPlayer p, LivingEntity target, DamageSource src, float amount) {
        for (ActiveNode a : active(p)) {
            PassiveExecutors.Passive ex = PassiveExecutors.get(a.node().kind());
            if (ex == null || ex.dealt() == null) continue;
            amount = ex.dealt().apply(new PassiveCtx(p, a.route(), a.node(), a.level()), target, src, amount);
        }
        LineStances.View stance = LineStances.current(p);
        if (stance != null) {
            amount = (float) (amount * (1.0 + stance.damagePct()));
            if (stance.critChance() > 0 && p.getRandom().nextDouble() < stance.critChance()) amount *= 1.5f;
        }
        return Math.max(0f, amount);
    }

    /** A blow of the player landed for {@code dealt} damage. */
    public static void dealtPost(ServerPlayer p, LivingEntity target, float dealt) {
        for (ActiveNode a : active(p)) {
            PassiveExecutors.Passive ex = PassiveExecutors.get(a.node().kind());
            if (ex != null && ex.post() != null) ex.post().apply(new PassiveCtx(p, a.route(), a.node(), a.level()), target, dealt);
        }
    }

    /** The player killed {@code victim}. */
    public static void kill(ServerPlayer p, LivingEntity victim) {
        for (ActiveNode a : active(p)) {
            PassiveExecutors.Passive ex = PassiveExecutors.get(a.node().kind());
            if (ex != null && ex.kill() != null) ex.kill().apply(new PassiveCtx(p, a.route(), a.node(), a.level()), victim);
        }
    }

    /** The experience a kill of the player gives. */
    public static int xp(ServerPlayer p, int xp) {
        for (ActiveNode a : active(p)) {
            PassiveExecutors.Passive ex = PassiveExecutors.get(a.node().kind());
            if (ex != null && ex.xp() != null) xp = ex.xp().apply(new PassiveCtx(p, a.route(), a.node(), a.level()), xp);
        }
        return xp;
    }

    /** The drops of a kill of the player. */
    public static void drops(ServerPlayer p, LivingDropsEvent e) {
        for (ActiveNode a : active(p)) {
            PassiveExecutors.Passive ex = PassiveExecutors.get(a.node().kind());
            if (ex != null && ex.drops() != null) ex.drops().apply(new PassiveCtx(p, a.route(), a.node(), a.level()), e);
        }
    }

    /** The half-second beat: the passives that act over time, the attribute bonuses, the stance. */
    public static void beat(ServerPlayer p) {
        for (ActiveNode a : active(p)) {
            PassiveExecutors.Passive ex = PassiveExecutors.get(a.node().kind());
            if (ex != null && ex.beat() != null) ex.beat().apply(new PassiveCtx(p, a.route(), a.node(), a.level()));
        }
        LineAttrs.sync(p);
        LineStances.sync(p);
        LineStances.View stance = LineStances.current(p);
        if (stance != null && stance.regen() > 0 && p.getHealth() < p.getMaxHealth()) p.heal((float) (stance.regen() * PassiveExecutors.BEAT_TICKS / 20.0));
    }
}
