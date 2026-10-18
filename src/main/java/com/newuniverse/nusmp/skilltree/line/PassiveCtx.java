package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: a passive executor is called from a damage, tick, kill or drop event with the player whose passive it is, and needs the same few things the arts need (the node's numbers at the bought level, the
 * node's enum words) plus two the arts do not: a stable key for the passive's own memory in the player's data (so its rest, counter and timers are its own and survive nothing but game time), and
 * the clock. This small immutable bundle is built by the dispatcher only AFTER the gate has said the node may act, so an executor can assume that the line is equipped, the node bought and the switch
 * on, and never has to check. It is created per event and not kept.
 */
public final class PassiveCtx {
    public final ServerPlayer p;
    public final SkillRoute route;
    public final SkillNode node;
    public final int level;
    public final long now;
    private final String key;

    public PassiveCtx(ServerPlayer p, SkillRoute route, SkillNode node, int level) {
        this.p = p;
        this.route = route;
        this.node = node;
        this.level = Math.max(1, level);
        this.now = p.serverLevel().getGameTime();
        this.key = SkillRoute.key(route.id(), node.id());
    }

    /** The passive's own key in the player's data: {@code <route>#<node>}. */
    public String key() { return key; }

    /** A number of the node's tables at its level; the catalogue's base value when the node has no such table. */
    public double num(String name) { return node.value(name, level, LineCatalog.get().base(node.kind(), name, 0.0)); }

    public int whole(String name) { return (int) Math.round(num(name)); }

    /** An enum word of the node. */
    public String str(String name, String fallback) { return node.param(name, fallback); }

    /** One roll against a probability given as a fraction (0 never, 1 always). */
    public boolean roll(double chance) { return chance > 0 && p.getRandom().nextDouble() < chance; }

    /** True and the rest starts when this passive may act now (its own rest in ticks has run out). */
    public boolean rested(String suffix, long restTicks) { return LineState.tryRest(p, key + (suffix.isEmpty() ? "" : "." + suffix), now, restTicks); }
}
