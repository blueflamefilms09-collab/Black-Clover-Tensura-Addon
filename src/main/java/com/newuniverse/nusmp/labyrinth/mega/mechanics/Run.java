package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the context a primitive runs in: which mechanic instance it belongs to, which effect line of the data it is, the arguments read with their catalogue defaults and scaled by
 * the floor, who its targets are, and the journal that records the blocks it changes. Created fresh for every firing of an effect (a repeating effect makes a new run each time),
 * kept in the instance's running list when the primitive is sustained. The helper methods exist so that a primitive reads like its description: {@code r.num("power")},
 * {@code r.players()}, {@code r.targets("mobs")}, {@code r.centres("auto")}.
 */
public final class Run {
    public final Instance inst;
    public final MechanicDef.Effect eff;
    public final OpCatalog.OpSpec spec;
    public final long startedAt;
    /** Ticks this run lasts (set by the engine from {@link Prim#duration}). */
    public int duration;
    /** Primitive-private memory for a sustained run (for example the current step of a platform). */
    public final Map<String, Object> mem = new HashMap<>();
    private Journal journal;

    Run(Instance inst, MechanicDef.Effect eff, OpCatalog.OpSpec spec, long now) {
        this.inst = inst;
        this.eff = eff;
        this.spec = spec;
        this.startedAt = now;
    }

    public ServerLevel level() { return inst.level; }

    public int floor() { return inst.floor; }

    public long age() { return inst.clock() - startedAt; }

    // ---------------------------------------------------------------- arguments
    private Object dflt(String k) {
        OpCatalog.ArgSpec a = spec == null ? null : spec.arg(k);
        return a == null ? null : a.def();
    }

    /** The number argument at the instance's floor, falling back to the catalogue default. */
    public double num(String k) {
        Object d = dflt(k);
        return eff.args().num(k, d instanceof Double dd ? dd : 0.0, inst.floor);
    }

    /** A damage-like number scaled by the difficulty multiplier. */
    public double hard(String k) { return Scaling.byDifficulty(num(k), inst.difficulty, 0.0); }

    public int ticks(String secsKey) { return (int) Math.round(num(secsKey) * 20.0); }

    public String str(String k) {
        Object d = dflt(k);
        return eff.args().str(k, d instanceof String s ? s : "");
    }

    public boolean bool(String k) {
        Object d = dflt(k);
        return eff.args().bool(k, d instanceof Boolean b ? b : false);
    }

    // ---------------------------------------------------------------- targets
    /** The players of the area that a mechanic may touch (alive, survival or adventure, not in a safe room). */
    public List<ServerPlayer> players() {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : inst.inside) if (Safety.affects(p)) out.add(p);
        return out;
    }

    /** Players, monsters of the mechanic and the dungeon in the area, or both, by the `who` argument (or the given default when the primitive has none). */
    public List<LivingEntity> targets(String defaultWho) {
        String who = spec != null && spec.arg("who") != null ? str("who") : defaultWho;
        List<LivingEntity> out = new ArrayList<>();
        if (!"mobs".equals(who)) out.addAll(players());
        if (!"players".equals(who)) out.addAll(inst.mobsInArea());
        return out;
    }

    /** Block positions an op should work at, by its `at` argument: anchor, each player's feet, the room's middle; "auto" = players for a floor mechanic, the anchor otherwise. */
    public List<BlockPos> centres(String at) {
        String a = at == null || at.isEmpty() ? "auto" : at;
        if (a.equals("auto")) a = inst.def.scope() == Scope.FLOOR ? "player" : "anchor";
        List<BlockPos> out = new ArrayList<>();
        switch (a) {
            case "player" -> {
                for (ServerPlayer p : players()) {
                    out.add(p.blockPosition());
                    if (out.size() >= 4) break;
                }
                if (out.isEmpty()) out.add(inst.anchor);
            }
            case "room" -> out.add(inst.room != null ? inst.room.center() : inst.anchor);
            default -> out.add(inst.anchor);
        }
        return out;
    }

    // ---------------------------------------------------------------- blocks
    /** The journal of this run, created on first use. */
    public Journal journal() {
        if (journal == null) journal = new Journal(inst.level);
        return journal;
    }

    boolean hasJournal() { return journal != null && !journal.isEmpty(); }

    Journal journalOrNull() { return journal; }

    Journal takeJournal() {
        Journal j = journal;
        journal = null;
        return j;
    }

    /** Restores and forgets this run's blocks. */
    void restoreBlocks() {
        if (journal != null) {
            journal.restore();
            journal = null;
        }
    }
}
