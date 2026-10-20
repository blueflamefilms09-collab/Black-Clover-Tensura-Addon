package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.rift.PocketRealm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the HARD GATE of the exclusive relics, the one place that answers "may this loot roll give a relic of tomb X": the roll must happen in the Mega-Dungeon's dimension, at a place inside
 * the cell of a LIVE exclusive pocket whose dungeon id is X, with the roller (the player who opens the chest or lifts the pedestal, the boss that dies) standing in that same cell, and at the
 * stage the table names (the boss table only after the boss is dead, the treasury tables only with the seal open). The decision itself is {@link PocketRules#gate}, pure and tested; this
 * class only reads the world for its inputs. It is asked by the loot condition {@code nusmp:in_pocket} (every pool of a pocket's tables carries it) and again by the relic entry
 * {@code nusmp:pocket_relic} itself, so a table that forgot its condition still cannot hand a relic out anywhere else.
 */
public final class PocketGate {
    private PocketGate() {}

    /** May a roll at {@code at} by {@code roller} (may be null) give what the tomb {@code pocket} guards, at this stage? */
    public static boolean allows(ServerLevel level, Vec3 at, Entity roller, String pocket, PocketRules.Stage stage) {
        if (level == null || at == null || pocket == null || !Labyrinth.inMega(level)) return false;
        int slot = PocketCell.slotAt(at.x, at.z);
        if (slot < 0) return false;
        if (roller != null && PocketCell.slotAt(roller.getX(), roller.getZ()) != slot) return false;          // the player is not in the tomb the chest is in
        PocketRealm.Pocket live = PocketRealm.get(level.getServer()).at(slot);
        if (live == null) return false;
        return PocketRules.gate(live.pocketId, pocket, stage, live.bossDead, live.sealOpen);
    }
}
